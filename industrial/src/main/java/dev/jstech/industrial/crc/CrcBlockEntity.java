/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.content.IMenuTitle;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.core.heat.FluidHeat;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.HeatForms;
import dev.jstech.core.heat.IHeatPort;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.machine.FaceSlot;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineScreenData;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.research.DataKind;
import dev.jstech.core.research.DataKinds;
import dev.jstech.core.research.Discoveries;
import dev.jstech.core.research.Discovery;
import dev.jstech.core.research.Research;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.GameText;
import dev.jstech.core.util.Loaded;
import dev.jstech.industrial.IndustrialFluids;
import dev.jstech.industrial.research.GroupPower;
import dev.jstech.industrial.research.IndustrialResearch;
import dev.jstech.industrial.research.ResearchLink;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The terminal of a Computational Research Centre, and the machine itself once its structure stands formed
 * (industrial/docs/design/science.md).
 *
 * <p>Its 27 places hold processors, which make research and draw power, all of it heat, and what takes the heat away:
 * heat sinks to the air, liquid coolers into the water of its coolant loop, heat pipe coolers out through its Heat
 * Port. Past 100 degrees the part that heats most burns. Its four data places hold the data its discoveries spend.
 *
 * <p>Once a second it reads the group its Link Socket's cable joins it to. The group works one discovery, which any of
 * its CRCs may choose; each team counts its own CRCs, the strongest whole and each other at 90%, and the lowest of a
 * team's CRCs works the team's research on with the data of all of them. The progress is the team's, so cutting the
 * cable loses nothing.
 */
public class CrcBlockEntity extends MachineBlockEntity implements IMenuTitle {

    private final HeatBody body = HeatBody.of(Thermal.ofKind(MaterialKind.METAL), BODY_CUBIC_METRES);
    private final HeatBody evaporator = HeatBody.of(Thermal.ofKind(MaterialKind.METAL), EVAPORATOR_CUBIC_METRES);
    private final FieldFluidTank coolantIn;
    private final FieldFluidTank coolantOut;
    private final ValueField<ResourceLocation> chosen;
    private final ValueField<List<ResourceLocation>> queue;
    /* What its formed model shows: the part in each place, then the entry in each rear cell, by number. */
    private final ValueField<List<Integer>> shown;
    /* Where its rear places are, while it stands formed, west to east and bottom to top. */
    private final List<BlockPos> rearPlaces = new ArrayList<>();
    /* The heat its processors made since its last second, in joules. */
    private double made;
    /* The ticks of the second now passing it worked researching, with its power. */
    private int workedTicks;
    /* The research it made its last second, in points a second. */
    private double lastPoints;
    /* What came of its team's research its last second; none before its first. */
    private Research.@Nullable Outcome outcome;
    /* Its team's pace in its group its last second, in points a second. */
    private double pace;
    /* What its cooling took away its last second, in watts. */
    private double shed;
    /* The heat its processors made its last second, in watts, and whether its coolant and its Heat Port took any. */
    private double lastMade;
    private boolean cooledByWater;
    private boolean cooledThroughPort;
    /* The power its processors draw this tick, in watts. */
    private double drawing;
    private long lastSecond = -1;
    /* What its screen shows, worked out at most once a second, and only while a screen asks. */
    private CrcReadings screenReadings = CrcReadings.NONE;
    /* The game time it last worked them out at; a second before the first, so the first ask works them out. */
    private long readAt = -TICKS_A_SECOND;

    /** Its places, then its data places, one for each of J's Industrial's kinds of data. */
    public static final int PLACES = 27;
    public static final int FIRST_DATA = PLACES;
    public static final List<DataKind> DATA = List.of(IndustrialResearch.SAMPLE, IndustrialResearch.CRYSTAL,
            IndustrialResearch.SPECTRAL, IndustrialResearch.PARTICLE);
    /** What a rear cell of its formed model shows: its panel, or the entry standing there. */
    public static final int CELL_PANEL = 0;
    public static final int CELL_POWER = 1;
    public static final int CELL_LINK = 2;
    public static final int CELL_COLD = 3;
    public static final int CELL_HOT = 4;
    public static final int CELL_HEAT = 5;
    /*
     * Its body, steel enough for 200 kJ a kelvin: a deficit of 5 kW takes most of an hour to burn a part, one of a
     * megawatt seconds. The evaporator its heat pipes start from is a small block of copper's worth.
     */
    private static final double BODY_CUBIC_METRES = 0.06;
    private static final double EVAPORATOR_CUBIC_METRES = 0.002;
    /* What the cabinet's own skin sheds to the air, so a CRC with no sink still cools while it rests. */
    private static final double SKIN_WATTS = 1_000.0;
    /* Its coolant tanks, in millibuckets. */
    private static final int COOLANT_TANK = 8_000;
    /* Its J's Energy store at medium voltage: a second of a CRC full of T5 processors, and a little more. */
    private static final long ENERGY_CAPACITY = 2_400_000L;
    private static final long ENERGY_RECEIVE = 120_000L;
    private static final int TICKS_A_SECOND = 20;
    private static final int STACK = 64;

    public CrcBlockEntity(final BlockPos pos, final BlockState state) {
        super(CrcContent.TERMINAL_BE.get(), pos, state, PLACES + DATA.size(),
                MachineEnergy.of(JsEnergy.ENERGY, JsEnergy.MEDIUM, ENERGY_CAPACITY, ENERGY_RECEIVE));
        final PartField bodyField = fields().part("Body", this.body).save().toClient();
        this.body.listening(bodyField::changed, this::wake);
        fields().part("Evaporator", this.evaporator).save();
        this.coolantIn = fields().fluid("CoolantIn", COOLANT_TANK).save().toClient();
        this.coolantIn.setValidator(fluid -> fluid.is(Fluids.WATER));
        this.coolantOut = fields().fluid("CoolantOut", COOLANT_TANK).save().toClient().outputOnly();
        this.chosen = fields().nullable("Chosen", ResourceLocation.CODEC).save().toClient();
        this.queue = fields().value("Queue", ResourceLocation.CODEC.listOf(), List.<ResourceLocation>of()).save()
                .toClient();
        this.shown = fields().value("Shown", Codec.INT.listOf(), List.<Integer>of()).toClient();
        fields().mirror(CrcBlock.FORMED, this::formed);
        fields().whenBroken((level, at) -> unflagEntries(level));
        getInventory().accepts(CrcBlockEntity::fits);
        getInventory().stackLimit((slot, stack) -> slot < PLACES ? 1 : STACK);
        formsWith(CrcContent.CRC.structure(CrcStructure.PATTERN).merges());
    }

    /** Whether its structure stands formed. */
    public boolean formed() {
        final MachineStructure structure = structure();
        return structure != null && structure.formed();
    }

    /** Its body's temperature, in kelvin; what the place round it is before its first look. */
    public double temperature() {
        if (this.body.known() || this.level == null) {
            return this.body.temperature();
        }
        return HeatClimate.ambient(this.level, this.worldPosition);
    }

    /** The part in place {@code index}, if any. */
    public Optional<CrcPart> part(final int index) {
        return getInventory().getStackInSlot(index).getItem() instanceof CrcPartItem item ? Optional.of(item.part())
                : Optional.empty();
    }

    /** The research its processors make, in points a second. */
    public double researchRate() {
        double rate = 0.0;
        for (int i = 0; i < PLACES; i++) {
            rate += part(i).map(CrcPart::research).orElse(0.0);
        }
        return rate;
    }

    /** The power its processors draw while they work, in watts. */
    public double drawWatts() {
        double watts = 0.0;
        for (int i = 0; i < PLACES; i++) {
            watts += part(i).filter(CrcPart::processor).map(CrcPart::watts).orElse(0.0);
        }
        return watts;
    }

    /** The discovery its group works on, if one is chosen. */
    public Optional<ResourceLocation> chosenDiscovery() {
        return Optional.ofNullable(this.chosen.get());
    }

    /** The discoveries chosen to come after it, in order. */
    public List<ResourceLocation> queue() {
        return this.queue.get();
    }

    /** What came of its team's research its last second; none before its first. */
    public Optional<Research.Outcome> outcome() {
        return Optional.ofNullable(this.outcome);
    }

    /** Its team's pace in its group its last second, in points a second. */
    public double pace() {
        return this.pace;
    }

    /** What its cooling took away its last second, in watts. */
    public double shed() {
        return this.shed;
    }

    /** Its coolant's water coming in. */
    public FieldFluidTank coolantIn() {
        return this.coolantIn;
    }

    /** Its coolant's hot water going out. */
    public FieldFluidTank coolantOut() {
        return this.coolantOut;
    }

    /** What its formed model shows: each place's part, then each rear cell's entry, by number. */
    public List<Integer> shown() {
        return this.shown.get();
    }

    /**
     * Chooses {@code discovery} for its whole group, from any of its CRCs: worked on now, and taken out of the queue if
     * it was waiting there.
     */
    public void choose(final ServerLevel level, final ResourceLocation discovery) {
        final List<ResourceLocation> rest = new ArrayList<>(this.queue.get());
        rest.remove(discovery);
        for (final CrcBlockEntity crc : group(level)) {
            crc.chosen.set(discovery);
            crc.queue.set(List.copyOf(rest));
            crc.outcome = null;
        }
    }

    /** Puts {@code discovery} at the end of its group's queue, if it is not chosen or waiting already. */
    public void enqueue(final ServerLevel level, final ResourceLocation discovery) {
        if (discovery.equals(this.chosen.get()) || this.queue.get().contains(discovery)) {
            return;
        }
        final List<ResourceLocation> more = new ArrayList<>(this.queue.get());
        more.add(discovery);
        for (final CrcBlockEntity crc : group(level)) {
            crc.queue.set(List.copyOf(more));
        }
    }

    /** Its group: itself and every formed CRC its Link Socket's run of cable joins it to, in no order. */
    public List<CrcBlockEntity> group(final ServerLevel level) {
        final List<CrcBlockEntity> crcs = new ArrayList<>();
        crcs.add(this);
        final Optional<LineGroup> run = run(level);
        if (run.isEmpty()) {
            return crcs;
        }
        for (final BlockPos member : run.get().members()) {
            if (Loaded.blockEntity(level, member) instanceof CrcEntryBlockEntity entry && entry.terminalPos() != null
                    && Loaded.blockEntity(level, entry.terminalPos()) instanceof CrcBlockEntity crc && crc != this
                    && crc.formed() && !crcs.contains(crc)) {
                crcs.add(crc);
            }
        }
        return crcs;
    }

    /** The team of its owner, whose research it works; none for a CRC nobody owns. */
    public @Nullable String team(final MinecraftServer server) {
        final Ownership owner = ownership();
        return owner == null ? null : CoreTeams.teamOf(server, owner.owner());
    }

    /** The research it made its last second, in points a second. */
    public double lastPoints() {
        return this.lastPoints;
    }

    /** The power its processors draw this tick, in watts: none while they rest. */
    public double drawing() {
        return this.drawing;
    }

    /** The heat its processors made its last second, in watts. */
    public double lastMade() {
        return this.lastMade;
    }

    /** Whether its coolant took heat away through its Liquid Coolers its last second. */
    public boolean cooledByWater() {
        return this.cooledByWater;
    }

    /** Whether heat left through its Heat Port its last second. */
    public boolean cooledThroughPort() {
        return this.cooledThroughPort;
    }

    /** The run of Research Link Cable its Link Socket joins, if it stands formed with one. */
    public Optional<LineGroup> run(final ServerLevel level) {
        final BlockPos socket = entryAt(CELL_LINK);
        return socket == null ? Optional.empty()
                : LineGroups.of(level, ResearchLink.GRID).flatMap(lines -> lines.groupOf(socket));
    }

    /** The CRCs of its group whose owner is on {@code team}, itself among them when it is. */
    public List<CrcBlockEntity> mates(final ServerLevel level, final String team) {
        final List<CrcBlockEntity> mates = new ArrayList<>();
        for (final CrcBlockEntity crc : group(level)) {
            if (team.equals(crc.team(level.getServer()))) {
                mates.add(crc);
            }
        }
        return mates;
    }

    /** The data {@code crcs} hold, as one store their research spends from. */
    public static Research.IDataStore pool(final List<CrcBlockEntity> crcs) {
        return new Pool(crcs);
    }

    /** Its terminal opens the Centre, under the Centre's name. */
    @Override
    public Component menuTitle() {
        return GameText.component(CrcTexts.NAME);
    }

    /** Its screen is wider than every machine's, for its three sections of places. */
    @Override
    public int screenWidth() {
        return CrcLayout.WIDTH;
    }

    /** Research and Group, after the five tabs every machine has. */
    @Override
    public int ownTabs() {
        return 2;
    }

    /** Research and Group take no items, so their face takes the whole window. */
    @Override
    public boolean inventoryOn(final int tab) {
        return tab != CrcLayout.RESEARCH_TAB && tab != CrcLayout.GROUP_TAB;
    }

    @Override
    public MachineScreenData<CrcReadings> screenData() {
        return new MachineScreenData<>(CrcReadings.STREAM_CODEC, CrcReadings.NONE, this::screenReadings);
    }

    @Override
    public List<FaceSlot> mainSlots() {
        final List<FaceSlot> slots = new ArrayList<>(PLACES + DATA.size());
        for (int i = 0; i < PLACES; i++) {
            final GuiLayout.SlotPosition at = CrcLayout.place(i);
            slots.add(new FaceSlot(i, at.x(), at.y(), false));
        }
        for (int k = 0; k < DATA.size(); k++) {
            final GuiLayout.SlotPosition at = CrcLayout.data(k);
            slots.add(new FaceSlot(FIRST_DATA + k, at.x(), at.y(), false));
        }
        return slots;
    }

    /** Nothing goes in or out of it by item through its structure: its places are filled by hand. */
    @Override
    public @Nullable IItemHandler itemPort(final PortKind kind, final BlockPos part, final @Nullable Direction side) {
        return null;
    }

    /** Its coolant: water in at the Coolant Inlet, hot water out at the Coolant Outlet. */
    @Override
    public @Nullable IFluidHandler fluidPort(final PortKind kind, final BlockPos part,
                                             final @Nullable Direction side) {
        if (!formed()) {
            return null;
        }
        return kind == PortKind.FLUID_INPUT ? this.coolantIn : kind == PortKind.FLUID_OUTPUT ? this.coolantOut : null;
    }

    /** The evaporator its heat pipe coolers give their heat to, through its Heat Port. */
    @Override
    public @Nullable IHeatPort heatPort(final BlockPos part, final @Nullable Direction side) {
        return formed() && part.equals(entryAt(CELL_HEAT)) ? this.evaporator : null;
    }

    @Override
    protected MachineStatus work(final ServerLevel level) {
        if (!this.body.known()) {
            this.body.set(HeatClimate.ambient(level, this.worldPosition));
        }
        final long now = level.getGameTime();
        if (this.lastSecond < 0) {
            this.lastSecond = now;
        }
        MachineStatus status = idleStatus();
        final double watts = researching() ? drawWatts() : 0.0;
        this.drawing = 0.0;
        if (watts > 0.0) {
            final long perTick = (long) Math.ceil(watts / TICKS_A_SECOND);
            final long spent = power().spend(perTick);
            drew(spent);
            if (spent >= perTick) {
                this.made += watts / TICKS_A_SECOND;
                this.workedTicks++;
                this.drawing = watts;
                status = MachineStatus.WORKING;
            } else {
                status = MachineStatus.NO_ENERGY;
            }
        }
        if (now - this.lastSecond >= TICKS_A_SECOND) {
            second(level, (now - this.lastSecond) / (double) TICKS_A_SECOND);
            this.lastSecond = now;
        }
        return status;
    }

    @Override
    protected double progressShare() {
        final MinecraftServer server = this.level == null ? null : this.level.getServer();
        final ResourceLocation id = this.chosen.get();
        final String team = server == null ? null : team(server);
        final Optional<Discovery> discovery = id == null ? Optional.empty() : Discoveries.byId(id);
        return team == null || discovery.isEmpty() ? 0.0 : Research.share(server, team, discovery.get());
    }

    /* Its structure was checked again: the entries are read, and drawn where the player put them, at once. */
    @Override
    protected void structureChecked(final ServerLevel level) {
        readRear(level);
        final List<Integer> now = visuals();
        if (!now.equals(this.shown.get())) {
            this.shown.set(now);
        }
    }

    /* What its screen shows, worked out again when a second has passed since the last time a screen asked. */
    private CrcReadings screenReadings() {
        if (this.level instanceof ServerLevel server && server.getGameTime() - this.readAt >= TICKS_A_SECOND) {
            this.readAt = server.getGameTime();
            this.screenReadings = CrcScreenReadings.read(this, server);
        }
        return this.screenReadings;
    }

    /* Whether its processors work this tick: its team's research goes on, and it has processors to work it. */
    private boolean researching() {
        return this.chosen.get() != null && this.outcome == Research.Outcome.WORKED && researchRate() > 0.0;
    }

    /* What it shows while its processors rest. */
    private MachineStatus idleStatus() {
        return this.outcome == Research.Outcome.LOCKED ? MachineStatus.LOCKED : MachineStatus.WAITING;
    }

    /* A second of it: what it shows, what it made, its heat, and its group's research. */
    private void second(final ServerLevel level, final double seconds) {
        readRear(level);
        this.lastPoints = researchRate() * this.workedTicks / TICKS_A_SECOND / seconds;
        this.workedTicks = 0;
        heat(level, seconds);
        research(level, seconds);
        final List<Integer> now = visuals();
        if (!now.equals(this.shown.get())) {
            this.shown.set(now);
        }
    }

    /* Where its rear places are, and the entries flagged formed so the formed model draws them; or none. */
    private void readRear(final ServerLevel level) {
        final MachineStructure structure = structure();
        final Optional<Orientation> standing = structure == null ? Optional.empty() : structure.orientation();
        if (!formed() || standing.isEmpty()) {
            unflagEntries(level);
            return;
        }
        this.rearPlaces.clear();
        for (int row = 0; row < CrcStructure.REAR_ROWS; row++) {
            for (int column = 0; column < CrcStructure.REAR_COLUMNS; column++) {
                final BlockPos at = CrcStructure.rearPlace(this.worldPosition, standing.get(), column, row);
                this.rearPlaces.add(at);
                final BlockState state = stateAt(level, at);
                if (state != null && state.getBlock() instanceof CrcBlock && !state.getValue(CrcBlock.FORMED)) {
                    level.setBlock(at, state.setValue(CrcBlock.FORMED, true), Block.UPDATE_ALL);
                }
            }
        }
    }

    /* Lets the entries it flagged draw themselves again: the structure came apart, or its terminal is gone. */
    private void unflagEntries(final ServerLevel level) {
        for (final BlockPos at : this.rearPlaces) {
            final BlockState state = stateAt(level, at);
            if (state != null && state.getBlock() instanceof CrcBlock && state.getValue(CrcBlock.FORMED)) {
                level.setBlock(at, state.setValue(CrcBlock.FORMED, false), Block.UPDATE_ALL);
            }
        }
        this.rearPlaces.clear();
    }

    /* The entry of {@code kind} standing at its rear, while it stands formed; none otherwise. */
    private @Nullable BlockPos entryAt(final int kind) {
        if (this.level == null) {
            return null;
        }
        for (final BlockPos at : this.rearPlaces) {
            final BlockState state = stateAt(this.level, at);
            if (state != null && cellOf(state) == kind) {
                return at;
            }
        }
        return null;
    }

    /* Its heat over {@code seconds}: what its processors made, then what each way of cooling took away. */
    private void heat(final ServerLevel level, final double seconds) {
        final double air = HeatClimate.ambient(level, this.worldPosition);
        final CrcHeat.Way byAir = new CrcHeat.Way((SKIN_WATTS + count(CrcPart.HEAT_SINK)
                * CrcPart.HEAT_SINK.watts()) * seconds, air);
        final FluidStack hot = new FluidStack(IndustrialFluids.HOT_WATER.get(), 1);
        final double perMillibucket = HeatForms.joulesPerMillibucket(new FluidStack(Fluids.WATER, 1), hot);
        CrcHeat.Way byWater = CrcHeat.Way.NONE;
        final int coolers = count(CrcPart.LIQUID_COOLER);
        if (coolers > 0 && perMillibucket > 0.0 && entryAt(CELL_COLD) != null && entryAt(CELL_HOT) != null) {
            final int water = Math.min(this.coolantIn.getFluidAmount(), this.coolantOut.getSpace());
            byWater = new CrcHeat.Way(Math.min(coolers * CrcPart.LIQUID_COOLER.watts() * seconds,
                    water * perMillibucket), FluidHeat.temperature(hot));
        }
        CrcHeat.Way byPipe = CrcHeat.Way.NONE;
        final int pipes = count(CrcPart.HEAT_PIPE_COOLER);
        if (pipes > 0 && entryAt(CELL_HEAT) != null) {
            if (!this.evaporator.known()) {
                this.evaporator.set(air);
            }
            byPipe = new CrcHeat.Way(pipes * CrcPart.HEAT_PIPE_COOLER.watts() * seconds,
                    this.evaporator.temperature());
        }
        final CrcHeat.Step step = CrcHeat.second(this.body.temperature(), this.body.capacity(), this.made, byAir,
                byWater, byPipe);
        this.lastMade = this.made / seconds;
        this.made = 0.0;
        this.cooledByWater = step.water() > 0.0;
        this.cooledThroughPort = step.pipe() > 0.0;
        this.body.set(step.kelvin());
        if (step.water() > 0.0) {
            final int used = (int) Math.ceil(step.water() / perMillibucket);
            this.coolantIn.drain(used, IFluidHandler.FluidAction.EXECUTE);
            this.coolantOut.fillInside(new FluidStack(IndustrialFluids.HOT_WATER.get(), used),
                    IFluidHandler.FluidAction.EXECUTE);
        }
        if (step.pipe() > 0.0) {
            this.evaporator.exchange(step.pipe());
        }
        this.shed = (step.air() + step.water() + step.pipe()) / seconds;
        if (step.burns()) {
            burn(level);
        }
    }

    /* The part that heats most burns: the hungriest processor, the first of them when two are alike. */
    private void burn(final ServerLevel level) {
        int worst = -1;
        for (int i = 0; i < PLACES; i++) {
            final Optional<CrcPart> part = part(i);
            if (part.isPresent() && part.get().processor() && (worst < 0
                    || part.get().watts() > part(worst).map(CrcPart::watts).orElse(0.0))) {
                worst = i;
            }
        }
        if (worst < 0) {
            return;
        }
        final ItemStack lost = getInventory().getStackInSlot(worst);
        getInventory().setStackInSlot(worst, ItemStack.EMPTY);
        final Ownership owner = ownership();
        final ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner.owner());
        if (player != null) {
            player.sendSystemMessage(GameText.component(CrcTexts.BURNT.with(this.worldPosition.getX(),
                    this.worldPosition.getY(), this.worldPosition.getZ(), lost.getHoverName().getString())));
        }
    }

    /*
     * Its group's research, a second on: the lowest CRC of the group keeps every one on its discovery and moves the
     * group on once every team has made it; the lowest of each team works its team's research on.
     */
    private void research(final ServerLevel level, final double seconds) {
        final List<CrcBlockEntity> group = group(level);
        if (lowest(group) == this) {
            lead(level, group);
        }
        final MinecraftServer server = level.getServer();
        final String team = team(server);
        final ResourceLocation id = this.chosen.get();
        if (team == null || id == null) {
            this.outcome = null;
            this.pace = 0.0;
            return;
        }
        final List<CrcBlockEntity> mates = new ArrayList<>();
        final List<GroupPower.Member> members = new ArrayList<>();
        for (final CrcBlockEntity crc : group) {
            if (team.equals(crc.team(server))) {
                mates.add(crc);
                members.add(new GroupPower.Member(team, crc.lastPoints));
            }
        }
        // The lowest of the team's CRCs works the team's research for them all.
        if (lowest(mates) != this) {
            return;
        }
        final Optional<Discovery> discovery = Discoveries.byId(id);
        Research.Outcome result = Research.Outcome.ABSENT;
        final double teamPace = GroupPower.byTeam(members).getOrDefault(team, 0.0);
        if (discovery.isPresent()) {
            final Pool pool = new Pool(mates);
            result = Research.check(server, team, discovery.get(), pool);
            if (result == Research.Outcome.WORKED && teamPace > 0.0) {
                result = Research.work(server, team, discovery.get(), teamPace * seconds, pool);
            }
        }
        for (final CrcBlockEntity mate : mates) {
            mate.outcome = result;
            mate.pace = teamPace;
        }
    }

    /* As its group's lowest CRC: every other on its discovery and queue, and the next once every team made it. */
    private void lead(final ServerLevel level, final List<CrcBlockEntity> group) {
        final ResourceLocation id = this.chosen.get();
        if (id != null) {
            final Optional<Discovery> discovery = Discoveries.byId(id);
            final MinecraftServer server = level.getServer();
            final Set<String> teams = new HashSet<>();
            for (final CrcBlockEntity crc : group) {
                final String team = crc.team(server);
                if (team != null) {
                    teams.add(team);
                }
            }
            final boolean everyTeamMadeIt = !teams.isEmpty() && (discovery.isEmpty()
                    || teams.stream().allMatch(team -> Research.made(server, team, discovery.get())));
            if (everyTeamMadeIt) {
                final List<ResourceLocation> waiting = this.queue.get();
                this.chosen.set(waiting.isEmpty() ? null : waiting.getFirst());
                this.queue.set(waiting.isEmpty() ? List.of() : List.copyOf(waiting.subList(1, waiting.size())));
                this.outcome = null;
            }
        }
        for (final CrcBlockEntity crc : group) {
            if (crc == this) {
                continue;
            }
            if (!Objects.equals(crc.chosen.get(), this.chosen.get())) {
                crc.chosen.set(this.chosen.get());
                crc.outcome = null;
            }
            if (!crc.queue.get().equals(this.queue.get())) {
                crc.queue.set(this.queue.get());
            }
        }
    }

    /* How many places hold {@code part}. */
    private int count(final CrcPart part) {
        int count = 0;
        for (int i = 0; i < PLACES; i++) {
            if (part(i).filter(held -> held == part).isPresent()) {
                count++;
            }
        }
        return count;
    }

    /* What its formed model shows: each place's part (0 none, else the part's number), then each cell. */
    private List<Integer> visuals() {
        final List<Integer> now = new ArrayList<>(PLACES + this.rearPlaces.size());
        for (int i = 0; i < PLACES; i++) {
            now.add(part(i).map(CrcPart::id).orElse(0));
        }
        for (final BlockPos at : this.rearPlaces) {
            final BlockState state = this.level == null ? null : stateAt(this.level, at);
            now.add(state == null ? CELL_PANEL : cellOf(state));
        }
        return Collections.unmodifiableList(now);
    }

    /* The block at {@code pos}, when its chunk is loaded; none otherwise, so a look never loads one. */
    private static @Nullable BlockState stateAt(final Level level, final BlockPos pos) {
        return Loaded.at(level, pos) ? level.getBlockState(pos) : null;
    }

    /* What a rear cell holding {@code state} shows. */
    private static int cellOf(final BlockState state) {
        if (state.is(CrcContent.POWER_ENTRY.get())) {
            return CELL_POWER;
        }
        if (state.is(CrcContent.LINK_SOCKET.get())) {
            return CELL_LINK;
        }
        if (state.is(CrcContent.COOLANT_INLET.get())) {
            return CELL_COLD;
        }
        if (state.is(CrcContent.COOLANT_OUTLET.get())) {
            return CELL_HOT;
        }
        return state.is(CrcContent.HEAT_PORT.get()) ? CELL_HEAT : CELL_PANEL;
    }

    /* The CRC of {@code crcs} standing lowest by its place, which leads them. */
    private static @Nullable CrcBlockEntity lowest(final List<CrcBlockEntity> crcs) {
        CrcBlockEntity lowest = null;
        for (final CrcBlockEntity crc : crcs) {
            if (lowest == null || crc.worldPosition.asLong() < lowest.worldPosition.asLong()) {
                lowest = crc;
            }
        }
        return lowest;
    }

    /* What a place takes: a place, one part; a data place, the data of its kind. */
    private static boolean fits(final int slot, final ItemStack stack) {
        if (slot < PLACES) {
            return stack.getItem() instanceof CrcPartItem;
        }
        final DataKind kind = DATA.get(slot - FIRST_DATA);
        return DataKinds.of(stack).filter(kind::equals).isPresent();
    }

    /* The data of a team's CRCs in a group, held and spent as one. */
    private static final class Pool implements Research.IDataStore {

        private final List<CrcBlockEntity> crcs;

        private Pool(final List<CrcBlockEntity> crcs) {
            this.crcs = crcs;
        }

        @Override
        public int held(final DataKind kind) {
            final int slot = slotOf(kind);
            if (slot < 0) {
                return 0;
            }
            int held = 0;
            for (final CrcBlockEntity crc : this.crcs) {
                held += crc.getInventory().getStackInSlot(slot).getCount();
            }
            return held;
        }

        @Override
        public void spend(final DataKind kind, final int count) {
            final int slot = slotOf(kind);
            int left = count;
            for (final CrcBlockEntity crc : this.crcs) {
                if (slot < 0 || left <= 0) {
                    return;
                }
                left -= crc.getInventory().extractItem(slot, left, false).getCount();
            }
        }

        private static int slotOf(final DataKind kind) {
            final int index = DATA.indexOf(kind);
            return index < 0 ? -1 : FIRST_DATA + index;
        }
    }
}
