package net.minecraft.client.resources.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
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
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.texture.AtlasSet;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ModelBakery {
	public static final Material FIRE_0 = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/fire_0"));
	public static final Material FIRE_1 = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/fire_1"));
	public static final Material LAVA_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/lava_flow"));
	public static final Material WATER_FLOW = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/water_flow"));
	public static final Material WATER_OVERLAY = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("block/water_overlay"));
	public static final Material BANNER_BASE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/banner_base"));
	public static final Material SHIELD_BASE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/shield_base"));
	public static final Material NO_PATTERN_SHIELD = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/shield_base_nopattern"));
	public static final int DESTROY_STAGE_COUNT = 10;
	public static final List<ResourceLocation> DESTROY_STAGES = (List<ResourceLocation>)IntStream.range(0, 10)
		.mapToObj(i -> new ResourceLocation("block/destroy_stage_" + i))
		.collect(Collectors.toList());
	public static final List<ResourceLocation> BREAKING_LOCATIONS = (List<ResourceLocation>)DESTROY_STAGES.stream()
		.map(resourceLocation -> new ResourceLocation("textures/" + resourceLocation.getPath() + ".png"))
		.collect(Collectors.toList());
	public static final List<RenderType> DESTROY_TYPES = (List<RenderType>)BREAKING_LOCATIONS.stream().map(RenderType::crumbling).collect(Collectors.toList());
	private static final Set<Material> UNREFERENCED_TEXTURES = Util.make(Sets.<Material>newHashSet(), hashSet -> {
		hashSet.add(WATER_FLOW);
		hashSet.add(LAVA_FLOW);
		hashSet.add(WATER_OVERLAY);
		hashSet.add(FIRE_0);
		hashSet.add(FIRE_1);
		hashSet.add(BellRenderer.BELL_RESOURCE_LOCATION);
		hashSet.add(ConduitRenderer.SHELL_TEXTURE);
		hashSet.add(ConduitRenderer.ACTIVE_SHELL_TEXTURE);
		hashSet.add(ConduitRenderer.WIND_TEXTURE);
		hashSet.add(ConduitRenderer.VERTICAL_WIND_TEXTURE);
		hashSet.add(ConduitRenderer.OPEN_EYE_TEXTURE);
		hashSet.add(ConduitRenderer.CLOSED_EYE_TEXTURE);
		hashSet.add(EnchantTableRenderer.BOOK_LOCATION);
		hashSet.add(BANNER_BASE);
		hashSet.add(SHIELD_BASE);
		hashSet.add(NO_PATTERN_SHIELD);

		for(ResourceLocation resourceLocation : DESTROY_STAGES) {
			hashSet.add(new Material(TextureAtlas.LOCATION_BLOCKS, resourceLocation));
		}

		hashSet.add(new Material(TextureAtlas.LOCATION_BLOCKS, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET));
		hashSet.add(new Material(TextureAtlas.LOCATION_BLOCKS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
		hashSet.add(new Material(TextureAtlas.LOCATION_BLOCKS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS));
		hashSet.add(new Material(TextureAtlas.LOCATION_BLOCKS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS));
		hashSet.add(new Material(TextureAtlas.LOCATION_BLOCKS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
		Sheets.getAllMaterials(hashSet::add);
	});
	static final int SINGLETON_MODEL_GROUP = -1;
	private static final int INVISIBLE_MODEL_GROUP = 0;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String BUILTIN_SLASH = "builtin/";
	private static final String BUILTIN_SLASH_GENERATED = "builtin/generated";
	private static final String BUILTIN_BLOCK_ENTITY = "builtin/entity";
	private static final String MISSING_MODEL_NAME = "missing";
	public static final ModelResourceLocation MISSING_MODEL_LOCATION = new ModelResourceLocation("builtin/missing", "missing");
	private static final String MISSING_MODEL_LOCATION_STRING = MISSING_MODEL_LOCATION.toString();
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
	public static final BlockModel GENERATION_MARKER = Util.make(
		BlockModel.fromString("{\"gui_light\": \"front\"}"), blockModel -> blockModel.name = "generation marker"
	);
	public static final BlockModel BLOCK_ENTITY_MARKER = Util.make(
		BlockModel.fromString("{\"gui_light\": \"side\"}"), blockModel -> blockModel.name = "block entity marker"
	);
	private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
		.add(BooleanProperty.create("map"))
		.create(Block::defaultBlockState, BlockState::new);
	private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
	private static final Map<ResourceLocation, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = ImmutableMap.of(
		new ResourceLocation("item_frame"), ITEM_FRAME_FAKE_DEFINITION, new ResourceLocation("glow_item_frame"), ITEM_FRAME_FAKE_DEFINITION
	);
	private final ResourceManager resourceManager;
	@Nullable
	private AtlasSet atlasSet;
	private final BlockColors blockColors;
	private final Set<ResourceLocation> loadingStack = Sets.<ResourceLocation>newHashSet();
	private final BlockModelDefinition.Context context = new BlockModelDefinition.Context();
	private final Map<ResourceLocation, UnbakedModel> unbakedCache = Maps.<ResourceLocation, UnbakedModel>newHashMap();
	private final Map<Triple<ResourceLocation, Transformation, Boolean>, BakedModel> bakedCache = Maps.<Triple<ResourceLocation, Transformation, Boolean>, BakedModel>newHashMap(
		
	);
	private final Map<ResourceLocation, UnbakedModel> topLevelModels = Maps.<ResourceLocation, UnbakedModel>newHashMap();
	private final Map<ResourceLocation, BakedModel> bakedTopLevelModels = Maps.<ResourceLocation, BakedModel>newHashMap();
	private final Map<ResourceLocation, Pair<TextureAtlas, TextureAtlas.Preparations>> atlasPreparations;
	private int nextModelGroup = 1;
	private final Object2IntMap<BlockState> modelGroups = Util.make(
		new Object2IntOpenHashMap<>(), object2IntOpenHashMap -> object2IntOpenHashMap.defaultReturnValue(-1)
	);

	public ModelBakery(ResourceManager resourceManager, BlockColors blockColors, ProfilerFiller profilerFiller, int i) {
		this.resourceManager = resourceManager;
		this.blockColors = blockColors;
		profilerFiller.push("missing_model");

		try {
			this.unbakedCache.put(MISSING_MODEL_LOCATION, this.loadBlockModel(MISSING_MODEL_LOCATION));
			this.loadTopLevel(MISSING_MODEL_LOCATION);
		} catch (IOException var12) {
			LOGGER.error("Error loading missing model, should never happen :(", var12);
			throw new RuntimeException(var12);
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
		this.loadTopLevel(new ModelResourceLocation("minecraft:spyglass_in_hand#inventory"));
		profilerFiller.popPush("textures");
		Set<Pair<String, String>> set = Sets.<Pair<String, String>>newLinkedHashSet();
		Set<Material> set2 = (Set)this.topLevelModels
			.values()
			.stream()
			.flatMap(unbakedModel -> unbakedModel.getMaterials(this::getModel, set).stream())
			.collect(Collectors.toSet());
		set2.addAll(UNREFERENCED_TEXTURES);
		set.stream()
			.filter(pair -> !((String)pair.getSecond()).equals(MISSING_MODEL_LOCATION_STRING))
			.forEach(pair -> LOGGER.warn("Unable to resolve texture reference: {} in {}", pair.getFirst(), pair.getSecond()));
		Map<ResourceLocation, List<Material>> map = (Map)set2.stream().collect(Collectors.groupingBy(Material::atlasLocation));
		profilerFiller.popPush("stitching");
		this.atlasPreparations = Maps.<ResourceLocation, Pair<TextureAtlas, TextureAtlas.Preparations>>newHashMap();

		for(Entry<ResourceLocation, List<Material>> entry : map.entrySet()) {
			TextureAtlas textureAtlas = new TextureAtlas((ResourceLocation)entry.getKey());
			TextureAtlas.Preparations preparations = textureAtlas.prepareToStitch(
				this.resourceManager, ((List)entry.getValue()).stream().map(Material::texture), profilerFiller, i
			);
			this.atlasPreparations.put((ResourceLocation)entry.getKey(), Pair.of(textureAtlas, preparations));
		}

		profilerFiller.pop();
	}

	public AtlasSet uploadTextures(TextureManager textureManager, ProfilerFiller profilerFiller) {
		profilerFiller.push("atlas");

		for(Pair<TextureAtlas, TextureAtlas.Preparations> pair : this.atlasPreparations.values()) {
			TextureAtlas textureAtlas = pair.getFirst();
			TextureAtlas.Preparations preparations = pair.getSecond();
			textureAtlas.reload(preparations);
			textureManager.register(textureAtlas.location(), textureAtlas);
			textureManager.bindForSetup(textureAtlas.location());
			textureAtlas.updateFilter(preparations);
		}

		this.atlasSet = new AtlasSet((Collection<TextureAtlas>)this.atlasPreparations.values().stream().map(Pair::getFirst).collect(Collectors.toList()));
		profilerFiller.popPush("baking");
		this.topLevelModels.keySet().forEach(resourceLocation -> {
			BakedModel bakedModel = null;

			try {
				bakedModel = this.bake(resourceLocation, BlockModelRotation.X0_Y0);
			} catch (Exception var4xx) {
				LOGGER.warn("Unable to bake model: '{}': {}", resourceLocation, var4xx);
			}

			if (bakedModel != null) {
				this.bakedTopLevelModels.put(resourceLocation, bakedModel);
			}
		});
		profilerFiller.pop();
		return this.atlasSet;
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
			if (blockState != null && blockState.is(block)) {
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
				StateDefinition<Block, BlockState> stateDefinition = (StateDefinition)Optional.ofNullable((StateDefinition)STATIC_DEFINITIONS.get(resourceLocation2))
					.orElseGet(() -> Registry.BLOCK.get(resourceLocation2).getStateDefinition());
				this.context.setDefinition(stateDefinition);
				List<Property<?>> list = ImmutableList.copyOf(this.blockColors.getColoringProperties(stateDefinition.getOwner()));
				ImmutableList<BlockState> immutableList = stateDefinition.getPossibleStates();
				Map<ModelResourceLocation, BlockState> map = Maps.<ModelResourceLocation, BlockState>newHashMap();
				immutableList.forEach(blockState -> map.put(BlockModelShaper.stateToModelLocation(resourceLocation2, blockState), blockState));
				Map<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>> map2 = Maps.<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>>newHashMap(
					
				);
				ResourceLocation resourceLocation3 = new ResourceLocation(resourceLocation.getNamespace(), "blockstates/" + resourceLocation.getPath() + ".json");
				UnbakedModel unbakedModel = (UnbakedModel)this.unbakedCache.get(MISSING_MODEL_LOCATION);
				ModelBakery.ModelGroupKey modelGroupKey = new ModelBakery.ModelGroupKey(ImmutableList.of(unbakedModel), ImmutableList.of());
				Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> pair = Pair.of(unbakedModel, (Supplier)() -> modelGroupKey);

				try {
					for(Pair<String, BlockModelDefinition> pair2 : this.resourceManager
						.getResourceStack(resourceLocation3)
						.stream()
						.map(
							resource -> {
								try {
									Reader reader = resource.openAsReader();
		
									Pair var4x;
									try {
										var4x = Pair.of(resource.sourcePackId(), BlockModelDefinition.fromStream(this.context, reader));
									} catch (Throwable var7xx) {
										if (reader != null) {
											try {
												reader.close();
											} catch (Throwable var6xx) {
												var7xx.addSuppressed(var6xx);
											}
										}
		
										throw var7xx;
									}
		
									if (reader != null) {
										reader.close();
									}
		
									return var4x;
								} catch (Exception var8xx) {
									throw new ModelBakery.BlockStateDefinitionException(
										String.format(
											"Exception loading blockstate definition: '%s' in resourcepack: '%s': %s", resourceLocation3, resource.sourcePackId(), var8xx.getMessage()
										)
									);
								}
							}
						)
						.toList()) {
						BlockModelDefinition blockModelDefinition = pair2.getSecond();
						Map<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>> map3 = Maps.<BlockState, Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>>>newIdentityHashMap(
							
						);
						MultiPart multiPart;
						if (blockModelDefinition.isMultiPart()) {
							multiPart = blockModelDefinition.getMultiPart();
							immutableList.forEach(
								blockState -> map3.put(blockState, Pair.of(multiPart, (Supplier)() -> ModelBakery.ModelGroupKey.create(blockState, multiPart, list)))
							);
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
													Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> pair2xxx = (Pair)map3.put(
														blockState, Pair.of(multiVariant, (Supplier)() -> ModelBakery.ModelGroupKey.create(blockState, multiVariant, list))
													);
													if (pair2xxx != null && pair2xxx.getFirst() != multiPart) {
														map3.put(blockState, pair);
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
						map2.putAll(map3);
					}
				} catch (ModelBakery.BlockStateDefinitionException var24) {
					throw var24;
				} catch (Exception var25) {
					throw new ModelBakery.BlockStateDefinitionException(String.format("Exception loading blockstate definition: '%s': %s", resourceLocation3, var25));
				} finally {
					Map<ModelBakery.ModelGroupKey, Set<BlockState>> map5 = Maps.newHashMap();
					map.forEach((modelResourceLocationx, blockState) -> {
						Pair<UnbakedModel, Supplier<ModelBakery.ModelGroupKey>> pair2xx = (Pair)map2.get(blockState);
						if (pair2xx == null) {
							LOGGER.warn("Exception loading blockstate definition: '{}' missing model for variant: '{}'", resourceLocation3, modelResourceLocationx);
							pair2xx = pair;
						}

						this.cacheAndQueueDependencies(modelResourceLocationx, pair2xx.getFirst());

						try {
							ModelBakery.ModelGroupKey modelGroupKeyxx = (ModelBakery.ModelGroupKey)((Supplier)pair2xx.getSecond()).get();
							((Set)map5.computeIfAbsent(modelGroupKeyxx, modelGroupKeyxx -> Sets.newIdentityHashSet())).add(blockState);
						} catch (Exception var9xx) {
							LOGGER.warn("Exception evaluating model definition: '{}'", modelResourceLocationx, var9xx);
						}
					});
					map5.forEach((modelGroupKeyx, set) -> {
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
		Triple<ResourceLocation, Transformation, Boolean> triple = Triple.of(resourceLocation, modelState.getRotation(), modelState.isUvLocked());
		if (this.bakedCache.containsKey(triple)) {
			return (BakedModel)this.bakedCache.get(triple);
		} else if (this.atlasSet == null) {
			throw new IllegalStateException("bake called too early");
		} else {
			UnbakedModel unbakedModel = this.getModel(resourceLocation);
			if (unbakedModel instanceof BlockModel blockModel && blockModel.getRootModel() == GENERATION_MARKER) {
				return ITEM_MODEL_GENERATOR.generateBlockModel(this.atlasSet::getSprite, blockModel)
					.bake(this, blockModel, this.atlasSet::getSprite, modelState, resourceLocation, false);
			}

			BakedModel bakedModel = unbakedModel.bake(this, this.atlasSet::getSprite, modelState, resourceLocation);
			this.bakedCache.put(triple, bakedModel);
			return bakedModel;
		}
	}

	private BlockModel loadBlockModel(ResourceLocation resourceLocation) throws IOException {
		Reader reader = null;

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
					reader = this.resourceManager.openAsReader(new ResourceLocation(resourceLocation.getNamespace(), "models/" + resourceLocation.getPath() + ".json"));
				}

				blockModel = BlockModel.fromStream(reader);
				blockModel.name = resourceLocation.toString();
				return blockModel;
			}

			blockModel = BLOCK_ENTITY_MARKER;
		} finally {
			IOUtils.closeQuietly(reader);
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
