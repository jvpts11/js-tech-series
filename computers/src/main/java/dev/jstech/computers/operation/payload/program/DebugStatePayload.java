/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a studio's debugger shows, as the machine sends it: whether the program runs, is stopped or has ended, where it
 * stopped and why, its calls and variables, what the names watched hold, what it has spent and on what, and what it
 * printed since the last of these.
 *
 * <p>Every piece of text is cut to what the payload carries before it is sent, since a string past its bound is a
 * payload that fails, not one that is shortened.
 *
 * @param reason   why it stopped: {@code breakpoint}, {@code step} or {@code interrupted}; empty while it runs
 * @param exitCode the code it ended with, once it has ended
 * @param halt     what it said as it halted, or empty for one that ended well
 * @param waiting  whether it waits for a line to be typed
 * @param output   what it printed since the last state was sent
 */
public record DebugStatePayload(BlockPos hostPos, int status, String reason, int exitCode, Text halt,
                                List<Frame> stack, List<Variable> locals, List<Variable> watches, long spent,
                                long steps, List<Cost> calls, long memory, long elapsed, boolean waiting,
                                List<String> output) implements CustomPacketPayload {

    /** No program is being debugged. */
    public static final int NONE = 0;
    public static final int RUNNING = 1;
    public static final int PAUSED = 2;
    public static final int ENDED = 3;

    /** The longest name, type or file carried, and the longest value or printed line. */
    public static final int MOST_NAME = 256;
    public static final int MOST_VALUE = 4096;
    /** The most calls, variables, costs and printed lines one state carries. */
    public static final int MOST_PARTS = 64;

    public static final CustomPacketPayload.Type<DebugStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "debug_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugStatePayload> STREAM_CODEC =
            StreamCodec.of(DebugStatePayload::write, DebugStatePayload::read);

    public DebugStatePayload {
        reason = clip(reason, MOST_NAME);
        stack = List.copyOf(stack.subList(0, Math.min(MOST_PARTS, stack.size())));
        locals = List.copyOf(locals.subList(0, Math.min(MOST_PARTS, locals.size())));
        watches = List.copyOf(watches.subList(0, Math.min(MOST_PARTS, watches.size())));
        calls = List.copyOf(calls.subList(0, Math.min(MOST_PARTS, calls.size())));
        // What was printed last is what matters most, so a flood keeps its end.
        output = List.copyOf(output.subList(Math.max(0, output.size() - MOST_PARTS), output.size()).stream()
                .map(line -> clip(line, MOST_VALUE)).toList());
    }

    /** A state that says no program is being debugged. */
    public static DebugStatePayload none(final BlockPos hostPos) {
        return new DebugStatePayload(hostPos, NONE, "", 0, Text.literal(""), List.of(), List.of(), List.of(), 0, 0,
                List.of(), 0, 0, false, List.of());
    }

    /** Whether a program is being debugged and has not ended. */
    public boolean live() {
        return this.status == RUNNING || this.status == PAUSED;
    }

    @Override
    public CustomPacketPayload.Type<DebugStatePayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final DebugStatePayload state) {
        BlockPos.STREAM_CODEC.encode(buf, state.hostPos);
        buf.writeVarInt(state.status);
        buf.writeUtf(state.reason, MOST_NAME);
        buf.writeVarInt(state.exitCode);
        TextCodecs.STREAM_CODEC.encode(buf, state.halt);
        writeList(buf, state.stack, DebugStatePayload::writeFrame);
        writeList(buf, state.locals, DebugStatePayload::writeVariable);
        writeList(buf, state.watches, DebugStatePayload::writeVariable);
        buf.writeVarLong(state.spent);
        buf.writeVarLong(state.steps);
        writeList(buf, state.calls, (out, cost) -> {
            out.writeUtf(cost.name(), MOST_NAME);
            out.writeVarLong(cost.credits());
        });
        buf.writeVarLong(state.memory);
        buf.writeVarLong(state.elapsed);
        buf.writeBoolean(state.waiting);
        writeList(buf, state.output, (out, line) -> out.writeUtf(line, MOST_VALUE));
    }

    private static DebugStatePayload read(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        final int status = buf.readVarInt();
        final String reason = buf.readUtf(MOST_NAME);
        final int exitCode = buf.readVarInt();
        final Text halt = TextCodecs.STREAM_CODEC.decode(buf);
        final List<Frame> stack = readList(buf, DebugStatePayload::readFrame);
        final List<Variable> locals = readList(buf, DebugStatePayload::readVariable);
        final List<Variable> watches = readList(buf, DebugStatePayload::readVariable);
        final long spent = buf.readVarLong();
        final long steps = buf.readVarLong();
        final List<Cost> calls = readList(buf, in -> new Cost(in.readUtf(MOST_NAME), in.readVarLong()));
        final long memory = buf.readVarLong();
        final long elapsed = buf.readVarLong();
        final boolean waiting = buf.readBoolean();
        final List<String> output = readList(buf, in -> in.readUtf(MOST_VALUE));
        return new DebugStatePayload(host, status, reason, exitCode, halt, stack, locals, watches, spent, steps,
                calls, memory, elapsed, waiting, output);
    }

    private static void writeFrame(final RegistryFriendlyByteBuf buf, final Frame frame) {
        buf.writeUtf(frame.method(), MOST_NAME);
        buf.writeUtf(frame.file(), MOST_NAME);
        buf.writeVarInt(frame.line());
        buf.writeVarInt(frame.column());
        writeList(buf, frame.arguments(), DebugStatePayload::writeVariable);
    }

    private static Frame readFrame(final RegistryFriendlyByteBuf buf) {
        return new Frame(buf.readUtf(MOST_NAME), buf.readUtf(MOST_NAME), buf.readVarInt(), buf.readVarInt(),
                readList(buf, DebugStatePayload::readVariable));
    }

    private static void writeVariable(final RegistryFriendlyByteBuf buf, final Variable variable) {
        buf.writeUtf(variable.name(), MOST_NAME);
        buf.writeUtf(variable.type(), MOST_NAME);
        buf.writeUtf(variable.value(), MOST_VALUE);
    }

    private static Variable readVariable(final RegistryFriendlyByteBuf buf) {
        return new Variable(buf.readUtf(MOST_NAME), buf.readUtf(MOST_NAME), buf.readUtf(MOST_VALUE));
    }

    private static <T> void writeList(final RegistryFriendlyByteBuf buf, final List<T> items,
                                      final BiConsumer<RegistryFriendlyByteBuf, T> each) {
        buf.writeVarInt(items.size());
        for (final T item : items) {
            each.accept(buf, item);
        }
    }

    private static <T> List<T> readList(final RegistryFriendlyByteBuf buf,
                                        final Function<RegistryFriendlyByteBuf, T> each) {
        final int count = Math.min(buf.readVarInt(), MOST_PARTS);
        final List<T> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(each.apply(buf));
        }
        return items;
    }

    private static String clip(final String text, final int most) {
        return text == null ? "" : text.length() <= most ? text : text.substring(0, most);
    }

    /** A call on the stack: the method, the file and the line and column it is at, and what it was handed. */
    public record Frame(String method, String file, int line, int column, List<Variable> arguments) {

        public Frame {
            method = clip(method, MOST_NAME);
            file = clip(file, MOST_NAME);
            arguments = List.copyOf(arguments.subList(0, Math.min(MOST_PARTS, arguments.size())));
        }
    }

    /** A variable, or a name watched: its name, its type, and its value written out; a name not found has no type. */
    public record Variable(String name, String type, String value) {

        public Variable {
            name = clip(name, MOST_NAME);
            type = clip(type, MOST_NAME);
            value = clip(value, MOST_VALUE);
        }
    }

    /** What the calls to one member of the library have cost the program. */
    public record Cost(String name, long credits) {

        public Cost {
            name = clip(name, MOST_NAME);
        }
    }
}
