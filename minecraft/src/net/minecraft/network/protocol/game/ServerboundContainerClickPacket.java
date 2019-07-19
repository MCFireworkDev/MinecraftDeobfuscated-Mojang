package net.minecraft.network.protocol.game;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class ServerboundContainerClickPacket implements Packet<ServerGamePacketListener> {
	private int containerId;
	private int slotNum;
	private int buttonNum;
	private short uid;
	private ItemStack itemStack = ItemStack.EMPTY;
	private ClickType clickType;

	public ServerboundContainerClickPacket() {
	}

	@Environment(EnvType.CLIENT)
	public ServerboundContainerClickPacket(int i, int j, int k, ClickType clickType, ItemStack itemStack, short s) {
		this.containerId = i;
		this.slotNum = j;
		this.buttonNum = k;
		this.itemStack = itemStack.copy();
		this.uid = s;
		this.clickType = clickType;
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleContainerClick(this);
	}

	@Override
	public void read(FriendlyByteBuf friendlyByteBuf) throws IOException {
		this.containerId = friendlyByteBuf.readByte();
		this.slotNum = friendlyByteBuf.readShort();
		this.buttonNum = friendlyByteBuf.readByte();
		this.uid = friendlyByteBuf.readShort();
		this.clickType = friendlyByteBuf.readEnum(ClickType.class);
		this.itemStack = friendlyByteBuf.readItem();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) throws IOException {
		friendlyByteBuf.writeByte(this.containerId);
		friendlyByteBuf.writeShort(this.slotNum);
		friendlyByteBuf.writeByte(this.buttonNum);
		friendlyByteBuf.writeShort(this.uid);
		friendlyByteBuf.writeEnum(this.clickType);
		friendlyByteBuf.writeItem(this.itemStack);
	}

	public int getContainerId() {
		return this.containerId;
	}

	public int getSlotNum() {
		return this.slotNum;
	}

	public int getButtonNum() {
		return this.buttonNum;
	}

	public short getUid() {
		return this.uid;
	}

	public ItemStack getItem() {
		return this.itemStack;
	}

	public ClickType getClickType() {
		return this.clickType;
	}
}
