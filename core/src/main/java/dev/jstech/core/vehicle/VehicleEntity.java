/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.energy.IEnergyHolder;
import dev.jstech.core.team.Ownership;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * A vehicle in the world, of whichever {@link VehicleDef kind} it was made: seated players and robots ride in it, the
 * one in a driving seat drives it with the walking keys, and it goes as its parts and its physics let it, burning its
 * fuel or drawing on its battery. A player's keys drive it on their own game, which tells the server where it went, as
 * a boat is driven; the server keeps its fuel, its engine's state and the wear of its parts, and tells every player
 * seeing it. Mods may extend it for a vehicle of their own, declared as an entity of their own.
 */
public class VehicleEntity extends Entity implements IEnergyHolder, IAudible {

    private final FittedParts parts = new FittedParts();
    private final VehicleDrive drive = new VehicleDrive();
    private final VehicleMotion motion = new VehicleMotion(this);
    private final VehicleUpkeep upkeep = new VehicleUpkeep(this);
    private final Map<UUID, Integer> seats = new HashMap<>();
    private final Battery battery = new Battery();
    private @Nullable VehicleDef def;
    private @Nullable ResourceLocation defId;
    private @Nullable VehicleBuild build;
    private @Nullable VehicleDef builtWith;
    private int builtFrom = -1;
    private @Nullable VehicleHolds holds;
    private @Nullable Ownership ownership;
    /* Whom its keys are cut for: kept as it is picked up and placed again, which makes a new entity of it. */
    private UUID keyId = UUID.randomUUID();
    private int keyGeneration = 1;
    private @Nullable UUID towedBy;
    private float blows;
    private int shift;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;

    private static final EntityDataAccessor<String> DEF =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PAINT =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> VARIANT =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> PARTS =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> SEATS =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> SWITCHES =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TOWED_BY =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    /* What the instruments read and every player sees: the engine's speed, gear, heat and load, the fuel left. */
    private static final EntityDataAccessor<Float> RPM =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GEAR =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TEMPERATURE =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOAD =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FUEL =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    /* How it leans and steers, for the players who do not drive it. */
    private static final EntityDataAccessor<Float> PITCH =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STEER =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final String SAVED_DEF = "Vehicle";
    private static final String SAVED_PAINT = "Paint";
    private static final String SAVED_VARIANT = "Variant";
    private static final String SAVED_PARTS = "Parts";
    private static final String SAVED_HOLDS = "Holds";
    private static final String SAVED_SWITCHES = "Switches";
    private static final String SAVED_SEATS = "Seats";
    private static final String SAVED_OWNER = "Owner";
    private static final String SAVED_KEY = "KeyGeneration";
    private static final String SAVED_KEY_ID = "KeyId";
    private static final String SAVED_TOWED_BY = "TowedBy";
    private static final String SAVED_DRIVE = "Drive";
    private static final String SAVED_GEAR = "Gear";
    private static final String SAVED_RPM = "Rpm";
    private static final String SAVED_TEMPERATURE = "Temperature";
    private static final String SAVED_BLOWS = "Blows";
    /* How many ticks a step of its place, sent from the server, is spread over for the players watching it. */
    private static final int LERP_STEPS = 10;
    /* What it takes of a player's blows to break it back into its item. */
    private static final float BREAKS_AT = 40;
    private static final double AIR_TEMPERATURE = 20;
    private static final int NO_ENTITY = -1;

    public VehicleEntity(final EntityType<?> type, final Level level) {
        super(type, level);
    }

    /** The kind of vehicle it is, or none while it is not known on this side. */
    public @Nullable VehicleDef def() {
        final String id = entityData.get(DEF);
        final ResourceLocation now = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        if (now == null) {
            return null;
        }
        if (!now.equals(defId) || def == null) {
            defId = now;
            def = Vehicles.get(level(), now).orElse(null);
            if (def != null) {
                refreshDimensions();
            }
        } else {
            // A data pack read again may change what it is: the newest wins.
            def = Vehicles.get(level(), now).orElse(def);
        }
        return def;
    }

    /** The id of the kind of vehicle it is, or none. */
    public Optional<ResourceLocation> defId() {
        return Optional.ofNullable(ResourceLocation.tryParse(entityData.get(DEF)));
    }

    /** Makes it a vehicle of the kind {@code id}, with the parts that kind is made with, as when it is first made. */
    public void make(final ResourceLocation id, final VehicleDef kind) {
        entityData.set(DEF, id.toString());
        def();
        for (final VehicleDef.Place place : kind.places()) {
            place.part().ifPresent(part -> parts.fit(place.name(), part, 1));
        }
        kind.paints().stream().findFirst().ifPresent(paint -> entityData.set(PAINT, paint.id()));
        kind.variants().stream().findFirst().ifPresent(variant -> entityData.set(VARIANT, variant.id()));
        entityData.set(SWITCHES, VehicleSwitches.MADE.bits());
        partsChanged();
    }

    /** It as its parts make it, worked out again whenever they change; none while its kind is not known. */
    public @Nullable VehicleBuild build() {
        final VehicleDef kind = def();
        if (kind == null) {
            return null;
        }
        if (build == null || builtFrom != parts.version() || builtWith != kind) {
            build = VehicleBuild.of(level(), kind, parts);
            builtFrom = parts.version();
            builtWith = kind;
            holds(kind).fit(level(), build);
        }
        return build;
    }

    /** Its parts. */
    public FittedParts parts() {
        return parts;
    }

    /** What it holds; none while its kind is not known. */
    public Optional<VehicleHolds> holds() {
        final VehicleDef kind = def();
        if (kind == null) {
            return Optional.empty();
        }
        build();
        return Optional.of(holds(kind));
    }

    /** Its switches. */
    public VehicleSwitches switches() {
        return new VehicleSwitches(entityData.get(SWITCHES));
    }

    /** Sets its switches, on the side that drives it; the server tells every player. */
    public void setSwitches(final VehicleSwitches to) {
        if (to.bits() != entityData.get(SWITCHES)) {
            entityData.set(SWITCHES, to.bits());
        }
    }

    /** The paint it wears, by the paint's id. */
    public String paint() {
        return entityData.get(PAINT);
    }

    public void setPaint(final String id) {
        entityData.set(PAINT, id);
    }

    /** The shape it is in, by the variant's id. */
    public String variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(final String id) {
        entityData.set(VARIANT, id);
    }

    /** Whose it is; none for a vehicle nobody placed. */
    public Optional<Ownership> ownership() {
        return Optional.ofNullable(ownership);
    }

    public void setOwnership(final @Nullable Ownership owner) {
        this.ownership = owner;
    }

    /** Whom its keys are cut for, the same however often it is picked up and placed again. */
    public UUID keyId() {
        return keyId;
    }

    /** The generation its keys are cut for; a key of an older one no longer opens it. */
    public int keyGeneration() {
        return keyGeneration;
    }

    /** Forgets every key cut so far: those cut after open it, none before. */
    public void forgetKeys() {
        keyGeneration++;
    }

    /** Its drivetrain. */
    public VehicleDrive drivetrain() {
        return drive;
    }

    /** The vehicle it is hitched behind, or none. */
    public @Nullable VehicleEntity towedBy() {
        if (level() instanceof ServerLevel server) {
            return towedBy == null ? null : server.getEntity(towedBy) instanceof VehicleEntity leader ? leader : null;
        }
        final int id = entityData.get(TOWED_BY);
        return id == NO_ENTITY ? null : level().getEntity(id) instanceof VehicleEntity leader ? leader : null;
    }

    /** Hitches it behind {@code leader}, or lets it go with null. */
    public void hitchTo(final @Nullable VehicleEntity leader) {
        this.towedBy = leader == null ? null : leader.getUUID();
        entityData.set(TOWED_BY, leader == null ? NO_ENTITY : leader.getId());
    }

    /** How fast its engine turns, in turns a minute, as the server last said, or as its own game drives it. */
    public float rpm() {
        return isControlledByLocalInstance() && level().isClientSide() ? (float) motion.lastResult().rpm()
                : entityData.get(RPM);
    }

    /** The gear it is in. */
    public int gear() {
        return isControlledByLocalInstance() && level().isClientSide() ? motion.lastResult().gear()
                : entityData.get(GEAR);
    }

    /** How warm its engine is, in degrees. */
    public float temperature() {
        return entityData.get(TEMPERATURE);
    }

    /** How hard its engine works, from nought to 1. */
    public float load() {
        return isControlledByLocalInstance() && level().isClientSide() ? (float) motion.lastResult().load()
                : entityData.get(LOAD);
    }

    /** How full its fuel is, or its battery for an electric engine, from nought to 1. */
    public float fuel() {
        return entityData.get(FUEL);
    }

    /** How far its nose leans up, in degrees. */
    public float leanPitch() {
        return isControlledByLocalInstance() || !level().isClientSide() ? motion.pitch() : entityData.get(PITCH);
    }

    /** How far it leans to its left, in degrees. */
    public float leanRoll() {
        return isControlledByLocalInstance() || !level().isClientSide() ? motion.roll() : entityData.get(ROLL);
    }

    /** How far its steering is turned, from -1 right to 1 left. */
    public float steering() {
        return isControlledByLocalInstance() || !level().isClientSide() ? motion.steer() : entityData.get(STEER);
    }

    /** How far its wheels have turned, in degrees, for drawing them. */
    public float wheelTurn() {
        return motion.wheelTurn();
    }

    /** How far below it each wheel's ground stands, in metres, in the order of its wheels. */
    public double[] wheelDrops() {
        return motion.drops();
    }

    /** Whether its engine runs: its ignition on, its engine working, and what it runs on left. */
    public boolean running() {
        final VehicleBuild built = build();
        if (built == null || built.engine().isEmpty() || !switches().ignition()) {
            return false;
        }
        if (level().isClientSide()) {
            return fuel() > 0;
        }
        final VehicleHolds held = holds().orElse(null);
        if (held == null) {
            return false;
        }
        return built.electric() ? held.charge() > 0 : held.heldFuel().isPresent();
    }

    /** What its driver asks of it this tick. */
    public DriverInput input() {
        return DriverInputs.of(getControllingPassenger());
    }

    /** Asks a manual gearbox to change up with 1, down with -1, on the side that drives it. */
    public void askShift(final int by) {
        this.shift = Mth.clamp(by, -1, 1);
    }

    /** The seat {@code passenger} sits in, or -1. */
    public int seatOf(final Entity passenger) {
        return seats.getOrDefault(passenger.getUUID(), -1);
    }

    /** Whoever sits in a seat of the role {@code role}. */
    public List<Entity> seatedAs(final ResourceLocation role) {
        final VehicleDef kind = def();
        if (kind == null) {
            return List.of();
        }
        return getPassengers().stream().filter(passenger -> {
            final int seat = seatOf(passenger);
            return seat >= 0 && seat < kind.seats().size() && kind.seats().get(seat).role().equals(role);
        }).toList();
    }

    /** Moves {@code passenger} to the next free seat its rights let it sit in; whether it moved. */
    public boolean nextSeat(final Entity passenger) {
        final VehicleDef kind = def();
        final int from = seatOf(passenger);
        if (kind == null || from < 0) {
            return false;
        }
        for (int step = 1; step < kind.seats().size(); step++) {
            final int seat = (from + step) % kind.seats().size();
            if (!seats.containsValue(seat) && VehicleAccess.maySit(this, kind, passenger, seat)) {
                seats.put(passenger.getUUID(), seat);
                seatsChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public IEnergyStorage energy() {
        return battery;
    }

    @Override
    public void tick() {
        super.tick();
        final VehicleDef kind = def();
        if (kind == null) {
            return;
        }
        final VehicleBuild built = build();
        if (built == null) {
            return;
        }
        final VehicleEntity leader = towedBy();
        // A trailer behind what this game drives follows it here at once, not as the server last had it.
        final boolean followsHere = leader != null && (!level().isClientSide() || leader.isControlledByLocalInstance());
        if (!followsHere) {
            tickLerp();
        }
        if (followsHere) {
            VehicleHitch.follow(this, kind, leader);
        } else if (leader == null && isControlledByLocalInstance()) {
            final VehicleDrive.Result result = motion.step(kind, built, input(), running());
            if (!level().isClientSide()) {
                upkeep.burn(built, result);
            }
        }
        if (level() instanceof ServerLevel server) {
            upkeep.tick(server, kind, built, motion);
        }
    }

    @Override
    public void lerpTo(final double x, final double y, final double z, final float yRot, final float xRot,
                       final int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpSteps = LERP_STEPS;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? lerpYRot : getYRot();
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        final VehicleDef kind = def();
        if (kind == null) {
            return null;
        }
        for (final Entity passenger : getPassengers()) {
            final int seat = seatOf(passenger);
            if (seat >= 0 && seat < kind.seats().size() && VehicleRoles.of(kind.seats().get(seat).role()).drives()
                    && passenger instanceof LivingEntity driver && (driver instanceof Player
                    || driver instanceof IDriver)) {
                return driver;
            }
        }
        return null;
    }

    @Override
    public InteractionResult interactAt(final Player player, final Vec3 hit, final InteractionHand hand) {
        return VehicleInteractions.interact(this, player, hand, hit);
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        return VehicleInteractions.interact(this, player, hand, Vec3.ZERO);
    }

    /** Whether {@code passenger} could get in now: a seat it may sit in is free. */
    public boolean canAddPassengerFrom(final Entity passenger) {
        return canAddPassenger(passenger);
    }

    /** A player's blows break it back into its item, all it holds with it; other harm wears its parts. */
    @Override
    public boolean hurt(final DamageSource source, final float amount) {
        if (isInvulnerableTo(source) || level().isClientSide() || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            if (!VehicleAccess.mayBreak(this, player)) {
                return false;
            }
            blows += amount;
            if (player.getAbilities().instabuild || blows >= BREAKS_AT) {
                breakApart(!player.getAbilities().instabuild);
            }
            return true;
        }
        final VehicleDef kind = def();
        if (kind != null) {
            VehicleUpkeep.wearAll(this, kind, amount / BREAKS_AT);
        }
        return true;
    }

    /** Breaks it back into its item, dropping it when {@code drop}, its riders let off. */
    public void breakApart(final boolean drop) {
        ejectPassengers();
        if (drop) {
            spawnAtLocation(asItem());
        }
        discard();
    }

    /** The item it is, carrying all it holds, as it breaks into and as it is picked. */
    public ItemStack asItem() {
        return Vehicles.itemOf(this);
    }

    /** All it is and holds, as its item carries it. */
    public CompoundTag saveState() {
        final CompoundTag tag = new CompoundTag();
        addAdditionalSaveData(tag);
        tag.remove(SAVED_SEATS);
        tag.remove(SAVED_TOWED_BY);
        return tag;
    }

    /** Takes all it is and holds from what its item carried. */
    public void loadState(final CompoundTag tag) {
        readAdditionalSaveData(tag);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return asItem();
    }

    @Override
    public float maxUpStep() {
        final VehicleBuild built = build();
        if (built == null || def == null || def.movement() != VehicleDef.Movement.ROAD || built.wheels().isEmpty()) {
            return 0;
        }
        return (float) Math.clamp(built.drivenRadius() * 1.7, 0.5, 1.1);
    }

    @Override
    public EntityDimensions getDimensions(final Pose pose) {
        final VehicleDef kind = def;
        return kind == null ? super.getDimensions(pose)
                : EntityDimensions.scalable(kind.body().width(), kind.body().height());
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide()) {
            return;
        }
        if (PARTS.equals(key)) {
            parts.load(entityData.get(PARTS));
        } else if (SEATS.equals(key)) {
            readSeats(entityData.get(SEATS));
        } else if (DEF.equals(key)) {
            def();
        }
    }

    @Override
    public double audioX() {
        return getX();
    }

    @Override
    public double audioY() {
        return getY() + 0.5;
    }

    @Override
    public double audioZ() {
        return getZ();
    }

    /** Its engine's running sound, as fast and as loud as it turns and works; none while it is off or silent. */
    @Override
    public List<LoopRequest> loops() {
        final VehicleBuild built = build();
        if (built == null || built.engine().isEmpty() || !running()) {
            return List.of();
        }
        final VehiclePartDef.Engine engine = built.engine().get().spec();
        final SoundKey sound = engine.sound().map(SoundKeys::find).orElse(null);
        if (sound == null) {
            return List.of();
        }
        return List.of(new LoopRequest(sound, VehicleMath.engineVolume(load()),
                VehicleMath.enginePitch(rpm(), engine.idleRpm(), engine.maxRpm()), null));
    }

    @Override
    protected boolean canAddPassenger(final Entity passenger) {
        final VehicleDef kind = def();
        return kind != null && VehicleAccess.freeSeat(this, kind, passenger) >= 0;
    }

    @Override
    protected void addPassenger(final Entity passenger) {
        super.addPassenger(passenger);
        final VehicleDef kind = def();
        if (!level().isClientSide() && kind != null) {
            final int seat = VehicleAccess.freeSeat(this, kind, passenger);
            if (seat >= 0) {
                seats.put(passenger.getUUID(), seat);
                seatsChanged();
            }
        }
    }

    @Override
    protected void removePassenger(final Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && seats.remove(passenger.getUUID()) != null) {
            seatsChanged();
        }
    }

    @Override
    protected void positionRider(final Entity passenger, final MoveFunction move) {
        final VehicleDef kind = def();
        if (kind == null || kind.seats().isEmpty()) {
            super.positionRider(passenger, move);
            return;
        }
        final int seat = Math.clamp(seatOf(passenger), 0, kind.seats().size() - 1);
        final Vec3 at = kind.seats().get(seat).at();
        final Vec3 ahead = Vec3.directionFromRotation(0, getYRot());
        final Vec3 left = new Vec3(ahead.z, 0, -ahead.x);
        move.accept(passenger, getX() + left.x * at.x + ahead.x * at.z, getY() + at.y,
                getZ() + left.z * at.x + ahead.z * at.z);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(DEF, "");
        builder.define(PAINT, "");
        builder.define(VARIANT, "");
        builder.define(PARTS, new CompoundTag());
        builder.define(SEATS, new CompoundTag());
        builder.define(SWITCHES, VehicleSwitches.MADE.bits());
        builder.define(TOWED_BY, NO_ENTITY);
        builder.define(RPM, 0F);
        builder.define(GEAR, 1);
        builder.define(TEMPERATURE, (float) AIR_TEMPERATURE);
        builder.define(LOAD, 0F);
        builder.define(FUEL, 0F);
        builder.define(PITCH, 0F);
        builder.define(ROLL, 0F);
        builder.define(STEER, 0F);
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        entityData.set(DEF, tag.getString(SAVED_DEF));
        entityData.set(PAINT, tag.getString(SAVED_PAINT));
        entityData.set(VARIANT, tag.getString(SAVED_VARIANT));
        parts.load(tag.getCompound(SAVED_PARTS));
        if (tag.contains(SAVED_SWITCHES)) {
            entityData.set(SWITCHES, tag.getInt(SAVED_SWITCHES));
        }
        readSeats(tag.getCompound(SAVED_SEATS));
        ownership = tag.contains(SAVED_OWNER) ? Ownership.CODEC.parse(NbtOps.INSTANCE, tag.get(SAVED_OWNER))
                .result().orElse(null) : null;
        keyGeneration = Math.max(1, tag.getInt(SAVED_KEY));
        if (tag.hasUUID(SAVED_KEY_ID)) {
            keyId = tag.getUUID(SAVED_KEY_ID);
        }
        towedBy = tag.hasUUID(SAVED_TOWED_BY) ? tag.getUUID(SAVED_TOWED_BY) : null;
        blows = tag.getFloat(SAVED_BLOWS);
        final CompoundTag saved = tag.getCompound(SAVED_DRIVE);
        drive.set(saved.contains(SAVED_GEAR) ? saved.getInt(SAVED_GEAR) : 1, saved.getDouble(SAVED_RPM),
                saved.contains(SAVED_TEMPERATURE) ? saved.getDouble(SAVED_TEMPERATURE) : Double.NaN);
        partsChanged();
        if (build() != null) {
            holds().ifPresent(held -> held.load(tag.getCompound(SAVED_HOLDS), level().registryAccess()));
        }
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        tag.putString(SAVED_DEF, entityData.get(DEF));
        tag.putString(SAVED_PAINT, paint());
        tag.putString(SAVED_VARIANT, variant());
        tag.put(SAVED_PARTS, parts.save());
        tag.putInt(SAVED_SWITCHES, entityData.get(SWITCHES));
        tag.put(SAVED_SEATS, writeSeats());
        if (ownership != null) {
            Ownership.CODEC.encodeStart(NbtOps.INSTANCE, ownership).result()
                    .ifPresent(owner -> tag.put(SAVED_OWNER, owner));
        }
        tag.putInt(SAVED_KEY, keyGeneration);
        tag.putUUID(SAVED_KEY_ID, keyId);
        if (towedBy != null) {
            tag.putUUID(SAVED_TOWED_BY, towedBy);
        }
        tag.putFloat(SAVED_BLOWS, blows);
        final CompoundTag saved = new CompoundTag();
        saved.putInt(SAVED_GEAR, drive.gear());
        saved.putDouble(SAVED_RPM, drive.rpm());
        saved.putDouble(SAVED_TEMPERATURE, drive.temperature(AIR_TEMPERATURE));
        tag.put(SAVED_DRIVE, saved);
        holds().ifPresent(held -> {
            final CompoundTag holding = new CompoundTag();
            held.save(holding, level().registryAccess());
            tag.put(SAVED_HOLDS, holding);
        });
    }

    /** Its parts changed: every player is told, and what is worked out of them is worked out again. */
    void partsChanged() {
        if (!level().isClientSide()) {
            entityData.set(PARTS, parts.save());
        }
    }

    /** Tells every player what the instruments read, its lean and its steering. */
    void report(final VehicleDrive.Result result, final float fullness) {
        entityData.set(RPM, (float) result.rpm());
        entityData.set(GEAR, result.gear());
        if (!Double.isNaN(result.temperature())) {
            entityData.set(TEMPERATURE, (float) result.temperature());
        }
        entityData.set(LOAD, (float) result.load());
        entityData.set(FUEL, fullness);
        entityData.set(PITCH, motion.pitch());
        entityData.set(ROLL, motion.roll());
        entityData.set(STEER, motion.steer());
    }

    /** Takes the change asked of a manual gearbox, once. */
    int takeShift() {
        final int asked = shift;
        shift = 0;
        return asked;
    }

    /** What it holds, made for its kind the first time it is asked. */
    private VehicleHolds holds(final VehicleDef kind) {
        if (holds == null) {
            holds = new VehicleHolds(kind, () -> { });
        }
        return holds;
    }

    private void seatsChanged() {
        entityData.set(SEATS, writeSeats());
    }

    private CompoundTag writeSeats() {
        final CompoundTag tag = new CompoundTag();
        for (final Map.Entry<UUID, Integer> each : seats.entrySet()) {
            tag.putInt(each.getKey().toString(), each.getValue());
        }
        return tag;
    }

    private void readSeats(final CompoundTag tag) {
        seats.clear();
        for (final String key : tag.getAllKeys()) {
            try {
                seats.put(UUID.fromString(key), tag.getInt(key));
            } catch (final IllegalArgumentException notAnId) {
                // A seat saved under no player's id is left empty.
            }
        }
    }

    /* Spreads the steps the server sends of its place over the next ticks, for the players watching it. */
    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, getXRot());
            lerpSteps--;
        }
    }

    /* Its battery, as chargers and cables reach it: it takes charge, and gives none but to its own engine. */
    private final class Battery implements IEnergyStorage {

        @Override
        public int receiveEnergy(final int amount, final boolean simulate) {
            return holds().map(held -> held.receiveCharge(amount, simulate)).orElse(0);
        }

        @Override
        public int extractEnergy(final int amount, final boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return holds().map(VehicleHolds::charge).orElse(0);
        }

        @Override
        public int getMaxEnergyStored() {
            return holds().map(VehicleHolds::chargeCapacity).orElse(0);
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return getMaxEnergyStored() > 0;
        }
    }
}
