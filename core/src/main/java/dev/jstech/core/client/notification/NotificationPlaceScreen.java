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
import dev.jstech.core.client.gui.component.MapView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.gui.layout.NotificationHistoryLayout;
import dev.jstech.core.gui.layout.NotificationPlaceLayout;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSprite;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.notification.NotificationTexts;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The place a notification is about, on a map: the land round it in the game's map colours, the place marked and the
 * player too when they are near, and under it where the place is and how far, or that it is in another dimension.
 */
public final class NotificationPlaceScreen extends Screen {

    @Nullable
    private final Screen parent;
    private final Notification notification;
    private final NotificationAction.ShowPlace place;
    private final Panel root = new Panel();
    private final MapView map;
    private final Button done;
    private StyleSkin skin;
    private StyleSkin windowSkin;

    public NotificationPlaceScreen(final Notification notification, final NotificationAction.ShowPlace place,
                                   @Nullable final Screen parent) {
        super(GameText.component(notification.title()));
        this.notification = notification;
        this.place = place;
        this.parent = parent;
        this.map = new MapView(place::pos, this::markers, GameText.resolve(NotificationTexts.NORTH));
        this.done = new Button(GameText.resolve(NotificationTexts.DONE), this::onClose);
        this.root.add(this.map);
        this.root.add(this.done);
    }

    /** The line under the map: where the place is, and how far, or that it is in another dimension. */
    public String where() {
        final LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        final String at = this.place.pos().toShortString();
        final String dimension = this.place.dimension().location().toString();
        if (player == null || !player.level().dimension().equals(this.place.dimension())) {
            return GameText.resolve(NotificationTexts.PLACE_ELSEWHERE.with(at, dimension));
        }
        final long far = Math.round(Math.sqrt(player.blockPosition().distSqr(this.place.pos())));
        return GameText.resolve(NotificationTexts.PLACE_AT.with(at, dimension, far));
    }

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
        final String title = Texts.clip(this.font, GameText.resolve(this.notification.title()),
                this.width - 2 * NotificationPlaceLayout.PAD);
        this.skin.drawText(g, this.font, title, (this.width - this.skin.textWidth(this.font, title)) / 2,
                NotificationPlaceLayout.TITLE_Y, NotificationToasts.levelColour(this.skin, this.notification.level()));
        this.skin.separator(g, 0, NotificationPlaceLayout.HEADER, this.width);
        this.map.render(g, new UiContext(this.windowSkin, this.font, mouseX, mouseY, partialTick));
        final StyleSprite line = this.skin.style().piece(StylePiece.SEPARATOR_FOOTER);
        this.skin.piece(g, StylePiece.SEPARATOR_FOOTER, 0, this.height - NotificationPlaceLayout.FOOTER, this.width,
                line instanceof StyleSprite.Tile tile ? tile.height() : 1);
        final NotificationHistoryLayout.Rect where = NotificationPlaceLayout.where(this.width, this.height);
        this.skin.drawText(g, this.font, Texts.clip(this.font, where(), where.w()), where.x(), where.y(),
                this.skin.colour(StyleRole.MENU_TEXT));
        this.done.render(g, new UiContext(this.skin, this.font, mouseX, mouseY, partialTick));
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        return this.root.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        return this.root.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        this.root.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.skin = Styles.menuSkin(Styles.forMod(JsCore.MODID));
        this.windowSkin = Styles.skin(this.skin.style());
        place();
    }

    private void place() {
        final NotificationHistoryLayout.Rect area = NotificationPlaceLayout.map(this.width, this.height);
        this.map.setBounds(area.x(), area.y(), area.w(), area.h());
        final NotificationHistoryLayout.Rect button = NotificationPlaceLayout.done(this.width, this.height);
        this.done.setBounds(button.x(), button.y(), button.w(), button.h());
    }

    /* The place, and the player when they are in its dimension. */
    private List<MapView.Marker> markers() {
        final List<MapView.Marker> out = new ArrayList<>();
        out.add(new MapView.Marker(this.place.pos(), GameText.resolve(NotificationTexts.HERE),
                NotificationToasts.levelColour(this.skin, this.notification.level())));
        final LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        if (player != null && player.level().dimension().equals(this.place.dimension())) {
            out.add(new MapView.Marker(player.blockPosition(), GameText.resolve(NotificationTexts.YOU),
                    this.skin.colour(StyleRole.INFO)));
        }
        return out;
    }
}
