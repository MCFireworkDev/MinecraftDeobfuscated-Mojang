package net.minecraft.server.players;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

public class GameProfileCache {
	private static boolean usesAuthentication;
	private final Map<String, GameProfileCache.GameProfileInfo> profilesByName = Maps.newConcurrentMap();
	private final Map<UUID, GameProfileCache.GameProfileInfo> profilesByUUID = Maps.newConcurrentMap();
	private final GameProfileRepository profileRepository;
	private final Gson gson = new GsonBuilder().create();
	private final File file;
	private final AtomicLong operationCount = new AtomicLong();

	public GameProfileCache(GameProfileRepository gameProfileRepository, File file) {
		this.profileRepository = gameProfileRepository;
		this.file = file;
		Lists.reverse(this.load()).forEach(this::safeAdd);
	}

	private void safeAdd(GameProfileCache.GameProfileInfo gameProfileInfo) {
		GameProfile gameProfile = gameProfileInfo.getProfile();
		gameProfileInfo.setLastAccess(this.getNextOperation());
		String string = gameProfile.getName();
		if (string != null) {
			this.profilesByName.put(string.toLowerCase(Locale.ROOT), gameProfileInfo);
		}

		UUID uUID = gameProfile.getId();
		if (uUID != null) {
			this.profilesByUUID.put(uUID, gameProfileInfo);
		}
	}

	@Nullable
	private static GameProfile lookupGameProfile(GameProfileRepository gameProfileRepository, String string) {
		final AtomicReference<GameProfile> atomicReference = new AtomicReference();
		ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
			@Override
			public void onProfileLookupSucceeded(GameProfile gameProfile) {
				atomicReference.set(gameProfile);
			}

			@Override
			public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
				atomicReference.set(null);
			}
		};
		gameProfileRepository.findProfilesByNames(new String[]{string}, Agent.MINECRAFT, profileLookupCallback);
		GameProfile gameProfile = (GameProfile)atomicReference.get();
		if (!usesAuthentication() && gameProfile == null) {
			UUID uUID = Player.createPlayerUUID(new GameProfile(null, string));
			gameProfile = new GameProfile(uUID, string);
		}

		return gameProfile;
	}

	public static void setUsesAuthentication(boolean bl) {
		usesAuthentication = bl;
	}

	private static boolean usesAuthentication() {
		return usesAuthentication;
	}

	public void add(GameProfile gameProfile) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(2, 1);
		Date date = calendar.getTime();
		GameProfileCache.GameProfileInfo gameProfileInfo = new GameProfileCache.GameProfileInfo(gameProfile, date);
		this.safeAdd(gameProfileInfo);
		this.save();
	}

	private long getNextOperation() {
		return this.operationCount.incrementAndGet();
	}

	@Nullable
	public GameProfile get(String string) {
		String string2 = string.toLowerCase(Locale.ROOT);
		GameProfileCache.GameProfileInfo gameProfileInfo = (GameProfileCache.GameProfileInfo)this.profilesByName.get(string2);
		boolean bl = false;
		if (gameProfileInfo != null && new Date().getTime() >= gameProfileInfo.expirationDate.getTime()) {
			this.profilesByUUID.remove(gameProfileInfo.getProfile().getId());
			this.profilesByName.remove(gameProfileInfo.getProfile().getName().toLowerCase(Locale.ROOT));
			bl = true;
			gameProfileInfo = null;
		}

		GameProfile gameProfile;
		if (gameProfileInfo != null) {
			gameProfileInfo.setLastAccess(this.getNextOperation());
			gameProfile = gameProfileInfo.getProfile();
		} else {
			gameProfile = lookupGameProfile(this.profileRepository, string2);
			if (gameProfile != null) {
				this.add(gameProfile);
				bl = false;
			}
		}

		if (bl) {
			this.save();
		}

		return gameProfile;
	}

	@Nullable
	public GameProfile get(UUID uUID) {
		GameProfileCache.GameProfileInfo gameProfileInfo = (GameProfileCache.GameProfileInfo)this.profilesByUUID.get(uUID);
		if (gameProfileInfo == null) {
			return null;
		} else {
			gameProfileInfo.setLastAccess(this.getNextOperation());
			return gameProfileInfo.getProfile();
		}
	}

	private static DateFormat createDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	}

	public List<GameProfileCache.GameProfileInfo> load() {
		List<GameProfileCache.GameProfileInfo> list = Lists.<GameProfileCache.GameProfileInfo>newArrayList();

		try {
			Reader reader = Files.newReader(this.file, StandardCharsets.UTF_8);
			Throwable var3 = null;

			try {
				JsonArray jsonArray = this.gson.fromJson(reader, JsonArray.class);
				DateFormat dateFormat = createDateFormat();
				jsonArray.forEach(jsonElement -> {
					GameProfileCache.GameProfileInfo gameProfileInfo = readGameProfile(jsonElement, dateFormat);
					if (gameProfileInfo != null) {
						list.add(gameProfileInfo);
					}
				});
			} catch (Throwable var14) {
				var3 = var14;
				throw var14;
			} finally {
				if (reader != null) {
					if (var3 != null) {
						try {
							reader.close();
						} catch (Throwable var13) {
							var3.addSuppressed(var13);
						}
					} else {
						reader.close();
					}
				}
			}
		} catch (JsonParseException | IOException var16) {
		}

		return list;
	}

	public void save() {
		JsonArray jsonArray = new JsonArray();
		DateFormat dateFormat = createDateFormat();
		this.getTopMRUProfiles(1000).forEach(gameProfileInfo -> jsonArray.add(writeGameProfile(gameProfileInfo, dateFormat)));
		String string = this.gson.toJson((JsonElement)jsonArray);

		try {
			Writer writer = Files.newWriter(this.file, StandardCharsets.UTF_8);
			Throwable var5 = null;

			try {
				writer.write(string);
			} catch (Throwable var15) {
				var5 = var15;
				throw var15;
			} finally {
				if (writer != null) {
					if (var5 != null) {
						try {
							writer.close();
						} catch (Throwable var14) {
							var5.addSuppressed(var14);
						}
					} else {
						writer.close();
					}
				}
			}
		} catch (IOException var17) {
		}
	}

	private Stream<GameProfileCache.GameProfileInfo> getTopMRUProfiles(int i) {
		return ImmutableList.copyOf(this.profilesByUUID.values())
			.stream()
			.sorted(Comparator.comparing(GameProfileCache.GameProfileInfo::getLastAccess).reversed())
			.limit((long)i);
	}

	private static JsonElement writeGameProfile(GameProfileCache.GameProfileInfo gameProfileInfo, DateFormat dateFormat) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("name", gameProfileInfo.getProfile().getName());
		UUID uUID = gameProfileInfo.getProfile().getId();
		jsonObject.addProperty("uuid", uUID == null ? "" : uUID.toString());
		jsonObject.addProperty("expiresOn", dateFormat.format(gameProfileInfo.getExpirationDate()));
		return jsonObject;
	}

	@Nullable
	private static GameProfileCache.GameProfileInfo readGameProfile(JsonElement jsonElement, DateFormat dateFormat) {
		if (jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			JsonElement jsonElement2 = jsonObject.get("name");
			JsonElement jsonElement3 = jsonObject.get("uuid");
			JsonElement jsonElement4 = jsonObject.get("expiresOn");
			if (jsonElement2 != null && jsonElement3 != null) {
				String string = jsonElement3.getAsString();
				String string2 = jsonElement2.getAsString();
				Date date = null;
				if (jsonElement4 != null) {
					try {
						date = dateFormat.parse(jsonElement4.getAsString());
					} catch (ParseException var12) {
					}
				}

				if (string2 != null && string != null && date != null) {
					UUID uUID;
					try {
						uUID = UUID.fromString(string);
					} catch (Throwable var11) {
						return null;
					}

					return new GameProfileCache.GameProfileInfo(new GameProfile(uUID, string2), date);
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	static class GameProfileInfo {
		private final GameProfile profile;
		private final Date expirationDate;
		private volatile long lastAccess;

		private GameProfileInfo(GameProfile gameProfile, Date date) {
			this.profile = gameProfile;
			this.expirationDate = date;
		}

		public GameProfile getProfile() {
			return this.profile;
		}

		public Date getExpirationDate() {
			return this.expirationDate;
		}

		public void setLastAccess(long l) {
			this.lastAccess = l;
		}

		public long getLastAccess() {
			return this.lastAccess;
		}
	}
}
