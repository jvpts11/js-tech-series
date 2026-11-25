/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.pvc;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.dust;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.kind;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.nugget;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.recipe;

import dev.jstech.computers.chain.BoardParts;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.material.Forms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * What is assembled by hand at an era's Electronics Workbench, and by that era's Electronics Assembly Line
 * (computers/docs/design/recipes.md): the components made at a bench, the Blank Film the computer's printer prints the
 * Vintage's Layouts on and the masks its Lithography Station writes from them, the board's components by their rule,
 * and each era's populated board.
 */
final class BenchRecipes {

    /* The controller chips a populated board of each era carries, from the Vintage. */
    private static final int[] CONTROLLERS = {4, 2, 2, 1, 1};
    private static final int PASSIVES = 4;
    private static final int PINS_A_FOIL = 100;
    /* The contacts each board component's recipe takes, as its rule reckons them: a module's edge takes its slot's. */
    private static final Map<BoardParts.Component, Integer> CONTACTS = new HashMap<>();

    private BenchRecipes() {
    }

    static void write(final RecipeOutput output) {
        components(output);
        layouts(output);
        boardParts(output);
        populatedBoards(output);
    }

    /** The kind of recipe of {@code era}'s bench and its Electronics Assembly Line. */
    static ProcessingKind bench(final Era era) {
        return ChainBenches.assembledAt(era).getLast();
    }

    /** The copper contacts the recipe of {@code component} takes, its edge's that goes in it. */
    static int contacts(final BoardParts.Component component) {
        if (CONTACTS.isEmpty()) {
            reckon((made, contacts) -> {
            });
        }
        return CONTACTS.get(component);
    }

    /* The components the design's table has made at a bench rather than by a machine. */
    private static void components(final RecipeOutput output) {
        made(output, HardwareEra.VINTAGE, "electric_motor", ChainParts.ELECTRIC_MOTOR.get(), 1,
                Items.IRON_INGOT, 4, Items.COPPER_INGOT, 4, Items.REDSTONE, 1);
        made(output, HardwareEra.LEGACY, "antenna", ChainParts.ANTENNA.get(), 2,
                of(Element.COPPER, Forms.ROD), 1, pvc(), 1);
        // A block of tool steel, a chromium steel, that the Injection Moulder's computer cuts a mould into.
        made(output, HardwareEra.VINTAGE, "mould_blank", ChainChips.MOULD_BLANK.get(), 1, BenchItems.steelPlate(), 4,
                nugget(Element.CHROMIUM), 1);
        // Film the computer's printer prints a Layout or a phototool on: a PVC base under a silver halide emulsion.
        made(output, HardwareEra.VINTAGE, "blank_film", ChainChips.BLANK_FILM.get(), 2, pvc(), 1,
                dust(Element.SILVER), 1);
    }

    /*
     * The masks the Vintage station writes, each die's from its Layout and a mask blank, the Layout used up. A Layout,
     * the chip's drawing, is printed on Blank Film by the computer's printer (FilmPrinting), not made at a bench.
     */
    private static void layouts(final RecipeOutput output) {
        final ProcessingKind station = kind("lithography_vintage", HardwareEra.VINTAGE);
        mask(output, station, ChainChips.MEMORY_LAYOUT.get(), ChainParts.MEMORY_MASKS.getFirst().get());
        mask(output, station, ChainChips.CONTROLLER_LAYOUT.get(), ChainParts.CONTROLLER_MASKS.getFirst().get());
        for (final ChainChips.Layout layout : ChainChips.PROCESSOR_LAYOUTS) {
            mask(output, station, layout.item().get(), vintageMask(ChainChips.PROCESSORS, layout.path()));
        }
        for (final ChainChips.Layout layout : ChainChips.GRAPHICS_LAYOUTS) {
            mask(output, station, layout.item().get(), vintageMask(ChainChips.GRAPHICS, layout.path()));
        }
    }

    /*
     * Each component of a board: its plastic body (PVC up to the Legacy, polycarbonate from the Transition; a big
     * socket two or three), moulded at the Injection Moulder in the component's own mould, one body for each component;
     * stamped contacts (copper foil, one for every hundred pins, a power connector's thick pins copper wire), their
     * plating (tin in the Vintage, gold after; a power connector always tin), and the metal its mechanism needs. Of the
     * components made of the same things, each with more pins takes at least one contact more than the one before it,
     * era after era, so no two are alike; Quince's sockets, reckoned last, after those already made.
     */
    private static void boardParts(final RecipeOutput output) {
        reckon((made, contacts) -> made.save(output));
    }

    /* Reckons every component's recipe in its order, handing each to {@code made} with its contacts. */
    private static void reckon(final BiConsumer<ChainRecipeItems.Saved, Integer> made) {
        final Map<List<Object>, Integer> contactsSoFar = new HashMap<>();
        final List<BoardParts.Component> catalogue = new ArrayList<>(BoardParts.CATALOGUE);
        catalogue.sort((a, b) -> a.era() != b.era() ? Integer.compare(ChainParts.ERAS.indexOf(a.era()),
                ChainParts.ERAS.indexOf(b.era())) : Integer.compare(a.pins(), b.pins()));
        for (final BoardParts.Component component : catalogue) {
            boardPart(component, contactsSoFar, made);
        }
        for (final BoardParts.Component component : BoardParts.QUINCE) {
            boardPart(component, contactsSoFar, made);
        }
    }

    private static void boardPart(final BoardParts.Component component,
                                  final Map<List<Object>, Integer> contactsSoFar,
                                  final BiConsumer<ChainRecipeItems.Saved, Integer> saved) {
        final boolean late = !component.era().isAtMost(HardwareEra.LEGACY);
        final boolean power = component.kind() == BoardParts.Kind.POWER;
        final Item body = late ? of(SeriesCompounds.POLYCARBONATE, Forms.SHEET) : pvc();
        final int bodies = component.kind() != BoardParts.Kind.SOCKET ? 1
                : component.pins() < 1000 ? 1 : component.pins() < 2500 ? 2 : 3;
        final Item contact = power ? of(Element.COPPER, Forms.WIRE) : of(Element.COPPER, Forms.FOIL);
        final Item plating = component.era() == HardwareEra.VINTAGE || power ? nugget(Element.TIN) : Items.GOLD_NUGGET;
        final List<Object> metal = mechanism(component);
        final List<Object> signature = new ArrayList<>(List.of(body, bodies, contact, plating));
        signature.addAll(metal);
        final int contacts = Math.max(Math.ceilDiv(component.pins(), PINS_A_FOIL),
                contactsSoFar.getOrDefault(signature, 0) + 1);
        contactsSoFar.put(signature, contacts);
        CONTACTS.put(component, contacts);
        // The plastic goes into the component's own body, moulded in its own mould, one body for each component made.
        final int made = component.kind().made();
        final ChainRecipeItems.Saved moulded = recipe(kind("injection_moulder"), component.body().getId().getPath());
        moulded.builder().input(body, bodies).catalyst(Ingredient.of(component.mould().get()), 1)
                .output(component.body().get(), made);
        saved.accept(moulded, 0);
        final ChainRecipeItems.Saved part = recipe(bench(component.era()), component.item().getId().getPath());
        part.builder().input(component.body().get(), made).input(contact, contacts).input(plating, 1);
        for (int i = 0; i < metal.size(); i += 2) {
            part.builder().input((Item) metal.get(i), (Integer) metal.get(i + 1));
        }
        part.builder().output(component.item().get(), made);
        saved.accept(part, contacts);
    }

    /*
     * The metal that holds a processor, item by count: a ZIF socket's lever a rod, a slot's two latches two rods, an
     * LGA socket's load plate a plate (two on the biggest, and on Quince's) and a rod, a processor card's connector two
     * rods, a DIP socket nothing.
     */
    private static List<Object> mechanism(final BoardParts.Component component) {
        final Item rod = of(SeriesAlloys.STEEL, Forms.ROD);
        final Item plate = of(SeriesAlloys.STEEL, Forms.PLATE);
        return switch (component.holds()) {
            case ZIF -> List.of(rod, 1);
            case SLOT, CARD -> List.of(rod, 2);
            case LGA -> List.of(plate, component.pins() > 3000 || BoardParts.QUINCE.contains(component) ? 2 : 1,
                    rod, 1);
            case DIP, NONE -> List.of();
        };
    }

    /* An era's etched board with its controller chips, its clock's oscillator and its passives. */
    private static void populatedBoards(final RecipeOutput output) {
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            final Era era = ChainParts.ERAS.get(i);
            final ChainRecipeItems.Saved made = recipe(bench(era), "populated_board_" + era.path());
            made.builder().input(ChainParts.ETCHED_BOARDS.get(i).get(), 1)
                    .input(ChainParts.CONTROLLER_CHIPS.get(i).get(), CONTROLLERS[i])
                    .input(ChainParts.OSCILLATOR.get(), 1).input(ChainParts.CERAMIC_CAPACITOR.get(), PASSIVES)
                    .input(ChainParts.TANTALUM_CAPACITOR.get(), PASSIVES).input(ChainParts.INDUCTOR.get(), PASSIVES)
                    .output(ChainParts.POPULATED_BOARDS.get(i).get(), 1);
            made.save(output);
        }
    }

    /* The Vintage station's mask of a die from its Layout and a mask blank. */
    private static void mask(final RecipeOutput output, final ProcessingKind station, final Item layout,
                             final Item mask) {
        final ChainRecipeItems.Saved made = recipe(station, ChainRecipeItems.idOf(mask).getPath());
        made.builder().input(ChainChips.MASK_BLANK.get(), 1).input(layout, 1).output(mask, 1);
        made.save(output);
    }

    private static Item vintageMask(final List<ChainChips.Die> dies, final String path) {
        return dies.stream().filter(die -> die.era() == HardwareEra.VINTAGE && die.path().equals(path)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no Vintage die " + path)).mask().get();
    }

}
