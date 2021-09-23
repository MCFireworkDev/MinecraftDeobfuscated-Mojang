package net.minecraft.world.level.levelgen.structure.pieces;

import java.lang.runtime.ObjectMethods;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public final class StructurePieceSerializationContext extends Record {
	private final ResourceManager resourceManager;
	private final RegistryAccess registryAccess;
	private final StructureManager structureManager;

	public StructurePieceSerializationContext(ResourceManager resourceManager, RegistryAccess registryAccess, StructureManager structureManager) {
		this.resourceManager = resourceManager;
		this.registryAccess = registryAccess;
		this.structureManager = structureManager;
	}

	public static StructurePieceSerializationContext fromLevel(ServerLevel serverLevel) {
		MinecraftServer minecraftServer = serverLevel.getServer();
		return new StructurePieceSerializationContext(minecraftServer.getResourceManager(), minecraftServer.registryAccess(), minecraftServer.getStructureManager());
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",StructurePieceSerializationContext,"resourceManager;registryAccess;structureManager",StructurePieceSerializationContext::resourceManager,StructurePieceSerializationContext::registryAccess,StructurePieceSerializationContext::structureManager>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",StructurePieceSerializationContext,"resourceManager;registryAccess;structureManager",StructurePieceSerializationContext::resourceManager,StructurePieceSerializationContext::registryAccess,StructurePieceSerializationContext::structureManager>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",StructurePieceSerializationContext,"resourceManager;registryAccess;structureManager",StructurePieceSerializationContext::resourceManager,StructurePieceSerializationContext::registryAccess,StructurePieceSerializationContext::structureManager>(
			this, object
		);
	}

	public ResourceManager resourceManager() {
		return this.resourceManager;
	}

	public RegistryAccess registryAccess() {
		return this.registryAccess;
	}

	public StructureManager structureManager() {
		return this.structureManager;
	}
}
