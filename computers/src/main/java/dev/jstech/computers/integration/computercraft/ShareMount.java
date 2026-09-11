/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.filesystem.WritableMount;
import dev.jstech.computers.gateway.GatewayService;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * A folder one of our computers shares, as a ComputerCraft computer sees it through a Gateway: mounted at
 * {@code /jsc/<computer>/<share>/}, read through the host computer's shell by the same network path the
 * host's own prompt uses, so the sharing computer's rules (a read-only share, a file type it does not
 * take, a disk that is full) are the ones that answer. Our files are text, so a file written from the
 * other side is kept as text, and its bytes weigh what any file on that computer weighs.
 */
public final class ShareMount implements WritableMount {

    private final GatewayService service;
    private final ServerCliComputer shell;
    private final String root;
    private final boolean writable;

    ShareMount(final GatewayService service, final String hostname, final String share, final boolean writable) {
        this.service = service;
        this.shell = service.shell();
        this.root = "\\\\" + hostname + "\\" + share;
        this.writable = writable;
    }

    public boolean writable() {
        return writable;
    }

    // Reading

    @Override
    public boolean exists(final String path) throws IOException {
        return isRoot(path) || entryOf(path) != null;
    }

    @Override
    public boolean isDirectory(final String path) throws IOException {
        if (isRoot(path)) {
            return true;
        }
        final ICliComputer.FsEntry entry = entryOf(path);
        return entry != null && entry.isDir();
    }

    @Override
    public void list(final String path, final List<String> contents) throws IOException {
        if (!isDirectory(path)) {
            throw new FileOperationException(path, exists(path) ? MountConstants.NOT_A_DIRECTORY : MountConstants.NO_SUCH_FILE);
        }
        final ICliComputer.FsResult listing = shell.listDisk(unc(path));
        if (!listing.ok() || listing.entries() == null) {
            throw new FileOperationException(path, listing.message());
        }
        for (final ICliComputer.FsEntry entry : listing.entries()) {
            contents.add(entry.name());
        }
        service.fileRead();
    }

    @Override
    public long getSize(final String path) throws IOException {
        if (isDirectory(path)) {
            return 0L;
        }
        return bytesOf(path).length;
    }

    @Override
    public SeekableByteChannel openForRead(final String path) throws IOException {
        if (isDirectory(path)) {
            throw new FileOperationException(path, MountConstants.NOT_A_FILE);
        }
        return new ReadChannel(bytesOf(path));
    }

    // Writing

    @Override
    public boolean isReadOnly(final String path) {
        return !writable;
    }

    @Override
    public void makeDirectory(final String path) throws IOException {
        requireWritable(path);
        if (isDirectory(path)) {
            return;
        }
        if (exists(path)) {
            throw new FileOperationException(path, MountConstants.FILE_EXISTS);
        }
        final ICliComputer.FsResult made = shell.makeDir(unc(path));
        if (!made.ok()) {
            throw new FileOperationException(path, made.message());
        }
        service.fileWritten();
    }

    @Override
    public void delete(final String path) throws IOException {
        requireWritable(path);
        if (isRoot(path)) {
            throw new FileOperationException(path, MountConstants.ACCESS_DENIED);
        }
        if (!exists(path)) {
            return;
        }
        final ICliComputer.FsResult gone = shell.deleteFile(unc(path));
        if (!gone.ok()) {
            throw new FileOperationException(path, gone.message());
        }
        service.fileWritten();
    }

    /**
     * Renames a file by copying it and deleting the original, the two operations the other machine's
     * shell offers across the network; a folder cannot be renamed this way and says so.
     */
    @Override
    public void rename(final String source, final String dest) throws IOException {
        requireWritable(source);
        final ICliComputer.FsEntry from = entryOf(source);
        if (from == null) {
            throw new FileOperationException(source, MountConstants.NO_SUCH_FILE);
        }
        if (from.isDir()) {
            throw new FileOperationException(source, "Cannot rename a folder through a Gateway");
        }
        if (exists(dest)) {
            throw new FileOperationException(dest, MountConstants.FILE_EXISTS);
        }
        final byte[] content = bytesOf(source);
        write(dest, content);
        final ICliComputer.FsResult gone = shell.deleteFile(unc(source));
        if (!gone.ok()) {
            throw new FileOperationException(source, gone.message());
        }
    }

    @Override
    public SeekableByteChannel openFile(final String path, final Set<OpenOption> options) throws IOException {
        final boolean writes = options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND);
        if (!writes) {
            return openForRead(path);
        }
        requireWritable(path);
        if (isDirectory(path)) {
            throw new FileOperationException(path, MountConstants.CANNOT_WRITE_TO_DIRECTORY);
        }
        final byte[] existing = options.contains(StandardOpenOption.APPEND) && exists(path) ? bytesOf(path) : new byte[0];
        return new WriteChannel(existing, content -> write(path, content));
    }

    @Override
    public long getRemainingSpace() {
        return shell.freeBytes(root);
    }

    @Override
    public long getCapacity() {
        return shell.capacityBytes(root);
    }

    // Helpers

    private static boolean isRoot(final String path) {
        return clean(path).isEmpty();
    }

    private static String clean(final String path) {
        String p = path == null ? "" : path.replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /** The network path the host's shell reaches this file by. */
    private String unc(final String path) {
        final String p = clean(path);
        return p.isEmpty() ? root : root + "\\" + p.replace('/', '\\');
    }

    /** The listing entry for {@code path}, from its parent folder, or null when there is none. */
    @Nullable
    private ICliComputer.FsEntry entryOf(final String path) throws IOException {
        final String p = clean(path);
        if (p.isEmpty()) {
            return null;
        }
        final int slash = p.lastIndexOf('/');
        final String parent = slash < 0 ? "" : p.substring(0, slash);
        final String name = slash < 0 ? p : p.substring(slash + 1);
        final ICliComputer.FsResult listing = shell.listDisk(unc(parent));
        if (!listing.ok() || listing.entries() == null) {
            if (parent.isEmpty()) {
                throw new FileOperationException(path, listing.message());
            }
            return null;
        }
        for (final ICliComputer.FsEntry entry : listing.entries()) {
            if (entry.name().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }

    private byte[] bytesOf(final String path) throws IOException {
        final ICliComputer.FsResult read = shell.readFile(unc(path));
        if (!read.ok()) {
            throw new FileOperationException(path, read.message() == null ? MountConstants.NO_SUCH_FILE : read.message());
        }
        service.fileRead();
        return read.message().getBytes(StandardCharsets.UTF_8);
    }

    private void write(final String path, final byte[] content) throws IOException {
        final ICliComputer.FsResult written = shell.writeFile(unc(path), new String(content, StandardCharsets.UTF_8));
        if (!written.ok()) {
            throw new FileOperationException(path, written.message());
        }
        service.fileWritten();
    }

    private void requireWritable(final String path) throws IOException {
        if (!writable) {
            throw new FileOperationException(path, MountConstants.ACCESS_DENIED);
        }
    }

    @Override
    public String toString() {
        return "ShareMount[" + root.toLowerCase(Locale.ROOT) + (writable ? ", writable]" : "]");
    }

    /** A file's bytes, read from where the cursor stands. */
    private static final class ReadChannel implements SeekableByteChannel {

        private final byte[] bytes;
        private long position;
        private boolean open = true;

        private ReadChannel(final byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int read(final ByteBuffer into) {
            if (position >= bytes.length) {
                return -1;
            }
            final int count = (int) Math.min(into.remaining(), bytes.length - position);
            into.put(bytes, (int) position, count);
            position += count;
            return count;
        }

        @Override
        public int write(final ByteBuffer from) {
            throw new NonWritableChannelException();
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public SeekableByteChannel position(final long newPosition) {
            position = Math.max(0L, newPosition);
            return this;
        }

        @Override
        public long size() {
            return bytes.length;
        }

        @Override
        public SeekableByteChannel truncate(final long size) {
            throw new NonWritableChannelException();
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }
    }

    /** Bytes gathered while a file is open for writing, handed over as one file when it is closed. */
    private static final class WriteChannel implements SeekableByteChannel {

        private interface IFlush {
            void flush(byte[] content) throws IOException;
        }

        private byte[] bytes;
        private int length;
        private long position;
        private boolean open = true;
        private final IFlush flush;

        private WriteChannel(final byte[] existing, final IFlush flush) {
            this.bytes = Arrays.copyOf(existing, Math.max(64, existing.length));
            this.length = existing.length;
            this.position = existing.length;
            this.flush = flush;
        }

        @Override
        public int read(final ByteBuffer into) {
            throw new NonReadableChannelException();
        }

        @Override
        public int write(final ByteBuffer from) {
            final int count = from.remaining();
            final int end = (int) position + count;
            if (end > bytes.length) {
                bytes = Arrays.copyOf(bytes, Math.max(end, bytes.length * 2));
            }
            from.get(bytes, (int) position, count);
            position = end;
            length = Math.max(length, end);
            return count;
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public SeekableByteChannel position(final long newPosition) {
            position = Math.max(0L, Math.min(newPosition, length));
            return this;
        }

        @Override
        public long size() {
            return length;
        }

        @Override
        public SeekableByteChannel truncate(final long size) {
            if (size < length) {
                length = (int) size;
                position = Math.min(position, length);
            }
            return this;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            if (!open) {
                return;
            }
            open = false;
            flush.flush(Arrays.copyOf(bytes, length));
        }
    }
}
