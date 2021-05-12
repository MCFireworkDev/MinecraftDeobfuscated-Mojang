package net.minecraft.world.level.levelgen.feature.structures;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JigsawPlacement {
	static final Logger LOGGER = LogManager.getLogger();

	public static void addPieces(
		RegistryAccess registryAccess,
		JigsawConfiguration jigsawConfiguration,
		JigsawPlacement.PieceFactory pieceFactory,
		ChunkGenerator chunkGenerator,
		StructureManager structureManager,
		BlockPos blockPos,
		StructurePieceAccessor structurePieceAccessor,
		Random random,
		boolean bl,
		boolean bl2,
		LevelHeightAccessor levelHeightAccessor
	) {
		StructureFeature.bootstrap();
		List<PoolElementStructurePiece> list = Lists.<PoolElementStructurePiece>newArrayList();
		Registry<StructureTemplatePool> registry = registryAccess.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
		Rotation rotation = Rotation.getRandom(random);
		StructureTemplatePool structureTemplatePool = (StructureTemplatePool)jigsawConfiguration.startPool().get();
		StructurePoolElement structurePoolElement = structureTemplatePool.getRandomTemplate(random);
		if (structurePoolElement != EmptyPoolElement.INSTANCE) {
			PoolElementStructurePiece poolElementStructurePiece = pieceFactory.create(
				structureManager,
				structurePoolElement,
				blockPos,
				structurePoolElement.getGroundLevelDelta(),
				rotation,
				structurePoolElement.getBoundingBox(structureManager, blockPos, rotation)
			);
			BoundingBox boundingBox = poolElementStructurePiece.getBoundingBox();
			int i = (boundingBox.maxX() + boundingBox.minX()) / 2;
			int j = (boundingBox.maxZ() + boundingBox.minZ()) / 2;
			int k;
			if (bl2) {
				k = blockPos.getY() + chunkGenerator.getFirstFreeHeight(i, j, Heightmap.Types.WORLD_SURFACE_WG, levelHeightAccessor);
			} else {
				k = blockPos.getY();
			}

			int l = boundingBox.minY() + poolElementStructurePiece.getGroundLevelDelta();
			poolElementStructurePiece.move(0, k - l, 0);
			list.add(poolElementStructurePiece);
			if (jigsawConfiguration.maxDepth() > 0) {
				int m = 80;
				AABB aABB = new AABB((double)(i - 80), (double)(k - 80), (double)(j - 80), (double)(i + 80 + 1), (double)(k + 80 + 1), (double)(j + 80 + 1));
				JigsawPlacement.Placer placer = new JigsawPlacement.Placer(
					registry, jigsawConfiguration.maxDepth(), pieceFactory, chunkGenerator, structureManager, list, random
				);
				placer.placing
					.addLast(
						new JigsawPlacement.PieceState(
							poolElementStructurePiece, new MutableObject<>(Shapes.join(Shapes.create(aABB), Shapes.create(AABB.of(boundingBox)), BooleanOp.ONLY_FIRST)), k + 80, 0
						)
					);

				while(!placer.placing.isEmpty()) {
					JigsawPlacement.PieceState pieceState = (JigsawPlacement.PieceState)placer.placing.removeFirst();
					placer.tryPlacingChildren(pieceState.piece, pieceState.free, pieceState.boundsTop, pieceState.depth, bl, levelHeightAccessor);
				}

				list.forEach(structurePieceAccessor::addPiece);
			}
		}
	}

	public static void addPieces(
		RegistryAccess registryAccess,
		PoolElementStructurePiece poolElementStructurePiece,
		int i,
		JigsawPlacement.PieceFactory pieceFactory,
		ChunkGenerator chunkGenerator,
		StructureManager structureManager,
		List<? super PoolElementStructurePiece> list,
		Random random,
		LevelHeightAccessor levelHeightAccessor
	) {
		Registry<StructureTemplatePool> registry = registryAccess.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
		JigsawPlacement.Placer placer = new JigsawPlacement.Placer(registry, i, pieceFactory, chunkGenerator, structureManager, list, random);
		placer.placing.addLast(new JigsawPlacement.PieceState(poolElementStructurePiece, new MutableObject<>(Shapes.INFINITY), 0, 0));

		while(!placer.placing.isEmpty()) {
			JigsawPlacement.PieceState pieceState = (JigsawPlacement.PieceState)placer.placing.removeFirst();
			placer.tryPlacingChildren(pieceState.piece, pieceState.free, pieceState.boundsTop, pieceState.depth, false, levelHeightAccessor);
		}
	}

	public interface PieceFactory {
		PoolElementStructurePiece create(
			StructureManager structureManager, StructurePoolElement structurePoolElement, BlockPos blockPos, int i, Rotation rotation, BoundingBox boundingBox
		);
	}

	static final class PieceState {
		final PoolElementStructurePiece piece;
		final MutableObject<VoxelShape> free;
		final int boundsTop;
		final int depth;

		PieceState(PoolElementStructurePiece poolElementStructurePiece, MutableObject<VoxelShape> mutableObject, int i, int j) {
			this.piece = poolElementStructurePiece;
			this.free = mutableObject;
			this.boundsTop = i;
			this.depth = j;
		}
	}

	static final class Placer {
		private final Registry<StructureTemplatePool> pools;
		private final int maxDepth;
		private final JigsawPlacement.PieceFactory factory;
		private final ChunkGenerator chunkGenerator;
		private final StructureManager structureManager;
		private final List<? super PoolElementStructurePiece> pieces;
		private final Random random;
		final Deque<JigsawPlacement.PieceState> placing = Queues.<JigsawPlacement.PieceState>newArrayDeque();

		Placer(
			Registry<StructureTemplatePool> registry,
			int i,
			JigsawPlacement.PieceFactory pieceFactory,
			ChunkGenerator chunkGenerator,
			StructureManager structureManager,
			List<? super PoolElementStructurePiece> list,
			Random random
		) {
			this.pools = registry;
			this.maxDepth = i;
			this.factory = pieceFactory;
			this.chunkGenerator = chunkGenerator;
			this.structureManager = structureManager;
			this.pieces = list;
			this.random = random;
		}

		void tryPlacingChildren(
			PoolElementStructurePiece poolElementStructurePiece,
			MutableObject<VoxelShape> mutableObject,
			int i,
			int j,
			boolean bl,
			LevelHeightAccessor levelHeightAccessor
		) {
			StructurePoolElement structurePoolElement = poolElementStructurePiece.getElement();
			BlockPos blockPos = poolElementStructurePiece.getPosition();
			Rotation rotation = poolElementStructurePiece.getRotation();
			StructureTemplatePool.Projection projection = structurePoolElement.getProjection();
			boolean bl2 = projection == StructureTemplatePool.Projection.RIGID;
			MutableObject<VoxelShape> mutableObject2 = new MutableObject<>();
			BoundingBox boundingBox = poolElementStructurePiece.getBoundingBox();
			int k = boundingBox.minY();

			label137:
			for(StructureTemplate.StructureBlockInfo structureBlockInfo : structurePoolElement.getShuffledJigsawBlocks(
				this.structureManager, blockPos, rotation, this.random
			)) {
				Direction direction = JigsawBlock.getFrontFacing(structureBlockInfo.state);
				BlockPos blockPos2 = structureBlockInfo.pos;
				BlockPos blockPos3 = blockPos2.relative(direction);
				int l = blockPos2.getY() - k;
				int m = -1;
				ResourceLocation resourceLocation = new ResourceLocation(structureBlockInfo.nbt.getString("pool"));
				Optional<StructureTemplatePool> optional = this.pools.getOptional(resourceLocation);
				if (optional.isPresent() && (((StructureTemplatePool)optional.get()).size() != 0 || Objects.equals(resourceLocation, Pools.EMPTY.location()))) {
					ResourceLocation resourceLocation2 = ((StructureTemplatePool)optional.get()).getFallback();
					Optional<StructureTemplatePool> optional2 = this.pools.getOptional(resourceLocation2);
					if (optional2.isPresent() && (((StructureTemplatePool)optional2.get()).size() != 0 || Objects.equals(resourceLocation2, Pools.EMPTY.location()))) {
						boolean bl3 = boundingBox.isInside(blockPos3);
						MutableObject<VoxelShape> mutableObject3;
						int n;
						if (bl3) {
							mutableObject3 = mutableObject2;
							n = k;
							if (mutableObject2.getValue() == null) {
								mutableObject2.setValue(Shapes.create(AABB.of(boundingBox)));
							}
						} else {
							mutableObject3 = mutableObject;
							n = i;
						}

						List<StructurePoolElement> list = Lists.<StructurePoolElement>newArrayList();
						if (j != this.maxDepth) {
							list.addAll(((StructureTemplatePool)optional.get()).getShuffledTemplates(this.random));
						}

						list.addAll(((StructureTemplatePool)optional2.get()).getShuffledTemplates(this.random));

						for(StructurePoolElement structurePoolElement2 : list) {
							if (structurePoolElement2 == EmptyPoolElement.INSTANCE) {
								break;
							}

							for(Rotation rotation2 : Rotation.getShuffled(this.random)) {
								List<StructureTemplate.StructureBlockInfo> list2 = structurePoolElement2.getShuffledJigsawBlocks(
									this.structureManager, BlockPos.ZERO, rotation2, this.random
								);
								BoundingBox boundingBox2 = structurePoolElement2.getBoundingBox(this.structureManager, BlockPos.ZERO, rotation2);
								int o;
								if (bl && boundingBox2.getYSpan() <= 16) {
									o = list2.stream()
										.mapToInt(
											structureBlockInfox -> {
												if (!boundingBox2.isInside(structureBlockInfox.pos.relative(JigsawBlock.getFrontFacing(structureBlockInfox.state)))) {
													return 0;
												} else {
													ResourceLocation resourceLocationxx = new ResourceLocation(structureBlockInfox.nbt.getString("pool"));
													Optional<StructureTemplatePool> optionalxx = this.pools.getOptional(resourceLocationxx);
													Optional<StructureTemplatePool> optional2xx = optionalxx.flatMap(
														structureTemplatePool -> this.pools.getOptional(structureTemplatePool.getFallback())
													);
													int ixx = optionalxx.map(structureTemplatePool -> structureTemplatePool.getMaxSize(this.structureManager)).orElse(0);
													int jxx = optional2xx.map(structureTemplatePool -> structureTemplatePool.getMaxSize(this.structureManager)).orElse(0);
													return Math.max(ixx, jxx);
												}
											}
										)
										.max()
										.orElse(0);
								} else {
									o = 0;
								}

								for(StructureTemplate.StructureBlockInfo structureBlockInfo2 : list2) {
									if (JigsawBlock.canAttach(structureBlockInfo, structureBlockInfo2)) {
										BlockPos blockPos4 = structureBlockInfo2.pos;
										BlockPos blockPos5 = blockPos3.subtract(blockPos4);
										BoundingBox boundingBox3 = structurePoolElement2.getBoundingBox(this.structureManager, blockPos5, rotation2);
										int p = boundingBox3.minY();
										StructureTemplatePool.Projection projection2 = structurePoolElement2.getProjection();
										boolean bl4 = projection2 == StructureTemplatePool.Projection.RIGID;
										int q = blockPos4.getY();
										int r = l - q + JigsawBlock.getFrontFacing(structureBlockInfo.state).getStepY();
										int s;
										if (bl2 && bl4) {
											s = k + r;
										} else {
											if (m == -1) {
												m = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), Heightmap.Types.WORLD_SURFACE_WG, levelHeightAccessor);
											}

											s = m - q;
										}

										int t = s - p;
										BoundingBox boundingBox4 = boundingBox3.moved(0, t, 0);
										BlockPos blockPos6 = blockPos5.offset(0, t, 0);
										if (o > 0) {
											int u = Math.max(o + 1, boundingBox4.maxY() - boundingBox4.minY());
											boundingBox4.encapsulate(new BlockPos(boundingBox4.minX(), boundingBox4.minY() + u, boundingBox4.minZ()));
										}

										if (!Shapes.joinIsNotEmpty(mutableObject3.getValue(), Shapes.create(AABB.of(boundingBox4).deflate(0.25)), BooleanOp.ONLY_SECOND)) {
											mutableObject3.setValue(Shapes.joinUnoptimized(mutableObject3.getValue(), Shapes.create(AABB.of(boundingBox4)), BooleanOp.ONLY_FIRST));
											int u = poolElementStructurePiece.getGroundLevelDelta();
											int v;
											if (bl4) {
												v = u - r;
											} else {
												v = structurePoolElement2.getGroundLevelDelta();
											}

											PoolElementStructurePiece poolElementStructurePiece2 = this.factory
												.create(this.structureManager, structurePoolElement2, blockPos6, v, rotation2, boundingBox4);
											int w;
											if (bl2) {
												w = k + l;
											} else if (bl4) {
												w = s + q;
											} else {
												if (m == -1) {
													m = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), Heightmap.Types.WORLD_SURFACE_WG, levelHeightAccessor);
												}

												w = m + r / 2;
											}

											poolElementStructurePiece.addJunction(new JigsawJunction(blockPos3.getX(), w - l + u, blockPos3.getZ(), r, projection2));
											poolElementStructurePiece2.addJunction(new JigsawJunction(blockPos2.getX(), w - q + v, blockPos2.getZ(), -r, projection));
											this.pieces.add(poolElementStructurePiece2);
											if (j + 1 <= this.maxDepth) {
												this.placing.addLast(new JigsawPlacement.PieceState(poolElementStructurePiece2, mutableObject3, n, j + 1));
											}
											continue label137;
										}
									}
								}
							}
						}
					} else {
						JigsawPlacement.LOGGER.warn("Empty or non-existent fallback pool: {}", resourceLocation2);
					}
				} else {
					JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", resourceLocation);
				}
			}
		}
	}
}
