/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.live.ITouchScreen;
import dev.jstech.core.live.ScreenTouches;
import dev.jstech.core.live.TouchArea;
import dev.jstech.core.live.TouchPress;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A pump's panel in the world, declared as a mod declares a screen on a block that answers the crosshair: a screen
 * across the middle of its north face showing the pump's name, its flow, and a Start and a Stop button, each a part a
 * press reaches by its name. The server keeps the last part pressed and whether the pump runs.
 */
@TextHolder
public final class TestTouchPanel {

    /** The screen's pixels, across the middle half of the north face. */
    public static final int WIDTH = 96;
    public static final int HEIGHT = 48;
    public static final TouchArea AREA = new TouchArea(0, 0.25, 1, 0.75, WIDTH, HEIGHT);
    /** The names of its buttons, as a press carries them. */
    public static final String START = "start";
    public static final String STOP = "stop";
    /** What the screen says. */
    public static final TextKey TITLE = TextKey.of("gui.jstests.touch_panel.title", "PUMP 2");
    public static final TextKey FLOW = TextKey.of("gui.jstests.touch_panel.flow", "120 mB/t");
    public static final TextKey START_LABEL = TextKey.of("gui.jstests.touch_panel.start", "Start");
    public static final TextKey STOP_LABEL = TextKey.of("gui.jstests.touch_panel.stop", "Stop");

    /* Its block state and model are written by hand, as the test mod's are. */
    public static final BlockEntry<DeviceBlock> PANEL = TestSounds.CONTENT.block("test_touch_panel",
            Device.of(() -> TestTouchPanel.PANEL_ENTITY.get())::block)
            .named("Test touch panel").look(IBlockLook.cubeAll("test_touch_panel")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PanelEntity>> PANEL_ENTITY =
            TestSounds.CONTENT.blockEntity("test_touch_panel", PanelEntity::new, PANEL);
    /** The name of the last part pressed, as the server heard it; empty at first. */
    public static final AtomicReference<String> PRESSED = new AtomicReference<>("");
    /** Whether the pump runs: Start sets it, Stop clears it. */
    public static final AtomicBoolean RUNNING = new AtomicBoolean();

    private TestTouchPanel() {
    }

    /** Declares that the panel's north face answers presses, before the test mod's content is registered. */
    public static void declare() {
        ScreenTouches.declare(PANEL, new ITouchScreen() {
            @Override
            public Optional<TouchArea> area(final BlockState state, final Direction face) {
                return face == Direction.NORTH ? Optional.of(AREA) : Optional.empty();
            }

            @Override
            public void pressed(final ServerPlayer player, final BlockPos pos, final Direction face,
                                final TouchPress press) {
                PRESSED.set(press.target());
                if (press.on(START)) {
                    RUNNING.set(true);
                } else if (press.on(STOP)) {
                    RUNNING.set(false);
                }
            }
        });
    }

    /** The panel: nothing of its own, its renderer shows its screen. */
    public static final class PanelEntity extends SyncedBlockEntity {

        public PanelEntity(final BlockPos pos, final BlockState state) {
            super(PANEL_ENTITY.get(), pos, state);
        }
    }
}
