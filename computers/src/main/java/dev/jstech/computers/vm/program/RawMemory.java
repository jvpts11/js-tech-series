/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The raw memory a program reaches through pointers: blocks of bytes, each at an address of its own.
 *
 * <p>A pointer is a number, the address of a byte, so it is stored, compared and moved as one; reading through it is
 * finding the block the address falls in. Every block knows its size, and an address is never given out twice, so a
 * read past a block's end, into a block that was freed, or through null finds no block and is a violation the program
 * is stopped for (computers/docs/design/sigma.md). A gap is left after every block, so running one past the end lands
 * on nothing rather than on the next block.
 *
 * <p>Each block is held on the process's heap at its exact size, which is what makes its bytes count in the program's
 * memory and in what it runs out of. A block a call made with {@code stackalloc} goes when the call returns.
 */
@TextHolder
public final class RawMemory {

    /** One block: where it starts, what it holds, and whether the call that made it frees it on returning. */
    public static final class Block {
        private final long address;
        private final byte[] bytes;
        private final boolean onStack;

        public Block(final long address, final byte[] bytes, final boolean onStack) {
            this.address = address;
            this.bytes = bytes;
            this.onStack = onStack;
        }

        public long address() {
            return this.address;
        }

        /** What it holds, the array itself: a snapshot reads it, and nothing else outside this class does. */
        public byte[] bytes() {
            return this.bytes;
        }

        public boolean onStack() {
            return this.onStack;
        }
    }

    /** How wide a read or a write through a pointer is, and the kind of value it gives. */
    public enum Width {
        /** A byte, read as a whole number from 0 to 255; a bool is one too. */
        BYTE(1),
        /** A character's two bytes. */
        CHAR(2),
        INT(4),
        /** A long, and a pointer. */
        LONG(8),
        FLOAT(4),
        DOUBLE(8);

        private final int bytes;

        Width(final int bytes) {
            this.bytes = bytes;
        }

        public int bytes() {
            return this.bytes;
        }
    }

    private final Heap heap;
    private final NavigableMap<Long, Block> blocks = new TreeMap<>();
    /** The address the next block gets. */
    private long next = FIRST;
    /** The block the last read or write reached, asked first, since most reach the same one again. */
    private Block last;

    /** The first address given out; everything below it, null among it, is no block's. */
    static final long FIRST = 0x10000L;
    /** Where every block starts a multiple of, and how much is left unused after each. */
    private static final int ALIGN = 16;
    /** The most one block may hold, past which no budget matters: the game holds it as one array. */
    private static final long LARGEST = 1L << 30;

    private static final VarHandle CHARS = MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INTS = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONGS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle FLOATS =
            MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle DOUBLES =
            MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);

    private static final TextKey PROTECTED = TextKey.of("jsc.vm.raw_memory.protected",
            "attempted to read or write protected memory");
    private static final TextKey NEGATIVE = TextKey.of("jsc.vm.raw_memory.negative",
            "a count of bytes cannot be negative: %s");
    private static final TextKey TOO_BIG = TextKey.of("jsc.vm.raw_memory.too_big",
            "a block of %s bytes is more than one block may hold");

    RawMemory(final Heap heap) {
        this.heap = heap;
    }

    /**
     * A new block of that many bytes, every one of them zero, held on the heap; the address it starts at is what the
     * program gets. One made for a call's {@code stackalloc} goes when that call returns.
     */
    public long allocate(final long bytes, final boolean onStack, final int line) {
        checkCount(bytes, line);
        if (bytes > LARGEST) {
            throw new Halt(Halt.Reason.OUT_OF_MEMORY, line, TOO_BIG.with(bytes));
        }
        // The budget is asked first, so a size past it is refused before the game is asked to hold the array.
        this.heap.reserve(bytes, line);
        final Block made = new Block(this.next, new byte[(int) bytes], onStack);
        this.heap.allocate(made, bytes, line);
        this.blocks.put(made.address, made);
        this.next = after(made.address, bytes);
        return made.address;
    }

    /**
     * The block moved to one of the new size, what fits of it copied over, and the old one freed: C's realloc, which
     * the program may never count on keeping its address. Null asks for a new block.
     */
    public long resize(final long address, final long bytes, final int line) {
        if (address == 0) {
            return this.allocate(bytes, false, line);
        }
        final Block old = this.started(address, line);
        final long moved = this.allocate(bytes, false, line);
        System.arraycopy(old.bytes, 0, this.blocks.get(moved).bytes, 0, (int) Math.min(bytes, old.bytes.length));
        this.drop(old);
        return moved;
    }

    /** Frees the block the address starts; freeing null does nothing, as C's free does. */
    public void free(final long address, final int line) {
        if (address != 0) {
            this.drop(this.started(address, line));
        }
    }

    /** Lets go of a block a returning call made, which nothing may reach once the call is gone. */
    void release(final long address) {
        final Block held = this.blocks.get(address);
        if (held != null) {
            this.drop(held);
        }
    }

    /** Copies bytes from one place to another; the two may overlap, as C's memmove allows. */
    public void copy(final long from, final long to, final long bytes, final int line) {
        checkCount(bytes, line);
        if (bytes == 0) {
            return;
        }
        final Block source = this.reach(from, bytes, line);
        final Block target = this.reach(to, bytes, line);
        System.arraycopy(source.bytes, (int) (from - source.address), target.bytes, (int) (to - target.address),
                (int) bytes);
    }

    /** Sets that many bytes to one value, of which only the low eight bits are kept, as C's memset keeps them. */
    public void fill(final long address, final long bytes, final int value, final int line) {
        checkCount(bytes, line);
        if (bytes == 0) {
            return;
        }
        final Block target = this.reach(address, bytes, line);
        final int from = (int) (address - target.address);
        Arrays.fill(target.bytes, from, from + (int) bytes, (byte) value);
    }

    /** Below zero, zero or above it, as the first differing byte of the two, read without a sign, is: C's memcmp. */
    public int compare(final long first, final long second, final long bytes, final int line) {
        checkCount(bytes, line);
        if (bytes == 0) {
            return 0;
        }
        final Block one = this.reach(first, bytes, line);
        final Block other = this.reach(second, bytes, line);
        final int at = (int) (first - one.address);
        final int otherAt = (int) (second - other.address);
        return Integer.signum(Arrays.compareUnsigned(one.bytes, at, at + (int) bytes,
                other.bytes, otherAt, otherAt + (int) bytes));
    }

    /** The value that many bytes at the address hold, read as the width says. */
    public Object read(final long address, final Width width, final int line) {
        final Block block = this.reach(address, width.bytes, line);
        final int at = (int) (address - block.address);
        return switch (width) {
            case BYTE -> block.bytes[at] & 0xFF;
            case CHAR -> (int) (char) CHARS.get(block.bytes, at);
            case INT -> (int) INTS.get(block.bytes, at);
            case LONG -> (long) LONGS.get(block.bytes, at);
            case FLOAT -> (float) FLOATS.get(block.bytes, at);
            case DOUBLE -> (double) DOUBLES.get(block.bytes, at);
        };
    }

    /** Writes the value into that many bytes at the address, as the width says. */
    public void write(final long address, final Width width, final Object value, final int line) {
        final Block block = this.reach(address, width.bytes, line);
        final int at = (int) (address - block.address);
        switch (width) {
            case BYTE -> block.bytes[at] = (byte) whole(value);
            case CHAR -> CHARS.set(block.bytes, at, (char) whole(value));
            case INT -> INTS.set(block.bytes, at, (int) whole(value));
            case LONG -> LONGS.set(block.bytes, at, whole(value));
            case FLOAT -> FLOATS.set(block.bytes, at, Numbers.toFloat(value));
            case DOUBLE -> DOUBLES.set(block.bytes, at, Numbers.toDouble(value));
        }
    }

    /** The address the next block gets, for a snapshot. */
    public long next() {
        return this.next;
    }

    /**
     * Takes back a block a snapshot kept, already held on the heap again; the next address stays past it, so it is
     * never given out twice.
     */
    void adopt(final Block block) {
        this.blocks.put(block.address, block);
        this.next = Math.max(this.next, after(block.address, block.bytes.length));
    }

    /** Gives out addresses from where a snapshot said, never back among the blocks already taken back. */
    void startFrom(final long address) {
        this.next = Math.max(this.next, address);
    }

    /*
     * The block that many bytes from the address lie inside, or a violation. The offset is worked out by subtraction,
     * so an address near the end of a long's range cannot wrap round into a block.
     */
    private Block reach(final long address, final long bytes, final int line) {
        final Block hit = this.last;
        if (hit != null && inside(hit, address, bytes)) {
            return hit;
        }
        final Map.Entry<Long, Block> floor = this.blocks.floorEntry(address);
        if (floor == null || !inside(floor.getValue(), address, bytes)) {
            throw violation(line);
        }
        this.last = floor.getValue();
        return this.last;
    }

    /* The block a free or a resize names, which has to be one the program asked for, by the address it starts at. */
    private Block started(final long address, final int line) {
        final Block block = this.blocks.get(address);
        if (block == null || block.onStack) {
            throw violation(line);
        }
        return block;
    }

    private void drop(final Block block) {
        this.blocks.remove(block.address);
        this.heap.release(block);
        if (this.last == block) {
            this.last = null;
        }
    }

    /* The address after a block of that size at that address, past the gap left behind every block. */
    private static long after(final long address, final long bytes) {
        return address + (bytes + ALIGN - 1) / ALIGN * ALIGN + ALIGN;
    }

    private static boolean inside(final Block block, final long address, final long bytes) {
        final long offset = address - block.address;
        return offset >= 0 && offset <= block.bytes.length - bytes;
    }

    private static void checkCount(final long bytes, final int line) {
        if (bytes < 0) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NEGATIVE.with(bytes));
        }
    }

    /* A whole number from what the stack holds: a bool goes in as one or zero, as C keeps it. */
    private static long whole(final Object value) {
        if (value instanceof Boolean truth) {
            return truth ? 1 : 0;
        }
        return Numbers.toLong(value);
    }

    /** The halt a read or a write outside every live block is. */
    static Halt violation(final int line) {
        return new Halt(Halt.Reason.ACCESS_VIOLATION, line, PROTECTED.text());
    }
}
