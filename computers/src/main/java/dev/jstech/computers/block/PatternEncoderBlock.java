/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.menu.PatternEncoderMenu;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.ITrayBlock;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.StandingTray;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IStandingParts;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Pattern Encoder block: the burner a computer's Pattern Studio sends finished recipe files to. One block
 * per hardware era, each writing the media of its day. A click with a medium the era accepts puts it in the bay,
 * a sneak-click takes it out (unless a job holds it), and a plain click opens the bay's small panel.
 *
 * <p>From the Legacy era on the encoder burns discs, which lie on a tray: a click on the eject button on its front
 * opens and closes the tray, and a disc is laid on it or lifted off it only while it is out. A stick goes into its
 * port whatever the tray is doing.
 */
@TextHolder
public class PatternEncoderBlock extends DeviceBlock implements IFaceConnector, IEraChassisBlock, ITrayBlock,
        IStandingParts {

    private final Era era;
    private final FacePorts ports;

    public static final MapCodec<PatternEncoderBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(b -> b.era)
    ).apply(i, PatternEncoderBlock::new));

    /** The encoder's block entity, ticking its link and its jobs, with the bay's panel as its menu. */
    private static final Device<PatternEncoderBlockEntity> DEVICE =
            Device.of(() -> ComputingModule.PATTERN_ENCODER_BE.get())
                    .ticks(PatternEncoderBlockEntity::serverTick)
                    .opensMenu(PatternEncoderMenu::new);

    private static final TextKey WRITING_WAIT = TextKey.of("jsc.pattern_encoder.writing_wait",
            "The encoder is writing - wait for it to finish.");
    private static final TextKey BAY_FULL = TextKey.of("jsc.pattern_encoder.bay_full",
            "The bay already holds a disc - sneak-click to eject it.");
    private static final TextKey READ_ONLY = TextKey.of("jsc.pattern_encoder.read_only",
            "That disc is read-only and cannot be written.");
    private static final TextKey VINTAGE_ONLY =
            TextKey.of("jsc.pattern_encoder.vintage_only", "A Vintage encoder writes floppy disks only.");
    private static final TextKey LEGACY_ONLY =
            TextKey.of("jsc.pattern_encoder.legacy_only", "A Legacy encoder writes CDs only.");
    private static final TextKey TRANSITION_ONLY =
            TextKey.of("jsc.pattern_encoder.transition_only", "A Transition encoder writes DVDs and CDs only.");
    private static final TextKey STANDARD_ONLY = TextKey.of("jsc.pattern_encoder.standard_only",
            "This encoder writes DVDs, CDs and USB sticks, not that.");
    private static final TextKey ADVANCED_ONLY = TextKey.of("jsc.pattern_encoder.advanced_only",
            "An Advanced encoder writes Blu-ray discs and USB sticks only.");

    /*
     * The eject button beside the tray, where each era's model draws it: on the Legacy burner below the tray's right
     * end, on the later ones a little higher and further right. Each front is set a pixel back in its case.
     */
    private static final EjectButton LEGACY_BUTTON = new EjectButton(46, 24, 54, 27, 4);
    private static final EjectButton LATER_BUTTON = new EjectButton(48, 23, 56, 25, 4);
    /* Each era's tray as its model slides it out, seven model pixels, with the disc lying on it. */
    private static final StandingTray LEGACY_TRAY = new StandingTray(new FrontArea(10, 12, 52, 20), 24);
    private static final StandingTray LATER_TRAY = new StandingTray(new FrontArea(8, 12, 46, 20), 24);
    /* The Vintage encoder writes floppies, which go through a slot, and has neither button nor tray. */
    private static final EraTable<EjectButton> BUTTONS = EraTable.<EjectButton>of(null)
            .from(HardwareEra.LEGACY, LEGACY_BUTTON)
            .from(HardwareEra.TRANSITION, LATER_BUTTON);
    private static final EraTable<StandingTray> TRAYS = EraTable.<StandingTray>of(null)
            .from(HardwareEra.LEGACY, LEGACY_TRAY)
            .from(HardwareEra.TRANSITION, LATER_TRAY);
    /* What each era's encoder says when handed a medium it does not write. */
    private static final EraTable<TextKey> ONLY = EraTable.ofFive(VINTAGE_ONLY, LEGACY_ONLY, TRANSITION_ONLY,
            STANDARD_ONLY, ADVANCED_ONLY);

    public PatternEncoderBlock(final Properties properties, final Era era) {
        super(properties, DEVICE);
        this.era = era;
        this.ports = PeripheralSockets.back(era);
    }

    /** The era of this encoder, which decides which media it writes. */
    public Era era() {
        return era;
    }

    @Override
    public Era chassisEra() {
        return era;
    }

    /** The device port of its era, in the middle of its back. */
    @Override
    public FacePorts ports() {
        return ports;
    }

    /** The button of its tray; the Vintage encoder writes floppies, which go through a slot, and has none. */
    @Override
    @Nullable
    public EjectButton ejectButton() {
        return BUTTONS.get(era);
    }

    @Override
    @Nullable
    public StandingTray standingTray() {
        return TRAYS.get(era);
    }

    /* Its tray, once open, stands out of the block, and is looked at and clicked as the encoder's own. */
    @Override
    public List<AABB> standingParts(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final StandingTray tray = standingTray();
        return tray != null && level.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder
                && encoder.tray().isOpen() ? List.of(tray.box(state.getValue(FACING))) : List.of();
    }

    /**
     * The eject button pressed, from the block or from the bay's panel: the tray rides out, or back in, unless the head
     * is on the disc, which the encoder does not give up mid-write.
     */
    public static void pressEjectButton(final PatternEncoderBlockEntity encoder, final Player player) {
        if (encoder.locked()) {
            player.displayClientMessage(GameText.component(WRITING_WAIT), true);
            return;
        }
        encoder.tray().press(encoder.getLevel(), encoder.getBlockPos());
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** The body is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /*
     * The body fills its block in every era, so the shape is the plain cube: nothing to walk through or
     * stand on beyond the block itself.
     */
    @Override
    protected VoxelShape getShape(
            final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // The eject button answers whatever the hand holds, as a real one does.
        final EjectButton button = ejectButton();
        if (button != null && DeviceFront.presses(button, state, pos, hit, player.getEyePosition())) {
            pressEjectButton(encoder, player);
            return ItemInteractionResult.SUCCESS;
        }
        // The open tray pushed with an empty hand slides back in, as a real one does, and opens no panel.
        if (heldStack.isEmpty() && !player.isShiftKeyDown()
                && DeviceFront.partAt(standingParts(state, level, pos), pos, hit.getLocation()) >= 0) {
            pressEjectButton(encoder, player);
            return ItemInteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            eject(encoder, level, pos, player);
            return ItemInteractionResult.SUCCESS;
        }
        if (encoder.acceptsMedia(heldStack)) {
            if (!encoder.tray().reaches(heldStack)) {
                player.displayClientMessage(GameText.component(DiscTray.CLOSED), true);
                return ItemInteractionResult.SUCCESS;
            }
            final ItemStack left = encoder.insertMedia(heldStack.copyWithCount(1));
            if (left.isEmpty()) {
                heldStack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            player.displayClientMessage(GameText.component(BAY_FULL), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (heldStack.getItem() instanceof MediaItem) {
            // A refused disc says why; a silent click reads as a broken block.
            player.displayClientMessage(GameText.component(refusal(heldStack)), true);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder) {
            eject(encoder, level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Gives the medium back to {@code player}, or says why the bay keeps it. */
    private static void eject(final PatternEncoderBlockEntity encoder, final Level level, final BlockPos pos,
                              final Player player) {
        if (encoder.locked()) {
            player.displayClientMessage(GameText.component(WRITING_WAIT), true);
            return;
        }
        if (!encoder.tray().reaches(encoder.mediaStack())) {
            player.displayClientMessage(GameText.component(DiscTray.CLOSED), true);
            return;
        }
        final ItemStack ejected = encoder.ejectMedia();
        if (!ejected.isEmpty() && !player.addItem(ejected)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ejected);
        }
    }

    /** What to tell a player whose disc the bay refused. */
    private TextKey refusal(final ItemStack held) {
        if (held.getItem() instanceof FormattedMediaItem fmt
                && !fmt.writable()) {
            return READ_ONLY;
        }
        return ONLY.get(era);
    }
}
