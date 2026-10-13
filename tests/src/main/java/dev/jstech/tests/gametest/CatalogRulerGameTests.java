/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
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
        assertCardsClimb(helper, "Vintage", List.of(HardwareItems.GPU_VGA_256, HardwareItems.GPU_WONDER_VGA,
                HardwareItems.GPU_3D_BLASTER, HardwareItems.GPU_RAVE_PRO, HardwareItems.GPU_PRISM_4,
                HardwareItems.GPU_PRISM_TNT, HardwareItems.GPU_VOODOO_GFX));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void legacyProcessors_climbTheirLadderInEverySocket(final GameTestHelper helper) {
        assertProcessorsClimb(helper, "Socket 370", List.of(HardwareItems.CPU_INTEGRA_PENTIX_700,
                HardwareItems.CPU_INTEGRA_PENTIX_III_S_1000, HardwareItems.CPU_INTEGRA_PENTIX_III_S_1400));
        assertProcessorsClimb(helper, "Socket A", List.of(HardwareItems.CPU_VELOCION_DURO_1300,
                HardwareItems.CPU_VELOCION_SPRINT_XP_2400, HardwareItems.CPU_VELOCION_SPRINT_XP_2800,
                HardwareItems.CPU_VELOCION_SPRINT_XP_3200));
        assertProcessorsClimb(helper, "Socket 478", List.of(HardwareItems.CPU_INTEGRA_CELER_2_0,
                HardwareItems.CPU_INTEGRA_PENTIX_4_2_4C, HardwareItems.CPU_INTEGRA_PENTIX_4_3_2C,
                HardwareItems.CPU_INTEGRA_PENTIX_4_EE_3_4));
        assertProcessorsClimb(helper, "LGA 775", List.of(HardwareItems.CPU_INTEGRA_CELER_D_325J,
                HardwareItems.CPU_INTEGRA_PENTIX_4_520, HardwareItems.CPU_INTEGRA_PENTIX_4_540,
                HardwareItems.CPU_INTEGRA_PENTIX_4_560));
        assertProcessorsClimb(helper, "Socket 754", List.of(HardwareItems.CPU_VELOCION_SEMPER_3100,
                HardwareItems.CPU_VELOCION_SPRINT_64_3200, HardwareItems.CPU_VELOCION_SPRINT_64_3700));
        assertProcessorsClimb(helper, "Socket 939", List.of(HardwareItems.CPU_VELOCION_SPRINT_64_3500,
                HardwareItems.CPU_VELOCION_SPRINT_64_4000, HardwareItems.CPU_VELOCION_SPRINT_64_FX_55));
        assertProcessorsClimb(helper, "Socket 604", List.of(HardwareItems.CPU_INTEGRA_SERVO_2800,
                HardwareItems.CPU_INTEGRA_SERVO_3060, HardwareItems.CPU_INTEGRA_SERVO_3200));
        assertProcessorsClimb(helper, "Socket 940", List.of(HardwareItems.CPU_VELOCION_OPTERA_244,
                HardwareItems.CPU_VELOCION_OPTERA_248, HardwareItems.CPU_VELOCION_OPTERA_250));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void legacyGraphicsCards_climbEachMakersLadder(final GameTestHelper helper) {
        assertCardsClimb(helper, "Envya", List.of(HardwareItems.GPU_PRISM_TNT2_M64, HardwareItems.GPU_VERTEX_2_MX_400,
                HardwareItems.GPU_VERTEX_256, HardwareItems.GPU_VERTEX_FX_5200, HardwareItems.GPU_VERTEX_6200,
                HardwareItems.GPU_VERTEX_4_TI_4200, HardwareItems.GPU_VERTEX_6600_GT,
                HardwareItems.GPU_VERTEX_6800_ULTRA));
        assertCardsClimb(helper, "Atrion", List.of(HardwareItems.GPU_RADIANCE_7000, HardwareItems.GPU_RADIANCE_9200_SE,
                HardwareItems.GPU_RADIANCE_8500, HardwareItems.GPU_RADIANCE_X300, HardwareItems.GPU_RADIANCE_9600_XT,
                HardwareItems.GPU_RADIANCE_9800_PRO, HardwareItems.GPU_RADIANCE_9800_XT,
                HardwareItems.GPU_RADIANCE_X800_XT));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void transitionProcessors_climbTheirLadderInEverySocket(final GameTestHelper helper) {
        assertProcessorsClimb(helper, "LGA 775", List.of(HardwareItems.CPU_INTEGRA_PENTIX_D_805,
                HardwareItems.CPU_INTEGRA_CELER_E1200, HardwareItems.CPU_INTEGRA_CENTRO_2_DUO_E4300,
                HardwareItems.CPU_INTEGRA_CENTRO_2_DUO_E6600, HardwareItems.CPU_INTEGRA_CENTRO_2_DUO_E8500,
                HardwareItems.CPU_INTEGRA_CENTRO_2_QUAD_Q6600, HardwareItems.CPU_INTEGRA_CENTRO_2_EXTREME_QX9650));
        assertProcessorsClimb(helper, "AM2", List.of(HardwareItems.CPU_VELOCION_SPRINT_64_X2_3800,
                HardwareItems.CPU_VELOCION_SPRINT_64_X2_6000, HardwareItems.CPU_VELOCION_ASCENT_X4_9850));
        assertProcessorsClimb(helper, "AM3", List.of(HardwareItems.CPU_VELOCION_SPRINT_II_X2_250,
                HardwareItems.CPU_VELOCION_SPRINT_II_X4_630, HardwareItems.CPU_VELOCION_ASCENT_X4_955,
                HardwareItems.CPU_VELOCION_ASCENT_X4_965, HardwareItems.CPU_VELOCION_ASCENT_X6_1090T,
                HardwareItems.CPU_VELOCION_ASCENT_X6_1100T));
        assertProcessorsClimb(helper, "LGA 1156", List.of(HardwareItems.CPU_INTEGRA_PENTIX_G6950,
                HardwareItems.CPU_INTEGRA_CENTRO_C3_530, HardwareItems.CPU_INTEGRA_CENTRO_C5_750,
                HardwareItems.CPU_INTEGRA_CENTRO_C7_860, HardwareItems.CPU_INTEGRA_CENTRO_C7_880));
        assertProcessorsClimb(helper, "LGA 1366 workstation", List.of(HardwareItems.CPU_INTEGRA_CENTRO_C7_920,
                HardwareItems.CPU_INTEGRA_CENTRO_C7_960, HardwareItems.CPU_INTEGRA_CENTRO_C7_980X));
        assertProcessorsClimb(helper, "LGA 1366 server", List.of(HardwareItems.CPU_INTEGRA_SERVO_5520,
                HardwareItems.CPU_INTEGRA_SERVO_5570, HardwareItems.CPU_INTEGRA_SERVO_5680));
        assertProcessorsClimb(helper, "LGA 771", List.of(HardwareItems.CPU_INTEGRA_SERVO_5100,
                HardwareItems.CPU_INTEGRA_SERVO_5160, HardwareItems.CPU_INTEGRA_SERVO_5335,
                HardwareItems.CPU_INTEGRA_SERVO_5450));
        assertProcessorsClimb(helper, "Socket F", List.of(HardwareItems.CPU_VELOCION_OPTERA_2218,
                HardwareItems.CPU_VELOCION_OPTERA_8356, HardwareItems.CPU_VELOCION_OPTERA_8384,
                HardwareItems.CPU_VELOCION_OPTERA_8435));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void transitionGraphicsCards_climbEachMakersLadder(final GameTestHelper helper) {
        assertCardsClimb(helper, "Envya", List.of(HardwareItems.GPU_VERTEX_7300_GS, HardwareItems.GPU_VERTEX_8600_GT,
                HardwareItems.GPU_VERTEX_7600_GT, HardwareItems.GPU_VERTEX_7800_GTX, HardwareItems.GPU_VERTEX_9600_GT,
                HardwareItems.GPU_VERTEX_GT_240, HardwareItems.GPU_VERTEX_8800_GT, HardwareItems.GPU_VERTEX_GTX_280,
                HardwareItems.GPU_VERTEX_GTX_480));
        assertCardsClimb(helper, "Atrion", List.of(HardwareItems.GPU_RADIANCE_X1300,
                HardwareItems.GPU_RADIANCE_X1950_XTX, HardwareItems.GPU_RADIANCE_HD_3850,
                HardwareItems.GPU_RADIANCE_HD_4670, HardwareItems.GPU_RADIANCE_HD_5770,
                HardwareItems.GPU_RADIANCE_HD_4870, HardwareItems.GPU_RADIANCE_HD_5870));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void standardProcessors_climbTheirLadderInEverySocket(final GameTestHelper helper) {
        assertProcessorsClimb(helper, "LGA 1155", List.of(HardwareItems.CPU_INTEGRA_CELER_G530,
                HardwareItems.CPU_INTEGRA_CENTRO_C3_2120, HardwareItems.CPU_INTEGRA_CENTRO_C5_2500K,
                HardwareItems.CPU_INTEGRA_CENTRO_C7_2600K, HardwareItems.CPU_INTEGRA_CENTRO_C7_3770K));
        assertProcessorsClimb(helper, "LGA 1150", List.of(HardwareItems.CPU_INTEGRA_PENTIX_G3258,
                HardwareItems.CPU_INTEGRA_CENTRO_C3_4160, HardwareItems.CPU_INTEGRA_CENTRO_C5_4590,
                HardwareItems.CPU_INTEGRA_CENTRO_C5_4690K, HardwareItems.CPU_INTEGRA_SERVO_1231_V3,
                HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K));
        assertProcessorsClimb(helper, "LGA 2011 workstation", List.of(HardwareItems.CPU_INTEGRA_CENTRO_C7_3820,
                HardwareItems.CPU_INTEGRA_CENTRO_C7_3930K, HardwareItems.CPU_INTEGRA_CENTRO_C7_4930K,
                HardwareItems.CPU_INTEGRA_CENTRO_C7_4960X));
        assertProcessorsClimb(helper, "LGA 2011 two-way", List.of(HardwareItems.CPU_INTEGRA_SERVO_2609,
                ComputingModule.CPU_SERVO_2620, ComputingModule.CPU_SERVO_2690, ComputingModule.CPU_SERVO_2697_V2));
        assertProcessorsClimb(helper, "LGA 2011 four-way", List.of(HardwareItems.CPU_INTEGRA_SERVO_4650,
                HardwareItems.CPU_INTEGRA_SERVO_4657L_V2));
        assertProcessorsClimb(helper, "AM3+", List.of(HardwareItems.CPU_VELOCION_FX_4300,
                HardwareItems.CPU_VELOCION_FX_6300, HardwareItems.CPU_VELOCION_FX_8350,
                HardwareItems.CPU_VELOCION_FX_9590));
        assertProcessorsClimb(helper, "FM2+", List.of(HardwareItems.CPU_VELOCION_FUSE_A4_6300,
                HardwareItems.CPU_VELOCION_FUSE_A8_6600K, HardwareItems.CPU_VELOCION_FUSE_A10_6800K));
        assertProcessorsClimb(helper, "G34", List.of(HardwareItems.CPU_VELOCION_OPTERA_6212,
                HardwareItems.CPU_VELOCION_OPTERA_6272, HardwareItems.CPU_VELOCION_OPTERA_6380));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void standardGraphicsCards_climbEachMakersLadder(final GameTestHelper helper) {
        assertCardsClimb(helper, "Envya", List.of(HardwareItems.GPU_VERTEX_GT_730,
                HardwareItems.GPU_VERTEX_GTX_550_TI, HardwareItems.GPU_VERTEX_GTX_750_TI,
                HardwareItems.GPU_VERTEX_GTX_660, HardwareItems.GPU_VERTEX_GTX_970, HardwareItems.GPU_VERTEX_GTX_780_TI,
                HardwareItems.GPU_VERTEX_GTX_1060, HardwareItems.GPU_VERTEX_GTX_980_TI,
                HardwareItems.GPU_VERTEX_GTX_1080));
        assertCardsClimb(helper, "Velocion", List.of(HardwareItems.GPU_RADIANCE_HD_7750,
                HardwareItems.GPU_RADIANCE_HD_7850, HardwareItems.GPU_RADIANCE_HD_7970,
                HardwareItems.GPU_RADIANCE_RX_480, HardwareItems.GPU_RADIANCE_R9_290X,
                HardwareItems.GPU_RADIANCE_R9_FURY_X));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void advancedDesktopProcessors_climbTheirLadderInEverySocket(final GameTestHelper helper) {
        assertProcessorsClimb(helper, "AM4", List.of(HardwareItems.CPU_VELOCION_SPRINT_200GE,
                HardwareItems.CPU_VELOCION_AWAYKEN_3_3200G, HardwareItems.CPU_VELOCION_AWAYKEN_5_1600,
                HardwareItems.CPU_VELOCION_AWAYKEN_5_3600, HardwareItems.CPU_VELOCION_AWAYKEN_7_1800X,
                HardwareItems.CPU_VELOCION_AWAYKEN_7_5800X3D, HardwareItems.CPU_VELOCION_AWAYKEN_9_3900X,
                HardwareItems.CPU_VELOCION_AWAYKEN_9_5900X, HardwareItems.CPU_VELOCION_AWAYKEN_9_5950X));
        assertProcessorsClimb(helper, "AM5", List.of(HardwareItems.CPU_VELOCION_AWAYKEN_5_7600,
                HardwareItems.CPU_VELOCION_AWAYKEN_7_7700X, HardwareItems.CPU_VELOCION_AWAYKEN_7_9800X3D,
                HardwareItems.CPU_VELOCION_AWAYKEN_9_7950X, HardwareItems.CPU_VELOCION_AWAYKEN_9_9950X));
        assertProcessorsClimb(helper, "LGA 1151", List.of(HardwareItems.CPU_INTEGRA_PENTIX_G4560,
                HardwareItems.CPU_INTEGRA_CENTRO_C3_8100, HardwareItems.CPU_INTEGRA_CENTRO_C5_8400,
                HardwareItems.CPU_INTEGRA_CENTRO_C7_8700K, HardwareItems.CPU_INTEGRA_CENTRO_C9_9900K));
        assertProcessorsClimb(helper, "LGA 1200", List.of(HardwareItems.CPU_INTEGRA_CENTRO_C3_10100,
                HardwareItems.CPU_INTEGRA_CENTRO_C5_10400, HardwareItems.CPU_INTEGRA_CENTRO_C9_10900K));
        assertProcessorsClimb(helper, "LGA 1700", List.of(HardwareItems.CPU_INTEGRA_CENTRO_C3_12100,
                HardwareItems.CPU_INTEGRA_CENTRO_C5_12400, HardwareItems.CPU_INTEGRA_CENTRO_C5_13600K,
                HardwareItems.CPU_INTEGRA_CENTRO_C9_12900K, HardwareItems.CPU_INTEGRA_CENTRO_C9_13900K,
                HardwareItems.CPU_INTEGRA_CENTRO_C9_13900KS));
        assertProcessorsClimb(helper, "LGA 1851", List.of(HardwareItems.CPU_INTEGRA_CENTRO_ULTRA_C5_225,
                HardwareItems.CPU_INTEGRA_CENTRO_ULTRA_C5_245K, HardwareItems.CPU_INTEGRA_CENTRO_ULTRA_C7_265K,
                HardwareItems.CPU_INTEGRA_CENTRO_ULTRA_C9_285K));
        assertProcessorsClimb(helper, "LGA 2066", List.of(HardwareItems.CPU_INTEGRA_CENTRO_C9_7900X,
                HardwareItems.CPU_INTEGRA_CENTRO_C9_7980XE, HardwareItems.CPU_INTEGRA_CENTRO_C9_10980XE));
        assertProcessorsClimb(helper, "sTR4", List.of(HardwareItems.CPU_VELOCION_THREADKILLER_1920X,
                HardwareItems.CPU_VELOCION_THREADKILLER_1950X, HardwareItems.CPU_VELOCION_THREADKILLER_2990WX));
        assertProcessorsClimb(helper, "sTR5", List.of(HardwareItems.CPU_VELOCION_THREADKILLER_7960X,
                HardwareItems.CPU_VELOCION_THREADKILLER_7980X, HardwareItems.CPU_VELOCION_THREADKILLER_7995WX));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void advancedServerProcessors_climbTheirLadderInEverySocket(final GameTestHelper helper) {
        assertProcessorsClimb(helper, "SP3", List.of(HardwareItems.CPU_VELOCION_EPIC_7251,
                HardwareItems.CPU_VELOCION_EPIC_7302, HardwareItems.CPU_VELOCION_EPIC_7452,
                HardwareItems.CPU_VELOCION_EPIC_7763));
        assertProcessorsClimb(helper, "SP5", List.of(HardwareItems.CPU_VELOCION_EPIC_9124,
                HardwareItems.CPU_VELOCION_EPIC_9374F, HardwareItems.CPU_VELOCION_EPIC_9654));
        assertProcessorsClimb(helper, "LGA 3647", List.of(HardwareItems.CPU_INTEGRA_SERVO_BRONZO_3106,
                HardwareItems.CPU_INTEGRA_SERVO_PLATA_4114, HardwareItems.CPU_INTEGRA_SERVO_ORO_6148,
                HardwareItems.CPU_INTEGRA_SERVO_PLATINA_8180, HardwareItems.CPU_INTEGRA_SERVO_W_3175X));
        assertProcessorsClimb(helper, "LGA 4189", List.of(HardwareItems.CPU_INTEGRA_SERVO_PLATA_4314,
                HardwareItems.CPU_INTEGRA_SERVO_PLATINA_8352Y, HardwareItems.CPU_INTEGRA_SERVO_PLATINA_8380H,
                HardwareItems.CPU_INTEGRA_SERVO_PLATINA_8380));
        assertProcessorsClimb(helper, "LGA 4677", List.of(HardwareItems.CPU_INTEGRA_SERVO_PLATA_4410Y,
                HardwareItems.CPU_INTEGRA_SERVO_ORO_6442Y, HardwareItems.CPU_INTEGRA_SERVO_W9_3495X,
                HardwareItems.CPU_INTEGRA_SERVO_PLATINA_8490H, HardwareItems.CPU_INTEGRA_SERVO_PLATINA_8592));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void advancedGraphicsCards_climbEachMakersLadder(final GameTestHelper helper) {
        assertCardsClimb(helper, "Envya", List.of(HardwareItems.GPU_VERTEX_GTX_1650, HardwareItems.GPU_VERTEX_RTX_2060,
                HardwareItems.GPU_VERTEX_RTX_3060, HardwareItems.GPU_VERTEX_GTX_1080_TI,
                HardwareItems.GPU_VERTEX_RTX_4060, HardwareItems.GPU_VERTEX_RTX_5060, HardwareItems.GPU_VERTEX_RTX_3080,
                HardwareItems.GPU_VERTEX_RTX_4090, HardwareItems.GPU_VERTEX_RTX_5090));
        assertCardsClimb(helper, "Velocion", List.of(HardwareItems.GPU_RADIANCE_RX_550,
                HardwareItems.GPU_RADIANCE_RX_580, HardwareItems.GPU_RADIANCE_RX_VEGA_64,
                HardwareItems.GPU_RADIANCE_RX_6600, HardwareItems.GPU_RADIANCE_RX_5700_XT,
                HardwareItems.GPU_RADIANCE_RX_6900_XT, HardwareItems.GPU_RADIANCE_RX_9070_XT,
                HardwareItems.GPU_RADIANCE_RX_7900_XTX));
        // The compute cards climb across the two eras they span.
        assertCardsClimb(helper, "Tessera", List.of(HardwareItems.GPU_TESSERA_K40, HardwareItems.GPU_TESSERA_V100,
                HardwareItems.GPU_TESSERA_A100, HardwareItems.GPU_TESSERA_H100));
        helper.succeed();
    }

    private static void assertCardsClimb(final GameTestHelper helper, final String ladderName,
                                         final List<DeferredItem<GpuItem>> ladder) {
        for (int i = 1; i < ladder.size(); i++) {
            final long below = ladder.get(i - 1).get().spec().power();
            final long card = ladder.get(i).get().spec().power();
            helper.assertTrue(card >= below, ladderName + ": " + ladder.get(i).getId() + " (" + card
                    + ") is weaker than " + ladder.get(i - 1).getId() + " (" + below + ") below it");
        }
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
