package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.MenuType;

public class ClientboundOpenScreenPacket implements Packet<ClientGamePacketListener> {
	private final int containerId;
	private final int type;
	private final Component title;

	public ClientboundOpenScreenPacket(int i, MenuType<?> menuType, Component component) {
		this.containerId = i;
		this.type = Registry.MENU.getId(menuType);
		this.title = component;
	}

	public ClientboundOpenScreenPacket(FriendlyByteBuf friendlyByteBuf) {
		this.containerId = friendlyByteBuf.readVarInt();
		this.type = friendlyByteBuf.readVarInt();
		this.title = friendlyByteBuf.readComponent();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeVarInt(this.containerId);
		friendlyByteBuf.writeVarInt(this.type);
		friendlyByteBuf.writeComponent(this.title);
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleOpenScreen(this);
	}

	public int getContainerId() {
		return this.containerId;
	}

	@Nullable
	public MenuType<?> getType() {
		return Registry.MENU.byId(this.type);
	}

	public Component getTitle() {
		return this.title;
	}
}
