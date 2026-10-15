/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * An item that grows a machine of a family into one tier of it, in its place, as Mekanism's tier installers do: used
 * on the tier just below, it is used up and the machine becomes that tier, its contents and settings kept.
 *
 * <pre>{@code
 * CONTENT.item("advanced_installer", properties -> new MachineInstallerItem(properties, () -> CRUSHERS, 1))
 *         .named("Advanced Installer").register();
 * }</pre>
 */
@TextHolder
public class MachineInstallerItem extends Item {

    private final Supplier<MachineFamily> family;
    private final int tier;

    public static final TextKey GREW = TextKey.of("jscore.machine.installer.grew", "It is now %s");
    public static final TextKey WRONG_TIER = TextKey.of("jscore.machine.installer.wrong_tier",
            "This installer makes %s, from the tier just below it");
    public static final TextKey MAKES = TextKey.of("jscore.machine.installer.makes",
            "Makes %s of the machine just below it, in its place");

    /** An installer making tier {@code tier} of {@code family}, counted from the lowest, 0. */
    public MachineInstallerItem(final Properties properties, final Supplier<MachineFamily> family, final int tier) {
        super(properties);
        this.family = Objects.requireNonNull(family, "family");
        if (tier < 1) {
            throw new IllegalArgumentException("an installer makes a tier above the lowest; got " + tier);
        }
        this.tier = tier;
    }

    /* It acts before the machine, which would otherwise open its screen. */
    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        final MachineFamily of = this.family.get();
        final Block made = of.tier(this.tier);
        if (of.tierOf(machine.getBlockState().getBlock()) != this.tier - 1) {
            player.displayClientMessage(GameText.component(WRONG_TIER.with(GameText.of(made.getName()))), true);
            return InteractionResult.CONSUME;
        }
        if (MachineGrowth.grow(player, machine, made)) {
            stack.consume(1, player);
            player.displayClientMessage(GameText.component(GREW.with(GameText.of(made.getName()))), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(GameText.component(MAKES.with(GameText.of(this.family.get().tier(this.tier).getName()))));
    }
}
