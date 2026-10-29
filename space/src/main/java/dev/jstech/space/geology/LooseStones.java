/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.geology;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.space.SpaceContent;
import java.util.List;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The loose stones of the Moon and of Mars, a decoration of small stones lying on the ground: the dark basalt strewn
 * over the maria, the pale anorthosite of the highlands, and the vesicular basalt lying all over Mars' plains, as
 * Viking 2 saw Utopia Planitia. Each kind is drawn in its rock's own texture.
 */
public final class LooseStones {

    /** The stones of the Moon's maria. */
    public static final Kind MARE_BASALT = new Kind("mare_basalt_stones", "block/mare_basalt", "block/mare_basalt");
    /** The stones of the Moon's highlands. */
    public static final Kind ANORTHOSITE = new Kind("anorthosite_stones", "block/anorthosite", "block/anorthosite");
    /** The stones of Mars' plains, pocked by gas bubbles. */
    public static final Kind MARTIAN_BASALT = new Kind("martian_basalt_stones", "block/vesicular_martian_basalt",
            "block/vesicular_martian_basalt_top");
    /** Every kind, in the tab's order. */
    public static final List<Kind> KINDS = List.of(MARE_BASALT, ANORTHOSITE, MARTIAN_BASALT);

    public static final BlockEntry<LooseStonesBlock> MARE_BASALT_STONES = stones(MARE_BASALT, MapColor.COLOR_BLACK)
            .named("Mare Basalt Stones").register();
    public static final BlockEntry<LooseStonesBlock> ANORTHOSITE_STONES = stones(ANORTHOSITE, MapColor.QUARTZ)
            .named("Anorthosite Stones").register();
    public static final BlockEntry<LooseStonesBlock> MARTIAN_BASALT_STONES = stones(MARTIAN_BASALT,
            MapColor.TERRACOTTA_BLACK).named("Martian Basalt Stones").register();

    /* Where the models are, beside the blocks' own. */
    private static final String FOLDER = "loose_stones/";

    private LooseStones() {
    }

    /** Declares the stones, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares every block above.
    }

    /** The model of the stones of {@code kind} lying in arrangement {@code arrangement}, in the block models. */
    public static String model(final Kind kind, final int arrangement) {
        return FOLDER + kind.id() + "_" + arrangement;
    }

    /** The model every kind's stones in {@code arrangement} take their boxes from, in the block models. */
    public static String arrangement(final int arrangement) {
        return FOLDER + "arrangement_" + arrangement;
    }

    private static BlockBuilder<LooseStonesBlock> stones(final Kind kind, final MapColor colour) {
        return SpaceContent.CONTENT.block(kind.id(), LooseStonesBlock::new)
                .properties(properties -> properties.mapColor(colour).instabreak().noCollission().noOcclusion()
                        .sound(SoundType.STONE).pushReaction(PushReaction.DESTROY))
                .look(IBlockLook.facingByState(state -> new IBlockModel.Handmade(model(kind,
                        state.getValue(LooseStonesBlock.ARRANGEMENT)))))
                .item().itemLook(IItemLook.parent("block/" + model(kind, 0))).drops(Drops.SELF)
                .tab(SpaceRocks.GROUND);
    }

    /**
     * A kind of loose stones: its block, and the textures of its rock on its stones' sides and tops.
     *
     * @param id   the block's id, in J's Space's namespace
     * @param side the texture of the stones' sides
     * @param top  the texture of their tops
     */
    public record Kind(String id, String side, String top) {
    }
}
