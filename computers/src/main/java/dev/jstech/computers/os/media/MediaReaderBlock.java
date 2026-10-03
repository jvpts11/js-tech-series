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
import net.minecraft.core.Direction;
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

/**
 * A media reader peripheral: a slot that holds one {@link MediaItem} whose {@link MediaFormat} this drive accepts.
 * Right-click inserts the held media; sneak-right-click ejects it. The loaded payload is available via
 * {@link MediaReaderBlockEntity#insertedPayload()}.
 *
 * <p>The concrete drive (Floppy / CD / DVD / Dock) is decided by the {@link MediaDriveType} passed at registration,
 * which also controls which media formats the slot accepts.
 *
 * <p>A peripheral of its computer: against it, or on a peripheral cable into the port of its era on its back, it links
 * to the computer, which then finds installation media in it without the reader having to stand beside it.
 */
@TextHolder
public class MediaReaderBlock extends DeviceBlock implements IFaceConnector {

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
    private static final TextKey FLOPPY_DRIVE = TextKey.of("jsc.media.media_reader_block.floppy_drive",
            "floppy drive");
    private static final TextKey CD_DRIVE = TextKey.of("jsc.media.media_reader_block.cd_drive", "CD drive");
    private static final TextKey DVD_DRIVE = TextKey.of("jsc.media.media_reader_block.dvd_drive", "DVD drive");
    private static final TextKey DOCK_STATION = TextKey.of("jsc.media.media_reader_block.dock_station",
            "dock station");

    /**
     * The Dock Station is a low hub on the desk, not a cube: 14 wide, 10 deep and a hand tall, with the stick
     * standing out of its front when one is docked. Its shape follows the model so a player can stand things on it
     * and walk past the stick; the disc drives stay full blocks.
     */
    private static final VoxelShape DOCK_NORTH_SOUTH = Block.box(1, 0, 3, 15, 6.5, 13);
    private static final VoxelShape DOCK_EAST_WEST = Block.box(3, 0, 1, 13, 6.5, 15);

    public MediaReaderBlock(final MediaDriveType driveType, final Properties properties) {
        super(properties, DEVICE);
        this.driveType = driveType;
        this.ports = PeripheralSockets.back(driveType.era());
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

        if (player.isShiftKeyDown()) {
            // Sneak-click: eject any loaded media back to the player.
            final ItemStack ejected = reader.ejectMedia();
            if (!ejected.isEmpty() && !player.addItem(ejected)) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ejected);
            }
            return ItemInteractionResult.SUCCESS;
        }

        // Normal click: insert held media if the slot is empty and the drive accepts its format.
        if (reader.acceptsMedia(heldStack)) {
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
            player.displayClientMessage(GameText.component(CANNOT_READ.with(driveName(reader))), true);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (driveType != MediaDriveType.DOCK_STATION) {
            return Shapes.block();
        }
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? DOCK_NORTH_SOUTH : DOCK_EAST_WEST;
    }

    /** A readable name for this drive, for the message a refused disc gets. */
    private static TextKey driveName(final MediaReaderBlockEntity reader) {
        return switch (reader.driveType()) {
            case FLOPPY_DRIVE -> FLOPPY_DRIVE;
            case CD_DRIVE -> CD_DRIVE;
            case DVD_DRIVE -> DVD_DRIVE;
            case DOCK_STATION -> DOCK_STATION;
        };
    }
}
