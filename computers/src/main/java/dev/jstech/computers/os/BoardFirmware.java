/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

/**
 * The firmware on a motherboard (computers/docs/design/survival.md): a board carries the firmware of its era, the text
 * BIOS of the Vintage, the blue BIOS of the Legacy and the Transition, UEFI from the Standard; one that came out of the
 * chain is blank until a Preinstaller writes it. A computer with a blank board powers its fans and nothing more.
 */
@TextHolder
public final class BoardFirmware {

    /** A part's firmware line, under its board: "Firmware: UEFI". */
    public static final TextKey FIRMWARE = TextKey.of("jsc.firmware.line", "Firmware: %s");
    /** What a blank board carries. */
    public static final TextKey NONE = TextKey.of("jsc.firmware.none", "none");
    /** What a blank board means, and where its firmware comes from. */
    public static final TextKey BLANK = TextKey.of("jsc.firmware.blank", "No firmware on this board");
    public static final TextKey FANS_ONLY = TextKey.of("jsc.firmware.fans_only", "The fans turn and nothing more");
    public static final TextKey WRITTEN_BY = TextKey.of("jsc.firmware.written_by", "A Preinstaller writes it");
    private static final TextKey TEXT_BIOS = TextKey.of("jsc.firmware.text_bios", "Text BIOS");
    private static final TextKey BLUE_BIOS = TextKey.of("jsc.firmware.blue_bios", "Blue BIOS");
    private static final TextKey UEFI = TextKey.of("jsc.firmware.uefi", "UEFI");

    private BoardFirmware() {
    }

    /** Whether {@code board} is a motherboard with no firmware on it. */
    public static boolean blank(final ItemStack board) {
        return !board.isEmpty() && board.has(ComputingComponents.NO_FIRMWARE.get());
    }

    /** {@code board} as it comes out of the chain, with no firmware on it. */
    public static ItemStack blanked(final ItemStack board) {
        final ItemStack out = board.copy();
        out.set(ComputingComponents.NO_FIRMWARE.get(), Unit.INSTANCE);
        return out;
    }

    /** {@code board} with the firmware of its era written on it. */
    public static ItemStack written(final ItemStack board) {
        final ItemStack out = board.copy();
        out.remove(ComputingComponents.NO_FIRMWARE.get());
        return out;
    }

    /** The firmware on {@code board}: its era's, or none on a blank board or on a stack that is no motherboard. */
    public static Optional<FirmwareKind> of(final ItemStack board) {
        final MotherboardSpec spec = HardwareRegistry.board(board);
        return spec == null || blank(board) ? Optional.empty() : Optional.of(FirmwareKind.forEra(spec.era()));
    }

    /** The name of {@code kind}, as a board's tooltip says it. */
    public static TextKey name(final FirmwareKind kind) {
        return switch (kind) {
            case CLI_BIOS -> TEXT_BIOS;
            case BLUE_BIOS -> BLUE_BIOS;
            case UEFI -> UEFI;
        };
    }

    /** The firmware line of {@code board}: its firmware's name, or none for a blank board. */
    public static Text line(final ItemStack board) {
        return FIRMWARE.with(of(board).map(BoardFirmware::name).orElse(NONE));
    }
}
