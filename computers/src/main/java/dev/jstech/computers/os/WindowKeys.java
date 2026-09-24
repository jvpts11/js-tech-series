/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What a window on a desktop is known by, on the client that draws it and on the machine that keeps it.
 *
 * <p>A program's window goes by the program's id ({@code jsc:files}), never by what the desktop calls the program:
 * that name changes with the desktop (Dolphin, Nautilus, the File Manager) and with the player's language, and a
 * layout saved under one of them has to come back under another. The few windows no program owns have keys of
 * their own: the welcome, a setup, the trash, and CDE's Application Manager, one window to each of its groups.
 */
public final class WindowKeys {

    /** The window a machine greets whoever looks at it with, the first time. */
    public static final String WELCOME = "jsc:welcome";

    /** The window a program being set up shows its progress in. */
    public static final String SETUP = "jsc:setup";

    /** The trash's window, one to a desktop. */
    public static final String TRASH = "jsc:trash";

    /** CDE's Application Manager, whose windows are one to a group and one for the groups themselves. */
    private static final String APPLICATION_MANAGER = "jsc:application_manager";

    private WindowKeys() {
    }

    /** The key of that program's window. */
    public static String of(final ResourceLocation program) {
        return program.toString();
    }

    /** The program a window so keyed belongs to, or null for a window no registered program owns. */
    @Nullable
    public static ProgramSpec program(final String key) {
        final ResourceLocation id = ResourceLocation.tryParse(key);
        return id == null ? null : OsRegistry.getProgram(id);
    }

    /** The key of the Application Manager's window on {@code group}, or of the one on the groups for null. */
    public static String applicationManager(@Nullable final CdeAppGroup group) {
        return group == null ? APPLICATION_MANAGER : APPLICATION_MANAGER + "/" + group.name().toLowerCase(Locale.ROOT);
    }

    /** Whether a window so keyed is one of the Application Manager's. */
    public static boolean isApplicationManager(final String key) {
        return key.equals(APPLICATION_MANAGER) || key.startsWith(APPLICATION_MANAGER + "/");
    }

    /** The group an Application Manager window so keyed has open, or null for the window on the groups. */
    @Nullable
    public static CdeAppGroup applicationManagerGroup(final String key) {
        if (!key.startsWith(APPLICATION_MANAGER + "/")) {
            return null;
        }
        final String name = key.substring(APPLICATION_MANAGER.length() + 1);
        for (final CdeAppGroup group : CdeAppGroup.values()) {
            if (group.name().toLowerCase(Locale.ROOT).equals(name)) {
                return group;
            }
        }
        return null;
    }
}
