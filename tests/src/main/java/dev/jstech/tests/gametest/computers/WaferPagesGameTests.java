/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainFaces;
import dev.jstech.computers.chain.machine.WaferPages;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A wafer's steps as the recipe viewer shows them (computers/docs/design/survival.md, "How a chip is made"): every
 * item of every page lies in a port of its machine's own face, so none is lost from the page, and every page has its
 * three lines; each mask exposes on a lithography page, each chip is packaged on one, each era is etched and implanted
 * on one.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WaferPagesGameTests {

    private static final String ARENA = "empty";
    private static final int LINES = 3;
    /* The wafers of light: three lasers' and the LEDs'. */
    private static final int LIGHTS = 4;

    private WaferPagesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void pages_putEveryItemInAPortOfTheirMachinesFace(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final WaferPages.Page page : WaferPages.all()) {
            final Optional<FacePicture> picture = ChainFaces.of(page.machine());
            if (picture.isEmpty()) {
                wrong.add(page.machine().id() + " has no face");
                continue;
            }
            final Set<Integer> ports = new HashSet<>();
            picture.get().ports().forEach(port -> ports.add(port.slot()));
            for (final Integer slot : page.slots().keySet()) {
                if (!ports.contains(slot)) {
                    wrong.add(page.step() + " on " + page.machine().id() + ": slot " + slot + " has no port");
                }
            }
            if (page.lines().size() != LINES) {
                wrong.add(page.step() + " on " + page.machine().id() + " has " + page.lines().size() + " lines");
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every page's items lie in its machine's face's ports: " + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pages_coverEveryMaskEveryChipAndEveryEra(final GameTestHelper helper) {
        final Set<Item> masks = new HashSet<>();
        final Set<Item> chips = new HashSet<>();
        int etched = 0;
        int implanted = 0;
        for (final WaferPages.Page page : WaferPages.all()) {
            switch (page.step()) {
                case LITHOGRAPHY -> page.slots().get(0).forEach(stack -> masks.add(stack.getItem()));
                case PACKAGING -> page.slots().get(page.machine().layout().firstOutput())
                        .forEach(stack -> chips.add(stack.getItem()));
                case ETCH -> etched++;
                case IMPLANT -> implanted++;
                default -> {
                    // Deposition's pages are its metals', counted by the client's test.
                }
            }
        }
        final Set<Item> expected = new HashSet<>();
        final Set<Item> made = new HashSet<>();
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            expected.add(ChainParts.MEMORY_MASKS.get(i).get());
            expected.add(ChainParts.CONTROLLER_MASKS.get(i).get());
            made.add(ChainParts.MEMORY_CHIPS.get(i).get());
            made.add(ChainParts.CONTROLLER_CHIPS.get(i).get());
        }
        for (final ChainChips.Die die : ChainChips.all()) {
            expected.add(die.mask().get());
            made.add(die.chip().get());
        }
        expected.add(ChainChips.LASER_DIODE_MASK.get());
        expected.add(ChainChips.LED_MASK.get());
        helper.assertTrue(masks.containsAll(expected), "every mask exposes on a page: missing " + missing(expected,
                masks));
        helper.assertTrue(chips.containsAll(made) && chips.size() >= made.size() + LIGHTS - 1,
                "every chip is packaged on a page: missing " + missing(made, chips));
        helper.assertTrue(etched == ChainParts.ERAS.size() && implanted == ChainParts.ERAS.size(),
                "every era is etched and implanted on a page: " + etched + " and " + implanted);
        helper.succeed();
    }

    private static List<String> missing(final Set<Item> wanted, final Set<Item> found) {
        final List<String> out = new ArrayList<>();
        for (final Item item : wanted) {
            if (!found.contains(item)) {
                out.add(new ItemStack(item).getHoverName().getString());
            }
        }
        return out;
    }
}
