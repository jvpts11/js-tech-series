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

/**
 * What is assembled by hand at an era's Electronics Workbench, and by that era's Electronics Assembly Line
 * (computers/docs/design/recipes.md): the components made at a bench, the Vintage's Layouts and the masks its
 * Lithography Station writes from them, the board's components by their rule, and each era's populated board.
 */
final class BenchRecipes {

    /* A die takes a sheet of PVC for every million transistors, and of two alike the bigger one a sheet more. */
    private static final Map<String, Integer> PROCESSOR_SHEETS = Map.of("m68k", 1, "i486", 2, "p5", 3, "k5", 4,
            "p6", 6, "k6", 9);
    private static final Map<String, Integer> GRAPHICS_SHEETS = Map.of("vga", 1, "rendition", 2, "threedfx", 3,
            "nv3", 4, "rage", 5, "fahrenheit", 7);
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
    }

    /*
     * The Vintage's Layouts, the chip's drawing cut in PVC film: the memory's on the ferrite of the core memory it
     * replaced, the controller's on the magnetic head of the drive it runs, a processor's on its clock's oscillator and
     * a graphics die's on the tube's deflection coil, the sheets by the die's size. The Vintage station writes each
     * die's mask from its Layout and a mask blank, the Layout used up.
     */
    private static void layouts(final RecipeOutput output) {
        made(output, HardwareEra.VINTAGE, "layout_memory", ChainChips.MEMORY_LAYOUT.get(), 1, pvc(), 1,
                ChainParts.FERRITE.get(), 1);
        made(output, HardwareEra.VINTAGE, "layout_controller", ChainChips.CONTROLLER_LAYOUT.get(), 1, pvc(), 1,
                ChainParts.MAGNETIC_HEAD.get(), 1);
        for (final ChainChips.Layout layout : ChainChips.PROCESSOR_LAYOUTS) {
            made(output, HardwareEra.VINTAGE, layout.item().getId().getPath(), layout.item().get(), 1, pvc(),
                    PROCESSOR_SHEETS.get(layout.path()), ChainParts.OSCILLATOR.get(), 1);
        }
        for (final ChainChips.Layout layout : ChainChips.GRAPHICS_LAYOUTS) {
            made(output, HardwareEra.VINTAGE, layout.item().getId().getPath(), layout.item().get(), 1, pvc(),
                    GRAPHICS_SHEETS.get(layout.path()), ChainParts.COPPER_COIL.get(), 1);
        }
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
     * Each component of a board: a plastic body (PVC up to the Legacy, polycarbonate from the Transition; a big socket
     * two or three), stamped contacts (copper foil, one for every hundred pins, a power connector's thick pins copper
     * wire), their plating (tin in the Vintage, gold after; a power connector always tin), and the metal its mechanism
     * needs. Of the components made of the same things, each with more pins takes at least one contact more than the
     * one before it, era after era, so no two are alike; Quince's sockets, reckoned last, after those already made.
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
        final ChainRecipeItems.Saved made = recipe(bench(component.era()), component.item().getId().getPath());
        made.builder().input(body, bodies).input(contact, contacts).input(plating, 1);
        for (int i = 0; i < metal.size(); i += 2) {
            made.builder().input((Item) metal.get(i), (Integer) metal.get(i + 1));
        }
        made.builder().output(component.item().get(), component.kind().made());
        saved.accept(made, contacts);
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
