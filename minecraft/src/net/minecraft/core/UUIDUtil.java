package net.minecraft.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.util.UndashedUuid;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.Util;

public final class UUIDUtil {
	public static final Codec<UUID> CODEC = Codec.INT_STREAM
		.comapFlatMap(intStream -> Util.fixedSize(intStream, 4).map(UUIDUtil::uuidFromIntArray), uUID -> Arrays.stream(uuidToIntArray(uUID)));
	public static final Codec<Set<UUID>> CODEC_SET = Codec.list(CODEC).xmap(Sets::newHashSet, Lists::newArrayList);
	public static final Codec<UUID> STRING_CODEC = Codec.STRING.comapFlatMap(string -> {
		try {
			return DataResult.success(UUID.fromString(string), Lifecycle.stable());
		} catch (IllegalArgumentException var2) {
			return DataResult.error(() -> "Invalid UUID " + string + ": " + var2.getMessage());
		}
	}, UUID::toString);
	public static Codec<UUID> AUTHLIB_CODEC = Codec.either(CODEC, Codec.STRING.comapFlatMap(string -> {
		try {
			return DataResult.success(UndashedUuid.fromStringLenient(string), Lifecycle.stable());
		} catch (IllegalArgumentException var2) {
			return DataResult.error(() -> "Invalid UUID " + string + ": " + var2.getMessage());
		}
	}, UndashedUuid::toString)).xmap(either -> either.map(uUID -> uUID, uUID -> uUID), Either::right);
	public static Codec<UUID> LENIENT_CODEC = Codec.either(CODEC, STRING_CODEC).xmap(either -> either.map(uUID -> uUID, uUID -> uUID), Either::left);
	public static final int UUID_BYTES = 16;
	private static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";

	private UUIDUtil() {
	}

	public static UUID uuidFromIntArray(int[] is) {
		return new UUID((long)is[0] << 32 | (long)is[1] & 4294967295L, (long)is[2] << 32 | (long)is[3] & 4294967295L);
	}

	public static int[] uuidToIntArray(UUID uUID) {
		long l = uUID.getMostSignificantBits();
		long m = uUID.getLeastSignificantBits();
		return leastMostToIntArray(l, m);
	}

	private static int[] leastMostToIntArray(long l, long m) {
		return new int[]{(int)(l >> 32), (int)l, (int)(m >> 32), (int)m};
	}

	public static byte[] uuidToByteArray(UUID uUID) {
		byte[] bs = new byte[16];
		ByteBuffer.wrap(bs).order(ByteOrder.BIG_ENDIAN).putLong(uUID.getMostSignificantBits()).putLong(uUID.getLeastSignificantBits());
		return bs;
	}

	public static UUID readUUID(Dynamic<?> dynamic) {
		int[] is = dynamic.asIntStream().toArray();
		if (is.length != 4) {
			throw new IllegalArgumentException("Could not read UUID. Expected int-array of length 4, got " + is.length + ".");
		} else {
			return uuidFromIntArray(is);
		}
	}

	public static UUID createOfflinePlayerUUID(String string) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + string).getBytes(StandardCharsets.UTF_8));
	}

	public static GameProfile createOfflineProfile(String string) {
		UUID uUID = createOfflinePlayerUUID(string);
		return new GameProfile(uUID, string);
	}
}
