/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureAssembly;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The Small Shipyard's moving parts as they open and close (space/docs/design/starships.md): its crane and its floor
 * become moving structures while they move, its cradle is taken off the grid and drawn folding into the floor. Opening
 * round a finished ship, the crane rolls back along its rails to the back of the bay, then the floor rises to the
 * ground's top as the cradle folds, and only then is the ship let go, standing on its own gear; closing round a ship
 * taken back, or for a new build, the floor goes down, the cradle rises round the hull and the crane rolls back over
 * the bay. A dock just formed with nothing to build opens by itself, to stand open, ready for a ship to come in; one
 * whose ship was let go from it as it stood closed stays closed. While its parts move the dock holds itself as it
 * stood. Nothing starts that cannot finish: not with something in the crane's or the floor's way, nor with someone in
 * the pit.
 */
public final class ShipyardDoors implements IFieldPart {

    private final ShipyardTerminalBlockEntity terminal;
    private final Runnable changed;
    private State state = State.CLOSED;
    /* When its moving part set off, by the game's ticks; the players' games draw it from there. */
    private long since;
    private @Nullable StructureUuid piece;
    private boolean sent;
    private int wait;
    /* The cradle's places, its blocks taken off the grid while the floor stands up. */
    private List<BlockPos> cradle = List.of();
    /* Opening to let its ship go, started by this pilot, if anyone. */
    private boolean letsGo;
    private @Nullable UUID pilot;
    /* How the dock stood formed the tick before, on the server: formed, and open. */
    private boolean wasFormed;
    private boolean wasOpen;
    /* Its ship let go from it as it stood closed: it stays closed till it has something to build. */
    private boolean shut;
    /* How long it has stood closed with nothing to build, in ticks. */
    private int empty;

    /** How long the crane takes to roll along the bay, and the floor to rise or fall, in seconds. */
    public static final double CRANE_SECONDS = 3;
    public static final double FLOOR_SECONDS = 5;
    private static final int TICKS = 20;
    /* How long a part waits before it tries again to stand down where something still is, in ticks. */
    private static final int RETRY = 20;
    /* How long a dock just formed with nothing to build stands closed before it opens by itself, in ticks. */
    private static final int EMPTY_WAIT = 100;
    private static final String STATE_KEY = "State";
    private static final String SINCE_KEY = "Since";
    private static final String PIECE_KEY = "Piece";
    private static final String SENT_KEY = "Sent";
    private static final String CRADLE_KEY = "Cradle";
    private static final String LETS_GO_KEY = "LetsGo";
    private static final String PILOT_KEY = "Pilot";
    private static final String SHUT_KEY = "Shut";

    ShipyardDoors(final ShipyardTerminalBlockEntity terminal, final Runnable changed) {
        this.terminal = terminal;
        this.changed = changed;
    }

    /** How the dock's doors stand or move. */
    public State state() {
        return this.state;
    }

    /** Whether its parts move now: the dock holds itself as it stood till they stand again. */
    public boolean moving() {
        return this.state != State.CLOSED && this.state != State.OPEN;
    }

    /**
     * How far its moving part has gone, from 0 as it sets off to 1 where it stands, at the game's tick {@code now};
     * the floor's share is the cradle's too. Nought before it sets off; 1 standing still.
     */
    public double progress(final double now) {
        if (!moving()) {
            return 1;
        }
        if (!this.sent) {
            return 0;
        }
        final double seconds = this.state == State.CRANE_BACK || this.state == State.CRANE_IN ? CRANE_SECONDS
                : FLOOR_SECONDS;
        return Math.clamp((now - this.since) / (seconds * TICKS), 0.0, 1.0);
    }

    /**
     * Starts opening the closed dock, round the ship built whole in it as {@code by} starts it, to let it go at the end
     * ({@code letsGo}), or empty; told why when it cannot: its parts already moving, something in their way, someone in
     * the pit.
     */
    boolean open(final ServerLevel level, final @Nullable UUID by, final boolean letsGo, final Consumer<Text> told) {
        if (moving()) {
            told.accept(ShipyardDoorTexts.MOVING.text());
            return false;
        }
        final Optional<Orientation> dock = this.terminal.turn();
        if (this.state != State.CLOSED || dock.isEmpty() || !clear(level, dock.get(), false, told)) {
            return false;
        }
        this.letsGo = letsGo;
        this.pilot = by;
        enter(State.CRANE_BACK);
        return true;
    }

    /* Its ship let go from it: standing closed, it stays closed till a design is sent to it. */
    void shipLetGo() {
        if (this.state == State.CLOSED) {
            this.shut = true;
        }
    }

    /** Starts closing the open dock, round a ship taken back or for a new build; told why when it cannot. */
    boolean close(final ServerLevel level, final Consumer<Text> told) {
        if (moving()) {
            told.accept(ShipyardDoorTexts.MOVING.text());
            return false;
        }
        final Optional<Orientation> dock = this.terminal.turn();
        if (this.state != State.OPEN || dock.isEmpty() || !clear(level, dock.get(), true, told)) {
            return false;
        }
        this.letsGo = false;
        enter(State.FLOOR_DOWN);
        return true;
    }

    /** One server tick: its moving part on its way, the next set off once it stands; an empty dock opening itself. */
    void tick(final ServerLevel level) {
        stood();
        final Optional<Orientation> turn = this.terminal.turn();
        if (turn.isEmpty() && !moving()) {
            return;
        }
        final Orientation dock = turn.orElse(Orientation.NORTH);
        final BlockPos at = this.terminal.getBlockPos();
        switch (this.state) {
            case CLOSED -> openWhenEmpty(level);
            case CRANE_BACK -> {
                if (moved(level, ShipyardStructure.cranePlaces(at, dock, false),
                        ShipyardStructure.cranePlaces(at, dock, true), CRANE_SECONDS)) {
                    takeCradleOff(level, dock);
                    enter(State.FLOOR_UP);
                }
            }
            case FLOOR_UP -> {
                if (moved(level, ShipyardStructure.deckPlaces(at, dock, false),
                        ShipyardStructure.deckPlaces(at, dock, true), FLOOR_SECONDS)) {
                    enter(State.OPEN);
                    if (this.letsGo) {
                        letGo(level);
                    }
                }
            }
            case FLOOR_DOWN -> {
                if (moved(level, ShipyardStructure.deckPlaces(at, dock, true),
                        ShipyardStructure.deckPlaces(at, dock, false), FLOOR_SECONDS) && putCradleBack(level)) {
                    enter(State.CRANE_IN);
                }
            }
            case CRANE_IN -> {
                if (moved(level, ShipyardStructure.cranePlaces(at, dock, true),
                        ShipyardStructure.cranePlaces(at, dock, false), CRANE_SECONDS)) {
                    enter(State.CLOSED);
                    this.terminal.doorsClosed();
                }
            }
            case OPEN -> {
                // Standing open, it waits for a ship to come in, or a design to close for.
            }
        }
    }

    /**
     * Its terminal broken: its moving part is set down where it stands, and the cradle's blocks it held off the grid
     * drop, so nothing is lost. A part that cannot stand down there is set down where it was going, its way clear when
     * it set off; one that cannot stand there either is taken away, so nothing of the dock goes on moving with no
     * terminal to stand it.
     */
    void broken(final ServerLevel level, final BlockPos at) {
        final MovingStructure body = this.piece == null ? null : MovingStructures.byId(level, this.piece);
        if (body != null && !MovingStructures.setDown(body).ok()) {
            if (body.motion() instanceof TowardMotion toward) {
                body.teleport(toward.target());
            }
            if (!MovingStructures.setDown(body).ok()) {
                MovingStructures.takeAway(body);
            }
        }
        this.piece = null;
        if (!this.cradle.isEmpty()) {
            Containers.dropItemStack(level, at.getX(), at.getY(), at.getZ(), new ItemStack(ShipyardBlocks.CRADLE.get(),
                    this.cradle.size()));
            this.cradle = List.of();
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putString(STATE_KEY, this.state.serializedName());
        tag.putLong(SINCE_KEY, this.since);
        tag.putBoolean(SENT_KEY, this.sent);
        tag.putBoolean(LETS_GO_KEY, this.letsGo);
        tag.putBoolean(SHUT_KEY, this.shut);
        if (this.piece != null) {
            tag.putUUID(PIECE_KEY, this.piece.value());
        }
        if (this.pilot != null) {
            tag.putUUID(PILOT_KEY, this.pilot);
        }
        final ListTag places = new ListTag();
        this.cradle.forEach(place -> places.add(LongTag.valueOf(place.asLong())));
        tag.put(CRADLE_KEY, places);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.state = State.named(tag.getString(STATE_KEY));
        this.since = tag.getLong(SINCE_KEY);
        this.sent = tag.getBoolean(SENT_KEY);
        this.letsGo = tag.getBoolean(LETS_GO_KEY);
        this.shut = tag.getBoolean(SHUT_KEY);
        this.piece = tag.hasUUID(PIECE_KEY) ? new StructureUuid(tag.getUUID(PIECE_KEY)) : null;
        this.pilot = tag.hasUUID(PILOT_KEY) ? tag.getUUID(PILOT_KEY) : null;
        final List<BlockPos> places = new ArrayList<>();
        for (final Tag place : tag.getList(CRADLE_KEY, Tag.TAG_LONG)) {
            places.add(BlockPos.of(((LongTag) place).getAsLong()));
        }
        this.cradle = List.copyOf(places);
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putString(STATE_KEY, this.state.serializedName());
        tag.putLong(SINCE_KEY, this.since);
        tag.putBoolean(SENT_KEY, this.sent);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.state = State.named(tag.getString(STATE_KEY));
        this.since = tag.getLong(SINCE_KEY);
        this.sent = tag.getBoolean(SENT_KEY);
    }

    /* Into {@code next}, its moving part not yet made. */
    private void enter(final State next) {
        this.state = next;
        this.piece = null;
        this.sent = false;
        this.wait = 0;
        this.changed.run();
    }

    /*
     * Moves the part standing at {@code from} to {@code to}, over {@code seconds}: made a moving structure of the
     * dock's own blocks, sent there once ready, set down once it rests there, again a second later while something
     * stands in the way. Whether it stands at {@code to} now.
     */
    private boolean moved(final ServerLevel level, final List<BlockPos> from, final List<BlockPos> to,
                          final double seconds) {
        if (this.piece == null && !this.sent) {
            if (from.stream().allMatch(level::isEmptyBlock)) {
                // Nothing of it left to move, broken away: the dock goes on, and stands unformed after.
                return true;
            }
            final StructureResult made = StructureAssembly.blocks(level, from, null);
            if (!made.ok() || made.structure() == null) {
                return false;
            }
            this.piece = made.structure().id();
            return false;
        }
        final MovingStructure body = this.piece == null ? null : MovingStructures.byId(level, this.piece);
        if (body == null) {
            this.piece = null;
            return true;
        }
        if (!body.ready()) {
            return false;
        }
        if (!this.sent) {
            final BlockPos a = from.getFirst();
            final BlockPos b = to.getFirst();
            final StructurePose pose = body.pose();
            body.move(new TowardMotion(pose.movedTo(pose.x() + b.getX() - a.getX(), pose.y() + b.getY() - a.getY(),
                    pose.z() + b.getZ() - a.getZ()), Math.sqrt(a.distSqr(b)) / seconds, 0));
            this.sent = true;
            this.since = level.getGameTime();
            this.changed.run();
            return false;
        }
        if (!body.resting() || --this.wait > 0) {
            return false;
        }
        if (!MovingStructures.setDown(body).ok()) {
            this.wait = RETRY;
            return false;
        }
        this.piece = null;
        return true;
    }

    /* The cradle's blocks taken off the grid, kept as places, while the floor rises past them. */
    private void takeCradleOff(final ServerLevel level, final Orientation dock) {
        final List<BlockPos> taken = new ArrayList<>();
        for (final BlockPos place : ShipyardStructure.cradlePlaces(this.terminal.getBlockPos(), dock)) {
            if (!level.isEmptyBlock(place)) {
                level.setBlock(place, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                taken.add(place);
            }
        }
        this.cradle = List.copyOf(taken);
    }

    /* The cradle's blocks put back where they stood, once no one stands there; whether they are back. */
    private boolean putCradleBack(final ServerLevel level) {
        for (final BlockPos place : this.cradle) {
            if (!level.isEmptyBlock(place)
                    || !level.getEntitiesOfClass(LivingEntity.class, new AABB(place)).isEmpty()) {
                return false;
            }
        }
        for (final BlockPos place : this.cradle) {
            level.setBlock(place, ShipyardBlocks.CRADLE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        this.cradle = List.of();
        return true;
    }

    /* Open round its ship: the ship goes now, or, unable to, the dock closes round it again, its pilot told why. */
    private void letGo(final ServerLevel level) {
        final List<Text> why = new ArrayList<>();
        final boolean went = this.terminal.release(this.pilot, why::add).isPresent();
        this.letsGo = false;
        if (!went) {
            tell(level, ShipyardDoorTexts.CANNOT_GO.with(why.isEmpty() ? "" : why.getFirst()));
            close(level, text -> tell(level, text));
        }
        this.pilot = null;
    }

    /* Formed, or formed again the other way, while its parts stand: its doors stand as its blocks do. */
    private void stood() {
        final boolean formed = this.terminal.formed();
        final boolean open = this.terminal.open();
        if (formed && !moving() && (!this.wasFormed || open != this.wasOpen)) {
            final State now = open ? State.OPEN : State.CLOSED;
            if (now != this.state) {
                this.state = now;
                this.changed.run();
            }
        }
        this.wasFormed = formed;
        this.wasOpen = open;
    }

    /*
     * Closed with nothing to build, as it formed, it opens by itself after a while, while it still has nothing to
     * build and no ship stands in its bay; one whose ship was let go from it as it stood closed stays closed till a
     * design is sent to it.
     */
    private void openWhenEmpty(final ServerLevel level) {
        if (this.terminal.plan().isPresent()) {
            this.shut = false;
        }
        if (this.shut || this.terminal.plan().isPresent() || !this.terminal.formed() || this.terminal.turn().isEmpty()
                || !MovingStructures.near(level, ShipyardStructure.bay(this.terminal.getBlockPos(),
                this.terminal.turn().get())).isEmpty()) {
            this.empty = 0;
            return;
        }
        if (++this.empty >= EMPTY_WAIT) {
            // Something in the way of its parts, it tries again a while later.
            this.empty = 0;
            open(level, null, false, text -> { });
        }
    }

    /*
     * Whether the dock's parts may move, closed opening or {@code open} closing: the crane's way along the rails free,
     * the floor's way through the pit free, no one in the pit; told what is in the way when not.
     */
    private boolean clear(final ServerLevel level, final Orientation dock, final boolean open,
                          final Consumer<Text> told) {
        final BlockPos at = this.terminal.getBlockPos();
        final List<BlockPos> crane = ShipyardStructure.cranePlaces(at, dock, open);
        final List<BlockPos> parked = ShipyardStructure.cranePlaces(at, dock, !open);
        final BlockPos step = new BlockPos(Integer.signum(parked.getFirst().getX() - crane.getFirst().getX()), 0,
                Integer.signum(parked.getFirst().getZ() - crane.getFirst().getZ()));
        final int steps = crane.getFirst().distManhattan(parked.getFirst());
        for (final BlockPos place : crane) {
            for (int k = 1; k <= steps; k++) {
                final BlockPos way = place.offset(step.multiply(k));
                if (!crane.contains(way) && !level.isEmptyBlock(way)) {
                    told.accept(ShipyardDoorTexts.CRANE_IN_THE_WAY.with(way.toShortString()));
                    return false;
                }
            }
        }
        final List<BlockPos> cradle = ShipyardStructure.cradlePlaces(at, dock);
        final int rise = open ? -1 : 1;
        final List<BlockPos> deck = ShipyardStructure.deckPlaces(at, dock, open);
        for (final BlockPos place : deck) {
            for (int k = 1; k <= ShipyardStructure.DECK_UP - ShipyardStructure.DECK_DOWN; k++) {
                final BlockPos way = place.above(rise * k);
                if (!cradle.contains(way) && !level.isEmptyBlock(way)) {
                    told.accept(ShipyardDoorTexts.FLOOR_IN_THE_WAY.with(way.toShortString()));
                    return false;
                }
            }
        }
        final AABB pit = AABB.encapsulatingFullBlocks(deck.getFirst(), deck.getLast())
                .expandTowards(0, rise * (ShipyardStructure.DECK_UP - ShipyardStructure.DECK_DOWN), 0);
        if (!level.getEntitiesOfClass(LivingEntity.class, pit).isEmpty()) {
            told.accept(ShipyardDoorTexts.SOMEONE_IN_THE_PIT.text());
            return false;
        }
        return true;
    }

    /* Tells the pilot who started the ship {@code text}, wherever they are. */
    private void tell(final Level level, final Text text) {
        if (this.pilot != null && level.getServer() != null) {
            final ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.pilot);
            if (player != null) {
                player.displayClientMessage(GameText.component(text), true);
            }
        }
    }

    /** How the dock's doors stand or move: closed, opening (the crane, then the floor), open, closing. */
    public enum State implements IStableName {
        CLOSED("closed"), CRANE_BACK("crane_back"), FLOOR_UP("floor_up"), OPEN("open"), FLOOR_DOWN("floor_down"),
        CRANE_IN("crane_in");

        private final String name;

        State(final String name) {
            this.name = name;
        }

        @Override
        public String serializedName() {
            return this.name;
        }

        /* The state saved as {@code name}, or closed for one not known. */
        static State named(final String name) {
            for (final State each : values()) {
                if (each.name.equals(name)) {
                    return each;
                }
            }
            return CLOSED;
        }
    }
}
