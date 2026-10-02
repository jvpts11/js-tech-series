/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the Network Manager's Services tab shows of the software a Mainframe runs for its network.
 *
 * <p>The engine that plans the network's work, with what it spends and does; the engines installed beside it; the
 * Subframes, each with the engine it runs and whether it takes the Mainframe's work; the other services; and a
 * replacement of the engine when one is under way. Sent in reply to {@link RequestNetworkServicesPayload} and after
 * every {@link EngineActionPayload}.
 *
 * @param hostPos     the Mainframe it describes
 * @param engine      the engine chosen to plan the network's work
 * @param installed   every engine installed on the Mainframe
 * @param subframes   the Subframes on the network
 * @param services    the other services a Mainframe runs
 * @param replacement the replacement under way, or {@link Replacement#NONE}
 */
public record NetworkServicesPayload(BlockPos hostPos, Engine engine, List<EngineRow> installed,
                                     List<SubframeRow> subframes, List<ServiceRow> services,
                                     Replacement replacement) implements CustomPacketPayload {

    /** The longest name, version or vendor the tab is sent; anything longer is cut on the server. */
    public static final int MAX_NAME = 64;
    public static final int MAX_ENGINES = 16;
    public static final int MAX_SUBFRAMES = 32;
    public static final int MAX_SERVICES = 8;
    public static final int MAX_CAPABILITIES = 16;

    /** The chosen engine plans the network's work. */
    public static final byte ENGINE_RUNNING = 0;
    /** An engine is chosen but stopped, so the network has none. */
    public static final byte ENGINE_STOPPED = 1;
    /** The engine is being replaced, so the network has none until the new one is up. */
    public static final byte ENGINE_REPLACING = 2;
    /** No engine is chosen. */
    public static final byte ENGINE_NONE = 3;

    /** An installed engine that is the one chosen, and running. */
    public static final byte ROW_ACTIVE = 0;
    /** An installed engine that is the one chosen, but stopped. */
    public static final byte ROW_STOPPED = 1;
    /** An installed engine that is not the one chosen. */
    public static final byte ROW_INSTALLED = 2;
    /** An installed engine a replacement is bringing up. */
    public static final byte ROW_STARTING = 3;

    /** A service installed and serving. */
    public static final byte SERVICE_RUNNING = 0;
    /** A service that is not installed. */
    public static final byte SERVICE_ABSENT = 1;

    public static final CustomPacketPayload.Type<NetworkServicesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "network_services"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkServicesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NetworkServicesPayload::hostPos,
                    Engine.STREAM_CODEC, NetworkServicesPayload::engine,
                    EngineRow.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENGINES)), NetworkServicesPayload::installed,
                    SubframeRow.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SUBFRAMES)),
                    NetworkServicesPayload::subframes,
                    ServiceRow.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SERVICES)), NetworkServicesPayload::services,
                    Replacement.STREAM_CODEC, NetworkServicesPayload::replacement,
                    NetworkServicesPayload::new);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public NetworkServicesPayload {
        installed = List.copyOf(installed);
        subframes = List.copyOf(subframes);
        services = List.copyOf(services);
    }

    /** {@code text} cut to what the tab is sent, so a long name never fails the whole payload. */
    public static String clip(final String text) {
        return text == null ? "" : text.length() <= MAX_NAME ? text : text.substring(0, MAX_NAME);
    }

    @Override
    public CustomPacketPayload.Type<NetworkServicesPayload> type() {
        return TYPE;
    }

    /**
     * The engine chosen to plan the network's work, with what it spends and does.
     *
     * @param program      its package, or empty when none is chosen
     * @param name         what it is called
     * @param version      the version installed
     * @param vendor       who makes it
     * @param host         the Mainframe it runs on, as the network names it
     * @param state        one of the {@code ENGINE_} states
     * @param dialect      the language it reads
     * @param memoryMb     the memory it takes while it runs
     * @param capabilities the stable names of what it offers past what every engine answers
     * @param itemTypes    the kinds of item its indexes hold
     * @param servers      the servers its indexes cover
     * @param upTicks      how long it has been up
     * @param plansToday   how many requests it planned today
     * @param inFlight     how many Operations are in flight on the network
     */
    public record Engine(String program, String name, String version, String vendor, String host, byte state,
                         String dialect, int memoryMb, List<String> capabilities, int itemTypes, int servers,
                         long upTicks, int plansToday, int inFlight) {

        // Hand-written because the field count is past what StreamCodec.composite overloads accept.
        public static final StreamCodec<RegistryFriendlyByteBuf, Engine> STREAM_CODEC =
                StreamCodec.of(Engine::encode, Engine::decode);

        public Engine {
            capabilities = List.copyOf(capabilities);
        }

        /** Whether an engine is chosen at all. */
        public boolean chosen() {
            return state != ENGINE_NONE;
        }

        private static void encode(final RegistryFriendlyByteBuf buf, final Engine engine) {
            buf.writeUtf(engine.program, MAX_NAME * 2);
            buf.writeUtf(engine.name, MAX_NAME);
            buf.writeUtf(engine.version, MAX_NAME);
            buf.writeUtf(engine.vendor, MAX_NAME);
            buf.writeUtf(engine.host, MAX_NAME);
            buf.writeByte(engine.state);
            buf.writeUtf(engine.dialect, MAX_NAME);
            buf.writeVarInt(engine.memoryMb);
            buf.writeVarInt(Math.min(engine.capabilities.size(), MAX_CAPABILITIES));
            for (int i = 0; i < engine.capabilities.size() && i < MAX_CAPABILITIES; i++) {
                buf.writeUtf(engine.capabilities.get(i), MAX_NAME);
            }
            buf.writeVarInt(engine.itemTypes);
            buf.writeVarInt(engine.servers);
            buf.writeVarLong(engine.upTicks);
            buf.writeVarInt(engine.plansToday);
            buf.writeVarInt(engine.inFlight);
        }

        private static Engine decode(final RegistryFriendlyByteBuf buf) {
            final String program = buf.readUtf(MAX_NAME * 2);
            final String name = buf.readUtf(MAX_NAME);
            final String version = buf.readUtf(MAX_NAME);
            final String vendor = buf.readUtf(MAX_NAME);
            final String host = buf.readUtf(MAX_NAME);
            final byte state = buf.readByte();
            final String dialect = buf.readUtf(MAX_NAME);
            final int memoryMb = buf.readVarInt();
            final int count = Math.min(buf.readVarInt(), MAX_CAPABILITIES);
            final List<String> capabilities = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                capabilities.add(buf.readUtf(MAX_NAME));
            }
            return new Engine(program, name, version, vendor, host, state, dialect, memoryMb, capabilities,
                    buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt());
        }
    }

    /**
     * One engine installed on the Mainframe.
     *
     * @param program its package
     * @param name    what it is called
     * @param vendor  who makes it
     * @param version the version installed
     * @param state   one of the {@code ROW_} states
     */
    public record EngineRow(String program, String name, String vendor, String version, byte state) {
        public static final StreamCodec<RegistryFriendlyByteBuf, EngineRow> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(MAX_NAME * 2), EngineRow::program,
                        ByteBufCodecs.stringUtf8(MAX_NAME), EngineRow::name,
                        ByteBufCodecs.stringUtf8(MAX_NAME), EngineRow::vendor,
                        ByteBufCodecs.stringUtf8(MAX_NAME), EngineRow::version,
                        ByteBufCodecs.BYTE, EngineRow::state,
                        EngineRow::new);
    }

    /**
     * One Subframe on the network.
     *
     * @param name      the network's name for it
     * @param software  the package of the engine it runs, or empty when it runs whatever its Mainframe runs
     * @param engine    that engine's name and version, or empty with no software of its own
     * @param takesWork whether it takes the Mainframe's work now
     */
    public record SubframeRow(String name, String software, String engine, boolean takesWork) {
        public static final StreamCodec<RegistryFriendlyByteBuf, SubframeRow> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(MAX_NAME), SubframeRow::name,
                        ByteBufCodecs.stringUtf8(MAX_NAME * 2), SubframeRow::software,
                        ByteBufCodecs.stringUtf8(MAX_NAME), SubframeRow::engine,
                        ByteBufCodecs.BOOL, SubframeRow::takesWork,
                        SubframeRow::new);
    }

    /**
     * One of the other services a Mainframe runs.
     *
     * @param name    what it is called
     * @param vendor  who makes it
     * @param version its version
     * @param state   one of the {@code SERVICE_} states
     */
    public record ServiceRow(String name, String vendor, String version, byte state) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ServiceRow> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(MAX_NAME), ServiceRow::name,
                        ByteBufCodecs.stringUtf8(MAX_NAME), ServiceRow::vendor,
                        ByteBufCodecs.stringUtf8(MAX_NAME), ServiceRow::version,
                        ByteBufCodecs.BYTE, ServiceRow::state,
                        ServiceRow::new);
    }

    /**
     * A replacement of the engine under way.
     *
     * @param from      the name of the engine being replaced, or empty when there was none
     * @param to        the name and version of the engine coming up
     * @param elapsed   how many of its ticks have gone by
     * @param ticks     how long it takes in all; none when there is no replacement
     * @param inFlight  how many Operations were in flight when it began
     * @param itemTypes how many kinds of item the new engine indexes
     */
    public record Replacement(String from, String to, int elapsed, int ticks, int inFlight, int itemTypes) {
        public static final Replacement NONE = new Replacement("", "", 0, 0, 0, 0);

        public static final StreamCodec<RegistryFriendlyByteBuf, Replacement> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(MAX_NAME), Replacement::from,
                        ByteBufCodecs.stringUtf8(MAX_NAME * 2), Replacement::to,
                        ByteBufCodecs.VAR_INT, Replacement::elapsed,
                        ByteBufCodecs.VAR_INT, Replacement::ticks,
                        ByteBufCodecs.VAR_INT, Replacement::inFlight,
                        ByteBufCodecs.VAR_INT, Replacement::itemTypes,
                        Replacement::new);

        /** Whether one is under way. */
        public boolean underWay() {
            return ticks > 0;
        }
    }
}
