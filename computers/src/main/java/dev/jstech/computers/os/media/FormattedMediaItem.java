/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

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
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.FilesystemTooltip;
import dev.jstech.computers.storage.ServerStorageContents;
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

/**
 * A {@link MediaItem} with a fixed physical {@link MediaFormat} (floppy, CD, DVD, USB). The format is
 * the item's identity (one item per format), not a component, so each medium has its own texture and
 * tooltip. The content it carries (OS installer / program installer / data snapshot) is still stored
 * in components and is orthogonal to the format.
 *
 * <p>{@code writable} distinguishes read-only pressed media (CD-ROM, DVD-ROM) from rewritable media
 * (floppy, CD-RW, DVD-RW, USB) for the future data-write flow.
 */
@TextHolder
public class FormattedMediaItem extends MediaItem {

    private final MediaFormat format;
    private final boolean writable;

    private static final TextKey READ_WRITE = TextKey.of("jsc.media.formatted_media_item.read_write",
            "%s · read/write");
    private static final TextKey READ_ONLY = TextKey.of("jsc.media.formatted_media_item.read_only", "%s · read-only");
    private static final TextKey CAPACITY = TextKey.of("jsc.media.formatted_media_item.capacity", "%s item capacity");
    private static final TextKey BOOTABLE = TextKey.of("jsc.media.formatted_media_item.bootable",
            "Bootable installer");
    private static final TextKey BOOTABLE_ERA = TextKey.of("jsc.media.formatted_media_item.bootable_era",
            "Bootable installer · %s era");
    private static final TextKey PACKAGE = TextKey.of("jsc.media.formatted_media_item.package", "Package: %s");
    private static final TextKey OS_HINT = TextKey.of("jsc.media.formatted_media_item.os_hint",
            "%s, then install from the firmware or This PC.");
    private static final TextKey BLANK = TextKey.of("jsc.media.formatted_media_item.blank", "blank · no files");
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
        super(properties);
        this.format = format;
        this.writable = writable;
    }

    /** The fixed physical format of this medium. */
    public MediaFormat format() {
        return format;
    }

    /** Whether this medium can be written to (false for pressed ROM media). */
    public boolean writable() {
        return writable;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component((writable ? READ_WRITE : READ_ONLY).with(format.name()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(CAPACITY.with(format.capacityItems()))
                .withStyle(ChatFormatting.DARK_GRAY));

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

        final ResourceLocation payload = MediaItem.payload(stack);
        switch (MediaItem.kind(stack)) {
            case OS_INSTALL -> {
                final OsDef os =
                        payload == null ? null : OsRegistry.getOs(payload);
                if (payload != null) {
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
                } else {
                    tooltip.add(GameText.component(BLANK).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case PROGRAM_INSTALL -> {
                if (payload != null) {
                    final ProgramSpec spec =
                            OsRegistry.getProgram(payload);
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
                            && spec.kind() == ProgramKind.SERVICE
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
                } else {
                    tooltip.add(GameText.component(BLANK).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case DATA -> {
                final ServerStorageContents data = MediaItem.data(stack);
                if (data.total() > 0) {
                    tooltip.add(GameText.component(DATA_MEDIUM.with(data.total()))
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    tooltip.add(GameText.component(BLANK).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
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
