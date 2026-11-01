/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

/**
 * The shapes of landforms, as heights over or under the ground at a distance from their middle: a crater's bowl, raised
 * rim and central peak; a volcano's cone, broad and gentle for a shield, steep for a stratovolcano, with its summit
 * crater; a mesa's flat top and cliffs; the ridges of dunes; the cross-sections of a canyon and of a rille; a glacier's
 * thickness down its length; a terraced apron's pools and rims, a chimney, a spider's troughs, a ridge and a layered
 * mound's steps. In blocks; pure.
 */
public final class LandformShapes {

    /* A crater's rim is as wide as half its radius; its central peak stands within a fifth of it. */
    private static final double RIM_WIDTH = 0.5;
    private static final double PEAK_REACH = 0.2;
    /* A volcano's summit crater is this share of its radius across, and this share of its height deep. */
    private static final double SUMMIT = 0.12;
    private static final double SUMMIT_DEPTH = 0.15;
    private static final double SHIELD_POWER = 2.0;
    private static final double STRATO_POWER = 1.15;
    /* How the lee side of a dune falls steeper than its windward side rises. */
    private static final double LEE = 0.7;
    /* Over what share of its length a glacier's head rises out of the land. */
    private static final double GLACIER_HEAD = 0.2;
    /* How many blocks wide a terrace's rim is, a little over one so it never falls between two columns. */
    private static final double TERRACE_RIM = 1.6;
    /* How much lower a chimney stands at its edge than in its middle, as a share of its height. */
    private static final double CHIMNEY_TAPER = 0.3;
    /*
     * A spider's pit and how deep it goes; how wide its legs are near the pit and at their tips; how far out they cut
     * two blocks deep; and past what share of its radius each forks, its branches spreading this far apart, in
     * radians, by its tip.
     */
    private static final double SPIDER_PIT = 1.6;
    private static final int SPIDER_PIT_DEPTH = 3;
    private static final double SPIDER_WIDEST = 1.4;
    private static final double SPIDER_THINNEST = 0.55;
    private static final double SPIDER_DEEP = 0.3;
    private static final double SPIDER_FORK = 0.5;
    private static final double SPIDER_SPREAD = 0.35;
    /* How a layered mound's slopes steepen towards its foot. */
    private static final double MOUND_POWER = 1.5;

    private LandformShapes() {
    }

    /**
     * How far a crater of {@code radius}, {@code depth} and a rim {@code rim} high moves the ground at {@code at}
     * blocks from its middle: down in its bowl, up on its rim, nothing beyond; a {@code peak} high hill in its middle.
     */
    public static double crater(final double at, final double radius, final double depth, final double rim,
                                final double peak) {
        if (at < radius) {
            final double share = at / radius;
            double height = -depth * (1 - share * share) + rim * share * share * share * share;
            if (peak > 0 && at < radius * PEAK_REACH) {
                height += peak * (1 - at / (radius * PEAK_REACH));
            }
            return height;
        }
        final double out = (at - radius) / (radius * RIM_WIDTH);
        return out >= 1 ? 0 : rim * (1 - out) * (1 - out);
    }

    /**
     * How high a volcano of {@code radius} and {@code height} stands at {@code at} blocks from its middle: a shield's
     * slope gentle and wide, a stratovolcano's steep; down into its summit crater at the top.
     */
    public static double volcano(final double at, final double radius, final double height, final boolean shield) {
        if (at >= radius) {
            return 0;
        }
        final double share = 1 - at / radius;
        double rise = height * Math.pow(share, shield ? SHIELD_POWER : STRATO_POWER);
        final double summit = radius * SUMMIT;
        if (at < summit) {
            rise -= height * SUMMIT_DEPTH * (1 - at / summit);
        }
        return rise;
    }

    /** Whether {@code at} is within a volcano's summit crater, where its lava lies. */
    public static boolean inSummit(final double at, final double radius) {
        return at < radius * SUMMIT * 0.6;
    }

    /**
     * How high a mesa of {@code radius} and {@code height} stands at {@code at}: its whole height over its top, falling
     * by cliffs over the last {@code edge} blocks of it.
     */
    public static double mesa(final double at, final double radius, final double height, final double edge) {
        if (at >= radius) {
            return 0;
        }
        final double fromEdge = radius - at;
        return fromEdge >= edge ? height : height * fromEdge / edge;
    }

    /**
     * How high dunes {@code height} high and {@code wavelength} apart stand at {@code along} blocks across their
     * ridges: up the windward side, down a steeper lee.
     */
    public static double dune(final double along, final double wavelength, final double height) {
        final double phase = ((along / wavelength) % 1 + 1) % 1;
        return phase < LEE ? height * Math.sin(phase / LEE * Math.PI / 2)
                : height * Math.cos((phase - LEE) / (1 - LEE) * Math.PI / 2);
    }

    /**
     * How deep a canyon {@code width} wide and {@code depth} deep is at {@code across} blocks from its middle line:
     * its whole depth on its floor, its walls steep, nothing past its rim.
     */
    public static double canyon(final double across, final double width, final double depth) {
        final double half = width / 2;
        final double out = Math.abs(across);
        if (out >= half) {
            return 0;
        }
        final double floor = half * 0.4;
        return out <= floor ? depth : depth * Math.sqrt(1 - (out - floor) / (half - floor));
    }

    /**
     * How deep a rille {@code width} wide and {@code depth} deep is at {@code across} blocks from its middle line: a
     * smooth trough, deepest in its middle and rising evenly to its rims, as a lava channel whose roof fell in lies;
     * nothing past them.
     */
    public static double rille(final double across, final double width, final double depth) {
        final double half = width / 2;
        final double out = Math.abs(across);
        return out >= half ? 0 : depth * Math.cos(out / half * Math.PI / 2);
    }

    /**
     * How thick a glacier {@code length} long either way of its middle and {@code width} wide is at {@code along}
     * blocks down it and {@code across} from its middle line, {@code thickness} at its thickest: thick near its head,
     * at {@code along} of minus its length, where it rises out of the land, thinning down to nothing at its snout, and
     * from its middle line to its edges.
     */
    public static double glacier(final double along, final double across, final double length, final double width,
                                 final double thickness) {
        final double half = width / 2;
        final double out = Math.abs(across);
        if (Math.abs(along) >= length || out >= half) {
            return 0;
        }
        final double down = (along + length) / (2 * length);
        final double head = Math.min(1, (along + length) / (length * GLACIER_HEAD));
        final double side = out / half;
        return thickness * head * Math.sqrt(1 - down) * (1 - side * side);
    }

    /**
     * How high the floor of a terraced apron of {@code radius} and {@code height} stands at {@code at}: an apron
     * sloping evenly from its whole height in its middle to the ground at its edge, cut into level pools {@code step}
     * high, each a step down from the one inside it; nothing beyond it.
     */
    public static double terrace(final double at, final double radius, final double height, final double step) {
        if (at >= radius) {
            return 0;
        }
        return Math.floor(height * (1 - at / radius) / step) * step;
    }

    /**
     * Whether {@code at} lies on a rim of a terraced apron: the last block of each pool, at its lower edge, where the
     * apron's slope has fallen to just above the pool's floor.
     */
    public static boolean terraceRim(final double at, final double radius, final double height, final double step) {
        if (at >= radius || height <= 0) {
            return false;
        }
        final double slope = height * (1 - at / radius);
        return slope - Math.floor(slope / step) * step < Math.min(TERRACE_RIM * height / radius, step);
    }

    /**
     * How tall a chimney of {@code radius} and {@code height} stands at {@code at}: its whole height in its middle, a
     * little lower at its edge; nothing beyond it.
     */
    public static double chimney(final double at, final double radius, final double height) {
        return at >= radius ? 0 : height * (1 - CHIMNEY_TAPER * at / radius);
    }

    /**
     * How deep the troughs of a spider of {@code radius} are at {@code at} from its middle and {@code angle} round it,
     * its legs running out at the angles {@code legs}, in radians: a pit in its middle, and each leg a trough two
     * blocks deep near it and one further out, narrowing outwards and forking in two past its middle; 0 off them.
     */
    public static int spider(final double at, final double angle, final double radius, final double[] legs) {
        if (at >= radius) {
            return 0;
        }
        if (at < SPIDER_PIT) {
            return SPIDER_PIT_DEPTH;
        }
        final double share = at / radius;
        final double width = Math.max(SPIDER_THINNEST, SPIDER_WIDEST * (1 - share));
        int deepest = 0;
        for (final double leg : legs) {
            if (beside(at, angle, leg) < width) {
                deepest = Math.max(deepest, share < SPIDER_DEEP ? 2 : 1);
            }
            if (share > SPIDER_FORK) {
                final double spread = SPIDER_SPREAD * (share - SPIDER_FORK) / (1 - SPIDER_FORK);
                if (beside(at, angle, leg + spread) < width || beside(at, angle, leg - spread) < width) {
                    deepest = Math.max(deepest, 1);
                }
            }
        }
        return deepest;
    }

    /**
     * How high a ridge {@code width} wide and {@code height} high stands at {@code across} blocks from its line: its
     * whole height on its crest, sloping evenly to the ground at its foot.
     */
    public static double ridge(final double across, final double width, final double height) {
        final double half = width / 2;
        final double out = Math.abs(across);
        return out >= half ? 0 : height * (1 - out / half);
    }

    /**
     * How high a layered mound of {@code radius} and {@code height} stands at {@code at}: highest in its middle and
     * falling ever faster to its foot, its slopes climbing in steps {@code step} high, each band set back from the one
     * under it; nothing beyond it.
     */
    public static double layeredMound(final double at, final double radius, final double height, final double step) {
        if (at >= radius) {
            return 0;
        }
        final double rise = height * (1 - Math.pow(at / radius, MOUND_POWER));
        return Math.floor(rise / step) * step;
    }

    /** How much of a landform's change holds at {@code at} of {@code radius}: all of it inside, fading to none. */
    public static double fade(final double at, final double radius, final double edge) {
        if (at <= radius - edge) {
            return 1;
        }
        return at >= radius ? 0 : (radius - at) / edge;
    }

    /* How far {@code at} blocks out at {@code angle} lies beside the line out from the middle at {@code leg}. */
    private static double beside(final double at, final double angle, final double leg) {
        final double turn = Math.IEEEremainder(angle - leg, 2 * Math.PI);
        return Math.abs(turn) >= Math.PI / 2 ? Double.MAX_VALUE : at * Math.sin(Math.abs(turn));
    }
}
