/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.network.INetworkMember;
import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.network.NetworkMembers;
import dev.jstech.core.jstechseries.network.StateRow;
import dev.jstech.core.jstechseries.network.StateRows;
import dev.jstech.core.jstechseries.network.StateTable;
import dev.jstech.core.jstechseries.operation.IOperationArgs;
import dev.jstech.core.jstechseries.operation.IOperationRequest;
import dev.jstech.core.jstechseries.operation.OperationFailure;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.jstechseries.peripheral.ControlLinks;
import dev.jstech.core.jstechseries.peripheral.PortKind;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.MachineParameter;
import dev.jstech.core.machine.RedstoneMode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.transit.CapsuleStationBlockEntity;
import dev.jstech.core.transit.SendOutcome;
import dev.jstech.industrial.integration.computers.ControllerOperations.MachineSetArgs;
import dev.jstech.industrial.integration.computers.ControllerOperations.StationSendArgs;
import dev.jstech.industrial.integration.computers.ControllerOperations.ValveSetArgs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Industrial Controller Computer's machine: a Personal Computer of its era in all it runs, and a member of the
 * data network it is on, through J's Core: it writes a row of the {@code machines} table for each machine its
 * Fieldbus Cards read and one of the {@code controllers} table for itself, and carries out the requests the network
 * gives it for those machines. It reads them through J's Core's values every machine offers, never through the
 * classes of the mod that made them, so it reads a machine of any mod.
 */
@TextHolder
public class IndustrialControllerBlockEntity extends PersonalComputerBlockEntity implements INetworkMember, Nameable {

    /* The capsules asked for while their station could not send yet, sent as soon as it can. */
    private final List<PendingSend> pending = new ArrayList<>();

    /** A card in the {@code card} column: its field bus and the machine's node on it, "PROFIBUS, node 5". */
    public static final TextKey CARD_NODE = TextKey.of("jsindustrial.controller.card_node", "%s, node %s");
    /** A machine's name in the {@code machines} table: its block's and its number, "Compressor #1". */
    public static final TextKey NUMBERED = TextKey.of("jsindustrial.controller.numbered", "%s #%s");
    /* Degrees Celsius above kelvin. */
    private static final double CELSIUS = 273.15;
    private static final double PERCENT = 100.0;

    public IndustrialControllerBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialControllers.ENTITY.get(), pos, state);
        NetworkMembers.keep(this);
    }

    /** Read and given work: the network reads its machines and asks it to set them. */
    @Override
    public NetworkCategory category() {
        return NetworkCategory.C;
    }

    @Override
    public List<OperationType<?>> carriesOut() {
        return List.of(ControllerOperations.MACHINE_SET, ControllerOperations.VALVE_SET,
                ControllerOperations.STATION_SEND);
    }

    @Override
    public List<StateTable> tables() {
        return List.of(ControllerTables.MACHINES, ControllerTables.CONTROLLERS);
    }

    /** A row of the machines table for each machine it reads, where the machine stands; one of its own for itself. */
    @Override
    public void fillRows(final StateTable table, final StateRows rows) {
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        if (table == ControllerTables.CONTROLLERS) {
            rows.own().set(ControllerTables.NAME_COLUMN, controllerName())
                    .set(ControllerTables.CARDS_COLUMN, cards().size())
                    .set(ControllerTables.PORTS_COLUMN, ports(PortKind.CONTROL))
                    .set(ControllerTables.READS_COLUMN, machines().size())
                    .set(ControllerTables.LEFT_OUT_COLUMN, leftOut(server));
            return;
        }
        if (!isRunning()) {
            // Switched off, it reads nothing.
            return;
        }
        final Map<Long, String> names = names(server);
        final Map<Long, String> cards = cardsOf();
        for (final long machine : machines()) {
            final BlockPos at = BlockPos.of(machine);
            final IMachineParameters values = server.getCapability(IMachineParameters.CAPABILITY, at, null);
            final StateRow.Writer row = rows.at(at).set(ControllerTables.NAME_COLUMN, names.getOrDefault(machine, ""))
                    .set(ControllerTables.KIND_COLUMN, kindOf(server, at))
                    .set(ControllerTables.CONTROLLER_COLUMN, controllerName())
                    .set(ControllerTables.CARD_COLUMN, cards.getOrDefault(machine, ""));
            if (values != null) {
                readInto(row, values);
            }
        }
    }

    /** Why it will not take a request: it is switched off, or it reads no machine of the name the request gives. */
    @Override
    public @Nullable Text refuses(final OperationType<?> type, final IOperationArgs args) {
        if (!isRunning()) {
            return ControllerOperations.SWITCHED_OFF.with(controllerName());
        }
        final String named = switch (args) {
            case MachineSetArgs set -> set.machine();
            case ValveSetArgs valve -> valve.valve();
            case StationSendArgs send -> send.station();
            default -> null;
        };
        return named != null && find(named).isEmpty() ? ControllerOperations.NOT_READ.with(named) : null;
    }

    /** Sets a value of a machine it reads, or what the machine does with redstone. */
    public OperationStatus machineSet(final IOperationRequest<MachineSetArgs> request) {
        final MachineSetArgs args = request.args();
        final Optional<IMachineParameters> values = valuesOf(args.machine());
        if (values.isEmpty()) {
            return failed(request, ControllerOperations.NOT_READ, args.machine());
        }
        if (args.redstone().isPresent()) {
            final String said = args.redstone().get().toLowerCase(Locale.ROOT);
            final RedstoneMode mode = StableNames.of(RedstoneMode.class).find(said);
            return mode != null && values.get().setRedstoneMode(mode) ? OperationStatus.COMPLETED
                    : failed(request, ControllerOperations.NO_REDSTONE, args.machine(), said);
        }
        if (args.parameter().isEmpty() || args.value().isEmpty()) {
            return failed(request, ControllerOperations.NOTHING_TO_SET);
        }
        final String parameter = args.parameter().get();
        final boolean settable = values.get().parameter(parameter).map(MachineParameter::settable).orElse(false);
        return settable && values.get().write(parameter, args.value().get()) ? OperationStatus.COMPLETED
                : failed(request, ControllerOperations.NOT_SETTABLE, args.machine(), parameter);
    }

    /** Opens or closes a valve it reads, one opened by hand. */
    public OperationStatus valveSet(final IOperationRequest<ValveSetArgs> request) {
        final ValveSetArgs args = request.args();
        final Optional<IMachineParameters> values = valuesOf(args.valve());
        if (values.isEmpty()) {
            return failed(request, ControllerOperations.NOT_READ, args.valve());
        }
        final boolean byHand = values.get().parameter("open").map(MachineParameter::settable).orElse(false);
        return byHand && values.get().write("open", args.open() ? 1 : 0) ? OperationStatus.COMPLETED
                : failed(request, ControllerOperations.NOT_BY_HAND, args.valve());
    }

    /**
     * Has a capsule station it reads send what waits in it to an address: at once, or as soon as the station can, the
     * request waiting until then, as a station without compressed air waits for it.
     */
    public OperationStatus stationSend(final IOperationRequest<StationSendArgs> request) {
        final StationSendArgs args = request.args();
        final Optional<BlockPos> at = find(args.station());
        if (at.isEmpty() || !(this.level instanceof ServerLevel server)) {
            return failed(request, ControllerOperations.NOT_READ, args.station());
        }
        if (!(server.getBlockEntity(at.get()) instanceof CapsuleStationBlockEntity station)) {
            return failed(request, ControllerOperations.NOT_A_STATION, args.station());
        }
        final PendingSend send = new PendingSend(request, at.get());
        return switch (send.tryOn(server, station)) {
            case SENT -> OperationStatus.COMPLETED;
            case NOT_YET -> {
                this.pending.add(send);
                request.whenCancelled(() -> this.pending.remove(send));
                yield OperationStatus.WAITING;
            }
            case NOTHING -> failed(request, ControllerOperations.NOTHING_WAITS, args.station());
            case NO_STATION -> failed(request, ControllerOperations.NO_STATION_THERE, String.valueOf(args.to()),
                    args.station());
        };
    }

    /** The machines it reads, where each stands, in the order it linked them, the nearest first. */
    public List<Long> machines() {
        final List<Long> read = new ArrayList<>();
        for (final long linked : linkedEndpoints()) {
            if (kindOf(linked) == PortKind.CONTROL) {
                read.add(linked);
            }
        }
        return read;
    }

    /** What the network calls it: the name a player gave the computer, or its block's. */
    @Override
    public Component getName() {
        return hasCustomName() ? Component.literal(customName()) : getBlockState().getBlock().getName();
    }

    @Override
    public boolean hasCustomName() {
        return !customName().isEmpty();
    }

    @Override
    public @Nullable Component getCustomName() {
        return hasCustomName() ? Component.literal(customName()) : null;
    }

    /* Its machine's tick, then the capsules that wait for their station. */
    @Override
    protected void tickNode(final ServerLevel level) {
        super.tickNode(level);
        if (this.pending.isEmpty()) {
            return;
        }
        for (final PendingSend send : List.copyOf(this.pending)) {
            final StationSendArgs args = send.request().args();
            if (!(level.getBlockEntity(send.station()) instanceof CapsuleStationBlockEntity station)) {
                failed(send.request(), ControllerOperations.NOT_A_STATION, args.station());
                this.pending.remove(send);
                continue;
            }
            switch (send.tryOn(level, station)) {
                case SENT -> send.request().report(OperationStatus.COMPLETED);
                case NOT_YET -> {
                    continue;
                }
                case NOTHING -> failed(send.request(), ControllerOperations.NOTHING_WAITS, args.station());
                case NO_STATION -> failed(send.request(), ControllerOperations.NO_STATION_THERE,
                        String.valueOf(args.to()), args.station());
            }
            this.pending.remove(send);
        }
    }

    /* The machine it reads that {@code named} names: by the name the machines table gives it, or by where it stands. */
    private Optional<BlockPos> find(final String named) {
        if (!(this.level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        final String wanted = named.strip();
        final Optional<BlockPos> place = placeIn(wanted);
        for (final Map.Entry<Long, String> machine : names(server).entrySet()) {
            final BlockPos at = BlockPos.of(machine.getKey());
            if (machine.getValue().equalsIgnoreCase(wanted) || place.isPresent() && place.get().equals(at)) {
                return Optional.of(at);
            }
        }
        return Optional.empty();
    }

    /* Where {@code written} says a block stands, "x, y, z"; none when it says no place. */
    private static Optional<BlockPos> placeIn(final String written) {
        final String[] parts = written.split(",");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BlockPos(Integer.parseInt(parts[0].strip()), Integer.parseInt(parts[1].strip()),
                    Integer.parseInt(parts[2].strip())));
        } catch (final NumberFormatException notAPlace) {
            return Optional.empty();
        }
    }

    /* The values of the machine {@code named} names, when it reads one of that name. */
    private Optional<IMachineParameters> valuesOf(final String named) {
        return find(named).map(at -> this.level == null ? null
                : this.level.getCapability(IMachineParameters.CAPABILITY, at, null));
    }

    /*
     * Each machine's name: its block's and its number among the machines of that block it reads, counted in the order
     * they stand (west to east, then down to up, then north to south), so a name holds while the machines do.
     */
    private Map<Long, String> names(final ServerLevel level) {
        final List<Long> read = new ArrayList<>(machines());
        read.sort(Comparator.comparingInt((Long at) -> BlockPos.of(at).getX())
                .thenComparingInt(at -> BlockPos.of(at).getY()).thenComparingInt(at -> BlockPos.of(at).getZ()));
        final Map<Long, String> names = new HashMap<>();
        final Map<Block, Integer> counted = new HashMap<>();
        for (final long at : read) {
            final Block block = level.getBlockState(BlockPos.of(at)).getBlock();
            final int number = counted.merge(block, 1, Integer::sum);
            names.put(at, NUMBERED.with(GameText.of(block.getName()).english(), number).english());
        }
        return names;
    }

    /* Which card reads each machine, and its node on that card: each card's ports taken in turn, nearest first. */
    private Map<Long, String> cardsOf() {
        final Map<Long, String> cards = new LinkedHashMap<>();
        final List<IExpansionCardSpec> fitted = cards();
        int card = 0;
        int node = 0;
        for (final long machine : machines()) {
            while (card < fitted.size() && node >= fitted.get(card).controlPorts()) {
                card++;
                node = 0;
            }
            if (card >= fitted.size()) {
                break;
            }
            node++;
            final String bus = fitted.get(card) instanceof FieldbusCardSpec fieldbus
                    ? fieldbus.fieldbus().english() : String.valueOf(card + 1);
            cards.put(machine, CARD_NODE.with(bus, node).english());
        }
        return cards;
    }

    /* Its cards that read machines, in the order they sit. */
    private List<IExpansionCardSpec> cards() {
        final ComputerBuild build = currentBuild();
        final List<IExpansionCardSpec> cards = new ArrayList<>();
        if (build != null) {
            for (final IExpansionCardSpec card : build.pcieCards()) {
                if (card.controlPorts() > 0) {
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    /* How many machines its cables reach that no controller reads: those its cards have no port left for. */
    private int leftOut(final ServerLevel level) {
        int left = 0;
        for (final long reached : ControlLinks.reached(level, this.worldPosition, this)) {
            left += ControlLinks.controllerOf(level, BlockPos.of(reached)).isEmpty() ? 1 : 0;
        }
        return left;
    }

    private String controllerName() {
        return GameText.of(getName()).english();
    }

    private static String kindOf(final ServerLevel level, final BlockPos at) {
        return GameText.of(level.getBlockState(at).getBlock().getName()).english();
    }

    /* What a machine's values say, in the machines table's columns; a value it does not have is left out. */
    private static void readInto(final StateRow.Writer row, final IMachineParameters values) {
        values.statusNow().ifPresent(status -> row.set(ControllerTables.STATE_COLUMN, status.serializedName()));
        values.parameter("temperature").ifPresent(temperature -> {
            final double now = temperature.value() - CELSIUS;
            final double most = temperature.most() - CELSIUS;
            row.set(ControllerTables.TEMPERATURE_COLUMN, Math.round(now))
                    .set(ControllerTables.LIMIT_COLUMN, Math.round(most));
            if (most > 0) {
                row.set(ControllerTables.HEAT_COLUMN, Math.round(PERCENT * now / most));
            }
        });
        values.powerDrawn().ifPresent(drawn -> {
            row.set(ControllerTables.POWER_COLUMN, drawn.perSecond());
            drawn.portClass().ifPresent(port -> row.set(ControllerTables.PORT_COLUMN, port.path()));
        });
        final OptionalDouble progress = values.read("progress");
        if (progress.isPresent()) {
            row.set(ControllerTables.PROGRESS_COLUMN, Math.round(PERCENT * progress.getAsDouble()));
        }
    }

    private static OperationStatus failed(final IOperationRequest<?> request, final TextKey why,
                                          final String... arguments) {
        request.fail(OperationFailure.of(why, arguments));
        return OperationStatus.FAILED;
    }

    /* A capsule asked of a station, kept while the station cannot send it yet. */
    private record PendingSend(IOperationRequest<StationSendArgs> request, BlockPos station) {

        /* Asks the station to send it now. */
        SendOutcome tryOn(final ServerLevel level, final CapsuleStationBlockEntity at) {
            return at.sendNow(level, this.request.args().to());
        }
    }
}
