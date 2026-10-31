/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.crew.CrewRole;
import dev.jstech.space.crew.ShipConsole;
import dev.jstech.space.crew.ShipConsoleMenu;
import dev.jstech.space.crew.ShipCrew;
import dev.jstech.space.crew.ShipCrews;
import dev.jstech.space.crew.ShipHatches;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Who may do what aboard a starship: its owner flies it, the owner's team works aboard, everyone else stays outside
 * till named, each named with a role of their own; its helm and its blocks are a pilot's, its fittings its crew's, its
 * airlock door, bunks and collar whoever may come aboard. A ship started from its dock is its pilot's, who alone sets
 * its roles on its console; its airlock door takes in whoever may come aboard, and out, and refuses everyone else.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipCrewGameTests {

    private static final String ARENA = "shipyard";
    /* Where the dock's lowest corner stands in the arena. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* How far out from its airlock door a player stands outside, in blocks. */
    private static final int OUTSIDE = 2;
    private static final ShipDesign SHIP = new ShipDesign("JS-09", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
            part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("legs")));

    private ShipCrewGameTests() {
    }

    @GameTest(template = "empty")
    public static void crew_rolesGoToTheOwnerTheTeamTheNamedAndEveryoneElse(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer owner = GameTestPlayers.join(helper, "jst-ship-own");
        final ServerPlayer mate = GameTestPlayers.join(helper, "jst-ship-mate");
        final ServerPlayer stranger = GameTestPlayers.join(helper, "jst-ship-out");
        final ServerPlayer named = GameTestPlayers.join(helper, "jst-ship-nam");
        final Scoreboard scoreboard = level.getServer().getScoreboard();
        final PlayerTeam team = scoreboard.addPlayerTeam("jstests_ship_" + owner.getUUID().toString().substring(0, 8));
        try {
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(mate.getScoreboardName(), team);
            ShipCrew crew = ShipCrew.of(owner.getUUID(), owner.getScoreboardName());
            helper.assertTrue(ShipConsole.teamName(level.getServer(), crew).equals(team.getDisplayName().getString()),
                    "its console names the owner's team as the team is shown");
            helper.assertTrue(ShipConsole.teamName(level.getServer(), ShipCrew.of(stranger.getUUID(), "x")).isEmpty(),
                    "and names no team for an owner on a team of their own");
            helper.assertTrue(crew.roleOf(owner) == CrewRole.PILOT, "its owner flies it");
            helper.assertTrue(crew.roleOf(mate) == CrewRole.CREW, "the owner's team works aboard");
            helper.assertTrue(crew.roleOf(stranger) == CrewRole.NONE, "everyone else stays outside");
            crew = crew.withNamed(named.getUUID(), named.getScoreboardName(), CrewRole.PASSENGER);
            helper.assertTrue(crew.roleOf(named) == CrewRole.PASSENGER, "a player named has their role");
            crew = crew.withNamed(named.getUUID(), named.getScoreboardName(), CrewRole.PILOT);
            helper.assertTrue(crew.roleOf(named) == CrewRole.PILOT && crew.named().size() == 1, "named again, their"
                    + " role changes and they are named once: " + crew.named());
            crew = crew.withTeam(CrewRole.PASSENGER).withOthers(CrewRole.PASSENGER);
            helper.assertTrue(crew.roleOf(mate) == CrewRole.PASSENGER && crew.roleOf(stranger) == CrewRole.PASSENGER,
                    "the team's and everyone else's roles are the owner's to set");
            crew = crew.withNamed(named.getUUID(), named.getScoreboardName(), CrewRole.NONE);
            helper.assertTrue(crew.named().isEmpty() && crew.roleOf(named) == CrewRole.PASSENGER, "taken off, a"
                    + " player named is as everyone else again");
            helper.assertTrue(crew.withNamed(owner.getUUID(), "x", CrewRole.NONE).roleOf(owner) == CrewRole.PILOT,
                    "the owner is never taken off");
        } finally {
            scoreboard.removePlayerTeam(team);
            List.of(owner, mate, stranger, named).forEach(GameTestPlayers::leave);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crew_aPilotHoldsTheHelmAndTheBlocksTheCrewTheFittingsAPassengerTheWayIn(
            final GameTestHelper helper) {
        helper.assertTrue(ShipCrews.needed(ShipKitBlocks.PILOT_SEAT.get(), false, false) == CrewRole.PILOT,
                "the helm is a pilot's");
        helper.assertTrue(ShipCrews.needed(ShipKitBlocks.HULL_PLATING.get(), false, true) == CrewRole.PILOT,
                "and so is putting a block in");
        for (final BlockEntry<Block> fitting : List.of(ShipKitBlocks.FLIGHT_CONSOLE, ShipKitBlocks.SUIT_LOCKER,
                ShipKitBlocks.OXYGEN_PANEL, ShipKitBlocks.AIRLOCK_CYCLER)) {
            helper.assertTrue(ShipCrews.needed(fitting.get(), false, false) == CrewRole.CREW, "its fittings are its"
                    + " crew's: " + fitting.getId());
        }
        helper.assertTrue(ShipCrews.needed(Blocks.FURNACE, true, false) == CrewRole.CREW, "and any machine aboard");
        for (final BlockEntry<Block> way : List.of(ShipKitBlocks.AIRLOCK_DOOR, ShipKitBlocks.BUNK_FRAME,
                ShipKitBlocks.DOCKING_COLLAR)) {
            helper.assertTrue(ShipCrews.needed(way.get(), false, true) == CrewRole.PASSENGER, "its airlock door,"
                    + " bunks and collar are whoever may come aboard's: " + way.getId());
        }
        helper.assertTrue(ShipCrews.needed(ShipKitBlocks.HULL_PLATING.get(), false, false) == CrewRole.NONE,
                "its hull asks for nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 1600)
    public static void crew_aShipStartedIsItsOwnersWhoSetsItsRolesAndItsAirlockDoorTakesInWhoMayBoard(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        // Players with a connection, who are told why they are refused.
        final ServerPlayer owner = GameTestPlayers.connected(helper, "jst-ship-boss");
        final ServerPlayer stranger = GameTestPlayers.connected(helper, "jst-ship-gate");
        final ServerPlayer rider = GameTestPlayers.connected(helper, "jst-ship-ride");
        final MovingStructure[] ship = new MovingStructure[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level.getBlockEntity(
                            terminal);
                    dock.design(SHIP);
                    helper.assertTrue(dock.confirm(), "the ship is sent");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan.plan().spots()) {
                        level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                    ship[0] = dock.release(owner.getUUID()).orElseThrow();
                })
                .thenWaitUntil(() -> helper.assertTrue(ship[0].ready(), "its blocks come into the structure"))
                .thenExecute(() -> {
                    final AirReserveEntity console = console(level, ship[0]);
                    final ShipCrew crew = console.crew().orElseThrow();
                    helper.assertTrue(crew.owner().equals(owner.getUUID()) && crew.team() == CrewRole.CREW
                            && crew.others() == CrewRole.NONE, "started, it is its pilot's, their team its crew,"
                            + " everyone else outside: " + crew);
                    final ShipConsoleMenu menu = new ShipConsoleMenu(0, owner.getInventory(), console,
                            CrewRole.PILOT, true, "");
                    menu.add(stranger, rider.getScoreboardName());
                    helper.assertTrue(console.crew().orElseThrow().named().isEmpty(), "only its owner names a"
                            + " player");
                    menu.add(owner, rider.getScoreboardName());
                    helper.assertTrue(console.crew().orElseThrow().roleOf(rider) == CrewRole.PASSENGER, "its owner"
                            + " names a player by name, aboard as a passenger: " + console.crew());
                    menu.set(owner, new ShipConsole.RoleSet(ShipConsole.RoleSet.TEAM, new UUID(0, 0),
                            CrewRole.PASSENGER));
                    helper.assertTrue(console.crew().orElseThrow().team() == CrewRole.PASSENGER, "and sets the"
                            + " team's role");
                    menu.set(owner, new ShipConsole.RoleSet(ShipConsole.RoleSet.TEAM, new UUID(0, 0),
                            CrewRole.CREW));
                    // The airlock door: the stranger refused at it, the passenger taken in, and out again.
                    final BlockPos door = aboard(ship[0], plan(helper).where(spot(helper,
                            ShipKitBlocks.AIRLOCK_DOOR.getId())));
                    final Direction out = ShipHatches.outward(ship[0], door).orElseThrow();
                    standOutside(level, ship[0], stranger, door, out);
                    final PlayerInteractEvent.RightClickBlock refused = click(stranger, door, out);
                    helper.assertTrue(refused.isCanceled() && refused.getCancellationResult() == InteractionResult.FAIL
                            && side(ship[0], stranger, door, out) > 0, "the airlock door refuses whoever may not"
                            + " come aboard, who stays outside");
                    standOutside(level, ship[0], rider, door, out);
                    final PlayerInteractEvent.RightClickBlock taken = click(rider, door, out);
                    helper.assertTrue(taken.isCanceled() && taken.getCancellationResult() == InteractionResult.SUCCESS
                            && side(ship[0], rider, door, out) < 0, "and takes a passenger in: "
                            + side(ship[0], rider, door, out));
                    click(rider, door, out);
                    helper.assertTrue(side(ship[0], rider, door, out) > 0, "and out again: "
                            + side(ship[0], rider, door, out));
                    MovingStructures.takeAway(ship[0]);
                    List.of(owner, stranger, rider).forEach(GameTestPlayers::leave);
                })
                .thenSucceed();
    }

    /* The console of {@code ship}. */
    private static AirReserveEntity console(final ServerLevel level, final MovingStructure ship) {
        return (AirReserveEntity) level.getBlockEntity(((ShipFlight) ship.motion()).consoleIn(ship).orElseThrow());
    }

    /* {@code player} standing in the world a little way out of {@code ship}'s airlock door at {@code door}. */
    private static void standOutside(final ServerLevel level, final MovingStructure ship, final ServerPlayer player,
                                     final BlockPos door, final Direction out) {
        final BlockPos there = door.relative(out, OUTSIDE);
        final Vec3 world = MovingStructures.inWorld(level, Vec3.atBottomCenterOf(there));
        player.teleportTo(level, world.x, world.y, world.z, 0F, 0F);
    }

    /* {@code player} uses the door at {@code door} from the side {@code out} faces, as the game posts it. */
    private static PlayerInteractEvent.RightClickBlock click(final ServerPlayer player, final BlockPos door,
                                                             final Direction out) {
        return NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND,
                door, new BlockHitResult(Vec3.atCenterOf(door), out, door, false)));
    }

    /* How far out of {@code ship}'s airlock door {@code player} stands, in its yard: above nought outside. */
    private static double side(final MovingStructure ship, final ServerPlayer player, final BlockPos door,
                               final Direction out) {
        final Vec3 at = MovingStructures.toYard(ship, player.position());
        return (at.x - door.getX() - 0.5) * out.getStepX() + (at.z - door.getZ() - 0.5) * out.getStepZ();
    }

    /* Where the block that stood at {@code built} in the dock is aboard {@code ship} now, in its yard. */
    private static BlockPos aboard(final MovingStructure ship, final BlockPos built) {
        return BlockPos.containing(MovingStructures.toYard(ship, Vec3.atCenterOf(built)));
    }

    /* The plan of the ship in the dock, where it was built. */
    private static PlannedBuild plan(final GameTestHelper helper) {
        final BlockPos terminal = helper.absolutePos(CORNER).offset(21, 7, 23);
        return new PlannedBuild(ShipyardPlans.of(helper.getLevel(), SHIP, Orientation.NORTH),
                ShipyardStructure.shipAt(terminal, Orientation.NORTH));
    }

    /* The first place of the ship's plan that wants {@code block}. */
    private static PlanSpot spot(final GameTestHelper helper, final ResourceLocation block) {
        return plan(helper).plan().spots().stream().filter(each -> each.block().equals(block)).findFirst()
                .orElseThrow();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }
}
