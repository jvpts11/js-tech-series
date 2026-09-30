/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * The speed formula measured on the parts the catalogue registers, era by era. Within a socket, each maker's
 * processors climb the catalogue's ladder (entry, middle, high, top) without one step being slower than the step
 * below it, and the graphics cards climb from the era's floor to its top. The ladders are the catalogue's own order,
 * so a part put on the wrong rung, or a design given the wrong efficiency, shows here rather than in a tooltip.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CatalogRulerGameTests {

    private static final String ARENA = "empty";

    private CatalogRulerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void vintageProcessors_climbTheirLadderInEverySocket(final GameTestHelper helper) {
        assertProcessorsClimb(helper, "Socket 3", List.of(HardwareItems.CPU_INTEGRA_486SX,
                HardwareItems.CPU_INTEGRA_486DX2, HardwareItems.CPU_INTEGRA_486DX4,
                HardwareItems.CPU_VELOCION_5X86_133));
        assertProcessorsClimb(helper, "Socket 7, Integra", List.of(HardwareItems.CPU_INTEGRA_PENTIX_75,
                HardwareItems.CPU_INTEGRA_PENTIX_133, HardwareItems.CPU_INTEGRA_PENTIX_MMX_233));
        assertProcessorsClimb(helper, "Socket 7, Velocion", List.of(HardwareItems.CPU_VELOCION_K5_PR133,
                HardwareItems.CPU_VELOCION_K6_II, HardwareItems.CPU_VELOCION_K6_III,
                HardwareItems.CPU_VELOCION_K6_III_PLUS));
        assertProcessorsClimb(helper, "Socket 8", List.of(HardwareItems.CPU_INTEGRA_PENTIX_PRO_150,
                HardwareItems.CPU_INTEGRA_PENTIX_PRO_180, HardwareItems.CPU_INTEGRA_PENTIX_PRO_200));
        assertProcessorsClimb(helper, "Slot 1", List.of(HardwareItems.CPU_INTEGRA_CELER_300A,
                HardwareItems.CPU_INTEGRA_PENTIX_II_300, HardwareItems.CPU_INTEGRA_PENTIX_II_450,
                HardwareItems.CPU_INTEGRA_PENTIX_III_600));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void vintageGraphicsCards_climbFromTheFloorToTheTop(final GameTestHelper helper) {
        final List<DeferredItem<GpuItem>> ladder = List.of(HardwareItems.GPU_VGA_256, HardwareItems.GPU_WONDER_VGA,
                HardwareItems.GPU_3D_BLASTER, HardwareItems.GPU_RAVE_PRO, HardwareItems.GPU_PRISM_4,
                HardwareItems.GPU_VOODOO_GFX, HardwareItems.GPU_PRISM_TNT);
        for (int i = 1; i < ladder.size(); i++) {
            final GpuItem below = ladder.get(i - 1).get();
            final GpuItem card = ladder.get(i).get();
            helper.assertTrue(card.spec().power() >= below.spec().power(), ladder.get(i).getId() + " ("
                    + card.spec().power() + ") is weaker than " + ladder.get(i - 1).getId() + " ("
                    + below.spec().power() + ") below it");
        }
        helper.succeed();
    }

    private static void assertProcessorsClimb(final GameTestHelper helper, final String socket,
                                              final List<DeferredItem<CpuItem>> ladder) {
        for (int i = 1; i < ladder.size(); i++) {
            final long below = ladder.get(i - 1).get().spec().orchestrationCapacity();
            final long step = ladder.get(i).get().spec().orchestrationCapacity();
            helper.assertTrue(step >= below, socket + ": " + ladder.get(i).getId() + " (" + step
                    + ") is slower than " + ladder.get(i - 1).getId() + " (" + below + ") below it");
        }
    }
}
