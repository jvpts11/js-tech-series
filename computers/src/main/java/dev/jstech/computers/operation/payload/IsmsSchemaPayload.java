/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.operation.index.IndexHealth;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the network as the IQL Server Management Studio's Object Explorer shows it: the network and the
 * computer the studio runs on, the Midsoft IQL Server with its version and state (or, when the network runs another
 * engine, which), how many rows each table holds, the saved views, procedures and jobs, the servers, the index's
 * health and the items held by hand.
 *
 * @param window    the studio window that asked
 * @param network   the network's name
 * @param host      the name of the computer the studio runs on
 * @param tableRows how many rows each table holds, in the order of the language's tables
 * @param views     the saved views, each with the query it runs, so the studio can script it again
 */
@TextHolder
public record IsmsSchemaPayload(int window, Text network, String host, Engine engine, List<Integer> tableRows,
                                List<Saved> views, List<Saved> procedures, List<Job> jobs, List<String> servers,
                                Index index, List<Lock> locks) implements CustomPacketPayload {

    public static final int MAX_SERVERS = 128;
    public static final int MAX_OBJECTS = 256;
    public static final int MAX_LOCKS = 64;
    private static final int MAX_NAME = 64;
    private static final int MAX_TABLES = 8;
    /** A saved object's body, which a statement of the language holds whole. */
    private static final int MAX_BODY = RunIqlPayload.MAX_LEN;

    /* A network the studio's machine is not on, by the name it would have. */
    public static final TextKey OFFLINE = TextKey.of("jsc.isms.offline", "%s (offline)");

    public static final CustomPacketPayload.Type<IsmsSchemaPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "isms_schema"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IsmsSchemaPayload> STREAM_CODEC =
            StreamCodec.of((buf, schema) -> schema.write(buf), IsmsSchemaPayload::read);

    /* Copied and cut on the way in, so what the client is handed cannot change under it and always fits. */
    public IsmsSchemaPayload {
        host = clip(host);
        tableRows = List.copyOf(tableRows.subList(0, Math.min(MAX_TABLES, tableRows.size())));
        views = List.copyOf(views.subList(0, Math.min(MAX_OBJECTS, views.size())));
        procedures = List.copyOf(procedures.subList(0, Math.min(MAX_OBJECTS, procedures.size())));
        jobs = List.copyOf(jobs.subList(0, Math.min(MAX_OBJECTS, jobs.size())));
        servers = servers.stream().limit(MAX_SERVERS).map(IsmsSchemaPayload::clip).toList();
        locks = List.copyOf(locks.subList(0, Math.min(MAX_LOCKS, locks.size())));
    }

    /** How the network's Midsoft IQL Server stands, and the word the studio shows for it. */
    @TextHolder
    public enum EngineState implements IStableId {
        NOT_INSTALLED(0, TextKey.of("jsc.isms.engine.not_installed", "not installed")),
        RUNNING(1, TextKey.of("jsc.isms.engine.running", "Running")),
        STOPPED(2, TextKey.of("jsc.isms.engine.stopped", "Stopped"));

        private final int id;
        private final TextKey word;

        EngineState(final int id, final TextKey word) {
            this.id = id;
            this.word = word;
        }

        @Override
        public int id() {
            return this.id;
        }

        /** What the studio calls it. */
        public Text word() {
            return this.word.text();
        }
    }

    /**
     * The Midsoft IQL Server on the network's Mainframe.
     *
     * @param name       its name
     * @param version    the version the Mainframe's age runs, as its banner says it
     * @param compatible whether it is the engine the network runs, which the studio needs
     * @param running    when it is not, the engine that is, with its version; empty with none running
     */
    public record Engine(EngineState state, String name, String version, boolean compatible, String running) {

        public static Engine offline() {
            return new Engine(EngineState.NOT_INSTALLED, "", "", false, "");
        }
    }

    /** A saved view or procedure: its name and what it runs, as its statement wrote it. */
    public record Saved(String name, String body) {
    }

    /** A saved job: its name, whether it is paused, what fires it and what it runs, as its statement wrote them. */
    public record Job(String name, boolean paused, String trigger, String body) {
    }

    /** The index: how far it is to be trusted, how many item kinds it knows and on how many servers. */
    public record Index(IndexHealth.State state, int catalog, int servers) {
    }

    /** An item held by hand: its id, its name and how many are held. */
    public record Lock(String item, Text name, long qty) {
    }

    @Override
    public CustomPacketPayload.Type<IsmsSchemaPayload> type() {
        return TYPE;
    }

    private static String clip(final String text) {
        return TextBounds.clip(text, MAX_NAME);
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(window);
        TextCodecs.STREAM_CODEC.encode(buf, network);
        string(buf, host);
        StableCodecs.byId(EngineState.class, EngineState.NOT_INSTALLED).encode(buf, engine.state());
        string(buf, engine.name());
        string(buf, engine.version());
        buf.writeBoolean(engine.compatible());
        string(buf, engine.running());
        buf.writeVarInt(tableRows.size());
        tableRows.forEach(buf::writeVarInt);
        saved(buf, views);
        saved(buf, procedures);
        buf.writeVarInt(jobs.size());
        for (final Job job : jobs) {
            string(buf, job.name());
            buf.writeBoolean(job.paused());
            string(buf, job.trigger());
            body(buf, job.body());
        }
        strings(buf, servers);
        buf.writeVarInt(index.state().id());
        buf.writeVarInt(index.catalog());
        buf.writeVarInt(index.servers());
        buf.writeVarInt(locks.size());
        for (final Lock lock : locks) {
            string(buf, lock.item());
            TextCodecs.STREAM_CODEC.encode(buf, lock.name());
            buf.writeVarLong(lock.qty());
        }
    }

    private static IsmsSchemaPayload read(final RegistryFriendlyByteBuf buf) {
        final int window = buf.readVarInt();
        final Text network = TextCodecs.STREAM_CODEC.decode(buf);
        final String host = string(buf);
        final EngineState state = StableCodecs.byId(EngineState.class, EngineState.NOT_INSTALLED).decode(buf);
        final Engine engine = new Engine(state, string(buf), string(buf), buf.readBoolean(), string(buf));
        final int tables = Math.min(buf.readVarInt(), MAX_TABLES);
        final List<Integer> tableRows = new ArrayList<>(tables);
        for (int i = 0; i < tables; i++) {
            tableRows.add(buf.readVarInt());
        }
        final List<Saved> views = saved(buf);
        final List<Saved> procedures = saved(buf);
        final int jobCount = Math.min(buf.readVarInt(), MAX_OBJECTS);
        final List<Job> jobs = new ArrayList<>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            jobs.add(new Job(string(buf), buf.readBoolean(), string(buf), body(buf)));
        }
        final List<String> servers = strings(buf, MAX_SERVERS);
        final Index index = new Index(IndexHealth.State.byId(buf.readVarInt()), buf.readVarInt(), buf.readVarInt());
        final int lockCount = Math.min(buf.readVarInt(), MAX_LOCKS);
        final List<Lock> locks = new ArrayList<>(lockCount);
        for (int i = 0; i < lockCount; i++) {
            locks.add(new Lock(string(buf), TextCodecs.STREAM_CODEC.decode(buf), buf.readVarLong()));
        }
        return new IsmsSchemaPayload(window, network, host, engine, tableRows, views, procedures, jobs, servers,
                index, locks);
    }

    private static void string(final RegistryFriendlyByteBuf buf, final String text) {
        ByteBufCodecs.stringUtf8(MAX_NAME).encode(buf, clip(text));
    }

    private static String string(final RegistryFriendlyByteBuf buf) {
        return ByteBufCodecs.stringUtf8(MAX_NAME).decode(buf);
    }

    private static void body(final RegistryFriendlyByteBuf buf, final String text) {
        final String body = TextBounds.clip(text, MAX_BODY);
        ByteBufCodecs.stringUtf8(MAX_BODY).encode(buf, body);
    }

    private static String body(final RegistryFriendlyByteBuf buf) {
        return ByteBufCodecs.stringUtf8(MAX_BODY).decode(buf);
    }

    private static void saved(final RegistryFriendlyByteBuf buf, final List<Saved> objects) {
        buf.writeVarInt(objects.size());
        for (final Saved object : objects) {
            string(buf, object.name());
            body(buf, object.body());
        }
    }

    private static List<Saved> saved(final RegistryFriendlyByteBuf buf) {
        final int count = Math.min(buf.readVarInt(), MAX_OBJECTS);
        final List<Saved> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(new Saved(string(buf), body(buf)));
        }
        return out;
    }

    private static void strings(final RegistryFriendlyByteBuf buf, final List<String> texts) {
        buf.writeVarInt(texts.size());
        for (final String text : texts) {
            string(buf, text);
        }
    }

    private static List<String> strings(final RegistryFriendlyByteBuf buf, final int most) {
        final int count = Math.min(buf.readVarInt(), most);
        final List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(string(buf));
        }
        return out;
    }
}
