/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.bigblock.AbstractBigBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The pool element {@code jscore:chosen}: one of a set of templates, all of one size, picked as it is placed by the
 * {@link ITemplateChooser} a mod registered under {@code chooser}, so a structure can be of the era its distance
 * gives it, worn or whole by a setting, without a pool per case. Any big block standing in the template raises its
 * parts as it is placed ({@link AbstractBigBlock#raise}), so racks and cabinets stand whole in a structure.
 *
 * <pre>{@code
 * { "element_type": "jscore:chosen", "chooser": "mymod:ruin", "projection": "rigid",
 *   "templates": ["mymod:ruin/old", "mymod:ruin/new"], "processors": "mymod:ruin" }
 * }</pre>
 */
public final class ChosenElement extends StructurePoolElement {

    private final ResourceLocation chooser;
    private final List<ResourceLocation> templates;
    private final Holder<StructureProcessorList> processors;

    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /* Spreads a place over the seed, so two places nearby draw unlike randoms. */
    private static final long SPREAD = 0x9E3779B97F4A7C15L;
    public static final MapCodec<ChosenElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("chooser").forGetter(ChosenElement::chooser),
            Codec.list(ResourceLocation.CODEC, 1, Integer.MAX_VALUE).fieldOf("templates")
                    .forGetter(ChosenElement::templates),
            StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(ChosenElement::processors),
            projectionCodec()).apply(instance, ChosenElement::new));
    /*
     * The big blocks each template holds, read once from its palette: by the template itself, so a template read
     * again on a reload, a new one, is read again too.
     */
    private static final Map<StructureTemplate, List<Block>> BIG_BLOCKS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public ChosenElement(final ResourceLocation chooser, final List<ResourceLocation> templates,
                         final Holder<StructureProcessorList> processors,
                         final StructureTemplatePool.Projection projection) {
        super(projection);
        this.chooser = chooser;
        this.templates = List.copyOf(templates);
        this.processors = processors;
    }

    public ResourceLocation chooser() {
        return this.chooser;
    }

    public List<ResourceLocation> templates() {
        return this.templates;
    }

    public Holder<StructureProcessorList> processors() {
        return this.processors;
    }

    @Override
    public Vec3i getSize(final StructureTemplateManager manager, final Rotation rotation) {
        return manager.getOrCreate(this.templates.getFirst()).getSize(rotation);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(final StructureTemplateManager manager,
                                                                             final BlockPos pos,
                                                                             final Rotation rotation,
                                                                             final RandomSource random) {
        return List.of();
    }

    @Override
    public BoundingBox getBoundingBox(final StructureTemplateManager manager, final BlockPos pos,
                                      final Rotation rotation) {
        return manager.getOrCreate(this.templates.getFirst())
                .getBoundingBox(new StructurePlaceSettings().setRotation(rotation), pos);
    }

    @Override
    public boolean place(final StructureTemplateManager manager, final WorldGenLevel level,
                         final StructureManager structures, final ChunkGenerator generator, final BlockPos offset,
                         final BlockPos pos, final Rotation rotation, final BoundingBox box, final RandomSource random,
                         final LiquidSettings liquids, final boolean keepJigsaws) {
        final ITemplateChooser picker = TemplateChoosers.get(this.chooser).orElseThrow(() ->
                new IllegalStateException("no template chooser is registered as " + this.chooser));
        final ITemplateChooser.Choice choice = picker.choose(level, offset,
                RandomSource.create(level.getSeed() ^ offset.asLong() * SPREAD), this.templates);
        final StructureTemplate template = manager.getOrCreate(choice.template());
        final StructurePlaceSettings settings = new StructurePlaceSettings().setBoundingBox(box)
                .setRotation(rotation).setKnownShape(true).setIgnoreEntities(false).setFinalizeEntities(true)
                .setLiquidSettings(liquids).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        this.processors.value().list().forEach(settings::addProcessor);
        choice.processors().forEach(settings::addProcessor);
        return placeTemplate(choice.template(), template, level, offset, pos, settings, random);
    }

    /**
     * Places {@code template}, saved as {@code id}, with {@code settings}, and raises the parts of the big blocks it
     * holds inside the settings' box (or the template's own), as a piece of this kind places it: so a structure placed
     * another way (a test, a command) stands the same. False when the template would not place.
     */
    public static boolean placeTemplate(final ResourceLocation id, final StructureTemplate template,
                                        final ServerLevelAccessor level, final BlockPos offset, final BlockPos pos,
                                        final StructurePlaceSettings settings, final RandomSource random) {
        if (!template.placeInWorld(level, offset, pos, settings, random, PLACE_FLAGS)) {
            return false;
        }
        final BoundingBox box = settings.getBoundingBox() != null ? settings.getBoundingBox()
                : template.getBoundingBox(settings, offset);
        for (final Block block : bigBlocks(template)) {
            for (final StructureTemplate.StructureBlockInfo origin : template.filterBlocks(offset, settings, block)) {
                // The template keeps a block as it was saved: turned as the piece is, the origin faces its way.
                final BlockState turned = origin.state().mirror(settings.getMirror()).rotate(settings.getRotation());
                ((AbstractBigBlock) block).raise(level, origin.pos(), turned, box);
            }
        }
        return true;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return CoreStructures.CHOSEN.get();
    }

    @Override
    public String toString() {
        return "Chosen[" + this.chooser + " " + this.templates + "]";
    }

    /* The big blocks {@code template} holds, by its palette. */
    private static List<Block> bigBlocks(final StructureTemplate template) {
        return BIG_BLOCKS.computeIfAbsent(template, key -> {
            final List<Block> out = new ArrayList<>();
            final CompoundTag saved = template.save(new CompoundTag());
            for (final Tag entry : saved.getList("palette", Tag.TAG_COMPOUND)) {
                final ResourceLocation name = ResourceLocation.tryParse(((CompoundTag) entry).getString("Name"));
                if (name != null && BuiltInRegistries.BLOCK.get(name) instanceof AbstractBigBlock big) {
                    out.add(big);
                }
            }
            return List.copyOf(out);
        });
    }
}
