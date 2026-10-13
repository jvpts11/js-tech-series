/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The installation media as stacks: a blank medium of the right format with a system or a program written on it.
 * The creative tabs make theirs here, and so does anything else that hands a player an installer, such as a modpack's
 * recipe, so an installer made anywhere is the same stack.
 */
public final class InstallerStacks {

    private InstallerStacks() {
    }

    /** The installer of the system registered as {@code osId}, or an empty stack when no system is. */
    public static ItemStack forSystem(final ResourceLocation osId) {
        final OsDef os = OsRegistry.getOs(osId);
        return os == null ? ItemStack.EMPTY : forSystem(os);
    }

    /** The installer of {@code os}, on the medium of the era it first runs in. */
    public static ItemStack forSystem(final OsDef os) {
        return stamped(InstallMedia.forSystem(os.minEra()), MediaKind.OS_INSTALL, os.id());
    }

    /**
     * The installer of the program registered as {@code programId}, or an empty stack when no program is or when it
     * is not one a player installs, such as a program every computer ships with.
     */
    public static ItemStack forProgram(final ResourceLocation programId) {
        final ProgramSpec program = OsRegistry.getProgram(programId);
        return program == null || !program.installable() ? ItemStack.EMPTY : forProgram(program);
    }

    /** The installer of {@code program}, on the medium of its era and kind. */
    public static ItemStack forProgram(final ProgramSpec program) {
        return stamped(InstallMedia.forProgram(program.era(), program.kind()), MediaKind.PROGRAM_INSTALL,
                program.id());
    }

    /** The blank medium item of a physical format, to write an installer onto. */
    public static Item blank(final MediaFormat format) {
        return switch (format) {
            case FLOPPY -> ComputingModule.FLOPPY_DISK.get();
            case CD -> ComputingModule.CD_ROM.get();
            case DVD -> ComputingModule.DVD_ROM.get();
            case USB -> ComputingModule.USB_FLASH_DRIVE.get();
            case BLU_RAY -> ComputingModule.BD_ROM.get();
        };
    }

    private static ItemStack stamped(final MediaFormat format, final MediaKind kind, final ResourceLocation payload) {
        final ItemStack medium = new ItemStack(blank(format));
        MediaItem.setKind(medium, kind);
        MediaItem.setPayload(medium, payload);
        return medium;
    }
}
