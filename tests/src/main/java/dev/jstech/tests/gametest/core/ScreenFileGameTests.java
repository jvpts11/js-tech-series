/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.gui.flex.Align;
import dev.jstech.core.gui.flex.Insets;
import dev.jstech.core.gui.flex.Track;
import dev.jstech.core.gui.screen.ControlElements;
import dev.jstech.core.gui.screen.CoreElements;
import dev.jstech.core.gui.screen.ElementSpec;
import dev.jstech.core.gui.screen.ElementType;
import dev.jstech.core.gui.screen.ElementTypes;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.tests.JsTests;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Screens declared as files, read as the game reads them: every kind of the Core's named by its short name, the
 * layout's values written as a file writes them, and a slip in a file said with what to fix.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ScreenFileGameTests {

    private static final String ARENA = "empty";

    private ScreenFileGameTests() {
    }

    @GameTest(template = ARENA)
    public static void codec_readsTheTestMachinesFile(final GameTestHelper helper) {
        final ScreenFile file = read(helper, testMachine());

        helper.assertTrue(file.width() == ScreenFile.GAME_WIDTH, "the screen is the game's width");
        helper.assertTrue(file.root().type() == CoreElements.COLUMN, "its root is a column");
        final List<ElementSpec> all = file.root().all();
        helper.assertTrue(all.stream().anyMatch(spec -> spec.type() == CoreElements.INVENTORY),
                "it holds the player's inventory");
        helper.assertTrue(all.stream().anyMatch(spec -> spec.common().id().filter("press"::equals).isPresent()
                && spec.props(CoreElements.BUTTON).action().filter("press"::equals).isPresent()),
                "its button names the action press");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void codec_readsTheScreensOfEveryKind(final GameTestHelper helper) {
        final ScreenFile controls = read(helper, file("test_controls.json"));
        final ScreenFile views = read(helper, file("test_views.json"));
        final List<ElementType<?>> kinds = new ArrayList<>();
        controls.root().all().forEach(spec -> kinds.add(spec.type()));
        views.root().all().forEach(spec -> kinds.add(spec.type()));

        for (final ElementType<?> kind : List.of(ControlElements.SLIDER, ControlElements.SWITCH, ControlElements.RADIO,
                ControlElements.DROPDOWN, ControlElements.COLOR, ControlElements.TANK, ControlElements.ENERGY,
                ControlElements.GHOST_SLOT, ControlElements.TREE, ControlElements.TABLE, ControlElements.CHART,
                ControlElements.GRAPH, ControlElements.MODEL, ControlElements.MAP)) {
            helper.assertTrue(kinds.contains(kind), "the test screens hold a " + kind);
        }
        final ElementSpec signal = controls.root().all().stream().filter(spec -> spec.common().id()
                .filter("signal"::equals).isPresent()).findFirst().orElseThrow();
        helper.assertTrue(signal.props(ControlElements.SLIDER).steps() == 16, "a slider in steps keeps its steps");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void codec_readsTheLayoutsValues(final GameTestHelper helper) {
        final ScreenFile file = read(helper, """
                {"root": {"type": "grid", "columns": ["auto", "2fr", 18], "padding": [1, 2, 3, 4], "children": [
                  {"type": "label", "text": "a", "margin": [2, 3], "align_self": "center", "grow": 2}
                ]}}""");

        helper.assertTrue(file.root().layout().columns().equals(List.of(Track.auto(), Track.fraction(2),
                Track.fixed(18))), "the grid's columns are auto, 2fr and 18");
        helper.assertTrue(file.root().layout().padding().orElseThrow().equals(new Insets(1, 2, 3, 4)),
                "its padding is four numbers");
        final ElementSpec label = file.root().children().get(0);
        helper.assertTrue(label.common().margin().equals(Insets.of(2, 3)), "two numbers are above and below, sides");
        helper.assertTrue(label.common().alignSelf().orElseThrow() == Align.CENTER, "a word is where it stands");
        helper.assertTrue(label.common().grow().orElseThrow() == 2, "and it grows by its share");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void codec_saysWhatIsWrongWithAFile(final GameTestHelper helper) {
        final DataResult<ScreenFile> unknown = parse("{\"root\": {\"type\": \"no_such_thing\"}}");
        final DataResult<ScreenFile> leafWithChildren = parse("{\"root\": {\"type\": \"label\", \"children\": ["
                + "{\"type\": \"spacer\"}]}}");

        helper.assertTrue(unknown.error().map(e -> e.message().contains("no kind of screen component")).orElse(false),
                "a kind nobody declares is said: " + unknown);
        helper.assertTrue(leafWithChildren.error().map(e -> e.message().contains("holds nothing")).orElse(false),
                "children in a component that holds nothing are said: " + leafWithChildren);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void types_areFoundByTheirShortAndLongNames(final GameTestHelper helper) {
        final ScreenFile shortName = read(helper, "{\"root\": {\"type\": \"spacer\"}}");
        final ScreenFile longName = read(helper, "{\"root\": {\"type\": \"jscore:spacer\"}}");

        helper.assertTrue(shortName.root().type() == CoreElements.SPACER && longName.root().type()
                == CoreElements.SPACER, "spacer and jscore:spacer are one kind");
        helper.assertTrue(ElementTypes.get(ResourceLocation.fromNamespaceAndPath("jscore", "inventory")).isPresent(),
                "the Core's kinds are declared as the game starts");
        helper.succeed();
    }

    private static ScreenFile read(final GameTestHelper helper, final String json) {
        final DataResult<ScreenFile> result = parse(json);
        if (result.error().isPresent()) {
            helper.fail("the file could not be read: " + result.error().get().message());
        }
        return result.getOrThrow();
    }

    private static DataResult<ScreenFile> parse(final String json) {
        final JsonElement tree = JsonParser.parseString(json);
        return ScreenFile.CODEC.parse(JsonOps.INSTANCE, tree);
    }

    private static String testMachine() {
        return file("test_machine.json");
    }

    private static String file(final String name) {
        final Path file = ModList.get().getModFileById(JsTests.MODID).getFile().findResource("assets",
                JsTests.MODID, "jstech", "screens", name);
        try {
            return Files.readString(file);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
