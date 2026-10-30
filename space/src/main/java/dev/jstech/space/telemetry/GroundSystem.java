/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.telemetry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One ground system: the devices one connected stretch of the telemetry line joins, each where it stands on its
 * world's sphere. It works once it has an antenna, a receiver, a decoder and a ground station; with a data recorder it
 * keeps what comes down. Several antennas on it add up, as the Deep Space Network joins its dishes to hear the
 * Voyagers: their rates add, and the largest sets how far it hears.
 *
 * <p>Pure: no game types.
 *
 * @param devices its devices
 */
public record GroundSystem(List<Device> devices) {

    public GroundSystem {
        devices = List.copyOf(devices);
    }

    /** Whether it works: an antenna, a receiver, a decoder and a ground station on it. */
    public boolean works() {
        return has(TelemetryRole.ANTENNA) && has(TelemetryRole.RECEIVER) && has(TelemetryRole.DECODER)
                && has(TelemetryRole.GROUND_STATION);
    }

    /** Whether it keeps what comes down: a data recorder on it. */
    public boolean keeps() {
        return has(TelemetryRole.RECORDER);
    }

    /** How far its antennas hear: the largest one's reach; none without an antenna. */
    public Optional<AntennaSize.Reach> reach() {
        AntennaSize.Reach best = null;
        for (final Device device : this.devices) {
            if (device.size().isPresent() && (best == null || device.size().get().reach().covers(best))) {
                best = device.size().get().reach();
            }
        }
        return Optional.ofNullable(best);
    }

    /** How much data its antennas bring down a second, all of them together; none while it does not work. */
    public int rate() {
        if (!works()) {
            return 0;
        }
        return this.devices.stream().flatMap(device -> device.size().stream()).mapToInt(AntennaSize::rate).sum();
    }

    /** Its antennas. */
    public List<Device> antennas() {
        return this.devices.stream().filter(device -> device.role() == TelemetryRole.ANTENNA).toList();
    }

    private boolean has(final TelemetryRole role) {
        return this.devices.stream().anyMatch(device -> device.role() == role);
    }

    /**
     * A device on the line.
     *
     * @param role      what it does
     * @param size      how big it is, for an antenna
     * @param longitude where it stands, in degrees east
     * @param latitude  where it stands, in degrees north
     */
    public record Device(TelemetryRole role, Optional<AntennaSize> size, double longitude, double latitude) {

        public Device {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(size, "size");
        }

        /** An antenna of {@code size} at {@code longitude}, {@code latitude}. */
        public static Device antenna(final AntennaSize size, final double longitude, final double latitude) {
            return new Device(TelemetryRole.ANTENNA, Optional.of(size), longitude, latitude);
        }

        /** A device that is not an antenna, doing {@code role}. */
        public static Device of(final TelemetryRole role) {
            return new Device(role, Optional.empty(), 0, 0);
        }
    }
}
