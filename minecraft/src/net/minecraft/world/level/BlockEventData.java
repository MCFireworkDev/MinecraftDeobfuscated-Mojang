package net.minecraft.world.level;

import java.lang.runtime.ObjectMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public final class BlockEventData extends Record {
	private final BlockPos pos;
	private final Block block;
	private final int paramA;
	private final int paramB;

	public BlockEventData(BlockPos blockPos, Block block, int i, int j) {
		this.pos = blockPos;
		this.block = block;
		this.paramA = i;
		this.paramB = j;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",BlockEventData,"pos;block;paramA;paramB",BlockEventData::pos,BlockEventData::block,BlockEventData::paramA,BlockEventData::paramB>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",BlockEventData,"pos;block;paramA;paramB",BlockEventData::pos,BlockEventData::block,BlockEventData::paramA,BlockEventData::paramB>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",BlockEventData,"pos;block;paramA;paramB",BlockEventData::pos,BlockEventData::block,BlockEventData::paramA,BlockEventData::paramB>(
			this, object
		);
	}

	public BlockPos pos() {
		return this.pos;
	}

	public Block block() {
		return this.block;
	}

	public int paramA() {
		return this.paramA;
	}

	public int paramB() {
		return this.paramB;
	}
}
