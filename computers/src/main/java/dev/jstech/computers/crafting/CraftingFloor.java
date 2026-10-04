/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.util.Loaded;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A crafting network as it stands: the crafting cable that runs from the Crafting Computers to the machines, and the
 * parts on it. Its Crafting Interfaces, in the order the cable reaches them from the computers, each with what it
 * feeds (a machine it sits against, or the routers on a cable of its own); its Crafting Receiving Buses, each tied
 * to the interfaces whose machine it takes the outputs of; and the routers that sit on it rather than on an
 * interface's own cable, which nothing uses. The cable is followed only where its wire really runs: a part on a face
 * cuts it there, and a colour keeps two runs apart, which is how an interface's own cable stays off this one.
 *
 * <p>The computers on it drive its interfaces in that order, as many as their cards drive between them; the rest wait,
 * driven by none, until another card goes in.
 */
public final class CraftingFloor {

    private final ServerLevel level;
    private final Set<BlockPos> cables;
    private final List<BlockPos> computers;
    private final List<Site> interfaces = new ArrayList<>();
    private final List<Site> receivers = new ArrayList<>();
    private final List<Site> looseRouters = new ArrayList<>();
    private final Map<Site, Reach> reaches = new HashMap<>();
    private final Map<Site, BlockPos> drivenBy = new HashMap<>();

    /** The most cable blocks one crafting network is followed through, so a huge or looping run stays bounded. */
    public static final int MOST_BLOCKS = 1024;
    /** The most cable blocks an interface's own cable is followed through. */
    public static final int MOST_OWN_BLOCKS = 128;

    private CraftingFloor(final ServerLevel level, final Set<BlockPos> cables, final List<BlockPos> computers) {
        this.level = level;
        this.cables = cables;
        this.computers = computers;
    }

    /** The crafting network the crafting cable beside {@code device} (a Crafting Computer) belongs to. */
    public static CraftingFloor around(final ServerLevel level, final BlockPos device) {
        final List<BlockPos> starts = new ArrayList<>();
        for (final Direction d : Direction.values()) {
            final BlockPos next = device.relative(d);
            if (holdsCrafting(level, next)) {
                starts.add(next);
            }
        }
        return scan(level, starts);
    }

    /** The crafting network the crafting cable at {@code cable} belongs to. */
    public static CraftingFloor through(final ServerLevel level, final BlockPos cable) {
        return scan(level, holdsCrafting(level, cable) ? List.of(cable) : List.of());
    }

    /** Every cable block of the network. */
    public Set<BlockPos> cables() {
        return cables;
    }

    /** The Crafting Computers touching the network, by position. */
    public List<BlockPos> computers() {
        return computers;
    }

    /** Every Crafting Interface on the network, in the order the cable reaches them. */
    public List<Site> interfaces() {
        return interfaces;
    }

    /** Every Crafting Receiving Bus on the network. */
    public List<Site> receivers() {
        return receivers;
    }

    /** The routers on the network itself rather than on an interface's own cable: nothing uses them. */
    public List<Site> looseRouters() {
        return looseRouters;
    }

    /** What the interface at {@code site} feeds. */
    public Reach reach(final Site site) {
        return reaches.getOrDefault(site, Reach.NONE);
    }

    /** The Crafting Computer that drives the interface at {@code site}, or null when none does. */
    @Nullable
    public BlockPos drivenBy(final Site site) {
        return drivenBy.get(site);
    }

    /** The interfaces a computer drives, in the order they are driven. */
    public List<Site> driven() {
        final List<Site> out = new ArrayList<>();
        for (final Site site : interfaces) {
            if (drivenBy.containsKey(site)) {
                out.add(site);
            }
        }
        return out;
    }

    /** The interface on the network with {@code id}, or null. */
    @Nullable
    public Site interfaceById(final UUID id) {
        for (final Site site : interfaces) {
            final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
            if (part != null && part.id().equals(id)) {
                return site;
            }
        }
        return null;
    }

    /**
     * The interfaces the Receiving Bus at {@code site} credits: those it was tied to by hand, or, tied by none, those
     * whose machine is the block it faces.
     */
    public List<Site> tiedTo(final Site site) {
        final ReceivingBusPart bus = site.part(level, ReceivingBusPart.class);
        if (bus == null) {
            return List.of();
        }
        final List<Site> out = new ArrayList<>();
        if (!bus.tiedByHand().isEmpty()) {
            for (final UUID id : bus.tiedByHand()) {
                final Site tied = interfaceById(id);
                if (tied != null) {
                    out.add(tied);
                }
            }
            return out;
        }
        final BlockPos faced = site.faced();
        for (final Site candidate : interfaces) {
            if (faced.equals(reach(candidate).machine())) {
                out.add(candidate);
            }
        }
        return out;
    }

    /** The Receiving Buses that credit the interface at {@code site}. */
    public List<Site> receiversOf(final Site site) {
        final List<Site> out = new ArrayList<>();
        for (final Site bus : receivers) {
            if (tiedTo(bus).contains(site)) {
                out.add(bus);
            }
        }
        return out;
    }

    /** The interface whose own cable the router at {@code site} sits on, or null for none. */
    @Nullable
    public Site cableOf(final Site router) {
        for (final Site site : interfaces) {
            if (reach(site).routers().contains(router)) {
                return site;
            }
        }
        return null;
    }

    /* Follows the crafting wire from {@code starts}, gathering the cable blocks, the computers and the parts. */
    private static CraftingFloor scan(final ServerLevel level, final List<BlockPos> starts) {
        final CableType crafting = ComputingModule.CRAFTING_CABLE.get();
        final Set<BlockPos> visited = new LinkedHashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();
        for (final BlockPos start : starts) {
            if (visited.add(start)) {
                queue.add(start);
            }
        }
        final Set<BlockPos> computers = new LinkedHashSet<>();
        while (!queue.isEmpty() && visited.size() <= MOST_BLOCKS) {
            final BlockPos current = queue.poll();
            final CableBlockEntity cable = Cables.at(level, current);
            if (cable == null) {
                continue;
            }
            for (final Direction d : Direction.values()) {
                final BlockPos next = current.relative(d);
                if (cable.crosses(crafting, d) && holdsCrafting(level, next) && visited.add(next)) {
                    queue.add(next);
                }
                if (!cable.hasPart(d) && level.isLoaded(next)
                        && Loaded.blockEntity(level, next) instanceof CraftingComputerBlockEntity) {
                    computers.add(next);
                }
            }
        }
        final List<BlockPos> sortedComputers = new ArrayList<>(computers);
        sortedComputers.sort(Comparator.comparingLong(BlockPos::asLong));
        final CraftingFloor floor = new CraftingFloor(level, visited, sortedComputers);
        floor.gatherParts();
        floor.drive();
        return floor;
    }

    private void gatherParts() {
        final Set<Site> onOwnCables = new LinkedHashSet<>();
        for (final BlockPos pos : cables) {
            final CableBlockEntity cable = Cables.at(level, pos);
            if (cable == null) {
                continue;
            }
            for (final Direction d : Direction.values()) {
                final IFacePart part = cable.getPart(d);
                final Site site = new Site(pos, d);
                if (part instanceof CraftingInterfacePart) {
                    interfaces.add(site);
                } else if (part instanceof ReceivingBusPart) {
                    receivers.add(site);
                } else if (part instanceof CraftingRouterPart) {
                    looseRouters.add(site);
                }
            }
        }
        for (final Site site : interfaces) {
            final Reach reach = reachOf(site);
            reaches.put(site, reach);
            onOwnCables.addAll(reach.routers());
        }
        looseRouters.removeAll(onOwnCables);
    }

    /* The computers drive the interfaces in order, each as many as its cards drive. */
    private void drive() {
        int at = 0;
        for (final BlockPos pos : computers) {
            if (!(Loaded.blockEntity(level, pos) instanceof CraftingComputerBlockEntity computer)) {
                continue;
            }
            final int budget = computer.interfaceBudget();
            for (int i = 0; i < budget && at < interfaces.size(); i++) {
                drivenBy.put(interfaces.get(at++), pos);
            }
        }
    }

    /* What one interface feeds: the machine it sits against, or the routers on its own cable and their machine. */
    private Reach reachOf(final Site site) {
        final BlockPos faced = site.faced();
        if (!holdsCrafting(level, faced)) {
            final ExternalDataPort port = ExternalDataPort.at(level, faced, site.face().getOpposite());
            return port.isEmpty() ? Reach.NONE : new Reach(Mode.DIRECT, faced, List.of(), null, false, false);
        }
        final CableType crafting = ComputingModule.CRAFTING_CABLE.get();
        final Set<BlockPos> own = new LinkedHashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();
        own.add(faced);
        queue.add(faced);
        BlockPos reachesAt = null;
        while (!queue.isEmpty() && own.size() <= MOST_OWN_BLOCKS) {
            final BlockPos current = queue.poll();
            if (reachesAt == null && cables.contains(current)) {
                reachesAt = current;
            }
            final CableBlockEntity cable = Cables.at(level, current);
            if (cable == null) {
                continue;
            }
            for (final Direction d : Direction.values()) {
                final BlockPos next = current.relative(d);
                if (cable.crosses(crafting, d) && holdsCrafting(level, next) && own.add(next)) {
                    queue.add(next);
                }
            }
        }
        final List<Site> routers = new ArrayList<>();
        boolean shared = false;
        for (final BlockPos pos : own) {
            final CableBlockEntity cable = Cables.at(level, pos);
            if (cable == null || cables.contains(pos)) {
                continue;
            }
            for (final Direction d : Direction.values()) {
                final IFacePart part = cable.getPart(d);
                if (part instanceof CraftingRouterPart) {
                    routers.add(new Site(pos, d));
                } else if (part instanceof CraftingInterfacePart) {
                    shared = true;
                }
            }
        }
        BlockPos machine = null;
        boolean several = false;
        for (final Site router : routers) {
            final BlockPos target = router.faced();
            if (machine == null) {
                machine = target;
            } else if (!machine.equals(target)) {
                several = true;
            }
        }
        return new Reach(Mode.CABLE, machine, List.copyOf(routers), reachesAt, shared, several);
    }

    private static boolean holdsCrafting(final ServerLevel level, final BlockPos pos) {
        return level.isLoaded(pos) && Cables.holds(level, pos, ComputingModule.CRAFTING_CABLE.get());
    }

    /** A part's place: the cable block it is on and the face it is on, which is the way it faces. */
    public record Site(BlockPos cable, Direction face) {

        /** The block the part faces. */
        public BlockPos faced() {
            return cable.relative(face);
        }

        /** The part there when it is of {@code kind}, else null. */
        @Nullable
        public <P extends IFacePart> P part(final ServerLevel level, final Class<P> kind) {
            final CableBlockEntity host = level.isLoaded(cable) ? Cables.at(level, cable) : null;
            final IFacePart part = host == null ? null : host.getPart(face);
            return kind.isInstance(part) ? kind.cast(part) : null;
        }
    }

    /** How an interface feeds its machine. */
    public enum Mode {
        /** It faces nothing it can feed. */
        NONE,
        /** It sits against its machine and feeds it there. */
        DIRECT,
        /** A crafting cable of its own leaves it, with routers on it that feed the machine's faces. */
        CABLE
    }

    /**
     * What an interface feeds.
     *
     * @param mode          how
     * @param machine       the machine, or null when it feeds none
     * @param routers       the routers on its own cable
     * @param reachesAt     where its own cable runs into the crafting network, or null when it is kept apart
     * @param sharedCable   whether another interface sits on its own cable too
     * @param severalMachines whether its routers face more than one machine
     */
    public record Reach(Mode mode, @Nullable BlockPos machine, List<Site> routers, @Nullable BlockPos reachesAt,
                        boolean sharedCable, boolean severalMachines) {

        public static final Reach NONE = new Reach(Mode.NONE, null, List.of(), null, false, false);
    }
}
