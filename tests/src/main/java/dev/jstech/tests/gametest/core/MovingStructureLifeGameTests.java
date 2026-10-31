/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureAutopilot;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureEffectsPayload;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureHelmPayload;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.StructureSync;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.ZiplineEntity;
import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestAboard;
import dev.jstech.tests.TestPhysics;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Living aboard moving structures, in a real world: a player sits on a seat aboard and is carried with it, and a seat
 * taken is refused to another and gone once left; a sitter crosses with it to another dimension still sitting, the
 * pilot's hold carried too; on a seat a formed model makes a couch, they lie as a sleeper does,
 * their head to the model's head side as it is turned, and stand once up; at a helm, the controls they hold fire the
 * engines pushing the way they ask, and are let go a second after they stop; an autopilot flies a hovering craft to a place and holds it
 * there; an arrow loosed from a moving deck flies on with it; a villager sleeps in a bed aboard where the bed is in the
 * world and is carried with it, and stands up beside it there; a player coming back to a bed aboard comes back where it
 * is; a rider hanging from a rope rides it down; and an engine burning is told to the players who see it, and told
 * no more once it goes out, and keeps a rocket too heavy for it awake on its pad, its flame shown.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureLifeGameTests {

    private static final String ARENA = "empty";
    private static final String TALL = "tall";
    private static final String BATCH = "jscore_moving_life";
    private static final int CLEAR = 12_000;
    /* How near what is carried keeps to where it is carried to, in blocks. */
    private static final double KEEPS = 0.1;
    /* The height above a bed's block a sleeper lies at, as the game's own. */
    private static final double LIES = 0.6875;
    /* The couch pod's terminal, in the middle of the west side of a pod built facing east, and its seat. */
    private static final BlockPos COUCH_POD = new BlockPos(4, 1, 5);
    private static final BlockPos COUCH = COUCH_POD.offset(1, 1, 0);
    /* Where in the Nether a structure is sent, in blocks up, and how far round it the land is loaded, in chunks. */
    private static final double NETHER_HEIGHT = 180;
    private static final int NETHER_LAND = 3;

    private MovingStructureLifeGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void seats_aPlayerSitsOnASeatAboardAndIsCarriedWithIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 4), TestAboard.SEAT.get());
        final MovingStructure cart = assembled(helper, new BlockPos(2, 2, 4), new BlockPos(3, 2, 4));
        final BlockPos seat = find(level, cart, state -> state.is(TestAboard.SEAT.get()));
        final ServerPlayer rider = rider(helper, new Vec3(3.5, 3, 4.5));
        helper.assertTrue(MovingStructures.sit(rider, seat), "the player sits on the seat aboard");
        helper.assertTrue(rider.getVehicle() instanceof StructureSeatEntity sitting && sitting.lyingHead() == null,
                "in the seat's entity, sat on upright");
        final StructurePose start = cart.pose();
        MovingStructures.move(cart, new TowardMotion(start.movedTo(start.x() + 4, start.y(), start.z()), 4, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cart.resting(), "the cart goes four blocks"))
                .thenIdle(2)
                .thenExecute(() -> {
                    final Vec3 seatAt = worldOf(cart, Vec3.atCenterOf(seat));
                    helper.assertTrue(Math.abs(rider.getX() - seatAt.x) < KEEPS
                            && Math.abs(rider.getZ() - seatAt.z) < KEEPS, "the rider went with the seat: "
                            + rider.position() + " for a seat at " + seatAt);
                    leave(rider);
                    MovingStructures.takeAway(cart);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void seats_aSeatTakenIsRefusedToAnotherAndGoneOnceLeft(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 2, 4), TestAboard.SEAT.get());
        final MovingStructure bench = assembled(helper, new BlockPos(3, 2, 4), new BlockPos(3, 2, 4));
        final BlockPos seat = find(level, bench, state -> state.is(TestAboard.SEAT.get()));
        final ServerPlayer first = rider(helper, new Vec3(3.5, 3, 3.5));
        final ServerPlayer second = rider(helper, new Vec3(3.5, 3, 5.5));
        helper.assertTrue(MovingStructures.sit(first, seat), "the first sits");
        helper.assertTrue(!MovingStructures.sit(second, seat), "the seat taken, the second does not");
        final StructureSeatEntity sitting = (StructureSeatEntity) first.getVehicle();
        first.stopRiding();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(sitting.isRemoved(), "left, the seat's entity is gone"))
                .thenExecute(() -> {
                    helper.assertTrue(MovingStructures.sit(second, seat), "and the second may sit now");
                    leave(first);
                    leave(second);
                    MovingStructures.takeAway(bench);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 6000)
    public static void seats_aSitterCrossesToAnotherDimensionStillSittingThePilotsHoldCarried(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "there is a Nether");
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 4), TestAboard.SEAT.get());
        final MovingStructure cart = assembled(helper, new BlockPos(2, 2, 4), new BlockPos(3, 2, 4));
        final BlockPos seat = find(level, cart, state -> state.is(TestAboard.SEAT.get()));
        final ServerPlayer rider = rider(helper, new Vec3(3.5, 3, 4.5));
        helper.assertTrue(MovingStructures.sit(rider, seat), "the player sits on the seat aboard");
        // The land it goes to loaded first, as the test's own doing: the game's generation takes real seconds.
        final Vec3 there = new Vec3(helper.absolutePos(BlockPos.ZERO).getX() + 0.5, NETHER_HEIGHT,
                helper.absolutePos(BlockPos.ZERO).getZ() + 0.5);
        for (int dx = -NETHER_LAND; dx <= NETHER_LAND; dx++) {
            for (int dz = -NETHER_LAND; dz <= NETHER_LAND; dz++) {
                nether.getChunk((Mth.floor(there.x) >> 4) + dx, (Mth.floor(there.z) >> 4) + dz);
            }
        }
        helper.startSequence()
                .thenExecute(() -> helper.assertTrue(MovingStructures.travel(cart, nether, there).ok(),
                        "the structure is sent to the Nether"))
                .thenWaitUntil(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, cart.id());
                    if (arrived == null) {
                        // Its pilot holds it forward all the while.
                        cart.controls().pilot(level.getGameTime(), Map.of(StructureControls.ControlAxis.FORWARD, 1F),
                                new Vec3(0, 0, -1), new Vec3(0, 1, 0));
                    }
                    helper.assertTrue(arrived != null && arrived.ready(), "the structure arrives in the Nether");
                })
                .thenExecute(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, cart.id());
                    helper.assertTrue(rider.level() == nether && rider.getVehicle() instanceof StructureSeatEntity
                            sitting && sitting.structure() == arrived && nether.getBlockState(sitting.seat())
                            .is(TestAboard.SEAT.get()), "the player crossed with it, sitting on the same seat: in "
                            + rider.level().dimension().location() + " riding " + rider.getVehicle());
                    helper.assertTrue(arrived.controls().value(StructureControls.ControlAxis.FORWARD,
                            nether.getGameTime()) == 1F, "and the pilot's hold went with it");
                    leave(rider);
                    MovingStructures.takeAway(arrived);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void seats_theSitterOfACouchItsFormedModelSaysLiesOnIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // The couch pod, built facing east: the test turbine's pattern with a seat in the middle of its top.
        for (int y = 0; y < 2; y++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = 0; b <= 2; b++) {
                    final BlockPos at = COUCH_POD.offset(b, y, a);
                    if (!at.equals(COUCH_POD)) {
                        helper.setBlock(at, at.equals(COUCH) ? TestAboard.SEAT.get() : Blocks.IRON_BLOCK);
                    }
                }
            }
        }
        helper.setBlock(COUCH_POD, TestAboard.COUCH_POD.get());
        final List<MovingStructure> made = new ArrayList<>();
        final List<ServerPlayer> riders = new ArrayList<>();
        final BlockPos[] couch = new BlockPos[1];
        final Direction[] head = new Direction[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(((TestAboard.CouchPodEntity) helper.getBlockEntity(COUCH_POD))
                        .structure().merged(), "the couch pod forms and merges"))
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(COUCH).getBlock() instanceof FormedPartBlock,
                            "its seat merged into it");
                    made.add(assembled(helper, COUCH_POD.offset(0, 0, -1), COUCH_POD.offset(2, 1, 1)));
                    for (final BlockPos at : BlockPos.betweenClosed(made.getFirst().bounds().minX(),
                            made.getFirst().bounds().minY(), made.getFirst().bounds().minZ(),
                            made.getFirst().bounds().maxX(), made.getFirst().bounds().maxY(),
                            made.getFirst().bounds().maxZ())) {
                        if (level.getBlockEntity(at) instanceof FormedPartBlockEntity part
                                && part.original().is(TestAboard.SEAT.get()) && part.place() != null) {
                            couch[0] = at.immutable();
                            // The model's head side, its south, turned as the pod stands.
                            final int[] way = part.place().turn().apply(0, 0, 1);
                            head[0] = Direction.getNearest(way[0], way[1], way[2]);
                        }
                    }
                    helper.assertTrue(head[0] != null, "the couch is aboard, a block of the pod's model");
                    final ServerPlayer rider = rider(helper, new Vec3(5.5, 4, 5.5));
                    riders.add(rider);
                    helper.assertTrue(MovingStructures.sit(rider, couch[0]), "the player lies down on the couch");
                    final StructureSeatEntity seat = (StructureSeatEntity) rider.getVehicle();
                    helper.assertTrue(seat.lyingHead() == head[0], "their head lies to the model's head side, as"
                            + " the pod is turned: " + seat.lyingHead() + " for " + head[0]);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    final ServerPlayer rider = riders.getFirst();
                    rider.doTick();
                    helper.assertTrue(rider.getPose() == Pose.SLEEPING, "they lie as a sleeper does: "
                            + rider.getPose());
                    helper.assertTrue(rider.getBedOrientation() == head[0], "drawn their head that way: "
                            + rider.getBedOrientation());
                    final BlockPos at = couch[0];
                    final Vec3 eyes = worldOf(made.getFirst(), new Vec3(
                            at.getX() + 0.5 + head[0].getStepX() * BlockAboard.Seat.LYING_REACH,
                            at.getY() + TestAboard.COUCH_HEIGHT + rider.getEyeHeight(),
                            at.getZ() + 0.5 + head[0].getStepZ() * BlockAboard.Seat.LYING_REACH));
                    helper.assertTrue(rider.getEyePosition().distanceTo(eyes) < KEEPS, "their eyes low over the "
                            + "couch, towards their head: " + rider.getEyePosition() + " for " + eyes);
                    rider.stopRiding();
                    rider.doTick();
                    // Neither lying nor asleep, the game has their head up.
                    helper.assertTrue(rider.getPose() != Pose.SLEEPING && rider.getVehicle() == null
                            && rider.getBedOrientation() == Direction.UP, "up again, they stand: " + rider.getPose()
                            + ", their head " + rider.getBedOrientation() + ", riding " + rider.getVehicle());
                    leave(rider);
                    MovingStructures.takeAway(made.getFirst());
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void helm_theControlsHeldFireTheEnginesAndAreLetGoASecondAfter(final GameTestHelper helper) {
        calm(helper);
        final ServerLevel level = helper.getLevel();
        final MovingStructure craft = hoverCraft(helper);
        final BlockPos helm = find(level, craft, state -> state.is(TestAboard.HELM.get()));
        final ServerPlayer pilot = rider(helper, new Vec3(4.5, 7, 7.5));
        helper.assertTrue(MovingStructures.sit(pilot, helm), "the pilot sits at the helm");
        helper.startSequence()
                .thenIdle(5)
                .thenExecuteFor(6, () -> helper.assertTrue(MovingStructures.helm(pilot,
                        StructureHelmPayload.bit(StructureControls.ControlAxis.FORWARD)), "at the helm"))
                .thenExecute(() -> {
                    helper.assertTrue(craft.physics() != null && craft.physics().body().velocity().z < -1,
                            "forward fires the engine looking forward, north: " + craft.physics().body().velocity());
                    helper.assertTrue(craft.controls().piloted(level.getGameTime()), "a pilot steers it");
                })
                .thenExecuteFor(6, () -> MovingStructures.helm(pilot,
                        StructureHelmPayload.bit(StructureControls.ControlAxis.BACK)))
                .thenExecute(() -> helper.assertTrue(craft.physics().body().velocity().z > -0.8,
                        "back fires the engine looking south, and it slows: " + craft.physics().body().velocity()))
                .thenExecuteAfter(25, () -> {
                    helper.assertTrue(!craft.controls().live(level.getGameTime()),
                            "told nothing a second, the controls let go");
                    leave(pilot);
                    MovingStructures.takeAway(craft);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 400)
    public static void autopilot_fliesAHoveringCraftToAPlaceAndHoldsItThere(final GameTestHelper helper) {
        calm(helper);
        final MovingStructure craft = hoverCraft(helper);
        final Vec3 target = craft.worldBox().getCenter().add(0, 0, -4);
        // With engines only forward and back, it is flown along them: what blows it aside it cannot answer.
        final double[] fastest = new double[1];
        final double[] tilted = new double[1];
        helper.startSequence()
                .thenExecuteAfter(5, () -> craft.fly(StructureAutopilot.goTo(target, 3)))
                .thenWaitUntil(() -> {
                    final double going = Math.abs(craft.physics().body().velocity().z);
                    fastest[0] = Math.max(fastest[0], going);
                    tilted[0] = Math.max(tilted[0], Math.max(Math.abs(craft.pose().qx()),
                            Math.abs(craft.pose().qz())));
                    final double off = craft.worldBox().getCenter().z - target.z;
                    helper.assertTrue(Math.abs(off) < 0.2 && going < 0.2, "it comes to the place and holds there: "
                            + off + " off, going " + going);
                })
                .thenExecuteAfter(20, () -> {
                    final double off = craft.worldBox().getCenter().z - target.z;
                    helper.assertTrue(Math.abs(off) < 0.3, "a second on, it holds there still: " + off + " off");
                    helper.assertTrue(fastest[0] <= 3.3, "never faster than it was told: " + fastest[0]);
                    // A quaternion's x or z of 0.05 is a tilt of under six degrees.
                    helper.assertTrue(tilted[0] < 0.05, "and level all the way: " + tilted[0]);
                    craft.fly(null);
                    MovingStructures.takeAway(craft);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void projectiles_anArrowLoosedFromAMovingDeckFliesOnWithIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 4), TestAboard.SEAT.get());
        final MovingStructure cart = assembled(helper, new BlockPos(2, 2, 4), new BlockPos(3, 2, 4));
        final BlockPos seat = find(level, cart, state -> state.is(TestAboard.SEAT.get()));
        final ServerPlayer archer = rider(helper, new Vec3(3.5, 3, 4.5));
        helper.assertTrue(MovingStructures.sit(archer, seat), "the archer sits aboard");
        final StructurePose start = cart.pose();
        MovingStructures.move(cart, new TowardMotion(start.movedTo(start.x() + 5, start.y(), start.z()), 4, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(cart.velocity()[0] > 0.1, "the deck gets going"))
                .thenExecute(() -> {
                    final Arrow arrow = new Arrow(EntityType.ARROW, level);
                    arrow.setOwner(archer);
                    arrow.setPos(archer.getEyePosition());
                    arrow.setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(arrow);
                    final double deck = cart.velocity()[0];
                    helper.assertTrue(Math.abs(arrow.getDeltaMovement().x - deck) < 0.02,
                            "the arrow goes on as fast as the deck: " + arrow.getDeltaMovement() + " for " + deck);
                    arrow.discard();
                    leave(archer);
                    MovingStructures.takeAway(cart);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void sleeping_aSleeperLiesInABedAboardAndIsCarriedWithIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (int x = 2; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 2, 4), Blocks.STONE);
        }
        final BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        helper.setBlock(new BlockPos(2, 3, 4), foot);
        helper.setBlock(new BlockPos(3, 3, 4), foot.setValue(BedBlock.PART, BedPart.HEAD));
        final MovingStructure boat = assembled(helper, new BlockPos(2, 2, 4), new BlockPos(3, 3, 4));
        final BlockPos head = find(level, boat, state -> state.getBlock() instanceof BedBlock
                && state.getValue(BedBlock.PART) == BedPart.HEAD);
        final Villager sleeper = new Villager(EntityType.VILLAGER, level);
        sleeper.setNoAi(true);
        sleeper.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 2, 4))));
        level.addFreshEntity(sleeper);
        sleeper.startSleeping(head);
        helper.assertTrue(!YardLayout.isYard(sleeper.getX()), "laid down where the bed is in the world, not out in the"
                + " yard: " + sleeper.position());
        helper.assertTrue(lying(boat, head).distanceTo(sleeper.position()) < KEEPS, "in the bed: "
                + sleeper.position() + " for " + lying(boat, head));
        final StructurePose start = boat.pose();
        MovingStructures.move(boat, new TowardMotion(start.movedTo(start.x() + 3, start.y(), start.z()), 3, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(boat.resting(), "the boat goes three blocks"))
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(sleeper.isSleeping(), "the sleeper sleeps on");
                    helper.assertTrue(lying(boat, head).distanceTo(sleeper.position()) < KEEPS,
                            "carried in the bed: " + sleeper.position() + " for " + lying(boat, head));
                    sleeper.stopSleeping();
                    helper.assertTrue(!YardLayout.isYard(sleeper.getX()), "and stands up beside it in the world: "
                            + sleeper.position());
                    sleeper.discard();
                    MovingStructures.takeAway(boat);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void sleeping_aPlayerComesBackToABedAboardWhereItIs(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
        final MovingStructure raft = assembled(helper, new BlockPos(3, 2, 4), new BlockPos(3, 2, 4));
        final BlockPos block = find(level, raft, state -> state.is(Blocks.STONE));
        final Vec3 yardPlace = Vec3.atBottomCenterOf(block.above());
        final PlayerRespawnPositionEvent coming = new PlayerRespawnPositionEvent(player(level, "sleeper"),
                new DimensionTransition(level, yardPlace, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING), false);
        NeoForge.EVENT_BUS.post(coming);
        final Vec3 to = coming.getDimensionTransition().pos();
        helper.assertTrue(to.distanceTo(worldOf(raft, yardPlace)) < 1.0e-6, "they come back where the bed is in the"
                + " world: " + to + " for " + worldOf(raft, yardPlace));
        MovingStructures.takeAway(raft);
        helper.succeed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 300)
    public static void zipline_aRiderHangingFromARopeRidesItDown(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(1, 10, 4), Blocks.STONE);
        final MovingStructure post = assembled(helper, new BlockPos(1, 10, 4), new BlockPos(1, 10, 4));
        final Vec3 high = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 10, 4))).add(0.5, 0, 0);
        final Vec3 low = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 4, 4)));
        final StructureJoint rope = StructureJoints.join(level, new JointKind.Rope(high.distanceTo(low),
                        JointKind.RopeLook.ROPE, Optional.empty()), StructureJoints.Anchor.on(post, high,
                        new Vec3(0, 1, 0)), StructureJoints.Anchor.world(low, new Vec3(0, 1, 0)), Optional.empty());
        final Vec3 holdAt = ZiplineEntity.along(high, low, 0.1);
        final ServerPlayer rider = rider(helper, new Vec3(1.5, 2, 4.5));
        rider.moveTo(holdAt.x, holdAt.y - rider.getEyeHeight(), holdAt.z, 0, 0);
        helper.assertTrue(MovingStructures.zipline(rider, rope.id(), 0.1), "the rider takes hold of the rope");
        helper.assertTrue(rider.getVehicle() instanceof ZiplineEntity, "and hangs from it");
        final Vec3 start = rider.position();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(rider.getVehicle() == null, "at its end, the rider lets go"))
                .thenExecute(() -> {
                    helper.assertTrue(rider.getX() > start.x + 4 && rider.getY() < start.y - 3,
                            "having ridden it down: " + rider.position() + " from " + start);
                    leave(rider);
                    StructureJoints.remove(level, rope.id());
                    MovingStructures.takeAway(post);
                })
                .thenSucceed();
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 200)
    public static void effects_anEngineBurningIsToldAndToldNoMoreOnceItGoesOut(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 2, 4), TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(new BlockPos(5, 2, 4), lever(Direction.EAST, false));
        final MovingStructure rocket = assembled(helper, new BlockPos(4, 2, 4), new BlockPos(5, 2, 4));
        rocket.move(PhysicsMotion.AT_REST);
        final BlockPos engine = find(level, rocket, state -> state.is(TestPhysics.THRUSTER.get()));
        final BlockPos lever = find(level, rocket, state -> state.is(Blocks.LEVER));
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(StructureSync.shown(rocket) == null, "unlit, it shows nothing");
                    level.setBlock(lever, level.getBlockState(lever).setValue(LeverBlock.POWERED, true), 3);
                })
                .thenWaitUntil(() -> {
                    final StructureEffectsPayload shown = StructureSync.shown(rocket);
                    helper.assertTrue(shown != null && shown.engines().size() == 1, "lit, its engine is told");
                    final StructureEffectsPayload.Engine burning = shown.engines().get(0);
                    helper.assertTrue(burning.pos() == engine.asLong() && burning.strength()
                            == StructureEffectsPayload.STEPS && burning.look() == StructureEffectsPayload.Engine.ROCKET,
                            "a rocket at its full: " + burning);
                })
                .thenExecute(() -> level.setBlock(lever, level.getBlockState(lever)
                        .setValue(LeverBlock.POWERED, false), 3))
                .thenWaitUntil(() -> helper.assertTrue(StructureSync.shown(rocket) == null,
                        "gone out, it is told no more"))
                .thenExecute(() -> MovingStructures.takeAway(rocket))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void effects_aRocketTooHeavyToLiftStaysAwakeBurningOnItsPad(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.IRON_BLOCK);
        helper.setBlock(new BlockPos(4, 3, 4), TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(new BlockPos(5, 3, 4), lever(Direction.EAST, true));
        final MovingStructure rocket = assembled(helper, new BlockPos(4, 2, 4), new BlockPos(5, 3, 4));
        rocket.move(PhysicsMotion.AT_REST);
        final double start = rocket.worldBox().minY;
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(Math.abs(rocket.worldBox().minY - start) < 0.05, "too heavy, it stays on its pad: "
                    + rocket.worldBox().minY + " from " + start);
            helper.assertTrue(rocket.physics() != null && !rocket.physics().asleep(),
                    "its engine pushing, it does not fall asleep");
            helper.assertTrue(StructureSync.shown(rocket) != null, "and its flame is still shown");
            MovingStructures.takeAway(rocket);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void effects_whatAStructureShowsGoesToItsPlayersWhole(final GameTestHelper helper) {
        final StructureEffectsPayload sent = new StructureEffectsPayload(42,
                List.of(new StructureEffectsPayload.Engine(BlockPos.asLong(1, 2, 3), (byte) 9,
                        StructureEffectsPayload.Engine.JET)), List.of(0, 1_200, 0, 0, 875, 0), true,
                List.of(BlockPos.asLong(4, 5, 6), BlockPos.asLong(-7, 8, -9)));
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        StructureEffectsPayload.STREAM_CODEC.encode(buffer, sent);
        final StructureEffectsPayload read = StructureEffectsPayload.STREAM_CODEC.decode(buffer);
        helper.assertTrue(read.equals(sent), "read back as sent: " + read);
        helper.assertTrue(read.any(), "and it shows something");
        helper.assertTrue(!new StructureEffectsPayload(42, List.of(), StructureEffectsPayload.NOTHING_HOT, false,
                List.of()).any(), "nothing burning, hot or trailing shows nothing");
        helper.succeed();
    }

    /*
     * A craft hovering in the air in a row: its helm looking north in the middle, a levitator either side holding it
     * up through its middle, and an engine at each end looking along the row, forward north, back south. Held up
     * from under its middle it would tip over, as anything held up from below does; held through it, it hovers level.
     * It stands at the south of the arena, with room to go north.
     */
    private static MovingStructure hoverCraft(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 6, 5), TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(4, 6, 6), TestPhysics.HOVER.get());
        helper.setBlock(new BlockPos(4, 6, 7), TestAboard.HELM.get());
        helper.setBlock(new BlockPos(4, 6, 8), TestPhysics.HOVER.get());
        helper.setBlock(new BlockPos(4, 6, 9), TestPhysics.THRUSTER.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.SOUTH));
        final MovingStructure craft = assembled(helper, new BlockPos(4, 6, 5), new BlockPos(4, 6, 9));
        craft.move(PhysicsMotion.AT_REST);
        return craft;
    }

    /*
     * A player of the server who plays in survival, at the arena's place {@code at}: one a mod stands in for may not
     * ride anything, so a seat needs a real one.
     */
    private static ServerPlayer rider(final GameTestHelper helper, final Vec3 at) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                "deck-rider"), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        for (int id = 0; id <= 8; id++) {
            player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id));
        }
        final Vec3 there = helper.absoluteVec(at);
        player.moveTo(there.x, there.y, there.z, 0, 0);
        return player;
    }

    /* Takes {@code player} off the server, out of their seat first. */
    private static void leave(final ServerPlayer player) {
        player.stopRiding();
        player.server.getPlayerList().remove(player);
    }

    /* Where a sleeper lies in the bed whose head is at the yard's {@code head} of {@code structure}, in the world. */
    private static Vec3 lying(final MovingStructure structure, final BlockPos head) {
        return worldOf(structure, new Vec3(head.getX() + 0.5, head.getY() + LIES, head.getZ() + 0.5));
    }

    /* Where the yard's place {@code place} of {@code structure} is in the world now. */
    private static Vec3 worldOf(final MovingStructure structure, final Vec3 place) {
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /* A stand-in player of its own, named {@code name}. */
    private static FakePlayer player(final ServerLevel level, final String name) {
        return FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(
                StandardCharsets.UTF_8)), name));
    }

    /* A lever on a wall, facing away from the block it is on, lit or not. */
    private static BlockState lever(final Direction facing, final boolean lit) {
        return Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, facing).setValue(LeverBlock.POWERED, lit);
    }

    /*
     * Clears the weather, so the wind blows as on a calm day: a storm another test brought would push a hovering
     * craft as it would a real one.
     */
    private static void calm(final GameTestHelper helper) {
        helper.getLevel().setWeatherParameters(CLEAR, 0, false, false);
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

    /* Makes every block of the arena's box from {@code from} to {@code to} into a structure. */
    private static MovingStructure assembled(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(), helper.absolutePos(from),
                helper.absolutePos(to));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
