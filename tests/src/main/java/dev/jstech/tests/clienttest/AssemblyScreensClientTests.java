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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.ClusterManagementComputerScreen;
import dev.jstech.computers.client.CraftingComputerScreen;
import dev.jstech.computers.client.MainframeScreen;
import dev.jstech.computers.client.PersonalComputerScreen;
import dev.jstech.computers.client.ServerAssemblyScreen;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.tests.testkit.ServerStacks;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * The assembly screens and the rack screen, each drawn as its machine. Every tower in the case of its age with a
 * machine built in it, bays and all, and one with nothing in it showing the shape of each part where it goes; the
 * Mainframe running in its cabinet; a held server's chassis; and a rack holding a named server, a storage server
 * running an array, a KVM Switch, a server short of its processor and an empty unit, whose card follows the unit
 * clicked and whose power buttons switch their bays.
 */
public final class AssemblyScreensClientTests {

    private static final int SETTLE = 6;
    private static final int SCREEN_WAIT = 60;
    /** How long the Mainframe may take to be known running once switched on. */
    private static final int RUNS_WITHIN = 100;
    /** The towers stand in a row facing south, two blocks apart. */
    private static final int TOWER_Z = 4;
    private static final BlockPos MAINFRAME = new BlockPos(2, 2, 12);
    private static final BlockPos RACK = new BlockPos(2, 2, 20);
    /** The rack's units: a server, a storage server over two units, a KVM Switch, and a server with no processor. */
    private static final int SERVER_ROW = 0;
    private static final int STORAGE_ROW = 1;
    private static final int KVM_ROW = 3;
    private static final int UNBUILT_ROW = 4;
    /** The storage chassis cables eight drive bays and then its two gadget bays: its first gadget bay is its ninth. */
    private static final int STORAGE_CONTROLLER = STORAGE_ROW * RackLayout.SLOTS_PER_U + 8;

    /** One tower of each kind and age, built as a player would build it, and one empty. */
    private static final List<Tower> TOWERS = List.of(
            new Tower("pc-vintage", ComputingModule.VINTAGE_PERSONAL_COMPUTER, PersonalComputerScreen.class,
                    "motherboard_babyat_vintage", "cpu_integra_486dx2",
                    List.of("ram_simm_4", "ram_simm_4", "ram_simm_4", "ram_simm_4"),
                    List.of("gpu_vga_256", "sound_card_tone_blaster"), "psu_200", List.of("disk_vaultis_trench_20m"),
                    List.of("hot_swap_drive_bay")),
            new Tower("pc-legacy", ComputingModule.LEGACY_PERSONAL_COMPUTER, PersonalComputerScreen.class,
                    "motherboard_atx_legacy_939", "cpu_velocion_sprint_64_fx_55", List.of("ram_ddr_1024", "ram_ddr_1024"),
                    List.of("gpu_radiance_x800_xt", "sound_card_tone_blaster_audigy"), "psu_500b",
                    List.of("disk_vaultis_link_ide_40g"), List.of("internal_cd_drive", "hot_swap_drive_bay")),
            new Tower("pc-transition", ComputingModule.TRANSITION_PERSONAL_COMPUTER, PersonalComputerScreen.class,
                    "motherboard_atx_transition_775", "cpu_integra_centro_2_duo_e6600",
                    List.of("ram_ddr2_2048", "ram_ddr2_2048"),
                    List.of("gpu_vertex_8800_gt", "sound_card_tone_blaster_hi_fi"), "psu_450b", List.of("disk_hdd_500g"),
                    List.of("internal_dvd_drive", "internal_cd_drive")),
            new Tower("pc-standard", ComputingModule.PERSONAL_COMPUTER, PersonalComputerScreen.class,
                    "motherboard_atx_standard_1155", "cpu_integra_centro_c5_2500k", List.of("ram_ddr3_8192", "ram_ddr3_8192"),
                    List.of("gpu_vertex_gtx_660"), "psu_850g", List.of("disk_ssd_500g", "disk_hdd_1t"),
                    List.of("internal_dvd_drive")),
            new Tower("pc-advanced", ComputingModule.ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                    PersonalComputerScreen.class, "motherboard_atx_advanced_1700", "cpu_integra_centro_c9_13900k",
                    List.of("ram_ddr5_32768", "ram_ddr5_32768", "ram_ddr5_32768", "ram_ddr5_32768"),
                    List.of("gpu_vertex_rtx_4090"), "psu_1200p", List.of("disk_nvme_8t", "disk_hdd_24t"), List.of()),
            new Tower("cc-legacy", ComputingModule.LEGACY_CRAFTING_COMPUTER, CraftingComputerScreen.class,
                    "motherboard_atx_legacy_478", "cpu_integra_pentix_4_2_4c", List.of("ram_ddr_256", "ram_ddr_256"),
                    List.of("gpu_radiance_9200_se", "crafting_card_t2"), "psu_350", List.of("disk_vaultis_link_ide_20g"),
                    List.of("internal_cd_drive")),
            new Tower("cc-advanced", ComputingModule.ADVANCED_CRAFTING_COMPUTER, CraftingComputerScreen.class,
                    "motherboard_atx_advanced_am4", "cpu_velocion_awayken_3_3200g", List.of("ram_ddr4_8192", "ram_ddr4_8192"),
                    List.of("", "crafting_card_t4"), "psu_1000g", List.of("disk_nvme_8t"), List.of()),
            new Tower("cmc-vintage", ComputingModule.VINTAGE_CLUSTER_MANAGEMENT_COMPUTER,
                    ClusterManagementComputerScreen.class, "motherboard_at_vintage", "cpu_velocion_k6_iii",
                    List.of("ram_simm_16", "ram_simm_16", "ram_simm_16", "ram_simm_16"),
                    List.of("gpu_3d_blaster", "serial_console_card"), "psu_300", List.of("disk_vaultis_trench_200m"),
                    List.of()),
            new Tower("cmc-standard", ComputingModule.HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER,
                    ClusterManagementComputerScreen.class, "motherboard_atx_standard_lga1150", "cpu_integra_servo_1231_v3",
                    List.of("ram_ddr3_8192", "ram_ddr3_8192", "ram_ddr3_8192", "ram_ddr3_8192"),
                    List.of("gpu_vertex_gt_730", "fabric_host_adapter"), "psu_850g", List.of("disk_hdd_16t"),
                    List.of("internal_dvd_drive")),
            new Tower("pc-standard-empty", ComputingModule.PERSONAL_COMPUTER, PersonalComputerScreen.class, "", "",
                    List.of(), List.of(), "", List.of(), List.of()));

    private AssemblyScreensClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void towerScreens_drawTheCaseOfTheirAgeWithEveryPartInItsSeat(final ClientTestContext ctx) {
        // An empty hand opens a machine; one holding something would use it on the machine instead.
        ctx.thenGive(0).thenBuild(0, world -> {
            for (int i = 0; i < TOWERS.size(); i++) {
                final BlockPos at = towerAt(i);
                world.setBlock(at, TOWERS.get(i).machine().get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                TOWERS.get(i).install(world.blockEntity(at, AbstractSmallComputerBlockEntity.class));
            }
        });
        for (int i = 0; i < TOWERS.size(); i++) {
            open(ctx, towerAt(i), TOWERS.get(i).screen(), TOWERS.get(i).name());
        }
    }

    @ClientTest(timeoutTicks = 900)
    public static void mainframeAndServerScreens_drawTheirCabinets(final ClientTestContext ctx) {
        ctx.thenGive(0).thenBuild(0, world -> {
            world.setBlock(MAINFRAME, ComputingModule.MAINFRAME.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            final ItemStackHandler slots = world.blockEntity(MAINFRAME, MainframeBlockEntity.class).getHardware();
            slots.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                    new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
            slots.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
            slots.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START + 1,
                    new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
            for (int i = 0; i < 4; i++) {
                slots.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START + i,
                        new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
            }
            slots.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
            slots.setStackInSlot(MainframeBlockEntity.PSU_SLOT, stack("psu_2000p"));
            slots.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                    new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
            world.blockEntity(MAINFRAME, MainframeBlockEntity.class).togglePower();
        }).thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(MAINFRAME))
                        instanceof MainframeBlockEntity mainframe && mainframe.visualRunning(), RUNS_WITHIN,
                "the Mainframe to run, as the player sees it");
        ctx.thenTeleport(SETTLE, MAINFRAME.south(3), Direction.NORTH)
                .thenRightClick(SETTLE, MAINFRAME)
                .thenAwaitScreen(MainframeScreen.class, SCREEN_WAIT)
                .thenScreenshot(SETTLE, "mainframe")
                .thenAssert(0, () -> ctx.openScreen(MainframeScreen.class) != null,
                        "the Mainframe's screen must open and stay")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
        // A server is assembled in the hand: using it opens its chassis.
        final ItemStack server = ServerStacks.defaultServer();
        ServerItem.setCustomName(server, "WEB-01");
        ctx.thenGive(0, server)
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .then(2, () -> Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .useItem(ctx.player(), InteractionHand.MAIN_HAND))
                .thenAwaitScreen(ServerAssemblyScreen.class, SCREEN_WAIT)
                .thenScreenshot(SETTLE, "server")
                .thenAssert(0, () -> ctx.openScreen(ServerAssemblyScreen.class) != null,
                        "the server's screen must open and stay")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    @ClientTest(timeoutTicks = 900)
    public static void rackScreen_drawsTheCabinetItsCardFollowsTheClickAndItsButtonsSwitchTheBays(
            final ClientTestContext ctx) {
        // An empty hand: a rack clicked with a server in it would mount the server instead of opening.
        ctx.thenGive(0).thenBuild(0, world -> {
            world.setBlock(RACK, ComputingModule.SERVER_RACK.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            final ServerRackBlockEntity rack = world.blockEntity(RACK, ServerRackBlockEntity.class);
            final ItemStack server = ServerStacks.defaultServer();
            ServerItem.setCustomName(server, "WEB-01");
            rack.getServers().setStackInSlot(SERVER_ROW, server);
            rack.insertDrive(SERVER_ROW, nvme());
            rack.insertDrive(SERVER_ROW, nvme());
            final ItemStack storage = ServerStacks.defaultStorageServer();
            ServerItem.setCustomName(storage, "VAULT");
            rack.getServers().setStackInSlot(STORAGE_ROW, storage);
            for (int i = 0; i < 3; i++) {
                rack.insertDrive(STORAGE_ROW, nvme());
            }
            rack.getFrontSlots().setStackInSlot(STORAGE_CONTROLLER, new ItemStack(ComputingModule.RAID_CONTROLLER.get()));
            rack.setRaidMode(STORAGE_ROW, RaidMode.RAID5);
            rack.getServers().setStackInSlot(KVM_ROW, new ItemStack(ComputingModule.KVM_SWITCH.get()));
            rack.getServers().setStackInSlot(UNBUILT_ROW, ServerStacks.cpulessServer());
            rack.toggleBayPower(UNBUILT_ROW);
        });
        ctx.thenTeleport(SETTLE, RACK.south(2), Direction.NORTH)
                .thenRightClick(SETTLE, RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                .thenScreenshot(SETTLE, "rack")
                // A click on the storage server's faceplate, clear of its bays, puts it on the card.
                .then(0, () -> ctx.clickGui(ServerRackLayout.PWR_X - 8, ServerRackLayout.rowY(STORAGE_ROW) + 10))
                .thenScreenshot(SETTLE, "rack-storage-on-the-card")
                .then(0, () -> ctx.clickGui(ServerRackLayout.PWR_X + ServerRackLayout.PWR_W / 2,
                        ServerRackLayout.pwrY(SERVER_ROW, 1) + ServerRackLayout.PWR_H / 2))
                .thenWaitUntilServer(level -> !rack(ctx, level).bayPowerOn(SERVER_ROW), SCREEN_WAIT,
                        "the server's power button to switch its bay off",
                        level -> "on=" + rack(ctx, level).bayPowerOn(SERVER_ROW))
                .thenScreenshot(SETTLE, "rack-server-switched-off")
                .thenAssert(0, () -> ctx.openScreen(ServerRackScreen.class) != null, "the rack's screen must stay open")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** Right-clicks the machine at {@code at}, waits for its screen, shoots it, checks it stays, and closes it. */
    private static void open(final ClientTestContext ctx, final BlockPos at, final Class<? extends Screen> screen,
                             final String label) {
        ctx.thenTeleport(SETTLE, at.south(2), Direction.NORTH)
                .thenRightClick(SETTLE, at)
                .thenAwaitScreen(screen, SCREEN_WAIT)
                .thenScreenshot(SETTLE, label)
                .thenAssert(0, () -> ctx.openScreen(screen) != null, label + " must open and stay")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    private static BlockPos towerAt(final int index) {
        return new BlockPos(index * 2, 2, TOWER_Z);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx,
                                              final net.minecraft.server.level.ServerLevel level) {
        return (ServerRackBlockEntity) level.getBlockEntity(ctx.abs(RACK));
    }

    private static ItemStack nvme() {
        return new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1));
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

    /**
     * A tower and what is built in it, by item id: the cards in their slots from the top, an empty name an empty slot,
     * and the parts in its 5.25-inch bays.
     */
    private record Tower(String name, BlockEntry<?> machine, Class<? extends Screen> screen, String board, String cpu,
                         List<String> rams, List<String> cards, String psu, List<String> disks, List<String> bays) {

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
            for (int k = 0; k < bays.size(); k++) {
                slots.setStackInSlot(layout.bayStart() + k, stack(bays.get(k)));
            }
        }
    }
}
