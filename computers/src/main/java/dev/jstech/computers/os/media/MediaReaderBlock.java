/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.DeviceFront;
import dev.jstech.computers.block.PeripheralSockets;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A media reader peripheral: a slot that holds one {@link MediaItem} whose {@link MediaFormat} this drive accepts.
 * Right-click inserts the held media; sneak-right-click ejects it. The loaded payload is available via
 * {@link MediaReaderBlockEntity#insertedPayload()}.
 *
 * <p>An optical drive has a disc tray instead, which a click on the eject button on its front opens and closes, as on
 * the drives of its day: the disc is laid on the open tray with a click and lifted off it with a sneak-click.
 *
 * <p>The concrete drive (Floppy / CD / DVD / Dock) is decided by the {@link MediaDriveType} passed at registration,
 * which also controls which media formats the slot accepts.
 *
 * <p>A peripheral of its computer: against it, or on a peripheral cable into the port of its era on its back, it links
 * to the computer, which then finds installation media in it without the reader having to stand beside it.
 */
@TextHolder
public class MediaReaderBlock extends DeviceBlock implements IFaceConnector, ITrayBlock {

    private final MediaDriveType driveType;
    private final FacePorts ports;

    public static final MapCodec<MediaReaderBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    StableCodecs.byName(MediaDriveType.class).fieldOf("drive_type").forGetter(b -> b.driveType),
                    propertiesCodec()
            ).apply(instance, MediaReaderBlock::new));

    /** True when the slot holds a medium, so the model shows the "loaded" (lit LED) front face. */
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");

    /** The drive's block entity, ticking to keep its link and to know whether its computer reads it. */
    private static final Device<MediaReaderBlockEntity> DEVICE =
            Device.of(() -> ComputingModule.MEDIA_READER_BE.get()).ticks(MediaReaderBlockEntity::serverTick);

    /** What a click with a disc is told when the drive will not take it. */
    private static final TextKey ALREADY_HOLDS = TextKey.of("jsc.media.media_reader_block.already_holds",
            "The drive already holds a disc - sneak-click to eject it.");
    private static final TextKey CANNOT_READ = TextKey.of("jsc.media.media_reader_block.cannot_read",
            "This %s cannot read that disc.");
    /** What a press of the eject button is told while the computer reads the disc. */
    private static final TextKey READING_WAIT = TextKey.of("jsc.media.media_reader_block.reading_wait",
            "The drive is reading - wait for it to finish.");

    public MediaReaderBlock(final MediaDriveType driveType, final Properties properties) {
        this(driveType, properties, DEVICE);
    }

    /** A reader with a block entity of its own kind, as the Dock Station's, which holds disks besides its stick. */
    protected MediaReaderBlock(final MediaDriveType driveType, final Properties properties, final Device<?> device) {
        super(properties, device);
        this.driveType = driveType;
        this.ports = PeripheralSockets.back(driveType.portEra());
        registerDefaultState(defaultBlockState().setValue(LOADED, false));
    }

    /** The kind of drive this block is, deciding which media formats its slot accepts. */
    public MediaDriveType driveType() {
        return driveType;
    }

    /** The device port of the drive's era, in the middle of its back. */
    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    @Nullable
    public EjectButton ejectButton() {
        return driveType.ejectButton();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LOADED);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof MediaReaderBlockEntity reader)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // The eject button answers whatever the hand holds, as a real one does.
        final EjectButton button = ejectButton();
        if (button != null && DeviceFront.presses(button, state, pos, hit, player.getEyePosition())) {
            pressEjectButton(reader, player);
            return ItemInteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            // Sneak-click: eject any loaded media back to the player, a disc only off an open tray.
            if (!reader.tray().reaches(reader.mediaSlot().getStackInSlot(0))) {
                player.displayClientMessage(GameText.component(DiscTray.CLOSED), true);
                return ItemInteractionResult.SUCCESS;
            }
            final ItemStack ejected = reader.ejectMedia();
            if (!ejected.isEmpty() && !player.addItem(ejected)) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ejected);
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Normal click: insert held media if the slot is empty and the drive accepts its format.
        if (reader.acceptsMedia(heldStack)) {
            if (!reader.tray().reaches(heldStack)) {
                player.displayClientMessage(GameText.component(DiscTray.CLOSED), true);
                return ItemInteractionResult.SUCCESS;
            }
            final ItemStack inserted = reader.insertMedia(heldStack.copyWithCount(1));
            if (inserted.isEmpty()) {
                heldStack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            // The drive reads this format but the slot is taken. Say so, or the click looks ignored.
            player.displayClientMessage(GameText.component(ALREADY_HOLDS), true);
            return ItemInteractionResult.SUCCESS;
        }

        /*
         * A refused disc must say why. A silent click is indistinguishable from a broken block, and a player holding
         * a DVD at a CD drive has no other way to learn the difference.
         */
        if (heldStack.getItem() instanceof MediaItem) {
            player.displayClientMessage(GameText.component(CANNOT_READ.with(reader.driveType().inSentence())), true);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /* Every reader fills its block, the Dock Station as much as the drives. */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return Shapes.block();
    }

    /*
     * The eject button pressed: the tray rides out, or back in, unless the computer is reading the disc on it, which a
     * drive does not give up mid-read.
     */
    private static void pressEjectButton(final MediaReaderBlockEntity reader, final Player player) {
        if (reader.reading()) {
            player.displayClientMessage(GameText.component(READING_WAIT), true);
            return;
        }
        reader.tray().press(reader.getLevel(), reader.getBlockPos());
    }
}
