/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.tests.JsTests;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

/**
 * A program with a window, registered the way another mod registers one: its name through the computers' register
 * event on both sides, and its window from the client, where it draws a picture of its own on a surface.
 */
public final class TestDesktopPrograms {

    /** A program whose window draws a moving picture of pixels, frame by frame. */
    public static final ResourceLocation PIXELS = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "pixels");

    private TestDesktopPrograms() {
    }

    /** Registers the program while the game loads. */
    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(ComputersRegisterEvent.class, event -> event.program(
                ProgramSpec.of(PIXELS, "pixels", false, EnumSet.allOf(Platform.class), 1, ProgramKind.APP, 0,
                                HostScope.ANY)
                        .named("Pixels")
                        .described("A window that draws its own picture, frame by frame.")));
    }
}
