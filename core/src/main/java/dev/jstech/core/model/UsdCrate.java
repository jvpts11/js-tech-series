/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Reads a USD layer written in binary, the crate format of {@code .usdc}, as Pixar's library writes it: its tokens,
 * strings, fields, field sets, paths and specs, each table as the format packs it (LZ4 blocks, integers stored as their
 * differences in as few bytes as each takes, paths as a tree of jumps), and each value as it is kept (inlined in its
 * reference, written out, an array plain or packed, a list of paths, samples over time). What a model needs is read;
 * dictionaries and the composition of other layers are let go. Pure.
 */
final class UsdCrate {

    private final String name;
    private final ByteBuffer data;
    private final int version;
    private String[] tokens = new String[0];
    private int[] strings = new int[0];
    private int[] fieldNames = new int[0];
    private long[] fieldReps = new long[0];
    private int[] fieldSets = new int[0];
    private String[] paths = new String[0];

    private static final String MAGIC = "PXR-USDC";
    private static final int SECTION_NAME = 16;
    private static final long ARRAY = 1L << 63;
    private static final long INLINED = 1L << 62;
    private static final long COMPRESSED = 1L << 61;
    private static final long PAYLOAD = (1L << 48) - 1;
    private static final int TYPE_SHIFT = 48;
    private static final int END_OF_SET = -1;
    /* Above this many of anything the file is taken to be broken, not a model. */
    private static final long MOST = 1L << 28;
    private static final int PRIM = 6;
    private static final int PSEUDO_ROOT = 7;
    private static final int ATTRIBUTE = 1;
    private static final int RELATIONSHIP = 8;

    private UsdCrate(final String name, final byte[] bytes) {
        this.name = name;
        this.data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        this.version = (bytes[8] & 0xFF) << 16 | (bytes[9] & 0xFF) << 8 | bytes[10] & 0xFF;
    }

    /** Whether {@code bytes} begin as a crate file does. */
    static boolean isCrate(final byte[] bytes) {
        return bytes.length > 16 && new String(bytes, 0, MAGIC.length(), StandardCharsets.US_ASCII).equals(MAGIC);
    }

    /**
     * The layer a crate file holds, as the tree of its prims under its root, the root carrying the layer's metadata.
     *
     * @throws ModelFormatException when it is not one, or a part of it does not read
     */
    static UsdPrim read(final String name, final byte[] bytes) {
        if (!isCrate(bytes)) {
            throw new ModelFormatException("the file " + name + " is not a USD crate");
        }
        try {
            return new UsdCrate(name, bytes).layer();
        } catch (final IndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException broken) {
            throw new ModelFormatException("the USD crate " + name + " does not read: " + broken, broken);
        }
    }

    private UsdPrim layer() {
        final long toc = this.data.getLong(16);
        final long count = this.data.getLong((int) toc);
        final Map<String, Integer> sections = new HashMap<>();
        for (int i = 0; i < count; i++) {
            final int at = (int) toc + 8 + i * (SECTION_NAME + 16);
            final byte[] written = new byte[SECTION_NAME];
            this.data.get(at, written);
            int end = 0;
            while (end < written.length && written[end] != 0) {
                end++;
            }
            sections.put(new String(written, 0, end, StandardCharsets.US_ASCII), (int) this.data.getLong(at
                    + SECTION_NAME));
        }
        readTokens(require(sections, "TOKENS"));
        readStrings(require(sections, "STRINGS"));
        readFields(require(sections, "FIELDS"));
        readFieldSets(require(sections, "FIELDSETS"));
        readPaths(require(sections, "PATHS"));
        return specs(require(sections, "SPECS"));
    }

    private int require(final Map<String, Integer> sections, final String wanted) {
        final Integer at = sections.get(wanted);
        if (at == null) {
            throw new ModelFormatException("the USD crate " + this.name + " has no " + wanted + " section");
        }
        return at;
    }

    private void readTokens(final int at) {
        final Cursor in = new Cursor(at);
        final long count = check(in.u64());
        final byte[] raw;
        if (this.version < 0x000400) {
            final long size = check(in.u64());
            raw = in.bytes((int) size);
        } else {
            final long size = check(in.u64());
            final long packed = check(in.u64());
            raw = Lz4.decompress(in.bytes((int) packed), (int) size);
        }
        this.tokens = new String[(int) count];
        int start = 0;
        int index = 0;
        for (int i = 0; i < raw.length && index < count; i++) {
            if (raw[i] == 0) {
                this.tokens[index++] = new String(raw, start, i - start, StandardCharsets.UTF_8);
                start = i + 1;
            }
        }
        while (index < count) {
            this.tokens[index++] = "";
        }
    }

    private void readStrings(final int at) {
        final Cursor in = new Cursor(at);
        final long count = check(in.u64());
        this.strings = new int[(int) count];
        for (int i = 0; i < count; i++) {
            this.strings[i] = in.u32();
        }
    }

    private void readFields(final int at) {
        final Cursor in = new Cursor(at);
        final long count = check(in.u64());
        if (this.version < 0x000400) {
            this.fieldNames = new int[(int) count];
            this.fieldReps = new long[(int) count];
            for (int i = 0; i < count; i++) {
                this.fieldNames[i] = in.u32();
                in.at += 4;
                this.fieldReps[i] = in.u64();
            }
            return;
        }
        this.fieldNames = in.ints((int) count);
        final long packed = check(in.u64());
        final ByteBuffer reps = ByteBuffer.wrap(Lz4.decompress(in.bytes((int) packed), (int) count * 8))
                .order(ByteOrder.LITTLE_ENDIAN);
        this.fieldReps = new long[(int) count];
        for (int i = 0; i < count; i++) {
            this.fieldReps[i] = reps.getLong(i * 8);
        }
    }

    private void readFieldSets(final int at) {
        final Cursor in = new Cursor(at);
        final long count = check(in.u64());
        if (this.version < 0x000400) {
            this.fieldSets = new int[(int) count];
            for (int i = 0; i < count; i++) {
                this.fieldSets[i] = in.u32();
            }
            return;
        }
        this.fieldSets = in.ints((int) count);
    }

    private void readPaths(final int at) {
        final Cursor in = new Cursor(at);
        final long count = check(in.u64());
        this.paths = new String[(int) count];
        if (this.version < 0x000400) {
            throw new ModelFormatException("the USD crate " + this.name + " is of a version too old to read, "
                    + "before 0.4.0");
        }
        final int encoded = (int) check(in.u64());
        final int[] indexes = in.ints(encoded);
        final int[] elements = in.ints(encoded);
        final int[] jumps = in.ints(encoded);
        build(indexes, elements, jumps, 0, null);
    }

    /* The paths from their tree: each a child of the one before, or a sibling a jump ahead. */
    private void build(final int[] indexes, final int[] elements, final int[] jumps, final int from,
                       @Nullable final String start) {
        String parent = start;
        int at = from;
        boolean more = true;
        while (more) {
            final int self = at++;
            if (parent == null) {
                parent = "/";
                this.paths[indexes[self]] = parent;
            } else {
                final int element = elements[self];
                final String token = this.tokens[Math.abs(element)];
                this.paths[indexes[self]] = element < 0 ? parent + "." + token
                        : (parent.equals("/") ? "/" : parent + "/") + token;
            }
            final boolean child = jumps[self] > 0 || jumps[self] == -1;
            final boolean sibling = jumps[self] >= 0;
            if (child) {
                if (sibling) {
                    build(indexes, elements, jumps, self + jumps[self], parent);
                }
                parent = this.paths[indexes[self]];
            }
            more = child || sibling;
        }
    }

    /* The prims and properties every spec writes, each with its fields. */
    private UsdPrim specs(final int at) {
        final Cursor in = new Cursor(at);
        final long count = check(in.u64());
        final int[] specPaths;
        final int[] specSets;
        final int[] specTypes;
        if (this.version < 0x000400) {
            throw new ModelFormatException("the USD crate " + this.name + " is of a version too old to read");
        }
        specPaths = in.ints((int) count);
        specSets = in.ints((int) count);
        specTypes = in.ints((int) count);
        final UsdPrim root = new UsdPrim("", "/");
        for (int i = 0; i < count; i++) {
            final String path = this.paths[specPaths[i]];
            if (path == null || path.indexOf('{') >= 0) {
                // A variant's opinions are left out: the model is read as its file stands by default.
                continue;
            }
            final Map<String, Long> fields = fieldSet(specSets[i]);
            switch (specTypes[i]) {
                case PSEUDO_ROOT -> fields.forEach((field, rep) -> root.metadata.put(field, value(rep)));
                case PRIM -> prim(root, path, fields);
                case ATTRIBUTE, RELATIONSHIP -> property(root, path, fields, specTypes[i] == RELATIONSHIP);
                default -> {
                    // Connections, variant sets and the rest are read through their prims and properties.
                }
            }
        }
        return root;
    }

    private Map<String, Long> fieldSet(final int start) {
        final Map<String, Long> out = new HashMap<>();
        for (int k = start; k < this.fieldSets.length && this.fieldSets[k] != END_OF_SET; k++) {
            final int field = this.fieldSets[k];
            out.put(this.tokens[this.fieldNames[field]], this.fieldReps[field]);
        }
        return out;
    }

    private static UsdPrim primAt(final UsdPrim root, final String path) {
        UsdPrim prim = root;
        for (final String part : path.substring(1).split("/")) {
            if (!part.isEmpty()) {
                prim = prim.child(part);
            }
        }
        return prim;
    }

    private void prim(final UsdPrim root, final String path, final Map<String, Long> fields) {
        final UsdPrim prim = primAt(root, path);
        fields.forEach((field, rep) -> {
            switch (field) {
                case "typeName" -> prim.type = String.valueOf(value(rep));
                case "specifier" -> prim.specifier = switch ((int) (rep & PAYLOAD)) {
                    case 1 -> "over";
                    case 2 -> "class";
                    default -> "def";
                };
                case "primChildren", "properties" -> {
                    // The prims and properties are read from their own specs.
                }
                default -> prim.metadata.put(field, value(rep));
            }
        });
    }

    private void property(final UsdPrim root, final String path, final Map<String, Long> fields,
                          final boolean relationship) {
        final int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return;
        }
        final UsdPrim prim = primAt(root, path.substring(0, dot));
        final UsdPrim.Property property = prim.property(path.substring(dot + 1));
        property.relationship = relationship;
        fields.forEach((field, rep) -> {
            switch (field) {
                case "typeName" -> property.typeName = String.valueOf(value(rep));
                case "default" -> property.value = value(rep);
                case "timeSamples" -> samples(rep, property);
                case "targetPaths", "connectionPaths" -> {
                    if (value(rep) instanceof List<?> targets) {
                        targets.forEach(target -> property.targets.add(String.valueOf(target)));
                    }
                }
                default -> property.metadata.put(field, value(rep));
            }
        });
    }

    /*
     * Samples over time: at the value's place a jump to the reference of the times, a list of doubles; after that
     * reference a jump to the values, a count and a reference for each.
     */
    private void samples(final long rep, final UsdPrim.Property into) {
        final int at = (int) (rep & PAYLOAD);
        final int timesAt = at + (int) this.data.getLong(at);
        final Object times = value(this.data.getLong(timesAt));
        final int valuesJump = timesAt + 8;
        final int valuesAt = valuesJump + (int) this.data.getLong(valuesJump);
        final long count = check(this.data.getLong(valuesAt));
        if (!(times instanceof double[] when) || when.length != count) {
            return;
        }
        for (int i = 0; i < count; i++) {
            into.samples.put(when[i], value(this.data.getLong(valuesAt + 8 + i * 8)));
        }
    }

    /* A value as its reference keeps it; none for a kind a model needs nothing of. */
    @Nullable
    private Object value(final long rep) {
        final int type = (int) (rep >>> TYPE_SHIFT & 0xFF);
        final long payload = rep & PAYLOAD;
        if ((rep & ARRAY) != 0) {
            return array(type, payload, (rep & COMPRESSED) != 0);
        }
        if ((rep & INLINED) != 0) {
            return inlined(type, payload);
        }
        final Cursor in = new Cursor((int) payload);
        return switch (type) {
            case 32, 34, 41, 40, 50 -> list(type, in);
            case 35 -> references(in);
            case 48 -> {
                final int count = (int) check(in.u64());
                final double[] out = new double[count];
                for (int i = 0; i < count; i++) {
                    out[i] = in.f64();
                }
                yield out;
            }
            default -> element(type, in);
        };
    }

    /* A value kept in its reference: a small number, an index into a table, or a tuple of small integers. */
    @Nullable
    private Object inlined(final int type, final long payload) {
        final int low = (int) payload;
        return switch (type) {
            case 1 -> low != 0;
            case 2, 3, 4, 5, 6, 42, 43, 44 -> (double) low;
            case 7 -> (double) Float.float16ToFloat((short) low);
            case 8, 9, 56 -> (double) Float.intBitsToFloat(low);
            case 10 -> this.tokens[this.strings[low]];
            case 11 -> this.tokens[low];
            case 12 -> new UsdPrim.Asset(this.tokens[low]);
            case 19, 20, 21, 22 -> bytes(payload, 2);
            case 23, 24, 25, 26 -> bytes(payload, 3);
            case 27, 28, 29, 30 -> bytes(payload, 4);
            case 13 -> diagonal(payload, 2);
            case 14 -> diagonal(payload, 3);
            case 15 -> diagonal(payload, 4);
            default -> null;
        };
    }

    private static double[] bytes(final long payload, final int count) {
        final double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = (byte) (payload >>> (8 * i));
        }
        return out;
    }

    /* A matrix kept by its diagonal, the rest 0, row by row. */
    private static double[] diagonal(final long payload, final int size) {
        final double[] out = new double[size * size];
        for (int i = 0; i < size; i++) {
            out[i * size + i] = (byte) (payload >>> (8 * i));
        }
        return out;
    }

    /* One value written out, of a scalar or a tuple. */
    @Nullable
    private Object element(final int type, final Cursor in) {
        return switch (type) {
            case 1 -> in.u8() != 0;
            case 2 -> (double) in.u8();
            case 3 -> (double) in.u32();
            case 4 -> (double) (in.u32() & 0xFFFFFFFFL);
            case 5, 6 -> (double) in.u64();
            case 7 -> (double) Float.float16ToFloat(in.u16());
            case 8 -> (double) in.f32();
            case 9, 56 -> in.f64();
            case 10 -> this.tokens[this.strings[in.u32()]];
            case 11 -> this.tokens[in.u32()];
            case 12 -> new UsdPrim.Asset(this.tokens[in.u32()]);
            case 13 -> doubles(in, 4, 8);
            case 14 -> doubles(in, 9, 8);
            case 15 -> doubles(in, 16, 8);
            case 16 -> doubles(in, 4, 8);
            case 17 -> doubles(in, 4, 4);
            case 18 -> doubles(in, 4, 2);
            case 19 -> doubles(in, 2, 8);
            case 20 -> doubles(in, 2, 4);
            case 21 -> doubles(in, 2, 2);
            case 22 -> doubles(in, 2, -4);
            case 23 -> doubles(in, 3, 8);
            case 24 -> doubles(in, 3, 4);
            case 25 -> doubles(in, 3, 2);
            case 26 -> doubles(in, 3, -4);
            case 27 -> doubles(in, 4, 8);
            case 28 -> doubles(in, 4, 4);
            case 29 -> doubles(in, 4, 2);
            case 30 -> doubles(in, 4, -4);
            default -> null;
        };
    }

    /* Numbers of a tuple: eight bytes a double, four a float, two a half; minus four an int. */
    private static double[] doubles(final Cursor in, final int count, final int size) {
        final double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = switch (size) {
                case 8 -> in.f64();
                case 4 -> in.f32();
                case 2 -> Float.float16ToFloat(in.u16());
                default -> in.u32();
            };
        }
        return out;
    }

    /* An array: a count, then its elements plain, or, packed, as integers or through a table of values. */
    @Nullable
    private Object array(final int type, final long payload, final boolean compressed) {
        if (payload == 0) {
            return emptyArray(type);
        }
        final Cursor in = new Cursor((int) payload);
        final int count = (int) check(this.version >= 0x000700 ? in.u64() : in.u32() & 0xFFFFFFFFL);
        if (compressed) {
            return switch (type) {
                case 3, 4 -> toDoubles(in.ints(count));
                case 5, 6 -> toDoubles(in.longs(count));
                case 7, 8, 9 -> packedReals(type, in, count);
                default -> null;
            };
        }
        if (type == 10 || type == 11 || type == 12) {
            final List<String> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                final int index = in.u32();
                out.add(type == 10 ? this.tokens[this.strings[index]] : this.tokens[index]);
            }
            return out;
        }
        final Object first = count == 0 ? null : element(type, in.copy());
        if (first instanceof double[]) {
            final double[][] out = new double[count][];
            for (int i = 0; i < count; i++) {
                out[i] = (double[]) element(type, in);
            }
            return out;
        }
        final double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            final Object each = element(type, in);
            out[i] = each instanceof Double number ? number : each instanceof Boolean flag && flag ? 1 : 0;
        }
        return out;
    }

    private static Object emptyArray(final int type) {
        return type == 10 || type == 11 || type == 12 ? List.of() : type >= 13 && type <= 30 ? new double[0][]
                : new double[0];
    }

    /* Real numbers packed: as integers, 'i', or as indices into a table of the values, 't'. */
    private double[] packedReals(final int type, final Cursor in, final int count) {
        final int code = in.u8();
        if (code == 'i') {
            return toDoubles(in.ints(count));
        }
        if (code == 't') {
            final int size = in.u32();
            final double[] table = new double[size];
            for (int i = 0; i < size; i++) {
                table[i] = type == 9 ? in.f64() : type == 8 ? in.f32() : Float.float16ToFloat(in.u16());
            }
            final int[] indices = in.ints(count);
            final double[] out = new double[count];
            for (int i = 0; i < count; i++) {
                out[i] = table[indices[i]];
            }
            return out;
        }
        throw new ModelFormatException("the USD crate " + this.name + " packs numbers in a way not read: " + code);
    }

    private static double[] toDoubles(final int[] values) {
        final double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }
        return out;
    }

    private static double[] toDoubles(final long[] values) {
        final double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i];
        }
        return out;
    }

    /*
     * A list of paths, tokens or strings, or a list operation of paths: its items, every list it holds (explicit,
     * added, prepended, appended) together, the deleted and ordered ones let go.
     */
    private List<String> list(final int type, final Cursor in) {
        final List<String> out = new ArrayList<>();
        if (type == 40 || type == 41 || type == 50) {
            final int count = (int) check(in.u64());
            for (int i = 0; i < count; i++) {
                final int index = in.u32();
                out.add(type == 40 ? this.paths[index] : type == 41 ? this.tokens[index]
                        : this.tokens[this.strings[index]]);
            }
            return out;
        }
        final int header = in.u8();
        // Explicit, added, prepended, appended, deleted, ordered: the bits after the first.
        final int[] bits = {1 << 1, 1 << 2, 1 << 5, 1 << 6, 1 << 3, 1 << 4};
        for (int k = 0; k < bits.length; k++) {
            if ((header & bits[k]) == 0) {
                continue;
            }
            final int count = (int) check(in.u64());
            for (int i = 0; i < count; i++) {
                final int index = in.u32();
                if (k < 4) {
                    out.add(type == 32 ? this.tokens[index] : this.paths[index]);
                }
            }
        }
        return out;
    }

    /* References a prim makes, as {@code asset@</Path>} strings, read only while they carry no custom data. */
    private List<String> references(final Cursor in) {
        final List<String> out = new ArrayList<>();
        final int header = in.u8();
        final int[] bits = {1 << 1, 1 << 2, 1 << 5, 1 << 6};
        for (final int bit : bits) {
            if ((header & bit) == 0) {
                continue;
            }
            final int count = (int) check(in.u64());
            for (int i = 0; i < count; i++) {
                final String asset = this.tokens[this.strings[in.u32()]];
                final String path = this.paths[in.u32()];
                in.at += 16;
                final long custom = in.u64();
                if (custom != 0) {
                    return out;
                }
                out.add(asset + "@" + path);
            }
        }
        return out;
    }

    private long check(final long count) {
        if (count < 0 || count > MOST) {
            throw new ModelFormatException("the USD crate " + this.name + " counts " + count + " of something");
        }
        return count;
    }

    /* A place in the file, read on from. */
    private final class Cursor {

        private int at;

        private Cursor(final int at) {
            this.at = at;
        }

        Cursor copy() {
            return new Cursor(this.at);
        }

        int u8() {
            return UsdCrate.this.data.get(this.at++) & 0xFF;
        }

        short u16() {
            final short value = UsdCrate.this.data.getShort(this.at);
            this.at += 2;
            return value;
        }

        int u32() {
            final int value = UsdCrate.this.data.getInt(this.at);
            this.at += 4;
            return value;
        }

        long u64() {
            final long value = UsdCrate.this.data.getLong(this.at);
            this.at += 8;
            return value;
        }

        float f32() {
            final float value = UsdCrate.this.data.getFloat(this.at);
            this.at += 4;
            return value;
        }

        double f64() {
            final double value = UsdCrate.this.data.getDouble(this.at);
            this.at += 8;
            return value;
        }

        byte[] bytes(final int count) {
            final byte[] out = new byte[count];
            UsdCrate.this.data.get(this.at, out);
            this.at += count;
            return out;
        }

        /* Integers of 32 bits packed: their LZ4 block, then each as its difference from the one before. */
        int[] ints(final int count) {
            final int size = (int) check(u64());
            final ByteBuffer packed = ByteBuffer.wrap(Lz4.decompress(bytes(size), Math.max(64, count * 5 + 16)))
                    .order(ByteOrder.LITTLE_ENDIAN);
            final int[] out = new int[count];
            final int common = packed.getInt(0);
            int codes = 4;
            int numbers = 4 + (count * 2 + 7) / 8;
            int previous = 0;
            for (int i = 0; i < count; i++) {
                final int code = packed.get(codes + i / 4) >> (2 * (i % 4)) & 3;
                final int step = switch (code) {
                    case 0 -> common;
                    case 1 -> packed.get(numbers++);
                    case 2 -> {
                        final short small = packed.getShort(numbers);
                        numbers += 2;
                        yield small;
                    }
                    default -> {
                        final int large = packed.getInt(numbers);
                        numbers += 4;
                        yield large;
                    }
                };
                previous += step;
                out[i] = previous;
            }
            return out;
        }

        /* Integers of 64 bits packed, as those of 32, their steps two, four or eight bytes. */
        long[] longs(final int count) {
            final int size = (int) check(u64());
            final ByteBuffer packed = ByteBuffer.wrap(Lz4.decompress(bytes(size), Math.max(64, count * 9 + 16)))
                    .order(ByteOrder.LITTLE_ENDIAN);
            final long[] out = new long[count];
            final long common = packed.getLong(0);
            int numbers = 8 + (count * 2 + 7) / 8;
            long previous = 0;
            for (int i = 0; i < count; i++) {
                final int code = packed.get(8 + i / 4) >> (2 * (i % 4)) & 3;
                final long step = switch (code) {
                    case 0 -> common;
                    case 1 -> {
                        final short small = packed.getShort(numbers);
                        numbers += 2;
                        yield small;
                    }
                    case 2 -> {
                        final int medium = packed.getInt(numbers);
                        numbers += 4;
                        yield medium;
                    }
                    default -> {
                        final long large = packed.getLong(numbers);
                        numbers += 8;
                        yield large;
                    }
                };
                previous += step;
                out[i] = previous;
            }
            return out;
        }
    }
}
