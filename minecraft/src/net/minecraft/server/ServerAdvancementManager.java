package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.slf4j.Logger;

public class ServerAdvancementManager extends SimpleJsonResourceReloadListener {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().create();
	private Map<ResourceLocation, AdvancementHolder> advancements = Map.of();
	private AdvancementTree tree = new AdvancementTree();
	private final LootDataManager lootData;

	public ServerAdvancementManager(LootDataManager lootDataManager) {
		super(GSON, "advancements");
		this.lootData = lootDataManager;
	}

	protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
		Builder<ResourceLocation, AdvancementHolder> builder = ImmutableMap.builder();
		map.forEach((resourceLocation, jsonElement) -> {
			try {
				Advancement advancement = Util.getOrThrow(Advancement.CODEC.parse(JsonOps.INSTANCE, jsonElement), JsonParseException::new);
				this.validate(resourceLocation, advancement);
				builder.put(resourceLocation, new AdvancementHolder(resourceLocation, advancement));
			} catch (Exception var5xx) {
				LOGGER.error("Parsing error loading custom advancement {}: {}", resourceLocation, var5xx.getMessage());
			}
		});
		this.advancements = builder.buildOrThrow();
		AdvancementTree advancementTree = new AdvancementTree();
		advancementTree.addAll(this.advancements.values());

		for(AdvancementNode advancementNode : advancementTree.roots()) {
			if (advancementNode.holder().value().display().isPresent()) {
				TreeNodePosition.run(advancementNode);
			}
		}

		this.tree = advancementTree;
	}

	private void validate(ResourceLocation resourceLocation, Advancement advancement) {
		ProblemReporter.Collector collector = new ProblemReporter.Collector();
		advancement.validate(collector, this.lootData);
		Multimap<String, String> multimap = collector.get();
		if (!multimap.isEmpty()) {
			String string = (String)multimap.asMap()
				.entrySet()
				.stream()
				.map(entry -> "  at " + (String)entry.getKey() + ": " + String.join("; ", (Iterable)entry.getValue()))
				.collect(Collectors.joining("\n"));
			LOGGER.warn("Found validation problems in advancement {}: \n{}", resourceLocation, string);
		}
	}

	@Nullable
	public AdvancementHolder get(ResourceLocation resourceLocation) {
		return (AdvancementHolder)this.advancements.get(resourceLocation);
	}

	public AdvancementTree tree() {
		return this.tree;
	}

	public Collection<AdvancementHolder> getAllAdvancements() {
		return this.advancements.values();
	}
}
