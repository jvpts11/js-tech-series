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
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * A speaker's screen: its name, the computer it plays for, the side it plays and how well. The name and the computer
 * come with the screen; whether a name asked for clashes, and the side, are the speaker's own fields, carried to the
 * client's copy of the same block entity while the screen is open.
 */
public class SpeakerMenu extends CoreMenu {

    private final SpeakerBlockEntity speaker;
    private final Opening opening;

    /** The server's menu, reading the speaker as it is; the client's, once it resolved its own block entity too. */
    public SpeakerMenu(final int containerId, final Inventory playerInventory, final SpeakerBlockEntity speaker,
                       final Opening opening) {
        super(ComputingMenus.SPEAKER_MENU.get(), containerId, playerInventory,
                MenuValidity.block(speaker.getLevel(), speaker.getBlockPos(), SpeakerBlock.class));
        this.speaker = speaker;
        this.opening = opening;
        data(speaker.fields().menuData());
    }

    /** The client's menu, on the client's own copy of the speaker standing at the opening's position. */
    public static SpeakerMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                          final RegistryFriendlyByteBuf buf) {
        final Opening opening = Opening.read(buf);
        if (!(playerInventory.player.level().getBlockEntity(opening.pos()) instanceof SpeakerBlockEntity speaker)) {
            throw new IllegalStateException("a menu was opened on a Speaker at " + opening.pos()
                    + " the client does not have");
        }
        return new SpeakerMenu(containerId, playerInventory, speaker, opening);
    }

    /** The screen closed: the name typed on it is taken, unless another speaker of its computer has it. */
    @Override
    public void removed(final Player player) {
        super.removed(player);
        speaker.takeAskedName();
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
        return speaker.nameClashes();
    }

    /** Which side it plays, as one of {@link SpeakerBlockEntity}'s channel numbers. */
    public int channel() {
        return speaker.channel();
    }

    /** Whether its computer has a subwoofer against a Transition satellite. */
    public boolean subwoofer() {
        return speaker.subwoofer();
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
