package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundSignUpdatePacket implements Packet<ServerGamePacketListener> {
	private static final int MAX_STRING_LENGTH = 384;
	private final BlockPos pos;
	private final String[] lines;
	private final boolean isFrontText;

	public ServerboundSignUpdatePacket(BlockPos blockPos, boolean bl, String string, String string2, String string3, String string4) {
		this.pos = blockPos;
		this.isFrontText = bl;
		this.lines = new String[]{string, string2, string3, string4};
	}

	public ServerboundSignUpdatePacket(FriendlyByteBuf friendlyByteBuf) {
		this.pos = friendlyByteBuf.readBlockPos();
		this.isFrontText = friendlyByteBuf.readBoolean();
		this.lines = new String[4];

		for(int i = 0; i < 4; ++i) {
			this.lines[i] = friendlyByteBuf.readUtf(384);
		}
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeBlockPos(this.pos);
		friendlyByteBuf.writeBoolean(this.isFrontText);

		for(int i = 0; i < 4; ++i) {
			friendlyByteBuf.writeUtf(this.lines[i]);
		}
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleSignUpdate(this);
	}

	public BlockPos getPos() {
		return this.pos;
	}

	public boolean isFrontText() {
		return this.isFrontText;
	}

	public String[] getLines() {
		return this.lines;
	}
}
