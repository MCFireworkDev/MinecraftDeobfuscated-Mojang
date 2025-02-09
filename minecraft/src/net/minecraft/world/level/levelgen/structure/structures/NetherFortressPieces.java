package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class NetherFortressPieces {
	private static final int MAX_DEPTH = 30;
	private static final int LOWEST_Y_POSITION = 10;
	public static final int MAGIC_START_Y = 64;
	static final NetherFortressPieces.PieceWeight[] BRIDGE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeStraight.class, 30, 0, true),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeCrossing.class, 10, 4),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.RoomCrossing.class, 10, 4),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.StairsRoom.class, 10, 3),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.MonsterThrone.class, 5, 2),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleEntrance.class, 5, 1)
	};
	static final NetherFortressPieces.PieceWeight[] CASTLE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorPiece.class, 25, 0, true),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorCrossingPiece.class, 15, 5),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class, 5, 10),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class, 5, 10),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorStairsPiece.class, 10, 3, true),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorTBalconyPiece.class, 7, 2),
		new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleStalkRoom.class, 5, 2)
	};

	static NetherFortressPieces.NetherBridgePiece findAndCreateBridgePieceFactory(
		NetherFortressPieces.PieceWeight pieceWeight,
		StructurePieceAccessor structurePieceAccessor,
		RandomSource randomSource,
		int i,
		int j,
		int k,
		Direction direction,
		int l
	) {
		Class<? extends NetherFortressPieces.NetherBridgePiece> class_ = pieceWeight.pieceClass;
		NetherFortressPieces.NetherBridgePiece netherBridgePiece = null;
		if (class_ == NetherFortressPieces.BridgeStraight.class) {
			netherBridgePiece = NetherFortressPieces.BridgeStraight.createPiece(structurePieceAccessor, randomSource, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.BridgeCrossing.class) {
			netherBridgePiece = NetherFortressPieces.BridgeCrossing.createPiece(structurePieceAccessor, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.RoomCrossing.class) {
			netherBridgePiece = NetherFortressPieces.RoomCrossing.createPiece(structurePieceAccessor, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.StairsRoom.class) {
			netherBridgePiece = NetherFortressPieces.StairsRoom.createPiece(structurePieceAccessor, i, j, k, l, direction);
		} else if (class_ == NetherFortressPieces.MonsterThrone.class) {
			netherBridgePiece = NetherFortressPieces.MonsterThrone.createPiece(structurePieceAccessor, i, j, k, l, direction);
		} else if (class_ == NetherFortressPieces.CastleEntrance.class) {
			netherBridgePiece = NetherFortressPieces.CastleEntrance.createPiece(structurePieceAccessor, randomSource, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.CastleSmallCorridorPiece.class) {
			netherBridgePiece = NetherFortressPieces.CastleSmallCorridorPiece.createPiece(structurePieceAccessor, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class) {
			netherBridgePiece = NetherFortressPieces.CastleSmallCorridorRightTurnPiece.createPiece(structurePieceAccessor, randomSource, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class) {
			netherBridgePiece = NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.createPiece(structurePieceAccessor, randomSource, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.CastleCorridorStairsPiece.class) {
			netherBridgePiece = NetherFortressPieces.CastleCorridorStairsPiece.createPiece(structurePieceAccessor, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.CastleCorridorTBalconyPiece.class) {
			netherBridgePiece = NetherFortressPieces.CastleCorridorTBalconyPiece.createPiece(structurePieceAccessor, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.CastleSmallCorridorCrossingPiece.class) {
			netherBridgePiece = NetherFortressPieces.CastleSmallCorridorCrossingPiece.createPiece(structurePieceAccessor, i, j, k, direction, l);
		} else if (class_ == NetherFortressPieces.CastleStalkRoom.class) {
			netherBridgePiece = NetherFortressPieces.CastleStalkRoom.createPiece(structurePieceAccessor, i, j, k, direction, l);
		}

		return netherBridgePiece;
	}

	public static class BridgeCrossing extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 19;
		private static final int HEIGHT = 10;
		private static final int DEPTH = 19;

		public BridgeCrossing(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, i, boundingBox);
			this.setOrientation(direction);
		}

		protected BridgeCrossing(int i, int j, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(i, 64, j, direction, 19, 10, 19));
			this.setOrientation(direction);
		}

		protected BridgeCrossing(StructurePieceType structurePieceType, CompoundTag compoundTag) {
			super(structurePieceType, compoundTag);
		}

		public BridgeCrossing(CompoundTag compoundTag) {
			this(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 8, 3, false);
			this.generateChildLeft((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 3, 8, false);
			this.generateChildRight((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 3, 8, false);
		}

		public static NetherFortressPieces.BridgeCrossing createPiece(StructurePieceAccessor structurePieceAccessor, int i, int j, int k, Direction direction, int l) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -8, -3, 0, 19, 10, 19, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.BridgeCrossing(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 8, 5, 0, 10, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 8, 18, 7, 10, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			this.generateBox(worldGenLevel, boundingBox, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 7; i <= 11; ++i) {
				for(int j = 0; j <= 2; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, boundingBox);
				}
			}

			this.generateBox(worldGenLevel, boundingBox, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 0; i <= 2; ++i) {
				for(int j = 7; j <= 11; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 18 - i, -1, j, boundingBox);
				}
			}
		}
	}

	public static class BridgeEndFiller extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 5;
		private static final int HEIGHT = 10;
		private static final int DEPTH = 8;
		private final int selfSeed;

		public BridgeEndFiller(int i, RandomSource randomSource, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, i, boundingBox);
			this.setOrientation(direction);
			this.selfSeed = randomSource.nextInt();
		}

		public BridgeEndFiller(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, compoundTag);
			this.selfSeed = compoundTag.getInt("Seed");
		}

		public static NetherFortressPieces.BridgeEndFiller createPiece(
			StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -1, -3, 0, 5, 10, 8, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.BridgeEndFiller(l, randomSource, boundingBox, direction)
				: null;
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {
			super.addAdditionalSaveData(structurePieceSerializationContext, compoundTag);
			compoundTag.putInt("Seed", this.selfSeed);
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			RandomSource randomSource2 = RandomSource.create((long)this.selfSeed);

			for(int i = 0; i <= 4; ++i) {
				for(int j = 3; j <= 4; ++j) {
					int k = randomSource2.nextInt(8);
					this.generateBox(worldGenLevel, boundingBox, i, j, 0, i, j, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
				}
			}

			int i = randomSource2.nextInt(8);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 0, 0, 5, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			i = randomSource2.nextInt(8);
			this.generateBox(worldGenLevel, boundingBox, 4, 5, 0, 4, 5, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int ix = 0; ix <= 4; ++ix) {
				int j = randomSource2.nextInt(5);
				this.generateBox(worldGenLevel, boundingBox, ix, 2, 0, ix, 2, j, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			}

			for(int ix = 0; ix <= 4; ++ix) {
				for(int j = 0; j <= 1; ++j) {
					int k = randomSource2.nextInt(3);
					this.generateBox(
						worldGenLevel, boundingBox, ix, j, 0, ix, j, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
					);
				}
			}
		}
	}

	public static class BridgeStraight extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 5;
		private static final int HEIGHT = 10;
		private static final int DEPTH = 19;

		public BridgeStraight(int i, RandomSource randomSource, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, i, boundingBox);
			this.setOrientation(direction);
		}

		public BridgeStraight(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 1, 3, false);
		}

		public static NetherFortressPieces.BridgeStraight createPiece(
			StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -1, -3, 0, 5, 10, 19, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.BridgeStraight(l, randomSource, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 5, 0, 3, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 0; i <= 4; ++i) {
				for(int j = 0; j <= 2; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, boundingBox);
				}
			}

			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			BlockState blockState2 = blockState.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState3 = blockState.setValue(FenceBlock.WEST, Boolean.valueOf(true));
			this.generateBox(worldGenLevel, boundingBox, 0, 1, 1, 0, 4, 1, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 4, 0, 4, 4, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 14, 0, 4, 14, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 1, 17, 0, 4, 17, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 1, 1, 4, 4, 1, blockState3, blockState3, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 3, 4, 4, 4, 4, blockState3, blockState3, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 3, 14, 4, 4, 14, blockState3, blockState3, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 1, 17, 4, 4, 17, blockState3, blockState3, false);
		}
	}

	public static class CastleCorridorStairsPiece extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 5;
		private static final int HEIGHT = 14;
		private static final int DEPTH = 10;

		public CastleCorridorStairsPiece(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, i, boundingBox);
			this.setOrientation(direction);
		}

		public CastleCorridorStairsPiece(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 1, 0, true);
		}

		public static NetherFortressPieces.CastleCorridorStairsPiece createPiece(
			StructurePieceAccessor structurePieceAccessor, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -1, -7, 0, 5, 14, 10, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleCorridorStairsPiece(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			BlockState blockState = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));

			for(int i = 0; i <= 9; ++i) {
				int j = Math.max(1, 7 - i);
				int k = Math.min(Math.max(j + 5, 14 - i), 13);
				int l = i;
				this.generateBox(worldGenLevel, boundingBox, 0, 0, i, 4, j, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
				this.generateBox(worldGenLevel, boundingBox, 1, j + 1, i, 3, k - 1, i, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
				if (i <= 6) {
					this.placeBlock(worldGenLevel, blockState, 1, j + 1, i, boundingBox);
					this.placeBlock(worldGenLevel, blockState, 2, j + 1, i, boundingBox);
					this.placeBlock(worldGenLevel, blockState, 3, j + 1, i, boundingBox);
				}

				this.generateBox(worldGenLevel, boundingBox, 0, k, i, 4, k, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
				this.generateBox(
					worldGenLevel, boundingBox, 0, j + 1, i, 0, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
				);
				this.generateBox(
					worldGenLevel, boundingBox, 4, j + 1, i, 4, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
				);
				if ((i & 1) == 0) {
					this.generateBox(worldGenLevel, boundingBox, 0, j + 2, i, 0, j + 3, i, blockState2, blockState2, false);
					this.generateBox(worldGenLevel, boundingBox, 4, j + 2, i, 4, j + 3, i, blockState2, blockState2, false);
				}

				for(int m = 0; m <= 4; ++m) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), m, -1, l, boundingBox);
				}
			}
		}
	}

	public static class CastleCorridorTBalconyPiece extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 9;
		private static final int HEIGHT = 7;
		private static final int DEPTH = 9;

		public CastleCorridorTBalconyPiece(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, i, boundingBox);
			this.setOrientation(direction);
		}

		public CastleCorridorTBalconyPiece(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			int i = 1;
			Direction direction = this.getOrientation();
			if (direction == Direction.WEST || direction == Direction.NORTH) {
				i = 5;
			}

			this.generateChildLeft((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, i, randomSource.nextInt(8) > 0);
			this.generateChildRight((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, i, randomSource.nextInt(8) > 0);
		}

		public static NetherFortressPieces.CastleCorridorTBalconyPiece createPiece(
			StructurePieceAccessor structurePieceAccessor, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -3, 0, 0, 9, 7, 9, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleCorridorTBalconyPiece(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 8, 5, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 3, 0, 1, 4, 0, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 7, 3, 0, 7, 4, 0, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 1, 4, 2, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 1, 4, 7, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 3, 8, 7, 3, 8, blockState2, blockState2, false);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
				0,
				3,
				8,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
				8,
				3,
				8,
				boundingBox
			);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 6, 0, 3, 7, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 8, 3, 6, 8, 3, 7, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 4, 5, 1, 5, 5, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 7, 4, 5, 7, 5, 5, blockState2, blockState2, false);

			for(int i = 0; i <= 5; ++i) {
				for(int j = 0; j <= 8; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), j, -1, i, boundingBox);
				}
			}
		}
	}

	public static class CastleEntrance extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 13;
		private static final int HEIGHT = 14;
		private static final int DEPTH = 13;

		public CastleEntrance(int i, RandomSource randomSource, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, i, boundingBox);
			this.setOrientation(direction);
		}

		public CastleEntrance(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 5, 3, true);
		}

		public static NetherFortressPieces.CastleEntrance createPiece(
			StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -5, -3, 0, 13, 14, 13, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleEntrance(l, randomSource, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			this.generateBox(worldGenLevel, boundingBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			this.generateBox(
				worldGenLevel, boundingBox, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.defaultBlockState(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), false
			);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));

			for(int i = 1; i <= 11; i += 2) {
				this.generateBox(worldGenLevel, boundingBox, i, 10, 0, i, 11, 0, blockState, blockState, false);
				this.generateBox(worldGenLevel, boundingBox, i, 10, 12, i, 11, 12, blockState, blockState, false);
				this.generateBox(worldGenLevel, boundingBox, 0, 10, i, 0, 11, i, blockState2, blockState2, false);
				this.generateBox(worldGenLevel, boundingBox, 12, 10, i, 12, 11, i, blockState2, blockState2, false);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, boundingBox);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, boundingBox);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, boundingBox);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, boundingBox);
				if (i != 11) {
					this.placeBlock(worldGenLevel, blockState, i + 1, 13, 0, boundingBox);
					this.placeBlock(worldGenLevel, blockState, i + 1, 13, 12, boundingBox);
					this.placeBlock(worldGenLevel, blockState2, 0, 13, i + 1, boundingBox);
					this.placeBlock(worldGenLevel, blockState2, 12, 13, i + 1, boundingBox);
				}
			}

			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
				0,
				13,
				0,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
				0,
				13,
				12,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
				12,
				13,
				12,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
				12,
				13,
				0,
				boundingBox
			);

			for(int i = 3; i <= 9; i += 2) {
				this.generateBox(
					worldGenLevel,
					boundingBox,
					1,
					7,
					i,
					1,
					8,
					i,
					blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true)),
					blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true)),
					false
				);
				this.generateBox(
					worldGenLevel,
					boundingBox,
					11,
					7,
					i,
					11,
					8,
					i,
					blockState2.setValue(FenceBlock.EAST, Boolean.valueOf(true)),
					blockState2.setValue(FenceBlock.EAST, Boolean.valueOf(true)),
					false
				);
			}

			this.generateBox(worldGenLevel, boundingBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 4; i <= 8; ++i) {
				for(int j = 0; j <= 2; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 12 - j, boundingBox);
				}
			}

			for(int i = 0; i <= 2; ++i) {
				for(int j = 4; j <= 8; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - i, -1, j, boundingBox);
				}
			}

			this.generateBox(worldGenLevel, boundingBox, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 1, 6, 6, 4, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 6, 0, 6, boundingBox);
			this.placeBlock(worldGenLevel, Blocks.LAVA.defaultBlockState(), 6, 5, 6, boundingBox);
			BlockPos blockPos2 = this.getWorldPos(6, 5, 6);
			if (boundingBox.isInside(blockPos2)) {
				worldGenLevel.scheduleTick(blockPos2, Fluids.LAVA, 0);
			}
		}
	}

	public static class CastleSmallCorridorCrossingPiece extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 5;
		private static final int HEIGHT = 7;
		private static final int DEPTH = 5;

		public CastleSmallCorridorCrossingPiece(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, i, boundingBox);
			this.setOrientation(direction);
		}

		public CastleSmallCorridorCrossingPiece(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 1, 0, true);
			this.generateChildLeft((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, 1, true);
			this.generateChildRight((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, 1, true);
		}

		public static NetherFortressPieces.CastleSmallCorridorCrossingPiece createPiece(
			StructurePieceAccessor structurePieceAccessor, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -1, 0, 0, 5, 7, 5, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleSmallCorridorCrossingPiece(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 0; i <= 4; ++i) {
				for(int j = 0; j <= 4; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
				}
			}
		}
	}

	public static class CastleSmallCorridorLeftTurnPiece extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 5;
		private static final int HEIGHT = 7;
		private static final int DEPTH = 5;
		private boolean isNeedingChest;

		public CastleSmallCorridorLeftTurnPiece(int i, RandomSource randomSource, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, i, boundingBox);
			this.setOrientation(direction);
			this.isNeedingChest = randomSource.nextInt(3) == 0;
		}

		public CastleSmallCorridorLeftTurnPiece(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, compoundTag);
			this.isNeedingChest = compoundTag.getBoolean("Chest");
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {
			super.addAdditionalSaveData(structurePieceSerializationContext, compoundTag);
			compoundTag.putBoolean("Chest", this.isNeedingChest);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildLeft((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, 1, true);
		}

		public static NetherFortressPieces.CastleSmallCorridorLeftTurnPiece createPiece(
			StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -1, 0, 0, 5, 7, 5, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleSmallCorridorLeftTurnPiece(l, randomSource, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			this.generateBox(worldGenLevel, boundingBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 3, 1, 4, 4, 1, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 3, 3, 4, 4, 3, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 3, 4, 1, 4, 4, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 3, 3, 4, 3, 4, 4, blockState, blockState, false);
			if (this.isNeedingChest && boundingBox.isInside(this.getWorldPos(3, 2, 3))) {
				this.isNeedingChest = false;
				this.createChest(worldGenLevel, boundingBox, randomSource, 3, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
			}

			this.generateBox(worldGenLevel, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 0; i <= 4; ++i) {
				for(int j = 0; j <= 4; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
				}
			}
		}
	}

	public static class CastleSmallCorridorPiece extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 5;
		private static final int HEIGHT = 7;
		private static final int DEPTH = 5;

		public CastleSmallCorridorPiece(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, i, boundingBox);
			this.setOrientation(direction);
		}

		public CastleSmallCorridorPiece(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 1, 0, true);
		}

		public static NetherFortressPieces.CastleSmallCorridorPiece createPiece(
			StructurePieceAccessor structurePieceAccessor, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -1, 0, 0, 5, 7, 5, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleSmallCorridorPiece(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 1, 0, 4, 1, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 3, 0, 4, 3, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 3, 1, 4, 4, 1, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 3, 3, 4, 4, 3, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 0; i <= 4; ++i) {
				for(int j = 0; j <= 4; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
				}
			}
		}
	}

	public static class CastleSmallCorridorRightTurnPiece extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 5;
		private static final int HEIGHT = 7;
		private static final int DEPTH = 5;
		private boolean isNeedingChest;

		public CastleSmallCorridorRightTurnPiece(int i, RandomSource randomSource, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, i, boundingBox);
			this.setOrientation(direction);
			this.isNeedingChest = randomSource.nextInt(3) == 0;
		}

		public CastleSmallCorridorRightTurnPiece(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, compoundTag);
			this.isNeedingChest = compoundTag.getBoolean("Chest");
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {
			super.addAdditionalSaveData(structurePieceSerializationContext, compoundTag);
			compoundTag.putBoolean("Chest", this.isNeedingChest);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildRight((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, 1, true);
		}

		public static NetherFortressPieces.CastleSmallCorridorRightTurnPiece createPiece(
			StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -1, 0, 0, 5, 7, 5, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleSmallCorridorRightTurnPiece(l, randomSource, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 1, 0, 4, 1, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 3, 0, 4, 3, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 3, 4, 1, 4, 4, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 3, 3, 4, 3, 4, 4, blockState, blockState, false);
			if (this.isNeedingChest && boundingBox.isInside(this.getWorldPos(1, 2, 3))) {
				this.isNeedingChest = false;
				this.createChest(worldGenLevel, boundingBox, randomSource, 1, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
			}

			this.generateBox(worldGenLevel, boundingBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int i = 0; i <= 4; ++i) {
				for(int j = 0; j <= 4; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
				}
			}
		}
	}

	public static class CastleStalkRoom extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 13;
		private static final int HEIGHT = 14;
		private static final int DEPTH = 13;

		public CastleStalkRoom(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, i, boundingBox);
			this.setOrientation(direction);
		}

		public CastleStalkRoom(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 5, 3, true);
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 5, 11, true);
		}

		public static NetherFortressPieces.CastleStalkRoom createPiece(
			StructurePieceAccessor structurePieceAccessor, int i, int j, int k, Direction direction, int l
		) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -5, -3, 0, 13, 14, 13, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.CastleStalkRoom(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			this.generateBox(worldGenLevel, boundingBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(
				worldGenLevel, boundingBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
			);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			BlockState blockState3 = blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true));
			BlockState blockState4 = blockState2.setValue(FenceBlock.EAST, Boolean.valueOf(true));

			for(int i = 1; i <= 11; i += 2) {
				this.generateBox(worldGenLevel, boundingBox, i, 10, 0, i, 11, 0, blockState, blockState, false);
				this.generateBox(worldGenLevel, boundingBox, i, 10, 12, i, 11, 12, blockState, blockState, false);
				this.generateBox(worldGenLevel, boundingBox, 0, 10, i, 0, 11, i, blockState2, blockState2, false);
				this.generateBox(worldGenLevel, boundingBox, 12, 10, i, 12, 11, i, blockState2, blockState2, false);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, boundingBox);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, boundingBox);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, boundingBox);
				this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, boundingBox);
				if (i != 11) {
					this.placeBlock(worldGenLevel, blockState, i + 1, 13, 0, boundingBox);
					this.placeBlock(worldGenLevel, blockState, i + 1, 13, 12, boundingBox);
					this.placeBlock(worldGenLevel, blockState2, 0, 13, i + 1, boundingBox);
					this.placeBlock(worldGenLevel, blockState2, 12, 13, i + 1, boundingBox);
				}
			}

			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
				0,
				13,
				0,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
				0,
				13,
				12,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
				12,
				13,
				12,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
				12,
				13,
				0,
				boundingBox
			);

			for(int i = 3; i <= 9; i += 2) {
				this.generateBox(worldGenLevel, boundingBox, 1, 7, i, 1, 8, i, blockState3, blockState3, false);
				this.generateBox(worldGenLevel, boundingBox, 11, 7, i, 11, 8, i, blockState4, blockState4, false);
			}

			BlockState blockState5 = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);

			for(int j = 0; j <= 6; ++j) {
				int k = j + 4;

				for(int l = 5; l <= 7; ++l) {
					this.placeBlock(worldGenLevel, blockState5, l, 5 + j, k, boundingBox);
				}

				if (k >= 5 && k <= 8) {
					this.generateBox(
						worldGenLevel, boundingBox, 5, 5, k, 7, j + 4, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
					);
				} else if (k >= 9 && k <= 10) {
					this.generateBox(
						worldGenLevel, boundingBox, 5, 8, k, 7, j + 4, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
					);
				}

				if (j >= 1) {
					this.generateBox(worldGenLevel, boundingBox, 5, 6 + j, k, 7, 9 + j, k, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
				}
			}

			for(int j = 5; j <= 7; ++j) {
				this.placeBlock(worldGenLevel, blockState5, j, 12, 11, boundingBox);
			}

			this.generateBox(worldGenLevel, boundingBox, 5, 6, 7, 5, 7, 7, blockState4, blockState4, false);
			this.generateBox(worldGenLevel, boundingBox, 7, 6, 7, 7, 7, 7, blockState3, blockState3, false);
			this.generateBox(worldGenLevel, boundingBox, 5, 13, 12, 7, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			BlockState blockState6 = blockState5.setValue(StairBlock.FACING, Direction.EAST);
			BlockState blockState7 = blockState5.setValue(StairBlock.FACING, Direction.WEST);
			this.placeBlock(worldGenLevel, blockState7, 4, 5, 2, boundingBox);
			this.placeBlock(worldGenLevel, blockState7, 4, 5, 3, boundingBox);
			this.placeBlock(worldGenLevel, blockState7, 4, 5, 9, boundingBox);
			this.placeBlock(worldGenLevel, blockState7, 4, 5, 10, boundingBox);
			this.placeBlock(worldGenLevel, blockState6, 8, 5, 2, boundingBox);
			this.placeBlock(worldGenLevel, blockState6, 8, 5, 3, boundingBox);
			this.placeBlock(worldGenLevel, blockState6, 8, 5, 9, boundingBox);
			this.placeBlock(worldGenLevel, blockState6, 8, 5, 10, boundingBox);
			this.generateBox(worldGenLevel, boundingBox, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

			for(int l = 4; l <= 8; ++l) {
				for(int m = 0; m <= 2; ++m) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), l, -1, m, boundingBox);
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), l, -1, 12 - m, boundingBox);
				}
			}

			for(int l = 0; l <= 2; ++l) {
				for(int m = 4; m <= 8; ++m) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), l, -1, m, boundingBox);
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - l, -1, m, boundingBox);
				}
			}
		}
	}

	public static class MonsterThrone extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 7;
		private static final int HEIGHT = 8;
		private static final int DEPTH = 9;
		private boolean hasPlacedSpawner;

		public MonsterThrone(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, i, boundingBox);
			this.setOrientation(direction);
		}

		public MonsterThrone(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, compoundTag);
			this.hasPlacedSpawner = compoundTag.getBoolean("Mob");
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {
			super.addAdditionalSaveData(structurePieceSerializationContext, compoundTag);
			compoundTag.putBoolean("Mob", this.hasPlacedSpawner);
		}

		public static NetherFortressPieces.MonsterThrone createPiece(StructurePieceAccessor structurePieceAccessor, int i, int j, int k, int l, Direction direction) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -2, 0, 0, 7, 8, 9, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.MonsterThrone(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 6, 7, 7, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 1, 6, 3, boundingBox);
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 5, 6, 3, boundingBox);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.NORTH, Boolean.valueOf(true)),
				0,
				6,
				3,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.NORTH, Boolean.valueOf(true)),
				6,
				6,
				3,
				boundingBox
			);
			this.generateBox(worldGenLevel, boundingBox, 0, 6, 4, 0, 6, 7, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 6, 6, 4, 6, 6, 7, blockState2, blockState2, false);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
				0,
				6,
				8,
				boundingBox
			);
			this.placeBlock(
				worldGenLevel,
				Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
				6,
				6,
				8,
				boundingBox
			);
			this.generateBox(worldGenLevel, boundingBox, 1, 6, 8, 5, 6, 8, blockState, blockState, false);
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 1, 7, 8, boundingBox);
			this.generateBox(worldGenLevel, boundingBox, 2, 7, 8, 4, 7, 8, blockState, blockState, false);
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 5, 7, 8, boundingBox);
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 2, 8, 8, boundingBox);
			this.placeBlock(worldGenLevel, blockState, 3, 8, 8, boundingBox);
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 4, 8, 8, boundingBox);
			if (!this.hasPlacedSpawner) {
				BlockPos blockPos2 = this.getWorldPos(3, 5, 5);
				if (boundingBox.isInside(blockPos2)) {
					this.hasPlacedSpawner = true;
					worldGenLevel.setBlock(blockPos2, Blocks.SPAWNER.defaultBlockState(), 2);
					BlockEntity blockEntity = worldGenLevel.getBlockEntity(blockPos2);
					if (blockEntity instanceof SpawnerBlockEntity spawnerBlockEntity) {
						spawnerBlockEntity.setEntityId(EntityType.BLAZE, randomSource);
					}
				}
			}

			for(int i = 0; i <= 6; ++i) {
				for(int j = 0; j <= 6; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
				}
			}
		}
	}

	abstract static class NetherBridgePiece extends StructurePiece {
		protected NetherBridgePiece(StructurePieceType structurePieceType, int i, BoundingBox boundingBox) {
			super(structurePieceType, i, boundingBox);
		}

		public NetherBridgePiece(StructurePieceType structurePieceType, CompoundTag compoundTag) {
			super(structurePieceType, compoundTag);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {
		}

		private int updatePieceWeight(List<NetherFortressPieces.PieceWeight> list) {
			boolean bl = false;
			int i = 0;

			for(NetherFortressPieces.PieceWeight pieceWeight : list) {
				if (pieceWeight.maxPlaceCount > 0 && pieceWeight.placeCount < pieceWeight.maxPlaceCount) {
					bl = true;
				}

				i += pieceWeight.weight;
			}

			return bl ? i : -1;
		}

		private NetherFortressPieces.NetherBridgePiece generatePiece(
			NetherFortressPieces.StartPiece startPiece,
			List<NetherFortressPieces.PieceWeight> list,
			StructurePieceAccessor structurePieceAccessor,
			RandomSource randomSource,
			int i,
			int j,
			int k,
			Direction direction,
			int l
		) {
			int m = this.updatePieceWeight(list);
			boolean bl = m > 0 && l <= 30;
			int n = 0;

			while(n < 5 && bl) {
				++n;
				int o = randomSource.nextInt(m);

				for(NetherFortressPieces.PieceWeight pieceWeight : list) {
					o -= pieceWeight.weight;
					if (o < 0) {
						if (!pieceWeight.doPlace(l) || pieceWeight == startPiece.previousPiece && !pieceWeight.allowInRow) {
							break;
						}

						NetherFortressPieces.NetherBridgePiece netherBridgePiece = NetherFortressPieces.findAndCreateBridgePieceFactory(
							pieceWeight, structurePieceAccessor, randomSource, i, j, k, direction, l
						);
						if (netherBridgePiece != null) {
							++pieceWeight.placeCount;
							startPiece.previousPiece = pieceWeight;
							if (!pieceWeight.isValid()) {
								list.remove(pieceWeight);
							}

							return netherBridgePiece;
						}
					}
				}
			}

			return NetherFortressPieces.BridgeEndFiller.createPiece(structurePieceAccessor, randomSource, i, j, k, direction, l);
		}

		private StructurePiece generateAndAddPiece(
			NetherFortressPieces.StartPiece startPiece,
			StructurePieceAccessor structurePieceAccessor,
			RandomSource randomSource,
			int i,
			int j,
			int k,
			@Nullable Direction direction,
			int l,
			boolean bl
		) {
			if (Math.abs(i - startPiece.getBoundingBox().minX()) <= 112 && Math.abs(k - startPiece.getBoundingBox().minZ()) <= 112) {
				List<NetherFortressPieces.PieceWeight> list = startPiece.availableBridgePieces;
				if (bl) {
					list = startPiece.availableCastlePieces;
				}

				StructurePiece structurePiece = this.generatePiece(startPiece, list, structurePieceAccessor, randomSource, i, j, k, direction, l + 1);
				if (structurePiece != null) {
					structurePieceAccessor.addPiece(structurePiece);
					startPiece.pendingChildren.add(structurePiece);
				}

				return structurePiece;
			} else {
				return NetherFortressPieces.BridgeEndFiller.createPiece(structurePieceAccessor, randomSource, i, j, k, direction, l);
			}
		}

		@Nullable
		protected StructurePiece generateChildForward(
			NetherFortressPieces.StartPiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, boolean bl
		) {
			Direction direction = this.getOrientation();
			if (direction != null) {
				switch(direction) {
					case NORTH:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() + i,
							this.boundingBox.minY() + j,
							this.boundingBox.minZ() - 1,
							direction,
							this.getGenDepth(),
							bl
						);
					case SOUTH:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() + i,
							this.boundingBox.minY() + j,
							this.boundingBox.maxZ() + 1,
							direction,
							this.getGenDepth(),
							bl
						);
					case WEST:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() - 1,
							this.boundingBox.minY() + j,
							this.boundingBox.minZ() + i,
							direction,
							this.getGenDepth(),
							bl
						);
					case EAST:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.maxX() + 1,
							this.boundingBox.minY() + j,
							this.boundingBox.minZ() + i,
							direction,
							this.getGenDepth(),
							bl
						);
				}
			}

			return null;
		}

		@Nullable
		protected StructurePiece generateChildLeft(
			NetherFortressPieces.StartPiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, boolean bl
		) {
			Direction direction = this.getOrientation();
			if (direction != null) {
				switch(direction) {
					case NORTH:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() - 1,
							this.boundingBox.minY() + i,
							this.boundingBox.minZ() + j,
							Direction.WEST,
							this.getGenDepth(),
							bl
						);
					case SOUTH:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() - 1,
							this.boundingBox.minY() + i,
							this.boundingBox.minZ() + j,
							Direction.WEST,
							this.getGenDepth(),
							bl
						);
					case WEST:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() + j,
							this.boundingBox.minY() + i,
							this.boundingBox.minZ() - 1,
							Direction.NORTH,
							this.getGenDepth(),
							bl
						);
					case EAST:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() + j,
							this.boundingBox.minY() + i,
							this.boundingBox.minZ() - 1,
							Direction.NORTH,
							this.getGenDepth(),
							bl
						);
				}
			}

			return null;
		}

		@Nullable
		protected StructurePiece generateChildRight(
			NetherFortressPieces.StartPiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource, int i, int j, boolean bl
		) {
			Direction direction = this.getOrientation();
			if (direction != null) {
				switch(direction) {
					case NORTH:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.maxX() + 1,
							this.boundingBox.minY() + i,
							this.boundingBox.minZ() + j,
							Direction.EAST,
							this.getGenDepth(),
							bl
						);
					case SOUTH:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.maxX() + 1,
							this.boundingBox.minY() + i,
							this.boundingBox.minZ() + j,
							Direction.EAST,
							this.getGenDepth(),
							bl
						);
					case WEST:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() + j,
							this.boundingBox.minY() + i,
							this.boundingBox.maxZ() + 1,
							Direction.SOUTH,
							this.getGenDepth(),
							bl
						);
					case EAST:
						return this.generateAndAddPiece(
							startPiece,
							structurePieceAccessor,
							randomSource,
							this.boundingBox.minX() + j,
							this.boundingBox.minY() + i,
							this.boundingBox.maxZ() + 1,
							Direction.SOUTH,
							this.getGenDepth(),
							bl
						);
				}
			}

			return null;
		}

		protected static boolean isOkBox(BoundingBox boundingBox) {
			return boundingBox != null && boundingBox.minY() > 10;
		}
	}

	static class PieceWeight {
		public final Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass;
		public final int weight;
		public int placeCount;
		public final int maxPlaceCount;
		public final boolean allowInRow;

		public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> class_, int i, int j, boolean bl) {
			this.pieceClass = class_;
			this.weight = i;
			this.maxPlaceCount = j;
			this.allowInRow = bl;
		}

		public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> class_, int i, int j) {
			this(class_, i, j, false);
		}

		public boolean doPlace(int i) {
			return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
		}

		public boolean isValid() {
			return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
		}
	}

	public static class RoomCrossing extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 7;
		private static final int HEIGHT = 9;
		private static final int DEPTH = 7;

		public RoomCrossing(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, i, boundingBox);
			this.setOrientation(direction);
		}

		public RoomCrossing(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildForward((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 2, 0, false);
			this.generateChildLeft((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, 2, false);
			this.generateChildRight((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 0, 2, false);
		}

		public static NetherFortressPieces.RoomCrossing createPiece(StructurePieceAccessor structurePieceAccessor, int i, int j, int k, Direction direction, int l) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -2, 0, 0, 7, 9, 7, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.RoomCrossing(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 6, 7, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			this.generateBox(worldGenLevel, boundingBox, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 0, 4, 5, 0, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 6, 4, 5, 6, blockState, blockState, false);
			this.generateBox(worldGenLevel, boundingBox, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 5, 2, 0, 5, 4, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 5, 2, 6, 5, 4, blockState2, blockState2, false);

			for(int i = 0; i <= 6; ++i) {
				for(int j = 0; j <= 6; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
				}
			}
		}
	}

	public static class StairsRoom extends NetherFortressPieces.NetherBridgePiece {
		private static final int WIDTH = 7;
		private static final int HEIGHT = 11;
		private static final int DEPTH = 7;

		public StairsRoom(int i, BoundingBox boundingBox, Direction direction) {
			super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, i, boundingBox);
			this.setOrientation(direction);
		}

		public StairsRoom(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, compoundTag);
		}

		@Override
		public void addChildren(StructurePiece structurePiece, StructurePieceAccessor structurePieceAccessor, RandomSource randomSource) {
			this.generateChildRight((NetherFortressPieces.StartPiece)structurePiece, structurePieceAccessor, randomSource, 6, 2, false);
		}

		public static NetherFortressPieces.StairsRoom createPiece(StructurePieceAccessor structurePieceAccessor, int i, int j, int k, int l, Direction direction) {
			BoundingBox boundingBox = BoundingBox.orientBox(i, j, k, -2, 0, 0, 7, 11, 7, direction);
			return isOkBox(boundingBox) && structurePieceAccessor.findCollisionPiece(boundingBox) == null
				? new NetherFortressPieces.StairsRoom(l, boundingBox, direction)
				: null;
		}

		@Override
		public void postProcess(
			WorldGenLevel worldGenLevel,
			StructureManager structureManager,
			ChunkGenerator chunkGenerator,
			RandomSource randomSource,
			BoundingBox boundingBox,
			ChunkPos chunkPos,
			BlockPos blockPos
		) {
			this.generateBox(worldGenLevel, boundingBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 6, 10, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			BlockState blockState = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.WEST, Boolean.valueOf(true))
				.setValue(FenceBlock.EAST, Boolean.valueOf(true));
			BlockState blockState2 = Blocks.NETHER_BRICK_FENCE
				.defaultBlockState()
				.setValue(FenceBlock.NORTH, Boolean.valueOf(true))
				.setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
			this.generateBox(worldGenLevel, boundingBox, 0, 3, 2, 0, 5, 4, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 6, 3, 2, 6, 5, 2, blockState2, blockState2, false);
			this.generateBox(worldGenLevel, boundingBox, 6, 3, 4, 6, 5, 4, blockState2, blockState2, false);
			this.placeBlock(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 5, 2, 5, boundingBox);
			this.generateBox(worldGenLevel, boundingBox, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 6, 8, 2, 6, 8, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
			this.generateBox(worldGenLevel, boundingBox, 2, 5, 0, 4, 5, 0, blockState, blockState, false);

			for(int i = 0; i <= 6; ++i) {
				for(int j = 0; j <= 6; ++j) {
					this.fillColumnDown(worldGenLevel, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, boundingBox);
				}
			}
		}
	}

	public static class StartPiece extends NetherFortressPieces.BridgeCrossing {
		public NetherFortressPieces.PieceWeight previousPiece;
		public List<NetherFortressPieces.PieceWeight> availableBridgePieces;
		public List<NetherFortressPieces.PieceWeight> availableCastlePieces;
		public final List<StructurePiece> pendingChildren = Lists.<StructurePiece>newArrayList();

		public StartPiece(RandomSource randomSource, int i, int j) {
			super(i, j, getRandomHorizontalDirection(randomSource));
			this.availableBridgePieces = Lists.<NetherFortressPieces.PieceWeight>newArrayList();

			for(NetherFortressPieces.PieceWeight pieceWeight : NetherFortressPieces.BRIDGE_PIECE_WEIGHTS) {
				pieceWeight.placeCount = 0;
				this.availableBridgePieces.add(pieceWeight);
			}

			this.availableCastlePieces = Lists.<NetherFortressPieces.PieceWeight>newArrayList();

			for(NetherFortressPieces.PieceWeight pieceWeight : NetherFortressPieces.CASTLE_PIECE_WEIGHTS) {
				pieceWeight.placeCount = 0;
				this.availableCastlePieces.add(pieceWeight);
			}
		}

		public StartPiece(CompoundTag compoundTag) {
			super(StructurePieceType.NETHER_FORTRESS_START, compoundTag);
		}
	}
}
