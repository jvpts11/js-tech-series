/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import java.util.Locale;

/**
 * How a Network Gateway is named. A Gateway gets a default name the first time it links to a computer
 * ({@code gateway-1}, {@code gateway-2} on the same host), the player may rename it from the Gateway
 * Manager or the shell, and the name is what the ComputerCraft side and Cannon programs address it by,
 * so it is kept to the characters both are comfortable with.
 */
public final class GatewayName {

    /** The longest name a Gateway may carry. */
    public static final int MAX = 24;
    /** What a Gateway is called before it ever links to a computer. */
    public static final String UNNAMED = "gateway";
    private static final String PREFIX = "gateway-";
    private static final String PERIPHERAL_PREFIX = "jsc_gateway_";

    private GatewayName() {
    }

    /** The default name of the {@code ordinal}th Gateway on a host, counted from one. */
    public static String defaultFor(final int ordinal) {
        return PREFIX + Math.max(1, ordinal);
    }

    /**
     * What a typed name becomes: lower case, letters, digits, dashes and underscores only, every other
     * run of characters folded into one dash, no dash at either end, at most {@link #MAX} characters.
     * An empty result means "back to the default".
     */
    public static String clean(final String typed) {
        if (typed == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder();
        boolean dash = false;
        for (final char c : typed.toLowerCase(Locale.ROOT).toCharArray()) {
            final boolean keep = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (keep) {
                out.append(c);
                dash = false;
            } else if (!dash && !out.isEmpty()) {
                out.append('-');
                dash = true;
            }
        }
        String name = out.toString();
        while (name.endsWith("-")) {
            name = name.substring(0, name.length() - 1);
        }
        return name.length() > MAX ? name.substring(0, MAX) : name;
    }

    /** The name a ComputerCraft computer wraps this Gateway by: {@code jsc_gateway_} and the name. */
    public static String peripheralName(final String name) {
        return PERIPHERAL_PREFIX + name.replace('-', '_');
    }
}
