/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Reads an FBX file into its records, in either of its forms: the binary one Blender, Maya and 3ds Max write by
 * default, its arrays packed with zlib or not, and the text one they write when asked. Only version 7 and later,
 * from 2010 on, the one every tool of today writes; the records of version 6 name their objects differently.
 */
final class FbxParser {

    private final String name;
    private final ByteBuffer file;
    private final boolean longRecords;

    private static final byte[] BINARY_MAGIC = "Kaydara FBX Binary  ".getBytes(StandardCharsets.US_ASCII);
    private static final int FIRST_RECORD = 27;
    private static final int LONG_RECORDS = 7500;
    private static final int OLDEST = 7000;

    private FbxParser(final String name, final ByteBuffer file, final int version) {
        this.name = name;
        this.file = file;
        this.longRecords = version >= LONG_RECORDS;
    }

    /** The records at the top of the file, in their order, and the version it says it is. */
    static Document parse(final String name, final byte[] bytes) {
        if (binary(bytes)) {
            final ByteBuffer file = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            final int version = file.getInt(23);
            old(name, version);
            final FbxParser parser = new FbxParser(name, file, version);
            final List<FbxNode> top = new ArrayList<>();
            int at = FIRST_RECORD;
            while (at < bytes.length) {
                final Read record = parser.record(at);
                if (record == null) {
                    break;
                }
                top.add(record.node);
                at = record.end;
            }
            return new Document(version, new FbxNode("", List.of(), top));
        }
        final List<FbxNode> top = new FbxText(name, new String(bytes, StandardCharsets.UTF_8)).nodes();
        int version = OLDEST;
        for (final FbxNode node : top) {
            if (node.name().equals("FBXHeaderExtension") && node.child("FBXVersion") != null) {
                version = (int) node.child("FBXVersion").whole(0);
            }
        }
        old(name, version);
        return new Document(version, new FbxNode("", List.of(), top));
    }

    private static boolean binary(final byte[] bytes) {
        if (bytes.length < FIRST_RECORD) {
            return false;
        }
        for (int i = 0; i < BINARY_MAGIC.length; i++) {
            if (bytes[i] != BINARY_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static void old(final String name, final int version) {
        if (version < OLDEST) {
            throw new ModelFormatException("the FBX file " + name + " is of version " + version
                    + "; export it as FBX 7.4 or later");
        }
    }

    /* The record at {@code at}, and where the next begins; none for the empty record that ends a list. */
    private Read record(final int at) {
        int cursor = at;
        final long end = this.longRecords ? this.file.getLong(cursor) : Integer.toUnsignedLong(this.file.getInt(cursor));
        cursor += this.longRecords ? 8 : 4;
        final long count = this.longRecords ? this.file.getLong(cursor) : Integer.toUnsignedLong(
                this.file.getInt(cursor));
        cursor += this.longRecords ? 8 : 4;
        cursor += this.longRecords ? 8 : 4;
        final int nameLength = this.file.get(cursor) & 0xFF;
        cursor++;
        if (end == 0) {
            return null;
        }
        if (end > this.file.limit() || end <= at) {
            throw new ModelFormatException("the FBX file " + this.name + " has a record past its end");
        }
        final byte[] nameBytes = new byte[nameLength];
        this.file.get(cursor, nameBytes);
        cursor += nameLength;
        final List<Object> properties = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            cursor = property(cursor, properties);
        }
        final List<FbxNode> children = new ArrayList<>();
        while (cursor < end) {
            final Read child = record(cursor);
            if (child == null) {
                cursor += this.longRecords ? 25 : 13;
                break;
            }
            children.add(child.node);
            cursor = child.end;
        }
        return new Read(new FbxNode(new String(nameBytes, StandardCharsets.UTF_8), properties, children), (int) end);
    }

    /* Reads the property at {@code at} into {@code out}; where the next begins. */
    private int property(final int at, final List<Object> out) {
        final char type = (char) this.file.get(at);
        int cursor = at + 1;
        switch (type) {
            case 'Y' -> {
                out.add((long) this.file.getShort(cursor));
                cursor += 2;
            }
            case 'C' -> {
                out.add(this.file.get(cursor) != 0);
                cursor += 1;
            }
            case 'I' -> {
                out.add((long) this.file.getInt(cursor));
                cursor += 4;
            }
            case 'F' -> {
                out.add((double) this.file.getFloat(cursor));
                cursor += 4;
            }
            case 'D' -> {
                out.add(this.file.getDouble(cursor));
                cursor += 8;
            }
            case 'L' -> {
                out.add(this.file.getLong(cursor));
                cursor += 8;
            }
            case 'S', 'R' -> {
                final int length = this.file.getInt(cursor);
                cursor += 4;
                final byte[] bytes = new byte[length];
                this.file.get(cursor, bytes);
                cursor += length;
                out.add(type == 'S' ? new String(bytes, StandardCharsets.UTF_8) : bytes);
            }
            case 'f', 'd', 'l', 'i', 'b' -> cursor = array(type, cursor, out);
            default -> throw new ModelFormatException("the FBX file " + this.name + " has a property of type "
                    + type + ", which FBX has none of");
        }
        return cursor;
    }

    private int array(final char type, final int at, final List<Object> out) {
        final int length = this.file.getInt(at);
        final int encoding = this.file.getInt(at + 4);
        final int stored = this.file.getInt(at + 8);
        final int size = switch (type) {
            case 'd', 'l' -> 8;
            case 'b' -> 1;
            default -> 4;
        };
        final byte[] raw = new byte[stored];
        this.file.get(at + 12, raw);
        final ByteBuffer data = ByteBuffer.wrap(encoding == 1 ? inflate(raw, length * size) : raw)
                .order(ByteOrder.LITTLE_ENDIAN);
        if (type == 'f' || type == 'd') {
            final double[] values = new double[length];
            for (int i = 0; i < length; i++) {
                values[i] = type == 'f' ? data.getFloat(i * 4) : data.getDouble(i * 8);
            }
            out.add(values);
        } else {
            final long[] values = new long[length];
            for (int i = 0; i < length; i++) {
                values[i] = switch (type) {
                    case 'l' -> data.getLong(i * 8);
                    case 'b' -> data.get(i);
                    default -> data.getInt(i * 4);
                };
            }
            out.add(values);
        }
        return at + 12 + stored;
    }

    private byte[] inflate(final byte[] packed, final int size) {
        final Inflater inflater = new Inflater();
        try {
            inflater.setInput(packed);
            final byte[] out = new byte[size];
            int done = 0;
            while (done < size && !inflater.finished()) {
                final int read = inflater.inflate(out, done, size - done);
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                done += read;
            }
            return out;
        } catch (final DataFormatException broken) {
            throw new ModelFormatException("the FBX file " + this.name + " has an array that does not unpack: "
                    + broken.getMessage());
        } finally {
            inflater.end();
        }
    }

    /**
     * An FBX file's records.
     *
     * @param version the version it says it is, as 7400 for 7.4
     * @param root    a record holding the file's top records
     */
    record Document(int version, FbxNode root) {
    }

    private record Read(FbxNode node, int end) {
    }
}
