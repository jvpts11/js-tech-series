/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A plan NextgreIQL made, as it is shown: the statement it was for, the plans it weighed with what each cost and why
 * any was set aside, and the chosen one as a tree of steps, each with the time reckoned for it and, once it ran, the
 * time it took. What the Planner Studio draws, and what the engine keeps of the plans it made.
 *
 * @param id            the plan's number on its Mainframe, counting up
 * @param statement     the statement as it was written
 * @param analyze       whether it was run to be measured ({@code EXPLAIN ANALYZE}) or only planned
 * @param at            the game time it was planned at
 * @param alternatives  the plans weighed, the chosen one marked
 * @param nodes         the chosen plan's tree, each node after its parent
 * @param planningMicros how long the weighing took, in microseconds
 * @param executionTicks how long the craft took, or {@link #UNKNOWN} while it runs or when it never did
 * @param state         where the plan stands: {@link #PLANNED}, {@link #RUNNING}, {@link #DONE} or {@link #FAILED}
 * @param outcome       what the engine said of it
 */
public record NextgrePlanView(int id, String statement, boolean analyze, long at, List<Alternative> alternatives,
                              List<Node> nodes, long planningMicros, long executionTicks, byte state, Text outcome) {

    /** A time nobody knows yet. */
    public static final long UNKNOWN = -1L;
    /** Planned and not run. */
    public static final byte PLANNED = 0;
    /** Running now. */
    public static final byte RUNNING = 1;
    /** Run, and made what it was asked for. */
    public static final byte DONE = 2;
    /** Run, and settled short of it. */
    public static final byte FAILED = 3;

    /** A step at a bench. */
    public static final byte BENCH = 1;
    /** A step on a machine. */
    public static final byte MACHINE = 2;
    /** Raw materials taken from the network's servers. */
    public static final byte PULL = 3;
    /** What nothing on the network holds or makes. */
    public static final byte MISSING = 4;
    /** A note another mod added under a step. */
    public static final byte NOTE = 5;
    /** The whole craft, at the top of the tree. */
    public static final byte ROOT = 0;
    /** Items read straight from the servers that hold them. */
    public static final byte SEEK = 6;

    /** The most nodes a plan is shown with, so a craft of hundreds of steps still fits in one packet. */
    public static final int MAX_NODES = 96;
    /** The most plans weighed that are kept. */
    public static final int MAX_ALTERNATIVES = 8;

    public static final StreamCodec<RegistryFriendlyByteBuf, NextgrePlanView> STREAM_CODEC = StreamCodec.of(
            NextgrePlanView::write, NextgrePlanView::read);

    public NextgrePlanView {
        alternatives = List.copyOf(alternatives.subList(0, Math.min(alternatives.size(), MAX_ALTERNATIVES)));
        nodes = List.copyOf(nodes.subList(0, Math.min(nodes.size(), MAX_NODES)));
    }

    /** The plan chosen, or null when every one was set aside. */
    public Alternative chosen() {
        for (final Alternative alternative : alternatives) {
            if (alternative.chosen()) {
                return alternative;
            }
        }
        return null;
    }

    /** The same plan where it stands now. */
    public NextgrePlanView with(final List<Node> nodes, final long executionTicks, final byte state) {
        return new NextgrePlanView(id, statement, analyze, at, alternatives, nodes, planningMicros, executionTicks,
                state, outcome);
    }

    /** The plan written into a tag, for the Mainframe to keep. */
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putString("Statement", statement);
        tag.putBoolean("Analyze", analyze);
        tag.putLong("At", at);
        final ListTag alts = new ListTag();
        for (final Alternative alternative : alternatives) {
            final CompoundTag row = new CompoundTag();
            row.putInt("Number", alternative.number());
            row.put("Description", TextTags.write(alternative.description()));
            row.putLong("Cost", alternative.cost());
            row.put("SetAside", TextTags.write(alternative.setAside()));
            row.putBoolean("Chosen", alternative.chosen());
            row.put("Notes", TextTags.writeAll(alternative.notes()));
            alts.add(row);
        }
        tag.put("Alternatives", alts);
        final ListTag rows = new ListTag();
        for (final Node node : nodes) {
            final CompoundTag row = new CompoundTag();
            row.putInt("Parent", node.parent());
            row.putInt("Step", node.step());
            row.putByte("Kind", node.kind());
            row.put("Title", TextTags.write(node.title()));
            row.put("Detail", TextTags.write(node.detail()));
            row.putLong("Estimate", node.estimate());
            row.putLong("Actual", node.actual());
            row.putBoolean("Finished", node.finished());
            final ListTag hints = new ListTag();
            node.hints().forEach(hint -> hints.add(StringTag.valueOf(hint)));
            row.put("Hints", hints);
            rows.add(row);
        }
        tag.put("Nodes", rows);
        tag.putLong("PlanningMicros", planningMicros);
        tag.putLong("ExecutionTicks", executionTicks);
        tag.putByte("State", state);
        tag.put("Outcome", TextTags.write(outcome));
        return tag;
    }

    /** A plan read back from {@link #save}. */
    public static NextgrePlanView load(final CompoundTag tag) {
        final List<Alternative> alternatives = new ArrayList<>();
        final ListTag alts = tag.getList("Alternatives", Tag.TAG_COMPOUND);
        for (int i = 0; i < alts.size(); i++) {
            final CompoundTag row = alts.getCompound(i);
            alternatives.add(new Alternative(row.getInt("Number"), TextTags.read(row.getCompound("Description")),
                    row.getLong("Cost"), TextTags.read(row.getCompound("SetAside")), row.getBoolean("Chosen"),
                    TextTags.readAll(row.getList("Notes", Tag.TAG_COMPOUND))));
        }
        final List<Node> nodes = new ArrayList<>();
        final ListTag rows = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) {
            final CompoundTag row = rows.getCompound(i);
            final List<String> hints = new ArrayList<>();
            final ListTag written = row.getList("Hints", Tag.TAG_STRING);
            for (int h = 0; h < written.size(); h++) {
                hints.add(written.getString(h));
            }
            nodes.add(new Node(row.getInt("Parent"), row.getInt("Step"), row.getByte("Kind"),
                    TextTags.read(row.getCompound("Title")), TextTags.read(row.getCompound("Detail")),
                    row.getLong("Estimate"), row.getLong("Actual"), row.getBoolean("Finished"), hints));
        }
        return new NextgrePlanView(tag.getInt("Id"), tag.getString("Statement"), tag.getBoolean("Analyze"),
                tag.getLong("At"), alternatives, nodes, tag.getLong("PlanningMicros"), tag.getLong("ExecutionTicks"),
                tag.getByte("State"), TextTags.read(tag.getCompound("Outcome")));
    }

    private static void write(final RegistryFriendlyByteBuf buf, final NextgrePlanView view) {
        buf.writeVarInt(view.id());
        ByteBufCodecs.STRING_UTF8.encode(buf, view.statement());
        buf.writeBoolean(view.analyze());
        buf.writeVarLong(view.at());
        buf.writeVarInt(view.alternatives().size());
        for (final Alternative alternative : view.alternatives()) {
            buf.writeVarInt(alternative.number());
            TextCodecs.STREAM_CODEC.encode(buf, alternative.description());
            buf.writeVarLong(alternative.cost());
            TextCodecs.STREAM_CODEC.encode(buf, alternative.setAside());
            buf.writeBoolean(alternative.chosen());
            buf.writeVarInt(alternative.notes().size());
            alternative.notes().forEach(note -> TextCodecs.STREAM_CODEC.encode(buf, note));
        }
        buf.writeVarInt(view.nodes().size());
        for (final Node node : view.nodes()) {
            buf.writeVarInt(node.parent() + 1);
            buf.writeVarInt(node.step() + 1);
            buf.writeByte(node.kind());
            TextCodecs.STREAM_CODEC.encode(buf, node.title());
            TextCodecs.STREAM_CODEC.encode(buf, node.detail());
            buf.writeVarLong(node.estimate() + 1);
            buf.writeVarLong(node.actual() + 1);
            buf.writeBoolean(node.finished());
            buf.writeVarInt(node.hints().size());
            node.hints().forEach(hint -> ByteBufCodecs.STRING_UTF8.encode(buf, hint));
        }
        buf.writeVarLong(view.planningMicros());
        buf.writeVarLong(view.executionTicks() + 1);
        buf.writeByte(view.state());
        TextCodecs.STREAM_CODEC.encode(buf, view.outcome());
    }

    private static NextgrePlanView read(final RegistryFriendlyByteBuf buf) {
        final int id = buf.readVarInt();
        final String statement = ByteBufCodecs.STRING_UTF8.decode(buf);
        final boolean analyze = buf.readBoolean();
        final long at = buf.readVarLong();
        final int altCount = Math.min(buf.readVarInt(), MAX_ALTERNATIVES);
        final List<Alternative> alternatives = new ArrayList<>(altCount);
        for (int i = 0; i < altCount; i++) {
            final int number = buf.readVarInt();
            final Text description = TextCodecs.STREAM_CODEC.decode(buf);
            final long cost = buf.readVarLong();
            final Text setAside = TextCodecs.STREAM_CODEC.decode(buf);
            final boolean chosen = buf.readBoolean();
            final int noteCount = Math.min(buf.readVarInt(), Alternative.MAX_NOTES);
            final List<Text> notes = new ArrayList<>(noteCount);
            for (int n = 0; n < noteCount; n++) {
                notes.add(TextCodecs.STREAM_CODEC.decode(buf));
            }
            alternatives.add(new Alternative(number, description, cost, setAside, chosen, notes));
        }
        final int nodeCount = Math.min(buf.readVarInt(), MAX_NODES);
        final List<Node> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            final int parent = buf.readVarInt() - 1;
            final int step = buf.readVarInt() - 1;
            final byte kind = buf.readByte();
            final Text title = TextCodecs.STREAM_CODEC.decode(buf);
            final Text detail = TextCodecs.STREAM_CODEC.decode(buf);
            final long estimate = buf.readVarLong() - 1;
            final long actual = buf.readVarLong() - 1;
            final boolean finished = buf.readBoolean();
            final int hintCount = Math.min(buf.readVarInt(), Node.MAX_HINTS);
            final List<String> hints = new ArrayList<>(hintCount);
            for (int h = 0; h < hintCount; h++) {
                hints.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            nodes.add(new Node(parent, step, kind, title, detail, estimate, actual, finished, hints));
        }
        final long planningMicros = buf.readVarLong();
        final long executionTicks = buf.readVarLong() - 1;
        final byte state = buf.readByte();
        final Text outcome = TextCodecs.STREAM_CODEC.decode(buf);
        return new NextgrePlanView(id, statement, analyze, at, alternatives, nodes, planningMicros, executionTicks,
                state, outcome);
    }

    /**
     * A plan that was weighed.
     *
     * @param number      its number in the list, from one
     * @param description how it differs from the others
     * @param cost        what it was reckoned to take, in ticks
     * @param setAside    why it was set aside, or empty when it was not
     * @param chosen      whether it is the plan the network runs
     * @param notes       why its cost changed, as rules and hints said
     */
    public record Alternative(int number, Text description, long cost, Text setAside, boolean chosen,
                              List<Text> notes) {

        /** The most notes kept with one plan. */
        public static final int MAX_NOTES = 8;

        public Alternative {
            notes = List.copyOf(notes.subList(0, Math.min(notes.size(), MAX_NOTES)));
        }
    }

    /**
     * One node of the chosen plan's tree.
     *
     * @param parent   the node above it, by its place in the list, or -1 for the top
     * @param step     the plan's step it shows, by its place in the plan, or -1 for one that is no step
     * @param kind     what it is: {@link #ROOT}, {@link #BENCH}, {@link #MACHINE}, {@link #PULL} and the rest
     * @param title    what it does
     * @param detail   where, and how
     * @param estimate the time reckoned for it, in ticks, or {@link #UNKNOWN}
     * @param actual   the time it took, or has taken so far, in ticks, or {@link #UNKNOWN}
     * @param finished whether {@code actual} is the whole of it
     * @param hints    the hints that changed this part of the plan, as written
     */
    public record Node(int parent, int step, byte kind, Text title, Text detail, long estimate, long actual,
                       boolean finished, List<String> hints) {

        /** The most hints shown on one node. */
        public static final int MAX_HINTS = 4;

        public Node {
            hints = List.copyOf(hints.subList(0, Math.min(hints.size(), MAX_HINTS)));
        }

        /** The same node with what it took. */
        public Node measured(final long took, final boolean done) {
            return new Node(parent, step, kind, title, detail, estimate, took, done, hints);
        }
    }
}
