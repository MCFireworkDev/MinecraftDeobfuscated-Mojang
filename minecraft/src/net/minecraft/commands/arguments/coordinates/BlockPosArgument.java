package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;

public class BlockPosArgument implements ArgumentType<Coordinates> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
	public static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(new TranslatableComponent("argument.pos.unloaded"));
	public static final SimpleCommandExceptionType ERROR_OUT_OF_WORLD = new SimpleCommandExceptionType(new TranslatableComponent("argument.pos.outofworld"));

	public static BlockPosArgument blockPos() {
		return new BlockPosArgument();
	}

	public static BlockPos getLoadedBlockPos(CommandContext<CommandSourceStack> commandContext, String string) throws CommandSyntaxException {
		BlockPos blockPos = commandContext.<Coordinates>getArgument(string, Coordinates.class).getBlockPos(commandContext.getSource());
		if (!commandContext.getSource().getLevel().hasChunkAt(blockPos)) {
			throw ERROR_NOT_LOADED.create();
		} else {
			commandContext.getSource().getLevel();
			if (!ServerLevel.isInWorldBounds(blockPos)) {
				throw ERROR_OUT_OF_WORLD.create();
			} else {
				return blockPos;
			}
		}
	}

	public static BlockPos getOrLoadBlockPos(CommandContext<CommandSourceStack> commandContext, String string) throws CommandSyntaxException {
		return commandContext.<Coordinates>getArgument(string, Coordinates.class).getBlockPos(commandContext.getSource());
	}

	public Coordinates parse(StringReader stringReader) throws CommandSyntaxException {
		return (Coordinates)(stringReader.canRead() && stringReader.peek() == '^' ? LocalCoordinates.parse(stringReader) : WorldCoordinates.parseInt(stringReader));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
		if (!(commandContext.getSource() instanceof SharedSuggestionProvider)) {
			return Suggestions.empty();
		} else {
			String string = suggestionsBuilder.getRemaining();
			Collection<SharedSuggestionProvider.TextCoordinates> collection;
			if (!string.isEmpty() && string.charAt(0) == '^') {
				collection = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
			} else {
				collection = ((SharedSuggestionProvider)commandContext.getSource()).getRelevantCoordinates();
			}

			return SharedSuggestionProvider.suggestCoordinates(string, collection, suggestionsBuilder, Commands.createValidator(this::parse));
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
