/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.block.OpticalPort;
import dev.jstech.computers.block.WanGatewayBlock;
import dev.jstech.computers.block.WanGatewayStructure;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.WanCardSpec;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.wan.GatewayLinks;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The WAN Gateway Computer: a computer of a 4 x 2 x 2 cabinet at either end of a long-distance line
 * (computers/docs/design/network.md). It takes hardware like any computer and runs a system; what makes it a gateway is
 * its WAN Interface Cards, one for each of the four ports on its back, the n-th card in its slots taking port n.
 *
 * <p>It is no orchestrator: it reads its network off the cables at its sides as a Personal Computer does, never off a
 * long-distance line, which joins it to the far end and not to the network it stands on.
 */
public class WanGatewayBlockEntity extends AbstractCabinetComputerBlockEntity {

    /* Each port's switches, as BorderLink sets them, six bits a port. */
    private final IntField ports = fields().integer("Ports", PortSettings.newPorts(WanGatewayStructure.PORTS)).save();
    /* What the lines do between the places they join, worked out once a tick on the server. */
    private final GatewayLinks links = new GatewayLinks(this);
    /* What each port's line is doing, two bits a port, for the cabinet's line modules and the screen's lamps. */
    private final DerivedInt lines = fields().derived("Lines", this::linesMask).toClient().toMenu();
    /* Whether it is on a network and whether its disks are at work, for its assembly menu. */
    private final DerivedInt assemblyNetworked = fields().derived("AssemblyNetworked",
            () -> networkUuid() != null).toMenu();
    private final DerivedInt assemblyDiskBusy = fields().derived("AssemblyDiskBusy", this::diskBusy).toMenu();

    /** A port with no card in it. */
    public static final int LINE_NONE = 0;
    /**
     * A port whose line is up: both gateways run BorderLink with the port on, and both cards take the line laid
     * between them.
     */
    public static final int LINE_UP = 1;
    /** A port whose card has no line: nothing at the far end that answers, a line its card does not take, or off. */
    public static final int LINE_DOWN = 2;
    private static final String PRESS = "animation.wan_gateway.press_power";
    private static final int LINE_BITS = 2;
    private static final int LINE_MASK = 3;

    public WanGatewayBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.WAN_GATEWAY_BE.get(), pos, state);
    }

    /**
     * What a WAN Gateway Computer of {@code era} is: the MTX board of its own era, as the Mainframe it stands beside,
     * in a cabinet that cools as the Mainframe's does.
     */
    public static ComputerDef computer(final Era era) {
        return new ComputerDef(era, Set.of(FormFactor.MTX), HostScope.WAN_GATEWAY_COMPUTER, PARTS,
                CaseCooling.MAINFRAME);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final WanGatewayBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickNode(serverLevel);
            be.links.tick(serverLevel);
        }
    }

    /** What the gateway's lines do, as the last tick worked out. */
    public GatewayLinks links() {
        return links;
    }

    /** Port {@code port}'s switches, 1 to 4. */
    public PortSettings portSettings(final int port) {
        return PortSettings.ofPort(ports.get(), port);
    }

    /** Sets port {@code port}'s switches. */
    public void setPortSettings(final int port, final PortSettings settings) {
        ports.set(settings.into(ports.get(), port));
    }

    /** What the gateway is called on the far side of its lines: the name a player gave it, else its id. */
    public String displayName() {
        return customName().isEmpty() ? machineId() : customName();
    }

    /** Writes {@code message} into the log of the gateway's network, under its name. */
    public void log(final ServerLevel level, final EventLevel what, final Text message) {
        logEvent(level, what, message);
    }

    /* A gateway taken down lets go of every line it joined or federated. */
    @Override
    public void onBroken(final ServerLevel level) {
        links.leave(level);
        super.onBroken(level);
    }

    /** What port {@code port}'s line is doing, 1 to 4: {@link #LINE_NONE}, {@link #LINE_UP} or {@link #LINE_DOWN}. */
    public int line(final int port) {
        return (lines.getAsInt() >> (LINE_BITS * (port - 1))) & LINE_MASK;
    }

    /** How many of its ports hold a card. */
    public int cardedPorts() {
        int carded = 0;
        for (int port = 1; port <= WanGatewayStructure.PORTS; port++) {
            carded += line(port) == LINE_NONE ? 0 : 1;
        }
        return carded;
    }

    /** How many of its ports have their line up. */
    public int portsUp() {
        int up = 0;
        for (int port = 1; port <= WanGatewayStructure.PORTS; port++) {
            up += line(port) == LINE_UP ? 1 : 0;
        }
        return up;
    }

    /** The WAN Interface Cards in its slots, in slot order: the first takes port 1. */
    public List<WanCardSpec> wanCards() {
        final List<WanCardSpec> cards = new ArrayList<>(WanGatewayStructure.PORTS);
        for (int k = 0; k < GPU_SLOTS && cards.size() < WanGatewayStructure.PORTS; k++) {
            if (HardwareRegistry.card(getHardware().getStackInSlot(GPU_SLOTS_START + k)) instanceof WanCardSpec card) {
                cards.add(card);
            }
        }
        return cards;
    }

    @Override
    public boolean isValidForSlot(final int slot, final ItemStack stack) {
        // A card for each of the four ports: a fifth would have no port to take.
        if (isWanCard(stack) && slot >= GPU_SLOTS_START && slot < GPU_SLOTS_START + GPU_SLOTS
                && wanCardsBesides(slot) >= WanGatewayStructure.PORTS) {
            return false;
        }
        return super.isValidForSlot(slot, stack);
    }

    @Override
    @Nullable
    public TextKey refusal(final int slot, final ItemStack stack) {
        final TextKey why = super.refusal(slot, stack);
        // The card itself fits; what keeps it out is that every port has one already.
        return why == PartRefusals.CARD_BUS && isWanCard(stack) && isValidPcieCard(stack)
                ? PartRefusals.FOUR_WAN_CARDS : why;
    }

    public boolean assemblyNetworked() {
        return assemblyNetworked.isSet();
    }

    public boolean assemblyDiskBusy() {
        return assemblyDiskBusy.isSet();
    }

    @Override
    public boolean computerRunning() {
        return isRunning();
    }

    @Override
    public boolean computerBuildValid() {
        return buildValid();
    }

    @Override
    public int networkLinkState() {
        return networkUuid() != null ? 1 : 0;
    }

    @Override
    public long orchestrationCapacity() {
        return capacity();
    }

    @Override
    public int computerQueues() {
        return 1;
    }

    @Override
    public boolean isMainframeHost() {
        return false;
    }

    @Override
    protected void registerNode(final NetworkSystem system, final NetworkUuid network) {
        // On its network it is a computer with local storage, addressed as a PC is.
        system.registerPersonalComputer(new NetworkSystem.PersonalComputerNode(
                nodeUuid(), network, capacity(), worldPosition.asLong()));
    }

    @Override
    protected void unregisterNode(final NetworkSystem system, final NetworkUuid network) {
        system.unregisterPersonalComputer(network, nodeUuid());
    }

    /* The long-distance line joins the gateway to the far end, never to the network it stands on. */
    @Override
    protected boolean readsNetworkOff(final Wire wire) {
        final DataLink link = DataWires.linkOf(wire);
        return DataWires.isNetwork(wire) && link != null && link.line() != DataLine.LONG_DISTANCE;
    }

    /* An Optical Network Card put in or taken out opens or closes the fibre on every block of the cabinet. */
    @Override
    protected void partsChanged(final List<ResourceLocation> before) {
        super.partsChanged(before);
        if (level != null && !level.isClientSide() && getBlockState().getBlock() instanceof WanGatewayBlock block) {
            block.setOptical(level, worldPosition, OpticalPort.holdsCard(currentBuild()));
        }
    }

    @Override
    protected List<BlockPos> cabinetBlocks(final Direction facing) {
        return WanGatewayStructure.allPositions(worldPosition, facing);
    }

    @Override
    protected String pressClip() {
        return PRESS;
    }

    /* Each port's line, two bits a port, as the gateway's links last worked it out. */
    private int linesMask() {
        int mask = 0;
        for (int port = 1; port <= WanGatewayStructure.PORTS; port++) {
            mask |= links.state(port) << (LINE_BITS * (port - 1));
        }
        return mask;
    }

    /* How many WAN Interface Cards the card slots hold, but for {@code slot}. */
    private int wanCardsBesides(final int slot) {
        int cards = 0;
        for (int i = GPU_SLOTS_START; i < GPU_SLOTS_START + GPU_SLOTS; i++) {
            cards += i != slot && isWanCard(getHardware().getStackInSlot(i)) ? 1 : 0;
        }
        return cards;
    }

    private static boolean isWanCard(final ItemStack stack) {
        return HardwareRegistry.card(stack) instanceof WanCardSpec;
    }
}
