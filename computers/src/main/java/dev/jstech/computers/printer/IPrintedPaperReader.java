/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import dev.jstech.computers.registry.ClientHook;


/**
 * Where a player's game opens a printed sheet to read it, page by page. The sheet's item is common code, so the client
 * sets the real reader here during its setup and a dedicated server never touches a client class.
 */
@FunctionalInterface
public interface IPrintedPaperReader {

    void read(PrintedDocument document);

    final class Holder {

        private static final ClientHook<IPrintedPaperReader> HOOK = new ClientHook<>();

        private Holder() {
        }

        public static void set(final IPrintedPaperReader reader) {
            HOOK.set(reader);
        }

        public static void read(final PrintedDocument document) {
            HOOK.ifPresent(target -> target.read(document));
        }
    }
}
