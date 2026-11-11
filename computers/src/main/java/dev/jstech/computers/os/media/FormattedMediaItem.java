/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.MinSpecTooltip;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.SoftwareHouse;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.FilesystemTooltip;
import dev.jstech.computers.storage.ServerStorageContents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link MediaItem} with a fixed physical {@link MediaFormat} (floppy, CD, DVD, USB). The format is
 * the item's identity (one item per format), not a component, so each medium has its own texture and
 * tooltip. The content it carries (OS installer / program installer / data snapshot) is still stored
 * in components and is orthogonal to the format.
 *
 * <p>{@code writable} distinguishes read-only pressed media (CD-ROM, DVD-ROM) from rewritable media
 * (floppy, CD-RW, DVD-RW, USB). A medium holds real bytes, its format's or, for a USB stick, its model's, and only a
 * writable one is bounded by them: an installer always fits, since what a system takes counts on the disk it goes to.
 */
@TextHolder
public class FormattedMediaItem extends MediaItem {

    private final MediaFormat format;
    private final boolean writable;
    /* The stick's model, or null for a disc or a floppy disk, which hold what their format does. */
    @Nullable
    private final UsbStick stick;

    private static final TextKey READ_ONLY = TextKey.of("jsc.media.formatted_media_item.read_only", "%s · read-only");
    /* What the box says, then the exact bytes: "1.44 MB (1,474,560 bytes)". */
    private static final TextKey CAPACITY = TextKey.of("jsc.media.formatted_media_item.capacity", "%s (%s bytes)");
    /* A stick's plug and the era it was sold in: "USB 2.0, Standard era". */
    private static final TextKey STICK = TextKey.of("jsc.media.formatted_media_item.stick", "%s, %s era");
    private static final TextKey FREE = TextKey.of("jsc.media.formatted_media_item.free", "Free: %s");
    private static final TextKey FREE_ALL = TextKey.of("jsc.media.formatted_media_item.free_all", "Free: all of it");
    private static final TextKey BOOTABLE = TextKey.of("jsc.media.formatted_media_item.bootable",
            "Bootable installer");
    private static final TextKey BOOTABLE_ERA = TextKey.of("jsc.media.formatted_media_item.bootable_era",
            "Bootable installer · %s era");
    private static final TextKey PACKAGE = TextKey.of("jsc.media.formatted_media_item.package", "Package: %s");
    private static final TextKey OS_HINT = TextKey.of("jsc.media.formatted_media_item.os_hint",
            "%s, then install from the firmware or This PC.");
    private static final TextKey BLANK = TextKey.of("jsc.media.formatted_media_item.blank", "blank · no files");
    private static final TextKey DISK_OF = TextKey.of("jsc.media.formatted_media_item.disk_of", "%s · disk %s of %s");
    private static final TextKey SERVICE_DISC = TextKey.of("jsc.media.formatted_media_item.service_disc",
            "Service disc");
    private static final TextKey PROGRAM_DISC = TextKey.of("jsc.media.formatted_media_item.program_disc",
            "Program disc");
    private static final TextKey PROGRAM_HINT = TextKey.of("jsc.media.formatted_media_item.program_hint",
            "%s: run %s, or Install from This PC.");
    private static final TextKey DATA_MEDIUM = TextKey.of("jsc.media.formatted_media_item.data_medium",
            "Data medium · %s stored");
    private static final TextKey PLUG_INTO = TextKey.of("jsc.media.formatted_media_item.plug_into",
            "Plug into a linked %s");
    private static final TextKey INSERT_IN = TextKey.of("jsc.media.formatted_media_item.insert_in",
            "Insert in a linked %s");

    public FormattedMediaItem(final Properties properties, final MediaFormat format, final boolean writable) {
        this(properties, format, writable, null);
    }

    /** A medium of {@code format}; {@code stick} is the model of a USB stick, null for any other medium. */
    public FormattedMediaItem(final Properties properties, final MediaFormat format, final boolean writable,
                              @Nullable final UsbStick stick) {
        super(properties);
        this.format = format;
        this.writable = writable;
        this.stick = stick;
    }

    /** The fixed physical format of this medium. */
    public MediaFormat format() {
        return format;
    }

    /** Whether this medium can be written to (false for pressed ROM media). */
    public boolean writable() {
        return writable;
    }

    /** The bytes this medium holds: its format's, or its model's for a USB stick. */
    public long capacityBytes() {
        return stick != null ? stick.capacityBytes() : format.capacityBytes();
    }

    /** The era this medium was sold in: its format's, or its model's for a USB stick. */
    public Era era() {
        return stick != null ? stick.era() : format.era();
    }

    /**
     * What the files on this medium may weigh together, in thousandths of an item: its bytes on the terms of its
     * format's era, the era a file's weight on it is worked out in.
     */
    public long capacityWeight() {
        return EraStorage.weightHeldBy(format.era(), capacityBytes());
    }

    /**
     * The weight still free on {@code medium}, in thousandths of an item: what it holds less its files and the items
     * stored on it; a medium of no format of its own holds 64 items, as a medium always did.
     */
    public static long freeWeight(final ItemStack medium) {
        final long capacity = medium.getItem() instanceof FormattedMediaItem item ? item.capacityWeight()
                : 64L * StorageKey.MB_EQ_PER_ITEM;
        final long files = DiskFilesystem.filesWeight(medium);
        final long stored = medium.getOrDefault(ComputingComponents.MEDIA_DATA.get(), ServerStorageContents.EMPTY)
                .usedWeight();
        return Math.max(0L, capacity - files - stored);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        if (!writable) {
            tooltip.add(GameText.component(READ_ONLY.with(format.text())).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(GameText.component(CAPACITY.with(MediaSizes.soldAs(format, stick),
                MediaSizes.grouped(capacityBytes()))).withStyle(ChatFormatting.AQUA));
        if (stick != null) {
            tooltip.add(GameText.component(STICK.with(stick.plug(), MinSpecTooltip.eraLabel(stick.era())))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (writable) {
            final long free = freeWeight(stack);
            tooltip.add(GameText.component(free >= capacityWeight() ? FREE_ALL.text()
                    : FREE.with(MediaSizes.label(EraStorage.bytesOfWeight(format.era(), free))))
                    .withStyle(ChatFormatting.GRAY));
        }

        /*
         * Files written on the medium (e.g. .craft recipes from the Pattern Encoder) take priority: a
         * medium carrying files reads as such, not as a blank installer. The installer/data lines only
         * show for a medium with no files of its own.
         */
        final FilesystemContents fs = stack.getOrDefault(
                ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY);
        if (!fs.files().isEmpty()) {
            FilesystemTooltip.append(fs, tooltip);
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }

        switch (MediaItem.kind(stack)) {
            case OS_INSTALL -> {
                appendOsInstallerLines(MediaItem.payload(stack), tooltip);
                appendDiskLine(stack, tooltip);
            }
            case PROGRAM_INSTALL -> appendProgramInstallerLines(MediaItem.payload(stack), tooltip);
            case DATA -> appendDataLines(MediaItem.data(stack), tooltip);
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private void appendOsInstallerLines(@Nullable final ResourceLocation payload, final List<Component> tooltip) {
        if (payload == null) {
            tooltip.add(GameText.component(BLANK).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        final OsDef os = OsRegistry.getOs(payload);
        tooltip.add(Component.translatable("os.jsc." + payload.getPath())
                .withStyle(ChatFormatting.AQUA)
                .append(os == null ? Component.empty() : Component.literal("  "
                        + os.house().name() + " · "
                        + Branding.osYear(os.displayName(), os.minEra()))
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(GameText.component(os == null ? BOOTABLE.text()
                        : BOOTABLE_ERA.with(MinSpecTooltip.eraLabel(os.minEra())))
                .withStyle(ChatFormatting.GREEN));
        tooltip.addAll(MinSpecTooltip.osMinSpec(payload));
        // The id a shell or a manifest names it by, so a stick on a shelf is enough to know it.
        tooltip.add(GameText.component(PACKAGE.with(payload.getPath())).withStyle(ChatFormatting.GOLD));
        tooltip.add(GameText.component(OS_HINT.with(insertHint(format)))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** Which of its system's disks this one is, by the name on its label, for a system that comes on several. */
    private static void appendDiskLine(final ItemStack stack, final List<Component> tooltip) {
        final ResourceLocation payload = MediaItem.payload(stack);
        final OsDef os = payload == null ? null : OsRegistry.getOs(payload);
        if (os == null) {
            return;
        }
        final List<Text> disks = os.installerStyle().disks();
        final int part = MediaItem.part(stack);
        if (disks.size() > 1 && part <= disks.size()) {
            tooltip.add(GameText.component(DISK_OF.with(disks.get(part - 1), part, disks.size()))
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private void appendProgramInstallerLines(@Nullable final ResourceLocation payload,
                                             final List<Component> tooltip) {
        if (payload == null) {
            tooltip.add(GameText.component(BLANK).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        final ProgramSpec spec = OsRegistry.getProgram(payload);
        /*
         * Lead with the program's friendly, translated name, then its house and the year it was
         * written. A disc of a bundled program has no shipper to lean on, so it says Midsoft.
         */
        tooltip.add(Component.translatable("program.jsc." + payload.getPath())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(spec == null ? Component.empty() : Component.literal("  "
                        + spec.houseOr(SoftwareHouse.MIDSOFT).name()
                        + " · " + Branding.year(spec.era()))
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(GameText.component(spec != null
                && spec.kind().runsInBackground()
                ? SERVICE_DISC : PROGRAM_DISC).withStyle(ChatFormatting.YELLOW));
        // What it actually does, so a disc is not just a name on a shelf.
        tooltip.add(Component.translatable("program.jsc." + payload.getPath() + ".desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.addAll(MinSpecTooltip.programMinSpec(payload));
        if (spec != null) {
            /*
             * The package id and the command that installs it: the only other place to learn
             * either was the Mirror's listing on a Mainframe.
             */
            tooltip.add(GameText.component(PACKAGE.with(spec.commandName()))
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(String.join(" · ", installCommands(spec)))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        // The setup program's file name is data, the same on every machine.
        final Text setup = Text.literal(spec != null && Platform.onlyUnixLike(spec.platforms())
                ? "install.sh" : format == MediaFormat.FLOPPY || format == MediaFormat.CD
                        ? "SETUP.EXE" : "setup.exe");
        tooltip.add(GameText.component(PROGRAM_HINT.with(insertHint(format), setup))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static void appendDataLines(final ServerStorageContents data, final List<Component> tooltip) {
        if (data.total() > 0) {
            tooltip.add(GameText.component(DATA_MEDIUM.with(data.total()))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(GameText.component(BLANK).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Where this medium goes, in the drive's own name: a stick is plugged, a disc is inserted. */
    private static Text insertHint(final MediaFormat format) {
        return (format == MediaFormat.USB ? PLUG_INTO : INSERT_IN).with(InstallMedia.readerName(format));
    }

    /**
     * The package-manager commands that install {@code spec}, one per platform family it runs on: the
     * Frames manager first, then the Linux form. A program with no platform that has a manager gets none.
     */
    public static List<String> installCommands(final ProgramSpec spec) {
        final List<String> commands = new ArrayList<>(2);
        if (spec.platforms().contains(Platform.FRAMES)) {
            commands.add(command(PackageManagerKind.PCKMGR, spec));
        }
        if (spec.platforms().contains(Platform.LINUX)) {
            commands.add(command(PackageManagerKind.APT, spec));
        }
        if (spec.platforms().contains(Platform.FREEBSD)) {
            commands.add(command(PackageManagerKind.PKG, spec));
        }
        // System V has no manager that asks a network: its tool reads the medium this is written on.
        if (spec.platforms().contains(Platform.UNIX)) {
            commands.add("installpkg");
        }
        return commands;
    }

    private static String command(final PackageManagerKind manager,
                                  final ProgramSpec spec) {
        final String verb = manager.installVerb();
        return manager.command() + (verb.isEmpty() ? " " : " " + verb + " ") + spec.commandName();
    }
}
