/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyDamage;
import dev.jstech.core.energy.LevelEnergy;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridMember;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.moving.StructureBlocksMovedEvent;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Overhead lines, as in Immersive Engineering: bare wire hung from insulator to insulator, the only link outside the
 * cable block. A line whose cable hangs ({@link CableType.Builder#hangs}) is hung by using its item on one insulator
 * and then the next, within the line's longest span, along a way nothing stands in; its wire then joins its grid as a
 * cable as long as itself, so what the line loses counts its length. A connector hands what the wire carries to its
 * block, a relay only passes it along, and the two insulators of a feedthrough are joined through their wall.
 *
 * <p>The Core gives the hanging, the spans, the drawing of the wire's sag and the touch of a live wire; a mod gives its
 * poles, towers and insulators, as blocks that offer {@link #ANCHORS}. A wire whose insulator is gone falls, and its
 * wire is left where the insulator was.
 */
@TextHolder
public final class OverheadLines {

    public static final TextKey START = TextKey.of("jscore.overhead.start",
            "Hung from %s: use it on the next insulator");
    public static final TextKey HUNG = TextKey.of("jscore.overhead.hung", "Hung %s blocks of wire");
    public static final TextKey LET_GO = TextKey.of("jscore.overhead.let_go", "The wire is let go");
    public static final TextKey TOO_LONG = TextKey.of("jscore.overhead.too_long",
            "Too far: this line spans at most %s blocks");
    public static final TextKey BLOCKED = TextKey.of("jscore.overhead.blocked", "Something is in the way at %s");
    public static final TextKey NOT_TAKEN = TextKey.of("jscore.overhead.not_taken",
            "This insulator does not take this line");
    public static final TextKey OTHER_LINE = TextKey.of("jscore.overhead.other_line",
            "This insulator carries another line");
    public static final TextKey FULL = TextKey.of("jscore.overhead.full", "This insulator holds no more wires");
    public static final TextKey TWICE = TextKey.of("jscore.overhead.twice", "A wire hangs between these already");
    public static final TextKey SHORT = TextKey.of("jscore.overhead.short", "Not enough wire: it takes %s");
    public static final TextKey PROTECTED = TextKey.of("jscore.overhead.protected", "The place is protected");

    /** The insulators of a block, which overhead wires are hung from. */
    public static final BlockCapability<IWireAnchors, @Nullable Void> ANCHORS = BlockCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "wire_anchors"), IWireAnchors.class);
    /** Every wire hung in a dimension, sent to the players in it to draw. */
    public static final DimensionState<WireBook> WIRES = CoreState.builder(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "overhead_wires"), WireBook.CODEC, WireBook.EMPTY).synced(WireBook.STREAM_CODEC)
            .dimension();
    /** Where the wire in a player's hand is being hung from. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HangStart>> START_AT =
            CoreItems.CONTENT.component("hang_start", HangStart.CODEC, HangStart.STREAM_CODEC);
    /** How often the wires are looked over for an insulator that is gone, in ticks. */
    public static final int SWEEP_EVERY = 20;
    /** How often whoever touches a live wire is shocked, in ticks. */
    public static final int TOUCH_EVERY = 4;
    /** How near lightning strikes a wire or an insulator to strike its line, in blocks. */
    public static final double STRIKE_REACH = 2.5;
    /* How near a body's middle or eyes come to a wire, past its own half width, to touch it, in blocks. */
    private static final double TOUCHING = 0.15;
    /* How finely a wire's way is looked over when it is hung, in pieces of a block. */
    private static final int CHECKS_A_BLOCK = 4;

    /* The wires in their grids, in each dimension: those whose two insulators are loaded. Nothing of it is saved. */
    private static final Map<ResourceKey<Level>, Set<Long>> IN_GRID = new HashMap<>();
    /* The chunks loaded since the last tick, in each dimension, whose wires may go in their grids now. */
    private static final Map<ResourceKey<Level>, Set<Long>> LOADED = new HashMap<>();

    private OverheadLines() {
    }

    /** Declares the kit's state and component, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        CoreStates.register(WIRES);
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, OverheadLines::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Unload.class, OverheadLines::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, OverheadLines::onLevelTick);
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, OverheadLines::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(StructureBlocksMovedEvent.class, OverheadLines::onBlocksMoved);
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
            IN_GRID.clear();
            LOADED.clear();
        });
    }

    /**
     * The item of a line that hangs, used on a block with insulators: the first insulator used is where the wire is
     * hung from, the next one where it is hung to; the same one again lets the wire go.
     */
    public static InteractionResult useOn(final UseOnContext context, final CableType type) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final IWireAnchors anchors = level.getCapability(ANCHORS, pos, null);
        if (anchors == null || anchors.anchors().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        final Player player = context.getPlayer();
        final ItemStack stack = context.getItemInHand();
        final WireEnd here = new WireEnd(pos, nearest(anchors, pos, context.getClickLocation()));
        final HangStart start = stack.get(START_AT.get());
        final Text said;
        if (start == null || !start.dimension().equals(server.dimension())) {
            final Optional<TextKey> refused = refusal(server, here, anchors, type);
            if (refused.isPresent()) {
                said = refused.get().text();
            } else {
                stack.set(START_AT.get(), new HangStart(server.dimension(), here));
                said = START.with(GameText.of(level.getBlockState(pos).getBlock().getName()));
            }
        } else if (start.end().equals(here)) {
            stack.remove(START_AT.get());
            said = LET_GO.text();
        } else {
            final Outcome outcome = hang(player instanceof ServerPlayer hanging ? hanging : null, server, start.end(),
                    here, type, stack);
            if (outcome.hung()) {
                stack.remove(START_AT.get());
            }
            said = outcome.said();
        }
        if (player != null) {
            player.displayClientMessage(GameText.component(said), true);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Hangs a wire of {@code type} from {@code from} to {@code to} in {@code level}, for {@code player} (none for the
     * world's own work), taking its wire from {@code coil}: a block of wire for each block of its length, none from a
     * player who builds freely. What came of it, and what to tell the player.
     */
    public static Outcome hang(@Nullable final ServerPlayer player, final ServerLevel level, final WireEnd from,
                               final WireEnd to, final CableType type, final ItemStack coil) {
        final IWireAnchors fromAnchors = loadedAnchors(level, from.pos());
        final IWireAnchors toAnchors = loadedAnchors(level, to.pos());
        if (fromAnchors == null || toAnchors == null || !type.hangs() || type.id() == null) {
            return Outcome.not(NOT_TAKEN.text());
        }
        final Optional<TextKey> refused = refusal(level, from, fromAnchors, type)
                .or(() -> refusal(level, to, toAnchors, type));
        if (refused.isPresent()) {
            return Outcome.not(refused.get().text());
        }
        final WireBook book = WIRES.get(level);
        if (book.joins(from, to) || from.pos().equals(to.pos()) && from.anchor() == to.anchor()) {
            return Outcome.not(TWICE.text());
        }
        final Vec3 a = fastening(from, fromAnchors);
        final Vec3 b = fastening(to, toAnchors);
        final double length = a.distanceTo(b);
        if (length > type.span()) {
            return Outcome.not(TOO_LONG.with(type.span()));
        }
        if (player != null && (!Protection.may(Actor.of(player), ProtectedAction.PLACE, level, from.pos())
                || !Protection.may(Actor.of(player), ProtectedAction.PLACE, level, to.pos()))) {
            return Outcome.not(PROTECTED.text());
        }
        final Optional<BlockPos> inTheWay = inTheWay(level, a, b, from.pos(), to.pos());
        if (inTheWay.isPresent()) {
            final BlockPos at = inTheWay.get();
            return Outcome.not(BLOCKED.with(Text.literal(at.getX() + ", " + at.getY() + ", " + at.getZ())));
        }
        final int blocks = (int) Math.ceil(length);
        final boolean free = player != null && player.getAbilities().instabuild;
        if (!free && coil.getCount() < blocks) {
            return Outcome.not(SHORT.with(blocks));
        }
        if (!free) {
            coil.shrink(blocks);
        }
        final WireBook hung = WIRES.update(level, before -> before.with(from, to, type.id(), a, b));
        hung.byId(hung.next() - 1).ifPresent(wire -> tryEnter(level, wire));
        return new Outcome(true, HUNG.with(blocks));
    }

    /** Takes the wire {@code wire} down from {@code level}, leaving its wire at {@code dropAt}, or nowhere for none. */
    public static void unhang(final ServerLevel level, final HungWire wire, @Nullable final BlockPos dropAt) {
        leave(level, wire);
        WIRES.update(level, book -> book.without(wire.id()));
        final Optional<CableType> type = CoreCables.REGISTRY.getOptional(wire.cable());
        if (dropAt != null && type.isPresent()) {
            int left = wire.blocks();
            while (left > 0) {
                final ItemStack stack = type.get().stack();
                final int count = Math.min(left, stack.getMaxStackSize());
                stack.setCount(count);
                Block.popResource(level, dropAt, stack);
                left -= count;
            }
        }
    }

    /** The wire known in its grid by {@code place}, a hung wire's place; none for any other. */
    public static Optional<HungWire> wireAt(final ServerLevel level, final GridPlace place) {
        return place.isHung() ? WIRES.get(level).hungFrom(BlockPos.of(place.pos()), place.lane() - GridPlace.HUNG)
                : Optional.empty();
    }

    /** The line hung from the insulator known in its grid by {@code place}; none for none, or any other place. */
    public static Optional<CableType> lineAt(final ServerLevel level, final GridPlace place) {
        if (!place.isAnchor()) {
            return Optional.empty();
        }
        final WireEnd end = new WireEnd(BlockPos.of(place.pos()), place.lane() - GridPlace.ANCHORS);
        return WIRES.get(level).at(end).stream().findFirst()
                .flatMap(wire -> CoreCables.REGISTRY.getOptional(wire.cable()));
    }

    /** The insulator known in its grid by {@code place}, while its block is loaded; none for any other place. */
    public static Optional<WireAnchor> anchorAt(final ServerLevel level, final GridPlace place) {
        if (!place.isAnchor()) {
            return Optional.empty();
        }
        final IWireAnchors anchors = loadedAnchors(level, BlockPos.of(place.pos()));
        final int index = place.lane() - GridPlace.ANCHORS;
        return anchors == null || index >= anchors.anchors().size() ? Optional.empty()
                : Optional.of(anchors.anchors().get(index));
    }

    /** Whether the wire numbered {@code id} is in its grid now, its two insulators loaded. */
    public static boolean inGrid(final ServerLevel level, final long id) {
        return inGrid(level).contains(id);
    }

    /**
     * Lightning striking at {@code at}: each overhead line of an energy it strikes, along a wire or at an insulator,
     * carries a surge to the machines on it, once for each part of a line ({@link LevelEnergy#surge}); whether any
     * surge reached a machine.
     */
    public static boolean struck(final ServerLevel level, final Vec3 at) {
        final Catenary.Point point = new Catenary.Point(at.x, at.y, at.z);
        final Set<String> surged = new HashSet<>();
        boolean reached = false;
        for (final HungWire wire : WIRES.get(level).wires()) {
            if (!inGrid(level).contains(wire.id())) {
                continue;
            }
            final boolean hit = Catenary.distance(point, wire.start(), wire.end()) <= STRIKE_REACH
                    || wire.a().distanceTo(at) <= STRIKE_REACH || wire.b().distanceTo(at) <= STRIKE_REACH;
            final Optional<CableType> type = CoreCables.REGISTRY.getOptional(wire.cable());
            if (!hit || type.isEmpty() || Energies.ofGrid(type.get().grid()).filter(Energy::surges).isEmpty()) {
                continue;
            }
            final OptionalLong number = CoreGrids.places(level).find(wire.place());
            final Optional<LevelEnergy> mover = CoreGrids.mover(level, type.get().grid())
                    .filter(LevelEnergy.class::isInstance).map(LevelEnergy.class::cast);
            if (number.isEmpty() || mover.isEmpty()) {
                continue;
            }
            final String part = type.get().grid().id() + "/" + mover.get().partOf(number.getAsLong()).orElse(-1);
            if (surged.add(part)) {
                reached |= mover.get().surge(level, number.getAsLong());
            }
        }
        return reached;
    }

    /* A bolt of lightning that strikes the world strikes the lines near it. */
    public static void onEntityJoin(final EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level) {
            struck(level, bolt.position());
        }
    }

    /*
     * Blocks moved with a moving structure: a wire hung between two of them goes with them, fastened again where its
     * insulators are now and as they turned; a wire hung from one that moved to one that stayed falls, its wire left
     * at the one that stayed, as when an insulator is broken.
     */
    public static void onBlocksMoved(final StructureBlocksMovedEvent event) {
        final BlockMove move = event.move();
        if (!(move.from() instanceof ServerLevel from) || !(move.to() instanceof ServerLevel to)) {
            return;
        }
        for (final HungWire wire : WIRES.get(from).wires()) {
            final boolean fromMoved = move.movedHere(wire.from().pos());
            final boolean toMoved = move.movedHere(wire.to().pos());
            if (fromMoved != toMoved) {
                unhang(from, wire, fromMoved ? wire.to().pos() : wire.from().pos());
                continue;
            }
            if (!fromMoved) {
                continue;
            }
            leave(from, wire);
            WIRES.update(from, book -> book.without(wire.id()));
            final WireEnd a = new WireEnd(move.where(wire.from().pos()), wire.from().anchor());
            final WireEnd b = new WireEnd(move.where(wire.to().pos()), wire.to().anchor());
            final IWireAnchors aAnchors = loadedAnchors(to, a.pos());
            final IWireAnchors bAnchors = loadedAnchors(to, b.pos());
            if (aAnchors == null || bAnchors == null || !stands(to, a) || !stands(to, b)) {
                continue;
            }
            final WireBook hung = WIRES.update(to, before -> before.with(a, b, wire.cable(), fastening(a, aAnchors),
                    fastening(b, bAnchors)));
            hung.byId(hung.next() - 1).ifPresent(there -> tryEnter(to, there));
        }
    }

    public static void onChunkLoad(final ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LOADED.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(event.getChunk().getPos().toLong());
        }
    }

    /* The wires with an end in a chunk that goes leave their grids; they come back when it loads again. */
    public static void onChunkUnload(final ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        final ChunkPos chunk = event.getChunk().getPos();
        for (final HungWire wire : WIRES.get(level).wires()) {
            if (inGrid(level).contains(wire.id()) && (new ChunkPos(wire.from().pos()).equals(chunk)
                    || new ChunkPos(wire.to().pos()).equals(chunk))) {
                leave(level, wire);
            }
        }
    }

    /*
     * After each tick of a dimension: the wires of the chunks just loaded go in their grids, and now and then every
     * wire is looked over, one whose insulator is gone falling, one whose two insulators are loaded going in its grid.
     */
    public static void onLevelTick(final LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % TOUCH_EVERY == 0) {
            touches(level);
        }
        final Set<Long> loaded = LOADED.remove(level.dimension());
        final boolean sweep = level.getGameTime() % SWEEP_EVERY == 0;
        if ((loaded == null || loaded.isEmpty()) && !sweep) {
            return;
        }
        for (final HungWire wire : WIRES.get(level).wires()) {
            final boolean touched = loaded != null && (loaded.contains(new ChunkPos(wire.from().pos()).toLong())
                    || loaded.contains(new ChunkPos(wire.to().pos()).toLong()));
            if (!touched && !sweep) {
                continue;
            }
            if (!level.isLoaded(wire.from().pos()) || !level.isLoaded(wire.to().pos())) {
                continue;
            }
            final boolean fromStands = stands(level, wire.from());
            final boolean toStands = stands(level, wire.to());
            if (!fromStands || !toStands) {
                unhang(level, wire, fromStands ? wire.to().pos() : wire.from().pos());
            } else {
                tryEnter(level, wire);
            }
        }
    }

    /*
     * Shocks whoever touches a live bare wire of an energy's line, as much as its class does: every wire in its grid
     * whose part had energy to give, and every living thing whose middle or eyes come within reach of its curve.
     */
    private static void touches(final ServerLevel level) {
        for (final HungWire wire : WIRES.get(level).wires()) {
            if (!inGrid(level).contains(wire.id())) {
                continue;
            }
            final Optional<CableType> type = CoreCables.REGISTRY.getOptional(wire.cable());
            final Optional<Energy> energy = type.flatMap(found -> Energies.ofGrid(found.grid()));
            final float shock = energy.flatMap(found -> found.classOf(type.get().grid())).map(lineClass ->
                    energy.get().shock(lineClass)).orElse(0F);
            if (shock <= 0 || !live(level, wire, type.get())) {
                continue;
            }
            final Catenary.Point start = wire.start();
            final Catenary.Point end = wire.end();
            final AABB around = new AABB(wire.a(), wire.b()).expandTowards(0, -Catenary.sag(start, end), 0)
                    .inflate(1.0);
            for (final LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, around)) {
                final double reach = living.getBbWidth() / 2.0 + TOUCHING;
                final Vec3 middle = living.position().add(0, living.getBbHeight() / 2.0, 0);
                final Vec3 eyes = living.getEyePosition();
                if (Catenary.distance(new Catenary.Point(middle.x, middle.y, middle.z), start, end) <= reach
                        || Catenary.distance(new Catenary.Point(eyes.x, eyes.y, eyes.z), start, end) <= reach) {
                    living.hurt(EnergyDamage.electric(level), shock);
                }
            }
        }
    }

    /* Whether {@code wire}'s part of its line had energy to give in the last tick. */
    private static boolean live(final ServerLevel level, final HungWire wire, final CableType type) {
        final OptionalLong number = CoreGrids.places(level).find(wire.place());
        return number.isPresent() && CoreGrids.mover(level, type.grid())
                .map(mover -> mover.live(number.getAsLong())).orElse(false);
    }

    /* Why a wire of {@code type} cannot be hung from {@code end}: another line, too many wires, not taken; none. */
    private static Optional<TextKey> refusal(final ServerLevel level, final WireEnd end, final IWireAnchors anchors,
                                             final CableType type) {
        if (end.anchor() >= anchors.anchors().size() || !type.hangs() || !anchors.takes(end.anchor(), type)) {
            return Optional.of(NOT_TAKEN);
        }
        final List<HungWire> there = WIRES.get(level).at(end);
        if (there.stream().anyMatch(wire -> !wire.cable().equals(type.id()))) {
            return Optional.of(OTHER_LINE);
        }
        if (there.size() >= anchors.anchors().get(end.anchor()).mostWires()) {
            return Optional.of(FULL);
        }
        return Optional.empty();
    }

    /* The insulator of a block nearest where it was used. */
    private static int nearest(final IWireAnchors anchors, final BlockPos pos, final Vec3 used) {
        int best = 0;
        double nearest = Double.MAX_VALUE;
        for (int k = 0; k < anchors.anchors().size(); k++) {
            final double distance = Vec3.atLowerCornerOf(pos).add(anchors.anchors().get(k).at()).distanceToSqr(used);
            if (distance < nearest) {
                nearest = distance;
                best = k;
            }
        }
        return best;
    }

    private static Vec3 fastening(final WireEnd end, final IWireAnchors anchors) {
        return Vec3.atLowerCornerOf(end.pos()).add(anchors.anchors().get(end.anchor()).at());
    }

    /*
     * The first block whose solid shape the wire would pass through, on its sagging way from {@code a} to {@code b};
     * the blocks it is hung from are its own.
     */
    private static Optional<BlockPos> inTheWay(final ServerLevel level, final Vec3 a, final Vec3 b,
                                               final BlockPos fromPos, final BlockPos toPos) {
        final Catenary.Point start = new Catenary.Point(a.x, a.y, a.z);
        final Catenary.Point end = new Catenary.Point(b.x, b.y, b.z);
        final int checks = Math.max(Catenary.FEWEST_PIECES, (int) Math.ceil(a.distanceTo(b) * CHECKS_A_BLOCK));
        for (int k = 0; k <= checks; k++) {
            final Catenary.Point p = Catenary.at(start, end, (double) k / checks);
            final BlockPos at = BlockPos.containing(p.x(), p.y(), p.z());
            if (at.equals(fromPos) || at.equals(toPos) || !level.isLoaded(at)) {
                continue;
            }
            final BlockState state = level.getBlockState(at);
            final VoxelShape shape = state.getCollisionShape(level, at);
            if (shape.isEmpty()) {
                continue;
            }
            for (final AABB box : shape.toAabbs()) {
                if (box.move(at).contains(p.x(), p.y(), p.z())) {
                    return Optional.of(at);
                }
            }
        }
        return Optional.empty();
    }

    /* Whether the insulator of {@code end} still stands. */
    private static boolean stands(final ServerLevel level, final WireEnd end) {
        final IWireAnchors anchors = loadedAnchors(level, end.pos());
        return anchors != null && end.anchor() < anchors.anchors().size();
    }

    private static @Nullable IWireAnchors loadedAnchors(final ServerLevel level, final BlockPos pos) {
        return level.isLoaded(pos) ? level.getCapability(ANCHORS, pos, null) : null;
    }

    private static Set<Long> inGrid(final ServerLevel level) {
        return IN_GRID.computeIfAbsent(level.dimension(), key -> new HashSet<>());
    }

    /* Puts {@code wire} in its grid, joined to its two insulators, when they are loaded and it is not in it yet. */
    private static void tryEnter(final ServerLevel level, final HungWire wire) {
        if (inGrid(level).contains(wire.id())) {
            return;
        }
        final Optional<CableType> type = CoreCables.REGISTRY.getOptional(wire.cable());
        final IWireAnchors fromAnchors = loadedAnchors(level, wire.from().pos());
        final IWireAnchors toAnchors = loadedAnchors(level, wire.to().pos());
        if (type.isEmpty() || fromAnchors == null || toAnchors == null || !stands(level, wire.from())
                || !stands(level, wire.to())) {
            return;
        }
        final GridMember member = Wire.of(type.get()).member();
        placeAnchor(level, type.get(), wire.from(), fromAnchors, member);
        placeAnchor(level, type.get(), wire.to(), toAnchors, member);
        CoreGrids.place(level, type.get().grid(), wire.place(), member, List.of(anchorPlace(wire.from()),
                anchorPlace(wire.to())));
        inGrid(level).add(wire.id());
    }

    /* Takes {@code wire} out of its grid, and each of its insulators no other wire in the grid hangs from. */
    private static void leave(final ServerLevel level, final HungWire wire) {
        if (!inGrid(level).remove(wire.id())) {
            return;
        }
        final Optional<CableType> type = CoreCables.REGISTRY.getOptional(wire.cable());
        if (type.isEmpty()) {
            return;
        }
        CoreGrids.remove(level, type.get().grid(), wire.place());
        final WireBook book = WIRES.get(level);
        for (final WireEnd end : List.of(wire.from(), wire.to())) {
            final boolean held = book.at(end).stream().anyMatch(other -> other.id() != wire.id()
                    && inGrid(level).contains(other.id()));
            if (!held) {
                CoreGrids.remove(level, type.get().grid(), anchorPlace(end));
            }
        }
    }

    /*
     * Puts an insulator in its line's grid, when it is not in it already: a feedthrough's joined to the other
     * feedthroughs of its block, through their wall.
     */
    private static void placeAnchor(final ServerLevel level, final CableType type, final WireEnd end,
                                    final IWireAnchors anchors, final GridMember member) {
        final GridPlace place = anchorPlace(end);
        final OptionalLong number = CoreGrids.places(level).find(place);
        if (number.isPresent() && CoreGrids.of(level, type.grid()).contains(number.getAsLong())) {
            return;
        }
        final List<GridPlace> joined = new ArrayList<>();
        if (anchors.anchors().get(end.anchor()).role() == WireAnchor.Role.FEEDTHROUGH) {
            for (int k = 0; k < anchors.anchors().size(); k++) {
                if (k != end.anchor() && anchors.anchors().get(k).role() == WireAnchor.Role.FEEDTHROUGH) {
                    joined.add(GridPlace.anchor(end.pos().asLong(), k));
                }
            }
        }
        CoreGrids.place(level, type.grid(), place, member, joined);
    }

    private static GridPlace anchorPlace(final WireEnd end) {
        return GridPlace.anchor(end.pos().asLong(), end.anchor());
    }

    /**
     * What came of hanging a wire, and what to tell the player.
     *
     * @param hung whether it was hung
     * @param said what to tell the player
     */
    public record Outcome(boolean hung, Text said) {

        static Outcome not(final Text said) {
            return new Outcome(false, said);
        }
    }
}
