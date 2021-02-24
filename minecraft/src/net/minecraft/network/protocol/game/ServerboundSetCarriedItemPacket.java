package net.minecraft.network.protocol.game;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundSetCarriedItemPacket implements Packet<ServerGamePacketListener> {
	private final int slot;

	@Environment(EnvType.CLIENT)
	public ServerboundSetCarriedItemPacket(int i) {
		this.slot = i;
	}

	public ServerboundSetCarriedItemPacket(FriendlyByteBuf friendlyByteBuf) {
		this.slot = friendlyByteBuf.readShort();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeShort(this.slot);
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleSetCarriedItem(this);
	}

	public int getSlot() {
		return this.slot;
	}
}
