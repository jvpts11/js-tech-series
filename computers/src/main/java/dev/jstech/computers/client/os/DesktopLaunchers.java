/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What this desktop can start, built from the one program registry: every program that opens a window and is present
 * on this machine, then the player's own programs installed from the Mirror. A program that ships with the system is
 * present when the desktop bundles it and the machine and the system allow it; one installed later is present when
 * the machine's listing names it. The wallpaper wears the same launchers as icons, with the trash ahead of them.
 */
final class DesktopLaunchers {

    private final DesktopState desktop;
    private final BlockPos host;
    private final BlockPos monitorPos;
    private final ResourceLocation desktopId;
    private final ResourceLocation osId;
    /** The desktop environment's descriptor (bundled programs, native names); null if nobody registered it. */
    @Nullable
    private final DesktopEnvironmentDef chrome;
    /** Everything this desktop can start, in the order its menus list it. */
    private final List<Launcher> all = new ArrayList<>();
    /** What the wallpaper wears as icons: the trash first, on every desktop but CDE, and then the programs. */
    private final List<Launcher> icons = new ArrayList<>();
    /** The ids of the programs installed on this machine, by id path, as its listing brings them. */
    private final List<String> installed = new ArrayList<>();
    /** The player's own programs installed from the Mirror. */
    private final List<CommunityLauncher> community = new ArrayList<>();

    /** What a player's own program is known by, ahead of the listing it starts at: it has no window to go by. */
    private static final String RUN_KEY = "run:";

    DesktopLaunchers(final DesktopState desktop, final BlockPos host, final BlockPos monitorPos,
                     final ResourceLocation desktopId, final ResourceLocation osId,
                     @Nullable final DesktopEnvironmentDef chrome) {
        this.desktop = desktop;
        this.host = host;
        this.monitorPos = monitorPos;
        this.desktopId = desktopId;
        this.osId = osId;
        this.chrome = chrome;
    }

    /** Everything this desktop can start, in the order its menus list it. */
    List<Launcher> all() {
        return all;
    }

    /** What the wallpaper wears as icons, ahead of the files of the desktop folder. */
    List<Launcher> icons() {
        return icons;
    }

    /** The programs installed on this machine, by id path. */
    List<String> installed() {
        return installed;
    }

    /**
     * Takes the programs a listing names: the installed ones and the player's own. Returns whether either changed,
     * which is when the launchers have to be built again, so a program installed or removed while the desktop is up
     * gets or loses its launcher at once.
     */
    boolean take(final List<String> programs, final List<DesktopFilesPayload.WireCommunity> theirs) {
        final Set<String> before = new HashSet<>(installed);
        final List<CommunityLauncher> theirsBefore = List.copyOf(community);
        installed.clear();
        installed.addAll(programs);
        community.clear();
        for (final DesktopFilesPayload.WireCommunity one : theirs) {
            community.add(new CommunityLauncher(one.name(), one.icon(), one.entry()));
        }
        return !before.equals(new HashSet<>(installed)) || !theirsBefore.equals(community);
    }

    /**
     * Builds the launchers again from the program registry. A program that ships with the system is gated here by
     * the desktop that bundles it, the machine it may run on and the system's rank (the Network Manager on a
     * Mainframe, Frames XP or newer); an installed one was gated by the server, which only lists it when it passed.
     */
    void build() {
        all.clear();
        final int rank = OsRegistry.osVersionRank(osId);
        final Platform platform = desktop.platform();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.kind() != ProgramKind.APP || !spec.platforms().contains(platform)
                    || !ProgramClient.hasWindow(spec.id())) {
                continue;
            }
            if (spec.preinstalled()) {
                if (chrome != null && !chrome.bundles(spec.id())) {
                    continue;
                }
                if (!hostScopeAllows(spec.hostScope())) {
                    continue;
                }
                if (rank != 0 && rank < spec.minOsRank()) {
                    continue;
                }
            } else if (!installed.contains(spec.id().getPath())) {
                continue;
            }
            final ProgramClient.IDesktopAppFactory factory = ProgramClient.factory(spec.id());
            // A program is given the desktop's id, its skin and icon key; the Frames editions' id is their system's.
            all.add(new Launcher(WindowKeys.of(spec.id()), labelOf(spec), spec.id(),
                    () -> factory.create(host, monitorPos, desktopId)));
        }
        /*
         * Then whatever the player installed from the Mirror. These are not the mod's programs and have no window of
         * their own: starting one gets it a terminal, exactly as opening it in the file explorer would. The icon id
         * is one the artwork can grow into; until it does they wear the generic one, which is what the icons fall
         * back to.
         */
        for (final CommunityLauncher one : community) {
            all.add(new Launcher(RUN_KEY + one.entry(), one.name(),
                    ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "sigma_" + one.icon()), null,
                    one.entry()));
        }
        rebuildIcons();
    }

    /** Makes the wallpaper's icons again, after the programs or the trash's picture changed. */
    void rebuildIcons() {
        icons.clear();
        final DeskTrash trash = desktop.trash();
        if (trash.onWallpaper()) {
            icons.add(trash.launcher());
        }
        icons.addAll(all);
    }

    /** The first launcher that answers {@code test}, or null. */
    @Nullable
    Launcher find(final Predicate<Launcher> test) {
        for (final Launcher launcher : all) {
            if (test.test(launcher)) {
                return launcher;
            }
        }
        return null;
    }

    /** The launcher known by that key, or null. */
    @Nullable
    Launcher byKey(final String key) {
        return find(l -> l.key().equals(key));
    }

    /** The launcher that reads as that label on this desktop, or null. */
    @Nullable
    Launcher byLabel(final String label) {
        return find(l -> l.label().equals(label));
    }

    /** The launcher of a program, or null when this desktop does not offer it. */
    @Nullable
    Launcher byProgram(final ResourceLocation id) {
        return find(l -> id.equals(l.programId()));
    }

    /** The launcher of the program with that id path, or null. */
    @Nullable
    Launcher byPath(final String path) {
        return find(l -> l.programId().getPath().equals(path));
    }

    /** The labels of the launchers, in order. */
    List<String> labels() {
        final List<String> out = new ArrayList<>();
        for (final Launcher launcher : all) {
            out.add(launcher.label());
        }
        return out;
    }

    /**
     * What a program is called on this desktop: the desktop environment's native name for it (Dolphin, Konsole,
     * Nautilus...), its own name otherwise; the shell reads "Megashell" on Frames 11.
     */
    String labelOf(final ProgramSpec spec) {
        return GameText.resolve(chrome != null ? chrome.launcherLabel(spec) : spec.name());
    }

    /** Whether the machine this desktop runs on is (an instance of) {@code type}. */
    private boolean hostIs(final Class<?> type) {
        return Minecraft.getInstance().level != null
                && type.isInstance(Minecraft.getInstance().level.getBlockEntity(host));
    }

    /** Whether a program's host scope allows it on this machine. */
    private boolean hostScopeAllows(final HostScope scope) {
        return switch (scope) {
            case ANY -> true;
            case MAINFRAME -> hostIs(MainframeBlockEntity.class);
            case CRAFTING_COMPUTER -> hostIs(CraftingComputerBlockEntity.class);
            // A rack shows the desktop of the server mounted in it, so a rack host is a server session.
            case SERVER -> hostIs(ServerRackBlockEntity.class);
            case CLUSTER_MANAGEMENT_COMPUTER -> hostIs(ClusterManagementComputerBlockEntity.class);
            case PERSONAL_COMPUTER -> hostIs(PersonalComputerBlockEntity.class);
        };
    }

    /** A player's own program on this desktop: what to call it, what to draw, and what to run. */
    private record CommunityLauncher(String name, String icon, String entry) {
    }
}
