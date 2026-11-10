/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.IMediaDrive;
import dev.jstech.computers.os.media.MachineMedia;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.player.PlayerDesktop;
import dev.jstech.computers.os.player.PlayerSystem;
import dev.jstech.computers.os.player.PlayerSystems;
import dev.jstech.computers.program.cli.ICliSystems;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.computers.sigma.pack.Manifest;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What a machine does with an operating system or a desktop a player built at its prompt
 * (computers/docs/design/player-systems.md): keeps it for the world, writes a system's installer onto a medium in one
 * of its drives, and publishes what was built to the network's Mirror as the player's own.
 */
@TextHolder
public final class PlayerBuilds {

    private static final TextKey NO_KERNEL = TextKey.of("jsc.machine.player_builds.no_kernel",
            "there is no kernel called %s");
    /* The medium and the drive it is in: "the CD-RW in the Internal CD Drive". */
    private static final TextKey ON_DRIVE = TextKey.of("jsc.machine.player_builds.on_drive", "the %s in the %s");
    private static final TextKey NOTHING_BUILT = TextKey.of("jsc.machine.player_builds.nothing_built",
            "nothing was built from %s here: build it with 'sigma build --os %s' first");
    private static final TextKey NO_MIRROR = TextKey.of("jsc.machine.player_builds.no_mirror",
            "no Mirror answers on this network");
    private static final TextKey SHELF_FULL = TextKey.of("jsc.machine.player_builds.shelf_full",
            "the Mirror's shelf is full");
    /* A megabyte is a quarter of an item a medium holds, as everything is four megabytes to the item. */
    private static final int MB_PER_ITEM = 4;

    private PlayerBuilds() {
    }

    /** Keeps the system and writes its installer onto the first blank medium that can be written and holds it. */
    public static ICliSystems.Kept keepSystem(final IOsHost machine, final ServerLevel level, final SystemFacts facts,
                                              final String folder, final Map<String, String> files) {
        if (!facts.kernel().own() && OsRegistry.getKernel(
                ResourceLocation.fromNamespaceAndPath("jsc", facts.kernel().base())) == null) {
            return ICliSystems.Kept.refused(NO_KERNEL.with(facts.kernel().base()));
        }
        final PlayerSystem system = new PlayerSystem(PlayerSystem.idOf(facts), facts, leaf(folder), files);
        PlayerSystems.get(level.getServer()).put(system);
        final int size = system.footprintMb();
        for (final IMediaDrive drive : MachineMedia.of(machine, level).drives()) {
            final ItemStack medium = drive.medium();
            if (blank(medium) && medium.getItem() instanceof FormattedMediaItem item && item.writable()
                    && (long) item.format().capacityItems() * MB_PER_ITEM >= size) {
                MediaItem.setKind(medium, MediaKind.OS_INSTALL);
                MediaItem.setPayload(medium, system.id());
                MediaItem.setPart(medium, 1);
                drive.commit();
                return ICliSystems.Kept.kept(ON_DRIVE.with(GameText.of(medium.getHoverName()),
                        drive.type().driveName()), size);
            }
        }
        return ICliSystems.Kept.kept(Text.EMPTY, size);
    }

    /** Keeps the desktop, for any machine to install as it installs one of the mod's desktop packages. */
    public static ICliSystems.Kept keepDesktop(final ServerLevel level, final DesktopFacts facts, final String folder,
                                               final Map<String, String> files) {
        final PlayerDesktop desktop = new PlayerDesktop(PlayerDesktop.idOf(facts.name()), facts, leaf(folder), files);
        PlayerSystems.get(level.getServer()).put(desktop);
        long chars = 0;
        for (final String content : files.values()) {
            chars += content.length();
        }
        return ICliSystems.Kept.kept(Text.EMPTY, (int) Math.max(1, (chars + (1 << 20) - 1) >> 20));
    }

    /**
     * Publishes what was last built from {@code folder}, the system and the desktop beside it, to the Mirror of the
     * machine's network, each as a package of its author's.
     */
    public static ICliSystems.Published publish(final IOsHost machine, final ServerLevel level, final String folder) {
        final PlayerSystems store = PlayerSystems.get(level.getServer());
        final PlayerSystem system = store.lastBuiltFrom(leaf(folder));
        final PlayerDesktop desktop = store.lastDesktopFrom(leaf(folder));
        if (system == null && desktop == null) {
            return ICliSystems.Published.refused(NOTHING_BUILT.with(folder, folder));
        }
        final MainframeBlockEntity mainframe = Installers.mirrorOf(machine, level);
        if (mainframe == null) {
            return ICliSystems.Published.refused(NO_MIRROR.text());
        }
        final String host = Installers.mirrorHost(machine, level);
        final String author = system != null ? system.facts().author() : desktop.facts().author();
        final List<String> published = new ArrayList<>();
        if (system != null) {
            final Manifest manifest = new Manifest(system.facts().packageName(), system.facts().version(),
                    system.facts().author(), system.id().toString(), Manifest.DEFAULT_ICON, system.facts().memoryMb(),
                    List.copyOf(system.files().keySet()), system.facts().label(), Manifest.SYSTEM);
            if (!mainframe.shelve(manifest.name(), new Packed(manifest, system.files()).write())) {
                return new ICliSystems.Published(published, SHELF_FULL.text(), host, author);
            }
            published.add(manifest.label());
        }
        if (desktop != null) {
            final Manifest manifest = desktop.manifest();
            if (!mainframe.shelve(manifest.name(), new Packed(manifest, desktop.files()).write())) {
                return new ICliSystems.Published(published, SHELF_FULL.text(), host, author);
            }
            published.add(manifest.label());
        }
        return new ICliSystems.Published(published, Text.EMPTY, host, author);
    }

    /* Whether a medium holds nothing: no installer, no files and no items. */
    private static boolean blank(final ItemStack medium) {
        if (medium.isEmpty() || !(medium.getItem() instanceof MediaItem)) {
            return false;
        }
        final ResourceLocation payload = MediaItem.payload(medium);
        final FilesystemContents files = medium.get(ComputingComponents.FILESYSTEM.get());
        final boolean empty = files == null || files.files().isEmpty();
        // A medium that already carries a player's system is written over by a new build, as a rewritable disc is.
        final boolean ours = payload != null && payload.getPath().startsWith("player/");
        return empty && MediaItem.data(medium).items().isEmpty() && (payload == null || ours);
    }

    /* The folder's own name, without the folders it is in: where the system's paths start from. */
    private static String leaf(final String folder) {
        final String trimmed = folder.replace('\\', '/').replaceAll("/+$", "");
        final int slash = trimmed.lastIndexOf('/');
        return slash < 0 ? trimmed : trimmed.substring(slash + 1);
    }
}
