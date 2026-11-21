/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonObject;
import dev.jstech.core.agent.AgentReport;
import java.util.Map;
import java.util.Optional;

/** What mods tell an agent of a thing ({@code dev.jstech.core.agent}), as the answer's JSON writes it. */
final class PlayReadings {

    /** The field the readings stand under, beside what the Core tells of the thing. */
    static final String READINGS = "readings";

    private PlayReadings() {
    }

    /** Adds {@code report} to {@code out} as its readings, unless it tells nothing. */
    static void add(final JsonObject out, final AgentReport report) {
        of(report).ifPresent(readings -> out.add(READINGS, readings));
    }

    private static Optional<JsonObject> of(final AgentReport report) {
        if (report.isEmpty()) {
            return Optional.empty();
        }
        final JsonObject out = new JsonObject();
        for (final Map.Entry<String, Object> each : report.values().entrySet()) {
            switch (each.getValue()) {
                case AgentReport part -> of(part).ifPresent(json -> out.add(each.getKey(), json));
                case Number number -> out.addProperty(each.getKey(), number);
                case Boolean flag -> out.addProperty(each.getKey(), flag);
                default -> out.addProperty(each.getKey(), String.valueOf(each.getValue()));
            }
        }
        return Optional.of(out);
    }
}
