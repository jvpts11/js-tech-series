/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.client.assembly.AssemblyPainter;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.gui.layout.MiningComputerLayout;
import dev.jstech.computers.menu.AbstractAssemblyComputerMenu;
import dev.jstech.computers.menu.MiningComputerMenu;
import dev.jstech.computers.teracoin.TeracoinHashrate;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Mining Computer's assembly screen (computers/docs/design/teracoin.md): its open frame with the risers along the
 * rail, the cards the player seated in the world standing on them, each riser's lamp lit under the card it carries,
 * and the mining board's row under them; on its front panel what it hashes, what it draws and how many cards it
 * carries, over the hint that its cards are seated in the world; its switches auto start and mine at start.
 */
public class MiningComputerScreen extends AbstractTowerAssemblyScreen<MiningComputerMenu> {

    /* How tall a card stands on its riser, and where a riser's lamp is under its seat. */
    private static final double CARD_HEIGHT = 34.0;
    private static final int SLOT = 16;
    private static final int LAMP_X = 7;
    private static final int LAMP_Y = 19;

    public MiningComputerScreen(final MiningComputerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Era caseEra() {
        return menu.rig().rigEra();
    }

    @Override
    protected AssemblyGeometry makeGeometry() {
        return MiningComputerLayout.geometry(caseEra());
    }

    @Override
    protected String machineTitle() {
        return GameText.resolve(AssemblyTexts.TITLE_RIG);
    }

    @Override
    protected TextKey nameHint() {
        return AssemblyTexts.NAME_THIS_RIG;
    }

    @Override
    protected String stateWord() {
        return menu.mining() ? GameText.resolve(AssemblyTexts.MINING) : super.stateWord();
    }

    @Override
    protected int stateColour() {
        return menu.mining() ? JsTechTheme.green() : super.stateColour();
    }

    @Override
    protected List<Readout> readouts() {
        int risers = 0;
        for (int i = 0; i < menu.riserCount(); i++) {
            risers += menu.hasRiser(i) ? 1 : 0;
        }
        return List.of(
                new Readout(GameText.resolve(AssemblyTexts.HASHRATE), TeracoinHashrate.format(menu.hashrate()),
                        menu.isRunning() ? JsTechTheme.accent() : JsTechTheme.text()),
                draw(),
                new Readout(GameText.resolve(AssemblyTexts.CARDS),
                        GameText.resolve(AssemblyTexts.CARDS_ON_RISERS.with(menu.seatedCards(), risers)),
                        JsTechTheme.text()));
    }

    @Override
    protected List<ServiceSwitch> serviceSwitches() {
        return List.of(
                new ServiceSwitch(GameText.resolve(AssemblyTexts.AUTO_START), menu.isAutoStart(),
                        AbstractAssemblyComputerMenu.BUTTON_AUTOSTART),
                new ServiceSwitch(GameText.resolve(AssemblyTexts.MINE_AT_START), menu.mineAtStart(),
                        MiningComputerMenu.BUTTON_MINE_AT_START));
    }

    /* The cards standing on their risers over the rail, each riser's lamp lit under the card it carries. */
    @Override
    protected void drawOnCase(final GuiGraphics g, final int x, final int y) {
        final AssemblyGeometry geo = geometry();
        for (final Seat seat : geo.seats(SeatKind.RISER)) {
            final ResourceLocation card = menu.cardOn(seat.index());
            if (card != null) {
                AssemblyPainter.drawStandingCard(g, x + seat.x() + SLOT / 2.0, y + geo.rail(), CARD_HEIGHT, card);
            }
            AssemblyPainter.drawLamp(g, x + seat.x() + LAMP_X, y + seat.y() + LAMP_Y, JsTechTheme.accent(),
                    card != null);
        }
        // The hint, wrapped to the front panel's width under the readouts.
        final List<FormattedText> lines = font.getSplitter().splitLines(
                GameText.resolve(AssemblyTexts.SEAT_IN_WORLD), (int) (AssemblyGeometry.RIGHT_W / JsTechTheme.small()),
                Style.EMPTY);
        for (int i = 0; i < lines.size() && i < geo.hintLines(); i++) {
            JsTechTheme.textS(g, font, lines.get(i).getString(), x + geo.rightX(), y + geo.hintY(i),
                    JsTechTheme.dim());
        }
    }
}
