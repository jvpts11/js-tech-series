/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.FilesystemTooltip;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.storage.DiskPrivacy;
import dev.jstech.computers.storage.DiskUsage;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A storage-disk component item.
 */
@TextHolder
public class DiskItem extends SpecItem<DiskSpec> {

    /*
     * A fresh disk exposes nothing to the network until the owner publishes part of it; this keeps a
     * newly placed computer's storage private by default. Tunable.
     */
    public static final int DEFAULT_PUBLIC_PERMILLE = 0;

    /* The drive's size, the word size of its era and what an item costs on it. */
    private static final TextKey NAMEPLATE = TextKey.of("jsc.item.disk.nameplate", "%s drive  -  %s-bit: %s");
    private static final TextKey SPEED = TextKey.of("jsc.item.disk.speed", "%s  -  %st latency  -  %sx speed");
    private static final TextKey SYSTEM = TextKey.of("jsc.item.disk.system", "System: %s");
    private static final TextKey DESKTOP = TextKey.of("jsc.item.disk.desktop", "Desktop: %s");
    private static final TextKey PROGRAMS_ONE = TextKey.of("jsc.item.disk.programs_one", "%s program installed");
    private static final TextKey PROGRAMS_MANY = TextKey.of("jsc.item.disk.programs_many", "%s programs installed");

    public DiskItem(final Properties properties, final DiskSpec spec) {
        super(properties, spec);
    }

    /**
     * The public-share permille stored on a disk stack, or the private default if the component is absent or the stack is not a disk.
     */
    public static int publicPermille(final ItemStack stack) {
        if (!(stack.getItem() instanceof DiskItem)) {
            return DEFAULT_PUBLIC_PERMILLE;
        }
        final Integer stored = stack.get(ComputingComponents.DISK_PUBLIC_PERMILLE.get());
        return stored == null ? DEFAULT_PUBLIC_PERMILLE
                : DiskPrivacy.clampPermille(stored);
    }

    /** Writes a clamped public-share permille onto a disk stack (a no-op for a non-disk stack). */
    public static void setPublicPermille(final ItemStack stack, final int permille) {
        if (stack.getItem() instanceof DiskItem) {
            stack.set(ComputingComponents.DISK_PUBLIC_PERMILLE.get(),
                    DiskPrivacy.clampPermille(permille));
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final DiskSpec spec = spec();
        /*
         * The nameplate, and the one budget in both units its data comes in: what an item costs on the
         * drive follows from the word size of the era it was made for.
         */
        tooltip.add(GameText.component(NAMEPLATE.with(DiskSpec.sizeLabel(spec.capacityMb()), spec.era().bits(),
                HardwareTooltip.MB_PER_ITEM.with(spec.era().mbPerItem()))).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(DiskUsage.capacityLine(spec.capacityItems())).withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
        tooltip.add(GameText.component(SPEED.with(spec.tier(), spec.tier().latencyTicks(),
                spec.tier().speedMultiplier())).withStyle(ChatFormatting.DARK_GRAY));
        appendSystem(stack, tooltip);
        /*
         * Files on the disk's filesystem (e.g. .iql scripts, .craft recipes), separate from the
         * item/fluid storage listed below.
         */
        final FilesystemContents fs = stack.getOrDefault(
                ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY);
        FilesystemTooltip.append(fs, tooltip);
        appendContents(stack, tooltip, spec.capacityItems());
    }

    /**
     * Names the system installed on this drive, and the desktop and program count it carries. Without
     * it a drive in the hand is anonymous, and pulling one out of a machine is a guess, which is how a
     * player wipes a system they meant to keep.
     */
    private static void appendSystem(final ItemStack stack, final List<Component> tooltip) {
        final ResourceLocation osId =
                OsDisks.systemOn(stack);
        if (osId == null) {
            return; // a blank drive says nothing, which is itself the answer
        }
        final OsDef os =
                OsRegistry.getOs(osId);
        tooltip.add(GameText.component(SYSTEM.with(os != null ? os.displayName() : osId.getPath()))
                .withStyle(ChatFormatting.AQUA));

        final CompoundTag software = stack.get(ComputingComponents.DISK_CONSOLE.get());
        if (software == null) {
            return;
        }
        final ComputerConsoleState state =
                new ComputerConsoleState();
        state.load(software);
        final ResourceLocation desktop =
                OsDisks.installedDesktopId(os, state);
        if (desktop != null) {
            final var def = OsRegistry.getDesktop(desktop);
            tooltip.add(GameText.component(DESKTOP.with(def != null ? def.displayName() : desktop.getPath()))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        final int programs = state.installed().size();
        if (programs > 0) {
            tooltip.add(GameText.component((programs == 1 ? PROGRAMS_ONE : PROGRAMS_MANY).with(programs))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /**
     * What is on the drive, from the usage summary it carries: items by the piece, fluids and chemicals by
     * the millibucket, and how much of the drive that takes: the contents themselves stay in the volume
     * store and are browsed on a machine, never listed from the hand.
     */
    private static void appendContents(final ItemStack stack, final List<Component> tooltip,
                                       final long capacityItems) {
        final DiskUsage usage = DriveVolumes.usage(stack);
        if (usage.isEmpty()) {
            return;
        }
        tooltip.add(GameText.component(usage.summary(capacityItems)).withStyle(ChatFormatting.AQUA));
    }
}
