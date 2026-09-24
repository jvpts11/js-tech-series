/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.content.ModContent;
import net.minecraft.resources.ResourceLocation;

/**
 * Two sounds the tests play through the series' sound system, one of the interface and one of the world. They play
 * files the game already has, so the test mod ships no sound of its own.
 */
public final class TestSounds {

    public static final ModContent CONTENT = new ModContent(JsTests.MODID);

    public static final SoundKey CLICK = CONTENT.sound("test/click").onScreen()
            .file(ResourceLocation.withDefaultNamespace("random/click"))
            .subtitle("A test click").register();

    public static final SoundKey HUM = CONTENT.sound("test/hum").world().range(8)
            .file(ResourceLocation.withDefaultNamespace("block/beacon/ambient"))
            .subtitle("A test hum").register();

    private TestSounds() {
    }
}
