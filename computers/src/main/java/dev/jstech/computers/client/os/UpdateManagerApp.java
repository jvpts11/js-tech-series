/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.SoftwareManagerLayout.Rect;
import dev.jstech.computers.gui.layout.UpdateManagerLayout;
import dev.jstech.computers.operation.payload.program.UpdatePayloads;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Linux Mint's Update Manager (computers/docs/design/operating-systems.md): every installed package the Mirror has a
 * newer version of, each ticked to be installed, with what it is under the list and how much is ticked along the
 * foot. Install Updates installs what is ticked: a program at once, and the desktop the next time it starts, since
 * the desktop already running is the one that keeps running.
 */
@TextHolder
public final class UpdateManagerApp implements IDesktopApp {

    private final BlockPos host;
    private OsSkin skin;
    /** What the Mirror last said it has newer, or null before it answered. */
    @Nullable
    private UpdatePayloads.Listing listing;
    /** The updates ticked, by package id; every update arrives ticked, as the real one has them. */
    private final Set<String> ticked = new LinkedHashSet<>();
    /** The update picked out, whose description is under the list, by its place in it. */
    private int picked;
    private int scroll;
    /* Where the content and the toolbar's buttons were last drawn, which a click is measured against. */
    private int left;
    private int top;
    private Rect[] drawnButtons = new Rect[0];

    private static final TextKey NAME = TextKey.of("jsc.mint.updates.name", "Update Manager");
    private static final TextKey CLEAR = TextKey.of("jsc.mint.updates.clear", "Clear");
    private static final TextKey SELECT_ALL = TextKey.of("jsc.mint.updates.select_all", "Select All");
    private static final TextKey REFRESH = TextKey.of("jsc.mint.updates.refresh", "Refresh");
    private static final TextKey INSTALL_UPDATES = TextKey.of("jsc.mint.updates.install", "Install Updates");
    private static final List<TextKey> BUTTONS = List.of(CLEAR, SELECT_ALL, REFRESH, INSTALL_UPDATES);
    private static final TextKey UPGRADE = TextKey.of("jsc.mint.updates.upgrade", "Upgrade");
    private static final TextKey NEW_VERSION = TextKey.of("jsc.mint.updates.new_version", "New version");
    private static final TextKey INSTALLED = TextKey.of("jsc.mint.updates.installed", "Installed");
    private static final TextKey SIZE = TextKey.of("jsc.mint.updates.size", "Size");
    private static final TextKey MEGABYTES = TextKey.of("jsc.mint.updates.megabytes", "%s MB");
    private static final TextKey ABOUT_PROGRAM = TextKey.of("jsc.mint.updates.about_program",
            "A newer version of %s is on the Mirror. It is installed at once.");
    private static final TextKey ABOUT_DESKTOP = TextKey.of("jsc.mint.updates.about_desktop",
            "A newer version of the desktop is on the Mirror. It is installed the next time the desktop starts.");
    private static final TextKey SELECTED = TextKey.of("jsc.mint.updates.selected", "%s updates selected (%s MB)");
    private static final TextKey UP_TO_DATE = TextKey.of("jsc.mint.updates.up_to_date", "Your system is up to date");
    private static final TextKey NO_MIRROR = TextKey.of("jsc.mint.updates.no_mirror",
            "No Mirror answers on this network, so there is nothing to check for updates.");
    /** The look its icons are drawn in, which is Cinnamon's. */
    private static final String ICONS = "cinnamon";

    /** The open Update Manager that takes the Mirror's answers, or null when none is up. */
    @Nullable
    private static UpdateManagerApp active;

    UpdateManagerApp(final BlockPos host) {
        this.host = host;
        active = this;
        PacketDistributor.sendToServer(new UpdatePayloads.Ask(host));
    }

    /**
     * Hands the open Update Manager what the Mirror has newer, every update ticked. A listing that says what the one
     * shown says, which the desktop asks for whenever it looks at the machine again, leaves the player's ticks alone.
     */
    public static void accept(final UpdatePayloads.Listing payload) {
        if (active == null || !active.host.equals(payload.host())) {
            return;
        }
        final UpdatePayloads.Listing shown = active.listing;
        active.listing = payload;
        if (shown != null && shown.reachable() == payload.reachable() && shown.rows().equals(payload.rows())) {
            return;
        }
        active.ticked.clear();
        for (final UpdatePayloads.Update update : payload.rows()) {
            active.ticked.add(update.program());
        }
        active.picked = 0;
    }

    @Override
    public String title() {
        return GameText.resolve(NAME);
    }

    @Override
    public int defaultWidth() {
        return UpdateManagerLayout.WIDTH;
    }

    @Override
    public int defaultHeight() {
        return UpdateManagerLayout.HEIGHT;
    }

    @Override
    public void applySkin(final OsSkin given) {
        this.skin = given;
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        if (this.skin == null) {
            return;
        }
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        g.fill(x, y, x + UpdateManagerLayout.WIDTH, y + UpdateManagerLayout.TOOLBAR_H, this.skin.panelBg());
        g.fill(x, y + UpdateManagerLayout.TOOLBAR_H, x + UpdateManagerLayout.WIDTH,
                y + UpdateManagerLayout.TOOLBAR_H + 1, this.skin.edge());
        final Rect[] buttons = UpdateManagerLayout.buttons(wordWidths(font));
        this.drawnButtons = buttons;
        for (int i = 0; i < buttons.length; i++) {
            final Rect b = buttons[i];
            this.skin.button(g, font, x + b.x(), y + b.y(), b.w(), b.h(), GameText.resolve(BUTTONS.get(i)),
                    b.contains(mouseX - x, mouseY - y), false, BUTTONS.get(i) == INSTALL_UPDATES);
        }
        final UpdatePayloads.Listing shown = this.listing;
        if (shown == null) {
            return;
        }
        if (!shown.reachable() || shown.rows().isEmpty()) {
            Draw.text(g, font, GameText.resolve(shown.reachable() ? UP_TO_DATE : NO_MIRROR), x + 7,
                    y + UpdateManagerLayout.HEADS_Y, this.skin.dim());
            return;
        }
        heads(g, font);
        rows(g, font, shown, mouseX - x, mouseY - y);
        about(g, font, shown);
        status(g, font, shown);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final double px = mouseX - this.left;
        final double py = mouseY - this.top;
        final UpdatePayloads.Listing shown = this.listing;
        final Rect[] buttons = this.drawnButtons;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(px, py)) {
                pressed(BUTTONS.get(i), shown);
                return;
            }
        }
        final int at = UpdateManagerLayout.rowAt(px, py);
        if (shown == null || at < 0 || this.scroll + at >= shown.rows().size()) {
            return;
        }
        this.picked = this.scroll + at;
        final String program = shown.rows().get(this.picked).program();
        if (UpdateManagerLayout.check(at).contains(px, py) && !this.ticked.remove(program)) {
            this.ticked.add(program);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(delta));
        return true;
    }

    /** What a button of the toolbar does: untick all, tick all, ask the Mirror again, install what is ticked. */
    private void pressed(final TextKey button, @Nullable final UpdatePayloads.Listing shown) {
        if (button == REFRESH) {
            PacketDistributor.sendToServer(new UpdatePayloads.Ask(this.host));
        } else if (shown != null && button == CLEAR) {
            this.ticked.clear();
        } else if (shown != null && button == SELECT_ALL) {
            shown.rows().forEach(update -> this.ticked.add(update.program()));
        } else if (shown != null && button == INSTALL_UPDATES && !this.ticked.isEmpty()) {
            PacketDistributor.sendToServer(new UpdatePayloads.Apply(this.host, List.copyOf(this.ticked)));
            // The updates land before the desktop asks again (one connection, in order), so it reads the new versions.
            ActiveDesktop.refreshActive();
        }
    }

    private void heads(final GuiGraphics g, final Font font) {
        final int y = this.top + UpdateManagerLayout.HEADS_Y;
        Draw.text(g, font, GameText.resolve(UPGRADE), this.left + UpdateManagerLayout.ICON_X, y, this.skin.dim());
        Draw.text(g, font, GameText.resolve(NEW_VERSION), this.left + UpdateManagerLayout.NEW_X, y, this.skin.dim());
        Draw.text(g, font, GameText.resolve(INSTALLED), this.left + UpdateManagerLayout.INSTALLED_X, y,
                this.skin.dim());
        Draw.text(g, font, GameText.resolve(SIZE), this.left + UpdateManagerLayout.SIZE_X, y, this.skin.dim());
        g.fill(this.left + 5, this.top + UpdateManagerLayout.RULE_Y, this.left + UpdateManagerLayout.WIDTH - 5,
                this.top + UpdateManagerLayout.RULE_Y + 1, this.skin.edge());
    }

    /** A row per update: its tick, its icon, its name and what it is, the two versions and its size. */
    private void rows(final GuiGraphics g, final Font font, final UpdatePayloads.Listing shown, final double mx,
                      final double my) {
        final List<UpdatePayloads.Update> updates = shown.rows();
        this.scroll = Math.max(0, Math.min(this.scroll, updates.size() - UpdateManagerLayout.ROWS));
        for (int i = 0; i < UpdateManagerLayout.ROWS && this.scroll + i < updates.size(); i++) {
            final UpdatePayloads.Update update = updates.get(this.scroll + i);
            final ProgramSpec spec = specOf(update.program());
            final Rect r = UpdateManagerLayout.row(i);
            final int x = this.left;
            final int y = this.top + r.y();
            this.skin.listRow(g, x + r.x(), y, r.w(), r.h(), r.contains(mx, my), this.scroll + i == this.picked);
            tick(g, font, UpdateManagerLayout.check(i), this.ticked.contains(update.program()));
            if (spec != null) {
                ProgramIcons.draw(g, x + UpdateManagerLayout.ICON_X, y + 1, UpdateManagerLayout.ICON,
                        UpdateManagerLayout.ICON, spec.id(), ICONS);
            }
            final int room = UpdateManagerLayout.NEW_X - UpdateManagerLayout.NAME_X - 6;
            Draw.text(g, font, Texts.clip(font, nameOf(update, spec), room), x + UpdateManagerLayout.NAME_X, y,
                    this.skin.text());
            Draw.text(g, font, Texts.clip(font, aboutOf(spec), room), x + UpdateManagerLayout.NAME_X,
                    y + UpdateManagerLayout.LINE, this.skin.dim());
            Draw.text(g, font, update.available(), x + UpdateManagerLayout.NEW_X, y + 4, this.skin.text());
            Draw.text(g, font, update.installed().isEmpty() ? "-" : update.installed(),
                    x + UpdateManagerLayout.INSTALLED_X, y + 4, this.skin.dim());
            Draw.text(g, font, GameText.resolve(MEGABYTES.with(sizeOf(spec))), x + UpdateManagerLayout.SIZE_X, y + 4,
                    this.skin.text());
        }
    }

    /** What the update picked out is: its name and version, and when it is installed. */
    private void about(final GuiGraphics g, final Font font, final UpdatePayloads.Listing shown) {
        g.fill(this.left + 5, this.top + UpdateManagerLayout.ABOUT_RULE_Y, this.left + UpdateManagerLayout.WIDTH - 5,
                this.top + UpdateManagerLayout.ABOUT_RULE_Y + 1, this.skin.edge());
        if (this.picked >= shown.rows().size()) {
            return;
        }
        final UpdatePayloads.Update update = shown.rows().get(this.picked);
        final ProgramSpec spec = specOf(update.program());
        final String name = nameOf(update, spec);
        Draw.text(g, font, name + " " + update.available(), this.left + UpdateManagerLayout.CHECK_X,
                this.top + UpdateManagerLayout.ABOUT_TITLE_Y, this.skin.text());
        final boolean desktop = spec != null && spec.kind() == ProgramKind.DESKTOP_ENVIRONMENT;
        final String said = GameText.resolve(desktop ? ABOUT_DESKTOP.text() : ABOUT_PROGRAM.with(name));
        int ly = this.top + UpdateManagerLayout.ABOUT_TEXT_Y;
        for (final FormattedCharSequence line : font.split(Component.literal(said),
                UpdateManagerLayout.WIDTH - 2 * UpdateManagerLayout.CHECK_X)) {
            Draw.text(g, font, line, this.left + UpdateManagerLayout.CHECK_X, ly, this.skin.dim());
            ly += UpdateManagerLayout.LINE + 1;
        }
    }

    /** The bar along the foot: how many updates are ticked and how much they come to. */
    private void status(final GuiGraphics g, final Font font, final UpdatePayloads.Listing shown) {
        final Rect bar = UpdateManagerLayout.STATUS;
        g.fill(this.left + bar.x(), this.top + bar.y(), this.left + bar.right(), this.top + bar.bottom(),
                this.skin.panelBg());
        int megabytes = 0;
        for (final UpdatePayloads.Update update : shown.rows()) {
            if (this.ticked.contains(update.program())) {
                megabytes += sizeOf(specOf(update.program()));
            }
        }
        Draw.text(g, font, GameText.resolve(SELECTED.with(this.ticked.size(), megabytes)), this.left + bar.x() + 6,
                this.top + bar.y() + 3, this.skin.text());
    }

    /** A tick box in the accent, filled and ticked when on. */
    private void tick(final GuiGraphics g, final Font font, final Rect box, final boolean on) {
        final int x = this.left + box.x();
        final int y = this.top + box.y();
        if (on) {
            g.fill(x, y, x + box.w(), y + box.h(), this.skin.accent());
            Draw.checkMark(g, font, x, y, box.w());
        } else {
            Draw.outline(g, x, y, box.w(), box.h(), this.skin.dim());
        }
    }

    private static int[] wordWidths(final Font font) {
        final int[] widths = new int[BUTTONS.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = font.width(GameText.resolve(BUTTONS.get(i)));
        }
        return widths;
    }

    @Nullable
    private static ProgramSpec specOf(final String id) {
        final ResourceLocation program = ResourceLocation.tryParse(id);
        return program == null ? null : OsRegistry.getProgram(program);
    }

    private static int sizeOf(@Nullable final ProgramSpec spec) {
        return spec == null ? 0 : spec.minDiskMb();
    }

    private static String nameOf(final UpdatePayloads.Update update, @Nullable final ProgramSpec spec) {
        return spec == null ? update.program() : GameText.resolve(spec.name());
    }

    private static String aboutOf(@Nullable final ProgramSpec spec) {
        return spec == null ? "" : GameText.resolve(TextKey.of(spec.descriptionKey(), spec.description()));
    }
}
