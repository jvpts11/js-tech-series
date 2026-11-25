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
import static dev.jstech.tests.testkit.computers.ChainMachineSite.place;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.chain.BoardParts;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.MouldedParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.MoulderBlockEntity;
import dev.jstech.computers.chain.machine.MouldCutting;
import dev.jstech.computers.lithography.LithographyDesk;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Injection Moulder: the computer on its peripheral port cuts the mould its lithography program picks on the
 * Mould Blank in the moulder, the mould standing where its blank was; and the moulder moulds a component's body from
 * the plastic its component took, in the body's own mould, which it never uses up.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MouldingGameTests {

    private static final String ARENA = "empty";
    private static final int STEP = 400;
    private static final int SETTLE = 20;
    private static final int SHEETS = 2;

    private MouldingGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = STEP)
    public static void mould_isCutOnTheBlankByTheLinkedComputer(final GameTestHelper helper) {
        final BlockPos terminal = linkedMoulder(helper);
        final BoardParts.Component lga = BoardParts.byPath("socket_lga_1700");
        helper.runAfterDelay(SETTLE, () -> {
            moulder(helper, terminal).getInventory().insertItem(0, new ItemStack(ChainChips.MOULD_BLANK.get()),
                    false);
            final LithographyLook look = LithographyDesk.look(helper.getLevel(), computer(helper), 0);
            helper.assertTrue(look.moulder() && look.blank() && look.picks().stream().anyMatch(pick ->
                    pick.mask().equals(BuiltInRegistries.ITEM.getKey(lga.mould().get()))),
                    "the program shows the moulder, its blank and the LGA 1700 socket body's mould: " + look);
            final Optional<TextKey> refused = LithographyDesk.write(helper.getLevel(), computer(helper), 0,
                    BuiltInRegistries.ITEM.getKey(lga.mould().get()));
            helper.assertTrue(refused.isEmpty(), "the moulder takes the cutting: " + refused);
        });
        helper.succeedWhen(() -> helper.assertTrue(moulder(helper, terminal).getInventory().getStackInSlot(0)
                .is(lga.mould().get()), "the mould stands where its blank was"));
    }

    @GameTest(template = ARENA, timeoutTicks = STEP)
    public static void body_isMouldedInItsMouldWhichStays(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("injection_moulder"));
        final BoardParts.Component lga = BoardParts.byPath("socket_lga_1700");
        helper.runAfterDelay(SETTLE, () -> {
            moulder(helper, terminal).getInventory().insertItem(0, new ItemStack(lga.mould().get()), false);
            moulder(helper, terminal).getInventory().insertItem(1, new ItemStack(MaterialUses.item(
                    SeriesCompounds.POLYCARBONATE, Forms.SHEET).get(), SHEETS), false);
        });
        helper.succeedWhen(() -> {
            final MoulderBlockEntity moulder = moulder(helper, terminal);
            final int out = moulder.layout().firstOutput();
            helper.assertTrue(moulder.getInventory().getStackInSlot(out).is(lga.body().get()),
                    "the LGA 1700 socket body is moulded");
            helper.assertTrue(moulder.getInventory().getStackInSlot(0).is(lga.mould().get()),
                    "its mould stays, never used up");
            helper.assertTrue(moulder.usesAsTool(new ItemStack(lga.mould().get())), "the mould is the moulder's tool");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = STEP)
    public static void fanFrame_isMouldedInItsMouldAndTheFanTakesIt(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("injection_moulder"));
        final MouldedParts.Part frame = MouldedParts.fanFrame(HardwareEra.TRANSITION);
        helper.runAfterDelay(SETTLE, () -> {
            moulder(helper, terminal).getInventory().insertItem(0, new ItemStack(frame.mould().get()), false);
            moulder(helper, terminal).getInventory().insertItem(1, new ItemStack(MaterialUses.item(
                    SeriesCompounds.POLYCARBONATE, Forms.SHEET).get(), SHEETS), false);
        });
        helper.succeedWhen(() -> {
            final MoulderBlockEntity moulder = moulder(helper, terminal);
            helper.assertTrue(moulder.getInventory().getStackInSlot(moulder.layout().firstOutput())
                    .is(frame.item().get()), "the Transition fan frame is moulded from its two sheets");
            helper.assertTrue(moulder.getInventory().getStackInSlot(0).is(frame.mould().get()),
                    "its mould stays, never used up");
            final List<ProcessingRecipe> fans = helper.getLevel().getRecipeManager().getRecipes().stream()
                    .map(RecipeHolder::value).filter(recipe -> recipe instanceof ProcessingRecipe)
                    .map(recipe -> (ProcessingRecipe) recipe)
                    .filter(recipe -> recipe.outputs().stream().anyMatch(out -> out.is(CoolingItems
                            .TRANSITION_CASE_FAN.get()))).toList();
            helper.assertTrue(fans.size() == 1 && fans.getFirst().inputs().stream().anyMatch(input ->
                    input.test(new ItemStack(frame.item().get()))), "the Transition fan is built on its frame");
        });
    }

    @GameTest(template = ARENA)
    public static void moulds_areListedByFamily(final GameTestHelper helper) {
        helper.assertTrue(MouldCutting.offers().size() == BoardParts.all().size() + MouldedParts.all().size(),
                "every component's and every moulded part's mould is cut: " + MouldCutting.offers().size());
        for (final MouldCutting.Kind kind : MouldCutting.Kind.values()) {
            helper.assertTrue(MouldCutting.offers().stream().anyMatch(offer -> offer.kind() == kind),
                    "the " + kind + " list has moulds");
        }
        helper.succeed();
    }

    /* The moulder formed, its peripheral port cabled to a running Legacy computer two blocks east of it. */
    private static BlockPos linkedMoulder(final GameTestHelper helper) {
        final ChainMachine moulder = machine("injection_moulder");
        final BlockPos terminal = formed(helper, moulder);
        TestCables.lay(helper, place(moulder, "peripheral_port").east(), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        EraNetwork.runningComputer(TestWorldBuilder.forGameTest(helper), EraKit.LEGACY, computerPlace());
        return terminal;
    }

    private static BlockPos computerPlace() {
        return place(machine("injection_moulder"), "peripheral_port").east(2);
    }

    private static AbstractComputerBlockEntity computer(final GameTestHelper helper) {
        return (AbstractComputerBlockEntity) helper.getBlockEntity(computerPlace());
    }

    private static MoulderBlockEntity moulder(final GameTestHelper helper, final BlockPos terminal) {
        return (MoulderBlockEntity) machineAt(helper, terminal);
    }
}
