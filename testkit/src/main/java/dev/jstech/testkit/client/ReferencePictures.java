/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.developer.Pictures;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * Pictures a client test compares what is on the screen with: a part of the frame, cut to the GUI's rectangle given,
 * against the reference picture of that name in the resources of the test's mod,
 * {@code clienttest/references/<name>.png}. A pixel may be a little off in each colour, and a few may be off by more,
 * so the small differences between one graphics card and another pass; a picture that differs more, or has none to
 * compare with, fails the test and leaves what was drawn, and where it differs, in the run's folder, to be looked at
 * and, when it is right, put in its place.
 */
public final class ReferencePictures {

    /** Where the references are among a mod's resources, and the folder a run leaves what it drew in. */
    public static final String FOLDER = "clienttest/references/";
    public static final String DRAWN = "reference-pictures";

    private ReferencePictures() {
    }

    /**
     * Fails unless the frame's rectangle {@code x}, {@code y}, {@code width} by {@code height}, in the GUI's pixels,
     * looks as the reference picture {@code name} of the mod {@code modid} does.
     */
    public static void check(final Minecraft mc, final String modid, final String name, final int x, final int y,
                             final int width, final int height) {
        try (NativeImage drawn = capture(mc, x, y, width, height)) {
            final Path file = referenceOf(modid, name);
            final String folder = DRAWN + "/" + modid;
            if (file == null || !Files.isRegularFile(file)) {
                final File left = leave(mc, folder, drawn, name + ".png");
                throw new ClientTestFailure("there is no reference picture " + name + ": what was drawn is at " + left
                        + "; when it is right, copy it into the resources of " + modid + " at " + FOLDER);
            }
            try (InputStream in = Files.newInputStream(file); NativeImage reference = NativeImage.read(in)) {
                compare(mc, folder, name, drawn, reference);
            }
        } catch (final IOException failed) {
            throw new ClientTestFailure("the reference picture " + name + " could not be read or left", failed);
        }
    }

    /** The frame's rectangle {@code x}, {@code y}, {@code width} by {@code height} in the GUI's pixels, a copy. */
    public static NativeImage capture(final Minecraft mc, final int x, final int y, final int width,
                                      final int height) {
        return Pictures.capture(mc, x, y, width, height);
    }

    /** Fails unless {@code drawn} looks as {@code other} does, both pictures of {@code name}. */
    public static void same(final Minecraft mc, final String name, final NativeImage drawn, final NativeImage other) {
        try {
            compare(mc, DRAWN, name, drawn, other);
        } catch (final IOException failed) {
            throw new ClientTestFailure("the pictures of " + name + " could not be left", failed);
        }
    }

    /* The reference picture's file among the mod's resources, or none when the mod is not there. */
    private static Path referenceOf(final String modid, final String name) {
        final IModFileInfo mod = ModList.get().getModFileById(modid);
        return mod == null ? null : mod.getFile().findResource(FOLDER + name + ".png");
    }

    private static void compare(final Minecraft mc, final String folder, final String name, final NativeImage drawn,
                                final NativeImage reference) throws IOException {
        if (drawn.getWidth() != reference.getWidth() || drawn.getHeight() != reference.getHeight()) {
            final File left = leave(mc, folder, drawn, name + ".png");
            throw new ClientTestFailure("the picture " + name + " is " + drawn.getWidth() + " by "
                    + drawn.getHeight() + " and its reference " + reference.getWidth() + " by "
                    + reference.getHeight() + "; what was drawn is at " + left);
        }
        try (NativeImage differences = new NativeImage(drawn.getWidth(), drawn.getHeight(), true)) {
            final int off = Pictures.differing(drawn, reference, differences);
            final int most = Pictures.mostOff(drawn);
            if (off > most) {
                final File left = leave(mc, folder, drawn, name + ".png");
                leave(mc, folder, differences, name + "-differences.png");
                throw new ClientTestFailure("the picture " + name + " differs from its reference in " + off
                        + " pixels, more than " + most + "; what was drawn is at " + left
                        + ", its differences in red beside it");
            }
        }
    }

    /* Leaves a picture in the run's folder, under the mod's own folder for a reference's, as the studio lists them. */
    private static File leave(final Minecraft mc, final String under, final NativeImage picture, final String file)
            throws IOException {
        final File folder = new File(mc.gameDirectory, under);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("the folder " + folder + " could not be made");
        }
        final File out = new File(folder, file);
        picture.writeToFile(out);
        return out;
    }
}
