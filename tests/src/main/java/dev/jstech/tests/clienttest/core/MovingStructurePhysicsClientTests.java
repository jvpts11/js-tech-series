/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.diagnostic.Diagnostics;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.physics.ForceSum;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.tests.TestPhysics;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The physics of moving structures as a player sees it, every frame watched: crates let go fall, one landing on
 * another, and rest drawn exactly where the server has them, never jumping on the way and never shaking at rest; a
 * raft floats in a pool and carries the player standing on it, the player's feet on its deck; and a cart driven by
 * its wheels rolls across the player's sight, smoothly and only forward. Screenshots show each.
 */
public final class MovingStructurePhysicsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final int WAIT = 400;
    private static final int SETTLE = 10;
    /* How close, in blocks, the player's game draws a structure at rest to where the server has it. */
    private static final double RESTS = 1.0e-3;
    /* How close a moving structure is drawn to where the server has it: within a tick's going. */
    private static final double FOLLOWS = 0.5;
    /* A frame drawing a structure this many times faster than the frames on both its sides is a jump. */
    private static final double JUMP = 4;
    /* Two frames nearer in the game's time than this, in ticks, are taken this far apart. */
    private static final double LEAST_TICKS = 0.05;
    /* The least step a jump is told by, in blocks: smaller ones are a slow structure's, never a jump. */
    private static final double LEAST_JUMP = 0.08;
    /* How many frames at rest are looked at for shaking, and how little a structure at rest may move in them. */
    private static final int REST_FRAMES = 20;
    private static final double STILL = 1.0e-3;
    private static final int CLEAR = 12_000;
    /* The scene: from here to there, cleared before and after. */
    private static final BlockPos SCENE_FROM = new BlockPos(-6, 2, -16);
    private static final BlockPos SCENE_TO = new BlockPos(6, 12, -4);

    private MovingStructurePhysicsClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void crates_fallLandAndRestAsTheServerHasThem(final ClientTestContext ctx) {
        final List<MovingStructure> crates = new ArrayList<>();
        final List<FrameWatch> watched = new ArrayList<>();
        ctx.afterTest(() -> {
            watched.forEach(FrameWatch::stop);
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            // Dropped from a few blocks: a crate of planks falling far enough onto stone breaks the planks the blow is
            // too much for, as a real one cracks, and this is about how they are drawn, not what breaks them.
            crates.add(crate(ctx, new BlockPos(-3, 2, -9), new BlockPos(-2, 2, -8), Blocks.STONE));
            crates.add(crate(ctx, new BlockPos(-3, 5, -9), new BlockPos(-2, 6, -8), Blocks.OAK_PLANKS));
            crates.add(crate(ctx, new BlockPos(2, 5, -9), new BlockPos(3, 6, -8), Blocks.OAK_PLANKS));
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 5, -9)))))
                .thenWaitUntil(() -> drawn(crates), WAIT, "the crates to reach the player's game and be built")
                .then(0, () -> {
                    for (final MovingStructure crate : crates) {
                        watched.add(FrameWatch.of(crate));
                    }
                })
                .thenServer(0, level -> crates.forEach(crate -> crate.move(PhysicsMotion.AT_REST)))
                .thenWaitUntilServer(level -> crates.stream().allMatch(MovingStructurePhysicsClientTests::asleep),
                        WAIT, "the crates to fall, land and come to rest", level -> crates.stream()
                                .map(crate -> crate.worldBox().toString()).toList().toString())
                .then(SETTLE + REST_FRAMES, () -> {
                    for (int i = 0; i < crates.size(); i++) {
                        same(ctx, crates.get(i), RESTS, "crate " + i + " at rest");
                        watched.get(i).assertSmooth(ctx, "crate " + i);
                        watched.get(i).assertStillAtTheEnd(ctx, "crate " + i);
                    }
                    ctx.assertTrue(Math.abs(crates.get(1).worldBox().minY - crates.get(0).worldBox().maxY) < 0.03,
                            "the crate that fell on the other rests on it: " + crates.get(1).worldBox() + " on "
                                    + crates.get(0).worldBox() + "; its physics timed " + Diagnostics.timings()
                                    .summaries().stream().filter(each -> each.section()
                                            .equals(StructurePhysics.TIMED.toString())).findFirst().orElse(null));
                })
                .thenScreenshot(0, "physics_crates_rested");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void raft_floatsAndCarriesThePlayerStandingOnIt(final ClientTestContext ctx) {
        final MovingStructure[] raft = new MovingStructure[1];
        final FrameWatch[] watched = new FrameWatch[1];
        ctx.afterTest(() -> {
            if (watched[0] != null) {
                watched[0].stop();
            }
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            for (int y = 2; y <= 4; y++) {
                for (int x = -5; x <= 5; x++) {
                    for (int z = -15; z <= -5; z++) {
                        final boolean wall = x == -5 || x == 5 || z == -15 || z == -5;
                        level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), (wall ? Blocks.GLASS
                                : Blocks.WATER).defaultBlockState());
                    }
                }
            }
            raft[0] = crate(ctx, new BlockPos(-1, 6, -11), new BlockPos(1, 6, -9), Blocks.OAK_PLANKS);
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> drawn(List.of(raft[0])), WAIT, "the raft to reach the player's game")
                .thenServer(0, level -> raft[0].move(PhysicsMotion.AT_REST))
                .thenWaitUntilServer(level -> floats(raft[0]), WAIT, "the raft to fall in and float",
                        level -> raft[0].worldBox() + " going " + Arrays.toString(raft[0].velocity()))
                .then(SETTLE, () -> {
                    final ClientStructure seen = ClientMovingStructures.byPlot(raft[0].plot());
                    final Vec3 middle = seen.worldBox().getCenter();
                    ctx.player().setPos(middle.x, seen.worldBox().maxY + 0.3, middle.z);
                    ctx.player().setDeltaMovement(Vec3.ZERO);
                    ctx.lookAt(new Vec3(middle.x, seen.worldBox().minY, middle.z - 3));
                })
                .thenWaitUntil(() -> StructureFooting.standingOn(ctx.player()) instanceof ClientStructure, WAIT,
                        "the player to stand on the raft")
                .then(0, () -> watched[0] = FrameWatch.of(raft[0]))
                .then(SETTLE * 6, () -> {
                    final IStructureBody under = StructureFooting.standingOn(ctx.player());
                    ctx.assertTrue(under instanceof ClientStructure, "the player still stands on the raft afloat");
                    final ClientStructure seen = (ClientStructure) under;
                    ctx.assertTrue(Math.abs(ctx.player().getY() - seen.worldBox().maxY) < 0.1,
                            "the player's feet on its deck: " + ctx.player().getY() + " on " + seen.worldBox());
                    same(ctx, raft[0], FOLLOWS, "the raft afloat");
                    watched[0].assertSmooth(ctx, "the raft");
                    final double surface = ctx.abs(new BlockPos(0, 4, 0)).getY() + 8.0 / 9;
                    ctx.assertTrue(raft[0].worldBox().minY < surface && raft[0].worldBox().minY > surface - 1,
                            "it floats in the water, partly under: " + raft[0].worldBox() + ", the surface at "
                                    + surface);
                })
                .thenScreenshot(0, "physics_raft_ridden");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void cart_drivesAndRollsSmoothlyForward(final ClientTestContext ctx) {
        final MovingStructure[] cart = new MovingStructure[1];
        final FrameWatch[] watched = new FrameWatch[1];
        final double[] parked = new double[1];
        ctx.afterTest(() -> {
            if (watched[0] != null) {
                watched[0].stop();
            }
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            // Wheels facing west, rolling across the player's sight; driven by the levers behind them, east.
            for (final BlockPos wheel : List.of(new BlockPos(2, 2, -10), new BlockPos(4, 2, -10),
                    new BlockPos(2, 2, -8), new BlockPos(4, 2, -8))) {
                level.setBlockAndUpdate(ctx.abs(wheel), TestPhysics.WHEEL.get().defaultBlockState()
                        .setValue(DirectionalBlock.FACING, Direction.WEST));
            }
            for (int x = 2; x <= 4; x++) {
                for (int z = -10; z <= -8; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 3, z)), TestPhysics.LIGHT.get()
                            .defaultBlockState());
                }
            }
            level.setBlockAndUpdate(ctx.abs(new BlockPos(5, 2, -10)), lever());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(5, 2, -8)), lever());
            cart[0] = MovingStructures.assemble(level, ctx.abs(new BlockPos(2, 2, -10)),
                    ctx.abs(new BlockPos(5, 3, -8))).structure();
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 3, -9)))))
                .thenWaitUntil(() -> drawn(List.of(cart[0])), WAIT, "the cart to reach the player's game")
                .thenServer(0, level -> cart[0].move(PhysicsMotion.AT_REST))
                .thenWaitUntilServer(level -> cart[0].physics() != null
                                && Math.abs(cart[0].physics().body().velocity().y) < 0.1
                                && cart[0].worldBox().minY > ctx.abs(BlockPos.ZERO).getY() + 2.2, WAIT,
                        "the cart to settle on its springs", level -> cart[0].worldBox().toString())
                .thenServer(SETTLE, level -> {
                    parked[0] = cart[0].worldBox().minX;
                    for (final BlockPos at : BlockPos.betweenClosed(cart[0].bounds().minX(), cart[0].bounds().minY(),
                            cart[0].bounds().minZ(), cart[0].bounds().maxX(), cart[0].bounds().maxY(),
                            cart[0].bounds().maxZ())) {
                        final BlockState state = level.getBlockState(at);
                        if (state.is(Blocks.LEVER)) {
                            level.setBlock(at.immutable(), state.setValue(LeverBlock.POWERED, true), Block.UPDATE_ALL);
                        }
                    }
                })
                .then(0, () -> watched[0] = FrameWatch.of(cart[0]))
                .thenScreenshot(SETTLE * 2, "physics_cart_driving")
                .then(SETTLE, () -> {
                    ctx.assertTrue(cart[0].worldBox().minX < parked[0] - 0.2, "driven, the cart rolls west: "
                            + cart[0].worldBox().minX + " from " + parked[0]);
                    same(ctx, cart[0], FOLLOWS, "the cart rolling");
                    watched[0].assertSmooth(ctx, "the cart");
                    watched[0].assertOnlyWest(ctx, "the cart");
                });
    }

    /* Makes the box from {@code from} to {@code to} of {@code block} a structure. */
    private static MovingStructure crate(final ClientTestContext ctx, final BlockPos from, final BlockPos to,
                                         final Block block) {
        final ServerLevel level = ctx.serverLevel();
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(from), ctx.abs(to))) {
            level.setBlockAndUpdate(at, block.defaultBlockState());
        }
        return MovingStructures.assemble(level, ctx.abs(from), ctx.abs(to)).structure();
    }

    /* Whether each of the structures has reached the player's game and been built. */
    private static boolean drawn(final List<MovingStructure> structures) {
        for (final MovingStructure structure : structures) {
            final ClientStructure seen = structure == null ? null : ClientMovingStructures.byPlot(structure.plot());
            if (seen == null || !seen.ready() || seen.meshes().built() == 0 || seen.meshes().waiting()) {
                return false;
            }
        }
        return true;
    }

    /* Asserts the player's game draws the structure within {@code close} of where the server has it, and turned so. */
    private static void same(final ClientTestContext ctx, final MovingStructure structure, final double close,
                             final String what) {
        final StructurePose server = ctx.server().submit(structure::pose).join();
        final ClientStructure seen = ClientMovingStructures.byPlot(structure.plot());
        ctx.assertTrue(seen != null, what + " is on the player's game");
        final StructurePose game = seen.frame().pose();
        ctx.assertTrue(game.distanceTo(server) < close && game.angleTo(server) < Math.max(close, 1.0e-3),
                what + " is drawn where the server has it: " + game + " on the player's game, " + server
                        + " on the server");
    }

    /* Whether the structure moves by its physics and has fallen asleep. */
    private static boolean asleep(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        return physics != null && physics.asleep();
    }

    /* Whether the raft has fallen in and floats, barely going up or down. */
    private static boolean floats(final MovingStructure raft) {
        final StructurePhysics physics = raft.physics();
        return physics != null && physics.forces().of(ForceSum.ForceKind.BUOYANCY).lengthSquared() > 0
                && Math.abs(physics.body().velocity().y) < 0.05;
    }

    private static BlockState lever() {
        return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.EAST).setValue(LeverBlock.POWERED, false);
    }

    /* Clears the weather, so a storm another test brought does not blow the scene about. */
    private static void calm(final ServerLevel level) {
        level.setWeatherParameters(CLEAR, 0, false, false);
    }

    /* Takes every structure away and clears the scene. */
    private static void clear(final ClientTestContext ctx) {
        final ServerLevel level = ctx.serverLevel();
        for (final IStructureBody left : List.copyOf(LevelMovingStructures.of(level).all())) {
            if (left instanceof MovingStructure structure) {
                MovingStructures.takeAway(structure);
            }
        }
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(SCENE_FROM), ctx.abs(SCENE_TO))) {
            if (!level.getBlockState(at).isAir()) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /*
     * Where a structure is drawn, every frame the player's game draws: the place its pivot is drawn at, between its
     * last two poses as the frame falls between ticks.
     */
    private static final class FrameWatch {

        private final int plot;
        private final List<Vec3> drawn = new ArrayList<>();
        /* The game's time each frame was drawn at, in ticks. */
        private final List<Double> times = new ArrayList<>();
        private final Consumer<RenderFrameEvent.Post> listener = this::frame;

        private FrameWatch(final int plotOf) {
            this.plot = plotOf;
        }

        /** Starts watching {@code structure}, every frame. */
        static FrameWatch of(final MovingStructure structure) {
            final FrameWatch watch = new FrameWatch(structure.plot());
            NeoForge.EVENT_BUS.addListener(watch.listener);
            return watch;
        }

        /** Stops watching. */
        void stop() {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        /**
         * Asserts no frame jumped: no frame drawn it going, for the game's time since the frame before, far faster than
         * in the frames on both sides of it. A fall, a landing and a frame the game drew late are all smooth so; a
         * structure snapped to another place is not.
         */
        void assertSmooth(final ClientTestContext ctx, final String what) {
            for (int i = 2; i < drawn.size() - 1; i++) {
                final double step = drawn.get(i).distanceTo(drawn.get(i - 1));
                if (step <= LEAST_JUMP) {
                    continue;
                }
                final double speed = speed(i);
                final double around = Math.max(speed(i - 1), speed(i + 1));
                ctx.assertTrue(speed <= JUMP * around, what + " is drawn without a jump: frame " + i + " of "
                        + drawn.size() + " went " + step + " blocks at " + speed + " a tick, the frames on its sides"
                        + " at " + speed(i - 1) + " and " + speed(i + 1));
            }
        }

        /** Asserts it stood still in its last frames: drawn at rest, it does not shake. */
        void assertStillAtTheEnd(final ClientTestContext ctx, final String what) {
            ctx.assertTrue(drawn.size() > REST_FRAMES, what + " was drawn in " + drawn.size() + " frames");
            final Vec3 last = drawn.getLast();
            double most = 0;
            for (int i = drawn.size() - REST_FRAMES; i < drawn.size(); i++) {
                most = Math.max(most, drawn.get(i).distanceTo(last));
            }
            ctx.assertTrue(most < STILL, what + " is drawn still at rest, not shaking: it moved " + most
                    + " in its last " + REST_FRAMES + " frames");
        }

        /** Asserts it only went west, frame after frame: never drawn going back. */
        void assertOnlyWest(final ClientTestContext ctx, final String what) {
            for (int i = 1; i < drawn.size(); i++) {
                final double back = drawn.get(i).x - drawn.get(i - 1).x;
                ctx.assertTrue(back < STILL, what + " is never drawn going back east: frame " + i + " went "
                        + back + " east");
            }
        }

        /* How fast frame {@code i} drew it going, in blocks a tick of the game's time since the frame before. */
        private double speed(final int i) {
            final double ticks = times.get(i) - times.get(i - 1);
            final double step = drawn.get(i).distanceTo(drawn.get(i - 1));
            return ticks > LEAST_TICKS ? step / ticks : step / LEAST_TICKS;
        }

        private void frame(final RenderFrameEvent.Post event) {
            final ClientStructure seen = ClientMovingStructures.byPlot(plot);
            if (seen == null || Minecraft.getInstance().level == null) {
                return;
            }
            final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            final StructurePose pose = seen.drawnAt(partial);
            drawn.add(new Vec3(pose.x(), pose.y(), pose.z()));
            times.add(Minecraft.getInstance().level.getGameTime() + (double) partial);
        }
    }
}
