/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Base for the computer-assembly screens, holding the header rename field they share. A computer is renamed in its own assembly GUI (never via an anvil), so each of these screens carried the same EditBox plus the same keyboard plumbing to keep typing in the field from leaking to the screen (and the inventory key from closing the GUI). That plumbing lives here once; the screen only positions the field and supplies its rename action. The Mainframe, which has no rename field, does not extend this.
 */
@PaletteHolder
public abstract class AbstractAssemblyScreen<T extends AbstractContainerMenu> extends AbstractComputerScreen<T> {

    @Nullable
    protected EditBox nameBox;

    /** The rename field's well on the header strip, its lower edge and the name typed into it. */
    private static final Palette<NameWell> NAME_WELL = Palettes.declare(JsComputers.MODID, "screen/assembly_name",
            new NameWell(0xFF11161D, 0xFF24323C, 0xFFFFFFFF));

    protected AbstractAssemblyScreen(final T menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * Installs the header rename field at {@code (x, y)} relative to this screen's top-left. Call from {@code init()} after {@code super.init()}. The responder runs on every edit (typically to send a rename packet).
     */
    protected void setupNameBox(final int x, final int y, final int width, final int maxLength,
                                final Component hint, final String initialValue,
                                final Consumer<String> responder) {
        nameBox = new EditBox(font, leftPos + x, topPos + y, width, 11, GameText.component(AssemblyTexts.NAME));
        nameBox.setBordered(false);
        nameBox.setMaxLength(maxLength);
        /*
         * The rename field sits on a dark header strip in every era, so its text is the well's own colour, not
         * the era's: the era's text goes dark on the Legacy strip and the name becomes unreadable.
         */
        nameBox.setTextColor(NAME_WELL.get().name());
        nameBox.setHint(hint);
        nameBox.setValue(initialValue);
        nameBox.setResponder(responder);
        addRenderableWidget(nameBox);
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

    /** The well the rename field is drawn over, from {@code left} to {@code right} on the header row at {@code top}. */
    protected static void nameWell(final GuiGraphics g, final int left, final int top, final int right) {
        final NameWell well = NAME_WELL.get();
        g.fill(left, top, right, top + 12, well.ground());
        g.fill(left, top + 11, right, top + 12, well.edge());
    }

    private record NameWell(int ground, int edge, int name) {
    }
}
