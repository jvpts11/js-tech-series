/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The removable media in real bytes: what each holds, weighed at its format's era; files filling a floppy disk to the
 * byte the weights allow; the tooltips' exact bytes and what is free; the four USB sticks by model; a copy needing a
 * blank as big as its source; and what a machine and a Dock Station say of a disk newer than themselves.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MediaCapacityGameTests {

    private static final String ARENA = "empty";
    /* The longest file there is, which weighs 32 thousandths of an item on a floppy disk's Vintage terms. */
    private static final String LONGEST = "x".repeat(32_767);

    private MediaCapacityGameTests() {
    }

    @GameTest(template = ARENA)
    public static void capacity_isRealBytesWeighedAtTheFormatsEra(final GameTestHelper helper) {
        final FormattedMediaItem floppy = ComputingModule.FLOPPY_DISK.get();
        helper.assertTrue(floppy.capacityBytes() == 1_474_560L && floppy.capacityWeight() == 1_406L,
                "a floppy disk holds 1,474,560 bytes, 1.406 items on Vintage terms; got " + floppy.capacityWeight());
        helper.assertTrue(ComputingModule.CD_RW.get().capacityBytes() == 737_280_000L
                        && ComputingModule.DVD_RW.get().capacityBytes() == 4_700_372_992L
                        && ComputingModule.BD_RE.get().capacityBytes() == 25_025_314_816L,
                "a CD holds 700 MB, a DVD 4.7 GB and a Blu-ray 25 GB");
        final long[] sticks = {8_000_000_000L, 16_000_000_000L, 32_000_000_000L, 64_000_000_000L};
        final List<FormattedMediaItem> models = List.of(ComputingModule.USB_FLASH_DRIVE_8G.get(),
                ComputingModule.USB_FLASH_DRIVE_16G.get(), ComputingModule.USB_FLASH_DRIVE.get(),
                ComputingModule.USB_FLASH_DRIVE_64G.get());
        for (int i = 0; i < sticks.length; i++) {
            final FormattedMediaItem stick = models.get(i);
            helper.assertTrue(stick.capacityBytes() == sticks[i] && stick.capacityWeight()
                            == EraStorage.weightHeldBy(HardwareEra.STANDARD, sticks[i]),
                    "the stick of " + sticks[i] + " bytes holds them on a stick's terms");
        }
        helper.assertTrue(ComputingModule.USB_FLASH_DRIVE_64G.get().era() == HardwareEra.ADVANCED
                        && ComputingModule.USB_FLASH_DRIVE_8G.get().era() == HardwareEra.STANDARD,
                "the 64G is of the Advanced and the others of the Standard");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void floppyDisk_holdsTheFilesItsBytesAllow(final GameTestHelper helper) {
        final ItemStack floppy = new ItemStack(ComputingModule.FLOPPY_DISK.get());
        helper.assertTrue(write(floppy, 0) == DiskFilesystem.WriteResult.OK
                        && FormattedMediaItem.freeWeight(floppy) == 1_406L - 32L,
                "a file of 32,767 bytes takes 32 thousandths of an item; free "
                        + FormattedMediaItem.freeWeight(floppy));
        int written = 1;
        while (write(floppy, written) == DiskFilesystem.WriteResult.OK) {
            written++;
        }
        helper.assertTrue(written == 43, "43 of the longest files fill a floppy disk; got " + written);
        helper.assertTrue(write(floppy, written) == DiskFilesystem.WriteResult.DISK_FULL,
                "the next one finds it full");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tooltips_sayTheBytesAndWhatIsFree(final GameTestHelper helper) {
        final ItemStack floppy = new ItemStack(ComputingModule.FLOPPY_DISK.get());
        assertSays(helper, floppy, "1.44 MB (1,474,560 bytes)");
        assertSays(helper, floppy, "Free: all of it");
        write(floppy, 0);
        assertSays(helper, floppy, "Free: 1.3 MB");
        final ItemStack pressed = new ItemStack(ComputingModule.CD_ROM.get());
        assertSays(helper, pressed, "700 MB (737,280,000 bytes)");
        helper.assertFalse(said(helper, pressed).stream().anyMatch(line -> line.startsWith("Free:")),
                "a pressed disc says nothing of free room, as nothing is written on it");
        final ItemStack stick = new ItemStack(ComputingModule.USB_FLASH_DRIVE_16G.get());
        assertSays(helper, stick, "Vaultis Pocket 16G");
        assertSays(helper, stick, "16 GB (16,000,000,000 bytes)");
        assertSays(helper, stick, "USB 2.0, Standard era");
        assertSays(helper, new ItemStack(ComputingModule.USB_FLASH_DRIVE_64G.get()), "USB-C, Advanced era");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void copies_needABlankAtLeastAsBigAsTheSource(final GameTestHelper helper) {
        final ItemStack big = new ItemStack(ComputingModule.USB_FLASH_DRIVE_64G.get());
        write(big, 0);
        final ItemStack small = new ItemStack(ComputingModule.USB_FLASH_DRIVE_8G.get());
        final Text refused = DiscCopies.refusal(place('D', big), place('E', small), false);
        helper.assertTrue(refused != null && refused.english().contains("smaller"),
                "a 64G stick does not copy onto an 8G one; got " + (refused == null ? "nothing" : refused.english()));
        final ItemStack full = new ItemStack(ComputingModule.USB_FLASH_DRIVE_8G.get());
        write(full, 0);
        helper.assertTrue(DiscCopies.refusal(place('D', full), place('E', new ItemStack(
                        ComputingModule.USB_FLASH_DRIVE_64G.get())), false) == null,
                "an 8G stick copies onto a 64G one");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tooNew_namesTheDiskAndTheEraItNeeds(final GameTestHelper helper) {
        final ItemStack nvme = new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1));
        final List<String> computer = strings(PartRefusals.lines(PartRefusals.DISK_TOO_NEW, nvme, false));
        helper.assertTrue(computer.equals(List.of(nvme.getHoverName().getString(), "Too new for this computer:",
                        "a Standard disk needs a Standard computer or a newer one")),
                "a computer names the disk, says it is too new and the era it asks for; got " + computer);
        final List<String> server = strings(PartRefusals.lines(PartRefusals.DISK_TOO_NEW, nvme, true));
        helper.assertTrue(server.get(1).equals("Too new for this server:")
                        && server.get(2).equals("a Standard disk needs a Standard server or a newer one"),
                "a server says it of itself; got " + server);
        helper.assertTrue(strings(PartRefusals.lines(PartRefusals.CPU, nvme, false))
                        .equals(List.of(PartRefusals.CPU.english())),
                "any other refusal is its one sentence");
        final ItemStack advanced = new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_4));
        final List<String> dock = strings(PartRefusals.dockTooNew(advanced, MediaDriveType.DOCK_STATION.era()));
        helper.assertTrue(dock.equals(List.of(advanced.getHoverName().getString(), "Too new for this Dock Station:",
                        "a Standard dock takes disks of its era or older")),
                "a Dock Station says its own era; got " + dock);
        helper.succeed();
    }

    private static DiskFilesystem.WriteResult write(final ItemStack medium, final int index) {
        return DiskFilesystem.write(medium, "f" + index + ".txt", FileType.TXT, LONGEST,
                FormattedMediaItem.freeWeight(medium), FilesystemKind.HIERARCHICAL);
    }

    private static DiscCopies.Place place(final char letter, final ItemStack medium) {
        return new DiscCopies.Place(letter, letter, MediaDriveType.DOCK_STATION, medium, false);
    }

    private static List<String> said(final GameTestHelper helper, final ItemStack stack) {
        return strings(stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL));
    }

    private static void assertSays(final GameTestHelper helper, final ItemStack stack, final String line) {
        final List<String> lines = said(helper, stack);
        helper.assertTrue(lines.contains(line), stack.getHoverName().getString() + " says '" + line + "'; got "
                + lines);
    }

    private static List<String> strings(final List<Component> lines) {
        return lines.stream().map(Component::getString).toList();
    }
}
