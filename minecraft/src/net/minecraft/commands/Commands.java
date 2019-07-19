package net.minecraft.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.commands.AdvancementCommands;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.commands.ClearInventoryCommands;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.commands.DataPackCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.DebugCommand;
import net.minecraft.server.commands.DefaultGameModeCommands;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.server.commands.EmoteCommands;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.ExperienceCommand;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.commands.HelpCommand;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.commands.LootCommand;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.ParticleCommand;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.commands.RecipeCommand;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.commands.ReplaceItemCommand;
import net.minecraft.server.commands.SaveAllCommand;
import net.minecraft.server.commands.SaveOffCommand;
import net.minecraft.server.commands.SaveOnCommand;
import net.minecraft.server.commands.SayCommand;
import net.minecraft.server.commands.ScheduleCommand;
import net.minecraft.server.commands.ScoreboardCommand;
import net.minecraft.server.commands.SeedCommand;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.commands.SetPlayerIdleTimeoutCommand;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.commands.SetWorldSpawnCommand;
import net.minecraft.server.commands.SpreadPlayersCommand;
import net.minecraft.server.commands.StopCommand;
import net.minecraft.server.commands.StopSoundCommand;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.commands.TagCommand;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.server.commands.TeamMsgCommand;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.commands.TellRawCommand;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.commands.TitleCommand;
import net.minecraft.server.commands.TriggerCommand;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Commands {
	private static final Logger LOGGER = LogManager.getLogger();
	private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

	public Commands(boolean bl) {
		AdvancementCommands.register(this.dispatcher);
		ExecuteCommand.register(this.dispatcher);
		BossBarCommands.register(this.dispatcher);
		ClearInventoryCommands.register(this.dispatcher);
		CloneCommands.register(this.dispatcher);
		DataCommands.register(this.dispatcher);
		DataPackCommand.register(this.dispatcher);
		DebugCommand.register(this.dispatcher);
		DefaultGameModeCommands.register(this.dispatcher);
		DifficultyCommand.register(this.dispatcher);
		EffectCommands.register(this.dispatcher);
		EmoteCommands.register(this.dispatcher);
		EnchantCommand.register(this.dispatcher);
		ExperienceCommand.register(this.dispatcher);
		FillCommand.register(this.dispatcher);
		ForceLoadCommand.register(this.dispatcher);
		FunctionCommand.register(this.dispatcher);
		GameModeCommand.register(this.dispatcher);
		GameRuleCommand.register(this.dispatcher);
		GiveCommand.register(this.dispatcher);
		HelpCommand.register(this.dispatcher);
		KickCommand.register(this.dispatcher);
		KillCommand.register(this.dispatcher);
		ListPlayersCommand.register(this.dispatcher);
		LocateCommand.register(this.dispatcher);
		LootCommand.register(this.dispatcher);
		MsgCommand.register(this.dispatcher);
		ParticleCommand.register(this.dispatcher);
		PlaySoundCommand.register(this.dispatcher);
		PublishCommand.register(this.dispatcher);
		ReloadCommand.register(this.dispatcher);
		RecipeCommand.register(this.dispatcher);
		ReplaceItemCommand.register(this.dispatcher);
		SayCommand.register(this.dispatcher);
		ScheduleCommand.register(this.dispatcher);
		ScoreboardCommand.register(this.dispatcher);
		SeedCommand.register(this.dispatcher);
		SetBlockCommand.register(this.dispatcher);
		SetSpawnCommand.register(this.dispatcher);
		SetWorldSpawnCommand.register(this.dispatcher);
		SpreadPlayersCommand.register(this.dispatcher);
		StopSoundCommand.register(this.dispatcher);
		SummonCommand.register(this.dispatcher);
		TagCommand.register(this.dispatcher);
		TeamCommand.register(this.dispatcher);
		TeamMsgCommand.register(this.dispatcher);
		TeleportCommand.register(this.dispatcher);
		TellRawCommand.register(this.dispatcher);
		TimeCommand.register(this.dispatcher);
		TitleCommand.register(this.dispatcher);
		TriggerCommand.register(this.dispatcher);
		WeatherCommand.register(this.dispatcher);
		WorldBorderCommand.register(this.dispatcher);
		if (bl) {
			BanIpCommands.register(this.dispatcher);
			BanListCommands.register(this.dispatcher);
			BanPlayerCommands.register(this.dispatcher);
			DeOpCommands.register(this.dispatcher);
			OpCommand.register(this.dispatcher);
			PardonCommand.register(this.dispatcher);
			PardonIpCommand.register(this.dispatcher);
			SaveAllCommand.register(this.dispatcher);
			SaveOffCommand.register(this.dispatcher);
			SaveOnCommand.register(this.dispatcher);
			SetPlayerIdleTimeoutCommand.register(this.dispatcher);
			StopCommand.register(this.dispatcher);
			WhitelistCommand.register(this.dispatcher);
		}

		this.dispatcher
			.findAmbiguities(
				(commandNode, commandNode2, commandNode3, collection) -> LOGGER.warn(
						"Ambiguity between arguments {} and {} with inputs: {}", this.dispatcher.getPath(commandNode2), this.dispatcher.getPath(commandNode3), collection
					)
			);
		this.dispatcher.setConsumer((commandContext, blx, i) -> commandContext.getSource().onCommandComplete(commandContext, blx, i));
	}

	public int performCommand(CommandSourceStack commandSourceStack, String string) {
		StringReader stringReader = new StringReader(string);
		if (stringReader.canRead() && stringReader.peek() == '/') {
			stringReader.skip();
		}

		commandSourceStack.getServer().getProfiler().push(string);

		int i;
		try {
			try {
				return this.dispatcher.execute(stringReader, commandSourceStack);
			} catch (CommandRuntimeException var13) {
				commandSourceStack.sendFailure(var13.getComponent());
				return 0;
			} catch (CommandSyntaxException var14) {
				commandSourceStack.sendFailure(ComponentUtils.fromMessage(var14.getRawMessage()));
				if (var14.getInput() != null && var14.getCursor() >= 0) {
					i = Math.min(var14.getInput().length(), var14.getCursor());
					Component component = new TextComponent("")
						.withStyle(ChatFormatting.GRAY)
						.withStyle(style -> style.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, string)));
					if (i > 10) {
						component.append("...");
					}

					component.append(var14.getInput().substring(Math.max(0, i - 10), i));
					if (i < var14.getInput().length()) {
						Component component2 = new TextComponent(var14.getInput().substring(i)).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.UNDERLINE});
						component.append(component2);
					}

					component.append(new TranslatableComponent("command.context.here").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC}));
					commandSourceStack.sendFailure(component);
				}
			} catch (Exception var15) {
				Component component3 = new TextComponent(var15.getMessage() == null ? var15.getClass().getName() : var15.getMessage());
				if (LOGGER.isDebugEnabled()) {
					StackTraceElement[] stackTraceElements = var15.getStackTrace();

					for(int j = 0; j < Math.min(stackTraceElements.length, 3); ++j) {
						component3.append("\n\n")
							.append(stackTraceElements[j].getMethodName())
							.append("\n ")
							.append(stackTraceElements[j].getFileName())
							.append(":")
							.append(String.valueOf(stackTraceElements[j].getLineNumber()));
					}
				}

				commandSourceStack.sendFailure(
					new TranslatableComponent("command.failed").withStyle(style -> style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component3)))
				);
				return 0;
			}

			i = 0;
		} finally {
			commandSourceStack.getServer().getProfiler().pop();
		}

		return i;
	}

	public void sendCommands(ServerPlayer serverPlayer) {
		Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map = Maps.<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>>newHashMap(
			
		);
		RootCommandNode<SharedSuggestionProvider> rootCommandNode = new RootCommandNode<>();
		map.put(this.dispatcher.getRoot(), rootCommandNode);
		this.fillUsableCommands(this.dispatcher.getRoot(), rootCommandNode, serverPlayer.createCommandSourceStack(), map);
		serverPlayer.connection.send(new ClientboundCommandsPacket(rootCommandNode));
	}

	private void fillUsableCommands(
		CommandNode<CommandSourceStack> commandNode,
		CommandNode<SharedSuggestionProvider> commandNode2,
		CommandSourceStack commandSourceStack,
		Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map
	) {
		for(CommandNode<CommandSourceStack> commandNode3 : commandNode.getChildren()) {
			if (commandNode3.canUse(commandSourceStack)) {
				ArgumentBuilder<SharedSuggestionProvider, ?> argumentBuilder = commandNode3.createBuilder();
				argumentBuilder.requires(sharedSuggestionProvider -> true);
				if (argumentBuilder.getCommand() != null) {
					argumentBuilder.executes(commandContext -> 0);
				}

				if (argumentBuilder instanceof RequiredArgumentBuilder) {
					RequiredArgumentBuilder<SharedSuggestionProvider, ?> requiredArgumentBuilder = (RequiredArgumentBuilder)argumentBuilder;
					if (requiredArgumentBuilder.getSuggestionsProvider() != null) {
						requiredArgumentBuilder.suggests(SuggestionProviders.safelySwap(requiredArgumentBuilder.getSuggestionsProvider()));
					}
				}

				if (argumentBuilder.getRedirect() != null) {
					argumentBuilder.redirect((CommandNode<SharedSuggestionProvider>)map.get(argumentBuilder.getRedirect()));
				}

				CommandNode<SharedSuggestionProvider> commandNode4 = argumentBuilder.build();
				map.put(commandNode3, commandNode4);
				commandNode2.addChild(commandNode4);
				if (!commandNode3.getChildren().isEmpty()) {
					this.fillUsableCommands(commandNode3, commandNode4, commandSourceStack, map);
				}
			}
		}
	}

	public static LiteralArgumentBuilder<CommandSourceStack> literal(String string) {
		return LiteralArgumentBuilder.literal(string);
	}

	public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String string, ArgumentType<T> argumentType) {
		return RequiredArgumentBuilder.argument(string, argumentType);
	}

	public static Predicate<String> createValidator(Commands.ParseFunction parseFunction) {
		return string -> {
			try {
				parseFunction.parse(new StringReader(string));
				return true;
			} catch (CommandSyntaxException var3) {
				return false;
			}
		};
	}

	public CommandDispatcher<CommandSourceStack> getDispatcher() {
		return this.dispatcher;
	}

	@FunctionalInterface
	public interface ParseFunction {
		void parse(StringReader stringReader) throws CommandSyntaxException;
	}
}
