package net.minecraft.client.renderer.entity;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

@Environment(EnvType.CLIENT)
public class ItemRenderer implements ResourceManagerReloadListener {
	public static final ResourceLocation ENCHANTED_GLINT_ENTITY = new ResourceLocation("textures/misc/enchanted_glint_entity.png");
	public static final ResourceLocation ENCHANTED_GLINT_ITEM = new ResourceLocation("textures/misc/enchanted_glint_item.png");
	private static final Set<Item> IGNORED = Sets.<Item>newHashSet(Items.AIR);
	public static final int GUI_SLOT_CENTER_X = 8;
	public static final int GUI_SLOT_CENTER_Y = 8;
	public static final int ITEM_COUNT_BLIT_OFFSET = 200;
	public static final float COMPASS_FOIL_UI_SCALE = 0.5F;
	public static final float COMPASS_FOIL_FIRST_PERSON_SCALE = 0.75F;
	public static final float COMPASS_FOIL_TEXTURE_SCALE = 0.0078125F;
	private static final ModelResourceLocation TRIDENT_MODEL = ModelResourceLocation.vanilla("trident", "inventory");
	public static final ModelResourceLocation TRIDENT_IN_HAND_MODEL = ModelResourceLocation.vanilla("trident_in_hand", "inventory");
	private static final ModelResourceLocation SPYGLASS_MODEL = ModelResourceLocation.vanilla("spyglass", "inventory");
	public static final ModelResourceLocation SPYGLASS_IN_HAND_MODEL = ModelResourceLocation.vanilla("spyglass_in_hand", "inventory");
	private final Minecraft minecraft;
	private final ItemModelShaper itemModelShaper;
	private final TextureManager textureManager;
	private final ItemColors itemColors;
	private final BlockEntityWithoutLevelRenderer blockEntityRenderer;

	public ItemRenderer(
		Minecraft minecraft,
		TextureManager textureManager,
		ModelManager modelManager,
		ItemColors itemColors,
		BlockEntityWithoutLevelRenderer blockEntityWithoutLevelRenderer
	) {
		this.minecraft = minecraft;
		this.textureManager = textureManager;
		this.itemModelShaper = new ItemModelShaper(modelManager);
		this.blockEntityRenderer = blockEntityWithoutLevelRenderer;

		for(Item item : BuiltInRegistries.ITEM) {
			if (!IGNORED.contains(item)) {
				this.itemModelShaper.register(item, new ModelResourceLocation(BuiltInRegistries.ITEM.getKey(item), "inventory"));
			}
		}

		this.itemColors = itemColors;
	}

	public ItemModelShaper getItemModelShaper() {
		return this.itemModelShaper;
	}

	private void renderModelLists(BakedModel bakedModel, ItemStack itemStack, int i, int j, PoseStack poseStack, VertexConsumer vertexConsumer) {
		RandomSource randomSource = RandomSource.create();
		long l = 42L;

		for(Direction direction : Direction.values()) {
			randomSource.setSeed(42L);
			this.renderQuadList(poseStack, vertexConsumer, bakedModel.getQuads(null, direction, randomSource), itemStack, i, j);
		}

		randomSource.setSeed(42L);
		this.renderQuadList(poseStack, vertexConsumer, bakedModel.getQuads(null, null, randomSource), itemStack, i, j);
	}

	public void render(
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		boolean bl,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int i,
		int j,
		BakedModel bakedModel
	) {
		if (!itemStack.isEmpty()) {
			poseStack.pushPose();
			boolean bl2 = itemDisplayContext == ItemDisplayContext.GUI
				|| itemDisplayContext == ItemDisplayContext.GROUND
				|| itemDisplayContext == ItemDisplayContext.FIXED;
			if (bl2) {
				if (itemStack.is(Items.TRIDENT)) {
					bakedModel = this.itemModelShaper.getModelManager().getModel(TRIDENT_MODEL);
				} else if (itemStack.is(Items.SPYGLASS)) {
					bakedModel = this.itemModelShaper.getModelManager().getModel(SPYGLASS_MODEL);
				}
			}

			bakedModel.getTransforms().getTransform(itemDisplayContext).apply(bl, poseStack);
			poseStack.translate(-0.5F, -0.5F, -0.5F);
			if (!bakedModel.isCustomRenderer() && (!itemStack.is(Items.TRIDENT) || bl2)) {
				boolean bl3;
				if (itemDisplayContext != ItemDisplayContext.GUI && !itemDisplayContext.firstPerson() && itemStack.getItem() instanceof BlockItem) {
					Block block = ((BlockItem)itemStack.getItem()).getBlock();
					bl3 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
				} else {
					bl3 = true;
				}

				RenderType renderType = ItemBlockRenderTypes.getRenderType(itemStack, bl3);
				VertexConsumer vertexConsumer;
				if (hasAnimatedTexture(itemStack) && itemStack.hasFoil()) {
					poseStack.pushPose();
					PoseStack.Pose pose = poseStack.last();
					if (itemDisplayContext == ItemDisplayContext.GUI) {
						MatrixUtil.mulComponentWise(pose.pose(), 0.5F);
					} else if (itemDisplayContext.firstPerson()) {
						MatrixUtil.mulComponentWise(pose.pose(), 0.75F);
					}

					if (bl3) {
						vertexConsumer = getCompassFoilBufferDirect(multiBufferSource, renderType, pose);
					} else {
						vertexConsumer = getCompassFoilBuffer(multiBufferSource, renderType, pose);
					}

					poseStack.popPose();
				} else if (bl3) {
					vertexConsumer = getFoilBufferDirect(multiBufferSource, renderType, true, itemStack.hasFoil());
				} else {
					vertexConsumer = getFoilBuffer(multiBufferSource, renderType, true, itemStack.hasFoil());
				}

				this.renderModelLists(bakedModel, itemStack, i, j, poseStack, vertexConsumer);
			} else {
				this.blockEntityRenderer.renderByItem(itemStack, itemDisplayContext, poseStack, multiBufferSource, i, j);
			}

			poseStack.popPose();
		}
	}

	private static boolean hasAnimatedTexture(ItemStack itemStack) {
		return itemStack.is(ItemTags.COMPASSES) || itemStack.is(Items.CLOCK);
	}

	public static VertexConsumer getArmorFoilBuffer(MultiBufferSource multiBufferSource, RenderType renderType, boolean bl, boolean bl2) {
		return bl2
			? VertexMultiConsumer.create(
				multiBufferSource.getBuffer(bl ? RenderType.armorGlint() : RenderType.armorEntityGlint()), multiBufferSource.getBuffer(renderType)
			)
			: multiBufferSource.getBuffer(renderType);
	}

	public static VertexConsumer getCompassFoilBuffer(MultiBufferSource multiBufferSource, RenderType renderType, PoseStack.Pose pose) {
		return VertexMultiConsumer.create(
			new SheetedDecalTextureGenerator(multiBufferSource.getBuffer(RenderType.glint()), pose.pose(), pose.normal(), 0.0078125F),
			multiBufferSource.getBuffer(renderType)
		);
	}

	public static VertexConsumer getCompassFoilBufferDirect(MultiBufferSource multiBufferSource, RenderType renderType, PoseStack.Pose pose) {
		return VertexMultiConsumer.create(
			new SheetedDecalTextureGenerator(multiBufferSource.getBuffer(RenderType.glintDirect()), pose.pose(), pose.normal(), 0.0078125F),
			multiBufferSource.getBuffer(renderType)
		);
	}

	public static VertexConsumer getFoilBuffer(MultiBufferSource multiBufferSource, RenderType renderType, boolean bl, boolean bl2) {
		if (bl2) {
			return Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet()
				? VertexMultiConsumer.create(multiBufferSource.getBuffer(RenderType.glintTranslucent()), multiBufferSource.getBuffer(renderType))
				: VertexMultiConsumer.create(multiBufferSource.getBuffer(bl ? RenderType.glint() : RenderType.entityGlint()), multiBufferSource.getBuffer(renderType));
		} else {
			return multiBufferSource.getBuffer(renderType);
		}
	}

	public static VertexConsumer getFoilBufferDirect(MultiBufferSource multiBufferSource, RenderType renderType, boolean bl, boolean bl2) {
		return bl2
			? VertexMultiConsumer.create(
				multiBufferSource.getBuffer(bl ? RenderType.glintDirect() : RenderType.entityGlintDirect()), multiBufferSource.getBuffer(renderType)
			)
			: multiBufferSource.getBuffer(renderType);
	}

	private void renderQuadList(PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> list, ItemStack itemStack, int i, int j) {
		boolean bl = !itemStack.isEmpty();
		PoseStack.Pose pose = poseStack.last();

		for(BakedQuad bakedQuad : list) {
			int k = -1;
			if (bl && bakedQuad.isTinted()) {
				k = this.itemColors.getColor(itemStack, bakedQuad.getTintIndex());
			}

			float f = (float)(k >> 16 & 0xFF) / 255.0F;
			float g = (float)(k >> 8 & 0xFF) / 255.0F;
			float h = (float)(k & 0xFF) / 255.0F;
			vertexConsumer.putBulkData(pose, bakedQuad, f, g, h, i, j);
		}
	}

	public BakedModel getModel(ItemStack itemStack, @Nullable Level level, @Nullable LivingEntity livingEntity, int i) {
		BakedModel bakedModel;
		if (itemStack.is(Items.TRIDENT)) {
			bakedModel = this.itemModelShaper.getModelManager().getModel(TRIDENT_IN_HAND_MODEL);
		} else if (itemStack.is(Items.SPYGLASS)) {
			bakedModel = this.itemModelShaper.getModelManager().getModel(SPYGLASS_IN_HAND_MODEL);
		} else {
			bakedModel = this.itemModelShaper.getItemModel(itemStack);
		}

		ClientLevel clientLevel = level instanceof ClientLevel ? (ClientLevel)level : null;
		BakedModel bakedModel2 = bakedModel.getOverrides().resolve(bakedModel, itemStack, clientLevel, livingEntity, i);
		return bakedModel2 == null ? this.itemModelShaper.getModelManager().getMissingModel() : bakedModel2;
	}

	public void renderStatic(
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		int i,
		int j,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		@Nullable Level level,
		int k
	) {
		this.renderStatic(null, itemStack, itemDisplayContext, false, poseStack, multiBufferSource, level, i, j, k);
	}

	public void renderStatic(
		@Nullable LivingEntity livingEntity,
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		boolean bl,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		@Nullable Level level,
		int i,
		int j,
		int k
	) {
		if (!itemStack.isEmpty()) {
			BakedModel bakedModel = this.getModel(itemStack, level, livingEntity, k);
			this.render(itemStack, itemDisplayContext, bl, poseStack, multiBufferSource, i, j, bakedModel);
		}
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		this.itemModelShaper.rebuildCache();
	}
}
