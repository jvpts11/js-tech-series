/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * An upgrade declared by what it does and how many of it fit in one machine, so a mod's upgrade needs no class of its
 * own; its tooltip says both.
 *
 * <pre>{@code
 * CONTENT.item("speed_upgrade", properties -> new UpgradeItem(properties,
 *         UpgradeEffect.NONE.withSpeed(1.5).withEnergy(1.3), 4)).named("Speed Upgrade").register();
 * }</pre>
 */
public class UpgradeItem extends Item implements IUpgrade {

    private final UpgradeEffect effect;
    private final int most;
    private final @Nullable ResourceLocation kind;

    /** An upgrade of {@code effect}, {@code most} of which fit in one machine ({@link IUpgrade#ANY} for no limit). */
    public UpgradeItem(final Properties properties, final UpgradeEffect effect, final int most) {
        this(properties, effect, most, null);
    }

    /**
     * The same, of the {@code kind} of upgrade whose items share how many fit in one machine, {@code most} of them in
     * all; a kind of its own for {@code null}.
     */
    public UpgradeItem(final Properties properties, final UpgradeEffect effect, final int most,
                       final @Nullable ResourceLocation kind) {
        super(properties);
        this.effect = Objects.requireNonNull(effect, "effect");
        if (most < 1) {
            throw new IllegalArgumentException("at least one of an upgrade fits in a machine; got " + most);
        }
        this.most = most;
        this.kind = kind;
    }

    @Override
    public UpgradeEffect effect(final ItemStack stack) {
        return this.effect;
    }

    @Override
    public int most(final ItemStack stack) {
        return this.most;
    }

    @Override
    public ResourceLocation kind(final ItemStack stack) {
        return this.kind != null ? this.kind : IUpgrade.super.kind(stack);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        for (final UpgradeTexts.Line each : UpgradeTexts.effects(this.effect)) {
            lines.add(GameText.component(each.text()).withStyle(each.gain() ? ChatFormatting.GREEN
                    : ChatFormatting.RED));
        }
        lines.add(GameText.component(UpgradeTexts.most(this.most)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
