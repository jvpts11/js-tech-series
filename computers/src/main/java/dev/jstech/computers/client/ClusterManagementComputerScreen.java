/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.ClusterManagementComputerLayout;
import dev.jstech.computers.menu.ClusterManagementComputerMenu;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Cluster Management Computer's assembly screen: its era's tower case, and on its front panel what it moves a tick
 * (its RAM buffer in the tooltip), whether it is on a network, the clusters its interface card reaches (each kind in the
 * tooltip) and whether the Cluster Manager is installed.
 */
public class ClusterManagementComputerScreen extends AbstractTowerAssemblyScreen<ClusterManagementComputerMenu> {

    public ClusterManagementComputerScreen(final ClusterManagementComputerMenu menu, final Inventory inventory,
                                           final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected AssemblyGeometry makeGeometry() {
        return ClusterManagementComputerLayout.geometry(caseEra());
    }

    @Override
    protected String machineTitle() {
        return GameText.resolve(AssemblyTexts.TITLE_CLUSTER_MANAGEMENT);
    }

    @Override
    protected TextKey nameHint() {
        return AssemblyTexts.NAME_THIS_COMPUTER;
    }

    @Override
    protected List<Readout> readouts() {
        final Readout clusters;
        if (!menu.hasCard()) {
            clusters = new Readout(GameText.resolve(AssemblyTexts.CLUSTERS_SHORT),
                    GameText.resolve(AssemblyTexts.NO_CARD), JsTechTheme.amber(),
                    List.of(GameText.component(AssemblyTexts.NO_INTERFACE_CARD)));
        } else {
            final int reach = menu.supercomputers() + menu.datacenters();
            clusters = new Readout(GameText.resolve(AssemblyTexts.CLUSTERS_SHORT),
                    reach == 0 ? GameText.resolve(AssemblyTexts.NONE)
                            : GameText.resolve(AssemblyTexts.IN_REACH.with(reach)),
                    JsTechTheme.text(), List.of(GameText.component(AssemblyTexts.CLUSTERS.with(menu.supercomputers(),
                            menu.datacenters(), menu.lanes()))));
        }
        final boolean manager = menu.managerInstalled();
        return List.of(
                capacity(List.of(GameText.component(AssemblyTexts.RAM_BUFFER_LINE.with(
                        JsTechTheme.fmt(menu.ramBuffer()))))),
                network(List.of()),
                clusters,
                new Readout(GameText.resolve(AssemblyTexts.MANAGEMENT),
                        GameText.resolve(manager ? AssemblyTexts.INSTALLED : AssemblyTexts.NOT_INSTALLED),
                        manager ? JsTechTheme.accent() : JsTechTheme.amber()));
    }
}
