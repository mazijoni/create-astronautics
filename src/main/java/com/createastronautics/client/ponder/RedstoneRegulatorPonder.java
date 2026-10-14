package com.createastronautics.client.ponder;

import com.createastronautics.block.RedstoneRegulatorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

/**
 * The Redstone Regulator's ponder scene. Its schematic ({@code ponder/redstone_regulator.nbt}) is a single
 * line - lever, wire, regulator, three wires, lamp - so that a strength of 3 just reaches the lamp and a
 * strength of 2 falls one block short.
 *
 * As with every scene here, the in-game text comes from the {@code createastronautics.ponder.
 * redstone_regulator.*} lang keys, numbered in the order the {@code text(...)} calls appear.
 */
public final class RedstoneRegulatorPonder {
    private static final int LEVER_X = 6;
    private static final int INPUT_WIRE_X = 5;
    private static final int REGULATOR_X = 4;
    private static final int[] OUTPUT_WIRE_XS = {3, 2, 1};
    private static final int LAMP_X = 0;
    private static final int Z = 3;

    private RedstoneRegulatorPonder() {}

    public static void signalStrength(SceneBuilder sceneBuilder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(sceneBuilder);
        scene.title("redstone_regulator", "Setting signal strength with the Redstone Regulator");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos lever = util.grid().at(LEVER_X, 1, Z);
        BlockPos regulator = util.grid().at(REGULATOR_X, 1, Z);
        BlockPos lamp = util.grid().at(LAMP_X, 1, Z);
        Vec3 regulatorTop = util.vector().blockSurface(regulator, Direction.DOWN).add(0.0, 3.0 / 16.0, 0.0);

        scene.world().showSection(util.select().fromTo(LAMP_X, 1, Z, LEVER_X, 1, Z), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("A Redstone Regulator turns any signal reaching its back into one fixed strength")
                .pointAt(regulatorTop)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);

        setLine(scene, util, true, 15);
        scene.effects().indicateRedstone(lever);
        scene.idle(40);

        scene.overlay().showControls(regulatorTop, Pointing.DOWN, 60).rightClick();
        scene.overlay().showText(80)
                .text("Hold Right-click on its top to pick that strength, anywhere from 0 to 15")
                .pointAt(regulatorTop)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(50);
        setLine(scene, util, true, 3);
        scene.idle(50);

        setLine(scene, util, true, 2);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("A weaker signal fades out sooner along the wire")
                .pointAt(util.vector().topOf(lamp))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        setLine(scene, util, false, 2);
        scene.effects().indicateRedstone(lever);
        scene.overlay().showText(70)
                .text("With nothing powering its back, it puts out nothing at all")
                .pointAt(regulatorTop)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.markAsFinished();
    }

    /** Puts every block in the line into the state it would really be in for this lever position and regulator setting. */
    private static void setLine(CreateSceneBuilder scene, SceneBuildingUtil util, boolean leverOn, int signal) {
        scene.world().modifyBlock(util.grid().at(LEVER_X, 1, Z), state -> state.setValue(LeverBlock.POWERED, leverOn), false);
        scene.world().modifyBlock(util.grid().at(INPUT_WIRE_X, 1, Z), state -> state.setValue(RedStoneWireBlock.POWER, leverOn ? 15 : 0), false);
        scene.world().modifyBlock(util.grid().at(REGULATOR_X, 1, Z), state -> state
                .setValue(RedstoneRegulatorBlock.POWERED, leverOn)
                .setValue(RedstoneRegulatorBlock.SIGNAL, signal), false);

        int power = leverOn ? signal : 0;
        for (int x : OUTPUT_WIRE_XS) {
            int wirePower = Math.max(power, 0);
            scene.world().modifyBlock(util.grid().at(x, 1, Z), state -> state.setValue(RedStoneWireBlock.POWER, wirePower), false);
            power--;
        }
        // The lamp is lit by the last wire, which is the one `power` was just stepped down past.
        boolean lampLit = power + 1 > 0;
        scene.world().modifyBlock(util.grid().at(LAMP_X, 1, Z), state -> state.setValue(RedstoneLampBlock.LIT, lampLit), false);
    }
}
