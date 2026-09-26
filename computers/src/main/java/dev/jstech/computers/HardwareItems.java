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
     * motherboards, then CPUs, RAM and GPUs, so the progression reads cleanly; then the supplies and the disks, which
     * follow every era. Parts the order ranks alike keep the order they are declared in.
     */
    public static final Comparator<Item> CREATIVE_ORDER = Comparator.comparingInt(HardwareItems::shelf)
            .thenComparing(HardwareItems::era)
            .thenComparingInt(HardwareItems::kind);

    //  VINTAGE: ISA/PCI buses, SIMM/EDO RAM, single-core CPUs

    public static final DeferredItem<CpuItem> CPU_INTEGRA_486SX = cpu("cpu_integra_486sx",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 25, 3, false)).named("Integra 486SX").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_486DX2 = cpu("cpu_integra_486dx2",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 66, 5, false)).named("Integra 486DX2").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_486DX4 = cpu("cpu_integra_486dx4",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 100, 5, false)).named("Integra 486DX4")
            .register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_K6_II = cpu("cpu_velocion_k6_ii",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 350, 15, false)).named("Velocion K6-II")
            .register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_K6_III = cpu("cpu_velocion_k6_iii",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 400, 20, false)).named("Velocion K6-III")
            .register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_K6_III_PLUS = cpu("cpu_velocion_k6_iii_plus",
            new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 450, 22, false)).named("Velocion K6-III+")
            .register();

    public static final DeferredItem<RamItem> RAM_SIMM_4 = ram("ram_simm_4",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.SIMM, 1, 1)).named("Stratix Layer SIMM-4").register();
    public static final DeferredItem<RamItem> RAM_EDO_16 = ram("ram_edo_16",
            new RamSpec(HardwareEra.VINTAGE, RamGeneration.EDO, 4, 2)).named("Stratix Layer EDO-16").register();

    /*
     * Vintage GPU ladder (ISA entry to PCI high-end). VGA-256 and the 3D Blaster are the floor; the two
     * PCI cards below extend the era upward with more cores, VRAM and draw. Single-digit cores and a few MB
     * of VRAM is era-appropriate for fixed-function 2D/early-3D accelerators.
     */
    public static final DeferredItem<GpuItem> GPU_VGA_256 = gpu("gpu_vga_256",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.ISA, 1, 1, 5)).named("Visara VGA-256").register();
    public static final DeferredItem<GpuItem> GPU_3D_BLASTER = gpu("gpu_3d_blaster",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 1, 2, 5)).named("Pyrix 3D Blaster").register();
    public static final DeferredItem<GpuItem> GPU_PRISM_4 = gpu("gpu_prism_4",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 2, 4, 12)).named("Visara Prism 4").register();
    public static final DeferredItem<GpuItem> GPU_VOODOO_GFX = gpu("gpu_voodoo_gfx",
            new GpuSpec(HardwareEra.VINTAGE, PcieGeneration.PCI, 3, 8, 18)).named("Pyrix Voodoo GFX").register();

    public static final DeferredItem<PsuItem> PSU_300B =
            psu("psu_300b", new PsuSpec(300, 80)).named("MF PowerBasic 300B").register();

    public static final DeferredItem<MotherboardItem> MOTHERBOARD_BABYAT_VINTAGE = board("motherboard_babyat_vintage",
            new MotherboardSpec(FormFactor.BABY_AT, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_3, 1, Set.of(RamGeneration.SIMM), 4, PcieGeneration.ISA, 4, 2, 2))
            .named("MF Baby-AT I Motherboard").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_AT_VINTAGE = board("motherboard_at_vintage",
            new MotherboardSpec(FormFactor.AT, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_7, 1, Set.of(RamGeneration.SIMM, RamGeneration.EDO), 8,
                    PcieGeneration.PCI, 7, 4, 2))
            .named("MF AT Standard Motherboard").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_MTX_VINTAGE = board("motherboard_mtx_vintage",
            new MotherboardSpec(FormFactor.MTX, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_7, 2, Set.of(RamGeneration.SIMM, RamGeneration.EDO), 16,
                    PcieGeneration.PCI, 8, 4, 8))
            .named("MF MTX-V Motherboard").register();
    /*
     * Dual-socket server board for vintage-era rack hardware; more RAM slots and PCIe slots
     * than the desktop MTX variant to match server-class density expectations of the era.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EEB_VINTAGE = board("motherboard_eeb_vintage",
            new MotherboardSpec(FormFactor.EEB, HardwareEra.VINTAGE,
                    CpuSocketId.SOCKET_7, 2, Set.of(RamGeneration.SIMM, RamGeneration.EDO), 16,
                    PcieGeneration.PCI, 10, 8, 8))
            .named("MF EEB-V Server Board").register();

    /*
     * Vintage spinning disks: MFM/IDE rotating platters of 20 MB and 100 MB. Tiny by design (the floor of
     * the storage ladder) and honest: at 16 bits an item costs 1 MB, so they hold 20 and 100 items.
     */
    public static final DeferredItem<DiskItem> DISK_TRENCH_20M = disk("disk_vaultis_trench_20m",
            new DiskSpec(StorageTier.HDD, HardwareEra.VINTAGE, 20L, 5)).named("Vaultis Trench HDD 20M").register();
    public static final DeferredItem<DiskItem> DISK_TRENCH_100M = disk("disk_vaultis_trench_100m",
            new DiskSpec(StorageTier.HDD, HardwareEra.VINTAGE, 100L, 6)).named("Vaultis Trench HDD 100M").register();

    //  LEGACY: AGP/PCIe 1.0 buses, SDRAM/DDR/DDR2 RAM, first multi-core CPUs

    public static final DeferredItem<CpuItem> CPU_INTEGRA_VERTEX_700 = cpu("cpu_integra_vertex_700",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 700, 28, false))
            .named("Integra Vertex 700").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_VERTEX_III_S_1000 = cpu("cpu_integra_vertex_iii_s_1000",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 1000, 30, false))
            .named("Integra Vertex III-S 1000").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_VERTEX_III_S_1400 = cpu("cpu_integra_vertex_iii_s_1400",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 1400, 32, false))
            .named("Integra Vertex III-S 1400").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_XP_2400 = cpu("cpu_velocion_sprint_xp_2400",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 2000, 65, false))
            .named("Velocion Sprint XP 2400+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_XP_3200 = cpu("cpu_velocion_sprint_xp_3200",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 2200, 76, false))
            .named("Velocion Sprint XP 3200+").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_SPRINT_XP_3800 = cpu("cpu_velocion_sprint_xp_3800",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_A, 1, 2400, 89, false))
            .named("Velocion Sprint XP 3800+").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_DUO_E4300 = cpu("cpu_integra_duo_e4300",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 2, 1800, 65, false))
            .named("Integra Duo E4300").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_DUO_E6600 = cpu("cpu_integra_duo_e6600",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 2, 2400, 65, false))
            .named("Integra Duo E6600").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_DUO_E8500 = cpu("cpu_integra_duo_e8500",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_775, 2, 3160, 65, false))
            .named("Integra Duo E8500").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_DUAL_240 = cpu("cpu_velocion_dual_240",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 2, 2200, 85, false))
            .named("Velocion Dual 240").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_DUAL_280 = cpu("cpu_velocion_dual_280",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 2, 2400, 95, false))
            .named("Velocion Dual 280").register();
    public static final DeferredItem<CpuItem> CPU_VELOCION_DUAL_285 = cpu("cpu_velocion_dual_285",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_940, 2, 2600, 95, false))
            .named("Velocion Dual 285").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5100 = cpu("cpu_integra_servo_5100",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_771, 2, 2000, 65, false))
            .named("Integra Servo 5100").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5160 = cpu("cpu_integra_servo_5160",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_771, 2, 3000, 80, false))
            .named("Integra Servo 5160").register();
    public static final DeferredItem<CpuItem> CPU_INTEGRA_SERVO_5365 = cpu("cpu_integra_servo_5365",
            new CpuSpec(HardwareEra.LEGACY, CpuSocketId.LGA_771, 4, 2000, 120, false))
            .named("Integra Servo 5365").register();

    public static final DeferredItem<RamItem> RAM_SDRAM_128 = ram("ram_sdram_128",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.SDRAM, 32, 5)).named("Stratix Layer SDRAM-128").register();
    public static final DeferredItem<RamItem> RAM_DDR_512 = ram("ram_ddr_512",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR, 128, 10)).named("Stratix Layer DDR-512").register();
    public static final DeferredItem<RamItem> RAM_DDR2_2048 = ram("ram_ddr2_2048",
            new RamSpec(HardwareEra.LEGACY, RamGeneration.DDR2, 512, 12)).named("Stratix Layer DDR2-2048").register();

    /*
     * Legacy GPU ladder (AGP entry to PCIe 1.0 high-end). The Radiance SE is the cheap AGP 4x floor; the
     * GTX 280 caps the era on PCIe 1.0 above the 8800 GT. Tens-to-hundreds of cores and tens-to-hundreds of
     * MB of VRAM track the AGP/early-PCIe generation.
     */
    public static final DeferredItem<GpuItem> GPU_RADIANCE_9200_SE = gpu("gpu_radiance_9200_se",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 2, 16, 30)).named("Pyrix Radiance 9200 SE")
            .register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_256 = gpu("gpu_vertex_256",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_4X, 4, 32, 50)).named("Visara Vertex 256").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_9800_PRO = gpu("gpu_radiance_9800_pro",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.AGP_8X, 8, 128, 70)).named("Pyrix Radiance 9800 Pro")
            .register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_8800_GT = gpu("gpu_vertex_8800_gt",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0, 112, 512, 110)).named("Visara Vertex 8800 GT")
            .register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_GTX_280 = gpu("gpu_vertex_gtx_280",
            new GpuSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0, 240, 1024, 145)).named("Visara Vertex GTX 280")
            .register();

    public static final DeferredItem<PsuItem> PSU_500B =
            psu("psu_500b", new PsuSpec(500, 80)).named("MF PowerBasic 500B").register();

    /*
     * The Legacy ATX board lists "one of Socket A / Socket 370 / LGA 775". A board spec carries a single
     * socket, so this is modeled as one board item per socket, the clean one-value-per-record mapping.
     * Socket A (Athlon XP generation) pre-dates PCIe; its primary GPU slot is AGP 8x.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_SKA = board("motherboard_atx_legacy_ska",
            new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_A, 1, Set.of(RamGeneration.DDR, RamGeneration.DDR2), 4,
                    PcieGeneration.AGP_8X, 4, 4, 4))
            .named("MF ATX Legacy Motherboard (Socket A)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_S370 =
            board("motherboard_atx_legacy_s370", new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_370, 1, Set.of(RamGeneration.DDR, RamGeneration.DDR2), 4,
                    PcieGeneration.PCIE_1_0, 4, 4, 4))
                    .named("MF ATX Legacy Motherboard (Socket 370)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_LEGACY_LGA775 =
            board("motherboard_atx_legacy_lga775", new MotherboardSpec(FormFactor.ATX, HardwareEra.LEGACY,
                    CpuSocketId.LGA_775, 1, Set.of(RamGeneration.DDR, RamGeneration.DDR2), 4,
                    PcieGeneration.PCIE_1_0, 4, 4, 4))
                    .named("MF ATX Legacy Motherboard (LGA 775)").register();
    // The Legacy EATX board lists "one of LGA 775 / Socket 940": one board per socket.
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EATX_LEGACY_LGA775 =
            board("motherboard_eatx_legacy_lga775", new MotherboardSpec(FormFactor.EATX, HardwareEra.LEGACY,
                    CpuSocketId.LGA_775, 2, Set.of(RamGeneration.DDR2), 8, PcieGeneration.PCIE_1_0, 6, 6, 4))
                    .named("MF EATX Legacy Motherboard (LGA 775)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EATX_LEGACY_S940 =
            board("motherboard_eatx_legacy_s940", new MotherboardSpec(FormFactor.EATX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_940, 2, Set.of(RamGeneration.DDR2), 8,
                    PcieGeneration.PCIE_1_0, 6, 6, 4))
                    .named("MF EATX Legacy Motherboard (Socket 940)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_MTX_LEGACY = board("motherboard_mtx_legacy",
            new MotherboardSpec(FormFactor.MTX, HardwareEra.LEGACY,
                    CpuSocketId.SOCKET_940, 4, Set.of(RamGeneration.DDR2), 24,
                    PcieGeneration.PCIE_1_0, 8, 6, 8))
            .named("MF MTX-L Motherboard").register();

    /*
     * Legacy rotating and early solid-state disks: IDE HDDs of 4 GB and 20 GB and the first affordable
     * SATA SSD of 64 GB. At 32 bits an item costs 16 MB, so they hold 256, 1 280 and 4 096 items,
     * between vintage and the standard 500 GB / 1 TB floor.
     */
    public static final DeferredItem<DiskItem> DISK_LINK_IDE_4G = disk("disk_vaultis_link_ide_4g",
            new DiskSpec(StorageTier.HDD, HardwareEra.LEGACY, 256L, 7)).named("Vaultis Link IDE-HDD 4G").register();
    public static final DeferredItem<DiskItem> DISK_LINK_IDE_20G = disk("disk_vaultis_link_ide_20g",
            new DiskSpec(StorageTier.HDD, HardwareEra.LEGACY, 1280L, 8)).named("Vaultis Link IDE-HDD 20G").register();
    public static final DeferredItem<DiskItem> DISK_LINK_SATA_SSD_64G = disk("disk_vaultis_link_sata_ssd_64g",
            new DiskSpec(StorageTier.SSD, HardwareEra.LEGACY, 4096L, 3)).named("Vaultis Link SATA-SSD 64G")
            .register();

    /*
     *  STANDARD: completion of the partially-registered set (PCIe 2.0/3.0, DDR3)
     *  The Servo 2620/2690/2699, the Ascent X4 965, the DDR3-8192, the HD 7970 GPU, the MTX-P /
     *  EEB-P / ATX-P boards and the 650G PSU already live in ComputingModule. These fill the gaps.
     */

    public static final DeferredItem<CpuItem> CPU_ASCENT_X4_955 = cpu("cpu_ascent_x4_955",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.AM3, 4, 3200, 125, false))
            .named("Velocion Ascent X4 955").register();
    public static final DeferredItem<CpuItem> CPU_ASCENT_X6_1090T = cpu("cpu_ascent_x6_1090t",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.AM3, 6, 3200, 125, false))
            .named("Velocion Ascent X6 1090T").register();
    public static final DeferredItem<CpuItem> CPU_APEX_5_4590 = cpu("cpu_apex_5_4590",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 3300, 84, false))
            .named("Integra Apex 5 4590").register();
    public static final DeferredItem<CpuItem> CPU_APEX_5_4690K = cpu("cpu_apex_5_4690k",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 3500, 88, false))
            .named("Integra Apex 5 4690K").register();
    public static final DeferredItem<CpuItem> CPU_APEX_7_4790K = cpu("cpu_apex_7_4790k",
            new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 4000, 88, false))
            .named("Integra Apex 7 4790K").register();

    /*
     * Standard GPU ladder (PCIe 2.0 entry to PCIe 3.0 high-end). The HD 7970 (in ComputingModule) is the
     * upper-mid card; the GTX 550 Ti and HD 6850 are the PCIe 2.0 floor, and the GTX 780 Ti tops the era on
     * PCIe 3.0 above the 7970. Hundreds-to-thousands of cores and 1-3 GB of VRAM fit this generation.
     */
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_7970 = ComputingModule.GPU_HD_7970;
    public static final DeferredItem<GpuItem> GPU_VERTEX_GTX_550_TI = gpu("gpu_vertex_gtx_550_ti",
            new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_2_0, 192, 1024, 116))
            .named("Visara Vertex GTX 550 Ti").register();
    public static final DeferredItem<GpuItem> GPU_RADIANCE_HD_6850 = gpu("gpu_radiance_hd_6850",
            new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_2_0, 960, 2048, 127))
            .named("Pyrix Radiance HD 6850").register();
    public static final DeferredItem<GpuItem> GPU_VERTEX_GTX_780_TI = gpu("gpu_vertex_gtx_780_ti",
            new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2880, 3072, 250))
            .named("Visara Vertex GTX 780 Ti").register();

    public static final DeferredItem<PsuItem> PSU_850G =
            psu("psu_850g", new PsuSpec(850, 90)).named("MF PowerGold 850G").register();

    /*
     * The Standard ATX board comes in an AM3 flavour (already MOTHERBOARD_ATX_P in ComputingModule) and
     * an LGA 1150 flavour for the Apex line, so register the missing socket variant and the WS board.
     */
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_STANDARD_LGA1150 =
            board("motherboard_atx_standard_lga1150", new MotherboardSpec(FormFactor.ATX, HardwareEra.STANDARD,
                    CpuSocketId.LGA_1150, 1, Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_3_0, 4, 2, 4))
                    .named("MF ATX Standard Motherboard (LGA 1150)").register();
    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EATX_STANDARD_WS =
            board("motherboard_eatx_standard_ws", new MotherboardSpec(FormFactor.EATX, HardwareEra.STANDARD,
                    CpuSocketId.LGA_2011, 1, Set.of(RamGeneration.DDR3), 8, PcieGeneration.PCIE_3_0, 7, 4, 4))
                    .named("MF EATX Standard Workstation Board").register();

    /*
     * The sound cards: one for each bus the boards of their era have. The two of an era sound the same and differ
     * in the slot they take and how they look. The Vintage ones are really lo-fi, eight bits in one channel at 22
     * kHz, making their notes by FM; the Legacy ones play recordings at CD quality from a bank of instruments.
     * Standard boards have their sound built in, so there is no Standard card.
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
    public static final DeferredItem<SoundCardItem> SOUND_CARD_TONE_BLASTER_HI_FI =
            soundCard("sound_card_tone_blaster_hi_fi", new SoundCardSpec(HardwareEra.LEGACY, PcieGeneration.PCIE_1_0,
                    10, SoundCardSpec.Synthesis.WAVETABLE, 32, 16, true, SoundCardSpec.SampleRate.KHZ_44))
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
            default -> HardwareEra.VINTAGE;
        };
    }

    private static int kind(final Item item) {
        return switch (item) {
            case MotherboardItem board -> 0;
            case CpuItem cpu -> 1;
            case RamItem ram -> 2;
            case GpuItem gpu -> 3;
            default -> 4;
        };
    }
}
