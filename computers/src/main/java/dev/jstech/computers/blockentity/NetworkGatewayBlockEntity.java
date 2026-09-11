/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.gateway.GatewayLog;
import dev.jstech.computers.gateway.GatewayName;
import dev.jstech.computers.gateway.GatewayPermissions;
import dev.jstech.computers.gateway.GatewayStats;
import dev.jstech.computers.gateway.IGatewayBridge;
import dev.jstech.computers.gateway.NetworkGateways;
import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.peripheral.ILinkResult;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLinkValidator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

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
public class NetworkGatewayBlockEntity extends BlockEntity implements IPeripheralEndpoint {

    public static final int BUFFER_SLOTS = 9;
    /** How long the lights blink after Identify, in ticks. */
    private static final int IDENTIFY_TICKS = 60;
    private static final int BLINK_TICKS = 5;

    private static final String NBT_LINKED_OWNER = "LinkedOwner";
    private static final String NBT_LINK_LENGTH = "LinkLength";
    private static final String NBT_NAME = "Name";
    private static final String NBT_READ = "Read";
    private static final String NBT_OPERATIONS = "Operations";
    private static final String NBT_FILES = "Files";
    private static final String NBT_CEILING = "Ceiling";
    private static final String NBT_CAP = "CallCap";
    private static final String NBT_LOG = "Log";
    private static final String NBT_BUFFER = "Buffer";
    private static final String NBT_HOST_NAME = "HostName";
    private static final String NBT_CC_ONLINE = "CcOnline";
    private static final String NBT_WHEN = "When";
    private static final String NBT_WHO = "Who";
    private static final String NBT_WHAT = "What";
    private static final String NBT_RESULT = "Result";
    private static final String NBT_TONE = "Tone";

    /** A ComputerCraft computer this Gateway is attached to, and when it was last heard from. */
    public record AttachedComputer(int id, long lastSeen) {
    }

    @Nullable
    private Long linkedOwner;
    private int linkLength = -1;
    private String name = "";
    private GatewayPermissions permissions = GatewayPermissions.DEFAULT;
    private final GatewayLog log = new GatewayLog();
    private final GatewayStats stats = new GatewayStats();
    private long identifyUntil = -1L;
    @Nullable
    private IGatewayBridge bridge;
    private String publishedAs = "";
    private final Map<Integer, Long> attached = new LinkedHashMap<>();
    /* What the client knows of the server side, for the block's own screen. */
    private String clientHostName = "";
    private boolean clientCcOnline;

    private final ItemStackHandler buffer = new ItemStackHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(final int slot) {
            setChanged();
        }
    };

    public NetworkGatewayBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.NETWORK_GATEWAY_BE.get(), pos, state);
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
        return PeripheralCableType.COMPUTING;
    }

    @Override
    public Optional<Long> linkedOwner() {
        return Optional.ofNullable(linkedOwner);
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        linkedOwner = ownerPos;
        setChanged();
    }

    @Override
    public void onOwnerUnlinked() {
        linkedOwner = null;
        linkLength = -1;
        setChanged();
    }

    /** The host computer's position, or null while not linked. */
    @Nullable
    public BlockPos ownerPos() {
        return linkedOwner == null ? null : BlockPos.of(linkedOwner);
    }

    /** The host as a peripheral owner, or null while not linked or when it is gone. */
    @Nullable
    public IPeripheralOwner owner() {
        return level != null && linkedOwner != null
                && level.getBlockEntity(BlockPos.of(linkedOwner)) instanceof IPeripheralOwner owner ? owner : null;
    }

    public boolean online() {
        return linkedOwner != null;
    }

    /** How the Gateway reaches its host, for the status lines. */
    public String linkKind() {
        if (linkedOwner == null) {
            return "not linked";
        }
        return linkLength <= 0 ? "adjacent" : "cable, " + linkLength + (linkLength == 1 ? " block" : " blocks");
    }

    /** The host computer's name, or what the client last heard it was. */
    public String hostName() {
        if (level != null && level.isClientSide()) {
            return clientHostName;
        }
        final IPeripheralOwner owner = owner();
        if (owner instanceof IOsHost host && host.console() != null) {
            return host.console().computerName();
        }
        return owner == null ? "" : "host";
    }

    // Name, permissions, log, stats

    /** The Gateway's name: the one it was given, or the default it took when it first linked. */
    public String name() {
        return name.isEmpty() ? GatewayName.UNNAMED : name;
    }

    /**
     * Renames the Gateway to what {@code typed} cleans down to; an empty name goes back to the default this
     * Gateway took when it linked. Says what it did, for the shell and the manager's status line.
     */
    public String rename(final String typed, final String by) {
        final String cleaned = GatewayName.clean(typed);
        final String was = name();
        name = cleaned.isEmpty() ? defaultName() : cleaned;
        setChanged();
        syncToClients();
        logged(by, "rename " + was + " to " + name(), "ok", GatewayLog.Tone.OK);
        return "renamed " + was + " to " + name();
    }

    /** Makes the block's lights blink for a few seconds, so this Gateway stands out among several. */
    public void identify(final String by) {
        if (level != null) {
            identifyUntil = level.getGameTime() + IDENTIFY_TICKS;
            logged(by, "identify", "blinking", GatewayLog.Tone.OK);
        }
    }

    public boolean identifying() {
        return level != null && level.getGameTime() < identifyUntil;
    }

    public GatewayPermissions permissions() {
        return permissions;
    }

    public void setPermissions(final GatewayPermissions value, final String by, final String what) {
        permissions = value;
        setChanged();
        logged(by, what, "ok", GatewayLog.Tone.OK);
    }

    public GatewayLog log() {
        return log;
    }

    public GatewayStats stats() {
        return stats;
    }

    /** Records something the Gateway did, stamped with the world's clock. */
    public void logged(final String who, final String what, final String result, final GatewayLog.Tone tone) {
        log.add(level == null ? 0L : level.getDayTime(), who, what, result, tone);
        setChanged();
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

    /** Drops what the buffer holds into the world, when the block is broken. */
    public void dropContents(final Level level, final BlockPos pos) {
        for (int i = 0; i < buffer.getSlots(); i++) {
            final ItemStack held = buffer.getStackInSlot(i);
            if (!held.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), held);
                buffer.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // The ComputerCraft side

    /** The bridge to ComputerCraft, or null without CC: Tweaked (or before the block joined the world). */
    @Nullable
    public IGatewayBridge bridge() {
        return bridge;
    }

    /** Whether a ComputerCraft computer is attached or the wired network has anything on it. */
    public boolean ccOnline() {
        if (level != null && level.isClientSide()) {
            return clientCcOnline;
        }
        return !attached.isEmpty() || (bridge != null && bridge.onWire());
    }

    /** Called by the peripheral when a ComputerCraft computer attaches to this Gateway. */
    public void ccAttached(final int computerId) {
        attached.put(computerId, level == null ? 0L : level.getGameTime());
        syncToClients();
    }

    public void ccDetached(final int computerId) {
        attached.remove(computerId);
        syncToClients();
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

    // Ticking

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final NetworkGatewayBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tick(serverLevel);
        }
    }

    private void tick(final ServerLevel level) {
        final long self = worldPosition.asLong();
        final long socket = socketPos().asLong();
        final PeripheralLinkValidator validator = PeripheralLinks.validator(level);
        if (linkedOwner == null) {
            /*
             * Only the back socket carries the link: a computer or a cable against any other face is
             * ignored, so the front stays ComputerCraft's and the block reads the way it looks.
             */
            PeripheralLinks.discoverOwnerThrough(level, self, socket).ifPresent(ownerPos -> {
                if (validator.tryEstablishLink(ownerPos, self) instanceof ILinkResult.Established made) {
                    linkLength = made.pathLength();
                    if (name.isEmpty()) {
                        name = defaultName();
                    }
                    logged(hostName(), "link", linkKind(), GatewayLog.Tone.OK);
                    syncToClients();
                }
            });
        } else if (!(level.getBlockEntity(BlockPos.of(linkedOwner)) instanceof IPeripheralOwner)
                || !validator.isLinkStillValid(linkedOwner, self, PeripheralCableType.COMPUTING)
                || !PeripheralLinks.socketReaches(level, socket, linkedOwner, PeripheralCableType.COMPUTING)) {
            final String was = hostName();
            unlink(level);
            logged(was, "link lost", "the cable or the computer is gone", GatewayLog.Tone.DENIED);
            syncToClients();
        }
        if (bridge != null && !peripheralName().equals(publishedAs)) {
            bridge.publish(peripheralName());
            publishedAs = peripheralName();
        }
        final boolean lit = identifying()
                ? (level.getGameTime() / BLINK_TICKS) % 2 == 0
                : online();
        final BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(NetworkGatewayBlock.LIT) && state.getValue(NetworkGatewayBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(NetworkGatewayBlock.LIT, lit), Block.UPDATE_CLIENTS);
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

    /** Breaks the link from this side, telling the host so it frees the port. Harmless when unlinked. */
    public void unlink(final ServerLevel level) {
        if (linkedOwner != null
                && level.getBlockEntity(BlockPos.of(linkedOwner)) instanceof IPeripheralOwner owner) {
            owner.onEndpointUnlinked(worldPosition.asLong());
        }
        onOwnerUnlinked();
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

    // Persistence

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedOwner != null) {
            tag.putLong(NBT_LINKED_OWNER, linkedOwner);
        }
        tag.putInt(NBT_LINK_LENGTH, linkLength);
        tag.putString(NBT_NAME, name);
        tag.putBoolean(NBT_READ, permissions.read());
        tag.putBoolean(NBT_OPERATIONS, permissions.operations());
        tag.putInt(NBT_FILES, permissions.filesIndex());
        tag.putInt(NBT_CEILING, permissions.ceilingIndex());
        tag.putInt(NBT_CAP, permissions.capIndex());
        final ListTag entries = new ListTag();
        final List<GatewayLog.Entry> newestFirst = log.entries();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            final GatewayLog.Entry e = newestFirst.get(i);
            final CompoundTag one = new CompoundTag();
            one.putLong(NBT_WHEN, e.dayTime());
            one.putString(NBT_WHO, e.who());
            one.putString(NBT_WHAT, e.what());
            one.putString(NBT_RESULT, e.result());
            one.putInt(NBT_TONE, e.tone().ordinal());
            entries.add(one);
        }
        tag.put(NBT_LOG, entries);
        tag.put(NBT_BUFFER, buffer.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linkedOwner = tag.contains(NBT_LINKED_OWNER) ? tag.getLong(NBT_LINKED_OWNER) : null;
        linkLength = tag.contains(NBT_LINK_LENGTH) ? tag.getInt(NBT_LINK_LENGTH) : -1;
        name = tag.getString(NBT_NAME);
        if (tag.contains(NBT_READ)) {
            permissions = GatewayPermissions.of(tag.getBoolean(NBT_READ), tag.getBoolean(NBT_OPERATIONS),
                    tag.getInt(NBT_FILES), tag.getInt(NBT_CEILING), tag.getInt(NBT_CAP));
        }
        if (tag.contains(NBT_LOG)) {
            final List<GatewayLog.Entry> oldestFirst = new ArrayList<>();
            for (final Tag raw : tag.getList(NBT_LOG, Tag.TAG_COMPOUND)) {
                final CompoundTag one = (CompoundTag) raw;
                oldestFirst.add(new GatewayLog.Entry(one.getLong(NBT_WHEN), one.getString(NBT_WHO),
                        one.getString(NBT_WHAT), one.getString(NBT_RESULT), GatewayLog.Tone.at(one.getInt(NBT_TONE))));
            }
            log.restore(oldestFirst);
        }
        if (tag.contains(NBT_BUFFER)) {
            buffer.deserializeNBT(registries, tag.getCompound(NBT_BUFFER));
        }
        // What only the server knows, handed to the client with the update tag.
        if (tag.contains(NBT_HOST_NAME)) {
            clientHostName = tag.getString(NBT_HOST_NAME);
        }
        if (tag.contains(NBT_CC_ONLINE)) {
            clientCcOnline = tag.getBoolean(NBT_CC_ONLINE);
        }
    }

    // Client sync

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        if (linkedOwner != null) {
            tag.putLong(NBT_LINKED_OWNER, linkedOwner);
        }
        tag.putInt(NBT_LINK_LENGTH, linkLength);
        tag.putString(NBT_NAME, name);
        tag.putString(NBT_HOST_NAME, hostName());
        tag.putBoolean(NBT_CC_ONLINE, ccOnline());
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            final BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof NetworkGatewayBlock) {
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }
}
