/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.lithography.FilmPrinting;
import dev.jstech.computers.lithography.LithographyDesk;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Printing a film (computers/docs/design/survival.md): the lithography program has the printer linked to its computer
 * print a phototool on a Blank Film from its tray, and refuses with no Blank Film there; the Vintage's Layouts are
 * printed, no longer made at a bench; the Blank Film is two of a PVC sheet; and each era's etched board is etched
 * through its phototool, which the job lends the etcher.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FilmPrintingGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(4, 2, 2);
    private static final BlockPos PRINTER = COMPUTER.south();
    private static final int SETTLE = 10;
    /* The memory's and the controller's Layouts, six processors', six graphics dies' and five phototools. */
    private static final int FILMS = 2 + 6 + 6 + 5;

    private FilmPrintingGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void phototool_isPrintedOnBlankFilmByTheComputersPrinter(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        final PrinterBlockEntity printer = printer(helper);
        final Item phototool = ChainParts.PHOTOTOOLS.getFirst().get();
        printer.paper().setStackInSlot(0, new ItemStack(ChainChips.BLANK_FILM.get(), 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final LithographyLook look = LithographyDesk.look(helper.getLevel(), pc, 0);
                    helper.assertTrue(look.printer() && look.blank() && look.picks().stream().anyMatch(pick ->
                            pick.mask().equals(BuiltInRegistries.ITEM.getKey(phototool))),
                            "the program shows the printer, its Blank Film and the Vintage's phototool: " + look);
                    final Optional<TextKey> refused = LithographyDesk.write(helper.getLevel(), pc, 0,
                            BuiltInRegistries.ITEM.getKey(phototool));
                    helper.assertTrue(refused.isEmpty(), "the printer takes the film: " + refused);
                })
                .thenWaitUntil(() -> helper.assertTrue(printer.output().getStackInSlot(0).is(phototool),
                        "the Vintage's phototool comes out"))
                .thenExecute(() -> helper.assertTrue(printer.paperCount() == 1 && printer.jobs().isEmpty(),
                        "a Blank Film used for it, and the queue done; left " + printer.paperCount()))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void film_isRefusedWithNoBlankFilmInTheTray(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        final PrinterBlockEntity printer = printer(helper);
        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 4));
        helper.runAfterDelay(SETTLE, () -> {
            final Optional<TextKey> refused = LithographyDesk.write(helper.getLevel(), pc, 0,
                    BuiltInRegistries.ITEM.getKey(ChainChips.MEMORY_LAYOUT.get()));
            helper.assertTrue(refused.equals(Optional.of(FilmPrinting.NO_BLANK)),
                    "with paper in the tray, the film is refused: " + refused);
            helper.assertTrue(printer.jobs().isEmpty(), "and nothing is queued");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void films_arePrintedAndThePhototoolsLentToTheEtcher(final GameTestHelper helper) {
        helper.assertTrue(FilmPrinting.offers().size() == FILMS, "every Layout and phototool is printed: "
                + FilmPrinting.offers().size());
        final List<Item> layouts = new ArrayList<>(List.of(ChainChips.MEMORY_LAYOUT.get(),
                ChainChips.CONTROLLER_LAYOUT.get()));
        ChainChips.PROCESSOR_LAYOUTS.forEach(layout -> layouts.add(layout.item().get()));
        ChainChips.GRAPHICS_LAYOUTS.forEach(layout -> layouts.add(layout.item().get()));
        for (final Item layout : layouts) {
            helper.assertTrue(recipesMaking(helper, layout).isEmpty(), BuiltInRegistries.ITEM.getKey(layout)
                    + " is printed, made by no recipe");
        }
        final List<ProcessingRecipe> film = recipesMaking(helper, ChainChips.BLANK_FILM.get());
        helper.assertTrue(film.size() == 1 && film.getFirst().outputs().getFirst().getCount() == 2
                && film.getFirst().inputs().size() == 1 && film.getFirst().inputs().getFirst().test(new ItemStack(
                MaterialUses.item(SeriesCompounds.PVC, Forms.SHEET).get())), "Blank Film is two of a PVC sheet");
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            final Item phototool = ChainParts.PHOTOTOOLS.get(i).get();
            final List<ProcessingRecipe> etched = recipesMaking(helper, ChainParts.ETCHED_BOARDS.get(i).get());
            helper.assertTrue(etched.size() == 1 && etched.getFirst().catalysts().size() == 1
                    && etched.getFirst().catalysts().getFirst().test(new ItemStack(phototool)),
                    ChainParts.ERAS.get(i).path() + "'s etched board is etched through its phototool");
        }
        helper.succeed();
    }

    /* A Vintage printer beside the computer, and its block entity. */
    private static PrinterBlockEntity printer(final GameTestHelper helper) {
        helper.setBlock(PRINTER, ComputingModule.VINTAGE_PRINTER.get());
        return (PrinterBlockEntity) helper.getBlockEntity(PRINTER);
    }

    /* The processing recipes that make {@code item}. */
    private static List<ProcessingRecipe> recipesMaking(final GameTestHelper helper, final Item item) {
        return helper.getLevel().getRecipeManager().getRecipes().stream().map(RecipeHolder::value)
                .filter(recipe -> recipe instanceof ProcessingRecipe).map(recipe -> (ProcessingRecipe) recipe)
                .filter(recipe -> recipe.outputs().stream().anyMatch(out -> out.is(item))).toList();
    }
}
