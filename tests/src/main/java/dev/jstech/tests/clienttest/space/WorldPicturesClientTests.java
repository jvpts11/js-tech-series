/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.universe.WorldPortraits;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;

/**
 * The pictures of the Moon and Mars on their sheets in the Mission Handbook are the worlds as the system's space draws
 * them, framed as the handbook shows them: each is drawn again and must look as the picture the mod carries, so a
 * change to a world's maps or to how space draws it cannot leave its picture behind. Run with {@code -PctPictures},
 * the test writes them again into the Space mod's sources instead; what it drew is left in the screenshots folder
 * either way.
 */
public final class WorldPicturesClientTests {

    /* Where the pictures are written from, the repository's root, when they are to be written again. */
    private static final String WRITE_FROM = "jstestkit.clienttests.pictures";
    private static final String SOURCES = "space/src/main/resources/assets/jsspace/textures/gui/guide/";
    private static final String CARRIED = "textures/gui/guide/";
    /* A picture's size on its sheet, and the sun 45 degrees west of the eye, so a slice of night lies on the east. */
    private static final int SIZE = 96;
    private static final double PHASE = 45;
    /*
     * Each world's picture and the place facing the eye, as the approved paintings framed them: the Moon's near side;
     * Mars over Valles Marineris, Tharsis' volcanoes to the west of it, its north pole leaning towards the eye so its
     * cap shows, as Mars is seen in its northern summer.
     */
    private static final List<Picture> PICTURES = List.of(new Picture(SolarBody.MOON, "moon", 0, 0),
            new Picture(SolarBody.MARS, "mars", 25, -70));

    private WorldPicturesClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void worldPictures_areTheWorldsAsSpaceDrawsThem(final ClientTestContext ctx) {
        ctx.then(2, () -> PICTURES.forEach(picture -> check(ctx, picture)));
    }

    private static void check(final ClientTestContext ctx, final Picture picture) {
        try (NativeImage drawn = WorldPortraits.draw(picture.body(), SIZE, picture.latitude(), picture.longitude(),
                PHASE)) {
            if (drawn == null) {
                ctx.fail(picture.name() + ": space cannot draw it yet, its maps or its shader not on the card");
                return;
            }
            keep(ctx, drawn, new File(new File(ctx.mc().gameDirectory, Screenshot.SCREENSHOT_DIR),
                    "ct-WorldPicturesClientTests-" + picture.name() + ".png"));
            final String root = System.getProperty(WRITE_FROM, "");
            if (!root.isEmpty()) {
                keep(ctx, drawn, new File(root, SOURCES + picture.name() + ".png"));
                return;
            }
            try (NativeImage carried = carried(ctx, picture)) {
                ctx.assertSamePicture(picture.name(), drawn, carried);
            }
        }
    }

    private static NativeImage carried(final ClientTestContext ctx, final Picture picture) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
                CARRIED + picture.name() + ".png");
        try (InputStream in = ctx.mc().getResourceManager().getResourceOrThrow(id).open()) {
            return NativeImage.read(in);
        } catch (final IOException failed) {
            ctx.fail(picture.name() + ": the handbook's picture " + id + " could not be read: " + failed);
            throw new IllegalStateException(failed);
        }
    }

    private static void keep(final ClientTestContext ctx, final NativeImage picture, final File file) {
        final File folder = file.getParentFile();
        try {
            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("the folder " + folder + " could not be made");
            }
            picture.writeToFile(file);
        } catch (final IOException failed) {
            ctx.fail("the picture " + file + " could not be written: " + failed);
        }
    }

    /* A world's picture: the world, the picture's name and the place facing the eye, in degrees north and east. */
    private record Picture(SolarBody body, String name, double latitude, double longitude) {
    }
}
