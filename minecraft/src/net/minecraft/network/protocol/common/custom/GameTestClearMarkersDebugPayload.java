package net.minecraft.network.protocol.common.custom;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class GameTestClearMarkersDebugPayload extends Record implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation("debug/game_test_clear");

	public GameTestClearMarkersDebugPayload(FriendlyByteBuf friendlyByteBuf) {
		this();
	}

	public GameTestClearMarkersDebugPayload() {
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",GameTestClearMarkersDebugPayload,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",GameTestClearMarkersDebugPayload,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",GameTestClearMarkersDebugPayload,"">(this, object);
	}
}
