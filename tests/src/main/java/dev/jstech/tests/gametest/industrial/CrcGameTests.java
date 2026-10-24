/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.jstechseries.builder.BuildJobs;
import dev.jstech.core.jstechseries.builder.BuilderState;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.ShapeMatch;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.Ownership;
import dev.jstech.industrial.IndustrialFluids;
import dev.jstech.industrial.crc.CrcBlock;
import dev.jstech.industrial.crc.CrcBlockEntity;
import dev.jstech.industrial.crc.CrcCalls;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.crc.CrcLayout;
import dev.jstech.industrial.crc.CrcPart;
import dev.jstech.industrial.crc.CrcReadings;
import dev.jstech.industrial.crc.CrcStructure;
import dev.jstech.industrial.research.IndustrialResearch;
import dev.jstech.industrial.research.ResearchLink;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.testkit.gametest.ScenarioBuilder;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCrcResearch;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * J's Industrial's Computational Research Centre, built in the world of its own blocks: it forms with exactly one
 * Power Entry, its entries only on the two lower rows of its rear, the coolant's inlet only with its outlet; once
 * formed its blocks merge into its model and its entries draw nothing of their own. Its processors work a discovery
 * on, spending its data a part of the way at a time; past 100 degrees the part that heats most burns; its liquid
 * coolers turn the water flowing through them hot; and two CRCs on one Research Link Cable work at the strongest's
 * pace and 90% of the other's. Its menu centres the player's inventory under its wider screen and leaves it off its
 * Research and Group tabs; its screen reads its team's research, data and group; and only who may set it up chooses
 * and queues its discoveries.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CrcGameTests {

    private static final String ARENA = "tall";
    private static final String BATCH = "jsindustrial_crc";
    /* The terminal in the middle layer, the body running south of it to the rear, as the pattern is drawn. */
    private static final BlockPos TERMINAL = new BlockPos(4, 3, 1);
    private static final BlockPos SECOND_TERMINAL = new BlockPos(8, 3, 1);
    private static final int[] POWER_PLACE = {1, 0};
    private static final int FORMS_WITHIN = 200;
    /* Ticks a structure takes to check a change once it settles, and a margin, well under the CRC's second. */
    private static final int SEES_A_CHANGE = 8;
    private static final double CLOSE = 1e-6;

    private CrcGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = FORMS_WITHIN)
    public static void crc_formsOfItsOwnBlocksAndItsEntriesDrawNothingOfTheirOwn(final GameTestHelper helper) {
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get(),
                cell(0, 1), CrcContent.LINK_SOCKET.get()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed() && entryFormed(helper, TERMINAL, 1, 0)
                        && entryFormed(helper, TERMINAL, 0, 1), "it forms, and flags its entries formed; "
                        + crc.structure().failure()))
                .thenExecute(() -> helper.assertTrue(helper.getBlockState(TERMINAL).getValue(CrcBlock.FORMED),
                        "its terminal too"))
                .thenExecute(() -> helper.setBlock(TERMINAL.offset(0, 0, 4), CrcContent.CASING.get()))
                .thenWaitUntil(() -> helper.assertTrue(!crc.formed() && !entryFormed(helper, TERMINAL, 1, 0),
                        "a casing where a gate goes undoes it, and its entries draw themselves again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void terminal_standsInTheMiddleOfTheFrontAtAPlayersEyes(final GameTestHelper helper) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        helper.assertTrue(pattern.terminalX() == 1 && pattern.terminalY() == 1 && pattern.terminalZ() == 0,
                "the terminal is the middle of the front's middle layer: " + pattern.terminalX() + ", "
                        + pattern.terminalY() + ", " + pattern.terminalZ());
        helper.assertTrue(CrcStructure.rearPlace(BlockPos.ZERO, Orientation.NORTH, 0, 0).equals(new BlockPos(-1, -1,
                8)), "the rear's bottom row is a layer under the terminal");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = FORMS_WITHIN)
    public static void entries_areDrawnWhereTheyStandAsSoonAsTheStructureSeesThem(final GameTestHelper helper) {
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        final int westUpper = CrcBlockEntity.PLACES + CrcStructure.REAR_COLUMNS;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed(), "it forms"))
                .thenExecute(() -> helper.setBlock(rear(TERMINAL, 0, 1), facingOut(CrcContent.LINK_SOCKET.get())))
                .thenExecuteAfter(SEES_A_CHANGE, () -> helper.assertTrue(crc.shown().size() > westUpper
                        && crc.shown().get(westUpper) == CrcBlockEntity.CELL_LINK
                        && entryFormed(helper, TERMINAL, 0, 1),
                        "the link socket put in the west column's upper place is drawn there at once: " + crc.shown()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = FORMS_WITHIN)
    public static void breakingTheTerminal_givesEveryMergedBlockBackInTheSameTick(final GameTestHelper helper) {
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        final BlockPos frontCorner = TERMINAL.offset(-1, -1, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed() && helper.getBlockState(frontCorner)
                        .is(FormedParts.BLOCK.get()), "it forms, its casing merged into its model"))
                .thenExecute(() -> {
                    helper.destroyBlock(TERMINAL);
                    helper.assertTrue(helper.getBlockState(frontCorner).is(CrcContent.CASING.get()),
                            "the casing is back the moment the terminal breaks: " + helper.getBlockState(frontCorner));
                    helper.assertTrue(!entryFormed(helper, TERMINAL, 1, 0), "and the entry draws itself again");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = FORMS_WITHIN)
    public static void clickOnTheFront_opensTheTerminalWherever(final GameTestHelper helper) {
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        final BlockPos frontCorner = TERMINAL.offset(1, 1, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed() && helper.getBlockState(frontCorner)
                        .is(FormedParts.BLOCK.get()), "it forms"))
                .thenExecute(() -> {
                    final ServerPlayer player = GameTestPlayers.connected(helper, "crc-front");
                    try {
                        player.moveTo(Vec3.atCenterOf(helper.absolutePos(TERMINAL.north(2))));
                        click(helper, player, frontCorner.south(), Direction.NORTH);
                        helper.assertTrue(!(player.containerMenu instanceof MachineMenu),
                                "a block behind the front, clicked, opens nothing");
                        click(helper, player, frontCorner, Direction.EAST);
                        helper.assertTrue(!(player.containerMenu instanceof MachineMenu),
                                "nor the front's corner clicked on its side");
                        click(helper, player, frontCorner, Direction.NORTH);
                        helper.assertTrue(player.containerMenu instanceof MachineMenu menu && menu.machine() == crc,
                                "the front's corner clicked on the front opens the CRC's screen");
                    } finally {
                        GameTestPlayers.leave(player);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void builder_putsInTheEntryTheCrcMustHaveSoItForms(final GameTestHelper helper) {
        helper.setBlock(TERMINAL, CrcContent.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "crc-builder".getBytes(StandardCharsets.UTF_8)), "crc-builder"));
        player.getInventory().clearContent();
        final BlockPos stand = helper.absolutePos(TERMINAL.north(2));
        player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        player.getInventory().add(new ItemStack(CrcContent.CASING.get(), 128));
        player.getInventory().add(new ItemStack(CrcContent.GATE.get(), 3));
        player.getInventory().add(new ItemStack(CrcContent.BUS.get(), 4));
        player.getInventory().add(new ItemStack(CrcContent.POWER_ENTRY.get(), 1));
        BuildJobs.start(player, helper.absolutePos(TERMINAL), BuilderState.DEFAULT);
        helper.succeedWhen(() -> {
            final CrcBlockEntity crc = helper.getBlockEntity(TERMINAL);
            helper.assertTrue(crc.formed(), "it forms once built; " + crc.structure().failure());
            helper.assertTrue(player.getInventory().countItem(CrcContent.POWER_ENTRY.get().asItem()) == 0,
                    "having put in the power entry it must have, though casing fits there too");
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = FORMS_WITHIN)
    public static void crc_takesItsEntriesOnlyWhereItSaysAndOnePowerEntry(final GameTestHelper helper) {
        // The power entry in the vent's row: the top of the rear, where no entry goes.
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 2), CrcContent.POWER_ENTRY.get()));
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(!crc.formed() && crc.structure().failure()
                        .filter(wrong -> wrong.wrong() == ShapeMatch.Wrong.BLOCK).isPresent(),
                        "an entry in the vent is a wrong block; " + crc.structure().failure()))
                .thenExecute(() -> {
                    helper.setBlock(rear(TERMINAL, 1, 2), CrcContent.CASING.get());
                    helper.setBlock(rear(TERMINAL, 0, 0), facingOut(CrcContent.COOLANT_INLET.get()));
                })
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(!crc.formed() && crc.structure().failure()
                        .filter(wrong -> wrong.wrong() == ShapeMatch.Wrong.COUNT
                                && wrong.wanted().equals(CrcStructure.POWER_ENTRY)).isPresent(),
                        "without a power entry it does not form; " + crc.structure().failure()))
                .thenExecute(() -> helper.setBlock(rear(TERMINAL, 1, 0), facingOut(CrcContent.POWER_ENTRY.get())))
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(!crc.formed() && crc.structure().failure()
                        .filter(wrong -> wrong.wrong() == ShapeMatch.Wrong.APART).isPresent(),
                        "an inlet with no outlet does not form; " + crc.structure().failure()))
                .thenExecute(() -> helper.setBlock(rear(TERMINAL, 2, 0), facingOut(CrcContent.COOLANT_OUTLET.get())))
                .thenWaitUntil(() -> helper.assertTrue(crc.formed(), "with its outlet it does"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void crc_worksADiscoveryOnSpendingItsData(final GameTestHelper helper) {
        final UUID owner = UUID.randomUUID();
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        crc.own(Ownership.of(owner));
        seat(crc, 0, CrcPart.NANOSCALE_RESEARCH_PROCESSOR);
        crc.getInventory().setStackInSlot(CrcBlockEntity.FIRST_DATA,
                new ItemStack(IndustrialResearch.SAMPLE_DATA.get(), 2));
        helper.onEachTick(() -> crc.power().portStore().setStored(crc.power().portStore().capacity()));
        final String team = CoreTeams.teamOf(helper.getLevel().getServer(), owner);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed(), "it forms"))
                .thenExecute(() -> crc.choose(helper.getLevel(), TestCrcResearch.SHORT.id()))
                .thenWaitUntil(() -> helper.assertTrue(TeamProgress.knows(helper.getLevel().getServer(), team,
                        TestCrcResearch.SHORT.id()), "it makes the discovery; share "
                        + TeamProgress.share(helper.getLevel().getServer(), team, TestCrcResearch.SHORT.id())))
                .thenExecute(() -> helper.assertTrue(crc.getInventory().getStackInSlot(CrcBlockEntity.FIRST_DATA)
                        .isEmpty(), "and spent both sample data on the way"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void crc_burnsThePartThatHeatsMostPastAHundredDegrees(final GameTestHelper helper) {
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        crc.own(Ownership.of(UUID.randomUUID()));
        for (int place = 0; place < CrcBlockEntity.PLACES; place++) {
            seat(crc, place, place == 0 ? CrcPart.HEAT_SINK : CrcPart.NANOSCALE_RESEARCH_PROCESSOR);
        }
        helper.onEachTick(() -> crc.power().portStore().setStored(crc.power().portStore().capacity()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed(), "it forms"))
                .thenExecute(() -> crc.choose(helper.getLevel(), TestCrcResearch.LONG.id()))
                .thenWaitUntil(() -> helper.assertTrue(crc.part(1).isEmpty() || countProcessors(crc) < 26,
                        "past 100 degrees a processor burns; at " + crc.temperature() + " K"))
                .thenExecute(() -> helper.assertTrue(crc.part(0).equals(Optional.of(CrcPart.HEAT_SINK)),
                        "the heat sink, which heats nothing, stays"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void crc_liquidCoolersTurnTheWaterFlowingThroughThemHot(final GameTestHelper helper) {
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get(),
                cell(0, 0), CrcContent.COOLANT_INLET.get(), cell(2, 0), CrcContent.COOLANT_OUTLET.get()));
        crc.own(Ownership.of(UUID.randomUUID()));
        for (int place = 0; place < CrcBlockEntity.PLACES; place++) {
            seat(crc, place, place < 20 ? CrcPart.NANOSCALE_RESEARCH_PROCESSOR : CrcPart.LIQUID_COOLER);
        }
        helper.onEachTick(() -> {
            crc.power().portStore().setStored(crc.power().portStore().capacity());
            crc.coolantIn().fill(new FluidStack(Fluids.WATER, crc.coolantIn().getSpace()),
                    IFluidHandler.FluidAction.EXECUTE);
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed(), "it forms"))
                .thenExecute(() -> crc.choose(helper.getLevel(), TestCrcResearch.LONG.id()))
                .thenWaitUntil(() -> helper.assertTrue(crc.coolantOut().getFluid().is(IndustrialFluids.HOT_WATER.get())
                        && crc.coolantOut().getFluidAmount() > 0, "its water comes out hot once it is hotter than"
                        + " hot water; at " + crc.temperature() + " K"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void group_countsTheStrongestWholeAndTheOtherAtNinetyPercent(final GameTestHelper helper) {
        final UUID owner = UUID.randomUUID();
        final CrcBlockEntity first = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get(),
                cell(2, 0), CrcContent.LINK_SOCKET.get()));
        final CrcBlockEntity second = build(helper, SECOND_TERMINAL, Map.of(cell(1, 0),
                CrcContent.POWER_ENTRY.get(), cell(0, 0), CrcContent.LINK_SOCKET.get()));
        for (final CrcBlockEntity crc : new CrcBlockEntity[] {first, second}) {
            crc.own(Ownership.of(owner));
            seat(crc, CrcBlockEntity.PLACES - 1, CrcPart.HEAT_SINK);
        }
        seat(first, 0, CrcPart.RESEARCH_PROCESSOR);
        seat(first, 1, CrcPart.RESEARCH_PROCESSOR);
        seat(second, 0, CrcPart.RESEARCH_PROCESSOR);
        // The cable runs behind both rears, from one Link Socket to the other.
        final BlockPos from = rear(TERMINAL, 2, 0).south();
        final BlockPos to = rear(SECOND_TERMINAL, 0, 0).south();
        for (int x = from.getX(); x <= to.getX(); x++) {
            ScenarioBuilder.layCable(helper.getLevel(), helper.absolutePos(new BlockPos(x, from.getY(), from.getZ())),
                    ResearchLink.CABLE.get());
        }
        helper.onEachTick(() -> {
            first.power().portStore().setStored(first.power().portStore().capacity());
            second.power().portStore().setStored(second.power().portStore().capacity());
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(first.formed() && second.formed(), "both form"))
                .thenWaitUntil(() -> helper.assertTrue(first.group(helper.getLevel()).contains(second),
                        "the cable joins them into one group"))
                .thenExecute(() -> second.choose(helper.getLevel(), TestCrcResearch.LONG.id()))
                .thenWaitUntil(() -> helper.assertTrue(Math.abs(first.pace() - 2.9) < CLOSE
                        && Math.abs(second.pace() - 2.9) < CLOSE, "two points whole and one at 90%: 2.9 a second;"
                        + " got " + first.pace() + " and " + second.pace()))
                .thenExecute(() -> helper.assertTrue(first.chosenDiscovery().equals(Optional.of(
                        TestCrcResearch.LONG.id())), "chosen on either, the group works it"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void menu_centresTheInventoryAndLeavesItOffResearchAndGroup(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "crc_menu");
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        crc.own(Ownership.of(owner.getUUID()));
        final MachineMenu menu = new MachineMenu(0, owner.getInventory(), crc);
        helper.assertTrue(menu.tabs() == MachineScreenLayout.TABS + 2, "five tabs of every machine and two of its own: "
                + menu.tabs());
        final Slot inventory = menu.slots.stream().filter(slot -> slot.container instanceof Inventory
                && slot.getContainerSlot() == Inventory.getSelectionSize()).findFirst().orElseThrow();
        helper.assertTrue(inventory.x == CrcLayout.INVENTORY_X + 8 && inventory.y == CrcLayout.INVENTORY_Y + 18,
                "the player's inventory is centred under its wider window: " + inventory.x + ", " + inventory.y);
        final Slot place = menu.faceSlots().getFirst();
        helper.assertTrue(place.x == CrcLayout.place(0).x() && place.y == CrcLayout.place(0).y(),
                "its first place is where its screen draws it");
        menu.clickMenuButton(owner, MachineMenu.FIRST_TAB + CrcLayout.RESEARCH_TAB);
        helper.assertTrue(menu.tab() == CrcLayout.RESEARCH_TAB && !inventory.isActive(),
                "its Research tab is a tab of the menu's, with no inventory under it");
        menu.clickMenuButton(owner, MachineMenu.FIRST_TAB + CrcLayout.GROUP_TAB);
        helper.assertTrue(menu.tab() == CrcLayout.GROUP_TAB && !inventory.isActive(), "nor under Group");
        menu.clickMenuButton(owner, MachineMenu.FIRST_TAB + MachineMenu.SIDES);
        helper.assertTrue(inventory.isActive(), "a tab every machine has shows it");
        if (crc.power().protection() != null) {
            final Slot fuse = menu.slots.stream().filter(slot -> slot instanceof SlotItemHandler handler
                    && handler.getItemHandler() == crc.power().protection().slots()).findFirst().orElseThrow();
            helper.assertTrue(fuse.x == MachineScreenLayout.fuse().x() + MachineScreenLayout.kitShift(CrcLayout.WIDTH),
                    "the fuse sits where the Protection tab's centred face draws it: " + fuse.x);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void screen_readsItsTeamsResearchItsDataAndItsGroup(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "crc_reader");
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        crc.own(Ownership.of(owner.getUUID()));
        seat(crc, 0, CrcPart.RESEARCH_PROCESSOR);
        seat(crc, 1, CrcPart.HEAT_SINK);
        crc.getInventory().setStackInSlot(CrcBlockEntity.FIRST_DATA,
                new ItemStack(IndustrialResearch.SAMPLE_DATA.get(), 3));
        helper.onEachTick(() -> crc.power().portStore().setStored(crc.power().portStore().capacity()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(crc.formed(), "it forms"))
                .thenExecute(() -> crc.choose(helper.getLevel(), TestCrcResearch.LONG.id()))
                .thenWaitUntil(() -> {
                    final CrcReadings read = crc.screenData().read().get();
                    helper.assertTrue(read.share(TestCrcResearch.LONG.id()) > 0.0 && read.points() > 0.0F
                            && read.draw() > 0L, "it reads its team's share and what it computes and draws; "
                            + read);
                })
                .thenExecute(() -> {
                    final CrcReadings read = crc.screenData().read().get();
                    helper.assertTrue(read.held(IndustrialResearch.SAMPLE.id()) == 3,
                            "the data its team holds in its group");
                    helper.assertTrue(!read.team().isEmpty() && read.group().members().size() == 1
                            && read.group().members().getFirst().pos().equals(crc.getBlockPos())
                            && read.group().cable() == CrcReadings.Group.NO_CABLE,
                            "a group of itself alone, joined by no cable; " + read.group());
                    helper.assertTrue(read.ready(TestCrcResearch.SHORT) && !read.made(TestCrcResearch.SHORT.id()),
                            "a discovery that comes after nothing may start");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void calls_chooseAndQueueOnlyForWhoMaySetItUp(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "crc_chooser");
        final FakePlayer stranger = player(helper, "crc_stranger");
        final CrcBlockEntity crc = build(helper, TERMINAL, Map.of(cell(1, 0), CrcContent.POWER_ENTRY.get()));
        crc.own(Ownership.of(owner.getUUID()));
        for (final FakePlayer player : new FakePlayer[] {owner, stranger}) {
            player.setPos(Vec3.atCenterOf(helper.absolutePos(TERMINAL.north())));
            player.containerMenu = new MachineMenu(0, player.getInventory(), crc);
        }
        helper.assertTrue(CrcCalls.CHOOSE.receive(owner, TestCrcResearch.SHORT.id())
                && crc.chosenDiscovery().equals(Optional.of(TestCrcResearch.SHORT.id())), "its owner chooses");
        helper.assertTrue(CrcCalls.ENQUEUE.receive(owner, TestCrcResearch.LONG.id())
                && crc.queue().equals(List.of(TestCrcResearch.LONG.id())), "and queues");
        CrcCalls.CHOOSE.receive(stranger, TestCrcResearch.LONG.id());
        helper.assertTrue(crc.chosenDiscovery().equals(Optional.of(TestCrcResearch.SHORT.id())),
                "a stranger chooses nothing");
        CrcCalls.ENQUEUE.receive(owner, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "no_such_discovery"));
        helper.assertTrue(crc.queue().size() == 1, "and nobody queues what is not there to research");
        helper.succeed();
    }

    /* A player with a name of its own, whom the server never sees join. */
    private static FakePlayer player(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    /* Builds a CRC facing north with its terminal at {@code terminal}, {@code entries} in its rear places. */
    private static CrcBlockEntity build(final GameTestHelper helper, final BlockPos terminal,
                                        final Map<Integer, Block> entries) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final BlockPos at = terminal.offset(x - pattern.terminalX(), y - pattern.terminalY(),
                            z - pattern.terminalZ());
                    final char letter = pattern.charAt(x, y, z);
                    if (letter == '#') {
                        helper.setBlock(at, CrcContent.TERMINAL.get().defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    } else if (letter == 'G') {
                        helper.setBlock(at, CrcContent.GATE.get());
                    } else if (letter == 'B') {
                        helper.setBlock(at, CrcContent.BUS.get());
                    } else {
                        helper.setBlock(at, CrcContent.CASING.get());
                    }
                }
            }
        }
        entries.forEach((cell, block) -> helper.setBlock(rear(terminal, cell / 10, cell % 10), facingOut(block)));
        return helper.getBlockEntity(terminal);
    }

    /* A rear cell as a number, column and row. */
    private static int cell(final int column, final int row) {
        return column * 10 + row;
    }

    /* The rear place of {@code column} (0 west) and {@code row} (0 bottom) of the CRC whose terminal is there. */
    private static BlockPos rear(final BlockPos terminal, final int column, final int row) {
        return CrcStructure.rearPlace(terminal, Orientation.NORTH, column, row);
    }

    /* {@code player} clicks with an empty hand on {@code face} of the block at {@code relative}. */
    private static void click(final GameTestHelper helper, final ServerPlayer player, final BlockPos relative,
                              final Direction face) {
        final BlockPos at = helper.absolutePos(relative);
        final Vec3 where = Vec3.atCenterOf(at).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        helper.getBlockState(relative).useWithoutItem(helper.getLevel(), player, new BlockHitResult(where, face, at,
                false));
    }

    private static BlockState facingOut(final Block block) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
    }

    private static boolean entryFormed(final GameTestHelper helper, final BlockPos terminal, final int column,
                                       final int row) {
        final BlockState state = helper.getBlockState(rear(terminal, column, row));
        return state.hasProperty(CrcBlock.FORMED) && state.getValue(CrcBlock.FORMED);
    }

    private static void seat(final CrcBlockEntity crc, final int place, final CrcPart part) {
        crc.getInventory().setStackInSlot(place, new ItemStack(CrcContent.PARTS.get(part).get()));
    }

    private static int countProcessors(final CrcBlockEntity crc) {
        int count = 0;
        for (int place = 0; place < CrcBlockEntity.PLACES; place++) {
            if (crc.part(place).filter(CrcPart::processor).isPresent()) {
                count++;
            }
        }
        return count;
    }
}
