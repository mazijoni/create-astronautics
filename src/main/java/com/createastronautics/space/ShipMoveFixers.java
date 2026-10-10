package com.createastronautics.space;

import com.createastronautics.space.fixer.BlockEntityRegistry;
import com.createastronautics.space.fixer.create.BearingFixer;
import com.createastronautics.space.fixer.create.DisplayLinkFixer;
import com.createastronautics.space.fixer.create.MechanicalPistonFixer;
import com.createastronautics.space.fixer.simulated.RopeConnectorFixer;
import com.createastronautics.space.fixer.simulated.SpringFixer;
import net.neoforged.fml.ModList;

import java.util.Set;

/**
 * Registers the block-entity NBT fixers {@link BlockEntityRegistry} needs to keep cross-references (a
 * kinetic block's "Source" pointer, a swivel bearing's linked plate, a rope's block attachments, ...) valid
 * after {@link SubLevelWarper} rebuilds a ship in another dimension. Only registers fixers for mods this
 * mod actually depends on (Create, Create Aeronautics, Create Simulated) - see the mod list in
 * neoforge.mods.toml.
 *
 * Ported from Create-Cosmonautics' "Dimensional Sable" module (dev.egg.DimensionalSable's constructor) -
 * https://github.com/CosmonauticsTeam/Create-Cosmonautics - GPLv3-licensed; see LICENSE.
 */
public final class ShipMoveFixers {
    private ShipMoveFixers() {}

    public static void register() {
        if (ModList.get().isLoaded("simulated")) {
            BlockEntityRegistry.publishCustomFixer("simulated", "spring", new SpringFixer());
            BlockEntityRegistry.publishCustomFixer("simulated", "rope_connector", new RopeConnectorFixer());
            BlockEntityRegistry.publishCustomFixer("simulated", "rope_winch", new RopeConnectorFixer()); // same NBT shape as rope_connector
            BlockEntityRegistry.publishConnectionFixer("simulated", "docking_connector", "OtherConnectorSubLevelId", "OtherConnector");
            BlockEntityRegistry.publishConnectionFixer("simulated", "swivel_bearing", "SubLevelID", "SwivelPlate");
            BlockEntityRegistry.publishConnectionFixer("simulated", "swivel_bearing_link_block", "ParentSubLevelId", "ParentPos");
        }
        if (ModList.get().isLoaded("create")) {
            BlockEntityRegistry.publishCustomFixer("create", "mechanical_bearing", new BearingFixer());
            BlockEntityRegistry.publishCustomFixer("create", "windmill_bearing", new BearingFixer());
            BlockEntityRegistry.publishCustomFixer("create", "clockwork_bearing", new BearingFixer());
            BlockEntityRegistry.publishCustomFixer("create", "mechanical_piston", new MechanicalPistonFixer());
            // Block entities with a "Source" position field pointing at their kinetic network's power source.
            // Copyright (c) The Create Team / The Creators of Create - list taken from Create's own source.
            BlockEntityRegistry.publishCompoundPosFixer("create", Set.of("adjustable_chain_gearshift", "backtank", "belt", "clockwork_bearing", "clutch", "cuckoo_clock",
                    "deployer", "drill", "elevator_pulley", "encased_fan", "flap_display", "fluid_valve", "flywheel",
                    "gantry_pinion", "gearbox", "gearshift", "hand_crank", "hose_pulley", "large_water_wheel", "mechanical_arm",
                    "mechanical_bearing", "mechanical_crafter", "mechanical_mixer", "mechanical_piston", "mechanical_press",
                    "mechanical_pump", "millstone", "powered_shaft", "rope_pulley", "saw", "sequenced_gearshift",
                    "simple_kinetic", "speedometer", "stressometer", "valve_handle", "water_wheel", "weighted_ejector",
                    "windmill_bearing"
            ), Set.of("Source"));

            BlockEntityRegistry.publishPosFixer("create", "belt", Set.of("Controller"));
            BlockEntityRegistry.publishCompoundPosFixer("create", Set.of("item_vault", "fluid_tank"), Set.of("LastKnownPos", "Controller"));
            BlockEntityRegistry.publishCustomFixer("create", "display_link", new DisplayLinkFixer());
            BlockEntityRegistry.publishCompoundPosFixer("create", Set.of("drill", "saw"), Set.of("Breaking"));
            BlockEntityRegistry.publishCompoundPosFixer("create", Set.of("rope_pulley", "elevator_pulley"), Set.of("MirrorChildren"));
            BlockEntityRegistry.publishPosFixer("create", "powered_shaft", Set.of("EnginePos"));
        }
        if (ModList.get().isLoaded("aeronautics")) {
            BlockEntityRegistry.publishCustomFixer("aeronautics", "propeller_bearing", new BearingFixer());
            BlockEntityRegistry.publishCustomFixer("aeronautics", "gyroscopic_propeller_bearing", new BearingFixer());

            BlockEntityRegistry.publishCompoundPosFixer("aeronautics", Set.of("propeller_bearing", "gyroscopic_propeller_bearing",
                    "smart_propeller", "wooden_propeller", "andesite_propeller"
            ), Set.of("Source"));
        }
    }
}
