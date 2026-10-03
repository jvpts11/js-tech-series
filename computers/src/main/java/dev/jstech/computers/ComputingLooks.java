/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.block.SupercomputerRackBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.content.GeoLook;
import dev.jstech.core.tier.HardwareEra;
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
            "mainframe", mainframe -> mainframe(mainframe.mainframeEra()));
    /** The racks: the Supercomputer Rack's own, the Server Rack's by era; they share the roof fans. */
    public static final GeoLook<ServerRackBlockEntity> RACK = GeoLook.of(JsComputers.MODID, "rack", "rack",
            ComputingLooks::rack);
    /** The Pattern Encoder's bodies, by era; they share the medium going in and out. */
    public static final GeoLook<PatternEncoderBlockEntity> PATTERN_ENCODER = GeoLook.of(JsComputers.MODID,
            "pattern_encoder", "pattern_encoder", encoder -> patternEncoder(encoder.era()));
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

    /** The case a block that is not a small computer's falls back to, so a stray state still draws something. */
    private static final String FALLBACK_CASE = "computer_standard_neutral_personal_computer";

    private ComputingLooks() {
    }

    /** The model of the small computer whose block is in {@code state}. */
    public static String computer(final BlockState state) {
        return state.getBlock() instanceof IComputerCase computer ? computer.caseModel() : FALLBACK_CASE;
    }

    /** The model of a Redstone Interface of {@code era}; an era after the Advanced has the Advanced's. */
    public static String redstoneInterface(final HardwareEra era) {
        final HardwareEra drawn = era.level() > HardwareEra.ADVANCED.level() ? HardwareEra.ADVANCED : era;
        return drawn.serializedName() + "_redstone_interface";
    }

    /** The model of a Mainframe cabinet of {@code era}. */
    public static String mainframe(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> "vintage_mainframe";
            case LEGACY -> "legacy_mainframe";
            case TRANSITION -> "transition_mainframe";
            case ADVANCED -> "advanced_mainframe";
            default -> "mainframe";
        };
    }

    /** The model of a Pattern Encoder body of {@code era}. */
    public static String patternEncoder(final HardwareEra era) {
        if (era == HardwareEra.VINTAGE) {
            return "vintage_pattern_encoder";
        }
        if (era == HardwareEra.LEGACY) {
            return "legacy_pattern_encoder";
        }
        if (era == HardwareEra.TRANSITION) {
            return "transition_pattern_encoder";
        }
        // An era after the Advanced has the Advanced's.
        return era.level() >= HardwareEra.ADVANCED.level() ? "advanced_pattern_encoder" : "pattern_encoder";
    }

    /** The model of a rack's cabinet: by kind first, then by era. */
    public static String rack(final ServerRackBlockEntity rack) {
        if (rack.getBlockState().getBlock() instanceof SupercomputerRackBlock) {
            return rack.rackEra() == HardwareEra.ADVANCED ? "advanced_supercomputer_rack" : "supercomputer_rack";
        }
        return switch (rack.rackEra()) {
            case VINTAGE -> "vintage_server_rack";
            case LEGACY -> "legacy_server_rack";
            case TRANSITION -> "transition_server_rack";
            case ADVANCED -> "advanced_server_rack";
            default -> "server_rack";
        };
    }
}
