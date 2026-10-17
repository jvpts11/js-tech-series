/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor.client;

import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ACROSS_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ACROSS_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.COLUMN_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.DISPLAY;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.DISPLAY_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.DISPLAY_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.FACE_HEIGHT;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.FACE_WIDTH;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.FACE_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.FACE_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.FULL_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.KEEPING_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.KEYS;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.KEYS_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.KEY_PITCH;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LAMPS_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LAMP_WORD_GAP;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LCD_HEIGHT;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LCD_PITCH;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LCD_WIDTH;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LCD_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LCD_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LIT_KEY_HEIGHT;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.LIT_KEY_WIDTH;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.NAME_MOST;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.NAME_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.OFF_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.OFF_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ON_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ON_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.PLATE_HEIGHT;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.PLATE_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.RADIUS_WORD_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.SHAPE_WORD_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.SHEET;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.SIZE_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.SIZE_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.SPRITES_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.SWITCH_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.SWITCH_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.TAB_ICON;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.TAB_ICON_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.WHEEL_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.WHEEL_Y;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ZOOM_PITCH;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ZOOM_SIZE;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ZOOM_X;
import static dev.jstech.core.jstechseries.persistor.SpacePersistorLayout.ZOOM_Y;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.LandPicture;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineChrome;
import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.client.machine.MachineView;
import dev.jstech.core.client.machine.MachineWords;
import dev.jstech.core.client.machine.ProtectionTab;
import dev.jstech.core.energy.EnergyTexts;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.persistor.PersistorArea;
import dev.jstech.core.jstechseries.persistor.PersistorReadings;
import dev.jstech.core.jstechseries.persistor.PersistorShape;
import dev.jstech.core.jstechseries.persistor.PersistorShapes;
import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.jstechseries.persistor.SpacePersistorBlockEntity;
import dev.jstech.core.jstechseries.persistor.SpacePersistorLayout;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * The Space Persistor's screen, as the approved design draws it: no inventory, since it holds no items; the window is
 * its steel face, with the display of the land round it from above in the game's map colours, chunk by chunk (the
 * chunks it keeps outlined in moving dashes, another owner's hatched, the chunk under the pointer named in a tooltip,
 * a click adding it or taking it off), its zoom keys taking it further out; and beside it its plates, its name, the
 * LCD of its counts, the shape keys, the radius' drums, the switch and the lamps KEEPING and FULL. When the server
 * makes it spend energy it has the Protection tab every machine has, drawn as every machine's is, with the inventory
 * below for its fuse and battery.
 */
@PaletteHolder
public final class SpacePersistorScreen extends CoreContainerScreen<MachineMenu> {

    private final SpacePersistorBlockEntity persistor;
    private final LandPicture land = new LandPicture();
    private final ProtectionTab protection = new ProtectionTab();
    private int zoom;
    private int ticks;
    private @Nullable EditBox naming;
    private @Nullable PersistorArea shownArea;
    private Set<Long> shownOffsets = Set.of();

    /** The face's picture: the steel, the display's bezel and the four keys unlit; the lit key and the tab's icon. */
    public static final ResourceLocation PICTURE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/space_persistor.png");
    /** The display's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "space_persistor", new Colours(
            0xFF << 24 | 0x55FFFF, 0xFF << 24 | 0x145050, 0x2E << 24 | 0x55FFFF, 0x96 << 24 | 0xFFAA00,
            0xFF << 24 | 0x14181E, 0x38 << 24 | 0x0A0E12, 0x4D << 24 | 0x0A0E12, 0x3C << 24, 0x22 << 24,
            0xFF << 24 | 0xFFFFFF, 0xFF << 24, 0xA0 << 24, 0xC8 << 24 | 0xC8C8C8, 0x96 << 24,
            0xFF << 24 | 0xDCDCDC, 0xFF << 24 | 0x3C2800, 0x28 << 24 | 0xFFFFFF));
    private static final int FRAME_TICKS = 2;
    private static final int RING_STEPS = 3;
    private static final int RING_POINTS = 16;
    private static final int DASH = 4;
    private static final int HOVER_GROW = 1;
    private static final int MARK = 4;
    private static final int LCD_LINES = 4;
    private static final int INVENTORY_WORD_X = 8;
    private static final int INVENTORY_WORD_Y = 6;
    private static final double RING_STRETCH = 1.4;

    public SpacePersistorScreen(final MachineMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.persistor = (SpacePersistorBlockEntity) menu.machine();
        this.imageWidth = SpacePersistorLayout.WIDTH;
        this.imageHeight = onProtection() ? MachineScreenLayout.HEIGHT : SpacePersistorLayout.HEIGHT;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }
        final List<Component> lines = tooltip(mouseX - this.leftPos, mouseY - this.topPos, mouseX, mouseY);
        if (!lines.isEmpty()) {
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final int tab = MachineScreenLayout.tabAt(x, y);
        if (tab >= 0 && tab < tabs().size() && button == InputConstants.MOUSE_BUTTON_LEFT) {
            show(tabs().get(tab));
            return true;
        }
        if (onProtection()) {
            final MachineView view = view(null, (int) mouseX, (int) mouseY, 0F);
            if (this.protection.clicked(view, view.faceMouseX(), view.faceMouseY(), button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.naming != null && !this.naming.isMouseOver(mouseX, mouseY)) {
            finishNaming(true);
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && clickedFace(x - FACE_X, y - FACE_Y)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final double fx = mouseX - this.leftPos - FACE_X;
        final double fy = mouseY - this.topPos - FACE_Y;
        if (!onProtection() && scrollY != 0) {
            if (SpacePersistorLayout.drumAt(fx, fy) >= 0) {
                turnRadius(scrollY > 0 ? 1 : -1);
                return true;
            }
            if (SpacePersistorLayout.chunkAt(fx, fy, this.zoom).isPresent()) {
                this.zoom = Math.clamp(this.zoom + (scrollY > 0 ? -1 : 1), 0, SpacePersistorLayout.zooms() - 1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (this.naming != null) {
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                finishNaming(true);
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                finishNaming(false);
                return true;
            }
            return this.naming.keyPressed(keyCode, scanCode, modifiers) || this.naming.canConsumeInput()
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        this.land.dispose();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
        // A server that stops charging energy takes the Protection tab away with it.
        if (onProtection() && !this.persistor.screenReadings().energy()) {
            show(MachineMenu.MAIN);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final List<Integer> tabs = tabs();
        final int active = this.menu.tab();
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i) != active) {
                tab(g, i, tabs.get(i), false);
            }
        }
        final int shown = Math.max(0, tabs.indexOf(active));
        final int tabLeft = this.leftPos + MachineScreenLayout.tabX(shown);
        final MachineView view = view(g, mouseX, mouseY, partialTick);
        if (onProtection()) {
            MachineArt.window(g, this.leftPos, this.topPos + MachineScreenLayout.WINDOW_TOP, this.leftPos
                    + MachineScreenLayout.WIDTH, this.topPos + MachineScreenLayout.WINDOW_TOP
                    + MachineScreenLayout.WINDOW_HEIGHT, tabLeft + 1, tabLeft + MachineScreenLayout.TAB_WIDTH - 1);
            tab(g, shown, active, true);
            g.blit(MachineScreen.STEEL, this.leftPos + MachineScreenLayout.FACE_X, this.topPos
                    + MachineScreenLayout.FACE_Y, 0, 0, MachineScreenLayout.FACE_WIDTH,
                    MachineScreenLayout.FACE_HEIGHT, MachineScreenLayout.FACE_WIDTH, MachineScreenLayout.FACE_HEIGHT);
            MachineChrome.plates(view, this.title);
            MachineChrome.lamps(view);
            this.protection.draw(view);
            inventory(g);
            return;
        }
        MachineArt.window(g, this.leftPos, this.topPos + SpacePersistorLayout.WINDOW_TOP, this.leftPos
                + SpacePersistorLayout.WIDTH, this.topPos + SpacePersistorLayout.WINDOW_BOTTOM, tabLeft + 1,
                tabLeft + MachineScreenLayout.TAB_WIDTH - 1);
        tab(g, shown, active, true);
        g.blit(PICTURE, this.leftPos + FACE_X, this.topPos + FACE_Y, 0, 0, FACE_WIDTH, FACE_HEIGHT, SHEET, SHEET);
        final PersistorReadings read = this.persistor.screenReadings();
        display(view, read);
        column(view, read);
    }

    /* The inventory's title on the Protection tab, where it shows; nothing on the face, where the plates name it. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        if (onProtection()) {
            Draw.text(g, this.font, this.playerInventoryTitle, MachineScreenLayout.INVENTORY_X + INVENTORY_WORD_X,
                    MachineScreenLayout.INVENTORY_Y + INVENTORY_WORD_Y, MachineArt.PALETTE.get().title());
        }
    }

    /* The tabs it has: its Main, and the Protection tab while the server makes it spend energy. */
    private List<Integer> tabs() {
        return this.persistor.screenReadings().energy() ? List.of(MachineMenu.MAIN, MachineMenu.PROTECTION)
                : List.of(MachineMenu.MAIN);
    }

    private boolean onProtection() {
        return this.menu.tab() == MachineMenu.PROTECTION;
    }

    /* Shows the machine's tab {@code index}, the screen growing or shrinking to it. */
    private void show(final int index) {
        finishNaming(false);
        this.menu.showTab(index);
        sendButton(MachineMenu.FIRST_TAB + index);
        this.imageHeight = onProtection() ? MachineScreenLayout.HEIGHT : SpacePersistorLayout.HEIGHT;
        rebuildWidgets();
    }

    /* The tab at place {@code place}, with the icon of the machine's tab {@code index}: its own for Main. */
    private void tab(final GuiGraphics g, final int place, final int index, final boolean active) {
        final int x = this.leftPos + MachineScreenLayout.tabX(place);
        if (index == MachineMenu.MAIN) {
            MachineArt.tab(g, x, this.topPos, active);
            g.blit(PICTURE, x + MachineScreenLayout.TAB_ICON, this.topPos + MachineScreenLayout.TAB_ICON, TAB_ICON_X,
                    SPRITES_Y, TAB_ICON, TAB_ICON, SHEET, SHEET);
        } else {
            MachineChrome.tab(g, x, this.topPos, index, active);
        }
    }

    /* What a click at {@code fx, fy} of the face does; whether it took it. */
    private boolean clickedFace(final double fx, final double fy) {
        final int zoomKey = SpacePersistorLayout.zoomAt(fx, fy);
        if (zoomKey >= 0) {
            this.zoom = Math.clamp(this.zoom + (zoomKey == 0 ? -1 : 1), 0, SpacePersistorLayout.zooms() - 1);
            return true;
        }
        final OptionalLong chunk = SpacePersistorLayout.chunkAt(fx, fy, this.zoom);
        if (chunk.isPresent()) {
            SpacePersistor.CLICK.send(chunk.getAsLong());
            return true;
        }
        final int key = SpacePersistorLayout.keyAt(fx, fy);
        if (key >= 0) {
            SpacePersistor.SET_SHAPE.send(key);
            return true;
        }
        final int drum = SpacePersistorLayout.drumAt(fx, fy);
        if (drum >= 0) {
            turnRadius((SpacePersistorLayout.drumUp(fy) ? 1 : -1) * (drum == 0 ? 10 : 1));
            return true;
        }
        if (SpacePersistorLayout.onSwitch(fx, fy)) {
            sendButton(MachineMenu.SWITCH);
            return true;
        }
        if (SpacePersistorLayout.onName(fx, fy)) {
            startNaming();
            return true;
        }
        return false;
    }

    private void turnRadius(final int by) {
        SpacePersistor.SET_RADIUS.send(Math.clamp(this.persistor.area().radius() + by, 0, PersistorArea.MOST_RADIUS));
    }

    private void startNaming() {
        if (this.naming != null) {
            return;
        }
        final EditBox box = new EditBox(this.font, this.leftPos + FACE_X + COLUMN_X, this.topPos + FACE_Y + NAME_Y,
                NAME_MOST, PLATE_HEIGHT, GameText.component(SpacePersistor.RENAME.text()));
        box.setMaxLength(SpacePersistorBlockEntity.LONGEST_NAME);
        box.setValue(this.persistor.name());
        box.setFocused(true);
        this.naming = addRenderableWidget(box);
        setFocused(box);
    }

    /* Ends the naming, sending the name typed when {@code keep}. */
    private void finishNaming(final boolean keep) {
        final EditBox box = this.naming;
        if (box == null) {
            return;
        }
        if (keep) {
            SpacePersistor.RENAME_TO.send(box.getValue());
        }
        removeWidget(box);
        this.naming = null;
    }

    /*
     * The display: the land, dimmed as a screen shows it, the chunk grid, another owner's chunks hatched, its own area
     * tinted and outlined in moving dashes, the Persistor's beacon, the player, the chunk under the pointer, north,
     * the zoom keys and how many chunks it shows.
     */
    private void display(final MachineView view, final PersistorReadings read) {
        final Colours c = PALETTE.get();
        final GuiGraphics g = view.g();
        final int x0 = view.x(DISPLAY_X);
        final int y0 = view.y(DISPLAY_Y);
        final int cell = SpacePersistorLayout.cell(this.zoom);
        final int across = SpacePersistorLayout.across(this.zoom);
        final int middle = SpacePersistorLayout.middle(this.zoom);
        final ChunkPos own = new ChunkPos(this.persistor.getBlockPos());
        final ResourceLocation picture = this.land.around(new BlockPos(own.getMinBlockX(),
                this.persistor.getBlockPos().getY(), own.getMinBlockZ()), Math.max(1, 16 / cell), DISPLAY, DISPLAY,
                c.unknown());
        if (picture != null) {
            g.blit(picture, x0, y0, 0, 0, DISPLAY, DISPLAY, DISPLAY, DISPLAY);
        }
        for (int row = 0; row < DISPLAY; row++) {
            MachineArt.hline(g, x0, x0 + DISPLAY, y0 + row, row % 2 == 0 ? c.dimEven() : c.dimOdd());
        }
        if (cell > 1) {
            for (int k = 1; k < across; k++) {
                final int grid = this.zoom == 0 ? c.gridNear() : c.gridFar();
                MachineArt.vline(g, x0 + k * cell, y0, y0 + DISPLAY, grid);
                MachineArt.hline(g, x0, x0 + DISPLAY, y0 + k * cell, grid);
            }
        }
        for (final long packed : read.others()) {
            final ChunkPos other = new ChunkPos(packed);
            final int cx = other.x - own.x + middle;
            final int cz = other.z - own.z + middle;
            if (cx >= 0 && cx < across && cz >= 0 && cz < across) {
                hatch(g, x0 + cx * cell, y0 + cz * cell, cell, c.othersHatch());
            }
        }
        area(g, x0, y0, cell, across, middle, c);
        beacon(g, x0 + middle * cell, y0 + middle * cell, cell, c);
        player(g, x0, y0, cell, middle, own, c);
        final OptionalLong hovered = SpacePersistorLayout.chunkAt(view.faceMouseX(), view.faceMouseY(), this.zoom);
        if (hovered.isPresent() && this.naming == null) {
            final int hx = x0 + (PersistorShapes.dx(hovered.getAsLong()) + middle) * cell;
            final int hy = y0 + (PersistorShapes.dz(hovered.getAsLong()) + middle) * cell;
            MachineArt.frame(g, hx - HOVER_GROW, hy - HOVER_GROW, hx + cell + HOVER_GROW, hy + cell + HOVER_GROW,
                    c.white());
        }
        MachineArt.textCentred(g, this.font, MachineWords.upper(SpacePersistor.NORTH), x0 + DISPLAY / 2, y0 + 2,
                c.label());
        zoomKeys(g, x0, y0, c);
        final String counted = MachineWords.upper(SpacePersistor.ACROSS.with(across, across));
        final int w = MachineArt.width(this.font, counted) + 4;
        MachineArt.fill(g, x0 + ACROSS_X, y0 + ACROSS_Y, x0 + ACROSS_X + w, y0 + ACROSS_Y + 7, c.labelGround());
        MachineArt.text(g, this.font, counted, x0 + ACROSS_X + 2, y0 + ACROSS_Y + 1, c.label());
        // A display this large catches the light along its top edge only; a streak across it would hide the land.
        MachineArt.hline(g, x0, x0 + DISPLAY, y0, c.shine());
    }

    /* Its own area, tinted, each edge it shares with no chunk of its own dashed in moving aqua. */
    private void area(final GuiGraphics g, final int x0, final int y0, final int cell, final int across,
                      final int middle, final Colours c) {
        final Set<Long> offsets = offsets();
        final int phase = this.ticks / FRAME_TICKS % DASH;
        for (int cz = 0; cz < across; cz++) {
            int runStart = -1;
            for (int cx = 0; cx <= across; cx++) {
                final boolean in = cx < across && offsets.contains(PersistorShapes.pack(cx - middle, cz - middle));
                if (in && runStart < 0) {
                    runStart = cx;
                } else if (!in && runStart >= 0) {
                    MachineArt.fill(g, x0 + runStart * cell, y0 + cz * cell, x0 + cx * cell, y0 + cz * cell + cell,
                            c.keptFill());
                    runStart = -1;
                }
            }
        }
        for (final long offset : offsets) {
            final int dx = PersistorShapes.dx(offset);
            final int dz = PersistorShapes.dz(offset);
            final int cx = dx + middle;
            final int cz = dz + middle;
            if (cx < 0 || cx >= across || cz < 0 || cz >= across) {
                continue;
            }
            final int x = x0 + cx * cell;
            final int y = y0 + cz * cell;
            for (int i = 0; i < cell; i++) {
                if (!offsets.contains(PersistorShapes.pack(dx, dz - 1))) {
                    dash(g, x + i, y, phase, c);
                }
                if (!offsets.contains(PersistorShapes.pack(dx, dz + 1))) {
                    dash(g, x + i, y + cell - 1, phase, c);
                }
                if (!offsets.contains(PersistorShapes.pack(dx - 1, dz))) {
                    dash(g, x, y + i, phase, c);
                }
                if (!offsets.contains(PersistorShapes.pack(dx + 1, dz))) {
                    dash(g, x + cell - 1, y + i, phase, c);
                }
            }
        }
    }

    /* Its area's offsets, worked out again only when its area changes. */
    private Set<Long> offsets() {
        final PersistorArea now = this.persistor.area();
        if (!now.equals(this.shownArea)) {
            this.shownArea = now;
            this.shownOffsets = now.offsets(this.persistor.alongX());
        }
        return this.shownOffsets;
    }

    private static void dash(final GuiGraphics g, final int x, final int y, final int phase, final Colours c) {
        MachineArt.dot(g, x, y, (x + y + phase) % DASH < DASH / 2 ? c.aqua() : c.aquaDark());
    }

    private static void hatch(final GuiGraphics g, final int x, final int y, final int cell, final int colour) {
        for (int s = 0; s < cell; s++) {
            for (int u = 0; u < cell; u++) {
                if ((s + u) % 3 == 0) {
                    MachineArt.dot(g, x + s, y + u, colour);
                }
            }
        }
    }

    /* The Persistor in its chunk, a pulsing ring round its mark. */
    private void beacon(final GuiGraphics g, final int x, final int y, final int cell, final Colours c) {
        final int bx = x + cell / 2 - 2;
        final int by = y + cell / 2 - 2;
        final int ring = 2 + this.ticks / FRAME_TICKS % RING_STEPS;
        for (int k = 0; k < RING_POINTS; k++) {
            final double angle = k * Math.PI * 2 / RING_POINTS;
            MachineArt.dot(g, bx + 2 + (int) Math.round(ring * RING_STRETCH * Math.cos(angle)),
                    by + 2 + (int) Math.round(ring * RING_STRETCH * Math.sin(angle)),
                    MachineArt.alpha(c.white(), Math.max(0, 140 - ring * 30)));
        }
        MachineArt.fill(g, bx, by, bx + MARK, by + MARK, c.black());
        MachineArt.fill(g, bx + 1, by + 1, bx + MARK - 1, by + MARK - 1, c.aqua());
    }

    /* The player, an arrowhead where they stand, when that is on the display. */
    private void player(final GuiGraphics g, final int x0, final int y0, final int cell, final int middle,
                        final ChunkPos own, final Colours c) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        final double bpp = 16.0 / cell;
        final int px = x0 + middle * cell + (int) Math.floor((this.minecraft.player.getX() - own.getMinBlockX())
                / bpp);
        final int py = y0 + middle * cell + (int) Math.floor((this.minecraft.player.getZ() - own.getMinBlockZ())
                / bpp);
        if (px < x0 + 2 || py < y0 + 2 || px >= x0 + DISPLAY - 2 || py >= y0 + DISPLAY - 3) {
            return;
        }
        MachineArt.dot(g, px, py - 2, c.white());
        MachineArt.hline(g, px - 1, px + 2, py - 1, c.white());
        MachineArt.hline(g, px - 2, px + 3, py, c.white());
        MachineArt.hline(g, px - 2, px, py + 1, c.white());
        MachineArt.hline(g, px + 1, px + 3, py + 1, c.white());
    }

    private void zoomKeys(final GuiGraphics g, final int x0, final int y0, final Colours c) {
        for (int k = 0; k < 2; k++) {
            final int x = x0 + ZOOM_X;
            final int y = y0 + ZOOM_Y + k * ZOOM_PITCH;
            MachineArt.fill(g, x, y, x + ZOOM_SIZE, y + ZOOM_SIZE, c.zoomGround());
            MachineArt.frame(g, x, y, x + ZOOM_SIZE, y + ZOOM_SIZE, c.zoomEdge());
            MachineArt.hline(g, x + 2, x + 8, y + 5, c.white());
            if (k == 0) {
                MachineArt.vline(g, x + 5, y + 2, y + 8, c.white());
            }
        }
    }

    /* The column beside the display: plates, LCD, shape keys, radius, switch, lamps. */
    private void column(final MachineView view, final PersistorReadings read) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final Colours c = PALETTE.get();
        final GuiGraphics g = view.g();
        MachineArt.plate(g, this.font, view.x(COLUMN_X), view.y(PLATE_Y), MachineArt.fit(this.font,
                MachineWords.upper(SpacePersistor.PLATE), NAME_MOST - 16), m.aluminium(), m.plateInk());
        if (this.naming == null) {
            MachineArt.plate(g, this.font, view.x(COLUMN_X), view.y(NAME_Y), MachineArt.fit(this.font,
                    nameShown(), NAME_MOST - 16), m.classPlate(), m.classInk());
        }
        lcd(view, read);
        MachineArt.engrave(g, this.font, MachineWords.upper(SpacePersistor.SHAPE), view.x(COLUMN_X),
                view.y(SHAPE_WORD_Y), false);
        final List<PersistorShape> shapes = PersistorShape.round();
        final PersistorShape chosen = this.persistor.area().shape();
        for (int k = 0; k < KEYS; k++) {
            if (shapes.get(k) == chosen) {
                final int kx = view.x(COLUMN_X + k * KEY_PITCH) - 1;
                final int ky = view.y(KEYS_Y) - 1;
                g.blit(PICTURE, kx, ky, 0, SPRITES_Y, LIT_KEY_WIDTH, LIT_KEY_HEIGHT, SHEET, SHEET);
                glyph(g, kx + 6, ky + 3, shapes.get(k), c.keyInk());
            }
        }
        MachineArt.engrave(g, this.font, MachineWords.upper(SpacePersistor.RADIUS), view.x(COLUMN_X),
                view.y(RADIUS_WORD_Y), false);
        final int radius = this.persistor.area().radius();
        MachineArt.thumbwheel(g, this.font, view.x(WHEEL_X), view.y(WHEEL_Y), String.format(Locale.ROOT, "%02d",
                radius));
        MachineArt.engrave(g, this.font, size(chosen, radius), view.x(SIZE_X), view.y(SIZE_Y), false);
        MachineArt.rocker(g, view.x(SWITCH_X), view.y(SWITCH_Y), this.persistor.control().on());
        MachineArt.engrave(g, this.font, MachineWords.upper(SpacePersistor.ON), view.x(ON_X), view.y(ON_Y), false);
        MachineArt.engrave(g, this.font, MachineWords.upper(SpacePersistor.OFF), view.x(OFF_X), view.y(OFF_Y),
                false);
        MachineArt.lamp(g, view.x(KEEPING_X), view.y(LAMPS_Y), m.lampRun(), read.keeping());
        MachineArt.engrave(g, this.font, MachineWords.upper(SpacePersistor.KEEPING), view.x(KEEPING_X
                + LAMP_WORD_GAP), view.y(LAMPS_Y + 1), false);
        MachineArt.lamp(g, view.x(FULL_X), view.y(LAMPS_Y), m.lampWait(), read.full());
        MachineArt.engrave(g, this.font, MachineWords.upper(SpacePersistor.FULL), view.x(FULL_X + LAMP_WORD_GAP),
                view.y(LAMPS_Y + 1), false);
    }

    /* The LCD: what it keeps, its owner's against their number, the server's against its budget, the power. */
    private void lcd(final MachineView view, final PersistorReadings read) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final GuiGraphics g = view.g();
        final int x = view.x(LCD_X);
        final int y = view.y(LCD_Y);
        MachineArt.lcd(g, this.font, x, y, LCD_WIDTH, LCD_HEIGHT, List.of(), true, false);
        final List<TextKey> labels = List.of(SpacePersistor.THIS, SpacePersistor.YOURS, SpacePersistor.SERVER,
                SpacePersistor.POWER);
        final List<String> values = List.of(Integer.toString(read.kept()), read.owners() + "/" + read.ownersLimit(),
                read.server() + "/" + read.serverBudget(), read.energy() ? watts(read.watts())
                        : MachineWords.upper(SpacePersistor.POWER_OFF));
        for (int i = 0; i < LCD_LINES; i++) {
            final int colour = i == 0 ? m.lcdGlow() : MachineArt.mix(m.lcdGlow(), m.lcdGround(), 0.25);
            final String value = values.get(i);
            MachineArt.text(g, this.font, MachineWords.upper(labels.get(i)), x + 4, y + 4 + i * LCD_PITCH, colour);
            MachineArt.text(g, this.font, value, x + LCD_WIDTH - 4 - MachineArt.width(this.font, value),
                    y + 4 + i * LCD_PITCH, colour);
        }
        MachineArt.glassShine(g, x, y, x + LCD_WIDTH, y + LCD_HEIGHT);
    }

    /* The shape on a key: a square, a circle, a line, or a hand-drawn stroke. */
    private static void glyph(final GuiGraphics g, final int x, final int y, final PersistorShape shape,
                              final int colour) {
        switch (shape) {
            case SQUARE -> MachineArt.frame(g, x, y, x + 7, y + 7, colour);
            case CIRCLE -> {
                for (int k = 0; k < RING_POINTS; k++) {
                    final double angle = k * Math.PI / 8;
                    MachineArt.dot(g, x + 3 + (int) Math.round(3.2 * Math.cos(angle)),
                            y + 3 + (int) Math.round(3.2 * Math.sin(angle)), colour);
                }
            }
            case LINE -> {
                MachineArt.hline(g, x, x + 8, y + 3, colour);
                MachineArt.hline(g, x, x + 8, y + 4, colour);
            }
            case FREE -> {
                final int[][] stroke = {{0, 5}, {1, 4}, {2, 4}, {3, 3}, {4, 2}, {5, 2}, {6, 1}, {7, 1}, {2, 6},
                    {3, 6}};
                for (final int[] dot : stroke) {
                    MachineArt.dot(g, x + dot[0], y + dot[1], colour);
                }
            }
        }
    }

    /* Its name, or the name of the biome it stands in when it has none. */
    private String nameShown() {
        final String given = this.persistor.name();
        if (!given.isEmpty()) {
            return given.toUpperCase(Locale.ROOT);
        }
        if (this.minecraft == null || this.minecraft.level == null) {
            return "";
        }
        return this.minecraft.level.getBiome(this.persistor.getBlockPos()).unwrapKey()
                .map(key -> MachineWords.upper(GameText.of(Component.translatable(key.location()
                        .toLanguageKey("biome"))))).orElse("");
    }

    private static String size(final PersistorShape shape, final int radius) {
        final int side = radius * 2 + 1;
        return switch (shape) {
            case SQUARE, CIRCLE -> MachineWords.upper(SpacePersistor.SIZE.with(side, side));
            case LINE -> MachineWords.upper(SpacePersistor.SIZE.with(side, 1));
            case FREE -> "";
        };
    }

    private static String watts(final long watts) {
        final UnitFormatter.Scaled scaled = UnitFormatter.forCurrentLocale().si(watts);
        return GameText.resolve(EnergyTexts.AMOUNT.with(scaled.number(), scaled.prefix(),
                JsEnergy.WATT_SYMBOL.text()));
    }

    /* What the mouse at {@code x, y} of the screen is told. */
    private List<Component> tooltip(final double x, final double y, final int mouseX, final int mouseY) {
        final int tab = MachineScreenLayout.tabAt(x, y);
        if (tab >= 0 && tab < tabs().size()) {
            return List.of(GameText.component(MachineScreen.TAB_NAMES.get(tabs().get(tab))));
        }
        if (onProtection()) {
            final MachineView view = view(null, mouseX, mouseY, 0F);
            return this.protection.tooltip(view, view.faceMouseX(), view.faceMouseY());
        }
        final double fx = x - FACE_X;
        final double fy = y - FACE_Y;
        final int zoomKey = SpacePersistorLayout.zoomAt(fx, fy);
        if (zoomKey >= 0) {
            return List.of(GameText.component(zoomKey == 0 ? SpacePersistor.CLOSER.text()
                    : SpacePersistor.FURTHER.text()));
        }
        final OptionalLong chunk = SpacePersistorLayout.chunkAt(fx, fy, this.zoom);
        if (chunk.isPresent()) {
            return chunkLines(chunk.getAsLong());
        }
        final int key = SpacePersistorLayout.keyAt(fx, fy);
        if (key >= 0) {
            return List.of(GameText.component(SpacePersistor.shapeName(PersistorShape.round().get(key)).text()));
        }
        if (SpacePersistorLayout.onName(fx, fy) && this.naming == null) {
            return List.of(GameText.component(SpacePersistor.RENAME.text()));
        }
        return List.of();
    }

    /* The lines of the chunk at {@code offset}: where it is, what a click does, and the room left. */
    private List<Component> chunkLines(final long offset) {
        final ChunkPos own = new ChunkPos(this.persistor.getBlockPos());
        final int dx = PersistorShapes.dx(offset);
        final int dz = PersistorShapes.dz(offset);
        final ChunkPos at = new ChunkPos(own.x + dx, own.z + dz);
        final PersistorReadings read = this.persistor.screenReadings();
        final List<Component> lines = new ArrayList<>();
        lines.add(GameText.component(SpacePersistor.CHUNK.with(at.x, at.z)));
        final Text action;
        if (dx == 0 && dz == 0) {
            action = SpacePersistor.OWN_CHUNK.text();
        } else if (read.others().contains(at.toLong())) {
            action = SpacePersistor.OTHERS.text();
        } else {
            action = offsets().contains(offset) ? SpacePersistor.CLICK_LET_GO.text() : SpacePersistor.CLICK_KEEP
                    .text();
        }
        lines.add(GameText.component(action).withStyle(ChatFormatting.GRAY));
        final Text room = read.roomLeft() > 0 ? SpacePersistor.MORE_FIT.with(read.roomLeft(), read.ownersLimit())
                : SpacePersistor.NONE_FIT.with(read.ownersLimit());
        lines.add(GameText.component(room).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private void inventory(final GuiGraphics g) {
        MachineArt.window(g, this.leftPos + MachineScreenLayout.INVENTORY_X, this.topPos
                + MachineScreenLayout.INVENTORY_Y, this.leftPos + MachineScreenLayout.INVENTORY_X
                + MachineScreenLayout.INVENTORY_WIDTH, this.topPos + MachineScreenLayout.INVENTORY_Y
                + MachineScreenLayout.INVENTORY_HEIGHT, 0, 0);
        for (final Slot slot : this.menu.slots) {
            if (slot.container instanceof Inventory) {
                MachineArt.slot(g, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
            }
        }
    }

    private MachineView view(final @Nullable GuiGraphics g, final int mouseX, final int mouseY,
                             final float partialTick) {
        return new MachineView(g, this.font, this.menu, this.leftPos, this.topPos, mouseX, mouseY,
                this.ticks + partialTick, this::sendButton);
    }

    /**
     * The display's colours.
     *
     * @param aqua        the dashes of its own area, lit
     * @param aquaDark    and dark
     * @param keptFill    the tint of its own area
     * @param othersHatch the hatching of another owner's chunks
     * @param unknown     land the player's game has not loaded
     * @param dimEven     the screen's dimming on even rows
     * @param dimOdd      and on odd rows
     * @param gridNear    the chunk grid, close in
     * @param gridFar     and further out
     * @param white       the pointer's frame, the beacon and the player
     * @param black       the beacon's edge
     * @param zoomGround  the zoom keys' ground
     * @param zoomEdge    their edge
     * @param labelGround the ground of the count of chunks across
     * @param label       the words on the display
     * @param keyInk      the shape on the lit key
     * @param shine       the light along the display's glass
     */
    public record Colours(int aqua, int aquaDark, int keptFill, int othersHatch, int unknown, int dimEven, int dimOdd,
                          int gridNear, int gridFar, int white, int black, int zoomGround, int zoomEdge,
                          int labelGround, int label, int keyInk, int shine) {
    }
}
