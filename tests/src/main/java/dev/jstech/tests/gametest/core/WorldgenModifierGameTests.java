/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonObject;
import dev.jstech.core.worldgen.modify.DensityFunctionModifier;
import dev.jstech.core.worldgen.modify.StructureSetModifier;
import dev.jstech.core.worldgen.modify.WorldgenModifiers;
import dev.jstech.core.worldgen.structure.OperatorStructures;
import dev.jstech.core.worldgen.structure.StructureReport;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The world generation of the game and of every mod changed as data and by the operator: the test mod's datapack
 * wraps a density function twice in the order of its priorities, spaces a structure set out and adds a structure to
 * it, names a function that is not there, and writes one of a kind of the test mod's own, made by its own code; the
 * Core's kinds of density function work as their files say; and the
 * operator's settings turn a structure off, give one other biomes and space out a mod's sets.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldgenModifierGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1e-9;
    private static final DensityFunction.SinglePointContext ORIGIN = new DensityFunction.SinglePointContext(0, 64, 0);

    private WorldgenModifierGameTests() {
    }

    @GameTest(template = ARENA)
    public static void densityModifiers_wrapTheOriginalInTheOrderOfTheirPriorities(final GameTestHelper helper) {
        final double value = density(helper, "test_modified").compute(ORIGIN);
        helper.assertTrue(Math.abs(value - (0.25 + 0.5) * 2) < CLOSE, "0.25 raised by 0.5, then doubled: " + value);
        final List<WorldgenModifiers.Made> made = WorldgenModifiers.made();
        final int raised = indexOf(made, "raise_test_modified");
        final int doubled = indexOf(made, "double_test_modified");
        helper.assertTrue(raised >= 0 && doubled > raised, "made in the order of their priorities: " + made);
        helper.assertTrue(made.get(raised).done() && made.get(raised).kind().equals(DensityFunctionModifier.KIND),
                "and made: " + made.get(raised));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aModifierNamingNothing_saysSoAndChangesNothing(final GameTestHelper helper) {
        final WorldgenModifiers.Made wrong = WorldgenModifiers.made().get(indexOf(WorldgenModifiers.made(),
                "name_nothing"));
        helper.assertTrue(!wrong.done() && wrong.problems().stream().anyMatch(problem -> problem.contains(
                "jstests:nothing_by_this_name")), "not made, saying what is missing: " + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aModsOwnKind_isReadAndMadeAsTheCoresAre(final GameTestHelper helper) {
        final int at = indexOf(WorldgenModifiers.made(), "note_test_modified");
        helper.assertTrue(at >= 0, "the file of the test mod's own kind is read: " + WorldgenModifiers.made());
        final WorldgenModifiers.Made note = WorldgenModifiers.made().get(at);
        helper.assertTrue(note.done() && note.kind().equals(TestWorldGen.NOTE_KIND)
                && note.target().equals(id("test_modified")), "and made, by its kind's own code: " + note);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void structureSetModifier_spacesTheSetOutAndAddsAStructure(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final StructureSet camp = set(registries, "test_camp").value();
        final Optional<JsonObject> placement = StructureSetModifier.placementOf(camp, registries);
        helper.assertTrue(placement.isPresent() && placement.get().get("spacing").getAsInt() == 48
                && placement.get().get("separation").getAsInt() == 16, "spaced 48 and 16: " + placement);
        helper.assertTrue(camp.structures().size() == 2 && camp.structures().stream().anyMatch(entry -> entry
                .structure().is(ResourceKey.create(Registries.STRUCTURE, id("test_hut"))) && entry.weight() == 2),
                "holding the hut beside the camp: " + camp.structures());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreDensities_workAsTheirFilesSay(final GameTestHelper helper) {
        helper.assertTrue(Math.abs(density(helper, "test_terraced").compute(ORIGIN) - 1.0) < CLOSE,
                "1.3 in steps of 0.5 is 1");
        helper.assertTrue(Math.abs(density(helper, "test_far").compute(new DensityFunction.SinglePointContext(30,
                0, 40)) - 5) < CLOSE, "50 blocks away is 5 radii of 10");
        helper.assertTrue(Math.abs(density(helper, "test_joined").compute(ORIGIN) - 0.175) < CLOSE,
                "0.2 and 0.4 joined smoothly below the smaller");
        helper.assertTrue(Math.abs(density(helper, "test_shaped").compute(ORIGIN) - 75) < CLOSE,
                "a ridge at 0.5, squared, made a height of 60 to 120");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void operator_turnsAStructureOffAndGivesOneOtherBiomes(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final Holder<Structure> hut = registries.registryOrThrow(Registries.STRUCTURE)
                .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, id("test_hut")));
        try {
            OperatorStructures.setOff(List.of("jstests:*"));
            helper.assertTrue(modified(hut).size() == 0, "every structure of the test mod off");
            OperatorStructures.setOff(List.of());
            OperatorStructures.setBiomes(List.of("jstests:test_hut = minecraft:plains, #minecraft:is_forest"));
            final HolderSet<Biome> biomes = modified(hut);
            helper.assertTrue(biomes.stream().anyMatch(biome -> biome.is(Biomes.PLAINS))
                    && biomes.stream().anyMatch(biome -> biome.is(Biomes.BIRCH_FOREST)),
                    "the hut stands in the plains and every forest: " + biomes.size());
            helper.assertTrue(OperatorStructures.biomesOf(id("test_town")).isEmpty(), "and the town in its own");
        } finally {
            OperatorStructures.setOff(List.of());
            OperatorStructures.setBiomes(List.of());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void operator_spacesOutEverySetOfAMod(final GameTestHelper helper) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final Registry<StructureSet> sets = registries.registryOrThrow(Registries.STRUCTURE_SET);
        final Map<Holder.Reference<StructureSet>, StructureSet> before = new HashMap<>();
        sets.holders().filter(set -> set.key().location().getNamespace().equals(JsTests.MODID))
                .forEach(set -> before.put(set, set.value()));
        try {
            OperatorStructures.setSpacing(Map.of("jstests:*", 2.0, "jstests:test_hut", 0.5));
            final List<WorldgenModifiers.Made> made = OperatorStructures.space(registries);
            helper.assertTrue(made.stream().allMatch(WorldgenModifiers.Made::done)
                    && made.stream().anyMatch(one -> one.target().equals(id("test_camp")))
                    && made.stream().noneMatch(one -> one.target().equals(id("test_monument"))),
                    "the test mod's sets spaced, but for the monument, placed where it is fixed: " + made);
            final JsonObject camp = StructureSetModifier.placementOf(set(registries, "test_camp").value(),
                    registries).orElseThrow();
            helper.assertTrue(camp.get("spacing").getAsInt() == 96 && camp.get("separation").getAsInt() == 32,
                    "the camp twice as far apart: " + camp);
            final JsonObject hutBefore = StructureSetModifier.placementOf(before.get(set(registries, "test_hut")),
                    registries).orElseThrow();
            final OperatorStructures.Spacing halved = OperatorStructures.spaced(hutBefore.get("spacing").getAsInt(),
                    hutBefore.get("separation").getAsInt(), 0.5);
            final JsonObject hut = StructureSetModifier.placementOf(set(registries, "test_hut").value(),
                    registries).orElseThrow();
            helper.assertTrue(hut.get("spacing").getAsInt() == halved.spacing()
                    && hut.get("separation").getAsInt() == halved.separation(),
                    "a set named itself takes its own share over its mod's: " + hut + " from " + hutBefore);
            helper.assertTrue(OperatorStructures.spaced(2, 1, 0.1).spacing() == 1
                    && OperatorStructures.spaced(2, 1, 0.1).separation() == 0, "never closer than a chunk");
        } finally {
            OperatorStructures.setSpacing(Map.of());
            before.forEach(WorldgenModifiers::rebind);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dump_listsEverySetWithItsStructuresAndTheModifiersMade(final GameTestHelper helper) {
        try {
            OperatorStructures.setOff(List.of("jstests:test_town"));
            final String page = StructureReport.page(helper.getLevel().getServer());
            helper.assertTrue(page.contains("## jstests:test_camp") && page.contains("## minecraft:villages"),
                    "every set by its id");
            helper.assertTrue(page.contains("jstests:test_town (off)"), "the structure turned off marked");
            helper.assertTrue(page.contains("spacing 48 chunks"), "the camp as the datapack spaced it");
            helper.assertTrue(page.contains("jstests:name_nothing: jscore:density_function on "
                    + "jstests:nothing_by_this_name, left as it was"), "and the modifier that changed nothing");
        } finally {
            OperatorStructures.setOff(List.of());
        }
        helper.succeed();
    }

    /* The biomes the operator's structure modifier gives {@code structure}, worked on a fresh copy of its own. */
    private static HolderSet<Biome> modified(final Holder<Structure> structure) {
        final ModifiableStructureInfo.StructureInfo.Builder builder = ModifiableStructureInfo.StructureInfo.Builder
                .copyOf(structure.value().modifiableStructureInfo().getOriginalStructureInfo());
        OperatorStructures.Modifier.INSTANCE.modify(structure, StructureModifier.Phase.MODIFY, builder);
        return builder.getStructureSettings().getBiomes();
    }

    private static DensityFunction density(final GameTestHelper helper, final String name) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION)
                .getOrThrow(ResourceKey.create(Registries.DENSITY_FUNCTION, id(name)));
    }

    private static Holder.Reference<StructureSet> set(final RegistryAccess registries, final String name) {
        return registries.registryOrThrow(Registries.STRUCTURE_SET).getHolderOrThrow(ResourceKey.create(
                Registries.STRUCTURE_SET, id(name)));
    }

    private static int indexOf(final List<WorldgenModifiers.Made> made, final String name) {
        for (int i = 0; i < made.size(); i++) {
            if (made.get(i).id().equals(id(name))) {
                return i;
            }
        }
        return -1;
    }

    private static ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, name);
    }
}
