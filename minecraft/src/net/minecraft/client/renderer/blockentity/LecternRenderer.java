package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.BookModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class LecternRenderer extends BlockEntityRenderer<LecternBlockEntity> {
	private static final ResourceLocation BOOK_LOCATION = new ResourceLocation("textures/entity/enchanting_table_book.png");
	private final BookModel bookModel = new BookModel();

	public void render(LecternBlockEntity lecternBlockEntity, double d, double e, double f, float g, int i) {
		BlockState blockState = lecternBlockEntity.getBlockState();
		if (blockState.getValue(LecternBlock.HAS_BOOK)) {
			RenderSystem.pushMatrix();
			RenderSystem.translatef((float)d + 0.5F, (float)e + 1.0F + 0.0625F, (float)f + 0.5F);
			float h = ((Direction)blockState.getValue(LecternBlock.FACING)).getClockWise().toYRot();
			RenderSystem.rotatef(-h, 0.0F, 1.0F, 0.0F);
			RenderSystem.rotatef(67.5F, 0.0F, 0.0F, 1.0F);
			RenderSystem.translatef(0.0F, -0.125F, 0.0F);
			this.bindTexture(BOOK_LOCATION);
			RenderSystem.enableCull();
			this.bookModel.render(0.0F, 0.1F, 0.9F, 1.2F, 0.0F, 0.0625F);
			RenderSystem.popMatrix();
		}
	}
}
