/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.os.BoardFirmware;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The machines of each era as an abandoned building holds them, their boards with their firmware written. A computer
 * is the one its mock draws in the case (the board, the processor and the memory) with the supply, the disk and the
 * cooler of its era, and the system its working machines run. A rack's server and a Mainframe have their era's board,
 * and on it the parts a player picks from the era's catalogue: the strongest processor of the board's socket and era,
 * the largest memory it takes, the strongest supply of the era. A rack holds its era's supply unit at its foot and its
 * switch at its top.
 */
public final class AbandonedLoadouts {

    private static final Map<Era, Loadout> LOADOUTS = Map.of(
            HardwareEra.VINTAGE, new Loadout(List.of("motherboard_babyat_vintage", "cpu_integra_486dx2", "ram_simm_16",
                    "ram_simm_16", "psu_200", "disk_vaultis_trench_100m", "stock_cooler_vintage"), "mc_dos",
                    "motherboard_eeb_vintage", "motherboard_mtx_vintage", "vintage_"),
            HardwareEra.LEGACY, new Loadout(List.of("motherboard_atx_legacy_478", "cpu_integra_pentix_4_520",
                    "ram_ddr_512", "ram_ddr_512", "psu_350", "disk_vaultis_link_ide_40g", "stock_cooler_legacy"),
                    "frames_xp", "motherboard_eatx_legacy_604", "motherboard_mtx_legacy", "legacy_"),
            HardwareEra.TRANSITION, new Loadout(List.of("motherboard_atx_transition_775",
                    "cpu_integra_centro_2_duo_e6600", "ram_ddr2_2048", "ram_ddr2_2048", "psu_500b", "disk_hdd_500g",
                    "stock_cooler_transition"), "frames_7", "motherboard_eeb_t_1366", "motherboard_mtx_t",
                    "transition_"),
            HardwareEra.STANDARD, new Loadout(List.of("motherboard_atx_standard_1155", "cpu_integra_centro_c7_2600k",
                    "ram_ddr3_8192", "ram_ddr3_8192", "psu_650g", "disk_ssd_500g", "stock_cooler_standard"),
                    "frames_10", "motherboard_eeb_s_2011", "motherboard_mtx_s_2011", ""),
            HardwareEra.ADVANCED, new Loadout(List.of("motherboard_atx_advanced_1700", "cpu_integra_centro_c9_12900k",
                    "ram_ddr5_16384", "ram_ddr5_16384", "psu_850g", "disk_nvme_1t", "stock_cooler_advanced"),
                    "frames_11", "motherboard_eeb_a_sp3", "motherboard_mtx_a_sp3", "advanced_"));
    /** The system every Mainframe runs its network with. */
    private static final ResourceLocation NETWORK_SYSTEM = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "mc_net");
    /* Where the disk comes in a computer's parts. */
    private static final int DISK = 5;

    private AbandonedLoadouts() {
    }

    /** The parts of {@code era}'s computer, one stack each, in the order a player seats them. */
    public static List<ItemStack> parts(final Era era) {
        final List<ItemStack> out = new ArrayList<>();
        for (final String path : loadout(era).parts()) {
            out.add(flashed(new ItemStack(item(path))));
        }
        return out;
    }

    /** Whether {@code part} sits on the board, and goes with it: everything but the supply and the disk. */
    public static boolean onBoard(final ItemStack part) {
        return HardwareRegistry.psu(part) == null && HardwareRegistry.disk(part) == null;
    }

    /** The system {@code era}'s working machines run. */
    public static ResourceLocation system(final Era era) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, loadout(era).system());
    }

    /** The system a Mainframe of any era runs its network with. */
    public static ResourceLocation networkSystem() {
        return NETWORK_SYSTEM;
    }

    /** {@code era}'s disk, the one its computers hold. */
    public static ItemStack disk(final Era era) {
        return new ItemStack(item(loadout(era).parts().get(DISK)));
    }

    /** The parts of {@code era}'s rack server: its board first, then the processor, the memory, the supply. */
    public static List<ItemStack> serverParts(final Era era) {
        return onBoard(item(loadout(era).serverBoard()), era);
    }

    /** The parts of {@code era}'s Mainframe: its board first, then the processor, the memory, the supply. */
    public static List<ItemStack> mainframeParts(final Era era) {
        return onBoard(item(loadout(era).mainframeBoard()), era);
    }

    /** {@code era}'s 1U server, still empty. */
    public static ItemStack server(final Era era) {
        return new ItemStack(item(loadout(era).prefix() + "server"));
    }

    /** {@code era}'s supercomputer node, still empty; the Advanced's from the Advanced on, the Standard's before. */
    public static ItemStack node(final Era era) {
        return new ItemStack(item(HardwareEra.withParts(era) == HardwareEra.ADVANCED ? "advanced_supercomputer_node"
                : "supercomputer_node"));
    }

    /** One of the mod's items by its id's path. */
    public static ItemStack named(final String path) {
        return new ItemStack(item(path));
    }

    /** {@code era}'s rack supply unit, which stands at a rack's foot. */
    public static ItemStack rackUps(final Era era) {
        return new ItemStack(item(loadout(era).prefix() + "rack_ups"));
    }

    /** {@code era}'s KVM switch, which stands at a rack's top. */
    public static ItemStack kvmSwitch(final Era era) {
        return new ItemStack(item(loadout(era).prefix() + "kvm_switch"));
    }

    /* A board and what a player seats on it, picked from the parts of its era. */
    private static List<ItemStack> onBoard(final Item board, final Era era) {
        final MotherboardSpec spec = HardwareRegistry.board(new ItemStack(board));
        if (spec == null) {
            throw new IllegalStateException(BuiltInRegistries.ITEM.getKey(board) + " is no board");
        }
        final Item cpu = best(item -> HardwareRegistry.cpu(new ItemStack(item)) instanceof CpuSpec c && !c.alien()
                && c.socket().equals(spec.socket()) && c.era() == spec.era(),
                item -> (long) cpu(item).cores() * cpu(item).freqMhz());
        final Item ram = best(item -> HardwareRegistry.ram(new ItemStack(item)) instanceof RamSpec r
                && spec.acceptedRam().contains(r.generation()) && r.era() == spec.era(),
                item -> ram(item).bufferItems());
        final Item psu = best(item -> HardwareRegistry.psu(new ItemStack(item)) instanceof PsuSpec
                && HardwareRegistry.part(item) instanceof HardwareRegistry.Part part && part.era() == era,
                item -> psu(item).wattage());
        return List.of(flashed(new ItemStack(board)), new ItemStack(cpu), new ItemStack(ram), new ItemStack(psu));
    }

    /* A board with its firmware written, as the machine that held it ran on it: anything else as it is. */
    private static ItemStack flashed(final ItemStack part) {
        return HardwareRegistry.board(part) != null && BoardFirmware.blank(part) ? BoardFirmware.written(part) : part;
    }

    private static Item best(final Predicate<Item> fits, final ToLongFunction<Item> score) {
        return HardwareRegistry.parts().stream().filter(fits)
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .max(Comparator.comparingLong(score))
                .orElseThrow(() -> new IllegalStateException("no part of an abandoned building's machine fits"));
    }

    private static CpuSpec cpu(final Item item) {
        return HardwareRegistry.cpu(new ItemStack(item));
    }

    private static RamSpec ram(final Item item) {
        return HardwareRegistry.ram(new ItemStack(item));
    }

    private static PsuSpec psu(final Item item) {
        return HardwareRegistry.psu(new ItemStack(item));
    }

    private static Loadout loadout(final Era era) {
        final Loadout found = LOADOUTS.get(HardwareEra.withParts(era));
        if (found == null) {
            throw new IllegalArgumentException("no abandoned computer of the " + era.path());
        }
        return found;
    }

    private static Item item(final String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
    }

    /*
     * An era's machines: its computer's parts by item and its system; its rack server's and its Mainframe's boards;
     * the prefix its rack units and server are named with.
     */
    private record Loadout(List<String> parts, String system, String serverBoard, String mainframeBoard,
                           String prefix) {
    }
}
