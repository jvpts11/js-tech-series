/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.jstech.core.client.developer.NewScreenScreen;
import dev.jstech.core.client.developer.ScreenEditor;
import dev.jstech.core.client.developer.ScreenInspector;
import dev.jstech.core.client.gui.data.DataContainerScreen;
import dev.jstech.core.client.gui.data.DataScreen;
import dev.jstech.core.developer.DevSources;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestScreens;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The developer's tools for screens declared in files: the inspector beside a screen shows the tree of what it is
 * built of, the problems found and a card of a component; the new screen for a block writes the file and opens it
 * with the editor on it; the editor adds a component, names it and undoes, each change written to the file.
 */
public final class ScreenToolsClientTests {

    private static final int WAIT = 60;
    private static final int SETTLE = 3;
    /* A screen of the test mod's that only this test makes, and takes away after. */
    private static final ResourceLocation MADE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_screen_tool");
    /* Where a new machine screen's face is in its file's tree: after the title, before the inventory. */
    private static final List<Integer> FACE = List.of(1);

    private ScreenToolsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void inspector_showsTheTreeTheProblemsAndACard(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenServer(0, level -> ctx.serverPlayer().openMenu(TestScreens.provider()))
                .thenAwaitScreen(DataContainerScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final DataContainerScreen<?> screen = ctx.screen(DataContainerScreen.class);
                    ScreenInspector.toggle(screen);
                    ctx.assertTrue(ScreenInspector.openOn(screen), "F12's inspector opens on the screen");
                    final List<String> rows = ScreenInspector.rows();
                    ctx.assertTrue(rows.stream().anyMatch(row -> row.startsWith("Button press"))
                            && rows.stream().anyMatch(row -> row.startsWith("Checkbox ready")),
                            "the tree names each component by its kind and its name: " + rows);
                    ctx.assertTrue(ScreenInspector.problems().stream().anyMatch(problem -> problem.contains(
                            "has no pt_br text")), "a text with no Portuguese is found: " + ScreenInspector.problems());
                    final int[] press = screen.find("press").orElseThrow().component().center();
                    final List<String> card = ScreenInspector.cardAt(press[0], press[1]);
                    ctx.assertTrue(card.stream().anyMatch(line -> line.startsWith("box"))
                            && card.stream().anyMatch(line -> line.startsWith("does  press"))
                            && card.stream().anyMatch(line -> line.contains("test_machine.json, line")),
                            "the card says its box, what it does and its line: " + card);
                    ctx.pointAt(press[0], press[1]);
                })
                .thenScreenshot(SETTLE, "inspector");
    }

    @ClientTest(timeoutTicks = 600)
    public static void newScreenAndEditor_writeTheFileAsTheyChangeIt(final ClientTestContext ctx) {
        final Path written = DevSources.resourcesOf(JsTests.MODID).orElseThrow().resolve("assets")
                .resolve(JsTests.MODID).resolve("jstech").resolve("screens").resolve(MADE.getPath() + ".json");
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            try {
                Files.deleteIfExists(written);
            } catch (final IOException failed) {
                throw new UncheckedIOException(failed);
            }
        });
        ctx.then(0, () -> ctx.mc().setScreen(new NewScreenScreen(MADE)))
                .thenAwaitScreen(NewScreenScreen.class, WAIT)
                .thenScreenshot(SETTLE, "new-screen")
                .then(0, () -> {
                    final NewScreenScreen made = ctx.screen(NewScreenScreen.class);
                    ctx.assertTrue(made.fileId().filter(MADE::equals).isPresent(),
                            "the file is named after the block: " + made.fileId());
                    made.create();
                })
                .thenAwaitScreen(DataScreen.class, WAIT)
                .then(SETTLE, () -> {
                    ctx.assertTrue(Files.isRegularFile(written), "Create wrote the file to the mod's source");
                    ctx.assertTrue(ScreenEditor.openOn(ctx.mc().screen), "the editor opens on the new screen");
                    // The pointer over the new screen's face, the second component of its column.
                    final int[] face = ScreenEditor.boxOf(FACE).orElseThrow();
                    ctx.pointAt(face[0] + face[2] / 2.0, face[1] + face[3] / 2.0);
                })
                .then(SETTLE, () -> {
                    ScreenEditor.add("button");
                    final JsonArray inFace = faceOf(ScreenEditor.file().orElseThrow());
                    ctx.assertTrue(inFace.size() == 1 && "button".equals(inFace.get(0).getAsJsonObject()
                            .get("type").getAsString()), "Add put a button in the face, where the pointer is, in "
                            + "place of the line saying it is empty: " + inFace);
                    final List<String> card = ScreenEditor.cardFields();
                    ctx.assertTrue(card.size() > 2 && card.subList(0, 2).equals(List.of("id", "bind"))
                            && card.contains("text") && card.contains("action"), "its card holds its name, what it "
                            + "is bound to and the fields a button has: " + card);
                })
                .then(SETTLE, () -> {
                    ScreenEditor.set("id", "made_by_the_editor");
                    ctx.assertTrue(read(written).contains("made_by_the_editor"),
                            "the name given in the panel is written to the file at once");
                })
                .thenScreenshot(SETTLE, "editor")
                .then(0, () -> {
                    ScreenEditor.undo();
                    ctx.assertTrue(!read(written).contains("made_by_the_editor") && read(written).contains("button"),
                            "Undo takes the name back and keeps the button");
                });
    }

    private static JsonArray faceOf(final String file) {
        return JsonParser.parseString(file).getAsJsonObject().getAsJsonObject("root").getAsJsonArray("children")
                .get(FACE.get(0)).getAsJsonObject().getAsJsonArray("children");
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }
}
