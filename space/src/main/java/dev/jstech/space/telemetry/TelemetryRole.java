/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.telemetry;

/**
 * What a device on the telemetry line does in its ground system, as the devices of a real station: the antenna that
 * hears the satellite, the receiver that amplifies the faint signal, the decoder that turns it into data, the data
 * recorder that keeps it, and the ground station, the antenna's console.
 *
 * <p>Pure: no game types.
 */
public enum TelemetryRole {

    ANTENNA,
    RECEIVER,
    DECODER,
    RECORDER,
    GROUND_STATION
}
