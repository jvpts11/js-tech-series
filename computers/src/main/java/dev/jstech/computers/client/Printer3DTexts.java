/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** A 3D printer's window's words, apart from the window so a server can read them without loading a screen. */
@TextHolder
final class Printer3DTexts {

    static final TextKey ITEM_OF = TextKey.of("jsc.printer3d.item_of", "ITEM %s OF %s");
    static final TextKey IDLE = TextKey.of("jsc.printer3d.idle", "READY");
    static final TextKey PAUSED = TextKey.of("jsc.printer3d.paused", "PAUSED");
    static final TextKey WAITING_TRC = TextKey.of("jsc.printer3d.waiting_trc", "WAITING FOR TRC");
    static final TextKey WAITING_WALLET = TextKey.of("jsc.printer3d.waiting_wallet", "WAITING FOR WALLET");
    static final TextKey OUTPUT_FULL = TextKey.of("jsc.printer3d.output_full", "OUTPUT FULL");
    static final TextKey NOW = TextKey.of("jsc.printer3d.now", "NOW");
    static final TextKey NOTHING = TextKey.of("jsc.printer3d.nothing", "Nothing to print");
    static final TextKey ITEM_SUB = TextKey.of("jsc.printer3d.item_sub", "item %s of %s");
    static final TextKey LAYER = TextKey.of("jsc.printer3d.layer", "LAYER");
    static final TextKey LAYER_OF = TextKey.of("jsc.printer3d.layer_of", "%s of %s");
    static final TextKey NEEDS = TextKey.of("jsc.printer3d.needs", "NEEDS");
    static final TextKey TRC = TextKey.of("jsc.printer3d.trc", "%s TRC");
    static final TextKey AN_ITEM = TextKey.of("jsc.printer3d.an_item", "AN ITEM");
    static final TextKey SECONDS = TextKey.of("jsc.printer3d.seconds", "%s s");
    static final TextKey PAUSE = TextKey.of("jsc.printer3d.pause", "PAUSE");
    static final TextKey RESUME = TextKey.of("jsc.printer3d.resume", "RESUME");
    static final TextKey CANCEL = TextKey.of("jsc.printer3d.cancel", "CANCEL");
    static final TextKey NOTE = TextKey.of("jsc.printer3d.note", "Each item is paid as it starts.");
    static final TextKey QUEUE = TextKey.of("jsc.printer3d.queue", "QUEUE");
    static final TextKey JOB = TextKey.of("jsc.printer3d.job", "%s  x%s");
    static final TextKey JOB_STATE = TextKey.of("jsc.printer3d.job_state", "%s, %s");
    static final TextKey PRINTING_DONE = TextKey.of("jsc.printer3d.printing_done", "printing, %s done");
    static final TextKey WAITS_FOR = TextKey.of("jsc.printer3d.waits_for", "waits for %s TRC");
    static final TextKey WAITS_FILE = TextKey.of("jsc.printer3d.waits_file", "its file is not there");
    static final TextKey WAITING = TextKey.of("jsc.printer3d.waiting", "waiting");
    static final TextKey NOW_LINE = TextKey.of("jsc.printer3d.now_line", "%s, %s");
    static final TextKey LEFT = TextKey.of("jsc.printer3d.left", "layer %s of %s, %s s left");
    static final TextKey HOLDS = TextKey.of("jsc.printer3d.holds", "%s has %s TRC");
    static final TextKey NO_FILE = TextKey.of("jsc.printer3d.no_file", "%s is not on its computer");
    static final TextKey TAKE_OUT = TextKey.of("jsc.printer3d.take_out", "Take the items out of its output");
    static final TextKey OUT = TextKey.of("jsc.printer3d.out", "OUT");
    static final TextKey LINKED = TextKey.of("jsc.printer3d.linked", "LINKED");
    static final TextKey PRINTED = TextKey.of("jsc.printer3d.printed", "PRINTED");
    static final TextKey ITEMS = TextKey.of("jsc.printer3d.items", "%s items");

    private Printer3DTexts() {
    }
}
