/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFootingPayload;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.robot.CoreRobotTasks;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.vehicle.DriverInput;
import dev.jstech.core.vehicle.VehicleAccess;
import dev.jstech.core.vehicle.VehicleBuild;
import dev.jstech.core.vehicle.VehicleEntity;
import dev.jstech.core.vehicle.VehicleHolds;
import dev.jstech.core.vehicle.VehicleLean;
import dev.jstech.core.vehicle.VehiclePartStack;
import dev.jstech.core.vehicle.VehicleRoles;
import dev.jstech.core.vehicle.VehicleStack;
import dev.jstech.core.vehicle.Vehicles;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestEntities;
import dev.jstech.tests.TestFluids;
import dev.jstech.tests.TestMaterials;
import dev.jstech.tests.TestVehicles;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.joml.Vector3d;

/**
 * The Core's vehicles, through the test mod's own: a van made of its parts, which goes ahead on its fuel and burns it,
 * stops when it runs out, changes gear as asked, leans where the ground falls away, grips less on ice, wears its parts
 * against a wall and on a hard landing, leaks from a worn tank and runs over what stands in its way; its seats and
 * their roles, its keys, its lock and whose it is; its paint, its parts taken out, fitted and mended; its boot and its
 * tanks through the game's capabilities; a trailer it tows; a drone rising on its battery; a boat afloat on its gas; a
 * fuel pump filling all three; a van carried by a moving deck, parked or driven; and a van that takes its driver
 * through to the Nether.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class VehicleGameTests {

    private static final String ARENA = "empty";
    /* Room to gather speed: forty-eight blocks a side. */
    private static final String ROAD = "bench";
    /* Room to rise and to fall: thirty-two blocks high. */
    private static final String SKY = "tall";
    private static final String BATCH = "jscore_vehicles";
    private static final String DECK_BATCH = "jscore_vehicles_deck";
    private static final DriverInput AHEAD = new DriverInput(1.0F, 0.0F, false, false);

    private VehicleGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void van_isMadeOfItsKindWithItsParts(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        final VehicleBuild build = van.build();
        helper.assertTrue(build != null && build.engine().isPresent() && build.wheels().size() == 4
                && build.drivenWheels() == 2 && build.tank() == TestVehicles.VAN_TANK && build.horn().isPresent()
                && build.lights().size() == 1, "the van is made of its engine, its four wheels, two of them driven, "
                + "its tank, its horn and its headlights: " + build);
        final VehicleHolds holds = van.holds().orElseThrow();
        helper.assertTrue(holds.boot().getSlots() == TestVehicles.VAN_BOOT && holds.cargo().size() == 1,
                "with its boot and its cargo tank");
        helper.assertTrue("white".equals(van.paint()) && "plain".equals(van.variant()) && van.switches().ignition(),
                "in its first paint and shape, its ignition on: " + van.paint() + ", " + van.variant());
        fuel(van, 300);
        van.setPaint("red");
        final ItemStack item = Vehicles.itemOf(van);
        final VehicleStack carried = item.get(Vehicles.VEHICLE.get());
        helper.assertTrue(carried != null && carried.vehicle().equals(TestVehicles.VAN) && carried.state().isPresent(),
                "as an item it names its kind and carries all it holds");
        final VehicleEntity again = new VehicleEntity(Vehicles.ENTITY.get(), helper.getLevel());
        again.loadState(carried.state().orElseThrow());
        helper.assertTrue("red".equals(again.paint()) && again.holds().map(held -> held.liquid().getFluidAmount())
                .orElse(0) == 300, "and placed again it is as it was: " + again.paint());
        van.discard();
        helper.succeed();
    }

    @GameTest(template = ROAD, batch = BATCH, timeoutTicks = 200)
    public static void van_goesAheadOnItsFuelAndBurnsIt(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(6.5, 2, 6.5));
        fuel(van, 500);
        final TestEntities.Robot robot = driver(helper, van);
        robot.queue(new CoreRobotTasks.Drive(AHEAD, 30));
        final Vec3 start = van.position();
        helper.runAfterDelay(35, () -> {
            final double moved = van.position().subtract(start).horizontalDistance();
            helper.assertTrue(moved > 3, "the van went ahead; it moved " + moved + " blocks");
            helper.assertTrue(fuelOf(van) < 500, "burning its fuel as it went; it holds " + fuelOf(van));
            robot.discard();
            van.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ROAD, batch = BATCH, timeoutTicks = 200)
    public static void van_stopsWhenItsFuelRunsOut(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(6.5, 2, 6.5));
        fuel(van, 1);
        final TestEntities.Robot robot = driver(helper, van);
        robot.queue(new CoreRobotTasks.Drive(AHEAD, 80));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(fuelOf(van) == 0 && !van.running(), "a millibucket burns away and the engine stops: "
                    + fuelOf(van) + " left, running " + van.running());
            robot.discard();
            van.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void van_changesGearAsAsked(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        fuel(van, 100);
        van.setSwitches(van.switches().manual(true));
        helper.startSequence()
                .thenExecuteAfter(2, () -> van.askShift(1))
                .thenExecuteAfter(3, () -> helper.assertTrue(van.gear() == 2, "asked up, it is in second: "
                        + van.gear()))
                .thenExecute(() -> van.askShift(-1))
                .thenExecuteAfter(3, () -> helper.assertTrue(van.gear() == 1, "asked down, back in first: "
                        + van.gear()))
                .thenExecute(van::discard)
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void van_leansWhereTheGroundFallsAway(final GameTestHelper helper) {
        // The ground under its front wheels taken away, a block ahead of its middle.
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 5), Blocks.AIR);
        }
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(van.leanPitch() < -5, "its nose leans down: " + van.leanPitch());
            helper.assertTrue(Math.abs(van.leanRoll()) < 1, "and not to a side: " + van.leanRoll());
            van.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void seats_leanWithTheBody(final GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 5), Blocks.AIR);
        }
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        helper.assertTrue(pig.startRiding(van, true), "a pig takes the passenger's seat");
        helper.runAfterDelay(10, () -> {
            final Vec3 at = van.def().seats().get(van.seatOf(pig)).at();
            final Vector3d leaning = VehicleLean.seat(at.x, at.y, at.z, van.getYRot(), van.leanPitch(),
                    van.leanRoll());
            final Vector3d level = VehicleLean.seat(at.x, at.y, at.z, van.getYRot(), 0.0F, 0.0F);
            final Vec3 seat = van.position().add(leaning.x, leaning.y, leaning.z);
            helper.assertTrue(van.leanPitch() < -5 && pig.position().distanceTo(seat) < 1.0e-3,
                    "the pig sits where its seat is with the body leaning nose down: " + pig.position() + " against "
                            + seat);
            helper.assertTrue(new Vector3d(leaning).sub(level).length() > 0.05, "not where it would be standing level");
            pig.discard();
            van.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ROAD, batch = BATCH, timeoutTicks = 200)
    public static void van_gripsLessOnIce(final GameTestHelper helper) {
        for (int x = 26; x <= 32; x++) {
            for (int z = 2; z <= 30; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.BLUE_ICE);
            }
        }
        final VehicleEntity dry = van(helper, new Vec3(10.5, 2, 6.5));
        final VehicleEntity icy = van(helper, new Vec3(29.5, 2, 6.5));
        fuel(dry, 500);
        fuel(icy, 500);
        final TestEntities.Robot dryDriver = driver(helper, dry);
        final TestEntities.Robot icyDriver = driver(helper, icy);
        dryDriver.queue(new CoreRobotTasks.Drive(AHEAD, 25));
        icyDriver.queue(new CoreRobotTasks.Drive(AHEAD, 25));
        final Vec3 dryStart = dry.position();
        final Vec3 icyStart = icy.position();
        helper.runAfterDelay(25, () -> {
            final double onDry = dry.position().subtract(dryStart).horizontalDistance();
            final double onIce = icy.position().subtract(icyStart).horizontalDistance();
            helper.assertTrue(onIce < onDry * 0.5, "on ice its wheels spin: " + onIce + " blocks against " + onDry);
            dryDriver.discard();
            icyDriver.discard();
            dry.discard();
            icy.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ROAD, batch = BATCH, timeoutTicks = 300)
    public static void van_wearsItsFrontAgainstAWall(final GameTestHelper helper) {
        for (int x = 2; x <= 11; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 20), Blocks.STONE);
            }
        }
        final VehicleEntity van = van(helper, new Vec3(6.5, 2, 4.5));
        fuel(van, 500);
        final TestEntities.Robot robot = driver(helper, van);
        robot.queue(new CoreRobotTasks.Drive(AHEAD, 120));
        helper.succeedWhen(() -> {
            final float engine = health(van, "engine");
            helper.assertTrue(engine < 1, "the blow wears its engine, at its front: " + engine);
            helper.assertTrue(health(van, "tank") > engine, "more than its tank, at its back: " + health(van, "tank"));
            robot.discard();
            van.discard();
        });
    }

    @GameTest(template = SKY, batch = BATCH, timeoutTicks = 200)
    public static void van_wearsItsWheelsLandingHard(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(6.5, 14, 6.5));
        helper.succeedWhen(() -> {
            helper.assertTrue(van.onGround(), "the van lands");
            helper.assertTrue(health(van, "front_left") < 1, "on its wheels, worn by the fall: "
                    + health(van, "front_left"));
            van.discard();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void van_leaksFromAWornTank(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        fuel(van, 500);
        van.setSwitches(van.switches().ignition(false));
        van.parts().wear("tank", 0.85F);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(fuelOf(van) < 500, "its fuel leaks away while it stands: " + fuelOf(van));
            van.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ROAD, batch = BATCH, timeoutTicks = 200)
    public static void van_runsOverWhatStandsInItsWay(final GameTestHelper helper) {
        final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 2, 16.5));
        final float health = pig.getHealth();
        final VehicleEntity van = van(helper, new Vec3(6.5, 2, 4.5));
        fuel(van, 500);
        final TestEntities.Robot robot = driver(helper, van);
        robot.queue(new CoreRobotTasks.Drive(AHEAD, 60));
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < health || pig.isRemoved(), "the pig is run over; its health is "
                    + pig.getHealth());
            pig.discard();
            robot.discard();
            van.discard();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void seats_holdTheirRoles(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2, 4.5));
        helper.assertTrue(pig.startRiding(van, true) && van.seatOf(pig) == 1, "a pig drives nothing: it sits as a "
                + "passenger; seat " + van.seatOf(pig));
        final TestEntities.Robot robot = driver(helper, van);
        helper.assertTrue(van.seatOf(robot) == 0 && van.getControllingPassenger() == robot
                && van.seatedAs(VehicleRoles.DRIVER).contains(robot), "a robot takes the driver's seat and drives");
        helper.assertTrue(!van.nextSeat(robot), "with every other seat taken it stays where it is");
        pig.stopRiding();
        helper.assertTrue(van.nextSeat(robot) && van.seatOf(robot) == 1 && van.getControllingPassenger() == null,
                "the passenger's seat free, it moves there, and nobody drives");
        robot.discard();
        pig.discard();
        van.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void keys_letDriveAndLock(final GameTestHelper helper) {
        final ServerPlayer owner = player(helper, new Vec3(2.5, 2, 2.5), "van-owner");
        final ServerPlayer stranger = player(helper, new Vec3(6.5, 2, 6.5), "van-stranger");
        final VehicleEntity van = Vehicles.make(helper.getLevel(), TestVehicles.VAN, helper.absoluteVec(
                new Vec3(4.5, 2, 4.5)), 0, "", owner).orElseThrow();
        helper.assertTrue(van.ownership().map(Ownership::owner).map(owner.getUUID()::equals).orElse(false),
                "the van is its maker's");
        helper.assertTrue(VehicleAccess.mayDrive(van, owner) && !VehicleAccess.mayDrive(van, stranger),
                "its owner may drive it, a stranger not");
        stranger.getInventory().add(Vehicles.keyFor(van));
        helper.assertTrue(VehicleAccess.mayDrive(van, stranger), "a stranger with its key may");
        van.forgetKeys();
        helper.assertTrue(!VehicleAccess.mayDrive(van, stranger), "and no longer once its keys are forgotten");
        owner.setItemInHand(InteractionHand.MAIN_HAND, Vehicles.keyFor(van));
        owner.setShiftKeyDown(true);
        van.interact(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(van.switches().locked(), "its key, used sneaking, locks it");
        van.interact(stranger, InteractionHand.MAIN_HAND);
        helper.assertTrue(!stranger.isPassenger(), "and locked it keeps out who may not drive it");
        van.interact(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(!van.switches().locked(), "used again, it unlocks it");
        van.interact(stranger, InteractionHand.MAIN_HAND);
        helper.assertTrue(stranger.getVehicle() == van && van.seatOf(stranger) == 1,
                "and the stranger gets in, as a passenger: seat " + van.seatOf(stranger));
        final ItemStack kept = Vehicles.keyFor(van);
        final VehicleStack carried = Vehicles.itemOf(van).get(Vehicles.VEHICLE.get());
        final VehicleEntity again = new VehicleEntity(Vehicles.ENTITY.get(), helper.getLevel());
        again.loadState(carried.state().orElseThrow());
        helper.assertTrue(VehicleAccess.opens(kept, again), "picked up and placed again, its keys still open it");
        leave(helper, owner);
        leave(helper, stranger);
        van.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void dye_paintsIt(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, new Vec3(2.5, 2, 2.5), "van-painter");
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE, 2));
        van.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue("red".equals(van.paint()), "red dye paints it red: " + van.paint());
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "taking the dye");
        leave(helper, player);
        van.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void parts_areTakenOutFittedAndMended(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, new Vec3(2.5, 2, 2.5), "van-mechanic");
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        final ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                "jscore", "basic_wrench")));
        // At the engine, at the front of the van facing south.
        final Vec3 atEngine = new Vec3(0, 0.5, 0.9);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        player.setShiftKeyDown(true);
        van.interactAt(player, atEngine, InteractionHand.MAIN_HAND);
        final ItemStack taken = partIn(player);
        final VehiclePartStack part = taken.get(Vehicles.PART.get());
        helper.assertTrue(van.parts().at("engine").isEmpty() && part != null && part.part().equals(TestVehicles.ENGINE),
                "the wrench, sneaking, takes out the engine into the player's hands");
        helper.assertTrue(van.build() != null && van.build().engine().isEmpty(), "and the van has none");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, taken);
        van.interactAt(player, atEngine, InteractionHand.MAIN_HAND);
        helper.assertTrue(van.parts().at("engine").isPresent() && player.getMainHandItem().isEmpty(),
                "the engine, used on it, goes back in");
        van.parts().wear("engine", 0.5F);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 2));
        van.interactAt(player, atEngine, InteractionHand.MAIN_HAND);
        helper.assertTrue(Math.abs(health(van, "engine") - 0.75F) < 0.01F && player.getOffhandItem().getCount() == 1,
                "the wrench, with iron in the other hand, mends it a quarter: " + health(van, "engine"));
        leave(helper, player);
        van.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void bootAndTanks_areReachedThroughTheCapabilities(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        final IItemHandler boot = van.getCapability(Capabilities.ItemHandler.ENTITY, null);
        helper.assertTrue(boot != null && boot.insertItem(0, new ItemStack(Items.COBBLESTONE, 16), false).isEmpty()
                && van.holds().orElseThrow().boot().getStackInSlot(0).getCount() == 16,
                "a hopper's handler puts stone in its boot");
        final IFluidHandler tanks = van.getCapability(Capabilities.FluidHandler.ENTITY, null);
        helper.assertTrue(tanks != null && tanks.fill(new FluidStack(Fluids.WATER, 1_000),
                IFluidHandler.FluidAction.EXECUTE) == 1_000, "a pipe fills its cargo tank with water");
        final VehicleHolds holds = van.holds().orElseThrow();
        helper.assertTrue(holds.liquid().isEmpty() && holds.cargo().get(0).getFluidAmount() == 1_000,
                "water, which its engine does not burn, goes to its cargo, not its fuel tank");
        helper.assertTrue(tanks.fill(TestFluids.COOL_OIL.stack(200), IFluidHandler.FluidAction.EXECUTE) == 200
                && holds.liquid().getFluidAmount() == 200, "and its fuel to its fuel tank");
        van.discard();
        helper.succeed();
    }

    @GameTest(template = ROAD, batch = BATCH, timeoutTicks = 200)
    public static void trailer_followsWhatTowsIt(final GameTestHelper helper) {
        final VehicleEntity van = van(helper, new Vec3(6.5, 2, 8.5));
        final VehicleEntity trailer = Vehicles.make(helper.getLevel(), TestVehicles.TRAILER, helper.absoluteVec(
                new Vec3(6.5, 2, 6.2)), 0, "", null).orElseThrow();
        trailer.hitchTo(van);
        fuel(van, 500);
        final TestEntities.Robot robot = driver(helper, van);
        robot.queue(new CoreRobotTasks.Drive(AHEAD, 30));
        final Vec3 start = trailer.position();
        helper.runAfterDelay(35, () -> {
            final double moved = trailer.position().subtract(start).horizontalDistance();
            final double apart = trailer.position().subtract(van.position()).horizontalDistance();
            helper.assertTrue(moved > 3, "the trailer comes along; it moved " + moved + " blocks");
            helper.assertTrue(trailer.towedBy() == van && apart > 1.5 && apart < 3.5,
                    "hitched behind the van, as far as its hitch is: " + apart);
            robot.discard();
            trailer.discard();
            van.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SKY, batch = BATCH, timeoutTicks = 200)
    public static void drone_risesOnItsBattery(final GameTestHelper helper) {
        final VehicleEntity drone = Vehicles.make(helper.getLevel(), TestVehicles.DRONE, helper.absoluteVec(
                new Vec3(6.5, 2, 6.5)), 0, "", null).orElseThrow();
        final IEnergyStorage battery = drone.getCapability(Capabilities.EnergyStorage.ENTITY, null);
        helper.assertTrue(battery != null && battery.receiveEnergy(1_000_000, false) == 1_000_000,
                "its battery is charged through the energy capability");
        final TestEntities.Robot robot = driver(helper, drone);
        robot.queue(new CoreRobotTasks.Drive(new DriverInput(0.0F, 0.0F, true, false), 30));
        final double start = drone.getY();
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(drone.getY() > start + 2, "the drone rose " + (drone.getY() - start) + " blocks");
            helper.assertTrue(battery.getEnergyStored() < 1_000_000, "on its battery: " + battery.getEnergyStored());
            robot.discard();
            drone.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void boat_floatsAndGoesOnItsGas(final GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                final boolean edge = x == 0 || z == 0 || x == 8 || z == 8;
                helper.setBlock(new BlockPos(x, 2, z), edge ? Blocks.GLASS : Blocks.WATER);
                helper.setBlock(new BlockPos(x, 3, z), edge ? Blocks.GLASS : Blocks.AIR);
            }
        }
        final VehicleEntity boat = Vehicles.make(helper.getLevel(), TestVehicles.BOAT, helper.absoluteVec(
                new Vec3(2.5, 3, 4.5)), -90, "", null).orElseThrow();
        final long filled = boat.holds().orElseThrow().fillGas(new MatterStack(TestMaterials.TESTOGEN, 20_000), false);
        helper.assertTrue(filled == 20_000, "the test gas fills its tank: " + filled);
        final TestEntities.Robot robot = driver(helper, boat);
        robot.setInvulnerable(true);
        robot.queue(new CoreRobotTasks.Drive(AHEAD, 20));
        final Vec3 start = helper.absoluteVec(new Vec3(2.5, 3, 4.5));
        final double floor = helper.absoluteVec(new Vec3(0, 2, 0)).y;
        helper.runAfterDelay(40, () -> {
            final double moved = boat.position().subtract(start).horizontalDistance();
            helper.assertTrue(moved > 1, "the boat went ahead; it moved " + moved + " blocks");
            helper.assertTrue(boat.getY() > floor + 0.3, "afloat, not on the bottom: " + (boat.getY() - floor));
            helper.assertTrue(boat.holds().orElseThrow().gas().flatMap(tank -> tank.held())
                    .map(MatterStack::amount).orElse(0L) < 20_000, "burning its gas");
            robot.discard();
            boat.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void pump_fillsWhatStandsBesideIt(final GameTestHelper helper) {
        final BlockPos pumpAt = new BlockPos(4, 2, 4);
        helper.setBlock(pumpAt, TestVehicles.PUMP.get());
        final BlockPos at = helper.absolutePos(pumpAt);
        final ServerLevel level = helper.getLevel();
        final IFluidHandler liquid = level.getCapability(Capabilities.FluidHandler.BLOCK, at, null);
        final IEnergyStorage charge = level.getCapability(Capabilities.EnergyStorage.BLOCK, at, null);
        final IMatterHandler gas = level.getCapability(MatterCapabilities.GAS, at, null);
        helper.assertTrue(liquid != null && liquid.fill(TestFluids.COOL_OIL.stack(2_000),
                IFluidHandler.FluidAction.EXECUTE) == 2_000, "a pipe fills the pump with fuel");
        helper.assertTrue(charge != null && charge.receiveEnergy(1_000_000, false) == 1_000_000,
                "a cable charges it");
        helper.assertTrue(gas != null && gas.insert(new MatterStack(TestMaterials.TESTOGEN, 20_000), false) == 20_000,
                "and a gas line fills it with gas");
        final VehicleEntity van = van(helper, new Vec3(2.5, 2, 4.5));
        final VehicleEntity drone = Vehicles.make(level, TestVehicles.DRONE, helper.absoluteVec(
                new Vec3(6.5, 2, 4.5)), 0, "", null).orElseThrow();
        final VehicleEntity boat = Vehicles.make(level, TestVehicles.BOAT, helper.absoluteVec(
                new Vec3(4.5, 2, 6.5)), 0, "", null).orElseThrow();
        boat.setSwitches(boat.switches().ignition(false));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(fuelOf(van) > 0 && liquid.getFluidInTank(0).getAmount() < 2_000,
                    "the van takes fuel from the pump: " + fuelOf(van));
            helper.assertTrue(drone.holds().orElseThrow().charge() > 0, "the drone takes charge");
            helper.assertTrue(boat.holds().orElseThrow().gas().flatMap(tank -> tank.held()).isPresent(),
                    "and the boat takes gas");
            helper.assertTrue(van.holds().orElseThrow().charge() == 0, "each only what it runs on: the van, no "
                    + "battery, takes no charge");
            van.discard();
            drone.discard();
            boat.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = DECK_BATCH, timeoutTicks = 400)
    public static void deck_carriesAParkedVehicle(final GameTestHelper helper) {
        final MovingStructure deck = floor(helper);
        final VehicleEntity van = van(helper, new Vec3(4.5, 4, 4.5));
        van.setSwitches(van.switches().parkingBrake(true));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(van.onGround(), "the van stands on the deck"))
                .thenExecute(() -> deck.move(new TowardMotion(deck.pose().movedTo(deck.pose().x(),
                        deck.pose().y() + 3, deck.pose().z()), 2, 0)))
                .thenWaitUntil(() -> helper.assertTrue(deck.resting(), "the deck arrives"))
                .thenExecuteAfter(5, () -> helper.assertTrue(van.getY() > helper.absoluteVec(new Vec3(0, 6.9, 0)).y,
                        "the van rose with the deck: " + (van.getY() - helper.absoluteVec(Vec3.ZERO).y)))
                .thenExecute(() -> {
                    van.discard();
                    MovingStructures.takeAway(deck);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = DECK_BATCH, timeoutTicks = 400)
    public static void deck_carriesAVehicleWhoseDriverSaysWhereItStands(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure deck = floor(helper);
        final VehicleEntity van = van(helper, new Vec3(4.5, 4, 4.5));
        final ServerPlayer player = player(helper, new Vec3(4.5, 4, 4.5), "van-driver");
        helper.assertTrue(player.startRiding(van, true) && van.getControllingPassenger() == player,
                "the player drives the van");
        final Vec3 stood = van.position();
        final double[] place = {deck.frame().yardX(stood.x, stood.y, stood.z),
                deck.frame().yardY(stood.x, stood.y, stood.z), deck.frame().yardZ(stood.x, stood.y, stood.z)};
        final double top = stood.y;
        // What the player's game says each tick, from a van it still sees where it stood: a stand-in at that place.
        final Pig said = new Pig(EntityType.PIG, level);
        said.moveTo(stood.x, stood.y, stood.z, van.getYRot(), 0);
        player.connection.tick();
        deck.move(new TowardMotion(deck.pose().movedTo(deck.pose().x(), deck.pose().y() + 3, deck.pose().z()), 2,
                0));
        helper.onEachTick(() -> {
            if (player.connection != null && !player.isRemoved()) {
                player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
                        new StructureFootingPayload(deck.entity().getId(), place, true)));
                player.connection.handleMoveVehicle(new ServerboundMoveVehiclePacket(said));
                player.connection.tick();
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(deck.resting(), "the deck arrives"))
                .thenExecuteAfter(5, () -> helper.assertTrue(Math.abs(van.getY() - (top + 3)) < 0.05,
                        "the van rose with the deck: " + van.getY() + " for " + (top + 3)))
                .thenExecute(() -> {
                    leave(helper, player);
                    van.discard();
                    MovingStructures.takeAway(deck);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void vehicle_takesItsDriverToTheNether(final GameTestHelper helper) {
        final ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "there is a Nether");
        final VehicleEntity van = van(helper, new Vec3(4.5, 2, 4.5));
        fuel(van, 300);
        final TestEntities.Robot robot = driver(helper, van);
        robot.setInvulnerable(true);
        final Vec3 there = new Vec3(helper.absolutePos(BlockPos.ZERO).getX() + 0.5, 180,
                helper.absolutePos(BlockPos.ZERO).getZ() + 0.5);
        final int landX = Mth.floor(there.x) >> 4;
        final int landZ = Mth.floor(there.z) >> 4;
        nether.setChunkForced(landX, landZ, true);
        final Entity gone = van.changeDimension(new DimensionTransition(nether, there, Vec3.ZERO, 0, 0,
                DimensionTransition.DO_NOTHING));
        helper.assertTrue(gone instanceof VehicleEntity && gone.level() == nether && van.isRemoved(),
                "the van goes through to the Nether: " + gone);
        final VehicleEntity arrived = (VehicleEntity) gone;
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(arrived.getPassengers().stream().anyMatch(TestEntities.Robot.class::isInstance)
                    && arrived.getControllingPassenger() instanceof TestEntities.Robot,
                    "with its driver in the driver's seat: " + arrived.getPassengers());
            helper.assertTrue(fuelOf(arrived) == 300 && TestVehicles.VAN.equals(arrived.defId().orElse(null)),
                    "the same van, holding its fuel: " + fuelOf(arrived));
            arrived.getPassengers().forEach(Entity::discard);
            arrived.discard();
            nether.setChunkForced(landX, landZ, false);
            helper.succeed();
        });
    }

    /* A test van at {@code at} in the test's own places, facing south, nobody's. */
    private static VehicleEntity van(final GameTestHelper helper, final Vec3 at) {
        return Vehicles.make(helper.getLevel(), TestVehicles.VAN, helper.absoluteVec(at), 0, "", null)
                .orElseThrow();
    }

    /* A robot in the driver's seat of {@code vehicle}. */
    private static TestEntities.Robot driver(final GameTestHelper helper, final VehicleEntity vehicle) {
        final TestEntities.Robot robot = TestEntities.ROBOT.get().create(helper.getLevel());
        robot.moveTo(vehicle.getX(), vehicle.getY(), vehicle.getZ(), 0, 0);
        helper.getLevel().addFreshEntity(robot);
        helper.assertTrue(robot.startRiding(vehicle, true), "the robot gets in");
        return robot;
    }

    private static void fuel(final VehicleEntity vehicle, final int millibuckets) {
        vehicle.holds().orElseThrow().liquid().fill(TestFluids.COOL_OIL.stack(millibuckets),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private static int fuelOf(final VehicleEntity vehicle) {
        return vehicle.holds().map(held -> held.liquid().getFluidAmount()).orElse(-1);
    }

    private static float health(final VehicleEntity vehicle, final String place) {
        return vehicle.parts().at(place).map(fitted -> fitted.health()).orElse(-1F);
    }

    /* The first vehicle part in the player's inventory, taken out of it. */
    private static ItemStack partIn(final ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (player.getInventory().items.get(slot).has(Vehicles.PART.get())) {
                return player.getInventory().removeItemNoUpdate(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    /* A floor of stone three blocks a side at y 3, made a structure. */
    private static MovingStructure floor(final GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        final ServerLevel level = helper.getLevel();
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 3)),
                helper.absolutePos(new BlockPos(5, 3, 5)));
        helper.assertTrue(made.ok() && made.structure() != null, "a floor is made");
        return made.structure();
    }

    /* A player of a game connected to the server, at {@code at} in the test's own places. */
    private static ServerPlayer player(final GameTestHelper helper, final Vec3 at, final String name) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                name), false);
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
        // In survival, so what they use is used up, as a player's is.
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 there = helper.absoluteVec(at);
        player.moveTo(there.x, there.y, there.z, 0, 0);
        return player;
    }

    private static void leave(final GameTestHelper helper, final ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }
}
