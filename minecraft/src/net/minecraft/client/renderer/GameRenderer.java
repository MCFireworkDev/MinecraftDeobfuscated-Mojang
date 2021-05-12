package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
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
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class GameRenderer implements ResourceManagerReloadListener, AutoCloseable {
	private static final ResourceLocation NAUSEA_LOCATION = new ResourceLocation("textures/misc/nausea.png");
	private static final Logger LOGGER = LogManager.getLogger();
	private static final boolean DEPTH_BUFFER_DEBUG = false;
	public static final float PROJECTION_Z_NEAR = 0.05F;
	private final Minecraft minecraft;
	private final ResourceManager resourceManager;
	private final Random random = new Random();
	private float renderDistance;
	public final ItemInHandRenderer itemInHandRenderer;
	private final MapRenderer mapRenderer;
	private final RenderBuffers renderBuffers;
	private int tick;
	private float fov;
	private float oldFov;
	private float darkenWorldAmount;
	private float darkenWorldAmountO;
	private boolean renderHand = true;
	private boolean renderBlockOutline = true;
	private long lastScreenshotAttempt;
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
	private PostChain postEffect;
	private static final ResourceLocation[] EFFECTS = new ResourceLocation[]{
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
	private int effectIndex = EFFECT_NONE;
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
	private static ShaderInstance blockShader;
	@Nullable
	private static ShaderInstance newEntityShader;
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
	private static ShaderInstance rendertypeTranslucentNoCrumblingShader;
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
	private static ShaderInstance rendertypeTextIntensityShader;
	@Nullable
	private static ShaderInstance rendertypeTextSeeThroughShader;
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

	public GameRenderer(Minecraft minecraft, ResourceManager resourceManager, RenderBuffers renderBuffers) {
		this.minecraft = minecraft;
		this.resourceManager = resourceManager;
		this.itemInHandRenderer = minecraft.getItemInHandRenderer();
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

	private void loadEffect(ResourceLocation resourceLocation) {
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

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		this.reloadShaders(resourceManager);
		if (this.postEffect != null) {
			this.postEffect.close();
		}

		this.postEffect = null;
		if (this.effectIndex == EFFECT_NONE) {
			this.checkEntityPostEffect(this.minecraft.getCameraEntity());
		} else {
			this.loadEffect(EFFECTS[this.effectIndex]);
		}
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

	public void reloadShaders(ResourceManager resourceManager) {
		RenderSystem.assertThread(RenderSystem::isOnRenderThread);
		List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list = Lists.<Pair<ShaderInstance, Consumer<ShaderInstance>>>newArrayListWithCapacity(
			this.shaders.size()
		);

		try {
			list.add(Pair.of(new ShaderInstance(resourceManager, "block", DefaultVertexFormat.BLOCK), (Consumer)shaderInstance -> blockShader = shaderInstance));
			list.add(
				Pair.of(new ShaderInstance(resourceManager, "new_entity", DefaultVertexFormat.NEW_ENTITY), (Consumer)shaderInstance -> newEntityShader = shaderInstance)
			);
			list.add(
				Pair.of(new ShaderInstance(resourceManager, "particle", DefaultVertexFormat.PARTICLE), (Consumer)shaderInstance -> particleShader = shaderInstance)
			);
			list.add(
				Pair.of(new ShaderInstance(resourceManager, "position", DefaultVertexFormat.POSITION), (Consumer)shaderInstance -> positionShader = shaderInstance)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_color", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> positionColorShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_color_lightmap", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
					(Consumer)shaderInstance -> positionColorLightmapShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX),
					(Consumer)shaderInstance -> positionColorTexShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> positionColorTexLightmapShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_tex", DefaultVertexFormat.POSITION_TEX), (Consumer)shaderInstance -> positionTexShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR),
					(Consumer)shaderInstance -> positionTexColorShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_tex_color_normal", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),
					(Consumer)shaderInstance -> positionTexColorNormalShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "position_tex_lightmap_color", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR),
					(Consumer)shaderInstance -> positionTexLightmapColorShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_solid", DefaultVertexFormat.BLOCK), (Consumer)shaderInstance -> rendertypeSolidShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_cutout_mipped", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeCutoutMippedShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_cutout", DefaultVertexFormat.BLOCK), (Consumer)shaderInstance -> rendertypeCutoutShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_translucent", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeTranslucentShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeTranslucentMovingBlockShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_translucent_no_crumbling", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeTranslucentNoCrumblingShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeArmorCutoutNoCullShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntitySolidShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityCutoutShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityCutoutNoCullShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityCutoutNoCullZOffsetShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeItemEntityTranslucentCullShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityTranslucentCullShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityTranslucentShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntitySmoothCutoutShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeBeaconBeamShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityDecalShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityNoOutlineShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityShadowShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEntityAlphaShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY), (Consumer)shaderInstance -> rendertypeEyesShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY),
					(Consumer)shaderInstance -> rendertypeEnergySwirlShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeLeashShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_water_mask", DefaultVertexFormat.POSITION),
					(Consumer)shaderInstance -> rendertypeWaterMaskShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_outline", DefaultVertexFormat.POSITION_COLOR_TEX),
					(Consumer)shaderInstance -> rendertypeOutlineShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_armor_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeArmorGlintShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_armor_entity_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeArmorEntityGlintShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeGlintTranslucentShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeGlintShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_glint_direct", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeGlintDirectShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_glint", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeEntityGlintShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_entity_glint_direct", DefaultVertexFormat.POSITION_TEX),
					(Consumer)shaderInstance -> rendertypeEntityGlintDirectShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextIntensityShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextSeeThroughShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
					(Consumer)shaderInstance -> rendertypeTextIntensitySeeThroughShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR),
					(Consumer)shaderInstance -> rendertypeLightningShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_tripwire", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeTripwireShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_end_portal", DefaultVertexFormat.POSITION),
					(Consumer)shaderInstance -> rendertypeEndPortalShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_end_gateway", DefaultVertexFormat.POSITION),
					(Consumer)shaderInstance -> rendertypeEndGatewayShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL),
					(Consumer)shaderInstance -> rendertypeLinesShader = shaderInstance
				)
			);
			list.add(
				Pair.of(
					new ShaderInstance(resourceManager, "rendertype_crumbling", DefaultVertexFormat.BLOCK),
					(Consumer)shaderInstance -> rendertypeCrumblingShader = shaderInstance
				)
			);
		} catch (IOException var4) {
			list.forEach(pair -> ((ShaderInstance)pair.getFirst()).close());
			throw new RuntimeException("could not reload shaders", var4);
		}

		this.shutdownShaders();
		list.forEach(pair -> {
			ShaderInstance shaderInstance = (ShaderInstance)pair.getFirst();
			this.shaders.put(shaderInstance.getName(), shaderInstance);
			((Consumer)pair.getSecond()).accept(shaderInstance);
		});
	}

	private void shutdownShaders() {
		RenderSystem.assertThread(RenderSystem::isOnRenderThread);
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
		++this.tick;
		this.itemInHandRenderer.tick();
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
			if (this.minecraft.level != null) {
				this.minecraft.getProfiler().push("pick");
				this.minecraft.crosshairPickEntity = null;
				double d = (double)this.minecraft.gameMode.getPickRange();
				this.minecraft.hitResult = entity.pick(d, f, false);
				Vec3 vec3 = entity.getEyePosition(f);
				boolean bl = false;
				int i = 3;
				double e = d;
				if (this.minecraft.gameMode.hasFarPickRange()) {
					e = 6.0;
					d = e;
				} else {
					if (d > 3.0) {
						bl = true;
					}

					d = d;
				}

				e *= e;
				if (this.minecraft.hitResult != null) {
					e = this.minecraft.hitResult.getLocation().distanceToSqr(vec3);
				}

				Vec3 vec32 = entity.getViewVector(1.0F);
				Vec3 vec33 = vec3.add(vec32.x * d, vec32.y * d, vec32.z * d);
				float g = 1.0F;
				AABB aABB = entity.getBoundingBox().expandTowards(vec32.scale(d)).inflate(1.0, 1.0, 1.0);
				EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
					entity, vec3, vec33, aABB, entityx -> !entityx.isSpectator() && entityx.isPickable(), e
				);
				if (entityHitResult != null) {
					Entity entity2 = entityHitResult.getEntity();
					Vec3 vec34 = entityHitResult.getLocation();
					double h = vec3.distanceToSqr(vec34);
					if (bl && h > 9.0) {
						this.minecraft.hitResult = BlockHitResult.miss(vec34, Direction.getNearest(vec32.x, vec32.y, vec32.z), new BlockPos(vec34));
					} else if (h < e || this.minecraft.hitResult == null) {
						this.minecraft.hitResult = entityHitResult;
						if (entity2 instanceof LivingEntity || entity2 instanceof ItemFrame) {
							this.minecraft.crosshairPickEntity = entity2;
						}
					}
				}

				this.minecraft.getProfiler().pop();
			}
		}
	}

	private void tickFov() {
		float f = 1.0F;
		if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractClientPlayer) {
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
				d = this.minecraft.options.fov;
				d *= (double)Mth.lerp(f, this.oldFov, this.fov);
			}

			if (camera.getEntity() instanceof LivingEntity && ((LivingEntity)camera.getEntity()).isDeadOrDying()) {
				float g = Math.min((float)((LivingEntity)camera.getEntity()).deathTime + f, 20.0F);
				d /= (double)((1.0F - 500.0F / (g + 500.0F)) * 2.0F + 1.0F);
			}

			FogType fogType = camera.getFluidInCamera();
			if (fogType == FogType.LAVA || fogType == FogType.WATER) {
				d *= (double)Mth.lerp(this.minecraft.options.fovEffectScale, 1.0F, 0.85714287F);
			}

			return d;
		}
	}

	private void bobHurt(PoseStack poseStack, float f) {
		if (this.minecraft.getCameraEntity() instanceof LivingEntity livingEntity) {
			float g = (float)livingEntity.hurtTime - f;
			if (livingEntity.isDeadOrDying()) {
				float h = Math.min((float)livingEntity.deathTime + f, 20.0F);
				poseStack.mulPose(Vector3f.ZP.rotationDegrees(40.0F - 8000.0F / (h + 200.0F)));
			}

			if (g < 0.0F) {
				return;
			}

			g /= (float)livingEntity.hurtDuration;
			g = Mth.sin(g * g * g * g * (float) Math.PI);
			float h = livingEntity.hurtDir;
			poseStack.mulPose(Vector3f.YP.rotationDegrees(-h));
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(-g * 14.0F));
			poseStack.mulPose(Vector3f.YP.rotationDegrees(h));
		}
	}

	private void bobView(PoseStack poseStack, float f) {
		if (this.minecraft.getCameraEntity() instanceof Player) {
			Player player = (Player)this.minecraft.getCameraEntity();
			float g = player.walkDist - player.walkDistO;
			float h = -(player.walkDist + g * f);
			float i = Mth.lerp(f, player.oBob, player.bob);
			poseStack.translate((double)(Mth.sin(h * (float) Math.PI) * i * 0.5F), (double)(-Math.abs(Mth.cos(h * (float) Math.PI) * i)), 0.0);
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(Mth.sin(h * (float) Math.PI) * i * 3.0F));
			poseStack.mulPose(Vector3f.XP.rotationDegrees(Math.abs(Mth.cos(h * (float) Math.PI - 0.2F) * i) * 5.0F));
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
			PoseStack.Pose pose = poseStack.last();
			pose.pose().setIdentity();
			pose.normal().setIdentity();
			poseStack.pushPose();
			this.bobHurt(poseStack, f);
			if (this.minecraft.options.bobView) {
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

			if (this.minecraft.options.bobView) {
				this.bobView(poseStack, f);
			}
		}
	}

	public void resetProjectionMatrix(Matrix4f matrix4f) {
		RenderSystem.setProjectionMatrix(matrix4f);
	}

	public Matrix4f getProjectionMatrix(double d) {
		PoseStack poseStack = new PoseStack();
		poseStack.last().pose().setIdentity();
		if (this.zoom != 1.0F) {
			poseStack.translate((double)this.zoomX, (double)(-this.zoomY), 0.0);
			poseStack.scale(this.zoom, this.zoom, 1.0F);
		}

		poseStack.last()
			.pose()
			.multiply(Matrix4f.perspective(d, (float)this.minecraft.getWindow().getWidth() / (float)this.minecraft.getWindow().getHeight(), 0.05F, this.getDepthFar()));
		return poseStack.last().pose();
	}

	public float getDepthFar() {
		return this.renderDistance * 4.0F;
	}

	public static float getNightVisionScale(LivingEntity livingEntity, float f) {
		int i = livingEntity.getEffect(MobEffects.NIGHT_VISION).getDuration();
		return i > 200 ? 1.0F : 0.7F + Mth.sin(((float)i - f) * (float) Math.PI * 0.2F) * 0.3F;
	}

	public void render(float f, long l, boolean bl) {
		if (!this.minecraft.isWindowActive()
			&& this.minecraft.options.pauseOnLostFocus
			&& (!this.minecraft.options.touchscreen || !this.minecraft.mouseHandler.isRightPressed())) {
			if (Util.getMillis() - this.lastActiveTime > 500L) {
				this.minecraft.pauseGame(false);
			}
		} else {
			this.lastActiveTime = Util.getMillis();
		}

		if (!this.minecraft.noRender) {
			int i = (int)(
				this.minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth()
			);
			int j = (int)(
				this.minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight()
			);
			RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
			if (bl && this.minecraft.level != null) {
				this.minecraft.getProfiler().push("level");
				this.renderLevel(f, l, new PoseStack());
				if (this.minecraft.hasSingleplayerServer() && this.lastScreenshotAttempt < Util.getMillis() - 1000L) {
					this.lastScreenshotAttempt = Util.getMillis();
					if (!this.minecraft.getSingleplayerServer().hasWorldScreenshot()) {
						this.takeAutoScreenshot();
					}
				}

				this.minecraft.levelRenderer.doEntityOutline();
				if (this.postEffect != null && this.effectActive) {
					RenderSystem.disableBlend();
					RenderSystem.disableDepthTest();
					RenderSystem.enableTexture();
					RenderSystem.resetTextureMatrix();
					this.postEffect.process(f);
				}

				this.minecraft.getMainRenderTarget().bindWrite(true);
			}

			Window window = this.minecraft.getWindow();
			RenderSystem.clear(256, Minecraft.ON_OSX);
			Matrix4f matrix4f = Matrix4f.orthographic(
				0.0F, (float)((double)window.getWidth() / window.getGuiScale()), 0.0F, (float)((double)window.getHeight() / window.getGuiScale()), 1000.0F, 3000.0F
			);
			RenderSystem.setProjectionMatrix(matrix4f);
			PoseStack poseStack = RenderSystem.getModelViewStack();
			poseStack.setIdentity();
			poseStack.translate(0.0, 0.0, -2000.0);
			RenderSystem.applyModelViewMatrix();
			Lighting.setupFor3DItems();
			PoseStack poseStack2 = new PoseStack();
			if (bl && this.minecraft.level != null) {
				this.minecraft.getProfiler().popPush("gui");
				if (this.minecraft.player != null) {
					float g = Mth.lerp(f, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime);
					if (g > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONFUSION) && this.minecraft.options.screenEffectScale < 1.0F) {
						this.renderConfusionOverlay(g * (1.0F - this.minecraft.options.screenEffectScale));
					}
				}

				if (!this.minecraft.options.hideGui || this.minecraft.screen != null) {
					this.renderItemActivationAnimation(this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight(), f);
					this.minecraft.gui.render(poseStack2, f);
					RenderSystem.clear(256, Minecraft.ON_OSX);
				}

				this.minecraft.getProfiler().pop();
			}

			if (this.minecraft.getOverlay() != null) {
				try {
					this.minecraft.getOverlay().render(poseStack2, i, j, this.minecraft.getDeltaFrameTime());
				} catch (Throwable var15) {
					CrashReport crashReport = CrashReport.forThrowable(var15, "Rendering overlay");
					CrashReportCategory crashReportCategory = crashReport.addCategory("Overlay render details");
					crashReportCategory.setDetail("Overlay name", (CrashReportDetail<String>)(() -> this.minecraft.getOverlay().getClass().getCanonicalName()));
					throw new ReportedException(crashReport);
				}
			} else if (this.minecraft.screen != null) {
				try {
					this.minecraft.screen.render(poseStack2, i, j, this.minecraft.getDeltaFrameTime());
				} catch (Throwable var14) {
					CrashReport crashReport = CrashReport.forThrowable(var14, "Rendering screen");
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
			}
		}
	}

	private void takeAutoScreenshot() {
		if (this.minecraft.levelRenderer.countRenderedChunks() > 10
			&& this.minecraft.levelRenderer.hasRenderedAllChunks()
			&& !this.minecraft.getSingleplayerServer().hasWorldScreenshot()) {
			NativeImage nativeImage = Screenshot.takeScreenshot(
				this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), this.minecraft.getMainRenderTarget()
			);
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
					nativeImage2.writeToFile(this.minecraft.getSingleplayerServer().getWorldScreenshotFile());
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
						bl = !itemStack.isEmpty()
							&& (
								itemStack.hasAdventureModeBreakTagForBlock(this.minecraft.level.getTagManager(), blockInWorld)
									|| itemStack.hasAdventureModePlaceTagForBlock(this.minecraft.level.getTagManager(), blockInWorld)
							);
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
		this.renderDistance = (float)(this.minecraft.options.renderDistance * 16);
		PoseStack poseStack2 = new PoseStack();
		double d = this.getFov(camera, f, true);
		poseStack2.last().pose().multiply(this.getProjectionMatrix(d));
		this.bobHurt(poseStack2, f);
		if (this.minecraft.options.bobView) {
			this.bobView(poseStack2, f);
		}

		float g = Mth.lerp(f, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime)
			* this.minecraft.options.screenEffectScale
			* this.minecraft.options.screenEffectScale;
		if (g > 0.0F) {
			int i = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
			float h = 5.0F / (g * g + 5.0F) - g * 0.04F;
			h *= h;
			Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
			poseStack2.mulPose(vector3f.rotationDegrees(((float)this.tick + f) * (float)i));
			poseStack2.scale(1.0F / h, 1.0F, 1.0F);
			float j = -((float)this.tick + f) * (float)i;
			poseStack2.mulPose(vector3f.rotationDegrees(j));
		}

		Matrix4f matrix4f = poseStack2.last().pose();
		this.resetProjectionMatrix(matrix4f);
		camera.setup(
			this.minecraft.level,
			(Entity)(this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity()),
			!this.minecraft.options.getCameraType().isFirstPerson(),
			this.minecraft.options.getCameraType().isMirrored(),
			f
		);
		poseStack.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
		poseStack.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot() + 180.0F));
		this.minecraft.levelRenderer.prepareCullFrustum(poseStack, camera.getPosition(), this.getProjectionMatrix(Math.max(d, this.minecraft.options.fov)));
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
			poseStack.translate((double)((float)(i / 2) + o * Mth.abs(Mth.sin(n * 2.0F))), (double)((float)(j / 2) + p * Mth.abs(Mth.sin(n * 2.0F))), -50.0);
			float q = 50.0F + 175.0F * Mth.sin(n);
			poseStack.scale(q, -q, q);
			poseStack.mulPose(Vector3f.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(n))));
			poseStack.mulPose(Vector3f.XP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
			poseStack.mulPose(Vector3f.ZP.rotationDegrees(6.0F * Mth.cos(g * 8.0F)));
			MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
			this.minecraft
				.getItemRenderer()
				.renderStatic(this.itemActivationItem, ItemTransforms.TransformType.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, 0);
			poseStack.popPose();
			bufferSource.endBatch();
			RenderSystem.enableCull();
			RenderSystem.disableDepthTest();
		}
	}

	private void renderConfusionOverlay(float f) {
		int i = this.minecraft.getWindow().getGuiScaledWidth();
		int j = this.minecraft.getWindow().getGuiScaledHeight();
		double d = Mth.lerp((double)f, 2.0, 1.0);
		float g = 0.2F * f;
		float h = 0.4F * f;
		float k = 0.2F * f;
		double e = (double)i * d;
		double l = (double)j * d;
		double m = ((double)i - e) / 2.0;
		double n = ((double)j - l) / 2.0;
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
			GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
		);
		RenderSystem.setShaderColor(g, h, k, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, NAUSEA_LOCATION);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(m, n + l, -90.0).uv(0.0F, 1.0F).endVertex();
		bufferBuilder.vertex(m + e, n + l, -90.0).uv(1.0F, 1.0F).endVertex();
		bufferBuilder.vertex(m + e, n, -90.0).uv(1.0F, 0.0F).endVertex();
		bufferBuilder.vertex(m, n, -90.0).uv(0.0F, 0.0F).endVertex();
		tesselator.end();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
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
	public static ShaderInstance getBlockShader() {
		return blockShader;
	}

	@Nullable
	public static ShaderInstance getNewEntityShader() {
		return newEntityShader;
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
	public static ShaderInstance getRendertypeTranslucentNoCrumblingShader() {
		return rendertypeTranslucentNoCrumblingShader;
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
	public static ShaderInstance getRendertypeTextIntensityShader() {
		return rendertypeTextIntensityShader;
	}

	@Nullable
	public static ShaderInstance getRendertypeTextSeeThroughShader() {
		return rendertypeTextSeeThroughShader;
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
}
