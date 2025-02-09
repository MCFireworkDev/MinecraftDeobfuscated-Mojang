package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddEntityPacket implements Packet<ClientGamePacketListener> {
	private static final double MAGICAL_QUANTIZATION = 8000.0;
	private static final double LIMIT = 3.9;
	private final int id;
	private final UUID uuid;
	private final EntityType<?> type;
	private final double x;
	private final double y;
	private final double z;
	private final int xa;
	private final int ya;
	private final int za;
	private final byte xRot;
	private final byte yRot;
	private final byte yHeadRot;
	private final int data;

	public ClientboundAddEntityPacket(Entity entity) {
		this(entity, 0);
	}

	public ClientboundAddEntityPacket(Entity entity, int i) {
		this(
			entity.getId(),
			entity.getUUID(),
			entity.getX(),
			entity.getY(),
			entity.getZ(),
			entity.getXRot(),
			entity.getYRot(),
			entity.getType(),
			i,
			entity.getDeltaMovement(),
			(double)entity.getYHeadRot()
		);
	}

	public ClientboundAddEntityPacket(Entity entity, int i, BlockPos blockPos) {
		this(
			entity.getId(),
			entity.getUUID(),
			(double)blockPos.getX(),
			(double)blockPos.getY(),
			(double)blockPos.getZ(),
			entity.getXRot(),
			entity.getYRot(),
			entity.getType(),
			i,
			entity.getDeltaMovement(),
			(double)entity.getYHeadRot()
		);
	}

	public ClientboundAddEntityPacket(int i, UUID uUID, double d, double e, double f, float g, float h, EntityType<?> entityType, int j, Vec3 vec3, double k) {
		this.id = i;
		this.uuid = uUID;
		this.x = d;
		this.y = e;
		this.z = f;
		this.xRot = (byte)Mth.floor(g * 256.0F / 360.0F);
		this.yRot = (byte)Mth.floor(h * 256.0F / 360.0F);
		this.yHeadRot = (byte)Mth.floor(k * 256.0 / 360.0);
		this.type = entityType;
		this.data = j;
		this.xa = (int)(Mth.clamp(vec3.x, -3.9, 3.9) * 8000.0);
		this.ya = (int)(Mth.clamp(vec3.y, -3.9, 3.9) * 8000.0);
		this.za = (int)(Mth.clamp(vec3.z, -3.9, 3.9) * 8000.0);
	}

	public ClientboundAddEntityPacket(FriendlyByteBuf friendlyByteBuf) {
		this.id = friendlyByteBuf.readVarInt();
		this.uuid = friendlyByteBuf.readUUID();
		this.type = friendlyByteBuf.readById(BuiltInRegistries.ENTITY_TYPE);
		this.x = friendlyByteBuf.readDouble();
		this.y = friendlyByteBuf.readDouble();
		this.z = friendlyByteBuf.readDouble();
		this.xRot = friendlyByteBuf.readByte();
		this.yRot = friendlyByteBuf.readByte();
		this.yHeadRot = friendlyByteBuf.readByte();
		this.data = friendlyByteBuf.readVarInt();
		this.xa = friendlyByteBuf.readShort();
		this.ya = friendlyByteBuf.readShort();
		this.za = friendlyByteBuf.readShort();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeVarInt(this.id);
		friendlyByteBuf.writeUUID(this.uuid);
		friendlyByteBuf.writeId(BuiltInRegistries.ENTITY_TYPE, this.type);
		friendlyByteBuf.writeDouble(this.x);
		friendlyByteBuf.writeDouble(this.y);
		friendlyByteBuf.writeDouble(this.z);
		friendlyByteBuf.writeByte(this.xRot);
		friendlyByteBuf.writeByte(this.yRot);
		friendlyByteBuf.writeByte(this.yHeadRot);
		friendlyByteBuf.writeVarInt(this.data);
		friendlyByteBuf.writeShort(this.xa);
		friendlyByteBuf.writeShort(this.ya);
		friendlyByteBuf.writeShort(this.za);
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleAddEntity(this);
	}

	public int getId() {
		return this.id;
	}

	public UUID getUUID() {
		return this.uuid;
	}

	public EntityType<?> getType() {
		return this.type;
	}

	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}

	public double getZ() {
		return this.z;
	}

	public double getXa() {
		return (double)this.xa / 8000.0;
	}

	public double getYa() {
		return (double)this.ya / 8000.0;
	}

	public double getZa() {
		return (double)this.za / 8000.0;
	}

	public float getXRot() {
		return (float)(this.xRot * 360) / 256.0F;
	}

	public float getYRot() {
		return (float)(this.yRot * 360) / 256.0F;
	}

	public float getYHeadRot() {
		return (float)(this.yHeadRot * 360) / 256.0F;
	}

	public int getData() {
		return this.data;
	}
}
