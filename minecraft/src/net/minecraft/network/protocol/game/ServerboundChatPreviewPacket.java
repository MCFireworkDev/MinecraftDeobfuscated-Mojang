package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.StringUtil;

public record ServerboundChatPreviewPacket(int queryId, String query) implements Packet<ServerGamePacketListener> {
	public ServerboundChatPreviewPacket(int i, String string) {
		string = StringUtil.trimChatMessage(string);
		this.queryId = i;
		this.query = string;
	}

	public ServerboundChatPreviewPacket(FriendlyByteBuf friendlyByteBuf) {
		this(friendlyByteBuf.readInt(), friendlyByteBuf.readUtf(256));
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeInt(this.queryId);
		friendlyByteBuf.writeUtf(this.query, 256);
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleChatPreview(this);
	}
}
