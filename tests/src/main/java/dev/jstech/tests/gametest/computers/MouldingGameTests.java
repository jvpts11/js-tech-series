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
import dev.jstech.computers.chain.BoardJigs;
import dev.jstech.computers.chain.BoardParts;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.MouldedParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.MoulderBlockEntity;
import dev.jstech.computers.chain.machine.MouldCutting;
import dev.jstech.computers.hardware.FormFactor;
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
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Injection Moulder: the computer on its peripheral port cuts the mould its lithography program picks on the
 * Mould Blank in the moulder, the mould standing where its blank was; and the moulder moulds a component's body from
 * the plastic its component took, in the body's own mould, which it never uses up. A board jig is cut the same way,
 * and every motherboard is assembled on its own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MouldingGameTests {

    private static final String ARENA = "empty";
    private static final int STEP = 400;
    private static final int SETTLE = 20;
    private static final int SHEETS = 2;
    /* The motherboards of the seven form factors, 57, and Quince's 16 logic boards. */
    private static final int BOARDS = 57 + 16;

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
            moulder(helper, terminal).getInventory().insertItem(0, new ItemStack(frame.tool().get()), false);
            moulder(helper, terminal).getInventory().insertItem(1, new ItemStack(MaterialUses.item(
                    SeriesCompounds.POLYCARBONATE, Forms.SHEET).get(), SHEETS), false);
        });
        helper.succeedWhen(() -> {
            final MoulderBlockEntity moulder = moulder(helper, terminal);
            helper.assertTrue(moulder.getInventory().getStackInSlot(moulder.layout().firstOutput())
                    .is(frame.item().get()), "the Transition fan frame is moulded from its two sheets");
            helper.assertTrue(moulder.getInventory().getStackInSlot(0).is(frame.tool().get()),
                    "its mould stays, never used up");
            final List<ProcessingRecipe> fans = recipesMaking(helper, CoolingItems.TRANSITION_CASE_FAN.get());
            helper.assertTrue(fans.size() == 1 && fans.getFirst().inputs().stream().anyMatch(input ->
                    input.test(new ItemStack(frame.item().get()))), "the Transition fan is built on its frame");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = STEP)
    public static void bezel_isMouldedOfTheErasPlasticInItsMould(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("injection_moulder"));
        final MouldedParts.Part bezel = MouldedParts.of("legacy_mainframe");
        helper.runAfterDelay(SETTLE, () -> {
            moulder(helper, terminal).getInventory().insertItem(0, new ItemStack(bezel.tool().get()), false);
            moulder(helper, terminal).getInventory().insertItem(1, new ItemStack(MaterialUses.item(
                    SeriesCompounds.PVC, Forms.SHEET).get(), SHEETS), false);
        });
        helper.succeedWhen(() -> {
            final MoulderBlockEntity moulder = moulder(helper, terminal);
            helper.assertTrue(moulder.getInventory().getStackInSlot(moulder.layout().firstOutput())
                    .is(bezel.item().get()), "the Legacy Mainframe's bezel is moulded from its two PVC sheets");
            helper.assertTrue(moulder.getInventory().getStackInSlot(0).is(bezel.tool().get()),
                    "its mould stays, never used up");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = STEP)
    public static void jacket_isExtrudedThroughItsCablesDie(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("injection_moulder"));
        final MouldedParts.Part jacket = MouldedParts.of("thick_coax_cable");
        helper.runAfterDelay(SETTLE, () -> {
            moulder(helper, terminal).getInventory().insertItem(0, new ItemStack(jacket.tool().get()), false);
            moulder(helper, terminal).getInventory().insertItem(1, new ItemStack(MaterialUses.item(
                    SeriesCompounds.PVC, Forms.SHEET).get(), SHEETS), false);
        });
        helper.succeedWhen(() -> {
            final MoulderBlockEntity moulder = moulder(helper, terminal);
            helper.assertTrue(moulder.getInventory().getStackInSlot(moulder.layout().firstOutput())
                    .is(jacket.item().get()), "the Thick Coaxial Cable's jacket is extruded from its two PVC sheets");
            helper.assertTrue(moulder.getInventory().getStackInSlot(0).is(jacket.tool().get()),
                    "its extrusion die stays, never used up");
        });
    }

    @GameTest(template = ARENA)
    public static void moulded_everyProductIsBuiltWithItsOwnPart(final GameTestHelper helper) {
        for (final MouldedParts.Part part : MouldedParts.all()) {
            final List<ProcessingRecipe> made = recipesMaking(helper, part.productItem());
            helper.assertTrue(made.size() == 1 && made.getFirst().inputs().stream().anyMatch(input ->
                    input.test(new ItemStack(part.item().get()))), part.product() + " is built with "
                    + part.item().getId().getPath());
            helper.assertTrue(recipesMaking(helper, part.item().get()).size() == 1,
                    part.item().getId().getPath() + " is moulded by one recipe");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = STEP)
    public static void jig_isCutOnTheBlankByTheLinkedComputer(final GameTestHelper helper) {
        final BlockPos terminal = linkedMoulder(helper);
        final Item atx = BoardJigs.of(FormFactor.ATX).orElseThrow().item().get();
        helper.runAfterDelay(SETTLE, () -> {
            moulder(helper, terminal).getInventory().insertItem(0, new ItemStack(ChainChips.MOULD_BLANK.get()),
                    false);
            final Optional<TextKey> refused = LithographyDesk.write(helper.getLevel(), computer(helper), 0,
                    BuiltInRegistries.ITEM.getKey(atx));
            helper.assertTrue(refused.isEmpty(), "the moulder takes the cutting of the ATX jig: " + refused);
        });
        helper.succeedWhen(() -> helper.assertTrue(moulder(helper, terminal).getInventory().getStackInSlot(0)
                .is(atx), "the jig stands where its blank was"));
    }

    @GameTest(template = ARENA)
    public static void boards_areEachAssembledOnTheirJig(final GameTestHelper helper) {
        final Set<Item> jigs = BoardJigs.all().stream().map(jig -> jig.item().get()).collect(Collectors.toSet());
        int boards = 0;
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof ProcessingRecipe recipe && !recipe.outputs().isEmpty()) {
                final String made = BuiltInRegistries.ITEM.getKey(recipe.outputs().getFirst().getItem()).getPath();
                if (made.startsWith("motherboard_") || made.startsWith("quince_board_")) {
                    boards++;
                    helper.assertTrue(recipe.catalysts().size() == 1 && jigs.stream().anyMatch(jig ->
                            recipe.catalysts().getFirst().test(new ItemStack(jig))), made + " is assembled on a jig");
                }
            }
        }
        helper.assertTrue(boards >= BOARDS, "every board's recipe was seen: " + boards);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void moulds_areListedByFamily(final GameTestHelper helper) {
        helper.assertTrue(MouldCutting.offers().size() == BoardParts.all().size() + MouldedParts.all().size()
                + BoardJigs.all().size(), "every component's and every moulded part's mould, and every jig, is cut: "
                + MouldCutting.offers().size());
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

    /* The processing recipes that make {@code item}. */
    private static List<ProcessingRecipe> recipesMaking(final GameTestHelper helper, final Item item) {
        return helper.getLevel().getRecipeManager().getRecipes().stream().map(RecipeHolder::value)
                .filter(recipe -> recipe instanceof ProcessingRecipe).map(recipe -> (ProcessingRecipe) recipe)
                .filter(recipe -> recipe.outputs().stream().anyMatch(out -> out.is(item))).toList();
    }
}
