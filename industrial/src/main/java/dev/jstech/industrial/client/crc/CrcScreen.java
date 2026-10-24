/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.machine.IMachineFace;
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineChrome;
import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.client.machine.MachineView;
import dev.jstech.core.client.machine.ProtectionTab;
import dev.jstech.core.client.machine.RedstoneTab;
import dev.jstech.core.client.machine.SidesTab;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.crc.CrcLayout;
import dev.jstech.industrial.crc.CrcReadings;
import dev.jstech.industrial.crc.CrcScreenTexts;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * The Computational Research Centre's screen, as the approved design draws it: six tabs along the top, its own three,
 * Processors, Research and Group ({@link CrcProcessorsTab}, {@link CrcResearchTab}, {@link CrcGroupTab}), then the
 * Sides, Redstone and Protection tabs every machine has, drawn as every machine's are, centred in its wider window. The
 * player's inventory sits centred below on the tabs that take items; Research and Group take none, so their face takes
 * the whole window.
 */
public final class CrcScreen extends CoreContainerScreen<MachineMenu> {

    private final Map<Integer, ICrcTab> own;
    private final Map<Integer, IMachineFace> kit;
    private int ticks;

    /** Its own tabs' icons: Research's flask, then Group's two cabinets on their cable. */
    public static final ResourceLocation TAB_ICONS = ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
            "textures/gui/crc/tabs.png");
    /** Processors' icon: a research processor. */
    public static final ResourceLocation PROCESSOR_ICON = ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
            "textures/item/research_processor.png");
    /* The tabs in their order along the top, by the menu's numbers. */
    private static final List<Integer> ORDER = List.of(MachineMenu.MAIN, CrcLayout.RESEARCH_TAB, CrcLayout.GROUP_TAB,
            MachineMenu.SIDES, MachineMenu.REDSTONE, MachineMenu.PROTECTION);
    private static final Map<Integer, TextKey> NAMES = Map.of(MachineMenu.MAIN, CrcScreenTexts.TAB_PROCESSORS,
            CrcLayout.RESEARCH_TAB, CrcScreenTexts.TAB_RESEARCH, CrcLayout.GROUP_TAB, CrcScreenTexts.TAB_GROUP);
    private static final int ICON = 16;
    private static final int INVENTORY_WORD_X = 8;
    private static final int INVENTORY_WORD_Y = 6;
    /* How far right the standard tabs' face sits, centred in the wider window. */
    private static final int SHIFT = MachineScreenLayout.kitShift(CrcLayout.WIDTH);

    public CrcScreen(final MachineMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = CrcLayout.WIDTH;
        this.imageHeight = CrcLayout.HEIGHT;
        this.own = Map.of(MachineMenu.MAIN, new CrcProcessorsTab(), CrcLayout.RESEARCH_TAB, new CrcResearchTab(),
                CrcLayout.GROUP_TAB, new CrcGroupTab());
        this.kit = Map.of(MachineMenu.SIDES, new SidesTab(), MachineMenu.REDSTONE, new RedstoneTab(),
                MachineMenu.PROTECTION, new ProtectionTab());
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }
        final List<Component> lines = tooltip(mouseX, mouseY, partialTick);
        if (!lines.isEmpty()) {
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final int place = MachineScreenLayout.tabAt(x, y, ORDER.size());
        if (place >= 0 && button == InputConstants.MOUSE_BUTTON_LEFT) {
            final int tab = ORDER.get(place);
            this.menu.showTab(tab);
            sendButton(MachineMenu.FIRST_TAB + tab);
            return true;
        }
        final ICrcTab tab = this.own.get(this.menu.tab());
        if (tab != null && tab.clicked(view(null, (int) mouseX, (int) mouseY, 0F), x, y, button)) {
            return true;
        }
        final IMachineFace face = this.kit.get(this.menu.tab());
        if (face != null) {
            final MachineView view = kitView(null, (int) mouseX, (int) mouseY, 0F);
            if (onKitFace(view) && face.clicked(view, view.faceMouseX(), view.faceMouseY(), button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (scrollY != 0) {
            final ICrcTab tab = this.own.get(this.menu.tab());
            if (tab != null && tab.scrolled(view(null, (int) mouseX, (int) mouseY, 0F), mouseX - this.leftPos,
                    mouseY - this.topPos, scrollY)) {
                return true;
            }
            final IMachineFace face = this.kit.get(this.menu.tab());
            final MachineView view = kitView(null, (int) mouseX, (int) mouseY, 0F);
            if (face != null && onKitFace(view) && face.scrolled(view, view.faceMouseX(), view.faceMouseY(),
                    scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.ticks++;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int active = this.menu.tab();
        final int shownAt = Math.max(0, ORDER.indexOf(active));
        for (int place = 0; place < ORDER.size(); place++) {
            if (place != shownAt) {
                tab(g, place, false);
            }
        }
        final boolean inventory = this.menu.machine().inventoryOn(active);
        final int tabLeft = this.leftPos + CrcLayout.tabX(shownAt);
        final int bottom = inventory ? CrcLayout.WINDOW_TOP + CrcLayout.WINDOW_HEIGHT : CrcLayout.HEIGHT;
        MachineArt.window(g, this.leftPos, this.topPos + CrcLayout.WINDOW_TOP, this.leftPos + CrcLayout.WIDTH,
                this.topPos + bottom, tabLeft + 1, tabLeft + MachineScreenLayout.TAB_WIDTH - 1);
        tab(g, shownAt, true);
        final ICrcTab tab = this.own.get(active);
        if (tab != null) {
            tab.draw(view(g, mouseX, mouseY, partialTick));
        } else {
            final MachineView view = kitView(g, mouseX, mouseY, partialTick);
            g.blit(MachineScreen.STEEL, this.leftPos + SHIFT + MachineScreenLayout.FACE_X, this.topPos
                    + MachineScreenLayout.FACE_Y, 0, 0, MachineScreenLayout.FACE_WIDTH, MachineScreenLayout.FACE_HEIGHT,
                    MachineScreenLayout.FACE_WIDTH, MachineScreenLayout.FACE_HEIGHT);
            MachineChrome.plates(view, this.title);
            MachineChrome.lamps(view);
            final IMachineFace face = this.kit.get(active);
            if (face != null) {
                face.draw(view);
            }
        }
        if (inventory) {
            MachineArt.window(g, this.leftPos + CrcLayout.INVENTORY_X, this.topPos + CrcLayout.INVENTORY_Y,
                    this.leftPos + CrcLayout.INVENTORY_X + CrcLayout.INVENTORY_WIDTH, this.topPos
                            + CrcLayout.INVENTORY_Y + CrcLayout.INVENTORY_HEIGHT, 0, 0);
            for (final Slot slot : this.menu.slots) {
                if (slot.container instanceof Inventory) {
                    MachineArt.slot(g, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
                }
            }
        }
    }

    /* The inventory's title where it shows, then what a standard tab shows over the items, in the screen's space. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        if (this.menu.machine().inventoryOn(this.menu.tab())) {
            Draw.text(g, this.font, this.playerInventoryTitle, CrcLayout.INVENTORY_X + INVENTORY_WORD_X,
                    CrcLayout.INVENTORY_Y + INVENTORY_WORD_Y, MachineArt.PALETTE.get().title());
        }
        final IMachineFace face = this.kit.get(this.menu.tab());
        if (face != null) {
            face.drawOver(new MachineView(g, this.font, this.menu, SHIFT, 0, mouseX - this.leftPos,
                    mouseY - this.topPos, this.ticks, this::sendButton));
        }
    }

    /* What a tab, or what the tab shown has under the mouse, tells it. */
    private List<Component> tooltip(final int mouseX, final int mouseY, final float partialTick) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final int place = MachineScreenLayout.tabAt(x, y, ORDER.size());
        if (place >= 0) {
            final int tab = ORDER.get(place);
            final TextKey own = NAMES.get(tab);
            return List.of(GameText.component(own != null ? own : MachineScreen.TAB_NAMES.get(tab)));
        }
        final ICrcTab tab = this.own.get(this.menu.tab());
        if (tab != null) {
            return tab.tooltip(view(null, mouseX, mouseY, partialTick), x, y);
        }
        final IMachineFace face = this.kit.get(this.menu.tab());
        final MachineView view = kitView(null, mouseX, mouseY, partialTick);
        if (face == null || !onKitFace(view)) {
            return List.of();
        }
        if (MachineScreenLayout.on(view.faceMouseX(), view.faceMouseY(), MachineScreenLayout.LAMP_X - 1,
                MachineScreenLayout.LAMP_Y - 1, MachineScreenLayout.LAMP_WORD_X - MachineScreenLayout.LAMP_X + 24,
                MachineScreenLayout.LAMP_PITCH * MachineChrome.lampCount())) {
            return List.of(GameText.component(MachineBlockEntity.STATUS_LINE.with(view.machine().status().text())));
        }
        return face.tooltip(view, view.faceMouseX(), view.faceMouseY());
    }

    /* The tab at place {@code place} along the top, raised when shown, with its icon. */
    private void tab(final GuiGraphics g, final int place, final boolean active) {
        final int tab = ORDER.get(place);
        final int x = this.leftPos + CrcLayout.tabX(place);
        if (tab == MachineMenu.MAIN || tab == CrcLayout.RESEARCH_TAB || tab == CrcLayout.GROUP_TAB) {
            MachineArt.tab(g, x, this.topPos, active);
            final int icon = MachineScreenLayout.TAB_ICON;
            if (tab == MachineMenu.MAIN) {
                g.blit(PROCESSOR_ICON, x + icon, this.topPos + icon, 0, 0, ICON, ICON, ICON, ICON);
            } else {
                g.blit(TAB_ICONS, x + icon, this.topPos + icon, (tab - CrcLayout.RESEARCH_TAB) * ICON, 0, ICON, ICON,
                        ICON * 2, ICON);
            }
        } else {
            MachineChrome.tab(g, x, this.topPos, tab, active);
        }
    }

    private CrcView view(final @Nullable GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        return new CrcView(g, this.font, this.menu, this.title, this.menu.screenData(CrcReadings.class),
                this.leftPos, this.topPos, mouseX, mouseY, this.ticks + partialTick, this::sendButton);
    }

    /* The view a standard tab draws with: its face centred in the wider window. */
    private MachineView kitView(final @Nullable GuiGraphics g, final int mouseX, final int mouseY,
                                final float partialTick) {
        return new MachineView(g, this.font, this.menu, this.leftPos + SHIFT, this.topPos, mouseX, mouseY,
                this.ticks + partialTick, this::sendButton);
    }

    private static boolean onKitFace(final MachineView view) {
        return MachineScreenLayout.on(view.faceMouseX(), view.faceMouseY(), 0, 0, MachineScreenLayout.FACE_WIDTH,
                MachineScreenLayout.FACE_HEIGHT);
    }
}
