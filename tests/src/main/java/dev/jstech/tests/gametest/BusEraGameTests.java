/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.block.part.ImportBusPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.multipart.PartType;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The storage buses of each era: how fast they move and that no cable is outrun, what the faced chest keeps and how
 * many a move takes, the filter of only these or all but these, a keep for each listed item, the priority between
 * buses and the conditions they wait for, the tags and the loose match, where each era mounts, and the activity each
 * move and each hold is written to.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BusEraGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos SOUTH_CHEST = new BlockPos(2, 2, 3);
    private static final BlockPos EAST_CHEST = new BlockPos(3, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);

    private BusEraGameTests() {
    }

    /** The Vintage bus moves an item a tick; in the time the Standard one empties the chest it has moved a few. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void vintage_movesAnItemATick(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.THICK_COAX_CABLE);
        final Container chest = chest(helper, SOUTH_CHEST, new ItemStack(Items.COBBLESTONE, 64));
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.VINTAGE_IMPORT.get());
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    final long moved = stock(helper, mainframe, Items.COBBLESTONE);
                    helper.assertTrue(bus.speed() == 1, "the Vintage bus moves one a tick; got " + bus.speed());
                    helper.assertTrue(moved > 0 && moved <= 40, "a few have gone in thirty ticks; got " + moved);
                    helper.assertTrue(chest.countItem(Items.COBBLESTONE) > 0, "the chest is not empty yet");
                })
                .thenSucceed();
    }

    /** The Standard bus empties the same chest in the same time. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void standard_movesFasterThanTheVintage(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        chest(helper, SOUTH_CHEST, new ItemStack(Items.COBBLESTONE, 64));
        mount(helper, Direction.SOUTH, ComputingParts.IMPORT.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.COBBLESTONE) == 64,
                        "all 64 have gone in"))
                .thenSucceed();
    }

    /** A bus is busy, its lamps blinking, while it moves, and goes idle a few seconds after its last move. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void import_isBusyWhileItMovesAndIdleAfter(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        chest(helper, SOUTH_CHEST, new ItemStack(Items.COBBLESTONE, 64));
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.IMPORT.get());
        helper.assertFalse(bus.busy(), "a bus just mounted is idle");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.COBBLESTONE) == 64,
                        "all 64 have gone in"))
                .thenExecute(() -> helper.assertTrue(bus.busy(), "the bus is busy as it moves"))
                .thenWaitUntil(() -> helper.assertFalse(bus.busy(), "the bus goes idle once it has nothing to move"))
                .thenSucceed();
    }

    /** A bus never moves faster than its cable carries: the Advanced bus on the Legacy's Ethernet moves at 16. */
    @GameTest(template = ARENA)
    public static void advanced_movesNoFasterThanItsCable(final GameTestHelper helper) {
        TestCables.lay(helper, CABLE, ComputingModule.ETHERNET_CABLE);
        final ImportBusPart onEthernet = mount(helper, Direction.SOUTH, ComputingParts.ADVANCED_IMPORT.get());
        final BlockPos other = new BlockPos(4, 2, 2);
        TestCables.lay(helper, other, ComputingModule.OM5_CABLE);
        final ImportBusPart onFibre = ComputingParts.ADVANCED_IMPORT.get().create();
        TestCables.cable(helper, other).addPart(Direction.SOUTH, onFibre);
        helper.assertTrue(onEthernet.speed() == 16, "the Ethernet carries 16; got " + onEthernet.speed());
        helper.assertTrue(onFibre.speed() == 64, "on its own fibre it moves its 64; got " + onFibre.speed());
        helper.succeed();
    }

    /** The Legacy bus leaves in the chest what it was set to keep, and says it is waiting on that. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void legacy_leavesWhatTheChestKeeps(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        final Container chest = chest(helper, SOUTH_CHEST, new ItemStack(Items.COBBLESTONE, 64));
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.LEGACY_IMPORT.get());
        bus.setKeep(16, "");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.COBBLESTONE) == 48,
                        "48 have gone in"))
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 16, "and the chest keeps its 16");
                    helper.assertTrue(has(bus, BusActivity.COMPLETED, BusActivity.MOVED), "its moves are written down");
                    helper.assertTrue(has(bus, BusActivity.WAITING, BusActivity.KEEPS),
                            "and that it waits on what the chest keeps");
                })
                .thenSucceed();
    }

    /** The Legacy filter: only these takes the cobblestone and leaves the dirt. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void legacy_takesOnlyWhatItLists(final GameTestHelper helper) {
        filtered(helper, false, Items.COBBLESTONE, Items.DIRT);
    }

    /** All but these takes the dirt and leaves the cobblestone. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void legacy_takesAllButWhatItLists(final GameTestHelper helper) {
        filtered(helper, true, Items.DIRT, Items.COBBLESTONE);
    }

    /** The Transition bus fills the chest up to a keep of its own for each listed item. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void transition_keepsAQuantityForEachListedItem(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        final Container chest = chest(helper, SOUTH_CHEST, ItemStack.EMPTY);
        final ExportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.TRANSITION_EXPORT.get());
        bus.setFilterSlot(0, new ItemStack(Items.COBBLESTONE), "");
        bus.setFilterSlot(1, new ItemStack(Items.DIRT), "");
        bus.setItemQuantities(0, 5, 0, "");
        bus.setItemQuantities(1, 10, 0, "");
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    seed(helper, mainframe, Items.COBBLESTONE, 64);
                    seed(helper, mainframe, Items.DIRT, 64);
                })
                .thenWaitUntil(() -> helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 5
                        && chest.countItem(Items.DIRT) == 10, "five cobblestone and ten dirt; got "
                        + chest.countItem(Items.COBBLESTONE) + " and " + chest.countItem(Items.DIRT)))
                .thenExecuteAfter(30, () -> helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 5
                        && chest.countItem(Items.DIRT) == 10, "and no more"))
                .thenSucceed();
    }

    /** Two Standard buses want the same item: the one of the higher priority takes it all. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void standard_higherPriorityTakesFirst(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        final Container high = chest(helper, SOUTH_CHEST, ItemStack.EMPTY);
        final Container low = chest(helper, EAST_CHEST, ItemStack.EMPTY);
        final ExportBusPart first = mount(helper, Direction.SOUTH, ComputingParts.EXPORT.get());
        final ExportBusPart second = mount(helper, Direction.EAST, ComputingParts.EXPORT.get());
        for (final ExportBusPart bus : List.of(first, second)) {
            bus.setFilter(new ItemStack(Items.COBBLESTONE));
            bus.setKeep(64, "");
        }
        first.setPriority(5, "");
        second.setPriority(1, "");
        helper.startSequence()
                .thenExecuteAfter(10, () -> seed(helper, mainframe, Items.COBBLESTONE, 20))
                .thenWaitUntil(() -> helper.assertTrue(high.countItem(Items.COBBLESTONE) == 20,
                        "the higher priority's chest has all 20; got " + high.countItem(Items.COBBLESTONE)))
                .thenExecute(() -> helper.assertTrue(low.countItem(Items.COBBLESTONE) == 0,
                        "and the lower one's none"))
                .thenSucceed();
    }

    /** A Standard bus that waits for the network to hold under ten stops bringing them in past that. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void standard_waitsOnTheNetworksStock(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        final Container chest = chest(helper, SOUTH_CHEST, new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64));
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.IMPORT.get());
        bus.setMax(4, "");
        bus.addCondition(BusCondition.stock("minecraft:cobblestone", 10), "Stock keeper");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.COBBLESTONE) >= 10,
                        "ten have gone in"))
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(chest.countItem(Items.COBBLESTONE) > 64, "the bus stopped well before the chest"
                            + " was empty; it holds " + chest.countItem(Items.COBBLESTONE));
                    helper.assertTrue(has(bus, BusActivity.WAITING, BusActivity.HELD), "and says it waits");
                    helper.assertValueEqual(bus.setBy(BusSettings.CONDITIONS), "Stock keeper",
                            "the condition is marked with the program that set it");
                })
                .thenSucceed();
    }

    /**
     * A Standard bus set to the night does not move at noon, and moves at midnight. It rewrites the shared level
     * clock, so it runs in a batch of its own: no other test shares the level while the clock jumps.
     */
    @GameTest(template = ARENA, batch = "jsc_clock", timeoutTicks = 300)
    public static void standard_waitsForItsHours(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        chest(helper, SOUTH_CHEST, new ItemStack(Items.COBBLESTONE, 10));
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.IMPORT.get());
        bus.addCondition(BusCondition.hours(18, 6), "");
        helper.getLevel().setDayTime(6000L);
        helper.startSequence()
                .thenExecuteAfter(40, () -> helper.assertTrue(stock(helper, mainframe, Items.COBBLESTONE) == 0,
                        "at noon nothing went in"))
                .thenExecute(() -> helper.getLevel().setDayTime(18000L))
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.COBBLESTONE) == 10,
                        "at midnight all ten went in"))
                .thenSucceed();
    }

    /** The Advanced bus takes by tag: the logs go in, the cobblestone stays. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void advanced_takesByTag(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        final Container chest = chest(helper, SOUTH_CHEST, new ItemStack(Items.OAK_LOG, 10),
                new ItemStack(Items.COBBLESTONE, 10));
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.ADVANCED_IMPORT.get());
        bus.setTags(List.of(ResourceLocation.withDefaultNamespace("logs")), "");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.OAK_LOG) == 10,
                        "the logs went in"))
                .thenExecuteAfter(20, () -> helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 10,
                        "the cobblestone stayed"))
                .thenSucceed();
    }

    /** The Advanced Export Bus sends by tag: the network's logs go out, its cobblestone stays. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void advanced_sendsByTag(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        final Container chest = chest(helper, SOUTH_CHEST, ItemStack.EMPTY);
        final ExportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.ADVANCED_EXPORT.get());
        bus.setTags(List.of(ResourceLocation.withDefaultNamespace("logs")), "");
        helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    seed(helper, mainframe, Items.OAK_LOG, 12);
                    seed(helper, mainframe, Items.COBBLESTONE, 12);
                })
                .thenWaitUntil(() -> helper.assertTrue(chest.countItem(Items.OAK_LOG) == 12, "the logs went out"))
                .thenExecuteAfter(20, () -> helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 0
                        && stock(helper, mainframe, Items.COBBLESTONE) == 12, "the cobblestone stayed in"))
                .thenSucceed();
    }

    /** Matched loosely, a worn sword is the sword listed; matched exactly, it is not. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void advanced_matchesLoosely(final GameTestHelper helper) {
        network(helper, ComputingModule.HBW_CABLE);
        final ItemStack worn = new ItemStack(Items.IRON_SWORD);
        worn.setDamageValue(30);
        final Container chest = chest(helper, SOUTH_CHEST, worn);
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.ADVANCED_IMPORT.get());
        bus.setFilter(new ItemStack(Items.IRON_SWORD));
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(chest.countItem(Items.IRON_SWORD) == 1, "matched exactly, the worn one stays");
                    bus.setFuzzy(true, "");
                })
                .thenWaitUntil(() -> helper.assertTrue(chest.countItem(Items.IRON_SWORD) == 0,
                        "matched loosely, it goes in"))
                .thenSucceed();
    }

    /** A bus mounts on an access or backbone cable of its era or an earlier one, and not on a later era's. */
    @GameTest(template = ARENA)
    public static void mount_takesItsErasCablesAndNoLaterOne(final GameTestHelper helper) {
        TestCables.lay(helper, CABLE, ComputingModule.GIGABIT_CABLE);
        final BlockPos older = new BlockPos(4, 2, 2);
        TestCables.lay(helper, older, ComputingModule.ETHERNET_CABLE);
        helper.assertFalse(use(helper, CABLE, ComputingModule.LEGACY_IMPORT_BUS_ITEM.get()),
                "a Legacy bus does not mount on the Standard's Gigabit");
        helper.assertTrue(use(helper, older, ComputingModule.LEGACY_IMPORT_BUS_ITEM.get()),
                "it mounts on its own era's Ethernet");
        helper.assertTrue(use(helper, CABLE, ComputingModule.ADVANCED_EXPORT_BUS_ITEM.get()),
                "and an Advanced bus mounts on the earlier Gigabit");
        helper.succeed();
    }

    /*
     * A running Mainframe at (1, 2, 2) with a cable of {@code cable} beside it, where the buses go, and a rack with a
     * server north of the cable, its back to it, which is where the network keeps what comes in.
     */
    private static MainframeBlockEntity network(final GameTestHelper helper, final CableEntry cable) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        TestCables.lay(helper, CABLE, cable);
        world.placeSeededRack(RACK);
        return mainframe;
    }

    /* A barrel at {@code at} holding {@code stacks}. */
    private static Container chest(final GameTestHelper helper, final BlockPos at, final ItemStack... stacks) {
        helper.setBlock(at, Blocks.BARREL);
        if (!(helper.getBlockEntity(at) instanceof Container container)) {
            throw new IllegalStateException("no barrel at " + at.toShortString());
        }
        for (int slot = 0; slot < stacks.length; slot++) {
            container.setItem(slot, stacks[slot].copy());
        }
        return container;
    }

    private static <T extends AbstractBusPart> T mount(final GameTestHelper helper, final Direction face,
                                                       final PartType<T> type) {
        final T bus = type.create();
        TestCables.cable(helper, CABLE).addPart(face, bus);
        return bus;
    }

    private static long stock(final GameTestHelper helper, final MainframeBlockEntity mainframe, final Item item) {
        return mainframe.networkUuid() == null ? 0L
                : NetworkStorage.of(helper.getLevel(), mainframe.networkUuid()).count(item);
    }

    private static void seed(final GameTestHelper helper, final MainframeBlockEntity mainframe, final Item item,
                             final long amount) {
        NetworkStorage.of(helper.getLevel(), mainframe.networkUuid()).insert(StorageKey.of(item), amount);
    }

    private static boolean has(final AbstractBusPart bus, final byte status, final byte reason) {
        return bus.activity().entries().stream().anyMatch(e -> e.status() == status && e.reason() == reason);
    }

    /*
     * A chest of ten cobblestone and ten dirt, and a Legacy bus listing the cobblestone, taking only these or all but
     * these: {@code taken} goes in, {@code left} stays.
     */
    private static void filtered(final GameTestHelper helper, final boolean allBut, final Item taken,
                                 final Item left) {
        final MainframeBlockEntity mainframe = network(helper, ComputingModule.HBW_CABLE);
        final Container chest = chest(helper, SOUTH_CHEST, new ItemStack(Items.COBBLESTONE, 10),
                new ItemStack(Items.DIRT, 10));
        final ImportBusPart bus = mount(helper, Direction.SOUTH, ComputingParts.LEGACY_IMPORT.get());
        bus.setFilterSlot(0, new ItemStack(Items.COBBLESTONE), "");
        bus.setExclude(allBut, "");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, taken) == 10, "ten went in"))
                .thenExecuteAfter(20, () -> helper.assertTrue(chest.countItem(left) == 10, "the other ten stayed"))
                .thenSucceed();
    }

    /* Uses {@code item} on the cable at {@code at} as a player would, and answers whether a bus is now on it. */
    private static boolean use(final GameTestHelper helper, final BlockPos at, final Item item) {
        final Player player = helper.makeMockPlayer(GameType.CREATIVE);
        final BlockPos abs = helper.absolutePos(at);
        final CableBlockEntity cable = TestCables.cable(helper, at);
        final long before = Arrays.stream(Direction.values()).filter(cable::hasPart).count();
        final ItemStack stack = new ItemStack(item);
        final BlockHitResult where = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        item.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, where));
        final long after = Arrays.stream(Direction.values()).filter(cable::hasPart).count();
        return after > before;
    }
}
