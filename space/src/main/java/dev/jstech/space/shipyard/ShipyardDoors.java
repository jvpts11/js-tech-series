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
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureTexts;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.region.Box;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 * stood. Nothing starts that cannot finish: not with something in the crane's or the floor's way, a ship standing in
 * the bay, or someone in the pit; someone who walks in meanwhile holds it where it stands till they are out. A closing
 * refused is tried again once a second, till its way is clear.
 */
public final class ShipyardDoors implements IFieldPart {

    private final ShipyardTerminalBlockEntity terminal;
    private final Runnable changed;
    private State state = State.CLOSED;
    /* When its moving part set off, by the game's ticks; the players' games draw it from there. */
    private long since;
    /* Its moving part, and the plot of the world's yard it keeps its blocks in while it moves. */
    private @Nullable StructureUuid piece;
    private int plot;
    private boolean sent;
    /* Its moving part stands where it went, set down: the next may set off once nothing is in its way. */
    private boolean arrived;
    private int wait;
    /* The cradle's places, its blocks taken off the grid while the floor stands up. */
    private List<BlockPos> cradle = List.of();
    /* Opening to let its ship go, started by this pilot, if anyone. */
    private boolean letsGo;
    private @Nullable UUID pilot;
    /* A closing asked for and refused: it is tried again a while later, till its way is clear. */
    private boolean closing;
    /* What holds it where it stands now, told once while it holds. */
    private @Nullable Text holding;
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
    /* How long a part, or a closing, waits before it tries again where something still is in its way, in ticks. */
    private static final int RETRY = 20;
    /* How long a dock just formed with nothing to build stands closed before it opens by itself, in ticks. */
    private static final int EMPTY_WAIT = 100;
    /* How high over the floor risen someone stands in its way, in blocks: on the floor, or on a jaw standing. */
    private static final int OVER_THE_FLOOR = 2;
    /* Blocks taken off and put back as a moving structure's are: no drops, no damage, no updates round them. */
    private static final int QUIETLY = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_MOVE_BY_PISTON;
    private static final String STATE_KEY = "State";
    private static final String SINCE_KEY = "Since";
    private static final String PIECE_KEY = "Piece";
    private static final String PLOT_KEY = "Plot";
    private static final String SENT_KEY = "Sent";
    private static final String ARRIVED_KEY = "Arrived";
    private static final String CRADLE_KEY = "Cradle";
    private static final String LETS_GO_KEY = "LetsGo";
    private static final String PILOT_KEY = "Pilot";
    private static final String CLOSING_KEY = "Closing";
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

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putString(STATE_KEY, this.state.serializedName());
        tag.putLong(SINCE_KEY, this.since);
        tag.putBoolean(SENT_KEY, this.sent);
        tag.putBoolean(ARRIVED_KEY, this.arrived);
        tag.putBoolean(LETS_GO_KEY, this.letsGo);
        tag.putBoolean(CLOSING_KEY, this.closing);
        tag.putBoolean(SHUT_KEY, this.shut);
        if (this.piece != null) {
            tag.putUUID(PIECE_KEY, this.piece.value());
            tag.putInt(PLOT_KEY, this.plot);
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
        this.arrived = tag.getBoolean(ARRIVED_KEY);
        this.letsGo = tag.getBoolean(LETS_GO_KEY);
        this.closing = tag.getBoolean(CLOSING_KEY);
        this.shut = tag.getBoolean(SHUT_KEY);
        this.piece = tag.hasUUID(PIECE_KEY) ? new StructureUuid(tag.getUUID(PIECE_KEY)) : null;
        this.plot = tag.getInt(PLOT_KEY);
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

    /**
     * Starts opening the closed dock, round the ship built whole in it as {@code by} starts it, to let it go at the end
     * ({@code letsGo}), or empty; told why when it cannot: its parts already moving, something in their way, a ship in
     * the bay, someone in the pit, or its parts more blocks than a moving structure may be.
     */
    boolean open(final ServerLevel level, final @Nullable UUID by, final boolean letsGo, final Consumer<Text> told) {
        if (moving()) {
            told.accept(ShipyardDoorTexts.MOVING.text());
            return false;
        }
        final Optional<Orientation> dock = this.terminal.turn();
        if (this.state != State.CLOSED || dock.isEmpty() || !movable(dock.get(), told)
                || !clear(level, dock.get(), false, told)) {
            return false;
        }
        this.letsGo = letsGo;
        this.pilot = by;
        enter(State.CRANE_BACK);
        return true;
    }

    /**
     * Starts closing the open dock, round a ship taken back or for a new build; one refused, told why, is tried again
     * a while later, till its way is clear. Whether it began now.
     */
    boolean close(final ServerLevel level, final Consumer<Text> told) {
        if (moving()) {
            told.accept(ShipyardDoorTexts.MOVING.text());
            return false;
        }
        final Optional<Orientation> dock = this.terminal.turn();
        if (this.state != State.OPEN || dock.isEmpty()) {
            return false;
        }
        if (!clear(level, dock.get(), true, told)) {
            this.closing = true;
            this.wait = RETRY;
            this.changed.run();
            return false;
        }
        this.closing = false;
        this.letsGo = false;
        enter(State.FLOOR_DOWN);
        return true;
    }

    /* Its ship let go from it: standing closed, it stays closed till a design is sent to it. */
    void shipLetGo() {
        if (this.state == State.CLOSED) {
            this.shut = true;
        }
    }

    /**
     * One server tick: its moving part on its way, the next set off once it stands and nothing is in its way; an empty
     * dock opening itself, a closing refused tried again.
     */
    void tick(final ServerLevel level) {
        stood();
        final Optional<Orientation> turn = this.terminal.turn();
        if (turn.isEmpty()) {
            return;
        }
        final Orientation dock = turn.get();
        final BlockPos at = this.terminal.getBlockPos();
        switch (this.state) {
            case CLOSED -> openWhenEmpty(level);
            case CRANE_BACK -> {
                // The cradle comes off and the floor rises only with no one in the pit.
                if (moved(level, ShipyardStructure.cranePlaces(at, dock, false),
                        ShipyardStructure.cranePlaces(at, dock, true), CRANE_SECONDS) && nobodyInTheWay(level, dock)) {
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
                // Standing open, it waits for a ship to come in, or a design to close for; one refused, again.
                if (this.closing && --this.wait <= 0) {
                    close(level, text -> { });
                }
            }
        }
    }

    /**
     * Its terminal broken: its moving part is set down where it stands, and the cradle's blocks it held off the grid
     * drop, so nothing is lost. A part that cannot stand down there is set down where it was going, its way clear when
     * it set off; one that cannot stand there either drops its blocks as they were, so nothing of the dock goes on
     * moving with no terminal to stand it, and nothing is lost.
     */
    void broken(final ServerLevel level, final BlockPos at) {
        final MovingStructure body = this.piece == null ? null : MovingStructures.byId(level, this.piece);
        if (body != null && !MovingStructures.setDown(body).ok()) {
            if (body.motion() instanceof TowardMotion toward) {
                body.teleport(toward.target());
            }
            if (!MovingStructures.setDown(body).ok()) {
                dropBlocks(level, body, at);
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

    /* Into {@code next}, its moving part not yet made. */
    private void enter(final State next) {
        this.state = next;
        this.piece = null;
        this.sent = false;
        this.arrived = false;
        this.wait = 0;
        this.holding = null;
        this.changed.run();
    }

    /*
     * Moves the part standing at {@code from} to {@code to}, over {@code seconds}: made a moving structure of the
     * dock's own blocks, sent there once ready, set down once it rests there, again a second later while something
     * stands in the way. Whether it stands at {@code to} now. A part not loaded, as its land is not yet after the
     * world opens again, is waited for while the world's yard still keeps its blocks; one gone, set down or broken
     * away, is no longer.
     */
    private boolean moved(final ServerLevel level, final List<BlockPos> from, final List<BlockPos> to,
                          final double seconds) {
        if (this.arrived) {
            return true;
        }
        if (this.piece == null) {
            if (from.stream().allMatch(level::isEmptyBlock)) {
                // Nothing of it left to move, broken away: the dock goes on, and stands unformed after.
                return arrive();
            }
            if (--this.wait > 0) {
                return false;
            }
            final StructureResult made = StructureAssembly.blocks(level, from, null);
            if (!made.ok() || made.structure() == null) {
                this.wait = RETRY;
                held(level, made.refusal());
                return false;
            }
            this.piece = made.structure().id();
            this.plot = made.structure().plot();
            this.changed.run();
            return false;
        }
        final MovingStructure body = MovingStructures.byId(level, this.piece);
        if (body == null) {
            return StructureYard.owner(level, this.plot).filter(this.piece::equals).isEmpty() && arrive();
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
        final StructureResult down = MovingStructures.setDown(body);
        if (!down.ok()) {
            this.wait = RETRY;
            held(level, down.refusal());
            return false;
        }
        return arrive();
    }

    /* Its moving part stands where it went; always true. */
    private boolean arrive() {
        this.piece = null;
        this.arrived = true;
        this.holding = null;
        this.changed.run();
        return true;
    }

    /*
     * Whether no one is in the pit, nor on the floor or a jaw over it, as the cradle comes off and the floor rises;
     * the pilot told once while someone is.
     */
    private boolean nobodyInTheWay(final ServerLevel level, final Orientation dock) {
        if (level.getEntitiesOfClass(LivingEntity.class, way(dock), EntitySelector.NO_SPECTATORS).isEmpty()) {
            this.holding = null;
            return true;
        }
        held(level, ShipyardDoorTexts.SOMEONE_IN_THE_PIT.text());
        return false;
    }

    /* Held where it stands by {@code why}, told to the pilot once while it holds. */
    private void held(final ServerLevel level, final @Nullable Text why) {
        if (why != null && !why.equals(this.holding)) {
            tell(level, why);
        }
        this.holding = why;
    }

    /* The cradle's blocks taken off the grid, kept as places, while the floor rises past them, as moving blocks are. */
    private void takeCradleOff(final ServerLevel level, final Orientation dock) {
        final List<BlockPos> taken = new ArrayList<>();
        for (final BlockPos place : ShipyardStructure.cradlePlaces(this.terminal.getBlockPos(), dock)) {
            if (!level.isEmptyBlock(place)) {
                level.setBlock(place, Blocks.AIR.defaultBlockState(), QUIETLY);
                taken.add(place);
            }
        }
        this.cradle = List.copyOf(taken);
        this.changed.run();
    }

    /*
     * The cradle's blocks put back where they stood, once nothing stands there, no one and no ship; whether they are
     * back.
     */
    private boolean putCradleBack(final ServerLevel level) {
        if (this.cradle.isEmpty()) {
            return true;
        }
        AABB box = new AABB(this.cradle.getFirst());
        for (final BlockPos place : this.cradle) {
            if (!level.isEmptyBlock(place)) {
                return false;
            }
            box = box.minmax(new AABB(place));
        }
        if (!level.getEntitiesOfClass(LivingEntity.class, box, EntitySelector.NO_SPECTATORS).isEmpty()
                || !MovingStructures.near(level, box).isEmpty()) {
            return false;
        }
        for (final BlockPos place : this.cradle) {
            level.setBlock(place, ShipyardBlocks.CRADLE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        this.cradle = List.of();
        this.changed.run();
        return true;
    }

    /* Open round its ship: the ship goes now, or, unable to, the dock closes round it again, its pilot told why. */
    private void letGo(final ServerLevel level) {
        final List<Text> why = new ArrayList<>();
        final boolean went = this.terminal.release(this.pilot, why::add).isPresent();
        this.letsGo = false;
        if (!went) {
            tell(level, ShipyardDoorTexts.CANNOT_GO.with(why.isEmpty() ? ShipyardDoorTexts.NOT_READY.text()
                    : why.getFirst()));
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
        if (this.shut || this.terminal.plan().isPresent() || !this.terminal.formed()) {
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
     * Whether its moving parts may go as moving structures: none more blocks than one may be; told so when one is.
     */
    private boolean movable(final Orientation dock, final Consumer<Text> told) {
        final int most = StructureBalance.mostBlocks();
        final int floor = ShipyardStructure.deckPlaces(this.terminal.getBlockPos(), dock, false).size();
        final int crane = ShipyardStructure.cranePlaces(this.terminal.getBlockPos(), dock, false).size();
        if (Math.max(floor, crane) > most) {
            told.accept(StructureTexts.TOO_MANY.with(most, Math.max(floor, crane)));
            return false;
        }
        return true;
    }

    /*
     * Whether the dock's parts may move, closed opening or {@code open} closing: the crane's way along the rails free,
     * the floor's way through the pit free, the cradle's places free to come back to, no ship standing in the bay, no
     * one in the pit nor on the floor; told what is in the way, and where, when not.
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
        // Closing, the cradle comes back where it stood, over the floor too: nothing may stand there.
        if (open) {
            for (final BlockPos place : cradle) {
                if (!deck.contains(place) && !level.isEmptyBlock(place)) {
                    told.accept(ShipyardDoorTexts.FLOOR_IN_THE_WAY.with(place.toShortString()));
                    return false;
                }
            }
        }
        if (!MovingStructures.near(level, ShipyardStructure.bay(at, dock)).isEmpty()) {
            told.accept(ShipyardDoorTexts.SHIP_IN_THE_BAY.text());
            return false;
        }
        if (!level.getEntitiesOfClass(LivingEntity.class, way(dock), EntitySelector.NO_SPECTATORS).isEmpty()) {
            told.accept(ShipyardDoorTexts.SOMEONE_IN_THE_PIT.text());
            return false;
        }
        return true;
    }

    /* The room its floor moves through: the pit, from the floor closed up to a little over the floor risen. */
    private AABB way(final Orientation dock) {
        final AABB pit = ShipyardStructure.bay(this.terminal.getBlockPos(), dock);
        return pit.setMaxY(pit.minY + ShipyardStructure.DECK_UP - ShipyardStructure.DECK_DOWN + OVER_THE_FLOOR);
    }

    /* Drops the blocks of the dock's part {@code body} as the blocks they were, at {@code at}, before it is taken. */
    private static void dropBlocks(final ServerLevel level, final MovingStructure body, final BlockPos at) {
        final Box box = body.bounds();
        final Map<Item, Integer> counts = new LinkedHashMap<>();
        for (final BlockPos place : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            final BlockState state = level.getBlockState(place);
            if (!state.isAir()) {
                counts.merge(FormedPartBlock.originalAt(level, place, state).getBlock().asItem(), 1, Integer::sum);
            }
        }
        counts.forEach((item, count) -> Containers.dropItemStack(level, at.getX(), at.getY(), at.getZ(),
                new ItemStack(item, count)));
    }

    /* Tells the pilot who started the ship {@code text}, wherever they are. */
    private void tell(final ServerLevel level, final Text text) {
        if (this.pilot != null) {
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
