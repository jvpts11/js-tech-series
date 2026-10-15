/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.notification;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.hud.IHudElement;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.notification.NotificationBoard;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.NotificationTexts;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The notifications on the HUD, in the shape of the game's toasts: each in the frame of the player's style (the
 * game's toast in the game's style), with its level's icon, its title in its level's colour and the count of alike
 * ones, what it says, the mod it came from and what a click on it does, small, and a line along its foot that
 * shortens as its time runs out. They stack from the corner inwards, the newest nearest it.
 */
final class NotificationToasts implements IHudElement {

    private final NotificationBoard<Notification> board;
    /* Where each notification was drawn last, for a click to find. */
    private final List<Placed> placed = new ArrayList<>();

    /** How wide a notification is, and how tall with and without an action's line. */
    static final int WIDTH = 200;
    static final int HEIGHT = 36;
    static final int HEIGHT_WITH_ACTION = 44;
    private static final int GAP = 2;
    private static final int ICON = 16;
    private static final int TEXT_X = 30;
    private static final int PAD = 8;
    /* The time line is the title's colour, a little see-through. */
    private static final int RGB = 0xFFFFFF;
    private static final int BAR_ALPHA = 200 << 24;

    NotificationToasts(final NotificationBoard<Notification> board) {
        this.board = board;
    }

    /* Not under the history or the map of a place, which show the same notifications already. */
    @Override
    public boolean shown(final Minecraft minecraft) {
        return !(minecraft.screen instanceof NotificationHistoryScreen)
                && !(minecraft.screen instanceof NotificationPlaceScreen)
                && !this.board.showing(Util.getMillis()).isEmpty();
    }

    @Override
    public HudStack.Size size(final Minecraft minecraft) {
        int height = 0;
        for (final NotificationBoard.Entry<Notification> entry : this.board.showing(Util.getMillis())) {
            height += heightOf(entry) + GAP;
        }
        return new HudStack.Size(width(minecraft), Math.max(0, height - GAP));
    }

    @Override
    public void render(final GuiGraphics g, final DeltaTracker delta, final int x, final int y) {
        final Minecraft minecraft = Minecraft.getInstance();
        final long now = Util.getMillis();
        final List<NotificationBoard.Entry<Notification>> shown = this.board.showing(now);
        final StyleSkin skin = Styles.menuSkin(Styles.forMod(JsCore.MODID));
        final int w = width(minecraft);
        // The newest goes nearest the corner: at the top of a top corner's stack, at the foot of a bottom one's.
        final boolean bottom = NotificationsClient.corner().bottom();
        this.placed.clear();
        int top = y;
        for (int i = 0; i < shown.size(); i++) {
            final NotificationBoard.Entry<Notification> entry = shown.get(bottom ? shown.size() - 1 - i : i);
            final int h = heightOf(entry);
            draw(g, minecraft.font, skin, entry, x, top, w, h, now);
            this.placed.add(new Placed(entry, x, top, w, h));
            top += h + GAP;
        }
    }

    /** The notification drawn under the point, if any. */
    Optional<NotificationBoard.Entry<Notification>> at(final double mx, final double my) {
        for (final Placed one : this.placed) {
            if (mx >= one.x() && mx < one.x() + one.w() && my >= one.y() && my < one.y() + one.h()) {
                return Optional.of(one.entry());
            }
        }
        return Optional.empty();
    }

    /** The colour of a title of {@code level} in a style: red for an alert, yellow for a warning. */
    static int levelColour(final StyleSkin skin, final NotificationLevel level) {
        return switch (level) {
            case ALERT -> skin.colour(StyleRole.WARNING);
            case WARNING -> skin.colour(StyleRole.HEADING);
            case INFO -> skin.colour(StyleRole.MENU_TEXT);
        };
    }

    /** The sprite of {@code level}'s icon. */
    static ResourceLocation icon(final NotificationLevel level) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "notification/" + level.serializedName());
    }

    /** What the line of an action says, or nothing. */
    static Optional<String> actionLine(final Notification notification) {
        return notification.action().map(action -> GameText.resolve(switch (action) {
            case NotificationAction.OpenManual ignored -> NotificationTexts.OPEN_PAGE;
            case NotificationAction.ShowPlace ignored -> NotificationTexts.SHOW_PLACE;
        }));
    }

    private static void draw(final GuiGraphics g, final Font font, final StyleSkin skin,
                             final NotificationBoard.Entry<Notification> entry, final int x, final int y, final int w,
                             final int h, final long now) {
        final Notification notification = entry.item();
        final int colour = levelColour(skin, notification.level());
        final float small = skin.style().metrics().smallScale();
        skin.piece(g, StylePiece.NOTIFICATION, x, y, w, h);
        g.blitSprite(icon(notification.level()), x + PAD, y + (h - ICON) / 2 - 2, ICON, ICON);
        final int right = x + w - PAD;
        int titleRoom = right - (x + TEXT_X);
        if (entry.count() > 1) {
            final String count = GameText.resolve(NotificationTexts.COUNT.with(entry.count()));
            final int cw = skin.textWidth(font, count);
            skin.drawText(g, font, count, right - cw, y + 6, skin.colour(StyleRole.NOTICE));
            titleRoom -= cw + 4;
        }
        skin.drawText(g, font, Texts.clip(font, GameText.resolve(notification.title()), titleRoom), x + TEXT_X, y + 6,
                colour);
        final int smallRoom = (int) ((right - (x + TEXT_X)) / small);
        skin.drawText(g, font, Texts.clip(font, GameText.resolve(notification.body()), smallRoom), x + TEXT_X, y + 17,
                skin.colour(StyleRole.MENU_TEXT), small);
        skin.drawText(g, font, Texts.clip(font, NotificationsClient.modName(entry.mod()), smallRoom), x + TEXT_X,
                y + 24, skin.colour(StyleRole.MENU_DIM), small);
        actionLine(notification).ifPresent(line -> skin.drawText(g, font, Texts.clip(font, line, smallRoom),
                x + TEXT_X, y + 31, skin.colour(StyleRole.INFO), small));
        final int left = (int) ((w - 2 * 4) * NotificationBoard.left(entry, now));
        g.fill(x + 4, y + h - 4, x + 4 + left, y + h - 3, (colour & RGB) | BAR_ALPHA);
    }

    private static int heightOf(final NotificationBoard.Entry<Notification> entry) {
        return entry.item().action().isPresent() ? HEIGHT_WITH_ACTION : HEIGHT;
    }

    private static int width(final Minecraft minecraft) {
        return Math.min(WIDTH, minecraft.getWindow().getGuiScaledWidth() - 2 * HudStack.MARGIN);
    }

    /* Where one notification was drawn. */
    private record Placed(NotificationBoard.Entry<Notification> entry, int x, int y, int w, int h) {
    }
}
