package net.minecraft.client.multiplayer.chat;

import java.time.Instant;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageValidator;

@Environment(EnvType.CLIENT)
public enum ChatTrustLevel {
	SECURE,
	MODIFIED,
	FILTERED,
	NOT_SECURE,
	BROKEN_CHAIN;

	public static ChatTrustLevel evaluate(PlayerChatMessage playerChatMessage, Component component, @Nullable PlayerInfo playerInfo, Instant instant) {
		if (playerInfo == null) {
			return NOT_SECURE;
		} else {
			SignedMessageValidator.State state = playerInfo.getMessageValidator().validateMessage(playerChatMessage);
			if (state == SignedMessageValidator.State.BROKEN_CHAIN) {
				return BROKEN_CHAIN;
			} else if (state == SignedMessageValidator.State.NOT_SECURE) {
				return NOT_SECURE;
			} else if (playerChatMessage.hasExpiredClient(instant)) {
				return NOT_SECURE;
			} else if (!playerChatMessage.filterMask().isEmpty()) {
				return FILTERED;
			} else if (playerChatMessage.unsignedContent().isPresent()) {
				return MODIFIED;
			} else {
				return !component.contains(playerChatMessage.signedContent().decorated()) ? MODIFIED : SECURE;
			}
		}
	}

	public boolean isNotSecure() {
		return this == NOT_SECURE || this == BROKEN_CHAIN;
	}

	@Nullable
	public GuiMessageTag createTag(PlayerChatMessage playerChatMessage) {
		return switch(this) {
			case MODIFIED -> GuiMessageTag.chatModified(playerChatMessage.signedContent().plain());
			case FILTERED -> GuiMessageTag.chatFiltered();
			case NOT_SECURE -> GuiMessageTag.chatNotSecure();
			default -> null;
		};
	}
}
