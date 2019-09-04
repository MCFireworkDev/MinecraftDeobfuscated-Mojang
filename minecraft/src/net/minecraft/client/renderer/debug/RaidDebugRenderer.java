package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
	private final Minecraft minecraft;
	private Collection<BlockPos> raidCenters = Lists.<BlockPos>newArrayList();

	public RaidDebugRenderer(Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	public void setRaidCenters(Collection<BlockPos> collection) {
		this.raidCenters = collection;
	}

	@Override
	public void render(long l) {
		RenderSystem.pushMatrix();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
			GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
		);
		RenderSystem.disableTexture();
		this.doRender();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		RenderSystem.popMatrix();
	}

	private void doRender() {
		BlockPos blockPos = this.getCamera().getBlockPosition();

		for(BlockPos blockPos2 : this.raidCenters) {
			if (blockPos.closerThan(blockPos2, 160.0)) {
				highlightRaidCenter(blockPos2);
			}
		}
	}

	private static void highlightRaidCenter(BlockPos blockPos) {
		DebugRenderer.renderFilledBox(blockPos.offset(-0.5, -0.5, -0.5), blockPos.offset(1.5, 1.5, 1.5), 1.0F, 0.0F, 0.0F, 0.15F);
		int i = -65536;
		renderTextOverBlock("Raid center", blockPos, -65536);
	}

	private static void renderTextOverBlock(String string, BlockPos blockPos, int i) {
		double d = (double)blockPos.getX() + 0.5;
		double e = (double)blockPos.getY() + 1.3;
		double f = (double)blockPos.getZ() + 0.5;
		DebugRenderer.renderFloatingText(string, d, e, f, i, 0.04F, true, 0.0F, true);
	}

	private Camera getCamera() {
		return this.minecraft.gameRenderer.getMainCamera();
	}
}
