/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import com.mojang.datafixers.util.Pair;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.SealedRooms;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A blast going out. Its shock wave leaves the centre at the speed of sound of the medium and reaches each place in
 * its own time: lines walked out block by block carry it, each block it reaches stands what it stands or breaks and
 * takes that much of the wave, and a wall it cannot break shields what is behind. The blocks it breaks are broken a
 * share each tick, within the server's budget, so a great blast never stalls the server; what it does to the world
 * goes through the game's explosion events, a share each tick, for other mods and protections to take part.
 */
public final class Detonation {

    private final ServerLevel level;
    private final ResourceLocation explosive;
    private final Vec3 centre;
    private final double tnt;
    private final BlastMedium medium;
    private final BlastConsequence consequence;
    private final boolean incendiary;
    private final boolean burns;
    @Nullable
    private final Blast.Casing casing;
    private final Actor actor;
    private final BlastExplosion explosion;
    private final double fireball;
    private final double rayReach;
    private final double bodyReach;
    private final double earReach;
    private final List<BlastRay> rays = new ArrayList<>();
    private final List<Entity> bodies = new ArrayList<>();
    private final Set<UUID> rung = new HashSet<>();
    /* The blocks the wave broke or is about to break, open to it and to what it reaches through them. */
    private final LongOpenHashSet opened = new LongOpenHashSet();
    private final LongArrayFIFOQueue toBreak = new LongArrayFIFOQueue();
    private final LongArrayList scorched = new LongArrayList();
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private double front;
    private int steps;
    private int cursor;
    private int aliveRays;
    private int brokenBlocks;
    private int thrownDebris;
    private int struckByFragments;
    /* How many of the broken blocks could have been thrown, and how many could have been lit: the first of each is,
     * then one in every so many. */
    private int throwable;
    private int lightable;
    private boolean finished;

    /* How many lines go out for each square metre of the sphere they reach to, and the fewest and most of them. */
    private static final double RAYS_PER_SQUARE_METRE = 1;
    private static final int FEWEST_RAYS = 64;
    private static final int MOST_RAYS = 16_384;
    /* The farthest a blast reaches, in blocks, however large. */
    private static final double FARTHEST = 256;
    private static final int TICKS_A_SECOND = 20;
    /* The overpressure out to which it breaks stone, the radius the drops of what it breaks take their chance from. */
    private static final double DROPS_FROM = 150;
    /* The overpressure that rings the ears, in kilopascals. */
    private static final double RINGS_EARS = 2;
    /* Each how many broken blocks one is thrown as debris, the most thrown, and how far past what it stands. */
    private static final int DEBRIS_EVERY = 8;
    private static final int MOST_DEBRIS = 32;
    private static final double DEBRIS_OVER = 2;
    private static final double DEBRIS_LIFT = 0.5;
    private static final float DEBRIS_HARM = 2F;
    private static final int DEBRIS_MOST_HARM = 40;
    /*
     * Each how many broken blocks near the fireball one is set alight, and how many times the fireball's radius its
     * heat scorches the ground out to, past the crater the wave dug.
     */
    private static final int FIRE_EVERY = 3;
    private static final double SCORCHES = 2.5;
    /* The least and most an effect is sent to players from, in blocks. */
    private static final int NEAREST_SHOWN = 64;
    private static final int FARTHEST_SHOWN = 512;
    /* How many times as far as its screen effect the show of a blast is sent, that effect's radius at its own size,
     * and the smallest the show of a blast is. */
    private static final double SHOWN_PAST_SCREEN = 2;
    private static final double SCREEN_RADIUS = 32;
    private static final double SMALLEST_SHOW = 0.5;
    /* How many of a thing one dropped stack is joined up to, as the game's explosions do. */
    private static final int MERGED = 16;

    Detonation(final Blast blast) {
        this.level = blast.level();
        this.explosive = blast.explosive();
        this.centre = blast.centre();
        this.tnt = BlastMath.tnt(blast.joules());
        final BlastMedium found = blast.medium().orElseGet(() -> Explosions.mediumAt(this.level, this.centre));
        this.medium = found;
        this.consequence = BlastBalance.consequenceOf(this.explosive).atMost(blast.most());
        this.incendiary = blast.isIncendiary();
        this.burns = GasMath.burns(SealedRooms.oxygenAt(this.level, BlockPos.containing(this.centre)));
        this.casing = blast.casing().orElse(null);
        this.fireball = BlastMath.fireball(this.tnt);
        this.rayReach = Math.min(FARTHEST, found.reach(this.tnt, BlastMath.FAINTEST));
        this.bodyReach = Math.min(FARTHEST, found.reach(this.tnt, BlastMath.HARMLESS));
        this.earReach = Math.min(FARTHEST, found.reach(this.tnt, RINGS_EARS));
        this.explosion = new BlastExplosion(this.level, blast.source(), this.centre,
                (float) Math.max(1, found.reach(this.tnt, DROPS_FROM)), this.incendiary,
                this.consequence.breaksBlocks(), this);
        final Actor blamed = Protection.actorOf(this.level.damageSources().explosion(this.explosion));
        this.actor = blamed instanceof Actor.World world && world.origin() == null
                ? Actor.world(BlockPos.containing(this.centre)) : blamed;
    }

    /** What went off, as its mod names it. */
    public ResourceLocation explosive() {
        return this.explosive;
    }

    /** The level it went off in. */
    public ServerLevel level() {
        return this.level;
    }

    /** Where it went off. */
    public Vec3 centre() {
        return this.centre;
    }

    /** How large it is, in kilograms of TNT. */
    public double tnt() {
        return this.tnt;
    }

    /** What it goes out through. */
    public BlastMedium medium() {
        return this.medium;
    }

    /** What it does to the world, as the server's setting and its mod's allow. */
    public BlastConsequence consequence() {
        return this.consequence;
    }

    /** The explosion the game's events and damage name. */
    public BlastExplosion explosion() {
        return this.explosion;
    }

    /** How far its shock wave has gone, in blocks. */
    public double front() {
        return this.front;
    }

    /** The radius of its fireball, in blocks. */
    public double fireball() {
        return this.fireball;
    }

    /** How far it breaks anything at all, in blocks: where the overpressure falls below a window's. */
    public double reach() {
        return this.rayReach;
    }

    /** How far it hurts a body in the open, in blocks. */
    public double bodyReach() {
        return this.bodyReach;
    }

    /** How far it rings the ears, in blocks. */
    public double earReach() {
        return this.earReach;
    }

    /** How many blocks it has broken. */
    public int brokenBlocks() {
        return this.brokenBlocks;
    }

    /** How many of the blocks it broke it threw as debris. */
    public int thrownDebris() {
        return this.thrownDebris;
    }

    /** How many of its casing's fragments struck a body. */
    public int struckByFragments() {
        return this.struckByFragments;
    }

    /** How many blocks it has reached but not yet broken, waiting for the budget. */
    public int waiting() {
        return this.toBreak.size();
    }

    /** Whether it has done all it does. */
    public boolean finished() {
        return this.finished;
    }

    /* Goes off: the flash, the boom, the fragments, and the lines the wave goes out along. */
    void begin() {
        final float scale = (float) Math.max(SMALLEST_SHOW, this.fireball / BlastMath.fireball(1));
        Effects.at(this.level, Explosions.EFFECT, this.centre).scale(scale)
                .reach((int) Math.clamp(SCREEN_RADIUS * scale * SHOWN_PAST_SCREEN, NEAREST_SHOWN, FARTHEST_SHOWN))
                .send();
        Audio.cue(this.level, this.centre.x, this.centre.y, this.centre.z, Explosions.BLAST, SoundContext.EMPTY, 1F,
                1F);
        if (!this.consequence.hurts()) {
            return;
        }
        if (this.casing != null) {
            this.struckByFragments = Fragments.fly(this, this.casing.material(), this.casing.kilograms());
        }
        final double reached = Math.max(this.bodyReach, this.fireball);
        this.bodies.addAll(this.level.getEntities((Entity) null, AABB.ofSize(this.centre, reached * 2, reached * 2,
                reached * 2), entity -> !entity.isSpectator()));
        if (this.consequence.breaksBlocks() && this.medium.carriesWave()) {
            spreadRays();
        }
    }

    /* Goes a tick further, within {@code budget}. */
    void step(final Budget budget) {
        if (this.finished) {
            return;
        }
        this.steps++;
        this.front = this.medium.carriesWave() ? this.steps * this.medium.speed() / TICKS_A_SECOND : 0;
        walk(budget);
        final List<Entity> struck = reachedBodies();
        ringEars();
        final List<BlockPos> batch = new ArrayList<>();
        while (!this.toBreak.isEmpty() && budget.breaks > 0) {
            batch.add(BlockPos.of(this.toBreak.dequeueLong()));
            budget.breaks--;
        }
        if (!batch.isEmpty() || !struck.isEmpty()) {
            this.explosion.clearToBlow();
            this.explosion.getToBlow().addAll(batch);
            NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(this.level, this.explosion, struck));
            final Set<BlockPos> allowed = new HashSet<>(this.explosion.getToBlow());
            for (final BlockPos pos : batch) {
                if (!allowed.contains(pos)) {
                    // Kept by another mod or a protection: it stands, and shields as a standing block does.
                    this.opened.remove(pos.asLong());
                }
            }
            breakBlocks(List.copyOf(this.explosion.getToBlow()));
            this.explosion.clearToBlow();
            for (final Entity body : struck) {
                BlastBodies.strike(this, body);
            }
        }
        scorch();
        this.finished = this.toBreak.isEmpty() && this.aliveRays == 0
                && (!this.medium.carriesWave() || this.front >= Math.max(this.bodyReach, this.earReach));
    }

    /* Whether the block at {@code pos} is open to the wave: broken by it, or about to be. */
    boolean opened(final long pos) {
        return this.opened.contains(pos);
    }

    /* Whether it may hurt {@code body}, as the server, the claims and the teams allow. */
    boolean mayHurt(final Entity body) {
        if (!this.consequence.hurts()) {
            return false;
        }
        return body instanceof ServerPlayer player ? Protection.mayHurt(this.actor, player)
                : Protection.may(this.actor, ProtectedAction.HURT, this.level, body.blockPosition());
    }

    /* Whether its fireball sets fires: it was made to and there is air to burn. */
    boolean incendiary() {
        return this.incendiary;
    }

    /* Whether there is air to burn where it went off. */
    boolean burns() {
        return this.burns;
    }

    /* The lines the wave goes out along, spread evenly over the sphere and turned by where it went off. */
    private void spreadRays() {
        final int count = (int) Math.clamp(Math.ceil(4 * Math.PI * this.rayReach * this.rayReach
                * RAYS_PER_SQUARE_METRE), FEWEST_RAYS, MOST_RAYS);
        final double golden = Math.PI * (3 - Math.sqrt(5));
        final double turn = (this.centre.x * 0.53 + this.centre.y * 0.29 + this.centre.z * 0.71) % (2 * Math.PI);
        for (int i = 0; i < count; i++) {
            final double up = 1 - 2 * (i + 0.5) / count;
            final double round = Math.sqrt(1 - up * up);
            final double angle = i * golden + turn;
            this.rays.add(new BlastRay(this.centre.x, this.centre.y, this.centre.z, Math.cos(angle) * round, up,
                    Math.sin(angle) * round));
        }
        this.aliveRays = count;
    }

    /* Walks each line out to the front of the wave, as far as the budget lets, from where the last tick stopped. */
    private void walk(final Budget budget) {
        final int count = this.rays.size();
        for (int done = 0; done < count && this.aliveRays > 0; done++) {
            final int index = (this.cursor + done) % count;
            final BlastRay ray = this.rays.get(index);
            while (ray.alive() && ray.nextDistance() <= this.front) {
                if (budget.looks <= 0) {
                    this.cursor = index;
                    return;
                }
                budget.looks--;
                ray.advance();
                if (ray.distance() > this.rayReach) {
                    ray.stop();
                } else {
                    visit(ray);
                }
                if (!ray.alive()) {
                    this.aliveRays--;
                }
            }
        }
    }

    /* The wave reaches the block the line {@code ray} is in: it breaks it, passes it or stops at it. */
    private void visit(final BlastRay ray) {
        this.at.set(ray.x(), ray.y(), ray.z());
        if (!this.level.isLoaded(this.at)) {
            ray.stop();
            return;
        }
        final long key = this.at.asLong();
        if (this.opened.contains(key)) {
            return;
        }
        final BlockState state = this.level.getBlockState(this.at);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return;
        }
        final double free = this.medium.overpressure(this.tnt, ray.distance());
        final double pressure = free * ray.share();
        if (pressure < BlastMath.FAINTEST) {
            ray.stop();
            return;
        }
        final double strength = BlastStrength.of(this.level, this.at, state, this.explosion);
        if (pressure > strength && state.getDestroySpeed(this.level, this.at) >= 0
                && Protection.may(this.actor, ProtectedAction.EXPLODE, this.level, this.at)) {
            this.opened.add(key);
            this.toBreak.enqueue(key);
            ray.spend(strength / free);
        } else if (!ray.atCentre() && state.isCollisionShapeFullBlock(this.level, this.at)) {
            ray.stop();
            if (ray.distance() <= this.fireball * SCORCHES) {
                this.scorched.add(key);
            }
        }
    }

    /* The bodies the front has reached since the last tick; in a vacuum, the fireball's at once and no more. */
    private List<Entity> reachedBodies() {
        final List<Entity> struck = new ArrayList<>();
        final Iterator<Entity> left = this.bodies.iterator();
        while (left.hasNext()) {
            final Entity body = left.next();
            final double distance = body.getBoundingBox().getCenter().distanceTo(this.centre);
            if (body.isRemoved()) {
                left.remove();
            } else if (distance <= this.front || !this.medium.carriesWave()) {
                left.remove();
                if (distance <= this.front || distance <= this.fireball) {
                    struck.add(body);
                }
            }
        }
        return struck;
    }

    /* The ears the wave has reached since the last tick ring. */
    private void ringEars() {
        if (!this.medium.carriesWave()) {
            return;
        }
        for (final ServerPlayer player : this.level.players()) {
            final double distance = player.getEyePosition().distanceTo(this.centre);
            if (distance <= this.front && distance <= this.earReach && this.rung.add(player.getUUID())) {
                BlastBodies.ring(this, player);
            }
        }
    }

    /* Breaks {@code broken}: each drops what the game's explosions drop of it, goes off if it is an explosive, or is
     * thrown as debris; the floor of the crater is scorched and, if the blast sets fires, the fireball lights it. */
    private void breakBlocks(final List<BlockPos> broken) {
        final List<Pair<ItemStack, BlockPos>> drops = new ArrayList<>();
        final List<BlockPos> alight = new ArrayList<>();
        for (final BlockPos pos : broken) {
            final BlockState state = this.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            this.brokenBlocks++;
            final double distance = Math.sqrt(pos.distToCenterSqr(this.centre));
            if (thrown(pos, state, distance)) {
                continue;
            }
            state.onExplosionHit(this.level, pos, this.explosion, (stack, from) -> gather(drops, stack, from));
            if (distance <= this.fireball * SCORCHES) {
                this.scorched.add(pos.below().asLong());
                if (this.incendiary && this.burns && this.lightable++ % FIRE_EVERY == 0) {
                    alight.add(pos);
                }
            }
        }
        for (final Pair<ItemStack, BlockPos> drop : drops) {
            Block.popResource(this.level, drop.getSecond(), drop.getFirst());
        }
        for (final BlockPos pos : alight) {
            if (this.level.getBlockState(pos).isAir() && this.level.getBlockState(pos.below())
                    .isSolidRender(this.level, pos.below())
                    && Protection.may(this.actor, ProtectedAction.BURN, this.level, pos)) {
                this.level.setBlockAndUpdate(pos, BaseFireBlock.getState(this.level, pos));
            }
        }
    }

    /* Throws the block {@code state} at {@code pos} as debris, if it is one to throw; whether it was. */
    private boolean thrown(final BlockPos pos, final BlockState state, final double distance) {
        if (this.thrownDebris >= MOST_DEBRIS || !state.is(Explosions.THROWN) || state.hasBlockEntity()) {
            return false;
        }
        final double pressure = this.medium.overpressure(this.tnt, distance);
        if (pressure <= DEBRIS_OVER * BlastStrength.of(this.level, pos, state, this.explosion)
                || this.throwable++ % DEBRIS_EVERY != 0) {
            return false;
        }
        final Vec3 middle = Vec3.atCenterOf(pos);
        final Vec3 away = middle.subtract(this.centre).normalize();
        final double speed = BlastMath.thrown(pressure);
        final FallingBlockEntity debris = FallingBlockEntity.fall(this.level, pos, state);
        debris.setDeltaMovement(away.x * speed, away.y * speed + DEBRIS_LIFT * speed, away.z * speed);
        debris.setHurtsEntities(DEBRIS_HARM, DEBRIS_MOST_HARM);
        debris.hurtMarked = true;
        this.thrownDebris++;
        return true;
    }

    /* Scorches the ground the fireball touched: grass and moss burnt down to coarse earth. */
    private void scorch() {
        for (int i = 0; i < this.scorched.size(); i++) {
            final BlockPos pos = BlockPos.of(this.scorched.getLong(i));
            if (!this.opened.contains(pos.asLong()) && this.level.getBlockState(pos).is(Explosions.SCORCHED)
                    && Protection.may(this.actor, ProtectedAction.EXPLODE, this.level, pos)) {
                this.level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        this.scorched.clear();
    }

    /* Adds {@code stack} to the drops, joined to a like stack where it fits, as the game's explosions do. */
    private static void gather(final List<Pair<ItemStack, BlockPos>> drops, final ItemStack stack,
                               final BlockPos pos) {
        ItemStack rest = stack;
        for (int i = 0; i < drops.size() && !rest.isEmpty(); i++) {
            final Pair<ItemStack, BlockPos> drop = drops.get(i);
            if (ItemEntity.areMergable(drop.getFirst(), rest)) {
                drops.set(i, Pair.of(ItemEntity.merge(drop.getFirst(), rest, MERGED), drop.getSecond()));
            }
        }
        if (!rest.isEmpty()) {
            drops.add(Pair.of(rest, pos));
        }
    }

    /* What the blasts of a level may still do in a tick: blocks to look at and blocks to break. */
    static final class Budget {

        int looks;
        int breaks;

        Budget(final int looks, final int breaks) {
            this.looks = looks;
            this.breaks = breaks;
        }
    }
}
