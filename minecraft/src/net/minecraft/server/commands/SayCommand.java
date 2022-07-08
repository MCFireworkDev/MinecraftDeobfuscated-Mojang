package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.players.PlayerList;

public class SayCommand {
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(
			Commands.literal("say")
				.requires(commandSourceStack -> commandSourceStack.hasPermission(2))
				.then(
					Commands.argument("message", MessageArgument.message())
						.executes(
							commandContext -> {
								MessageArgument.ChatMessage chatMessage = MessageArgument.getChatMessage(commandContext, "message");
								CommandSourceStack commandSourceStack = commandContext.getSource();
								PlayerList playerList = commandSourceStack.getServer().getPlayerList();
								chatMessage.resolve(commandSourceStack)
									.thenAcceptAsync(
										filteredText -> playerList.broadcastChatMessage(filteredText, commandSourceStack, ChatType.bind(ChatType.SAY_COMMAND, commandSourceStack)),
										commandSourceStack.getServer()
									);
								return 1;
							}
						)
				)
		);
	}
}
