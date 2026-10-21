/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hud;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.gui.HudPlacement;
import dev.jstech.core.gui.HudSnap;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.gui.layout.HudEditorLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The HUD editor: the game seen through a dim grid, every mod's HUD element in a dashed box with its mod's and its own
 * name on a label above it, whether it shows now or not. A box dragged moves its element and settles against the
 * others or on the grid; the chosen box, yellow, is resized by its corners; its eye hides it, and a hidden box is
 * grey. Reset all puts every element back where its mod declared it, Cancel leaves as it was, and Done keeps what was
 * done.
 */
public final class HudEditorScreen extends Screen {

    private final @Nullable Screen parent;
    private final ConfigDraft draft;
    private final ConfigKey<Map<String, HudPlacement>> key;
    private final boolean applies;
    private final List<HudElements.Editable> elements = HudElements.editable();
    private final Map<String, HudPlacement> placed = new LinkedHashMap<>();
    private @Nullable ResourceLocation picked;
    private Drag drag = Drag.NONE;
    private double grabX;
    private double grabY;
    private int anchorX;
    private int anchorY;

    /** How small the small text of the labels and the window is drawn. */
    private static final float SMALL = 0.75F;

    /**
     * The editor of {@code key} in {@code draft}, going back to {@code parent}; when {@code applies}, Done also
     * writes the draft to the player's file, as it does when the editor is opened by its key.
     */
    public HudEditorScreen(final @Nullable Screen parent, final ConfigDraft draft,
                           final ConfigKey<Map<String, HudPlacement>> key, final boolean applies) {
        super(GameText.component(HudTexts.EDIT));
        this.parent = parent;
        this.draft = draft;
        this.key = key;
        this.applies = applies;
        this.placed.putAll(draft.get(key));
    }

    /** Where the element {@code id} is now in the editor, its box on the screen. */
    public HudSnap.Box boxOf(final ResourceLocation id) {
        return boxes().get(id);
    }

    /** What the editor holds now, by element id. */
    public Map<String, HudPlacement> placements() {
        return Map.copyOf(placed);
    }

    /** The element chosen, if any. */
    public @Nullable ResourceLocation picked() {
        return picked;
    }

    /* Under the buttons: the dim game, the grid, every element's box and the window. */
    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        final HudEditorPalette.Colours colours = HudEditorPalette.get();
        g.fill(0, 0, width, height, colours.dim());
        grid(g, colours.grid());
        final Map<ResourceLocation, HudSnap.Box> boxes = boxes();
        for (final HudElements.Editable element : elements) {
            final HudSnap.Box box = boxes.get(element.id());
            final boolean hidden = hidden(element.id());
            final int colour = element.id().equals(picked) ? colours.picked() : hidden ? colours.hidden()
                    : colours.box();
            box(g, box, element, colour, hidden, colours);
        }
        window(g, colours);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        final Map<ResourceLocation, HudSnap.Box> boxes = boxes();
        if (picked != null && grabCorner(boxes.get(picked), mouseX, mouseY)) {
            return true;
        }
        for (int i = elements.size() - 1; i >= 0; i--) {
            final ResourceLocation id = elements.get(i).id();
            final HudSnap.Box box = boxes.get(id);
            if (onEye(box, mouseX, mouseY)) {
                placed.put(id.toString(), placementOf(id, box).withHidden(!hidden(id)));
                HudElements.preview(placed);
                return true;
            }
            if (box.contains(mouseX, mouseY)) {
                picked = id;
                drag = Drag.MOVE;
                grabX = mouseX - box.x();
                grabY = mouseY - box.y();
                return true;
            }
        }
        picked = null;
        return false;
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        if (picked == null || drag == Drag.NONE) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        final ResourceLocation id = picked;
        final HudStack.Size own = own(id);
        final HudPlacement before = placementOf(id, boxes().get(id));
        if (drag == Drag.MOVE) {
            final HudStack.Size size = before.scaled(own);
            final HudStack.Point at = HudSnap.snap((int) Math.round(mouseX - grabX), (int) Math.round(mouseY - grabY),
                    size, others(id), width, height);
            placed.put(id.toString(), HudPlacement.at(at.x(), at.y(), size, width, height, before.scale(),
                    before.hidden()));
        } else {
            // The corner held follows the pointer; the opposite one stays, and the size goes in quarter steps.
            final float scale = HudSnap.scale((float) Math.abs(mouseX - anchorX) / own.width());
            final HudStack.Size size = new HudPlacement(before.corner(), 0, 0, scale, false).scaled(own);
            final int left = mouseX < anchorX ? anchorX - size.width() : anchorX;
            final int top = mouseY < anchorY ? anchorY - size.height() : anchorY;
            placed.put(id.toString(), HudPlacement.at(left, top, size, width, height, scale, before.hidden()));
        }
        HudElements.preview(placed);
        return true;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        drag = Drag.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /* The HUD goes back to what the player's settings say, which Done has kept or Cancel left. */
    @Override
    public void removed() {
        HudElements.preview(null);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        final int left = HudEditorLayout.left(width);
        final int top = HudEditorLayout.top(height) + HudEditorLayout.BUTTONS_Y;
        HudElements.preview(placed);
        addRenderableWidget(Button.builder(GameText.component(HudTexts.RESET), button -> {
            placed.clear();
            HudElements.preview(placed);
        })
                .bounds(left + HudEditorLayout.PAD, top, HudEditorLayout.RESET_WIDTH, HudEditorLayout.BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(left + HudEditorLayout.cancelX(), top, HudEditorLayout.CANCEL_WIDTH,
                        HudEditorLayout.BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> done())
                .bounds(left + HudEditorLayout.doneX(), top, HudEditorLayout.DONE_WIDTH,
                        HudEditorLayout.BUTTON_HEIGHT).build());
    }

    /* Keeps what was done in the draft, and in the player's file when the editor was opened by its key. */
    private void done() {
        draft.choose(key, Map.copyOf(placed));
        if (applies) {
            draft.apply();
        }
        onClose();
    }

    /* Every element's box now: where the player placed it, or stacked in its corner as its mod declared it. */
    private Map<ResourceLocation, HudSnap.Box> boxes() {
        final Set<ResourceLocation> unplaced = new HashSet<>();
        for (final HudElements.Editable element : elements) {
            if (!placed.containsKey(element.id().toString())) {
                unplaced.add(element.id());
            }
        }
        final Map<ResourceLocation, HudSnap.Box> out = new LinkedHashMap<>(HudElements.declaredBoxes(minecraft,
                width, height, unplaced));
        for (final HudElements.Editable element : elements) {
            final HudPlacement placement = placed.get(element.id().toString());
            if (placement != null) {
                final HudStack.Size size = placement.scaled(own(element.id()));
                final HudStack.Point at = placement.topLeft(width, height, size);
                out.put(element.id(), new HudSnap.Box(at.x(), at.y(), size.width(), size.height()));
            }
        }
        return out;
    }

    private List<HudSnap.Box> others(final ResourceLocation id) {
        final List<HudSnap.Box> out = new ArrayList<>();
        boxes().forEach((other, box) -> {
            if (!other.equals(id)) {
                out.add(box);
            }
        });
        return out;
    }

    /* The element's box at its own size. */
    private HudStack.Size own(final ResourceLocation id) {
        for (final HudElements.Editable element : elements) {
            if (element.id().equals(id)) {
                return HudElements.boxSize(minecraft, element.element());
            }
        }
        return new HudStack.Size(1, 1);
    }

    private boolean hidden(final ResourceLocation id) {
        final HudPlacement placement = placed.get(id.toString());
        return placement != null && placement.hidden();
    }

    /* The placement of an element as its box stands now, the player's or one made from where it is stacked. */
    private HudPlacement placementOf(final ResourceLocation id, final HudSnap.Box box) {
        final HudPlacement placement = placed.get(id.toString());
        if (placement != null) {
            return placement;
        }
        return HudPlacement.at(box.x(), box.y(), new HudStack.Size(box.width(), box.height()), width, height, 1,
                false);
    }

    /* Whether the press takes a corner of the chosen box, which it then resizes from the opposite one. */
    private boolean grabCorner(final HudSnap.Box box, final double mouseX, final double mouseY) {
        final int right = box.x() + box.width();
        final int bottom = box.y() + box.height();
        for (final int[] corner : new int[][] {{box.x(), box.y()}, {right, box.y()}, {box.x(), bottom},
            {right, bottom}}) {
            if (Math.abs(mouseX - corner[0]) <= HudEditorLayout.HANDLE + HudEditorLayout.HANDLE_REACH
                    && Math.abs(mouseY - corner[1]) <= HudEditorLayout.HANDLE + HudEditorLayout.HANDLE_REACH) {
                drag = Drag.RESIZE;
                anchorX = corner[0] == box.x() ? right : box.x();
                anchorY = corner[1] == box.y() ? bottom : box.y();
                return true;
            }
        }
        return false;
    }

    private static boolean onEye(final HudSnap.Box box, final double mouseX, final double mouseY) {
        final int eyeX = box.x() + box.width() - HudEditorLayout.EYE_IN;
        final int eyeY = box.y() + HudEditorLayout.EYE_DOWN;
        return mouseX >= eyeX - 1 && mouseX < eyeX + HudEditorLayout.EYE_WIDTH + 1 && mouseY >= eyeY - 1
                && mouseY < eyeY + HudEditorLayout.EYE_HEIGHT + 1;
    }

    /* The grid's dotted lines, across and down. */
    private void grid(final GuiGraphics g, final int colour) {
        for (int x = 0; x < width; x += HudSnap.GRID) {
            for (int y = 0; y < height; y += 2) {
                g.fill(x, y, x + 1, y + 1, colour);
            }
        }
        for (int y = 0; y < height; y += HudSnap.GRID) {
            for (int x = 0; x < width; x += 2) {
                g.fill(x, y, x + 1, y + 1, colour);
            }
        }
    }

    /* One element's dashed box, its label above it and its eye, and the chosen one's corners. */
    private void box(final GuiGraphics g, final HudSnap.Box box, final HudElements.Editable element, final int colour,
                     final boolean hidden, final HudEditorPalette.Colours colours) {
        final int x0 = box.x();
        final int y0 = box.y();
        final int x1 = x0 + box.width();
        final int y1 = y0 + box.height();
        for (int x = x0; x < x1; x += HudEditorLayout.DASH_EVERY) {
            g.fill(x, y0, Math.min(x + HudEditorLayout.DASH, x1), y0 + 1, colour);
            g.fill(x, y1 - 1, Math.min(x + HudEditorLayout.DASH, x1), y1, colour);
        }
        for (int y = y0; y < y1; y += HudEditorLayout.DASH_EVERY) {
            g.fill(x0, y, x0 + 1, Math.min(y + HudEditorLayout.DASH, y1), colour);
            g.fill(x1 - 1, y, x1, Math.min(y + HudEditorLayout.DASH, y1), colour);
        }
        final Component label = GameText.component(HudTexts.OWNED.with(
                ItemTooltips.modName(element.id().getNamespace()), GameText.of(element.name())));
        final int labelWidth = Math.round(font.width(label) * SMALL) + 2 * HudEditorLayout.LABEL_PAD;
        // Above the box, or under it when the box stands at the top; kept on the screen.
        final int labelTop = y0 >= HudEditorLayout.LABEL_HEIGHT ? y0 - HudEditorLayout.LABEL_HEIGHT : y1;
        final int labelLeft = Math.max(0, Math.min(width - labelWidth, x0));
        TooltipRenderUtil.renderTooltipBackground(g, labelLeft + 3, labelTop + 4, labelWidth - 6,
                HudEditorLayout.LABEL_HEIGHT - 8, 0);
        g.pose().pushPose();
        g.pose().translate(0, 0, 1);
        Draw.textScaled(g, font, label.getString(), labelLeft + HudEditorLayout.LABEL_PAD, labelTop + 3, colour,
                SMALL);
        g.pose().popPose();
        final int eyeX = x1 - HudEditorLayout.EYE_IN;
        final int eyeY = y0 + HudEditorLayout.EYE_DOWN;
        g.fill(eyeX, eyeY, eyeX + HudEditorLayout.EYE_WIDTH, eyeY + HudEditorLayout.EYE_HEIGHT, colours.eyeGround());
        g.fill(eyeX + 2, eyeY, eyeX + HudEditorLayout.EYE_WIDTH - 2, eyeY + 1, colour);
        g.fill(eyeX + 2, eyeY + HudEditorLayout.EYE_HEIGHT - 1, eyeX + HudEditorLayout.EYE_WIDTH - 2,
                eyeY + HudEditorLayout.EYE_HEIGHT, colour);
        g.fill(eyeX + 3, eyeY + 1, eyeX + HudEditorLayout.EYE_WIDTH - 3, eyeY + HudEditorLayout.EYE_HEIGHT - 1,
                hidden ? colours.closedEye() : colour);
        if (element.id().equals(picked)) {
            final int h = HudEditorLayout.HANDLE;
            for (final int[] corner : new int[][] {{x0, y0}, {x1 - h, y0}, {x0, y1 - h}, {x1 - h, y1 - h}}) {
                g.fill(corner[0], corner[1], corner[0] + h, corner[1] + h, colours.picked());
            }
        }
    }

    /* The small window: the game's own look, its title and what to do. */
    private void window(final GuiGraphics g, final HudEditorPalette.Colours colours) {
        final int left = HudEditorLayout.left(width);
        final int top = HudEditorLayout.top(height);
        Styles.skin(Styles.game()).window(g, left, top, HudEditorLayout.WIDTH, HudEditorLayout.HEIGHT);
        Draw.text(g, font, GameText.component(HudTexts.EDIT), left + HudEditorLayout.PAD,
                top + HudEditorLayout.TITLE_Y, colours.title());
        Draw.textScaled(g, font, GameText.resolve(HudTexts.HOW.text()), left + HudEditorLayout.PAD,
                top + HudEditorLayout.HOW_Y, colours.title(), SMALL);
        Draw.textScaled(g, font, GameText.resolve(HudTexts.SNAPS.text()), left + HudEditorLayout.PAD,
                top + HudEditorLayout.SNAPS_Y, colours.snaps(), SMALL);
    }

    /* What a drag does: nothing, move the chosen box, or resize it by a corner. */
    private enum Drag {
        NONE,
        MOVE,
        RESIZE
    }
}
