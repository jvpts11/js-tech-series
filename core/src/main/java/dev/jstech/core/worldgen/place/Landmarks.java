/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.place;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import dev.jstech.core.state.TeamState;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The landmarks of the worlds, by id: each declared in code by its mod, or a file a data pack writes over or adds,
 * sent to every player for their maps. A player who comes near one discovers it, for themselves and their team; the
 * discovery is noted, posted on the game's event bus ({@link Discovered}) and shown as a notification that points at
 * it.
 *
 * <pre>{@code
 * public static final ResourceLocation OLYMPUS_MONS = Landmarks.landmark(rl("olympus_mons"), MyTexts.OLYMPUS)
 *         .in(MARS).at(1200, 140, -800).radius(160).declare();
 * }</pre>
 */
@TextHolder
public final class Landmarks {

    public static final TextKey KIND = TextKey.of("jscore.landmark.kind", "Landmarks");
    public static final TextKey DISCOVERED = TextKey.of("jscore.landmark.discovered", "Landmark discovered");
    /** The kinds the data packs say, by id, sent to every player. */
    public static final DataRegistry<Landmark> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "landmarks"), "jstech/landmarks", Landmark.CODEC)
            .synced().register();
    /** The notification of a discovery. */
    public static final NotificationKind FOUND = Notifications.kind(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "landmark_discovered"), NotificationLevel.INFO, KIND)
            .declare();

    private static final Codec<List<ResourceLocation>> IDS = ResourceLocation.CODEC.listOf();
    /** The landmarks each player discovered. */
    private static final PlayerState<List<ResourceLocation>> MINE = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "landmarks_found"), IDS, List.<ResourceLocation>of())
            .synced(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())).player();
    /** The landmarks each team discovered. */
    private static final TeamState<List<ResourceLocation>> OURS = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "landmarks_found_by_team"), IDS,
            List.<ResourceLocation>of()).synced(ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())).team();
    private static final Map<ResourceLocation, Landmark> DECLARED = new ConcurrentHashMap<>();
    /* How often players are looked at for landmarks near them, in ticks. */
    private static final int EVERY = 20;

    private Landmarks() {
    }

    /** Registers the states and hands the game's bus the discovery, once, from the Core's constructor. */
    public static void listen() {
        CoreStates.register(MINE);
        CoreStates.register(OURS);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (event.getServer().getTickCount() % EVERY == 0) {
                event.getServer().getPlayerList().getPlayers().forEach(Landmarks::look);
            }
        });
    }

    /** Starts declaring the landmark {@code id}, named {@code name}. */
    public static LandmarkBuilder landmark(final ResourceLocation id, final TextKey name) {
        return new LandmarkBuilder(id, name);
    }

    /** Every landmark declared in code, by id. */
    public static Map<ResourceLocation, Landmark> declared() {
        return Map.copyOf(DECLARED);
    }

    /** Every landmark on the side {@code level} is on, by id: the files', then the declared ones no file replaces. */
    public static Map<ResourceLocation, Landmark> all(final Level level) {
        final Map<ResourceLocation, Landmark> byId = new TreeMap<>(DECLARED);
        byId.putAll(FILES.entries(level));
        return byId;
    }

    /** The landmark {@code id}, or none. */
    public static Optional<Landmark> get(final Level level, final ResourceLocation id) {
        return Optional.ofNullable(all(level).get(id));
    }

    /** The landmarks {@code player} or their team discovered. */
    public static Set<ResourceLocation> discovered(final MinecraftServer server, final UUID player) {
        final Set<ResourceLocation> found = new LinkedHashSet<>(MINE.get(server, player));
        found.addAll(OURS.ofPlayer(server, player));
        return found;
    }

    /** The landmarks this client's player or their team discovered, as the server last said. */
    public static Set<ResourceLocation> discoveredHere() {
        final Set<ResourceLocation> found = new LinkedHashSet<>(MINE.client());
        found.addAll(OURS.client());
        return found;
    }

    /**
     * Has {@code player} discover {@code id}, for themselves and their team, as coming near does; whether it was new
     * to them.
     */
    public static boolean discover(final ServerPlayer player, final ResourceLocation id) {
        final MinecraftServer server = player.getServer();
        if (server == null || discovered(server, player.getUUID()).contains(id)) {
            return false;
        }
        final Optional<Landmark> landmark = get(player.serverLevel(), id);
        if (landmark.isEmpty()) {
            return false;
        }
        MINE.update(server, player.getUUID(), found -> plus(found, id));
        OURS.update(server, CoreTeams.teamOf(server, player.getUUID()), found -> plus(found, id));
        NeoForge.EVENT_BUS.post(new Discovered(player, id, landmark.get()));
        final Notification notice = FOUND.of(DISCOVERED.text(), landmark.get().title().text());
        Notifications.sendToTeamOf(server, player.getUUID(), new Notification(notice.kind(), notice.level(),
                notice.title(), notice.body(), Optional.of(new NotificationAction.ShowPlace(landmark.get()
                .dimension(), landmark.get().pos()))));
        return true;
    }

    /** Has {@code player} and their team forget {@code id}, so they discover it again when they come near. */
    public static void forget(final MinecraftServer server, final UUID player, final ResourceLocation id) {
        MINE.update(server, player, found -> minus(found, id));
        OURS.update(server, CoreTeams.teamOf(server, player), found -> minus(found, id));
    }

    /* Discovers each landmark {@code player} has come near. */
    private static void look(final ServerPlayer player) {
        final ResourceKey<Level> in = player.level().dimension();
        final BlockPos at = player.blockPosition();
        for (final Map.Entry<ResourceLocation, Landmark> one : all(player.serverLevel()).entrySet()) {
            if (one.getValue().near(in, at)) {
                discover(player, one.getKey());
            }
        }
    }

    private static List<ResourceLocation> plus(final List<ResourceLocation> found, final ResourceLocation id) {
        if (found.contains(id)) {
            return found;
        }
        final List<ResourceLocation> next = new ArrayList<>(found);
        next.add(id);
        return List.copyOf(next);
    }

    private static List<ResourceLocation> minus(final List<ResourceLocation> found, final ResourceLocation id) {
        if (!found.contains(id)) {
            return found;
        }
        final List<ResourceLocation> next = new ArrayList<>(found);
        next.remove(id);
        return List.copyOf(next);
    }

    /**
     * Posted on the game's event bus when a player discovers a landmark.
     *
     * @see Landmarks#discover
     */
    public static final class Discovered extends Event {

        private final ServerPlayer player;
        private final ResourceLocation id;
        private final Landmark landmark;

        public Discovered(final ServerPlayer player, final ResourceLocation id, final Landmark landmark) {
            this.player = player;
            this.id = id;
            this.landmark = landmark;
        }

        public ServerPlayer player() {
            return this.player;
        }

        public ResourceLocation id() {
            return this.id;
        }

        public Landmark landmark() {
            return this.landmark;
        }
    }

    /** A landmark being declared; it starts in the overworld at its origin, discovered from 64 blocks. */
    public static final class LandmarkBuilder {

        private final ResourceLocation id;
        private final TextKey name;
        private ResourceKey<Level> dimension = Level.OVERWORLD;
        private BlockPos pos = BlockPos.ZERO;
        private int radius = 64;
        private Optional<ResourceLocation> structure = Optional.empty();

        private LandmarkBuilder(final ResourceLocation id, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
        }

        public LandmarkBuilder in(final ResourceKey<Level> where) {
            this.dimension = where;
            return this;
        }

        public LandmarkBuilder at(final int x, final int y, final int z) {
            this.pos = new BlockPos(x, y, z);
            return this;
        }

        /** It is discovered from {@code blocks} away. */
        public LandmarkBuilder radius(final int blocks) {
            this.radius = blocks;
            return this;
        }

        /** The structure that stands at it. */
        public LandmarkBuilder structure(final ResourceLocation standing) {
            this.structure = Optional.of(standing);
            return this;
        }

        /** Declares the landmark, and gives its id. */
        public ResourceLocation declare() {
            DECLARED.put(id, new Landmark(name.key(), name.english(), dimension, pos, radius, structure));
            return id;
        }
    }
}
