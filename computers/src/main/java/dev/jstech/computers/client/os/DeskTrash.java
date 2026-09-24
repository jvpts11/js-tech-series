/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.DeleteFilePayload;
import dev.jstech.computers.operation.payload.TrashActionPayload;
import dev.jstech.computers.operation.payload.TrashFilePayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.TrashKind;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The trash as the desktop has it: the icon the wallpaper wears first of all, full or empty, what deleting a thing
 * means, and the menu of the trash's own icon.
 *
 * <p>Deleting something on the system disk puts it in the trash, and says nothing, since it can be put back. A thing
 * on a removable medium or on another machine's share has no trash to go to, so it is deleted for good, and the
 * desktop asks first, the way Windows asked before a delete it could not undo.
 */
final class DeskTrash {

    private final DesktopScreen desktop;

    /** Whether anything is in the trash, as the machine last said, which is the picture its icon wears. */
    private boolean full;

    /** The trash's two pictures, which are also what its icon on the wallpaper is known by. */
    private static final ResourceLocation EMPTY_ICON =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "trash");
    private static final ResourceLocation FULL_ICON =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "trash_full");

    DeskTrash(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /**
     * Deletes the things at those paths the way a desktop does: into the trash for what is on the system disk, and
     * for good, once the player says yes, for what is on a medium or a share.
     */
    static void delete(final BlockPos host, final List<String> paths) {
        final List<String> forGood = new ArrayList<>();
        boolean trashed = false;
        for (final String path : paths) {
            if (outsideTheDisk(path)) {
                forGood.add(path);
            } else {
                PacketDistributor.sendToServer(new TrashFilePayload(host, path));
                trashed = true;
            }
        }
        if (trashed) {
            FilesApps.diskChanged();
        }
        if (forGood.isEmpty()) {
            return;
        }
        final boolean disc = forGood.getFirst().startsWith("media:");
        final Text question = forGood.size() == 1
                ? (disc ? TrashTexts.ONE_ON_DISC : TrashTexts.ONE_ON_SHARE).with(nameOf(forGood.getFirst()),
                        titleTextHere())
                : (disc ? TrashTexts.MANY_ON_DISC : TrashTexts.MANY_ON_SHARE).with(forGood.size(), titleTextHere());
        DesktopScreen.ask(GameText.resolve(TrashTexts.CONFIRM), GameText.resolve(question), () -> {
            for (final String path : forGood) {
                PacketDistributor.sendToServer(new DeleteFilePayload(host, path));
            }
            FilesApps.diskChanged();
        });
    }

    /** Whether a path is somewhere a trash cannot reach: a medium in a drive, or another machine's share. */
    static boolean outsideTheDisk(final String path) {
        return path.startsWith("media:") || path.startsWith("net:");
    }

    /** What the desktop that is up calls its trash, as the player reads it. */
    static Text titleTextHere() {
        final DesktopScreen shown = DesktopScreen.current();
        return (shown == null ? TrashKind.RECYCLER : TrashApp.kindOf(shown.panelStyle())).titleText();
    }

    /** Takes whether the trash holds anything, answering whether that changed its picture. */
    boolean setFull(final boolean now) {
        final boolean changed = this.full != now;
        this.full = now;
        return changed;
    }

    boolean full() {
        return this.full;
    }

    /** What this desktop calls its trash, which is also the key its window goes by. */
    String title() {
        return TrashApp.kindOf(this.desktop.panelStyle()).title();
    }

    /** Whether the wallpaper wears the trash: every desktop does but CDE, which keeps it on its Front Panel. */
    boolean onWallpaper() {
        return this.desktop.panelStyle() != PanelStyle.CDE;
    }

    /** The trash's picture, full or empty. */
    ResourceLocation icon() {
        return this.full ? FULL_ICON : EMPTY_ICON;
    }

    /** The trash as an icon on the wallpaper. */
    DesktopScreen.Launcher launcher() {
        return new DesktopScreen.Launcher(title(), icon(), this::window);
    }

    /** Whether a launcher is the trash's own. */
    boolean is(final DesktopScreen.Launcher launcher) {
        final ResourceLocation id = launcher.programId();
        return (EMPTY_ICON.equals(id) || FULL_ICON.equals(id)) && launcher.label().equals(title());
    }

    /** A new trash window for this desktop. */
    TrashApp window() {
        return new TrashApp(this.desktop.hostPos(), this.desktop.panelStyle());
    }

    /** Opens the trash, or brings its window forward when it is already up: one trash window a desktop. */
    void open() {
        this.desktop.openOnce(title(), this::window);
    }

    /** The menu of the trash's own icon: open it, or empty it without opening it. */
    List<ContextMenu.Item> menu() {
        final Text trash = TrashApp.kindOf(this.desktop.panelStyle()).titleText();
        return List.of(new ContextMenu.Item(GameText.resolve(TrashTexts.OPEN), true, this::open),
                ContextMenu.Item.separator(),
                new ContextMenu.Item(GameText.resolve(TrashTexts.EMPTY_NAMED.with(trash)), this.full, this::empty));
    }

    /** Empties the trash from its icon, once the player says yes. */
    private void empty() {
        final BlockPos host = this.desktop.hostPos();
        final Text trash = TrashApp.kindOf(this.desktop.panelStyle()).titleText();
        DesktopScreen.ask(GameText.resolve(TrashTexts.CONFIRM),
                GameText.resolve(TrashTexts.EMPTY_EVERYTHING.with(trash)), () -> {
            PacketDistributor.sendToServer(new TrashActionPayload(host, TrashActionPayload.Action.EMPTY, List.of()));
            FilesApps.diskChanged();
        });
    }

    /** The last name in a path, a medium's or a share's included. */
    private static String nameOf(final String path) {
        final int colon = path.indexOf(':');
        final String rest = colon >= 0 ? path.substring(colon + 1) : path;
        return FsPaths.fileName(rest);
    }
}
