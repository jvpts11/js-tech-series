/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import dev.jstech.core.client.multiblock.PlanHologram;
import dev.jstech.core.multiblock.FormedPartBlock;
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
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.phys.Vec3;

/**
 * The hologram of the rocket a Rocket Hangar builds, as the approved design shows it, through the Core's plan
 * hologram: every block still to place stands where it goes as a ghost of itself, the layer being built now glowing
 * amber; each part, a multiblock, has its box in the colour of its kind, with its name, its size and how far it stands
 * (to build, the layer it is at, or formed); and the line across the top of the screen names the rocket, the part and
 * layer being built, and the block that layer needs most. It is shown while the rocket is not yet whole.
 */
@TextHolder
@PaletteHolder
public final class RocketHolograms {

    public static final TextKey SIZE = TextKey.of("jsspace.hologram.size", "%s x %s x %s  %s");
    public static final TextKey TO_BUILD = TextKey.of("jsspace.hologram.to_build", "to build");
    public static final TextKey LAYER = TextKey.of("jsspace.hologram.layer", "layer %s of %s");
    public static final TextKey FORMED = TextKey.of("jsspace.hologram.formed", "formed");
    public static final TextKey BANNER = TextKey.of("jsspace.hologram.banner", "%s  -  %s, layer %s of %s  -  %s x %s");
    /** The colours of the parts' boxes, by what they are. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "rocket_hologram", new Colours(
            0xFF << 24 | 0x78E66E, 0xFF << 24 | 0xBE82FF, 0xFF << 24 | 0xFAD246, 0xFF << 24 | 0x5AC8F0,
            0xFF << 24 | 0xFF963C, 0xFF << 24 | 0xF08CC8, 0xFF << 24 | 0xE6E6E6));
    /* How often a hologram looks at the world again, in ticks. */
    private static final int LOOK_EVERY = 5;

    /* What each terminal's hologram last was: its plan laid out, and what it showed, and when. */
    private static final Map<HangarTerminalBlockEntity, Kept> KEPT = new WeakHashMap<>();

    private RocketHolograms() {
    }

    /** The holograms of the rockets being built in the hangars loaded on this player's game. */
    public static List<PlanHologram.Shown> shown() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        final List<PlanHologram.Shown> out = new ArrayList<>();
        for (final HangarTerminalBlockEntity terminal : HangarTerminalBlockEntity.loadedOnClient()) {
            if (terminal.getLevel() != level || terminal.isRemoved()) {
                continue;
            }
            hologram(level, terminal).ifPresent(out::add);
        }
        return out;
    }

    /* The hologram of {@code terminal}'s rocket, looked at again when it is older than a moment; none when whole. */
    private static Optional<PlanHologram.Shown> hologram(final ClientLevel level,
                                                         final HangarTerminalBlockEntity terminal) {
        final Optional<RocketDesign> plan = terminal.plan();
        final Optional<BlockPos> at = terminal.planAt();
        if (plan.isEmpty() || at.isEmpty()) {
            return Optional.empty();
        }
        final long now = level.getGameTime();
        Kept kept = KEPT.get(terminal);
        if (kept == null || !kept.design().equals(plan.get()) || !kept.at().equals(at.get())) {
            // Made now, it looks at the world at once.
            kept = new Kept(plan.get(), at.get(), RocketPlans.of(level, plan.get().stack()), now - LOOK_EVERY,
                    Optional.empty());
        }
        if (now - kept.when() >= LOOK_EVERY) {
            kept = new Kept(kept.design(), kept.at(), kept.laid(), now, look(level, terminal, kept));
            KEPT.put(terminal, kept);
        }
        return kept.shown();
    }

    /* Looks at the world: what each place holds, each part's box and state, the layer being built now. */
    private static Optional<PlanHologram.Shown> look(final ClientLevel level,
                                                     final HangarTerminalBlockEntity terminal, final Kept kept) {
        final BuildPlan plan = kept.laid();
        final List<Integer> formed = terminal.formedParts();
        if (formed.size() >= plan.parts()) {
            return Optional.empty();
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final int count = plan.parts();
        final boolean[] present = new boolean[plan.spots().size()];
        final int[] lowestMissing = new int[count];
        final int[] placed = new int[count];
        final AABB[] boxes = new AABB[count];
        Arrays.fill(lowestMissing, Integer.MAX_VALUE);
        for (int i = 0; i < plan.spots().size(); i++) {
            final PlanSpot spot = plan.spots().get(i);
            final BlockPos pos = kept.at().offset(spot.at());
            final BlockState state = level.getBlockState(pos);
            final Block wanted = BuiltInRegistries.BLOCK.get(spot.block());
            present[i] = FormedPartBlock.originalAt(level, pos, state).is(wanted);
            final int part = spot.part();
            boxes[part] = boxes[part] == null ? new AABB(pos) : boxes[part].minmax(new AABB(pos));
            if (present[i]) {
                placed[part]++;
            } else {
                lowestMissing[part] = Math.min(lowestMissing[part], pos.getY());
            }
        }
        // The part being built now: the lowest of the rocket not yet formed.
        int building = -1;
        for (int part = 0; part < count && building < 0; part++) {
            if (!formed.contains(part) && boxes[part] != null) {
                building = part;
            }
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
                    : next ? PlanHologram.Look.NEXT : PlanHologram.Look.MISSING));
        }
        final List<RocketPart> kinds = new ArrayList<>();
        for (final RocketStack.Section section : kept.design().stack().sections()) {
            kinds.add(parts.get(section.part().id()));
        }
        final List<PlanHologram.Part> shownParts = new ArrayList<>();
        for (int part = 0; part < count; part++) {
            final AABB box = boxes[part];
            final RocketPart kind = part < kinds.size() ? kinds.get(part) : null;
            if (box == null || kind == null) {
                continue;
            }
            final int tall = (int) Math.round(box.getYsize());
            final Text state = formed.contains(part) ? FORMED.text() : placed[part] == 0 ? TO_BUILD.text()
                    : LAYER.with(lowestMissing[part] - (int) box.minY + 1, tall);
            shownParts.add(new PlanHologram.Part(GameText.of(Component.translatable(kind.name())), SIZE.with(
                    (int) Math.round(box.getXsize()), (int) Math.round(box.getZsize()), tall, state), colour(kind),
                    box));
        }
        final Vec3 middle = Vec3.atBottomCenterOf(kept.at()).add(0, rocketHeight(boxes) / 2, 0);
        return Optional.of(new PlanHologram.Shown(middle, places, shownParts, banner(kept.design(), kinds, boxes,
                building, lowestMissing, needed)));
    }

    /* The line across the top: the rocket, the part and layer being built, the block it needs most. */
    private static Optional<Text> banner(final RocketDesign design, final List<RocketPart> kinds, final AABB[] boxes,
                                         final int building, final int[] lowestMissing,
                                         final Map<Block, Integer> needed) {
        if (building < 0 || building >= kinds.size() || kinds.get(building) == null || needed.isEmpty()) {
            return Optional.empty();
        }
        final Map.Entry<Block, Integer> most = needed.entrySet().stream().max(Map.Entry.comparingByValue())
                .orElseThrow();
        final AABB box = boxes[building];
        return Optional.of(BANNER.with(Text.literal(design.name()), GameText.of(Component.translatable(
                kinds.get(building).name())), lowestMissing[building] - (int) box.minY + 1,
                (int) Math.round(box.getYsize()), most.getValue(), GameText.of(most.getKey().getName())));
    }

    private static double rocketHeight(final AABB[] boxes) {
        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        for (final AABB box : boxes) {
            if (box != null) {
                low = Math.min(low, box.minY);
                high = Math.max(high, box.maxY);
            }
        }
        return low == Double.MAX_VALUE ? 0 : high - low;
    }

    /* The colour of a part's box, by what it is. */
    private static int colour(final RocketPart part) {
        final Colours c = PALETTE.get();
        return switch (part.kind()) {
            case CAPSULE, ESCAPE_TOWER, HABITATION, SERVICE_MODULE, DOCKING_PORT, FAIRING, PAYLOAD_ADAPTER,
                 CARGO_CONTAINER -> c.command();
            case INSTRUMENT_RING, CONTROL_THRUSTERS, ADAPTER -> c.instrument();
            case SEPARATION_RING, RADIAL_SEPARATOR -> c.separation();
            case TANK, SOLID_BOOSTER -> c.tank();
            case ENGINE, ENGINE_MOUNT -> c.engine();
            case LANDING_LEGS, GRID_FINS -> c.legs();
            case LANDER_DESCENT, LANDER_ASCENT -> c.lander();
        };
    }

    /**
     * The colours of the parts' boxes.
     *
     * @param command    a capsule and what rides on top
     * @param instrument an instrument ring
     * @param separation a separation ring
     * @param tank       a tank
     * @param engine     an engine
     * @param legs       landing legs
     * @param lander     a lunar lander's stage
     */
    public record Colours(int command, int instrument, int separation, int tank, int engine, int legs, int lander) {
    }

    /* A terminal's hologram as last looked at. */
    private record Kept(RocketDesign design, BlockPos at, BuildPlan laid, long when,
                        Optional<PlanHologram.Shown> shown) {
    }
}
