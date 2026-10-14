package com.createastronautics.client.ponder;

import com.createastronautics.block.OxygenDetectorBlock;
import com.createastronautics.block.OxygenDetectorDisplaySource;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

/**
 * The Oxygen Detector's ponder scene. The schematic it plays on ({@code assets/createastronautics/ponder/
 * oxygen_detector.nbt}) has the detector facing the viewer (north), two wires and a lamp running out of its
 * west side, a Display Link on its top, and a 3x2 Display Board behind the redstone with a creative motor
 * turning it - all on the side of the scene Ponder's default camera looks at.
 *
 * Nothing here actually simulates oxygen - the ponder world has no oxygen fans running in it - so every state
 * the real block would go through is set by hand, with oxygen mist shown in front of the detector's face
 * whenever it's "detecting".
 *
 * The text here is only what Ponder shows in its own editing mode - in game it's read from the
 * {@code createastronautics.ponder.oxygen_detector.*} keys in the lang file, which have to be kept in step
 * with it by hand (text_1, text_2, ... in the order the {@code text(...)} calls appear below).
 */
public final class OxygenDetectorPonder {
    private OxygenDetectorPonder() {}

    public static void detectingOxygen(SceneBuilder sceneBuilder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(sceneBuilder);
        scene.title("oxygen_detector", "Detecting oxygen with the Oxygen Detector");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos detector = util.grid().at(3, 1, 2);
        BlockPos checked = util.grid().at(3, 1, 1);
        BlockPos lamp = util.grid().at(0, 1, 2);
        BlockPos link = util.grid().at(3, 2, 2);
        BlockPos boardController = util.grid().at(2, 2, 4);
        Selection redstone = util.select().fromTo(0, 1, 2, 2, 1, 2);
        Selection board = util.select().fromTo(0, 1, 4, 3, 2, 4);
        Vec3 face = util.vector().blockSurface(detector, Direction.NORTH);

        scene.world().showSection(util.select().position(detector), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("The Oxygen Detector checks for breathable air in the block in front of its face")
                .pointAt(face)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(redstone, Direction.EAST);
        scene.idle(15);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("With no oxygen there, its lights stay red and it gives no signal")
                .pointAt(face)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        setDetecting(scene, util, detector, true);
        emitOxygen(scene, util, checked, 100);
        scene.effects().indicateRedstone(detector);
        scene.overlay().showText(90)
                .colored(PonderPalette.BLUE)
                .text("Once there is oxygen in front of it, its lights turn blue and it gives a constant Redstone signal")
                .pointAt(face)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        emitOxygen(scene, util, checked, 80);
        scene.overlay().showText(70)
                .text("The signal comes out of every side except its face")
                .pointAt(util.vector().topOf(lamp))
                .placeNearTarget();
        scene.idle(80);

        setDetecting(scene, util, detector, false);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("As soon as the oxygen is gone, the signal turns off again")
                .pointAt(face)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().showSection(board, Direction.DOWN);
        scene.world().setKineticSpeed(board, 32);
        scene.idle(15);
        scene.world().showSection(util.select().position(link), Direction.DOWN);
        scene.idle(10);
        scene.world().flashDisplayLink(link);
        scene.world().setDisplayBoardText(boardController, 0, OxygenDetectorDisplaySource.statusLine(false));
        scene.overlay().showText(90)
                .text("A Display Link on it shows whether there is oxygen on a Display Board")
                .pointAt(util.vector().topOf(link))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);

        setDetecting(scene, util, detector, true);
        emitOxygen(scene, util, checked, 80);
        scene.world().flashDisplayLink(link);
        scene.world().setDisplayBoardText(boardController, 0, OxygenDetectorDisplaySource.statusLine(true));
        scene.idle(100);

        scene.markAsFinished();
    }

    /** Puts the detector, its wires and the lamp into the states the real ones would be in. */
    private static void setDetecting(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos detector, boolean detecting) {
        scene.world().modifyBlock(detector, state -> state.setValue(OxygenDetectorBlock.DETECTING, detecting), false);
        // Full strength out of the detector, one weaker per wire along the line.
        scene.world().modifyBlock(util.grid().at(2, 1, 2), state -> state.setValue(RedStoneWireBlock.POWER, detecting ? 15 : 0), false);
        scene.world().modifyBlock(util.grid().at(1, 1, 2), state -> state.setValue(RedStoneWireBlock.POWER, detecting ? 14 : 0), false);
        scene.world().modifyBlock(util.grid().at(0, 1, 2), state -> state.setValue(RedstoneLampBlock.LIT, detecting), false);
    }

    /** The same mist a real sealed room shows, here just in the block the detector is checking. */
    private static void emitOxygen(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos checked, int ticks) {
        ParticleEmitter mist = scene.effects().simpleParticleEmitter(ModParticleTypes.OXYGEN_MIST.get(), new Vec3(0.0, 0.01, 0.0));
        scene.effects().emitParticles(util.vector().centerOf(checked), mist, 0.3F, ticks);
    }
}
