package net.minecraft.client.multiplayer;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.InsecurePublicKeyException.MissingException;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ProfileKeyPairManager {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path PROFILE_KEY_PAIR_DIR = Path.of("profilekeys");
	private final UserApiService userApiService;
	private final Path profileKeyPairPath;
	private CompletableFuture<Optional<ProfileKeyPairManager.Result>> keyPair;

	public ProfileKeyPairManager(UserApiService userApiService, UUID uUID, Path path) {
		this.userApiService = userApiService;
		this.profileKeyPairPath = path.resolve(PROFILE_KEY_PAIR_DIR).resolve(uUID + ".json");
		this.keyPair = CompletableFuture.supplyAsync(
				() -> this.readProfileKeyPair().filter(profileKeyPair -> !profileKeyPair.publicKey().data().hasExpired()), Util.backgroundExecutor()
			)
			.thenCompose(this::readOrFetchProfileKeyPair);
	}

	public CompletableFuture<Optional<ProfilePublicKey.Data>> preparePublicKey() {
		this.keyPair = this.keyPair.thenCompose(optional -> {
			Optional<ProfileKeyPair> optional2 = optional.map(ProfileKeyPairManager.Result::keyPair);
			return this.readOrFetchProfileKeyPair(optional2);
		});
		return this.keyPair.thenApply(optional -> optional.map(result -> result.keyPair().publicKey().data()));
	}

	private CompletableFuture<Optional<ProfileKeyPairManager.Result>> readOrFetchProfileKeyPair(Optional<ProfileKeyPair> optional) {
		return CompletableFuture.supplyAsync(() -> {
			if (optional.isPresent() && !((ProfileKeyPair)optional.get()).dueRefresh()) {
				if (SharedConstants.IS_RUNNING_IN_IDE) {
					return optional;
				}

				this.writeProfileKeyPair(null);
			}

			try {
				ProfileKeyPair profileKeyPair = this.fetchProfileKeyPair(this.userApiService);
				this.writeProfileKeyPair(profileKeyPair);
				return Optional.of(profileKeyPair);
			} catch (CryptException | MinecraftClientException | IOException var3) {
				LOGGER.error("Failed to retrieve profile key pair", var3);
				this.writeProfileKeyPair(null);
				return optional;
			}
		}, Util.backgroundExecutor()).thenApply(optionalx -> optionalx.map(ProfileKeyPairManager.Result::new));
	}

	private Optional<ProfileKeyPair> readProfileKeyPair() {
		if (Files.notExists(this.profileKeyPairPath, new LinkOption[0])) {
			return Optional.empty();
		} else {
			try {
				BufferedReader bufferedReader = Files.newBufferedReader(this.profileKeyPairPath);

				Optional var2;
				try {
					var2 = ProfileKeyPair.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader)).result();
				} catch (Throwable var5) {
					if (bufferedReader != null) {
						try {
							bufferedReader.close();
						} catch (Throwable var4) {
							var5.addSuppressed(var4);
						}
					}

					throw var5;
				}

				if (bufferedReader != null) {
					bufferedReader.close();
				}

				return var2;
			} catch (Exception var6) {
				LOGGER.error("Failed to read profile key pair file {}", this.profileKeyPairPath, var6);
				return Optional.empty();
			}
		}
	}

	private void writeProfileKeyPair(@Nullable ProfileKeyPair profileKeyPair) {
		try {
			Files.deleteIfExists(this.profileKeyPairPath);
		} catch (IOException var3) {
			LOGGER.error("Failed to delete profile key pair file {}", this.profileKeyPairPath, var3);
		}

		if (profileKeyPair != null) {
			if (SharedConstants.IS_RUNNING_IN_IDE) {
				ProfileKeyPair.CODEC.encodeStart(JsonOps.INSTANCE, profileKeyPair).result().ifPresent(jsonElement -> {
					try {
						Files.createDirectories(this.profileKeyPairPath.getParent());
						Files.writeString(this.profileKeyPairPath, jsonElement.toString());
					} catch (Exception var3xx) {
						LOGGER.error("Failed to write profile key pair file {}", this.profileKeyPairPath, var3xx);
					}
				});
			}
		}
	}

	private ProfileKeyPair fetchProfileKeyPair(UserApiService userApiService) throws CryptException, IOException {
		KeyPairResponse keyPairResponse = userApiService.getKeyPair();
		if (keyPairResponse != null) {
			ProfilePublicKey.Data data = parsePublicKey(keyPairResponse);
			return new ProfileKeyPair(
				Crypt.stringToPemRsaPrivateKey(keyPairResponse.getPrivateKey()), new ProfilePublicKey(data), Instant.parse(keyPairResponse.getRefreshedAfter())
			);
		} else {
			throw new IOException("Could not retrieve profile key pair");
		}
	}

	private static ProfilePublicKey.Data parsePublicKey(KeyPairResponse keyPairResponse) throws CryptException {
		if (!Strings.isNullOrEmpty(keyPairResponse.getPublicKey())
			&& keyPairResponse.getPublicKeySignature() != null
			&& keyPairResponse.getPublicKeySignature().array().length != 0) {
			try {
				Instant instant = Instant.parse(keyPairResponse.getExpiresAt());
				PublicKey publicKey = Crypt.stringToRsaPublicKey(keyPairResponse.getPublicKey());
				ByteBuffer byteBuffer = keyPairResponse.getPublicKeySignature();
				return new ProfilePublicKey.Data(instant, publicKey, byteBuffer.array());
			} catch (IllegalArgumentException | DateTimeException var4) {
				throw new CryptException(var4);
			}
		} else {
			throw new CryptException(new MissingException());
		}
	}

	@Nullable
	public Signer signer() {
		return (Signer)((Optional)this.keyPair.join()).map(ProfileKeyPairManager.Result::signer).orElse(null);
	}

	public Optional<ProfilePublicKey> profilePublicKey() {
		return ((Optional)this.keyPair.join()).map(result -> result.keyPair().publicKey());
	}

	@Environment(EnvType.CLIENT)
	static record Result(ProfileKeyPair keyPair, Signer signer) {
		public Result(ProfileKeyPair profileKeyPair) {
			this(profileKeyPair, Signer.from(profileKeyPair.privateKey(), "SHA256withRSA"));
		}
	}
}
