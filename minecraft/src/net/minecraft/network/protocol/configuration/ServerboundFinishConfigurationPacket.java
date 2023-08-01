package net.minecraft.network.protocol.configuration;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public final class ServerboundFinishConfigurationPacket extends Record implements Packet<ServerConfigurationPacketListener> {
	public ServerboundFinishConfigurationPacket(FriendlyByteBuf friendlyByteBuf) {
		this();
	}

	public ServerboundFinishConfigurationPacket() {
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	public void handle(ServerConfigurationPacketListener serverConfigurationPacketListener) {
		serverConfigurationPacketListener.handleConfigurationFinished(this);
	}

	@Override
	public ConnectionProtocol nextProtocol() {
		return ConnectionProtocol.PLAY;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ServerboundFinishConfigurationPacket,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ServerboundFinishConfigurationPacket,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ServerboundFinishConfigurationPacket,"">(this, object);
	}
}
