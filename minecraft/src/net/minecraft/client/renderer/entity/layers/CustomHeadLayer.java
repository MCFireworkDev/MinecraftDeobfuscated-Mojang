package net.minecraft.client.renderer.entity.layers;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.HeadedModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.apache.commons.lang3.StringUtils;

@Environment(EnvType.CLIENT)
public class CustomHeadLayer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
	public CustomHeadLayer(RenderLayerParent<T, M> renderLayerParent) {
		super(renderLayerParent);
	}

	public void render(T livingEntity, float f, float g, float h, float i, float j, float k, float l) {
		ItemStack itemStack = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
		if (!itemStack.isEmpty()) {
			Item item = itemStack.getItem();
			GlStateManager.pushMatrix();
			if (livingEntity.isVisuallySneaking()) {
				GlStateManager.translatef(0.0F, 0.2F, 0.0F);
			}

			boolean bl = livingEntity instanceof Villager || livingEntity instanceof ZombieVillager;
			if (livingEntity.isBaby() && !(livingEntity instanceof Villager)) {
				float m = 2.0F;
				float n = 1.4F;
				GlStateManager.translatef(0.0F, 0.5F * l, 0.0F);
				GlStateManager.scalef(0.7F, 0.7F, 0.7F);
				GlStateManager.translatef(0.0F, 16.0F * l, 0.0F);
			}

			this.getParentModel().translateToHead(0.0625F);
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			if (item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof AbstractSkullBlock) {
				float m = 1.1875F;
				GlStateManager.scalef(1.1875F, -1.1875F, -1.1875F);
				if (bl) {
					GlStateManager.translatef(0.0F, 0.0625F, 0.0F);
				}

				GameProfile gameProfile = null;
				if (itemStack.hasTag()) {
					CompoundTag compoundTag = itemStack.getTag();
					if (compoundTag.contains("SkullOwner", 10)) {
						gameProfile = NbtUtils.readGameProfile(compoundTag.getCompound("SkullOwner"));
					} else if (compoundTag.contains("SkullOwner", 8)) {
						String string = compoundTag.getString("SkullOwner");
						if (!StringUtils.isBlank(string)) {
							gameProfile = SkullBlockEntity.updateGameprofile(new GameProfile(null, string));
							compoundTag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), gameProfile));
						}
					}
				}

				SkullBlockRenderer.instance
					.renderSkull(-0.5F, 0.0F, -0.5F, null, 180.0F, ((AbstractSkullBlock)((BlockItem)item).getBlock()).getType(), gameProfile, -1, f);
			} else if (!(item instanceof ArmorItem) || ((ArmorItem)item).getSlot() != EquipmentSlot.HEAD) {
				float m = 0.625F;
				GlStateManager.translatef(0.0F, -0.25F, 0.0F);
				GlStateManager.rotatef(180.0F, 0.0F, 1.0F, 0.0F);
				GlStateManager.scalef(0.625F, -0.625F, -0.625F);
				if (bl) {
					GlStateManager.translatef(0.0F, 0.1875F, 0.0F);
				}

				Minecraft.getInstance().getItemInHandRenderer().renderItem(livingEntity, itemStack, ItemTransforms.TransformType.HEAD);
			}

			GlStateManager.popMatrix();
		}
	}

	@Override
	public boolean colorsOnDamage() {
		return false;
	}
}
