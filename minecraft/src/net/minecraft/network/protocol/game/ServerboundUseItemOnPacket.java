package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class ServerboundUseItemOnPacket implements Packet<ServerGamePacketListener> {
	private final BlockHitResult blockHit;
	private final InteractionHand hand;
	private final int sequence;

	public ServerboundUseItemOnPacket(InteractionHand interactionHand, BlockHitResult blockHitResult, int i) {
		this.hand = interactionHand;
		this.blockHit = blockHitResult;
		this.sequence = i;
	}

	public ServerboundUseItemOnPacket(FriendlyByteBuf friendlyByteBuf) {
		this.hand = friendlyByteBuf.readEnum(InteractionHand.class);
		this.blockHit = friendlyByteBuf.readBlockHitResult();
		this.sequence = friendlyByteBuf.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeEnum(this.hand);
		friendlyByteBuf.writeBlockHitResult(this.blockHit);
		friendlyByteBuf.writeVarInt(this.sequence);
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleUseItemOn(this);
	}

	public InteractionHand getHand() {
		return this.hand;
	}

	public BlockHitResult getHitResult() {
		return this.blockHit;
	}

	public int getSequence() {
		return this.sequence;
	}
}
