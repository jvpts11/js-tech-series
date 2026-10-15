/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.notification;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.gui.layout.NotificationHistoryLayout;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSprite;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationBoard;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.NotificationSettings;
import dev.jstech.core.notification.NotificationTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * The history of notifications, a menu over the game drawn in the player's style: the newest first, each with its
 * icon, its title in its level's colour and the count of alike ones, what it says, its mod and when it came, and a
 * cross that takes it off the list. Buttons over the list show only a level or a mod and move the notifications to
 * another corner; a click on a line does what it offers, and the right button on one silences its kind or its mod.
 * The footer turns "do not disturb" on and off.
 */
public final class NotificationHistoryScreen extends Screen {

    @Nullable
    private final Screen parent;
    private final Panel root = new Panel();
    private final List<Button> filters = new ArrayList<>();
    private final Button mods;
    private final Button place;
    private final Checkbox doNotDisturb;
    private final Button done;
    private final ListView<NotificationBoard.Entry<Notification>> list;
    private final ContextMenu menu = new ContextMenu(MENU_WIDTH, MENU_ROW);
    private StyleSkin skin;
    private NotificationHistoryLayout.Plan plan;
    @Nullable
    private NotificationLevel level;
    private String mod = "";

    private static final int MENU_WIDTH = 120;
    private static final int MENU_ROW = 12;
    private static final List<NotificationLevel> FILTERS = List.of(NotificationLevel.ALERT, NotificationLevel.WARNING,
            NotificationLevel.INFO);
    private static final List<String> PLACES = List.of(NotificationSettings.TOP_RIGHT, NotificationSettings.TOP_LEFT,
            NotificationSettings.BOTTOM_LEFT, NotificationSettings.BOTTOM_RIGHT);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final String X = "x";

    public NotificationHistoryScreen(@Nullable final Screen parent) {
        super(GameText.component(NotificationTexts.TITLE));
        this.parent = parent;
        this.filters.add(new Button(GameText.resolve(NotificationTexts.ALL), () -> setLevel(null)));
        for (final NotificationLevel one : FILTERS) {
            this.filters.add(new Button(GameText.resolve(filterName(one)), () -> setLevel(one)));
        }
        this.mods = new Button(() -> GameText.resolve(NotificationTexts.MODS.with(modLabel())), this::nextMod);
        this.place = new Button(() -> GameText.resolve(NotificationTexts.PLACE.with(placeName(
                NotificationSettings.place()))), this::nextPlace);
        this.doNotDisturb = new Checkbox(() -> GameText.resolve(NotificationTexts.DO_NOT_DISTURB),
                NotificationSettings::doNotDisturb,
                () -> NotificationsClient.setDoNotDisturb(!NotificationSettings.doNotDisturb()))
                .setBoxSize(NotificationHistoryLayout.BOX);
        this.done = new Button(GameText.resolve(NotificationTexts.DONE), this::onClose);
        this.list = new ListView<>(this::entries, NotificationHistoryLayout.ROW, this::renderRow)
                .setSelectable(true).setOnClick(this::clickRow);
        this.filters.forEach(this.root::add);
        this.root.add(this.mods);
        this.root.add(this.place);
        this.root.add(this.doNotDisturb);
        this.root.add(this.done);
        this.root.add(this.list);
    }

    // What a test reads and does, as a player would.

    /** The notifications the list shows now, newest first. */
    public List<NotificationBoard.Entry<Notification>> entries() {
        final List<NotificationBoard.Entry<Notification>> out = new ArrayList<>();
        for (final NotificationBoard.Entry<Notification> entry : NotificationsClient.board().history()) {
            if ((this.level == null || entry.level() == this.level) && (this.mod.isEmpty() || entry.mod().equals(
                    this.mod))) {
                out.add(entry);
            }
        }
        return out;
    }

    /** Shows only the notifications of {@code shown}, or every one, as a filter's button does. */
    public void setLevel(@Nullable final NotificationLevel shown) {
        this.level = shown;
        this.list.setSelected(-1);
    }

    /** Shows the next mod's notifications, as the button of mods does: every mod after the last. */
    public void nextMod() {
        final List<String> present = presentMods();
        final int at = present.indexOf(this.mod);
        this.mod = at + 1 < present.size() ? present.get(at + 1) : "";
        this.list.setSelected(-1);
    }

    /** Moves the notifications to the next corner, as the button of places does. */
    public void nextPlace() {
        final int at = PLACES.indexOf(NotificationSettings.place());
        NotificationsClient.setPlace(PLACES.get((at + 1) % PLACES.size()));
    }

    // The game's screen.

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        if (this.skin.style().piece(StylePiece.MENU_GROUND) instanceof StyleSprite.GameMenu) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        } else {
            this.skin.piece(g, StylePiece.MENU_GROUND, 0, 0, this.width, this.height);
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        place();
        super.render(g, mouseX, mouseY, partialTick);
        final boolean menuOpen = this.menu.isOpen();
        final UiContext ctx = new UiContext(this.skin, this.font, menuOpen ? -1 : mouseX, menuOpen ? -1 : mouseY,
                partialTick);
        final String title = GameText.resolve(NotificationTexts.TITLE);
        this.skin.drawText(g, this.font, title, (this.width - this.skin.textWidth(this.font, title)) / 2,
                NotificationHistoryLayout.TITLE_Y, this.skin.colour(StyleRole.MENU_TEXT));
        this.skin.separator(g, 0, NotificationHistoryLayout.HEADER, this.width);
        final NotificationHistoryLayout.Rect area = this.plan.list();
        this.skin.piece(g, StylePiece.LIST_GROUND, area.x(), area.y(), area.w(), area.h());
        this.root.render(g, ctx);
        if (entries().isEmpty()) {
            final String empty = GameText.resolve(NotificationTexts.EMPTY);
            this.skin.drawText(g, this.font, empty, (this.width - this.skin.textWidth(this.font, empty)) / 2,
                    area.y() + 8, this.skin.colour(StyleRole.MENU_DIM));
        }
        drawFooter(g);
        if (menuOpen) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            this.menu.render(g, new UiContext(Styles.skin(this.skin.style()), this.font, mouseX, mouseY,
                    partialTick));
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (this.menu.isOpen()) {
            this.menu.mouseClicked(mx, my, button);
            return true;
        }
        return this.root.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        return this.root.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double scrollX, final double scrollY) {
        return this.root.mouseScrolled(mx, my, scrollY);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.menu.isOpen() && this.menu.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.skin = Styles.menuSkin(Styles.forMod(JsCore.MODID));
        place();
    }

    /* Puts everything in place for this frame, the buttons as wide as their labels. */
    private void place() {
        final List<Integer> labels = this.filters.stream().map(one -> this.skin.textWidth(this.font, one.label()))
                .toList();
        this.plan = NotificationHistoryLayout.plan(this.width, this.height, labels,
                this.skin.textWidth(this.font, this.mods.label()), this.skin.textWidth(this.font, this.place.label()),
                this.skin.textWidth(this.font, GameText.resolve(NotificationTexts.DO_NOT_DISTURB)));
        for (int i = 0; i < this.filters.size(); i++) {
            final NotificationHistoryLayout.Rect at = this.plan.filters().get(i);
            this.filters.get(i).setBounds(at.x(), at.y(), at.w(), at.h());
            // The filter shown is the one that cannot be pressed, as the game's own tabs of buttons do.
            this.filters.get(i).setEnabled(i == 0 ? this.level != null : this.level != FILTERS.get(i - 1));
        }
        bounds(this.mods, this.plan.mods());
        bounds(this.place, this.plan.place());
        bounds(this.doNotDisturb, this.plan.checkbox());
        bounds(this.done, this.plan.done());
        final NotificationHistoryLayout.Rect area = this.plan.list();
        this.list.setBounds(area.x() + 2, area.y() + 2, area.w() - 4, area.h() - 4);
    }

    private void drawFooter(final GuiGraphics g) {
        final StyleSprite line = this.skin.style().piece(StylePiece.SEPARATOR_FOOTER);
        this.skin.piece(g, StylePiece.SEPARATOR_FOOTER, 0, this.plan.footerTop(), this.width,
                line instanceof StyleSprite.Tile tile ? tile.height() : 1);
        if (this.plan.hintRoom() <= 0) {
            return;
        }
        final float small = this.skin.style().metrics().smallScale();
        final List<FormattedCharSequence> lines = this.font.split(GameText.component(NotificationTexts.HINT),
                (int) (this.plan.hintRoom() / small));
        int y = this.plan.checkbox().y() + (lines.size() > 1 ? 3 : 7);
        for (final FormattedCharSequence hint : lines.subList(0, Math.min(2, lines.size()))) {
            g.pose().pushPose();
            g.pose().translate(this.plan.hintX(), y, 0);
            g.pose().scale(small, small, 1f);
            Draw.text(g, this.font, hint, 0, 0, this.skin.colour(StyleRole.MENU_DIM));
            g.pose().popPose();
            y += Math.round(this.font.lineHeight * small) + 1;
        }
    }

    private void renderRow(final GuiGraphics g, final UiContext ctx, final NotificationBoard.Entry<Notification> entry,
                           final int index, final int x, final int y, final int w, final int h, final boolean hovered,
                           final boolean selected) {
        if (selected) {
            this.skin.piece(g, StylePiece.LIST_SELECTED, x, y, w, h - 1);
        } else if (hovered) {
            this.skin.piece(g, StylePiece.LIST_HOVER, x, y, w, h - 1);
        }
        final Notification notification = entry.item();
        final NotificationHistoryLayout.Row row = NotificationHistoryLayout.row(x, w);
        g.blitSprite(NotificationToasts.icon(notification.level()), row.iconX(), y + 5, 16, 16);
        String title = GameText.resolve(notification.title());
        if (entry.count() > 1) {
            title += "  " + GameText.resolve(NotificationTexts.COUNT.with(entry.count()));
        }
        this.skin.drawText(g, this.font, Texts.clip(this.font, title, row.textRoom()), row.textX(), y + 3,
                NotificationToasts.levelColour(this.skin, notification.level()));
        this.skin.drawText(g, this.font, Texts.clip(this.font, GameText.resolve(notification.body()),
                row.textRoom()), row.textX(), y + 14, this.skin.colour(StyleRole.MENU_TEXT));
        this.skin.drawText(g, this.font, Texts.clip(this.font, NotificationsClient.modName(entry.mod()),
                NotificationHistoryLayout.MOD), row.modX(), y + 14, this.skin.colour(StyleRole.MENU_DIM));
        // The board keeps the game's own clock; the time of day is the wall clock's, as far back as that.
        final String when = WHEN.format(Instant.ofEpochMilli(System.currentTimeMillis() - (Util.getMillis()
                - entry.lastAt())).atZone(ZoneId.systemDefault()));
        this.skin.drawText(g, this.font, when, row.timeRight() - this.skin.textWidth(this.font, when), y + 3,
                this.skin.colour(StyleRole.MENU_DIM));
        final boolean overCross = ctx.over(row.crossX(), y + 2, NotificationHistoryLayout.CROSS,
                NotificationHistoryLayout.CROSS);
        final StylePiece cross = overCross ? StylePiece.CLOSE_HOVER : StylePiece.CLOSE;
        if (this.skin.style().piece(cross) instanceof StyleSprite.Nothing) {
            this.skin.drawText(g, this.font, X, row.crossX() + 4, y + 5, overCross
                    ? this.skin.colour(StyleRole.MENU_TEXT) : this.skin.colour(StyleRole.MENU_DIM));
        } else {
            this.skin.piece(g, cross, row.crossX(), y + 2, NotificationHistoryLayout.CROSS,
                    NotificationHistoryLayout.CROSS);
        }
    }

    /* A left click on a row's cross takes it off; elsewhere on the row, does what it offers. The right silences. */
    private void clickRow(final int index, final int button, final double mx, final double my) {
        final List<NotificationBoard.Entry<Notification>> shown = entries();
        if (index < 0 || index >= shown.size()) {
            return;
        }
        final NotificationBoard.Entry<Notification> entry = shown.get(index);
        if (button == 1) {
            openMenu(entry, (int) mx, (int) my);
            return;
        }
        final int[] rect = this.list.rowRect(index);
        final NotificationHistoryLayout.Row row = NotificationHistoryLayout.row(rect[0], rect[2]);
        if (mx >= row.crossX() && mx < row.crossX() + NotificationHistoryLayout.CROSS) {
            NotificationsClient.board().forget(entry.id());
            this.list.setSelected(-1);
            return;
        }
        NotificationsClient.act(entry.item(), this);
    }

    private void openMenu(final NotificationBoard.Entry<Notification> entry, final int x, final int y) {
        final boolean kindSilenced = NotificationSettings.silencedKinds().contains(entry.kind());
        final boolean modSilenced = NotificationSettings.silencedMods().contains(entry.mod());
        final String kindName = GameText.resolve(NotificationsClient.kindName(entry.kind()));
        final String modName = NotificationsClient.modName(entry.mod());
        final List<ContextMenu.Item> items = List.of(
                new ContextMenu.Item(GameText.resolve((kindSilenced ? NotificationTexts.HEAR_KIND
                        : NotificationTexts.SILENCE_KIND).with(kindName)), true,
                        () -> NotificationsClient.toggleKind(entry.kind())),
                new ContextMenu.Item(GameText.resolve((modSilenced ? NotificationTexts.HEAR_MOD
                        : NotificationTexts.SILENCE_MOD).with(modName)), true,
                        () -> NotificationsClient.toggleMod(entry.mod())),
                ContextMenu.Item.separator(),
                new ContextMenu.Item(GameText.resolve(NotificationTexts.FORGET), true,
                        () -> NotificationsClient.board().forget(entry.id())));
        this.menu.open(items, x, y, 0, 0, this.width, this.height);
    }

    /* The mods whose notifications are in the history, by name. */
    private List<String> presentMods() {
        final List<String> out = new ArrayList<>();
        for (final NotificationBoard.Entry<Notification> entry : NotificationsClient.board().history()) {
            if (!out.contains(entry.mod())) {
                out.add(entry.mod());
            }
        }
        out.sort(Comparator.comparing(NotificationsClient::modName));
        return out;
    }

    private String modLabel() {
        return this.mod.isEmpty() ? GameText.resolve(NotificationTexts.ALL) : NotificationsClient.modName(this.mod);
    }

    private static TextKey filterName(final NotificationLevel shown) {
        return switch (shown) {
            case ALERT -> NotificationTexts.ALERTS;
            case WARNING -> NotificationTexts.WARNINGS;
            case INFO -> NotificationTexts.INFORMATION;
        };
    }

    private static String placeName(final String corner) {
        return GameText.resolve(switch (corner) {
            case NotificationSettings.TOP_LEFT -> NotificationTexts.TOP_LEFT;
            case NotificationSettings.BOTTOM_LEFT -> NotificationTexts.BOTTOM_LEFT;
            case NotificationSettings.BOTTOM_RIGHT -> NotificationTexts.BOTTOM_RIGHT;
            default -> NotificationTexts.TOP_RIGHT;
        });
    }

    private static void bounds(final UiComponent component, final NotificationHistoryLayout.Rect at) {
        component.setBounds(at.x(), at.y(), at.w(), at.h());
    }
}
