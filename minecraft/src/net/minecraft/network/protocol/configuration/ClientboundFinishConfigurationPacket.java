package net.minecraft.network.protocol.configuration;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public final class ClientboundFinishConfigurationPacket extends Record implements Packet<ClientConfigurationPacketListener> {
	public ClientboundFinishConfigurationPacket(FriendlyByteBuf friendlyByteBuf) {
		this();
	}

	public ClientboundFinishConfigurationPacket() {
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	public void handle(ClientConfigurationPacketListener clientConfigurationPacketListener) {
		clientConfigurationPacketListener.handleConfigurationFinished(this);
	}

	@Override
	public ConnectionProtocol nextProtocol() {
		return ConnectionProtocol.PLAY;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ClientboundFinishConfigurationPacket,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ClientboundFinishConfigurationPacket,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ClientboundFinishConfigurationPacket,"">(this, object);
	}
}
