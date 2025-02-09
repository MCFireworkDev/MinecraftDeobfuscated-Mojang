package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SwimNodeEvaluator extends NodeEvaluator {
	private final boolean allowBreaching;
	private final Long2ObjectMap<BlockPathTypes> pathTypesByPosCache = new Long2ObjectOpenHashMap();

	public SwimNodeEvaluator(boolean bl) {
		this.allowBreaching = bl;
	}

	@Override
	public void prepare(PathNavigationRegion pathNavigationRegion, Mob mob) {
		super.prepare(pathNavigationRegion, mob);
		this.pathTypesByPosCache.clear();
	}

	@Override
	public void done() {
		super.done();
		this.pathTypesByPosCache.clear();
	}

	@Override
	public Node getStart() {
		return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ));
	}

	@Override
	public Target getGoal(double d, double e, double f) {
		return this.getTargetFromNode(this.getNode(Mth.floor(d), Mth.floor(e), Mth.floor(f)));
	}

	@Override
	public int getNeighbors(Node[] nodes, Node node) {
		int i = 0;
		Map<Direction, Node> map = Maps.newEnumMap(Direction.class);

		for(Direction direction : Direction.values()) {
			Node node2 = this.findAcceptedNode(node.x + direction.getStepX(), node.y + direction.getStepY(), node.z + direction.getStepZ());
			map.put(direction, node2);
			if (this.isNodeValid(node2)) {
				nodes[i++] = node2;
			}
		}

		for(Direction direction2 : Direction.Plane.HORIZONTAL) {
			Direction direction3 = direction2.getClockWise();
			Node node3 = this.findAcceptedNode(node.x + direction2.getStepX() + direction3.getStepX(), node.y, node.z + direction2.getStepZ() + direction3.getStepZ());
			if (this.isDiagonalNodeValid(node3, (Node)map.get(direction2), (Node)map.get(direction3))) {
				nodes[i++] = node3;
			}
		}

		return i;
	}

	protected boolean isNodeValid(@Nullable Node node) {
		return node != null && !node.closed;
	}

	protected boolean isDiagonalNodeValid(@Nullable Node node, @Nullable Node node2, @Nullable Node node3) {
		return this.isNodeValid(node) && node2 != null && node2.costMalus >= 0.0F && node3 != null && node3.costMalus >= 0.0F;
	}

	@Nullable
	protected Node findAcceptedNode(int i, int j, int k) {
		Node node = null;
		BlockPathTypes blockPathTypes = this.getCachedBlockType(i, j, k);
		if (this.allowBreaching && blockPathTypes == BlockPathTypes.BREACH || blockPathTypes == BlockPathTypes.WATER) {
			float f = this.mob.getPathfindingMalus(blockPathTypes);
			if (f >= 0.0F) {
				node = this.getNode(i, j, k);
				node.type = blockPathTypes;
				node.costMalus = Math.max(node.costMalus, f);
				if (this.level.getFluidState(new BlockPos(i, j, k)).isEmpty()) {
					node.costMalus += 8.0F;
				}
			}
		}

		return node;
	}

	protected BlockPathTypes getCachedBlockType(int i, int j, int k) {
		return (BlockPathTypes)this.pathTypesByPosCache
			.computeIfAbsent(BlockPos.asLong(i, j, k), (Long2ObjectFunction)(l -> this.getBlockPathType(this.level, i, j, k)));
	}

	@Override
	public BlockPathTypes getBlockPathType(BlockGetter blockGetter, int i, int j, int k) {
		return this.getBlockPathType(blockGetter, i, j, k, this.mob);
	}

	@Override
	public BlockPathTypes getBlockPathType(BlockGetter blockGetter, int i, int j, int k, Mob mob) {
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

		for(int l = i; l < i + this.entityWidth; ++l) {
			for(int m = j; m < j + this.entityHeight; ++m) {
				for(int n = k; n < k + this.entityDepth; ++n) {
					FluidState fluidState = blockGetter.getFluidState(mutableBlockPos.set(l, m, n));
					BlockState blockState = blockGetter.getBlockState(mutableBlockPos.set(l, m, n));
					if (fluidState.isEmpty() && blockState.isPathfindable(blockGetter, mutableBlockPos.below(), PathComputationType.WATER) && blockState.isAir()) {
						return BlockPathTypes.BREACH;
					}

					if (!fluidState.is(FluidTags.WATER)) {
						return BlockPathTypes.BLOCKED;
					}
				}
			}
		}

		BlockState blockState2 = blockGetter.getBlockState(mutableBlockPos);
		return blockState2.isPathfindable(blockGetter, mutableBlockPos, PathComputationType.WATER) ? BlockPathTypes.WATER : BlockPathTypes.BLOCKED;
	}
}
