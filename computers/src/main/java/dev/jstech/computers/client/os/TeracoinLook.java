/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.teracoin.WalletStatePayload;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What Blockwright's two Teracoin windows add to the look of a tabbed window (computers/docs/design/teracoin.md): the
 * chain's line at their foot, the Crypto Wallet's and the Mining Panel's alike.
 */
final class TeracoinLook {

    private TeracoinLook() {
    }

    /**
     * The status bar with the chain's height, difficulty and reward, or {@code left} in its place while it says
     * something; the blocks to the next halving at its right.
     */
    static void statusBar(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                          final int width, final WalletStatePayload.Chain chain, final String left) {
        final String line = left.isEmpty() ? GameText.resolve(CryptoWalletTexts.CHAIN_LINE.with(
                TabbedWindowLook.grouped(chain.height()), TabbedWindowLook.grouped(chain.difficulty()),
                Trc.format(chain.reward(), chain.reward() % Trc.UNITS == 0L ? 0 : Trc.DECIMALS))) : left;
        final String right = chain.toHalving() < 0 ? GameText.resolve(CryptoWalletTexts.NO_HALVING)
                : GameText.resolve(CryptoWalletTexts.HALVING_IN.with(TabbedWindowLook.grouped(chain.toHalving())));
        TabbedWindowLook.statusBar(g, font, skin, x, y, width, line, right);
    }
}
