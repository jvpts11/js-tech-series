/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.PersonalComputerLayout;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Personal Computer's assembly screen: its era's tower case, and on its front panel what it moves a tick, its RAM
 * buffer and whether it is on a network, with how many servers that network has.
 */
public class PersonalComputerScreen extends AbstractTowerAssemblyScreen<PersonalComputerMenu> {

    public PersonalComputerScreen(final PersonalComputerMenu menu, final Inventory inventory,
                                  final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected AssemblyGeometry makeGeometry() {
        return PersonalComputerLayout.geometry(caseEra());
    }

    @Override
    protected String machineTitle() {
        return GameText.resolve(AssemblyTexts.TITLE_PC);
    }

    @Override
    protected TextKey nameHint() {
        return AssemblyTexts.NAME_THIS_PC;
    }

    @Override
    protected List<Readout> readouts() {
        final int servers = menu.networkServerCount();
        final List<Component> network = menu.isOnNetwork()
                ? List.of(GameText.component((servers == 1 ? AssemblyTexts.ONE_SERVER : AssemblyTexts.SERVERS)
                        .with(servers)))
                : List.of();
        return List.of(capacity(List.of()),
                new Readout(GameText.resolve(AssemblyTexts.RAM_BUFFER),
                        JsTechTheme.fmt(menu.ramBuffer()) + " " + GameText.resolve(AssemblyTexts.ITEMS),
                        JsTechTheme.text()),
                network(network));
    }
}
