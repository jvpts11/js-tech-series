/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.backup.TapeContents;
import dev.jstech.computers.backup.TapeFormat;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * A tape for the Tape Library of its era, the cold storage of the Backup Service: its real native capacity, and the
 * backups written along it, which its tooltip lists by the label written on it (computers/docs/design/programs.md).
 */
@TextHolder
public final class TapeItem extends Item {

    private final TapeFormat format;
    private static final TextKey HOLDS = TextKey.of("jsc.item.tape.holds", "%s tape: %s items");
    private static final TextKey LABEL = TextKey.of("jsc.item.tape.label", "Label: %s");
    private static final TextKey BLANK = TextKey.of("jsc.item.tape.blank", "Blank");
    private static final TextKey USED = TextKey.of("jsc.item.tape.used", "%s of %s used, %s backups");
    private static final TextKey FOR_LIBRARY = TextKey.of("jsc.item.tape.for_library",
            "For a Tape Library of its era");

    public TapeItem(final Properties properties, final TapeFormat format) {
        super(properties.stacksTo(1));
        this.format = format;
    }

    /** The format of the tape this item is. */
    public TapeFormat format() {
        return format;
    }

    /** The format of {@code stack}, or null when it is no tape. */
    @Nullable
    public static TapeFormat formatOf(final ItemStack stack) {
        return stack.getItem() instanceof TapeItem tape ? tape.format : null;
    }

    /** What {@code stack} holds; blank for a tape nothing was written on and for anything that is no tape. */
    public static TapeContents contents(final ItemStack stack) {
        return stack.getOrDefault(ComputingComponents.TAPE_CONTENTS.get(), TapeContents.BLANK);
    }

    /** Writes what the tape holds onto it, leaving no component on one wiped blank and unlabelled. */
    public static void setContents(final ItemStack stack, final TapeContents contents) {
        if (contents.equals(TapeContents.BLANK)) {
            stack.remove(ComputingComponents.TAPE_CONTENTS.get());
        } else {
            stack.set(ComputingComponents.TAPE_CONTENTS.get(), contents);
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip,
                                final TooltipFlag flag) {
        final TapeContents contents = contents(stack);
        tooltip.add(GameText.component(HOLDS.with(DiskSpec.sizeLabel(format.capacityMb()), format.capacityItems()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(LABEL.with(contents.label().isEmpty() ? BLANK.text()
                : contents.label())).withStyle(ChatFormatting.GRAY));
        if (!contents.spots().isEmpty()) {
            tooltip.add(GameText.component(USED.with(DiskSpec.sizeLabel(contents.used()
                            * EraStorage.mbPerItem(format.era())), DiskSpec.sizeLabel(format.capacityMb()),
                    contents.spots().size())).withStyle(ChatFormatting.GRAY));
        }
        HardwareTooltip.appendEra(tooltip, format.era());
        tooltip.add(GameText.component(FOR_LIBRARY.text()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
