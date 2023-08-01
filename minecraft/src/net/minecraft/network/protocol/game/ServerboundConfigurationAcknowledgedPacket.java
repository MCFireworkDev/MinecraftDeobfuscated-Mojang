package net.minecraft.network.protocol.game;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public final class ServerboundConfigurationAcknowledgedPacket extends Record implements Packet<ServerGamePacketListener> {
	public ServerboundConfigurationAcknowledgedPacket(FriendlyByteBuf friendlyByteBuf) {
		this();
	}

	public ServerboundConfigurationAcknowledgedPacket() {
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleConfigurationAcknowledged(this);
	}

	@Override
	public ConnectionProtocol nextProtocol() {
		return ConnectionProtocol.CONFIGURATION;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ServerboundConfigurationAcknowledgedPacket,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ServerboundConfigurationAcknowledgedPacket,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ServerboundConfigurationAcknowledgedPacket,"">(this, object);
	}
}
