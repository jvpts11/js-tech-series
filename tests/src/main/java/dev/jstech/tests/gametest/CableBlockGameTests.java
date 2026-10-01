/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.core.cable.BundleShape;
import dev.jstech.core.cable.CableBlock;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.testkit.TestCables;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's shared cable block: each line in its own lane, wires crossing faces alone in the middle and together in
 * their lanes, the junction box where one changes place, wires of one block apart in the grid, colours joining their
 * own and the uncoloured, laying, breaking and dyeing the wire looked at, the long-distance line alone in its block,
 * and the cables of a mod declared through the Core.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CableBlockGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos A = new BlockPos(1, 2, 2);
    private static final BlockPos B = new BlockPos(2, 2, 2);
    private static final BlockPos C = new BlockPos(3, 2, 2);
    private static final double PIXEL = 1.0 / 16.0;
    /* Where a lane runs across a block along x, in pixels of z and of y. */
    private static final double MIDDLE = 8.0;

    private CableBlockGameTests() {
    }

    // The lanes and the junction box

    @GameTest(template = ARENA)
    public static void bundle_laysEachLineInItsOwnLane(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, B, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, B, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, B, ComputingModule.HPC_CABLE);
        TestCables.lay(helper, B, ComputingModule.CRAFTING_CABLE);

        same(helper, 4, cable.wires().size(), "four wires in one block");
        same(helper, Lane.TOP_LEFT, wire(cable, ComputingModule.ETHERNET_CABLE).slot(), "access top left");
        same(helper, Lane.TOP, wire(cable, ComputingModule.HBW_CABLE).slot(), "backbone top middle");
        same(helper, Lane.MIDDLE, wire(cable, ComputingModule.HPC_CABLE).slot(), "compute in the middle");
        same(helper, Lane.RIGHT, wire(cable, ComputingModule.CRAFTING_CABLE).slot(), "crafting middle right");
        helper.assertTrue(!Cables.lay(helper.getLevel(), helper.absolutePos(B), ComputingModule.ETHERNET_CABLE.get()),
                "the same cable twice in one block");
        helper.assertTrue(!cable.takes(Wire.of(ComputingModule.ETHERNET_CABLE.get()).dyed(DyeColor.RED)),
                "the same cable in another colour takes the lane already taken");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wires_crossAloneInTheMiddleAndTogetherInTheirLanes(final GameTestHelper helper) {
        for (final BlockPos at : List.of(A, B, C)) {
            TestCables.lay(helper, at, ComputingModule.ETHERNET_CABLE);
            TestCables.lay(helper, at, ComputingModule.HBW_CABLE);
        }
        final BlockPos alone = new BlockPos(2, 2, 4);
        TestCables.lay(helper, alone, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, alone.east(), ComputingModule.ETHERNET_CABLE);

        helper.succeedWhen(() -> {
            final CableBlockEntity shared = TestCables.cable(helper, B);
            final CableType ethernet = ComputingModule.ETHERNET_CABLE.get();
            helper.assertTrue(shared.crosses(ethernet, Direction.WEST) && shared.crosses(ethernet, Direction.EAST),
                    "Ethernet runs on through");
            final BundleShape shape = shared.shape();
            helper.assertTrue(!shape.junction(), "two runs side by side need no junction box");
            // Along x, a lane's across is z and its up is y, five pixels from one lane to the next.
            same(helper, List.of(new BundleShape.Box(0, 11, 1, 16, 15, 5)), jackets(shape, 0),
                    "Ethernet straight through, in its lane");
            same(helper, List.of(new BundleShape.Box(0, 11, 6, 16, 15, 10)), jackets(shape, 1),
                    "HBW straight through, in its lane");
            final BundleShape single = TestCables.cable(helper, alone).shape();
            same(helper, List.of(new BundleShape.Box(6, 6, 6, 10, 10, 10), new BundleShape.Box(10, 6, 6, 16, 10, 10)),
                    jackets(single, 0), "a wire alone in the middle, its core and its arm east");
        });
    }

    @GameTest(template = ARENA)
    public static void junction_boxesTheBlockWhereAWireTurnsOff(final GameTestHelper helper) {
        for (final BlockPos at : List.of(A, B, C)) {
            TestCables.lay(helper, at, ComputingModule.HBW_CABLE);
        }
        TestCables.lay(helper, A, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, B, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, B.north(), ComputingModule.ETHERNET_CABLE);

        helper.succeedWhen(() -> {
            helper.assertTrue(TestCables.cable(helper, B).shape().junction(),
                    "where Ethernet leaves the run, the block is a junction box");
            helper.assertTrue(!TestCables.cable(helper, A).shape().junction(),
                    "where both run side by side, it is not");
            helper.assertTrue(!TestCables.cable(helper, C).shape().junction(), "nor where HBW runs alone");
        });
    }

    @GameTest(template = ARENA)
    public static void wiresOfOneBlock_standApartInTheGrid(final GameTestHelper helper) {
        TestCables.lay(helper, A, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, B, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, B, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, C, ComputingModule.HBW_CABLE);

        helper.succeedWhen(() -> {
            final Grid data = CoreGrids.of(helper.getLevel(), GridKind.DATA);
            final long ethernetA = number(helper, A, ComputingModule.ETHERNET_CABLE);
            final long ethernetB = number(helper, B, ComputingModule.ETHERNET_CABLE);
            final long hbwB = number(helper, B, ComputingModule.HBW_CABLE);
            final long hbwC = number(helper, C, ComputingModule.HBW_CABLE);
            helper.assertTrue(ethernetB != hbwB, "each wire of the block is a place of its own");
            helper.assertTrue(data.connected(ethernetA, ethernetB), "Ethernet joins the Ethernet beside it");
            helper.assertTrue(data.connected(hbwB, hbwC), "HBW joins the HBW beside it");
            helper.assertTrue(!data.connected(ethernetB, hbwB), "the two wires of one block never join");
        });
    }

    // The colours

    @GameTest(template = ARENA)
    public static void colours_joinTheirOwnAndTheUncoloured(final GameTestHelper helper) {
        final CableType hbw = ComputingModule.HBW_CABLE.get();
        TestCables.lay(helper, A, ComputingModule.HBW_CABLE).dye(hbw, DyeColor.RED);
        TestCables.lay(helper, C, ComputingModule.HBW_CABLE).dye(hbw, DyeColor.BLUE);
        TestCables.lay(helper, B, ComputingModule.HBW_CABLE);
        final BlockPos red = new BlockPos(1, 2, 4);
        final BlockPos blue = new BlockPos(2, 2, 4);
        TestCables.lay(helper, red, ComputingModule.HBW_CABLE).dye(hbw, DyeColor.RED);
        TestCables.lay(helper, blue, ComputingModule.HBW_CABLE).dye(hbw, DyeColor.BLUE);

        helper.succeedWhen(() -> {
            final Grid data = CoreGrids.of(helper.getLevel(), GridKind.DATA);
            helper.assertTrue(TestCables.cable(helper, B).crosses(hbw, Direction.WEST)
                    && TestCables.cable(helper, B).crosses(hbw, Direction.EAST), "the uncoloured joins red and blue");
            helper.assertTrue(data.connected(number(helper, A, ComputingModule.HBW_CABLE),
                    number(helper, C, ComputingModule.HBW_CABLE)), "red and blue meet through the uncoloured");
            helper.assertTrue(!TestCables.cable(helper, red).crosses(hbw, Direction.EAST),
                    "red and blue side by side draw no link");
            helper.assertTrue(!data.connected(number(helper, red, ComputingModule.HBW_CABLE),
                    number(helper, blue, ComputingModule.HBW_CABLE)), "and do not join");
            helper.assertTrue(TestCables.cable(helper, A).shape().pieces().stream()
                    .anyMatch(piece -> piece.kind() == BundleShape.Kind.RING), "a dyed wire wears its ring");
            helper.assertTrue(TestCables.cable(helper, B).shape().pieces().stream()
                    .noneMatch(piece -> piece.kind() == BundleShape.Kind.RING), "an uncoloured one wears none");
        });
    }

    // Laying, breaking and dyeing the wire looked at

    @GameTest(template = ARENA)
    public static void cableItem_laysIntoTheBundleClickedOrBesideIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        TestCables.lay(helper, B, ComputingModule.HBW_CABLE);
        final FakePlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ComputingModule.ETHERNET_CABLE.stack(8));

        use(helper, player, B, Direction.UP);
        helper.assertTrue(TestCables.cable(helper, B).holds(ComputingModule.ETHERNET_CABLE.get()),
                "clicking the bundle lays the cable in it");
        use(helper, player, B, Direction.UP);
        helper.assertTrue(Cables.holds(level, helper.absolutePos(B.above()), ComputingModule.ETHERNET_CABLE.get()),
                "clicking it again, with the cable in it, lays the next block against the face clicked");
        player.setShiftKeyDown(true);
        use(helper, player, B.above(), Direction.NORTH);
        player.setShiftKeyDown(false);
        helper.assertTrue(Cables.holds(level, helper.absolutePos(B.above().north()),
                ComputingModule.ETHERNET_CABLE.get()), "sneaking lays it beside, never in the block clicked");
        same(helper, 5, player.getMainHandItem().getCount(), "one cable spent for each laid");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void breaking_takesOutTheWireLookedAtLaneByLane(final GameTestHelper helper) {
        final List<CableEntry> four = List.of(ComputingModule.ETHERNET_CABLE, ComputingModule.HBW_CABLE,
                ComputingModule.HPC_CABLE, ComputingModule.CRAFTING_CABLE);
        for (final BlockPos at : List.of(A, B, C)) {
            four.forEach(cable -> TestCables.lay(helper, at, cable));
        }
        final FakePlayer player = player(helper);
        final BlockPos pos = helper.absolutePos(B);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    // From above, along x: access top left (z 3), backbone above compute (z 8), crafting right (z 13).
                    breakLookingDown(helper, player, 3);
                    leaves(helper, ComputingModule.ETHERNET_CABLE, ComputingModule.HBW_CABLE,
                            ComputingModule.HPC_CABLE, ComputingModule.CRAFTING_CABLE);
                    breakLookingDown(helper, player, MIDDLE);
                    leaves(helper, ComputingModule.HBW_CABLE, ComputingModule.HPC_CABLE,
                            ComputingModule.CRAFTING_CABLE);
                    breakLookingDown(helper, player, MIDDLE);
                    leaves(helper, ComputingModule.HPC_CABLE, ComputingModule.CRAFTING_CABLE);
                })
                .thenExecute(() -> {
                    helper.assertTrue(player.getInventory().countItem(ComputingModule.ETHERNET_CABLE.asItem()) == 1
                                    && player.getInventory().countItem(ComputingModule.HPC_CABLE.asItem()) == 1,
                            "each wire taken out comes back to the player");
                    // The last piece takes the block with it, as any block goes.
                    player.gameMode.destroyBlock(pos);
                    helper.assertBlockNotPresent(CoreCables.BLOCK.get(), B);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void breaking_theWholeBlockDropsEveryWireAndPart(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, B, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, B, ComputingModule.HBW_CABLE);
        cable.addPart(Direction.UP, ComputingParts.IMPORT.get().create());

        helper.getLevel().destroyBlock(helper.absolutePos(B), true);

        helper.assertItemEntityPresent(ComputingModule.ETHERNET_CABLE.asItem(), B, 2.0);
        helper.assertItemEntityPresent(ComputingModule.HBW_CABLE.asItem(), B, 2.0);
        helper.assertItemEntityPresent(ComputingModule.IMPORT_BUS_ITEM.get(), B, 2.0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dye_coloursTheWireLookedAt(final GameTestHelper helper) {
        for (final BlockPos at : List.of(A, B, C)) {
            TestCables.lay(helper, at, ComputingModule.ETHERNET_CABLE);
            TestCables.lay(helper, at, ComputingModule.HBW_CABLE);
        }
        final FakePlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE, 4));
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    final BlockPos pos = helper.absolutePos(B);
                    lookDown(player, pos, MIDDLE);
                    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                    helper.getLevel().getBlockState(pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player,
                            InteractionHand.MAIN_HAND, hit);
                    final CableBlockEntity cable = TestCables.cable(helper, B);
                    same(helper, Optional.of(DyeColor.RED), wire(cable, ComputingModule.HBW_CABLE).colour(),
                            "the backbone looked at is red");
                    same(helper, Optional.empty(), wire(cable, ComputingModule.ETHERNET_CABLE).colour(),
                            "the access beside it is not");
                    same(helper, 3, player.getMainHandItem().getCount(), "one dye spent");
                })
                .thenSucceed();
    }

    // The long-distance line

    @GameTest(template = ARENA)
    public static void longDistance_neverSharesABlock(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final CableBlockEntity alone = TestCables.lay(helper, A, TestCableTypes.LONG_DISTANCE);
        helper.assertTrue(!Cables.lay(level, helper.absolutePos(A), ComputingModule.ETHERNET_CABLE.get()),
                "nothing joins a long-distance cable in its block");
        same(helper, 1, alone.wires().size(), "the long-distance cable alone");
        TestCables.lay(helper, B, ComputingModule.ETHERNET_CABLE);
        helper.assertTrue(!Cables.lay(level, helper.absolutePos(B), TestCableTypes.LONG_DISTANCE.get()),
                "a long-distance cable joins no other in its block");
        same(helper, List.of(new BundleShape.Box(5, 5, 5, 11, 11, 11)), jackets(alone.shape(), 0),
                "six pixels thick");
        helper.succeed();
    }

    // The cables of a mod, declared through the Core

    @GameTest(template = ARENA)
    public static void cables_areDeclaredThroughTheCore(final GameTestHelper helper) {
        final List<CableEntry> declared = ModContent.of("jsc").declaredCables();
        same(helper, List.of(ComputingModule.ETHERNET_CABLE, ComputingModule.HBW_CABLE, ComputingModule.HPC_CABLE,
                ComputingModule.CRAFTING_CABLE), declared, "the four data cables, declared");
        final Set<Lane> lanes = new HashSet<>();
        for (final CableEntry entry : declared) {
            same(helper, entry.id(), CoreCables.REGISTRY.getKey(entry.get()), entry.id() + " registered under its id");
            helper.assertTrue(entry.asItem() instanceof CableItem item && item.type() == entry.get(),
                    entry.id() + "'s item lays it");
            helper.assertTrue(entry.get().grid() == GridKind.DATA && entry.get().thickness() == 4,
                    entry.id() + " is a four-pixel data cable");
            helper.assertTrue(lanes.add(entry.get().lane().orElseThrow()), entry.id() + " takes a lane of its own");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parts_closeTheirFaceToWires(final GameTestHelper helper) {
        final CableBlockEntity west = TestCables.lay(helper, A, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, B, ComputingModule.ETHERNET_CABLE);
        final CableType ethernet = ComputingModule.ETHERNET_CABLE.get();
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(west.crosses(ethernet, Direction.EAST), "the two join");
                    west.addPart(Direction.EAST, ComputingParts.EXPORT.get().create());
                })
                .thenExecuteAfter(2, () -> {
                    final Grid data = CoreGrids.of(helper.getLevel(), GridKind.DATA);
                    helper.assertTrue(!west.crosses(ethernet, Direction.EAST)
                            && !TestCables.cable(helper, B).crosses(ethernet, Direction.WEST),
                            "a part on the face between them parts them");
                    helper.assertTrue(!data.connected(number(helper, A, ComputingModule.ETHERNET_CABLE),
                            number(helper, B, ComputingModule.ETHERNET_CABLE)), "in the grid too");
                    west.removePart(Direction.EAST);
                })
                .thenExecuteAfter(2, () -> {
                    final Grid data = CoreGrids.of(helper.getLevel(), GridKind.DATA);
                    helper.assertTrue(west.crosses(ethernet, Direction.EAST)
                            && data.connected(number(helper, A, ComputingModule.ETHERNET_CABLE),
                            number(helper, B, ComputingModule.ETHERNET_CABLE)), "taken off, they join again");
                })
                .thenSucceed();
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.setShiftKeyDown(false);
        return player;
    }

    /* Uses the item in the player's hand on {@code face} of the block at {@code relative}. */
    private static void use(final GameTestHelper helper, final FakePlayer player, final BlockPos relative,
                            final Direction face) {
        final BlockPos pos = helper.absolutePos(relative);
        final Vec3 at = Vec3.atCenterOf(pos).relative(face, 0.5);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(at, face, pos, false)));
    }

    /* Stands the player above the block in the middle, {@code z} pixels into it, looking straight down. */
    private static void lookDown(final FakePlayer player, final BlockPos pos, final double z) {
        player.moveTo(pos.getX() + MIDDLE * PIXEL, pos.getY() + 2.0 - player.getEyeHeight(),
                pos.getZ() + z * PIXEL, 0.0F, 90.0F);
    }

    /* Breaks, as the player, what it sees in the middle block looking down {@code z} pixels into it. */
    private static void breakLookingDown(final GameTestHelper helper, final FakePlayer player, final double z) {
        final BlockPos pos = helper.absolutePos(B);
        lookDown(player, pos, z);
        helper.assertTrue(!CableBlock.aimOf(helper.getLevel(), pos, player).isNothing(),
                "the player looks at a wire " + z + " pixels in");
        player.gameMode.destroyBlock(pos);
    }

    /* The middle block holds exactly {@code kept}. */
    @SafeVarargs
    private static void leaves(final GameTestHelper helper, final CableEntry removed,
                               final Supplier<CableType>... kept) {
        final CableBlockEntity cable = TestCables.cable(helper, B);
        helper.assertTrue(!cable.holds(removed.get()), removed.id() + " taken out");
        for (final Supplier<CableType> each : kept) {
            helper.assertTrue(cable.holds(each.get()), each.get().id() + " left in");
        }
    }

    private static Wire wire(final CableBlockEntity cable, final CableEntry entry) {
        return Objects.requireNonNull(cable.wire(entry.get()), entry.id() + " in the block");
    }

    private static long number(final GameTestHelper helper, final BlockPos relative, final CableEntry entry) {
        return Cables.number(helper.getLevel(), helper.absolutePos(relative), entry.get()).orElse(Long.MIN_VALUE);
    }

    /* The jacket boxes of the strand at {@code index}. */
    private static List<BundleShape.Box> jackets(final BundleShape shape, final int index) {
        return shape.pieces().stream()
                .filter(piece -> piece.kind() == BundleShape.Kind.JACKET && piece.strand() == index)
                .map(BundleShape.Piece::box).toList();
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
