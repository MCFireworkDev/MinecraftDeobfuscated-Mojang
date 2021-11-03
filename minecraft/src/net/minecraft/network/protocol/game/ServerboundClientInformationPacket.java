package net.minecraft.network.protocol.game;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

public final class ServerboundClientInformationPacket extends Record implements Packet<ServerGamePacketListener> {
	private final String language;
	private final int viewDistance;
	private final ChatVisiblity chatVisibility;
	private final boolean chatColors;
	private final int modelCustomisation;
	private final HumanoidArm mainHand;
	private final boolean textFilteringEnabled;
	private final boolean allowsListing;
	public static final int MAX_LANGUAGE_LENGTH = 16;

	public ServerboundClientInformationPacket(FriendlyByteBuf friendlyByteBuf) {
		this(
			friendlyByteBuf.readUtf(16),
			friendlyByteBuf.readByte(),
			friendlyByteBuf.readEnum(ChatVisiblity.class),
			friendlyByteBuf.readBoolean(),
			friendlyByteBuf.readUnsignedByte(),
			friendlyByteBuf.readEnum(HumanoidArm.class),
			friendlyByteBuf.readBoolean(),
			friendlyByteBuf.readBoolean()
		);
	}

	public ServerboundClientInformationPacket(
		String string, int i, ChatVisiblity chatVisiblity, boolean bl, int j, HumanoidArm humanoidArm, boolean bl2, boolean bl3
	) {
		this.language = string;
		this.viewDistance = i;
		this.chatVisibility = chatVisiblity;
		this.chatColors = bl;
		this.modelCustomisation = j;
		this.mainHand = humanoidArm;
		this.textFilteringEnabled = bl2;
		this.allowsListing = bl3;
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeUtf(this.language);
		friendlyByteBuf.writeByte(this.viewDistance);
		friendlyByteBuf.writeEnum(this.chatVisibility);
		friendlyByteBuf.writeBoolean(this.chatColors);
		friendlyByteBuf.writeByte(this.modelCustomisation);
		friendlyByteBuf.writeEnum(this.mainHand);
		friendlyByteBuf.writeBoolean(this.textFilteringEnabled);
		friendlyByteBuf.writeBoolean(this.allowsListing);
	}

	public void handle(ServerGamePacketListener serverGamePacketListener) {
		serverGamePacketListener.handleClientInformation(this);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ServerboundClientInformationPacket,"language;viewDistance;chatVisibility;chatColors;modelCustomisation;mainHand;textFilteringEnabled;allowsListing",ServerboundClientInformationPacket::language,ServerboundClientInformationPacket::viewDistance,ServerboundClientInformationPacket::chatVisibility,ServerboundClientInformationPacket::chatColors,ServerboundClientInformationPacket::modelCustomisation,ServerboundClientInformationPacket::mainHand,ServerboundClientInformationPacket::textFilteringEnabled,ServerboundClientInformationPacket::allowsListing>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ServerboundClientInformationPacket,"language;viewDistance;chatVisibility;chatColors;modelCustomisation;mainHand;textFilteringEnabled;allowsListing",ServerboundClientInformationPacket::language,ServerboundClientInformationPacket::viewDistance,ServerboundClientInformationPacket::chatVisibility,ServerboundClientInformationPacket::chatColors,ServerboundClientInformationPacket::modelCustomisation,ServerboundClientInformationPacket::mainHand,ServerboundClientInformationPacket::textFilteringEnabled,ServerboundClientInformationPacket::allowsListing>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ServerboundClientInformationPacket,"language;viewDistance;chatVisibility;chatColors;modelCustomisation;mainHand;textFilteringEnabled;allowsListing",ServerboundClientInformationPacket::language,ServerboundClientInformationPacket::viewDistance,ServerboundClientInformationPacket::chatVisibility,ServerboundClientInformationPacket::chatColors,ServerboundClientInformationPacket::modelCustomisation,ServerboundClientInformationPacket::mainHand,ServerboundClientInformationPacket::textFilteringEnabled,ServerboundClientInformationPacket::allowsListing>(
			this, object
		);
	}

	public String language() {
		return this.language;
	}

	public int viewDistance() {
		return this.viewDistance;
	}

	public ChatVisiblity chatVisibility() {
		return this.chatVisibility;
	}

	public boolean chatColors() {
		return this.chatColors;
	}

	public int modelCustomisation() {
		return this.modelCustomisation;
	}

	public HumanoidArm mainHand() {
		return this.mainHand;
	}

	public boolean textFilteringEnabled() {
		return this.textFilteringEnabled;
	}

	public boolean allowsListing() {
		return this.allowsListing;
	}
}
