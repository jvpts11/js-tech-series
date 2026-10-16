/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.Season;
import dev.jstech.core.time.WorldCalendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The events the worlds go through ({@link WorldEventType}): each kind declared in code by its mod, or a file a data
 * pack writes over or adds under {@code data/<namespace>/jstech/world_events/}. Once a day each kind of a dimension is
 * rolled for, or found due by its schedule, and planned; those carrying its instrument are warned as it nears, it
 * begins, its effect plays round the players it covers, and it ends, each step posted on the game's bus
 * ({@link WorldEventStage}). What is planned and under way is kept with each dimension, so a restart loses nothing,
 * and any mod, or a computer of the series, reads it here.
 *
 * <pre>{@code
 * public static final ResourceLocation SOLAR_STORM = WorldEvents.event(rl("solar_storm"), MyTexts.SOLAR_STORM)
 *         .in(Level.OVERWORLD).chance(0.05).warning(6000).instrument(MyTags.MAGNETOMETERS).lasts(12000)
 *         .effect(rl("aurora")).declare();
 * }</pre>
 */
@TextHolder
public final class WorldEvents {

    public static final TextKey KIND = TextKey.of("jscore.world_event.kind", "World events");
    public static final TextKey EXPECTED = TextKey.of("jscore.world_event.expected", "Expected in %s");
    public static final TextKey BEGAN = TextKey.of("jscore.world_event.began", "Under way now");
    /** The kinds the data packs write, by id. */
    public static final DataRegistry<WorldEventType> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "world_events"), "jstech/world_events",
            WorldEventType.CODEC).synced().register();
    /** The warning of an event to come, sent to those carrying its instrument. */
    public static final NotificationKind WARNING = Notifications.kind(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "world_event_warning"), NotificationLevel.WARNING, KIND)
            .declare();
    /** An event beginning, told to everyone it covers. */
    public static final NotificationKind BEGUN = Notifications.kind(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "world_event_began"), NotificationLevel.INFO, KIND)
            .declare();

    private static final DimensionState<Calendar> CALENDAR = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "world_events"), Calendar.CODEC, Calendar.EMPTY)
            .dimension();
    private static final Map<ResourceLocation, WorldEventType> DECLARED = new LinkedHashMap<>();
    /* How often the dimensions' events are looked at, in ticks. */
    private static final int EVERY = 20;
    private static final long DAY = 24_000L;
    private static final long HOUR = 1_000L;
    /* The hour a day of the game's clock begins at. */
    private static final int MORNING = 6;
    private static final int TICKS_PER_SECOND = 20;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3_600;
    /* How far beyond its reach a coming event is felt by an instrument, in blocks. */
    private static final int FELT_BEYOND = 256;

    private WorldEvents() {
    }

    /** Registers the dimensions' calendars and hands the game's bus the days, once, from the Core's constructor. */
    public static void listen() {
        CoreStates.register(CALENDAR);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % EVERY == 0) {
                tick(level);
            }
        });
        WorldEventCommands.declare();
    }

    /** Starts declaring the kind of event {@code id}, named {@code name}. */
    public static WorldEventBuilder event(final ResourceLocation id, final TextKey name) {
        return new WorldEventBuilder(id, name);
    }

    /** Every kind on {@code level}'s side, by id: the declared ones, then the files, which replace or add. */
    public static Map<ResourceLocation, WorldEventType> all(final Level level) {
        final Map<ResourceLocation, WorldEventType> byId;
        synchronized (DECLARED) {
            byId = new TreeMap<>(DECLARED);
        }
        byId.putAll(FILES.entries(level));
        return byId;
    }

    /** The kind {@code id}, or none. */
    public static Optional<WorldEventType> get(final Level level, final ResourceLocation id) {
        return Optional.ofNullable(all(level).get(id));
    }

    /** The events of {@code level} planned or under way, the soonest first. */
    public static List<WorldEvent> planned(final ServerLevel level) {
        final List<WorldEvent> events = new ArrayList<>(CALENDAR.get(level).events());
        events.sort((a, b) -> Long.compare(a.startsAt(), b.startsAt()));
        return events;
    }

    /** The events under way in {@code level}. */
    public static List<WorldEvent> active(final ServerLevel level) {
        return planned(level).stream().filter(event -> event.activeAt(level.getGameTime())).toList();
    }

    /** Whether an event of the kind {@code id} is under way in {@code level} at {@code pos}. */
    public static boolean activeAt(final ServerLevel level, final ResourceLocation id, final BlockPos pos) {
        final Optional<WorldEventType> type = get(level, id);
        return type.isPresent() && active(level).stream().anyMatch(event -> event.type().equals(id)
                && event.covers(pos, type.get().radius()));
    }

    /**
     * Plans an event of the kind {@code id} in {@code level} to begin {@code inTicks} from now, round {@code centre}
     * for one that reaches only so far (a player's place, picked as it would be, when none is given), lasting its
     * kind's time.
     *
     * @return whether it was planned: the kind exists, and no event of it is planned or under way there
     */
    public static boolean plan(final ServerLevel level, final ResourceLocation id, final int inTicks,
                               final Optional<BlockPos> centre) {
        final Optional<WorldEventType> type = get(level, id);
        if (type.isEmpty() || planned(level).stream().anyMatch(event -> event.type().equals(id))) {
            return false;
        }
        final Optional<BlockPos> place = type.get().local() ? centre.or(() -> pickPlace(level, type.get()))
                : Optional.empty();
        if (type.get().local() && place.isEmpty()) {
            return false;
        }
        final long startsAt = level.getGameTime() + Math.max(0, inTicks);
        save(level, CALENDAR.get(level).with(new WorldEvent(id, startsAt, startsAt + type.get().duration(), place,
                false, false)));
        return true;
    }

    /**
     * Starts an event of the kind {@code id} in {@code level} now, unwarned, for {@code duration} ticks, or its kind's
     * time when it is 0 or less.
     *
     * @return whether it started
     */
    public static boolean start(final ServerLevel level, final ResourceLocation id, final int duration,
                                final Optional<BlockPos> centre) {
        if (!plan(level, id, 0, centre)) {
            return false;
        }
        if (duration > 0) {
            replace(level, id, event -> event.warn().endingAt(event.startsAt() + duration));
        } else {
            replace(level, id, WorldEvent::warn);
        }
        tick(level);
        return true;
    }

    /**
     * Puts the planned event of the kind {@code id} in {@code level} off by {@code ticks}, warned of again as it nears.
     *
     * @return whether there was one planned, not yet begun
     */
    public static boolean delay(final ServerLevel level, final ResourceLocation id, final int ticks) {
        final Optional<WorldEvent> event = planned(level).stream().filter(one -> one.type().equals(id)).findFirst();
        if (event.isEmpty() || event.get().started()) {
            return false;
        }
        final long by = Math.max(0, ticks);
        replace(level, id, one -> new WorldEvent(one.type(), one.startsAt() + by, one.endsAt() + by, one.centre(),
                false, false));
        return true;
    }

    /**
     * Ends the event of the kind {@code id} in {@code level} now, planned or under way.
     *
     * @return whether there was one
     */
    public static boolean stop(final ServerLevel level, final ResourceLocation id) {
        final Optional<WorldEvent> event = planned(level).stream().filter(one -> one.type().equals(id)).findFirst();
        if (event.isEmpty()) {
            return false;
        }
        save(level, CALENDAR.get(level).without(id));
        if (event.get().started()) {
            get(level, id).ifPresent(type -> NeoForge.EVENT_BUS.post(new WorldEventStage.Ended(level, event.get(),
                    type)));
        }
        return true;
    }

    /**
     * {@code ticks} of the world's time as a clock shows a wait, {@code m:ss}, or {@code h:mm:ss} past an hour, a part
     * of a second counted whole, so a wait not over never reads as none.
     */
    public static String clock(final long ticks) {
        final long seconds = (Math.max(0, ticks) + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        final long hours = seconds / SECONDS_PER_HOUR;
        final long minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE;
        final long rest = seconds % SECONDS_PER_MINUTE;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
                : String.format(Locale.ROOT, "%d:%02d", minutes, rest);
    }

    /* Rolls each kind once a day, then warns, begins, plays and ends what is planned. */
    private static void tick(final ServerLevel level) {
        final long now = level.getGameTime();
        final Map<ResourceLocation, WorldEventType> kinds = all(level);
        if (WorldEventBalance.enabled()) {
            roll(level, kinds);
        }
        for (final WorldEvent event : planned(level)) {
            final WorldEventType type = kinds.get(event.type());
            if (type == null || now >= event.endsAt()) {
                save(level, CALENDAR.get(level).without(event.type()));
                if (type != null && event.started()) {
                    NeoForge.EVENT_BUS.post(new WorldEventStage.Ended(level, event, type));
                }
                continue;
            }
            if (!event.warned() && now >= event.startsAt() - type.warning()) {
                warn(level, event, type);
                replace(level, event.type(), WorldEvent::warn);
                NeoForge.EVENT_BUS.post(new WorldEventStage.Warned(level, event.warn(), type));
            }
            if (!event.started() && now >= event.startsAt()) {
                final WorldEvent begun = event.start();
                replace(level, event.type(), WorldEvent::start);
                if (type.announced()) {
                    tell(level, begun, type);
                }
                NeoForge.EVENT_BUS.post(new WorldEventStage.Started(level, begun, type));
            }
            if (event.activeAt(now)) {
                type.effect().ifPresent(effect -> {
                    for (final ServerPlayer player : level.players()) {
                        if (event.covers(player.blockPosition(), type.radius())) {
                            Effects.at(level, effect, player.position()).send();
                        }
                    }
                });
            }
        }
    }

    /* On the first look of each day, finds each kind due by its schedule or its chance, and plans it. */
    private static void roll(final ServerLevel level, final Map<ResourceLocation, WorldEventType> kinds) {
        final long day = Math.floorDiv(level.getDayTime(), DAY);
        final RandomSource random = level.getRandom();
        Season season = null;
        for (final Map.Entry<ResourceLocation, WorldEventType> kind : kinds.entrySet()) {
            final WorldEventType type = kind.getValue();
            final String key = kind.getKey().toString();
            final Calendar calendar = CALENDAR.get(level);
            if (!type.happensIn(level.dimension()) || calendar.rolled().getOrDefault(key, Long.MIN_VALUE) >= day) {
                continue;
            }
            save(level, calendar.rolledOn(key, day));
            if (WorldEventBalance.disabled(kind.getKey())) {
                continue;
            }
            if (!type.seasons().isEmpty()) {
                season = season == null ? WorldCalendar.now(level).season() : season;
                if (!type.seasons().contains(season)) {
                    continue;
                }
            }
            if (type.everyDays() > 0) {
                if (day % type.everyDays() == 0) {
                    plan(level, kind.getKey(), (int) Math.max(type.warning(), untilHour(level, type.atHour())),
                            Optional.empty());
                }
            } else if (random.nextDouble() < type.chancePerDay() * WorldEventBalance.frequency(kind.getKey())) {
                plan(level, kind.getKey(), type.warning() + random.nextInt((int) (DAY / 2)), Optional.empty());
            }
        }
    }

    /* How many ticks until the game's clock next shows {@code hour}. */
    private static long untilHour(final ServerLevel level, final int hour) {
        final long into = Math.floorMod(level.getDayTime(), DAY);
        return Math.floorMod(Math.floorMod((hour - MORNING) * HOUR, DAY) - into, DAY);
    }

    /* A place round a player standing in the kind's biomes, picked at random among them; none when no one is. */
    private static Optional<BlockPos> pickPlace(final ServerLevel level, final WorldEventType type) {
        final List<ServerPlayer> standing = new ArrayList<>();
        for (final ServerPlayer player : level.players()) {
            final Optional<TagKey<Biome>> biomes = type.biomes();
            if (biomes.isEmpty() || level.getBiome(player.blockPosition()).is(biomes.get())) {
                standing.add(player);
            }
        }
        return standing.isEmpty() ? Optional.empty()
                : Optional.of(standing.get(level.getRandom().nextInt(standing.size())).blockPosition());
    }

    /* Warns those carrying the kind's instrument within its reach, and a little beyond. */
    private static void warn(final ServerLevel level, final WorldEvent event, final WorldEventType type) {
        if (type.instrument().isEmpty()) {
            return;
        }
        final TagKey<Item> instrument = type.instrument().get();
        final String left = clock(event.startsAt() - level.getGameTime());
        for (final ServerPlayer player : level.players()) {
            if (event.covers(player.blockPosition(), type.radius() + (type.local() ? FELT_BEYOND : 0))
                    && player.getInventory().contains(instrument)) {
                Notifications.send(player, WARNING.of(type.title().text(), EXPECTED.with(left)));
            }
        }
    }

    /* Tells everyone the event covers that it has begun. */
    private static void tell(final ServerLevel level, final WorldEvent event, final WorldEventType type) {
        for (final ServerPlayer player : level.players()) {
            if (event.covers(player.blockPosition(), type.radius())) {
                Notifications.send(player, BEGUN.of(type.title().text(), BEGAN.text()));
            }
        }
    }

    private static void replace(final ServerLevel level, final ResourceLocation id,
                                final UnaryOperator<WorldEvent> change) {
        save(level, CALENDAR.get(level).changed(id, change));
    }

    private static void save(final ServerLevel level, final Calendar calendar) {
        CALENDAR.set(level, calendar);
    }

    /**
     * What a dimension has planned and under way, and the day each kind was last rolled for.
     *
     * @param events the events planned or under way, one of each kind at most
     * @param rolled the last day each kind was rolled for, by its id
     */
    public record Calendar(List<WorldEvent> events, Map<String, Long> rolled) {

        public static final Codec<Calendar> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                WorldEvent.CODEC.listOf().fieldOf("events").forGetter(Calendar::events),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("rolled").forGetter(Calendar::rolled))
                .apply(instance, Calendar::new));
        static final Calendar EMPTY = new Calendar(List.of(), Map.of());

        public Calendar {
            events = List.copyOf(events);
            rolled = Map.copyOf(rolled);
        }

        Calendar with(final WorldEvent event) {
            final List<WorldEvent> next = new ArrayList<>(this.events);
            next.add(event);
            return new Calendar(next, this.rolled);
        }

        Calendar without(final ResourceLocation id) {
            final List<WorldEvent> next = new ArrayList<>(this.events);
            next.removeIf(event -> event.type().equals(id));
            return new Calendar(next, this.rolled);
        }

        Calendar changed(final ResourceLocation id, final UnaryOperator<WorldEvent> change) {
            final List<WorldEvent> next = new ArrayList<>();
            for (final WorldEvent event : this.events) {
                next.add(event.type().equals(id) ? change.apply(event) : event);
            }
            return new Calendar(next, this.rolled);
        }

        Calendar rolledOn(final String id, final long day) {
            final Map<String, Long> next = new HashMap<>(this.rolled);
            next.put(id, day);
            return new Calendar(this.events, next);
        }
    }

    /** A kind of event being declared; it starts by chance, never, over the whole of every dimension. */
    public static final class WorldEventBuilder {

        private final ResourceLocation id;
        private final TextKey name;
        private final List<ResourceKey<Level>> dimensions = new ArrayList<>();
        private final List<Season> seasons = new ArrayList<>();
        private Optional<TagKey<Biome>> biomes = Optional.empty();
        private double chance;
        private int everyDays;
        private int atHour;
        private int warning;
        private Optional<TagKey<Item>> instrument = Optional.empty();
        private int duration = (int) DAY / 2;
        private int radius;
        private boolean announced = true;
        private Optional<ResourceLocation> effect = Optional.empty();

        private WorldEventBuilder(final ResourceLocation id, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
        }

        /** It happens in {@code dimension}; add more for several. */
        public WorldEventBuilder in(final ResourceKey<Level> dimension) {
            this.dimensions.add(dimension);
            return this;
        }

        /** It happens in {@code season}; add more for several. */
        public WorldEventBuilder in(final Season season) {
            this.seasons.add(season);
            return this;
        }

        /** It happens round a place in these biomes. */
        public WorldEventBuilder biomes(final TagKey<Biome> where) {
            this.biomes = Optional.of(where);
            return this;
        }

        /** It happens by chance, this likely on any day, 0 to 1. */
        public WorldEventBuilder chance(final double perDay) {
            this.chance = perDay;
            return this;
        }

        /** It happens every {@code days} days, beginning at {@code hour} of the game's clock. */
        public WorldEventBuilder every(final int days, final int hour) {
            this.everyDays = days;
            this.atHour = hour;
            return this;
        }

        /** Those carrying its instrument are warned {@code ticks} before it begins. */
        public WorldEventBuilder warning(final int ticks) {
            this.warning = ticks;
            return this;
        }

        /** What warns of it, carried anywhere in the inventory. */
        public WorldEventBuilder instrument(final TagKey<Item> items) {
            this.instrument = Optional.of(items);
            return this;
        }

        /** It lasts {@code ticks}. */
        public WorldEventBuilder lasts(final int ticks) {
            this.duration = ticks;
            return this;
        }

        /** It reaches {@code blocks} round its place, rather than the whole dimension. */
        public WorldEventBuilder radius(final int blocks) {
            this.radius = blocks;
            return this;
        }

        /** No one is told as it begins: only its instrument warns of it. */
        public WorldEventBuilder quiet() {
            this.announced = false;
            return this;
        }

        /** The effect played round each player it covers while it lasts. */
        public WorldEventBuilder effect(final ResourceLocation played) {
            this.effect = Optional.of(played);
            return this;
        }

        /** Declares the kind, and gives its id. */
        public ResourceLocation declare() {
            synchronized (DECLARED) {
                DECLARED.put(id, new WorldEventType(name.key(), name.english(), dimensions, biomes, seasons, chance,
                        everyDays, atHour, warning, instrument, duration, radius, announced, effect));
            }
            return id;
        }
    }
}
