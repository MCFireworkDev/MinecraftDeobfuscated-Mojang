package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
	private static final Logger LOGGER = LogManager.getLogger();

	public static void main(String[] strings) {
		OptionParser optionParser = new OptionParser();
		OptionSpec<Void> optionSpec = optionParser.accepts("nogui");
		OptionSpec<Void> optionSpec2 = optionParser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
		OptionSpec<Void> optionSpec3 = optionParser.accepts("demo");
		OptionSpec<Void> optionSpec4 = optionParser.accepts("bonusChest");
		OptionSpec<Void> optionSpec5 = optionParser.accepts("forceUpgrade");
		OptionSpec<Void> optionSpec6 = optionParser.accepts("eraseCache");
		OptionSpec<Void> optionSpec7 = optionParser.accepts("help").forHelp();
		OptionSpec<String> optionSpec8 = optionParser.accepts("singleplayer").withRequiredArg();
		OptionSpec<String> optionSpec9 = optionParser.accepts("universe").withRequiredArg().defaultsTo(".");
		OptionSpec<String> optionSpec10 = optionParser.accepts("world").withRequiredArg();
		OptionSpec<Integer> optionSpec11 = optionParser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
		OptionSpec<String> optionSpec12 = optionParser.accepts("serverId").withRequiredArg();
		OptionSpec<String> optionSpec13 = optionParser.nonOptions();

		try {
			OptionSet optionSet = optionParser.parse(strings);
			if (optionSet.has(optionSpec7)) {
				optionParser.printHelpOn(System.err);
				return;
			}

			CrashReport.preload();
			Bootstrap.bootStrap();
			Bootstrap.validate();
			Path path = Paths.get("server.properties");
			DedicatedServerSettings dedicatedServerSettings = new DedicatedServerSettings(path);
			dedicatedServerSettings.forceSave();
			Path path2 = Paths.get("eula.txt");
			Eula eula = new Eula(path2);
			if (optionSet.has(optionSpec2)) {
				LOGGER.info("Initialized '{}' and '{}'", path.toAbsolutePath(), path2.toAbsolutePath());
				return;
			}

			if (!eula.hasAgreedToEULA()) {
				LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
				return;
			}

			File file = new File(optionSet.valueOf(optionSpec9));
			YggdrasilAuthenticationService yggdrasilAuthenticationService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString());
			MinecraftSessionService minecraftSessionService = yggdrasilAuthenticationService.createMinecraftSessionService();
			GameProfileRepository gameProfileRepository = yggdrasilAuthenticationService.createProfileRepository();
			GameProfileCache gameProfileCache = new GameProfileCache(gameProfileRepository, new File(file, MinecraftServer.USERID_CACHE_FILE.getName()));
			String string = (String)Optional.ofNullable(optionSet.valueOf(optionSpec10)).orElse(dedicatedServerSettings.getProperties().levelName);
			LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(file.toPath());
			LevelStorageSource.LevelStorageAccess levelStorageAccess = levelStorageSource.createAccess(string);
			MinecraftServer.convertFromRegionFormatIfNeeded(levelStorageAccess);
			if (optionSet.has(optionSpec5)) {
				forceUpgrade(levelStorageAccess, DataFixers.getDataFixer(), optionSet.has(optionSpec6), () -> true);
			}

			WorldData worldData = levelStorageAccess.getDataTag();
			if (worldData == null) {
				LevelSettings levelSettings;
				if (optionSet.has(optionSpec3)) {
					levelSettings = MinecraftServer.DEMO_SETTINGS;
				} else {
					DedicatedServerProperties dedicatedServerProperties = dedicatedServerSettings.getProperties();
					levelSettings = new LevelSettings(
						dedicatedServerProperties.levelName,
						dedicatedServerProperties.gamemode,
						dedicatedServerProperties.hardcore,
						dedicatedServerProperties.difficulty,
						false,
						new GameRules(),
						optionSet.has(optionSpec4) ? dedicatedServerProperties.worldGenSettings.withBonusChest() : dedicatedServerProperties.worldGenSettings
					);
				}

				worldData = new PrimaryLevelData(levelSettings);
			}

			final DedicatedServer dedicatedServer = new DedicatedServer(
				levelStorageAccess,
				worldData,
				dedicatedServerSettings,
				DataFixers.getDataFixer(),
				minecraftSessionService,
				gameProfileRepository,
				gameProfileCache,
				LoggerChunkProgressListener::new
			);
			dedicatedServer.setSingleplayerName(optionSet.valueOf(optionSpec8));
			dedicatedServer.setPort(optionSet.valueOf(optionSpec11));
			dedicatedServer.setDemo(optionSet.has(optionSpec3));
			dedicatedServer.setId(optionSet.valueOf(optionSpec12));
			boolean bl = !optionSet.has(optionSpec) && !optionSet.valuesOf(optionSpec13).contains("nogui");
			if (bl && !GraphicsEnvironment.isHeadless()) {
				dedicatedServer.showGui();
			}

			dedicatedServer.forkAndRun();
			Thread thread = new Thread("Server Shutdown Thread") {
				public void run() {
					dedicatedServer.halt(true);
				}
			};
			thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
			Runtime.getRuntime().addShutdownHook(thread);
		} catch (Exception var32) {
			LOGGER.fatal("Failed to start the minecraft server", var32);
		}
	}

	private static void forceUpgrade(LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, boolean bl, BooleanSupplier booleanSupplier) {
		LOGGER.info("Forcing world upgrade!");
		WorldData worldData = levelStorageAccess.getDataTag();
		if (worldData != null) {
			WorldUpgrader worldUpgrader = new WorldUpgrader(levelStorageAccess, dataFixer, worldData, bl);
			Component component = null;

			while(!worldUpgrader.isFinished()) {
				Component component2 = worldUpgrader.getStatus();
				if (component != component2) {
					component = component2;
					LOGGER.info(worldUpgrader.getStatus().getString());
				}

				int i = worldUpgrader.getTotalChunks();
				if (i > 0) {
					int j = worldUpgrader.getConverted() + worldUpgrader.getSkipped();
					LOGGER.info("{}% completed ({} / {} chunks)...", Mth.floor((float)j / (float)i * 100.0F), j, i);
				}

				if (!booleanSupplier.getAsBoolean()) {
					worldUpgrader.cancel();
				} else {
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException var10) {
					}
				}
			}
		}
	}
}
