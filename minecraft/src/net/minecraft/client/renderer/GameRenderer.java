package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class GameRenderer implements AutoCloseable {
	private static final ResourceLocation NAUSEA_LOCATION = new ResourceLocation("textures/misc/nausea.png");
	static final Logger LOGGER = LogUtils.getLogger();
	private static final boolean DEPTH_BUFFER_DEBUG = false;
	public static final float PROJECTION_Z_NEAR = 0.05F;
	private static final float GUI_Z_NEAR = 1000.0F;
	final Minecraft minecraft;
	private final ResourceManager resourceManager;
	private final RandomSource random = RandomSource.create();
	private float renderDistance;
	public final ItemInHandRenderer itemInHandRenderer;
	private final MapRenderer mapRenderer;
	private final RenderBuffers renderBuffers;
	private int confusionAnimationTick;
	private float fov;
	private float oldFov;
	private float darkenWorldAmount;
	private float darkenWorldAmountO;
	private boolean renderHand = true;
	private boolean renderBlockOutline = true;
	private long lastScreenshotAttempt;
	private boolean hasWorldScreenshot;
	private long lastActiveTime = Util.getMillis();
	private final LightTexture lightTexture;
	private final OverlayTexture overlayTexture = new OverlayTexture();
	private boolean panoramicMode;
	private float zoom = 1.0F;
	private float zoomX;
	private float zoomY;
	public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
	@Nullable
	private ItemStack itemActivationItem;
	private int itemActivationTicks;
	private float itemActivationOffX;
	private float itemActivationOffY;
	@Nullable
	PostChain postEffect;
	static final ResourceLocation[] EFFECTS = new ResourceLocation[]{
		new ResourceLocation("shaders/post/notch.json"),
		new ResourceLocation("shaders/post/fxaa.json"),
		new ResourceLocation("shaders/post/art.json"),
		new ResourceLocation("shaders/post/bumpy.json"),
		new ResourceLocation("shaders/post/blobs2.json"),
		new ResourceLocation("shaders/post/pencil.json"),
		new ResourceLocation("shaders/post/color_convolve.json"),
		new ResourceLocation("shaders/post/deconverge.json"),
		new ResourceLocation("shaders/post/flip.json"),
		new ResourceLocation("shaders/post/invert.json"),
		new ResourceLocation("shaders/post/ntsc.json"),
		new ResourceLocation("shaders/post/outline.json"),
		new ResourceLocation("shaders/post/phosphor.json"),
		new ResourceLocation("shaders/post/scan_pincushion.json"),
		new ResourceLocation("shaders/post/sobel.json"),
		new ResourceLocation("shaders/post/bits.json"),
		new ResourceLocation("shaders/post/desaturate.json"),
		new ResourceLocation("shaders/post/green.json"),
		new ResourceLocation("shaders/post/blur.json"),
		new ResourceLocation("shaders/post/wobble.json"),
		new ResourceLocation("shaders/post/blobs.json"),
		new ResourceLocation("shaders/post/antialias.json"),
		new ResourceLocation("shaders/post/creeper.json"),
		new ResourceLocation("shaders/post/spider.json")
	};
	public static final int EFFECT_NONE = EFFECTS.length;
	int effectIndex = EFFECT_NONE;
	private boolean effectActive;
	private final Camera mainCamera = new Camera();
	public ShaderInstance blitShader;
	private final Map<String, ShaderInstance> shaders = Maps.newHashMap();
	@Nullable
	private static ShaderInstance positionShader;
	@Nullable
	private static ShaderInstance positionColorShader;
	@Nullable
	private static ShaderInstance positionColorTexShader;
	@Nullable
	private static ShaderInstance positionTexShader;
	@Nullable
	private static ShaderInstance positionTexColorShader;
	@Nullable
	private static ShaderInstance particleShader;
	@Nullable
	private static ShaderInstance positionColorLightmapShader;
	@Nullable
	private static ShaderInstance positionColorTexLightmapShader;
	@Nullable
	private static ShaderInstance positionTexColorNormalShader;
	@Nullable
	private static ShaderInstance positionTexLightmapColorShader;
	@Nullable
	private static ShaderInstance rendertypeSolidShader;
	@Nullable
	private static ShaderInstance rendertypeCutoutMippedShader;
	@Nullable
	private static ShaderInstance rendertypeCutoutShader;
	@Nullable
	private static ShaderInstance rendertypeTranslucentShader;
	@Nullable
	private static ShaderInstance rendertypeTranslucentMovingBlockShader;
	@Nullable
	private static ShaderInstance rendertypeArmorCutoutNoCullShader;
	@Nullable
	private static ShaderInstance rendertypeEntitySolidShader;
	@Nullable
	private static ShaderInstance rendertypeEntityCutoutShader;
	@Nullable
	private static ShaderInstance rendertypeEntityCutoutNoCullShader;
	@Nullable
	private static ShaderInstance rendertypeEntityCutoutNoCullZOffsetShader;
	@Nullable
	private static ShaderInstance rendertypeItemEntityTranslucentCullShader;
	@Nullable
	private static ShaderInstance rendertypeEntityTranslucentCullShader;
	@Nullable
	private static ShaderInstance rendertypeEntityTranslucentShader;
	@Nullable
	private static ShaderInstance rendertypeEntityTranslucentEmissiveShader;
	@Nullable
	private static ShaderInstance rendertypeEntitySmoothCutoutShader;
	@Nullable
	private static ShaderInstance rendertypeBeaconBeamShader;
	@Nullable
	private static ShaderInstance rendertypeEntityDecalShader;
	@Nullable
	private static ShaderInstance rendertypeEntityNoOutlineShader;
	@Nullable
	private static ShaderInstance rendertypeEntityShadowShader;
	@Nullable
	private static ShaderInstance rendertypeEntityAlphaShader;
	@Nullable
	private static ShaderInstance rendertypeEyesShader;
	@Nullable
	private static ShaderInstance rendertypeEnergySwirlShader;
	@Nullable
	private static ShaderInstance rendertypeBreezeWindShader;
	@Nullable
	private static ShaderInstance rendertypeLeashShader;
	@Nullable
	private static ShaderInstance rendertypeWaterMaskShader;
	@Nullable
	private static ShaderInstance rendertypeOutlineShader;
	@Nullable
	private static ShaderInstance rendertypeArmorGlintShader;
	@Nullable
	private static ShaderInstance rendertypeArmorEntityGlintShader;
	@Nullable
	private static ShaderInstance rendertypeGlintTranslucentShader;
	@Nullable
	private static ShaderInstance rendertypeGlintShader;
	@Nullable
	private static ShaderInstance rendertypeGlintDirectShader;
	@Nullable
	private static ShaderInstance rendertypeEntityGlintShader;
	@Nullable
	private static ShaderInstance rendertypeEntityGlintDirectShader;
	@Nullable
	private static ShaderInstance rendertypeTextShader;
	@Nullable
	private static ShaderInstance rendertypeTextBackgroundShader;
	@Nullable
	private static ShaderInstance rendertypeTextIntensityShader;
	@Nullable
	private static ShaderInstance rendertypeTextSeeThroughShader;
	@Nullable
	private static ShaderInstance rendertypeTextBackgroundSeeThroughShader;
	@Nullable
	private static ShaderInstance rendertypeTextIntensitySeeThroughShader;
	@Nullable
	private static ShaderInstance rendertypeLightningShader;
	@Nullable
	private static ShaderInstance rendertypeTripwireShader;
	@Nullable
	private static ShaderInstance rendertypeEndPortalShader;
	@Nullable
	private static ShaderInstance rendertypeEndGatewayShader;
	@Nullable
	private static ShaderInstance rendertypeLinesShader;
	@Nullable
	private static ShaderInstance rendertypeCrumblingShader;
	@Nullable
	private static ShaderInstance rendertypeGuiShader;
	@Nullable
	private static ShaderInstance rendertypeGuiOverlayShader;
	@Nullable
	private static ShaderInstance rendertypeGuiTextHighlightShader;
	@Nullable
	private static ShaderInstance rendertypeGuiGhostRecipeOverlayShader;

	public GameRenderer(Minecraft minecraft, ItemInHandRenderer itemInHandRenderer, ResourceManager resourceManager, RenderBuffers renderBuffers) {
		this.minecraft = minecraft;
		this.resourceManager = resourceManager;
		this.itemInHandRenderer = itemInHandRenderer;
		this.mapRenderer = new MapRenderer(minecraft.getTextureManager());
		this.lightTexture = new LightTexture(this, minecraft);
		this.renderBuffers = renderBuffers;
		this.postEffect = null;
	}

	public void close() {
		this.lightTexture.close();
		this.mapRenderer.close();
		this.overlayTexture.close();
		this.shutdownEffect();
		this.shutdownShaders();
		if (this.blitShader != null) {
			this.blitShader.close();
		}
	}

	public void setRenderHand(boolean bl) {
		this.renderHand = bl;
	}

	public void setRenderBlockOutline(boolean bl) {
		this.renderBlockOutline = bl;
	}

	public void setPanoramicMode(boolean bl) {
		this.panoramicMode = bl;
	}

	public boolean isPanoramicMode() {
		return this.panoramicMode;
	}

	public void shutdownEffect() {
		if (this.postEffect != null) {
			this.postEffect.close();
		}

		this.postEffect = null;
		this.effectIndex = EFFECT_NONE;
	}

	public void togglePostEffect() {
		this.effectActive = !this.effectActive;
	}

	public void checkEntityPostEffect(@Nullable Entity entity) {
		if (this.postEffect != null) {
			this.postEffect.close();
		}

		this.postEffect = null;
		if (entity instanceof Creeper) {
			this.loadEffect(new ResourceLocation("shaders/post/creeper.json"));
		} else if (entity instanceof Spider) {
			this.loadEffect(new ResourceLocation("shaders/post/spider.json"));
		} else if (entity instanceof EnderMan) {
			this.loadEffect(new ResourceLocation("shaders/post/invert.json"));
		}
	}

	public void cycleEffect() {
		if (this.minecraft.getCameraEntity() instanceof Player) {
			if (this.postEffect != null) {
				this.postEffect.close();
			}

			this.effectIndex = (this.effectIndex + 1) % (EFFECTS.length + 1);
			if (this.effectIndex == EFFECT_NONE) {
				this.postEffect = null;
			} else {
				this.loadEffect(EFFECTS[this.effectIndex]);
			}
		}
	}

	void loadEffect(ResourceLocation resourceLocation) {
		if (this.postEffect != null) {
			this.postEffect.close();
		}

		try {
			this.postEffect = new PostChain(this.minecraft.getTextureManager(), this.resourceManager, this.minecraft.getMainRenderTarget(), resourceLocation);
			this.postEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
			this.effectActive = true;
		} catch (IOException var3) {
			LOGGER.warn("Failed to load shader: {}", resourceLocation, var3);
			this.effectIndex = EFFECT_NONE;
			this.effectActive = false;
		} catch (JsonSyntaxException var4) {
			LOGGER.warn("Failed to parse shader: {}", resourceLocation, var4);
			this.effectIndex = EFFECT_NONE;
			this.effectActive = false;
		}
	}

	public PreparableReloadListener createReloadListener() {
		return new SimplePreparableReloadListener<GameRenderer.ResourceCache>() {
			protected GameRenderer.ResourceCache prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
				Map<ResourceLocation, Resource> map = resourceManager.listResources(
					"shaders",
					resourceLocation -> {
						String string = resourceLocation.getPath();
						return string.endsWith(".json")
							|| string.endsWith(Program.Type.FRAGMENT.getExtension())
							|| string.endsWith(Program.Type.VERTEX.getExtension())
							|| string.endsWith(".glsl");
					}
				);
				Map<ResourceLocation, Resource> map2 = new HashMap();
				map.forEach((resourceLocation, resource) -> {
					try {
						InputStream inputStream = resource.open();

						try {
							byte[] bs = inputStream.readAllBytes();
							map2.put(resourceLocation, new Resource(resource.source(), () -> new ByteArrayInputStream(bs)));
						} catch (Throwable var7) {
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch (Throwable var6) {
									var7.addSuppressed(var6);
								}
							}

							throw var7;
						}

						if (inputStream != null) {
							inputStream.close();
						}
					} catch (Exception var8) {
						GameRenderer.LOGGER.warn("Failed to read resource {}", resourceLocation, var8);
					}
				});
				return new GameRenderer.ResourceCache(resourceManager, map2);
			}

			protected void apply(GameRenderer.ResourceCache resourceCache, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
				GameRenderer.this.reloadShaders(resourceCache);
				if (GameRenderer.this.postEffect != null) {
					GameRenderer.this.postEffect.close();
				}

				GameRenderer.this.postEffect = null;
				if (GameRenderer.this.effectIndex == GameRenderer.EFFECT_NONE) {
					GameRenderer.this.checkEntityPostEffect(GameRenderer.this.minecraft.getCameraEntity());
				} else {
					GameRenderer.this.loadEffect(GameRenderer.EFFECTS[GameRenderer.this.effectIndex]);
				}
			}

			@Override
			public String getName() {
				return "Shader Loader";
			}
		};
	}

	public void preloadUiShader(ResourceProvider resourceProvider) {
		if (this.blitShader != null) {
			throw new RuntimeException("Blit shader already preloaded");
		} else {
			try {
				this.blitShader = new ShaderInstance(resourceProvider, "blit_screen", DefaultVertexFormat.BLIT_SCREEN);
			} catch (IOException var3) {
				throw new RuntimeException("could not preload blit shader", var3);
			}

			rendertypeGuiShader = this.preloadShader(resourceProvider, "rendertype_gui", DefaultVertexFormat.POSITION_COLOR);
			rendertypeGuiOverlayShader = this.preloadShader(resourceProvider, "rendertype_gui_overlay", DefaultVertexFormat.POSITION_COLOR);
			positionShader = this.preloadShader(resourceProvider, "position", DefaultVertexFormat.POSITION);
			positionColorShader = this.preloadShader(resourceProvider, "position_color", DefaultVertexFormat.POSITION_COLOR);
			positionColorTexShader = this.preloadShader(resourceProvider, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX);
			positionTexShader = this.preloadShader(resourceProvider, "position_tex", DefaultVertexFormat.POSITION_TEX);
			positionTexColorShader = this.preloadShader(resourceProvider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR);
			rendertypeTextShader = this.preloadShader(resourceProvider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
		}
	}

	private ShaderInstance preloadShader(ResourceProvider resourceProvider, String string, VertexFormat vertexFormat) {
		try {
			ShaderInstance shaderInstance = new ShaderInstance(resourceProvider, string, vertexFormat);
			this.shaders.put(string, shaderInstance);
			return shaderInstance;
		} catch (Exception var5) {
			throw new IllegalStateException("could not preload shader " + string, var5);
		}
	}

	void reloadShaders(ResourceProvider resourceProvider) {
		RenderSystem.assertOnRenderThread();
		List<Program> list = Lists.<Program>newArrayList();
		list.addAll(Program.Type.FRAGMENT.getPrograms().values());
		list.addAll(Program.Type.VERTEX.getPrograms().values());
		list.forEach(Program::close);
		List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list2 = Lists.<Pair<ShaderInstance, Consumer<ShaderInstance>>>newArrayListWithCapacity(
			this.shaders.size()
		);

		try {
			list2.add(
				Pair.of(new ShaderInstance(resourceProvider, "particle", DefaultVertexFormat.PARTICLE), (Consumer)shaderInstance -> particleShader = shaderInstance)
			);
			list2.add(
				Pair.of(new ShaderInstance(resourceProvider, "position", DefaultVertexFormat.POSITION), (Consumer)shaderInstance -> positionShader = shaderInstance)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_color", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> positionColorShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_color_lightmap", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
					(Consumer)shaderInstance -> positionColorLightmapShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX),
					(Consumer)shaderInstance -> positionColorTexShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> positionColorTexLightmapShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_tex", DefaultVertexFormat.POSITION_TEX), (Consumer)shaderInstance -> positionTexShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR),
					(Consumer)shaderInstance -> positionTexColorShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_tex_color_normal", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),
					(Consumer)shaderInstance -> positionTexColorNormalShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "position_tex_lightmap_color", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR),
					(Consumer)shaderInstance -> positionTexLightmapColorShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_solid", DefaultVertexFormat.BLOCK), (Consumer)shaderInstance -> rendertypeSolidShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_cutout_mipped", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeCutoutMippedShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_cutout", DefaultVertexFormat.BLOCK), (Consumer)shaderInstance -> rendertypeCutoutShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_translucent", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeTranslucentShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeTranslucentMovingBlockShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeArmorCutoutNoCullShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntitySolidShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityCutoutShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityCutoutNoCullShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityCutoutNoCullZOffsetShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeItemEntityTranslucentCullShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityTranslucentCullShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityTranslucentShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityTranslucentEmissiveShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntitySmoothCutoutShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeBeaconBeamShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityDecalShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityNoOutlineShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityShadowShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityAlphaShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEyesShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEnergySwirlShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeLeashShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_water_mask", DefaultVertexFormat.POSITION),
					(Consumer)shaderInstance -> rendertypeWaterMaskShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_outline", DefaultVertexFormat.POSITION_COLOR_TEX),
					(Consumer)shaderInstance -> rendertypeOutlineShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_armor_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeArmorGlintShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_armor_entity_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeArmorEntityGlintShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeGlintTranslucentShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeGlintShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_glint_direct", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeGlintDirectShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeEntityGlintShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_entity_glint_direct", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeEntityGlintDirectShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_text_background", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextBackgroundShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextIntensityShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextSeeThroughShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_text_background_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextBackgroundSeeThroughShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextIntensitySeeThroughShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> rendertypeLightningShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_tripwire", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeTripwireShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_end_portal", DefaultVertexFormat.POSITION),
					(Consumer)shaderInstance -> rendertypeEndPortalShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_end_gateway", DefaultVertexFormat.POSITION),
					(Consumer)shaderInstance -> rendertypeEndGatewayShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL),
					(Consumer)shaderInstance -> rendertypeLinesShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_crumbling", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeCrumblingShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_gui", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> rendertypeGuiShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_gui_overlay", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> rendertypeGuiOverlayShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_gui_text_highlight", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> rendertypeGuiTextHighlightShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_gui_ghost_recipe_overlay", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> rendertypeGuiGhostRecipeOverlayShader = shaderInstance
				)
			);
			list2.add(
				Pair.of(
					new ShaderInstance(resourceProvider, "rendertype_breeze_wind", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeBreezeWindShader = shaderInstance
				)
			);
		} catch (IOException var5) {
			list2.forEach(pair -> ((ShaderInstance)pair.getFirst()).close());
			throw new RuntimeException("could not reload shaders", var5);
		}

		this.shutdownShaders();
		list2.forEach(pair -> {
			ShaderInstance shaderInstance = (ShaderInstance)pair.getFirst();
			this.shaders.put(shaderInstance.getName(), shaderInstance);
			((Consumer)pair.getSecond()).accept(shaderInstance);
		});
	}

	private void shutdownShaders() {
		RenderSystem.assertOnRenderThread();
		this.shaders.values().forEach(ShaderInstance::close);
		this.shaders.clear();
	}

	@Nullable
	public ShaderInstance getShader(@Nullable String string) {
		return string == null ? null : (ShaderInstance)this.shaders.get(string);
	}

	public void tick() {
		this.tickFov();
		this.lightTexture.tick();
		if (this.minecraft.getCameraEntity() == null) {
			this.minecraft.setCameraEntity(this.minecraft.player);
		}

		this.mainCamera.tick();
		this.itemInHandRenderer.tick();
		++this.confusionAnimationTick;
		if (this.minecraft.level.tickRateManager().runsNormally()) {
			this.minecraft.levelRenderer.tickRain(this.mainCamera);
			this.darkenWorldAmountO = this.darkenWorldAmount;
			if (this.minecraft.gui.getBossOverlay().shouldDarkenScreen()) {
				this.darkenWorldAmount += 0.05F;
				if (this.darkenWorldAmount > 1.0F) {
					this.darkenWorldAmount = 1.0F;
				}
			} else if (this.darkenWorldAmount > 0.0F) {
				this.darkenWorldAmount -= 0.0125F;
			}

			if (this.itemActivationTicks > 0) {
				--this.itemActivationTicks;
				if (this.itemActivationTicks == 0) {
					this.itemActivationItem = null;
				}
			}
		}
	}

	@Nullable
	public PostChain currentEffect() {
		return this.postEffect;
	}

	public void resize(int i, int j) {
		if (this.postEffect != null) {
			this.postEffect.resize(i, j);
		}

		this.minecraft.levelRenderer.resize(i, j);
	}

	public void pick(float f) {
		Entity entity = this.minecraft.getCameraEntity();
		if (entity != null) {
			if (this.minecraft.level != null && this.minecraft.player != null) {
				this.minecraft.getProfiler().push("pick");
				this.minecraft.crosshairPickEntity = null;
				double d = this.minecraft.player.blockInteractionRange();
				double e = this.minecraft.player.entityInteractionRange();
				double g = Math.max(d, e);
				this.minecraft.hitResult = entity.pick(d, f, false);
				Vec3 vec3 = entity.getEyePosition(f);
				double h = g;
				double i = Mth.square(g);
				if (this.minecraft.hitResult != null) {
					i = this.minecraft.hitResult.getLocation().distanceToSqr(vec3);
					h = Math.sqrt(i);
				}

				Vec3 vec32 = entity.getViewVector(1.0F);
				Vec3 vec33 = vec3.add(vec32.x * g, vec32.y * g, vec32.z * g);
				float j = 1.0F;
				AABB aABB = entity.getBoundingBox().expandTowards(vec32.scale(h)).inflate(1.0, 1.0, 1.0);
				EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
					entity, vec3, vec33, aABB, entityx -> !entityx.isSpectator() && entityx.isPickable(), i
				);
				if (entityHitResult != null) {
					Vec3 vec34 = entityHitResult.getLocation();
					if (vec3.closerThan(vec34, e)) {
						this.minecraft.hitResult = entityHitResult;
						this.minecraft.crosshairPickEntity = entityHitResult.getEntity();
					} else {
						this.minecraft.hitResult = BlockHitResult.miss(vec34, Direction.getNearest(vec32.x, vec32.y, vec32.z), BlockPos.containing(vec34));
					}
				}

				this.minecraft.getProfiler().pop();
			}
		}
	}

	private void tickFov() {
		float f = 1.0F;
		Entity var3 = this.minecraft.getCameraEntity();
		if (var3 instanceof AbstractClientPlayer abstractClientPlayer) {
			f = abstractClientPlayer.getFieldOfViewModifier();
		}

		this.oldFov = this.fov;
		this.fov += (f - this.fov) * 0.5F;
		if (this.fov > 1.5F) {
			this.fov = 1.5F;
		}

		if (this.fov < 0.1F) {
			this.fov = 0.1F;
		}
	}

	private double getFov(Camera camera, float f, boolean bl) {
		if (this.panoramicMode) {
			return 90.0;
		} else {
			double d = 70.0;
			if (bl) {
				d = (double)((Integer)this.minecraft.options.fov().get()).intValue();
				d *= (double)Mth.lerp(f, this.oldFov, this.fov);
			}

			if (camera.getEntity() instanceof LivingEntity && ((LivingEntity)camera.getEntity()).isDeadOrDying()) {
				float g = Math.min((float)((LivingEntity)camera.getEntity()).deathTime + f, 20.0F);
				d /= (double)((1.0F - 500.0F / (g + 500.0F)) * 2.0F + 1.0F);
			}

			FogType fogType = camera.getFluidInCamera();
			if (fogType == FogType.LAVA || fogType == FogType.WATER) {
				d *= Mth.lerp(this.minecraft.options.fovEffectScale().get(), 1.0, 0.85714287F);
			}

			return d;
		}
	}

	private void bobHurt(PoseStack poseStack, float f) {
		Entity g = this.minecraft.getCameraEntity();
		if (g instanceof LivingEntity livingEntity) {
			float gx = (float)livingEntity.hurtTime - f;
			if (livingEntity.isDeadOrDying()) {
				float h = Math.min((float)livingEntity.deathTime + f, 20.0F);
				poseStack.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (h + 200.0F)));
			}

			if (gx < 0.0F) {
				return;
			}

			float var8 = gx / (float)livingEntity.hurtDuration;
			float var9 = Mth.sin(var8 * var8 * var8 * var8 * (float) Math.PI);
			float h = livingEntity.getHurtDir();
			poseStack.mulPose(Axis.YP.rotationDegrees(-h));
			float i = (float)((double)(-var9) * 14.0 * this.minecraft.options.damageTiltStrength().get());
			poseStack.mulPose(Axis.ZP.rotationDegrees(i));
			poseStack.mulPose(Axis.YP.rotationDegrees(h));
		}
	}

	private void bobView(PoseStack poseStack, float f) {
		if (this.minecraft.getCameraEntity() instanceof Player) {
			Player player = (Player)this.minecraft.getCameraEntity();
			float g = player.walkDist - player.walkDistO;
			float h = -(player.walkDist + g * f);
			float i = Mth.lerp(f, player.oBob, player.bob);
			poseStack.translate(Mth.sin(h * (float) Math.PI) * i * 0.5F, -Math.abs(Mth.cos(h * (float) Math.PI) * i), 0.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(h * (float) Math.PI) * i * 3.0F));
			poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(h * (float) Math.PI - 0.2F) * i) * 5.0F));
		}
	}

	public void renderZoomed(float f, float g, float h) {
		this.zoom = f;
		this.zoomX = g;
		this.zoomY = h;
		this.setRenderBlockOutline(false);
		this.setRenderHand(false);
		this.renderLevel(1.0F, 0L, new PoseStack());
		this.zoom = 1.0F;
	}

	private void renderItemInHand(PoseStack poseStack, Camera camera, float f) {
		if (!this.panoramicMode) {
			this.resetProjectionMatrix(this.getProjectionMatrix(this.getFov(camera, f, false)));
			poseStack.setIdentity();
			poseStack.pushPose();
			this.bobHurt(poseStack, f);
			if (this.minecraft.options.bobView().get()) {
				this.bobView(poseStack, f);
			}

			boolean bl = this.minecraft.getCameraEntity() instanceof LivingEntity && ((LivingEntity)this.minecraft.getCameraEntity()).isSleeping();
			if (this.minecraft.options.getCameraType().isFirstPerson()
				&& !bl
				&& !this.minecraft.options.hideGui
				&& this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
				this.lightTexture.turnOnLightLayer();
				this.itemInHandRenderer
					.renderHandsWithItems(
						f,
						poseStack,
						this.renderBuffers.bufferSource(),
						this.minecraft.player,
						this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, f)
					);
				this.lightTexture.turnOffLightLayer();
			}

			poseStack.popPose();
			if (this.minecraft.options.getCameraType().isFirstPerson() && !bl) {
				ScreenEffectRenderer.renderScreenEffect(this.minecraft, poseStack);
				this.bobHurt(poseStack, f);
			}

			if (this.minecraft.options.bobView().get()) {
				this.bobView(poseStack, f);
			}
		}
	}

	public void resetProjectionMatrix(Matrix4f matrix4f) {
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
	}

	public Matrix4f getProjectionMatrix(double d) {
		PoseStack poseStack = new PoseStack();
		poseStack.last().pose().identity();
		if (this.zoom != 1.0F) {
			poseStack.translate(this.zoomX, -this.zoomY, 0.0F);
			poseStack.scale(this.zoom, this.zoom, 1.0F);
		}

		poseStack.last()
			.pose()
			.mul(
				new Matrix4f()
					.setPerspective(
						(float)(d * (float) (Math.PI / 180.0)),
						(float)this.minecraft.getWindow().getWidth() / (float)this.minecraft.getWindow().getHeight(),
						0.05F,
						this.getDepthFar()
					)
			);
		return poseStack.last().pose();
	}

	public float getDepthFar() {
		return this.renderDistance * 4.0F;
	}

	public static float getNightVisionScale(LivingEntity livingEntity, float f) {
		MobEffectInstance mobEffectInstance = livingEntity.getEffect(MobEffects.NIGHT_VISION);
		return !mobEffectInstance.endsWithin(200) ? 1.0F : 0.7F + Mth.sin(((float)mobEffectInstance.getDuration() - f) * (float) Math.PI * 0.2F) * 0.3F;
	}

	public void render(float f, long l, boolean bl) {
		if (!this.minecraft.isWindowActive()
			&& this.minecraft.options.pauseOnLostFocus
			&& (!this.minecraft.options.touchscreen().get() || !this.minecraft.mouseHandler.isRightPressed())) {
			if (Util.getMillis() - this.lastActiveTime > 500L) {
				this.minecraft.pauseGame(false);
			}
		} else {
			this.lastActiveTime = Util.getMillis();
		}

		if (!this.minecraft.noRender) {
			float g = this.minecraft.level != null && this.minecraft.level.tickRateManager().runsNormally() ? f : 1.0F;
			boolean bl2 = this.minecraft.isGameLoadFinished();
			int i = (int)(
				this.minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth()
			);
			int j = (int)(
				this.minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight()
			);
			RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
			if (bl2 && bl && this.minecraft.level != null) {
				this.minecraft.getProfiler().push("level");
				this.renderLevel(f, l, new PoseStack());
				this.tryTakeScreenshotIfNeeded();
				this.minecraft.levelRenderer.doEntityOutline();
				if (this.postEffect != null && this.effectActive) {
					RenderSystem.disableBlend();
					RenderSystem.disableDepthTest();
					RenderSystem.resetTextureMatrix();
					this.postEffect.process(g);
				}

				this.minecraft.getMainRenderTarget().bindWrite(true);
			}

			Window window = this.minecraft.getWindow();
			RenderSystem.clear(256, Minecraft.ON_OSX);
			Matrix4f matrix4f = new Matrix4f()
				.setOrtho(
					0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, 21000.0F
				);
			RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
			PoseStack poseStack = RenderSystem.getModelViewStack();
			poseStack.pushPose();
			poseStack.setIdentity();
			poseStack.translate(0.0F, 0.0F, -11000.0F);
			RenderSystem.applyModelViewMatrix();
			Lighting.setupFor3DItems();
			GuiGraphics guiGraphics = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());
			if (bl2 && bl && this.minecraft.level != null) {
				this.minecraft.getProfiler().popPush("gui");
				if (this.minecraft.player != null) {
					float h = Mth.lerp(g, this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity);
					float k = ((Double)this.minecraft.options.screenEffectScale().get()).floatValue();
					if (h > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONFUSION) && k < 1.0F) {
						this.renderConfusionOverlay(guiGraphics, h * (1.0F - k));
					}
				}

				if (!this.minecraft.options.hideGui) {
					this.renderItemActivationAnimation(this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight(), g);
				}

				this.minecraft.gui.render(guiGraphics, g);
				RenderSystem.clear(256, Minecraft.ON_OSX);
				this.minecraft.getProfiler().pop();
			}

			if (this.minecraft.getOverlay() != null) {
				try {
					this.minecraft.getOverlay().render(guiGraphics, i, j, this.minecraft.getDeltaFrameTime());
				} catch (Throwable var18) {
					CrashReport crashReport = CrashReport.forThrowable(var18, "Rendering overlay");
					CrashReportCategory crashReportCategory = crashReport.addCategory("Overlay render details");
					crashReportCategory.setDetail("Overlay name", (CrashReportDetail<String>)(() -> this.minecraft.getOverlay().getClass().getCanonicalName()));
					throw new ReportedException(crashReport);
				}
			} else if (bl2 && this.minecraft.screen != null) {
				try {
					this.minecraft.screen.renderWithTooltip(guiGraphics, i, j, this.minecraft.getDeltaFrameTime());
				} catch (Throwable var17) {
					CrashReport crashReport = CrashReport.forThrowable(var17, "Rendering screen");
					CrashReportCategory crashReportCategory = crashReport.addCategory("Screen render details");
					crashReportCategory.setDetail("Screen name", (CrashReportDetail<String>)(() -> this.minecraft.screen.getClass().getCanonicalName()));
					crashReportCategory.setDetail(
						"Mouse location",
						(CrashReportDetail<String>)(() -> String.format(
								Locale.ROOT, "Scaled: (%d, %d). Absolute: (%f, %f)", i, j, this.minecraft.mouseHandler.xpos(), this.minecraft.mouseHandler.ypos()
							))
					);
					crashReportCategory.setDetail(
						"Screen size",
						(CrashReportDetail<String>)(() -> String.format(
								Locale.ROOT,
								"Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %f",
								this.minecraft.getWindow().getGuiScaledWidth(),
								this.minecraft.getWindow().getGuiScaledHeight(),
								this.minecraft.getWindow().getWidth(),
								this.minecraft.getWindow().getHeight(),
								this.minecraft.getWindow().getGuiScale()
							))
					);
					throw new ReportedException(crashReport);
				}

				try {
					if (this.minecraft.screen != null) {
						this.minecraft.screen.handleDelayedNarration();
					}
				} catch (Throwable var16) {
					CrashReport crashReport = CrashReport.forThrowable(var16, "Narrating screen");
					CrashReportCategory crashReportCategory = crashReport.addCategory("Screen details");
					crashReportCategory.setDetail("Screen name", (CrashReportDetail<String>)(() -> this.minecraft.screen.getClass().getCanonicalName()));
					throw new ReportedException(crashReport);
				}
			}

			if (bl2) {
				this.minecraft.getProfiler().push("toasts");
				this.minecraft.getToasts().render(guiGraphics);
				this.minecraft.getProfiler().pop();
			}

			guiGraphics.flush();
			poseStack.popPose();
			RenderSystem.applyModelViewMatrix();
		}
	}

	private void tryTakeScreenshotIfNeeded() {
		if (!this.hasWorldScreenshot && this.minecraft.isLocalServer()) {
			long l = Util.getMillis();
			if (l - this.lastScreenshotAttempt >= 1000L) {
				this.lastScreenshotAttempt = l;
				IntegratedServer integratedServer = this.minecraft.getSingleplayerServer();
				if (integratedServer != null && !integratedServer.isStopped()) {
					integratedServer.getWorldScreenshotFile().ifPresent(path -> {
						if (Files.isRegularFile(path, new LinkOption[0])) {
							this.hasWorldScreenshot = true;
						} else {
							this.takeAutoScreenshot(path);
						}
					});
				}
			}
		}
	}

	private void takeAutoScreenshot(Path path) {
		if (this.minecraft.levelRenderer.countRenderedSections() > 10 && this.minecraft.levelRenderer.hasRenderedAllSections()) {
			NativeImage nativeImage = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
			Util.ioPool().execute(() -> {
				int i = nativeImage.getWidth();
				int j = nativeImage.getHeight();
				int k = 0;
				int l = 0;
				if (i > j) {
					k = (i - j) / 2;
					i = j;
				} else {
					l = (j - i) / 2;
					j = i;
				}

				try (NativeImage nativeImage2 = new NativeImage(64, 64, false)) {
					nativeImage.resizeSubRectTo(k, l, i, j, nativeImage2);
					nativeImage2.writeToFile(path);
				} catch (IOException var16) {
					LOGGER.warn("Couldn't save auto screenshot", var16);
				} finally {
					nativeImage.close();
				}
			});
		}
	}

	private boolean shouldRenderBlockOutline() {
		if (!this.renderBlockOutline) {
			return false;
		} else {
			Entity entity = this.minecraft.getCameraEntity();
			boolean bl = entity instanceof Player && !this.minecraft.options.hideGui;
			if (bl && !((Player)entity).getAbilities().mayBuild) {
				ItemStack itemStack = ((LivingEntity)entity).getMainHandItem();
				HitResult hitResult = this.minecraft.hitResult;
				if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
					BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
					BlockState blockState = this.minecraft.level.getBlockState(blockPos);
					if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
						bl = blockState.getMenuProvider(this.minecraft.level, blockPos) != null;
					} else {
						BlockInWorld blockInWorld = new BlockInWorld(this.minecraft.level, blockPos, false);
						Registry<Block> registry = this.minecraft.level.registryAccess().registryOrThrow(Registries.BLOCK);
						bl = !itemStack.isEmpty()
							&& (itemStack.hasAdventureModeBreakTagForBlock(registry, blockInWorld) || itemStack.hasAdventureModePlaceTagForBlock(registry, blockInWorld));
					}
				}
			}

			return bl;
		}
	}

	public void renderLevel(float f, long l, PoseStack poseStack) {
		this.lightTexture.updateLightTexture(f);
		if (this.minecraft.getCameraEntity() == null) {
			this.minecraft.setCameraEntity(this.minecraft.player);
		}

		this.pick(f);
		this.minecraft.getProfiler().push("center");
		boolean bl = this.shouldRenderBlockOutline();
		this.minecraft.getProfiler().popPush("camera");
		Camera camera = this.mainCamera;
		Entity entity = (Entity)(this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity());
		camera.setup(
			this.minecraft.level,
			entity,
			!this.minecraft.options.getCameraType().isFirstPerson(),
			this.minecraft.options.getCameraType().isMirrored(),
			this.minecraft.level.tickRateManager().isEntityFrozen(entity) ? 1.0F : f
		);
		this.renderDistance = (float)(this.minecraft.options.getEffectiveRenderDistance() * 16);
		PoseStack poseStack2 = new PoseStack();
		double d = this.getFov(camera, f, true);
		poseStack2.mulPoseMatrix(this.getProjectionMatrix(d));
		this.bobHurt(poseStack2, camera.getPartialTickTime());
		if (this.minecraft.options.bobView().get()) {
			this.bobView(poseStack2, camera.getPartialTickTime());
		}

		float g = ((Double)this.minecraft.options.screenEffectScale().get()).floatValue();
		float h = Mth.lerp(f, this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity) * g * g;
		if (h > 0.0F) {
			int i = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
			float j = 5.0F / (h * h + 5.0F) - h * 0.04F;
			j *= j;
			Axis axis = Axis.of(new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F));
			poseStack2.mulPose(axis.rotationDegrees(((float)this.confusionAnimationTick + f) * (float)i));
			poseStack2.scale(1.0F / j, 1.0F, 1.0F);
			float k = -((float)this.confusionAnimationTick + f) * (float)i;
			poseStack2.mulPose(axis.rotationDegrees(k));
		}

		Matrix4f matrix4f = poseStack2.last().pose();
		this.resetProjectionMatrix(matrix4f);
		poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		Matrix3f matrix3f = new Matrix3f(poseStack.last().normal()).invert();
		RenderSystem.setInverseViewRotationMatrix(matrix3f);
		this.minecraft
			.levelRenderer
			.prepareCullFrustum(
				poseStack, camera.getPosition(), this.getProjectionMatrix(Math.max(d, (double)((Integer)this.minecraft.options.fov().get()).intValue()))
			);
		this.minecraft.levelRenderer.renderLevel(poseStack, f, l, bl, camera, this, this.lightTexture, matrix4f);
		this.minecraft.getProfiler().popPush("hand");
		if (this.renderHand) {
			RenderSystem.clear(256, Minecraft.ON_OSX);
			this.renderItemInHand(poseStack, camera, f);
		}

		this.minecraft.getProfiler().pop();
	}

	public void resetData() {
		this.itemActivationItem = null;
		this.mapRenderer.resetData();
		this.mainCamera.reset();
		this.hasWorldScreenshot = false;
	}

	public MapRenderer getMapRenderer() {
		return this.mapRenderer;
	}

	public void displayItemActivation(ItemStack itemStack) {
		this.itemActivationItem = itemStack;
		this.itemActivationTicks = 40;
		this.itemActivationOffX = this.random.nextFloat() * 2.0F - 1.0F;
		this.itemActivationOffY = this.random.nextFloat() * 2.0F - 1.0F;
	}

	private void renderItemActivationAnimation(int i, int j, float f) {
		if (this.itemActivationItem != null && this.itemActivationTicks > 0) {
			int k = 40 - this.itemActivationTicks;
			float g = ((float)k + f) / 40.0F;
			float h = g * g;
			float l = g * h;
			float m = 10.25F * l * h - 24.95F * h * h + 25.5F * l - 13.8F * h + 4.0F * g;
			float n = m * (float) Math.PI;
			float o = this.itemActivationOffX * (float)(i / 4);
			float p = this.itemActivationOffY * (float)(j / 4);
			RenderSystem.enableDepthTest();
			RenderSystem.disableCull();
			PoseStack poseStack = new PoseStack();
			poseStack.pushPose();
			poseStack.translate((float)(i / 2) + o * Mth.abs(Mth.sin(n * 2.0F)), (float)(j / 2) + p * Mth.abs(Mth.sin(n * 2.0F)), -50.0F);
			float q = 50.0F + 175.0F * Mth.sin(n);
			poseStack.scale(q, -q, q);
			poseStack.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(n))));
			poseStack.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
			poseStack.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
			MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
			this.minecraft
				.getItemRenderer()
				.renderStatic(this.itemActivationItem, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, this.minecraft.level, 0);
			poseStack.popPose();
			bufferSource.endBatch();
			RenderSystem.enableCull();
			RenderSystem.disableDepthTest();
		}
	}

	private void renderConfusionOverlay(GuiGraphics guiGraphics, float f) {
		int i = guiGraphics.guiWidth();
		int j = guiGraphics.guiHeight();
		guiGraphics.pose().pushPose();
		float g = Mth.lerp(f, 2.0F, 1.0F);
		guiGraphics.pose().translate((float)i / 2.0F, (float)j / 2.0F, 0.0F);
		guiGraphics.pose().scale(g, g, g);
		guiGraphics.pose().translate((float)(-i) / 2.0F, (float)(-j) / 2.0F, 0.0F);
		float h = 0.2F * f;
		float k = 0.4F * f;
		float l = 0.2F * f;
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
			GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
		);
		guiGraphics.setColor(h, k, l, 1.0F);
		guiGraphics.blit(NAUSEA_LOCATION, 0, 0, -90, 0.0F, 0.0F, i, j, i, j);
		guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		guiGraphics.pose().popPose();
	}

	public Minecraft getMinecraft() {
		return this.minecraft;
	}

	public float getDarkenWorldAmount(float f) {
		return Mth.lerp(f, this.darkenWorldAmountO, this.darkenWorldAmount);
	}

	public float getRenderDistance() {
		return this.renderDistance;
	}

	public Camera getMainCamera() {
		return this.mainCamera;
	}

	public LightTexture lightTexture() {
		return this.lightTexture;
	}

	public OverlayTexture overlayTexture() {
		return this.overlayTexture;
	}

	@Nullable
	public static ShaderInstance getPositionShader() {
		return positionShader;
	}

	@Nullable
	public static ShaderInstance getPositionColorShader() {
		return positionColorShader;
	}

	@Nullable
	public static ShaderInstance getPositionColorTexShader() {
		return positionColorTexShader;
	}

	@Nullable
	public static ShaderInstance getPositionTexShader() {
		return positionTexShader;
	}

	@Nullable
	public static ShaderInstance getPositionTexColorShader() {
		return positionTexColorShader;
	}

	@Nullable
	public static ShaderInstance getParticleShader() {
		return particleShader;
	}

	@Nullable
	public static ShaderInstance getPositionColorLightmapShader() {
		return positionColorLightmapShader;
	}

	@Nullable
	public static ShaderInstance getPositionColorTexLightmapShader() {
		return positionColorTexLightmapShader;
	}

	@Nullable
	public static ShaderInstance getPositionTexColorNormalShader() {
		return positionTexColorNormalShader;
	}

	@Nullable
	public static ShaderInstance getPositionTexLightmapColorShader() {
		return positionTexLightmapColorShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeSolidShader() {
		return rendertypeSolidShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeCutoutMippedShader() {
		return rendertypeCutoutMippedShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeCutoutShader() {
		return rendertypeCutoutShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTranslucentShader() {
		return rendertypeTranslucentShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTranslucentMovingBlockShader() {
		return rendertypeTranslucentMovingBlockShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeArmorCutoutNoCullShader() {
		return rendertypeArmorCutoutNoCullShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntitySolidShader() {
		return rendertypeEntitySolidShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityCutoutShader() {
		return rendertypeEntityCutoutShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityCutoutNoCullShader() {
		return rendertypeEntityCutoutNoCullShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityCutoutNoCullZOffsetShader() {
		return rendertypeEntityCutoutNoCullZOffsetShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeItemEntityTranslucentCullShader() {
		return rendertypeItemEntityTranslucentCullShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityTranslucentCullShader() {
		return rendertypeEntityTranslucentCullShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityTranslucentShader() {
		return rendertypeEntityTranslucentShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityTranslucentEmissiveShader() {
		return rendertypeEntityTranslucentEmissiveShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntitySmoothCutoutShader() {
		return rendertypeEntitySmoothCutoutShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeBeaconBeamShader() {
		return rendertypeBeaconBeamShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityDecalShader() {
		return rendertypeEntityDecalShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityNoOutlineShader() {
		return rendertypeEntityNoOutlineShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityShadowShader() {
		return rendertypeEntityShadowShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityAlphaShader() {
		return rendertypeEntityAlphaShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEyesShader() {
		return rendertypeEyesShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEnergySwirlShader() {
		return rendertypeEnergySwirlShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeBreezeWindShader() {
		return rendertypeBreezeWindShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeLeashShader() {
		return rendertypeLeashShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeWaterMaskShader() {
		return rendertypeWaterMaskShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeOutlineShader() {
		return rendertypeOutlineShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeArmorGlintShader() {
		return rendertypeArmorGlintShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeArmorEntityGlintShader() {
		return rendertypeArmorEntityGlintShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeGlintTranslucentShader() {
		return rendertypeGlintTranslucentShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeGlintShader() {
		return rendertypeGlintShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeGlintDirectShader() {
		return rendertypeGlintDirectShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityGlintShader() {
		return rendertypeEntityGlintShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEntityGlintDirectShader() {
		return rendertypeEntityGlintDirectShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTextShader() {
		return rendertypeTextShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTextBackgroundShader() {
		return rendertypeTextBackgroundShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTextIntensityShader() {
		return rendertypeTextIntensityShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTextSeeThroughShader() {
		return rendertypeTextSeeThroughShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTextBackgroundSeeThroughShader() {
		return rendertypeTextBackgroundSeeThroughShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTextIntensitySeeThroughShader() {
		return rendertypeTextIntensitySeeThroughShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeLightningShader() {
		return rendertypeLightningShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTripwireShader() {
		return rendertypeTripwireShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEndPortalShader() {
		return rendertypeEndPortalShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeEndGatewayShader() {
		return rendertypeEndGatewayShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeLinesShader() {
		return rendertypeLinesShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeCrumblingShader() {
		return rendertypeCrumblingShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeGuiShader() {
		return rendertypeGuiShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeGuiOverlayShader() {
		return rendertypeGuiOverlayShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeGuiTextHighlightShader() {
		return rendertypeGuiTextHighlightShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeGuiGhostRecipeOverlayShader() {
		return rendertypeGuiGhostRecipeOverlayShader;
	}

	@Environment(EnvType.CLIENT)
	public static record ResourceCache(ResourceProvider original, Map<ResourceLocation, Resource> cache) implements ResourceProvider {
		@Override
		public Optional<Resource> getResource(ResourceLocation resourceLocation) {
			Resource resource = (Resource)this.cache.get(resourceLocation);
			return resource != null ? Optional.of(resource) : this.original.getResource(resourceLocation);
		}
	}
}
