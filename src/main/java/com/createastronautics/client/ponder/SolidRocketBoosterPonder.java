package com.createastronautics.client.ponder;

import com.createastronautics.block.SolidRocketBoosterBlock;
import com.createastronautics.particle.ModParticleTypes;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.Vec3;

/**
 * The Solid Rocket Booster's ponder scene: a small rocket is assembled on a launch stand, fuelled, lit, and
 * then actually lifts off and accelerates out of view on its exhaust. Its schematic
 * ({@code ponder/solid_rocket_booster.nbt}) is three boosters in a row at y=2 with casing either side of
 * the middle one, two 3x3 layers of coal blocks stacked on top as fuel, and a lever on the front casing.
 *
 * The exhaust is emitted by the scene itself, in step with the rocket - the booster's own particles are
 * sent by the server, which a ponder scene doesn't have.
 */
public final class SolidRocketBoosterPonder {
    private static final int[] BOOSTER_XS = {2, 3, 4};
    private static final int BOOSTER_Y = 2;
    private static final int Z = 3;

    // The climb, as {blocks, ticks} legs - each one covers more ground in the same time than the last, so
    // the rocket visibly accelerates instead of gliding away at a constant speed.
    private static final double[][] CLIMB = {{0.4, 10}, {1.2, 10}, {2.4, 10}, {4.0, 10}, {6.0, 10}, {9.0, 10}};
    private static final int EXHAUST_INTERVAL_TICKS = 2;

    private SolidRocketBoosterPonder() {}

    public static void liftoff(SceneBuilder sceneBuilder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(sceneBuilder);
        scene.title("solid_rocket_booster", "Lifting off with Solid Rocket Boosters");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.85F);
        scene.setSceneOffsetY(-1.5F);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos lever = util.grid().at(3, 2, 1);
        BlockPos middleBooster = util.grid().at(3, BOOSTER_Y, Z);
        BlockPos frontFuel = util.grid().at(3, 3, 2);
        BlockPos burntFuel = util.grid().at(2, 4, 2);
        Selection groundUnderNozzles = util.select().fromTo(2, 0, Z, 4, 0, Z);
        Selection stand = util.select().fromTo(3, 1, 2, 3, 1, 4);
        Selection boosters = util.select().fromTo(2, BOOSTER_Y, Z, 4, BOOSTER_Y, Z);
        Selection engineSection = util.select().fromTo(2, BOOSTER_Y, 2, 4, BOOSTER_Y, 4);

        scene.world().showSection(stand, Direction.DOWN);
        scene.idle(10);

        // Everything that flies is one independent section, built up piece by piece, so it can be moved as
        // a whole later while the stand stays behind.
        ElementLink<WorldSectionElement> rocket = scene.world().showIndependentSection(engineSection, Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(80)
                .text("Solid Rocket Boosters push whatever physics structure they are built into")
                .pointAt(util.vector().blockSurface(middleBooster, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSectionAndMerge(util.select().fromTo(2, 3, 2, 4, 3, 4), Direction.DOWN, rocket);
        scene.idle(8);
        scene.world().showSectionAndMerge(util.select().fromTo(2, 4, 2, 4, 4, 4), Direction.DOWN, rocket);
        scene.idle(15);

        scene.overlay().showControls(util.vector().blockSurface(frontFuel, Direction.NORTH), Pointing.RIGHT, 60)
                .rightClick()
                .withItem(new ItemStack(Items.COAL_BLOCK));
        scene.overlay().showText(90)
                .text("Fuel is built on, not put in: stack Blocks of Coal on its intake, up to 3 wide and 4 high")
                .pointAt(util.vector().blockSurface(frontFuel, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSectionAndMerge(util.select().position(lever), Direction.SOUTH, rocket);
        scene.idle(15);
        scene.world().modifyBlock(lever, state -> state.setValue(LeverBlock.POWERED, true), false);
        scene.effects().indicateRedstone(lever);
        scene.world().modifyBlocks(boosters, state -> state.setValue(SolidRocketBoosterBlock.POWERED, true), false);
        scene.overlay().showText(60)
                .text("A Redstone signal ignites it - and every other Booster touching a lit one")
                .pointAt(util.vector().centerOf(lever))
                .placeNearTarget()
                .attachKeyFrame();

        // Held on the stand for a while, exhaust washing out across the pad: long enough for the ground to
        // cook and the first fuel block to burn away.
        for (int tick = 0; tick < 150; tick += EXHAUST_INTERVAL_TICKS) {
            emitExhaust(scene, 0.0);
            if (tick % 6 == 0) {
                emitPadSmoke(scene);
            }
            if (tick == 50) {
                scene.world().replaceBlocks(groundUnderNozzles, Blocks.MAGMA_BLOCK.defaultBlockState(), true);
            }
            if (tick == 70) {
                scene.overlay().showText(70)
                        .colored(PonderPalette.RED)
                        .text("Exhaust this close turns the ground to Magma - bars and grates let it pass through instead")
                        .pointAt(util.vector().topOf(3, 0, Z))
                        .placeNearTarget()
                        .attachKeyFrame();
            }
            if (tick == 110) {
                scene.world().destroyBlock(burntFuel);
            }
            scene.idle(EXHAUST_INTERVAL_TICKS);
        }

        scene.addKeyframe();
        double climbed = 0.0;
        for (double[] leg : CLIMB) {
            double distance = leg[0];
            int ticks = (int) leg[1];
            scene.world().moveSection(rocket, new Vec3(0.0, distance, 0.0), ticks);
            for (int tick = 0; tick < ticks; tick += EXHAUST_INTERVAL_TICKS) {
                emitExhaust(scene, climbed + distance * tick / ticks);
                scene.idle(EXHAUST_INTERVAL_TICKS);
            }
            climbed += distance;
        }
        scene.world().hideIndependentSection(rocket, Direction.UP);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The stack burns away block by block from the top, until it runs out or the signal stops")
                .pointAt(util.vector().centerOf(3, 3, Z))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    /** A burst of plume from under each booster, {@code climbed} blocks above where the rocket started. */
    private static void emitExhaust(CreateSceneBuilder scene, double climbed) {
        ParticleEmitter plume = scene.effects().simpleParticleEmitter(ModParticleTypes.ROCKET_PLUME.get(), new Vec3(0.0, -0.35, 0.0));
        for (int x : BOOSTER_XS) {
            scene.effects().emitParticles(new Vec3(x + 0.5, BOOSTER_Y - 0.1 + climbed, Z + 0.5), plume, 3.0F, EXHAUST_INTERVAL_TICKS);
        }
    }

    /** Smoke rolling outward along the ground from under the nozzles. */
    private static void emitPadSmoke(CreateSceneBuilder scene) {
        Vec3 pad = new Vec3(3.5, 1.2, Z + 0.5);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vec3 outward = new Vec3(direction.getStepX() * 0.12, 0.01, direction.getStepZ() * 0.12);
            scene.effects().emitParticles(pad, scene.effects().simpleParticleEmitter(ParticleTypes.CAMPFIRE_COSY_SMOKE, outward), 1.0F, 1);
        }
    }
}
