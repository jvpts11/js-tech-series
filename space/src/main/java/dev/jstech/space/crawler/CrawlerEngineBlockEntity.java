/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crawler;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureAssembly;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A Crawler Engine as its block entity: it looks now and then for the crawler round it and forms it whole, or lets
 * it fall apart; and once a cab is used it drives the trip from aboard. With a launcher centred over it, the crawler
 * lifts it off its pedestals, goes slowly along its crawlerway to the far end, sets it down there on the grid and comes
 * back by itself; empty, it goes to the other end. The bay doors in its way open for it and close once it is back.
 */
public final class CrawlerEngineBlockEntity extends SyncedBlockEntity {

    private final ValueField<Direction> along;
    private final ValueField<Phase> phase;
    private final ValueField<Direction> way;
    private final ValueField<Integer> steps;
    private final ValueField<StructureUuid> crawler;
    private final ValueField<StructureUuid> load;
    private final ValueField<List<BlockPos>> doors;
    private int wait;

    /** How far the jacks lift a launcher off its pedestals, in blocks, and how fast, in blocks a second. */
    public static final double LIFT = 0.375;
    public static final double LIFT_SPEED = 0.1;
    /** How fast the crawler goes, in blocks a second: slowly with a launcher on it, faster empty. */
    public static final double LOADED_SPEED = 0.45;
    public static final double EMPTY_SPEED = 0.9;
    /* How many ticks between looks for the crawler round it, and before trying again to set down what is in the way. */
    private static final int LOOK = 40;
    private static final int RETRY = 20;

    public CrawlerEngineBlockEntity(final BlockPos pos, final BlockState state) {
        super(CrawlerBlocks.ENGINE_ENTITY.get(), pos, state);
        this.along = fields().nullable("Along", Direction.CODEC).save().toClient();
        this.phase = fields().value("Phase", StableCodecs.byName(Phase.class), Phase.IDLE).save().toClient();
        this.way = fields().nullable("Way", Direction.CODEC).save();
        this.steps = fields().value("Steps", Codec.INT, 0).save();
        this.crawler = fields().nullable("Crawler", StructureUuid.CODEC).save();
        this.load = fields().nullable("Load", StructureUuid.CODEC).save().toClient();
        this.doors = fields().value("Doors", BlockPos.CODEC.listOf(), List.<BlockPos>of()).save();
    }

    /** The way the crawler round it is built along, if it is whole. */
    public Optional<Direction> along() {
        return Optional.ofNullable(this.along.get());
    }

    /** Where it is on its trip. */
    public Phase phase() {
        return this.phase.get();
    }

    /** Whether its jacks are up, holding a launcher off its pedestals. */
    public boolean lifted() {
        final Phase now = this.phase.get();
        return now == Phase.LIFTING || now == Phase.DRIVING && this.load.get() != null || now == Phase.LOWERING;
    }

    /**
     * Starts the trip along its crawlerway, as a cab used asks; or says why it cannot: not whole, already going, no
     * crawlerway, a rocket unfinished or without its flight computer, more than it can carry, or the way blocked.
     */
    public Optional<Text> start() {
        if (!(this.level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        if (this.phase.get() != Phase.IDLE) {
            return Optional.of(CrawlerTexts.MOVING.text());
        }
        final BlockPos engine = getBlockPos();
        final Optional<Direction> built = CrawlerLayout.find(server, engine);
        if (built.isEmpty()) {
            return Optional.of(CrawlerTexts.NOT_WHOLE.text());
        }
        final Optional<CrawlerWay.Route> route = CrawlerWay.route(server, engine, built.get());
        if (route.isEmpty()) {
            return Optional.of(CrawlerTexts.NO_WAY.text());
        }
        final Optional<CrawlerWay.Load> carried;
        try {
            carried = CrawlerWay.launcherOver(server, engine, built.get());
        } catch (final CrawlerWay.Refusal refusal) {
            return Optional.of(refusal.why());
        }
        final List<BlockPos> own = CrawlerLayout.spots(engine, built.get()).stream().map(CrawlerLayout.Spot::at)
                .toList();
        final List<BlockPos> moving = new ArrayList<>(own);
        carried.ifPresent(launcher -> moving.addAll(launcher.blocks()));
        final Set<BlockPos> opened = new HashSet<>();
        final Optional<BlockPos> blocked = CrawlerWay.blocked(server, moving, route.get().way(),
                route.get().steps(), opened);
        if (blocked.isPresent()) {
            final BlockPos at = blocked.get();
            return Optional.of(CrawlerTexts.BLOCKED.with(at.getX(), at.getY(), at.getZ()));
        }
        if (carried.isPresent()) {
            final Optional<Text> handed = handOver(server, carried.get());
            if (handed.isPresent()) {
                return handed;
            }
        }
        // A door it passes through opens whole, as a real bay door rolls back.
        return go(server, own, carried, route.get(), CrawlerWay.wholeDoors(server, opened));
    }

    /** Looks for its crawler now and then while it stands; on its trip, takes the next step once the last is done. */
    public void tick() {
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        switch (this.phase.get()) {
            case IDLE -> {
                if (--this.wait <= 0) {
                    this.wait = LOOK;
                    look(server);
                }
            }
            case ASSEMBLING -> assembled(server);
            case LIFTING -> lifted(server);
            case DRIVING -> driven(server);
            case LOWERING -> lowered(server);
            case RETURNING -> returned(server);
        }
    }

    /* Forms the crawler round it if it has become whole, or lets it go if it has not. */
    private void look(final ServerLevel server) {
        final Direction now = CrawlerLayout.find(server, getBlockPos()).orElse(null);
        final Direction was = this.along.get();
        if (now == was) {
            return;
        }
        if (was != null) {
            form(server, was, false);
        }
        if (now != null) {
            form(server, now, true);
        }
        this.along.set(now);
        setChanged();
    }

    /* Sets the pieces of the crawler along {@code along} formed or not, where they still stand. */
    private void form(final ServerLevel server, final Direction along, final boolean formed) {
        for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(getBlockPos(), along)) {
            final BlockState state = server.getBlockState(spot.at());
            if (state.getBlock() instanceof CrawlerPieceBlock && state.getValue(CrawlerPieceBlock.FORMED) != formed) {
                server.setBlock(spot.at(), state.setValue(CrawlerPieceBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    /*
     * The hangar terminal that built the rocket hands its plan to the rocket's flight computer, which draws its formed
     * parts from then on, aboard; refused while the rocket is unfinished, or has no flight computer to take it.
     */
    private static Optional<Text> handOver(final ServerLevel server, final CrawlerWay.Load launcher) {
        if (launcher.terminal().isEmpty()
                || !(server.getBlockEntity(launcher.terminal().get()) instanceof HangarTerminalBlockEntity terminal)
                || terminal.plan().isEmpty()) {
            return Optional.empty();
        }
        if (!terminal.finished()) {
            return Optional.of(CrawlerTexts.NOT_BUILT.text());
        }
        if (launcher.computer().isEmpty()
                || !(server.getBlockEntity(launcher.computer().get()) instanceof FlightComputerBlockEntity computer)) {
            return Optional.of(CrawlerTexts.NO_COMPUTER.text());
        }
        terminal.release().ifPresent(plan -> computer.carry(plan.design(),
                plan.foot().subtract(computer.getBlockPos()), plan.formed()));
        return Optional.empty();
    }

    /* Makes the crawler, and the launcher on it, moving structures, opens the doors on the way and sets off. */
    private Optional<Text> go(final ServerLevel server, final List<BlockPos> own,
                              final Optional<CrawlerWay.Load> carried, final CrawlerWay.Route route,
                              final Set<BlockPos> opened) {
        StructureUuid launcher = null;
        if (carried.isPresent()) {
            final StructureResult made = StructureAssembly.blocks(server, carried.get().blocks(), null);
            if (!made.ok() || made.structure() == null) {
                return Optional.ofNullable(made.refusal());
            }
            launcher = made.structure().id();
        }
        // Everything it keeps goes aboard with it, so it is all set before it becomes a structure.
        this.phase.set(Phase.ASSEMBLING);
        this.way.set(route.way());
        this.steps.set(route.steps());
        this.load.set(launcher);
        this.doors.set(List.copyOf(opened));
        setChanged();
        final StructureResult made = StructureAssembly.blocks(server, own, Vec3.atCenterOf(getBlockPos()));
        if (!made.ok() || made.structure() == null) {
            this.phase.set(Phase.IDLE);
            this.load.set(null);
            this.doors.set(List.of());
            setChanged();
            if (launcher != null) {
                final MovingStructure back = MovingStructures.byId(server, launcher);
                if (back != null) {
                    MovingStructures.setDown(back);
                }
            }
            return Optional.ofNullable(made.refusal());
        }
        // It keeps the trip from aboard: from here while its blocks wait for their plot, from its yard once moved.
        final MovingStructure body = made.structure();
        final CrawlerEngineBlockEntity keeper = body.arriving() ? this : server.getBlockEntity(BlockPos.containing(
                MovingStructures.toYard(body, Vec3.atCenterOf(getBlockPos())))) instanceof CrawlerEngineBlockEntity
                aboard ? aboard : null;
        if (keeper != null) {
            keeper.crawler.set(body.id());
            keeper.setChanged();
        }
        doors(server, opened, true);
        return Optional.empty();
    }

    /* Once the crawler and its load stand ready, the jacks lift the launcher, or an empty crawler sets off. */
    private void assembled(final ServerLevel server) {
        final MovingStructure body = body(server);
        final StructureUuid carried = this.load.get();
        final MovingStructure launcher = carried == null ? null : structure(server, carried);
        if (body == null || carried != null && launcher == null) {
            stranded();
            return;
        }
        if (!body.ready() || launcher != null && !launcher.ready()) {
            return;
        }
        if (launcher != null) {
            final StructurePose at = launcher.pose();
            launcher.move(new TowardMotion(at.movedTo(at.x(), at.y() + LIFT, at.z()), LIFT_SPEED, 0));
            phase(Phase.LIFTING);
        } else {
            drive(body, this.way.get(), EMPTY_SPEED);
        }
    }

    /* The launcher up off its pedestals, it goes on the crawler and the crawler sets off with it. */
    private void lifted(final ServerLevel server) {
        final MovingStructure body = body(server);
        final MovingStructure launcher = structure(server, this.load.get());
        if (body == null || launcher == null) {
            stranded();
            return;
        }
        if (!launcher.resting()) {
            return;
        }
        MovingStructures.mount(launcher, body, Optional.empty());
        drive(body, this.way.get(), LOADED_SPEED);
    }

    /* At the end of the way: a launcher is let go and lowered; an empty crawler sets itself down there. */
    private void driven(final ServerLevel server) {
        final MovingStructure body = body(server);
        if (body == null) {
            stranded();
            return;
        }
        if (!body.resting()) {
            return;
        }
        final MovingStructure launcher = this.load.get() == null ? null : structure(server, this.load.get());
        if (launcher == null) {
            settle(server, body);
            return;
        }
        MovingStructures.unmount(launcher, false);
        final StructurePose at = launcher.pose();
        launcher.move(new TowardMotion(at.movedTo(at.x(), at.y() - LIFT, at.z()), LIFT_SPEED, 0));
        phase(Phase.LOWERING);
    }

    /* The launcher down on the grid at the far end, the crawler goes back the way it came. */
    private void lowered(final ServerLevel server) {
        final MovingStructure body = body(server);
        final MovingStructure launcher = structure(server, this.load.get());
        if (body == null) {
            stranded();
            return;
        }
        if (launcher != null) {
            if (!launcher.resting() || --this.wait > 0) {
                return;
            }
            if (!MovingStructures.setDown(launcher).ok()) {
                // Something stands where it goes: it waits there, lowered, until the place is free.
                this.wait = RETRY;
                return;
            }
        }
        this.load.set(null);
        final Direction back = this.way.get().getOpposite();
        this.way.set(back);
        drive(body, back, EMPTY_SPEED);
        phase(Phase.RETURNING);
    }

    /* Back where it started: it sets itself down on the grid. */
    private void returned(final ServerLevel server) {
        final MovingStructure body = body(server);
        if (body == null) {
            stranded();
            return;
        }
        if (body.resting()) {
            settle(server, body);
        }
    }

    /* Sets off {@code steps} blocks {@code way} at {@code speed}. */
    private void drive(final MovingStructure body, final Direction toward, final double speed) {
        final StructurePose at = body.pose();
        final int k = this.steps.get();
        body.move(new TowardMotion(at.movedTo(at.x() + toward.getStepX() * k, at.y(), at.z() + toward.getStepZ() * k),
                speed, 0));
        phase(Phase.DRIVING);
    }

    /* The trip over: it closes the doors it opened and sets itself down, or tries again soon where it cannot. */
    private void settle(final ServerLevel server, final MovingStructure body) {
        if (--this.wait > 0) {
            return;
        }
        final List<BlockPos> opened = this.doors.get();
        final Phase was = this.phase.get();
        // Set before it goes back onto the grid, which takes what it keeps with it.
        this.phase.set(Phase.IDLE);
        this.crawler.set(null);
        this.doors.set(List.of());
        setChanged();
        if (MovingStructures.setDown(body).ok()) {
            doors(server, new HashSet<>(opened), false);
            return;
        }
        this.phase.set(was);
        this.crawler.set(body.id());
        this.doors.set(opened);
        this.wait = RETRY;
        setChanged();
    }

    /* Its structure lost from under it, which only a command does: it forgets the trip. */
    private void stranded() {
        this.phase.set(Phase.IDLE);
        this.crawler.set(null);
        this.load.set(null);
        setChanged();
    }

    /* The crawler's moving structure, while it is on its trip. */
    private @Nullable MovingStructure body(final ServerLevel server) {
        return structure(server, this.crawler.get());
    }

    private void phase(final Phase to) {
        this.phase.set(to);
        setChanged();
    }

    private static @Nullable MovingStructure structure(final ServerLevel server, @Nullable final StructureUuid id) {
        return id == null ? null : MovingStructures.byId(server, id);
    }

    /* Opens or closes the bay doors at {@code at}, those still standing. */
    private static void doors(final ServerLevel server, final Set<BlockPos> at, final boolean open) {
        for (final BlockPos door : at) {
            final BlockState state = server.getBlockState(door);
            if (state.is(HangarBlocks.BAY_DOOR.get())) {
                server.setBlock(door, state.setValue(HangarPieceBlock.OPEN, open), Block.UPDATE_ALL);
            }
        }
    }

    /** Where a crawler is on its trip. */
    public enum Phase implements IStableName {
        /** Standing on the grid. */
        IDLE("idle"),
        /** Becoming a moving structure, with its load. */
        ASSEMBLING("assembling"),
        /** Lifting the launcher off its pedestals. */
        LIFTING("lifting"),
        /** Going along its crawlerway. */
        DRIVING("driving"),
        /** Lowering the launcher at the far end. */
        LOWERING("lowering"),
        /** Going back empty. */
        RETURNING("returning");

        private final String serializedName;

        Phase(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
