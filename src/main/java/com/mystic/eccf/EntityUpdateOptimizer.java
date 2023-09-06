package com.mystic.eccf;

import java.util.*;

import net.minecraft.entity.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import com.falsepattern.lib.compat.ChunkPos;
import com.mystic.eccf.config.ECCFeccfConfig;
import com.mystic.eccf.proxy.CommonProxy;

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

    private static Map<Integer, Class<? extends Entity>> entityIdMap;
    // Le nombre maximum d'entités dans un chunk avant que l'optimisation ne soit déclenchée
    private Map<ChunkPos, Integer> entityCountMap;
    private Set<Integer> pendingRemovalEntities;
    @SidedProxy(clientSide = Tags.CLIENTPROXY, serverSide = Tags.SERVERPROXY)
    public static CommonProxy proxy;
    public static EntityUpdateOptimizer instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Code de pré-initialisation pour votre mod
        entityCountMap = new HashMap<>();
        pendingRemovalEntities = new HashSet<>();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);

        entityIdMap = new HashMap<Integer, Class<? extends Entity>>();

        // Parcourez tous les ID d'entités possibles
        for (int entityId = 0; entityId < 32000; entityId++) {
            Class<? extends Entity> entityClass = EntityList.getClassFromID(entityId);

            if (entityClass != null) {
                entityIdMap.put(entityId, entityClass);
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.entity;
        World world = entity.worldObj;

        if (!world.isRemote && shouldOptimizeEntity(entity) && !entity.isDead) {
            ChunkPos chunkPos = getChunkPos(entity);
            int entityCount = getEntityCountInChunk(chunkPos, (WorldServer) world);

            if (entityCount > ECCFeccfConfig.maxEntitiesPerChunk) {
                unloadAndReloadChunk(chunkPos, (WorldServer) world);
                entity.setDead();
                pendingRemovalEntities.add(entity.getEntityId());
            } else {
                incrementEntityCount(chunkPos, (WorldServer) world);
            }
        }
    }

    private boolean shouldOptimizeEntity(Entity entity) {
        int entityId = EntityRegistry.findGlobalUniqueEntityId();

        if (entityIdMap.containsKey(entityId)) {
            Class<? extends Entity> entityClass = entityIdMap.get(entityId);
            String entityName = EntityList.getEntityString(entity);

            if (entityName != null && entity instanceof EntityLiving) {
                for (String blacklistId : ECCFeccfConfig.entityBlacklistIds) {
                    if (blacklistId.equals(entityName)) {
                        return false;
                    }
                }
            }

            return true;
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
            Chunk chunk = worldServer.getChunkFromBlockCoords(chunkPos.x, chunkPos.z);

            if (chunk != null) {
                chunk.removeEntity(entity);
                worldServer.getEntityTracker()
                    .removeEntityFromAllTrackingPlayers(entity);
                chunk.setChunkModified(); // Mettre à jour le chunk
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

        Chunk chunk = world.getChunkFromBlockCoords(chunkX, chunkZ);
        if (chunk != null) {
            int entityCount = 0;

            for (List<Entity> entityList : chunk.entityLists) {
                for (Entity entity : entityList) {
                    if (entity != null) {
                        entityCount++;
                    }
                }
            }

            return entityCount;
        }
        return 0;
    }

    private void unloadAndReloadChunk(ChunkPos chunkPos, WorldServer world) {
        int x = chunkPos.x;
        int z = chunkPos.z;

        world.getChunkProvider()
            .loadChunk(x, z);
    }
}
