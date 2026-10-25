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
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.Eras;
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
        final Era wanted = era(era);
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
        final Era wanted = era(era);
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
        for (final Era era : HardwareEra.SERIES) {
            if (ComputingContent.tab(era) != null) {
                names.add(era.path());
            }
        }
        return names;
    }

    /*
     * The parts and the kinds a startup script declares, each as an object of its sheet's fields: they are kept as the
     * script runs and declared once the game sets up, as an add-on's are.
     */

    /** Makes the item {@code item} a processor of that sheet: era, isa, socket, cores, freq_mhz, tdp_watts. */
    public static void processor(final String item, final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.PROCESSOR, item, sheet, null);
    }

    /** Makes the item a graphics card: era, bus, cores, vram_mb, tdp_watts, clock_mhz, arch. */
    public static void graphicsCard(final String item, final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.GRAPHICS_CARD, item, sheet, null);
    }

    /** Makes the item a memory module: era, generation, buffer_items, tdp_watts. */
    public static void memory(final String item, final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.MEMORY, item, sheet, null);
    }

    /** Makes the item a disk: tier (hdd, ssd or nvme), era, capacity_items, tdp_watts. */
    public static void disk(final String item, final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.DISK, item, sheet, null);
    }

    /**
     * Makes the item a motherboard: form_factor, era, socket, cpu_slots, memory (the generations it takes),
     * ram_slots, bus, slots, disk_slots.
     */
    public static void motherboard(final String item, final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.MOTHERBOARD, item, sheet, null);
    }

    /** Makes the item a power supply, shown with {@code era}: wattage, efficiency_percent. */
    public static void powerSupply(final String item, final Object sheet, final String era) {
        ScriptedHardware.keep(ScriptedHardware.Kind.POWER_SUPPLY, item, sheet, era);
    }

    /** Makes the item an expansion card of a kind that asks only for its slot: kind, era, bus, tdp_watts. */
    public static void expansionCard(final String item, final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.CARD, item, sheet, null);
    }

    /** Adds a generation of memory: id, label, latency_ticks. */
    public static void memoryGeneration(final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.MEMORY_GENERATION, null, sheet, null);
    }

    /** Adds a generation of expansion slot: id, generation, label, slot_name, family (isa, pci, agp or pcie). */
    public static void slotGeneration(final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.SLOT_GENERATION, null, sheet, null);
    }

    /** Adds a form factor of motherboard: id, label. */
    public static void formFactor(final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.FORM_FACTOR, null, sheet, null);
    }

    /** Adds a kind of expansion card: id. */
    public static void cardKind(final Object sheet) {
        ScriptedHardware.keep(ScriptedHardware.Kind.CARD_KIND, null, sheet, null);
    }

    private static ResourceLocation id(final String id) {
        return id.indexOf(':') < 0 ? ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id)
                : ResourceLocation.parse(id);
    }

    private static Era era(final String name) {
        final Era found = Eras.byName(name).orElse(null);
        if (found != null) {
            return found;
        }
        throw new IllegalArgumentException("no era is called '" + name + "'; the eras are " + eras());
    }
}
