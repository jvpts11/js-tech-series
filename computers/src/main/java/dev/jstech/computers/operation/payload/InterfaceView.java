/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the window of a Crafting Interface shows: its name and id, the skin of the computer that commands it,
 * the patterns it holds with the router each input goes through, how it feeds its machine, its mode, state and most
 * jobs, its warnings, what it is doing now, what its jobs did lately and what programs set.
 *
 * @param name      what it is called
 * @param shortId   the first characters of its id
 * @param skin      the era whose skin its window wears
 * @param linked    whether a Crafting Computer drives it
 * @param capacity  how many patterns it holds at most
 * @param patterns  the patterns it holds
 * @param routers   the names of the routers on its own cable, in the order they are listed
 * @param mode      {@link #NONE}, {@link #DIRECT} or {@link #CABLE}
 * @param exclusive whether it runs one recipe at a time
 * @param feeds     what it feeds, in a line
 * @param receiving the Receiving Buses that credit it, in a line
 * @param paused    whether it is paused
 * @param maxJobs   the most jobs at once, 0 for as many as come
 * @param warnings  what keeps it from working, each in a line
 * @param now       what it is doing now
 * @param nowTone   {@link #GOOD}, {@link #WARN} or {@link #DIM}
 * @param log       what its jobs did lately, newest first
 * @param marks     which setting a program set last, by the setting's word, and the program's name
 */
public record InterfaceView(String name, String shortId, HardwareEra skin, boolean linked, int capacity,
                            List<PatternView> patterns, List<Text> routers, byte mode, boolean exclusive, Text feeds,
                            Text receiving, boolean paused, int maxJobs, List<Text> warnings, Text now, byte nowTone,
                            List<CraftingLog.Entry> log, Map<String, String> marks) {

    public static final byte NONE = 0;
    public static final byte DIRECT = 1;
    public static final byte CABLE = 2;
    public static final byte GOOD = 0;
    public static final byte WARN = 1;
    public static final byte DIM = 2;
    /** Why an input goes through its router, as the interface routes say. */
    public static final byte CHOSEN = 0;
    public static final byte FILTER = 1;
    public static final byte ANY = 2;
    public static final byte NO_ROUTER = 3;
    private static final int MOST = 16;
    private static final int MOST_TEXT = 128;

    public InterfaceView {
        patterns = List.copyOf(patterns);
        routers = List.copyOf(routers);
        warnings = List.copyOf(warnings);
        log = List.copyOf(log);
        marks = Map.copyOf(marks);
    }

    /**
     * A pattern the interface holds.
     *
     * @param icon   its first output, to draw
     * @param name   what it is called
     * @param inputs its inputs with the router each goes through
     * @param runs   whether it can run here
     */
    public record PatternView(ItemStack icon, Text name, List<InputView> inputs, boolean runs) {

        public PatternView {
            inputs = List.copyOf(inputs);
        }
    }

    /**
     * An input of a pattern.
     *
     * @param key    what goes in
     * @param amount how much a lot takes
     * @param router the router it goes through, by its place in the routers' list; -1 for none
     * @param why    {@link #CHOSEN}, {@link #FILTER}, {@link #ANY} or {@link #NO_ROUTER}
     */
    public record InputView(StorageKey key, long amount, int router, byte why) {
    }

    /** Writes {@code view} onto {@code buf}. */
    public static void write(final RegistryFriendlyByteBuf buf, final InterfaceView view) {
        buf.writeUtf(cut(view.name), MOST_TEXT);
        buf.writeUtf(cut(view.shortId), MOST_TEXT);
        buf.writeVarInt(view.skin.level());
        buf.writeBoolean(view.linked);
        buf.writeVarInt(view.capacity);
        final List<PatternView> patterns = view.patterns.subList(0, Math.min(MOST, view.patterns.size()));
        buf.writeVarInt(patterns.size());
        for (final PatternView pattern : patterns) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, pattern.icon());
            TextCodecs.STREAM_CODEC.encode(buf, pattern.name());
            buf.writeBoolean(pattern.runs());
            final List<InputView> inputs = pattern.inputs().subList(0, Math.min(MOST, pattern.inputs().size()));
            buf.writeVarInt(inputs.size());
            for (final InputView input : inputs) {
                StorageKey.STREAM_CODEC.encode(buf, input.key());
                buf.writeVarLong(input.amount());
                buf.writeVarInt(input.router());
                buf.writeByte(input.why());
            }
        }
        writeTexts(buf, view.routers);
        buf.writeByte(view.mode);
        buf.writeBoolean(view.exclusive);
        TextCodecs.STREAM_CODEC.encode(buf, view.feeds);
        TextCodecs.STREAM_CODEC.encode(buf, view.receiving);
        buf.writeBoolean(view.paused);
        buf.writeVarInt(view.maxJobs);
        writeTexts(buf, view.warnings);
        TextCodecs.STREAM_CODEC.encode(buf, view.now);
        buf.writeByte(view.nowTone);
        final List<CraftingLog.Entry> log = view.log.subList(0, Math.min(MOST, view.log.size()));
        buf.writeVarInt(log.size());
        for (final CraftingLog.Entry entry : log) {
            buf.writeVarLong(entry.time());
            buf.writeUtf(cut(entry.what()), MOST_TEXT);
            buf.writeVarLong(entry.amount());
            buf.writeVarLong(entry.total());
            buf.writeByte(entry.kind());
            buf.writeUtf(cut(entry.note()), MOST_TEXT);
        }
        buf.writeVarInt(Math.min(MOST, view.marks.size()));
        view.marks.entrySet().stream().limit(MOST).forEach(mark -> {
            buf.writeUtf(cut(mark.getKey()), MOST_TEXT);
            buf.writeUtf(cut(mark.getValue()), MOST_TEXT);
        });
    }

    /** Reads a view {@link #write} wrote. */
    public static InterfaceView read(final RegistryFriendlyByteBuf buf) {
        final String name = buf.readUtf(MOST_TEXT);
        final String shortId = buf.readUtf(MOST_TEXT);
        final HardwareEra skin = HardwareEra.fromLevel(buf.readVarInt());
        final boolean linked = buf.readBoolean();
        final int capacity = buf.readVarInt();
        final int patternCount = Math.min(MOST, buf.readVarInt());
        final List<PatternView> patterns = new ArrayList<>();
        for (int i = 0; i < patternCount; i++) {
            final ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            final Text patternName = TextCodecs.STREAM_CODEC.decode(buf);
            final boolean runs = buf.readBoolean();
            final int inputCount = Math.min(MOST, buf.readVarInt());
            final List<InputView> inputs = new ArrayList<>();
            for (int j = 0; j < inputCount; j++) {
                inputs.add(new InputView(StorageKey.STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readVarInt(),
                        buf.readByte()));
            }
            patterns.add(new PatternView(icon, patternName, inputs, runs));
        }
        final List<Text> routers = readTexts(buf);
        final byte mode = buf.readByte();
        final boolean exclusive = buf.readBoolean();
        final Text feeds = TextCodecs.STREAM_CODEC.decode(buf);
        final Text receiving = TextCodecs.STREAM_CODEC.decode(buf);
        final boolean paused = buf.readBoolean();
        final int maxJobs = buf.readVarInt();
        final List<Text> warnings = readTexts(buf);
        final Text now = TextCodecs.STREAM_CODEC.decode(buf);
        final byte nowTone = buf.readByte();
        final int logCount = Math.min(MOST, buf.readVarInt());
        final List<CraftingLog.Entry> log = new ArrayList<>();
        for (int i = 0; i < logCount; i++) {
            log.add(new CraftingLog.Entry(buf.readVarLong(), buf.readUtf(MOST_TEXT), buf.readVarLong(),
                    buf.readVarLong(), buf.readByte(), buf.readUtf(MOST_TEXT)));
        }
        final int markCount = Math.min(MOST, buf.readVarInt());
        final Map<String, String> marks = new LinkedHashMap<>();
        for (int i = 0; i < markCount; i++) {
            marks.put(buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT));
        }
        return new InterfaceView(name, shortId, skin, linked, capacity, patterns, routers, mode, exclusive, feeds,
                receiving, paused, maxJobs, warnings, now, nowTone, log, marks);
    }

    private static void writeTexts(final RegistryFriendlyByteBuf buf, final List<Text> texts) {
        final List<Text> shown = texts.subList(0, Math.min(MOST, texts.size()));
        buf.writeVarInt(shown.size());
        shown.forEach(text -> TextCodecs.STREAM_CODEC.encode(buf, text));
    }

    private static List<Text> readTexts(final RegistryFriendlyByteBuf buf) {
        final int count = Math.min(MOST, buf.readVarInt());
        final List<Text> texts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            texts.add(TextCodecs.STREAM_CODEC.decode(buf));
        }
        return texts;
    }

    private static String cut(final String text) {
        return text.length() > MOST_TEXT ? text.substring(0, MOST_TEXT) : text;
    }
}
