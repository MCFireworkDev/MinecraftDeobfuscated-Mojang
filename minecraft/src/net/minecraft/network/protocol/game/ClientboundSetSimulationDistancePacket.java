package net.minecraft.network.protocol.game;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public final class ClientboundSetSimulationDistancePacket extends Record implements Packet<ClientGamePacketListener> {
	private final int simulationDistance;

	public ClientboundSetSimulationDistancePacket(FriendlyByteBuf friendlyByteBuf) {
		this(friendlyByteBuf.readVarInt());
	}

	public ClientboundSetSimulationDistancePacket(int i) {
		this.simulationDistance = i;
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeVarInt(this.simulationDistance);
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleSetSimulationDistance(this);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ClientboundSetSimulationDistancePacket,"simulationDistance",ClientboundSetSimulationDistancePacket::simulationDistance>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ClientboundSetSimulationDistancePacket,"simulationDistance",ClientboundSetSimulationDistancePacket::simulationDistance>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ClientboundSetSimulationDistancePacket,"simulationDistance",ClientboundSetSimulationDistancePacket::simulationDistance>(
			this, object
		);
	}

	public int simulationDistance() {
		return this.simulationDistance;
	}
}
