/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.hardware.CpuSocketId;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.Microarchitectures;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.item.SoundCardItem;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

/**
 * The per-era hardware item catalog: every CPU, RAM module, PSU, GPU and motherboard a player can
 * install across the Vintage, Legacy and Standard eras.
 *
 * <p>Derived numbers (orchestration capacity in items/tick, and GPU threads) are never stored here.
 * They come from the formulas on {@link CpuSpec} and {@link GpuSpec}; only the raw inputs are listed.
 *
 * <p>The components already registered in {@link ComputingModule} for the Standard era (the Servo and
 * Ascent CPUs, the DDR3 module, the HD 7970 GPU, the MTX/EEB/ATX standard boards, the 650G PSU and the
 * disks) are not duplicated here; this class adds the items those leave out and completes the partial
 * sets. The specialized Mining (MNG) and Simulation (SIM) boards are intentionally deferred to their own
 * modules and are not registered here.
 *
 * <p>A declaration carries the item's id, its spec and its name, and nothing else is written anywhere: its model,
 * its name in the language file and its place in the creative tab all come from it. Adding a part is one line here.
 */
public final class HardwareItems {

    /**
     * The catalogue's order in the creative tab: era by era (Vintage to Singularity), and within each era
     * motherboards, then CPUs, RAM, GPUs and sound cards, so the progression reads cleanly; then the supplies and the
     * disks, which follow every era. Parts the order ranks alike keep the order they are declared in.
     */
    public static final Comparator<Item> CREATIVE_ORDER = Comparator.comparingInt(HardwareItems::shelf)
            .thenComparing(HardwareItems::era)
            .thenComparingInt(HardwareItems::kind);

    //  VINTAGE: ISA/PCI/AGP 2x buses, SIMM/EDO/SDRAM RAM, single-core CPUs

    // Socket 3: the 486, and the chip that took the socket to 133 MHz.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_486SX = cpu("cpu_integra_486sx",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 25, 3, false)
                    .on(Microarchitectures.I486, "")).named("Integra 486SX").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_486DX2 = cpu("cpu_integra_486dx2",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 66, 5, false)
                    .on(Microarchitectures.I486, "")).named("Integra 486DX2").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_486DX4 = cpu("cpu_integra_486dx4",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 100, 5, false)
                    .on(Microarchitectures.I486, "")).named("Integra 486DX4").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_5X86_133 = cpu("cpu_velocion_5x86_133",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 133, 4, false)
                    .on(Microarchitectures.I486, "X5")).named("Velocion 5x86-133").register();

    // Socket 7: the Pentium and the chips that raced it, up to the K6-III+.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_75 = cpu("cpu_integra_pentix_75",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 75, 8, false)
                    .on(Microarchitectures.P5, "P54C")).named("Integra Pentix 75").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_K5_PR133 = cpu("cpu_velocion_k5_pr133",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 100, 11, false)
                    .on(Microarchitectures.K5, "5k86")).named("Velocion K5 PR133").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_133 = cpu("cpu_integra_pentix_133",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 133, 11, false)
                    .on(Microarchitectures.P5, "P54CS")).named("Integra Pentix 133").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_MMX_233 = cpu("cpu_integra_pentix_mmx_233",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 233, 17, false)
                    .on(Microarchitectures.P5, "P55C")).named("Integra Pentix MMX 233").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_K6_II = cpu("cpu_velocion_k6_ii",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 350, 15, false)
                    .on(Microarchitectures.K6, "Chomper")).named("Velocion K6-II").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_K6_III = cpu("cpu_velocion_k6_iii",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 400, 20, false)
                    .on(Microarchitectures.K6, "Sharptooth")).named("Velocion K6-III").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_K6_III_PLUS = cpu("cpu_velocion_k6_iii_plus",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 450, 22, false)
                    .on(Microarchitectures.K6, "Sharptooth")).named("Velocion K6-III+").register();

    // Socket 8: the Pentium Pro, the processor of the Vintage server and Mainframe boards.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_PRO_150 = cpu("cpu_integra_pentix_pro_150",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_8, 1, 150, 29, false)
                    .on(Microarchitectures.P6, "")).named("Integra Pentix Pro 150").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_PRO_180 = cpu("cpu_integra_pentix_pro_180",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_8, 1, 180, 32, false)
                    .on(Microarchitectures.P6, "")).named("Integra Pentix Pro 180").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_PRO_200 = cpu("cpu_integra_pentix_pro_200",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_8, 1, 200, 35, false)
                    .on(Microarchitectures.P6, "")).named("Integra Pentix Pro 200").register();

    // Slot 1: the cartridges of the last Vintage board, the top of the era.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CELER_300A = cpu("cpu_integra_celer_300a",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SLOT_1, 1, 300, 19, false)
                    .on(Microarchitectures.P6, "Mendocino")).named("Integra Celer 300A").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_II_300 = cpu("cpu_integra_pentix_ii_300",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SLOT_1, 1, 300, 43, false)
                    .on(Microarchitectures.P6, "Klamath")).named("Integra Pentix II 300").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_II_450 = cpu("cpu_integra_pentix_ii_450",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SLOT_1, 1, 450, 27, false)
                    .on(Microarchitectures.P6, "Deschutes")).named("Integra Pentix II 450").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_III_600 = cpu("cpu_integra_pentix_iii_600",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SLOT_1, 1, 600, 35, false)
                    .on(Microarchitectures.P6, "Katmai")).named("Integra Pentix III 600").register();

    /*
     * Vintage memory: the 30- and 72-pin SIMMs, EDO, and the first SDRAM DIMMs, which only the Slot 1 board takes.
     * A module holds an item for every 4 MB; the watts climb gently with the size.
     */
    public static final DeferredItem<RamItem> RAM_SIMM_4 = ram("ram_simm_4",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.SIMM, 1, 1)).named("Stratix Layer SIMM-4").register();
    public static final DeferredItem<RamItem> RAM_SIMM_16 = ram("ram_simm_16",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.SIMM, 4, 2)).named("Stratix Layer SIMM-16").register();
    public static final DeferredItem<RamItem> RAM_EDO_16 = ram("ram_edo_16",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.EDO, 4, 2)).named("Stratix Layer EDO-16").register();
    public static final DeferredItem<RamItem> RAM_EDO_32 = ram("ram_edo_32",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.EDO, 8, 3)).named("Stratix Layer EDO-32").register();
    public static final DeferredItem<RamItem> RAM_EDO_64 = ram("ram_edo_64",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.EDO, 16, 4)).named("Stratix Layer EDO-64").register();
    public static final DeferredItem<RamItem> RAM_SDRAM_32 = ram("ram_sdram_32",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.SDRAM, 8, 3)).named("Stratix Layer SDRAM-32").register();
    public static final DeferredItem<RamItem> RAM_SDRAM_64 = ram("ram_sdram_64",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.SDRAM, 16, 4)).named("Stratix Layer SDRAM-64").register();

    /*
     * Vintage GPU ladder, ISA to AGP. The two VGA cards are the floor; the 3D accelerators climb from the 3D Blaster
     * to the Voodoo GFX, with the Prism TNT, the card for the Slot 1 board's AGP, just under it. Single-digit cores and
     * a few MB of VRAM is era-appropriate for fixed-function 2D and early 3D chips. The VGA-256 is IBM's own
     * adapter, from before the card makers the mod parodies existed, so it carries no maker's name.
     */
    public static final DeferredItem<GpuItem> GPU_VGA_256 = gpu("gpu_vga_256",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.ISA, 1, 1, 5).on(Microarchitectures.VGA, "", 25))
            .named("VGA-256").register();
    public static final DeferredItem<GpuItem> GPU_WONDER_VGA = gpu("gpu_wonder_vga",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.ISA, 1, 1, 5).on(Microarchitectures.VGA, "18800", 28))
            .named("Atrion Wonder VGA").register();
    public static final DeferredItem<GpuItem> GPU_3D_BLASTER = gpu("gpu_3d_blaster",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 1, 2, 5).on(Microarchitectures.RENDITION, "V1000", 25))
            .named("Artisan 3D Blaster").register();
    public static final DeferredItem<GpuItem> GPU_RAVE_PRO = gpu("gpu_rave_pro",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 1, 4, 8).on(Microarchitectures.RAGE, "", 75))
            .named("Atrion Rave Pro").register();
    public static final DeferredItem<GpuItem> GPU_PRISM_4 = gpu("gpu_prism_4",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 2, 4, 12).on(Microarchitectures.NV3, "", 100))
            .named("Envya Prism 4").register();
    public static final DeferredItem<GpuItem> GPU_VOODOO_GFX = gpu("gpu_voodoo_gfx",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 3, 8, 18).on(Microarchitectures.THREEDFX, "", 90))
            .named("Tridex Voodoo GFX").register();
    public static final DeferredItem<GpuItem> GPU_PRISM_TNT = gpu("gpu_prism_tnt",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.AGP_2X, 2, 16, 15)
                    .on(Microarchitectures.FAHRENHEIT, "NV4", 90))
            .named("Envya Prism TNT").register();

    public static final DeferredItem<PsuItem> PSU_200 =
            psu("psu_200", new PsuSpec(200, 80)).named("MF PowerBasic 200").register();
    public static final DeferredItem<PsuItem> PSU_300 =
            psu("psu_300", new PsuSpec(300, 80)).named("MF PowerBasic 300").register();

    public static final DeferredItem<MotherboardItem> MOTHERBOARD_BABYAT_VINTAGE = board("motherboard_babyat_vintage",
            new MotherboardSpec(FormFactor.BABY_AT, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_3, 1, Set.of(RamGeneration.SIMM), 4, PcieGeneration.ISA, 4, 2, 2))
            .named("MF Baby-AT I Motherboard").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_AT_VINTAGE = board("motherboard_at_vintage",
            new MotherboardSpec(FormFactor.AT, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_7, 1, Set.of(RamGeneration.SIMM, RamGeneration.EDO), 8,
                    PcieGeneration.PCI, 7, 4, 2))
            .named("MF AT Classic Motherboard").register();
    /*
     * The top of the Vintage: a 440BX board for the Slot 1 cartridges, with SDRAM and the first AGP. A board has one
     * bus, and this one's is the AGP its graphics card needs.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_AT_VINTAGE_SLOT1 =
            board("motherboard_at_vintage_slot1", new MotherboardSpec(FormFactor.AT, HardwareEra.VINTAGE,
                    CpuSocketId.SLOT_1, 1, Set.of(RamGeneration.SDRAM), 8, PcieGeneration.AGP_2X, 7, 4, 2))
                    .named("MF AT Slot 1 Motherboard").register();
    // The Mainframe's board: four Pentium Pros, as the 450GX boards of the time held.
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_MTX_VINTAGE = board("motherboard_mtx_vintage",
            new MotherboardSpec(FormFactor.MTX, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_8, 4, Set.of(RamGeneration.SIMM, RamGeneration.EDO), 16,
                    PcieGeneration.PCI, 8, 4, 8))
            .named("MF MTX-V Motherboard").register();
    /*
     * Dual-socket server board for vintage-era rack hardware, two Pentium Pros as on a 440FX board; more RAM slots
     * and card slots than the desktop boards to match server-class density expectations of the era.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EEB_VINTAGE = board("motherboard_eeb_vintage",
            new MotherboardSpec(FormFactor.EEB, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_8, 2, Set.of(RamGeneration.SIMM, RamGeneration.EDO), 16,
                    PcieGeneration.PCI, 10, 8, 8))
            .named("MF EEB-V Server Board").register();

    /*
     * Vintage spinning disks: MFM and IDE platters of 20, 100 and 200 MB. Tiny by design (the floor of the storage
     * ladder) and honest: at 16 bits an item costs 1 MB, so they hold 20, 100 and 200 items.
     */
    public static final DeferredItem<DiskItem> DISK_TRENCH_20M = disk("disk_vaultis_trench_20m",
            new DiskSpec(StorageTier.HDD, HardwareEra.VINTAGE, 20L, 5)).named("Vaultis Trench HDD 20M").register();
    public static final DeferredItem<DiskItem> DISK_TRENCH_100M = disk("disk_vaultis_trench_100m",
            new DiskSpec(StorageTier.HDD, HardwareEra.VINTAGE, 100L, 6)).named("Vaultis Trench HDD 100M").register();
    public static final DeferredItem<DiskItem> DISK_TRENCH_200M = disk("disk_vaultis_trench_200m",
            new DiskSpec(StorageTier.HDD, HardwareEra.VINTAGE, 200L, 7)).named("Vaultis Trench HDD 200M").register();

    //  LEGACY: AGP/PCIe 1.0 buses, SDRAM/DDR/DDR2 RAM, the last single-core CPUs

    // Socket 370: the Pentix III, from Coppermine to Tualatin.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_700 = cpu("cpu_integra_pentix_700",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 700, 28, false)
                    .on(Microarchitectures.P6, "Coppermine"))
            .named("Integra Pentix 700").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_III_S_1000 = cpu("cpu_integra_pentix_iii_s_1000",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 1000, 30, false)
                    .on(Microarchitectures.P6, "Tualatin"))
            .named("Integra Pentix III-S 1000").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_III_S_1400 = cpu("cpu_integra_pentix_iii_s_1400",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 1400, 32, false)
                    .on(Microarchitectures.P6, "Tualatin"))
            .named("Integra Pentix III-S 1400").register();

    // Socket A: the Duro, and the Sprint XP up to the last Barton.
    public static final DeferredItem<CpuItem> CPU_VELOCION_DURO_1300 = cpu("cpu_velocion_duro_1300",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 1300, 57, false)
                    .on(Microarchitectures.K7, "Applebred"))
            .named("Velocion Duro 1300").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_XP_2400 = cpu("cpu_velocion_sprint_xp_2400",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 2000, 65, false)
                    .on(Microarchitectures.K7, "Thoroughbred"))
            .named("Velocion Sprint XP 2400+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_XP_2800 = cpu("cpu_velocion_sprint_xp_2800",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 2083, 68, false)
                    .on(Microarchitectures.K7, "Barton"))
            .named("Velocion Sprint XP 2800+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_XP_3200 = cpu("cpu_velocion_sprint_xp_3200",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 2200, 76, false)
                    .on(Microarchitectures.K7, "Barton"))
            .named("Velocion Sprint XP 3200+").register();

    // Socket 478: the Pentix 4 of 2002 and 2003, the first chips with two threads a core.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CELER_2_0 = cpu("cpu_integra_celer_2_0",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_478, 1, 2000, 52, false)
                    .on(Microarchitectures.NETBURST, "Northwood"))
            .named("Integra Celer 2.0").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_4_2_4C = cpu("cpu_integra_pentix_4_2_4c",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_478, 1, 2400, 66, false)
                    .on(Microarchitectures.NETBURST, "Northwood").withSmt())
            .named("Integra Pentix 4 2.4C").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_4_3_2C = cpu("cpu_integra_pentix_4_3_2c",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_478, 1, 3200, 82, false)
                    .on(Microarchitectures.NETBURST, "Northwood").withSmt())
            .named("Integra Pentix 4 3.2C").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_4_EE_3_4 = cpu("cpu_integra_pentix_4_ee_3_4",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_478, 1, 3400, 110, false)
                    .on(Microarchitectures.NETBURST, "Gallatin").withSmt())
            .named("Integra Pentix 4 EE 3.4").register();

    // LGA 775: the Prescott Pentix 4 of 2004.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CELER_D_325J = cpu("cpu_integra_celer_d_325j",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 1, 2530, 84, false)
                    .on(Microarchitectures.NETBURST, "Prescott"))
            .named("Integra Celer D 325J").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_4_520 = cpu("cpu_integra_pentix_4_520",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 1, 2800, 84, false)
                    .on(Microarchitectures.NETBURST, "Prescott").withSmt())
            .named("Integra Pentix 4 520").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_4_540 = cpu("cpu_integra_pentix_4_540",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 1, 3200, 84, false)
                    .on(Microarchitectures.NETBURST, "Prescott").withSmt())
            .named("Integra Pentix 4 540").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_4_560 = cpu("cpu_integra_pentix_4_560",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 1, 3600, 115, false)
                    .on(Microarchitectures.NETBURST, "Prescott").withSmt())
            .named("Integra Pentix 4 560").register();

    // Socket 754: the first 64-bit chips, still on AGP boards.
    public static final DeferredItem<CpuItem> CPU_VELOCION_SEMPER_3100 = cpu("cpu_velocion_semper_3100",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_754, 1, 1800, 62, false)
                    .on(Microarchitectures.K8, "Paris"))
            .named("Velocion Semper 3100+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_64_3200 = cpu("cpu_velocion_sprint_64_3200",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_754, 1, 2000, 89, false)
                    .on(Microarchitectures.K8, "ClawHammer"))
            .named("Velocion Sprint 64 3200+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_64_3700 = cpu("cpu_velocion_sprint_64_3700",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_754, 1, 2400, 89, false)
                    .on(Microarchitectures.K8, "ClawHammer"))
            .named("Velocion Sprint 64 3700+").register();

    // Socket 939: two memory channels and PCIe, up to the FX-55, the fastest chip of the era.
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_64_3500 = cpu("cpu_velocion_sprint_64_3500",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_939, 1, 2200, 89, false)
                    .on(Microarchitectures.K8, "Newcastle"))
            .named("Velocion Sprint 64 3500+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_64_4000 = cpu("cpu_velocion_sprint_64_4000",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_939, 1, 2400, 89, false)
                    .on(Microarchitectures.K8, "ClawHammer"))
            .named("Velocion Sprint 64 4000+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_64_FX_55 = cpu("cpu_velocion_sprint_64_fx_55",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_939, 1, 2600, 104, false)
                    .on(Microarchitectures.K8, "ClawHammer"))
            .named("Velocion Sprint 64 FX-55").register();

    // Socket 604: the server NetBurst, two to a board.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_2800 = cpu("cpu_integra_servo_2800",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_604, 1, 2800, 74, false)
                    .on(Microarchitectures.NETBURST, "Prestonia").withSmt())
            .named("Integra Servo 2800").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_3060 = cpu("cpu_integra_servo_3060",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_604, 1, 3060, 87, false)
                    .on(Microarchitectures.NETBURST, "Prestonia").withSmt())
            .named("Integra Servo 3060").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_3200 = cpu("cpu_integra_servo_3200",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_604, 1, 3200, 92, false)
                    .on(Microarchitectures.NETBURST, "Gallatin").withSmt())
            .named("Integra Servo 3200").register();

    // Socket 940: the Optera, two to a server board and four to the Mainframe's.
    public static final DeferredItem<CpuItem> CPU_VELOCION_OPTERA_244 = cpu("cpu_velocion_optera_244",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 1, 1800, 89, false)
                    .on(Microarchitectures.K8, "SledgeHammer"))
            .named("Velocion Optera 244").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_OPTERA_248 = cpu("cpu_velocion_optera_248",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 1, 2200, 89, false)
                    .on(Microarchitectures.K8, "SledgeHammer"))
            .named("Velocion Optera 248").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_OPTERA_250 = cpu("cpu_velocion_optera_250",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 1, 2400, 89, false)
                    .on(Microarchitectures.K8, "SledgeHammer"))
            .named("Velocion Optera 250").register();

    /*
     * Legacy memory: SDRAM for the Socket 370 and Socket A boards, DDR for every board from Socket A on, and the DDR2
     * the LGA 775 board also takes.
     */
    public static final DeferredItem<RamItem> RAM_SDRAM_128 = ram("ram_sdram_128",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.SDRAM, 32, 5)).named("Stratix Layer SDRAM-128").register();
    public static final DeferredItem<RamItem> RAM_SDRAM_256 = ram("ram_sdram_256",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.SDRAM, 64, 6)).named("Stratix Layer SDRAM-256").register();
    public static final DeferredItem<RamItem> RAM_DDR_256 = ram("ram_ddr_256",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR, 64, 7)).named("Stratix Layer DDR-256").register();
    public static final DeferredItem<RamItem> RAM_DDR_512 = ram("ram_ddr_512",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR, 128, 10)).named("Stratix Layer DDR-512").register();
    public static final DeferredItem<RamItem> RAM_DDR_1024 = ram("ram_ddr_1024",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR, 256, 12)).named("Stratix Layer DDR-1024").register();
    public static final DeferredItem<RamItem> RAM_DDR2_512 = ram("ram_ddr2_512",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR2, 128, 9)).named("Stratix Layer DDR2-512").register();

    /*
     * Legacy GPU ladder, AGP to PCIe 1.0. The TNT2 M64 and the Radiance 7000 are the floor; each maker climbs through
     * its AGP cards to the Vertex 6800 Ultra and the Radiance 9800 XT, and the first PCIe cards run from the Vertex
     * 6200 and the Radiance X300 to the X800 XT, the top of the era. Pipelines are the cores these cards count.
     */
    public static final DeferredItem<GpuItem> GPU_PRISM_TNT2_M64 = gpu("gpu_prism_tnt2_m64",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 2, 32, 10)
                    .on(Microarchitectures.FAHRENHEIT, "NV6", 125))
            .named("Envya Prism TNT2 M64").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_256 = gpu("gpu_vertex_256",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 4, 32, 50)
                    .on(Microarchitectures.CELSIUS, "NV10", 120))
            .named("Envya Vertex 256").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_7000 = gpu("gpu_radiance_7000",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 1, 32, 8)
                    .on(Microarchitectures.R100, "RV100", 183))
            .named("Atrion Radiance 7000").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_2_MX_400 = gpu("gpu_vertex_2_mx_400",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 2, 64, 10)
                    .on(Microarchitectures.CELSIUS, "NV11", 200))
            .named("Envya Vertex 2 MX 400").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_8500 = gpu("gpu_radiance_8500",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 4, 64, 25)
                    .on(Microarchitectures.R200, "R200", 275))
            .named("Atrion Radiance 8500").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_4_TI_4200 = gpu("gpu_vertex_4_ti_4200",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 4, 128, 30)
                    .on(Microarchitectures.KELVIN, "NV25", 250))
            .named("Envya Vertex 4 Ti 4200").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_9200_SE = gpu("gpu_radiance_9200_se",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 4, 128, 30)
                    .on(Microarchitectures.R200, "RV280", 200))
            .named("Atrion Radiance 9200 SE").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_FX_5200 = gpu("gpu_vertex_fx_5200",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 4, 128, 20)
                    .on(Microarchitectures.RANKINE, "NV34", 250))
            .named("Envya Vertex FX 5200").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_9600_XT = gpu("gpu_radiance_9600_xt",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 4, 128, 30)
                    .on(Microarchitectures.R300, "RV360", 500))
            .named("Atrion Radiance 9600 XT").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_9800_PRO = gpu("gpu_radiance_9800_pro",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 8, 128, 70)
                    .on(Microarchitectures.R300, "R350", 380))
            .named("Atrion Radiance 9800 Pro").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_9800_XT = gpu("gpu_radiance_9800_xt",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 8, 256, 75)
                    .on(Microarchitectures.R300, "R360", 412))
            .named("Atrion Radiance 9800 XT").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_6800_ULTRA = gpu("gpu_vertex_6800_ultra",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 16, 256, 81)
                    .on(Microarchitectures.CURIE, "NV40", 400))
            .named("Envya Vertex 6800 Ultra").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_6200 = gpu("gpu_vertex_6200",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0, 4, 128, 25)
                    .on(Microarchitectures.CURIE, "NV44", 300))
            .named("Envya Vertex 6200").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_X300 = gpu("gpu_radiance_x300",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0, 4, 128, 20)
                    .on(Microarchitectures.R300, "RV370", 325))
            .named("Atrion Radiance X300").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_6600_GT = gpu("gpu_vertex_6600_gt",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0, 8, 128, 48)
                    .on(Microarchitectures.CURIE, "NV43", 500))
            .named("Envya Vertex 6600 GT").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_X800_XT = gpu("gpu_radiance_x800_xt",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0, 16, 256, 70)
                    .on(Microarchitectures.R400, "R423", 500))
            .named("Atrion Radiance X800 XT").register();

    public static final DeferredItem<PsuItem> PSU_350 =
            psu("psu_350", new PsuSpec(350, 80)).named("MF PowerBasic 350").register();
    public static final DeferredItem<PsuItem> PSU_500B =
            psu("psu_500b", new PsuSpec(500, 80)).named("MF PowerBasic 500B").register();

    /*
     * The Legacy ATX boards, one per socket, since a board spec carries a single socket and a single bus: each has
     * the memory and the graphics bus its chipset had. The Socket 370, Socket A, Socket 478 and Socket 754 boards
     * pre-date PCIe and seat their graphics card on AGP.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_S370 =
            board("motherboard_atx_legacy_s370", new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_370, 1, Set.of(RamGeneration.SDRAM), 4, PcieGeneration.AGP_4X, 4, 4, 4))
                    .named("MF ATX Legacy Motherboard (Socket 370)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_SKA = board("motherboard_atx_legacy_ska",
            new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_A, 1, Set.of(RamGeneration.SDRAM, RamGeneration.DDR), 4,
                    PcieGeneration.AGP_8X, 4, 4, 4))
            .named("MF ATX Legacy Motherboard (Socket A)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_478 =
            board("motherboard_atx_legacy_478", new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_478, 1, Set.of(RamGeneration.DDR), 4, PcieGeneration.AGP_8X, 4, 4, 4))
                    .named("MF ATX Legacy Motherboard (Socket 478)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_754 =
            board("motherboard_atx_legacy_754", new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_754, 1, Set.of(RamGeneration.DDR), 4, PcieGeneration.AGP_8X, 4, 4, 4))
                    .named("MF ATX Legacy Motherboard (Socket 754)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_LGA775 =
            board("motherboard_atx_legacy_lga775", new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.LGA_775, 1, Set.of(RamGeneration.DDR, RamGeneration.DDR2), 4,
                    PcieGeneration.PCIE_1_0, 4, 4, 4))
                    .named("MF ATX Legacy Motherboard (LGA 775)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_939 =
            board("motherboard_atx_legacy_939", new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_939, 1, Set.of(RamGeneration.DDR), 4, PcieGeneration.PCIE_1_0, 4, 4, 4))
                    .named("MF ATX Legacy Motherboard (Socket 939)").register();
    // The server boards: two Servos on Socket 604, two Opteras on Socket 940, four on the Mainframe's, all on DDR.
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EATX_LEGACY_604 =
            board("motherboard_eatx_legacy_604", new MotherboardSpec(FormFactor.EATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_604, 2, Set.of(RamGeneration.DDR), 8, PcieGeneration.PCIE_1_0, 6, 6, 4))
                    .named("MF EATX Legacy Motherboard (2x Socket 604)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EATX_LEGACY_S940 =
            board("motherboard_eatx_legacy_s940", new MotherboardSpec(FormFactor.EATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_940, 2, Set.of(RamGeneration.DDR), 8,
                    PcieGeneration.PCIE_1_0, 6, 6, 4))
                    .named("MF EATX Legacy Motherboard (2x Socket 940)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_MTX_LEGACY = board("motherboard_mtx_legacy",
            new MotherboardSpec(FormFactor.MTX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_940, 4, Set.of(RamGeneration.DDR), 24,
                    PcieGeneration.PCIE_1_0, 8, 6, 8))
            .named("MF MTX-L Motherboard").register();

    /*
     * Legacy rotating disks: IDE HDDs of 4, 20 and 40 GB. At 32 bits an item costs 16 MB, so they hold 256, 1 280 and
     * 2 560 items, between the Vintage platters and the Transition's 500 GB floor.
     */
    public static final DeferredItem<DiskItem> DISK_LINK_IDE_4G = disk("disk_vaultis_link_ide_4g",
            new DiskSpec(StorageTier.HDD, HardwareEra.LEGACY, 256L, 7)).named("Vaultis Link IDE-HDD 4G").register();
    public static final DeferredItem<DiskItem> DISK_LINK_IDE_20G = disk("disk_vaultis_link_ide_20g",
            new DiskSpec(StorageTier.HDD, HardwareEra.LEGACY, 1280L, 8)).named("Vaultis Link IDE-HDD 20G").register();
    public static final DeferredItem<DiskItem> DISK_LINK_IDE_40G = disk("disk_vaultis_link_ide_40g",
            new DiskSpec(StorageTier.HDD, HardwareEra.LEGACY, 2560L, 9)).named("Vaultis Link IDE-HDD 40G").register();

    //  TRANSITION: PCIe 1.0 and 2.0, DDR2 and DDR3, the first multi-core CPUs

    // LGA 775: the Centro 2 line, from the Celer E1200 to the Extreme QX9650, and the last NetBurst, the Pentix D.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_D_805 = cpu("cpu_integra_pentix_d_805",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 2, 2660, 95, false)
                    .on(Microarchitectures.NETBURST, "Smithfield"))
            .named("Integra Pentix D 805").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CELER_E1200 = cpu("cpu_integra_celer_e1200",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 2, 1600, 65, false)
                    .on(Microarchitectures.CENTRO, "Allendale"))
            .named("Integra Celer E1200").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_2_DUO_E4300 = cpu("cpu_integra_centro_2_duo_e4300",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 2, 1800, 65, false)
                    .on(Microarchitectures.CENTRO, "Allendale"))
            .named("Integra Centro 2 Duo E4300").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_2_DUO_E6600 = cpu("cpu_integra_centro_2_duo_e6600",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 2, 2400, 65, false)
                    .on(Microarchitectures.CENTRO, "Conroe"))
            .named("Integra Centro 2 Duo E6600").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_2_DUO_E8500 = cpu("cpu_integra_centro_2_duo_e8500",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 2, 3160, 65, false)
                    .on(Microarchitectures.CENTRO, "Wolfdale"))
            .named("Integra Centro 2 Duo E8500").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_2_QUAD_Q6600 =
            cpu("cpu_integra_centro_2_quad_q6600", new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 4, 2400,
                    105, false).on(Microarchitectures.CENTRO, "Kentsfield"))
                    .named("Integra Centro 2 Quad Q6600").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_2_EXTREME_QX9650 =
            cpu("cpu_integra_centro_2_extreme_qx9650", new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_775, 4,
                    3000, 130, false).on(Microarchitectures.CENTRO, "Yorkfield"))
                    .named("Integra Centro 2 Extreme QX9650").register();

    // AM2 and AM2+: the dual-core Sprint 64 X2 and the first Ascent quad.
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_64_X2_3800 = cpu("cpu_velocion_sprint_64_x2_3800",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM2, 2, 2000, 89, false)
                    .on(Microarchitectures.K8, "Windsor"))
            .named("Velocion Sprint 64 X2 3800+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_64_X2_6000 = cpu("cpu_velocion_sprint_64_x2_6000",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM2, 2, 3000, 125, false)
                    .on(Microarchitectures.K8, "Windsor"))
            .named("Velocion Sprint 64 X2 6000+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_ASCENT_X4_9850 = cpu("cpu_velocion_ascent_x4_9850",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM2, 4, 2500, 125, false)
                    .on(Microarchitectures.K10, "Agena"))
            .named("Velocion Ascent X4 9850").register();

    // AM3: the Sprint II and the Ascent II line, up to the six-core X6 1100T.
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_II_X2_250 = cpu("cpu_velocion_sprint_ii_x2_250",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM3, 2, 3000, 65, false)
                    .on(Microarchitectures.K10, "Regor"))
            .named("Velocion Sprint II X2 250").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_II_X4_630 = cpu("cpu_velocion_sprint_ii_x4_630",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM3, 4, 2800, 95, false)
                    .on(Microarchitectures.K10, "Propus"))
            .named("Velocion Sprint II X4 630").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_ASCENT_X4_955 = cpu("cpu_velocion_ascent_x4_955",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM3, 4, 3200, 125, false)
                    .on(Microarchitectures.K10, "Deneb"))
            .named("Velocion Ascent X4 955").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_ASCENT_X4_965 = cpu("cpu_velocion_ascent_x4_965",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM3, 4, 3400, 125, false)
                    .on(Microarchitectures.K10, "Deneb"))
            .named("Velocion Ascent X4 965").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_ASCENT_X6_1090T = cpu("cpu_velocion_ascent_x6_1090t",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM3, 6, 3200, 125, false)
                    .on(Microarchitectures.K10, "Thuban"))
            .named("Velocion Ascent X6 1090T").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_ASCENT_X6_1100T = cpu("cpu_velocion_ascent_x6_1100t",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.AM3, 6, 3300, 125, false)
                    .on(Microarchitectures.K10, "Thuban"))
            .named("Velocion Ascent X6 1100T").register();

    // LGA 1156: the first Centro c3, c5 and c7.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_PENTIX_G6950 = cpu("cpu_integra_pentix_g6950",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1156, 2, 2800, 73, false)
                    .on(Microarchitectures.WESTMERE, "Clarkdale"))
            .named("Integra Pentix G6950").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_C3_530 = cpu("cpu_integra_centro_c3_530",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1156, 2, 2930, 73, false)
                    .on(Microarchitectures.WESTMERE, "Clarkdale").withSmt())
            .named("Integra Centro c3 530").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_C5_750 = cpu("cpu_integra_centro_c5_750",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1156, 4, 2660, 95, false)
                    .on(Microarchitectures.NEHALEM, "Lynnfield"))
            .named("Integra Centro c5 750").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_C7_860 = cpu("cpu_integra_centro_c7_860",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1156, 4, 2800, 95, false)
                    .on(Microarchitectures.NEHALEM, "Lynnfield").withSmt())
            .named("Integra Centro c7 860").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_C7_880 = cpu("cpu_integra_centro_c7_880",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1156, 4, 3060, 95, false)
                    .on(Microarchitectures.NEHALEM, "Lynnfield").withSmt())
            .named("Integra Centro c7 880").register();

    // LGA 1366: the workstation Centro c7, up to the six-core 980X, and the Servo of the two-way server boards.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_C7_920 = cpu("cpu_integra_centro_c7_920",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 4, 2660, 130, false)
                    .on(Microarchitectures.NEHALEM, "Bloomfield").withSmt())
            .named("Integra Centro c7 920").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_C7_960 = cpu("cpu_integra_centro_c7_960",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 4, 3200, 130, false)
                    .on(Microarchitectures.NEHALEM, "Bloomfield").withSmt())
            .named("Integra Centro c7 960").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_CENTRO_C7_980X = cpu("cpu_integra_centro_c7_980x",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 6, 3330, 130, false)
                    .on(Microarchitectures.WESTMERE, "Gulftown").withSmt())
            .named("Integra Centro c7 980X").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5520 = cpu("cpu_integra_servo_5520",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 4, 2260, 80, false)
                    .on(Microarchitectures.NEHALEM, "Gainestown").withSmt())
            .named("Integra Servo 5520").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5570 = cpu("cpu_integra_servo_5570",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 4, 2930, 95, false)
                    .on(Microarchitectures.NEHALEM, "Gainestown").withSmt())
            .named("Integra Servo 5570").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5680 = cpu("cpu_integra_servo_5680",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_1366, 6, 3330, 130, false)
                    .on(Microarchitectures.WESTMERE, "Westmere-EP").withSmt())
            .named("Integra Servo 5680").register();

    // LGA 771: the Servo of the first Centro servers, two to a board.
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5100 = cpu("cpu_integra_servo_5100",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_771, 2, 2000, 65, false)
                    .on(Microarchitectures.CENTRO, "Woodcrest"))
            .named("Integra Servo 5100").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5160 = cpu("cpu_integra_servo_5160",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_771, 2, 3000, 80, false)
                    .on(Microarchitectures.CENTRO, "Woodcrest"))
            .named("Integra Servo 5160").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5335 = cpu("cpu_integra_servo_5335",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_771, 4, 2000, 80, false)
                    .on(Microarchitectures.CENTRO, "Clovertown"))
            .named("Integra Servo 5335").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5450 = cpu("cpu_integra_servo_5450",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.LGA_771, 4, 3000, 80, false)
                    .on(Microarchitectures.CENTRO, "Harpertown"))
            .named("Integra Servo 5450").register();

    // Socket F: the Optera, two to a server board and four to the Mainframe's.
    public static final DeferredItem<CpuItem> CPU_VELOCION_OPTERA_2218 = cpu("cpu_velocion_optera_2218",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.SOCKET_F, 2, 2600, 95, false)
                    .on(Microarchitectures.K8, "Santa Rosa"))
            .named("Velocion Optera 2218").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_OPTERA_8356 = cpu("cpu_velocion_optera_8356",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.SOCKET_F, 4, 2300, 95, false)
                    .on(Microarchitectures.K10, "Barcelona"))
            .named("Velocion Optera 8356").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_OPTERA_8384 = cpu("cpu_velocion_optera_8384",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.SOCKET_F, 4, 2700, 75, false)
                    .on(Microarchitectures.K10, "Shanghai"))
            .named("Velocion Optera 8384").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_OPTERA_8435 = cpu("cpu_velocion_optera_8435",
            new CpuSpec(HardwareEra.TRANSITION, CpuSocketId.SOCKET_F, 6, 2600, 75, false)
                    .on(Microarchitectures.K10, "Istanbul"))
            .named("Velocion Optera 8435").register();

    /*
     * Transition memory: DDR2 for the LGA 775, AM2, LGA 771 and Socket F boards, with the registered module of the
     * servers, and DDR3 for the LGA 775 board after it, the AM3, LGA 1156 and LGA 1366 boards.
     */
    public static final DeferredItem<RamItem> RAM_DDR2_1024 = ram("ram_ddr2_1024",
            new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR2, 256, 10))
            .named("Stratix Layer DDR2-1024").register();
    public static final DeferredItem<RamItem> RAM_DDR2_2048 = ram("ram_ddr2_2048",
            new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR2, 512, 12))
            .named("Stratix Layer DDR2-2048").register();
    public static final DeferredItem<RamItem> RAM_DDR2_4096_RDIMM = ram("ram_ddr2_4096_rdimm",
            new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR2, 1024, 15))
            .named("Stratix Layer DDR2-4096 RDIMM").register();
    public static final DeferredItem<RamItem> RAM_DDR3_1024 = ram("ram_ddr3_1024",
            new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR3, 256, 8))
            .named("Stratix Layer DDR3-1024").register();
    public static final DeferredItem<RamItem> RAM_DDR3_2048 = ram("ram_ddr3_2048",
            new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR3, 512, 10))
            .named("Stratix Layer DDR3-2048").register();
    public static final DeferredItem<RamItem> RAM_DDR3_4096 = ram("ram_ddr3_4096",
            new RamSpec(HardwareEra.TRANSITION, RamGeneration.DDR3, 1024, 12))
            .named("Stratix Layer DDR3-4096").register();

    /*
     * Transition GPU ladder, PCIe 1.0 to 2.0. The Vertex 7300 GS and the Radiance X1300 are the floor; the first
     * unified-shader cards take over from the 8600 GT and the HD 3850, and the Vertex GTX 480 and the Radiance HD
     * 5870 top the era. Shaders are the cores from the 8600 GT on, and the Radiance ones count in groups of five.
     */
    public static final DeferredItem<GpuItem> GPU_VERTEX_7300_GS = gpu("gpu_vertex_7300_gs",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 4, 128, 19)
                    .on(Microarchitectures.CURIE, "G72", 550))
            .named("Envya Vertex 7300 GS").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_X1300 = gpu("gpu_radiance_x1300",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 4, 128, 15)
                    .on(Microarchitectures.R500, "RV515", 450))
            .named("Atrion Radiance X1300").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_7600_GT = gpu("gpu_vertex_7600_gt",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 12, 256, 36)
                    .on(Microarchitectures.CURIE, "G73", 560))
            .named("Envya Vertex 7600 GT").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_7800_GTX = gpu("gpu_vertex_7800_gtx",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 24, 256, 86)
                    .on(Microarchitectures.CURIE, "G70", 430))
            .named("Envya Vertex 7800 GTX").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_X1950_XTX = gpu("gpu_radiance_x1950_xtx",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 16, 512, 125)
                    .on(Microarchitectures.R500, "R580", 650))
            .named("Atrion Radiance X1950 XTX").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_8600_GT = gpu("gpu_vertex_8600_gt",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_1_0, 32, 256, 47)
                    .on(Microarchitectures.TESLA, "G84", 1190))
            .named("Envya Vertex 8600 GT").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_8800_GT = gpu("gpu_vertex_8800_gt",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 112, 512, 110)
                    .on(Microarchitectures.TESLA, "G92", 1500))
            .named("Envya Vertex 8800 GT").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_3850 = gpu("gpu_radiance_hd_3850",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 320, 256, 95)
                    .on(Microarchitectures.TERASCALE, "RV670", 670))
            .named("Atrion Radiance HD 3850").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_9600_GT = gpu("gpu_vertex_9600_gt",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 64, 512, 95)
                    .on(Microarchitectures.TESLA, "G94", 1625))
            .named("Envya Vertex 9600 GT").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_4670 = gpu("gpu_radiance_hd_4670",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 320, 512, 59)
                    .on(Microarchitectures.TERASCALE, "RV730", 750))
            .named("Atrion Radiance HD 4670").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_GTX_280 = gpu("gpu_vertex_gtx_280",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 240, 1024, 236)
                    .on(Microarchitectures.TESLA, "GT200", 1296))
            .named("Envya Vertex GTX 280").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_4870 = gpu("gpu_radiance_hd_4870",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 800, 512, 150)
                    .on(Microarchitectures.TERASCALE, "RV770", 750))
            .named("Atrion Radiance HD 4870").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_5770 = gpu("gpu_radiance_hd_5770",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 800, 1024, 108)
                    .on(Microarchitectures.TERASCALE_2, "Juniper", 850))
            .named("Atrion Radiance HD 5770").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_GT_240 = gpu("gpu_vertex_gt_240",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 96, 512, 69)
                    .on(Microarchitectures.TESLA, "GT215", 1340))
            .named("Envya Vertex GT 240").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_6850 = gpu("gpu_radiance_hd_6850",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 960, 1024, 127)
                    .on(Microarchitectures.TERASCALE_2, "Barts", 775))
            .named("Velocion Radiance HD 6850").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_GTX_480 = gpu("gpu_vertex_gtx_480",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 480, 1536, 250)
                    .on(Microarchitectures.FERMI, "GF100", 1401))
            .named("Envya Vertex GTX 480").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_5870 = gpu("gpu_radiance_hd_5870",
            new GpuSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 1600, 1024, 188)
                    .on(Microarchitectures.TERASCALE_2, "Cypress", 850))
            .named("Atrion Radiance HD 5870").register();

    public static final DeferredItem<PsuItem> PSU_450B =
            psu("psu_450b", new PsuSpec(450, 85)).named("MF PowerBasic 450B").register();

    /*
     * The Transition ATX boards, one per socket, all on PCIe 2.0: the LGA 775 board of the Centro 2 with DDR2 and
     * DDR3, the AM2 board with DDR2, and the AM3 and LGA 1156 boards with DDR3.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_TRANSITION_775 =
            board("motherboard_atx_transition_775", new MotherboardSpec(FormFactor.ATX, HardwareEra.TRANSITION,
                    CpuSocketId.LGA_775, 1, Set.of(RamGeneration.DDR2, RamGeneration.DDR3), 4, PcieGeneration.PCIE_2_0,
                    4, 4, 4))
                    .named("MF ATX Transition Motherboard (LGA 775)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_TRANSITION_AM2 =
            board("motherboard_atx_transition_am2", new MotherboardSpec(FormFactor.ATX, HardwareEra.TRANSITION,
                    CpuSocketId.AM2, 1, Set.of(RamGeneration.DDR2), 4, PcieGeneration.PCIE_2_0, 4, 4, 4))
                    .named("MF ATX Transition Motherboard (AM2)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_TRANSITION_AM3 =
            board("motherboard_atx_transition_am3", new MotherboardSpec(FormFactor.ATX, HardwareEra.TRANSITION,
                    CpuSocketId.AM3, 1, Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_2_0, 4, 2, 4))
                    .named("MF ATX Transition Motherboard (AM3)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_TRANSITION_1156 =
            board("motherboard_atx_transition_1156", new MotherboardSpec(FormFactor.ATX, HardwareEra.TRANSITION,
                    CpuSocketId.LGA_1156, 1, Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_2_0, 4, 4, 4))
                    .named("MF ATX Transition Motherboard (LGA 1156)").register();
    // The workstation board of the LGA 1366 Centro c7.
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EATX_TRANSITION_1366 =
            board("motherboard_eatx_transition_1366", new MotherboardSpec(FormFactor.EATX, HardwareEra.TRANSITION,
                    CpuSocketId.LGA_1366, 1, Set.of(RamGeneration.DDR3), 6, PcieGeneration.PCIE_2_0, 7, 6, 4))
                    .named("MF EATX Transition Motherboard (LGA 1366)").register();
    /*
     * The server boards: two Servos on LGA 771 or LGA 1366, two Opteras on Socket F, four on the Mainframe's. The
     * LGA 771 and Socket F boards are of 2006 and 2007, still on DDR2 and PCIe 1.0.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_TRANSITION_771 =
            board("motherboard_transition_771", new MotherboardSpec(FormFactor.EATX, HardwareEra.TRANSITION,
                    CpuSocketId.LGA_771, 2, Set.of(RamGeneration.DDR2), 8, PcieGeneration.PCIE_1_0, 6, 6, 4))
                    .named("MF EATX Transition Motherboard (2x LGA 771)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EEB_T_1366 =
            board("motherboard_eeb_t_1366", new MotherboardSpec(FormFactor.EEB, HardwareEra.TRANSITION,
                    CpuSocketId.LGA_1366, 2, Set.of(RamGeneration.DDR3), 12, PcieGeneration.PCIE_2_0, 6, 6, 6))
                    .named("MF EEB-T Server Board (2x LGA 1366)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EEB_T_F =
            board("motherboard_eeb_t_f", new MotherboardSpec(FormFactor.EEB, HardwareEra.TRANSITION,
                    CpuSocketId.SOCKET_F, 2, Set.of(RamGeneration.DDR2), 16, PcieGeneration.PCIE_1_0, 6, 6, 6))
                    .named("MF EEB-T Server Board (2x Socket F)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_MTX_T =
            board("motherboard_mtx_t", new MotherboardSpec(FormFactor.MTX, HardwareEra.TRANSITION,
                    CpuSocketId.SOCKET_F, 4, Set.of(RamGeneration.DDR2), 32, PcieGeneration.PCIE_1_0, 8, 6, 8))
                    .named("MF MTX-T Motherboard (4x Socket F)").register();

    /*
     * The first affordable SATA SSD. At the Transition's 64 bits an item costs 256 MB, so its 64 GB hold 256 items,
     * sixteen times fewer than it held as a Legacy part; the Transition's hard disks start at the 500 GB of the disk
     * grid.
     */
    public static final DeferredItem<DiskItem> DISK_LINK_SATA_SSD_64G = disk("disk_vaultis_link_sata_ssd_64g",
            new DiskSpec(StorageTier.SSD, HardwareEra.TRANSITION, 256L, 3)).named("Vaultis Link SATA-SSD 64G")
            .register();

    /*
     *  STANDARD: completion of the partially-registered set (PCIe 2.0/3.0, DDR3)
     *  The Servo 2620/2690/2699, the DDR3-8192, the HD 7970 GPU, the MTX-P and EEB-P boards and the 650G PSU
     *  live in ComputingModule. These fill the gaps.
     */

    public static final DeferredItem<CpuItem> CPU_APEX_5_4590 = cpu("cpu_apex_5_4590",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 3300, 84, false)
                    .on(Microarchitectures.HASWELL, ""))
            .named("Integra Apex 5 4590").register();
    public static final DeferredItem<CpuItem> CPU_APEX_5_4690K = cpu("cpu_apex_5_4690k",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 3500, 88, false)
                    .on(Microarchitectures.HASWELL, "Devil's Canyon"))
            .named("Integra Apex 5 4690K").register();
    public static final DeferredItem<CpuItem> CPU_APEX_7_4790K = cpu("cpu_apex_7_4790k",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 4000, 88, false)
                    .on(Microarchitectures.HASWELL, "Devil's Canyon").withSmt())
            .named("Integra Apex 7 4790K").register();

    /*
     * Standard GPU ladder (PCIe 2.0 entry to PCIe 3.0 high-end). The HD 7970 (in ComputingModule) is the
     * upper-mid card; the GTX 550 Ti is the PCIe 2.0 floor, and the GTX 780 Ti tops the era on PCIe 3.0 above the
     * 7970. Hundreds-to-thousands of cores and 1-3 GB of VRAM fit this generation.
     */
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_7970 = ComputingModule.GPU_HD_7970;
    public static final DeferredItem<GpuItem> GPU_VERTEX_GTX_550_TI = gpu("gpu_vertex_gtx_550_ti",
            new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_2_0, 192, 1024, 116)
                    .on(Microarchitectures.FERMI, "GF116", 1800))
            .named("Visara Vertex GTX 550 Ti").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_GTX_780_TI = gpu("gpu_vertex_gtx_780_ti",
            new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2880, 3072, 250)
                    .on(Microarchitectures.KEPLER, "GK110", 875))
            .named("Visara Vertex GTX 780 Ti").register();

    public static final DeferredItem<PsuItem> PSU_850G =
            psu("psu_850g", new PsuSpec(850, 90)).named("MF PowerGold 850G").register();

    // The Standard ATX board of the Apex line, on LGA 1150, and the workstation board.
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_STANDARD_LGA1150 =
            board("motherboard_atx_standard_lga1150", new MotherboardSpec(FormFactor.ATX, HardwareEra.STANDARD,
                    CpuSocketId.LGA_1150, 1, Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_3_0, 4, 2, 4))
                    .named("MF ATX Standard Motherboard (LGA 1150)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EATX_STANDARD_WS =
            board("motherboard_eatx_standard_ws", new MotherboardSpec(FormFactor.EATX, HardwareEra.STANDARD,
                    CpuSocketId.LGA_2011, 1, Set.of(RamGeneration.DDR3), 8, PcieGeneration.PCIE_3_0, 7, 4, 4))
                    .named("MF EATX Standard Workstation Board").register();

    /*
     * The sound cards: one for each bus the boards of their era have. The ones of an era sound the same and differ
     * in the slot they take and how they look. The Vintage ones are really lo-fi, eight bits in one channel at 22
     * kHz, making their notes by FM; the Legacy ones play recordings at CD quality from a bank of instruments. The
     * boards have their sound built in from the Transition on, where the Hi-Fi is the one card, and there is no
     * Standard card.
     */
    public static final DeferredItem<SoundCardItem> SOUND_CARD_TONE_BLASTER = soundCard("sound_card_tone_blaster",
            new SoundCardSpec(HardwareEra.VINTAGE, PcieGeneration.ISA, 5, SoundCardSpec.Synthesis.FM, 9, 8, false,
                    SoundCardSpec.SampleRate.KHZ_22)).named("Artisan Tone Blaster").register();
    public static final DeferredItem<SoundCardItem> SOUND_CARD_TONE_BLASTER_128 =
            soundCard("sound_card_tone_blaster_128", new SoundCardSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 5,
                    SoundCardSpec.Synthesis.FM, 9, 8, false, SoundCardSpec.SampleRate.KHZ_22))
                    .named("Artisan Tone Blaster 128").register();
    public static final DeferredItem<SoundCardItem> SOUND_CARD_TONE_BLASTER_LIVE =
            soundCard("sound_card_tone_blaster_live", new SoundCardSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 8,
                    SoundCardSpec.Synthesis.WAVETABLE, 32, 16, true, SoundCardSpec.SampleRate.KHZ_44))
                    .named("Artisan Tone Blaster Live").register();
    public static final DeferredItem<SoundCardItem> SOUND_CARD_TONE_BLASTER_AUDIGY =
            soundCard("sound_card_tone_blaster_audigy", new SoundCardSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0,
                    8, SoundCardSpec.Synthesis.WAVETABLE, 32, 16, true, SoundCardSpec.SampleRate.KHZ_44))
                    .named("Artisan Tone Blaster Audigy").register();
    public static final DeferredItem<SoundCardItem> SOUND_CARD_TONE_BLASTER_HI_FI =
            soundCard("sound_card_tone_blaster_hi_fi", new SoundCardSpec(HardwareEra.TRANSITION,
                    PcieGeneration.PCIE_1_0, 10, SoundCardSpec.Synthesis.WAVETABLE, 32, 16, true,
                    SoundCardSpec.SampleRate.KHZ_44))
                    .named("Artisan Tone Blaster Hi-Fi").register();

    private HardwareItems() {
    }

    /**
     * Forces this class to load so its items are declared onto the mod's content. Called from
     * {@link ComputingModule} before the content is handed to the mod event bus.
     */
    public static void init() {
        /*
         * Intentionally empty: referencing the class triggers static initialization, which performs the
         * declarations above.
         */
    }

    private static <T extends Item> ItemBuilder<T> declare(final String id,
                                                          final Function<Item.Properties, T> factory) {
        return ComputingContent.CONTENT.item(id, factory).tab(ComputingContent.CATALOGUE);
    }

    private static ItemBuilder<CpuItem> cpu(final String id, final CpuSpec spec) {
        return declare(id, properties -> new CpuItem(properties, spec));
    }

    private static ItemBuilder<RamItem> ram(final String id, final RamSpec spec) {
        return declare(id, properties -> new RamItem(properties, spec));
    }

    private static ItemBuilder<GpuItem> gpu(final String id, final GpuSpec spec) {
        return declare(id, properties -> new GpuItem(properties, spec));
    }

    private static ItemBuilder<SoundCardItem> soundCard(final String id, final SoundCardSpec spec) {
        return declare(id, properties -> new SoundCardItem(properties, spec));
    }

    private static ItemBuilder<PsuItem> psu(final String id, final PsuSpec spec) {
        return declare(id, properties -> new PsuItem(properties, spec));
    }

    private static ItemBuilder<MotherboardItem> board(final String id, final MotherboardSpec spec) {
        return declare(id, properties -> new MotherboardItem(properties, spec));
    }

    private static ItemBuilder<DiskItem> disk(final String id, final DiskSpec spec) {
        return declare(id, properties -> new DiskItem(properties, spec));
    }

    /** The eras' parts first, then the supplies, then the disks: the last two carry no era worth sorting by. */
    private static int shelf(final Item item) {
        return item instanceof PsuItem ? 1 : item instanceof DiskItem ? 2 : 0;
    }

    private static HardwareEra era(final Item item) {
        return switch (item) {
            case MotherboardItem board -> board.spec().era();
            case CpuItem cpu -> cpu.spec().era();
            case RamItem ram -> ram.spec().era();
            case GpuItem gpu -> gpu.spec().era();
            case SoundCardItem sound -> sound.spec().era();
            default -> HardwareEra.VINTAGE;
        };
    }

    private static int kind(final Item item) {
        return switch (item) {
            case MotherboardItem board -> 0;
            case CpuItem cpu -> 1;
            case RamItem ram -> 2;
            case GpuItem gpu -> 3;
            case SoundCardItem sound -> 4;
            default -> 5;
        };
    }
}
