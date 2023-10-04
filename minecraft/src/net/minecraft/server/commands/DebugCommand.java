package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ProfileResults;
import org.slf4j.Logger;

public class DebugCommand {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(Component.translatable("commands.debug.notRunning"));
	private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(
		Component.translatable("commands.debug.alreadyRunning")
	);

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(
			Commands.literal("debug")
				.requires(commandSourceStack -> commandSourceStack.hasPermission(3))
				.then(Commands.literal("start").executes(commandContext -> start(commandContext.getSource())))
				.then(Commands.literal("stop").executes(commandContext -> stop(commandContext.getSource())))
				.then(
					Commands.literal("function")
						.requires(commandSourceStack -> commandSourceStack.hasPermission(3))
						.then(
							Commands.argument("name", FunctionArgument.functions())
								.suggests(FunctionCommand.SUGGEST_FUNCTION)
								.executes(commandContext -> traceFunction(commandContext.getSource(), FunctionArgument.getFunctions(commandContext, "name")))
						)
				)
		);
	}

	private static int start(CommandSourceStack commandSourceStack) throws CommandSyntaxException {
		MinecraftServer minecraftServer = commandSourceStack.getServer();
		if (minecraftServer.isTimeProfilerRunning()) {
			throw ERROR_ALREADY_RUNNING.create();
		} else {
			minecraftServer.startTimeProfiler();
			commandSourceStack.sendSuccess(() -> Component.translatable("commands.debug.started"), true);
			return 0;
		}
	}

	private static int stop(CommandSourceStack commandSourceStack) throws CommandSyntaxException {
		MinecraftServer minecraftServer = commandSourceStack.getServer();
		if (!minecraftServer.isTimeProfilerRunning()) {
			throw ERROR_NOT_RUNNING.create();
		} else {
			ProfileResults profileResults = minecraftServer.stopTimeProfiler();
			double d = (double)profileResults.getNanoDuration() / (double)TimeUtil.NANOSECONDS_PER_SECOND;
			double e = (double)profileResults.getTickDuration() / d;
			commandSourceStack.sendSuccess(
				() -> Component.translatable(
						"commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d), profileResults.getTickDuration(), String.format(Locale.ROOT, "%.2f", e)
					),
				true
			);
			return (int)e;
		}
	}

	private static int traceFunction(CommandSourceStack commandSourceStack, Collection<CommandFunction> collection) {
		int i = 0;
		MinecraftServer minecraftServer = commandSourceStack.getServer();
		String string = "debug-trace-" + Util.getFilenameFormattedDateTime() + ".txt";

		try {
			Path path = minecraftServer.getFile("debug").toPath();
			Files.createDirectories(path);
			Writer writer = Files.newBufferedWriter(path.resolve(string), StandardCharsets.UTF_8);

			try {
				PrintWriter printWriter = new PrintWriter(writer);

				for(CommandFunction commandFunction : collection) {
					printWriter.println(commandFunction.getId());
					DebugCommand.Tracer tracer = new DebugCommand.Tracer(printWriter);

					try {
						i += commandSourceStack.getServer()
							.getFunctions()
							.execute(commandFunction, commandSourceStack.withSource(tracer).withMaximumPermission(2), tracer, null);
					} catch (FunctionInstantiationException var13) {
						commandSourceStack.sendFailure(var13.messageComponent());
					}
				}
			} catch (Throwable var14) {
				if (writer != null) {
					try {
						writer.close();
					} catch (Throwable var12) {
						var14.addSuppressed(var12);
					}
				}

				throw var14;
			}

			if (writer != null) {
				writer.close();
			}
		} catch (IOException | UncheckedIOException var15) {
			LOGGER.warn("Tracing failed", var15);
			commandSourceStack.sendFailure(Component.translatable("commands.debug.function.traceFailed"));
		}

		int j = i;
		if (collection.size() == 1) {
			commandSourceStack.sendSuccess(
				() -> Component.translatable(
						"commands.debug.function.success.single", j, Component.translationArg(((CommandFunction)collection.iterator().next()).getId()), string
					),
				true
			);
		} else {
			commandSourceStack.sendSuccess(() -> Component.translatable("commands.debug.function.success.multiple", j, collection.size(), string), true);
		}

		return i;
	}

	static class Tracer implements ServerFunctionManager.TraceCallbacks, CommandSource {
		public static final int INDENT_OFFSET = 1;
		private final PrintWriter output;
		private int lastIndent;
		private boolean waitingForResult;

		Tracer(PrintWriter printWriter) {
			this.output = printWriter;
		}

		private void indentAndSave(int i) {
			this.printIndent(i);
			this.lastIndent = i;
		}

		private void printIndent(int i) {
			for(int j = 0; j < i + 1; ++j) {
				this.output.write("    ");
			}
		}

		private void newLine() {
			if (this.waitingForResult) {
				this.output.println();
				this.waitingForResult = false;
			}
		}

		@Override
		public void onCommand(int i, String string) {
			this.newLine();
			this.indentAndSave(i);
			this.output.print("[C] ");
			this.output.print(string);
			this.waitingForResult = true;
		}

		@Override
		public void onReturn(int i, String string, int j) {
			if (this.waitingForResult) {
				this.output.print(" -> ");
				this.output.println(j);
				this.waitingForResult = false;
			} else {
				this.indentAndSave(i);
				this.output.print("[R = ");
				this.output.print(j);
				this.output.print("] ");
				this.output.println(string);
			}
		}

		@Override
		public void onCall(int i, ResourceLocation resourceLocation, int j) {
			this.newLine();
			this.indentAndSave(i);
			this.output.print("[F] ");
			this.output.print(resourceLocation);
			this.output.print(" size=");
			this.output.println(j);
		}

		@Override
		public void onError(int i, String string) {
			this.newLine();
			this.indentAndSave(i + 1);
			this.output.print("[E] ");
			this.output.print(string);
		}

		@Override
		public void sendSystemMessage(Component component) {
			this.newLine();
			this.printIndent(this.lastIndent + 1);
			this.output.print("[M] ");
			this.output.println(component.getString());
		}

		@Override
		public boolean acceptsSuccess() {
			return true;
		}

		@Override
		public boolean acceptsFailure() {
			return true;
		}

		@Override
		public boolean shouldInformAdmins() {
			return false;
		}

		@Override
		public boolean alwaysAccepts() {
			return true;
		}
	}
}
