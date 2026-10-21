/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.google.gson.GsonBuilder;
import dev.jstech.core.client.developer.ScreenPoints;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.developer.studio.RecordedTest;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The studio's "Record a test": while it records, the block the player uses to open a screen, each click on a screen
 * (by the name of the component clicked, on a screen declared in a file), what is typed, the keys that move about a
 * screen, the waits between them and the pictures asked for are kept, in order. Two buttons over every screen take a
 * picture and stop. What was kept is shown in the studio's tests, which keeps it as a client test of its mod.
 */
public final class TestRecorder {

    private static final List<RecordedTest.Step> STEPS = new ArrayList<>();
    /** The keys that move about a screen, which a test presses as the player did. */
    private static final List<Integer> KEPT_KEYS = List.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_TAB,
            GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT);
    /** The fewest ticks between two steps kept as a wait, and the most a wait is kept as. */
    private static final int LEAST_WAIT = 5;
    private static final int MOST_WAIT = 100;
    /** The buttons over a screen while recording. */
    private static final int BUTTON_WIDTH = 64;
    private static final int BUTTON_HEIGHT = 12;
    private static final float SMALL = 0.75F;
    private static final String JSON = ".json";

    private static volatile boolean recording;
    private static @Nullable String mod;
    private static @Nullable String block;
    private static long lastTick;
    private static int pictures;

    private TestRecorder() {
    }

    /** Hands the game's bus what the recorder hears, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, TestRecorder::onUse);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, TestRecorder::onClick);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.CharacterTyped.Pre.class, TestRecorder::onType);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, TestRecorder::onKey);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, event -> {
            if (recording) {
                buttons(event.getGuiGraphics(), event.getScreen());
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, event -> {
            if (recording && Minecraft.getInstance().screen == null) {
                Draw.textScaled(event.getGuiGraphics(), Minecraft.getInstance().font,
                        GameText.resolve(StudioTexts.RECORDING), 4, 4, StudioPalette.get().recording(), SMALL);
            }
        });
    }

    /** Starts recording, from nothing, and closes the studio so the player can open the screens to record. */
    public static void start() {
        STEPS.clear();
        mod = null;
        block = null;
        pictures = 0;
        lastTick = now();
        recording = true;
        Minecraft.getInstance().setScreen(null);
    }

    /** Stops recording, keeping what was recorded for the studio's tests. */
    public static void stop() {
        recording = false;
    }

    /** Whether it records now. */
    public static boolean recording() {
        return recording;
    }

    /** What was recorded, in order. */
    public static List<RecordedTest.Step> steps() {
        return List.copyOf(STEPS);
    }

    /** The mod whose block opened the first screen recorded, which the test is kept for; none before one. */
    public static Optional<String> mod() {
        return Optional.ofNullable(mod);
    }

    /**
     * Keeps what was recorded as a client test of its mod, in the mod's source in this run, named after the block
     * first used; the file's path, or none when nothing was recorded or this run has no source of the mod's.
     */
    public static Optional<Path> keep() {
        if (mod == null || STEPS.isEmpty()) {
            return Optional.empty();
        }
        final Optional<Path> source = DevSources.resourcesOf(mod);
        if (source.isEmpty()) {
            return Optional.empty();
        }
        final Path folder = source.get().resolve("assets").resolve(mod).resolve(RecordedTest.FOLDER);
        final String base = block == null ? "recorded" : block;
        int number = 1;
        while (Files.exists(folder.resolve(base + "_" + number + JSON))) {
            number++;
        }
        final String name = base + "_" + number;
        final RecordedTest test = new RecordedTest(mod, name, STEPS);
        try {
            Files.createDirectories(folder);
            final Path file = folder.resolve(name + JSON);
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(test.toJson()),
                    StandardCharsets.UTF_8);
            return Optional.of(file);
        } catch (final IOException unwritten) {
            return Optional.empty();
        }
    }

    private static void onUse(final PlayerInteractEvent.RightClickBlock event) {
        if (!recording || !event.getLevel().isClientSide()) {
            return;
        }
        final ResourceLocation used = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos())
                .getBlock());
        if (mod == null) {
            mod = used.getNamespace();
            block = used.getPath();
        }
        keep(new RecordedTest.Use(used.toString()));
    }

    private static void onClick(final ScreenEvent.MouseButtonPressed.Pre event) {
        if (!recording || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        final Screen screen = event.getScreen();
        final double x = event.getMouseX();
        final double y = event.getMouseY();
        final int left = screen.width - 2 * BUTTON_WIDTH - 6;
        if (y >= 2 && y < 2 + BUTTON_HEIGHT && x >= left && x < screen.width - 2) {
            if (x < left + BUTTON_WIDTH) {
                pictures++;
                keep(new RecordedTest.Picture((block == null ? "picture" : block) + "_" + pictures));
            } else {
                stop();
                Minecraft.getInstance().setScreen(StudioClient.openOn(StudioTool.TESTS));
            }
            event.setCanceled(true);
            return;
        }
        if (screen instanceof StudioScreen) {
            return;
        }
        final Optional<String> named = screen instanceof IDataScreen data ? ScreenPoints.nameAt(data, x, y)
                : Optional.empty();
        keep(named.<RecordedTest.Step>map(RecordedTest.Click::new).orElseGet(() -> new RecordedTest.ClickAt(
                (int) Math.round(x - screen.width / 2.0), (int) Math.round(y - screen.height / 2.0))));
    }

    private static void onType(final ScreenEvent.CharacterTyped.Pre event) {
        if (!recording || event.getScreen() instanceof StudioScreen) {
            return;
        }
        final String typed = String.valueOf(event.getCodePoint());
        if (!STEPS.isEmpty() && STEPS.getLast() instanceof RecordedTest.Type type) {
            STEPS.set(STEPS.size() - 1, new RecordedTest.Type(type.text() + typed));
        } else {
            keep(new RecordedTest.Type(typed));
        }
    }

    private static void onKey(final ScreenEvent.KeyPressed.Pre event) {
        if (!recording || event.getScreen() instanceof StudioScreen) {
            return;
        }
        if (event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
            keep(new RecordedTest.Close());
        } else if (KEPT_KEYS.contains(event.getKeyCode())) {
            keep(new RecordedTest.Key(event.getKeyCode()));
        }
    }

    /* Keeps a step, with the wait before it when the player took a while. */
    private static void keep(final RecordedTest.Step step) {
        final long tick = now();
        final long waited = tick - lastTick;
        if (!STEPS.isEmpty() && waited >= LEAST_WAIT) {
            STEPS.add(new RecordedTest.Wait((int) Math.min(MOST_WAIT, waited)));
        }
        STEPS.add(step);
        lastTick = tick;
    }

    private static long now() {
        final Minecraft game = Minecraft.getInstance();
        return game.level == null ? 0L : game.level.getGameTime();
    }

    /* The two buttons over the screen at its top right: take a picture, and stop. */
    private static void buttons(final GuiGraphics g, final Screen screen) {
        final Font font = Minecraft.getInstance().font;
        final StudioPalette.Colours colours = StudioPalette.get();
        final int left = screen.width - 2 * BUTTON_WIDTH - 6;
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 500.0F);
        g.fill(left, 2, left + BUTTON_WIDTH, 2 + BUTTON_HEIGHT, colours.overlay());
        g.fill(left + BUTTON_WIDTH + 2, 2, screen.width - 2, 2 + BUTTON_HEIGHT, colours.overlay());
        Draw.textScaled(g, font, GameText.resolve(ToolTexts.PICTURE), left + 4, 5, colours.text(), SMALL);
        Draw.textScaled(g, font, GameText.resolve(ToolTexts.STOP_RECORDING), left + BUTTON_WIDTH + 6, 5,
                colours.recording(), SMALL);
        g.pose().popPose();
    }
}
