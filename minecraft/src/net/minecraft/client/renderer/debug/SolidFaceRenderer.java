package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
	private final Minecraft minecraft;

	public SolidFaceRenderer(Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	@Override
	public void render(long l) {
		Camera camera = this.minecraft.gameRenderer.getMainCamera();
		double d = camera.getPosition().x;
		double e = camera.getPosition().y;
		double f = camera.getPosition().z;
		BlockGetter blockGetter = this.minecraft.player.level;
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(
			GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
		);
		GlStateManager.lineWidth(2.0F);
		GlStateManager.disableTexture();
		GlStateManager.depthMask(false);
		BlockPos blockPos = new BlockPos(camera.getPosition());

		for(BlockPos blockPos2 : BlockPos.betweenClosed(blockPos.offset(-6, -6, -6), blockPos.offset(6, 6, 6))) {
			BlockState blockState = blockGetter.getBlockState(blockPos2);
			if (blockState.getBlock() != Blocks.AIR) {
				VoxelShape voxelShape = blockState.getShape(blockGetter, blockPos2);

				for(AABB aABB : voxelShape.toAabbs()) {
					AABB aABB2 = aABB.move(blockPos2).inflate(0.002).move(-d, -e, -f);
					double g = aABB2.minX;
					double h = aABB2.minY;
					double i = aABB2.minZ;
					double j = aABB2.maxX;
					double k = aABB2.maxY;
					double m = aABB2.maxZ;
					float n = 1.0F;
					float o = 0.0F;
					float p = 0.0F;
					float q = 0.5F;
					if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.WEST)) {
						Tesselator tesselator = Tesselator.getInstance();
						BufferBuilder bufferBuilder = tesselator.getBuilder();
						bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);
						bufferBuilder.vertex(g, h, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, h, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, k, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, k, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						tesselator.end();
					}

					if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.SOUTH)) {
						Tesselator tesselator = Tesselator.getInstance();
						BufferBuilder bufferBuilder = tesselator.getBuilder();
						bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);
						bufferBuilder.vertex(g, k, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, h, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, k, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, h, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						tesselator.end();
					}

					if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.EAST)) {
						Tesselator tesselator = Tesselator.getInstance();
						BufferBuilder bufferBuilder = tesselator.getBuilder();
						bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);
						bufferBuilder.vertex(j, h, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, h, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, k, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, k, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						tesselator.end();
					}

					if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.NORTH)) {
						Tesselator tesselator = Tesselator.getInstance();
						BufferBuilder bufferBuilder = tesselator.getBuilder();
						bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);
						bufferBuilder.vertex(j, k, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, h, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, k, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, h, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						tesselator.end();
					}

					if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.DOWN)) {
						Tesselator tesselator = Tesselator.getInstance();
						BufferBuilder bufferBuilder = tesselator.getBuilder();
						bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);
						bufferBuilder.vertex(g, h, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, h, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, h, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, h, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						tesselator.end();
					}

					if (blockState.isFaceSturdy(blockGetter, blockPos2, Direction.UP)) {
						Tesselator tesselator = Tesselator.getInstance();
						BufferBuilder bufferBuilder = tesselator.getBuilder();
						bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);
						bufferBuilder.vertex(g, k, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(g, k, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, k, i).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						bufferBuilder.vertex(j, k, m).color(1.0F, 0.0F, 0.0F, 0.5F).endVertex();
						tesselator.end();
					}
				}
			}
		}

		GlStateManager.depthMask(true);
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
	}
}
