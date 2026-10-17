/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.jstechseries.hazard.DeviceState;
import dev.jstech.core.jstechseries.hazard.DeviceStatePayload;
import dev.jstech.core.jstechseries.hazard.HazardDevice;
import dev.jstech.core.jstechseries.hazard.HazardDeviceItem;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Geiger counter or the dosimeter held up, full size and live: its buttons pressed with the mouse (the counter's
 * range and sound, the dosimeter's light), each press kept on the device. The game goes on behind it.
 */
public final class HeldDeviceScreen extends Screen {

    private final HazardDevice device;
    private final InteractionHand hand;
    private int left;
    private int top;

    /** Held up, the devices are drawn at twice their HUD size. */
    public static final int UNIT = 2;
    /* How far right of a button its moulded word reaches, so the word is pressed with it. */
    private static final int WORD = 26;

    public HeldDeviceScreen(final HazardDevice device, final InteractionHand hand) {
        super((device == HazardDevice.GEIGER_COUNTER ? HazardDevices.GEIGER_COUNTER : HazardDevices.POCKET_DOSIMETER)
                .get().getDescription());
        this.device = device;
        this.hand = hand;
    }

    /** The device held. */
    public HazardDevice device() {
        return this.device;
    }

    /** Where the device's case starts on the screen, its left then its top. */
    public int left() {
        return this.left;
    }

    public int top() {
        return this.top;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final ItemStack held = held();
        if (held.isEmpty()) {
            return;
        }
        if (this.device == HazardDevice.GEIGER_COUNTER) {
            DeviceHud.drawGeiger(g, this.font, this.left, this.top, UNIT, HazardDevices.state(held));
        } else {
            DeviceHud.drawDosimeter(g, this.font, this.left, this.top, UNIT, HazardDevices.state(held));
        }
    }

    @Override
    public void tick() {
        if (held().isEmpty()) {
            this.onClose();
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final ItemStack held = held();
        if (button == 0 && !held.isEmpty()) {
            final DeviceState state = HazardDevices.state(held);
            final DeviceState pressed = this.device == HazardDevice.GEIGER_COUNTER
                    ? on(mouseX, mouseY, DeviceArt.RANGE_X, DeviceArt.GEIGER_BUTTON_Y) ? state.nextRange()
                    : on(mouseX, mouseY, DeviceArt.SOUND_X, DeviceArt.GEIGER_BUTTON_Y) ? state.toggleSound() : null
                    : on(mouseX, mouseY, DeviceArt.LIGHT_X, DeviceArt.LIGHT_Y) ? state.toggleLight() : null;
            if (pressed != null) {
                press(held, pressed);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Presses the button that sets the device to {@code pressed}: kept on this game at once and on the server. */
    public void press(final ItemStack held, final DeviceState pressed) {
        held.set(HazardDevices.STATE.get(), pressed);
        PacketDistributor.sendToServer(new DeviceStatePayload(this.hand == InteractionHand.MAIN_HAND, pressed));
    }

    @Override
    protected void init() {
        final int width = (this.device == HazardDevice.GEIGER_COUNTER ? DeviceArt.GEIGER_WIDTH
                : DeviceArt.DOSIMETER_WIDTH) * UNIT;
        final int height = (this.device == HazardDevice.GEIGER_COUNTER ? DeviceArt.GEIGER_HEIGHT
                : DeviceArt.DOSIMETER_HEIGHT) * UNIT;
        this.left = (this.width - width) / 2;
        this.top = (this.height - height) / 2;
    }

    /* The device still in the hand it was held up from; empty once it is gone from there. */
    private ItemStack held() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = this.minecraft.player.getItemInHand(this.hand);
        return stack.getItem() instanceof HazardDeviceItem item && item.device() == this.device ? stack
                : ItemStack.EMPTY;
    }

    /* Whether the pointer is on the button whose top left is (x, y) at a unit of 1, or on its word. */
    private boolean on(final double mouseX, final double mouseY, final int x, final int y) {
        final int bx = this.left + x * UNIT;
        final int by = this.top + y * UNIT;
        return mouseX >= bx && mouseX < bx + WORD * UNIT && mouseY >= by - UNIT
                && mouseY < by + (DeviceArt.BUTTON_HEIGHT + 2) * UNIT;
    }
}
