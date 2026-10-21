/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.data.DataScreen;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.client.gui.data.DataScreens;
import dev.jstech.core.client.gui.data.IScreenLogic;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.gui.layout.NewScreenLayout;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * A new screen for the block the player looks at, in a development run: the block and the file to write, what to
 * start from (a machine with the player's inventory below, a screen with no inventory, a tab, a HUD element, or a copy
 * of a screen there is), and the new screen beside it as it would open. Create writes the file to the block's mod's
 * source and opens it, with the editor of screens on it to fill it with Add.
 */
public final class NewScreenScreen extends Screen {

    private final ResourceLocation block;
    private final Panel panel = new Panel();
    private final TextField blockField = panel.add(new TextField(128));
    private final TextField fileField = panel.add(new TextField(128));
    private final Button create;
    private final List<ResourceLocation> copies = new ArrayList<>();
    private final DataScreenView preview;
    private int start;
    private int copied;
    private Optional<String> problem = Optional.empty();

    /** What the preview is built from, under a name of the Core's own. */
    private static final ResourceLocation PREVIEW = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "developer/new_screen_preview");
    private static final int MACHINE = 0;
    private static final int NO_INVENTORY = 1;
    private static final int TAB = 2;
    private static final int HUD = 3;
    private static final int COPY = 4;
    private static final int GAME_WIDTH = 176;
    private static final int FACE_HEIGHT = 72;
    private static final float SMALL = 0.75F;

    /** A new screen for {@code block}, written as the file {@code jstech/screens/<its path>.json} unless changed. */
    public NewScreenScreen(final ResourceLocation block) {
        super(GameText.component(DeveloperTexts.NEW_FOR.with(block.toString())));
        this.block = block;
        this.blockField.set(block.toString());
        this.fileField.set(ScreenFile.FOLDER + "/" + block.getPath() + ".json");
        this.create = panel.add(new Button(() -> GameText.resolve(DeveloperTexts.CREATE.text()), this::create));
        this.copies.addAll(ScreenFiles.all().keySet());
        this.copies.remove(PREVIEW);
        this.preview = DataScreens.preview(PREVIEW, Component.literal(block.getPath()));
    }

    /** The template chosen, by its row; for a test. */
    public void startFrom(final int row) {
        start = Math.max(MACHINE, Math.min(COPY, row));
        if (start == COPY && !copies.isEmpty()) {
            copied = (copied + 1) % copies.size();
        }
        rebuildPreview();
    }

    /** Writes the file and opens the new screen with the editor on it; for a test as for the button. */
    public void create() {
        final Optional<ResourceLocation> file = fileId();
        if (file.isEmpty()) {
            return;
        }
        final JsonObject json = template();
        try {
            if (ScreenJson.write(file.get(), json).isEmpty()) {
                problem = Optional.of(GameText.resolve(DeveloperTexts.NO_SOURCE.with(file.get().getNamespace())));
            }
        } catch (final IOException unwritten) {
            problem = Optional.of(GameText.resolve(DeveloperTexts.NOT_WRITTEN.with(unwritten.getMessage())));
            return;
        }
        final Optional<String> wrong = ScreenFiles.put(file.get(), json);
        if (wrong.isPresent()) {
            problem = wrong;
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final IScreenLogic<Void> nothing = wiring -> {
        };
        final DataScreen opened = new DataScreen(file.get(), Component.literal(block.getPath()), nothing, null);
        minecraft.setScreen(opened);
        ScreenEditor.openFor(opened);
    }

    /** The file the new screen would be written as. */
    public Optional<ResourceLocation> fileId() {
        final ResourceLocation typed = ResourceLocation.tryParse(blockField.edit().trim());
        final String path = fileField.edit().trim();
        final String folder = ScreenFile.FOLDER + "/";
        if (typed == null || !path.startsWith(folder) || !path.endsWith(".json")) {
            return Optional.empty();
        }
        final String name = path.substring(folder.length(), path.length() - ".json".length());
        return Optional.ofNullable(ResourceLocation.tryBuild(typed.getNamespace(), name));
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final InspectorPalette.Colours colours = InspectorPalette.get();
        final StyleSkin skin = Styles.skin(Styles.game());
        Draw.text(g, font, getTitle(), NewScreenLayout.PAD, NewScreenLayout.TOP_TEXT_Y, colours.handle());
        final int left = NewScreenLayout.PANEL_X;
        final int top = NewScreenLayout.PANEL_Y;
        final int pad = NewScreenLayout.PAD;
        final int inner = NewScreenLayout.WIDTH - 2 * pad;
        skin.window(g, left, top, NewScreenLayout.WIDTH, NewScreenLayout.HEIGHT);
        Draw.text(g, font, GameText.component(DeveloperTexts.NEW), left + pad, top + NewScreenLayout.TITLE_Y,
                colours.text());
        Draw.textScaled(g, font, GameText.resolve(DeveloperTexts.LOOKED_AT.text()), left + pad,
                top + NewScreenLayout.SUBTITLE_Y, colours.dim(), SMALL);
        Draw.textScaled(g, font, GameText.resolve(DeveloperTexts.BLOCK.text()), left + pad,
                top + NewScreenLayout.BLOCK_LABEL_Y, colours.text(), SMALL);
        Draw.textScaled(g, font, GameText.resolve(DeveloperTexts.FILE_FIELD.text()), left + pad,
                top + NewScreenLayout.FILE_LABEL_Y, colours.text(), SMALL);
        Draw.text(g, font, GameText.component(DeveloperTexts.START_FROM), left + pad, top + NewScreenLayout.START_Y,
                colours.text());
        blockField.setBounds(left + pad, top + NewScreenLayout.BLOCK_Y, inner, NewScreenLayout.FIELD_HEIGHT);
        fileField.setBounds(left + pad, top + NewScreenLayout.FILE_Y, inner, NewScreenLayout.FIELD_HEIGHT);
        create.setBounds(left + pad, top + NewScreenLayout.CREATE_Y, inner, NewScreenLayout.CREATE_HEIGHT);
        final List<String> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            final int y = top + NewScreenLayout.ROWS_Y + i * NewScreenLayout.ROW;
            if (i == start) {
                g.fill(left + 2, y - 2, left + NewScreenLayout.WIDTH - 2, y + NewScreenLayout.ROW - 2,
                        colours.selected());
            }
            Draw.text(g, font, font.plainSubstrByWidth(rows.get(i), inner), left + pad, y, colours.text());
        }
        panel.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        preview.render(g, preview.left(), preview.top(), mouseX, mouseY, partialTick);
        final String hint = problem.orElseGet(() -> GameText.resolve(DeveloperTexts.CREATE_HINT.text()));
        final int hintWidth = Math.round(font.width(hint) * SMALL);
        Draw.textScaled(g, font, hint, Math.max(2, (width - hintWidth) / 2), height - NewScreenLayout.HINT_UP,
                problem.isPresent() ? colours.error() : colours.handle(), SMALL);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (panel.mouseClicked(mouseX, mouseY, button)) {
            panel.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        final int left = NewScreenLayout.PANEL_X;
        final int rowsTop = NewScreenLayout.PANEL_Y + NewScreenLayout.ROWS_Y - 2;
        if (mouseX >= left && mouseX < left + NewScreenLayout.WIDTH && mouseY >= rowsTop
                && mouseY < rowsTop + NewScreenLayout.ROWS * NewScreenLayout.ROW) {
            startFrom((int) ((mouseY - rowsTop) / NewScreenLayout.ROW));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (panel.focusedChild() != null && key != GLFW.GLFW_KEY_ESCAPE && panel.keyPressed(key, scanCode,
                modifiers)) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return panel.charTyped(c) || super.charTyped(c, modifiers);
    }

    @Override
    public void removed() {
        preview.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        rebuildPreview();
    }

    private void rebuildPreview() {
        ScreenFiles.put(PREVIEW, template());
        preview.build(font);
        final int left = NewScreenLayout.previewX() + Math.max(0, (width - NewScreenLayout.previewX()
                - preview.width()) / 2);
        preview.layout(left, Math.max(NewScreenLayout.PANEL_Y, (height - preview.height()) / 2), height);
    }

    private List<String> rows() {
        final String copy = copies.isEmpty() ? GameText.resolve(DeveloperTexts.COPY_NONE.text())
                : GameText.resolve(DeveloperTexts.COPY.with(copies.get(copied).toString()));
        return List.of(GameText.resolve(DeveloperTexts.MACHINE.text()),
                GameText.resolve(DeveloperTexts.NO_INVENTORY.text()), GameText.resolve(DeveloperTexts.TAB.text()),
                GameText.resolve(DeveloperTexts.HUD.text()), copy);
    }

    /* The file the new screen starts from, as the choice says. */
    private JsonObject template() {
        if (start == COPY && !copies.isEmpty()) {
            final Optional<JsonObject> copy = ScreenJson.read(copies.get(copied));
            if (copy.isPresent()) {
                return copy.get();
            }
        }
        final JsonObject file = new JsonObject();
        file.addProperty("width", start == HUD ? GAME_WIDTH / 2 : GAME_WIDTH);
        final JsonObject root = component("column");
        root.addProperty("gap", 4);
        final JsonArray children = new JsonArray();
        if (start != TAB && start != HUD) {
            children.add(component("title"));
        }
        final JsonObject face = component("well");
        face.addProperty("height", start == HUD ? FACE_HEIGHT / 3 : FACE_HEIGHT);
        face.addProperty("justify", "center");
        final JsonArray inFace = new JsonArray();
        final JsonObject empty = component("label");
        empty.addProperty("text", DeveloperTexts.EMPTY_FACE.key());
        empty.addProperty("wrap", true);
        empty.addProperty("text_align", "center");
        empty.addProperty("tone", "dim");
        inFace.add(empty);
        face.add("children", inFace);
        children.add(face);
        if (start == MACHINE) {
            children.add(component("inventory"));
        }
        root.add("children", children);
        file.add("root", root);
        return file;
    }

    private static JsonObject component(final String type) {
        final JsonObject component = new JsonObject();
        component.addProperty("type", type);
        return component;
    }
}
