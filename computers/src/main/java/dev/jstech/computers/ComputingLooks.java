/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.SupercomputerRackBlock;
import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.blockentity.AsicBlockEntity;
import dev.jstech.computers.blockentity.ChargingDockBlockEntity;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.UpsBlockEntity;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.teracoin.AsicModel;
import dev.jstech.computers.teracoin.Printer3DModel;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.content.GeoLook;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The machines drawn with GeckoLib, family by family: which model each is drawn with, the folder its atlas is in,
 * and the animation the family shares. Each era is a different shape of machine and not a repaint, so a family has a
 * model per era (or, for drives, per drive).
 */
public final class ComputingLooks {

    /** The Mainframe's cabinets, by era; they share the roof fans' and the tape reels' animation. */
    public static final GeoLook<MainframeBlockEntity> MAINFRAME = GeoLook.of(JsComputers.MODID, "mainframe",
            "mainframe", mainframe -> mainframe.getBlockState().getBlock() instanceof MainframeBlock block
                    ? block.cabinetModel() : mainframe(mainframe.cabinetEra()));
    /** The WAN Gateway Computer's cabinets, by era, beside the Mainframe's; they share the roof fans' animation. */
    public static final GeoLook<WanGatewayBlockEntity> WAN_GATEWAY = GeoLook.of(JsComputers.MODID, "mainframe",
            "wan_gateway", gateway -> wanGateway(gateway.cabinetEra()));
    /** The Mining Computer's open frames, by era: the Transition's, the Standard's and the Advanced's. */
    public static final GeoLook<MiningComputerBlockEntity> MINING_COMPUTER = GeoLook.of(JsComputers.MODID,
            "mining_computer", "mining_computer", rig -> miningComputer(rig.rigEra()));
    /** The 3D printers, by model: the Prosa i3 MK3 and the Rattan Lab X1. */
    public static final GeoLook<Printer3DBlockEntity> PRINTER_3D = GeoLook.of(JsComputers.MODID, "printer_3d",
            "printer_3d", printer -> printer3d(printer.model()));
    /** Blockwright's ASICs, by model: the Forge S1 and the Forge A2. */
    public static final GeoLook<AsicBlockEntity> ASIC = GeoLook.of(JsComputers.MODID, "asic", "asic",
            asic -> asic(asic.model()));
    /** The racks: the Supercomputer Rack's own, the Server Rack's by era; they share the roof fans. */
    public static final GeoLook<ServerRackBlockEntity> RACK = GeoLook.of(JsComputers.MODID, "rack", "rack",
            ComputingLooks::rack);
    /** The Pattern Encoder's bodies, by era; they share the medium going in and out. */
    public static final GeoLook<PatternEncoderBlockEntity> PATTERN_ENCODER = GeoLook.of(JsComputers.MODID,
            "pattern_encoder", "pattern_encoder", encoder -> patternEncoder(encoder.era()));
    /** The printers, one model each era; they share one animation file, a clip of the page coming out for each. */
    public static final GeoLook<PrinterBlockEntity> PRINTER = GeoLook.of(JsComputers.MODID, "printer", "printer",
            printer -> printer(printer.model().era()));
    /** A printer's lamps: its power, lit while linked, and its busy lamp; and the sheet coming out. */
    public static final String PRINTER_POWER_LAMP = "lamp_power";
    public static final String PRINTER_BUSY_LAMP = "lamp_busy";
    public static final String PRINTER_PAPER = "paper";
    /**
     * The flatbed scanners, one model each era; they share one animation file, the lid lying closed at rest and, while
     * a page is read, standing open with the light bar running under the glass, a clip of each for each era.
     */
    public static final GeoLook<ScannerBlockEntity> SCANNER = GeoLook.of(JsComputers.MODID, "scanner", "scanner",
            scanner -> scanner(scanner.model().era()));
    /** A scanner's light bar, lit while it reads, its busy lamp, and the stand-in for what lies on the glass. */
    public static final String SCANNER_BAR_LAMP = "lamp_bar";
    public static final String SCANNER_BUSY_LAMP = "lamp_busy";
    public static final String SCANNER_ITEM = "item";
    /** What a scanner's item does not show: its lamps and anything on its glass. */
    public static final List<String> SCANNER_AT_REST = List.of(SCANNER_BAR_LAMP, SCANNER_BUSY_LAMP, SCANNER_ITEM);
    /**
     * The Vinyl RW Drives, one model each era; they share one animation file, the record and the platter turning at
     * thirty-three and a third while the drive records or cuts, a clip for each era.
     */
    public static final GeoLook<VinylDriveBlockEntity> VINYL_DRIVE = GeoLook.of(JsComputers.MODID, "vinyl",
            "vinyl_rw_drive", drive -> vinylDrive(drive.era()));
    /** A drive's record, shown while one lies on its platter, its lamp lit while it works, and its ring of light. */
    public static final String VINYL_RECORD = "record";
    public static final String VINYL_REC_LAMP = "lamp_rec";
    public static final String VINYL_RING_LAMP = "lamp_ring";
    /** What a drive's item does not show: a record, and its lamps. */
    public static final List<String> VINYL_DRIVE_AT_REST = List.of(VINYL_RECORD, VINYL_REC_LAMP, VINYL_RING_LAMP);
    /** The Extended CD Drives, a tower two blocks tall each era; a lamp for the reading tray and each writing tray. */
    public static final GeoLook<ExtendedCdDriveBlockEntity> EXTENDED_CD_DRIVE = GeoLook.of(JsComputers.MODID,
            "disc_tower", "extended_cd_drive", tower -> extendedCdDrive(tower.era()));
    /** A tower's lamps: the reading tray's, then the seven writing trays'. */
    public static final List<String> EXTENDED_CD_DRIVE_LAMPS = List.of("lamp_0", "lamp_1", "lamp_2", "lamp_3",
            "lamp_4", "lamp_5", "lamp_6", "lamp_7");
    /** What a tower's item does not show: its lamps. */
    public static final List<String> EXTENDED_CD_DRIVE_AT_REST = EXTENDED_CD_DRIVE_LAMPS;
    /** The Dock Station: one model, its trays marking where each disk is drawn, no clips. */
    public static final GeoLook<DockStationBlockEntity> DOCK_STATION = GeoLook.of(JsComputers.MODID, "dock",
            "dock_station", dock -> "dock_station");
    /**
     * The Dock Station's power lamp, each tray's lamp, and the place in its USB port where a stick plugged in is drawn,
     * as the stick's own item, the way the Pattern Encoder draws it.
     */
    public static final String DOCK_POWER_LAMP = "lamp_power";
    public static final List<String> DOCK_BAY_LAMPS = List.of("lamp_bay_hdd", "lamp_bay_ssd", "lamp_bay_nvme");
    public static final List<String> DOCK_BAYS = List.of("bay_hdd", "bay_ssd", "bay_nvme");
    /** What the Dock's item does not show: its lamps. */
    public static final List<String> DOCK_AT_REST = List.of(DOCK_POWER_LAMP, "lamp_bay_hdd", "lamp_bay_ssd",
            "lamp_bay_nvme");
    /** The drives, one model each: a floppy drive, a CD drive and a DVD drive are different machines. */
    public static final GeoLook<MediaReaderBlockEntity> MEDIA_DRIVE = GeoLook.of(JsComputers.MODID, "media_drive",
            "media_drive", drive -> drive.driveType().serializedName());
    /** The Redstone Interfaces, one model each era: the sensor of its day, its housing and the bezel of its lens. */
    public static final GeoLook<RedstoneInterfaceBlockEntity> REDSTONE_INTERFACE = GeoLook.of(JsComputers.MODID,
            "redstone", "redstone_interface", sensor -> redstoneInterface(sensor.era()));
    /** A Redstone Interface's lens at each of the three brightnesses it is drawn in, and its two mode lamps. */
    public static final String REDSTONE_LENS_DARK = "lens_0";
    public static final String REDSTONE_LENS_HALF = "lens_8";
    public static final String REDSTONE_LENS_FULL = "lens_15";
    public static final String REDSTONE_LAMP_IN = "mode_in";
    public static final String REDSTONE_LAMP_OUT = "mode_out";
    /** The two runners a Redstone Interface stands on, drawn only while it faces one of the four sides. */
    public static final String REDSTONE_FEET = "feet";
    /** The MF PowerGuards, one model each era: the UPS of its day, its screen, button and lamps on its front. */
    public static final GeoLook<UpsBlockEntity> UPS = GeoLook.of(JsComputers.MODID, "ups", "ups",
            ups -> ups(ups.era()));
    /** A UPS's three lamps: the line, the battery, and the battery to be replaced. */
    public static final String UPS_LINE_LAMP = "lamp_line";
    public static final String UPS_BATTERY_LAMP = "lamp_battery";
    public static final String UPS_REPLACE_LAMP = "lamp_replace";
    /**
     * The Wireless Access Points, one model each era and, for the Standard's and the Advanced's, each place they are
     * fixed: on a ceiling or on a wall. They share no clip.
     */
    public static final GeoLook<AccessPointBlockEntity> ACCESS_POINT = GeoLook.of(JsComputers.MODID, "wireless",
            "access_point", point -> accessPoint(point.getBlockState()));
    /** A desk access point's three lamps: its power, its Wi-Fi and its network. */
    public static final String ACCESS_POINT_POWER_LAMP = "lamp_power";
    public static final String ACCESS_POINT_WLAN_LAMP = "lamp_wlan";
    public static final String ACCESS_POINT_LAN_LAMP = "lamp_lan";
    /**
     * What a desk access point's data hookups are named from, {@code hookup_data_<column>_<row>}, its data cord in its
     * era's access cable's colour; its energy's are named as a computer's ({@link #COMPUTER_HOOKUP}).
     */
    public static final String ACCESS_POINT_DATA_HOOKUP = "hookup_data_";
    /**
     * The Electronics Workbenches, one model each era: the bench of an electronics workshop of its day, its top, its
     * drawers, its legs and its era's tools on it. Their energy hookups are named as a computer's
     * ({@link #COMPUTER_HOOKUP}); they share no clip.
     */
    public static final GeoLook<WorkbenchBlockEntity> WORKBENCH = GeoLook.of(JsComputers.MODID, "chain",
            "workbench", bench -> workbench(bench.era()));
    /**
     * The Mobile Device Charging Docks, one model each era: the PDA's cradle, the white base, the aluminium stand, the
     * wireless stand. Their energy hookups are named as a computer's ({@link #COMPUTER_HOOKUP}); they share no clip.
     */
    public static final GeoLook<ChargingDockBlockEntity> CHARGING_DOCK = GeoLook.of(JsComputers.MODID, "wireless",
            "charging_dock", dock -> chargingDock(dock.era()));
    /** A dock's lamp while it charges, and the Legacy cradle's second, lit when the battery is full. */
    public static final String CHARGING_DOCK_LAMP = "lamp_charge";
    public static final String CHARGING_DOCK_FULL_LAMP = "lamp_full";
    /** The twelve portables, a model each, drawn in a hand and standing in a dock; they play no clip. */
    public static final GeoLook<PortableModel> PORTABLE = GeoLook.of(JsComputers.MODID, "portable", "portable",
            PortableModel::serializedName);
    /**
     * The small computers' cases, a model for each machine in each case: the tower of each age, and from the Standard
     * age on the three cases a machine comes in. They share the fans' turning.
     */
    public static final GeoLook<BlockEntity> COMPUTER = GeoLook.of(JsComputers.MODID, "computer", "computer",
            computer -> computer(computer.getBlockState()));
    /** The lamps on a small computer's front: its power and its disk. */
    public static final String COMPUTER_POWER_LAMP = "led_power";
    public static final String COMPUTER_DISK_LAMP = "led_disk";
    /** A small computer's left side, the panel that comes off. */
    public static final String COMPUTER_SIDE_PANEL = "side_panel";
    /**
     * What every hookup on the back of a computer that takes its cables only from behind is named from:
     * {@code hookup_<column>_<row>}, the socket and the cord shown for a line plugged in at that lane of the cable
     * block.
     */
    public static final String COMPUTER_HOOKUP = "hookup_";
    /**
     * What every part a small computer's bay can hold is named from: {@code drive_<bay>_<part>}, with its tray, its
     * disc and its lamp under it. The case as it comes holds none, and shows each bay's cover.
     */
    public static final String COMPUTER_BAY_PARTS = "drive_";

    /** The case a block that is not a small computer's falls back to, so a stray state still draws something. */
    private static final String FALLBACK_CASE = "computer_standard_neutral_personal_computer";
    /* Each era's model of each family; an era after the Advanced is drawn as the Advanced is, until it has its own. */
    private static final EraTable<String> MAINFRAMES = EraTable.ofFive("vintage_mainframe", "legacy_mainframe",
            "transition_mainframe", "mainframe", "advanced_mainframe");
    private static final EraTable<String> WAN_GATEWAYS = EraTable.ofFive("vintage_wan_gateway",
            "legacy_wan_gateway", "transition_wan_gateway", "wan_gateway", "advanced_wan_gateway");
    private static final EraTable<String> MINING_COMPUTERS = EraTable.of("mining_computer_transition")
            .from(HardwareEra.STANDARD, "mining_computer_standard")
            .from(HardwareEra.ADVANCED, "mining_computer_advanced");
    private static final EraTable<String> PATTERN_ENCODERS = EraTable.ofFive("vintage_pattern_encoder",
            "legacy_pattern_encoder", "transition_pattern_encoder", "pattern_encoder", "advanced_pattern_encoder");
    private static final EraTable<String> PRINTERS = EraTable.ofFive("vintage_printer", "legacy_printer",
            "transition_printer", "printer", "advanced_printer");
    private static final EraTable<String> SCANNERS = EraTable.ofFive("vintage_scanner", "legacy_scanner",
            "transition_scanner", "scanner", "advanced_scanner");
    private static final EraTable<String> VINYL_DRIVES = EraTable.ofFive("vintage_vinyl_rw_drive",
            "legacy_vinyl_rw_drive", "transition_vinyl_rw_drive", "vinyl_rw_drive", "advanced_vinyl_rw_drive");
    private static final EraTable<String> SERVER_RACKS = EraTable.ofFive("vintage_server_rack",
            "legacy_server_rack", "transition_server_rack", "server_rack", "advanced_server_rack");
    private static final EraTable<String> SUPERCOMPUTER_RACKS = EraTable.of("supercomputer_rack")
            .from(HardwareEra.ADVANCED, "advanced_supercomputer_rack");

    private ComputingLooks() {
    }

    /** The model of the small computer whose block is in {@code state}. */
    public static String computer(final BlockState state) {
        return state.getBlock() instanceof IComputerCase computer ? computer.caseModel() : FALLBACK_CASE;
    }

    /** The model of a Redstone Interface of {@code era}; an era after the Advanced has the Advanced's. */
    public static String redstoneInterface(final Era era) {
        return HardwareEra.withParts(era).path() + "_redstone_interface";
    }

    /** The model of an MF PowerGuard of {@code era}; an era after the Advanced has the Advanced's. */
    public static String ups(final Era era) {
        return HardwareEra.withParts(era).path() + "_ups";
    }

    /** The model of the access point in {@code state}: its era's, and where a fixed one is fixed. */
    public static String accessPoint(final BlockState state) {
        if (!(state.getBlock() instanceof AccessPointBlock block)) {
            return accessPoint(HardwareEra.LEGACY, AccessPointMount.DESK);
        }
        return accessPoint(block.era(), state.getValue(AccessPointBlock.MOUNT));
    }

    /** The model of the access point of {@code era} fixed as {@code mount}. */
    public static String accessPoint(final Era era, final AccessPointMount mount) {
        final WirelessStandard standard = WirelessStandard.of(era);
        final String name = "access_point_" + (standard == null ? WirelessStandard.LEGACY : standard).serializedName();
        return mount == AccessPointMount.DESK ? name : name + "_" + mount.getSerializedName();
    }

    /** What a desk access point's item does not show: its lamps and its hookups. */
    public static List<String> accessPointAtRest() {
        final List<String> out = new ArrayList<>(List.of(ACCESS_POINT_POWER_LAMP, ACCESS_POINT_WLAN_LAMP,
                ACCESS_POINT_LAN_LAMP));
        for (final String prefix : List.of(COMPUTER_HOOKUP, ACCESS_POINT_DATA_HOOKUP)) {
            hookups(prefix, out);
        }
        return List.copyOf(out);
    }

    /** The model of the Electronics Workbench of {@code era}; an era after the Advanced has the Advanced's. */
    public static String workbench(final Era era) {
        return "workbench_" + HardwareEra.withParts(era).path();
    }

    /** What a bench's item does not show: its hookups. */
    public static List<String> workbenchAtRest() {
        final List<String> out = new ArrayList<>();
        hookups(COMPUTER_HOOKUP, out);
        return List.copyOf(out);
    }

    /** The model of the Mobile Device Charging Dock of {@code era}; an era after the Advanced has the Advanced's. */
    public static String chargingDock(final Era era) {
        return "charging_dock_" + HardwareEra.withParts(era).path();
    }

    /** What a dock's item does not show: its lamps and its hookups. */
    public static List<String> chargingDockAtRest() {
        final List<String> out = new ArrayList<>(List.of(CHARGING_DOCK_LAMP, CHARGING_DOCK_FULL_LAMP));
        hookups(COMPUTER_HOOKUP, out);
        return List.copyOf(out);
    }

    /** The model of a Mainframe cabinet of {@code era}. */
    public static String mainframe(final Era era) {
        return MAINFRAMES.get(era);
    }

    /** The model of a WAN Gateway Computer's cabinet of {@code era}. */
    public static String wanGateway(final Era era) {
        return WAN_GATEWAYS.get(era);
    }

    /** The model of a 3D printer of {@code model}: {@code prosa_i3_mk3}. */
    public static String printer3d(final Printer3DModel model) {
        return model.id();
    }

    /** The model of an ASIC of {@code model}: {@code asic_forge_s1}. */
    public static String asic(final AsicModel model) {
        return "asic_" + model.id();
    }

    /** The model of a Mining Computer of {@code era}: the frame of its era, from the Transition on. */
    public static String miningComputer(final Era era) {
        return MINING_COMPUTERS.get(era);
    }

    /** The model of a Pattern Encoder body of {@code era}. */
    public static String patternEncoder(final Era era) {
        return PATTERN_ENCODERS.get(era);
    }

    /** The model of the printer of {@code era}: the Standard one has no era in its name, as its block has none. */
    public static String printer(final Era era) {
        return PRINTERS.get(era);
    }

    /** The model of the scanner of {@code era}: the Standard one has no era in its name, as its block has none. */
    public static String scanner(final Era era) {
        return SCANNERS.get(era);
    }

    /** The model of the Vinyl RW Drive of {@code era}: the Standard one has no era in its name. */
    public static String vinylDrive(final Era era) {
        return VINYL_DRIVES.get(era);
    }

    /** The model of the Extended CD Drive of {@code era}: the Legacy's CD tower, the Transition's, the Advanced's. */
    public static String extendedCdDrive(final Era era) {
        final Era parts = HardwareEra.withParts(era);
        if (parts == HardwareEra.ADVANCED) {
            return "advanced_extended_cd_drive";
        }
        return parts == HardwareEra.TRANSITION || parts == HardwareEra.STANDARD ? "transition_extended_cd_drive"
                : "legacy_extended_cd_drive";
    }

    /** The model of a rack's cabinet: by kind first, then by era. */
    public static String rack(final ServerRackBlockEntity rack) {
        return (rack.getBlockState().getBlock() instanceof SupercomputerRackBlock ? SUPERCOMPUTER_RACKS : SERVER_RACKS)
                .get(rack.rackEra());
    }

    /* Adds the nine hookups named from {@code prefix}, one for each lane of the cable block, to {@code out}. */
    private static void hookups(final String prefix, final List<String> out) {
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) {
                out.add(prefix + column + "_" + row);
            }
        }
    }
}
