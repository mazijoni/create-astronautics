package com.createastronautics.client.ponder;

import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * The Oxygen Fan's ponder scene. The schematic it plays on ({@code assets/createastronautics/ponder/
 * oxygen_fan.nbt}) is a small glass room with the fan outside its west wall, the fan's nozzle plugged
 * through that wall, and a door in the south wall.
 *
 * The text here is only what Ponder shows in its own editing mode - in game it's read from the
 * {@code createastronautics.ponder.oxygen_fan.*} keys in the lang file, which have to be kept in step with
 * it by hand (text_1, text_2, ... in the order the {@code text(...)} calls appear below).
 */
public final class OxygenFanPonder {
    private OxygenFanPonder() {}

    public static void sealedRoom(SceneBuilder sceneBuilder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(sceneBuilder);
        scene.title("oxygen_fan", "Keeping a room breathable with the Oxygen Fan");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos motor = util.grid().at(0, 1, 3);
        BlockPos fan = util.grid().at(1, 1, 3);
        BlockPos nozzle = util.grid().at(2, 1, 3);
        BlockPos pipe = util.grid().at(1, 2, 3);
        BlockPos tank = util.grid().at(1, 3, 3);
        BlockPos doorLower = util.grid().at(4, 1, 5);
        BlockPos doorUpper = util.grid().at(4, 2, 5);

        Selection room = util.select().fromTo(2, 1, 1, 6, 3, 5).substract(util.select().position(nozzle));
        Selection drive = util.select().fromTo(motor, fan);
        Selection door = util.select().fromTo(doorLower, doorUpper);
        Vec3 roomCentre = util.vector().centerOf(4, 2, 3);

        scene.world().showSection(room, Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("On the Moon and in Deep Space there is no air to breathe, even indoors")
                .pointAt(roomCentre)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().showSection(util.select().position(fan), Direction.EAST);
        scene.idle(5);
        scene.world().showSection(util.select().position(nozzle), Direction.EAST);
        scene.idle(5);
        scene.world().showSection(util.select().position(motor), Direction.EAST);
        scene.idle(10);
        scene.world().setKineticSpeed(drive, 64);

        scene.overlay().showText(80)
                .text("An Oxygen Fan with a Nozzle on its outlet fills the sealed room the Nozzle sits in")
                .pointAt(util.vector().blockSurface(nozzle, Direction.UP))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(pipe, tank), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("It needs Oxygen piped into the port on its side to do that")
                .pointAt(util.vector().centerOf(pipe))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);
        emitMist(scene, 60);
        scene.idle(60);

        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("While it runs, nobody inside the room needs a space suit")
                .pointAt(roomCentre)
                .placeNearTarget()
                .attachKeyFrame();
        emitMist(scene, 90);
        scene.idle(90);

        scene.world().setKineticSpeed(drive, 256);
        scene.overlay().showText(90)
                .text("Spinning it faster lets it fill a bigger room, and a bigger room uses more Oxygen")
                .pointAt(util.vector().topOf(fan))
                .placeNearTarget()
                .attachKeyFrame();
        emitMist(scene, 100);
        scene.idle(100);

        scene.world().modifyBlocks(door, state -> state.setValue(BlockStateProperties.OPEN, true), false);
        ParticleEmitter gust = scene.effects().simpleParticleEmitter(ParticleTypes.POOF, new Vec3(0.0, 0.0, 0.15));
        scene.effects().emitParticles(util.vector().centerOf(doorLower), gust, 3, 20);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("Any opening to the outside lets all of the air escape")
                .pointAt(util.vector().centerOf(doorUpper))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().modifyBlocks(door, state -> state.setValue(BlockStateProperties.OPEN, false), false);
        scene.idle(10);
        emitMist(scene, 80);
        scene.overlay().showText(90)
                .text("Engineer's Goggles show how much room the Fan can fill, and how long its Oxygen will last")
                .pointAt(util.vector().blockSurface(fan, Direction.SOUTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.markAsFinished();
    }

    /** Scatters the same mist a real sealed room shows across the room's 3x3x2 interior for {@code ticks}. */
    private static void emitMist(CreateSceneBuilder scene, int ticks) {
        ParticleEmitter mist = scene.effects().simpleParticleEmitter(ModParticleTypes.OXYGEN_MIST.get(), new Vec3(0.0, 0.01, 0.0));
        for (int x = 3; x <= 5; x++) {
            for (int z = 2; z <= 4; z++) {
                scene.effects().emitParticles(new Vec3(x + 0.5, 1.0 + ((x + z) % 2) * 0.9 + 0.5, z + 0.5), mist, 0.15F, ticks);
            }
        }
    }
}
