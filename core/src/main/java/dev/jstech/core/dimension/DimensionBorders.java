/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A dimension's own border, which the game does not give: every dimension's border follows the overworld's, and a
 * border is a square. A dimension given bounds here keeps them, saved with it, whatever the overworld's border does;
 * its game border spans their width, so the game's own wall, collision and warning stand at its east and west ends,
 * and the Core holds players inside its north and south ends, which may be nearer, as a round world's poles are. Once
 * fixed, a dimension's bounds stay as they were.
 *
 * <pre>{@code
 * DimensionBorders.fix(level, new DimensionBorders.Bounds(-8_000_000, 8_000_000, -4_000_000, 4_000_000));
 * }</pre>
 */
public final class DimensionBorders {

    private static final DimensionState<Optional<Bounds>> STATE = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "dimension_border"),
                    Bounds.CODEC.optionalFieldOf("bounds").codec(), Optional.<Bounds>empty())
            .fileName("jscore_dimension_border")
            .dimension();
    /* How far inside a north or south end a player who went past it is put back. */
    private static final double BACK_INSIDE = 0.5;

    /* The game borders of the dimensions that keep their own, so the overworld's changes leave them alone. */
    private static final Set<WorldBorder> OWNED = Collections.synchronizedSet(Collections.newSetFromMap(
            new WeakHashMap<>()));
    /* The bounds in force on the running server, by dimension, for what has no level at hand: its generator. */
    private static final Map<ResourceKey<Level>, Bounds> IN_FORCE = new ConcurrentHashMap<>();

    private DimensionBorders() {
    }

    /** Registers the state the bounds are kept in, from the Core's constructor. */
    public static void register() {
        CoreStates.register(STATE);
    }

    /** Hands the game's bus what keeps the borders, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> event.getServer().getAllLevels()
                .forEach(DimensionBorders::apply));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> IN_FORCE.clear());
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getServer().isReady()) {
                apply(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                holdInside(player);
            }
        });
        // The game tells a player the overworld's border wherever they arrive; one arriving in a dimension that keeps
        // its own is told that one, after.
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> tell(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> tell(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> tell(event.getEntity()));
    }

    /**
     * Gives {@code level} the bounds {@code bounds} if it has none yet, and puts them in force.
     *
     * @return the bounds in force: those it already had, or these
     */
    public static Bounds fix(final ServerLevel level, final Bounds bounds) {
        final Bounds kept = STATE.update(level, held -> held.isPresent() ? held : Optional.of(bounds))
                .orElse(bounds);
        apply(level);
        return kept;
    }

    /** The bounds {@code level} keeps, if it keeps any. */
    public static Optional<Bounds> of(final ServerLevel level) {
        return STATE.get(level);
    }

    /**
     * The bounds in force in {@code dimension} on the running server, if it keeps any: what a world's generator, which
     * has no level at hand, reads its size from. They are put in force as the dimension loads, before its ground is
     * made.
     */
    public static Optional<Bounds> inForce(final ResourceKey<Level> dimension) {
        return Optional.ofNullable(IN_FORCE.get(dimension));
    }

    /** Whether {@code border} belongs to a dimension that keeps its own. */
    public static boolean owns(final WorldBorder border) {
        return OWNED.contains(border);
    }

    /* Sets the dimension's game border across its width, and marks it as its own. */
    private static void apply(final ServerLevel level) {
        of(level).ifPresent(bounds -> {
            IN_FORCE.put(level.dimension(), bounds);
            final WorldBorder border = level.getWorldBorder();
            OWNED.add(border);
            border.setCenter((bounds.minX() + bounds.maxX()) / 2, (bounds.minZ() + bounds.maxZ()) / 2);
            border.setSize(Math.max(bounds.maxX() - bounds.minX(), bounds.maxZ() - bounds.minZ()));
            level.players().forEach(DimensionBorders::tell);
        });
    }

    /* Tells {@code player} the border of the dimension they stand in, when it keeps its own. */
    private static void tell(final Player player) {
        if (player instanceof ServerPlayer server && of(server.serverLevel()).isPresent()) {
            server.connection.send(new ClientboundInitializeBorderPacket(server.serverLevel().getWorldBorder()));
        }
    }

    /* A player past a north or south end is put back inside it, at a stop. */
    private static void holdInside(final ServerPlayer player) {
        final Optional<Bounds> bounds = of(player.serverLevel());
        if (bounds.isEmpty()) {
            return;
        }
        final double z = player.getZ();
        final double inside = Math.max(bounds.get().minZ() + BACK_INSIDE, Math.min(bounds.get().maxZ() - BACK_INSIDE,
                z));
        if (inside != z) {
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(player.serverLevel(), player.getX(), player.getY(), inside, player.getYRot(),
                    player.getXRot());
        }
    }

    /**
     * A dimension's bounds, in blocks: from west to east and from north to south.
     *
     * @param minX its west end
     * @param maxX its east end
     * @param minZ its north end
     * @param maxZ its south end
     */
    public record Bounds(double minX, double maxX, double minZ, double maxZ) {

        public static final Codec<Bounds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("min_x").forGetter(Bounds::minX),
                Codec.DOUBLE.fieldOf("max_x").forGetter(Bounds::maxX),
                Codec.DOUBLE.fieldOf("min_z").forGetter(Bounds::minZ),
                Codec.DOUBLE.fieldOf("max_z").forGetter(Bounds::maxZ))
                .apply(instance, Bounds::new));

        public Bounds {
            if (!(maxX > minX) || !(maxZ > minZ)) {
                throw new IllegalArgumentException("a dimension's bounds run from west to east and north to south;"
                        + " got " + minX + ".." + maxX + " by " + minZ + ".." + maxZ);
            }
        }

        /** Whether the point ({@code x}, {@code z}) lies inside them. */
        public boolean contains(final double x, final double z) {
            return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
        }
    }
}
