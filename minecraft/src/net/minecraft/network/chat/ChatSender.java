package net.minecraft.network.chat;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;

public record ChatSender(UUID profileId, Component name, @Nullable Component teamName) {
	public ChatSender(UUID uUID, Component component) {
		this(uUID, component, null);
	}

	public ChatSender(FriendlyByteBuf friendlyByteBuf) {
		this(friendlyByteBuf.readUUID(), friendlyByteBuf.readComponent(), friendlyByteBuf.readNullable(FriendlyByteBuf::readComponent));
	}

	public static ChatSender system(Component component) {
		return new ChatSender(Util.NIL_UUID, component);
	}

	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeUUID(this.profileId);
		friendlyByteBuf.writeComponent(this.name);
		friendlyByteBuf.writeNullable(this.teamName, FriendlyByteBuf::writeComponent);
	}

	public ChatSender withTeamName(Component component) {
		return new ChatSender(this.profileId, this.name, component);
	}

	public boolean isPlayer() {
		return !this.profileId.equals(Util.NIL_UUID);
	}
}
