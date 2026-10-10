package com.createastronautics.space;

import com.createastronautics.CreateAstronautics;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Milestone-0 spike command: {@code /astronautics_move_ship <dimension>} moves the Sable ship the
 * commanding player is standing on/in - and everything Sable considers connected to it - into the given
 * dimension, carrying along every entity riding any of it, using {@link SubLevelWarper}. Exists purely to
 * validate that cross-dimension ship reconstruction works at all before anything else in the space-flight
 * feature is built on top of it - not meant to be the player-facing way ships eventually travel to Deep Space.
 */
@EventBusSubscriber(modid = CreateAstronautics.MODID)
public final class ShipDimensionDebugCommands {
    private ShipDimensionDebugCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("astronautics_move_ship")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ShipDimensionDebugCommands::execute)));
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel destination = DimensionArgument.getDimension(context, "dimension");
        ServerLevel source = player.serverLevel();

        if (destination == source) {
            context.getSource().sendFailure(Component.literal("Already in that dimension."));
            return 0;
        }

        ServerSubLevel ship = findShipUnder(player);
        if (ship == null) {
            context.getSource().sendFailure(Component.literal("Not standing on/in a Sable ship."));
            return 0;
        }

        int warped;
        try {
            warped = SubLevelWarper.warpSubLevel(ship, destination);
        } catch (Exception e) {
            CreateAstronautics.LOGGER.error("astronautics_move_ship failed", e);
            context.getSource().sendFailure(Component.literal(
                    "Move threw " + e.getClass().getSimpleName() + " - see server log for details."));
            return 0;
        }

        int warpedCount = warped;
        context.getSource().sendSuccess(() -> Component.literal(
                "Moved " + warpedCount + " sub-level(s) into " + destination.dimension().location()), true);
        return 1;
    }

    /** Covers both standing on deck and sitting in a seat - see {@code SableCompanion#getTrackingOrVehicleSubLevel}. */
    private static ServerSubLevel findShipUnder(ServerPlayer player) {
        SubLevelAccess subLevel = Sable.HELPER.getTrackingOrVehicleSubLevel(player);
        return subLevel instanceof ServerSubLevel serverSubLevel ? serverSubLevel : null;
    }
}
