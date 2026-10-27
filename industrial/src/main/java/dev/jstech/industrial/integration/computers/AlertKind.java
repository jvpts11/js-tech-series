/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What an Industrial Controller Computer raises an alert for, as a plant's alarm panel does: a machine that has just
 * overheated, blown its fuse, fallen short of its voltage or been damaged, or a gas leaking into the air around it past
 * that gas's limit. Each is as grave as a plant's operator would take it, which decides its colour and the level it is
 * written into the network's event log at.
 */
@TextHolder
public enum AlertKind implements IStableName {

    OVERHEATING("overheating", TextKey.of("jsindustrial.alert.overheating", "Overheating"), Severity.CRITICAL),
    BLOWN_FUSE("blown_fuse", TextKey.of("jsindustrial.alert.blown_fuse", "Blown fuse"), Severity.CRITICAL),
    UNDERVOLTAGE("undervoltage", TextKey.of("jsindustrial.alert.undervoltage", "Undervoltage"), Severity.WARNING),
    GAS_LEAK("gas_leak", TextKey.of("jsindustrial.alert.gas_leak", "Gas leak"), Severity.WARNING),
    DAMAGED("damaged", TextKey.of("jsindustrial.alert.damaged", "Damaged"), Severity.NOTICE);

    private final String serializedName;
    private final TextKey text;
    private final Severity severity;

    AlertKind(final String serializedName, final TextKey text, final Severity severity) {
        this.serializedName = serializedName;
        this.text = text;
        this.severity = severity;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What the alert is called. */
    public TextKey text() {
        return this.text;
    }

    /** How grave it is. */
    public Severity severity() {
        return this.severity;
    }

    /** How grave a plant's operator takes an alert: a fault that stops the machine, a warning, or a notice. */
    public enum Severity implements IStableName {

        CRITICAL("critical", EventLevel.ERROR),
        WARNING("warning", EventLevel.WARN),
        NOTICE("notice", EventLevel.INFO);

        private final String serializedName;
        private final EventLevel logged;

        Severity(final String serializedName, final EventLevel logged) {
            this.serializedName = serializedName;
            this.logged = logged;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** The level the network's event log writes an alert of it at. */
        public EventLevel logged() {
            return this.logged;
        }
    }
}
