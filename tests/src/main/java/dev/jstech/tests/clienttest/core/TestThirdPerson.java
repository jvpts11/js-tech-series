/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.camera.ThirdPersonView;
import dev.jstech.core.client.camera.ThirdPersonCameras;
import dev.jstech.tests.JsTests;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The test mod's camera in the third person, registered as another mod registers one: it stands the player's camera
 * where a test says, and leaves the view to the game when the test says nothing.
 */
public final class TestThirdPerson {

    /** Where the camera stands, or none for the game's own. */
    public static final AtomicReference<ThirdPersonView> SHOWN = new AtomicReference<>();

    private TestThirdPerson() {
    }

    /** Registers it, once, from the test mod's constructor on the player's game. */
    public static void register() {
        ThirdPersonCameras.register(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_camera"), 0,
                (viewer, front, partialTick) -> viewer instanceof LocalPlayer ? SHOWN.get() : null);
    }
}
