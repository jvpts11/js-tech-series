/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a Flight Computer holds: the page its screen shows, the world it plans the transfer to, the spot marked there to
 * land on, how near its map is drawn and whether its grid is laid over it; each told to the players who see it. Its
 * keys work it ({@link #press}), as the approved face lays them: the pages along the top; on the transfer the worlds
 * to go to down the left; on the landing the zoom, the mark and its clearing down the left, and the grid on the right.
 */
public final class FlightComputerBlockEntity extends SyncedBlockEntity {

    private final ValueField<FlightPage> page;
    private final ValueField<ResourceLocation> target;
    private final ValueField<BlockPos> mark;
    private final IntField zoom;
    private final BoolField grid;
    private final ValueField<RocketDesign> rocket;
    private final ValueField<BlockPos> foot;
    private final ValueField<List<Integer>> formed;

    /** The keys: those along the top, then those down the left and down the right, five of each from the top. */
    public static final int TOP = 0;
    public static final int LEFT = 10;
    public static final int RIGHT = 20;
    public static final int KEYS = 5;
    /** How near the landing map is drawn: the nearer of the far maps' rings, or the wider. */
    public static final int NEAR = 0;
    public static final int WIDE = 1;
    /** How many blocks wide a pixel of the wider landing map is. */
    public static final int WIDE_BLOCKS = 128;
    /**
     * The ellipse a rocket comes down in round the spot marked, in blocks, as this computer's guidance lands it: the
     * size Perseverance's had, 7.7 by 6.6 kilometres. A better guidance shrinks it.
     */
    public static final int ELLIPSE_LENGTH = 7_700;
    public static final int ELLIPSE_WIDTH = 6_600;
    /* The landing page's keys: the zoom in and out, the mark and its clearing; the grid on the right. */
    private static final int ZOOM_IN = LEFT;
    private static final int ZOOM_OUT = LEFT + 1;
    private static final int MARK = LEFT + 2;
    private static final int CLEAR = LEFT + 3;
    private static final int GRID = RIGHT + 2;

    public FlightComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(FlightComputer.ENTITY.get(), pos, state);
        this.page = fields().value("Page", StableCodecs.byName(FlightPage.class), FlightPage.TRANSFER).save()
                .toClient();
        this.target = fields().value("Target", ResourceLocation.CODEC, SolarSystemWorlds.MARS).save().toClient();
        this.mark = fields().nullable("Mark", BlockPos.CODEC).save().toClient();
        this.zoom = fields().integer("Zoom", WIDE).save().toClient();
        this.grid = fields().flag("Grid", false).save().toClient();
        // The plan of the rocket it is in, once its hangar has let it go: the design, where its foot is from here,
        // and the parts formed, which it draws from then on.
        this.rocket = fields().nullable("Rocket", RocketDesign.CODEC).save().toClient();
        this.foot = fields().nullable("Foot", BlockPos.CODEC).save().toClient();
        this.formed = fields().value("Formed", Codec.INT.listOf(), List.<Integer>of()).save().toClient();
    }

    /** Takes the plan of the rocket it is in: {@code design}, its foot {@code foot} from here, {@code parts} formed. */
    public void carry(final RocketDesign design, final BlockPos foot, final List<Integer> parts) {
        this.rocket.set(design);
        this.foot.set(foot.immutable());
        this.formed.set(List.copyOf(parts));
        setChanged();
    }

    /** The design of the rocket it is in, once it keeps its plan. */
    public Optional<RocketDesign> rocket() {
        return Optional.ofNullable(this.rocket.get());
    }

    /** Where the middle of its rocket's foot is from it, once it keeps the plan. */
    public Optional<BlockPos> foot() {
        return Optional.ofNullable(this.foot.get());
    }

    /** The parts of its rocket's plan formed, which it draws. */
    public List<Integer> formed() {
        final List<Integer> parts = this.formed.get();
        return parts == null ? List.of() : parts;
    }

    /** The page its screen shows. */
    public FlightPage page() {
        final FlightPage shown = this.page.get();
        return shown == null ? FlightPage.TRANSFER : shown;
    }

    /** The world it plans the transfer to. */
    public ResourceLocation target() {
        final ResourceLocation going = this.target.get();
        return going == null ? SolarSystemWorlds.MARS : going;
    }

    /** The spot marked to land on in its target world, its height not used; none before one is marked. */
    public @Nullable BlockPos mark() {
        return this.mark.get();
    }

    /** How near its landing map is drawn: {@link #NEAR} or {@link #WIDE}. */
    public int zoom() {
        return this.zoom.get();
    }

    /** Whether its landing map has its grid laid over it. */
    public boolean grid() {
        return this.grid.get();
    }

    /** The worlds it can plan a transfer to from the world it stands in, in the order of its left keys. */
    public List<ResourceLocation> destinations() {
        final List<ResourceLocation> out = new ArrayList<>();
        final Optional<SolarBody> here = here(this.level);
        for (final SolarBody body : List.of(SolarBody.OVERWORLD, SolarBody.MOON, SolarBody.MARS)) {
            if (here.isEmpty() || here.get() != body) {
                out.add(SolarSystemWorlds.worldOf(body));
            }
        }
        return List.copyOf(out);
    }

    /** The transfer it plans now, from the world it stands in to its target, if it plans one. */
    public Optional<TransferPlan> plan() {
        if (this.level == null) {
            return Optional.empty();
        }
        final Optional<SolarBody> from = here(this.level);
        final Optional<SolarBody> to = SolarSystemWorlds.bodyOf(target());
        if (from.isEmpty() || to.isEmpty()) {
            return Optional.empty();
        }
        return TransferPlan.of(from.get(), to.get(), this.level.getGameTime());
    }

    /**
     * Its key {@code key} is pressed: a page opened, a world chosen to go to, the map drawn nearer or wider, the mark
     * set where the map is centred or cleared, the grid laid or lifted. A key with nothing to do on its page does
     * nothing.
     */
    public void press(final int key) {
        if (key >= TOP && key < TOP + KEYS) {
            final FlightPage opened = FlightPage.KEYS.get(key - TOP);
            if (opened.open()) {
                this.page.set(opened);
            }
        } else if (page() == FlightPage.TRANSFER && key >= LEFT && key < LEFT + KEYS) {
            final List<ResourceLocation> worlds = destinations();
            if (key - LEFT < worlds.size() && !worlds.get(key - LEFT).equals(target())) {
                this.target.set(worlds.get(key - LEFT));
                this.mark.set(null);
            }
        } else if (page() == FlightPage.LANDING) {
            switch (key) {
                case ZOOM_IN -> this.zoom.set(NEAR);
                case ZOOM_OUT -> this.zoom.set(WIDE);
                case MARK -> {
                    if (mark() == null) {
                        this.mark.set(BlockPos.ZERO);
                    }
                }
                case CLEAR -> this.mark.set(null);
                case GRID -> this.grid.set(!grid());
                default -> {
                    // A key the landing page does not use yet.
                }
            }
        }
        setChanged();
    }

    /** Marks the spot at {@code x}, {@code z} of its target world to land on. */
    public void markAt(final int x, final int z) {
        this.mark.set(new BlockPos(x, 0, z));
        setChanged();
    }

    /** The world of the system {@code level} is, if it is one of the Solar System's. */
    public static Optional<SolarBody> here(@Nullable final Level level) {
        if (level == null) {
            return Optional.empty();
        }
        for (final Map.Entry<ResourceLocation, SpaceWorld> world : SpaceWorlds.all(level).entrySet()) {
            if (world.getValue().dimension().equals(level.dimension())) {
                return SolarSystemWorlds.bodyOf(world.getKey());
            }
        }
        return Optional.empty();
    }
}
