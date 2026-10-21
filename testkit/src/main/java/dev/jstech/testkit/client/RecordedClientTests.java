/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.developer.studio.RecordedTest;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

/**
 * The client tests kept as files, as the studio's "Record a test" keeps them: every mod's
 * {@code assets/<mod>/jstech/client_tests/<name>.json}, each a suite of its mod's own, named after the mod, its steps
 * played as the player played them: the block used put in front of the player and used, each click on its component
 * by name, what was typed and the keys, the waits, the pictures compared with the mod's references, and the screen
 * closed.
 */
public final class RecordedClientTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** Where the block used stands, from the test's area, and how long a step waits before it, in ticks. */
    private static final BlockPos BLOCK = new BlockPos(2, 1, 3);
    private static final int SETTLE = 3;
    private static final int SCREEN_WAIT = 60;
    /** How long a test may take: a while to begin, and more for each step. */
    private static final int BASE_TICKS = 200;
    private static final int TICKS_A_STEP = 20;
    private static final String JSON = ".json";
    private static final String PREFIX = "Recorded.";

    private RecordedClientTests() {
    }

    /** Each mod's recorded tests, a suite a mod, read from the game's resources as they are now. */
    public static List<ClientTests.Suite> suites() {
        final Minecraft game = Minecraft.getInstance();
        if (game == null || game.getResourceManager() == null) {
            return List.of();
        }
        final Map<String, List<ClientTests.Test>> byMod = new LinkedHashMap<>();
        final Map<ResourceLocation, Resource> files = game.getResourceManager().listResources(RecordedTest.FOLDER,
                file -> file.getPath().endsWith(JSON));
        for (final Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                final RecordedTest test = RecordedTest.fromJson(JsonParser.parseReader(reader).getAsJsonObject());
                byMod.computeIfAbsent(test.mod(), any -> new ArrayList<>()).add(new ClientTests.Test(
                        PREFIX + test.name(), helper -> play(helper, test),
                        BASE_TICKS + TICKS_A_STEP * test.steps().size()));
            } catch (final IOException | RuntimeException unread) {
                LOGGER.warn("The recorded client test {} could not be read: {}", file.getKey(), unread.getMessage());
            }
        }
        final List<ClientTests.Suite> out = new ArrayList<>();
        byMod.forEach((mod, tests) -> out.add(new ClientTests.Suite(mod, mod, tests, ClientTestHelper::new)));
        return out;
    }

    /** Builds the steps of {@code test} on {@code helper}, as the player played them. */
    static void play(final ClientTestHelper helper, final RecordedTest test) {
        final Minecraft game = helper.mc();
        helper.afterTest(() -> game.setScreen(null));
        for (final RecordedTest.Step step : test.steps()) {
            switch (step) {
                case RecordedTest.Use use -> {
                    final ResourceLocation id = ResourceLocation.parse(use.block());
                    helper.thenServer(0, level -> level.setBlockAndUpdate(helper.abs(BLOCK),
                            BuiltInRegistries.BLOCK.get(id).defaultBlockState()));
                    helper.thenRightClick(SETTLE, BLOCK);
                    helper.thenWaitUntil(() -> game.screen != null, SCREEN_WAIT, "a screen opens on " + id);
                }
                case RecordedTest.Click click -> helper.then(SETTLE, () -> {
                    if (!(game.screen instanceof IDataScreen data)) {
                        throw new ClientTestFailure("the screen is not declared in a file, to click " + click
                                .component());
                    }
                    final int[] at = data.find(click.component()).orElseThrow(() -> new ClientTestFailure(
                            "the screen has no " + click.component())).component().center();
                    helper.click(at[0], at[1]);
                });
                case RecordedTest.ClickAt at -> helper.then(SETTLE, () -> {
                    final Screen screen = shown(game);
                    helper.click(screen.width / 2.0 + at.dx(), screen.height / 2.0 + at.dy());
                });
                case RecordedTest.Type type -> helper.then(SETTLE, () -> helper.type(type.text()));
                case RecordedTest.Key key -> helper.then(SETTLE, () -> helper.key(key.key()));
                case RecordedTest.Wait wait -> helper.then(wait.ticks(), () -> shown(game));
                case RecordedTest.Picture picture -> helper.then(SETTLE, () -> {
                    final Screen screen = shown(game);
                    if (screen instanceof AbstractContainerScreen<?> container) {
                        helper.matchesReference(picture.name(), container.getGuiLeft(), container.getGuiTop(),
                                container.getXSize(), container.getYSize());
                    } else {
                        helper.matchesReference(picture.name(), 0, 0, screen.width, screen.height);
                    }
                });
                case RecordedTest.Close close -> helper.then(SETTLE, () -> game.setScreen(null));
            }
        }
    }

    /* The screen open, which the step needs. */
    private static Screen shown(final Minecraft game) {
        if (game.screen == null) {
            throw new ClientTestFailure("no screen is open");
        }
        return game.screen;
    }
}
