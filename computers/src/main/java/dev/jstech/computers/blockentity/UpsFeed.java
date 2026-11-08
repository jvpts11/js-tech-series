/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.power.UpsReading;
import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.text.Text;

/**
 * How the UPS a computer stands against stands, as that UPS last told it, for the battery its system shows: a UPS tells
 * the computers it feeds every tick, so one taken away stops being shown at once. The players who see the computer are
 * sent it as it changes by a percent or a minute.
 */
final class UpsFeed {

    private final IntField mode;
    private final IntField percent;
    private final LongField minutes;
    private long toldAt = NEVER;
    /* The UPS by name, which the machine's Device Manager tells it by; kept on the server, which builds that list. */
    private Text name = Text.EMPTY;

    /* A UPS that has not told it for this long is gone: it tells every tick, before or after the computer's. */
    private static final int GONE_AFTER = 2;
    private static final long NEVER = Long.MIN_VALUE / 2;
    private static final int PERMILLE_A_PERCENT = 10;

    UpsFeed(final BlockEntityFields fields) {
        this.mode = fields.integer("UpsMode", UpsReading.Mode.NONE.id()).toClient();
        this.percent = fields.integer("UpsPercent", 0).toClient();
        this.minutes = fields.longInteger("UpsMinutes", 0L).toClient();
    }

    /** Its UPS, {@code name}, tells it, in tick {@code gameTime}, how it stands. */
    void told(final UpsReading reading, final Text name, final long gameTime) {
        this.toldAt = gameTime;
        this.name = name;
        show(reading);
    }

    /** A tick of the computer, at {@code gameTime}: a UPS that stopped telling it is no longer shown. */
    void tick(final long gameTime) {
        if (gameTime - this.toldAt > GONE_AFTER && this.mode.get() != UpsReading.Mode.NONE.id()) {
            this.name = Text.EMPTY;
            show(UpsReading.NONE);
        }
    }

    /** Its UPS by name, on the server; empty without one. */
    Text name() {
        return this.name;
    }

    /** How its UPS stands, on either side: none without one. */
    UpsReading reading() {
        return new UpsReading(UpsReading.Mode.byId(this.mode.get()), this.percent.get() * PERMILLE_A_PERCENT,
                this.minutes.get());
    }

    private void show(final UpsReading reading) {
        this.mode.set(reading.mode().id());
        this.percent.set(reading.percent());
        this.minutes.set(reading.minutesLeft());
    }
}
