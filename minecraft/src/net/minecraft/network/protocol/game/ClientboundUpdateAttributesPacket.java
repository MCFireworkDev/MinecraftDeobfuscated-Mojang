package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ClientboundUpdateAttributesPacket implements Packet<ClientGamePacketListener> {
	private int entityId;
	private final List<ClientboundUpdateAttributesPacket.AttributeSnapshot> attributes = Lists.<ClientboundUpdateAttributesPacket.AttributeSnapshot>newArrayList();

	public ClientboundUpdateAttributesPacket() {
	}

	public ClientboundUpdateAttributesPacket(int i, Collection<AttributeInstance> collection) {
		this.entityId = i;

		for(AttributeInstance attributeInstance : collection) {
			this.attributes
				.add(
					new ClientboundUpdateAttributesPacket.AttributeSnapshot(
						attributeInstance.getAttribute().getName(), attributeInstance.getBaseValue(), attributeInstance.getModifiers()
					)
				);
		}
	}

	@Override
	public void read(FriendlyByteBuf friendlyByteBuf) throws IOException {
		this.entityId = friendlyByteBuf.readVarInt();
		int i = friendlyByteBuf.readInt();

		for(int j = 0; j < i; ++j) {
			String string = friendlyByteBuf.readUtf(64);
			double d = friendlyByteBuf.readDouble();
			List<AttributeModifier> list = Lists.<AttributeModifier>newArrayList();
			int k = friendlyByteBuf.readVarInt();

			for(int l = 0; l < k; ++l) {
				UUID uUID = friendlyByteBuf.readUUID();
				list.add(
					new AttributeModifier(
						uUID, "Unknown synced attribute modifier", friendlyByteBuf.readDouble(), AttributeModifier.Operation.fromValue(friendlyByteBuf.readByte())
					)
				);
			}

			this.attributes.add(new ClientboundUpdateAttributesPacket.AttributeSnapshot(string, d, list));
		}
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) throws IOException {
		friendlyByteBuf.writeVarInt(this.entityId);
		friendlyByteBuf.writeInt(this.attributes.size());

		for(ClientboundUpdateAttributesPacket.AttributeSnapshot attributeSnapshot : this.attributes) {
			friendlyByteBuf.writeUtf(attributeSnapshot.getName());
			friendlyByteBuf.writeDouble(attributeSnapshot.getBase());
			friendlyByteBuf.writeVarInt(attributeSnapshot.getModifiers().size());

			for(AttributeModifier attributeModifier : attributeSnapshot.getModifiers()) {
				friendlyByteBuf.writeUUID(attributeModifier.getId());
				friendlyByteBuf.writeDouble(attributeModifier.getAmount());
				friendlyByteBuf.writeByte(attributeModifier.getOperation().toValue());
			}
		}
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleUpdateAttributes(this);
	}

	@Environment(EnvType.CLIENT)
	public int getEntityId() {
		return this.entityId;
	}

	@Environment(EnvType.CLIENT)
	public List<ClientboundUpdateAttributesPacket.AttributeSnapshot> getValues() {
		return this.attributes;
	}

	public class AttributeSnapshot {
		private final String name;
		private final double base;
		private final Collection<AttributeModifier> modifiers;

		public AttributeSnapshot(String string, double d, Collection<AttributeModifier> collection) {
			this.name = string;
			this.base = d;
			this.modifiers = collection;
		}

		public String getName() {
			return this.name;
		}

		public double getBase() {
			return this.base;
		}

		public Collection<AttributeModifier> getModifiers() {
			return this.modifiers;
		}
	}
}
