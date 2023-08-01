package net.minecraft.network.protocol.game;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public final class ClientboundStartConfigurationPacket extends Record implements Packet<ClientGamePacketListener> {
	public ClientboundStartConfigurationPacket(FriendlyByteBuf friendlyByteBuf) {
		this();
	}

	public ClientboundStartConfigurationPacket() {
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleConfigurationStart(this);
	}

	@Override
	public ConnectionProtocol nextProtocol() {
		return ConnectionProtocol.CONFIGURATION;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ClientboundStartConfigurationPacket,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ClientboundStartConfigurationPacket,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ClientboundStartConfigurationPacket,"">(this, object);
	}
}
