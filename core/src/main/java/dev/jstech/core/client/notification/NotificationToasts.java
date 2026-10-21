/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.notification;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.TextScale;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.hud.IHudElement;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.gui.TextSizes;
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

    /** How wide a notification is at the normal text size. */
    static final int WIDTH = 200;
    private static final int GAP = 2;
    private static final int ICON = 16;
    private static final int TEXT_X = 30;
    private static final int PAD = 8;
    /*
     * Down a notification at the normal text size: the title's top, its line, the room under it, each small line, and
     * the room left at the foot for the time line, without an action's line and with one.
     */
    private static final int TITLE_Y = 6;
    private static final int LINE = 9;
    private static final int UNDER_TITLE = 2;
    private static final int SMALL_LINE = 7;
    private static final int FOOT = 5;
    private static final int FOOT_WITH_ACTION = 6;
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
        final float small = skin().style().metrics().smallScale();
        int height = 0;
        for (final NotificationBoard.Entry<Notification> entry : this.board.showing(Util.getMillis())) {
            height += heightOf(entry, small) + GAP;
        }
        return new HudStack.Size(width(minecraft), Math.max(0, height - GAP));
    }

    /* In the HUD editor, as tall as one notification at least, so that the box stands while none shows. */
    @Override
    public HudStack.Size editSize(final Minecraft minecraft) {
        final HudStack.Size now = size(minecraft);
        final int one = Lines.of(false, skin().style().metrics().smallScale()).height();
        return new HudStack.Size(now.width(), Math.max(one, now.height()));
    }

    @Override
    public void render(final GuiGraphics g, final DeltaTracker delta, final int x, final int y) {
        final Minecraft minecraft = Minecraft.getInstance();
        final long now = Util.getMillis();
        final List<NotificationBoard.Entry<Notification>> shown = this.board.showing(now);
        final StyleSkin skin = skin();
        final float small = skin.style().metrics().smallScale();
        final int w = width(minecraft);
        // The newest goes nearest the corner: at the top of a top corner's stack, at the foot of a bottom one's.
        final boolean bottom = NotificationsClient.corner().bottom();
        this.placed.clear();
        int top = y;
        for (int i = 0; i < shown.size(); i++) {
            final NotificationBoard.Entry<Notification> entry = shown.get(bottom ? shown.size() - 1 - i : i);
            final int h = heightOf(entry, small);
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
        // The text at the size the player wants it; the icon and the frame keep the GUI scale.
        final float title = TextScale.factor();
        final float small = TextScale.of(skin.style().metrics().smallScale());
        final Lines lines = Lines.of(notification.action().isPresent(), skin.style().metrics().smallScale());
        skin.piece(g, StylePiece.NOTIFICATION, x, y, w, h);
        g.blitSprite(icon(notification.level()), x + PAD, y + (h - ICON) / 2 - 2, ICON, ICON);
        final int right = x + w - PAD;
        int titleRoom = right - (x + TEXT_X);
        if (entry.count() > 1) {
            final String count = GameText.resolve(NotificationTexts.COUNT.with(entry.count()));
            final int cw = TextScale.width(skin, font, count, title);
            TextScale.draw(skin, g, font, count, right - cw, y + TITLE_Y, skin.colour(StyleRole.NOTICE), title);
            titleRoom -= cw + 4;
        }
        TextScale.draw(skin, g, font, Texts.clip(font, GameText.resolve(notification.title()),
                (int) (titleRoom / title)), x + TEXT_X, y + TITLE_Y, colour, title);
        final int smallRoom = (int) ((right - (x + TEXT_X)) / small);
        skin.drawText(g, font, Texts.clip(font, GameText.resolve(notification.body()), smallRoom), x + TEXT_X,
                y + lines.body(), skin.colour(StyleRole.MENU_TEXT), small);
        skin.drawText(g, font, Texts.clip(font, NotificationsClient.modName(entry.mod()), smallRoom), x + TEXT_X,
                y + lines.mod(), skin.colour(StyleRole.MENU_DIM), small);
        actionLine(notification).ifPresent(line -> skin.drawText(g, font, Texts.clip(font, line, smallRoom),
                x + TEXT_X, y + lines.action(), skin.colour(StyleRole.INFO), small));
        final int left = (int) ((w - 2 * 4) * NotificationBoard.left(entry, now));
        g.fill(x + 4, y + h - 4, x + 4 + left, y + h - 3, (colour & RGB) | BAR_ALPHA);
    }

    private static int heightOf(final NotificationBoard.Entry<Notification> entry, final float small) {
        return Lines.of(entry.item().action().isPresent(), small).height();
    }

    /* As wide as the game's toast, wider with a larger text, and never wider than the screen. */
    private static int width(final Minecraft minecraft) {
        return Math.min(TextSizes.grown(WIDTH, TextScale.factor()),
                minecraft.getWindow().getGuiScaledWidth() - 2 * HudStack.MARGIN);
    }

    private static StyleSkin skin() {
        return Styles.menuSkin(Styles.forMod(JsCore.MODID));
    }

    /*
     * Where a notification's lines go down it, and how tall it is: the game's toast at the normal text size, each line
     * grown with a larger text.
     */
    private record Lines(int body, int mod, int action, int height) {

        private static Lines of(final boolean action, final float smallScale) {
            final int titleLine = TextSizes.grown(LINE, TextScale.factor());
            final int smallLine = TextScale.larger()
                    ? Math.max(SMALL_LINE, TextSizes.grown(LINE, TextScale.of(smallScale))) : SMALL_LINE;
            final int body = TITLE_Y + titleLine + UNDER_TITLE;
            final int mod = body + smallLine;
            final int last = action ? mod + smallLine : mod;
            return new Lines(body, mod, mod + smallLine, last + smallLine + (action ? FOOT_WITH_ACTION : FOOT));
        }
    }

    /* Where one notification was drawn. */
    private record Placed(NotificationBoard.Entry<Notification> entry, int x, int y, int w, int h) {
    }
}
