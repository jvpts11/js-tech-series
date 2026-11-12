/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.audio.MediaLibrary;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.PictureScaling;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.NetworkFixtures;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Media Player's side on the computer: which systems come with it, the image sequences it finds, each a folder of
 * pictures in the order of their names, and a picture travelling to it as a small PNG (computers/docs/design/
 * programs.md). Its songs play through the computer's player, which Soundfoundry's tests cover.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MediaPlayerGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final String SUNRISE = SystemLayout.PICTURES_DIR + "/sunrise";
    private static final int SIDE = 8;

    private MediaPlayerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void library_findsEachFolderOfPicturesInTheOrderOfTheirNames(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = framesComputer(helper);
        helper.assertTrue(MediaLibrary.playerOn(pc), "a desktop system comes with the Media Player");
        write(helper, pc, SUNRISE + "/frame10.pix", 3);
        write(helper, pc, SUNRISE + "/frame01.pix", 1);
        write(helper, pc, SUNRISE + "/frame02.pix", 2);
        write(helper, pc, SystemLayout.PICTURES_DIR + "/house.pix", 4);
        final List<MediaLibrary.Sequence> sequences = MediaLibrary.sequences(helper.getLevel(), pc);
        helper.assertTrue(sequences.size() == 2, "the Pictures folder and its sunrise folder; got " + sequences);
        helper.assertTrue(sequences.get(0).frames().equals(List.of(SystemLayout.PICTURES_DIR + "/house.pix")),
                "the pictures right in Pictures first; got " + sequences.get(0));
        helper.assertTrue(sequences.get(1).frames().equals(List.of(SUNRISE + "/frame01.pix", SUNRISE + "/frame02.pix",
                SUNRISE + "/frame10.pix")), "then sunrise's, by name; got " + sequences.get(1));
        helper.assertTrue(MediaLibrary.sequence(helper.getLevel(), pc, SUNRISE).frames().size() == 3,
                "and a folder asked for by its path plays the same pictures");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void frame_isThePictureAsAPngOfItsOwnSize(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = framesComputer(helper);
        write(helper, pc, SUNRISE + "/frame01.pix", 5);
        final byte[] png = MediaLibrary.frame(helper.getLevel(), pc, SUNRISE + "/frame01.pix");
        helper.assertTrue(png != null, "a picture of Paint travels");
        final BufferedImage image;
        try {
            image = PictureScaling.decode(png, 1L << 20);
        } catch (final IOException unreadable) {
            throw new IllegalStateException("the picture that travelled does not read", unreadable);
        }
        helper.assertTrue(image != null && image.getWidth() == SIDE && image.getHeight() == SIDE,
                "at its own size, small as it is; got " + (image == null ? "nothing" : image.getWidth() + "x"
                        + image.getHeight()));
        helper.assertTrue(image.getRGB(0, 0) == PixImage.colourOf(5), "with its colours");
        helper.assertTrue(MediaLibrary.frame(helper.getLevel(), pc, SUNRISE + "/frame01.pix") == png,
                "a picture asked for again is the one made before, not made a second time");
        helper.assertTrue(MediaLibrary.frame(helper.getLevel(), pc, SUNRISE + "/missing.pix") == null,
                "and a file that is not there is no picture");
        helper.succeed();
    }

    private static PersonalComputerBlockEntity framesComputer(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = NetworkFixtures.placeRunningPC(helper, PC);
        if (!pc.installOs(ResourceLocation.fromNamespaceAndPath("jsc", "frames_10"))) {
            throw new IllegalStateException("could not install Frames 10");
        }
        return pc;
    }

    /* Writes a picture of one colour at {@code path}, its folders made first. */
    private static void write(final GameTestHelper helper, final PersonalComputerBlockEntity pc, final String path,
                              final int colour) {
        final ItemStack disk = pc.systemDisk();
        final FilesystemKind kind = FileAccess.filesystemKindOf(pc);
        DiskFilesystem.mkdirs(disk, path.substring(0, path.lastIndexOf('/')), kind);
        final PixImage picture = new PixImage(SIDE, SIDE);
        picture.fillAll(colour);
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, path, FileType.PIX, picture.encode(),
                pc.systemDiskFreeWeight(), kind);
        helper.assertTrue(result == DiskFilesystem.WriteResult.OK, "the picture is written at " + path + "; got "
                + result);
    }
}
