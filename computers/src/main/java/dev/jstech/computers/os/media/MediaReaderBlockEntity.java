/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.audio.MediaBaySounds;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.client.audio.MachineSoundSources;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.SetupJob;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.storage.ServerStorageContents;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

/**
 * The block entity of a {@link MediaReaderBlock}: one slot holding a {@link MediaItem} of a format the drive reads,
 * whose payload, kind and data a linked computer reads through {@link #insertedPayload()}, {@link #insertedKind()}
 * and {@link #insertedData()}.
 *
 * <p>It is a peripheral on the computing cable: it links to the computer the cables reach, and a computer can find
 * installation media in any drive linked to it. The medium, the link and whether the computer is reading the drive
 * are sent to the players who see it, and the block shows whether it holds a medium.
 *
 * <p>The floppy, CD and DVD drives are drawn as models of the drives of their day: the medium in the drive is the
 * very item the player put in, the tray or the slot plays its clip as a medium goes in or comes out, and the lamps
 * say whether a computer is linked and whether it is reading the drive. The Dock Station keeps its block model.
 */
public class MediaReaderBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint, IAudible,
        GeoBlockEntity {

    /** What the drive sounds like taking a medium in and giving it back. */
    private final MediaBaySounds baySounds = new MediaBaySounds();
    /** On the client, the medium drawn in the drive, kept a moment after it is taken so its way out is seen. */
    private final MediaBay bay = new MediaBay();
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private final FieldItemHandler slot = fields().items("MediaSlot", 1).save().toClient().dropsWhenBroken()
            .slotLimit(1).accepts((index, stack) -> acceptsMedia(stack))
            .onChange(index -> mediaMoved()).onLoad(() -> baySounds.settle(mediaStack()));
    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    /*
     * Whether the linked computer is installing a system or a program from the medium in this drive: the server
     * works it out, and the client hears the drive read and sees its activity lamp blink while it is true.
     */
    private final BoolField reading = fields().flag("Reading", false).toClient();
    /** The medium held just before an update from the server was read, on the client. */
    private ItemStack beforeUpdate = ItemStack.EMPTY;
    /** Whether the block is being broken, when the medium leaves without the sound of an eject. */
    private boolean breaking;

    public MediaReaderBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.MEDIA_READER_BE.get(), pos, state);
        fields().mirror(MediaReaderBlock.LOADED, this::hasMedia);
        fields().whenBroken((level, at) -> breaking = true);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final MediaReaderBlockEntity drive) {
        if (level instanceof ServerLevel server) {
            drive.link.tick(server, pos);
            drive.reading.set(drive.installingFromHere(server));
        }
    }

    /** The drive type of this reader's block, deciding which media formats its slot accepts. */
    public MediaDriveType driveType() {
        return getBlockState().getBlock() instanceof MediaReaderBlock drive
                ? drive.driveType() : MediaDriveType.FLOPPY_DRIVE;
    }

    /**
     * Whether this reader takes the given stack. A {@link FormattedMediaItem} is taken only when this drive reads its
     * {@link MediaFormat}; generic media is taken by any drive.
     */
    public boolean acceptsMedia(final ItemStack stack) {
        if (stack.getItem() instanceof FormattedMediaItem media) {
            return driveType().accepts(media.format());
        }
        return stack.getItem() instanceof MediaItem;
    }

    /** The format of the medium in the drive, or null when it is empty or the medium has no fixed format. */
    @Nullable
    public MediaFormat insertedFormat() {
        return mediaStack().getItem() instanceof FormattedMediaItem media ? media.format() : null;
    }

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
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
    }

    /** The linked computer's position, or null while unlinked. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            MachineSoundSources.track(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            MachineSoundSources.untrack(this);
        }
    }

    @Override
    public double audioX() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double audioY() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double audioZ() {
        return worldPosition.getZ() + 0.5;
    }

    /**
     * A floppy drive's head stepping, or a disc turning, while a system or a program installs from what the drive
     * holds. A USB drive has nothing that moves.
     */
    @Override
    public List<LoopRequest> loops() {
        final MediaFormat format = insertedFormat();
        if (!reading.get() || format == null) {
            return List.of();
        }
        return switch (format) {
            case FLOPPY -> List.of(LoopRequest.of(ComputingSounds.FLOPPY_READ));
            case CD, DVD -> List.of(LoopRequest.of(ComputingSounds.OPTICAL_READ));
            case USB -> List.of();
        };
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(MediaBay.controller(this, "media_drive"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    /** Whether this reader is drawn as a model: the drives are, the Dock Station keeps its block model. */
    public boolean modelled() {
        return driveType() != MediaDriveType.DOCK_STATION;
    }

    /** Whether the linked computer is reading this drive now, which its activity lamp shows by blinking. */
    public boolean reading() {
        return reading.get();
    }

    /** The medium the client draws in the drive, kept a moment after it is taken so its way out is seen. */
    public ItemStack drawnMedium() {
        return bay.drawn(mediaStack(), level);
    }

    /** The OS or program id the medium in the drive carries, or null when it is empty or carries none. */
    @Nullable
    public ResourceLocation insertedPayload() {
        final ItemStack stack = mediaStack();
        return stack.isEmpty() ? null : MediaItem.payload(stack);
    }

    /** The kind of the medium in the drive, or null when it is empty. */
    @Nullable
    public MediaKind insertedKind() {
        final ItemStack stack = mediaStack();
        return stack.isEmpty() ? null : MediaItem.kind(stack);
    }

    /**
     * The storage snapshot of the DATA medium in the drive, or {@link ServerStorageContents#EMPTY} when it is empty
     * or the medium holds no data.
     */
    public ServerStorageContents insertedData() {
        final ItemStack stack = mediaStack();
        return stack.isEmpty() ? ServerStorageContents.EMPTY : MediaItem.data(stack);
    }

    /**
     * Puts the given medium in the drive.
     *
     * @return what is left over: empty when it went in, the stack unchanged when the drive already holds one
     */
    public ItemStack insertMedia(final ItemStack stack) {
        return slot.insertItem(0, stack, false);
    }

    /** Takes out and returns the medium in the drive, or {@link ItemStack#EMPTY} when it is empty. */
    public ItemStack ejectMedia() {
        final ItemStack held = mediaStack();
        if (held.isEmpty()) {
            return ItemStack.EMPTY;
        }
        slot.setStackInSlot(0, ItemStack.EMPTY);
        return held;
    }

    /** The drive's slot. */
    public ItemStackHandler mediaSlot() {
        return slot;
    }

    @Override
    protected void beforeClientUpdate() {
        beforeUpdate = mediaStack().copy();
    }

    @Override
    protected void afterClientUpdate() {
        bay.seen(beforeUpdate, mediaStack(), level);
    }

    private ItemStack mediaStack() {
        return slot.getStackInSlot(0);
    }

    private boolean hasMedia() {
        return !mediaStack().isEmpty();
    }

    /*
     * A medium went in or came out: the drive sounds it, a USB stick is a device coming or going for the linked
     * computer's system, and the tray or the slot plays its clip. A medium spilled as the drive breaks is silent.
     */
    private void mediaMoved() {
        if (breaking) {
            baySounds.settle(mediaStack());
            return;
        }
        final MediaBaySounds.Move move = baySounds.changed(level, worldPosition, mediaStack());
        if (move == null || move.format() == null) {
            return;
        }
        if (move.format() == MediaFormat.USB && level instanceof ServerLevel server) {
            deviceMoved(server, move.in());
        }
        if (modelled()) {
            triggerAnim(MediaBay.CONTROLLER, MediaBay.clip(move.format(), move.in()));
        }
    }

    /*
     * Whether the linked computer is installing something from the medium in this very drive: a system, whose job
     * names the drive it reads from, or a program, whose setup is for the one this medium carries.
     */
    private boolean installingFromHere(final ServerLevel level) {
        final BlockPos owner = link.ownerPos();
        if (owner == null || mediaStack().isEmpty() || !(level.getBlockEntity(owner) instanceof IOsHost host)) {
            return false;
        }
        final OsInstallJob job = host.installing();
        if (job != null && job.hasReader() && job.readerPos() == worldPosition.asLong()) {
            return true;
        }
        final ComputerConsoleState console = host.console();
        final SetupJob setup = console == null ? null : console.setup();
        final ResourceLocation program = insertedPayload();
        return setup != null && !setup.removing() && program != null && program.toString().equals(setup.programId());
    }

    /*
     * A USB drive plugged in or pulled out is a device coming and going for the system of the computer it is linked
     * to, which says so with its own sound when it is up at its desktop.
     */
    private void deviceMoved(final ServerLevel server, final boolean in) {
        final BlockPos owner = link.ownerPos();
        if (owner != null && server.getBlockEntity(owner) instanceof IOsHost host && host.isRunning()
                && host.bootedDesktopId() != null) {
            host.systemSound(server, in ? SystemSound.DEVICE_CONNECT : SystemSound.DEVICE_DISCONNECT);
        }
    }
}
