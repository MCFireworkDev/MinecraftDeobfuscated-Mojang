package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerTradeItemLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;

@Environment(EnvType.CLIENT)
public class WanderingTraderRenderer extends MobRenderer<WanderingTrader, VillagerModel<WanderingTrader>> {
	private static final ResourceLocation VILLAGER_BASE_SKIN = new ResourceLocation("textures/entity/wandering_trader.png");

	public WanderingTraderRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		super(entityRenderDispatcher, new VillagerModel<>(0.0F), 0.5F);
		this.addLayer(new CustomHeadLayer<>(this));
		this.addLayer(new VillagerTradeItemLayer<>(this));
	}

	protected ResourceLocation getTextureLocation(WanderingTrader wanderingTrader) {
		return VILLAGER_BASE_SKIN;
	}

	protected void scale(WanderingTrader wanderingTrader, float f) {
		float g = 0.9375F;
		GlStateManager.scalef(0.9375F, 0.9375F, 0.9375F);
	}
}
