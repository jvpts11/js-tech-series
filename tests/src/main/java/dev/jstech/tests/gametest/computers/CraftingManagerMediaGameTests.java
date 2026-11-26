/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.crafting.CraftManagerPayloads;
import dev.jstech.computers.operation.payload.crafting.CraftPlaces;
import dev.jstech.computers.operation.payload.crafting.CraftSources;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.CraftFiles;
import dev.jstech.tests.testkit.computers.CraftingRig;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Crafting Manager shows the disc that carries the recipes. A computer usually has more than one drive
 * linked, and the first of them may hold a blank medium: a demo went wrong when three crafts written onto a
 * DVD-RW were nowhere to be seen because the manager was looking at the first writable medium it found.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingManagerMediaGameTests {

    private CraftingManagerMediaGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /*
     * The crafting network's computer sits at (5,2,2) with its rear on the cable to the west; the drives go
     * on its free sides, the floppy drive first so it is the first linked endpoint.
     */
    private static final BlockPos FLOPPY_DRIVE = new BlockPos(5, 2, 3);
    private static final BlockPos DVD_DRIVE = new BlockPos(5, 2, 1);
    private static final BlockPos ENCODER = new BlockPos(1, 2, 4);
    private static final String FURNACE = "Recipes/Furnace";

    @GameTest(template = ARENA)
    public static void craftManagerState_showsTheDriveWhoseDiscHoldsTheCrafts(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        world.setBlock(FLOPPY_DRIVE, ComputingModule.FLOPPY_DRIVE.get());
        world.setBlock(DVD_DRIVE, ComputingModule.DVD_DRIVE.get());
        // Three... well, one craft on a DVD-RW, as the encoder leaves it once its job is through.
        final ItemStack dvd = new ItemStack(ComputingModule.DVD_RW.get());
        CraftFiles.writeBench(dvd, CraftFiles.oakPlanks(), helper.getLevel().registryAccess());

        final MediaReaderBlockEntity floppyDrive = world.blockEntity(FLOPPY_DRIVE, MediaReaderBlockEntity.class);
        final MediaReaderBlockEntity dvdDrive = world.blockEntity(DVD_DRIVE, MediaReaderBlockEntity.class);
        floppyDrive.mediaSlot().setStackInSlot(0, new ItemStack(ComputingModule.FLOPPY_DISK.get()));
        dvdDrive.mediaSlot().setStackInSlot(0, dvd);

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final BlockPos computer = net.cc().getBlockPos();
                    helper.assertTrue(computer.equals(floppyDrive.ownerPos()) && computer.equals(dvdDrive.ownerPos()),
                            "both drives link to the Crafting Computer; got " + floppyDrive.ownerPos() + " and "
                                    + dvdDrive.ownerPos());
                    final CraftManagerStatePayload state =
                            CraftManagerPayloads.buildCraftManagerState(net.cc(), helper.getLevel());
                    helper.assertTrue(state.mediaVolumeKey().equals("media:" + dvdDrive.getBlockPos().asLong()),
                            "the manager shows the DVD drive, whose disc holds the craft; got " + state.mediaVolumeKey());
                    helper.assertTrue(state.mediaFiles().size() == 1,
                            "the DVD's one craft is listed; got " + state.mediaFiles());
                    // With the DVD out, the blank floppy is still offered: a download needs a writable target.
                    dvdDrive.mediaSlot().setStackInSlot(0, ItemStack.EMPTY);
                    final CraftManagerStatePayload fallback =
                            CraftManagerPayloads.buildCraftManagerState(net.cc(), helper.getLevel());
                    helper.assertTrue(fallback.mediaVolumeKey().equals("media:" + floppyDrive.getBlockPos().asLong())
                                    && fallback.mediaFiles().isEmpty(),
                            "without crafts anywhere, the first writable medium is offered; got "
                                    + fallback.mediaVolumeKey());
                })
                .thenSucceed();
    }

    /**
     * A machine recipe goes into a second interface the player names, so a second machine works it too; never twice
     * into one interface, and put into the first place with room, one the computer keeps anywhere is left as it is.
     */
    @GameTest(template = ARENA)
    public static void load_aRecipeGoesIntoASecondInterfaceThePlayerNames(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        for (int z = 3; z <= 4; z++) {
            CraftingRig.lay(world, new BlockPos(5, 2, z));
            CraftingRig.addPart(world, new CraftingFloor.Site(new BlockPos(5, 2, z), Direction.EAST),
                    new CraftingInterfacePart(HardwareEra.STANDARD));
        }
        final ItemStack dvd = new ItemStack(ComputingModule.DVD_RW.get());
        final String file = CraftFiles.writeProcessing(dvd, CraftFiles.furnaceIron(200),
                helper.getLevel().registryAccess());
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final CraftingComputerBlockEntity cc = net.cc();
                    cc.forgetFloor();
                    final List<Integer> interfaces = interfacePlaces(cc, helper.getLevel());
                    helper.assertTrue(interfaces.size() == 2, "two interfaces to load into: " + interfaces);
                    helper.assertTrue(load(helper, cc, dvd, file, interfaces.get(0)) == 1,
                            "the recipe goes into the first interface");
                    helper.assertTrue(load(helper, cc, dvd, file, -1) == 0,
                            "put into the first place with room, a recipe the computer keeps is left as it is");
                    helper.assertTrue(load(helper, cc, dvd, file, interfaces.get(1)) == 1,
                            "named, it goes into the second interface too");
                    helper.assertTrue(load(helper, cc, dvd, file, interfaces.get(1)) == 0,
                            "and never twice into one interface");
                })
                .thenSucceed();
    }

    /**
     * A recipe file renamed in the explorer to the longest name the filesystem allows, and a ROM pattern whose
     * result was renamed past the wire field, must still leave the manager's state sendable: an unsendable
     * payload disconnects the player on every refresh.
     */
    @GameTest(template = ARENA)
    public static void craftManagerState_fitsTheWireWithLongNames(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        world.setBlock(DVD_DRIVE, ComputingModule.DVD_DRIVE.get());
        final ItemStack dvd = new ItemStack(ComputingModule.DVD_RW.get());
        CraftFiles.writeBench(dvd, CraftFiles.oakPlanks(), helper.getLevel().registryAccess());

        // The explorer lets a player rename a file to anything up to the filesystem's limit.
        final String longName = "a".repeat(FsPaths.MAX_NAME_LENGTH - 6)
                + ".craft";
        String written = null;
        for (final DiskFilesystem.FileEntry entry
                : DiskFilesystem.list(dvd, "",
                        FilesystemKind.HIERARCHICAL)) {
            if (entry.type() == FileType.CRAFT) {
                written = entry.path();
            }
        }
        helper.assertTrue(written != null, "the craft is on the disc");
        helper.assertTrue(DiskFilesystem.rename(dvd, written, longName,
                        FilesystemKind.HIERARCHICAL),
                "the craft takes the longest name the filesystem allows");
        world.blockEntity(DVD_DRIVE, MediaReaderBlockEntity.class).mediaSlot().setStackInSlot(0, dvd);

        // A ROM pattern whose result carries a name longer than the wire field.
        final List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        grid.set(0, new ItemStack(Items.OAK_LOG));
        final ItemStack renamed = new ItemStack(Items.OAK_PLANKS, 4);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("n".repeat(70)));
        helper.assertTrue(net.cc().loadPattern(
                        new CraftingPattern(grid, renamed)),
                "the renamed pattern goes into the ROM");

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final CraftManagerStatePayload state =
                            CraftManagerPayloads.buildCraftManagerState(net.cc(), helper.getLevel());
                    final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                            Unpooled.buffer(), helper.getLevel().registryAccess());
                    CraftManagerStatePayload.STREAM_CODEC.encode(buf, state); // must not throw
                    final CraftManagerStatePayload decoded = CraftManagerStatePayload.STREAM_CODEC.decode(buf);
                    helper.assertTrue(decoded.mediaFiles().contains(longName),
                            "the long file name travels whole; got " + decoded.mediaFiles());
                    boolean whole = false;
                    for (final CraftManagerStatePayload.WireRomEntry entry : decoded.entries()) {
                        whole |= "n".repeat(70).equals(entry.name().english());
                    }
                    helper.assertTrue(whole, "the long result name travels whole; got " + decoded.entries());
                })
                .thenSucceed();
    }

    /**
     * Writing a recipe onto a disc that already holds a different one under the same name must not erase it:
     * the newcomer takes the next free suffix, and the same recipe written twice lands on its own file.
     */
    @GameTest(template = ARENA)
    public static void uniquePath_keepsARecipeAlreadyOnTheDiscUnderThatName(final GameTestHelper helper) {
        final ItemStack dvd = new ItemStack(ComputingModule.DVD_RW.get());
        final FilesystemKind kind =
                FilesystemKind.HIERARCHICAL;
        final String first = DiskFilesystem.uniquePath(
                dvd, "oak_planks", ".craft", "recipe A");
        helper.assertTrue(first.equals("oak_planks.craft"), "an empty disc takes the plain name; got " + first);
        helper.assertTrue(DiskFilesystem.write(dvd, first,
                        FileType.CRAFT, "recipe A", 1_000_000L, kind)
                        == DiskFilesystem.WriteResult.OK,
                "the first recipe is written");
        final String second = DiskFilesystem.uniquePath(
                dvd, "oak_planks", ".craft", "recipe B");
        helper.assertTrue(second.equals("oak_planks_2.craft"),
                "a different recipe with the same name takes the next suffix; got " + second);
        final String again = DiskFilesystem.uniquePath(
                dvd, "oak_planks", ".craft", "recipe A");
        helper.assertTrue(again.equals("oak_planks.craft"), "the same recipe lands on its own file; got " + again);
        helper.succeed();
    }

    /**
     * The Manager reads the computer's own disk as well as a disc: it lists the disk among the sources, walks into its
     * folders and back to the top for a folder that is not there, and a medium it does not read is answered with the
     * one the computer shows first.
     */
    @GameTest(template = ARENA)
    public static void listing_walksTheFoldersOfTheComputersOwnDisk(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final CraftingComputerBlockEntity cc = net.cc();
                    final ServerLevel level = helper.getLevel();
                    putRecipe(helper, cc.systemDisk(), FURNACE);
                    helper.assertTrue(DiskFilesystem.mkdir(cc.systemDisk(), "Recipes/Old", FilesystemKind.HIERARCHICAL),
                            "a second folder goes beside the first");
                    helper.assertTrue(CraftSources.sources(level, cc).stream().anyMatch(source -> source.usable()
                                    && source.key().equals(CraftManagerStatePayload.SYSTEM_DISK)),
                            "the computer's own disk is a source; got " + CraftSources.sources(level, cc));
                    final CraftSources.Listing recipes = CraftSources.listing(level, cc,
                            CraftManagerStatePayload.SYSTEM_DISK, "Recipes");
                    helper.assertTrue(recipes.key().equals(CraftManagerStatePayload.SYSTEM_DISK)
                                    && recipes.folder().equals("Recipes")
                                    && recipes.folders().equals(List.of("Furnace", "Old")) && recipes.files().isEmpty(),
                            "Recipes holds two folders and no file; got " + recipes);
                    final CraftSources.Listing furnace = CraftSources.listing(level, cc,
                            CraftManagerStatePayload.SYSTEM_DISK, FURNACE);
                    helper.assertTrue(furnace.files().equals(List.of("oak_planks.craft")),
                            "the furnace folder lists its recipe by name; got " + furnace.files());
                    helper.assertTrue(CraftSources.listing(level, cc, CraftManagerStatePayload.SYSTEM_DISK, "Gone")
                                    .folder().isEmpty(),
                            "a folder that is not there lists the disk's top");
                    helper.assertTrue(CraftSources.listing(level, cc, "media:12345", "").key().isEmpty(),
                            "a medium the computer does not read gives the computer's choice, here none");
                })
                .thenSucceed();
    }

    /** A recipe loads from a folder of the computer's own disk, and one downloaded goes into the folder open. */
    @GameTest(template = ARENA)
    public static void folder_aRecipeLoadsFromAFolderAndDownloadsIntoIt(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final CraftingComputerBlockEntity cc = net.cc();
                    final ServerLevel level = helper.getLevel();
                    final String name = putRecipe(helper, cc.systemDisk(), FURNACE);
                    final CraftSources.Volume disk = CraftSources.resolve(level, cc,
                            CraftManagerStatePayload.SYSTEM_DISK);
                    helper.assertTrue(disk != null, "the computer's own disk resolves");
                    helper.assertTrue(CraftManagerPayloads.loadFromMedia(cc, level, disk, FURNACE, List.of(name), -1,
                            false).loaded() == 1, "the recipe in the folder goes into the card's ROM");
                    helper.assertTrue(DiskFilesystem.mkdir(cc.systemDisk(), "Backup", FilesystemKind.HIERARCHICAL),
                            "a folder to download into");
                    final int ref = CraftManagerPayloads.buildCraftManagerState(cc, level).entries().getFirst().ref();
                    helper.assertTrue(CraftManagerPayloads.downloadTo(cc, level, disk, "Backup", List.of(ref)) == 1,
                            "one recipe is written");
                    helper.assertTrue(DiskFilesystem.exists(cc.systemDisk(), "Backup/oak_planks.craft"),
                            "it is written into the folder open, not at the disk's top");
                })
                .thenSucceed();
    }

    /* Writes the oak planks recipe into {@code folder} of {@code disk}, making the folder; returns the file's name. */
    private static String putRecipe(final GameTestHelper helper, final ItemStack disk, final String folder) {
        final ItemStack scratch = new ItemStack(ComputingModule.DVD_RW.get());
        final String file = CraftFiles.writeBench(scratch, CraftFiles.oakPlanks(), helper.getLevel().registryAccess());
        helper.assertTrue(DiskFilesystem.mkdirs(disk, folder, FilesystemKind.HIERARCHICAL), "the folder is made");
        helper.assertTrue(DiskFilesystem.write(disk, FsPaths.join(folder, file), FileType.CRAFT,
                        DiskFilesystem.read(scratch, file).orElseThrow(), 1_000_000L, FilesystemKind.HIERARCHICAL)
                        == DiskFilesystem.WriteResult.OK, "the recipe is written into the folder");
        return file;
    }

    /* How many recipes loading {@code file} from {@code media} into the place numbered {@code place} put in. */
    private static int load(final GameTestHelper helper, final CraftingComputerBlockEntity cc, final ItemStack media,
                            final String file, final int place) {
        return CraftManagerPayloads.loadFromMedia(cc, helper.getLevel(),
                new CraftSources.Volume("media:0", media, FilesystemKind.HIERARCHICAL), "", List.of(file), place,
                false).loaded();
    }

    /* The numbers of the places that are interfaces the computer drives, in order. */
    private static List<Integer> interfacePlaces(final CraftingComputerBlockEntity cc, final ServerLevel level) {
        final List<CraftPlaces.Place> all = CraftPlaces.of(cc, level).all();
        final List<Integer> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (!all.get(i).isCard()) {
                out.add(i);
            }
        }
        return out;
    }
}
