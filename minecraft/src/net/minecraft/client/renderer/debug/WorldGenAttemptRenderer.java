package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class WorldGenAttemptRenderer implements DebugRenderer.SimpleDebugRenderer {
	private final Minecraft minecraft;
	private final List<BlockPos> toRender = Lists.<BlockPos>newArrayList();
	private final List<Float> scales = Lists.newArrayList();
	private final List<Float> alphas = Lists.newArrayList();
	private final List<Float> reds = Lists.newArrayList();
	private final List<Float> greens = Lists.newArrayList();
	private final List<Float> blues = Lists.newArrayList();

	public WorldGenAttemptRenderer(Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	public void addPos(BlockPos blockPos, float f, float g, float h, float i, float j) {
		this.toRender.add(blockPos);
		this.scales.add(f);
		this.alphas.add(j);
		this.reds.add(g);
		this.greens.add(h);
		this.blues.add(i);
	}
}
