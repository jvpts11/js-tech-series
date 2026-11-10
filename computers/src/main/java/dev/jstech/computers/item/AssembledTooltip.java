/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CaseFanSpec;
import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.os.BoardFirmware;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/**
 * The tooltip of a computer carried as an item with its parts inside (computers/docs/design/survival.md): each part
 * under its kind, the firmware under the board and the system under the disk, and what it still lacks before it
 * starts: a Preinstaller, or its first start, which runs its self-test.
 */
@TextHolder
public final class AssembledTooltip {

    private static final TextKey BOARD = TextKey.of("jsc.assembled.board", "Board: %s");
    private static final TextKey CPU = TextKey.of("jsc.assembled.cpu", "CPU: %s");
    private static final TextKey RAM = TextKey.of("jsc.assembled.ram", "RAM: %s");
    private static final TextKey GRAPHICS = TextKey.of("jsc.assembled.graphics", "Graphics: %s");
    private static final TextKey CARD = TextKey.of("jsc.assembled.card", "Card: %s");
    private static final TextKey POWER = TextKey.of("jsc.assembled.power", "Power: %s");
    private static final TextKey DISK = TextKey.of("jsc.assembled.disk", "Disk: %s");
    private static final TextKey DRIVE = TextKey.of("jsc.assembled.drive", "Drive: %s");
    private static final TextKey COOLER = TextKey.of("jsc.assembled.cooler", "Cooler: %s");
    private static final TextKey FAN = TextKey.of("jsc.assembled.fan", "Fan: %s");
    /* Several of the same part: "2 x Stratix Layer DDR3-8192". */
    private static final TextKey MANY = TextKey.of("jsc.assembled.many", "%s x %s");
    private static final TextKey SYSTEM = TextKey.of("jsc.assembled.system", "System: %s");
    private static final TextKey NEEDS_PREINSTALLER = TextKey.of("jsc.assembled.needs_preinstaller",
            "Needs a Preinstaller before it starts");
    private static final TextKey NOT_STARTED = TextKey.of("jsc.assembled.not_started",
            "Not started yet: its first start runs the self-test");
    /* The firmware under its board and the system under its disk stand in by two spaces. */
    private static final String UNDER = "  ";

    private AssembledTooltip() {
    }

    /** Adds the lines of the computer in {@code stack} to {@code tooltip}; nothing for a case with nothing inside. */
    public static void append(final ItemStack stack, final List<Component> tooltip) {
        final ItemContainerContents contents = stack.get(ComputingComponents.ASSEMBLED_HARDWARE.get());
        if (contents == null) {
            return;
        }
        final List<ItemStack> parts = new ArrayList<>();
        contents.nonEmptyItems().forEach(parts::add);
        boolean blankBoard = false;
        boolean anySystem = false;
        boolean noneSaid = false;
        int at = 0;
        while (at < parts.size()) {
            final ItemStack part = parts.get(at);
            int same = 1;
            while (at + same < parts.size() && ItemStack.isSameItemSameComponents(part, parts.get(at + same))) {
                same++;
            }
            final TextKey kind = kindOf(part);
            if (kind != null) {
                final Component named = same > 1
                        ? GameText.component(MANY.with(same, part.getHoverName().getString()))
                        : part.getHoverName();
                tooltip.add(GameText.component(kind.with(named.getString())).withStyle(ChatFormatting.GRAY));
            }
            if (HardwareRegistry.board(part) != null) {
                blankBoard |= BoardFirmware.blank(part);
                tooltip.add(under(GameText.component(BoardFirmware.line(part)), BoardFirmware.blank(part)));
            }
            if (kind == DISK) {
                final ResourceLocation system = OsDisks.systemOn(part);
                anySystem |= system != null;
                if (system != null || !noneSaid && !anyDiskHasSystem(parts)) {
                    noneSaid |= system == null;
                    tooltip.add(under(GameText.component(SYSTEM.with(systemName(system))), system == null));
                }
            }
            at += same;
        }
        if (blankBoard || !anySystem) {
            tooltip.add(GameText.component(NEEDS_PREINSTALLER).withStyle(ChatFormatting.DARK_GRAY));
        } else if (stack.has(ComputingComponents.NEVER_STARTED.get())) {
            tooltip.add(GameText.component(NOT_STARTED).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /* A line under its part, in amber when it says what is missing. */
    private static Component under(final Component line, final boolean missing) {
        return Component.literal(UNDER).append(line).withStyle(missing ? ChatFormatting.GOLD : ChatFormatting.GRAY);
    }

    /* The kind a part is listed under; none for a stack that is no part. */
    @Nullable
    private static TextKey kindOf(final ItemStack part) {
        final HardwareRegistry.Part found = HardwareRegistry.part(part.getItem());
        return switch (found == null ? null : found.spec()) {
            case MotherboardSpec board -> BOARD;
            case CpuSpec cpu -> CPU;
            case RamSpec ram -> RAM;
            case GpuSpec gpu -> GRAPHICS;
            case IExpansionCardSpec card -> CARD;
            case PsuSpec psu -> POWER;
            case DiskSpec disk -> DISK;
            case BayDriveSpec drive -> DRIVE;
            case CoolerSpec cooler -> COOLER;
            case CaseFanSpec fan -> FAN;
            case null, default -> null;
        };
    }

    private static boolean anyDiskHasSystem(final List<ItemStack> parts) {
        return parts.stream().anyMatch(part -> kindOf(part) == DISK && OsDisks.systemOn(part) != null);
    }

    /* The name of the system on a disk, or none. */
    private static Component systemName(@Nullable final ResourceLocation system) {
        final OsDef os = system == null ? null : OsRegistry.getOs(system);
        return os == null ? GameText.component(BoardFirmware.NONE) : Component.translatable(os.titleKey());
    }
}
