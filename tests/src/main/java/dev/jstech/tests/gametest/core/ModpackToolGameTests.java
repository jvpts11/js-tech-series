/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.modpack.ModpackDumps;
import dev.jstech.core.modpack.ModpackTexts;
import dev.jstech.core.modpack.ReferenceSite;
import dev.jstech.core.modpack.References;
import dev.jstech.core.modpack.ReloadReport;
import dev.jstech.core.unify.UnifyPlan;
import dev.jstech.tests.JsTests;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The tools of those who build a modpack: the dumps of what the game holds, the generated reference written as a
 * website, and what a reload says of the data and of what waits for a restart.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ModpackToolGameTests {

    private static final String ARENA = "empty";
    private static final int RECIPES = 12;
    private static final int LOOT_TABLES = 3;

    private ModpackToolGameTests() {
    }

    @GameTest(template = ARENA)
    public static void dumps_listWhatTheGameHolds(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String materials = ModpackDumps.materials();
        helper.assertTrue(materials.lines().anyMatch(line -> line.startsWith("| jscore:tin |")
                && line.contains("ingot")), "tin with its ingot among the materials");
        helper.assertTrue(ModpackDumps.forms().contains("| jscore:ingot | item | c:ingots/<material> |"),
                "the ingot among the forms");
        helper.assertTrue(ModpackDumps.recipes(server).contains("| jscore:processing") || ModpackDumps.recipes(server)
                .contains("| minecraft:crafting |"), "the kinds of recipe the data holds");
        helper.assertTrue(ModpackDumps.tags().contains("| #c:ingots/tin |"), "the common tag of tin's ingots");
        helper.assertTrue(ModpackDumps.hazards().contains("## Hazards of items"), "the hazards");
        helper.assertTrue(ModpackDumps.ids().contains("- jstests:foreign_tin_ingot"),
                "the test mod's ids among the series'");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void reference_writesEveryDataFormatWithARealExample(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final List<ReferenceSite.Section> sections = References.sections(server);
        final Optional<ReferenceSite.Entry> deposits = entry(sections, "data", "jscore:deposits");
        helper.assertTrue(deposits.isPresent() && deposits.get().where().contains("jstech/deposits")
                && deposits.get().example().startsWith("{"), "the deposits' kind of data, with a file of the "
                + "world's as its example; got " + deposits);
        final Optional<ReferenceSite.Entry> seals = entry(sections, "data_maps", "jscore:seals");
        helper.assertTrue(seals.isPresent() && seals.get().where().equals("data/jscore/data_maps/block/seals.json")
                && seals.get().example().contains("\"values\""), "how blocks seal, with some of its values; got "
                + seals);
        final Optional<ReferenceSite.Entry> knowledge = entry(sections, "recipes", "minecraft:crafting");
        helper.assertTrue(knowledge.isPresent() && !knowledge.get().example().isEmpty(),
                "the game's crafting recipes, with one written out");
        helper.assertTrue(entry(sections, "conditions", "jscore:setting").isPresent(), "the Core's conditions");
        final Path folder;
        final int written;
        try {
            folder = Files.createTempDirectory("jstests-reference");
            written = ModpackDumps.writeReference(server, folder);
            final String front = Files.readString(folder.resolve(ReferenceSite.FRONT), StandardCharsets.UTF_8);
            helper.assertTrue(front.contains("sections/data_maps.html"), "the front page leads to the data maps");
            helper.assertTrue(Files.readString(folder.resolve("sections/data.html"), StandardCharsets.UTF_8)
                    .contains("jscore:deposits"), "and the page of the kinds of data names the deposits");
        } catch (final IOException e) {
            helper.fail("the reference could not be written: " + e.getMessage());
            return;
        }
        helper.assertTrue(written == sections.size() + 2, "a page a section, the front page and the style; got "
                + written);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void reload_saysWhatItDidAndWhatWaitsForARestart(final GameTestHelper helper) {
        final List<String> calm = texts(ReloadReport.lines(RECIPES, LOOT_TABLES, UnifyPlan.NONE, false, Map.of()));
        helper.assertTrue(calm.getFirst().contains(String.valueOf(RECIPES)) && calm.getFirst().contains(
                String.valueOf(LOOT_TABLES)), "how many recipes and loot tables; got " + calm);
        helper.assertTrue(calm.getLast().equals(ModpackTexts.NOTHING_WAITS.english()), "and that nothing waits");
        final UnifyPlan plan = UnifyPlan.of(Map.of("c:ingots/tin", List.of("othermod:tin_ingot",
                "jscore:tin_ingot")), List.of("jscore"), Set.of());
        final List<String> waiting = texts(ReloadReport.lines(RECIPES, LOOT_TABLES, plan, true,
                Map.of("jstests-yaml", Set.of("speed", "boot.show_menu"))));
        helper.assertTrue(waiting.contains(ModpackTexts.PACKS_WAIT.english()), "content packs changed on disk wait");
        helper.assertTrue(waiting.stream().anyMatch(line -> line.contains("jstests-yaml")
                && line.contains("boot.show_menu, speed")), "and the settings, by file; got " + waiting);
        helper.assertTrue(waiting.stream().anyMatch(line -> line.contains("1") && line.startsWith("Unification")),
                "what the unification made of the data; got " + waiting);
        helper.assertFalse(waiting.contains(ModpackTexts.NOTHING_WAITS.english()), "so something waits");
        helper.assertTrue(ReloadReport.lines(helper.getLevel().getServer()).size() >= 2,
                "the server says the same of itself");
        helper.succeed();
    }

    private static Optional<ReferenceSite.Entry> entry(final List<ReferenceSite.Section> sections,
                                                       final String section, final String id) {
        return sections.stream().filter(found -> found.id().equals(section)).flatMap(found -> found.entries()
                .stream()).filter(found -> found.id().equals(id)).findFirst();
    }

    private static List<String> texts(final List<Component> lines) {
        return lines.stream().map(Component::getString).toList();
    }
}
