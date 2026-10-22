/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import com.mojang.blaze3d.audio.Channel;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.SoundTraits;
import dev.jstech.core.audio.acoustics.AcousticSpace;
import dev.jstech.core.audio.acoustics.MaterialSounds;
import dev.jstech.core.audio.acoustics.Reverb;
import dev.jstech.core.audio.acoustics.SoundMedium;
import dev.jstech.core.audio.acoustics.SoundTravel;
import dev.jstech.core.client.audio.AlertSigns;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.core.stepaside.StepAside;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * How every sound of the world reaches the listener, on its way to the speakers: not at all through a vacuum, unless
 * the listener touches what makes it; late, as far as it comes at the speed of sound; muffled by the walls between,
 * by what each is made of; weaker in thin air and dull under water; echoing in the place the listener stands in; and
 * higher or lower as it and the listener move, by the Doppler effect. What a block's steps and blows play can be a
 * resource pack's, by what the block is made of; a sound declared with a distant one plays that from far away; a
 * sound that matters is shown on screen to a player who asked for signs.
 *
 * <p>A sound is weighed on the game's thread as it is asked for, the walls between kept by the two places a couple of
 * seconds so a sound heard again from the same spot costs nothing, and its shape is put on its source on the sound
 * thread as it starts; long sounds and moving ones are weighed again as they play, on the acoustics' own thread
 * ({@link AcousticWork}), so the game never waits on them.
 */
public final class SoundShaper {

    /* What each sound about to play was weighed as, kept until its source starts. */
    private static final Map<SoundInstance, Shape> SHAPES = Collections.synchronizedMap(new WeakHashMap<>());
    /* The pitch each sound about to play was given beside its own, kept until its source starts. */
    private static final Map<SoundInstance, Float> PITCHES = Collections.synchronizedMap(new WeakHashMap<>());
    /* What else changes the pitch of what the player hears, each giving a factor. */
    private static final List<DoubleSupplier> PITCH_SHIFTS = new CopyOnWriteArrayList<>();
    /* The sounds held back by the speed of sound, which are not held back again when they come round. */
    private static final Set<SoundInstance> DELAYED = Collections.newSetFromMap(
            Collections.synchronizedMap(new WeakHashMap<>()));
    /* Every sound playing, with its source, while it plays. */
    private static final Map<SoundInstance, Playing> PLAYING = new ConcurrentHashMap<>();
    /* How often long and moving sounds are weighed again, and the place's echo, in ticks. */
    private static final int REWEIGH_EVERY = 10;
    private static final int ECHO_EVERY = 5;
    /* How far the echo moves toward the place's at each look, as a share of the way. */
    private static final double EASE = 0.3;
    private static final double TICK_SECONDS = 0.05;

    private static Reverb echo = Reverb.DRY;
    private static @Nullable Vec3 lastListener;
    private static long ticks;

    private SoundShaper() {
    }

    /** Hands the buses what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(SoundEngineLoadEvent.class, event -> Efx.forget());
        NeoForge.EVENT_BUS.addListener(PlaySoundSourceEvent.class,
                event -> started(event.getSound(), event.getChannel()));
        NeoForge.EVENT_BUS.addListener(PlayStreamingSourceEvent.class,
                event -> started(event.getSound(), event.getChannel()));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> forget());
    }

    /**
     * Adds something that changes the pitch of every sound of the world the player hears, once, from a mod's client:
     * {@code factor} is asked as each sound is about to play, and what it gives, with every other such factor, is what
     * the sound's own pitch is multiplied by; 1 changes nothing. A hazard of the body gives one, sounds wavering higher
     * and lower. Sounds of the screen and music are left as they are.
     */
    public static void shiftPitch(final DoubleSupplier factor) {
        PITCH_SHIFTS.add(Objects.requireNonNull(factor, "factor"));
    }

    /**
     * The sound as it is to be heard where the listener is: nothing through a vacuum, nothing now when it is held back
     * by the speed of sound (it comes round later), another sound in its place when a pack or its declaration says so,
     * and otherwise itself, weighed for when it starts.
     */
    public static @Nullable SoundInstance shape(@Nullable final SoundInstance sound) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft.level;
        if (sound == null || level == null) {
            return sound;
        }
        if (sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
            SHAPES.put(sound, Shape.within());
            return sound;
        }
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final Vec3 listener = listener(minecraft);
        final Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        final double blocks = listener.distanceTo(at);
        final SoundMedium medium = ListenerAir.medium();
        if (medium.gain(blocks) <= 0) {
            return null;
        }
        SoundInstance heard = material(level, sound);
        final Optional<ResourceLocation> distant = SoundTraits.of(heard.getLocation()).heardAt(blocks);
        if (distant.isPresent()) {
            heard = new RelocatedSoundInstance(heard, distant.get());
        }
        if (prefs.acoustics() && !heard.isLooping() && !(heard instanceof TickableSoundInstance)
                && !DELAYED.remove(heard)) {
            final int late = SoundTravel.delayTicks(blocks, medium.speed());
            if (late > 0) {
                DELAYED.add(heard);
                minecraft.getSoundManager().playDelayed(heard, late);
                return null;
            }
        }
        if (SoundTraits.of(sound.getLocation()).matters()) {
            AlertSigns.raise(heard);
        }
        SHAPES.put(heard, weigh(level, listener, at, medium, SoundDirector.muffles(heard), false));
        final float pitch = pitchShift();
        if (pitch != 1F) {
            PITCHES.put(heard, pitch);
        }
        return heard;
    }

    /** The place's echo as it is put on the sound card now, for the debug screen. */
    static synchronized Reverb echo() {
        return echo;
    }

    /** How many sounds play now, each with its source, for the debug overlay. */
    static Map<SoundInstance, Playing> playing() {
        return Map.copyOf(PLAYING);
    }

    /* On the sound thread: the sound's source has started; its shape goes on it, and it is kept while it plays. */
    private static void started(final SoundInstance sound, final Channel channel) {
        final Shape shape = SHAPES.remove(sound);
        final Shape put = shape == null ? Shape.within() : shape;
        Efx.shape(channel.source, put.gain(), put.highs(), put.sendGain(), put.sendHighs(), put.air());
        final Float pitch = PITCHES.remove(sound);
        if (pitch != null) {
            Efx.pitch(channel.source, pitch);
        }
        PLAYING.put(sound, new Playing(channel.source, new Vec3(sound.getX(), sound.getY(), sound.getZ())));
    }

    private static void tick() {
        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft.level;
        if (level == null || minecraft.gameRenderer == null) {
            return;
        }
        ticks++;
        RingingEars.tick();
        final Vec3 listener = listener(minecraft);
        AcousticProbe.tick(level, listener, level.getGameTime());
        final SoundMedium medium = ListenerAir.medium();
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final boolean moving = prefs.acoustics();
        final Vec3 before = lastListener;
        lastListener = listener;
        final SoundTravel.Motion walking = moving && before != null ? SoundTravel.velocity(listener.x - before.x,
                listener.y - before.y, listener.z - before.z, TICK_SECONDS) : SoundTravel.Motion.STILL;
        Efx.listener((float) walking.x(), (float) walking.y(), (float) walking.z(), (float) medium.speed());
        final boolean reweigh = ticks % REWEIGH_EVERY == 0 || RingingEars.ringing();
        final SoundManager sounds = minecraft.getSoundManager();
        PLAYING.entrySet().removeIf(entry -> !sounds.isActive(entry.getKey()));
        final List<Reweighed> again = new ArrayList<>();
        for (final Map.Entry<SoundInstance, Playing> entry : PLAYING.entrySet()) {
            final SoundInstance sound = entry.getKey();
            final Playing playing = entry.getValue();
            final Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
            final boolean placed = !sound.isRelative() && sound.getAttenuation() != SoundInstance.Attenuation.NONE;
            if (placed && sound instanceof TickableSoundInstance) {
                final SoundTravel.Motion motion = moving ? SoundTravel.velocity(at.x - playing.at().x,
                        at.y - playing.at().y, at.z - playing.at().z, TICK_SECONDS) : SoundTravel.Motion.STILL;
                Efx.velocity(playing.source(), (float) motion.x(), (float) motion.y(), (float) motion.z());
                entry.setValue(new Playing(playing.source(), at));
            }
            if (reweigh && (sound.isLooping() || sound instanceof TickableSoundInstance || RingingEars.ringing())) {
                again.add(new Reweighed(playing.source(), placed ? at : null, SoundDirector.muffles(sound)));
            }
        }
        if (!again.isEmpty()) {
            // The walls between are read again away from the game's thread, and each source shaped as they come.
            AcousticWork.inBackground(() -> {
                for (final Reweighed one : again) {
                    final Shape shape = one.at() == null ? Shape.within()
                            : weigh(level, listener, one.at(), medium, one.muffles(), true);
                    Efx.shape(one.source(), shape.gain(), shape.highs(), shape.sendGain(), shape.sendHighs(),
                            shape.air());
                }
            });
        }
        if (ticks % ECHO_EVERY == 0) {
            setEcho(target(medium, prefs));
        }
    }

    /* The echo the place round the listener gives, in its medium: none in the open air, under water or in a vacuum. */
    private static Reverb target(final SoundMedium medium, final AudioPrefs prefs) {
        if (!prefs.acoustics() || !Efx.echoesOurs() || medium.kind() != SoundMedium.Kind.AIR) {
            return Reverb.DRY;
        }
        AcousticSpace space = AcousticProbe.space();
        if (ListenerAir.inRoom()) {
            space = space.inRoom(ListenerAir.now().roomVolume(), ListenerAir.now().roomSurface());
        }
        final Reverb place = space.reverb();
        // Thin air carries the echo as faintly as it carries the sound.
        return new Reverb(place.decaySeconds(), place.wet() * medium.gain(0), place.reflectionsDelay(),
                place.lateDelay(), place.highRatio());
    }

    private static synchronized void setEcho(final Reverb target) {
        echo = new Reverb(ease(echo.decaySeconds(), target.decaySeconds()), ease(echo.wet(), target.wet()),
                ease(echo.reflectionsDelay(), target.reflectionsDelay()), ease(echo.lateDelay(), target.lateDelay()),
                ease(echo.highRatio(), target.highRatio()));
        Efx.echo(echo);
    }

    private static double ease(final double now, final double target) {
        return now + (target - now) * EASE;
    }

    /*
     * How a sound at {@code at} reaches the listener through the walls between, the medium and ringing ears; the walls
     * read again from the world when {@code fresh}, else as kept for the two places while they are fresh.
     */
    private static Shape weigh(final Level level, final Vec3 listener, final Vec3 at, final SoundMedium medium,
                               final boolean mufflesItself, final boolean fresh) {
        final double blocks = listener.distanceTo(at);
        final boolean occludes = AudioPrefsStore.prefs().occlusion() && !StepAside.SOUND_PHYSICS.steps();
        final float walls = !occludes ? 1F : fresh ? AcousticWork.wallsNow(level, listener, at, ticks)
                : AcousticWork.walls(level, listener, at, ticks);
        final float ears = RingingEars.heard();
        final float earHighs = RingingEars.highs();
        final float carried = (float) medium.gain(blocks);
        final float highs = (float) medium.highs(blocks);
        final boolean echoes = AudioPrefsStore.prefs().acoustics() && Efx.echoesOurs();
        // Walls take a sound's high notes before its loudness, so they count twice in what the highs keep.
        return new Shape(carried * (mufflesItself ? 1F : walls) * ears, highs * walls * walls * earHighs,
                echoes ? carried * ears : 0F, highs * earHighs, medium.kind() == SoundMedium.Kind.AIR ? 1F : 0F);
    }

    /*
     * The sound a block plays for a step on it, a blow, its breaking, its placing or a fall, as a pack says its
     * material sounds; the game's own sound when no pack says so.
     */
    private static SoundInstance material(final Level level, final SoundInstance sound) {
        if (!(sound instanceof SimpleSoundInstance) || BlockAcoustics.silent()) {
            return sound;
        }
        final BlockPos at = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        for (final BlockPos pos : new BlockPos[] {at, at.below()}) {
            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            final MaterialSounds.Kind kind = kindOf(state.getSoundType(level, pos, null), sound.getLocation());
            if (kind == MaterialSounds.Kind.STEP && StepAside.FOOTSTEPS.steps()) {
                // Presence Footsteps plays the steps by what is walked on.
                return sound;
            }
            if (kind != null) {
                return BlockAcoustics.of(state).sound(kind)
                        .<SoundInstance>map(own -> new RelocatedSoundInstance(sound, ResourceLocation.parse(own)))
                        .orElse(sound);
            }
        }
        return sound;
    }

    private static @Nullable MaterialSounds.Kind kindOf(final SoundType type, final ResourceLocation played) {
        if (type.getStepSound().getLocation().equals(played)) {
            return MaterialSounds.Kind.STEP;
        }
        if (type.getHitSound().getLocation().equals(played)) {
            return MaterialSounds.Kind.HIT;
        }
        if (type.getBreakSound().getLocation().equals(played)) {
            return MaterialSounds.Kind.BREAK;
        }
        if (type.getPlaceSound().getLocation().equals(played)) {
            return MaterialSounds.Kind.PLACE;
        }
        return type.getFallSound().getLocation().equals(played) ? MaterialSounds.Kind.FALL : null;
    }

    private static Vec3 listener(final Minecraft minecraft) {
        return minecraft.gameRenderer.getMainCamera().getPosition();
    }

    /* Every factor of the pitch put together, as a sound is about to play. */
    private static float pitchShift() {
        double factor = 1;
        for (final DoubleSupplier shift : PITCH_SHIFTS) {
            factor *= shift.getAsDouble();
        }
        return (float) factor;
    }

    private static void forget() {
        SHAPES.clear();
        PITCHES.clear();
        AcousticWork.forget();
        DELAYED.clear();
        PLAYING.clear();
        AcousticProbe.forget();
        ListenerAir.forget();
        RingingEars.stop();
        lastListener = null;
        synchronized (SoundShaper.class) {
            echo = Reverb.DRY;
        }
    }

    /**
     * How a sound is put on its source.
     *
     * @param gain      what reaches the listener straight, from 1
     * @param highs     how much of its high notes that keeps, from 1
     * @param sendGain  how much of it goes into the place's echo
     * @param sendHighs how much of its high notes the echo keeps
     * @param air       how much the air takes away of its high notes with distance, 1 in air and 0 elsewhere
     */
    private record Shape(float gain, float highs, float sendGain, float sendHighs, float air) {

        /* A sound from within the listener's own head or screen: only ringing ears change it. */
        static Shape within() {
            return new Shape(RingingEars.heard(), RingingEars.highs(), 0F, 1F, 0F);
        }
    }

    /**
     * A sound playing.
     *
     * @param source the sound card's source it plays from
     * @param at     where it was at the last tick
     */
    record Playing(int source, Vec3 at) {
    }

    /**
     * A sound playing, to be weighed again away from the game's thread.
     *
     * @param source  the sound card's source it plays from
     * @param at      where it is, or null for one of the listener's own head or screen
     * @param muffles whether it muffles itself, as a sound from inside a helmet does
     */
    private record Reweighed(int source, @Nullable Vec3 at, boolean muffles) {
    }
}
