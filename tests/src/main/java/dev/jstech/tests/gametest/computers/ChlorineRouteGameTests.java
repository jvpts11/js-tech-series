/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The chlorine route as a player lays it: the Electrolytic Cell splits salt water into chlorine and hydrogen; an
 * Import Bus on its chlorine outlet takes the chlorine into the network; an Export Bus, a cylinder filled at that
 * outlet in its filter, sends it to the Plasma Etcher's chlorine inlet; and the hydrogen, which nobody wants, goes
 * along a gas pipe to a Gas Vent, so the cell never stops full. Only the salt and the water a player would bring are
 * handed in.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChlorineRouteGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos CELL = new BlockPos(1, 1, 6);
    private static final BlockPos ETCHER = new BlockPos(6, 1, 6);
    /*
     * The cell's gas outlets, west on its top layer, the hydrogen's at the front and the chlorine's behind it, and the
     * inlets of its salt and its water, east.
     */
    private static final BlockPos HYDROGEN_OUTLET = CELL.offset(0, 1, 0);
    private static final BlockPos CHLORINE_OUTLET = CELL.offset(0, 1, 1);
    private static final BlockPos SALT_INLET = CELL.offset(2, 1, 1);
    private static final BlockPos WATER_INLET = CELL.offset(2, 0, 1);
    /* The cable the Import Bus sits on, west of the chlorine outlet; the hydrogen's pipe, up and north to its vent. */
    private static final BlockPos IMPORT_CABLE = CHLORINE_OUTLET.west();
    private static final List<BlockPos> HYDROGEN_PIPE = List.of(HYDROGEN_OUTLET.above(),
            HYDROGEN_OUTLET.above().north());
    private static final BlockPos VENT = HYDROGEN_OUTLET.above().north(2);
    /* The Etcher's two gas inlets, east on its back row; the cable east of each. */
    private static final List<BlockPos> ETCHER_INLETS = List.of(ETCHER.offset(1, 0, 1), ETCHER.offset(1, 1, 1));
    /* The data cable from the crafting network's router out to the cell's and the Etcher's sides. */
    private static final List<BlockPos> CABLE = List.of(new BlockPos(4, 2, 3), new BlockPos(4, 2, 4),
            new BlockPos(4, 2, 5), new BlockPos(3, 2, 5), new BlockPos(2, 2, 5), new BlockPos(1, 2, 5),
            new BlockPos(0, 2, 5), new BlockPos(0, 2, 6), IMPORT_CABLE, new BlockPos(5, 2, 5),
            new BlockPos(6, 2, 5), new BlockPos(7, 2, 5), new BlockPos(8, 2, 5), new BlockPos(8, 2, 6),
            ETCHER_INLETS.get(1).east(), ETCHER_INLETS.get(0).east());
    /* The salt a player brings, twelve splittings of 500 mB of each gas; the water the cell's inlet holds. */
    private static final int SALT = 12;
    private static final int WATER = 4_000;
    /* The chlorine that must reach the Etcher, and the most hydrogen the cell may keep once its vent has it. */
    private static final long REACHED = 2_000L;
    private static final long KEPT = 1_000L;

    private ChlorineRouteGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 12_000)
    public static void chlorine_goesFromTheCellThroughTheNetworkToTheEtcher(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.buildCraftingNetwork();
        CABLE.forEach(at -> world.setBlock(at, ComputingModule.ETHERNET_CABLE));
        final BlockPos cellTerminal = formed(helper, machine("electrolytic_cell"), CELL);
        final BlockPos etcherTerminal = formed(helper, machine("plasma_etcher"), ETCHER);
        HYDROGEN_PIPE.forEach(at -> TestCables.lay(helper, at, SharedPipes.IRON_GAS_PIPE));
        helper.setBlock(VENT, SharedPipes.GAS_VENT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        final Material chlorine = Elements.of(Element.CHLORINE);
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(items(helper, SALT_INLET) != null, "the cell forms"))
                .thenExecute(() -> helper.assertTrue(ItemHandlerHelper.insertItem(items(helper, SALT_INLET),
                        new ItemStack(MaterialUses.item(SeriesCompounds.SALT, Forms.DUST).get(), SALT), false)
                        .isEmpty(), "the cell takes the salt"))
                .thenWaitUntil(() -> {
                    water(helper);
                    helper.assertTrue(held(gas(helper, CHLORINE_OUTLET, Direction.WEST), chlorine) > 0,
                            "the cell makes chlorine: " + said(helper, cellTerminal));
                })
                .thenExecute(() -> lay(helper, cellTerminal, etcherTerminal, chlorine))
                .thenWaitUntil(() -> {
                    water(helper);
                    helper.assertTrue(saltLeft(helper) == 0, "the cell works all its salt: " + said(helper,
                            cellTerminal));
                    helper.assertTrue(held(gas(helper, HYDROGEN_OUTLET, Direction.UP), hydrogen) <= KEPT,
                            "its hydrogen goes down the pipe to the vent: " + said(helper, cellTerminal));
                    final long reached = ETCHER_INLETS.stream().mapToLong(inlet -> held(gas(helper, inlet,
                            Direction.EAST), chlorine)).sum();
                    helper.assertTrue(reached >= REACHED, "the chlorine reaches the Etcher: " + reached + " mB; "
                            + said(helper, cellTerminal));
                })
                .thenSucceed();
    }

    /*
     * Once the cell makes chlorine: a player fills a cylinder at the chlorine outlet; an Import Bus goes on the cable
     * facing that outlet; an Export Bus with the cylinder in its filter on the cable facing the Etcher's chlorine
     * inlet.
     */
    private static void lay(final GameTestHelper helper, final BlockPos cellTerminal, final BlockPos etcherTerminal,
                            final Material chlorine) {
        final ChainMachineBlockEntity cell = machineAt(helper, cellTerminal);
        helper.assertTrue(cell.portProducts(helper.absolutePos(CHLORINE_OUTLET)).contains(chlorine.id()),
                "the back outlet is the chlorine's: " + cell.portProducts(helper.absolutePos(CHLORINE_OUTLET)));
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "chlorine_route".getBytes(StandardCharsets.UTF_8)), "chlorine_route"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GasCylinders.CYLINDER.get()));
        final BlockPos outlet = helper.absolutePos(CHLORINE_OUTLET);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(outlet).relative(Direction.WEST, 0.5), Direction.WEST, outlet,
                        false));
        final ItemStack cylinder = player.getMainHandItem().copy();
        helper.assertTrue(held(cylinder.getCapability(MatterCapabilities.GAS_ITEM), chlorine) > 0,
                "the cylinder fills with chlorine at the outlet");
        if (TestCables.cable(helper, IMPORT_CABLE) instanceof CableBlockEntity cable) {
            cable.addPart(Direction.EAST, ComputingParts.IMPORT.get().create());
        }
        final ChainMachineBlockEntity etcher = machineAt(helper, etcherTerminal);
        final BlockPos inlet = ETCHER_INLETS.stream().filter(at -> etcher.portProducts(helper.absolutePos(at))
                .contains(chlorine.id())).findFirst().orElse(null);
        helper.assertTrue(inlet != null, "one of the Etcher's gas inlets is the chlorine's");
        if (TestCables.cable(helper, inlet.east()) instanceof CableBlockEntity cable) {
            final var bus = ComputingParts.EXPORT.get().create();
            cable.addPart(Direction.WEST, bus);
            bus.setFilter(cylinder);
        }
    }

    /*
     * What the cell shows the player, whether it stands formed, its status and its note, what it holds, and what stands
     * on the hydrogen's way out, for a failure to say where the route stopped.
     */
    private static String said(final GameTestHelper helper, final BlockPos terminal) {
        final ChainMachineBlockEntity machine = machineAt(helper, terminal);
        final StringBuilder held = new StringBuilder();
        for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
            final ItemStack stack = machine.getInventory().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                held.append(' ').append(stack);
            }
        }
        return "formed " + machine.formed() + ", " + machine.status() + ", " + machine.note() + ", holds" + held
                + ", gas " + machine.matter().outputs(MatterState.GAS).stream().map(tank -> tank.held()
                .map(Object::toString).orElse("empty") + "/" + tank.capacity()).toList() + ", pipe "
                + HYDROGEN_PIPE.stream().map(at -> helper.getBlockState(at).getBlock().toString()).toList();
    }

    /* Keeps the cell's water inlet full, as a pipe from a spring would. */
    private static void water(final GameTestHelper helper) {
        final IFluidHandler inlet = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(WATER_INLET), Direction.EAST);
        helper.assertTrue(inlet != null, "the cell's water inlet takes water");
        inlet.fill(new FluidStack(Fluids.WATER, WATER), IFluidHandler.FluidAction.EXECUTE);
    }

    private static int saltLeft(final GameTestHelper helper) {
        final IItemHandler inlet = items(helper, SALT_INLET);
        int left = 0;
        for (int slot = 0; slot < inlet.getSlots(); slot++) {
            left += inlet.getStackInSlot(slot).getCount();
        }
        return left;
    }

    private static IItemHandler items(final GameTestHelper helper, final BlockPos relative) {
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(relative),
                Direction.EAST);
    }

    private static IMatterHandler gas(final GameTestHelper helper, final BlockPos relative, final Direction face) {
        return helper.getLevel().getCapability(MatterCapabilities.GAS, helper.absolutePos(relative), face);
    }

    /* How much of {@code gas} {@code handler} holds, in every tank; none for no handler. */
    private static long held(final IMatterHandler handler, final Material gas) {
        long held = 0L;
        for (int tank = 0; handler != null && tank < handler.tanks(); tank++) {
            held += handler.stack(tank).filter(stack -> stack.material() == gas).map(MatterStack::amount).orElse(0L);
        }
        return held;
    }
}
