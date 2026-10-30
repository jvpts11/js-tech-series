/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import dev.jstech.core.client.multiblock.PlanHologram;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The hologram of the ship a Small Shipyard builds, as the approved drawing of a ship half built shows it, through the
 * Core's plan hologram: every block still to place stands where it goes as a ghost of itself, the layer of the part
 * being built glowing amber; each part not yet formed, a multiblock, has its box in the colour of its kind, with its
 * name, its size across, up and along the ship, how many of its blocks are in place, and whether it is the one being
 * built, on a tag lifted over it on a leader; a part formed needs no box, for it is its model. The line across the top
 * of the screen names the ship, the part and layer being built, and the block that layer needs most. It is shown while
 * the ship is not yet whole.
 */
@TextHolder
@PaletteHolder
public final class ShipHolograms {

    public static final TextKey SIZE = TextKey.of("jsspace.ship_hologram.size", "%sx%sx%s  %s");
    public static final TextKey PLACED = TextKey.of("jsspace.ship_hologram.placed", "%s OF %s");
    public static final TextKey BUILDING = TextKey.of("jsspace.ship_hologram.building", "%s OF %s  BUILDING");
    public static final TextKey BANNER = TextKey.of("jsspace.ship_hologram.banner",
            "%s  -  %s, layer %s of %s  -  %s x %s");
    /** The colours of the parts' boxes, by what they are. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "ship_hologram", new Colours(
            0xFF << 24 | 0x5AC8FF, 0xFF << 24 | 0x78E68C, 0xFF << 24 | 0xFFC43C, 0xFF << 24 | 0xFF7850,
            0xFF << 24 | 0xAA6EFF, 0xFF << 24 | 0xF06EC8, 0xFF << 24 | 0xA0A0FF, 0xFF << 24 | 0x6EF0DC,
            0xFF << 24 | 0xFFA078, 0xFF << 24 | 0xDCDC78, 0xFF << 24 | 0xC8C8C8));
    /* How often a hologram looks at the world again, in ticks. */
    private static final int LOOK_EVERY = 5;

    /* What each terminal's hologram last was: its plan laid out, and what it showed, and when. */
    private static final Map<ShipyardTerminalBlockEntity, Kept> KEPT = new WeakHashMap<>();

    private ShipHolograms() {
    }

    /** The holograms of the ships being built in the shipyards loaded on this player's game. */
    public static List<PlanHologram.Shown> shown() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        final List<PlanHologram.Shown> out = new ArrayList<>();
        for (final ShipyardTerminalBlockEntity terminal : ShipyardTerminalBlockEntity.loadedOnClient()) {
            if (terminal.getLevel() != level || terminal.isRemoved()) {
                continue;
            }
            hologram(level, terminal).ifPresent(out::add);
        }
        return out;
    }

    /* The hologram of {@code terminal}'s ship, looked at again when it is older than a moment; none when whole. */
    private static Optional<PlanHologram.Shown> hologram(final ClientLevel level,
                                                         final ShipyardTerminalBlockEntity terminal) {
        final Optional<ShipDesign> plan = terminal.plan();
        final Optional<BlockPos> at = terminal.planAt();
        if (plan.isEmpty() || at.isEmpty()) {
            return Optional.empty();
        }
        final long now = level.getGameTime();
        final Orientation turn = terminal.planTurn();
        Kept kept = KEPT.get(terminal);
        if (kept == null || !kept.design().equals(plan.get()) || !kept.at().equals(at.get())
                || !kept.turn().equals(turn)) {
            // Made now, it looks at the world at once.
            kept = new Kept(plan.get(), at.get(), turn, ShipyardPlans.of(level, plan.get(), turn), now - LOOK_EVERY,
                    Optional.empty());
        }
        if (now - kept.when() >= LOOK_EVERY) {
            kept = new Kept(kept.design(), kept.at(), kept.turn(), kept.laid(), now, look(level, terminal, kept));
            KEPT.put(terminal, kept);
        }
        return kept.shown();
    }

    /* Looks at the world: what each place holds, each part's box and count, the layer being built now. */
    private static Optional<PlanHologram.Shown> look(final ClientLevel level,
                                                     final ShipyardTerminalBlockEntity terminal, final Kept kept) {
        final BuildPlan plan = kept.laid();
        final List<Integer> formed = terminal.formedParts();
        final int count = plan.parts();
        if (formed.size() >= count) {
            return Optional.empty();
        }
        final boolean[] present = new boolean[plan.spots().size()];
        final int[] lowestMissing = new int[count];
        final int[] placed = new int[count];
        final int[] blocks = new int[count];
        final AABB[] boxes = new AABB[count];
        Arrays.fill(lowestMissing, Integer.MAX_VALUE);
        for (int i = 0; i < plan.spots().size(); i++) {
            final PlanSpot spot = plan.spots().get(i);
            final BlockPos pos = kept.at().offset(spot.at());
            final Block wanted = BuiltInRegistries.BLOCK.get(spot.block());
            present[i] = FormedPartBlock.originalAt(level, pos, level.getBlockState(pos)).is(wanted);
            final int part = spot.part();
            boxes[part] = boxes[part] == null ? new AABB(pos) : boxes[part].minmax(new AABB(pos));
            blocks[part]++;
            if (present[i]) {
                placed[part]++;
            } else {
                lowestMissing[part] = Math.min(lowestMissing[part], pos.getY());
            }
        }
        final int building = building(formed, placed, boxes);
        final Map<ResourceLocation, ShipPart> parts = ShipParts.all(level);
        final List<ShipPart> kinds = new ArrayList<>();
        for (int part = 0; part < count; part++) {
            kinds.add(parts.get(ShipyardPlans.partOf(kept.design(), part)));
        }
        final List<PlanHologram.Place> places = new ArrayList<>();
        final Map<Block, Integer> needed = new LinkedHashMap<>();
        for (int i = 0; i < plan.spots().size(); i++) {
            if (present[i]) {
                continue;
            }
            final PlanSpot spot = plan.spots().get(i);
            final BlockPos pos = kept.at().offset(spot.at());
            final Block wanted = BuiltInRegistries.BLOCK.get(spot.block());
            final BlockState there = level.getBlockState(pos);
            final boolean empty = there.isAir() || there.canBeReplaced();
            final boolean next = spot.part() == building && pos.getY() == lowestMissing[building];
            if (next && empty) {
                needed.merge(wanted, 1, Integer::sum);
            }
            places.add(new PlanHologram.Place(pos, wanted.defaultBlockState(), !empty ? PlanHologram.Look.WRONG
                    : next ? PlanHologram.Look.NEXT : PlanHologram.Look.MISSING, colour(kinds.get(spot.part()))));
        }
        final AABB whole = Arrays.stream(boxes).filter(Objects::nonNull).reduce(AABB::minmax)
                .orElse(new AABB(kept.at()));
        final List<PlanHologram.Part> shownParts = new ArrayList<>();
        for (int part = 0; part < count; part++) {
            final AABB box = boxes[part];
            final ShipPart kind = kinds.get(part);
            // A part formed is its model, and needs no box.
            if (box == null || kind == null || formed.contains(part)) {
                continue;
            }
            final Text state = (part == building ? BUILDING : PLACED).with(placed[part], blocks[part]);
            shownParts.add(new PlanHologram.Part(name(kind), SIZE.with(along(box, kept.turn(), 0),
                    along(box, kept.turn(), 1), along(box, kept.turn(), 2), state), colour(kind), box));
        }
        return Optional.of(new PlanHologram.Shown(whole.getCenter(), places, shownParts, banner(kept, kinds, boxes,
                building, lowestMissing, needed), PlanHologram.Labels.TAGS));
    }

    /*
     * The part being built now: of those not formed, the first from the nose with a block in place, or the first of
     * them all when none has one yet.
     */
    private static int building(final List<Integer> formed, final int[] placed, final AABB[] boxes) {
        int first = -1;
        for (int part = 0; part < placed.length; part++) {
            if (formed.contains(part) || boxes[part] == null) {
                continue;
            }
            if (placed[part] > 0) {
                return part;
            }
            if (first < 0) {
                first = part;
            }
        }
        return first;
    }

    /* The line across the top: the ship, the part and layer being built, the block it needs most. */
    private static Optional<Text> banner(final Kept kept, final List<ShipPart> kinds, final AABB[] boxes,
                                         final int building, final int[] lowestMissing,
                                         final Map<Block, Integer> needed) {
        if (building < 0 || kinds.get(building) == null || needed.isEmpty()) {
            return Optional.empty();
        }
        final Map.Entry<Block, Integer> most = needed.entrySet().stream().max(Map.Entry.comparingByValue())
                .orElseThrow();
        final AABB box = boxes[building];
        return Optional.of(BANNER.with(Text.literal(kept.design().name()), name(kinds.get(building)),
                lowestMissing[building] - (int) box.minY + 1, (int) Math.round(box.getYsize()), most.getValue(),
                GameText.of(most.getKey().getName())));
    }

    /* How many blocks {@code box} spans along the ship's axis {@code axis}, the ship turned as {@code turn} says. */
    private static int along(final AABB box, final Orientation turn, final int axis) {
        final double size = turn.xFrom() == axis ? box.getXsize() : turn.yFrom() == axis ? box.getYsize()
                : box.getZsize();
        return (int) Math.round(size);
    }

    private static Text name(final ShipPart part) {
        return GameText.of(Component.translatable(part.name()));
    }

    /* The colour of a part's box, by what it is. */
    private static int colour(final ShipPart part) {
        final Colours c = PALETTE.get();
        if (part == null) {
            return c.gear();
        }
        return switch (part.kind()) {
            case COCKPIT -> c.cockpit();
            case HULL -> c.hull();
            case HOLD -> c.hold();
            case REACTOR -> c.reactor();
            case MODULE -> c.module();
            case MAIN_ENGINE -> c.engines();
            case AIR_ENGINE -> c.intakes();
            case WINGS -> c.wings();
            case FINS -> c.fins();
            case THRUSTERS -> c.thrusters();
            case GEAR -> c.gear();
        };
    }

    /**
     * The colours of the parts' boxes.
     *
     * @param cockpit   a cockpit
     * @param hull      a hull section, an airlock or a corridor
     * @param hold      a hold
     * @param reactor   a reactor
     * @param module    a small module
     * @param engines   the main engines
     * @param intakes   the air engines
     * @param wings     the wings
     * @param fins      the fins
     * @param thrusters the manoeuvring thrusters
     * @param gear      the landing gear
     */
    public record Colours(int cockpit, int hull, int hold, int reactor, int module, int engines, int intakes,
                          int wings, int fins, int thrusters, int gear) {
    }

    /* A terminal's hologram as last looked at: its plan laid out, what it showed, and when. */
    private record Kept(ShipDesign design, BlockPos at, Orientation turn, BuildPlan laid, long when,
                        Optional<PlanHologram.Shown> shown) {
    }
}
