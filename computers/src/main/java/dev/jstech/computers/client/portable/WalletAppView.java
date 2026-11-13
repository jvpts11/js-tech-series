/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableWalletPayload;
import dev.jstech.computers.operation.payload.teracoin.WalletStatePayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.teracoin.BlockSquare;
import dev.jstech.computers.teracoin.LedgerEntry;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * A portable's Crypto Wallet (computers/docs/design/wireless.md): the device's one wallet, kept on it, its balance in
 * a card with its address under it, Send and Receive, and its history, a send waiting for the next block shown as
 * such. Sending takes an address and an amount; receiving shows the address whole and as its square of blocks.
 */
@PaletteHolder
final class WalletAppView implements IMobileAppView {

    private static final StableIds<LedgerEntry.Kind> KINDS = StableIds.of(LedgerEntry.Kind.class);
    private static final int ASK_EVERY = 40;
    /** Its colours, as the K0.11 apps page drew them: Blockwright's gold, a send waiting, the square, qOS's buttons. */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/crypto_wallet",
            new Colours(0xFFF0B23A, 0xFFFF9F0A, 0xFF000000, 0xFFFFFFFF, 0xFF2C2C2E));
    private static final int PLACES = 4;
    private static final float ROW = 21.0F;
    /* The pitch of the lines of a note broken over several. */
    private static final float LINE = 9.0F;

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.of(device, Action.WALLET);
    }

    @Override
    public void paint(final AppPage page) {
        final MobileAppState state = page.state();
        final float top = page.kit().bar(page.c(), MobileApp.CRYPTO_WALLET, page.top(),
                MobileParts.name(MobileApp.CRYPTO_WALLET), null, state.walletPage() != MobileAppState.WALLET_MAIN);
        final PortableWalletPayload wallet = PortableApps.wallet(page.device());
        if (wallet == null) {
            return;
        }
        if (!wallet.connected()) {
            page.kit().note(page.c(), top + 20, say(WalletAppTexts.NOT_CONNECTED), IAppKit.Tone.DIM);
            return;
        }
        if (wallet.address().isEmpty()) {
            first(page, top);
            return;
        }
        switch (state.walletPage()) {
            case MobileAppState.WALLET_SEND -> send(page, wallet, top);
            case MobileAppState.WALLET_RECEIVE -> receive(page, wallet, top);
            default -> main(page, wallet, top);
        }
    }

    /* No wallet yet: what one is, the warning whole on as many lines as it takes, and the button that makes it. */
    private static void first(final AppPage page, final float top) {
        final MobileCanvas c = page.c();
        page.kit().note(c, top + 16, say(WalletAppTexts.NO_WALLET), IAppKit.Tone.TEXT);
        float y = top + 28;
        for (final String line : c.wrap(say(WalletAppTexts.KEPT_HERE), c.width() - 8, MobileCanvas.SMALL)) {
            page.kit().note(c, y, line, IAppKit.Tone.DIM);
            y += LINE;
        }
        page.kit().button(c, 6, y + 7, c.width() - 12, say(WalletAppTexts.MAKE), true,
                MobileTap.run(() -> page.send(PortableActionPayload.of(page.device(), Action.WALLET_CREATE))));
    }

    /* The balance in its card, the address under it, Send and Receive, then the history. */
    private static void main(final AppPage page, final PortableWalletPayload wallet, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float w = c.width();
        kit.card(c, 6, top + 2, w - 12, 56);
        c.text(say(WalletAppTexts.BALANCE), 12, top + 8, kit.ink(IAppKit.Tone.DIM), MobileCanvas.SMALL);
        final String amount = Trc.format(wallet.balance(), PLACES);
        final String unit = say(WalletAppTexts.TRC);
        // A balance too long for the card at its big size, past a thousand coins, comes at the normal one.
        final boolean fits = c.width(amount, MobileCanvas.BIG) + 3 + c.width(unit, MobileCanvas.SMALL) <= w - 24;
        final float amountW = c.text(amount, 12, fits ? top + 18 : top + 24, kit.ink(IAppKit.Tone.TEXT),
                fits ? MobileCanvas.BIG : MobileCanvas.NORMAL);
        c.text(unit, 12 + amountW + 3, top + 26, COLOURS.get().gold(), MobileCanvas.SMALL);
        c.text(TeracoinAddress.shortForm(wallet.address()), 12, top + 44, kit.ink(IAppKit.Tone.DIM),
                MobileCanvas.SMALL);
        final float half = w / 2 - 3;
        button(page, 6, top + 64, half - 6, say(WalletAppTexts.SEND), true,
                MobileTap.run(() -> page.state().walletPage(MobileAppState.WALLET_SEND)));
        button(page, 6 + half, top + 64, half - 6, say(WalletAppTexts.RECEIVE), false,
                MobileTap.run(() -> page.state().walletPage(MobileAppState.WALLET_RECEIVE)));
        float y = top + 86;
        if (wallet.note() != WalletStatePayload.NOTE_NONE && wallet.note() != WalletStatePayload.NOTE_SENT
                && wallet.note() != WalletStatePayload.NOTE_CREATED) {
            kit.note(c, y, say(noteOf(wallet.note())), IAppKit.Tone.WARN);
            y += 10;
        }
        c.text(say(WalletAppTexts.HISTORY), 8, y, kit.ink(IAppKit.Tone.DIM), MobileCanvas.SMALL);
        y += 10;
        if (wallet.moves().isEmpty()) {
            c.text(say(WalletAppTexts.NOTHING_YET), 8, y, kit.ink(IAppKit.Tone.DIM), MobileCanvas.SMALL);
            return;
        }
        // As many moves as the page holds, from where the wheel has scrolled the history to.
        final List<WalletStatePayload.Move> moves = wallet.moves();
        final int rows = Math.max(1, (int) ((page.bottom() - y) / ROW));
        final int from = Math.min(page.frame().scroll(), Math.max(0, moves.size() - rows));
        for (int n = from; n < moves.size() && n < from + rows; n++) {
            final WalletStatePayload.Move move = moves.get(n);
            c.text(who(move), 8, y, kit.ink(IAppKit.Tone.TEXT), MobileCanvas.SMALL);
            c.text(when(move), 8, y + 9, kit.ink(IAppKit.Tone.DIM), MobileCanvas.SMALL);
            final int colour = move.waiting() && move.amount() < 0L ? COLOURS.get().pending()
                    : move.amount() > 0L ? kit.ink(IAppKit.Tone.GOOD) : kit.ink(IAppKit.Tone.TEXT);
            c.text(Trc.signed(move.amount(), PLACES), w - 8, y + 3, colour, MobileCanvas.SMALL,
                    MobileCanvas.Align.RIGHT, false);
            y += ROW;
            c.rect(8, y - 3, w - 16, 1, kit.ink(IAppKit.Tone.LINE));
        }
    }

    /* Sending: the address and the amount, what the wallet holds, and Send. */
    private static void send(final AppPage page, final PortableWalletPayload wallet, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final MobileAppState state = page.state();
        final float w = c.width();
        float y = kit.section(c, top + 2, say(WalletAppTexts.PAY_TO));
        y = kit.field(c, 6, y, w - 12, state.walletTo(), say(WalletAppTexts.ADDRESS_HINT),
                state.typing() == MobileAppState.Field.WALLET_TO,
                MobileTap.run(() -> state.typeInto(MobileAppState.Field.WALLET_TO)));
        y = kit.section(c, y + 4, say(WalletAppTexts.AMOUNT));
        y = kit.field(c, 6, y, w - 12, state.walletAmount(), "0.0000",
                state.typing() == MobileAppState.Field.WALLET_AMOUNT,
                MobileTap.run(() -> state.typeInto(MobileAppState.Field.WALLET_AMOUNT)));
        c.text(say(WalletAppTexts.AVAILABLE.with(Trc.format(wallet.balance(), PLACES))), 8, y + 3,
                kit.ink(IAppKit.Tone.DIM), MobileCanvas.SMALL);
        c.text(say(WalletAppTexts.NO_FEE), 8, y + 12, kit.ink(IAppKit.Tone.DIM), MobileCanvas.SMALL);
        button(page, 6, y + 24, w - 12, say(WalletAppTexts.SEND), true, MobileTap.run(() -> {
            final long amount = Trc.parse(state.walletAmount());
            page.send(new PortableActionPayload(page.device(), Action.WALLET_SEND, state.walletTo().trim(), "",
                    Optional.empty(), Math.max(0L, amount)));
            state.walletSent();
        }));
    }

    /* Receiving: the address whole, its square of blocks, and Copy. */
    private static void receive(final AppPage page, final PortableWalletPayload wallet, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float w = c.width();
        final float y = kit.section(c, top + 2, say(WalletAppTexts.YOUR_ADDRESS));
        final String address = wallet.address();
        final int half = address.length() / 2 + 1;
        c.text(address.substring(0, half), w / 2, y, kit.ink(IAppKit.Tone.TEXT), MobileCanvas.SMALL,
                MobileCanvas.Align.CENTRE, false);
        c.text(address.substring(half), w / 2, y + 9, kit.ink(IAppKit.Tone.TEXT), MobileCanvas.SMALL,
                MobileCanvas.Align.CENTRE, false);
        final float module = Math.max(1.0F, Math.min(3.0F, (w - 24) / (BlockSquare.SIDE + 4)));
        final float side = module * (BlockSquare.SIDE + 4);
        final float sx = (w - side) / 2;
        final float sy = y + 22;
        final Colours k = COLOURS.get();
        c.rect(sx, sy, side, side, k.paper());
        final boolean[][] square = BlockSquare.of(address);
        for (int row = 0; row < BlockSquare.SIDE; row++) {
            for (int column = 0; column < BlockSquare.SIDE; column++) {
                if (square[row][column]) {
                    c.rect(sx + (column + 2) * module, sy + (row + 2) * module, module, module, k.ink());
                }
            }
        }
        button(page, 6, sy + side + 6, w - 12, say(WalletAppTexts.COPY), false,
                MobileTap.run(() -> Minecraft.getInstance().keyboardHandler.setClipboard(address)));
    }

    /* A button as the look draws one; on qOS, the mock's own: Send in Blockwright's gold, Receive dark. */
    private static void button(final AppPage page, final float x, final float y, final float w, final String label,
                               final boolean primary, final MobileTap tap) {
        final MobileCanvas c = page.c();
        if (page.kit() instanceof QosKit) {
            final Colours k = COLOURS.get();
            c.rounded(x, y, w, 14, 7, primary ? k.gold() : k.button(), null);
            c.text(label, x + w / 2, y + 3.5F, primary ? k.ink() : k.paper(), MobileCanvas.SMALL,
                    MobileCanvas.Align.CENTRE,
                    false);
            c.press(x, y, w, 14, tap);
        } else {
            page.kit().button(c, x, y, w, label, primary, tap);
        }
    }

    private static String who(final WalletStatePayload.Move move) {
        final LedgerEntry.Kind kind = KINDS.byId(move.kind(), LedgerEntry.Kind.SEND);
        return say(switch (kind) {
            case SEND -> move.amount() < 0L ? WalletAppTexts.TO.with(TeracoinAddress.shortForm(move.other()))
                    : WalletAppTexts.FROM.with(TeracoinAddress.shortForm(move.other()));
            case PAYOUT, FEE -> WalletAppTexts.POOL_PAYOUT.text();
            case REWARD -> WalletAppTexts.BLOCK_REWARD.text();
            case PRINT -> WalletAppTexts.PRINTED.text();
        });
    }

    private static String when(final WalletStatePayload.Move move) {
        if (move.waiting()) {
            return say(WalletAppTexts.PENDING_NEXT);
        }
        return say(WalletAppTexts.BLOCK.with(String.format(Locale.ROOT, "%,d", move.block())));
    }

    private static TextKey noteOf(final int note) {
        return switch (note) {
            case WalletStatePayload.NOTE_BAD_ADDRESS -> WalletAppTexts.BAD_ADDRESS;
            case WalletStatePayload.NOTE_TOO_MUCH -> WalletAppTexts.TOO_MUCH;
            case WalletStatePayload.NOTE_NOTHING -> WalletAppTexts.NOTHING;
            default -> WalletAppTexts.NOT_SENT;
        };
    }

    private static String say(final TextKey key) {
        return GameText.resolve(key);
    }

    private static String say(final Text text) {
        return GameText.resolve(text);
    }

    /* Blockwright's gold, a send that waits, the square's ink and paper, and qOS's dark button. */
    private record Colours(int gold, int pending, int ink, int paper, int button) {
    }
}
