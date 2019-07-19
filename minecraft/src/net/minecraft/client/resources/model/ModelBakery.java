package net.minecraft.client.resources.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class ModelBakery {
	public static final ResourceLocation FIRE_0 = new ResourceLocation("block/fire_0");
	public static final ResourceLocation FIRE_1 = new ResourceLocation("block/fire_1");
	public static final ResourceLocation LAVA_FLOW = new ResourceLocation("block/lava_flow");
	public static final ResourceLocation WATER_FLOW = new ResourceLocation("block/water_flow");
	public static final ResourceLocation WATER_OVERLAY = new ResourceLocation("block/water_overlay");
	public static final ResourceLocation DESTROY_STAGE_0 = new ResourceLocation("block/destroy_stage_0");
	public static final ResourceLocation DESTROY_STAGE_1 = new ResourceLocation("block/destroy_stage_1");
	public static final ResourceLocation DESTROY_STAGE_2 = new ResourceLocation("block/destroy_stage_2");
	public static final ResourceLocation DESTROY_STAGE_3 = new ResourceLocation("block/destroy_stage_3");
	public static final ResourceLocation DESTROY_STAGE_4 = new ResourceLocation("block/destroy_stage_4");
	public static final ResourceLocation DESTROY_STAGE_5 = new ResourceLocation("block/destroy_stage_5");
	public static final ResourceLocation DESTROY_STAGE_6 = new ResourceLocation("block/destroy_stage_6");
	public static final ResourceLocation DESTROY_STAGE_7 = new ResourceLocation("block/destroy_stage_7");
	public static final ResourceLocation DESTROY_STAGE_8 = new ResourceLocation("block/destroy_stage_8");
	public static final ResourceLocation DESTROY_STAGE_9 = new ResourceLocation("block/destroy_stage_9");
	private static final Set<ResourceLocation> UNREFERENCED_TEXTURES = Sets.<ResourceLocation>newHashSet(
		WATER_FLOW,
		LAVA_FLOW,
		WATER_OVERLAY,
		FIRE_0,
		FIRE_1,
		DESTROY_STAGE_0,
		DESTROY_STAGE_1,
		DESTROY_STAGE_2,
		DESTROY_STAGE_3,
		DESTROY_STAGE_4,
		DESTROY_STAGE_5,
		DESTROY_STAGE_6,
		DESTROY_STAGE_7,
		DESTROY_STAGE_8,
		DESTROY_STAGE_9,
		new ResourceLocation("item/empty_armor_slot_helmet"),
		new ResourceLocation("item/empty_armor_slot_chestplate"),
		new ResourceLocation("item/empty_armor_slot_leggings"),
		new ResourceLocation("item/empty_armor_slot_boots"),
		new ResourceLocation("item/empty_armor_slot_shield")
	);
	private static final Logger LOGGER = LogManager.getLogger();
	public static final ModelResourceLocation MISSING_MODEL_LOCATION = new ModelResourceLocation("builtin/missing", "missing");
	@VisibleForTesting
	public static final String MISSING_MODEL_MESH = ("{    'textures': {       'particle': '"
			+ MissingTextureAtlasSprite.getLocation().getPath()
			+ "',       'missingno': '"
			+ MissingTextureAtlasSprite.getLocation().getPath()
			+ "'    },    'elements': [         {  'from': [ 0, 0, 0 ],            'to': [ 16, 16, 16 ],            'faces': {                'down':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'down',  'texture': '#missingno' },                'up':    { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'up',    'texture': '#missingno' },                'north': { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'north', 'texture': '#missingno' },                'south': { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'south', 'texture': '#missingno' },                'west':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'west',  'texture': '#missingno' },                'east':  { 'uv': [ 0, 0, 16, 16 ], 'cullface': 'east',  'texture': '#missingno' }            }        }    ]}")
		.replace('\'', '"');
	private static final Map<String, String> BUILTIN_MODELS = Maps.newHashMap(ImmutableMap.of("missing", MISSING_MODEL_MESH));
	private static final Splitter COMMA_SPLITTER = Splitter.on(',');
	private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);
	public static final BlockModel GENERATION_MARKER = Util.make(BlockModel.fromString("{}"), blockModel -> blockModel.name = "generation marker");
	public static final BlockModel BLOCK_ENTITY_MARKER = Util.make(BlockModel.fromString("{}"), blockModel -> blockModel.name = "block entity marker");
	private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
		.add(BooleanProperty.create("map"))
		.create(BlockState::new);
	private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
	private static final Map<ResourceLocation, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = ImmutableMap.of(
		new ResourceLocation("item_frame"), ITEM_FRAME_FAKE_DEFINITION
	);
	private final ResourceManager resourceManager;
	private final TextureAtlas blockAtlas;
	private final BlockColors blockColors;
	private final Set<ResourceLocation> loadingStack = Sets.<ResourceLocation>newHashSet();
	private final BlockModelDefinition.Context context = new BlockModelDefinition.Context();
	private final Map<ResourceLocation, UnbakedModel> unbakedCache = Maps.<ResourceLocation, UnbakedModel>newHashMap();
	private final Map<Triple<ResourceLocation, BlockModelRotation, Boolean>, BakedModel> bakedCache = Maps.<Triple<ResourceLocation, BlockModelRotation, Boolean>, BakedModel>newHashMap(
		
	);
	private final Map<ResourceLocation, UnbakedModel> topLevelModels = Maps.<ResourceLocation, UnbakedModel>newHashMap();
	private final Map<ResourceLocation, BakedModel> bakedTopLevelModels = Maps.<ResourceLocation, BakedModel>newHashMap();
	private final TextureAtlas.Preparations atlasPreparations;
	private int nextModelGroup = 1;
	private final Object2IntMap<BlockState> modelGroups = Util.make(
		new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> object2IntOpenHashMap.defaultReturnValue(-1)
	);

	public ModelBakery(ResourceManager resourceManager, TextureAtlas textureAtlas, BlockColors blockColors, ProfilerFiller profilerFiller) {
		this.resourceManager = resourceManager;
		this.blockAtlas = textureAtlas;
		this.blockColors = blockColors;
		profilerFiller.push("missing_model");

		try {
			this.unbakedCache.put(MISSING_MODEL_LOCATION, this.loadBlockModel(MISSING_MODEL_LOCATION));
			this.loadTopLevel(MISSING_MODEL_LOCATION);
		} catch (IOException var7) {
			LOGGER.error("Error loading missing model, should never happen :(", var7);
			throw new RuntimeException(var7);
		}

		profilerFiller.popPush("static_definitions");
		STATIC_DEFINITIONS.forEach(
			(resourceLocation, stateDefinition) -> stateDefinition.getPossibleStates()
					.forEach(blockState -> this.loadTopLevel(BlockModelShaper.stateToModelLocation(resourceLocation, blockState)))
		);
		profilerFiller.popPush("blocks");

		for(Block block : Registry.BLOCK) {
			block.getStateDefinition().getPossibleStates().forEach(blockState -> this.loadTopLevel(BlockModelShaper.stateToModelLocation(blockState)));
		}

		profilerFiller.popPush("items");

		for(ResourceLocation resourceLocation : Registry.ITEM.keySet()) {
			this.loadTopLevel(new ModelResourceLocation(resourceLocation, "inventory"));
		}

		profilerFiller.popPush("special");
		this.loadTopLevel(new ModelResourceLocation("minecraft:trident_in_hand#inventory"));
		profilerFiller.popPush("textures");
		Set<String> set = Sets.newLinkedHashSet();
		Set<ResourceLocation> set2 = (Set)this.topLevelModels
			.values()
			.stream()
			.flatMap(unbakedModel -> unbakedModel.getTextures(this::getModel, set).stream())
			.collect(Collectors.toSet());
		set2.addAll(UNREFERENCED_TEXTURES);
		set.forEach(string -> LOGGER.warn("Unable to resolve texture reference: {}", string));
		profilerFiller.popPush("stitching");
		this.atlasPreparations = this.blockAtlas.prepareToStitch(this.resourceManager, set2, profilerFiller);
		profilerFiller.pop();
	}

	public void uploadTextures(ProfilerFiller profilerFiller) {
		profilerFiller.push("atlas");
		this.blockAtlas.reload(this.atlasPreparations);
		profilerFiller.popPush("baking");
		this.topLevelModels.keySet().forEach(resourceLocation -> {
			BakedModel bakedModel = null;

			try {
				bakedModel = this.bake(resourceLocation, BlockModelRotation.X0_Y0);
			} catch (Exception var4) {
				LOGGER.warn("Unable to bake model: '{}': {}", resourceLocation, var4);
			}

			if (bakedModel != null) {
				this.bakedTopLevelModels.put(resourceLocation, bakedModel);
			}
		});
		profilerFiller.pop();
	}

	private static Predicate<BlockState> predicate(StateDefinition<Block, BlockState> stateDefinition, String string) {
		Map<Property<?>, Comparable<?>> map = Maps.newHashMap();

		for(String string2 : COMMA_SPLITTER.split(string)) {
			Iterator<String> iterator = EQUAL_SPLITTER.split(string2).iterator();
			if (iterator.hasNext()) {
				String string3 = (String)iterator.next();
				Property<?> property = stateDefinition.getProperty(string3);
				if (property != null && iterator.hasNext()) {
					String string4 = (String)iterator.next();
					Comparable<?> comparable = getValueHelper(property, string4);
					if (comparable == null) {
						throw new RuntimeException("Unknown value: '" + string4 + "' for blockstate property: '" + string3 + "' " + property.getPossibleValues());
					}

					map.put(property, comparable);
				} else if (!string3.isEmpty()) {
					throw new RuntimeException("Unknown blockstate property: '" + string3 + "'");
				}
			}
		}

		Block block = stateDefinition.getOwner();
		return blockState -> {
			if (blockState != null && block == blockState.getBlock()) {
				for(Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
					if (!Objects.equals(blockState.getValue((Property)entry.getKey()), entry.getValue())) {
						return false;
					}
				}

				return true;
			} else {
				return false;
			}
		};
	}

	@Nullable
	static <T extends Comparable<T>> T getValueHelper(Property<T> property, String string) {
		return (T)property.getValue(string).orElse(null);
	}

	public UnbakedModel getModel(ResourceLocation resourceLocation) {
		if (this.unbakedCache.containsKey(resourceLocation)) {
			return (UnbakedModel)this.unbakedCache.get(resourceLocation);
		} else if (this.loadingStack.contains(resourceLocation)) {
			throw new IllegalStateException("Circular reference while loading " + resourceLocation);
		} else {
			this.loadingStack.add(resourceLocation);
			UnbakedModel unbakedModel = (UnbakedModel)this.unbakedCache.get(MISSING_MODEL_LOCATION);

			while(!this.loadingStack.isEmpty()) {
				ResourceLocation resourceLocation2 = (ResourceLocation)this.loadingStack.iterator().next();

				try {
					if (!this.unbakedCache.containsKey(resourceLocation2)) {
						this.loadModel(resourceLocation2);
					}
				} catch (ModelBakery.BlockStateDefinitionException var9) {
					LOGGER.warn(var9.getMessage());
					this.unbakedCache.put(resourceLocation2, unbakedModel);
				} catch (Exception var10) {
					LOGGER.warn("Unable to load model: '{}' referenced from: {}: {}", resourceLocation2, resourceLocation, var10);
					this.unbakedCache.put(resourceLocation2, unbakedModel);
				} finally {
					this.loadingStack.remove(resourceLocation2);
				}
			}

			return (UnbakedModel)this.unbakedCache.getOrDefault(resourceLocation, unbakedModel);
		}
	}

	private void loadModel(ResourceLocation resourceLocation) throws Exception {
		if (!(resourceLocation instanceof ModelResourceLocation)) {
			this.cacheAndQueueDependencies(resourceLocation, this.loadBlockModel(resourceLocation));
		} else {
			ModelResourceLocation modelResourceLocation = (ModelResourceLocation)resourceLocation;
			if (Objects.equals(modelResourceLocation.getVariant(), "inventory")) {
				ResourceLocation resourceLocation2 = new ResourceLocation(resourceLocation.getNamespace(), "item/" + resourceLocation.getPath());
				BlockModel blockModel = this.loadBlockModel(resourceLocation2);
				this.cacheAndQueueDependencies(modelResourceLocation, blockModel);
				this.unbakedCache.put(resourceLocation2, blockModel);
			} else {
				ResourceLocation resourceLocation2 = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath());
				StateDefinition<Block, BlockState> stateDefinition = (StateDefinition)Optional.ofNullable(STATIC_DEFINITIONS.get(resourceLocation2))
					.orElseGet(() -> Registry.BLOCK.get(resourceLocation2).getStateDefinition());
				this.context.setDefinition(stateDefinition);
				List<Property<?>> list = ImmutableList.copyOf(this.blockColors.getColoringProperties(stateDefinition.getOwner()));
				ImmutableList<BlockState> immutableList = stateDefinition.getPossibleStates();
				Map<ModelResourceLocation, BlockState> map = Maps.<ModelResourceLocation, BlockState>newHashMap();
				immutableList.forEach(blockState -> BlockModelShaper.stateToModelLocation(resourceLocation2, blockState));
				Map<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>> map2 = Maps.<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>>newHashMap(
					
				);
				ResourceLocation resourceLocation3 = new ResourceLocation(resourceLocation.getNamespace(), "blockstates/" + resourceLocation.getPath() + ".json");
				UnbakedModel unbakedModel = (UnbakedModel)this.unbakedCache.get(MISSING_MODEL_LOCATION);
				ModelBakery.ModelGroupKey modelGroupKey = new ModelBakery.ModelGroupKey(ImmutableList.of(unbakedModel), ImmutableList.of());
				Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> pair = Pair.of(unbakedModel, (Supplier)() -> modelGroupKey);

				try {
					List<Pair<String, BlockModelDefinition>> list2;
					try {
						list2 = (List)this.resourceManager
							.getResources(resourceLocation3)
							.stream()
							.map(
								resource -> {
									try {
										InputStream inputStream = resource.getInputStream();
										Throwable var3xx = null;
		
										Pair var4x;
										try {
											var4x = Pair.of(resource.getSourceName(), BlockModelDefinition.fromStream(this.context, new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
										} catch (Throwable var14) {
											var3xx = var14;
											throw var14;
										} finally {
											if (inputStream != null) {
												if (var3xx != null) {
													try {
														inputStream.close();
													} catch (Throwable var13xx) {
														var3xx.addSuppressed(var13xx);
													}
												} else {
													inputStream.close();
												}
											}
										}
		
										return var4x;
									} catch (Exception var16xx) {
										throw new ModelBakery.BlockStateDefinitionException(
											String.format(
												"Exception loading blockstate definition: '%s' in resourcepack: '%s': %s", resource.getLocation(), resource.getSourceName(), var16xx.getMessage()
											)
										);
									}
								}
							)
							.collect(Collectors.toList());
					} catch (IOException var25) {
						LOGGER.warn("Exception loading blockstate definition: {}: {}", resourceLocation3, var25);
						return;
					}

					for(Pair<String, BlockModelDefinition> pair2 : list2) {
						BlockModelDefinition blockModelDefinition = pair2.getSecond();
						Map<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>> map4 = Maps.<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>>newIdentityHashMap(
							
						);
						MultiPart multiPart;
						if (blockModelDefinition.isMultiPart()) {
							multiPart = blockModelDefinition.getMultiPart();
							immutableList.forEach(blockState -> {
							});
						} else {
							multiPart = null;
						}

						blockModelDefinition.getVariants()
							.forEach(
								(string, multiVariant) -> {
									try {
										immutableList.stream()
											.filter(predicate(stateDefinition, string))
											.forEach(
												blockState -> {
													Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> pair2xxx = (Pair)map4.put(
														blockState, Pair.of(multiVariant, (Supplier)() -> ModelBakery.ModelGroupKey.create(blockState, multiVariant, list))
													);
													if (pair2xxx != null && pair2xxx.getFirst() != multiPart) {
														map4.put(blockState, pair);
														throw new RuntimeException(
															"Overlapping definition with: "
																+ (String)((Entry)blockModelDefinition.getVariants()
																		.entrySet()
																		.stream()
																		.filter(entry -> entry.getValue() == pair2xx.getFirst())
																		.findFirst()
																		.get())
																	.getKey()
														);
													}
												}
											);
									} catch (Exception var12xx) {
										LOGGER.warn(
											"Exception loading blockstate definition: '{}' in resourcepack: '{}' for variant: '{}': {}",
											resourceLocation3,
											pair2.getFirst(),
											string,
											var12xx.getMessage()
										);
									}
								}
							);
						map2.putAll(map4);
					}
				} catch (ModelBakery.BlockStateDefinitionException var26) {
					throw var26;
				} catch (Exception var27) {
					throw new ModelBakery.BlockStateDefinitionException(String.format("Exception loading blockstate definition: '%s': %s", resourceLocation3, var27));
				} finally {
					Map<ModelBakery.ModelGroupKey, Set<BlockState>> map6 = Maps.newHashMap();
					map.forEach((modelResourceLocationx, blockState) -> {
						Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> pair2xx = (Pair)map2.get(blockState);
						if (pair2xx == null) {
							LOGGER.warn("Exception loading blockstate definition: '{}' missing model for variant: '{}'", resourceLocation3, modelResourceLocationx);
							pair2xx = pair;
						}

						this.cacheAndQueueDependencies(modelResourceLocationx, pair2xx.getFirst());

						try {
							ModelBakery.ModelGroupKey modelGroupKeyxx = (ModelBakery.ModelGroupKey)((Supplier)pair2xx.getSecond()).get();
							((Set)map6.computeIfAbsent(modelGroupKeyxx, modelGroupKeyxx -> Sets.newIdentityHashSet())).add(blockState);
						} catch (Exception var9xx) {
							LOGGER.warn("Exception evaluating model definition: '{}'", modelResourceLocationx, var9xx);
						}
					});
					map6.forEach((modelGroupKeyx, set) -> {
						Iterator<BlockState> iterator = set.iterator();

						while(iterator.hasNext()) {
							BlockState blockState = (BlockState)iterator.next();
							if (blockState.getRenderShape() != RenderShape.MODEL) {
								iterator.remove();
								this.modelGroups.put(blockState, 0);
							}
						}

						if (set.size() > 1) {
							this.registerModelGroup(set);
						}
					});
				}
			}
		}
	}

	private void cacheAndQueueDependencies(ResourceLocation resourceLocation, UnbakedModel unbakedModel) {
		this.unbakedCache.put(resourceLocation, unbakedModel);
		this.loadingStack.addAll(unbakedModel.getDependencies());
	}

	private void loadTopLevel(ModelResourceLocation modelResourceLocation) {
		UnbakedModel unbakedModel = this.getModel(modelResourceLocation);
		this.unbakedCache.put(modelResourceLocation, unbakedModel);
		this.topLevelModels.put(modelResourceLocation, unbakedModel);
	}

	private void registerModelGroup(Iterable<BlockState> iterable) {
		int i = this.nextModelGroup++;
		iterable.forEach(blockState -> this.modelGroups.put(blockState, i));
	}

	@Nullable
	public BakedModel bake(ResourceLocation resourceLocation, ModelState modelState) {
		Triple<ResourceLocation, BlockModelRotation, Boolean> triple = Triple.of(resourceLocation, modelState.getRotation(), modelState.isUvLocked());
		if (this.bakedCache.containsKey(triple)) {
			return (BakedModel)this.bakedCache.get(triple);
		} else {
			UnbakedModel unbakedModel = this.getModel(resourceLocation);
			if (unbakedModel instanceof BlockModel) {
				BlockModel blockModel = (BlockModel)unbakedModel;
				if (blockModel.getRootModel() == GENERATION_MARKER) {
					return ITEM_MODEL_GENERATOR.generateBlockModel(this.blockAtlas::getSprite, blockModel).bake(this, blockModel, this.blockAtlas::getSprite, modelState);
				}
			}

			BakedModel bakedModel = unbakedModel.bake(this, this.blockAtlas::getSprite, modelState);
			this.bakedCache.put(triple, bakedModel);
			return bakedModel;
		}
	}

	private BlockModel loadBlockModel(ResourceLocation resourceLocation) throws IOException {
		Reader reader = null;
		Resource resource = null;

		BlockModel blockModel;
		try {
			String string = resourceLocation.getPath();
			if ("builtin/generated".equals(string)) {
				return GENERATION_MARKER;
			}

			if (!"builtin/entity".equals(string)) {
				if (string.startsWith("builtin/")) {
					String string2 = string.substring("builtin/".length());
					String string3 = (String)BUILTIN_MODELS.get(string2);
					if (string3 == null) {
						throw new FileNotFoundException(resourceLocation.toString());
					}

					reader = new StringReader(string3);
				} else {
					resource = this.resourceManager.getResource(new ResourceLocation(resourceLocation.getNamespace(), "models/" + resourceLocation.getPath() + ".json"));
					reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
				}

				blockModel = BlockModel.fromStream(reader);
				blockModel.name = resourceLocation.toString();
				return blockModel;
			}

			blockModel = BLOCK_ENTITY_MARKER;
		} finally {
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(resource);
		}

		return blockModel;
	}

	public Map<ResourceLocation, BakedModel> getBakedTopLevelModels() {
		return this.bakedTopLevelModels;
	}

	public Object2IntMap<BlockState> getModelGroups() {
		return this.modelGroups;
	}

	@Environment(EnvType.CLIENT)
	static class BlockStateDefinitionException extends RuntimeException {
		public BlockStateDefinitionException(String string) {
			super(string);
		}
	}

	@Environment(EnvType.CLIENT)
	static class ModelGroupKey {
		private final List<UnbakedModel> models;
		private final List<Object> coloringValues;

		public ModelGroupKey(List<UnbakedModel> list, List<Object> list2) {
			this.models = list;
			this.coloringValues = list2;
		}

		public boolean equals(Object object) {
			if (this == object) {
				return true;
			} else if (!(object instanceof ModelBakery.ModelGroupKey)) {
				return false;
			} else {
				ModelBakery.ModelGroupKey modelGroupKey = (ModelBakery.ModelGroupKey)object;
				return Objects.equals(this.models, modelGroupKey.models) && Objects.equals(this.coloringValues, modelGroupKey.coloringValues);
			}
		}

		public int hashCode() {
			return 31 * this.models.hashCode() + this.coloringValues.hashCode();
		}

		public static ModelBakery.ModelGroupKey create(BlockState blockState, MultiPart multiPart, Collection<Property<?>> collection) {
			StateDefinition<Block, BlockState> stateDefinition = blockState.getBlock().getStateDefinition();
			List<UnbakedModel> list = (List)multiPart.getSelectors()
				.stream()
				.filter(selector -> selector.getPredicate(stateDefinition).test(blockState))
				.map(Selector::getVariant)
				.collect(ImmutableList.toImmutableList());
			List<Object> list2 = getColoringValues(blockState, collection);
			return new ModelBakery.ModelGroupKey(list, list2);
		}

		public static ModelBakery.ModelGroupKey create(BlockState blockState, UnbakedModel unbakedModel, Collection<Property<?>> collection) {
			List<Object> list = getColoringValues(blockState, collection);
			return new ModelBakery.ModelGroupKey(ImmutableList.of(unbakedModel), list);
		}

		private static List<Object> getColoringValues(BlockState blockState, Collection<Property<?>> collection) {
			return (List<Object>)collection.stream().map(blockState::getValue).collect(ImmutableList.toImmutableList());
		}
	}
}
