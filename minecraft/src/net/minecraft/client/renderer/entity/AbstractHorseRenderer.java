package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HorseModel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

@Environment(EnvType.CLIENT)
public abstract class AbstractHorseRenderer<T extends AbstractHorse, M extends HorseModel<T>> extends MobRenderer<T, M> {
	private final float scale;

	public AbstractHorseRenderer(EntityRenderDispatcher entityRenderDispatcher, M horseModel, float f) {
		super(entityRenderDispatcher, horseModel, 0.75F);
		this.scale = f;
	}

	protected void scale(T abstractHorse, float f) {
		GlStateManager.scalef(this.scale, this.scale, this.scale);
		super.scale(abstractHorse, f);
	}
}
