package net.minecraft.network.protocol.login.custom;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.FriendlyByteBuf;

public final class DiscardedQueryAnswerPayload extends Record implements CustomQueryAnswerPayload {
	public static final DiscardedQueryAnswerPayload INSTANCE = new DiscardedQueryAnswerPayload();

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",DiscardedQueryAnswerPayload,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",DiscardedQueryAnswerPayload,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",DiscardedQueryAnswerPayload,"">(this, object);
	}
}
