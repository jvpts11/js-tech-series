/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.cable.LineBreach;
import dev.jstech.core.cable.LineWear;
import dev.jstech.core.chemical.ChemicalBridges;
import dev.jstech.core.chemical.IChemicalPort;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.fluid.PipeMarks;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.integration.mekanism.MekanismIntegration;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.Isotopes;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.industrial.config.IndustrialServerConfig;
import dev.jstech.industrial.line.IndustrialPipeMarks;
import dev.jstech.industrial.line.PipeFailures;
import dev.jstech.testkit.gametest.ScenarioBuilder;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestMatterEnds;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * J's Industrial's pipes: the marks of what they carry, a pipe made for each or wearing under it; a corrosive gas
 * refused by a plain pipe and eating it through in about a minute, which falls as its scrap; hydrogen wearing plain
 * steel through, which leaks as a cloud, and Mekanism's hydrogen as the series' own; the wear of a pipe forgotten as it
 * goes; and each consequence a setting.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialPipeGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos OUTPUT = new BlockPos(1, 2, 2);
    private static final BlockPos INPUT = new BlockPos(5, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final BlockPos ALONE = new BlockPos(3, 2, 4);
    private static final long FILL = 2_000L;
    private static final long LASTING = 60_000L;
    private static final double NEAR = 16;
    private static final ResourceLocation MEKANISM_HYDROGEN = ResourceLocation.fromNamespaceAndPath("mekanism",
            "hydrogen");
    private static final ResourceLocation MEKANISM_TANK = ResourceLocation.fromNamespaceAndPath("mekanism",
            "basic_chemical_tank");

    private IndustrialPipeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void marks_areDeclaredOnceEachOfTheirKind(final GameTestHelper helper) {
        for (final TagKey<Fluid> made : List.of(IndustrialPipeMarks.ACIDS, IndustrialPipeMarks.MOLTEN_METAL,
                IndustrialPipeMarks.MOLTEN_SALT, IndustrialPipeMarks.ULTRA_PURE_WATER,
                IndustrialPipeMarks.CORROSIVE_GASES)) {
            helper.assertTrue(PipeMarks.of(made).orElseThrow().kind() == PipeMarks.Kind.MADE_FOR,
                    made.location() + " asks a pipe made for it");
        }
        for (final TagKey<Fluid> wears : List.of(IndustrialPipeMarks.HYDROGEN, IndustrialPipeMarks.ABRASIVE)) {
            helper.assertTrue(PipeMarks.of(wears).orElseThrow().kind() == PipeMarks.Kind.WEARS,
                    wears.location() + " wears the pipes declared worn by it");
        }
        final PipeMarks.Mark corrosive = PipeMarks.of(IndustrialPipeMarks.CORROSIVE_GASES).orElseThrow();
        helper.assertTrue(corrosive.on(Elements.of(Element.CHLORINE)) && corrosive.on(Elements.of(Element.FLUORINE))
                && !corrosive.on(Elements.of(Element.HYDROGEN)), "chlorine and fluorine are the corrosive gases");
        final PipeMarks.Mark hydrogen = PipeMarks.of(IndustrialPipeMarks.HYDROGEN).orElseThrow();
        helper.assertTrue(hydrogen.on(Elements.of(Element.HYDROGEN)) && hydrogen.on(Isotopes.DEUTERIUM)
                && !hydrogen.on(Elements.of(Element.HELIUM)), "hydrogen and its isotopes wear steel");
        try {
            PipeMarks.wears(IndustrialPipeMarks.HYDROGEN, material -> false);
            helper.fail("a mark declared twice is refused");
        } catch (final DeclarationError expected) {
            // As it should.
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void corrosiveGas_isRefusedByAPlainGasPipe(final GameTestHelper helper) {
        final Material chlorine = Elements.of(Element.CHLORINE);
        lay(helper, TestCableTypes.STEEL_GAS_LINE, chlorine, FILL);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(TestMatterEnds.amountAt(helper.getLevel(), helper.absolutePos(INPUT), MatterState.GAS,
                    chlorine) == 0L, "chlorine does not go into a plain steel gas pipe");
            helper.assertTrue(helper.getBlockState(RUN.get(1)).is(CoreCables.BLOCK.get()),
                    "which stands a minute before it is eaten through");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void corrosiveGas_runsInAPipeMadeForIt(final GameTestHelper helper) {
        final Material chlorine = Elements.of(Element.CHLORINE);
        lay(helper, TestCableTypes.STAINLESS_GAS_LINE, chlorine, FILL);
        helper.succeedWhen(() -> helper.assertTrue(TestMatterEnds.amountAt(helper.getLevel(),
                helper.absolutePos(INPUT), MatterState.GAS, chlorine) == FILL,
                "a stainless gas pipe carries all the chlorine"));
    }

    @GameTest(template = ARENA)
    public static void corrosion_eatsAPipeThroughInAMinuteAndItFallsAsScrap(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(ALONE);
        TestCables.lay(helper, ALONE, TestCableTypes.STEEL_GAS_LINE);
        final long now = level.getGameTime();
        expose(helper, LineBreach.Reason.NOT_MADE_FOR, "jscore:chlorine", now, PipeFailures.CORRODES_IN - 1);
        helper.assertTrue(helper.getBlockState(ALONE).is(CoreCables.BLOCK.get()), "it stands just short of a minute");
        expose(helper, LineBreach.Reason.NOT_MADE_FOR, "jscore:chlorine", now + PipeFailures.CORRODES_IN - 1, 1);
        helper.assertFalse(helper.getBlockState(ALONE).is(CoreCables.BLOCK.get()), "and is eaten through at it");
        helper.assertItemEntityPresent(Items.IRON_NUGGET, ALONE, 2.0);
        helper.assertTrue(cloudsNear(level, at, Elements.of(Element.CHLORINE)) == 1,
                "its chlorine leaks out as a cloud");
        clearClouds(level, at);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void hydrogen_wearsPlainSteelThroughAndLeaksAsACloud(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        lay(helper, TestCableTypes.STEEL_GAS_LINE, hydrogen, LASTING);
        helper.succeedWhen(() -> {
            for (final BlockPos at : RUN) {
                helper.assertFalse(helper.getBlockState(at).is(CoreCables.BLOCK.get()),
                        "hydrogen carried as long as the steel stands wears it through");
            }
            helper.assertItemEntityPresent(Items.IRON_NUGGET, RUN.get(1), 3.0);
            final BlockPos middle = helper.absolutePos(RUN.get(1));
            helper.assertTrue(cloudsNear(helper.getLevel(), middle, hydrogen) >= 1,
                    "and the hydrogen leaks out as a cloud");
            helper.assertTrue(LineWear.worn(helper.getLevel(), middle, TestCableTypes.STEEL_GAS_LINE.get()) == 0,
                    "the broken pipe's wear is forgotten");
            clearClouds(helper.getLevel(), middle);
        });
    }

    @GameTest(template = ARENA)
    public static void wear_isKeptUntilThePipeGoes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(ALONE);
        final CableType steel = TestCableTypes.STEEL_GAS_LINE.get();
        TestCables.lay(helper, ALONE, TestCableTypes.STEEL_GAS_LINE);
        LineWear.wear(level, Map.of(new LineWear.Pipe(at, steel), Integer.MAX_VALUE));
        helper.assertTrue(LineWear.worn(level, at, steel) == LineWear.EVERY, "a span of wear is kept with the world");
        helper.setBlock(ALONE, Blocks.AIR);
        helper.assertTrue(LineWear.worn(level, at, steel) == 0, "and forgotten as the pipe's block goes");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void settings_offLeaveThePipeOrTheLeakHarmless(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(ALONE);
        TestCables.lay(helper, ALONE, TestCableTypes.STEEL_GAS_LINE);
        final long now = level.getGameTime();
        with(IndustrialServerConfig.PIPES_FAIL, () -> expose(helper, LineBreach.Reason.NOT_MADE_FOR,
                "jscore:chlorine", now, PipeFailures.CORRODES_IN + 1));
        helper.assertTrue(helper.getBlockState(ALONE).is(CoreCables.BLOCK.get()),
                "pipes that do not fail only refuse what they are not made for");
        with(IndustrialServerConfig.HYDROGEN_EMBRITTLES, () -> expose(helper, LineBreach.Reason.WORN_OUT,
                "jscore:hydrogen", now, 1));
        helper.assertTrue(helper.getBlockState(ALONE).is(CoreCables.BLOCK.get()),
                "hydrogen that does not embrittle wears nothing through");
        with(IndustrialServerConfig.LEAKS_HAZARDOUS, () -> expose(helper, LineBreach.Reason.WORN_OUT,
                "jscore:hydrogen", now, 1));
        helper.assertFalse(helper.getBlockState(ALONE).is(CoreCables.BLOCK.get()), "worn through, it breaks");
        helper.assertTrue(cloudsNear(level, at, Elements.of(Element.HYDROGEN)) == 0,
                "and a leak that is not hazardous only loses what it carried");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void mekanismHydrogen_comesAlongASteelLineAndWearsItThrough(final GameTestHelper helper) {
        helper.assertTrue(MekanismIntegration.isLoaded() && ChemicalBridges.anyRegistered(),
                "Mekanism and its bridge are on the dev runtime");
        final ServerLevel level = helper.getLevel();
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        TestMatterEnds.clear(level, helper.absolutePos(INPUT));
        helper.setBlock(INPUT, Blocks.WAXED_CHISELED_COPPER);
        // Placed facing north, Mekanism's tank gives out of its front alone: the line starts there.
        final BlockPos source = OUTPUT.south();
        ScenarioBuilder.forGameTest(helper).placeFromItem(source, BuiltInRegistries.BLOCK.get(MEKANISM_TANK));
        final IChemicalPort tank = ChemicalBridges.portFor(level, helper.absolutePos(source), Direction.UP)
                .orElseThrow();
        helper.assertTrue(tank.fill(MEKANISM_HYDROGEN, LASTING, false) == LASTING,
                "Mekanism's tank holds its hydrogen");
        TestCables.lay(helper, OUTPUT, TestCableTypes.STEEL_GAS_LINE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.STEEL_GAS_LINE);
        }
        final BlockPos middle = helper.absolutePos(RUN.get(1));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(TestMatterEnds.amountAt(level, helper.absolutePos(INPUT),
                        MatterState.GAS, hydrogen) > 0, "Mekanism's hydrogen comes along the line as the Core's"))
                .thenWaitUntil(() -> helper.assertFalse(helper.getBlockState(RUN.get(1)).is(CoreCables.BLOCK.get()),
                        "and wears the steel through, as any hydrogen does"))
                .thenExecute(() -> clearClouds(level, middle))
                .thenSucceed();
    }

    /* A run of {@code line} from a store holding {@code fill} of {@code gas} to an empty one. */
    private static void lay(final GameTestHelper helper, final DeferredHolder<CableType, CableType> line,
                            final Material gas, final long fill) {
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestMatterEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        helper.setBlock(OUTPUT, Blocks.CHISELED_COPPER);
        helper.setBlock(INPUT, Blocks.WAXED_CHISELED_COPPER);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, line);
        }
        TestMatterEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT), MatterState.GAS)
                .insert(new MatterStack(gas, fill), false);
    }

    /*
     * Tells the steel pipe standing alone {@code ticks} ticks in a row, from tick {@code from}, that {@code what}
     * reached it.
     */
    private static void expose(final GameTestHelper helper, final LineBreach.Reason reason, final String what,
                               final long from, final int ticks) {
        final CableType steel = TestCableTypes.STEEL_GAS_LINE.get();
        final BlockPos at = helper.absolutePos(ALONE);
        for (int tick = 0; tick < ticks; tick++) {
            steel.breach().exposed(helper.getLevel(), new LineBreach(at, steel, reason, what), from + tick);
        }
    }

    /* Runs {@code what} with {@code setting} off, putting it back on after. */
    private static void with(final ConfigKey<Boolean> setting, final Runnable what) {
        IndustrialServerConfig.FILE.set(setting, false);
        try {
            what.run();
        } finally {
            IndustrialServerConfig.FILE.set(setting, true);
        }
    }

    /* How many clouds of {@code gas} there are near {@code at}. */
    private static long cloudsNear(final ServerLevel level, final BlockPos at, final Material gas) {
        final Vec3 centre = Vec3.atCenterOf(at);
        return GasClouds.of(level).stream().filter(cloud -> cloud.gas().equals(gas.id())
                && cloud.centre().distanceTo(centre) <= NEAR).count();
    }

    /* Takes the clouds near {@code at} away, so no other test breathes them. */
    private static void clearClouds(final ServerLevel level, final BlockPos at) {
        final Vec3 centre = Vec3.atCenterOf(at);
        GasClouds.STATE.set(level, new GasClouds.Book(GasClouds.of(level).stream()
                .filter(cloud -> cloud.centre().distanceTo(centre) > NEAR).toList()));
    }
}
