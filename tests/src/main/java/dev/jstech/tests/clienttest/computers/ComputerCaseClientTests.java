/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.DeviceFront;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CaseFrontLayout;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.os.media.MediaVolume;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The small computers' cases in every age: the Personal Computer, the Crafting Computer and the Cluster Management
 * Computer side by side, and from the Standard age on the three cases each comes in, Neutral, High Performance and
 * Aesthetic, one above the other. Each age is seen from the front and from the side that comes off; every case fills
 * its block. With the side off, each case shows the parts built in it.
 */
public final class ComputerCaseClientTests {

    private static final int SETTLE = 6;
    /** The row seen from the front, facing south, and the row turned to show its side, facing west. */
    private static final int FRONT_Z = 10;
    private static final int SIDE_Z = 6;
    /** How far from the cases the camera stands, and how far apart the ages are. */
    private static final int AWAY = 5;
    private static final int AGE_SPACING = 8;
    /** The closed case whose side is taken off. */
    private static final BlockPos CASE = new BlockPos(4, 2, 6);
    /** The machines built with their parts stand in a row, apart enough for each to be seen on its own. */
    private static final int BUILD_SPACING = 3;
    private static final int BUILD_Z = 16;
    /**
     * The machines switched on, a block up at the eye, apart from the rows, each built as one of the builds below:
     * one seen through its glass, one open whose cooler's fan faces the open side.
     */
    private static final BlockPos THROUGH_GLASS = new BlockPos(4, 3, 24);
    private static final BlockPos OPEN = new BlockPos(8, 3, 24);
    private static final String GLASS_BUILD = "advanced-aesthetic";
    private static final String OPEN_BUILD = "legacy-939";
    /** Ticks between two looks at the turning fans, and between two looks at a blinking lamp (it blinks each 5). */
    private static final int FAN_GAP = 3;
    private static final int BLINK_GAP = 5;
    /** How long the machines may take to be known running once switched on. */
    private static final int RUNS_WITHIN = 100;
    /** The machines of the two ages whose skins came with them, opened one by one in a row of their own. */
    private static final int SKIN_Z = 30;
    private static final List<BlockEntry<?>> SKINNED = List.of(ComputingModule.TRANSITION_PERSONAL_COMPUTER,
            ComputingModule.TRANSITION_CRAFTING_COMPUTER, ComputingModule.TRANSITION_CLUSTER_MANAGEMENT_COMPUTER,
            ComputingModule.ADVANCED_PERSONAL_COMPUTER, ComputingModule.ADVANCED_CRAFTING_COMPUTER,
            ComputingModule.ADVANCED_CLUSTER_MANAGEMENT_COMPUTER);
    /** The fronts with their bays filled stand in a row of their own, seen a little from above. */
    private static final int FRONT_ROW_Z = 36;
    private static final float FRONT_PITCH = 18F;
    /** The machine whose drive reads and whose hot-swap bay takes a disk, on its own; its drive's lamp bone. */
    private static final BlockPos BAYS_AT = new BlockPos(2, 2, 44);
    private static final String BAY_LAMP = "lamp_0_cd";
    /** The case and the Mainframe whose fronts are looked at, part by part, at the eye. */
    private static final BlockPos OUTLINED = new BlockPos(2, 3, 52);
    private static final BlockPos OUTLINED_MAINFRAME = new BlockPos(9, 2, 52);
    /** The machine whose hot-swap bay is worked by hand, on its own, and the items it keeps on the bay's disk. */
    private static final BlockPos HOT_SWAP_AT = new BlockPos(2, 2, 60);
    private static final int KEPT = 32;
    /** The case with an internal CD drive and the CD drive of its own whose trays are opened empty, side by side. */
    private static final BlockPos TRAY_CASE = new BlockPos(2, 2, 68);
    private static final BlockPos TRAY_DRIVE = new BlockPos(5, 2, 68);
    /** The case and the drive whose standing parts are aimed at, at the eye, on a row of their own. */
    private static final BlockPos STANDING_CASE = new BlockPos(2, 3, 76);
    private static final BlockPos STANDING_DRIVE = new BlockPos(8, 3, 76);
    /** A front of each age with what its bays and ports hold. */
    private static final List<Front> FRONTS = List.of(
            new Front("vintage-hot-swap-with-a-disk", ComputingModule.VINTAGE_PERSONAL_COMPUTER,
                    List.of("hot_swap_drive_bay"),
                    (pc, level) -> pc.front().insert(0, Build.stack("disk_vaultis_trench_20m"))),
            new Front("legacy-cd-tray-out-and-an-empty-hot-swap", ComputingModule.LEGACY_PERSONAL_COMPUTER,
                    List.of("internal_cd_drive", "hot_swap_drive_bay"), (pc, level) -> {
                        pc.front().tray(0).press(level, pc.getBlockPos());
                        pc.front().insert(0, new ItemStack(ComputingModule.CD_ROM.get()));
                    }),
            new Front("transition-dvd-blu-ray-out-two-sticks", ComputingModule.TRANSITION_PERSONAL_COMPUTER,
                    List.of("internal_dvd_drive", "internal_blu_ray_drive"), (pc, level) -> {
                        pc.front().tray(1).press(level, pc.getBlockPos());
                        pc.front().insert(1, new ItemStack(ComputingModule.BD_ROM.get()));
                        pc.front().insert(CaseFront.portSlot(0), new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
                        pc.front().insert(CaseFront.portSlot(1), new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
                    }),
            new Front("standard-dvd-in-and-a-stick", ComputingModule.PERSONAL_COMPUTER,
                    List.of("internal_dvd_drive"), (pc, level) -> {
                        pc.front().insert(0, new ItemStack(ComputingModule.DVD_RW.get()));
                        pc.front().insert(CaseFront.portSlot(0), new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
                    }),
            new Front("standard-aesthetic-empty-bay", ComputingModule.AESTHETIC_PERSONAL_COMPUTER, List.of(),
                    (pc, level) -> { }),
            new Front("advanced-two-sticks", ComputingModule.ADVANCED_PERSONAL_COMPUTER, List.of(), (pc, level) -> {
                pc.front().insert(CaseFront.portSlot(0), new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
                pc.front().insert(CaseFront.portSlot(1), new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
            }));
    /** One machine per case, after the builds round 17 showed, and two holding parts of other ages. */
    private static final List<Build> BUILDS = List.of(
            new Build("vintage-486", ComputingModule.VINTAGE_PERSONAL_COMPUTER, "motherboard_babyat_vintage",
                    "cpu_integra_486dx2", List.of("ram_simm_4", "ram_simm_4", "ram_simm_4", "ram_simm_4"),
                    List.of("gpu_vga_256", "sound_card_tone_blaster"), "psu_200", List.of("disk_vaultis_trench_20m")),
            new Build("vintage-slot-1", ComputingModule.VINTAGE_PERSONAL_COMPUTER, "motherboard_at_vintage_slot1",
                    "cpu_integra_pentix_iii_600", List.of("ram_sdram_64", "ram_sdram_64"),
                    List.of("gpu_prism_tnt", "gpu_voodoo_gfx"), "psu_300", List.of("disk_vaultis_trench_200m")),
            new Build("vintage-cluster", ComputingModule.VINTAGE_CLUSTER_MANAGEMENT_COMPUTER,
                    "motherboard_at_vintage", "cpu_velocion_k6_iii",
                    List.of("ram_simm_16", "ram_simm_16", "ram_simm_16", "ram_simm_16"),
                    List.of("gpu_3d_blaster", "serial_console_card"), "psu_300", List.of("disk_vaultis_trench_200m")),
            new Build("legacy-939", ComputingModule.LEGACY_PERSONAL_COMPUTER, "motherboard_atx_legacy_939",
                    "cpu_velocion_sprint_64_fx_55", List.of("ram_ddr_1024", "ram_ddr_1024"),
                    List.of("gpu_radiance_x800_xt", "sound_card_tone_blaster_audigy"), "psu_500b",
                    List.of("disk_vaultis_link_ide_40g")),
            new Build("legacy-crafting", ComputingModule.LEGACY_CRAFTING_COMPUTER, "motherboard_atx_legacy_478",
                    "cpu_integra_pentix_4_2_4c", List.of("ram_ddr_256", "ram_ddr_256"),
                    List.of("gpu_radiance_9200_se", "crafting_card_t2"), "psu_350",
                    List.of("disk_vaultis_link_ide_20g")),
            new Build("transition-775", ComputingModule.TRANSITION_PERSONAL_COMPUTER,
                    "motherboard_atx_transition_775", "cpu_integra_centro_2_duo_e6600",
                    List.of("ram_ddr2_2048", "ram_ddr2_2048"),
                    List.of("gpu_vertex_8800_gt", "sound_card_tone_blaster_hi_fi"), "psu_450b",
                    List.of("disk_hdd_500g")),
            new Build("transition-x58", ComputingModule.TRANSITION_PERSONAL_COMPUTER,
                    "motherboard_eatx_transition_1366", "cpu_integra_centro_c7_980x",
                    List.of("ram_ddr3_4096", "ram_ddr3_4096", "ram_ddr3_4096"), List.of("gpu_vertex_gtx_480"),
                    "psu_650g", List.of("disk_vaultis_link_sata_ssd_64g")),
            new Build("standard-neutral", ComputingModule.PERSONAL_COMPUTER, "motherboard_atx_standard_1155",
                    "cpu_integra_centro_c5_2500k", List.of("ram_ddr3_8192", "ram_ddr3_8192"),
                    List.of("gpu_vertex_gtx_660"), "psu_850g", List.of("disk_ssd_500g")),
            new Build("standard-high-performance", ComputingModule.HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                    "motherboard_atx_standard_lga1150", "cpu_integra_centro_c7_4790k",
                    List.of("ram_ddr3_8192", "ram_ddr3_8192"), List.of("gpu_vertex_gtx_970"), "psu_850g",
                    List.of("disk_nvme_500g", "disk_hdd_4t")),
            new Build("standard-aesthetic", ComputingModule.AESTHETIC_PERSONAL_COMPUTER,
                    "motherboard_eatx_standard_2011", "cpu_integra_centro_c7_4960x",
                    List.of("ram_ddr3_8192", "ram_ddr3_8192", "ram_ddr3_8192", "ram_ddr3_8192"),
                    List.of("gpu_vertex_gtx_780_ti"), "psu_850g", List.of("disk_hdd_8t", "disk_ssd_1t")),
            new Build("standard-cluster", ComputingModule.HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER,
                    "motherboard_atx_standard_lga1150", "cpu_integra_servo_1231_v3",
                    List.of("ram_ddr3_8192", "ram_ddr3_8192", "ram_ddr3_8192", "ram_ddr3_8192"),
                    List.of("gpu_vertex_gt_730", "fabric_host_adapter"), "psu_850g", List.of("disk_hdd_16t")),
            new Build("advanced-neutral", ComputingModule.ADVANCED_PERSONAL_COMPUTER,
                    "motherboard_atx_advanced_1151", "cpu_integra_centro_c9_9900k",
                    List.of("ram_ddr4_16384", "ram_ddr4_16384"), List.of("gpu_vertex_gtx_1080_ti"), "psu_1000g",
                    List.of("disk_ssd_8t")),
            new Build("advanced-high-performance", ComputingModule.ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                    "motherboard_atx_advanced_1700", "cpu_integra_centro_c9_13900k",
                    List.of("ram_ddr5_32768", "ram_ddr5_32768", "ram_ddr5_32768", "ram_ddr5_32768"),
                    List.of("gpu_vertex_rtx_4090"), "psu_1200p", List.of("disk_nvme_8t", "disk_hdd_24t")),
            new Build("advanced-aesthetic", ComputingModule.ADVANCED_AESTHETIC_PERSONAL_COMPUTER,
                    "motherboard_atx_advanced_1851", "cpu_integra_centro_ultra_c9_285k",
                    List.of("ram_ddr5_16384", "ram_ddr5_16384", "ram_ddr5_16384", "ram_ddr5_16384"),
                    List.of("gpu_vertex_rtx_5090"), "psu_1600p", List.of("disk_nvme_8t")),
            new Build("advanced-crafting", ComputingModule.ADVANCED_CRAFTING_COMPUTER, "motherboard_atx_advanced_am4",
                    "cpu_velocion_awayken_3_3200g", List.of("ram_ddr4_8192", "ram_ddr4_8192"),
                    List.of("", "crafting_card_t4"), "psu_1000g", List.of("disk_nvme_8t")),
            new Build("standard-older-parts", ComputingModule.PERSONAL_COMPUTER, "motherboard_atx_standard_lga1150",
                    "cpu_integra_centro_c7_4790k", List.of("ram_ddr3_4096", "ram_ddr3_4096"),
                    List.of("gpu_vertex_8800_gt"), "psu_500b",
                    List.of("disk_vaultis_link_sata_ssd_64g", "disk_hdd_1t")),
            new Build("transition-newer-supply", ComputingModule.TRANSITION_PERSONAL_COMPUTER,
                    "motherboard_atx_transition_775", "cpu_integra_pentix_4_560", List.of("ram_ddr2_512"),
                    List.of("gpu_vertex_6600_gt"), "psu_850g", List.of("disk_vaultis_link_ide_40g")));

    /** The ages, each its rows of cases from the floor up: in a row the three machines, in one case. */
    private static final List<Age> AGES = List.of(
            new Age("vintage", List.of(List.of(ComputingModule.VINTAGE_PERSONAL_COMPUTER,
                    ComputingModule.VINTAGE_CRAFTING_COMPUTER, ComputingModule.VINTAGE_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("legacy", List.of(List.of(ComputingModule.LEGACY_PERSONAL_COMPUTER,
                    ComputingModule.LEGACY_CRAFTING_COMPUTER, ComputingModule.LEGACY_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("transition", List.of(List.of(ComputingModule.TRANSITION_PERSONAL_COMPUTER,
                    ComputingModule.TRANSITION_CRAFTING_COMPUTER,
                    ComputingModule.TRANSITION_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("standard", List.of(
                    List.of(ComputingModule.PERSONAL_COMPUTER, ComputingModule.CRAFTING_COMPUTER,
                            ComputingModule.CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                            ComputingModule.HIGH_PERFORMANCE_CRAFTING_COMPUTER,
                            ComputingModule.HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.AESTHETIC_PERSONAL_COMPUTER, ComputingModule.AESTHETIC_CRAFTING_COMPUTER,
                            ComputingModule.AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER))),
            new Age("advanced", List.of(
                    List.of(ComputingModule.ADVANCED_PERSONAL_COMPUTER, ComputingModule.ADVANCED_CRAFTING_COMPUTER,
                            ComputingModule.ADVANCED_CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                            ComputingModule.ADVANCED_HIGH_PERFORMANCE_CRAFTING_COMPUTER,
                            ComputingModule.ADVANCED_HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER),
                    List.of(ComputingModule.ADVANCED_AESTHETIC_PERSONAL_COMPUTER,
                            ComputingModule.ADVANCED_AESTHETIC_CRAFTING_COMPUTER,
                            ComputingModule.ADVANCED_AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER))));

    private ComputerCaseClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void computerCases_fillTheirBlockInEveryAge(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int age = 0; age < AGES.size(); age++) {
                final List<List<BlockEntry<?>>> rows = AGES.get(age).rows();
                for (int row = 0; row < rows.size(); row++) {
                    for (int machine = 0; machine < rows.get(row).size(); machine++) {
                        final var block = rows.get(row).get(machine).get();
                        final int x = age * AGE_SPACING + machine;
                        world.setBlock(new BlockPos(x, 2 + row, FRONT_Z), block.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                        world.setBlock(new BlockPos(x, 2 + row, SIDE_Z), block.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                    }
                }
            }
        });
        for (int age = 0; age < AGES.size(); age++) {
            final int middle = age * AGE_SPACING + 1;
            ctx.thenTeleport(SETTLE, new BlockPos(middle, 2, FRONT_Z + AWAY), Direction.NORTH)
                    .thenScreenshot(SETTLE, AGES.get(age).name() + "-front")
                    .thenTeleport(SETTLE, new BlockPos(middle, 2, SIDE_Z - AWAY), Direction.SOUTH)
                    .thenScreenshot(SETTLE, AGES.get(age).name() + "-side");
        }
    }

    /**
     * A closed case has its side taken off by sneaking and using it, which shows its inside; its assembly screen then
     * says the side is open.
     */
    @ClientTest(timeoutTicks = 600)
    public static void sideTakenOff_showsTheInsideAndTheScreenSaysSo(final ClientTestContext ctx) {
        // Turned to the west, the case's left side faces north, toward the camera.
        ctx.thenBuild(0, world -> world.setBlock(CASE, ComputingModule.ADVANCED_PERSONAL_COMPUTER.get()
                        .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)))
                .thenTeleport(SETTLE, CASE.north(3), Direction.SOUTH)
                .thenScreenshot(SETTLE, "side-on")
                .thenSneakClick(SETTLE, CASE, Direction.NORTH)
                .thenScreenshot(SETTLE, "side-off")
                .thenRightClick(SETTLE, CASE)
                .thenScreenshot(SETTLE * 2, "screen-says-open")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    /**
     * Each case with a machine built in it and its side off, seen from the open side: every part drawn by its own
     * model in its place, the board's parts on the board's seats, the supply and the disks on the case's, the case's
     * cooler on the processor (none on a cartridge that brings its own); an Advanced case's hard disk is in the cage
     * under its shroud, hidden like its supply. The last two machines hold parts of other
     * ages: a supply made for the other end of the case is turned over and has no leads, a card and a disk of an
     * older age sit where this case puts them.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void installedParts_eachDrawnInItsPlace(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int i = 0; i < BUILDS.size(); i++) {
                // A block up, at the eye of the player standing before it.
                final BlockPos at = new BlockPos(i * BUILD_SPACING, 3, BUILD_Z);
                world.setBlock(at, BUILDS.get(i).machine().get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                final AbstractSmallComputerBlockEntity computer =
                        world.blockEntity(at, AbstractSmallComputerBlockEntity.class);
                BUILDS.get(i).install(computer);
                computer.toggleSidePanel();
            }
        });
        // Turned to the west, a case's open side faces north, toward the camera.
        for (int i = 0; i < BUILDS.size(); i++) {
            ctx.thenTeleport(SETTLE, new BlockPos(i * BUILD_SPACING, 2, BUILD_Z - 2), Direction.SOUTH)
                    .thenScreenshot(SETTLE, BUILDS.get(i).name());
        }
    }

    /**
     * Two machines switched on. Through the glass of the one with its side on, its parts and its turning fans; in the
     * open one, the cooler's fan facing the open side, caught twice a few ticks apart as it turns; on its front the
     * power lamp lit, and the disk lamp blinking while it works its disk, caught lit and dark.
     */
    @ClientTest(timeoutTicks = 600)
    public static void running_lightsItsLampsAndTurnsItsFans(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (final BlockPos at : List.of(THROUGH_GLASS, OPEN)) {
                final Build build = build(at.equals(OPEN) ? OPEN_BUILD : GLASS_BUILD);
                world.setBlock(at, build.machine().get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                final AbstractSmallComputerBlockEntity computer =
                        world.blockEntity(at, AbstractSmallComputerBlockEntity.class);
                build.install(computer);
                if (at.equals(OPEN)) {
                    computer.toggleSidePanel();
                }
                computer.togglePower();
            }
        })
                .thenWaitUntil(() -> runs(ctx, THROUGH_GLASS) && runs(ctx, OPEN), RUNS_WITHIN,
                        "both machines to run, as their players see them")
                .thenTeleport(SETTLE, new BlockPos(THROUGH_GLASS.getX(), 2, THROUGH_GLASS.getZ() - 2),
                        Direction.SOUTH)
                .thenScreenshot(SETTLE, "through-the-glass")
                .thenTeleport(SETTLE, new BlockPos(OPEN.getX(), 2, OPEN.getZ() - 2), Direction.SOUTH)
                .thenScreenshot(SETTLE, "cooler-fan-turned")
                .thenScreenshot(FAN_GAP, "cooler-fan-turned-further")
                .thenTeleport(SETTLE, new BlockPos(OPEN.getX() - 2, 2, OPEN.getZ()), Direction.EAST)
                .thenBuild(0, world -> world.blockEntity(OPEN, AbstractSmallComputerBlockEntity.class)
                        .diskWorked(world.level()))
                .thenScreenshot(SETTLE, "disk-lamp-a")
                .thenScreenshot(BLINK_GAP, "disk-lamp-b");
    }

    /**
     * The fronts of every age with their bays filled: each part installed in a bay drawn in it, an empty bay showing its
     * cover; a drive with its tray out and a disc on it, another with its tray in; the hot-swap bay with its caddy in
     * and a disk in it, or with none; and a USB flash drive in each port of the cases that have them.
     */
    @ClientTest(timeoutTicks = 900)
    public static void fronts_showWhatTheirBaysAndPortsHold(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int i = 0; i < FRONTS.size(); i++) {
                final Front front = FRONTS.get(i);
                final BlockPos at = new BlockPos(i * 2, 2, FRONT_ROW_Z);
                world.setBlock(at, front.machine().get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                final AbstractSmallComputerBlockEntity computer =
                        world.blockEntity(at, AbstractSmallComputerBlockEntity.class);
                final ComputerHardwareLayout layout = computer.hardwareLayout();
                for (int bay = 0; bay < front.bays().size(); bay++) {
                    computer.getHardware().setStackInSlot(layout.bayStart() + bay, Build.stack(front.bays().get(bay)));
                }
                front.fill(computer, world.level());
            }
        });
        for (int i = 0; i < FRONTS.size(); i++) {
            ctx.thenTeleport(SETTLE, new BlockPos(i * 2, 2, FRONT_ROW_Z + 3), Direction.NORTH)
                    .then(0, () -> ctx.player().setXRot(FRONT_PITCH))
                    .thenScreenshot(SETTLE * 4, FRONTS.get(i).name());
        }
    }

    /**
     * A Legacy machine switched on with a CD drive and the hot-swap bay: the drive's lamp blinks while the machine
     * reads the installer on its disc, caught lit and dark; the hot-swap bay's caddy rides in as a disk goes in and
     * out as it comes out, caught on its way and at rest.
     */
    @ClientTest(timeoutTicks = 900)
    public static void bays_blinkWhileReadingAndTheCaddyRides(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            world.setBlock(BAYS_AT, ComputingModule.LEGACY_PERSONAL_COMPUTER.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            final AbstractSmallComputerBlockEntity computer =
                    world.blockEntity(BAYS_AT, AbstractSmallComputerBlockEntity.class);
            final ComputerHardwareLayout layout = computer.hardwareLayout();
            build("legacy-939").install(computer);
            computer.getHardware().setStackInSlot(layout.diskStart(), Build.stack("disk_vaultis_link_ide_40g"));
            computer.getHardware().setStackInSlot(layout.bayStart(), Build.stack("internal_cd_drive"));
            computer.getHardware().setStackInSlot(layout.bayStart() + 1, Build.stack("hot_swap_drive_bay"));
            final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
            MediaItem.setKind(disc, MediaKind.OS_INSTALL);
            MediaItem.setPayload(disc, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp"));
            // Put in as a drive comes with its disc already drawn in, so no tray rides as the camera arrives.
            computer.front().insert(0, disc);
            computer.togglePower();
            final MediaVolume volume = computer.front().volume(0);
            if (FirmwarePayloads.beginInstall(world.level(), computer, volume.readerPos(), volume.bay(), -1) != null) {
                throw new ClientTestFailure("the install from the bay's disc did not start");
            }
        })
                .thenWaitUntil(() -> runs(ctx, BAYS_AT) && clientFront(ctx).reading(0), RUNS_WITHIN,
                        "the machine to run and read the bay's disc, as the player sees it")
                .thenTeleport(SETTLE, BAYS_AT.south(2), Direction.NORTH)
                .then(0, () -> ctx.player().setXRot(FRONT_PITCH))
                .thenWaitUntil(() -> bayLamp(ctx, false), BLINK_GAP * 4, "the drive's lamp to be lit")
                .thenScreenshot(0, "reading-lamp-lit")
                .thenWaitUntil(() -> bayLamp(ctx, true), BLINK_GAP * 4, "and then dark, blinking")
                .thenScreenshot(0, "reading-lamp-dark")
                .thenBuild(0, world -> world.blockEntity(BAYS_AT, AbstractSmallComputerBlockEntity.class).front()
                        .insert(1, Build.stack("disk_vaultis_link_ide_20g")))
                .thenWaitUntil(() -> clientFront(ctx).caddyMoving(1, ctx.mc().level), SETTLE * 4,
                        "the caddy to ride in, as the player sees it")
                .thenScreenshot(2, "caddy-riding-in")
                .thenScreenshot(SETTLE * 3, "caddy-in")
                .thenBuild(0, world -> world.blockEntity(BAYS_AT, AbstractSmallComputerBlockEntity.class).front()
                        .take(1))
                .thenWaitUntil(() -> clientFront(ctx).caddyMoving(1, ctx.mc().level), SETTLE * 4,
                        "the caddy to ride out")
                .thenScreenshot(2, "caddy-riding-out")
                .thenScreenshot(SETTLE * 3, "caddy-gone");
    }

    /**
     * The parts of a front outlined on their own while the crosshair rests on them: a Transition case's power button,
     * the eject button of the DVD drive in its first bay, a USB port and the hot-swap bay in its second bay; and a
     * Mainframe's power button on its sill.
     */
    @ClientTest(timeoutTicks = 900)
    public static void frontParts_areOutlinedWhenLookedAt(final ClientTestContext ctx) {
        final CaseFrontLayout layout = ComputingModule.TRANSITION_PERSONAL_COMPUTER.get().frontLayout();
        final EjectButton eject = layout.ejectButton(0, BayDriveSpec.Kind.DVD);
        ctx.thenBuild(0, world -> {
            world.setBlock(OUTLINED, ComputingModule.TRANSITION_PERSONAL_COMPUTER.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            final AbstractSmallComputerBlockEntity computer =
                    world.blockEntity(OUTLINED, AbstractSmallComputerBlockEntity.class);
            final ComputerHardwareLayout hardware = computer.hardwareLayout();
            computer.getHardware().setStackInSlot(hardware.bayStart(), Build.stack("internal_dvd_drive"));
            computer.getHardware().setStackInSlot(hardware.bayStart() + 1, Build.stack("hot_swap_drive_bay"));
            world.setBlock(OUTLINED_MAINFRAME, ComputingModule.MAINFRAME.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        })
                .thenTeleport(SETTLE, OUTLINED.south(2), Direction.NORTH)
                .then(SETTLE, () -> ctx.lookAtFront(OUTLINED, layout.power().middleX(), layout.power().middleY()))
                .thenScreenshot(SETTLE, "power-button")
                .then(0, () -> ctx.lookAtFront(OUTLINED, (eject.left() + eject.right()) / 2.0,
                        (eject.top() + eject.bottom()) / 2.0))
                .thenScreenshot(SETTLE, "eject-button")
                .then(0, () -> ctx.lookAtFront(OUTLINED, layout.ports().get(0).middleX(),
                        layout.ports().get(0).middleY()))
                .thenScreenshot(SETTLE, "usb-port")
                .then(0, () -> ctx.lookAtFront(OUTLINED, layout.bays().get(1).middleX(), layout.bays().get(1).middleY()))
                .thenScreenshot(SETTLE, "hot-swap-bay")
                .thenTeleport(SETTLE, OUTLINED_MAINFRAME.south(3), Direction.NORTH)
                .then(SETTLE, () -> ctx.lookAtFront(OUTLINED_MAINFRAME, MainframeBlock.POWER_BUTTON.middleX(),
                        MainframeBlock.POWER_BUTTON.middleY()))
                .thenScreenshot(SETTLE, "mainframe-power-button");
    }

    /**
     * The hot-swap bay worked by hand, as a player does it, with the machine running. Empty, it shows its empty sleeve
     * and the board at its back, and an empty hand on it opens nothing. A click with a disk puts it in and it joins
     * the build; the machine keeps its items on it, its only disk; and a click with an empty hand gives it back with
     * the items on it, and the machine has none left.
     */
    @ClientTest(timeoutTicks = 900)
    public static void hotSwapDisk_byHandKeepsTheMachinesItemsAndLeavesWithThem(final ClientTestContext ctx) {
        final FrontArea bay = ComputingModule.LEGACY_PERSONAL_COMPUTER.get().frontLayout().bays().get(0);
        final StorageKey cobblestone = StorageKey.of(Items.COBBLESTONE);
        ctx.thenBuild(0, world -> {
            world.setBlock(HOT_SWAP_AT, ComputingModule.LEGACY_PERSONAL_COMPUTER.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            final AbstractSmallComputerBlockEntity computer =
                    world.blockEntity(HOT_SWAP_AT, AbstractSmallComputerBlockEntity.class);
            final ComputerHardwareLayout layout = computer.hardwareLayout();
            build("legacy-939").install(computer);
            computer.getHardware().setStackInSlot(layout.diskStart(), ItemStack.EMPTY);
            computer.getHardware().setStackInSlot(layout.bayStart(), Build.stack("hot_swap_drive_bay"));
            computer.togglePower();
        });
        DeviceCloseUp.closeUp(ctx, HOT_SWAP_AT)
                .thenScreenshot(SETTLE, "hot-swap-empty")
                .thenGive(0)
                .then(1, () -> ctx.selectHotbar(0))
                .then(2, () -> ctx.rightClickFront(HOT_SWAP_AT, bay.middleX(), bay.middleY(), 0))
                .thenAssert(SETTLE, () -> ctx.mc().screen == null, "an empty hand on the empty bay opens nothing")
                .thenGive(0, Build.stack("disk_vaultis_link_ide_20g"))
                .then(0, () -> ctx.selectHotbar(0))
                .thenWaitUntil(() -> ctx.player().getMainHandItem().getItem() instanceof DiskItem, SETTLE * 4,
                        "the disk to reach the player's hand")
                .thenRightClickFront(1, HOT_SWAP_AT, bay.middleX(), bay.middleY())
                .thenWaitUntilServer(level -> {
                            final PersonalComputerBlockEntity computer = hotSwapMachine(ctx, level);
                            return !computer.front().held(0).isEmpty() && computer.currentBuild() != null
                                    && computer.currentBuild().frontDisks().size() == 1;
                        }, SETTLE * 4, "the clicked disk to go in and join the build",
                        level -> "held=" + hotSwapMachine(ctx, level).front().held(0)
                                + " build=" + hotSwapMachine(ctx, level).currentBuild())
                .thenServer(0, level -> ctx.assertTrue(
                        hotSwapMachine(ctx, level).localStore().insert(cobblestone, KEPT) == KEPT,
                        "the machine keeps the items on the bay's disk, its only disk"))
                .thenScreenshot(SETTLE * 3, "hot-swap-disk-in")
                // The hand that put it in is empty now, and a plain click with it takes the disk back out.
                .thenAssert(0, () -> ctx.player().getMainHandItem().isEmpty(), "the disk left the hand for the bay")
                .then(2, () -> ctx.rightClickFront(HOT_SWAP_AT, bay.middleX(), bay.middleY(), 0))
                .thenWaitUntilServer(level -> hotSwapMachine(ctx, level).front().held(0).isEmpty(), SETTLE * 4,
                        "a click with an empty hand to take the disk out",
                        level -> "held=" + hotSwapMachine(ctx, level).front().held(0))
                .thenServer(0, level -> {
                    final PersonalComputerBlockEntity computer = hotSwapMachine(ctx, level);
                    final ItemStack out = ctx.serverPlayer().getInventory().getItem(0);
                    ctx.assertTrue(out.getItem() instanceof DiskItem, "the disk is back in the hand; got " + out);
                    ctx.assertEquals(KEPT, (int) new LocalStore(List.of(out), () -> { }).count(cobblestone),
                            "with the machine's items on it");
                    ctx.assertEquals(0, (int) computer.localStore().count(cobblestone), "and the machine has none left");
                    ctx.assertTrue(computer.currentBuild() != null && computer.currentBuild().frontDisks().isEmpty(),
                            "and the disk has left the build");
                })
                .thenScreenshot(SETTLE * 4, "hot-swap-disk-out")
                // The disk goes, so the tests after this one start with an empty inventory.
                .thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent());
    }

    /**
     * The parts that stand out of a front are aimed at, outlined and worked as the player sees them: a stick in a
     * Transition case's port, the open tray of the DVD drive in its bay, and the open tray of a CD drive of its own.
     * The crosshair lands on each, out of its block, and a sneak-click takes the stick and the disc, a click with a disc
     * lays it on the tray.
     */
    @ClientTest(timeoutTicks = 900)
    public static void standingParts_areAimedOutlinedAndWorked(final ClientTestContext ctx) {
        final CaseFrontLayout layout = ComputingModule.TRANSITION_PERSONAL_COMPUTER.get().frontLayout();
        final FrontArea stick = layout.stick(0);
        final FrontArea tray = layout.tray(0);
        ctx.thenBuild(0, world -> {
            world.setBlock(STANDING_CASE, ComputingModule.TRANSITION_PERSONAL_COMPUTER.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            final AbstractSmallComputerBlockEntity computer =
                    world.blockEntity(STANDING_CASE, AbstractSmallComputerBlockEntity.class);
            computer.getHardware().setStackInSlot(computer.hardwareLayout().bayStart(),
                    Build.stack("internal_dvd_drive"));
            computer.front().insert(CaseFront.portSlot(0), new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
            world.setBlock(STANDING_DRIVE, DeviceCloseUp.facingPlayer(ComputingModule.CD_DRIVE.get()));
        })
                .thenServer(SETTLE, level -> {
                    ((AbstractSmallComputerBlockEntity) level.getBlockEntity(ctx.abs(STANDING_CASE))).front().tray(0)
                            .press(level, ctx.abs(STANDING_CASE));
                    ((MediaReaderBlockEntity) level.getBlockEntity(ctx.abs(STANDING_DRIVE))).tray()
                            .press(level, ctx.abs(STANDING_DRIVE));
                })
                .thenGive(0)
                .then(1, () -> ctx.selectHotbar(0))
                .thenTeleport(DeviceCloseUp.TRAY_OUT, STANDING_CASE.south(2), Direction.NORTH)
                // The stick in the port, half its length out of the face.
                .then(SETTLE, () -> ctx.lookAtFront(STANDING_CASE, stick.middleX(), stick.middleY(),
                        CaseFrontLayout.STICK_OUT / 2.0))
                .thenWaitUntil(() -> aimedOutOf(ctx, STANDING_CASE), SETTLE * 4,
                        "the crosshair to land on the stick, out of the case")
                .thenScreenshot(SETTLE, "stick-aimed")
                // Crouching lowers the eye, so the stick is aimed at again from there.
                .then(0, () -> ctx.sneak(true))
                .then(SETTLE, () -> ctx.lookAtFront(STANDING_CASE, stick.middleX(), stick.middleY(),
                        CaseFrontLayout.STICK_OUT / 2.0))
                .thenWaitUntil(() -> aimedOutOf(ctx, STANDING_CASE), SETTLE * 4,
                        "the crosshair to stay on the stick from a crouch")
                .then(1, () -> clickCrosshair(ctx))
                .then(1, () -> ctx.sneak(false))
                .thenWaitUntilServer(level -> ((AbstractSmallComputerBlockEntity) level.getBlockEntity(
                                ctx.abs(STANDING_CASE))).front().stick(0).isEmpty(), SETTLE * 4,
                        "a sneak-click on the stick to pull it out", level -> "still in")
                // The DVD drive's open tray, half its length out of the bay.
                .then(SETTLE, () -> ctx.lookAtFront(STANDING_CASE, tray.middleX(), tray.middleY(),
                        CaseFrontLayout.TRAY_OUT / 2.0))
                .thenWaitUntil(() -> aimedOutOf(ctx, STANDING_CASE), SETTLE * 4,
                        "the crosshair to land on the open tray, out of the case")
                .thenScreenshot(SETTLE, "tray-aimed")
                .thenGive(0, new ItemStack(ComputingModule.DVD_RW.get()))
                .then(2, () -> clickCrosshair(ctx))
                .thenWaitUntilServer(level -> ((AbstractSmallComputerBlockEntity) level.getBlockEntity(
                                ctx.abs(STANDING_CASE))).front().held(0).is(ComputingModule.DVD_RW.get()),
                        SETTLE * 4, "a click on the tray with a disc to lay it there", level -> "not on it")
                .thenScreenshot(SETTLE, "tray-with-disc")
                .thenGive(0)
                .then(0, () -> ctx.sneak(true))
                .then(SETTLE, () -> ctx.lookAtFront(STANDING_CASE, tray.middleX(), tray.middleY(),
                        CaseFrontLayout.TRAY_OUT / 2.0))
                .thenWaitUntil(() -> aimedOutOf(ctx, STANDING_CASE), SETTLE * 4,
                        "the crosshair to stay on the tray from a crouch")
                .then(1, () -> clickCrosshair(ctx))
                .then(1, () -> ctx.sneak(false))
                .thenWaitUntilServer(level -> ((AbstractSmallComputerBlockEntity) level.getBlockEntity(
                                ctx.abs(STANDING_CASE))).front().held(0).isEmpty(), SETTLE * 4,
                        "a sneak-click on the tray to lift the disc off", level -> "still on it")
                // Pushed with an empty hand, the open tray slides back in.
                .thenGive(0)
                .then(SETTLE, () -> ctx.lookAtFront(STANDING_CASE, tray.middleX(), tray.middleY(),
                        CaseFrontLayout.TRAY_OUT / 2.0))
                .thenWaitUntil(() -> aimedOutOf(ctx, STANDING_CASE), SETTLE * 4,
                        "the crosshair back on the tray, standing")
                .then(1, () -> clickCrosshair(ctx))
                .thenWaitUntilServer(level -> !((AbstractSmallComputerBlockEntity) level.getBlockEntity(
                                ctx.abs(STANDING_CASE))).front().tray(0).isOpen(), SETTLE * 4,
                        "the tray pushed with an empty hand to slide back in", level -> "still out")
                // A drive of its own: its open tray is aimed at the same way, and pushed in the same way.
                .thenTeleport(SETTLE, STANDING_DRIVE.south(2), Direction.NORTH)
                .then(SETTLE, () -> ctx.lookAtFront(STANDING_DRIVE, 32, 12, 14))
                .thenWaitUntil(() -> aimedOutOf(ctx, STANDING_DRIVE), SETTLE * 4,
                        "the crosshair to land on the CD drive's open tray")
                .thenScreenshot(SETTLE, "drive-tray-aimed")
                .then(1, () -> clickCrosshair(ctx))
                .thenWaitUntilServer(level -> !((MediaReaderBlockEntity) level.getBlockEntity(
                                ctx.abs(STANDING_DRIVE))).tray().isOpen(), SETTLE * 4,
                        "the CD drive's tray pushed in", level -> "still out")
                .thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent());
    }

    /**
     * A disc tray opened with no disc on it, in a computer's bay and on a drive of its own: moulded, with the well a
     * disc lies in, the slot to the back and the spindle's hub, rather than a flat slab.
     */
    @ClientTest(timeoutTicks = 600)
    public static void trays_openEmptyShowTheirWellAndHub(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(TRAY_CASE, ComputingModule.LEGACY_PERSONAL_COMPUTER.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    final AbstractSmallComputerBlockEntity computer =
                            world.blockEntity(TRAY_CASE, AbstractSmallComputerBlockEntity.class);
                    computer.getHardware().setStackInSlot(computer.hardwareLayout().bayStart(),
                            Build.stack("internal_cd_drive"));
                    world.setBlock(TRAY_DRIVE, DeviceCloseUp.facingPlayer(ComputingModule.CD_DRIVE.get()));
                })
                .thenServer(SETTLE, level -> {
                    final AbstractSmallComputerBlockEntity computer = (AbstractSmallComputerBlockEntity)
                            level.getBlockEntity(ctx.abs(TRAY_CASE));
                    computer.front().tray(0).press(level, ctx.abs(TRAY_CASE));
                    ((MediaReaderBlockEntity) level.getBlockEntity(ctx.abs(TRAY_DRIVE))).tray()
                            .press(level, ctx.abs(TRAY_DRIVE));
                });
        DeviceCloseUp.closeUp(ctx, TRAY_CASE)
                .thenScreenshot(DeviceCloseUp.TRAY_OUT, "internal-tray-out-empty");
        DeviceCloseUp.closeUp(ctx, TRAY_DRIVE)
                .thenScreenshot(SETTLE, "drive-tray-out-empty");
    }

    /**
     * The assembly screens of the Transition and Advanced machines, each in the skin of its age: the case says the
     * age, whatever board is in it.
     */
    @ClientTest(timeoutTicks = 900)
    public static void assemblyScreens_wearTheSkinOfTheirAge(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
            for (int i = 0; i < SKINNED.size(); i++) {
                world.setBlock(new BlockPos(i * 2, 2, SKIN_Z), SKINNED.get(i).get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            }
        });
        for (int i = 0; i < SKINNED.size(); i++) {
            final BlockPos at = new BlockPos(i * 2, 2, SKIN_Z);
            ctx.thenTeleport(SETTLE, at.south(2), Direction.NORTH)
                    .thenRightClick(SETTLE, at)
                    .thenScreenshot(SETTLE * 2, SKINNED.get(i).getId().getPath())
                    .then(0, () -> ctx.mc().setScreen(null));
        }
    }

    private static Build build(final String name) {
        return BUILDS.stream().filter(b -> b.name().equals(name)).findFirst().orElseThrow();
    }

    /** Whether the player's game knows the machine there runs. */
    private static boolean runs(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(relative))
                instanceof AbstractSmallComputerBlockEntity computer && computer.visualRunning();
    }

    /* Whether the crosshair is on that block, at a point out of its cube: on a part standing out of it. */
    private static boolean aimedOutOf(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.mc().hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(ctx.abs(relative))
                && DeviceFront.outside(hit.getBlockPos(), hit.getLocation());
    }

    /* A right click on whatever the crosshair is on, the path the player's own click takes. */
    private static void clickCrosshair(final ClientTestContext ctx) {
        if (ctx.mc().hitResult instanceof BlockHitResult hit) {
            Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                    .useItemOn(ctx.player(), InteractionHand.MAIN_HAND, hit);
        }
    }

    /** The machine whose hot-swap bay is worked by hand, as the server knows it. */
    private static PersonalComputerBlockEntity hotSwapMachine(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(HOT_SWAP_AT, PersonalComputerBlockEntity.class);
    }

    /** The front of the machine with the bays, as the player's game knows it. */
    private static CaseFront clientFront(final ClientTestContext ctx) {
        if (ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(BAYS_AT))
                instanceof AbstractSmallComputerBlockEntity computer) {
            return computer.front();
        }
        throw new ClientTestFailure("no machine with bays in the player's game");
    }

    /** Whether the lamp of the CD drive in the first bay was last drawn hidden (or, given false, lit). */
    private static boolean bayLamp(final ClientTestContext ctx, final boolean hidden) {
        if (ctx.mc().level == null || !(ctx.mc().level.getBlockEntity(ctx.abs(BAYS_AT))
                instanceof AbstractSmallComputerBlockEntity computer)) {
            return false;
        }
        return KitAnimations.drawn(computer).map(drawn -> {
            final int lamp = drawn.pose().index(BAY_LAMP);
            return lamp >= 0 && drawn.pose().hides(lamp) == hidden;
        }).orElse(false);
    }

    /** An age's cases, row by row from the floor up. */
    private record Age(String name, List<List<BlockEntry<?>>> rows) {
    }

    /** A case with a part in each of its bays, by item id, and what its bays and ports are then given to hold. */
    private record Front(String name, BlockEntry<?> machine, List<String> bays,
                         BiConsumer<AbstractSmallComputerBlockEntity, ServerLevel> filling) {

        void fill(final AbstractSmallComputerBlockEntity computer, final ServerLevel level) {
            filling.accept(computer, level);
        }
    }

    /**
     * A machine and what is built in it, by item id: the cards in their slots from the top, an empty name an empty
     * slot.
     */
    private record Build(String name, BlockEntry<?> machine, String board, String cpu, List<String> rams,
                         List<String> cards, String psu, List<String> disks) {

        void install(final AbstractSmallComputerBlockEntity computer) {
            final ComputerHardwareLayout layout = computer.hardwareLayout();
            final ItemStackHandler slots = computer.getHardware();
            slots.setStackInSlot(layout.motherboardSlot(), stack(board));
            slots.setStackInSlot(layout.cpuStart(), stack(cpu));
            for (int k = 0; k < rams.size(); k++) {
                slots.setStackInSlot(layout.ramStart() + k, stack(rams.get(k)));
            }
            for (int k = 0; k < cards.size(); k++) {
                slots.setStackInSlot(layout.pcieStart() + k, stack(cards.get(k)));
            }
            slots.setStackInSlot(layout.psuSlot(), stack(psu));
            for (int k = 0; k < disks.size(); k++) {
                slots.setStackInSlot(layout.diskStart() + k, stack(disks.get(k)));
            }
        }

        private static ItemStack stack(final String id) {
            if (id.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id));
            if (item == Items.AIR) {
                throw new IllegalStateException("no item " + id);
            }
            return new ItemStack(item);
        }
    }
}
