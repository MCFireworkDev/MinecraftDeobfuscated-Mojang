package net.minecraft.network.protocol.game;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundSelectTradePacket implements Packet<ServerGamePacketListener> {
	private final int item;

	@Environment(EnvType.CLIENT)
	public ServerboundSelectTradePacket(int i) {
		this.item = i;
	}

	public ServerboundSelectTradePacket(FriendlyByteBuf friendlyByteBuf) {
		this.item = friendlyByteBuf.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeVarInt(this.item);
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleSelectTrade(this);
	}

	public int getItem() {
		return this.item;
	}
}
