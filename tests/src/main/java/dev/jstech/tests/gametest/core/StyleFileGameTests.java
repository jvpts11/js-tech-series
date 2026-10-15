/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.gui.ColorContrast;
import dev.jstech.core.gui.style.DeclaredPalettes;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StyleFile;
import dev.jstech.core.gui.style.StyleFiles;
import dev.jstech.core.gui.style.StylePalette;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSprite;
import dev.jstech.tests.JsTests;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The styles that come with the Core, read as a resource pack's are: every one reads, builds on what is there, sets
 * every colour, and keeps its text readable on its grounds, which is the contrast check run in every style.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StyleFileGameTests {

    private static final String ARENA = "empty";
    /** The contrast a text needs on its ground; a quieter text, three to one. */
    private static final double TEXT_CONTRAST = 4.5;
    private static final double QUIET_CONTRAST = 3.0;
    /** The class whose loading declares the eras' palettes. */
    private static final String ERA_THEMES = "dev.jstech.core.client.gui.theme.EraThemes";

    private StyleFileGameTests() {
    }

    @GameTest(template = ARENA)
    public static void coreStyles_readAndBuildWithNothingWrong(final GameTestHelper helper) {
        final List<String> problems = new ArrayList<>();
        final Map<ResourceLocation, Style> styles = StyleFiles.resolve(coreFiles(helper), DeclaredPalettes.INSTANCE,
                problems);

        helper.assertTrue(problems.isEmpty(), "the Core's styles have nothing wrong: " + problems);
        helper.assertTrue(styles.containsKey(StyleFiles.GAME), "the game's style is among them");
        helper.assertTrue(styles.size() >= 11, "the game's, J's Tech, high contrast, three for colour blindness and "
                + "five of the eras: " + styles.keySet());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreStyles_setEveryColour(final GameTestHelper helper) {
        final int unset = StylePalette.COLOURS.get().unset();
        final List<String> missing = new ArrayList<>();
        StyleFiles.resolve(coreFiles(helper), DeclaredPalettes.INSTANCE, new ArrayList<>()).forEach((id, style) -> {
            for (final StyleRole role : StyleRole.values()) {
                if (style.colour(role) == unset) {
                    missing.add(id + " " + role.key());
                }
            }
        });
        helper.assertTrue(missing.isEmpty(), "every role has a colour: " + missing);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreStyles_keepTheirTextReadable(final GameTestHelper helper) {
        final List<String> faint = new ArrayList<>();
        StyleFiles.resolve(coreFiles(helper), DeclaredPalettes.INSTANCE, new ArrayList<>()).forEach((id, style) -> {
            check(faint, id, style, StyleRole.TEXT, StyleRole.SCREEN, TEXT_CONTRAST);
            check(faint, id, style, StyleRole.TITLE, StyleRole.SCREEN, TEXT_CONTRAST);
            check(faint, id, style, StyleRole.TEXT, StyleRole.PANEL, TEXT_CONTRAST);
            check(faint, id, style, StyleRole.DIM, StyleRole.SCREEN, QUIET_CONTRAST);
            check(faint, id, style, StyleRole.TAB_LABEL_ON, StyleRole.TAB_ON, QUIET_CONTRAST);
            // A button drawn flat carries its text on its own fill; a sprite's ground is the sprite's to keep.
            if (style.piece(StylePiece.BUTTON) instanceof StyleSprite.Flat flat && flat.fill().isPresent()) {
                final double ratio = ColorContrast.ratio(style.colour(StyleRole.BUTTON_TEXT),
                        style.colour(flat.fill().get()));
                if (ratio < TEXT_CONTRAST) {
                    faint.add(id + " button text " + String.format("%.2f", ratio));
                }
            }
        });
        helper.assertTrue(faint.isEmpty(), "every text reads on its ground: " + faint);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void resolve_saysAMissingParentAndAColourNobodySets(final GameTestHelper helper) {
        final StyleFile orphan = parse("{\"parent\": \"jstests:nowhere\", \"colours\": {\"text\": \"#FFFFFF\"}}");
        final List<String> problems = new ArrayList<>();
        final Map<ResourceLocation, StyleFile> files = new LinkedHashMap<>();
        files.put(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "orphan"), orphan);

        StyleFiles.resolve(files, DeclaredPalettes.INSTANCE, problems);

        helper.assertTrue(problems.stream().anyMatch(p -> p.contains("no resource pack has")),
                "a missing parent is said: " + problems);
        helper.assertTrue(problems.stream().anyMatch(p -> p.contains("no style in its chain sets the colours")),
                "colours nobody sets are said: " + problems);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void codec_refusesARoleAndAPieceNoStyleHas(final GameTestHelper helper) {
        final DataResult<StyleFile> wrong = StyleFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"colours\": {\"sky\": \"#FFFFFF\"}, \"pieces\": {\"knob\": \"minecraft:widget/button\"}}"));

        helper.assertTrue(wrong.error().map(e -> e.message().contains("no colour role")
                && e.message().contains("no piece of a screen")).orElse(false), "both slips are said: " + wrong);
        helper.succeed();
    }

    private static void check(final List<String> faint, final ResourceLocation id, final Style style,
                              final StyleRole text, final StyleRole ground, final double least) {
        final double ratio = ColorContrast.ratio(style.colour(text), style.colour(ground));
        if (ratio < least) {
            faint.add(id + " " + text.key() + " on " + ground.key() + " " + String.format("%.2f", ratio));
        }
    }

    /*
     * Every style file the Core carries, by its id. The eras' palettes are declared by the class of the eras' themes,
     * which the server never loads on its own; it holds no game type of the player's, so it is loaded here.
     */
    private static Map<ResourceLocation, StyleFile> coreFiles(final GameTestHelper helper) {
        try {
            Class.forName(ERA_THEMES, true, StyleFileGameTests.class.getClassLoader());
        } catch (final ClassNotFoundException missing) {
            helper.fail("the eras' themes are gone: " + missing.getMessage());
        }
        final Path folder = ModList.get().getModFileById(JsCore.MODID).getFile().findResource("assets",
                JsCore.MODID, "jstech", "styles");
        final Map<ResourceLocation, StyleFile> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(folder)) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                final String relative = folder.relativize(file).toString().replace('\\', '/');
                final String path = relative.substring(0, relative.length() - ".json".length());
                out.put(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), parse(Files.readString(file)));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        if (out.isEmpty()) {
            helper.fail("the Core carries no style files at " + folder);
        }
        return out;
    }

    private static StyleFile parse(final String json) {
        return StyleFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }
}
