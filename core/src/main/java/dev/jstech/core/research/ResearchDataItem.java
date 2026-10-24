/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * The item of a kind of data, as its mod registers it: one item for each kind, its tooltip saying what gives it.
 *
 * <pre>{@code
 * public static final ItemEntry<ResearchDataItem> CRYSTAL_DATA = CONTENT.item("crystal_data",
 *         properties -> new ResearchDataItem(properties, MyResearch.CRYSTAL, MyResearch.CRYSTAL_FROM))
 *         .named("Crystal Data").register();
 * }</pre>
 */
public class ResearchDataItem extends Item implements IResearchData {

    private final DataKind kind;
    private final @Nullable TextKey from;

    /** Data of {@code kind}, its tooltip saying nothing of where it comes from. */
    public ResearchDataItem(final Properties properties, final DataKind kind) {
        this(properties, kind, null);
    }

    /** Data of {@code kind}, its tooltip saying {@code from}: "From the X-ray Diffractometer". */
    public ResearchDataItem(final Properties properties, final DataKind kind, final @Nullable TextKey from) {
        super(properties);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.from = from;
    }

    @Override
    public DataKind dataKind() {
        return this.kind;
    }

    /** What gives it, as its tooltip says: "From the X-ray Diffractometer"; none when it says nothing. */
    public Optional<TextKey> from() {
        return Optional.ofNullable(this.from);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (this.from != null) {
            lines.add(GameText.component(this.from).withStyle(ChatFormatting.GRAY));
        }
    }
}
