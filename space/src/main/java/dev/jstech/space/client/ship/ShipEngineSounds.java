/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipLayout;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.ship.ShipSounds;
import dev.jstech.space.shipyard.ShipyardLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A small ship's engines, gear and console heard on a player's game, for every ship loaded there whether the player
 * looks at it or not. Its engines hum while it flies or holds itself up and roar with its throttle open, heard from its
 * main engines, muffled through the hull for whoever is inside it; its boost is heard as it lights, its gear as it
 * starts up or down, and a switch of its console as its pilot throws one (the gear, the autopilot). The doors, the
 * stair and a hard landing are played by the server ({@link ShipSounds}).
 */
public final class ShipEngineSounds {

    private static final Map<AirReserveEntity, Engines> HEARD = new WeakHashMap<>();
    /* The blocks of each design's main engines, in the ship's places, found once. */
    private static final Map<ShipDesign, List<ShipLayout.Cell>> ENGINES = new HashMap<>();
    /* How loud the idle and the thrust are, the thrust from its least with the throttle barely open to full. */
    private static final float IDLE = 0.8F;
    private static final float THRUST_LEAST = 0.4F;
    private static final float PERCENT = 100;
    private static final double HALF = 0.5;
    /* The world and the resources the engines were found with. */
    private static WeakReference<ClientLevel> readIn = new WeakReference<>(null);
    private static long readAt = -1;

    private ShipEngineSounds() {
    }

    /** Hands the game's bus what this answers, once, from the mod's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /** What the engines of the ship whose console is {@code console} play now, for a test to look at. */
    public static List<SoundKey> playing(final AirReserveEntity console) {
        final Engines engines = HEARD.get(console);
        return engines == null ? List.of() : engines.loops().stream().map(LoopRequest::sound).toList();
    }

    private static void tick() {
        final ClientLevel level = Minecraft.getInstance().level;
        for (final AirReserveEntity console : AirReserveEntity.onClient()) {
            if (console.getLevel() == level && !HEARD.containsKey(console)) {
                final Engines engines = new Engines(console);
                HEARD.put(console, engines);
                SoundDirector.track(engines);
            }
        }
        final Iterator<Map.Entry<AirReserveEntity, Engines>> each = HEARD.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<AirReserveEntity, Engines> entry = each.next();
            if (entry.getKey().isRemoved() || entry.getKey().getLevel() != level
                    || !AirReserveEntity.onClient().contains(entry.getKey())) {
                SoundDirector.untrack(entry.getValue());
                each.remove();
            } else {
                entry.getValue().heard();
            }
        }
    }

    /* Plays {@code sound} once, from the ship's place {@code place} in its yard, where the ship carries it. */
    private static void once(final ClientLevel level, final SoundKey sound, final Vec3 place) {
        final Vec3 at = MovingStructures.inWorld(level, place);
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound.event().get(),
                sound.spec().channel().source(), 1.0F, 1.0F, SoundInstance.createUnseededRandom(), at.x, at.y,
                at.z));
    }

    /* The blocks of the main engines of a ship of {@code design}, in the ship's places; none for a ship with none. */
    private static List<ShipLayout.Cell> engines(final ClientLevel level, final ShipDesign design) {
        // The parts a design is read with are the world's and the resources', so another of either reads it again.
        if (readIn.get() != level || readAt != KitModels.reloadedAt()) {
            ENGINES.clear();
            readIn = new WeakReference<>(level);
            readAt = KitModels.reloadedAt();
        }
        return ENGINES.computeIfAbsent(design, key -> {
            final Map<ResourceLocation, ShipPart> parts = ShipParts.all(level);
            final List<ResourceLocation> all = new ArrayList<>(design.spine());
            all.addAll(design.hung());
            final List<ShipLayout.Cell> found = new ArrayList<>();
            for (final ShipLayout.Block block : ShipyardLayout.blocks(parts, ShipKits.all(level), design)) {
                final ShipPart part = block.part() < all.size() ? parts.get(all.get(block.part())) : null;
                if (part != null && part.kind() == ShipPartKind.MAIN_ENGINE) {
                    found.add(block.cell());
                }
            }
            return List.copyOf(found);
        });
    }

    /* A ship heard: its engines' loops, and what it played last, to hear what changes. */
    private static final class Engines implements IAudible {

        private final AirReserveEntity console;
        private ShipReadings last;

        private Engines(final AirReserveEntity console) {
            this.console = console;
            this.last = console.flight();
        }

        @Override
        public List<LoopRequest> loops() {
            final ShipReadings now = this.console.flight();
            if (!running(now) || this.console.ship().isEmpty()) {
                return List.of();
            }
            final boolean inside = inside();
            if (now.throttle() > 0 || now.boosting()) {
                final float open = now.boosting() ? 1.0F : now.throttle() / PERCENT;
                return List.of(new LoopRequest(inside ? ShipSounds.ENGINE_THRUST_INSIDE : ShipSounds.ENGINE_THRUST,
                        THRUST_LEAST + (1 - THRUST_LEAST) * open, 1.0F, null));
            }
            return List.of(new LoopRequest(inside ? ShipSounds.ENGINE_IDLE_INSIDE : ShipSounds.ENGINE_IDLE, IDLE,
                    1.0F, null));
        }

        @Override
        public double audioX() {
            return enginesPlace().x;
        }

        @Override
        public double audioY() {
            return enginesPlace().y;
        }

        @Override
        public double audioZ() {
            return enginesPlace().z;
        }

        /* Hears what changed since the last tick: the boost lit, the gear moved, a switch thrown. */
        private void heard() {
            final ShipReadings now = this.console.flight();
            final ClientLevel level = (ClientLevel) this.console.getLevel();
            if (level != null && this.console.ship().isPresent()) {
                if (now.boosting() && !this.last.boosting()) {
                    once(level, ShipSounds.ENGINE_BOOST, enginesPlace());
                }
                if (now.gearDown() != this.last.gearDown()) {
                    once(level, ShipSounds.GEAR, middle());
                }
                // Compared by what they say: each update of the readings is a new pilot, the same one or not.
                if (now.gearDown() != this.last.gearDown() || !Objects.equals(now.pilot(), this.last.pilot())) {
                    once(level, ShipSounds.CONSOLE_SWITCH, Vec3.atCenterOf(this.console.getBlockPos()));
                }
            }
            this.last = now;
        }

        /* Whether its engines run: in the air, held up on its nozzles, or with its throttle open. */
        private static boolean running(final ShipReadings now) {
            return !now.grounded() || now.lifting() || now.throttle() > 0 || now.boosting();
        }

        /* Where the middle of its main engines is in its yard: its console's block if it has none. */
        private Vec3 enginesPlace() {
            final BlockPos at = this.console.getBlockPos();
            final Optional<ShipAboard> ship = this.console.ship();
            final ClientLevel level = (ClientLevel) this.console.getLevel();
            if (ship.isEmpty() || level == null) {
                return Vec3.atCenterOf(at);
            }
            final List<ShipLayout.Cell> cells = engines(level, ship.get().design());
            if (cells.isEmpty()) {
                return Vec3.atCenterOf(at);
            }
            double x = 0;
            double y = 0;
            double z = 0;
            for (final ShipLayout.Cell cell : cells) {
                final int[] turned = ship.get().orientation().apply(cell.x(), cell.y(), cell.z());
                x += turned[0];
                y += turned[1];
                z += turned[2];
            }
            final int count = cells.size();
            return new Vec3(at.getX() + ship.get().nose().getX() + x / count + HALF,
                    at.getY() + ship.get().nose().getY() + y / count + HALF,
                    at.getZ() + ship.get().nose().getZ() + z / count + HALF);
        }

        /* The middle of its box in its yard, where its gear is heard from. */
        private Vec3 middle() {
            final ClientStructure structure = structure();
            if (structure == null) {
                return Vec3.atCenterOf(this.console.getBlockPos());
            }
            final Box box = structure.bounds();
            return new Vec3((box.minX() + box.maxX() + 1) * HALF, box.minY(), (box.minZ() + box.maxZ() + 1) * HALF);
        }

        /* Whether the player's eyes are inside the ship's box, aboard it. */
        private boolean inside() {
            final ClientStructure structure = structure();
            final Minecraft mc = Minecraft.getInstance();
            if (structure == null || mc.player == null) {
                return false;
            }
            final Vec3 yard = MovingStructures.toYard(structure, mc.gameRenderer.getMainCamera().getPosition());
            return structure.bounds().contains((int) Math.floor(yard.x), (int) Math.floor(yard.y),
                    (int) Math.floor(yard.z));
        }

        private ClientStructure structure() {
            final BlockPos at = this.console.getBlockPos();
            return ClientMovingStructures.byPlot(YardLayout.plotOfBlock(at.getX(), at.getZ()));
        }
    }
}
