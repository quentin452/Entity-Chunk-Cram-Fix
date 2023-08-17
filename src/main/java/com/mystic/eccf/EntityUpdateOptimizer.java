package com.mystic.eccf;

import java.util.*;

import net.minecraft.entity.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import com.falsepattern.lib.compat.ChunkPos;
import com.falsepattern.lib.internal.proxy.CommonProxy;
import com.mystic.eccf.config.ECCFConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

@Mod(modid = Tags.MODID, version = Tags.VERSION, name = Tags.MODNAME, acceptedMinecraftVersions = Tags.MCVERSION)
public class EntityUpdateOptimizer {

    // The maximum number of entities in a chunk before optimization is triggered
    private Map<ChunkPos, Integer> entityCountMap;
    private Set<Integer> pendingRemovalEntities;
    @SidedProxy(clientSide = Tags.CLIENTPROXY, serverSide = Tags.SERVERPROXY)
    public static CommonProxy proxy;
    public static Configuration config;
    public static EntityUpdateOptimizer instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Pre-initialization code for your mod
        entityCountMap = new HashMap<>();
        pendingRemovalEntities = new HashSet<>();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.entity;
        World world = entity.worldObj;

        if (!world.isRemote && shouldOptimizeEntity(entity) && !entity.isDead) {
            ChunkPos chunkPos = getChunkPos(entity);
            int entityCount = getEntityCountInChunk(chunkPos, (WorldServer) world);

            if (entityCount > ECCFConfig.maxEntitiesPerChunk) {
                unloadAndReloadChunk(chunkPos, (WorldServer) world);
                entity.setDead();
                pendingRemovalEntities.add(entity.getEntityId());
            } else {
                incrementEntityCount(chunkPos, (WorldServer) world);
            }
        }
    }

    private boolean shouldOptimizeEntity(Entity entity) {
        String entityId = EntityList.getStringFromID(entity.getEntityId());

        if (entityId != null) {
            return entity instanceof EntityLiving && !ECCFConfig.entityBlacklistIds.contains(entityId);
        }

        return false;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            MinecraftServer minecraftServer = FMLCommonHandler.instance()
                .getMinecraftServerInstance();
            for (WorldServer worldServer : minecraftServer.worldServers) {
                if (!worldServer.isRemote) {
                    for (Integer entityId : pendingRemovalEntities) {
                        Entity entity = worldServer.getEntityByID(entityId);
                        if (entity != null && entity.isEntityAlive()) {
                            ChunkPos chunkPos = getChunkPos(entity);
                            removeEntityFromTracker(entity, worldServer, chunkPos);
                        }
                    }
                }
            }
            pendingRemovalEntities.clear();
        }
    }

    private void incrementEntityCount(ChunkPos chunkPos, WorldServer world) {
        int count = getEntityCountInChunk(chunkPos, world);
        entityCountMap.put(chunkPos, count + 1);
    }

    private void removeEntityFromTracker(Entity entity, WorldServer worldServer, ChunkPos chunkPos) {
        EntityRegistry.EntityRegistration registration = EntityRegistry.instance()
            .lookupModSpawn(entity.getClass(), true);
        if (registration != null) {
            IChunkProvider chunkProvider = worldServer.getChunkProvider();
            Chunk chunk = chunkProvider.provideChunk(chunkPos.x, chunkPos.z);

            if (chunk != null) {
                chunk.removeEntity(entity);
                worldServer.getEntityTracker()
                    .removeEntityFromAllTrackingPlayers(entity);
            }
        }
    }

    private ChunkPos getChunkPos(Entity entity) {
        int chunkX = MathHelper.floor_double(entity.posX) >> 4;
        int chunkZ = MathHelper.floor_double(entity.posZ) >> 4;
        return new ChunkPos(chunkX, chunkZ);
    }

    private int getEntityCountInChunk(ChunkPos chunkPos, WorldServer world) {
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        if (chunk != null) {
            int entityCount = 0;

            for (Object entity : chunk.entityLists[0]) {
                if (entity instanceof Entity) {
                    entityCount++;
                }
            }

            return entityCount;
        }
        return 0;
    }

    private void unloadAndReloadChunk(ChunkPos chunkPos, WorldServer world) {

        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();

        int x = chunkPos.x;
        int z = chunkPos.z;

        if (provider.chunkExists(x, z)) {

            Chunk chunk = provider.provideChunk(x, z);

            if (chunk != null) {
                chunk.onChunkUnload();
            }

            provider.loadChunk(x, z);

        }

    }
}
