/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A speaker's screen: its name, the computer it plays for, the side it plays and how well. The name and the computer
 * come with the screen; whether a name asked for clashes, and the side, are kept up to date while it is open.
 */
public class SpeakerMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Opening opening;
    /** The speaker itself, on the server; null on the client, which only shows it. */
    @Nullable
    private final SpeakerBlockEntity speaker;

    /** The server's menu, reading the speaker as it is. */
    public SpeakerMenu(final int containerId, final Inventory playerInventory, final SpeakerBlockEntity be,
                       final Opening opening) {
        this(containerId, be.dataAccess(), ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), opening,
                be);
    }

    private SpeakerMenu(final int containerId, final ContainerData data, final ContainerLevelAccess access,
                        final Opening opening, @Nullable final SpeakerBlockEntity speaker) {
        super(ComputingMenus.SPEAKER_MENU.get(), containerId);
        this.data = data;
        this.access = access;
        this.opening = opening;
        this.speaker = speaker;
        addDataSlots(data);
    }

    /** The client's menu, holding what the server sends it. */
    @Nullable
    public static SpeakerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                          final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        return new SpeakerMenu(containerId, new SimpleContainerData(SpeakerBlockEntity.DATA_COUNT),
                ContainerLevelAccess.create(playerInventory.player.level(), opening.pos()), opening, null);
    }

    /** The screen closed: the name typed on it is taken, unless another speaker of its computer has it. */
    @Override
    public void removed(final Player player) {
        super.removed(player);
        if (speaker != null) {
            speaker.takeAskedName();
        }
    }

    public BlockPos speakerPos() {
        return opening.pos();
    }

    /** What its screen opened with. */
    public Opening opening() {
        return opening;
    }

    /** Whether the name last typed is one another speaker of its computer already has. */
    public boolean nameClashes() {
        return data.get(SpeakerBlockEntity.DATA_CLASH) != 0;
    }

    /** Which side it plays, as one of {@link SpeakerBlockEntity}'s channel numbers. */
    public int channel() {
        return data.get(SpeakerBlockEntity.DATA_CHANNEL);
    }

    @Override
    public boolean stillValid(final Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof SpeakerBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    /**
     * What a speaker's screen opens with.
     *
     * @param pos          where the speaker stands
     * @param name         the name a player gave it, empty for none
     * @param computerName the name a player gave its computer, empty for none
     * @param computerKind the translation key of its computer's block, empty when it is not linked to one
     * @param era          the era of its model
     */
    public record Opening(BlockPos pos, String name, String computerName, String computerKind, HardwareEra era) {

        public void write(final RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(name, SpeakerBlockEntity.MAX_NAME);
            buf.writeUtf(computerName, SpeakerBlockEntity.MAX_NAME);
            buf.writeUtf(computerKind);
            buf.writeVarInt(era.id());
        }

        public static Opening read(final RegistryFriendlyByteBuf buf) {
            final BlockPos pos = buf.readBlockPos();
            final String name = buf.readUtf(SpeakerBlockEntity.MAX_NAME);
            final String computerName = buf.readUtf(SpeakerBlockEntity.MAX_NAME);
            final String computerKind = buf.readUtf();
            final HardwareEra era = HardwareEra.find(buf.readVarInt());
            return new Opening(pos, name, computerName, computerKind, era == null ? HardwareEra.STANDARD : era);
        }
    }
}
