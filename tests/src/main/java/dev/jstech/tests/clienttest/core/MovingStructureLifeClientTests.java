/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.client.moving.JointRenderer;
import dev.jstech.core.client.moving.SleepersDrawn;
import dev.jstech.core.client.moving.StructureEffectsClient;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.ZiplineEntity;
import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.tests.TestAboard;
import dev.jstech.tests.TestPhysics;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Living aboard moving structures as a player sees it, every frame watched where it matters: a villager asleep in a
 * bed of a boat turned a quarter lies along the bed as it is drawn, turned with it; the player at a helm steers a
 * hovering craft with the keys they walk with, sitting steady in their seat; a rope from a post to the ground is drawn
 * between its ends, and the player hanging from it rides it down smoothly; an engine's flame is shown while it burns
 * and gone when it goes out; an armour stand on a moving deck is drawn standing steady on it; the piece of a broken
 * bar is drawn where the server has it; and a clamp draws a visitor in smoothly. Screenshots show each.
 */
public final class MovingStructureLifeClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final int WAIT = 400;
    private static final int SETTLE = 10;
    private static final int CLEAR = 12_000;
    /* How close, in blocks, the player's game draws a structure at rest to where the server has it. */
    private static final double RESTS = 1.0e-3;
    /* How far what stands or sits on a moving deck may wander on it, frame to frame, as drawn, in blocks. */
    private static final double STEADY = 0.05;
    /* A frame drawing a thing this many times faster than the frames on both its sides is a jump. */
    private static final double JUMP = 4;
    /* Two frames nearer in the game's time than this, in ticks, are taken this far apart. */
    private static final double LEAST_TICKS = 0.05;
    /* The least step a jump is told by, in blocks: smaller ones are a slow thing's, never a jump. */
    private static final double LEAST_JUMP = 0.08;
    /* The height above a bed's block a sleeper lies at, as the game's own. */
    private static final double LIES = 0.6875;
    /* The scene: from here to there, cleared before and after. */
    private static final BlockPos SCENE_FROM = new BlockPos(-6, 2, -16);
    private static final BlockPos SCENE_TO = new BlockPos(6, 12, -4);

    private MovingStructureLifeClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void sleeper_liesAlongABedAboardTurnedWithIt(final ClientTestContext ctx) {
        final MovingStructure[] boat = new MovingStructure[1];
        final int[] sleeper = {-1};
        final long[] before = new long[1];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx)));
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            for (int x = -1; x <= 0; x++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 2, -9)), Blocks.STONE.defaultBlockState());
            }
            final BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(-1, 3, -9)), foot);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 3, -9)), foot.setValue(BedBlock.PART, BedPart.HEAD));
            boat[0] = assembled(ctx, new BlockPos(-1, 2, -9), new BlockPos(0, 3, -9));
            final StructurePose at = boat[0].pose();
            boat[0].teleport(StructurePose.turned(at.x(), at.y(), at.z(), 0, 1, 0, Math.PI / 2));
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 3, -9)))))
                .thenWaitUntil(() -> drawn(List.of(boat[0])), WAIT, "the boat to reach the player's game")
                // Its blocks come into the yard as the yard's chunks load: the bed is looked for once they did.
                .thenServer(0, level -> {
                    final BlockPos head = find(level, boat[0], state -> state.getBlock() instanceof BedBlock
                            && state.getValue(BedBlock.PART) == BedPart.HEAD);
                    final Villager villager = new Villager(EntityType.VILLAGER, level);
                    villager.setNoAi(true);
                    villager.setPos(Vec3.atBottomCenterOf(ctx.abs(new BlockPos(3, 2, -9))));
                    level.addFreshEntity(villager);
                    villager.startSleeping(head);
                    sleeper[0] = villager.getId();
                })
                .thenWaitUntil(() -> living(sleeper[0]) != null && living(sleeper[0]).isSleeping(), WAIT,
                        "the villager to be asleep on the player's game")
                .then(0, () -> before[0] = SleepersDrawn.turned())
                .then(SETTLE, () -> {
                    ctx.assertTrue(SleepersDrawn.turned() > before[0],
                            "the sleeper is drawn turned with the bed aboard, frame after frame");
                    final LivingEntity seen = living(sleeper[0]);
                    final float turn = SleepersDrawn.turnOf(seen, 1F);
                    ctx.assertTrue(Math.abs(Math.abs(turn) - 90) < 1, "turned a quarter, as the boat is: " + turn);
                    final ClientStructure structure = ClientMovingStructures.byPlot(boat[0].plot());
                    final BlockPos head = seen.getSleepingPos().orElseThrow();
                    final Vec3 lying = worldOf(structure.frame(), new Vec3(head.getX() + 0.5, head.getY() + LIES,
                            head.getZ() + 0.5));
                    ctx.assertTrue(seen.position().distanceTo(lying) < 0.2, "and lies in the bed where it is drawn: "
                            + seen.position() + " for " + lying);
                })
                .thenScreenshot(0, "life_sleeper_turned");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void helm_theKeysOfThePlayerAtItSteerTheCraft(final ClientTestContext ctx) {
        final MovingStructure[] craft = new MovingStructure[1];
        final double[] parked = new double[1];
        final double[] northmost = {Double.MAX_VALUE};
        final OffsetWatch[] sitting = new OffsetWatch[1];
        ctx.afterTest(() -> {
            ctx.mc().options.keyUp.setDown(false);
            ctx.mc().options.keyDown.setDown(false);
            if (sitting[0] != null) {
                sitting[0].stop();
            }
            ctx.server().execute(() -> {
                ctx.serverPlayer().stopRiding();
                clear(ctx);
            });
        });
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            // In a row along the way the player looks, north: an engine at each end, levitators either side of
            // the helm holding the craft up through its middle.
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 6, -11)), TestPhysics.THRUSTER.get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.NORTH));
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 6, -10)), TestPhysics.HOVER.get().defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 6, -9)), TestAboard.HELM.get().defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 6, -8)), TestPhysics.HOVER.get().defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 6, -7)), TestPhysics.THRUSTER.get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.SOUTH));
            craft[0] = assembled(ctx, new BlockPos(0, 6, -11), new BlockPos(0, 6, -7));
            craft[0].move(PhysicsMotion.AT_REST);
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> drawn(List.of(craft[0])), WAIT, "the craft to reach the player's game")
                .thenServer(SETTLE, level -> {
                    final BlockPos helm = find(level, craft[0], state -> state.is(TestAboard.HELM.get()));
                    ctx.assertTrue(MovingStructures.sit(ctx.serverPlayer(), helm), "the player sits at the helm");
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof StructureSeatEntity, WAIT,
                        "the player to sit at the helm on their game")
                .then(SETTLE, () -> {
                    parked[0] = ctx.server().submit(() -> craft[0].worldBox().getCenter().z).join();
                    sitting[0] = OffsetWatch.of(craft[0], () -> ctx.player().getId());
                    ctx.mc().options.keyUp.setDown(true);
                })
                // What the keys hold reaches the server a few ticks on, and goes on as long after they are let go;
                // in the air nothing stops the craft but its engines, so each key is held until it did its work.
                .thenWaitUntilServer(level -> craft[0].physics().body().velocity().z < -1, WAIT,
                        "held forward, the craft to go forward, north", level -> craft[0].physics().body().velocity()
                                .toString())
                .then(0, () -> {
                    ctx.mc().options.keyUp.setDown(false);
                    ctx.mc().options.keyDown.setDown(true);
                })
                .thenScreenshot(0, "life_helm_steering")
                .thenWaitUntilServer(level -> {
                    northmost[0] = Math.min(northmost[0], craft[0].worldBox().getCenter().z);
                    return craft[0].physics().body().velocity().z > -0.3;
                }, WAIT, "held back, the craft to slow", level -> craft[0].physics().body().velocity().toString())
                .then(0, () -> ctx.mc().options.keyDown.setDown(false))
                .then(SETTLE, () -> {
                    ctx.assertTrue(northmost[0] < parked[0] - 0.3, "it went forward, north, as far as "
                            + northmost[0] + " from " + parked[0]);
                    sitting[0].stop();
                    sitting[0].assertSteady(ctx, "the player at the helm", STEADY);
                });
    }

    @ClientTest(timeoutTicks = 2400)
    public static void rope_isDrawnBetweenItsEndsAndRiddenDownSmoothly(final ClientTestContext ctx) {
        final MovingStructure[] post = new MovingStructure[1];
        final UUID[] rope = new UUID[1];
        final Vec3[] ends = new Vec3[2];
        final PlayerWatch[] riding = new PlayerWatch[1];
        final double[] start = new double[1];
        ctx.afterTest(() -> {
            if (riding[0] != null) {
                riding[0].stop();
            }
            ctx.server().execute(() -> {
                ctx.serverPlayer().stopRiding();
                clear(ctx);
            });
        });
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(-4, 10, -9)), Blocks.STONE.defaultBlockState());
            post[0] = assembled(ctx, new BlockPos(-4, 10, -9), new BlockPos(-4, 10, -9));
            ends[0] = Vec3.atCenterOf(ctx.abs(new BlockPos(-4, 10, -9))).add(0.5, 0, 0);
            ends[1] = Vec3.atCenterOf(ctx.abs(new BlockPos(4, 3, -9)));
            rope[0] = StructureJoints.join(level, new JointKind.Rope(ends[0].distanceTo(ends[1]),
                            JointKind.RopeLook.ROPE, Optional.empty()), StructureJoints.Anchor.on(post[0], ends[0],
                            new Vec3(0, 1, 0)), StructureJoints.Anchor.world(ends[1], new Vec3(0, 1, 0)),
                    Optional.empty()).id();
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(ends[0].add(ends[1]).scale(0.5)))
                .thenWaitUntil(() -> drawn(List.of(post[0])) && seenJoint(rope[0]) != null, WAIT,
                        "the post and its rope to reach the player's game")
                .then(SETTLE, () -> {
                    final StructureJoint seen = seenJoint(rope[0]);
                    final Vec3 a = JointRenderer.drawnEnd(ctx.mc().level, seen.a(), 1F);
                    final Vec3 b = JointRenderer.drawnEnd(ctx.mc().level, seen.b(), 1F);
                    ctx.assertTrue(a != null && a.distanceTo(ends[0]) < 0.01, "the rope is drawn from the post: " + a
                            + " for " + ends[0]);
                    ctx.assertTrue(b != null && b.distanceTo(ends[1]) < 0.01, "to the ground: " + b + " for "
                            + ends[1]);
                })
                .thenScreenshot(0, "life_rope_drawn")
                .thenServer(0, level -> {
                    final Vec3 hold = ZiplineEntity.along(ends[0], ends[1], 0.1);
                    ctx.serverPlayer().teleportTo(level, hold.x, hold.y - ctx.serverPlayer().getEyeHeight(), hold.z,
                            0, 0);
                    ctx.assertTrue(MovingStructures.zipline(ctx.serverPlayer(), rope[0], 0.1),
                            "the player takes hold of the rope");
                })
                .thenWaitUntil(() -> ctx.player().getVehicle() instanceof ZiplineEntity, WAIT,
                        "the player to hang from the rope on their game")
                .then(0, () -> {
                    start[0] = ctx.player().getX();
                    riding[0] = PlayerWatch.start();
                })
                .thenScreenshot(10, "life_zipline_riding")
                .thenWaitUntil(() -> ctx.player().getVehicle() == null, WAIT, "the player to ride down and let go")
                .then(0, () -> {
                    riding[0].stop();
                    ctx.assertTrue(ctx.player().getX() > start[0] + 4, "having ridden it down: "
                            + ctx.player().position());
                    riding[0].assertSmooth(ctx, "the player riding the rope");
                });
    }

    @ClientTest(timeoutTicks = 2400)
    public static void engine_flameIsShownWhileItBurnsAndGoneWhenItGoesOut(final ClientTestContext ctx) {
        final MovingStructure[] rocket = new MovingStructure[1];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx)));
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            // Too heavy for its engine, it stays on its pad, burning.
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 2, -9)), Blocks.IRON_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 3, -9)), TestPhysics.THRUSTER.get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.UP));
            level.setBlockAndUpdate(ctx.abs(new BlockPos(1, 3, -9)), lever(false));
            rocket[0] = assembled(ctx, new BlockPos(0, 2, -9), new BlockPos(1, 3, -9));
            rocket[0].move(PhysicsMotion.AT_REST);
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 5, -9)))))
                .thenWaitUntil(() -> drawn(List.of(rocket[0])), WAIT, "the rocket to reach the player's game")
                .thenServer(SETTLE, level -> setLever(level, rocket[0], true))
                .thenWaitUntil(() -> StructureEffectsClient.held(rocket[0].entity().getId()) == 1, WAIT,
                        "its flame to be shown on the player's game")
                .thenScreenshot(SETTLE * 2, "life_engine_burning")
                .then(0, () -> ctx.assertTrue(StructureEffectsClient.held(rocket[0].entity().getId()) == 1,
                        "a second on, its flame is shown still"))
                .thenServer(0, level -> setLever(level, rocket[0], false))
                .thenWaitUntil(() -> StructureEffectsClient.held(rocket[0].entity().getId()) == 0, WAIT,
                        "its flame to go when its engine goes out");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void riders_standSteadyOnAMovingDeck(final ClientTestContext ctx) {
        final MovingStructure[] deck = new MovingStructure[1];
        final int[] rider = {-1};
        final OffsetWatch[] standing = new OffsetWatch[1];
        ctx.afterTest(() -> {
            if (standing[0] != null) {
                standing[0].stop();
            }
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            for (int x = -3; x <= -1; x++) {
                for (int z = -10; z <= -8; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 2, z)), Blocks.STONE.defaultBlockState());
                }
            }
            deck[0] = assembled(ctx, new BlockPos(-3, 2, -10), new BlockPos(-1, 2, -8));
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 3, -9)))))
                .thenWaitUntil(() -> drawn(List.of(deck[0])), WAIT, "the deck to reach the player's game")
                // An armour stand: it stands where it is put, as a creature with no mind of its own does not, and falls
                // and is carried as anything is; the test world is peaceful, and takes a monster away at once.
                .thenServer(0, level -> {
                    final ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
                    stand.setPos(Vec3.atBottomCenterOf(ctx.abs(new BlockPos(-2, 3, -9))));
                    level.addFreshEntity(stand);
                    rider[0] = stand.getId();
                })
                .thenWaitUntil(() -> entity(rider[0]) != null, WAIT,
                        "the stand on the deck to reach the player's game")
                .thenServer(SETTLE * 2, level -> {
                    final StructurePose at = deck[0].pose();
                    deck[0].move(new TowardMotion(at.movedTo(at.x() + 4, at.y(), at.z()), 2, 0));
                })
                .thenWaitUntil(() -> moving(deck[0]), WAIT, "the deck to get going on the player's game")
                .then(0, () -> standing[0] = OffsetWatch.of(deck[0], () -> rider[0]))
                .thenScreenshot(SETTLE, "life_rider_on_deck")
                .thenWaitUntilServer(level -> deck[0].resting(), WAIT, "the deck to go its four blocks",
                        level -> deck[0].worldBox().toString())
                .then(0, () -> {
                    standing[0].stop();
                    standing[0].assertSteady(ctx, "the stand on the deck", STEADY);
                    final Entity stand = entity(rider[0]);
                    final AABB box = ClientMovingStructures.byPlot(deck[0].plot()).worldBox();
                    ctx.assertTrue(stand.getX() > box.minX && stand.getX() < box.maxX,
                            "carried with it: " + stand.position() + " on " + box);
                });
    }

    @ClientTest(timeoutTicks = 2400)
    public static void parting_thePieceOfABrokenBarIsDrawnWhereTheServerHasIt(final ClientTestContext ctx) {
        final MovingStructure[] bar = new MovingStructure[1];
        final MovingStructure[] piece = new MovingStructure[1];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx)));
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            for (int x = -2; x <= 2; x++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 5, -9)), Blocks.STONE.defaultBlockState());
            }
            bar[0] = assembled(ctx, new BlockPos(-2, 5, -9), new BlockPos(2, 5, -9));
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 4, -9)))))
                .thenWaitUntil(() -> drawn(List.of(bar[0])), WAIT, "the bar to reach the player's game")
                .thenServer(SETTLE, level -> {
                    final int middle = (bar[0].bounds().minX() + bar[0].bounds().maxX()) / 2;
                    level.setBlockAndUpdate(new BlockPos(middle, bar[0].bounds().minY(), bar[0].bounds().minZ()),
                            Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntilServer(level -> {
                    piece[0] = other(level, bar[0]);
                    return piece[0] != null && piece[0].physics() != null && piece[0].physics().asleep();
                }, WAIT, "the piece to part, fall and come to rest", level -> String.valueOf(piece[0]))
                .thenWaitUntil(() -> drawn(List.of(bar[0], piece[0])), WAIT, "both to be drawn on the player's game")
                .then(SETTLE, () -> {
                    same(ctx, bar[0], RESTS, "the bar left");
                    same(ctx, piece[0], RESTS, "the piece fallen");
                })
                .thenScreenshot(0, "life_bar_parted");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void docking_aClampDrawsTheVisitorInSmoothly(final ClientTestContext ctx) {
        final MovingStructure[] host = new MovingStructure[1];
        final MovingStructure[] visitor = new MovingStructure[1];
        final StructureWatch[] watched = new StructureWatch[1];
        ctx.afterTest(() -> {
            if (watched[0] != null) {
                watched[0].stop();
            }
            ctx.server().execute(() -> clear(ctx));
        });
        ctx.thenServer(0, level -> {
            clear(ctx);
            calm(level);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(-2, 3, -9)), TestAboard.CLAMP.get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.EAST));
            level.setBlockAndUpdate(ctx.abs(new BlockPos(-3, 3, -9)), Blocks.STONE.defaultBlockState());
            host[0] = assembled(ctx, new BlockPos(-3, 3, -9), new BlockPos(-2, 3, -9));
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 3, -9)), TestAboard.CLAMP.get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.WEST));
            visitor[0] = assembled(ctx, new BlockPos(0, 3, -9), new BlockPos(0, 3, -9));
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(-1, 3, -9)))))
                .thenWaitUntil(() -> drawn(List.of(host[0], visitor[0])), WAIT,
                        "both structures to reach the player's game")
                .then(0, () -> watched[0] = StructureWatch.of(visitor[0]))
                // The host's clamp closes on its signal: a block of redstone set beside it, aboard.
                .thenServer(0, level -> level.setBlockAndUpdate(find(level, host[0],
                        state -> state.is(Blocks.STONE)), Blocks.REDSTONE_BLOCK.defaultBlockState()))
                .thenWaitUntilServer(level -> visitor[0].mount() != null, WAIT, "the clamp to hold the visitor",
                        level -> visitor[0].worldBox().toString())
                .thenWaitUntilServer(level -> visitor[0].resting(), WAIT, "the visitor to be drawn in",
                        level -> visitor[0].worldBox().toString())
                .then(SETTLE, () -> {
                    watched[0].stop();
                    same(ctx, visitor[0], RESTS, "the visitor docked");
                    watched[0].assertSmooth(ctx, "the visitor drawn in");
                })
                .thenScreenshot(0, "life_docked");
    }

    /* Makes every block of the box from {@code from} to {@code to} a structure. */
    private static MovingStructure assembled(final ClientTestContext ctx, final BlockPos from, final BlockPos to) {
        return MovingStructures.assemble(ctx.serverLevel(), ctx.abs(from), ctx.abs(to)).structure();
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

    /* Whether the player's game draws the structure going. */
    private static boolean moving(final MovingStructure structure) {
        final ClientStructure seen = ClientMovingStructures.byPlot(structure.plot());
        return seen != null && !seen.frame().pose().equals(seen.previousFrame().pose());
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

    /* The entity of id {@code id} on the player's game, or null. */
    private static Entity entity(final int id) {
        return Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(id);
    }

    /* The living entity of id {@code id} on the player's game, or null. */
    private static LivingEntity living(final int id) {
        return entity(id) instanceof LivingEntity seen ? seen : null;
    }

    /* The joint named {@code id} as the player's game has it, or null. */
    private static StructureJoint seenJoint(final UUID id) {
        for (final StructureJoint joint : StructureJoints.seen()) {
            if (joint.id().equals(id)) {
                return joint;
            }
        }
        return null;
    }

    /* The structure loaded in the level that is not {@code one}, or null. */
    private static MovingStructure other(final ServerLevel level, final MovingStructure one) {
        for (final IStructureBody body : LevelMovingStructures.of(level).all()) {
            if (body != one && body instanceof MovingStructure structure && !structure.entity().isRemoved()) {
                return structure;
            }
        }
        return null;
    }

    /* Where the yard's place {@code place} is in the world, for a structure standing at {@code frame}. */
    private static Vec3 worldOf(final StructureFrame frame, final Vec3 place) {
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /* The first block of the structure's yard that is so, or null. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        for (final BlockPos at : BlockPos.betweenClosed(structure.bounds().minX(), structure.bounds().minY(),
                structure.bounds().minZ(), structure.bounds().maxX(), structure.bounds().maxY(),
                structure.bounds().maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                return at.immutable();
            }
        }
        return null;
    }

    /* Lights the lever aboard {@code structure}, or puts it out. */
    private static void setLever(final ServerLevel level, final MovingStructure structure, final boolean lit) {
        final BlockPos lever = find(level, structure, state -> state.is(Blocks.LEVER));
        level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, lit), Block.UPDATE_ALL);
    }

    private static BlockState lever(final boolean lit) {
        return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.EAST).setValue(LeverBlock.POWERED, lit);
    }

    /* Clears the weather, so a storm another test brought does not blow the scene about. */
    private static void calm(final ServerLevel level) {
        level.setWeatherParameters(CLEAR, 0, false, false);
    }

    /* Takes every structure, joint and creature away and clears the scene. */
    private static void clear(final ClientTestContext ctx) {
        final ServerLevel level = ctx.serverLevel();
        for (final StructureJoint joint : List.copyOf(StructureJoints.all(level))) {
            StructureJoints.remove(level, joint.id());
        }
        for (final IStructureBody left : List.copyOf(LevelMovingStructures.of(level).all())) {
            if (left instanceof MovingStructure structure) {
                MovingStructures.takeAway(structure);
            }
        }
        final AABB scene = new AABB(Vec3.atLowerCornerOf(ctx.abs(SCENE_FROM)), Vec3.atLowerCornerOf(ctx.abs(SCENE_TO)))
                .inflate(8);
        for (final LivingEntity left : level.getEntitiesOfClass(LivingEntity.class, scene,
                any -> !(any instanceof Player))) {
            left.discard();
        }
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(SCENE_FROM), ctx.abs(SCENE_TO))) {
            if (!level.getBlockState(at).isAir()) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /* How fast frame {@code i} drew a thing going, in blocks a tick of the game's time since the frame before. */
    private static double speed(final List<Vec3> drawn, final List<Double> times, final int i) {
        final double ticks = times.get(i) - times.get(i - 1);
        final double step = drawn.get(i).distanceTo(drawn.get(i - 1));
        return ticks > LEAST_TICKS ? step / ticks : step / LEAST_TICKS;
    }

    /*
     * Asserts no frame of {@code drawn} jumped: none drawn going, for the game's time since the frame before, far
     * faster than in the frames on both its sides.
     */
    private static void assertNoJump(final ClientTestContext ctx, final List<Vec3> drawn, final List<Double> times,
                                     final String what) {
        ctx.assertTrue(drawn.size() > 3, what + " was drawn in " + drawn.size() + " frames");
        for (int i = 2; i < drawn.size() - 1; i++) {
            final double step = drawn.get(i).distanceTo(drawn.get(i - 1));
            if (step <= LEAST_JUMP) {
                continue;
            }
            final double speed = speed(drawn, times, i);
            final double around = Math.max(speed(drawn, times, i - 1), speed(drawn, times, i + 1));
            ctx.assertTrue(speed <= JUMP * around, what + " is drawn without a jump: frame " + i + " of "
                    + drawn.size() + " went " + step + " blocks at " + speed + " a tick, the frames on its sides at "
                    + speed(drawn, times, i - 1) + " and " + speed(drawn, times, i + 1));
        }
    }

    /*
     * Where something standing or sitting on a structure is drawn on it, every frame the player's game draws: its
     * place as drawn, between its last two ticks, in the yard of the structure as drawn then.
     */
    private static final class OffsetWatch {

        private final int plot;
        private final IntSupplier watched;
        private final List<Vec3> onIt = new ArrayList<>();
        private final List<Double> times = new ArrayList<>();
        private final Consumer<RenderFrameEvent.Post> listener = this::frame;

        private OffsetWatch(final int plotOf, final IntSupplier entity) {
            this.plot = plotOf;
            this.watched = entity;
        }

        /** Starts watching the entity {@code entity} names on {@code structure}, every frame. */
        static OffsetWatch of(final MovingStructure structure, final IntSupplier entity) {
            final OffsetWatch watch = new OffsetWatch(structure.plot(), entity);
            NeoForge.EVENT_BUS.addListener(watch.listener);
            return watch;
        }

        /** Stops watching. */
        void stop() {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        /** Asserts it kept to one place on the structure, as drawn, within {@code most} blocks. */
        void assertSteady(final ClientTestContext ctx, final String what, final double most) {
            ctx.assertTrue(onIt.size() > 3, what + " was drawn in " + onIt.size() + " frames");
            final Vec3 first = onIt.getFirst();
            double furthest = 0;
            int at = 0;
            for (int i = 0; i < onIt.size(); i++) {
                if (onIt.get(i).distanceTo(first) > furthest) {
                    furthest = onIt.get(i).distanceTo(first);
                    at = i;
                }
            }
            ctx.assertTrue(furthest < most, what + " is drawn steady on it, frame after frame: it wandered "
                    + furthest + " blocks on it in " + onIt.size() + " frames, furthest at frame " + at + " (tick "
                    + times.get(at) + "): " + around(at));
        }

        /* Where it was on it at frame {@code at} and the frames beside, for a failure to tell. */
        private String around(final int at) {
            final StringBuilder said = new StringBuilder("first ").append(onIt.getFirst());
            for (int i = Math.max(0, at - 2); i <= Math.min(onIt.size() - 1, at + 2); i++) {
                said.append("; ").append(i).append(" at ").append(times.get(i)).append(' ').append(onIt.get(i));
            }
            return said.toString();
        }

        private void frame(final RenderFrameEvent.Post event) {
            final ClientStructure seen = ClientMovingStructures.byPlot(plot);
            final Entity entity = entity(watched.getAsInt());
            if (seen == null || entity == null) {
                return;
            }
            final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            final StructureFrame frame = new StructureFrame(seen.drawnAt(partial), seen.pivotX(), seen.pivotY(),
                    seen.pivotZ());
            final Vec3 at = entity.getPosition(partial);
            onIt.add(new Vec3(frame.yardX(at.x, at.y, at.z), frame.yardY(at.x, at.y, at.z),
                    frame.yardZ(at.x, at.y, at.z)));
            times.add(Minecraft.getInstance().level.getGameTime() + (double) partial);
        }
    }

    /* Where the player is drawn, every frame the player's game draws while they ride something. */
    private static final class PlayerWatch {

        private final List<Vec3> drawn = new ArrayList<>();
        private final List<Double> times = new ArrayList<>();
        private final Consumer<RenderFrameEvent.Post> listener = this::frame;

        private PlayerWatch() {
        }

        /** Starts watching the player, every frame. */
        static PlayerWatch start() {
            final PlayerWatch watch = new PlayerWatch();
            NeoForge.EVENT_BUS.addListener(watch.listener);
            return watch;
        }

        /** Stops watching. */
        void stop() {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        /** Asserts no frame jumped. */
        void assertSmooth(final ClientTestContext ctx, final String what) {
            assertNoJump(ctx, drawn, times, what);
        }

        private void frame(final RenderFrameEvent.Post event) {
            final Minecraft game = Minecraft.getInstance();
            if (game.player == null || game.level == null || !game.player.isPassenger()) {
                return;
            }
            final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            drawn.add(game.player.getPosition(partial));
            times.add(game.level.getGameTime() + (double) partial);
        }
    }

    /* Where a structure is drawn, every frame the player's game draws: the place its pivot is drawn at. */
    private static final class StructureWatch {

        private final int plot;
        private final List<Vec3> drawn = new ArrayList<>();
        private final List<Double> times = new ArrayList<>();
        private final Consumer<RenderFrameEvent.Post> listener = this::frame;

        private StructureWatch(final int plotOf) {
            this.plot = plotOf;
        }

        /** Starts watching {@code structure}, every frame. */
        static StructureWatch of(final MovingStructure structure) {
            final StructureWatch watch = new StructureWatch(structure.plot());
            NeoForge.EVENT_BUS.addListener(watch.listener);
            return watch;
        }

        /** Stops watching. */
        void stop() {
            NeoForge.EVENT_BUS.unregister(listener);
        }

        /** Asserts no frame jumped. */
        void assertSmooth(final ClientTestContext ctx, final String what) {
            assertNoJump(ctx, drawn, times, what);
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
