package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import org.slf4j.Logger;

public class MessageArgument implements SignedArgument<MessageArgument.Message> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
	static final Logger LOGGER = LogUtils.getLogger();

	public static MessageArgument message() {
		return new MessageArgument();
	}

	public static Component getMessage(CommandContext<CommandSourceStack> commandContext, String string) throws CommandSyntaxException {
		MessageArgument.Message message = commandContext.getArgument(string, MessageArgument.Message.class);
		return message.resolveComponent(commandContext.getSource());
	}

	public static MessageArgument.ChatMessage getChatMessage(CommandContext<CommandSourceStack> commandContext, String string) throws CommandSyntaxException {
		MessageArgument.Message message = commandContext.getArgument(string, MessageArgument.Message.class);
		Component component = message.resolveComponent(commandContext.getSource());
		CommandSigningContext commandSigningContext = commandContext.getSource().getSigningContext();
		MessageSignature messageSignature = commandSigningContext.getArgumentSignature(string);
		boolean bl = commandSigningContext.signedArgumentPreview(string);
		return new MessageArgument.ChatMessage(message.text, component, messageSignature, bl);
	}

	public MessageArgument.Message parse(StringReader stringReader) throws CommandSyntaxException {
		return MessageArgument.Message.parseText(stringReader, true);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public Component getPlainSignableComponent(MessageArgument.Message message) {
		return Component.literal(message.getText());
	}

	public CompletableFuture<Component> resolvePreview(CommandSourceStack commandSourceStack, MessageArgument.Message message) throws CommandSyntaxException {
		return message.resolveDecoratedComponent(commandSourceStack);
	}

	@Override
	public Class<MessageArgument.Message> getValueType() {
		return MessageArgument.Message.class;
	}

	static void logResolutionFailure(CommandSourceStack commandSourceStack, CompletableFuture<?> completableFuture) {
		completableFuture.exceptionally(throwable -> {
			LOGGER.error("Encountered unexpected exception while resolving chat message argument from '{}'", commandSourceStack.getDisplayName().getString(), throwable);
			return null;
		});
	}

	public static record ChatMessage(String plain, Component formatted, MessageSignature signature, boolean signedPreview) {
		public CompletableFuture<FilteredText<PlayerChatMessage>> resolve(CommandSourceStack commandSourceStack) {
			CompletableFuture<FilteredText<PlayerChatMessage>> completableFuture = this.filterComponent(commandSourceStack, this.formatted)
				.thenComposeAsync(filteredText -> {
					ChatDecorator chatDecorator = commandSourceStack.getServer().getChatDecorator();
					return chatDecorator.decorateChat(commandSourceStack.getPlayer(), filteredText, this.signature, this.signedPreview);
				}, commandSourceStack.getServer())
				.thenApply(filteredText -> {
					PlayerChatMessage playerChatMessage = this.getSignedMessage(filteredText);
					if (playerChatMessage != null) {
						this.verify(commandSourceStack, playerChatMessage);
					}
	
					return filteredText;
				});
			MessageArgument.logResolutionFailure(commandSourceStack, completableFuture);
			return completableFuture;
		}

		@Nullable
		private PlayerChatMessage getSignedMessage(FilteredText<PlayerChatMessage> filteredText) {
			if (this.signature.isValid()) {
				return this.signedPreview ? (PlayerChatMessage)filteredText.raw() : PlayerChatMessage.signed(this.plain, this.signature);
			} else {
				return null;
			}
		}

		private void verify(CommandSourceStack commandSourceStack, PlayerChatMessage playerChatMessage) {
			if (!playerChatMessage.verify(commandSourceStack)) {
				MessageArgument.LOGGER
					.warn("{} sent message with invalid signature: '{}'", commandSourceStack.getDisplayName().getString(), playerChatMessage.signedContent().getString());
			}

			if (playerChatMessage.hasExpiredServer(Instant.now())) {
				MessageArgument.LOGGER
					.warn(
						"{} sent expired chat: '{}'. Is the client/server system time unsynchronized?",
						commandSourceStack.getDisplayName().getString(),
						playerChatMessage.signedContent().getString()
					);
			}
		}

		private CompletableFuture<FilteredText<Component>> filterComponent(CommandSourceStack commandSourceStack, Component component) {
			ServerPlayer serverPlayer = commandSourceStack.getPlayer();
			return serverPlayer != null
				? serverPlayer.getTextFilter().processStreamComponent(component)
				: CompletableFuture.completedFuture(FilteredText.<Component>passThrough(component));
		}
	}

	public static class Message {
		final String text;
		private final MessageArgument.Part[] parts;

		public Message(String string, MessageArgument.Part[] parts) {
			this.text = string;
			this.parts = parts;
		}

		public String getText() {
			return this.text;
		}

		public MessageArgument.Part[] getParts() {
			return this.parts;
		}

		CompletableFuture<Component> resolveDecoratedComponent(CommandSourceStack commandSourceStack) throws CommandSyntaxException {
			Component component = this.resolveComponent(commandSourceStack);
			CompletableFuture<Component> completableFuture = commandSourceStack.getServer().getChatDecorator().decorate(commandSourceStack.getPlayer(), component);
			MessageArgument.logResolutionFailure(commandSourceStack, completableFuture);
			return completableFuture;
		}

		Component resolveComponent(CommandSourceStack commandSourceStack) throws CommandSyntaxException {
			return this.toComponent(commandSourceStack, commandSourceStack.hasPermission(2));
		}

		public Component toComponent(CommandSourceStack commandSourceStack, boolean bl) throws CommandSyntaxException {
			if (this.parts.length != 0 && bl) {
				MutableComponent mutableComponent = Component.literal(this.text.substring(0, this.parts[0].getStart()));
				int i = this.parts[0].getStart();

				for(MessageArgument.Part part : this.parts) {
					Component component = part.toComponent(commandSourceStack);
					if (i < part.getStart()) {
						mutableComponent.append(this.text.substring(i, part.getStart()));
					}

					if (component != null) {
						mutableComponent.append(component);
					}

					i = part.getEnd();
				}

				if (i < this.text.length()) {
					mutableComponent.append(this.text.substring(i));
				}

				return mutableComponent;
			} else {
				return Component.literal(this.text);
			}
		}

		public static MessageArgument.Message parseText(StringReader stringReader, boolean bl) throws CommandSyntaxException {
			String string = stringReader.getString().substring(stringReader.getCursor(), stringReader.getTotalLength());
			if (!bl) {
				stringReader.setCursor(stringReader.getTotalLength());
				return new MessageArgument.Message(string, new MessageArgument.Part[0]);
			} else {
				List<MessageArgument.Part> list = Lists.<MessageArgument.Part>newArrayList();
				int i = stringReader.getCursor();

				while(true) {
					int j;
					EntitySelector entitySelector;
					while(true) {
						if (!stringReader.canRead()) {
							return new MessageArgument.Message(string, (MessageArgument.Part[])list.toArray(new MessageArgument.Part[0]));
						}

						if (stringReader.peek() == '@') {
							j = stringReader.getCursor();

							try {
								EntitySelectorParser entitySelectorParser = new EntitySelectorParser(stringReader);
								entitySelector = entitySelectorParser.parse();
								break;
							} catch (CommandSyntaxException var8) {
								if (var8.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE && var8.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
									throw var8;
								}

								stringReader.setCursor(j + 1);
							}
						} else {
							stringReader.skip();
						}
					}

					list.add(new MessageArgument.Part(j - i, stringReader.getCursor() - i, entitySelector));
				}
			}
		}
	}

	public static class Part {
		private final int start;
		private final int end;
		private final EntitySelector selector;

		public Part(int i, int j, EntitySelector entitySelector) {
			this.start = i;
			this.end = j;
			this.selector = entitySelector;
		}

		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		public EntitySelector getSelector() {
			return this.selector;
		}

		@Nullable
		public Component toComponent(CommandSourceStack commandSourceStack) throws CommandSyntaxException {
			return EntitySelector.joinNames(this.selector.findEntities(commandSourceStack));
		}
	}
}
