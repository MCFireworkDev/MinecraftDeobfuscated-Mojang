package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Date;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class PendingInvite extends ValueObject {
	private static final Logger LOGGER = LogUtils.getLogger();
	public String invitationId;
	public String worldName;
	public String worldOwnerName;
	public UUID worldOwnerUuid;
	public Date date;

	public static PendingInvite parse(JsonObject jsonObject) {
		PendingInvite pendingInvite = new PendingInvite();

		try {
			pendingInvite.invitationId = JsonUtils.getStringOr("invitationId", jsonObject, "");
			pendingInvite.worldName = JsonUtils.getStringOr("worldName", jsonObject, "");
			pendingInvite.worldOwnerName = JsonUtils.getStringOr("worldOwnerName", jsonObject, "");
			pendingInvite.worldOwnerUuid = JsonUtils.getUuidOr("worldOwnerUuid", jsonObject, Util.NIL_UUID);
			pendingInvite.date = JsonUtils.getDateOr("date", jsonObject);
		} catch (Exception var3) {
			LOGGER.error("Could not parse PendingInvite: {}", var3.getMessage());
		}

		return pendingInvite;
	}
}
