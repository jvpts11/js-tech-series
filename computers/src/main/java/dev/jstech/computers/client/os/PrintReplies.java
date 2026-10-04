/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.printer.PrintedPayload;
import dev.jstech.computers.operation.payload.printer.PrintersPayload;
import dev.jstech.core.text.GameText;
import org.jetbrains.annotations.Nullable;

/**
 * Where the server's answers about printing go on this client: the printers a Print dialog asked for go to that
 * dialog, and what became of a document goes to the notification area of the desktop looking at its machine.
 */
public final class PrintReplies {

    /** The Print dialog waiting for its machine's printers, if one is. */
    @Nullable
    private static PrintDialog waiting;

    private PrintReplies() {
    }

    /** Marks {@code dialog} as the one the next list of printers is for. */
    public static void expectPrinters(final PrintDialog dialog) {
        waiting = dialog;
    }

    public static void acceptPrinters(final PrintersPayload payload) {
        final PrintDialog dialog = waiting;
        if (dialog != null && dialog.host().equals(payload.hostPos())) {
            dialog.onPrinters(payload.machine(), payload.printers());
        }
    }

    public static void acceptPrinted(final PrintedPayload payload) {
        ActiveDesktop.raise(payload.hostPos(),
                GameText.resolve(payload.ok() ? PrintTexts.SENT_TITLE : PrintTexts.FAILED_TITLE),
                GameText.resolve(payload.message()), "");
    }

    /** Lets go of the dialog that was waiting, as it closes. */
    static void forget(final PrintDialog dialog) {
        if (waiting == dialog) {
            waiting = null;
        }
    }
}
