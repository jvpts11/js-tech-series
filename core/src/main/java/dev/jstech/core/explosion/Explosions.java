/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Explosions with real physics, decided by their energy: {@link #blast} one and set it off. The shock wave goes out at
 * the speed of sound, pushes through blocks by the strength of each, hurts and throws the bodies it reaches by the
 * overpressure there; a casing breaks into fragments, broken blocks are thrown as debris, explosives it breaks go off,
 * the ground of the crater is scorched, and the medium decides the rest: a vacuum carries no wave, water carries it
 * far. What each mod's explosions do to the world is the server's setting, and claimed land is respected.
 */
@TextHolder
public final class Explosions {

    /** The effect a blast shows: the fireball, the smoke, the sparks, the flash and the scorch marks. */
    public static final ResourceLocation EFFECT = id("explosion");
    /** What fragments kill as, with its own death message. */
    public static final ResourceKey<DamageType> SHRAPNEL = ResourceKey.create(Registries.DAMAGE_TYPE, id("shrapnel"));
    /** The blocks a blast throws as debris instead of breaking: earth, stone, wood, brick. */
    public static final TagKey<Block> THROWN = TagKey.create(Registries.BLOCK, id("thrown_by_blasts"));
    /** The ground a fireball scorches down to coarse earth: grass, moss, mycelium. */
    public static final TagKey<Block> SCORCHED = TagKey.create(Registries.BLOCK, id("scorched_by_blasts"));
    /** The boom, silent until a pack gives it its sound; it arrives after the flash, by the speed of sound. */
    public static final SoundCue BLAST = CoreItems.CONTENT.cue("explosion/blast").world().range(256).register();
    public static final TextKey DIED = TextKey.of("death.attack.jscore.shrapnel", "%1$s was struck by shrapnel");
    public static final TextKey DIED_FIGHTING = TextKey.of("death.attack.jscore.shrapnel.player",
            "%1$s was struck by shrapnel from %2$s");
    /* How large the game's own explosions are shown, for each point of their power: TNT's four is the effect's own. */
    private static final float SHOWN_PER_POWER = 0.25F;
    private static final int GAME_SHOWN = 128;
    /* The blasts going out in each level, oldest first; touched on the server's thread only. */
    private static final Map<ResourceKey<Level>, List<Detonation>> RUNNING = new HashMap<>();

    private Explosions() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        BlastStrength.listen(modBus);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                RUNNING.remove(level.dimension());
            }
        });
        NeoForge.EVENT_BUS.addListener(ExplosionEvent.Detonate.class, Explosions::onGameExplosion);
    }

    /** Declares the boom while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * A blast of {@code joules} at {@code centre}, of the explosive {@code explosive} (its id, whose mod's setting
     * decides what it does to the world): say more, then {@link Blast#detonate()}. A kilogram of TNT is 4.184 MJ.
     */
    public static Blast blast(final ServerLevel level, final Vec3 centre, final ResourceLocation explosive,
                              final double joules) {
        return new Blast(level, centre, explosive, joules);
    }

    /** The blasts going out in {@code level} now, oldest first. */
    public static List<Detonation> running(final ServerLevel level) {
        return List.copyOf(RUNNING.getOrDefault(level.dimension(), List.of()));
    }

    /** Whether {@code explosion} is one of these blasts, as against the game's own or another mod's. */
    public static boolean isBlast(final Explosion explosion) {
        return explosion instanceof BlastExplosion;
    }

    /**
     * What a blast at {@code at} goes out through: water if it goes off in water, else the air of the sealed room or
     * the place there, at its pressure and temperature, a vacuum when there is next to none; paced as the laws of the
     * place say when they slow explosions.
     */
    public static BlastMedium mediumAt(final Level level, final Vec3 at) {
        final BlockPos pos = BlockPos.containing(at);
        final BlastMedium medium;
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            medium = BlastMedium.WATER;
        } else {
            final Climate climate = SealedRooms.climateAt(level, pos);
            medium = BlastMedium.air(climate.pressure(), climate.temperature());
        }
        final double pace = Physics.lawsAt(level, at).matter().map(Laws.Matter::explosionSpeed).orElse(0.0);
        return pace > 0 ? medium.paced(pace) : medium;
    }

    static Optional<Detonation> detonate(final Blast blast) {
        final Detonation detonation = new Detonation(blast);
        if (NeoForge.EVENT_BUS.post(new ExplosionEvent.Start(blast.level(), detonation.explosion())).isCanceled()) {
            return Optional.empty();
        }
        detonation.begin();
        RUNNING.computeIfAbsent(blast.level().dimension(), key -> new ArrayList<>()).add(detonation);
        return Optional.of(detonation);
    }

    /* Each blast of {@code level} goes a tick further, oldest first, sharing the tick's budget. */
    private static void tick(final ServerLevel level) {
        final List<Detonation> running = RUNNING.get(level.dimension());
        if (running == null || running.isEmpty()) {
            return;
        }
        final Detonation.Budget budget = new Detonation.Budget(BlastBalance.lookedAt(), BlastBalance.broken());
        // A copy: an explosive a blast breaks may set off another, which joins the list.
        for (final Detonation detonation : List.copyOf(running)) {
            detonation.step(budget);
        }
        running.removeIf(Detonation::finished);
    }

    /* The game's own explosions show the Core's fireball, smoke and marks too, a wind charge's gust aside. */
    private static void onGameExplosion(final ExplosionEvent.Detonate event) {
        final Explosion explosion = event.getExplosion();
        if (!BlastBalance.gameExplosionsShown() || isBlast(explosion)
                || explosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Effects.at(level, EFFECT, explosion.center()).scale(explosion.radius() * SHOWN_PER_POWER).reach(GAME_SHOWN)
                .send();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
