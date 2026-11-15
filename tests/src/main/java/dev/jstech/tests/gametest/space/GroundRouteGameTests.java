/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.Text;
import dev.jstech.space.crawler.CrawlerEngineBlockEntity;
import dev.jstech.space.crawler.CrawlerLayout;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launch.LaunchCount;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.pad.LaunchPads;
import dev.jstech.space.pad.PadBlocks;
import dev.jstech.space.pad.PadServicePortBlockEntity;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A rocket's whole way on the ground, from its design to lift-off, each step by the machine a player uses for it:
 * designed at the Rocket Hangar's terminal and sent to its bay; built there with its mobile launcher by the crane, from
 * the blocks that come in through the loading dock; rolled out of the bay door on its launcher by the
 * crawler-transporter to the end of the crawlerway, the hangar handing the rocket's plan to its flight computer;
 * fuelled there by the pad's service port from its liquid oxygen and liquid methane lines, its deluge held; and counted
 * down from the flight computer's LAUNCH page to its release, climbing off the pad.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GroundRouteGameTests {

    private static final String HANGAR = "rocket_hangar";
    private static final String BATCH = "jsspace_ground_route";
    private static final String NAME = "GROUND";
    /* The hangar's terminal in the arena's middle, the hangar standing with its door to the north. */
    private static final BlockPos TERMINAL = new BlockPos(40, 2, 42);
    /*
     * A block of the loading dock, in the control room's outer wall to the east, and the door's place on the bay's
     * middle line, as the hangar's pattern draws them.
     */
    private static final int[] DOCK = {27, 1, 6};
    private static final int[] DOOR = {0, 0, -19};
    /* How high the crawler's engine stands over the bay's floor, its tracks on the floor's gravel. */
    private static final int ENGINE_UP = 2;
    /* The crawlerway's lanes outside, as the floor's run, each side of the middle line; and where they end. */
    private static final int LANE_IN = 3;
    private static final int LANE_OUT = 6;
    private static final int LANE_END_Z = 3;
    /* How far east of the rocket's axis the pad's service port stands on the ground, clear of the launcher's deck. */
    private static final int PORT_OUT = 10;
    /* How far round the rocket's axis its launcher's measuring hold-down arm is looked for. */
    private static final int ARM_REACH = 8;
    /* The flight computer's keys: MASTER and IGNITION down the left, COUNT and LAUNCH down the right. */
    private static final int MASTER = FlightComputerBlockEntity.LEFT;
    private static final int IGNITION = FlightComputerBlockEntity.LEFT + 1;
    private static final int COUNT = FlightComputerBlockEntity.RIGHT;
    private static final int LAUNCH = FlightComputerBlockEntity.RIGHT + 4;
    private static final int TIME = 12_000;

    private GroundRouteGameTests() {
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = TIME)
    public static void rocket_goesFromTheHangarsDesignToLiftOff(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        fairWeather(level);
        final Orientation turn = Orientation.NORTH;
        final BlockPos at = TestHangars.build(level, helper.absolutePos(TERMINAL), turn);
        final BlockPos dock = TestHangars.at(at, turn, DOCK[0], DOCK[1], DOCK[2]);
        final BlockPos door = TestHangars.at(at, turn, DOOR[0], DOOR[1], DOOR[2]);
        final ServerPlayer crew = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ground_crew"));
        final BlockPos[] engine = new BlockPos[1];
        final BlockPos[] axis = new BlockPos[1];
        final BlockPos[] port = new BlockPos[1];
        final IEnergyStorage[] cable = new IEnergyStorage[1];
        final MovingStructure[] flying = new MovingStructure[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).formed(), "the hangar forms"))
                .thenExecute(() -> {
                    final HangarTerminalBlockEntity terminal = terminal(level, at);
                    terminal.design(new RocketDesign(NAME, rocket()));
                    helper.assertTrue(terminal.confirm(), "a ready design is sent to the bay");
                    final IItemHandler inbox = level.getCapability(Capabilities.ItemHandler.BLOCK, dock,
                            Direction.EAST);
                    helper.assertTrue(inbox != null, "the loading dock passes items to its terminal");
                    for (final Map.Entry<Item, Integer> need : terminal.crane().needs().entrySet()) {
                        helper.assertTrue(insert(inbox, need.getKey(), need.getValue()) == need.getValue(),
                                "every " + need.getKey() + " the rocket and its launcher want goes in");
                    }
                    helper.assertTrue(terminal.crane().needs().isEmpty(),
                            "the crane holds every block of the rocket and its launcher");
                })
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).finished(), "the crane builds the rocket"
                        + " and its launcher whole: " + terminal(level, at).missingParts() + " blocks to go"))
                .thenExecute(() -> {
                    final HangarTerminalBlockEntity terminal = terminal(level, at);
                    final HangarBay bay = terminal.bay().orElseThrow();
                    // The crawler under the launcher's deck, as it drives in empty; its crawlerway out of the door.
                    engine[0] = underDeck(bay, terminal.build().orElseThrow());
                    for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(engine[0], Direction.SOUTH)) {
                        level.setBlockAndUpdate(spot.at(), spot.kind().block().defaultBlockState());
                    }
                    final BlockPos edge = helper.absolutePos(new BlockPos(0, 1, LANE_END_Z));
                    for (int z = edge.getZ(); z < door.getZ(); z++) {
                        for (int off = LANE_IN; off <= LANE_OUT; off++) {
                            for (final int side : new int[] {-1, 1}) {
                                final BlockPos lane = new BlockPos(door.getX() + side * off, edge.getY(), z);
                                level.setBlockAndUpdate(lane.below(), Blocks.STONE.defaultBlockState());
                                level.setBlockAndUpdate(lane, Blocks.GRAVEL.defaultBlockState());
                            }
                        }
                    }
                    // Where the rocket's axis comes to stand: as far north as the crawler's front goes.
                    final int steps = engine[0].getZ() - CrawlerLayout.HALF_LENGTH - edge.getZ();
                    axis[0] = terminal.planAt().orElseThrow().relative(Direction.NORTH, steps);
                })
                .thenWaitUntil(() -> helper.assertTrue(crawler(level, engine[0]).along().isPresent(),
                        "the crawler forms under the launcher"))
                .thenExecute(() -> {
                    final Optional<Text> why = crawler(level, engine[0]).start();
                    helper.assertTrue(why.isEmpty(), "with the rocket built whole over it, it sets off: " + why);
                    helper.assertTrue(terminal(level, at).plan().isEmpty(),
                            "the hangar hands the rocket's plan over as the crawler takes it");
                })
                .thenWaitUntil(() -> helper.assertTrue(launcher(level, axis[0]).isPresent(),
                        "the crawler sets the launcher down at the crawlerway's end"))
                .thenExecute(() -> {
                    // The pad's service port on the ground by it, its lines and its deluge filled by pipe.
                    port[0] = axis[0].below(LauncherLayout.RAISE).relative(Direction.EAST, PORT_OUT);
                    helper.assertTrue(level.getBlockState(port[0]).isAir(),
                            "the port's place is clear of the launcher");
                    level.setBlockAndUpdate(port[0], PadBlocks.PAD_SERVICE_PORT.get().defaultBlockState());
                    final IFluidHandler pipe = level.getCapability(Capabilities.FluidHandler.BLOCK, port[0],
                            Direction.UP);
                    cable[0] = level.getCapability(Capabilities.EnergyStorage.BLOCK, port[0], Direction.UP);
                    helper.assertTrue(pipe != null && cable[0] != null, "the port takes pipes and a cable");
                    pipe.fill(new FluidStack(SkeletonPropellants.LIQUID_OXYGEN.get(), PadServicePortBlockEntity.LINE),
                            IFluidHandler.FluidAction.EXECUTE);
                    pipe.fill(new FluidStack(SkeletonPropellants.LIQUID_METHANE.get(),
                            PadServicePortBlockEntity.LINE), IFluidHandler.FluidAction.EXECUTE);
                    pipe.fill(new FluidStack(Fluids.WATER, PadServicePortBlockEntity.DELUGE_WATER),
                            IFluidHandler.FluidAction.EXECUTE);
                })
                .thenWaitUntil(() -> {
                    charge(cable[0]);
                    final LaunchCount.Shown board = count(level, axis[0]).shown();
                    helper.assertTrue(board.loaded(), "the port fuels the rocket full: oxidiser " + board.oxidiser()
                            + ", fuel " + board.fuel());
                })
                .thenWaitUntil(() -> {
                    charge(cable[0]);
                    final FlightComputerBlockEntity rocket = computer(level, axis[0]);
                    helper.assertTrue(rocket.rocket().map(RocketDesign::name).orElse("").equals(NAME),
                            "the rocket's flight computer keeps its plan from the hangar");
                    helper.assertTrue(rocket.page() == FlightPage.LAUNCH && count(level, axis[0]).shown().go(),
                            "on the launcher it opens on its LAUNCH page, all go, its deluge held");
                })
                .thenExecute(() -> {
                    final FlightComputerBlockEntity rocket = computer(level, axis[0]);
                    rocket.press(MASTER, crew);
                    rocket.press(IGNITION, crew);
                    rocket.press(COUNT, crew);
                    helper.assertTrue(phase(level, axis[0]) == LaunchCount.Phase.COUNTING,
                            "the key and the ignition armed in the capsule, COUNT starts the count");
                })
                .thenWaitUntil(() -> helper.assertTrue(phase(level, axis[0]) == LaunchCount.Phase.HOLDING,
                        "it holds at T-6 till LAUNCH is pressed"))
                .thenExecute(() -> computer(level, axis[0]).press(LAUNCH, crew))
                .thenWaitUntil(() -> {
                    flying[0] = count(level, axis[0]).flight().map(id -> MovingStructures.byId(level, id))
                            .orElse(null);
                    helper.assertTrue(flying[0] != null && flying[0].ready()
                            && flying[0].motion() instanceof RocketAscent, "past the deluge poured at ignition, the"
                            + " count runs on to T-0, the rocket released and climbing: " + phase(level, axis[0]));
                })
                .thenWaitUntil(() -> helper.assertTrue(flying[0].pose().y() > axis[0].getY() + 1,
                        "off the pad: " + flying[0].pose().y()))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockEntity(port[0]) instanceof PadServicePortBlockEntity poured
                            && poured.water() == 0, "the port's deluge was poured over the pad at ignition");
                    MovingStructures.takeAway(flying[0]);
                })
                .thenSucceed();
    }

    /* An engine, a tank and a capsule, its flight computer in it. */
    private static RocketStack rocket() {
        return RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }

    private static void fairWeather(final ServerLevel level) {
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
    }

    /*
     * Where the crawler's engine stands under the middle of the deck of the launcher {@code plan} builds in
     * {@code bay}: on the bay's middle line, a crawler carrying only a launcher centred over it.
     */
    private static BlockPos underDeck(final HangarBay bay, final PlannedBuild plan) {
        int near = Integer.MAX_VALUE;
        int far = Integer.MIN_VALUE;
        for (final PlanSpot spot : plan.plan().spots()) {
            if (spot.block().equals(LauncherBlocks.LAUNCHER_DECK.getId())) {
                near = Math.min(near, plan.where(spot).getZ());
                far = Math.max(far, plan.where(spot).getZ());
            }
        }
        return new BlockPos(bay.pad().getX(), bay.pad().getY() + ENGINE_UP, (near + far) / 2);
    }

    /* How many of {@code count} of {@code item} the handler takes, put in as a hopper would, a stack at a time. */
    private static int insert(final IItemHandler handler, final Item item, final int count) {
        int taken = 0;
        int left = count;
        final int most = new ItemStack(item).getMaxStackSize();
        while (left > 0) {
            final int offered = Math.min(left, most);
            final int took = offered - ItemHandlerHelper.insertItem(handler, new ItemStack(item, offered), false)
                    .getCount();
            taken += took;
            left -= offered;
            if (took < offered) {
                break;
            }
        }
        return taken;
    }

    /* A tick's worth of energy into the port's pumps, as a cable run to it keeps them full while they spend it. */
    private static void charge(final IEnergyStorage cable) {
        cable.receiveEnergy(PadServicePortBlockEntity.DELUGE_ENERGY, false);
    }

    /* The measuring hold-down arm of the launcher standing round {@code axis}, if one stands there. */
    private static Optional<LauncherDeckBlockEntity> launcher(final ServerLevel level, final BlockPos axis) {
        return LaunchPads.launcherBy(level, axis, ARM_REACH, LauncherLayout.RAISE, LauncherLayout.RAISE);
    }

    private static LaunchCount count(final ServerLevel level, final BlockPos axis) {
        return launcher(level, axis).map(LauncherDeckBlockEntity::count)
                .orElseThrow(() -> new GameTestAssertException("No launcher stands at " + axis));
    }

    private static LaunchCount.Phase phase(final ServerLevel level, final BlockPos axis) {
        return count(level, axis).shown().phase();
    }

    /* The flight computer of the rocket on the launcher round {@code axis}. */
    private static FlightComputerBlockEntity computer(final ServerLevel level, final BlockPos axis) {
        return launcher(level, axis).flatMap(arm -> LaunchPads.stand(level, arm)).flatMap(LaunchPads.Stand::computer)
                .map(level::getBlockEntity).filter(FlightComputerBlockEntity.class::isInstance)
                .map(FlightComputerBlockEntity.class::cast)
                .orElseThrow(() -> new GameTestAssertException("No rocket stands on the launcher at " + axis));
    }

    private static HangarTerminalBlockEntity terminal(final ServerLevel level, final BlockPos at) {
        return (HangarTerminalBlockEntity) level.getBlockEntity(at);
    }

    private static CrawlerEngineBlockEntity crawler(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof CrawlerEngineBlockEntity found) {
            return found;
        }
        throw new IllegalStateException("No crawler engine at " + at);
    }
}
