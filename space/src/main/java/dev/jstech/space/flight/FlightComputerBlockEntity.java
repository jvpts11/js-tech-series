/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.air.AirMath;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.cruise.CruiseEvents;
import dev.jstech.space.cruise.CruiseFlight;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.platform.LandingPlatforms;
import dev.jstech.space.rocket.BoilOff;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.RocketStaging;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.StageFate;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
    private final ValueField<Map<Integer, TankLoad>> tanks;
    private final IntField clock;
    private final LongField liftOff;
    private final ValueField<CabinReadings> readings;
    private final IntField stage;
    private final ValueField<Map<Integer, StageChoice>> choices;
    private final ValueField<Map<Integer, ShownPlatform>> shown;
    private final IntField separated;
    private final ValueField<TransferChoice> choice;
    private final ValueField<PlannedTransfer> executed;
    private final ValueField<Map<Integer, Integer>> lost;
    private final BoolField cruising;
    private final BoolField hazards;
    private final ValueField<PlannedLanding> landing;
    private final IntField guidance;
    private int wait;
    /* Ticks till its rocket's lost blocks are counted again, and the plan they are counted by, for its design. */
    private int countWait;
    private @Nullable BuildPlan counted;
    private @Nullable RocketDesign countedFor;

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
     * The guidance a rocket comes down by: the flight computer's own, or an instrument ring's guidance platform, which
     * lands it nearer the spot marked.
     */
    public static final int COMPUTER_GUIDANCE = 1;
    public static final int PLATFORM_GUIDANCE = 2;
    /*
     * The ellipse a rocket comes down in round the spot marked, in blocks, length and width, by its guidance: a
     * guidance platform's the size Perseverance's had, 7.7 by 6.6 kilometres, the computer alone's twice that.
     * Estimates, to be set by playing.
     */
    private static final int[][] ELLIPSES = {{15_400, 13_200}, {7_700, 6_600}};
    /*
     * The landing page's keys: the zoom in and out, the mark and its clearing; the hazards, the grid and setting the
     * landing on the right.
     */
    private static final int ZOOM_IN = LEFT;
    private static final int ZOOM_OUT = LEFT + 1;
    private static final int MARK = LEFT + 2;
    private static final int CLEAR = LEFT + 3;
    private static final int HAZARD = RIGHT + 1;
    private static final int GRID = RIGHT + 2;
    private static final int SET = RIGHT + 4;
    /* The stages page's keys on the right: anywhere, a fall zone, a platform, the next platform, the next radius. */
    private static final int ANY = RIGHT;
    private static final int ZONE = RIGHT + 1;
    private static final int PLATFORM = RIGHT + 2;
    private static final int NEXT_PLATFORM = RIGHT + 3;
    private static final int NEXT_RADIUS = RIGHT + 4;
    /* The transfer page's keys on the right: more delta-v, less, the gravity assist, aerobraking, and to set it. */
    private static final int MORE_DELTA_V = RIGHT;
    private static final int LESS_DELTA_V = RIGHT + 1;
    private static final int ASSIST = RIGHT + 2;
    private static final int AEROBRAKE = RIGHT + 3;
    private static final int EXECUTE = RIGHT + 4;
    /* The delta-v the climb from each world's ground to its low orbit takes, before a transfer. */
    private static final Map<SolarBody, Double> CLIMBS = Map.of(SolarBody.OVERWORLD, Rocketry.SURFACE_TO_LOW_ORBIT,
            SolarBody.MOON, Rocketry.MOON_ORBIT_TO_SURFACE, SolarBody.MARS, Rocketry.MARS_SURFACE_TO_ORBIT);
    /** The lift-off of a rocket that has not left the ground ({@link #liftOff}). */
    public static final long NOT_LIFTED = -1;
    /* A tank's place in the stack, written as its number. */
    private static final Codec<Integer> SECTION = Codec.STRING.xmap(Integer::parseInt, String::valueOf);
    /* Ticks in a second; between reading the flight for the cabins, and between the vapour off the tanks. */
    private static final int SECOND = 20;
    private static final int READ_EVERY = 5;
    private static final int VAPOUR_EVERY = 20;

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
        // What its rocket's tanks hold while it stands on the ground, by their places in its stack, from the bottom.
        this.tanks = fields().value("Tanks", Codec.unboundedMap(SECTION, TankLoad.CODEC), Map.<Integer, TankLoad>of())
                .save().toClient();
        // The mission's clock as the launch panel counts it, in seconds: below nought, counting down to lift-off.
        this.clock = fields().integer("Clock", 0).save().toClient();
        // When its rocket left the ground, the game's tick; and what its cabins read of the flight since.
        this.liftOff = fields().longInteger("LiftOff", NOT_LIFTED).save();
        this.readings = fields().nullable("Readings", CabinReadings.CODEC).toClient();
        // Its stages page: the stage chosen, where each stage is set to go and the platforms set as its players are
        // shown them; and how many stages its rocket has let go in flight.
        this.stage = fields().integer("Stage", 1).save().toClient();
        this.choices = fields().value("Choices", Codec.unboundedMap(SECTION, StageChoice.CODEC),
                Map.<Integer, StageChoice>of()).save().toClient();
        this.shown = fields().value("Shown", Codec.unboundedMap(SECTION, ShownPlatform.CODEC),
                Map.<Integer, ShownPlatform>of()).toClient();
        this.separated = fields().integer("Separated", 0).save();
        // How the transfer is planned on its transfer page, and the plan it is set to fly.
        this.choice = fields().value("Choice", TransferChoice.CODEC, TransferChoice.CHEAPEST).save().toClient();
        this.executed = fields().nullable("Executed", PlannedTransfer.CODEC).save().toClient();
        // How many blocks each part of its rocket has lost in flight, by the part's place in the stack.
        this.lost = fields().value("Lost", Codec.unboundedMap(SECTION, Codec.INT), Map.<Integer, Integer>of()).save()
                .toClient();
        // Whether its rocket is on its cruise to the world its plan goes to.
        this.cruising = fields().flag("Cruising", false).save().toClient();
        // Its landing page: whether the hazards are shown, the landing set, and the guidance its rocket comes down by.
        this.hazards = fields().flag("Hazards", true).save().toClient();
        this.landing = fields().nullable("Landing", PlannedLanding.CODEC).save().toClient();
        this.guidance = fields().integer("Guidance", COMPUTER_GUIDANCE).toClient();
    }

    /** How long the ellipse its rocket comes down in is by {@code guidance}, in blocks. */
    public static int ellipseLength(final int guidance) {
        return ELLIPSES[Math.clamp(guidance - 1, 0, ELLIPSES.length - 1)][0];
    }

    /** How wide the ellipse its rocket comes down in is by {@code guidance}, in blocks. */
    public static int ellipseWidth(final int guidance) {
        return ELLIPSES[Math.clamp(guidance - 1, 0, ELLIPSES.length - 1)][1];
    }

    /**
     * On the server, a few times a second: aboard its rocket climbing, it reads the flight for the cabins; on the
     * ground, its rocket's loaded tanks give off their vapour.
     */
    public void tick() {
        if (!(this.level instanceof ServerLevel server) || --this.wait > 0) {
            return;
        }
        if (!YardLayout.isYardBlock(this.worldPosition.getX())) {
            this.wait = VAPOUR_EVERY;
            BoilOff.rise(server, this);
            showPlatforms(server);
            followGuidance(server);
            return;
        }
        this.wait = READ_EVERY;
        this.countWait -= READ_EVERY;
        if (this.countWait <= 0 && MovingStructures.at(server, this.worldPosition) instanceof MovingStructure carried) {
            this.countWait = SECOND;
            countLost(server, carried);
            followGuidance(server);
            followTanks(server, carried);
            CruiseEvents.leak(server, formedPlaces(server));
            CruiseFlight.second(server, carried, this);
        }
        if (this.liftOff.get() == NOT_LIFTED) {
            return;
        }
        final int seconds = (int) ((server.getGameTime() - this.liftOff.get()) / SECOND);
        clock(seconds);
        final CabinReadings was = this.readings.get();
        if (MovingStructures.at(server, this.worldPosition) instanceof MovingStructure body
                && body.motion() instanceof RocketAscent ascent) {
            final Optional<Room> room = SealedRooms.roomBeside(server, this.worldPosition);
            final Climate air = room.map(found -> found.air().climate()).orElseGet(() ->
                    SealedRooms.placeClimate(server, Vec3.atCenterOf(this.worldPosition)));
            this.readings.set(ascent.readings(server, body, seconds, (float) (air.pressure() / AirMath.SEA_LEVEL),
                    room.isPresent()));
            // Each stage burnt out below the one burning is let go.
            RocketStaging.letGo(server, body, this, ascent);
        } else if (was != null) {
            this.readings.set(was.clocked(seconds));
        }
    }

    /** What its cabins read of its rocket's flight, once it has left the ground. */
    public Optional<CabinReadings> readings() {
        return Optional.ofNullable(this.readings.get());
    }

    /**
     * Its rocket leaves the ground at the game's tick {@code gameTime}, its clock counting from it; or, at
     * {@link #NOT_LIFTED}, it has not.
     */
    public void liftOff(final long gameTime) {
        this.liftOff.set(gameTime);
        setChanged();
    }

    /** The mission's clock as the launch panel counts it, in seconds: below nought, counting down to lift-off. */
    public int clock() {
        return this.clock.get();
    }

    /** Sets the mission's clock to {@code seconds}, as the launch panel counts. */
    public void clock(final int seconds) {
        if (this.clock.get() != seconds) {
            this.clock.set(seconds);
            setChanged();
        }
    }

    /** What its rocket's tanks hold while it stands on the ground, by their places in its stack from the bottom. */
    public Map<Integer, TankLoad> tanks() {
        final Map<Integer, TankLoad> held = this.tanks.get();
        return held == null ? Map.of() : held;
    }

    /** Keeps {@code load} as what its rocket's tank at {@code section} holds. */
    public void load(final int section, final TankLoad load) {
        final Map<Integer, TankLoad> held = new TreeMap<>(tanks());
        held.put(section, load);
        this.tanks.set(Map.copyOf(held));
        setChanged();
    }

    /**
     * Takes the plan of the rocket it is in: {@code design}, its foot {@code foot} from here, {@code parts} formed; its
     * stages set to go nowhere yet.
     */
    public void carry(final RocketDesign design, final BlockPos foot, final List<Integer> parts) {
        this.rocket.set(design);
        this.foot.set(foot.immutable());
        this.formed.set(List.copyOf(parts));
        this.stage.set(1);
        this.choices.set(Map.of());
        this.shown.set(Map.of());
        this.separated.set(0);
        setChanged();
    }

    /** How many stages its rocket has; none before it keeps a rocket's plan. */
    public int stages() {
        final Optional<RocketDesign> design = rocket();
        return this.level == null || design.isEmpty() ? 0
                : RocketStages.stages(RocketParts.all(this.level), design.get().stack()).size();
    }

    /** The stage its stages page shows, numbered from one at the bottom. */
    public int stage() {
        return Math.max(1, this.stage.get());
    }

    /** Where each stage of its rocket is set to go, by the stage's number; a stage not in it falls anywhere. */
    public Map<Integer, StageChoice> choices() {
        final Map<Integer, StageChoice> set = this.choices.get();
        return set == null ? Map.of() : set;
    }

    /** The platforms its stages are set to land on, as its players are shown them, by the stage's number. */
    public Map<Integer, ShownPlatform> shownPlatforms() {
        final Map<Integer, ShownPlatform> set = this.shown.get();
        return set == null ? Map.of() : set;
    }

    /** Where each stage of its rocket goes once it flies from {@code dimension}, standing at {@code at}. */
    public Map<Integer, StageFate> fates(final ResourceKey<Level> dimension, final BlockPos at) {
        final Map<Integer, StageFate> out = new TreeMap<>();
        choices().forEach((stageNumber, choice) -> out.put(stageNumber, choice.fate(dimension, at)));
        return Map.copyOf(out);
    }

    /** How many of its rocket's stages it has let go in flight, from the bottom. */
    public int separated() {
        return this.separated.get();
    }

    /**
     * Its rocket has let go its stage {@code stageNumber}, whose places in the stack are {@code sections}: it draws
     * them no more.
     */
    public void letGo(final int stageNumber, final Collection<Integer> sections) {
        final List<Integer> left = new ArrayList<>(formed());
        left.removeAll(sections);
        this.formed.set(List.copyOf(left));
        this.separated.set(Math.max(this.separated.get(), stageNumber));
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

    /** Whether its landing map shows the hazards: the ground too steep to land on. */
    public boolean hazards() {
        return this.hazards.get();
    }

    /** The landing set for its rocket's descent, once its SET key has set one where the spot is marked. */
    public Optional<PlannedLanding> landing() {
        return Optional.ofNullable(this.landing.get());
    }

    /** The guidance its rocket comes down by: {@link #COMPUTER_GUIDANCE} or {@link #PLATFORM_GUIDANCE}. */
    public int guidance() {
        return this.guidance.get();
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

    /** How the transfer is planned on its transfer page. */
    public TransferChoice choice() {
        final TransferChoice set = this.choice.get();
        return set == null ? TransferChoice.CHEAPEST : set;
    }

    /** The transfer planned now, as its transfer page plans it, if it plans one from here. */
    public Optional<PlannedTransfer> planned() {
        return this.level == null ? Optional.empty() : plan().map(found -> PlannedTransfer.of(found, choice(),
                shield(), this.level.getGameTime()));
    }

    /** The transfer it is set to fly, once its EXEC key has set one. */
    public Optional<PlannedTransfer> executed() {
        return Optional.ofNullable(this.executed.get());
    }

    /** Whether its rocket carries a heat shield, its capsule's, to aerobrake with: one not holed. */
    public boolean shield() {
        final Optional<RocketDesign> design = rocket();
        if (this.level == null || design.isEmpty()) {
            return false;
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(this.level);
        final List<RocketStack.Section> sections = design.get().stack().sections();
        for (int section = 0; section < sections.size(); section++) {
            final RocketPart part = parts.get(sections.get(section).part().id());
            if (part != null && part.kind() == RocketPart.Kind.CAPSULE && lost(section) == 0) {
                return true;
            }
        }
        return false;
    }

    /** Sets {@code plan} for its rocket to fly: it sets out once its window comes, in a world's orbit. */
    public void execute(final PlannedTransfer plan) {
        this.executed.set(plan);
        setChanged();
    }

    /** Whether its rocket is on its cruise to the world its plan goes to. */
    public boolean cruising() {
        return this.cruising.get();
    }

    /** Its rocket set out on its cruise, or no longer cruises. */
    public void cruising(final boolean now) {
        this.cruising.set(now);
        setChanged();
    }

    /** Its rocket arrived in the orbit its plan went to: the plan is flown. */
    public void arrived() {
        this.cruising.set(false);
        this.executed.set(null);
        setChanged();
    }

    /**
     * Where the merged blocks of its rocket's formed parts stand now, in its yard, those of its stages let go aside;
     * none before it keeps a rocket's plan.
     */
    public List<BlockPos> formedPlaces(final ServerLevel server) {
        final List<BlockPos> out = new ArrayList<>();
        final Optional<BlockPos> stands = foot();
        if (stands.isEmpty() || plan(server) == null) {
            return out;
        }
        final Set<Integer> gone = separatedSections(server);
        final BlockPos base = this.worldPosition.offset(stands.get());
        for (final PlanSpot spot : this.counted.spots()) {
            final BlockPos at = base.offset(spot.at());
            if (spot.place().isPresent() && !gone.contains(spot.part())
                    && server.getBlockState(at).getBlock() instanceof FormedPartBlock) {
                out.add(at);
            }
        }
        return out;
    }

    /** How many blocks its rocket's part at {@code section} has lost, counted in flight. */
    public int lost(final int section) {
        final Map<Integer, Integer> kept = this.lost.get();
        return kept == null ? 0 : kept.getOrDefault(section, 0);
    }

    /**
     * The delta-v its rocket has for a transfer, in metres a second: what the stages it has not let go can give in a
     * vacuum with its tanks as they hold, less, on the ground, the climb to the low orbit; none before it keeps a
     * rocket's plan.
     */
    public OptionalDouble available() {
        final Optional<RocketDesign> design = rocket();
        if (this.level == null || design.isEmpty()) {
            return OptionalDouble.empty();
        }
        final Map<Integer, Integer> holed = this.lost.get();
        final Optional<List<StageMath.Burn>> burns = RocketStages.burns(RocketParts.all(this.level),
                design.get().stack(), Propellants.all(this.level), tanks(), holed == null ? Set.of() : holed.keySet());
        if (burns.isEmpty()) {
            return OptionalDouble.empty();
        }
        final int gone = separated();
        double left = StageMath.deltaV(burns.get().stream().filter(burn -> burn.stage() > gone).toList(), 0);
        if (!YardLayout.isYardBlock(this.worldPosition.getX())) {
            left -= here(this.level).map(body -> CLIMBS.getOrDefault(body, 0.0)).orElse(0.0);
        }
        return OptionalDouble.of(Math.max(0, left));
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
                this.landing.set(null);
                this.executed.set(null);
            } else if (key - LEFT == worlds.size()) {
                this.choice.set(choice().windowBy(-1));
            } else if (key - LEFT == worlds.size() + 1) {
                this.choice.set(choice().windowBy(1));
            }
        } else if (page() == FlightPage.TRANSFER && key >= RIGHT && key < RIGHT + KEYS) {
            final TransferChoice now = choice();
            switch (key) {
                case MORE_DELTA_V -> this.choice.set(now.spendBy(now.spend() == TransferChoice.SPENDS.length - 1
                        ? -now.spend() : 1));
                case LESS_DELTA_V -> this.choice.set(now.spendBy(now.spend() == 0
                        ? TransferChoice.SPENDS.length - 1 : -1));
                case ASSIST -> this.choice.set(now.assisted(!now.assist()));
                case AEROBRAKE -> this.choice.set(now.aerobraking(!now.aero()));
                // Set to fly; with the server's fast travel, it skips its cruise.
                case EXECUTE -> planned().ifPresent(plan -> execute(SpaceServerConfig.fastTravel() ? plan.atOnce()
                        : plan));
                default -> {
                    // A key the transfer page does not use.
                }
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
                case CLEAR -> {
                    this.mark.set(null);
                    this.landing.set(null);
                }
                case HAZARD -> this.hazards.set(!hazards());
                case GRID -> this.grid.set(!grid());
                // The landing set where the spot is marked, for the descent.
                case SET -> {
                    final BlockPos marked = mark();
                    if (marked != null) {
                        this.landing.set(new PlannedLanding(target(), marked.getX(), marked.getZ()));
                    }
                }
                default -> {
                    // A key the landing page does not use: its guidance only shows which guides the rocket.
                }
            }
        }
        setChanged();
    }

    /**
     * Its key {@code key} is pressed by {@code by}: on the stages page a stage is chosen on the left and where it goes
     * set on the right, a platform only one {@code by} may use; any other key as {@link #press(int)} has it.
     */
    public void press(final int key, @Nullable final ServerPlayer by) {
        if (page() != FlightPage.STAGES || key < LEFT || !(this.level instanceof ServerLevel server)) {
            press(key);
            return;
        }
        final int count = stages();
        if (key < LEFT + KEYS) {
            // The top stage holds this computer and goes with it: it has nowhere to be set to go.
            if (key - LEFT + 1 < count) {
                this.stage.set(key - LEFT + 1);
            }
        } else if (stage() < count) {
            final int chosen = stage();
            final Map<Integer, StageChoice> set = new TreeMap<>(choices());
            final StageChoice now = set.get(chosen);
            switch (key) {
                case ANY -> set.remove(chosen);
                case ZONE -> set.put(chosen, StageChoice.zone(StageChoice.FIRST_RADIUS));
                case NEXT_RADIUS -> {
                    if (now != null && !now.lands()) {
                        set.put(chosen, now.nextRadius());
                    }
                }
                case PLATFORM, NEXT_PLATFORM -> nextPlatform(server, by, chosen,
                        key == NEXT_PLATFORM && now != null ? now.platform() : Optional.empty())
                        .ifPresent(platform -> set.put(chosen, StageChoice.landing(platform)));
                default -> {
                    // A key the stages page does not use.
                }
            }
            this.choices.set(Map.copyOf(set));
            showPlatforms(server);
        }
        setChanged();
    }

    /** Marks the spot at {@code x}, {@code z} of its target world to land on. */
    public void markAt(final int x, final int z) {
        this.mark.set(new BlockPos(x, 0, z));
        final PlannedLanding set = this.landing.get();
        if (set != null && (set.x() != x || set.z() != z)) {
            // A spot marked anew is not where the landing was set: it is set again there, or not at all.
            this.landing.set(null);
        }
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

    /*
     * The platform after {@code after} for the stage {@code stageNumber}, or the nearest with none: one of this world
     * {@code by} may use, wide enough for the stage; none for a stage that cannot come back.
     */
    private Optional<UUID> nextPlatform(final ServerLevel server, @Nullable final ServerPlayer by,
                                        final int stageNumber, final Optional<UUID> after) {
        final Optional<RocketDesign> design = rocket();
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(server);
        if (design.isEmpty() || !RocketStages.canComeBack(parts, design.get().stack(), stageNumber)) {
            return Optional.empty();
        }
        final int wide = RocketStages.diameter(parts, design.get().stack(), stageNumber);
        final List<UUID> fit = new ArrayList<>();
        for (final Map.Entry<UUID, LandingPlatforms.Entry> platform : numbered(server)) {
            if (platform.getValue().takes(wide) && (by == null || platform.getValue().ownership().mayUse(by))) {
                fit.add(platform.getKey());
            }
        }
        if (fit.isEmpty()) {
            return Optional.empty();
        }
        final int at = after.map(fit::indexOf).orElse(-1);
        return Optional.of(fit.get((at + 1) % fit.size()));
    }

    /*
     * Counts the blocks its rocket {@code body} has lost, by the part of its stack each was of, its stages let go
     * aside: a block of a formed part not standing in its place any more. The count goes to its players and to the
     * rocket's record when it changes, holed tanks leaking and holed engines giving no thrust till they are mended.
     */
    private void countLost(final ServerLevel server, final MovingStructure body) {
        final Optional<BlockPos> stands = foot();
        if (stands.isEmpty() || plan(server) == null) {
            return;
        }
        final Set<Integer> gone = separatedSections(server);
        final BlockPos base = this.worldPosition.offset(stands.get());
        final Map<Integer, Integer> now = new TreeMap<>();
        for (final PlanSpot spot : this.counted.spots()) {
            if (spot.place().isEmpty() || gone.contains(spot.part())) {
                continue;
            }
            final BlockPos at = base.offset(spot.at());
            final BlockState there = FormedPartBlock.originalAt(server, at, server.getBlockState(at));
            if (!BuiltInRegistries.BLOCK.getKey(there.getBlock()).equals(spot.block())) {
                now.merge(spot.part(), 1, Integer::sum);
            }
        }
        final Map<Integer, Integer> counted = Map.copyOf(now);
        if (!counted.equals(this.lost.get())) {
            this.lost.set(counted);
            Rockets.lost(server.getServer(), body.id(), counted);
            setChanged();
        }
    }

    /* The plan its rocket was built by, made once for its design; none before it keeps a rocket's plan. */
    private @Nullable BuildPlan plan(final ServerLevel server) {
        final Optional<RocketDesign> design = rocket();
        if (design.isEmpty()) {
            return null;
        }
        if (this.counted == null || !design.get().equals(this.countedFor)) {
            this.counted = RocketPlans.of(server, design.get().stack());
            this.countedFor = design.get();
        }
        return this.counted;
    }

    /* The places in its rocket's stack of the parts its stages let go carried away. */
    private Set<Integer> separatedSections(final ServerLevel server) {
        final Set<Integer> gone = new HashSet<>();
        final Optional<RocketDesign> design = rocket();
        if (design.isPresent()) {
            final Map<ResourceLocation, RocketPart> parts = RocketParts.all(server);
            for (int stage = 1; stage <= separated(); stage++) {
                gone.addAll(RocketStages.stage(parts, design.get().stack(), stage));
            }
        }
        return gone;
    }

    /*
     * Keeps the guidance its rocket comes down by, for its landing page: an instrument ring's guidance platform, formed
     * in its place, its stage not let go; else the computer's own.
     */
    private void followGuidance(final ServerLevel server) {
        int now = COMPUTER_GUIDANCE;
        final Optional<BlockPos> stands = foot();
        if (stands.isPresent() && plan(server) != null) {
            final Set<Integer> gone = separatedSections(server);
            final BlockPos base = this.worldPosition.offset(stands.get());
            for (final PlanSpot spot : this.counted.spots()) {
                if (spot.block().equals(RocketKitBlocks.GUIDANCE_PLATFORM.getId()) && !gone.contains(spot.part())
                        && server.getBlockState(base.offset(spot.at())).getBlock() instanceof FormedPartBlock) {
                    now = PLATFORM_GUIDANCE;
                    break;
                }
            }
        }
        if (now != guidance()) {
            this.guidance.set(now);
            setChanged();
        }
    }

    /* Keeps what its rocket's tanks hold in flight as its record reckons it, for its pages to read. */
    private void followTanks(final ServerLevel server, final MovingStructure body) {
        Rockets.settle(server.getServer(), body.id()).ifPresent(record -> {
            if (!record.tanks().equals(tanks())) {
                this.tanks.set(record.tanks());
                setChanged();
            }
        });
    }

    /* Shows its players the platforms its stages are set to: each one's number, width, distance and whether free. */
    private void showPlatforms(final ServerLevel server) {
        final Map<Integer, ShownPlatform> out = new TreeMap<>();
        final List<Map.Entry<UUID, LandingPlatforms.Entry>> all = numbered(server);
        choices().forEach((stageNumber, choice) -> choice.platform().ifPresent(id -> {
            for (int k = 0; k < all.size(); k++) {
                final LandingPlatforms.Entry platform = all.get(k).getValue();
                if (all.get(k).getKey().equals(id)) {
                    out.put(stageNumber, new ShownPlatform(k + 1, platform.side(), (int) Math.round(Math.sqrt(
                            platform.terminal().distSqr(this.worldPosition))), platform.free()));
                }
            }
        }));
        final Map<Integer, ShownPlatform> shownNow = Map.copyOf(out);
        if (!shownNow.equals(shownPlatforms())) {
            this.shown.set(shownNow);
        }
    }

    /* The platforms of the world it stands in, the nearest first, numbered so from one. */
    private List<Map.Entry<UUID, LandingPlatforms.Entry>> numbered(final ServerLevel server) {
        final List<Map.Entry<UUID, LandingPlatforms.Entry>> out = new ArrayList<>();
        for (final UUID id : LandingPlatforms.all(server.getServer())) {
            LandingPlatforms.get(server.getServer(), id).filter(platform -> platform.dimension()
                    .equals(server.dimension())).ifPresent(platform -> out.add(Map.entry(id, platform)));
        }
        out.sort(Comparator.comparingDouble(platform -> platform.getValue().terminal().distSqr(this.worldPosition)));
        return out;
    }

    /**
     * A platform a stage is set to land on, as the stages page shows it.
     *
     * @param number   its number among the world's platforms, the nearest first, from one; 0 for one gone
     * @param side     how wide it is, in blocks
     * @param distance how far it is from the rocket, in blocks
     * @param free     whether it holds no stage
     */
    public record ShownPlatform(int number, int side, int distance, boolean free) {

        public static final Codec<ShownPlatform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("number").forGetter(ShownPlatform::number),
                Codec.INT.fieldOf("side").forGetter(ShownPlatform::side),
                Codec.INT.fieldOf("distance").forGetter(ShownPlatform::distance),
                Codec.BOOL.fieldOf("free").forGetter(ShownPlatform::free)
        ).apply(instance, ShownPlatform::new));
    }
}
