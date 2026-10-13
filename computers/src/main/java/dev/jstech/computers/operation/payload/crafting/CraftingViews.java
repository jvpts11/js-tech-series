/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.crafting.CraftingDispatch;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.crafting.InterfaceRoutes;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.payload.CraftingView;
import dev.jstech.computers.operation.payload.InterfaceView;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What the window of a Crafting Input Router or a Crafting Receiving Bus shows of the crafting network it is part of,
 * worked out on the server from the network as it stands.
 */
@TextHolder
public final class CraftingViews {

    private static final TextKey CABLE_OF = TextKey.of("jsc.crafting.view.cable_of", "CABLE OF");
    private static final TextKey CABLE_OF_NOTE = TextKey.of("jsc.crafting.view.cable_of_note",
            "the interface whose cable this is");
    private static final TextKey FEEDS = TextKey.of("jsc.crafting.view.feeds", "FEEDS");
    /* A machine and the face of it a router or a bus is against: "Alloy Smelter, west face". */
    private static final TextKey MACHINE_FACE = TextKey.of("jsc.crafting.view.machine_face", "%s, %s face");
    private static final TextKey ACCEPTS = TextKey.of("jsc.crafting.view.accepts", "accepts here");
    private static final TextKey REFUSES = TextKey.of("jsc.crafting.view.refuses", "takes nothing here");
    private static final TextKey NOTHING_THERE = TextKey.of("jsc.crafting.view.nothing_there",
            "no machine on that face");
    private static final TextKey ROUTES = TextKey.of("jsc.crafting.view.routes", "ROUTES");
    /* An input a router carries and the pattern it is for: "Copper Ingot x3 for Bronze Ingot". */
    private static final TextKey ROUTE = TextKey.of("jsc.crafting.view.route", "%s x%s for %s");
    private static final TextKey NO_ROUTES = TextKey.of("jsc.crafting.view.no_routes", "none yet");
    private static final TextKey ROUTES_NOTE = TextKey.of("jsc.crafting.view.routes_note",
            "Chosen in the interface. A router whose filter takes an input is its default.");
    private static final TextKey FILTER_REFUSES = TextKey.of("jsc.crafting.view.filter_refuses",
            "%s is routed here for %s, but the filter does not take it.");
    private static final TextKey ALSO_TAKEN = TextKey.of("jsc.crafting.view.also_taken",
            "%s is also taken by %s: %s uses this one.");
    private static final TextKey OFF_CABLE = TextKey.of("jsc.crafting.view.off_cable",
            "Not on the cable of any interface: nothing uses it. Routers go on the cable that leaves an interface.");
    private static final TextKey TIED_TO = TextKey.of("jsc.crafting.view.tied_to", "TIED TO");
    private static final TextKey BY_ITSELF = TextKey.of("jsc.crafting.view.by_itself",
            "by itself: the interface that feeds this machine");
    private static final TextKey BY_HAND = TextKey.of("jsc.crafting.view.by_hand", "by hand");
    private static final TextKey NOT_TIED = TextKey.of("jsc.crafting.view.not_tied",
            "Not tied: no interface feeds this machine. Tie it to one by hand.");
    private static final TextKey ARRIVAL_ORDER = TextKey.of("jsc.crafting.view.arrival_order",
            "Tied to several: between them, what arrives is credited in arrival order.");
    private static final TextKey CREDIT = TextKey.of("jsc.crafting.view.credit", "CREDIT");
    private static final TextKey CREDIT_NOTE =
            TextKey.of("jsc.crafting.view.credit_note", "%s of %s back · fed %s lots");
    private static final TextKey NO_JOB = TextKey.of("jsc.crafting.view.no_job", "no job running");
    private static final TextKey DRAINING = TextKey.of("jsc.crafting.view.draining",
            "Draining: what comes out late goes where its job's outputs go, not to the next job.");
    private static final TextKey UNEXPECTED = TextKey.of("jsc.crafting.view.unexpected", "UNEXPECTED");
    private static final TextKey UNEXPECTED_VALUE = TextKey.of("jsc.crafting.view.unexpected_value", "%s %s");
    private static final TextKey UNEXPECTED_NOTE = TextKey.of("jsc.crafting.view.unexpected_note",
            "to the network");
    private static final TextKey FACE_DOWN = TextKey.of("jsc.crafting.view.face_down", "bottom");
    private static final TextKey FACE_UP = TextKey.of("jsc.crafting.view.face_up", "top");
    private static final TextKey FACE_NORTH = TextKey.of("jsc.crafting.view.face_north", "north");
    private static final TextKey FACE_SOUTH = TextKey.of("jsc.crafting.view.face_south", "south");
    private static final TextKey FACE_WEST = TextKey.of("jsc.crafting.view.face_west", "west");
    private static final TextKey FACE_EAST = TextKey.of("jsc.crafting.view.face_east", "east");
    private static final TextKey FEEDS_DIRECT = TextKey.of("jsc.crafting.view.feeds_direct",
            "%s · directly, against it");
    private static final TextKey FEEDS_CABLE = TextKey.of("jsc.crafting.view.feeds_cable",
            "%s · through its own cable: %s");
    private static final TextKey NO_MACHINE_YET = TextKey.of("jsc.crafting.view.no_machine_yet", "no machine");
    private static final TextKey RECEIVING_SELF = TextKey.of("jsc.crafting.view.receiving_self",
            "%s (tied by itself)");
    private static final TextKey RECEIVING_HAND = TextKey.of("jsc.crafting.view.receiving_hand", "%s (tied by hand)");
    private static final TextKey FEEDS_NOTHING = TextKey.of("jsc.crafting.view.feeds_nothing",
            "Feeds nothing: place it against a machine, or run its own cable to routers.");
    private static final TextKey REACHES_NETWORK = TextKey.of("jsc.crafting.view.reaches_network",
            "Its cable reaches the crafting network at %s, %s, %s: its routers would be seen there. Keep its cable "
                    + "apart; a colour on the cable helps.");
    private static final TextKey SHARED_CABLE = TextKey.of("jsc.crafting.view.shared_cable",
            "Another interface sits on its cable: each interface needs a cable of its own.");
    private static final TextKey SEVERAL_MACHINES = TextKey.of("jsc.crafting.view.several_machines",
            "Its routers face more than one machine: they must all feed one.");
    private static final TextKey NO_ROUTERS = TextKey.of("jsc.crafting.view.no_routers",
            "Its own cable has no routers on it: put one against each input face of the machine.");
    private static final TextKey NOT_DRIVEN = TextKey.of("jsc.crafting.view.not_driven",
            "Not driven: the Crafting Cards on this cable drive no more interfaces. Another card drives more.");
    private static final TextKey NO_COMPUTER = TextKey.of("jsc.crafting.view.no_computer",
            "No Crafting Computer on this crafting cable: nothing drives it.");
    private static final TextKey NO_RECEIVING = TextKey.of("jsc.crafting.view.no_receiving",
            "No Receiving Bus takes its machine's outputs: put one against the machine's output.");
    private static final TextKey NOW_ONE = TextKey.of("jsc.crafting.view.now_one", "%s · %s of %s back");
    private static final TextKey NOW_MANY = TextKey.of("jsc.crafting.view.now_many", "%s jobs: %s");
    private static final TextKey NOW_DRAINING = TextKey.of("jsc.crafting.view.now_draining",
            "draining: what its last job fed is still coming out");
    private static final TextKey NOW_PAUSED = TextKey.of("jsc.crafting.view.now_paused", "paused");
    private static final TextKey NOW_IDLE = TextKey.of("jsc.crafting.view.now_idle", "idle");
    private static final String NONE = "-";

    private CraftingViews() {
    }

    /** What the window of the interface on {@code cable}'s face {@code face} shows. */
    public static InterfaceView of(final ServerLevel level, final BlockPos cable, final Direction face,
                                   final CraftingInterfacePart part) {
        final CraftingFloor floor = CraftingFloor.through(level, cable);
        final CraftingFloor.Site site = new CraftingFloor.Site(cable, face);
        final CraftingFloor.Reach reach = floor.reach(site);
        final List<Text> routers = new ArrayList<>();
        for (final CraftingFloor.Site router : reach.routers()) {
            final CraftingRouterPart routerPart = router.part(level, CraftingRouterPart.class);
            routers.add(routerPart == null || routerPart.name().isEmpty()
                    ? GameText.of(ComputingModule.CRAFTING_ROUTER_ITEM.get().getDescription())
                    : Text.literal(routerPart.name()));
        }
        final List<InterfaceView.PatternView> patterns = new ArrayList<>();
        for (final CraftingInterfacePart.HeldPattern held : part.patterns()) {
            patterns.add(patternView(level, held, reach));
        }
        final byte mode = switch (reach.mode()) {
            case DIRECT -> InterfaceView.DIRECT;
            case CABLE -> InterfaceView.CABLE;
            default -> InterfaceView.NONE;
        };
        final List<Text> warnings = warnings(level, floor, site, reach);
        final MainframeBlockEntity mainframe = mainframeOf(level, floor);
        final List<NetworkProcessingOperation> jobs = mainframe == null ? List.of() : mainframe.craftJobsOn(part.id());
        final Text now;
        final byte tone;
        if (part.paused()) {
            now = NOW_PAUSED.text();
            tone = InterfaceView.WARN;
        } else if (jobs.size() == 1) {
            final NetworkProcessingOperation job = jobs.get(0);
            now = NOW_ONE.with(CraftManagerPayloads.jobText(job), job.credited(0), job.cap(0));
            tone = InterfaceView.GOOD;
        } else if (!jobs.isEmpty()) {
            final List<Text> named = new ArrayList<>();
            jobs.forEach(job -> named.add(CraftManagerPayloads.jobText(job)));
            now = NOW_MANY.with(jobs.size(), TextLists.join(", ", named));
            tone = InterfaceView.GOOD;
        } else if (!part.owed().isEmpty()) {
            now = NOW_DRAINING.text();
            tone = InterfaceView.WARN;
        } else {
            now = NOW_IDLE.text();
            tone = InterfaceView.DIM;
        }
        return new InterfaceView(part.name(), shortId(part.id()), skinOf(level, cable), floor.drivenBy(site) != null,
                part.capacity(), patterns, routers, mode, CraftingDispatch.exclusive(part, reach),
                feedsText(level, reach, routers), receivingText(level, floor, site), part.paused(), part.maxJobs(),
                warnings, now, tone, part.log().entries(), part.marks());
    }

    private static InterfaceView.PatternView patternView(final ServerLevel level,
                                                         final CraftingInterfacePart.HeldPattern held,
                                                         final CraftingFloor.Reach reach) {
        final List<InterfaceView.InputView> inputs = new ArrayList<>();
        final List<StorageKey> seen = new ArrayList<>();
        boolean runs = reach.machine() != null;
        for (final ProcessingPattern pattern : processing(held)) {
            for (final ProcessingPattern.ProcessingInput in : pattern.inputs()) {
                if (seen.contains(in.key())) {
                    continue;
                }
                seen.add(in.key());
                final InterfaceRoutes.Route route = InterfaceRoutes.routeFor(level, held, in.key(), reach);
                final byte why = switch (route.why()) {
                    case CHOSEN -> InterfaceView.CHOSEN;
                    case FILTER -> InterfaceView.FILTER;
                    case ANY -> InterfaceView.ANY;
                    case NONE -> InterfaceView.NO_ROUTER;
                };
                if (reach.mode() == CraftingFloor.Mode.CABLE && route.router() == null) {
                    runs = false;
                }
                inputs.add(new InterfaceView.InputView(in.key(), in.amount(),
                        route.router() == null ? -1 : reach.routers().indexOf(route.router()), why));
            }
        }
        final StorageKey result = held.recipe().resultKey();
        final ItemStack icon = result != null && result.isItem() ? result.stack(1) : ItemStack.EMPTY;
        return new InterfaceView.PatternView(icon, held.recipe().displayText(), inputs, runs);
    }

    private static List<Text> warnings(final ServerLevel level, final CraftingFloor floor,
                                       final CraftingFloor.Site site, final CraftingFloor.Reach reach) {
        final List<Text> warnings = new ArrayList<>();
        if (reach.mode() == CraftingFloor.Mode.NONE) {
            warnings.add(FEEDS_NOTHING.text());
        }
        if (reach.reachesAt() != null) {
            warnings.add(REACHES_NETWORK.with(reach.reachesAt().getX(), reach.reachesAt().getY(),
                    reach.reachesAt().getZ()));
        }
        if (reach.sharedCable()) {
            warnings.add(SHARED_CABLE.text());
        }
        if (reach.severalMachines()) {
            warnings.add(SEVERAL_MACHINES.text());
        }
        if (reach.mode() == CraftingFloor.Mode.CABLE && reach.routers().isEmpty()) {
            warnings.add(NO_ROUTERS.text());
        }
        if (floor.computers().isEmpty()) {
            warnings.add(NO_COMPUTER.text());
        } else if (floor.drivenBy(site) == null) {
            warnings.add(NOT_DRIVEN.text());
        }
        if (reach.machine() != null && floor.receiversOf(site).isEmpty()) {
            warnings.add(NO_RECEIVING.text());
        }
        return warnings;
    }

    private static Text feedsText(final ServerLevel level, final CraftingFloor.Reach reach, final List<Text> routers) {
        final Text machine = reach.machine() == null ? NO_MACHINE_YET.text()
                : GameText.of(level.getBlockState(reach.machine()).getBlock().getName());
        return switch (reach.mode()) {
            case DIRECT -> FEEDS_DIRECT.with(machine);
            case CABLE -> FEEDS_CABLE.with(machine, routers.isEmpty() ? Text.literal(NONE)
                    : TextLists.join(", ", routers));
            default -> Text.literal(NONE);
        };
    }

    private static Text receivingText(final ServerLevel level, final CraftingFloor floor,
                                      final CraftingFloor.Site site) {
        final List<Text> names = new ArrayList<>();
        boolean byHand = false;
        for (final CraftingFloor.Site bus : floor.receiversOf(site)) {
            final ReceivingBusPart part = bus.part(level, ReceivingBusPart.class);
            if (part != null) {
                names.add(part.name().isEmpty() ? GameText.of(part.partItem().getHoverName())
                        : Text.literal(part.name()));
                byHand |= !part.tiedByHand().isEmpty();
            }
        }
        if (names.isEmpty()) {
            return Text.literal(NONE);
        }
        return (byHand ? RECEIVING_HAND : RECEIVING_SELF).with(TextLists.join(", ", names));
    }

    /** What the window of the router on {@code cable}'s face {@code face} shows. */
    public static CraftingView router(final ServerLevel level, final BlockPos cable, final Direction face,
                                      final CraftingRouterPart router) {
        final List<CraftingView.Line> lines = new ArrayList<>();
        final CraftingFloor.Site self = new CraftingFloor.Site(cable, face);
        final OwnCable own = ownCable(level, self);
        lines.add(line(CABLE_OF, own == null ? Text.literal(NONE)
                : CraftManagerPayloads.interfaceTitle(own.part(), own.site()), CABLE_OF_NOTE.text()));
        lines.add(feeds(level, self, router));
        if (own == null) {
            lines.add(box(CraftingView.BAD, OFF_CABLE.text()));
            return new CraftingView(lines, List.of(), List.of());
        }
        final List<CraftingView.Line> warnings = new ArrayList<>();
        boolean first = true;
        for (final CraftingInterfacePart.HeldPattern held : own.part().patterns()) {
            for (final ProcessingPattern pattern : processing(held)) {
                for (final ProcessingPattern.ProcessingInput in : pattern.inputs()) {
                    final InterfaceRoutes.Route route = InterfaceRoutes.routeFor(level, held, in.key(), own.reach());
                    if (!self.equals(route.router())) {
                        continue;
                    }
                    final Text routeText = ROUTE.with(GameText.of(in.key().displayName()), in.amount(),
                            pattern.displayText());
                    lines.add(first ? line(ROUTES, routeText, Text.EMPTY)
                            : new CraftingView.Line(CraftingView.LIST, Text.EMPTY, routeText, Text.EMPTY));
                    first = false;
                    if (route.why() == InterfaceRoutes.Why.CHOSEN && !router.filterKeys().isEmpty()
                            && !router.filterKeys().contains(in.key())) {
                        warnings.add(box(CraftingView.WARN, FILTER_REFUSES.with(GameText.of(in.key().displayName()),
                                pattern.displayText())));
                    }
                    final Text other = otherTaker(level, own.reach(), self, in.key());
                    if (other != null) {
                        warnings.add(box(CraftingView.INFO, ALSO_TAKEN.with(GameText.of(in.key().displayName()),
                                other, CraftManagerPayloads.interfaceTitle(own.part(), own.site()))));
                    }
                }
            }
        }
        if (first) {
            lines.add(line(ROUTES, NO_ROUTES.text(), Text.EMPTY));
        }
        lines.add(box(CraftingView.INFO, ROUTES_NOTE.text()));
        lines.addAll(warnings);
        return new CraftingView(lines, List.of(), List.of());
    }

    /** What the window of the Receiving Bus on {@code cable}'s face {@code face} shows. */
    public static CraftingView receiving(final ServerLevel level, final BlockPos cable, final Direction face,
                                         final ReceivingBusPart bus) {
        final CraftingFloor floor = CraftingFloor.through(level, cable);
        final MainframeBlockEntity mainframe = mainframeOf(level, floor);
        final CraftingFloor.Site self = new CraftingFloor.Site(cable, face);
        final List<CraftingFloor.Site> tied = floor.tiedTo(self);
        final List<CraftingView.Line> lines = new ArrayList<>();
        if (tied.isEmpty()) {
            lines.add(line(TIED_TO, Text.literal(NONE), Text.EMPTY));
            lines.add(box(CraftingView.BAD, NOT_TIED.text()));
        } else {
            final List<Text> names = new ArrayList<>();
            for (final CraftingFloor.Site site : tied) {
                final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
                if (part != null) {
                    names.add(CraftManagerPayloads.interfaceTitle(part, site));
                }
            }
            final boolean byHand = !bus.tiedByHand().isEmpty();
            lines.add(line(TIED_TO, TextLists.join(", ", names), (byHand ? BY_HAND : BY_ITSELF).text()));
            if (byHand && tied.size() > 1) {
                lines.add(box(CraftingView.INFO, ARRIVAL_ORDER.text()));
            }
        }
        lines.add(creditLine(level, tied, mainframe));
        boolean owes = false;
        for (final CraftingFloor.Site site : tied) {
            final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
            owes |= part != null && !part.owed().isEmpty();
        }
        if (owes) {
            lines.add(box(CraftingView.INFO, DRAINING.text()));
        }
        for (final CraftingLog.Entry entry : bus.log().entries()) {
            if (entry.kind() == CraftingLog.UNEXPECTED) {
                final StorageKey key = StorageKey.byName(entry.what());
                final Text what = key == null ? Text.literal(entry.what()) : GameText.of(key.displayName());
                lines.add(line(UNEXPECTED, UNEXPECTED_VALUE.with(entry.amount(), what), UNEXPECTED_NOTE.text()));
                break;
            }
        }
        return new CraftingView(lines, picks(level, floor, bus), bus.log().entries());
    }

    /** Whether the router on {@code cable}'s face {@code face} is on an interface's own cable, which uses it. */
    public static boolean routerLinked(final ServerLevel level, final BlockPos cable, final Direction face) {
        return ownCable(level, new CraftingFloor.Site(cable, face)) != null;
    }

    /** Whether the crafting cable at {@code cable} reaches a Crafting Computer. */
    public static boolean reachesComputer(final ServerLevel level, final BlockPos cable) {
        return !CraftingFloor.through(level, cable).computers().isEmpty();
    }

    /**
     * The era whose skin a crafting part's window wears: the Crafting Computer's that commands its network, the
     * Standard's when it reaches none.
     */
    public static HardwareEra skinOf(final ServerLevel level, final BlockPos cable) {
        for (final BlockPos pos : CraftingFloor.through(level, cable).computers()) {
            if (level.getBlockState(pos).getBlock() instanceof CraftingComputerBlock computer) {
                return computer.era();
            }
        }
        return HardwareEra.STANDARD;
    }

    /** The Mainframe of the network the Crafting Computers on {@code floor} are on, or null. */
    @Nullable
    public static MainframeBlockEntity mainframeOf(final ServerLevel level, final CraftingFloor floor) {
        for (final BlockPos pos : floor.computers()) {
            if (Loaded.blockEntity(level, pos) instanceof CraftingComputerBlockEntity computer
                    && computer.networkUuid() != null) {
                final MainframeBlockEntity mainframe = NetworkLookup.resolveMainframe(level, computer.networkUuid());
                if (mainframe != null) {
                    return mainframe;
                }
            }
        }
        return null;
    }

    /** The interfaces a Receiving Bus on {@code floor} can be tied to, in the order its window lists them. */
    public static List<UUID> pickIds(final ServerLevel level, final CraftingFloor floor) {
        final List<UUID> ids = new ArrayList<>();
        for (final CraftingFloor.Site site : floor.interfaces()) {
            final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
            if (part != null) {
                ids.add(part.id());
            }
        }
        return ids;
    }

    /** The first characters of an interface's id, which tell two of one name apart: "7c41-0a2e". */
    public static String shortId(final UUID id) {
        final String hex = id.toString().replace("-", "");
        return hex.substring(0, 4) + "-" + hex.substring(4, 8);
    }

    /** The word for a face of a block, "west". */
    public static Text faceWord(final Direction face) {
        return (switch (face) {
            case DOWN -> FACE_DOWN;
            case UP -> FACE_UP;
            case NORTH -> FACE_NORTH;
            case SOUTH -> FACE_SOUTH;
            case WEST -> FACE_WEST;
            case EAST -> FACE_EAST;
        }).text();
    }

    private static List<CraftingView.Pick> picks(final ServerLevel level, final CraftingFloor floor,
                                                 final ReceivingBusPart bus) {
        final List<CraftingView.Pick> picks = new ArrayList<>();
        for (final CraftingFloor.Site site : floor.interfaces()) {
            final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
            if (part != null) {
                picks.add(new CraftingView.Pick(CraftManagerPayloads.interfaceTitle(part, site), shortId(part.id()),
                        bus.tiedByHand().contains(part.id())));
            }
        }
        return picks;
    }

    /* What the oldest live job of the tied interfaces has had back. */
    private static CraftingView.Line creditLine(final ServerLevel level, final List<CraftingFloor.Site> tied,
                                                @Nullable final MainframeBlockEntity mainframe) {
        if (mainframe != null) {
            for (final CraftingFloor.Site site : tied) {
                final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
                if (part == null) {
                    continue;
                }
                for (final NetworkProcessingOperation job : mainframe.craftJobsOn(part.id())) {
                    return line(CREDIT, CraftManagerPayloads.jobText(job),
                            CREDIT_NOTE.with(job.credited(0), job.cap(0), job.lotsFed()));
                }
            }
        }
        return line(CREDIT, NO_JOB.text(), Text.EMPTY);
    }

    /* The face a router or a bus is against, and whether the machine takes anything there. */
    private static CraftingView.Line feeds(final ServerLevel level, final CraftingFloor.Site self,
                                           final CraftingRouterPart router) {
        final BlockPos machine = self.faced();
        final ExternalDataPort port = ExternalDataPort.at(level, machine, self.face().getOpposite());
        if (port.isEmpty()) {
            return line(FEEDS, Text.literal(NONE), NOTHING_THERE.text());
        }
        final Text where = MACHINE_FACE.with(GameText.of(level.getBlockState(machine).getBlock().getName()),
                faceWord(self.face().getOpposite()));
        boolean accepts = router.filterKeys().isEmpty();
        for (final StorageKey key : router.filterKeys()) {
            accepts |= port.insert(key, 1L, true) > 0L;
        }
        return new CraftingView.Line(accepts ? CraftingView.GOOD : CraftingView.PLAIN, FEEDS.text(), where,
                (accepts ? ACCEPTS : REFUSES).text());
    }

    /* Another router of the cable whose filter takes {@code key} too, by its name, or null. */
    @Nullable
    private static Text otherTaker(final ServerLevel level, final CraftingFloor.Reach reach,
                                   final CraftingFloor.Site self, final StorageKey key) {
        for (final CraftingFloor.Site site : reach.routers()) {
            final CraftingRouterPart other = site.part(level, CraftingRouterPart.class);
            if (!site.equals(self) && other != null && other.filterKeys().contains(key)) {
                return other.name().isEmpty() ? GameText.of(other.partItem().getHoverName())
                        : Text.literal(other.name());
            }
        }
        return null;
    }

    /* The interface whose own cable the router at {@code self} is on, or null. */
    @Nullable
    private static OwnCable ownCable(final ServerLevel level, final CraftingFloor.Site self) {
        /*
         * The router's cable is an interface's own: the interface sits on the network beside one of its blocks,
         * facing it. Each interface found so is asked whether this router is among its own.
         */
        final CraftingFloor own = CraftingFloor.through(level, self.cable());
        for (final BlockPos pos : own.cables()) {
            for (final Direction d : Direction.values()) {
                final BlockPos host = pos.relative(d);
                final CableBlockEntity cable = level.isLoaded(host) ? Cables.at(level, host) : null;
                if (cable == null || !(cable.getPart(d.getOpposite()) instanceof CraftingInterfacePart part)) {
                    continue;
                }
                final CraftingFloor.Site site = new CraftingFloor.Site(host, d.getOpposite());
                final CraftingFloor.Reach reach = CraftingFloor.through(level, host).reach(site);
                if (reach.routers().contains(self)) {
                    return new OwnCable(site, part, reach);
                }
            }
        }
        return null;
    }

    private static List<ProcessingPattern> processing(final CraftingInterfacePart.HeldPattern held) {
        final List<ProcessingPattern> out = new ArrayList<>();
        held.recipe().proc().ifPresent(out::add);
        held.recipe().multi().ifPresent(multi -> multi.stages().forEach(stage -> stage.proc().ifPresent(out::add)));
        return out;
    }

    private static CraftingView.Line line(final TextKey label, final Text value, final Text note) {
        return new CraftingView.Line(CraftingView.PLAIN, label.text(), value, note);
    }

    private static CraftingView.Line box(final byte tone, final Text text) {
        return new CraftingView.Line(tone, Text.EMPTY, text, Text.EMPTY);
    }

    /* The interface whose own cable a router is on. */
    private record OwnCable(CraftingFloor.Site site, CraftingInterfacePart part, CraftingFloor.Reach reach) {
    }
}
