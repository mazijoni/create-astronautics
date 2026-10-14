package com.createastronautics.space;

import com.createastronautics.CreateAstronautics;
import com.createastronautics.ModDimensions;
import com.createastronautics.config.Config;
import com.createastronautics.network.SeamlessTransitionPayload;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The player-facing way ships travel between the Overworld and Deep Space: any Sable ship that climbs past
 * {@link Config#SPACE_TRANSITION_HEIGHT} in the Overworld is moved into Deep Space, and any ship that drops
 * below {@link Config#DEEP_SPACE_EXIT_HEIGHT} there falls back into the Overworld sky at the same X/Z -
 * carrying everything connected to it and everyone aboard, via {@link SubLevelWarper}. The ship arrives at
 * rest, so anyone standing on deck rather than seated isn't left behind by it.
 *
 * Follows Create-Cosmonautics' SpaceTransitionHandler (dev.devce.rocketnautics.content.physics), minus its
 * orbital simulation - https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID)
public final class SpaceTransitionHandler {
    // Players this close to a ship's bounds count as aboard it - generous, since a fast-moving ship's deck
    // can be a few blocks away from where someone standing on it was last ticked.
    private static final double PLAYER_CAPTURE_MARGIN = 10.0;


    private SpaceTransitionHandler() {}

    public static void init() {
        SableEventPlatform.INSTANCE.onPhysicsTick(SpaceTransitionHandler::onPhysicsTick);
    }


    private static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        ServerLevel level = physicsSystem.getLevel();
        boolean ascending;
        if (level.dimension() == Level.OVERWORLD) {
            ascending = true;
        } else if (level.dimension() == ModDimensions.DEEP_SPACE) {
            ascending = false;
        } else {
            return;
        }

        ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);
        ServerLevel destination = level.getServer().getLevel(ascending ? ModDimensions.DEEP_SPACE : Level.OVERWORLD);
        if (container == null || destination == null) {
            return;
        }

        double boundary = ascending ? Config.SPACE_TRANSITION_HEIGHT.get() : Config.DEEP_SPACE_EXIT_HEIGHT.get();
        double safeOffset = Config.TRANSITION_SAFE_OFFSET.get();
        double arrivalY = ascending
                ? Config.DEEP_SPACE_EXIT_HEIGHT.get() + safeOffset
                : Config.SPACE_TRANSITION_HEIGHT.get() - safeOffset;

        // Snapshotted by id rather than iterated directly - warping removes the ship (and everything
        // connected to it) from this container mid-loop.
        List<UUID> shipIds = container.getAllSubLevels().stream().map(SubLevel::getUniqueId).toList();
        for (UUID id : shipIds) {
            if (!(container.getSubLevel(id) instanceof ServerSubLevel ship) || ship.isRemoved()) {
                continue;
            }

            double y = ship.logicalPose().position().y();
            if (ascending ? y <= boundary : y >= boundary) {
                continue;
            }

            RigidBodyHandle handle = physicsSystem.getPhysicsHandle(ship);
            if (handle == null || !handle.isValid()) {
                continue;
            }

            try {
                transfer(ship, level, destination, arrivalY);
            } catch (Exception e) {
                CreateAstronautics.LOGGER.error("Failed to move ship {} from {} to {}", id,
                        level.dimension().location(), destination.dimension().location(), e);
            }
        }
    }

    private static void transfer(ServerSubLevel ship, ServerLevel source, ServerLevel destination, double arrivalY) {
        Vector3d shipPos = new Vector3d(ship.logicalPose().position());
        Vector3d arrival = new Vector3d(shipPos.x, arrivalY, shipPos.z);

        // Players go first and are re-seated afterwards, rather than being left to the generic entity pass
        // in SubLevelWarper - that way the seamless-transition packet reaches them before the dimension
        // change does, and whatever seat they were in is remembered across it.
        AABB captureBox = ship.boundingBox().toMojang().inflate(PLAYER_CAPTURE_MARGIN);
        Map<ServerPlayer, UUID> riding = new HashMap<>();
        for (ServerPlayer player : source.getPlayers(candidate -> captureBox.contains(candidate.position()))) {
            PacketDistributor.sendToPlayer(player, new SeamlessTransitionPayload(true));
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                riding.put(player, vehicle.getUUID());
            }

            Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(source, player.position());
            player.teleportTo(destination,
                    arrival.x + (worldPos.x - shipPos.x),
                    arrival.y + (worldPos.y - shipPos.y),
                    arrival.z + (worldPos.z - shipPos.z),
                    player.getYRot(), player.getXRot());
            player.resetFallDistance();
        }

        SubLevelWarper.warpSubLevel(ship, destination, arrival);

        riding.forEach((player, vehicleId) -> {
            Entity vehicle = destination.getEntity(vehicleId);
            if (vehicle != null) {
                player.startRiding(vehicle, true);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new SeamlessTransitionPayload(false));
        }
    }

}
