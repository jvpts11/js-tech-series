/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.scripting;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ReadyDisks;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * What J's Computers hands the scripts of a modpack, under the name {@code JsComputers} (with KubeJS, in its server
 * and client scripts): the stacks that only exist made, an installer with its software written on it and a disk with
 * a system already on it, and the lists of what there is, so a script can go through an era rather than name every
 * system in it.
 *
 * <pre>{@code
 * ServerEvents.recipes(event => {
 *   event.shapeless(JsComputers.installer('frames_95'), ['minecraft:paper', 'jsc:floppy_disk'])
 * })
 * }</pre>
 *
 * <p>An id without a namespace is J's Computers' own: {@code 'frames_95'} is {@code 'jsc:frames_95'}. An id nothing is
 * registered under gives an empty stack, which a recipe refuses with the script's line in the log.
 */
public final class JsComputersScripts {

    private JsComputersScripts() {
    }

    /** The installer of a system: the medium of the era it first runs in, with the system written on it. */
    public static ItemStack installer(final String system) {
        return InstallerStacks.forSystem(id(system));
    }

    /** The installer of a program a player installs: the medium of its era and kind, with the program on it. */
    public static ItemStack programInstaller(final String program) {
        return InstallerStacks.forProgram(id(program));
    }

    /**
     * A disk of the system's own era with the system already on it, which brings a machine straight up, the machine
     * named My Computer: the creative tab's shortcut past the installer.
     */
    public static ItemStack readyDisk(final String system) {
        final OsDef os = OsRegistry.getOs(id(system));
        return os == null ? ItemStack.EMPTY : ReadyDisks.withSystem(os.id(), os.minEra());
    }

    /** The ids of the systems that first run in {@code era} ({@code 'vintage'}, {@code 'legacy'}...). */
    public static List<String> systems(final String era) {
        final HardwareEra wanted = era(era);
        final List<String> ids = new ArrayList<>();
        for (final OsDef os : OsRegistry.oses()) {
            if (os.minEra() == wanted) {
                ids.add(os.id().toString());
            }
        }
        return ids;
    }

    /** The ids of the programs of {@code era} a player installs from a medium. */
    public static List<String> programs(final String era) {
        final HardwareEra wanted = era(era);
        final List<String> ids = new ArrayList<>();
        for (final ProgramSpec program : OsRegistry.programs()) {
            if (program.installable() && program.era() == wanted) {
                ids.add(program.id().toString());
            }
        }
        return ids;
    }

    /**
     * The eras that have things in the game, oldest first, by the names the other calls take; each is also the item
     * tag holding everything of it, such as {@code #jsc:era/vintage}.
     */
    public static List<String> eras() {
        final List<String> names = new ArrayList<>();
        for (final HardwareEra era : HardwareEra.values()) {
            if (ComputingContent.tab(era) != null) {
                names.add(era.serializedName());
            }
        }
        return names;
    }

    private static ResourceLocation id(final String id) {
        return id.indexOf(':') < 0 ? ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id)
                : ResourceLocation.parse(id);
    }

    private static HardwareEra era(final String name) {
        for (final HardwareEra era : HardwareEra.values()) {
            if (era.serializedName().equals(name)) {
                return era;
            }
        }
        throw new IllegalArgumentException("no era is called '" + name + "'; the eras are " + eras());
    }
}
