package net.minecraft.network.chat;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.SignatureValidator;

public record MessageSignature(byte[] bytes) {
	public static final int BYTES = 256;

	public MessageSignature(byte[] bs) {
		Preconditions.checkState(bs.length == 256, "Invalid message signature size");
		this.bytes = bs;
	}

	public static MessageSignature read(FriendlyByteBuf friendlyByteBuf) {
		byte[] bs = new byte[256];
		friendlyByteBuf.readBytes(bs);
		return new MessageSignature(bs);
	}

	public static void write(FriendlyByteBuf friendlyByteBuf, MessageSignature messageSignature) {
		friendlyByteBuf.writeBytes(messageSignature.bytes);
	}

	public boolean verify(SignatureValidator signatureValidator, SignatureUpdater signatureUpdater) {
		return signatureValidator.validate(signatureUpdater, this.bytes);
	}

	public ByteBuffer asByteBuffer() {
		return ByteBuffer.wrap(this.bytes);
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else {
			if (object instanceof MessageSignature messageSignature && Arrays.equals(this.bytes, messageSignature.bytes)) {
				return true;
			}

			return false;
		}
	}

	public int hashCode() {
		return Arrays.hashCode(this.bytes);
	}

	public String toString() {
		return Base64.getEncoder().encodeToString(this.bytes);
	}

	public MessageSignature.Packed pack(MessageSignature.Packer packer) {
		int i = packer.pack(this);
		return i != -1 ? new MessageSignature.Packed(i) : new MessageSignature.Packed(this);
	}

	public static record Packed(int id, @Nullable MessageSignature fullSignature) {
		public static final int FULL_SIGNATURE = -1;

		public Packed(MessageSignature messageSignature) {
			this(-1, messageSignature);
		}

		public Packed(int i) {
			this(i, null);
		}

		public static MessageSignature.Packed read(FriendlyByteBuf friendlyByteBuf) {
			int i = friendlyByteBuf.readVarInt() - 1;
			return i == -1 ? new MessageSignature.Packed(MessageSignature.read(friendlyByteBuf)) : new MessageSignature.Packed(i);
		}

		public static void write(FriendlyByteBuf friendlyByteBuf, MessageSignature.Packed packed) {
			friendlyByteBuf.writeVarInt(packed.id() + 1);
			if (packed.fullSignature() != null) {
				MessageSignature.write(friendlyByteBuf, packed.fullSignature());
			}
		}

		public Optional<MessageSignature> unpack(MessageSignature.Unpacker unpacker) {
			return this.fullSignature != null ? Optional.of(this.fullSignature) : Optional.ofNullable(unpacker.unpack(this.id));
		}
	}

	public interface Packer {
		int NOT_FOUND = -1;

		int pack(MessageSignature messageSignature);
	}

	public interface Unpacker {
		@Nullable
		MessageSignature unpack(int i);
	}
}
