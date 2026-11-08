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
import dev.jstech.computers.blockentity.UpsBlockEntity;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.computers.power.UpsTexts;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An MF PowerGuard, the UPS block of each era (computers/docs/design/power.md): a whole block, as real tower UPSes are.
 * The energy cable plugs into the inlet in the middle of its back or of either side; every computer standing against
 * it draws from it through the face they share, and is carried by its battery when the line goes.
 */
@TextHolder
public final class UpsBlock extends DeviceBlock {

    private final Era era;

    public static final MapCodec<UpsBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(UpsBlock::era)
    ).apply(i, UpsBlock::new));
    /** Where the energy cable plugs in: the inlet in the middle of its back or of either side. */
    public static final FaceRule INLET = FaceRule.of(RelativeFace.BACK, RelativeFace.LEFT, RelativeFace.RIGHT);

    /* The MF PowerGuard of each era: the 300 of the Vintage and the Legacy, the 500, the 800, the 1500. */
    private static final EraTable<UpsRating> RATINGS = EraTable.ofFive(UpsRating.POWERGUARD_300,
            UpsRating.POWERGUARD_300, UpsRating.POWERGUARD_500, UpsRating.POWERGUARD_800, UpsRating.POWERGUARD_1500);
    private static final Device<UpsBlockEntity> DEVICE = Device.of(() -> ComputingModule.UPS_BE.get())
            .ticks(UpsBlockEntity::serverTick)
            .ports(FacePorts.builder().port(INLET, SharedEnergy.LOW_VOLTAGE_LINE).build());
    private static final TextKey TOOLTIP = TextKey.of("block.jsc.ups.tooltip",
            "Carries the computers standing against it when the power goes; at 10% it shuts them down cleanly");

    public UpsBlock(final Properties properties, final Era era) {
        super(properties, DEVICE);
        this.era = era;
    }

    /** The era of its model. */
    public Era era() {
        return era;
    }

    /** What it holds and carries. */
    public UpsRating rating() {
        return Objects.requireNonNull(RATINGS.get(era), "every era has a PowerGuard");
    }

    /** What a UPS of {@code rating} holds and the most it carries, as its tooltips say it. */
    public static Text holds(final UpsRating rating) {
        return UpsTexts.HOLDS.with(JsEnergy.ENERGY.amount(rating.batteryJoules()),
                JsEnergy.ENERGY.rate(UpsRating.tickOf(rating.carryWatts())));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** It is one model drawn by the block entity, its lamps lit as it stands. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component(TOOLTIP).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(holds(rating())).withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, era);
    }
}
