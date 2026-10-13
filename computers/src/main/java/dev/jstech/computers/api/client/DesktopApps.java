/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import dev.jstech.computers.client.os.AddonDesktopApp;
import dev.jstech.computers.client.os.ProgramClient;
import dev.jstech.computers.os.GraphicsPrograms;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * Which Java program opens which program's window, on the side of the game that has screens.
 *
 * <p>The other half of adding a program with a window: the program goes in through the register event, on both sides,
 * so the computers can install it and list it; what opens its window goes in here, from client setup. A program
 * registered with no window here has none, and opening it does nothing.
 */
@ApiStatus.Experimental
public final class DesktopApps {

    private DesktopApps() {
    }

    /**
     * Says what opens the window of the program registered under {@code program}.
     *
     * @param surface whether its window draws through a surface, which holds video memory on the machine while it is
     *                open; a program that answers a renderer from {@link DesktopProgram#renderer()} says yes. The
     *                server counts that memory too, so the program must also be named by
     *                {@code ComputersRegisterEvent.graphicsProgram}; this flag only covers this game
     */
    public static void register(final ResourceLocation program, final boolean surface,
                                final IDesktopProgramFactory factory) {
        if (program == null || factory == null) {
            return;
        }
        if (surface) {
            GraphicsPrograms.register(program);
        }
        ProgramClient.register(program, (host, monitor, desktop) ->
                new AddonDesktopApp(program, factory.create(host, monitor, desktop)));
    }
}
