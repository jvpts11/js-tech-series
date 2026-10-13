/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Base for the computer-assembly screens, holding the header rename field they share. A computer is renamed in its own assembly GUI (never via an anvil), so each of these screens carried the same EditBox plus the same keyboard plumbing to keep typing in the field from leaking to the screen (and the inventory key from closing the GUI). That plumbing lives here once; the screen only positions the field and supplies its rename action. The Mainframe, which has no rename field, does not extend this.
 */
public abstract class AbstractAssemblyScreen<T extends AbstractContainerMenu> extends AbstractComputerScreen<T> {

    @Nullable
    protected EditBox nameBox;
    /* What the empty field says, kept to be dressed again in the era's colour when the skin changes. */
    @Nullable
    private Component nameHint;

    protected AbstractAssemblyScreen(final T menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        /*
         * While the name field has focus, route typing to it and never let a key (e.g. the inventory
         * key 'E') reach the screen and close the GUI. ESC just unfocuses the field.
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

    /**
     * Installs the header rename field at {@code (x, y)} relative to this screen's top-left. Call from {@code init()} after {@code super.init()}. The responder runs on every edit (typically to send a rename packet).
     */
    protected void setupNameBox(final int x, final int y, final int width, final int maxLength,
                                final Component hint, final String initialValue,
                                final Consumer<String> responder) {
        nameBox = new EditBox(font, leftPos + x, topPos + y, width, 11, GameText.component(AssemblyTexts.NAME));
        nameBox.setBordered(false);
        nameBox.setTextShadow(false);
        nameBox.setMaxLength(maxLength);
        nameHint = hint;
        dressNameBox();
        nameBox.setValue(initialValue);
        nameBox.setResponder(responder);
        addRenderableWidget(nameBox);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // A machine whose era follows its board changes its skin when the board does, and the field goes with it.
        dressNameBox();
    }

    /**
     * Handles a click against the rename field: one on it focuses it and is taken, one anywhere else lets the field
     * go. Call it first in {@code mouseClicked}, and stop when it answers true.
     */
    protected boolean clickNameBox(final double mouseX, final double mouseY, final int button) {
        if (nameBox == null) {
            return false;
        }
        if (nameBox.isMouseOver(mouseX, mouseY)) {
            setFocused(nameBox);
            nameBox.setFocused(true);
            nameBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        nameBox.setFocused(false);
        return false;
    }

    /*
     * The field sits in a well of the era's own track colour, so the name is written in the era's text and the hint in
     * its dim text, as every other word on the screen is.
     */
    private void dressNameBox() {
        if (nameBox == null) {
            return;
        }
        nameBox.setTextColor(theme.text());
        if (nameHint != null) {
            nameBox.setHint(nameHint.copy().withColor(theme.dim()));
        }
    }
}
