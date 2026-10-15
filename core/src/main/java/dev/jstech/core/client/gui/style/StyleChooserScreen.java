/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.style;

import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.gui.layout.StyleChooserLayout;
import dev.jstech.core.gui.screen.InterfaceTexts;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

/**
 * The chooser of styles, opened from the player's settings: down the left, whose screens the choice is for (every
 * screen, or one mod's); in the middle, the styles the resource packs bring; on the right, a machine's screen drawn in
 * the style under the pointer, so a style is seen before it is chosen. A choice goes into the settings screen's
 * changes, kept with Save or dropped with Cancel.
 */
public final class StyleChooserScreen extends Screen {

    private final Screen parent;
    private final ConfigDraft draft;
    private final List<Scope> scopes = new ArrayList<>();
    private int scope;
    private int left;
    private int top;
    @Nullable
    private Style hovered;

    public StyleChooserScreen(final Screen parent, final ConfigDraft draft) {
        super(GameText.component(InterfaceTexts.CHOOSER_TITLE));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.draft = Objects.requireNonNull(draft, "draft");
        scopes.add(new Scope(null, GameText.resolve(InterfaceTexts.EVERY_SCREEN)));
        for (final IModInfo mod : ModList.get().getMods()) {
            if (!mod.getModId().equals(JsCore.MODID) && dependsOnCore(mod)) {
                scopes.add(new Scope(mod.getModId(), GameText.resolve(InterfaceTexts.SCREENS_OF.with(
                        mod.getDisplayName()))));
            }
        }
    }

    /** The scopes down the left, for a test to read. */
    public List<String> scopeLabels() {
        final List<String> out = new ArrayList<>();
        for (final Scope one : scopes) {
            out.add(one.label());
        }
        return out;
    }

    /** Picks {@code style} for the open scope, as a click on its row does; none puts the scope back to its default. */
    public void choose(@Nullable final Style style) {
        final Scope open = scopes.get(scope);
        if (open.modid() == null) {
            draft.choose(StyleSettings.EVERYTHING, style == null ? "" : style.id().toString());
        } else {
            final Map<String, String> byMod = new HashMap<>(draft.value(StyleSettings.BY_MOD));
            if (style == null) {
                byMod.remove(open.modid());
            } else {
                byMod.put(open.modid(), style.id().toString());
            }
            draft.choose(StyleSettings.BY_MOD, Map.copyOf(byMod));
        }
    }

    /** Opens the scope at {@code index}, as a click on it does. */
    public void openScope(final int index) {
        scope = Math.max(0, Math.min(scopes.size() - 1, index));
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final StyleSkin chrome = Styles.skin(Styles.forMod(JsCore.MODID));
        chrome.window(g, left, top, StyleChooserLayout.WIDTH, StyleChooserLayout.HEIGHT);
        chrome.drawText(g, font, title.getString(), left + StyleChooserLayout.PAD, top + StyleChooserLayout.PAD,
                chrome.colour(StyleRole.TITLE));
        drawScopes(g, chrome, mouseX, mouseY);
        hovered = drawStyles(g, chrome, mouseX, mouseY);
        drawPreview(g, hovered != null ? hovered : current());
        final int dx = left + StyleChooserLayout.doneX();
        final int dy = top + StyleChooserLayout.doneY();
        chrome.button(g, font, dx, dy, StyleChooserLayout.DONE_WIDTH, StyleChooserLayout.DONE_HEIGHT,
                GameText.resolve(InterfaceTexts.DONE), over(mouseX, mouseY, dx, dy, StyleChooserLayout.DONE_WIDTH,
                        StyleChooserLayout.DONE_HEIGHT), false, true);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int x = (int) mouseX;
        final int y = (int) mouseY;
        final int rowsTop = top + StyleChooserLayout.columnsTop();
        final int row = (y - rowsTop) / StyleChooserLayout.ROW;
        if (y >= rowsTop && row < StyleChooserLayout.rows()) {
            if (x >= left + StyleChooserLayout.PAD && x < left + StyleChooserLayout.PAD
                    + StyleChooserLayout.SCOPES_WIDTH && row < scopes.size()) {
                openScope(row);
                return true;
            }
            final List<Style> styles = Styles.all();
            if (x >= left + StyleChooserLayout.stylesX() && x < left + StyleChooserLayout.stylesX()
                    + StyleChooserLayout.STYLES_WIDTH && row <= styles.size()) {
                choose(row == 0 ? null : styles.get(row - 1));
                return true;
            }
        }
        if (over(x, y, left + StyleChooserLayout.doneX(), top + StyleChooserLayout.doneY(),
                StyleChooserLayout.DONE_WIDTH, StyleChooserLayout.DONE_HEIGHT)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    protected void init() {
        left = (width - StyleChooserLayout.WIDTH) / 2;
        top = (height - StyleChooserLayout.HEIGHT) / 2;
    }

    private void drawScopes(final GuiGraphics g, final StyleSkin chrome, final int mouseX, final int mouseY) {
        final int x = left + StyleChooserLayout.PAD;
        final int y = top + StyleChooserLayout.columnsTop();
        chrome.piece(g, StylePiece.WELL, x, y, StyleChooserLayout.SCOPES_WIDTH, StyleChooserLayout.columnsHeight());
        for (int i = 0; i < Math.min(scopes.size(), StyleChooserLayout.rows()); i++) {
            final int ry = y + i * StyleChooserLayout.ROW;
            chrome.listRow(g, x + 1, ry, StyleChooserLayout.SCOPES_WIDTH - 2, StyleChooserLayout.ROW,
                    over(mouseX, mouseY, x, ry, StyleChooserLayout.SCOPES_WIDTH, StyleChooserLayout.ROW), i == scope);
            chrome.drawText(g, font, clip(scopes.get(i).label(), StyleChooserLayout.SCOPES_WIDTH - 6), x + 3, ry + 2,
                    chrome.listRowText(i == scope));
        }
    }

    /* The styles of the open scope, the one chosen marked; returns the one under the pointer, if any. */
    @Nullable
    private Style drawStyles(final GuiGraphics g, final StyleSkin chrome, final int mouseX, final int mouseY) {
        final int x = left + StyleChooserLayout.stylesX();
        final int y = top + StyleChooserLayout.columnsTop();
        chrome.piece(g, StylePiece.WELL, x, y, StyleChooserLayout.STYLES_WIDTH, StyleChooserLayout.columnsHeight());
        final List<Style> styles = Styles.all();
        final String chosen = chosenId();
        Style under = null;
        for (int i = 0; i <= styles.size() && i < StyleChooserLayout.rows(); i++) {
            final int ry = y + i * StyleChooserLayout.ROW;
            final Style style = i == 0 ? null : styles.get(i - 1);
            final boolean selected = style == null ? chosen.isEmpty() : chosen.equals(style.id().toString());
            final boolean hover = over(mouseX, mouseY, x, ry, StyleChooserLayout.STYLES_WIDTH, StyleChooserLayout.ROW);
            if (hover) {
                under = style;
            }
            chrome.listRow(g, x + 1, ry, StyleChooserLayout.STYLES_WIDTH - 2, StyleChooserLayout.ROW, hover, selected);
            final String name = style == null ? GameText.resolve(scopes.get(scope).modid() == null
                    ? InterfaceTexts.MODS_OWN : InterfaceTexts.FOR_EVERY_SCREEN) : I18n.get(style.nameKey());
            chrome.drawText(g, font, clip(name, StyleChooserLayout.STYLES_WIDTH - 6), x + 3, ry + 2,
                    chrome.listRowText(selected));
        }
        return under;
    }

    /* A small machine's screen drawn in {@code style}: its window, title, a slot, a bar, a button and a check box. */
    private void drawPreview(final GuiGraphics g, final Style style) {
        final StyleSkin skin = Styles.skin(style);
        final int x = left + StyleChooserLayout.previewX();
        final int y = top + StyleChooserLayout.columnsTop();
        final int w = StyleChooserLayout.previewWidth();
        skin.window(g, x, y, w, StyleChooserLayout.columnsHeight());
        final int pad = 6;
        skin.drawText(g, font, GameText.resolve(InterfaceTexts.SAMPLE_TITLE), x + pad, y + pad,
                skin.colour(StyleRole.TITLE));
        skin.slot(g, x + pad + 1, y + pad + 15);
        skin.progress(g, x + pad + 22, y + pad + 20, w - 2 * pad - 44, 8, 0.42);
        skin.slot(g, x + w - pad - 17, y + pad + 15);
        skin.drawText(g, font, clip(GameText.resolve(InterfaceTexts.SAMPLE_TEXT), w - 2 * pad), x + pad, y + pad + 38,
                skin.text());
        skin.drawText(g, font, clip(GameText.resolve(InterfaceTexts.SAMPLE_DIM), w - 2 * pad), x + pad, y + pad + 50,
                skin.dim());
        final int bh = Math.min(20, style.metrics().controlHeight());
        skin.button(g, font, x + pad, y + pad + 64, w - 2 * pad, bh, GameText.resolve(InterfaceTexts.SAMPLE_BUTTON),
                false, false, false);
        final int box = Math.min(14, style.metrics().controlHeight());
        skin.checkbox(g, font, x + pad, y + pad + 70 + bh, box, true, false);
        skin.drawText(g, font, clip(GameText.resolve(InterfaceTexts.SAMPLE_CHECK), w - 3 * pad - box),
                x + 2 * pad + box, y + pad + 73 + bh, skin.text());
        skin.separator(g, x + pad, y + pad + 78 + bh + box, w - 2 * pad);
    }

    /* The style the open scope wears now, with the changes not yet kept. */
    private Style current() {
        final String id = chosenId();
        final ResourceLocation parsed = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        return parsed == null ? Styles.game() : Styles.get(parsed).orElseGet(Styles::game);
    }

    private String chosenId() {
        final Scope open = scopes.get(scope);
        if (open.modid() == null) {
            return draft.value(StyleSettings.EVERYTHING);
        }
        return draft.value(StyleSettings.BY_MOD).getOrDefault(open.modid(), "");
    }

    private String clip(final String text, final int room) {
        return font.plainSubstrByWidth(text, room);
    }

    private static boolean over(final int mouseX, final int mouseY, final int x, final int y, final int w,
                                final int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static boolean dependsOnCore(final IModInfo mod) {
        return mod.getDependencies().stream().anyMatch(dependency -> dependency.getModId().equals(JsCore.MODID));
    }

    /**
     * Whose screens a choice is for.
     *
     * @param modid the mod, or none for every screen
     * @param label what the list calls it
     */
    private record Scope(@Nullable String modid, String label) {
    }
}
