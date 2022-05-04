package net.minecraft.network.protocol.game;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.StringUtil;

public class ServerboundChatPacket implements Packet<ServerGamePacketListener> {
	public static final Duration MESSAGE_EXPIRES_AFTER = Duration.ofMinutes(2L);
	private final String message;
	private final Instant timeStamp;
	private final Crypt.SaltSignaturePair saltSignature;

	public ServerboundChatPacket(String string, MessageSignature messageSignature) {
		this.message = StringUtil.trimChatMessage(string);
		this.timeStamp = messageSignature.timeStamp();
		this.saltSignature = messageSignature.saltSignature();
	}

	public ServerboundChatPacket(FriendlyByteBuf friendlyByteBuf) {
		this.message = friendlyByteBuf.readUtf(256);
		this.timeStamp = Instant.ofEpochSecond(friendlyByteBuf.readLong());
		this.saltSignature = new Crypt.SaltSignaturePair(friendlyByteBuf);
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeUtf(this.message);
		friendlyByteBuf.writeLong(this.timeStamp.getEpochSecond());
		this.saltSignature.write(friendlyByteBuf);
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleChat(this);
	}

	public String getMessage() {
		return this.message;
	}

	public MessageSignature getSignature(UUID uUID) {
		return new MessageSignature(uUID, this.timeStamp, this.saltSignature);
	}

	private Instant getExpiresAt() {
		return this.timeStamp.plus(MESSAGE_EXPIRES_AFTER);
	}

	public boolean hasExpired(Instant instant) {
		return instant.isAfter(this.getExpiresAt());
	}
}
