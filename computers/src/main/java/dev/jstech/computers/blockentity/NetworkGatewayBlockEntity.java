/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.gateway.GatewayLog;
import dev.jstech.computers.gateway.GatewayName;
import dev.jstech.computers.gateway.GatewayPermissions;
import dev.jstech.computers.gateway.GatewayService;
import dev.jstech.computers.gateway.GatewayStats;
import dev.jstech.computers.gateway.IGatewayBridge;
import dev.jstech.computers.gateway.NetworkGateways;
import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.vm.system.SigmaCosts;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.peripheral.ILinkResult;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import dev.jstech.core.peripheral.PeripheralLinkValidator;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextTags;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Network Gateway: the device that puts this data network within reach of ComputerCraft's computers.
 * A peripheral of one of our computers (the host), linked through the socket on its back face by a
 * peripheral cable or by standing against the host; on its front face it is a ComputerCraft peripheral
 * and a node of CC's wired network. It keeps a name, the permissions the host grants the other side, a
 * log of what it did, a buffer of nine slots the items it moves pass through, and the tallies of what it
 * served this minute.
 *
 * <p>Everything about it is configured from the host, in the Gateway Manager or at the shell. Its own
 * screen shows only the buffer and a status line. Without CC: Tweaked the block still links and holds
 * items; its ComputerCraft side simply never comes up.
 */
@TextHolder
public class NetworkGatewayBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    /** How many cable blocks lie between the Gateway and its host; -1 while not linked, 0 standing against it. */
    private final IntField linkLength = fields().integer("LinkLength", -1).save().toClient();
    private final ValueField<String> name = fields().value("Name", Codec.STRING, "").save().toClient();
    private GatewayPermissions permissions = GatewayPermissions.DEFAULT;
    private final PartField permissionsPart = fields().part("Permissions", new PermissionsPart()).save();
    private final GatewayLog log = new GatewayLog();
    private final PartField logPart = fields().part("Log", new LogPart()).save();
    /*
     * The buffer is reached from the sides, the top and the bottom only, so it is offered to the world by the
     * Gateway's own side-by-side capability rather than exposed on every face.
     */
    private final FieldItemHandler buffer = fields().items("Buffer", BUFFER_SLOTS).save().dropsWhenBroken();
    /** The host's name as the client last heard it; the server works it out each tick. */
    private final ValueField<String> hostSeen = fields().value("HostName", Codec.STRING, "").toClient();
    private final Map<Integer, Long> attached = new LinkedHashMap<>();
    @Nullable
    private IGatewayBridge bridge;
    private final DerivedInt ccOnline = fields().derived("CcOnline",
            () -> !attached.isEmpty() || (bridge != null && bridge.onWire())).toClient();
    private final GatewayStats stats = new GatewayStats();
    private long identifyUntil = -1L;
    private String publishedAs = "";
    /* The other side's use of the host's tick: calls and credits this tick, and the last second of credits. */
    private long tickSeen = Long.MIN_VALUE;
    private int callsThisTick;
    private int spentThisTick;
    private final int[] spent = new int[BUDGET_TICKS];
    private int spentAt;
    /** What each attached computer watches: by computer id, the name and the total it last heard. */
    private final Map<Integer, Map<String, Long>> watches = new LinkedHashMap<>();
    /**
     * What ComputerCraft computers have said to this side and nobody has read yet.
     *
     * <p>A message waits here for the programs on the host machine to be handed it on the next tick, and
     * no longer: one nobody is listening for is dropped rather than piling up for ever.
     */
    private final Deque<Message> messages = new ArrayDeque<>();

    public static final int BUFFER_SLOTS = 9;

    private static final TextKey NOT_LINKED = TextKey.of("jsc.gateway.not_linked", "not linked");
    private static final TextKey ADJACENT = TextKey.of("jsc.gateway.adjacent", "adjacent");
    private static final TextKey CABLE_ONE_BLOCK = TextKey.of("jsc.gateway.cable_one_block", "cable, %s block");
    private static final TextKey CABLE_BLOCKS = TextKey.of("jsc.gateway.cable_blocks", "cable, %s blocks");
    private static final TextKey RENAMED = TextKey.of("jsc.gateway.renamed", "renamed %s to %s");
    /* What the log records of the Gateway's own doings, and how they went. */
    private static final TextKey LOG_RENAME = TextKey.of("jsc.gateway.log.rename", "rename %s to %s");
    private static final TextKey LOG_IDENTIFY = TextKey.of("jsc.gateway.log.identify", "identify");
    private static final TextKey LOG_BLINKING = TextKey.of("jsc.gateway.log.blinking", "blinking");
    private static final TextKey LOG_LINK = TextKey.of("jsc.gateway.log.link", "link");
    private static final TextKey LOG_LINK_LOST = TextKey.of("jsc.gateway.log.link_lost", "link lost");
    private static final TextKey LOG_LINK_GONE =
            TextKey.of("jsc.gateway.log.link_gone", "the cable or the computer is gone");
    private static final TextKey LOG_OK = TextKey.of("jsc.gateway.log.ok", "ok");
    private static final TextKey LOG_SAID = TextKey.of("jsc.gateway.log.said", "said");
    /** How long the lights blink after Identify, in ticks. */
    private static final int IDENTIFY_TICKS = 60;
    private static final int BLINK_TICKS = 5;
    private static final int BUDGET_TICKS = 20;
    private static final int WATCH_EVERY = 20;
    /** The most messages that wait to be read; a side nobody listens to does not grow for ever. */
    private static final int MESSAGES_KEPT = 64;

    private static final String NBT_READ = "Read";
    private static final String NBT_OPERATIONS = "Operations";
    private static final String NBT_CEILING = "Ceiling";
    private static final String NBT_CAP = "CallCap";
    private static final String NBT_LOG = "Log";
    private static final String NBT_WHEN = "When";
    private static final String NBT_WHO = "Who";
    private static final String NBT_WHAT = "What";
    private static final String NBT_RESULT = "Result";
    private static final String NBT_TONE = "Tone";

    public NetworkGatewayBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.NETWORK_GATEWAY_BE.get(), pos, state);
        fields().mirror(NetworkGatewayBlock.LIT, this::lampsLit);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final NetworkGatewayBlockEntity gateway) {
        if (level instanceof ServerLevel server) {
            gateway.tick(server);
        }
    }

    // The two sockets

    /** The direction the front (the ComputerCraft side) faces. */
    public Direction facing() {
        return getBlockState().hasProperty(NetworkGatewayBlock.FACING)
                ? getBlockState().getValue(NetworkGatewayBlock.FACING) : Direction.NORTH;
    }

    /** The block behind the Gateway, where the peripheral cable plugs in. */
    public BlockPos socketPos() {
        return worldPosition.relative(facing().getOpposite());
    }

    // IPeripheralEndpoint

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
        if (!messages.isEmpty()) {
            tellHostMailWaits();
        }
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
        linkLength.set(-1);
    }

    /** The host computer's position, or null while not linked. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /**
     * The host as a peripheral owner, or null while not linked, when it is gone, or while its chunk is not loaded:
     * asked every tick, it must never load the host's chunk back.
     */
    @Nullable
    public IPeripheralOwner owner() {
        final BlockPos at = link.ownerPos();
        return level != null && at != null && level.isLoaded(at)
                && level.getBlockEntity(at) instanceof IPeripheralOwner owner ? owner : null;
    }

    public boolean online() {
        return link.linkedOwner().isPresent();
    }

    /** How the Gateway reaches its host, for the status lines. */
    public Text linkKind() {
        if (!online()) {
            return NOT_LINKED.text();
        }
        final int length = linkLength.get();
        return length <= 0 ? ADJACENT.text() : (length == 1 ? CABLE_ONE_BLOCK : CABLE_BLOCKS).with(length);
    }

    /** The host computer's name, or what the client last heard it was. */
    public String hostName() {
        return level != null && level.isClientSide() ? hostSeen.get() : workOutHostName();
    }

    /** Breaks the link from this side, telling the host so it frees the port. Harmless when unlinked. */
    public void unlink(final ServerLevel level) {
        link.unlink(level, worldPosition);
        linkLength.set(-1);
    }

    // Name, permissions, log, stats

    /** The Gateway's name: the one it was given, or the default it took when it first linked. */
    public String name() {
        return name.get().isEmpty() ? GatewayName.UNNAMED : name.get();
    }

    /**
     * Renames the Gateway to what {@code typed} cleans down to; an empty name goes back to the default this
     * Gateway took when it linked. Says what it did, for the shell and the manager's status line.
     */
    public Text rename(final String typed, final String by) {
        final String cleaned = GatewayName.clean(typed);
        final String was = name();
        name.set(cleaned.isEmpty() ? defaultName() : cleaned);
        logged(by, LOG_RENAME.with(was, name()), LOG_OK.text(), GatewayLog.Tone.OK);
        return RENAMED.with(was, name());
    }

    /** Makes the block's lights blink for a few seconds, so this Gateway stands out among several. */
    public void identify(final String by) {
        if (level != null) {
            identifyUntil = level.getGameTime() + IDENTIFY_TICKS;
            logged(by, LOG_IDENTIFY.text(), LOG_BLINKING.text(), GatewayLog.Tone.OK);
        }
    }

    public boolean identifying() {
        return level != null && level.getGameTime() < identifyUntil;
    }

    public GatewayPermissions permissions() {
        return permissions;
    }

    public void setPermissions(final GatewayPermissions value, final String by, final Text what) {
        permissions = value;
        permissionsPart.changed();
        logged(by, what, LOG_OK.text(), GatewayLog.Tone.OK);
    }

    public GatewayLog log() {
        return log;
    }

    public GatewayStats stats() {
        return stats;
    }

    /** Records something the Gateway did, stamped with the world's clock. */
    public void logged(final Text who, final Text what, final Text result, final GatewayLog.Tone tone) {
        log.add(level == null ? 0L : level.getDayTime(), who, what, result, tone);
        logPart.changed();
    }

    /** The same, asked by a computer that goes by its name or its id, which read the same in every language. */
    public void logged(final String who, final Text what, final Text result, final GatewayLog.Tone tone) {
        logged(Text.literal(who), what, result, tone);
    }

    /** Records a request as it was asked, which reads the same in every language, and how it went. */
    public void logged(final String who, final String asked, final Text result, final GatewayLog.Tone tone) {
        logged(Text.literal(who), Text.literal(asked), result, tone);
    }

    /** Records that a ComputerCraft computer said something to this side. */
    public void loggedSaid(final String who) {
        logged(who, "send", LOG_SAID.text(), GatewayLog.Tone.OK);
    }

    /** The name ComputerCraft computers wrap this Gateway by. */
    public String peripheralName() {
        return GatewayName.peripheralName(name());
    }

    // The buffer

    public ItemStackHandler buffer() {
        return buffer;
    }

    /**
     * The buffer as the world sees it: reachable from the sides, the top and the bottom, where a chest, a
     * hopper or a turtle can take from it and feed it; the two socket faces carry only their cables.
     */
    @Nullable
    public IItemHandler bufferFor(@Nullable final Direction side) {
        if (side == null) {
            return buffer;
        }
        return side == facing() || side == facing().getOpposite() ? null : buffer;
    }

    /** How many of the nine slots hold something. */
    public int bufferUsed() {
        int used = 0;
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    // The ComputerCraft side

    /** Takes what a ComputerCraft computer said to this side, for the host machine's programs to read. */
    public void said(final int from, final String text, final long tick) {
        JscEvents.awardOperator(this, JscEvents.COMPUTERCRAFT_MESSAGE);
        while (messages.size() >= MESSAGES_KEPT) {
            messages.removeFirst();
        }
        messages.addLast(new Message(from, text == null ? "" : text, tick));
        tellHostMailWaits();
    }

    /** Everything said to this side since the last time anyone asked, oldest first. */
    public List<Message> takeMessages() {
        if (messages.isEmpty()) {
            return List.of();
        }
        final List<Message> said = List.copyOf(messages);
        messages.clear();
        return said;
    }

    /** The bridge to ComputerCraft, or null without CC: Tweaked (or before the block joined the world). */
    @Nullable
    public IGatewayBridge bridge() {
        return bridge;
    }

    /** Whether a ComputerCraft computer is attached or the wired network has anything on it. */
    public boolean ccOnline() {
        return ccOnline.isSet();
    }

    /** Called by the peripheral when a ComputerCraft computer attaches to this Gateway. */
    public void ccAttached(final int computerId) {
        attached.put(computerId, level == null ? 0L : level.getGameTime());
    }

    public void ccDetached(final int computerId) {
        attached.remove(computerId);
        watches.remove(computerId);
    }

    /** Queues an event on one attached computer; false without the mod or once it has gone. */
    public boolean eventTo(final int computerId, final String event, final Object... arguments) {
        return bridge != null && bridge.eventTo(computerId, event, arguments);
    }

    // The other side's use of the host

    /** Counts a call from the other side against this tick's cap; false once the cap is spent. */
    public boolean admit() {
        rollTick();
        if (callsThisTick >= permissions.callCap()) {
            return false;
        }
        callsThisTick++;
        stats.count(GatewayStats.Kind.CALL, level == null ? 0L : level.getGameTime());
        return true;
    }

    /**
     * Charges the host for work done on the other side's behalf: its programs get that much less of the
     * next tick, so a ComputerCraft computer hammering the bridge slows the host, not the server.
     */
    public void charge(final int credits) {
        rollTick();
        spentThisTick += Math.max(0, credits);
        if (owner() instanceof AbstractComputerBlockEntity host) {
            host.programs().owe(credits);
        }
    }

    /** How much of the host's tick the other side has been taking over the last second, per mille. */
    public int budgetPermille() {
        if (!(owner() instanceof AbstractComputerBlockEntity host)) {
            return 0;
        }
        final int credits = host.sigmaCredits();
        if (credits <= 0) {
            return 0;
        }
        long sum = spentThisTick;
        for (final int tick : spent) {
            sum += tick;
        }
        return (int) Math.min(1000L, sum * 1000L / ((long) credits * (BUDGET_TICKS + 1)));
    }

    // Watches

    /** Remembers that {@code computer} wants to hear when the total of {@code name} moves from {@code total}. */
    public void watch(final int computer, final String name, final long total) {
        watches.computeIfAbsent(computer, k -> new LinkedHashMap<>()).put(name, total);
    }

    /** Forgets a watch; whether there was one. */
    public boolean unwatch(final int computer, final String name) {
        final Map<String, Long> mine = watches.get(computer);
        if (mine == null) {
            return false;
        }
        final boolean was = mine.remove(name) != null;
        if (mine.isEmpty()) {
            watches.remove(computer);
        }
        return was;
    }

    /** What {@code computer} watches, with the totals it last heard. */
    public Map<String, Long> watchesOf(final int computer) {
        return new LinkedHashMap<>(watches.getOrDefault(computer, Map.of()));
    }

    /** Notes that computer {@code computerId} just called, for the "last seen" column. */
    public void ccSeen(final int computerId) {
        attached.put(computerId, level == null ? 0L : level.getGameTime());
    }

    /** The ComputerCraft computers attached right now, in the order they came. */
    public List<AttachedComputer> attachedComputers() {
        final List<AttachedComputer> out = new ArrayList<>(attached.size());
        attached.forEach((id, seen) -> out.add(new AttachedComputer(id, seen)));
        return out;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && bridge == null && ComputerCraftIntegration.isLoaded()) {
            bridge = ComputerCraftIntegration.bridge(this);
            publishedAs = "";
            level.invalidateCapabilities(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (bridge != null) {
            bridge.remove();
            bridge = null;
            publishedAs = "";
        }
    }

    private void tick(final ServerLevel level) {
        rollTick();
        final long self = worldPosition.asLong();
        final long socket = socketPos().asLong();
        final PeripheralLinkValidator validator = PeripheralLinks.validator(level);
        final Long owner = link.linkedOwner().orElse(null);
        if (owner == null) {
            /*
             * Only the back socket carries the link: a computer or a cable against any other face is
             * ignored, so the front stays ComputerCraft's and the block reads the way it looks.
             */
            PeripheralLinks.discoverOwnerThrough(level, self, socket).ifPresent(ownerPos -> {
                if (validator.tryEstablishLink(ownerPos, self) instanceof ILinkResult.Established made) {
                    linkLength.set(made.pathLength());
                    if (name.get().isEmpty()) {
                        name.set(defaultName());
                    }
                    logged(hostName(), LOG_LINK.text(), linkKind(), GatewayLog.Tone.OK);
                }
            });
        } else if (!(Loaded.blockEntity(level, BlockPos.of(owner)) instanceof IPeripheralOwner)
                || !validator.isLinkStillValid(owner, self, PeripheralCableType.COMPUTING)
                || !PeripheralLinks.socketReaches(level, socket, owner, PeripheralCableType.COMPUTING)) {
            final String was = hostName();
            unlink(level);
            logged(was, LOG_LINK_LOST.text(), LOG_LINK_GONE.text(), GatewayLog.Tone.DENIED);
        }
        hostSeen.set(workOutHostName());
        if (bridge != null && !peripheralName().equals(publishedAs)) {
            bridge.publish(peripheralName());
            publishedAs = peripheralName();
        }
        if (!watches.isEmpty() && online() && level.getGameTime() % WATCH_EVERY == 0L) {
            tickWatches();
        }
    }

    /* The status lights: on while linked, blinking for a few seconds after Identify. */
    private boolean lampsLit() {
        return identifying() && level != null ? (level.getGameTime() / BLINK_TICKS) % 2 == 0 : online();
    }

    /* The host computer's name, from the host itself. */
    private String workOutHostName() {
        final IPeripheralOwner owner = owner();
        if (owner instanceof IOsHost host && host.console() != null) {
            return host.console().computerName();
        }
        return owner == null ? "" : "host";
    }

    /**
     * Lets the linked host know something waits here, so it looks for its Gateways on its next tick instead of on
     * every tick. A host whose chunk is not loaded looks once when it loads anyway.
     */
    private void tellHostMailWaits() {
        final BlockPos host = link.ownerPos();
        if (level != null && host != null && level.isLoaded(host)
                && level.getBlockEntity(host) instanceof AbstractComputerBlockEntity machine) {
            machine.gatewayMailWaits();
        }
    }

    private void rollTick() {
        final long now = level == null ? 0L : level.getGameTime();
        if (now != tickSeen) {
            spent[spentAt] = spentThisTick;
            spentAt = (spentAt + 1) % BUDGET_TICKS;
            spentThisTick = 0;
            callsThisTick = 0;
            tickSeen = now;
        }
    }

    /**
     * Looks every watched name up once a second and tells the computer that asked when a total moved: on
     * the change, not for as long as it stays changed. Each look costs the host a read.
     */
    private void tickWatches() {
        for (final Map.Entry<Integer, Map<String, Long>> byComputer : watches.entrySet()) {
            for (final Map.Entry<String, Long> watched : byComputer.getValue().entrySet()) {
                final long total = GatewayService.stockOf(this, watched.getKey());
                charge(SigmaCosts.READ);
                if (total != watched.getValue()) {
                    final long previous = watched.getValue();
                    watched.setValue(total);
                    eventTo(byComputer.getKey(), GatewayService.EVENT_STOCK, watched.getKey(), total, previous);
                }
            }
        }
    }

    /** The default name for this Gateway: its number among the host's Gateways. */
    private String defaultName() {
        final IPeripheralOwner owner = owner();
        if (owner == null || level == null) {
            return GatewayName.defaultFor(1);
        }
        final List<NetworkGatewayBlockEntity> siblings = NetworkGateways.linkedTo(level, owner);
        int ordinal = siblings.indexOf(this) + 1;
        if (ordinal <= 0) {
            ordinal = siblings.size() + 1;
        }
        return GatewayName.defaultFor(ordinal);
    }

    /** A ComputerCraft computer this Gateway is attached to, and when it was last heard from. */
    public record AttachedComputer(int id, long lastSeen) {
    }

    /** Something a ComputerCraft computer said to this side: which computer, what it said, and when. */
    public record Message(int from, String text, long tick) {
    }

    /** What the host lets the other side do, saved as four plain keys. */
    private final class PermissionsPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            tag.putBoolean(NBT_READ, permissions.read());
            tag.putBoolean(NBT_OPERATIONS, permissions.operations());
            tag.putInt(NBT_CEILING, permissions.ceilingIndex());
            tag.putInt(NBT_CAP, permissions.capIndex());
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            if (tag.contains(NBT_READ)) {
                permissions = GatewayPermissions.of(tag.getBoolean(NBT_READ), tag.getBoolean(NBT_OPERATIONS),
                        tag.getInt(NBT_CEILING), tag.getInt(NBT_CAP));
            }
        }
    }

    /** The log, saved oldest first so it reads back in the order it was written. */
    private final class LogPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final ListTag entries = new ListTag();
            final List<GatewayLog.Entry> newestFirst = log.entries();
            for (int i = newestFirst.size() - 1; i >= 0; i--) {
                final GatewayLog.Entry entry = newestFirst.get(i);
                final CompoundTag one = new CompoundTag();
                one.putLong(NBT_WHEN, entry.dayTime());
                one.put(NBT_WHO, TextTags.write(entry.who()));
                one.put(NBT_WHAT, TextTags.write(entry.what()));
                one.put(NBT_RESULT, TextTags.write(entry.result()));
                one.putInt(NBT_TONE, entry.tone().id());
                entries.add(one);
            }
            tag.put(NBT_LOG, entries);
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            if (!tag.contains(NBT_LOG)) {
                return;
            }
            final List<GatewayLog.Entry> oldestFirst = new ArrayList<>();
            for (final Tag raw : tag.getList(NBT_LOG, Tag.TAG_COMPOUND)) {
                final CompoundTag one = (CompoundTag) raw;
                oldestFirst.add(new GatewayLog.Entry(one.getLong(NBT_WHEN), TextTags.read(one.getCompound(NBT_WHO)),
                        TextTags.read(one.getCompound(NBT_WHAT)), TextTags.read(one.getCompound(NBT_RESULT)),
                        GatewayLog.Tone.byId(one.getInt(NBT_TONE))));
            }
            log.restore(oldestFirst);
        }
    }
}
