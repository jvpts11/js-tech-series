/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.client.AbstractComputerScreen;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.computers.operation.payload.BusStatePayload;
import dev.jstech.computers.operation.payload.SetBusNamePayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The window of a bus, in its era's skin: a header with the bus's kind and the lamp of its link, and three tabs.
 * Configure holds the bus's name, the rows of what its era can be set to and the player's inventory; Activity, what it
 * did lately; Software, the same settings as software writes them. Each tab draws itself; this screen draws the frame,
 * switches the tabs, and keeps the window's top where it is when a tab is shorter.
 */
public abstract class AbstractBusScreen<T extends AbstractBusMenu> extends AbstractComputerScreen<T> {

    private final BusConfigureView configure;
    private final BusActivityView activity;
    private final BusSoftwareView software;
    private EditBox nameBox;
    private String nameValue;
    private int tab = BusLayout.TAB_CONFIGURE;

    private static final TextKey[] TAB_WORDS = {BusTexts.TAB_CONFIGURE, BusTexts.TAB_ACTIVITY, BusTexts.TAB_SOFTWARE};

    protected AbstractBusScreen(final T menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = BusLayout.WIDTH;
        this.imageHeight = configureHeight();
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.configure = new BusConfigureView(menu, Minecraft.getInstance().font, PacketDistributor::sendToServer);
        this.activity = new BusActivityView(menu, Minecraft.getInstance().font);
        this.software = new BusSoftwareView(menu, Minecraft.getInstance().font);
    }

    /** The window caption, e.g. "EXPORT BUS". */
    protected abstract TextKey windowTitle();

    /** The tab shown: Configure, Activity or Software. */
    public int tab() {
        return tab;
    }

    /** Shows {@code shown}, as a click on its tab does. */
    public void showTab(final int shown) {
        tab = shown;
        menu.showInventory(shown == BusLayout.TAB_CONFIGURE);
        if (nameBox != null) {
            nameBox.setVisible(shown == BusLayout.TAB_CONFIGURE);
            nameBox.setFocused(false);
        }
        if (shown != BusLayout.TAB_CONFIGURE) {
            configure.hideFields();
        }
        imageHeight = heightOf(shown);
        place();
    }

    @Override
    protected void init() {
        // Centred for Configure, the tallest tab: the other tabs keep its top and end higher.
        imageHeight = configureHeight();
        super.init();
        nameBox = new EditBox(font, leftPos + BusLayout.NAME_X + 3, topPos + BusLayout.NAME_Y + 2,
                BusLayout.NAME_W - 6, BusLayout.NAME_H - 3, GameText.component(BusTexts.NAME_FIELD));
        nameBox.setBordered(false);
        nameBox.setTextShadow(false);
        nameBox.setMaxLength(AbstractBusPart.MAX_NAME_LENGTH);
        nameBox.setTextColor(JsTechTheme.text());
        // Set the value before the responder so restoring it (open, or a window resize) sends no packet.
        nameBox.setValue(nameValue != null ? nameValue : menu.busName());
        nameBox.setResponder(this::onNameChanged);
        addRenderableWidget(nameBox);
        configure.widgets().forEach(this::addRenderableWidget);
        showTab(tab);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // The state arrives after the screen opens, and the Configure rows it brings set how tall the window is.
        imageHeight = heightOf(tab);
        place();
    }

    @Override
    @Nullable
    protected HardwareEra screenEra() {
        final BusStatePayload state = menu.state();
        if (state != null) {
            return state.skin();
        }
        return menu.crafting() ? HardwareEra.STANDARD : menu.era();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + BusLayout.HEADER_X, y + BusLayout.HEADER_Y, BusLayout.HEADER_W);
        // The early-PC skin's windows have a title bar in the system blue, with the title in cream on it.
        final boolean titleBar = JsTechTheme.active().style().doubleBevel();
        if (titleBar) {
            Grounds.fill(g, x + BusLayout.HEADER_X, y + BusLayout.HEADER_Y, x + BusLayout.HEADER_X
                    + BusLayout.HEADER_W, y + BusLayout.HEADER_Y + 16, JsTechTheme.tabOn());
        }
        final int titleColour = titleBar ? JsTechTheme.tabLabelOn() : JsTechTheme.text();
        final String title = GameText.resolve(windowTitle());
        final int room = BusLayout.LAMP_X - BusLayout.TITLE_X - 4;
        if (font.width(title) <= room) {
            Draw.text(g, font, title, x + BusLayout.TITLE_X, y + BusLayout.TITLE_Y, titleColour);
        } else {
            // A longer title, as some languages give, takes the small letters rather than run under the lamp.
            BusDraw.fitted(g, font, title, x + BusLayout.TITLE_X, y + BusLayout.TITLE_Y + 1, titleColour, room);
        }
        BusDraw.lamp(g, x + BusLayout.LAMP_X, y + BusLayout.LAMP_Y, BusLayout.LAMP_SIZE, linked());
        drawTabs(g, x, y, mouseX, mouseY);
        switch (tab) {
            case BusLayout.TAB_ACTIVITY -> activity.render(g, x, y);
            case BusLayout.TAB_SOFTWARE -> software.render(g, x, y);
            default -> drawConfigure(g, x, y, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn with the window in renderBg; the container's own two labels are not used.
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (BusDraw.inside(mouseX, mouseY, leftPos + BusLayout.LAMP_X, topPos + BusLayout.LAMP_Y,
                BusLayout.LAMP_SIZE, BusLayout.LAMP_SIZE)) {
            g.renderTooltip(font, GameText.component(linked() ? BusTexts.LINKED : BusTexts.OFFLINE), mouseX,
                    mouseY);
        } else if (tab == BusLayout.TAB_CONFIGURE) {
            final List<Component> lines = configure.tooltip(mouseX, mouseY, leftPos, topPos);
            if (!lines.isEmpty()) {
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (overField(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            for (int i = 0; i < BusLayout.TABS; i++) {
                if (BusDraw.inside(mouseX, mouseY, leftPos + BusLayout.tabX(i), topPos + BusLayout.TAB_Y,
                        BusLayout.tabW(i), BusLayout.TAB_H)) {
                    showTab(i);
                    return true;
                }
            }
            final boolean onBar = switch (tab) {
                case BusLayout.TAB_ACTIVITY -> activity.pressBar(mouseX, mouseY);
                case BusLayout.TAB_SOFTWARE -> software.pressBar(mouseX, mouseY);
                default -> configure.pressBar(mouseX, mouseY);
            };
            if (onBar) {
                return true;
            }
            if (tab == BusLayout.TAB_CONFIGURE && configure.click(mouseX, mouseY, leftPos, topPos)) {
                setFocused(null);
                nameBox.setFocused(false);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** A drag with a scrollbar held moves its list; any other drag is the container's. */
    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        final boolean onBar = button == 0 && switch (tab) {
            case BusLayout.TAB_ACTIVITY -> activity.dragBar(mouseY);
            case BusLayout.TAB_SOFTWARE -> software.dragBar(mouseY);
            default -> configure.dragBar(mouseY);
        };
        return onBar || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        configure.releaseBar();
        activity.releaseBar();
        software.releaseBar();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final boolean taken = switch (tab) {
            case BusLayout.TAB_ACTIVITY -> activity.scrolled(mouseX, mouseY, scrollY, leftPos, topPos);
            case BusLayout.TAB_SOFTWARE -> software.scrolled(mouseX, mouseY, scrollY, leftPos, topPos);
            default -> configure.scrolled(mouseX, mouseY, scrollY, leftPos, topPos);
        };
        return taken || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        /*
         * While a field has focus, route keys to it and swallow the inventory key so 'e' types a character instead
         * of closing the window; escape just leaves the field.
         */
        if (nameBox != null && nameBox.isFocused()) {
            if (key == InputConstants.KEY_ESCAPE) {
                nameBox.setFocused(false);
                setFocused(null);
                return true;
            }
            nameBox.keyPressed(key, scan, mods);
            return true;
        }
        if (tab == BusLayout.TAB_CONFIGURE && configure.keyPressed(key)) {
            setFocused(null);
            return true;
        }
        if (tab == BusLayout.TAB_CONFIGURE && configure.typing() && getFocused() != null) {
            getFocused().keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void drawConfigure(final GuiGraphics g, final int x, final int y, final int mouseX, final int mouseY) {
        BusDraw.small(g, font, GameText.resolve(BusTexts.NAME), x + BusLayout.LABEL_X, y + BusLayout.NAME_Y + 2,
                JsTechTheme.dim());
        BusDraw.well(g, x + BusLayout.NAME_X, y + BusLayout.NAME_Y, BusLayout.NAME_W, BusLayout.NAME_H);
        configure.render(g, x, y, mouseX, mouseY, hasShiftDown());
        BusDraw.small(g, font, GameText.resolve(BusTexts.INVENTORY), x + BusLayout.INV_X,
                y + BusLayout.inventoryLabelY(abilities(), menu.window()), JsTechTheme.dim());
        /*
         * Every active slot the menu placed. Drawing from the menu's own slots, rather than re-deriving the grid here,
         * means a change in where the inventory sits can never leave a frame drifted off its slot.
         */
        for (final Slot slot : menu.slots) {
            if (slot.isActive()) {
                JsTechTheme.slot(g, x + slot.x, y + slot.y);
            }
        }
    }

    private void drawTabs(final GuiGraphics g, final int x, final int y, final int mouseX, final int mouseY) {
        for (int i = 0; i < BusLayout.TABS; i++) {
            final int tx = x + BusLayout.tabX(i);
            final int ty = y + BusLayout.TAB_Y;
            final int tw = BusLayout.tabW(i);
            final boolean chosen = i == tab;
            if (chosen) {
                JsTechTheme.selectedTab(g, tx, ty, tw, BusLayout.TAB_H);
                Draw.outline(g, tx, ty, tw, BusLayout.TAB_H, JsTechTheme.accent());
            } else {
                JsTechTheme.button(g, tx, ty, tw, BusLayout.TAB_H, BusDraw.inside(mouseX, mouseY, tx, ty, tw,
                        BusLayout.TAB_H));
                Draw.outline(g, tx, ty, tw, BusLayout.TAB_H, JsTechTheme.line());
            }
            final String word = BusDraw.clip(font, GameText.resolve(TAB_WORDS[i]), tw - 4);
            BusDraw.smallCentered(g, font, word, tx + tw / 2, ty + 2,
                    chosen ? JsTechTheme.tabLabelOn() : JsTechTheme.dim());
        }
    }

    private void onNameChanged(final String value) {
        nameValue = value;
        PacketDistributor.sendToServer(new SetBusNamePayload(menu.cablePos(), menu.face().get3DDataValue(), value));
    }

    /* Whether the point is over a text field that is shown: a click there is the field's. */
    private boolean overField(final double mouseX, final double mouseY) {
        if (nameBox != null && nameBox.isVisible() && nameBox.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        return configure.widgets().stream().anyMatch(box -> box.isVisible() && box.isMouseOver(mouseX, mouseY));
    }

    private int heightOf(final int shown) {
        return switch (shown) {
            case BusLayout.TAB_ACTIVITY -> BusLayout.ACTIVITY_HEIGHT;
            case BusLayout.TAB_SOFTWARE -> software.windowHeight();
            default -> configureHeight();
        };
    }

    /* Centred for Configure, the tallest tab: the other tabs keep its top and end higher, or rise to stay on screen. */
    private void place() {
        final int moved = placeVertically(configureHeight());
        if (nameBox != null && moved != 0) {
            nameBox.setY(nameBox.getY() + moved);
        }
    }

    private int configureHeight() {
        return BusLayout.configureHeight(abilities(), menu.window());
    }

    private BusAbilities abilities() {
        return menu.abilities();
    }

    private boolean linked() {
        return menu.state() != null && menu.state().linked();
    }
}
