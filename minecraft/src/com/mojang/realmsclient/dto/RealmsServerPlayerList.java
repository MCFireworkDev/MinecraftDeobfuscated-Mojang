package com.mojang.realmsclient.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsServerPlayerList extends ValueObject {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final JsonParser jsonParser = new JsonParser();
	public long serverId;
	public List<String> players;

	public static RealmsServerPlayerList parse(JsonObject jsonObject) {
		RealmsServerPlayerList realmsServerPlayerList = new RealmsServerPlayerList();

		try {
			realmsServerPlayerList.serverId = JsonUtils.getLongOr("serverId", jsonObject, -1L);
			String string = JsonUtils.getStringOr("playerList", jsonObject, null);
			if (string != null) {
				JsonElement jsonElement = jsonParser.parse(string);
				if (jsonElement.isJsonArray()) {
					realmsServerPlayerList.players = parsePlayers(jsonElement.getAsJsonArray());
				} else {
					realmsServerPlayerList.players = new ArrayList();
				}
			} else {
				realmsServerPlayerList.players = new ArrayList();
			}
		} catch (Exception var4) {
			LOGGER.error("Could not parse RealmsServerPlayerList: " + var4.getMessage());
		}

		return realmsServerPlayerList;
	}

	private static List<String> parsePlayers(JsonArray jsonArray) {
		ArrayList<String> arrayList = new ArrayList();

		for(JsonElement jsonElement : jsonArray) {
			try {
				arrayList.add(jsonElement.getAsString());
			} catch (Exception var5) {
			}
		}

		return arrayList;
	}
}
