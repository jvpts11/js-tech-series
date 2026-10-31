/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.descent.DescentFlight;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipLayout;
import dev.jstech.space.shipyard.ShipyardLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The air engines of a ship hovering on its nozzles, on a player's game: each nozzle blows down a jet of flame, and,
 * near the land, the dust under the ship is blown out round it, as the approved landing on feet draws them. Shown for
 * the ships whose consoles are drawn, while their consoles say their nozzles hold them up.
 */
public final class ShipExhaust {

    /* The jet of flame each nozzle blows down, as the approved landing on feet draws it. */
    private static final ResourceLocation JET = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "lift_jet");
    /* How long an unseen console still blows, how often the dust rises, in ticks; how near the land, in blocks. */
    private static final int SEEN_FOR = 5;
    private static final int DUST_EVERY = 10;
    private static final double DUST_HEIGHT = 8;
    /* The letter of a nozzle in an air engine's kit. */
    private static final char NOZZLE = 'V';
    private static final Vec3 UP = new Vec3(0, 1, 0);
    /* The consoles drawn and what they blow; the nozzles of each design, in the ship's places. */
    private static final Map<AirReserveEntity, Blown> SEEN = new WeakHashMap<>();
    private static final Map<ShipDesign, List<ShipLayout.Cell>> NOZZLES = new HashMap<>();

    private ShipExhaust() {
    }

    /** Hands the game's bus the jets, once, from the mod's player-side entry point. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /** {@code console} was drawn: its ship's jets are blown while it is. */
    public static void seen(final AirReserveEntity console) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            SEEN.computeIfAbsent(console, key -> new Blown()).seen = level.getGameTime();
        }
    }

    private static void tick() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || SEEN.isEmpty()) {
            return;
        }
        final long now = level.getGameTime();
        final Iterator<Map.Entry<AirReserveEntity, Blown>> each = SEEN.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<AirReserveEntity, Blown> entry = each.next();
            final AirReserveEntity console = entry.getKey();
            final Blown blown = entry.getValue();
            if (console.isRemoved() || now - blown.seen > SEEN_FOR) {
                blown.stop();
                each.remove();
            } else if (console.flight().lifting() && console.ship().isPresent()) {
                blow(level, console, console.ship().get(), blown, now);
            } else {
                blown.stop();
            }
        }
    }

    /* Holds the jets of {@code console}'s ship, one a nozzle, and raises the dust under it near the land. */
    private static void blow(final ClientLevel level, final AirReserveEntity console, final ShipAboard ship,
                             final Blown blown, final long now) {
        final BlockPos at = console.getBlockPos();
        final ClientStructure structure = ClientMovingStructures.byPlot(YardLayout.plotOfBlock(at.getX(), at.getZ()));
        if (structure == null || !structure.ready()) {
            blown.stop();
            return;
        }
        final List<ShipLayout.Cell> nozzles = nozzles(level, ship.design());
        while (blown.jets.size() < nozzles.size()) {
            blown.jets.add(new Object());
        }
        final boolean dust = now - blown.dust >= DUST_EVERY;
        for (int i = 0; i < nozzles.size(); i++) {
            final ShipLayout.Cell cell = nozzles.get(i);
            // Each jet follows its nozzle as the ship moves, asked where it is every tick.
            if (!ClientEffects.holds(blown.jets.get(i))) {
                ClientEffects.hold(blown.jets.get(i), JET, () -> nozzle(structure, at, ship, cell),
                        () -> down(structure));
            }
            final Vec3 jet = nozzle(structure, at, ship, cell);
            final int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(jet.x),
                    (int) Math.floor(jet.z));
            if (dust && jet.y - ground < DUST_HEIGHT) {
                ClientEffects.play(DescentFlight.DUST, new Vec3(jet.x, ground, jet.z), UP);
                blown.dust = now;
            }
        }
    }

    /* Where in the world the bottom of the nozzle at {@code cell} of {@code ship} is, its console at {@code at}. */
    private static Vec3 nozzle(final ClientStructure structure, final BlockPos at, final ShipAboard ship,
                               final ShipLayout.Cell cell) {
        final Orientation turn = ship.orientation();
        final int[] off = turn.apply(cell.x(), cell.y(), cell.z());
        final double x = at.getX() + ship.nose().getX() + off[0] + 0.5;
        final double y = at.getY() + ship.nose().getY() + off[1];
        final double z = at.getZ() + ship.nose().getZ() + off[2] + 0.5;
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /* The way down from the ship, as its frame is turned in the world. */
    private static Vec3 down(final ClientStructure structure) {
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.turnX(0, -1, 0), frame.turnY(0, -1, 0), frame.turnZ(0, -1, 0));
    }

    /* The nozzles of the air engines of a ship of {@code design}, in the ship's places, read once. */
    private static List<ShipLayout.Cell> nozzles(final ClientLevel level, final ShipDesign design) {
        return NOZZLES.computeIfAbsent(design, key -> {
            final Map<ResourceLocation, ShipPart> parts = ShipParts.all(level);
            final List<ResourceLocation> all = new ArrayList<>(design.spine());
            all.addAll(design.hung());
            final List<ShipLayout.Cell> found = new ArrayList<>();
            for (final ShipLayout.Block block : ShipyardLayout.blocks(parts, ShipKits.all(level), design)) {
                final ShipPart part = block.part() < all.size() ? parts.get(all.get(block.part())) : null;
                if (part != null && part.kind() == ShipPartKind.AIR_ENGINE && block.letter() == NOZZLE) {
                    found.add(block.cell());
                }
            }
            return List.copyOf(found);
        });
    }

    /* What a drawn console blows: the tick it was last drawn at, the tick its dust last rose, its jets' holders. */
    private static final class Blown {

        private final List<Object> jets = new ArrayList<>();
        private long seen;
        private long dust;

        /* Lets its jets go: they blow no more, and their flames burn out. */
        private void stop() {
            this.jets.forEach(ClientEffects::release);
        }
    }
}
