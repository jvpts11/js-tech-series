/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import com.mojang.serialization.DataResult;
import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.config.format.IConfigComments;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * A world's settings kept the same on every player's game, and changed from an operator's own: the server sends every
 * world's file to a player as they join and again, at the end of the tick, whenever a setting of it changes; a player's
 * game takes those values, shows them on its settings screen and uses them, and forgets them as it leaves. An operator
 * who changes them on that screen sends the changes back, which the server keeps only after checking they may.
 *
 * <p>In a game the player runs themselves the world's settings are the same objects on both sides, so nothing is sent
 * to the player running it.
 */
@TextHolder
public final class ConfigSync {

    /* The number of settings kept, and the file. */
    public static final TextKey SAVED = TextKey.of("jscore.config.remote.saved",
            "The server kept %s changed settings of %s");
    public static final TextKey REFUSED = TextKey.of("jscore.config.remote.refused",
            "You may not change the server's settings");
    public static final TextKey NO_FILE = TextKey.of("jscore.config.remote.no_file",
            "The server keeps no world settings file named %s");
    /* The setting's path, and why it was not taken. */
    public static final TextKey NOT_TAKEN = TextKey.of("jscore.config.remote.not_taken",
            "The server did not take %s: %s");
    public static final TextKey NO_SUCH_SETTING = TextKey.of("jscore.config.remote.no_such_setting",
            "the file has no such setting");

    private static final String NETWORK_VERSION = "1";
    /** The world's files with a setting changed since the end of the last tick, sent to every player at its end. */
    private static final Set<ConfigFile> CHANGED = ConcurrentHashMap.newKeySet();
    /**
     * On a player's game, the world's files whose values came from a server, put back to their defaults on leaving,
     * and whether the server lets this player change each.
     */
    private static final Map<ConfigFile, Boolean> RECEIVED = new ConcurrentHashMap<>();

    private ConfigSync() {
    }

    /** Hands the buses what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(NETWORK_VERSION)
                .playToClient(ConfigValuesPayload.TYPE, ConfigValuesPayload.STREAM_CODEC, ConfigSync::onValues)
                .playToServer(ConfigEditPayload.TYPE, ConfigEditPayload.STREAM_CODEC, ConfigSync::onEdit));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                for (final ConfigFile file : ConfigFiles.all()) {
                    if (file.side() == ConfigSide.SERVER) {
                        send(player, file);
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> sendChanged(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> CHANGED.clear());
    }

    /**
     * Whether {@code player} may change the world's settings from their own game: the player who runs it, and whoever
     * holds {@link CorePermissions#EDIT_SETTINGS}, the operators unless the server's permission mod says otherwise.
     */
    public static boolean mayEdit(final ServerPlayer player) {
        return player.server.isSingleplayerOwner(player.getGameProfile())
                || CorePermissions.has(player, CorePermissions.EDIT_SETTINGS);
    }

    /**
     * Sends {@code changes}, each setting's new value by its dotted path as a file writes it, of the world's file
     * {@code file} to the server, from a player's game, in as many payloads as it takes.
     *
     * @return whether every change fitted in a payload; one that alone is too big to send is left out
     */
    public static boolean sendEdits(final ConfigFile file, final Map<String, Object> changes) {
        boolean whole = true;
        Map<String, Object> batch = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> change : changes.entrySet()) {
            final Map<String, Object> one = Map.of(change.getKey(), change.getValue());
            if (json(one).length > ConfigEditPayload.MOST_BYTES) {
                whole = false;
                continue;
            }
            final Map<String, Object> grown = new LinkedHashMap<>(batch);
            grown.put(change.getKey(), change.getValue());
            if (json(grown).length > ConfigEditPayload.MOST_BYTES) {
                PacketDistributor.sendToServer(new ConfigEditPayload(file.name(), json(batch)));
                batch = new LinkedHashMap<>(one);
            } else {
                batch = grown;
            }
        }
        if (!batch.isEmpty()) {
            PacketDistributor.sendToServer(new ConfigEditPayload(file.name(), json(batch)));
        }
        return whole;
    }

    /** On a player's game leaving a server: the world's settings it was sent go back to their defaults. */
    @ApiStatus.Internal
    public static void forgetServerValues() {
        for (final ConfigFile file : RECEIVED.keySet()) {
            file.reset();
        }
        RECEIVED.clear();
    }

    /** Whether this game's values of the world's {@code file} came from a server it is playing on. */
    public static boolean fromServer(final ConfigFile file) {
        return RECEIVED.containsKey(file);
    }

    /** Whether the server this game plays on lets its player change the world's {@code file}, as it last said. */
    public static boolean mayEditOnServer(final ConfigFile file) {
        return RECEIVED.getOrDefault(file, false);
    }

    /** Marks a world's file to be sent to every player at the end of the tick. */
    static void changed(final ConfigFile file) {
        CHANGED.add(file);
    }

    /**
     * Applies changes made to a world's file, from a command or a player's game, each by its dotted path as a file
     * writes it, then saves the file once.
     *
     * @return what was not taken, a line each
     */
    static List<Text> apply(final ConfigFile file, final Map<String, Object> changes) {
        final List<Text> refused = new ArrayList<>();
        for (final Map.Entry<String, Object> change : changes.entrySet()) {
            final Optional<ConfigKey<?>> key = file.key(change.getKey());
            if (key.isEmpty()) {
                refused.add(NOT_TAKEN.with(change.getKey(), NO_SUCH_SETTING));
                continue;
            }
            stage(file, key.get(), change.getValue())
                    .ifPresent(why -> refused.add(NOT_TAKEN.with(change.getKey(), why)));
        }
        file.save();
        return refused;
    }

    /** Stages one change, or says why it cannot be read as the setting's value. */
    static <T> Optional<String> stage(final ConfigFile file, final ConfigKey<T> key, final Object plain) {
        final DataResult<T> read = key.read(plain);
        final Optional<T> value = read.result();
        if (value.isEmpty()) {
            return Optional.of(read.error().map(DataResult.Error::message).orElse(""));
        }
        file.stage(key, value.get());
        return Optional.empty();
    }

    private static void sendChanged(final MinecraftServer server) {
        if (CHANGED.isEmpty()) {
            return;
        }
        final List<ConfigFile> files = new ArrayList<>(CHANGED);
        CHANGED.removeAll(files);
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (final ConfigFile file : files) {
                send(player, file);
            }
        }
    }

    private static void send(final ServerPlayer player, final ConfigFile file) {
        // The player who runs the game shares the server's settings already; one without the Core has no use for them.
        if (player.connection.getConnection().isMemoryConnection()
                || !player.connection.hasChannel(ConfigValuesPayload.TYPE)) {
            return;
        }
        final byte[] values = json(file.toPlain());
        if (values.length > ConfigValuesPayload.MOST_BYTES) {
            ConfigFiles.LOGGER.warn("{} is too large to send to players ({} bytes); they keep its last values",
                    file.fileName(), values.length);
            return;
        }
        PacketDistributor.sendToPlayer(player, new ConfigValuesPayload(file.name(), values, mayEdit(player)));
    }

    /* On a player's game: a server's values of a world's file, taken as the file's own. */
    private static void onValues(final ConfigValuesPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.connection().isMemoryConnection()) {
                return;
            }
            final Optional<ConfigFile> file = ConfigFiles.byName(payload.file())
                    .filter(found -> found.side() == ConfigSide.SERVER);
            if (file.isEmpty()) {
                ConfigFiles.LOGGER.debug("the server sent the values of {}, which this game does not have",
                        payload.file());
                return;
            }
            try {
                file.get().read(ConfigFormats.JSON.read(payload.values()));
                RECEIVED.put(file.get(), payload.editable());
            } catch (final ConfigFormatException e) {
                ConfigFiles.LOGGER.warn("the server's values of {} could not be read", payload.file(), e);
            }
        });
    }

    /* On the server: changes an operator made on their own game, kept when they may make them. */
    private static void onEdit(final ConfigEditPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!mayEdit(player)) {
                ConfigFiles.LOGGER.warn("{} tried to change the world's settings of {} without the permission",
                        player.getScoreboardName(), payload.file());
                player.sendSystemMessage(GameText.component(REFUSED));
                return;
            }
            final Optional<ConfigFile> file = ConfigFiles.byName(payload.file())
                    .filter(found -> found.side() == ConfigSide.SERVER);
            if (file.isEmpty()) {
                player.sendSystemMessage(GameText.component(NO_FILE.with(payload.file())));
                return;
            }
            final Map<String, Object> changes;
            try {
                changes = ConfigFormats.JSON.read(payload.changes());
            } catch (final ConfigFormatException e) {
                ConfigFiles.LOGGER.warn("{} sent changes to {} that could not be read", player.getScoreboardName(),
                        payload.file(), e);
                return;
            }
            final List<Text> refused = apply(file.get(), changes);
            ConfigFiles.LOGGER.info("{} changed {} settings of {}", player.getScoreboardName(),
                    changes.size() - refused.size(), file.get().fileName());
            player.sendSystemMessage(GameText.component(SAVED.with(changes.size() - refused.size(),
                    file.get().fileName())));
            for (final Text line : refused) {
                player.sendSystemMessage(GameText.component(line));
            }
        });
    }

    private static byte[] json(final Map<String, Object> values) {
        return ConfigFormats.JSON.write(values, IConfigComments.NONE);
    }
}
