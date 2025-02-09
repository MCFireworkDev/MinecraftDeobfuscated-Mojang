package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimPattern;

@Environment(EnvType.CLIENT)
public class HumanoidArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
	private static final Map<String, ResourceLocation> ARMOR_LOCATION_CACHE = Maps.newHashMap();
	private final A innerModel;
	private final A outerModel;
	private final TextureAtlas armorTrimAtlas;

	public HumanoidArmorLayer(RenderLayerParent<T, M> renderLayerParent, A humanoidModel, A humanoidModel2, ModelManager modelManager) {
		super(renderLayerParent);
		this.innerModel = humanoidModel;
		this.outerModel = humanoidModel2;
		this.armorTrimAtlas = modelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET);
	}

	public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, T livingEntity, float f, float g, float h, float j, float k, float l) {
		this.renderArmorPiece(poseStack, multiBufferSource, livingEntity, EquipmentSlot.CHEST, i, this.getArmorModel(EquipmentSlot.CHEST));
		this.renderArmorPiece(poseStack, multiBufferSource, livingEntity, EquipmentSlot.LEGS, i, this.getArmorModel(EquipmentSlot.LEGS));
		this.renderArmorPiece(poseStack, multiBufferSource, livingEntity, EquipmentSlot.FEET, i, this.getArmorModel(EquipmentSlot.FEET));
		this.renderArmorPiece(poseStack, multiBufferSource, livingEntity, EquipmentSlot.HEAD, i, this.getArmorModel(EquipmentSlot.HEAD));
	}

	private void renderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int i, A humanoidModel) {
		ItemStack itemStack = livingEntity.getItemBySlot(equipmentSlot);
		Item bl = itemStack.getItem();
		if (bl instanceof ArmorItem armorItem) {
			if (armorItem.getEquipmentSlot() == equipmentSlot) {
				this.getParentModel().copyPropertiesTo(humanoidModel);
				this.setPartVisibility(humanoidModel, equipmentSlot);
				boolean blx = this.usesInnerModel(equipmentSlot);
				if (armorItem instanceof DyeableArmorItem dyeableArmorItem) {
					int j = dyeableArmorItem.getColor(itemStack);
					float f = (float)(j >> 16 & 0xFF) / 255.0F;
					float g = (float)(j >> 8 & 0xFF) / 255.0F;
					float h = (float)(j & 0xFF) / 255.0F;
					this.renderModel(poseStack, multiBufferSource, i, armorItem, humanoidModel, blx, f, g, h, null);
					this.renderModel(poseStack, multiBufferSource, i, armorItem, humanoidModel, blx, 1.0F, 1.0F, 1.0F, "overlay");
				} else {
					this.renderModel(poseStack, multiBufferSource, i, armorItem, humanoidModel, blx, 1.0F, 1.0F, 1.0F, null);
				}

				ArmorTrim.getTrim(livingEntity.level().registryAccess(), itemStack, true)
					.ifPresent(armorTrim -> this.renderTrim(armorItem.getMaterial(), poseStack, multiBufferSource, i, armorTrim, humanoidModel, bl));
				if (itemStack.hasFoil()) {
					this.renderGlint(poseStack, multiBufferSource, i, humanoidModel);
				}
			}
		}
	}

	protected void setPartVisibility(A humanoidModel, EquipmentSlot equipmentSlot) {
		humanoidModel.setAllVisible(false);
		switch(equipmentSlot) {
			case HEAD:
				humanoidModel.head.visible = true;
				humanoidModel.hat.visible = true;
				break;
			case CHEST:
				humanoidModel.body.visible = true;
				humanoidModel.rightArm.visible = true;
				humanoidModel.leftArm.visible = true;
				break;
			case LEGS:
				humanoidModel.body.visible = true;
				humanoidModel.rightLeg.visible = true;
				humanoidModel.leftLeg.visible = true;
				break;
			case FEET:
				humanoidModel.rightLeg.visible = true;
				humanoidModel.leftLeg.visible = true;
		}
	}

	private void renderModel(
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int i,
		ArmorItem armorItem,
		A humanoidModel,
		boolean bl,
		float f,
		float g,
		float h,
		@Nullable String string
	) {
		VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.armorCutoutNoCull(this.getArmorLocation(armorItem, bl, string)));
		humanoidModel.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, f, g, h, 1.0F);
	}

	private void renderTrim(
		ArmorMaterial armorMaterial, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ArmorTrim armorTrim, A humanoidModel, boolean bl
	) {
		TextureAtlasSprite textureAtlasSprite = this.armorTrimAtlas.getSprite(bl ? armorTrim.innerTexture(armorMaterial) : armorTrim.outerTexture(armorMaterial));
		VertexConsumer vertexConsumer = textureAtlasSprite.wrap(
			multiBufferSource.getBuffer(Sheets.armorTrimsSheet(((TrimPattern)armorTrim.pattern().value()).decal()))
		);
		humanoidModel.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void renderGlint(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, A humanoidModel) {
		humanoidModel.renderToBuffer(poseStack, multiBufferSource.getBuffer(RenderType.armorEntityGlint()), i, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
	}

	private A getArmorModel(EquipmentSlot equipmentSlot) {
		return (A)(this.usesInnerModel(equipmentSlot) ? this.innerModel : this.outerModel);
	}

	private boolean usesInnerModel(EquipmentSlot equipmentSlot) {
		return equipmentSlot == EquipmentSlot.LEGS;
	}

	private ResourceLocation getArmorLocation(ArmorItem armorItem, boolean bl, @Nullable String string) {
		String string2 = "textures/models/armor/" + armorItem.getMaterial().getName() + "_layer_" + (bl ? 2 : 1) + (string == null ? "" : "_" + string) + ".png";
		return (ResourceLocation)ARMOR_LOCATION_CACHE.computeIfAbsent(string2, ResourceLocation::new);
	}
}
