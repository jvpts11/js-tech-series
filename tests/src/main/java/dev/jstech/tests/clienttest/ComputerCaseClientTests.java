/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.core.content.BlockEntry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
                    List.of("gpu_radiance_x800_xt", "sound_card_tone_blaster_live"), "psu_500b",
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
                    List.of("gpu_vertex_gt_730", "fabric_host_adapter"), "psu_850g", List.of("disk_hdd_8t")),
            new Build("advanced-neutral", ComputingModule.ADVANCED_PERSONAL_COMPUTER,
                    "motherboard_atx_advanced_1151", "cpu_integra_centro_c9_9900k",
                    List.of("ram_ddr4_16384", "ram_ddr4_16384"), List.of("gpu_vertex_gtx_1080_ti"), "psu_1000g",
                    List.of("disk_ssd_8t")),
            new Build("advanced-high-performance", ComputingModule.ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                    "motherboard_atx_advanced_1700", "cpu_integra_centro_c9_13900k",
                    List.of("ram_ddr5_32768", "ram_ddr5_32768", "ram_ddr5_32768", "ram_ddr5_32768"),
                    List.of("gpu_vertex_rtx_4090"), "psu_1200p", List.of("disk_nvme_8t")),
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
     * cooler on the processor (none on a cartridge that brings its own). The last two machines hold parts of other
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

    /** An age's cases, row by row from the floor up. */
    private record Age(String name, List<List<BlockEntry<?>>> rows) {
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
