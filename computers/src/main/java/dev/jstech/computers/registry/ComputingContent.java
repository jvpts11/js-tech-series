/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ReadyDisks;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.scripting.JsComputersScripts;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.ScriptBinding;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What J's Computers puts in the game is declared through {@link #CONTENT}, and shown in its creative tabs: one for
 * each hardware era, holding everything of that era, and one before them for what every era shares. Every tab has the
 * same shelves in the same order, and a thing is declared onto the shelf of its era's tab, so it is never in two.
 *
 * <p>A thing's era is the one it carries: a machine's is its chassis, a part's its specification, a cable's its
 * generation, a medium's its format, an installer's the system or program written on it. What works the same in every
 * era, such as the crafting cable, goes to the shared tab.
 *
 * <p>Nothing here names a block or an item except lazily, so the catalogue classes that declare into these shelves
 * can load in any order.
 */
public final class ComputingContent {

    public static final ModContent CONTENT = new ModContent(JsComputers.MODID);

    /*
     * The tabs, declared in the order they stand. Each era's icon is the thing that era is remembered by: the floppy,
     * the CD, the GeForce 8800 GT, a Haswell board and the RTX 5090.
     */
    private static final ContentTab SHARED_TAB =
            CONTENT.tab("computing", "J's Computers", () -> ComputingModule.MAINFRAME);
    private static final Map<HardwareEra, ContentTab> ERA_TABS = eraTabs();

    private static final Map<Shelf, ContentTab.Section> SHARED = shelves(SHARED_TAB, null);
    private static final Map<HardwareEra, Map<Shelf, ContentTab.Section>> BY_ERA = eraShelves();

    /** What a modpack's scripts reach as {@code JsComputers}: installers, ready disks and the lists of each era. */
    public static final ScriptBinding SCRIPTS = CONTENT.scriptBinding("JsComputers", JsComputersScripts.class);

    private ComputingContent() {
    }

    /**
     * The shelves of a tab, in the order the tab shows them: the computers by kind, the racks and what they carry, the
     * devices at a desk, the network, the parts, then the media and what is written on them.
     *
     * <p>Each shelf is also an item tag of the mod, holding what that shelf holds in every tab, so a datapack or a
     * script can ask for any part ({@code #jsc:components}) the way the era tags ({@code #jsc:era/vintage}) give
     * anything of one era.
     */
    public enum Shelf {
        /** Personal Computers, in every case of their era. */
        PERSONAL_COMPUTERS("personal_computers"),
        /** Crafting Computers. */
        CRAFTING_COMPUTERS("crafting_computers"),
        /** Cluster Management Computers. */
        CLUSTER_MANAGEMENT_COMPUTERS("cluster_management_computers"),
        /** The Mainframes, which run a network. */
        MAINFRAMES("mainframes"),
        /** Server racks and supercomputer racks, the server cases, the servers and the nodes. */
        SERVER_RACK("racks_and_servers"),
        /** What a rack carries in its bays besides servers: the rack units. */
        RACK_BAYS("rack_bays"),
        /** The devices at a desk: monitors, drives, encoders, printers, speakers, hubs and the peripheral cables. */
        PERIPHERALS("peripherals"),
        /** The data network: its cables, routers, repeaters and interfaces, and the buses that mount on a cable. */
        NETWORK("network"),
        /** The parts a machine is built from: boards, processors, memory, cards, supplies and disks. */
        COMPONENTS("components"),
        /** Blank media, then an installer of every system and every installable program. */
        PROGRAMS("programs");

        private final String tag;

        Shelf(final String tag) {
            this.tag = tag;
        }

        /** The path of the shelf's item tag in the mod's namespace. */
        public String tag() {
            return tag;
        }
    }

    /** The path of the item tag holding everything of {@code era}, in the mod's namespace. */
    public static String eraTag(final HardwareEra era) {
        return "era/" + era.serializedName();
    }

    /**
     * The shelf of the tab of {@code era}, or of the shared tab for a thing every era shares.
     *
     * @throws IllegalArgumentException for an era with no tab, which has nothing declared in it yet
     */
    public static ContentTab.Section shelf(final Shelf shelf, @Nullable final HardwareEra era) {
        if (era == null) {
            return SHARED.get(shelf);
        }
        final Map<Shelf, ContentTab.Section> tab = BY_ERA.get(era);
        if (tab == null) {
            throw new IllegalArgumentException("the " + era.serializedName() + " era has no creative tab");
        }
        return tab.get(shelf);
    }

    /** The tab of {@code era}, or the shared tab for null. */
    public static ContentTab tab(@Nullable final HardwareEra era) {
        return era == null ? SHARED_TAB : ERA_TABS.get(era);
    }

    private static Map<HardwareEra, ContentTab> eraTabs() {
        final Map<HardwareEra, ContentTab> tabs = new EnumMap<>(HardwareEra.class);
        tabs.put(HardwareEra.VINTAGE, CONTENT.tab("computing_vintage", "J's Computers - Vintage",
                () -> ComputingModule.FLOPPY_DISK));
        tabs.put(HardwareEra.LEGACY, CONTENT.tab("computing_legacy", "J's Computers - Legacy",
                () -> ComputingModule.CD_ROM));
        tabs.put(HardwareEra.TRANSITION, CONTENT.tab("computing_transition", "J's Computers - Transition",
                () -> HardwareItems.GPU_VERTEX_8800_GT));
        tabs.put(HardwareEra.STANDARD, CONTENT.tab("computing_standard", "J's Computers - Standard",
                () -> HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150));
        tabs.put(HardwareEra.ADVANCED, CONTENT.tab("computing_advanced", "J's Computers - Advanced",
                () -> HardwareItems.GPU_VERTEX_RTX_5090));
        tabs.forEach((era, tab) -> tab.tagging(eraTag(era)));
        return tabs;
    }

    private static Map<HardwareEra, Map<Shelf, ContentTab.Section>> eraShelves() {
        final Map<HardwareEra, Map<Shelf, ContentTab.Section>> byEra = new EnumMap<>(HardwareEra.class);
        // The eras that have a tab are exactly the keys of ERA_TABS, so a tab can never lack its shelves.
        for (final Map.Entry<HardwareEra, ContentTab> entry : ERA_TABS.entrySet()) {
            byEra.put(entry.getKey(), shelves(entry.getValue(), entry.getKey()));
        }
        return byEra;
    }

    /*
     * The shelves of one tab, made in the order the tab shows them. The parts are sorted by kind, the way the hardware
     * catalogue lists them; the order is asked for only when the tab is filled, since reading it now would load the
     * catalogue, which declares onto these shelves before they exist.
     */
    private static Map<Shelf, ContentTab.Section> shelves(final ContentTab tab, @Nullable final HardwareEra era) {
        final Map<Shelf, ContentTab.Section> shelves = new EnumMap<>(Shelf.class);
        for (final Shelf shelf : Shelf.values()) {
            final ContentTab.Section section = tab.section().tagging(shelf.tag());
            if (shelf == Shelf.COMPONENTS) {
                section.sortedBy((one, other) -> HardwareItems.CREATIVE_ORDER.compare(one, other));
            }
            if (shelf == Shelf.PROGRAMS && era != null) {
                section.alsoShowing(output -> installers(output, era));
            }
            shelves.put(shelf, section);
        }
        return shelves;
    }

    /**
     * The installers of {@code era}: one per registered system that installs from it and per installable program of
     * it, straight from the registries, each on the medium of its generation: Vintage on a floppy, Legacy on a CD, a
     * Standard system on a bootable flash drive, a Standard application on a DVD. Size never decides, so a small
     * server daemon is not a floppy.
     */
    private static void installers(final CreativeModeTab.Output output, final HardwareEra era) {
        for (final OsDef os : OsBootstrap.builtinOses()) {
            if (os.minEra() == era) {
                output.accept(InstallerStacks.forSystem(os));
            }
        }
        // Then each of those systems already on a disk of the era, for a machine that should simply come up.
        for (final OsDef os : OsBootstrap.builtinOses()) {
            if (os.minEra() == era) {
                final ItemStack ready = ReadyDisks.withSystem(os.id(), era);
                if (!ready.isEmpty()) {
                    output.accept(ready);
                }
            }
        }
        for (final ProgramSpec program : OsBootstrap.builtinPrograms()) {
            if (program.installable() && program.era() == era) {
                output.accept(InstallerStacks.forProgram(program));
            }
        }
    }
}
