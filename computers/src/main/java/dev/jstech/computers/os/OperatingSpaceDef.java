/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.api.ComputersRegisterEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * An operating space: what a network system puts on the glass instead of a bare prompt.
 *
 * <p>It is to MC-NET what a desktop environment is to a Linux. The system ships with one; uninstall it and
 * the machine is a terminal and nothing else; install another and that one draws. The decision of what a
 * monitor opens therefore belongs to the space that is installed rather than to what the system is capable
 * of, which is the same move a distribution made when it stopped bundling its desktop.
 *
 * <p>The difference from a desktop environment is deliberate. A desktop is a theme over one engine, because
 * every desktop here is windows and a panel. A space is not a theme: an add-on registering one supplies the
 * whole screen, so it can invent a way of working a network that nothing here imagined. What it does not
 * supply is the slots. Items are the server's to move and every space needs the same ones, so the machine
 * keeps the menu and the space keeps the drawing and the keyboard, which is how the desktop already hosts
 * the Network Interactor.
 *
 * <p>Registered through {@link ComputersRegisterEvent}; the screen that draws it is registered separately on
 * the client, since nothing on a server has a screen to give.
 *
 * @param id          unique registry key (e.g. {@code jsc:interactor})
 * @param displayName the human name, which is what the machine calls it when listing or uninstalling it
 * @param house       who makes it, and so who is credited for it
 */
public record OperatingSpaceDef(
        ResourceLocation id,
        String displayName,
        SoftwareHouse house
) {

    public OperatingSpaceDef {
        if (displayName == null || displayName.isBlank()) {
            displayName = id.getPath();
        }
        if (house == null) {
            house = SoftwareHouse.NOUVELL;
        }
    }
}
