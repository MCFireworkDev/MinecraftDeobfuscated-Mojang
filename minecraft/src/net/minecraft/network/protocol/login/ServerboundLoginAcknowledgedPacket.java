package net.minecraft.network.protocol.login;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public final class ServerboundLoginAcknowledgedPacket extends Record implements Packet<ServerLoginPacketListener> {
	public ServerboundLoginAcknowledgedPacket(FriendlyByteBuf friendlyByteBuf) {
		this();
	}

	public ServerboundLoginAcknowledgedPacket() {
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	public void handle(ServerLoginPacketListener serverLoginPacketListener) {
		serverLoginPacketListener.handleLoginAcknowledgement(this);
	}

	@Override
	public ConnectionProtocol nextProtocol() {
		return ConnectionProtocol.CONFIGURATION;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ServerboundLoginAcknowledgedPacket,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ServerboundLoginAcknowledgedPacket,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ServerboundLoginAcknowledgedPacket,"">(this, object);
	}
}
