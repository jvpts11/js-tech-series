/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.block.ScannerBlock;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.printer.Printers;
import dev.jstech.computers.scan.ScannerModel;
import dev.jstech.computers.scan.Scans;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.jstechseries.peripheral.IPeripheralEndpoint;
import dev.jstech.core.jstechseries.peripheral.PeripheralCableType;
import dev.jstech.core.jstechseries.peripheral.PeripheralLink;
import dev.jstech.core.util.Loaded;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A flatbed scanner: a peripheral of a computer on the peripheral cable, like the printer, that reads what lies on its
 * glass into a file in the scans folder of the system at the cable's other end (computers/docs/design/peripherals.md).
 * Scan reads it a page at a time, at its era's pace; the item stays on the glass and nothing of it is used up.
 *
 * <p>The players who see it are sent whether it reads, how far it is, where the file goes, the last file it made and
 * what stopped it; the file itself is made on the server when the last page is read.
 */
public class ScannerBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    /* The glass takes one of the things a scanner reads. */
    private final FieldItemHandler glass = fields().items("Glass", 1).save().toClient().dropsWhenBroken()
            .slotLimit(1).accepts((index, stack) -> Scans.reads(stack));
    private final BoolField scanning = fields().flag("Scanning", false).save().toClient();
    /** How far into the current page the scanner is, in ticks. */
    private final IntField pageTicks = fields().integer("PageTicks", 0).save();
    /** How many pages of what lies on the glass are read. */
    private final IntField page = fields().integer("Page", 0).save().toClient();
    private final IntField scanned = fields().integer("Scanned", 0).save().toClient();
    private final ValueField<String> lastFile = fields().value("LastFile", Codec.STRING, "").save().toClient();
    /** The computer at the cable's other end, by the name its documents carry, and its scans folder. */
    private final ValueField<String> computer = fields().value("Computer", Codec.STRING, "").toClient();
    private final ValueField<String> folder = fields().value("Folder", Codec.STRING, "").toClient();
    private final ValueField<Problem> problem = fields().value("Problem", Problem.CODEC, Problem.NONE).toClient();
    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);

    private static final String FALLBACK_NAME = "scan";

    public ScannerBlockEntity(final BlockPos pos, final BlockState state) {
        super(MediaDevices.SCANNER_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ScannerBlockEntity scanner) {
        if (level instanceof ServerLevel server) {
            scanner.link.tick(server, pos);
            scanner.tickScan(server);
        }
    }

    /** The scanner this block is, by its era. */
    public ScannerModel model() {
        return getBlockState().getBlock() instanceof ScannerBlock block ? block.model()
                : ScannerModel.EPSILON_PERFECTA_V600;
    }

    /** Starts reading what lies on the glass from its first page; nothing happens with the glass empty. */
    public void scan() {
        if (level instanceof ServerLevel server) {
            refreshTarget(server);
        }
        if (glass.getStackInSlot(0).isEmpty()) {
            problem.set(Problem.NOTHING_ON_GLASS);
            return;
        }
        if (ownerPos() == null) {
            problem.set(Problem.NOT_LINKED);
            return;
        }
        problem.set(Problem.NONE);
        page.set(0);
        pageTicks.set(0);
        scanning.set(true);
    }

    /** Stops reading; what was read is let go, and nothing is written. */
    public void stop() {
        scanning.set(false);
        page.set(0);
        pageTicks.set(0);
    }

    /** Reads again where the file would go: the linked computer's name and its scans folder, or nothing. */
    public void refreshTarget(final ServerLevel server) {
        final IOsHost host = host(server);
        if (host == null) {
            computer.set("");
            folder.set("");
            return;
        }
        computer.set(Printers.machineName(host));
        final OsDef os = host.installedOs();
        folder.set(os == null ? "" : SystemLayout.scansDirFor(os, OsRegistry.getKernel(os.kernelId())));
    }

    public ItemStackHandler glass() {
        return glass;
    }

    /** What lies on the glass. */
    public ItemStack onGlass() {
        return glass.getStackInSlot(0);
    }

    /** Whether it is reading now. */
    public boolean scanning() {
        return scanning.get();
    }

    /** How many pages of what lies on the glass are read. */
    public int page() {
        return page.get();
    }

    /** How many pages what lies on the glass has. */
    public int pages() {
        return Scans.pages(onGlass());
    }

    /** How many files it has made. */
    public int scanned() {
        return scanned.get();
    }

    /** The name of the last file it made, or empty. */
    public String lastFile() {
        return lastFile.get();
    }

    /** The name the linked computer goes by, or empty while it is not linked to one with a system. */
    public String computer() {
        return computer.get();
    }

    /** Where on the linked computer the file goes, or empty. */
    public String folder() {
        return folder.get();
    }

    /** What stopped the last scan, {@link Problem#NONE} when nothing did. */
    public Problem problem() {
        return problem.get();
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
        if (level instanceof ServerLevel server) {
            refreshTarget(server);
        }
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
        computer.set("");
        folder.set("");
    }

    /** The linked computer's position, or null while unlinked. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /* A page at a time while it reads; the file is made when the last page is read. */
    private void tickScan(final ServerLevel server) {
        if (!scanning.get()) {
            return;
        }
        final ItemStack item = onGlass();
        if (item.isEmpty()) {
            stop();
            problem.set(Problem.NOTHING_ON_GLASS);
            return;
        }
        pageTicks.set(pageTicks.get() + 1);
        if (pageTicks.get() < model().pageTicks()) {
            return;
        }
        pageTicks.set(0);
        page.set(page.get() + 1);
        if (page.get() < Scans.pages(item)) {
            return;
        }
        scanning.set(false);
        page.set(0);
        problem.set(save(server, item));
    }

    /* Makes the file of {@code item} in the linked computer's scans folder; what went wrong, if anything did. */
    private Problem save(final ServerLevel server, final ItemStack item) {
        final IOsHost host = host(server);
        if (host == null) {
            return Problem.NOT_LINKED;
        }
        final Scans.Scan scan = Scans.read(server, item).orElse(null);
        final ItemStack disk = host.systemDisk();
        final FilesystemKind kind = FileAccess.filesystemKindOf(host);
        final OsDef os = host.installedOs();
        if (scan == null || disk.isEmpty() || os == null || kind != FilesystemKind.HIERARCHICAL) {
            return Problem.NO_SYSTEM;
        }
        refreshTarget(server);
        final String dir = folder.get();
        if (!DiskFilesystem.mkdirs(disk, dir, kind)) {
            return Problem.NOT_SAVED;
        }
        final String path = DiskFilesystem.uniquePath(disk, FsPaths.join(dir,
                RecordingFile.stemOf(scan.name(), FALLBACK_NAME)), "." + scan.type().extension(), scan.content());
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, path, scan.type(), scan.content(),
                host.systemDiskFreeWeight() + DiskFilesystem.weightOf(disk, path), kind, server.getGameTime());
        host.setChanged();
        if (result != DiskFilesystem.WriteResult.OK) {
            return result == DiskFilesystem.WriteResult.DISK_FULL ? Problem.DISK_FULL : Problem.NOT_SAVED;
        }
        scanned.set(scanned.get() + 1);
        lastFile.set(FsPaths.fileName(path));
        return Problem.NONE;
    }

    /* The computer at the cable's other end, or null. */
    @Nullable
    private IOsHost host(final ServerLevel server) {
        final BlockPos owner = ownerPos();
        return owner != null && Loaded.at(server, owner) && server.getBlockEntity(owner) instanceof IOsHost host
                ? host : null;
    }

    /** What stopped a scan, as its window says it. */
    public enum Problem implements IStableId {
        NONE(0),
        NOTHING_ON_GLASS(1),
        NOT_LINKED(2),
        NO_SYSTEM(3),
        DISK_FULL(4),
        NOT_SAVED(5);

        private final int id;

        private static final StableIds<Problem> IDS = StableIds.of(Problem.class);
        /** A problem sent as the byte of its id; an id no problem declares reads as {@link #NONE}. */
        private static final Codec<Problem> CODEC = Codec.BYTE.xmap(b -> IDS.byId(b, NONE), p -> (byte) p.id());

        Problem(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }
    }
}
