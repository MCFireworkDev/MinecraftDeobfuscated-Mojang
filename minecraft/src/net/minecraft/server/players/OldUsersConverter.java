package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OldUsersConverter {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final File OLD_IPBANLIST = new File("banned-ips.txt");
	public static final File OLD_USERBANLIST = new File("banned-players.txt");
	public static final File OLD_OPLIST = new File("ops.txt");
	public static final File OLD_WHITELIST = new File("white-list.txt");

	static List<String> readOldListFormat(File file, Map<String, String[]> map) throws IOException {
		List<String> list = Files.readLines(file, StandardCharsets.UTF_8);

		for(String string : list) {
			string = string.trim();
			if (!string.startsWith("#") && string.length() >= 1) {
				String[] strings = string.split("\\|");
				map.put(strings[0].toLowerCase(Locale.ROOT), strings);
			}
		}

		return list;
	}

	private static void lookupPlayers(MinecraftServer minecraftServer, Collection<String> collection, ProfileLookupCallback profileLookupCallback) {
		String[] strings = (String[])collection.stream().filter(stringx -> !StringUtil.isNullOrEmpty(stringx)).toArray(i -> new String[i]);
		if (minecraftServer.usesAuthentication()) {
			minecraftServer.getProfileRepository().findProfilesByNames(strings, Agent.MINECRAFT, profileLookupCallback);
		} else {
			for(String string : strings) {
				UUID uUID = Player.createPlayerUUID(new GameProfile(null, string));
				GameProfile gameProfile = new GameProfile(uUID, string);
				profileLookupCallback.onProfileLookupSucceeded(gameProfile);
			}
		}
	}

	public static boolean convertUserBanlist(MinecraftServer minecraftServer) {
		final UserBanList userBanList = new UserBanList(PlayerList.USERBANLIST_FILE);
		if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
			if (userBanList.getFile().exists()) {
				try {
					userBanList.load();
				} catch (IOException var6) {
					LOGGER.warn("Could not load existing file {}", userBanList.getFile().getName(), var6);
				}
			}

			try {
				final Map<String, String[]> map = Maps.newHashMap();
				readOldListFormat(OLD_USERBANLIST, map);
				ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
					@Override
					public void onProfileLookupSucceeded(GameProfile gameProfile) {
						minecraftServer.getProfileCache().add(gameProfile);
						String[] strings = (String[])map.get(gameProfile.getName().toLowerCase(Locale.ROOT));
						if (strings == null) {
							OldUsersConverter.LOGGER.warn("Could not convert user banlist entry for {}", gameProfile.getName());
							throw new OldUsersConverter.ConversionError("Profile not in the conversionlist");
						} else {
							Date date = strings.length > 1 ? OldUsersConverter.parseDate(strings[1], null) : null;
							String string = strings.length > 2 ? strings[2] : null;
							Date date2 = strings.length > 3 ? OldUsersConverter.parseDate(strings[3], null) : null;
							String string2 = strings.length > 4 ? strings[4] : null;
							userBanList.add(new UserBanListEntry(gameProfile, date, string, date2, string2));
						}
					}

					@Override
					public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
						OldUsersConverter.LOGGER.warn("Could not lookup user banlist entry for {}", gameProfile.getName(), exception);
						if (!(exception instanceof ProfileNotFoundException)) {
							throw new OldUsersConverter.ConversionError("Could not request user " + gameProfile.getName() + " from backend systems", exception);
						}
					}
				};
				lookupPlayers(minecraftServer, map.keySet(), profileLookupCallback);
				userBanList.save();
				renameOldFile(OLD_USERBANLIST);
				return true;
			} catch (IOException var4) {
				LOGGER.warn("Could not read old user banlist to convert it!", var4);
				return false;
			} catch (OldUsersConverter.ConversionError var5) {
				LOGGER.error("Conversion failed, please try again later", var5);
				return false;
			}
		} else {
			return true;
		}
	}

	public static boolean convertIpBanlist(MinecraftServer minecraftServer) {
		IpBanList ipBanList = new IpBanList(PlayerList.IPBANLIST_FILE);
		if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
			if (ipBanList.getFile().exists()) {
				try {
					ipBanList.load();
				} catch (IOException var11) {
					LOGGER.warn("Could not load existing file {}", ipBanList.getFile().getName(), var11);
				}
			}

			try {
				Map<String, String[]> map = Maps.newHashMap();
				readOldListFormat(OLD_IPBANLIST, map);

				for(String string : map.keySet()) {
					String[] strings = (String[])map.get(string);
					Date date = strings.length > 1 ? parseDate(strings[1], null) : null;
					String string2 = strings.length > 2 ? strings[2] : null;
					Date date2 = strings.length > 3 ? parseDate(strings[3], null) : null;
					String string3 = strings.length > 4 ? strings[4] : null;
					ipBanList.add(new IpBanListEntry(string, date, string2, date2, string3));
				}

				ipBanList.save();
				renameOldFile(OLD_IPBANLIST);
				return true;
			} catch (IOException var10) {
				LOGGER.warn("Could not parse old ip banlist to convert it!", var10);
				return false;
			}
		} else {
			return true;
		}
	}

	public static boolean convertOpsList(MinecraftServer minecraftServer) {
		final ServerOpList serverOpList = new ServerOpList(PlayerList.OPLIST_FILE);
		if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
			if (serverOpList.getFile().exists()) {
				try {
					serverOpList.load();
				} catch (IOException var6) {
					LOGGER.warn("Could not load existing file {}", serverOpList.getFile().getName(), var6);
				}
			}

			try {
				List<String> list = Files.readLines(OLD_OPLIST, StandardCharsets.UTF_8);
				ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
					@Override
					public void onProfileLookupSucceeded(GameProfile gameProfile) {
						minecraftServer.getProfileCache().add(gameProfile);
						serverOpList.add(new ServerOpListEntry(gameProfile, minecraftServer.getOperatorUserPermissionLevel(), false));
					}

					@Override
					public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
						OldUsersConverter.LOGGER.warn("Could not lookup oplist entry for {}", gameProfile.getName(), exception);
						if (!(exception instanceof ProfileNotFoundException)) {
							throw new OldUsersConverter.ConversionError("Could not request user " + gameProfile.getName() + " from backend systems", exception);
						}
					}
				};
				lookupPlayers(minecraftServer, list, profileLookupCallback);
				serverOpList.save();
				renameOldFile(OLD_OPLIST);
				return true;
			} catch (IOException var4) {
				LOGGER.warn("Could not read old oplist to convert it!", var4);
				return false;
			} catch (OldUsersConverter.ConversionError var5) {
				LOGGER.error("Conversion failed, please try again later", var5);
				return false;
			}
		} else {
			return true;
		}
	}

	public static boolean convertWhiteList(MinecraftServer minecraftServer) {
		final UserWhiteList userWhiteList = new UserWhiteList(PlayerList.WHITELIST_FILE);
		if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
			if (userWhiteList.getFile().exists()) {
				try {
					userWhiteList.load();
				} catch (IOException var6) {
					LOGGER.warn("Could not load existing file {}", userWhiteList.getFile().getName(), var6);
				}
			}

			try {
				List<String> list = Files.readLines(OLD_WHITELIST, StandardCharsets.UTF_8);
				ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
					@Override
					public void onProfileLookupSucceeded(GameProfile gameProfile) {
						minecraftServer.getProfileCache().add(gameProfile);
						userWhiteList.add(new UserWhiteListEntry(gameProfile));
					}

					@Override
					public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
						OldUsersConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", gameProfile.getName(), exception);
						if (!(exception instanceof ProfileNotFoundException)) {
							throw new OldUsersConverter.ConversionError("Could not request user " + gameProfile.getName() + " from backend systems", exception);
						}
					}
				};
				lookupPlayers(minecraftServer, list, profileLookupCallback);
				userWhiteList.save();
				renameOldFile(OLD_WHITELIST);
				return true;
			} catch (IOException var4) {
				LOGGER.warn("Could not read old whitelist to convert it!", var4);
				return false;
			} catch (OldUsersConverter.ConversionError var5) {
				LOGGER.error("Conversion failed, please try again later", var5);
				return false;
			}
		} else {
			return true;
		}
	}

	@Nullable
	public static UUID convertMobOwnerIfNecessary(MinecraftServer minecraftServer, String string) {
		if (!StringUtil.isNullOrEmpty(string) && string.length() <= 16) {
			GameProfile gameProfile = minecraftServer.getProfileCache().get(string);
			if (gameProfile != null && gameProfile.getId() != null) {
				return gameProfile.getId();
			} else if (!minecraftServer.isSingleplayer() && minecraftServer.usesAuthentication()) {
				final List<GameProfile> list = Lists.<GameProfile>newArrayList();
				ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
					@Override
					public void onProfileLookupSucceeded(GameProfile gameProfile) {
						minecraftServer.getProfileCache().add(gameProfile);
						list.add(gameProfile);
					}

					@Override
					public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
						OldUsersConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", gameProfile.getName(), exception);
					}
				};
				lookupPlayers(minecraftServer, Lists.newArrayList(string), profileLookupCallback);
				return !list.isEmpty() && ((GameProfile)list.get(0)).getId() != null ? ((GameProfile)list.get(0)).getId() : null;
			} else {
				return Player.createPlayerUUID(new GameProfile(null, string));
			}
		} else {
			try {
				return UUID.fromString(string);
			} catch (IllegalArgumentException var5) {
				return null;
			}
		}
	}

	public static boolean convertPlayers(DedicatedServer dedicatedServer) {
		final File file = getWorldPlayersDirectory(dedicatedServer);
		final File file2 = new File(file.getParentFile(), "playerdata");
		final File file3 = new File(file.getParentFile(), "unknownplayers");
		if (file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			List<String> list = Lists.newArrayList();

			for(File file4 : files) {
				String string = file4.getName();
				if (string.toLowerCase(Locale.ROOT).endsWith(".dat")) {
					String string2 = string.substring(0, string.length() - ".dat".length());
					if (!string2.isEmpty()) {
						list.add(string2);
					}
				}
			}

			try {
				final String[] strings = (String[])list.toArray(new String[list.size()]);
				ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
					@Override
					public void onProfileLookupSucceeded(GameProfile gameProfile) {
						dedicatedServer.getProfileCache().add(gameProfile);
						UUID uUID = gameProfile.getId();
						if (uUID == null) {
							throw new OldUsersConverter.ConversionError("Missing UUID for user profile " + gameProfile.getName());
						} else {
							this.movePlayerFile(file2, this.getFileNameForProfile(gameProfile), uUID.toString());
						}
					}

					@Override
					public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
						OldUsersConverter.LOGGER.warn("Could not lookup user uuid for {}", gameProfile.getName(), exception);
						if (exception instanceof ProfileNotFoundException) {
							String string = this.getFileNameForProfile(gameProfile);
							this.movePlayerFile(file3, string, string);
						} else {
							throw new OldUsersConverter.ConversionError("Could not request user " + gameProfile.getName() + " from backend systems", exception);
						}
					}

					private void movePlayerFile(File file, String string, String string2) {
						File file2x = new File(file, string + ".dat");
						File file3x = new File(file, string2 + ".dat");
						OldUsersConverter.ensureDirectoryExists(file);
						if (!file2x.renameTo(file3x)) {
							throw new OldUsersConverter.ConversionError("Could not convert file for " + string);
						}
					}

					private String getFileNameForProfile(GameProfile gameProfile) {
						String string = null;

						for(String string2 : strings) {
							if (string2 != null && string2.equalsIgnoreCase(gameProfile.getName())) {
								string = string2;
								break;
							}
						}

						if (string == null) {
							throw new OldUsersConverter.ConversionError("Could not find the filename for " + gameProfile.getName() + " anymore");
						} else {
							return string;
						}
					}
				};
				lookupPlayers(dedicatedServer, Lists.newArrayList(strings), profileLookupCallback);
				return true;
			} catch (OldUsersConverter.ConversionError var12) {
				LOGGER.error("Conversion failed, please try again later", var12);
				return false;
			}
		} else {
			return true;
		}
	}

	private static void ensureDirectoryExists(File file) {
		if (file.exists()) {
			if (!file.isDirectory()) {
				throw new OldUsersConverter.ConversionError("Can't create directory " + file.getName() + " in world save directory.");
			}
		} else if (!file.mkdirs()) {
			throw new OldUsersConverter.ConversionError("Can't create directory " + file.getName() + " in world save directory.");
		}
	}

	public static boolean serverReadyAfterUserconversion(MinecraftServer minecraftServer) {
		boolean bl = areOldUserlistsRemoved();
		return bl && areOldPlayersConverted(minecraftServer);
	}

	private static boolean areOldUserlistsRemoved() {
		boolean bl = false;
		if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
			bl = true;
		}

		boolean bl2 = false;
		if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
			bl2 = true;
		}

		boolean bl3 = false;
		if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
			bl3 = true;
		}

		boolean bl4 = false;
		if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
			bl4 = true;
		}

		if (!bl && !bl2 && !bl3 && !bl4) {
			return true;
		} else {
			LOGGER.warn("**** FAILED TO START THE SERVER AFTER ACCOUNT CONVERSION!");
			LOGGER.warn("** please remove the following files and restart the server:");
			if (bl) {
				LOGGER.warn("* {}", OLD_USERBANLIST.getName());
			}

			if (bl2) {
				LOGGER.warn("* {}", OLD_IPBANLIST.getName());
			}

			if (bl3) {
				LOGGER.warn("* {}", OLD_OPLIST.getName());
			}

			if (bl4) {
				LOGGER.warn("* {}", OLD_WHITELIST.getName());
			}

			return false;
		}
	}

	private static boolean areOldPlayersConverted(MinecraftServer minecraftServer) {
		File file = getWorldPlayersDirectory(minecraftServer);
		if (!file.exists() || !file.isDirectory() || file.list().length <= 0 && file.delete()) {
			return true;
		} else {
			LOGGER.warn("**** DETECTED OLD PLAYER DIRECTORY IN THE WORLD SAVE");
			LOGGER.warn("**** THIS USUALLY HAPPENS WHEN THE AUTOMATIC CONVERSION FAILED IN SOME WAY");
			LOGGER.warn("** please restart the server and if the problem persists, remove the directory '{}'", file.getPath());
			return false;
		}
	}

	private static File getWorldPlayersDirectory(MinecraftServer minecraftServer) {
		String string = minecraftServer.getLevelIdName();
		File file = new File(string);
		return new File(file, "players");
	}

	private static void renameOldFile(File file) {
		File file2 = new File(file.getName() + ".converted");
		file.renameTo(file2);
	}

	private static Date parseDate(String string, Date date) {
		Date date2;
		try {
			date2 = BanListEntry.DATE_FORMAT.parse(string);
		} catch (ParseException var4) {
			date2 = date;
		}

		return date2;
	}

	static class ConversionError extends RuntimeException {
		private ConversionError(String string, Throwable throwable) {
			super(string, throwable);
		}

		private ConversionError(String string) {
			super(string);
		}
	}
}
