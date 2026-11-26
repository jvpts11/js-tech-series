/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import static dev.jstech.core.machine.MachineScreenLayout.CLASS_PLATE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.FACE_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.FACE_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.FACE_X;
import static dev.jstech.core.machine.MachineScreenLayout.FACE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.INVENTORY_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.INVENTORY_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.INVENTORY_X;
import static dev.jstech.core.machine.MachineScreenLayout.INVENTORY_Y;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_PITCH;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_WORD_X;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_X;
import static dev.jstech.core.machine.MachineScreenLayout.LAMP_Y;
import static dev.jstech.core.machine.MachineScreenLayout.NAME_PLATE_MOST;
import static dev.jstech.core.machine.MachineScreenLayout.NAME_PLATE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.PLATE_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.PLATE_X;
import static dev.jstech.core.machine.MachineScreenLayout.TAB_ICON;
import static dev.jstech.core.machine.MachineScreenLayout.TAB_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.TABS;
import static dev.jstech.core.machine.MachineScreenLayout.WINDOW_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.WINDOW_TOP;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.Machines;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The screen every machine of every mod opens, as the approved design draws it, in the game's own look: five tabs
 * along the top of a window holding the machine's steel face, with its name and class on plates and its three lamps,
 * RUN, WAIT and FAULT, on every tab; and the player's inventory below, in a window of its own. The Main tab shows the
 * machine's own face ({@link MachineFaces}); Sides, Redstone, Upgrades and Protection are every machine's; a machine
 * with tabs of its own has them after these, each with its icon and its face.
 */
public final class MachineScreen extends CoreContainerScreen<MachineMenu> {

    private final List<IMachineFace> faces = new ArrayList<>();
    /* The machine's own tabs after the five, as many as both its menu and its mod's client give it. */
    private final List<MachineFaces.OwnTab> own;
    private int ticks;

    /** The steel face every machine's tabs are drawn on. */
    public static final ResourceLocation STEEL = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "textures/gui/machine/steel_face.png");
    /** The tabs' names, in their order. */
    public static final List<TextKey> TAB_NAMES = List.of(MachineScreenTexts.TAB_MAIN,
            MachineScreenTexts.TAB_SIDES, MachineScreenTexts.TAB_REDSTONE, MachineScreenTexts.TAB_UPGRADES,
            MachineScreenTexts.TAB_PROTECTION);
    private static final int INVENTORY_WORD_X = 8;
    private static final int INVENTORY_WORD_Y = 6;
    /* How far a row of bins' window's top runs on, hidden, under the face's; how far in its band of plate stands. */
    private static final int TRAY_RUN_ON = 6;
    private static final int TRAY_BAND_INSET = 4;
    /* The most items an empty inlet names, and how wide its tooltip's lines run, in pixels. */
    private static final int MOST_INLET_ITEMS = 16;
    private static final int TIP_WIDTH = 200;

    public MachineScreen(final MachineMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = MachineScreenLayout.WIDTH;
        this.imageHeight = MachineScreenLayout.HEIGHT + below();
        this.faces.add(MachineFaces.of(menu.machine()));
        this.faces.add(new SidesTab());
        this.faces.add(new RedstoneTab());
        this.faces.add(new UpgradesTab());
        this.faces.add(new ProtectionTab());
        final List<MachineFaces.OwnTab> declared = MachineFaces.tabsOf(menu.machine());
        this.own = declared.subList(0, Math.min(declared.size(), menu.tabs() - TABS));
        for (final MachineFaces.OwnTab tab : this.own) {
            this.faces.add(tab.face().apply(menu.machine()));
        }
    }

    /**
     * Hands the mod's bus the screen, once, from the Core's client: a machine with a screen of its own opens that one
     * ({@link MachineScreens}), every other this one.
     */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(Machines.MENU.get(),
                MachineScreens::open));
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }
        final List<Component> lines = tooltip(view(null, mouseX, mouseY, partialTick));
        if (!lines.isEmpty()) {
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int tab = MachineScreenLayout.tabAt(mouseX - this.leftPos, mouseY - this.topPos, this.faces.size());
        if (tab >= 0 && button == InputConstants.MOUSE_BUTTON_LEFT) {
            this.menu.showTab(tab);
            sendButton(MachineMenu.FIRST_TAB + tab);
            return true;
        }
        if (hasControlDown() && button == InputConstants.MOUSE_BUTTON_LEFT && lockable(this.hoveredSlot)) {
            sendButton(MachineMenu.FIRST_LOCK + this.hoveredSlot.getContainerSlot());
            return true;
        }
        final MachineView view = view(null, (int) mouseX, (int) mouseY, 0F);
        if (onFace(view) && this.faces.get(shownTab()).clicked(view, view.faceMouseX(), view.faceMouseY(),
                button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final MachineView view = view(null, (int) mouseX, (int) mouseY, 0F);
        if (scrollY != 0 && onFace(view) && this.faces.get(shownTab()).scrolled(view, view.faceMouseX(),
                view.faceMouseY(), scrollY)) {
            return true;
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
        final int active = shownTab();
        for (int i = 0; i < this.faces.size(); i++) {
            if (i != active) {
                tab(g, i, false);
            }
        }
        final int tabLeft = this.leftPos + MachineScreenLayout.tabX(active);
        MachineArt.window(g, this.leftPos, this.topPos + WINDOW_TOP, this.leftPos + MachineScreenLayout.WIDTH,
                this.topPos + WINDOW_TOP + WINDOW_HEIGHT, tabLeft + 1, tabLeft + TAB_WIDTH - 1);
        tab(g, active, true);
        g.blit(STEEL, this.leftPos + FACE_X, this.topPos + FACE_Y, 0, 0, FACE_WIDTH, FACE_HEIGHT, FACE_WIDTH,
                FACE_HEIGHT);
        final MachineView view = view(g, mouseX, mouseY, partialTick);
        if (!this.faces.get(active).drawsPlates()) {
            MachineChrome.plates(view, this.title);
        }
        MachineChrome.lamps(view);
        this.faces.get(active).draw(view);
        this.menu.machine().tray().ifPresent(tray -> tray(g, tray));
        final int inventoryY = INVENTORY_Y + below();
        MachineArt.window(g, this.leftPos + INVENTORY_X, this.topPos + inventoryY,
                this.leftPos + INVENTORY_X + INVENTORY_WIDTH, this.topPos + inventoryY + INVENTORY_HEIGHT, 0, 0);
        for (final Slot slot : this.menu.slots) {
            if (slot.container instanceof Inventory) {
                MachineArt.slot(g, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
            }
        }
    }

    /* The inventory's title, then what the tab shows over the items, in the screen's own space. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        Draw.text(g, this.font, this.playerInventoryTitle, INVENTORY_X + INVENTORY_WORD_X,
                INVENTORY_Y + below() + INVENTORY_WORD_Y, MachineArt.PALETTE.get().title());
        this.faces.get(shownTab()).drawOver(new MachineView(g, this.font, this.menu, 0, 0,
                mouseX - this.leftPos, mouseY - this.topPos, this.ticks, this::sendButton));
    }

    /* How far a row of bins under the window, when the machine has one, pushes the inventory down. */
    private int below() {
        return MachineScreenLayout.below(this.menu.machine().tray().isPresent());
    }

    /*
     * The row of a sorter's bins under the window: a window whose top runs on under the face's, so only its sides and
     * its foot show, on a band of the face's plate; its name at its left, a frame for each bin and what is written
     * under each.
     */
    private void tray(final GuiGraphics g, final FacePicture.Tray tray) {
        final int x = this.leftPos;
        final int y = this.topPos + MachineScreenLayout.TRAY_Y;
        final int foot = y + MachineScreenLayout.TRAY_HEIGHT;
        g.enableScissor(x, y, x + MachineScreenLayout.WIDTH, foot);
        MachineArt.window(g, x, y - TRAY_RUN_ON, x + MachineScreenLayout.WIDTH, foot, 0, 0);
        g.disableScissor();
        MachineArt.fill(g, x + TRAY_BAND_INSET, y, x + MachineScreenLayout.WIDTH - TRAY_BAND_INSET - 1,
                foot - TRAY_BAND_INSET, tray.argb());
        MachineArt.text(g, this.font, Component.translatable(tray.label()).getString(),
                x + MachineScreenLayout.TRAY_LABEL_X, y + MachineScreenLayout.TRAY_LABEL_Y, tray.inkArgb());
        for (int i = 0; i < tray.slots().size(); i++) {
            final int bin = x + MachineScreenLayout.TRAY_SLOTS_X + i * MachineScreenLayout.TRAY_PITCH;
            MachineArt.slot(g, bin, this.topPos + MachineScreenLayout.TRAY_SLOT_Y);
            if (i < tray.marks().size()) {
                MachineArt.textCentred(g, this.font, tray.marks().get(i), bin + MachineScreenLayout.SLOT / 2,
                        this.topPos + MachineScreenLayout.TRAY_SLOT_Y + MachineScreenLayout.SLOT + 1, tray.inkArgb());
            }
        }
    }

    /* What the tab, the plates, a tab's icon or an empty input slot tells the mouse over it. */
    private List<Component> tooltip(final MachineView view) {
        final double x = view.mouseX() - this.leftPos;
        final double y = view.mouseY() - this.topPos;
        final int tab = MachineScreenLayout.tabAt(x, y, this.faces.size());
        if (tab >= 0) {
            return List.of(GameText.component(tab < TABS ? TAB_NAMES.get(tab) : this.own.get(tab - TABS).name()));
        }
        if (this.hoveredSlot != null && lockable(this.hoveredSlot)) {
            final ItemStack locked = lockedTo(this.hoveredSlot);
            if (!locked.isEmpty()) {
                return List.of(GameText.component(MachineScreenTexts.LOCKED.with(GameText.of(locked.getHoverName()))));
            }
            return inletTip((ProcessingMachineBlockEntity) this.menu.machine());
        }
        if (!onFace(view)) {
            return List.of();
        }
        final double fx = view.faceMouseX();
        final double fy = view.faceMouseY();
        if (MachineScreenLayout.on(fx, fy, PLATE_X, NAME_PLATE_Y, NAME_PLATE_MOST, PLATE_HEIGHT)) {
            final Ownership owned = view.machine().ownership();
            return List.of(owned == null ? this.title : GameText.component(MachineScreenTexts.TIP_NAME.with(
                    GameText.of(this.title), Text.literal(ProtectionTab.ownerName(owned)))));
        }
        if (MachineScreenLayout.on(fx, fy, LAMP_X - 1, LAMP_Y - 1, LAMP_WORD_X - LAMP_X + 24,
                LAMP_PITCH * MachineChrome.lampCount())) {
            final Component status = GameText.component(MachineBlockEntity.STATUS_LINE.with(
                    view.machine().status().text()));
            // Under the status, why it waits, when the machine says.
            return view.machine().note().isEmpty() ? List.of(status)
                    : List.of(status, GameText.component(view.machine().note()));
        }
        return this.faces.get(shownTab()).tooltip(view, fx, fy);
    }

    /* Tab {@code index} on the window's top edge, with its icon. */
    private void tab(final GuiGraphics g, final int index, final boolean active) {
        if (index < TABS) {
            MachineChrome.tab(g, this.leftPos + MachineScreenLayout.tabX(index), this.topPos, index, active);
        } else {
            MachineChrome.tab(g, this.leftPos + MachineScreenLayout.tabX(index), this.topPos,
                    this.own.get(index - TABS).icon(), active);
        }
    }

    private MachineView view(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        return new MachineView(g, this.font, this.menu, this.leftPos, this.topPos, mouseX, mouseY,
                this.ticks + partialTick, this::sendButton);
    }

    /* The tab shown: the menu's, or the Main tab for one of the machine's own its mod's client gave no face. */
    private int shownTab() {
        return this.menu.tab() < this.faces.size() ? this.menu.tab() : MachineMenu.MAIN;
    }

    private boolean onFace(final MachineView view) {
        return MachineScreenLayout.on(view.faceMouseX(), view.faceMouseY(), 0, 0, FACE_WIDTH, FACE_HEIGHT);
    }

    /* Whether {@code slot} is an input slot of the Main tab, which Ctrl and a click locks. */
    private boolean lockable(final Slot slot) {
        return slot != null && this.menu.tab() == MachineMenu.MAIN && this.menu.faceSlots().contains(slot)
                && this.menu.machine() instanceof ProcessingMachineBlockEntity processing
                && processing.layout().isInput(slot.getContainerSlot());
    }

    /*
     * What an empty item inlet says: that it is one, the items the machine's recipes take, how the machine works them
     * where its face does not show it, and that Ctrl and a click lock it. A player read the slot under the cell's
     * BRINE as where its brine goes.
     */
    private List<Component> inletTip(final ProcessingMachineBlockEntity machine) {
        final List<Component> lines = new ArrayList<>();
        lines.add(GameText.component(MachineScreenTexts.INLET.text()));
        final List<ItemStack> takes = machine.recipeItems();
        if (!takes.isEmpty()) {
            lines.add(GameText.component(MachineScreenTexts.INLET_TAKES.text()).copy().withStyle(ChatFormatting.GRAY));
            // By name, so a player finds one where they look for it; as many as a tooltip holds.
            final List<String> names = new ArrayList<>();
            for (final ItemStack item : takes) {
                names.add(item.getHoverName().getString());
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            addWrapped(lines, String.join(", ", names.subList(0, Math.min(MOST_INLET_ITEMS, names.size())))
                    + (names.size() > MOST_INLET_ITEMS ? ", ..." : ""));
        }
        machine.inletNote().ifPresent(note -> addWrapped(lines, GameText.resolve(note)));
        addWrapped(lines, GameText.resolve(MachineScreenTexts.LOCK_HINT.text()));
        return lines;
    }

    /* {@code text} as grey lines no wider than a tooltip of the screen reads well. */
    private void addWrapped(final List<Component> lines, final String text) {
        for (final FormattedText line : this.font.getSplitter().splitLines(text, TIP_WIDTH, Style.EMPTY)) {
            lines.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
        }
    }

    private ItemStack lockedTo(final Slot slot) {
        return this.menu.machine() instanceof ProcessingMachineBlockEntity processing
                ? processing.locks().locked(slot.getContainerSlot()) : ItemStack.EMPTY;
    }
}
