/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.block.WanGatewayStructure;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.WanGatewayLayout;
import dev.jstech.computers.menu.WanGatewayMenu;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The WAN Gateway Computer's assembly screen: the Mainframe's cabinet, its WAN Interface Cards in the I/O cages among
 * the other cards, and on its front panel what it moves a tick, how many of its lines are up, its network, its draw and
 * its temperature; its service panel has auto start and, under it, a lamp for each of its four lines, green for a line
 * up, red for one down, dark for a port with no card.
 */
public class WanGatewayScreen extends AbstractCabinetScreen<WanGatewayMenu> {

    public WanGatewayScreen(final WanGatewayMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected AssemblyGeometry makeGeometry() {
        return WanGatewayLayout.geometry();
    }

    @Override
    protected String machineTitle() {
        return GameText.resolve(AssemblyTexts.TITLE_WAN_GATEWAY);
    }

    @Override
    protected String stateWord() {
        if (!menu.buildValid()) {
            return GameText.resolve(AssemblyTexts.OFFLINE);
        }
        if (menu.isRunning()) {
            return GameText.resolve(menu.thermal().throttled() ? AssemblyTexts.THROTTLED : AssemblyTexts.ONLINE);
        }
        return GameText.resolve(AssemblyTexts.READY);
    }

    @Override
    protected int stateColour() {
        if (!menu.buildValid()) {
            return JsTechTheme.red();
        }
        return menu.isRunning() && !menu.thermal().throttled() ? JsTechTheme.green() : JsTechTheme.amber();
    }

    @Override
    protected List<Readout> readouts() {
        final int cards = menu.cardedPorts();
        final int up = menu.portsUp();
        final String lines = cards == 0 ? "--" : GameText.resolve(AssemblyTexts.LINES_UP.with(up, cards));
        final int linesColour = cards == 0 ? JsTechTheme.dim()
                : up == cards ? JsTechTheme.green() : JsTechTheme.amber();
        return List.of(
                new Readout(GameText.resolve(AssemblyTexts.CAPACITY),
                        JsTechTheme.fmt(menu.capacity()) + " " + GameText.resolve(AssemblyTexts.ITEMS_PER_TICK),
                        menu.isRunning() ? JsTechTheme.accent() : JsTechTheme.text()),
                new Readout(GameText.resolve(AssemblyTexts.LINES), lines, linesColour),
                new Readout(GameText.resolve(AssemblyTexts.NETWORK),
                        menu.networked() ? GameText.resolve(AssemblyTexts.LINKED) : "--",
                        menu.networked() ? JsTechTheme.accent() : JsTechTheme.dim()),
                drawReadout(menu.powerShown(), menu.psu()), tempReadout(menu.thermal()));
    }

    @Override
    protected List<ServiceSwitch> serviceSwitches() {
        return List.of(autoStartSwitch());
    }

    /* A lamp for each port's line: up in green, down in red while the gateway runs, dark with no card in the port. */
    @Override
    protected List<LampRow> lampRows() {
        final List<RowLamp> lamps = new ArrayList<>(WanGatewayStructure.PORTS);
        for (int port = 1; port <= WanGatewayStructure.PORTS; port++) {
            lamps.add(switch (menu.line(port)) {
                case WanGatewayBlockEntity.LINE_UP -> new RowLamp(JsTechTheme.green(), true);
                case WanGatewayBlockEntity.LINE_DOWN -> new RowLamp(JsTechTheme.red(), menu.isRunning());
                default -> new RowLamp(JsTechTheme.dim(), false);
            });
        }
        return List.of(new LampRow(GameText.resolve(AssemblyTexts.LINES), lamps));
    }

    @Override
    protected boolean[] lampsLit() {
        return new boolean[] {menu.isRunning(), menu.diskBusy(), menu.networked()};
    }
}
