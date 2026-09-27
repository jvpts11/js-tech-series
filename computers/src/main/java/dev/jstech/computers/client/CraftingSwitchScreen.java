/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.blockentity.CraftingSwitchBlockEntity;
import dev.jstech.computers.crafting.MachineCategory;
import dev.jstech.computers.gui.layout.CraftingSwitchLayout;
import dev.jstech.computers.menu.CraftingSwitchMenu;
import dev.jstech.computers.operation.payload.SetCraftingSwitchFacePayload;
import dev.jstech.computers.operation.payload.crafting.RenameSwitchBusPayload;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Crafting Switch screen: a master-detail panel listing the six faces (five can host a machine, one carries
 * the crafting cable) and, for the selected face, its detected machine, an editable name and an active toggle.
 * Reads the switch's block entity locally (the server keeps it in sync via the update tag) and pushes edits
 * back with {@link SetCraftingSwitchFacePayload}.
 */
@PaletteHolder
public class CraftingSwitchScreen extends CoreContainerScreen<CraftingSwitchMenu> {

    /** The screen's colours, {@code jsc:screen/crafting_switch}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "screen/crafting_switch",
            new Colours(0xFF0B0E13, 0xFF11161D, 0xFF1D2530, 0xFF15212A, 0xFF39D6C4, 0xFFCDD6E2, 0xFF7D8A9C,
                    0xFF5FE07A, 0xC0000000));

    private int selectedFace;
    private EditBox nameBox;
    private Button activeBtn;
    private Button categoryBtn;
    private boolean categoryPickerOpen;
    private int categoryScroll;
    private List<String> allCategories;

    public CraftingSwitchScreen(final CraftingSwitchMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = CraftingSwitchLayout.WIDTH;
        this.imageHeight = CraftingSwitchLayout.HEIGHT;
        this.inventoryLabelY = CraftingSwitchLayout.INV_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(this.font, leftPos + CraftingSwitchLayout.NAME_X, topPos + CraftingSwitchLayout.NAME_Y,
                CraftingSwitchLayout.NAME_W, CraftingSwitchLayout.NAME_H, Component.empty());
        nameBox.setMaxLength(48);
        nameBox.setBordered(true);
        nameBox.setResponder(this::onNameChanged);
        addRenderableWidget(nameBox);

        activeBtn = new ThemeButton(leftPos + CraftingSwitchLayout.ACTIVE_X, topPos + CraftingSwitchLayout.ACTIVE_Y,
                CraftingSwitchLayout.ACTIVE_W, CraftingSwitchLayout.ACTIVE_H,
                GameText.component(CraftingSwitchTexts.ACTIVE), b -> toggleActive());
        addRenderableWidget(activeBtn);

        categoryBtn = new ThemeButton(leftPos + CraftingSwitchLayout.CATEGORY_X,
                topPos + CraftingSwitchLayout.CATEGORY_Y,
                CraftingSwitchLayout.CATEGORY_W, CraftingSwitchLayout.CATEGORY_H,
                GameText.component(CraftingSwitchTexts.CATEGORY), b -> {
                    categoryPickerOpen = true;
                    categoryScroll = 0;
                });
        addRenderableWidget(categoryBtn);

        selectFace(defaultFace());
    }

    private CraftingSwitchBlockEntity blockEntity() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.switchPos()) instanceof CraftingSwitchBlockEntity be
                ? be : null;
    }

    /** The first face that holds a machine, else face 0. */
    private int defaultFace() {
        final CraftingSwitchBlockEntity be = blockEntity();
        if (be != null) {
            for (final Direction d : Direction.values()) {
                if (be.machineOnFace(d)) {
                    return d.get3DDataValue();
                }
            }
        }
        return 0;
    }

    /** The machines whose cable run hangs off the given switch face (client-synced). */
    private List<CraftingSwitchBlockEntity.BusMachineLine> busMachinesOnFace(final int face) {
        final CraftingSwitchBlockEntity be = blockEntity();
        if (be == null) {
            return List.of();
        }
        final List<CraftingSwitchBlockEntity.BusMachineLine> out = new ArrayList<>();
        for (final var line : be.busMachineLines()) {
            if (line.switchFace() == face) {
                out.add(line);
            }
        }
        return out;
    }

    private void selectFace(final int face) {
        this.selectedFace = face;
        final CraftingSwitchBlockEntity be = blockEntity();
        final Direction d = Direction.from3DDataValue(face);
        final boolean adjacent = be != null && be.machineOnFace(d);
        final var viaBus = busMachinesOnFace(face);
        /*
         * Widen before filling, then narrow: EditBox.setMaxLength truncates whatever text is already in the
         * box and fires the responder at once, and selectedFace already points at the new face by then. A
         * premature narrow would send a rename for the wrong bus with the previous face's text, truncated.
         */
        nameBox.setMaxLength(48);
        if (adjacent) {
            nameBox.setValue(be.faceName(d));
        } else if (!viaBus.isEmpty()) {
            nameBox.setValue(viaBus.get(0).busName()); // the machine's name IS its bus's name
        } else {
            nameBox.setValue(be == null ? "" : be.faceName(d));
        }
        // A name reached over a bus is edited (and clamped) as a bus name, shorter than a face's own.
        nameBox.setMaxLength(!adjacent && !viaBus.isEmpty() ? AbstractBusPart.MAX_NAME_LENGTH : 48);
        /*
         * Active + category govern the face's whole cable run too, so they stay editable when machines hang
         * off this face via buses.
         */
        final boolean editable = adjacent || !viaBus.isEmpty();
        nameBox.setEditable(editable);
        activeBtn.active = editable;
        categoryBtn.active = editable;
        categoryPickerOpen = false;
        refreshActiveLabel();
    }

    private void refreshActiveLabel() {
        final CraftingSwitchBlockEntity be = blockEntity();
        final Direction d = Direction.from3DDataValue(selectedFace);
        final boolean on = be != null && be.faceActive(d);
        activeBtn.setMessage(GameText.component(on ? CraftingSwitchTexts.ACCEPTING : CraftingSwitchTexts.INACTIVE));
        final String category = be == null ? "" : be.faceCategory(d);
        categoryBtn.setMessage(GameText.component(category.isEmpty() ? CraftingSwitchTexts.NO_CATEGORY.text()
                : CraftingSwitchTexts.SHORT_CATEGORY.with(shortCategory(category))));
    }

    /** Trims a recipe-type id for the button label ({@code minecraft:smelting} → {@code smelting}). */
    private static String shortCategory(final String category) {
        final int colon = category.indexOf(':');
        return colon >= 0 ? category.substring(colon + 1) : category;
    }

    private static String clamp(final String s, final int maxChars) {
        return s.length() <= maxChars ? s : s.substring(0, maxChars - 1) + "…";
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        /*
         * While the name field has focus, route typing to it and never let a key (e.g. the inventory key
         * 'E') reach the screen and close the GUI. ESC just unfocuses the field.
         */
        if (nameBox != null && nameBox.isFocused()) {
            if (key == 256) {
                nameBox.setFocused(false);
                setFocused(null);
                return true;
            }
            nameBox.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(final char c, final int mods) {
        if (nameBox != null && nameBox.isFocused()) {
            return nameBox.charTyped(c, mods);
        }
        return super.charTyped(c, mods);
    }

    private void onNameChanged(final String name) {
        final CraftingSwitchBlockEntity be = blockEntity();
        if (be == null) {
            return;
        }
        final Direction d = Direction.from3DDataValue(selectedFace);
        if (be.machineOnFace(d)) {
            sendFace(d, name, be.faceActive(d), be.faceCategory(d));
            return;
        }
        // A machine reached via buses: editing the name edits its Input/Receiving Bus (patterns match by it).
        final var viaBus = busMachinesOnFace(selectedFace);
        if (!viaBus.isEmpty()) {
            final var line = viaBus.get(0);
            PacketDistributor.sendToServer(
                    new RenameSwitchBusPayload(menu.switchPos(), line.cablePos(), line.busFace(), name));
        }
    }

    private boolean faceConfigurable(final CraftingSwitchBlockEntity be, final Direction d) {
        return be != null && (be.machineOnFace(d) || !busMachinesOnFace(d.get3DDataValue()).isEmpty());
    }

    private void toggleActive() {
        final CraftingSwitchBlockEntity be = blockEntity();
        final Direction d = Direction.from3DDataValue(selectedFace);
        if (!faceConfigurable(be, d)) {
            return;
        }
        sendFace(d, be.faceName(d), !be.faceActive(d), be.faceCategory(d));
    }

    private void selectCategory(final String category) {
        final CraftingSwitchBlockEntity be = blockEntity();
        final Direction d = Direction.from3DDataValue(selectedFace);
        if (faceConfigurable(be, d)) {
            sendFace(d, be.faceName(d), be.faceActive(d), category);
        }
        categoryPickerOpen = false;
    }

    private void sendFace(final Direction face, final String name, final boolean active, final String category) {
        PacketDistributor.sendToServer(new SetCraftingSwitchFacePayload(
                menu.switchPos(), face.get3DDataValue(), name, active, category));
    }

    /** "none" first, then every installed recipe type id, the dynamic generic machine categories. */
    private List<String> categories() {
        if (allCategories == null) {
            final List<String> list = new ArrayList<>();
            list.add("");
            list.addAll(MachineCategory.categoryIds());
            allCategories = list;
        }
        return allCategories;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        // The category picker is modal: a row click tags the face, any other click closes it.
        if (categoryPickerOpen) {
            final List<String> list = categories();
            for (int r = 0; r < CraftingSwitchLayout.CP_VISIBLE_ROWS && categoryScroll + r < list.size(); r++) {
                if (hover((int) mouseX, (int) mouseY, CraftingSwitchLayout.CP_X + CraftingSwitchLayout.CP_PAD,
                        CraftingSwitchLayout.cpRowY(r), CraftingSwitchLayout.CP_W - 2 * CraftingSwitchLayout.CP_PAD,
                        CraftingSwitchLayout.CP_ROW_H)) {
                    selectCategory(list.get(categoryScroll + r));
                    return true;
                }
            }
            categoryPickerOpen = false;
            return true;
        }
        // Click a face row in the left list to select it.
        for (int i = 0; i < CraftingSwitchLayout.FACES; i++) {
            if (hover((int) mouseX, (int) mouseY, CraftingSwitchLayout.LIST_X, CraftingSwitchLayout.rowY(i),
                    CraftingSwitchLayout.LIST_W, CraftingSwitchLayout.ROW_BOX_H)) {
                selectFace(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (categoryPickerOpen && scrollY != 0) {
            final int max = Math.max(0, categories().size() - CraftingSwitchLayout.CP_VISIBLE_ROWS);
            categoryScroll = Math.max(0, Math.min(max, categoryScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * The text drawn on face row {@code face}: a machine reached over that face's cable run is listed on the
     * row exactly like an adjacent one ("SOUTH > Furnace", "+N" when several), otherwise the plain face label.
     */
    private String rowLabel(final CraftingSwitchBlockEntity be, final int face) {
        final Direction d = Direction.from3DDataValue(face);
        final var viaBus = busMachinesOnFace(face);
        if (be != null && !be.machineOnFace(d) && !viaBus.isEmpty()) {
            final String machine = clamp(GameText.resolve(viaBus.get(0).blockName()), 10);
            return GameText.resolve(viaBus.size() > 1
                    ? CraftingSwitchTexts.VIA_BUS_MORE.with(dirShort(d), machine, viaBus.size() - 1)
                    : CraftingSwitchTexts.VIA_BUS.with(dirShort(d), machine));
        }
        return faceRowLabel(be, d);
    }

    // inspection (client tests assert on what the player sees)

    public int selectedFace() {
        return selectedFace;
    }

    /** The face row text as drawn (see {@link #rowLabel}); {@code face} is a {@link Direction} 3D data value. */
    public String faceRowText(final int face) {
        return rowLabel(blockEntity(), face);
    }

    /** Whether the header pill reads LINKED (the client-synced link flag). */
    public boolean isLinkedShown() {
        final CraftingSwitchBlockEntity be = blockEntity();
        return be != null && be.isLinked();
    }

    public boolean isCategoryPickerOpen() {
        return categoryPickerOpen;
    }

    /** Window-relative centre of face row {@code face}. */
    public static int faceRowX() {
        return CraftingSwitchLayout.LIST_X + CraftingSwitchLayout.LIST_W / 2;
    }

    public static int faceRowY(final int face) {
        return CraftingSwitchLayout.rowY(face) + CraftingSwitchLayout.ROW_BOX_H / 2;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, colours().ground());
        g.fill(x, y, x + imageWidth, y + 1, colours().line());

        final CraftingSwitchBlockEntity be = blockEntity();
        final boolean linked = be != null && be.isLinked();

        // Header status pill. Machines found over the cables show on their face rows, so this stays simple.
        final String pill = GameText.resolve(linked ? CraftingSwitchTexts.LINKED : CraftingSwitchTexts.UNLINKED);
        g.drawString(this.font, pill, x + imageWidth - 8 - this.font.width(pill),
                y + CraftingSwitchLayout.HEADER_Y + 1, linked ? colours().linked() : colours().dim(), false);

        /*
         * Face rows. A machine reached over a face's cable run is listed ON that face row, exactly like an
         * adjacent one, since the face is how the player thinks of the connection.
         */
        for (int i = 0; i < CraftingSwitchLayout.FACES; i++) {
            final Direction d = Direction.from3DDataValue(i);
            final int ry = y + CraftingSwitchLayout.rowY(i);
            final int rx = x + CraftingSwitchLayout.LIST_X;
            g.fill(rx, ry, rx + CraftingSwitchLayout.LIST_W, ry + CraftingSwitchLayout.ROW_BOX_H,
                    i == selectedFace ? colours().selection() : colours().panel());
            final boolean hasMachine = be != null && (be.machineOnFace(d) || !busMachinesOnFace(i).isEmpty());
            g.drawString(this.font, rowLabel(be, i), rx + 3, ry + 2,
                    hasMachine ? colours().text() : colours().dim(), false);
        }

        // Detail panel.
        final int dx = x + CraftingSwitchLayout.DETAIL_X;
        final Direction sel = Direction.from3DDataValue(selectedFace);
        final var selViaBus = busMachinesOnFace(selectedFace);
        if (be != null && !be.machineOnFace(sel) && !selViaBus.isEmpty()) {
            final var line = selViaBus.get(0);
            g.drawString(this.font, clamp(GameText.resolve(line.blockName()), 15), dx + 4,
                    y + CraftingSwitchLayout.MACHINE_LABEL_Y, colours().accent(), false);
            g.drawString(this.font, GameText.resolve(CraftingSwitchTexts.NAME), dx + 4,
                    y + CraftingSwitchLayout.NAME_LABEL_Y, colours().dim(), false);
            // Absolute coordinates, tucked between the name field and the active toggle.
            JsTechTheme.textS(g, this.font, GameText.resolve(CraftingSwitchTexts.AT.with(line.machinePos().getX(),
                            line.machinePos().getY(), line.machinePos().getZ())),
                    dx + 4, y + CraftingSwitchLayout.NAME_Y + CraftingSwitchLayout.NAME_H + 2, colours().text());
            if (selViaBus.size() > 1) {
                JsTechTheme.textSRight(g, this.font,
                        GameText.resolve(CraftingSwitchTexts.MORE.with(selViaBus.size() - 1)),
                        dx + CraftingSwitchLayout.DETAIL_W - 2,
                        y + CraftingSwitchLayout.MACHINE_LABEL_Y + 1, colours().dim());
            }
        } else {
            final String machine = detailMachineLabel(be, sel);
            g.drawString(this.font, machine, dx + 4, y + CraftingSwitchLayout.MACHINE_LABEL_Y, colours().accent(),
                    false);
            g.drawString(this.font, GameText.resolve(CraftingSwitchTexts.NAME), dx + 4,
                    y + CraftingSwitchLayout.NAME_LABEL_Y, colours().dim(), false);
        }

        /*
         * Player inventory slot frames, drawn from the menu's own slots (its only slots). Without these, the
         * empty creative-mode slots have nothing drawn behind them and the inventory looks like it vanished.
         */
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        g.drawString(this.font, GameText.resolve(CraftingSwitchTexts.TITLE), CraftingSwitchLayout.HEADER_X,
                CraftingSwitchLayout.HEADER_Y + 1, colours().text(), false);
        g.drawString(this.font, this.playerInventoryTitle, CraftingSwitchLayout.INV_X,
                CraftingSwitchLayout.INV_LABEL_Y, colours().dim(), false);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // Keep the controls reflecting the latest synced state (e.g. a machine placed/removed while open).
        refreshActiveLabel();
        super.render(g, mouseX, mouseY, partialTick);
        if (categoryPickerOpen) {
            // Raised Z so the modal sits above the slot items, same pattern as the Pattern Encoder popups.
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            renderCategoryPicker(g, mouseX, mouseY);
            g.pose().popPose();
        }
    }

    /** Skipped while the category picker is open, so a slot's tooltip never peeks out from behind the modal. */
    @Override
    protected void renderTooltip(final GuiGraphics g, final int mouseX, final int mouseY) {
        if (!categoryPickerOpen) {
            super.renderTooltip(g, mouseX, mouseY);
        }
    }

    /** Modal list of the dynamic machine categories (installed recipe types); a row click tags the face. */
    private void renderCategoryPicker(final GuiGraphics g, final int mouseX, final int mouseY) {
        final List<String> list = categories();
        final int shown = CraftingSwitchLayout.CP_VISIBLE_ROWS;
        categoryScroll = Math.max(0, Math.min(Math.max(0, list.size() - shown), categoryScroll));
        final int px = leftPos + CraftingSwitchLayout.CP_X;
        final int py = topPos + CraftingSwitchLayout.CP_Y;
        final int ph = CraftingSwitchLayout.cpHeight();
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, colours().veil());
        g.fill(px, py, px + CraftingSwitchLayout.CP_W, py + ph, colours().panel());
        g.fill(px, py, px + 1, py + ph, colours().line());
        g.fill(px + CraftingSwitchLayout.CP_W - 1, py, px + CraftingSwitchLayout.CP_W, py + ph, colours().line());
        g.drawString(font, GameText.resolve(CraftingSwitchTexts.MACHINE_CATEGORY), px + 6, py + 3,
                colours().accent(), false);
        g.fill(px + 4, py + 13, px + CraftingSwitchLayout.CP_W - 4, py + 14, colours().line());
        final int pad = CraftingSwitchLayout.CP_PAD;
        for (int r = 0; r < shown && categoryScroll + r < list.size(); r++) {
            final String category = list.get(categoryScroll + r);
            final int ry = topPos + CraftingSwitchLayout.cpRowY(r);
            final boolean hovered = hover(mouseX, mouseY, CraftingSwitchLayout.CP_X + pad,
                    CraftingSwitchLayout.cpRowY(r), CraftingSwitchLayout.CP_W - 2 * pad,
                    CraftingSwitchLayout.CP_ROW_H);
            if (hovered) {
                g.fill(px + pad, ry, px + CraftingSwitchLayout.CP_W - pad, ry + CraftingSwitchLayout.CP_ROW_H,
                        colours().selection());
            }
            JsTechTheme.textS(g, font, category.isEmpty() ? GameText.resolve(CraftingSwitchTexts.NONE) : category,
                    px + 8, ry + 2,
                    hovered ? colours().text() : colours().dim());
        }
        if (list.size() > shown) {
            JsTechTheme.textSRight(g, font, (categoryScroll + 1) + "-"
                            + Math.min(list.size(), categoryScroll + shown) + "/" + list.size(),
                    px + CraftingSwitchLayout.CP_W - 6, py + 4, colours().dim());
        }
    }

    private static String faceRowLabel(final CraftingSwitchBlockEntity be, final Direction d) {
        final String dir = dirShort(d);
        if (be == null) {
            return dir;
        }
        if (be.cableFace() == d) {
            return GameText.resolve(CraftingSwitchTexts.TO_COMPUTER.with(dir));
        }
        if (be.machineOnFace(d)) {
            final String name = be.faceName(d);
            return GameText.resolve(CraftingSwitchTexts.FACE_MACHINE.with(dir,
                    name.isEmpty() ? CraftingSwitchTexts.MACHINE.text() : Text.literal(name)));
        }
        return GameText.resolve(CraftingSwitchTexts.FACE_NONE.with(dir));
    }

    private static String detailMachineLabel(final CraftingSwitchBlockEntity be, final Direction d) {
        if (be == null) {
            return dirShort(d);
        }
        if (be.cableFace() == d) {
            return GameText.resolve(CraftingSwitchTexts.CRAFTING_CABLE.with(dirShort(d)));
        }
        return GameText.resolve((be.machineOnFace(d) ? CraftingSwitchTexts.HAS_MACHINE : CraftingSwitchTexts.NO_MACHINE)
                .with(dirShort(d)));
    }

    private static String dirShort(final Direction d) {
        return GameText.resolve(switch (d) {
            case DOWN -> CraftingSwitchTexts.DOWN;
            case UP -> CraftingSwitchTexts.UP;
            case NORTH -> CraftingSwitchTexts.NORTH;
            case SOUTH -> CraftingSwitchTexts.SOUTH;
            case WEST -> CraftingSwitchTexts.WEST;
            case EAST -> CraftingSwitchTexts.EAST;
        });
    }

    private static Colours colours() {
        return PALETTE.get();
    }

    /**
     * The screen's colours: its ground, a panel, the rules, the chosen row, the accent, the text and the quiet
     * lines, a switch that is on a network, and the veil behind the machine picker.
     */
    private record Colours(int ground, int panel, int line, int selection, int accent, int text, int dim,
                           int linked, int veil) {
    }
}
