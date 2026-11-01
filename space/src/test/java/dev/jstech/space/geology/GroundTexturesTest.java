/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class GroundTexturesTest {

    /* Every picture of the Moon's and Mars' ground, each in four variants. */
    private static final List<String> GROUND = List.of("highland_regolith", "mare_regolith", "icy_regolith",
            "anorthosite", "mare_basalt", "martian_regolith", "hematite_regolith", "basalt_sand", "martian_basalt",
            "martian_basalt_top", "columnar_martian_basalt", "columnar_martian_basalt_top", "vesicular_martian_basalt",
            "vesicular_martian_basalt_top", "mudstone", "mudstone_top", "buff_mudstone", "buff_mudstone_top",
            "dusted_mudstone", "dusted_mudstone_top", "water_ice", "dry_ice", "silica_sinter", "silica_sinter_top",
            "nodular_silica", "silica_soil", "layered_sulfate", "layered_sulfate_top", "fan_dust", "digitate_silica");
    /* The game's block each looked like before it had its own. */
    private static final Map<String, String> LOOKALIKE = Map.ofEntries(Map.entry("anorthosite", "diorite"),
            Map.entry("highland_regolith", "gravel"), Map.entry("mare_regolith", "gravel"),
            Map.entry("mare_basalt", "smooth_basalt"), Map.entry("icy_regolith", "snow"),
            Map.entry("martian_regolith", "red_sand"), Map.entry("hematite_regolith", "terracotta"),
            Map.entry("basalt_sand", "blackstone"), Map.entry("martian_basalt", "basalt_side"),
            Map.entry("mudstone", "andesite"), Map.entry("water_ice", "packed_ice"), Map.entry("dry_ice", "snow"));
    /*
     * What each rock looks like, by its mean colour, red, green and blue: the anorthosite near white, the mare basalt
     * dark, Mars' regolith red, its mudstone grey and its buff mudstone warm, the ices pale and blue, the silica pale.
     */
    private static final Map<String, Predicate<double[]>> LOOKS = Map.ofEntries(
            Map.entry("anorthosite", c -> lum(c) >= 180),
            Map.entry("highland_regolith", c -> lum(c) >= 100 && lum(c) <= 170 && Math.abs(c[0] - c[2]) < 15),
            Map.entry("mare_regolith", c -> lum(c) <= 110),
            Map.entry("mare_basalt", c -> lum(c) <= 90),
            Map.entry("icy_regolith", c -> c[2] >= c[0] && lum(c) >= 80 && lum(c) <= 180),
            Map.entry("martian_regolith", c -> c[0] - c[2] >= 50),
            Map.entry("hematite_regolith", c -> c[0] - c[2] >= 25),
            Map.entry("basalt_sand", c -> lum(c) <= 80),
            Map.entry("martian_basalt", c -> lum(c) <= 100),
            Map.entry("mudstone", c -> Math.abs(c[0] - c[2]) <= 15),
            Map.entry("buff_mudstone", c -> c[0] - c[2] >= 25),
            Map.entry("water_ice", c -> c[2] > c[0] && lum(c) >= 170),
            Map.entry("dry_ice", c -> lum(c) >= 190),
            Map.entry("silica_sinter", c -> lum(c) >= 170),
            Map.entry("layered_sulfate", c -> c[0] - c[2] >= 25 && lum(c) >= 150),
            Map.entry("fan_dust", c -> lum(c) <= 70));
    private static final int SIDE = 16;
    private static final int VARIANTS = 4;
    /* Above this, a picture's light and dark follow the game's texel by texel: a recolouring, not a drawing. */
    private static final double RECOLOURED = 0.8;

    @Test
    void ground_hasFourVariantsOfSixteenTexelsEach() {
        for (final String name : GROUND) {
            for (int v = 0; v < VARIANTS; v++) {
                final BufferedImage image = own(variant(name, v));
                assertEquals(SIDE, image.getWidth(), variant(name, v) + " is sixteen texels wide");
                assertEquals(SIDE, image.getHeight(), variant(name, v) + " and sixteen high");
            }
        }
    }

    @Test
    void variants_ofEachPictureDifferFromOneAnother() {
        for (final String name : GROUND) {
            for (int a = 0; a < VARIANTS; a++) {
                for (int b = a + 1; b < VARIANTS; b++) {
                    assertFalse(Arrays.equals(texels(own(variant(name, a))), texels(own(variant(name, b)))),
                            variant(name, a) + " and " + variant(name, b) + " are not one stamp");
                }
            }
        }
    }

    @Test
    void pictures_areNoneOfTheGamesOwnNorItsRecoloured() throws IOException {
        for (final Map.Entry<String, String> each : LOOKALIKE.entrySet()) {
            final BufferedImage game = games("assets/minecraft/textures/block/" + each.getValue() + ".png");
            for (int v = 0; v < VARIANTS; v++) {
                final BufferedImage drawn = own(variant(each.getKey(), v));
                final double alike = correlation(light(drawn), light(game));
                assertTrue(alike < RECOLOURED, variant(each.getKey(), v) + " is drawn, not " + each.getValue()
                        + " recoloured: its light follows the game's by " + alike);
            }
        }
    }

    @Test
    void colours_areTheRocksOwn() {
        for (final Map.Entry<String, Predicate<double[]>> each : LOOKS.entrySet()) {
            for (int v = 0; v < VARIANTS; v++) {
                final double[] mean = mean(own(variant(each.getKey(), v)));
                assertTrue(each.getValue().test(mean), variant(each.getKey(), v) + " looks as its rock does: "
                        + Arrays.toString(mean));
            }
        }
    }

    private static String variant(final String name, final int v) {
        return v == 0 ? name : name + "_" + v;
    }

    private static BufferedImage own(final String name) {
        return image("/assets/jsspace/textures/block/" + name + ".png");
    }

    private static BufferedImage image(final String path) {
        try (InputStream in = GroundTexturesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "the picture " + path + " is there");
            return ImageIO.read(in);
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /*
     * A picture of the game's own, from the jar of its resources the build unpacks beside the mod: the tests' class
     * path holds the game's code but not its pictures.
     */
    private static BufferedImage games(final String path) throws IOException {
        final Path jar;
        try (Stream<Path> files = Files.list(Path.of("build", "moddev", "artifacts"))) {
            jar = files.filter(file -> file.getFileName().toString().contains("minecraft-resources")).findFirst()
                    .orElseThrow(() -> new IllegalStateException("the game's resources are unpacked by the build"));
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            final ZipEntry entry = zip.getEntry(path);
            assertNotNull(entry, "the game's picture " + path + " is there");
            try (InputStream in = zip.getInputStream(entry)) {
                return ImageIO.read(in);
            }
        }
    }

    private static int[] texels(final BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    /* The mean colour of the opaque texels, red, green and blue. */
    private static double[] mean(final BufferedImage image) {
        final double[] sum = new double[3];
        int count = 0;
        for (final int argb : texels(image)) {
            if (argb >>> 24 < 128) {
                continue;
            }
            sum[0] += argb >> 16 & 0xFF;
            sum[1] += argb >> 8 & 0xFF;
            sum[2] += argb & 0xFF;
            count++;
        }
        return new double[] {sum[0] / count, sum[1] / count, sum[2] / count};
    }

    private static double lum(final double[] colour) {
        return 0.299 * colour[0] + 0.587 * colour[1] + 0.114 * colour[2];
    }

    /* Each texel's light, the first sixteen by sixteen of the picture. */
    private static double[] light(final BufferedImage image) {
        final double[] out = new double[SIDE * SIDE];
        for (int y = 0; y < SIDE; y++) {
            for (int x = 0; x < SIDE; x++) {
                final int argb = image.getRGB(x, y);
                out[y * SIDE + x] = lum(new double[] {argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF});
            }
        }
        return out;
    }

    private static double correlation(final double[] a, final double[] b) {
        final double meanA = Arrays.stream(a).average().orElse(0);
        final double meanB = Arrays.stream(b).average().orElse(0);
        double both = 0;
        double onlyA = 0;
        double onlyB = 0;
        for (int i = 0; i < a.length; i++) {
            both += (a[i] - meanA) * (b[i] - meanB);
            onlyA += (a[i] - meanA) * (a[i] - meanA);
            onlyB += (b[i] - meanB) * (b[i] - meanB);
        }
        return onlyA == 0 || onlyB == 0 ? 0 : both / Math.sqrt(onlyA * onlyB);
    }
}
