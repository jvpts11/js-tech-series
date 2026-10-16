/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A seal over a chamber of a structure, opened only by a player whose team knows what it asks: until then it stands
 * and says what it waits for; once the team knows, a touch opens it, and the structure remembers it open
 * ({@link StructureStates}). What it asks for is its block entity's, set in the structure's template under
 * {@code "knowledge"}, so one block seals any chamber. A mod registers its own seal, in its own look, with its own
 * block entity type; make it unbreakable and blast-proof in its properties, as a seal is.
 *
 * <pre>{@code
 * BlockEntry<KnowledgeSealBlock> SEAL = CONTENT.block("vault_seal",
 *         properties -> new KnowledgeSealBlock(properties.strength(-1, 3_600_000), () -> SEAL_ENTITY.get()))
 *         .named("Vault Seal").look(IBlockLook.cubeAll("vault_seal")).register();
 * }</pre>
 */
@TextHolder
public class KnowledgeSealBlock extends Block implements EntityBlock {

    private final Supplier<? extends BlockEntityType<? extends Entity>> type;

    public static final TextKey SEALED = TextKey.of("jscore.structure.seal.sealed",
            "Sealed. It opens to a team that knows %s.");
    public static final TextKey UNKNOWN = TextKey.of("jscore.structure.seal.unknown", "something not yet known");
    /** The key a structure's state notes each seal opened under, by the seal's packed place. */
    public static final String OPENED = "jscore_opened_seals";

    public KnowledgeSealBlock(final Properties properties,
                              final Supplier<? extends BlockEntityType<? extends Entity>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new Entity(this.type.get(), pos, state);
    }

    /** Whether the team of {@code player} knows what {@code seal} asks, and may open it. */
    public static boolean opensTo(final ServerLevel level, final Player player, final Entity seal) {
        return seal.knowledge().isEmpty() || TeamProgress.knows(level.getServer(),
                CoreTeams.teamOf(level.getServer(), player.getUUID()), seal.knowledge().get());
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof Entity seal)) {
            return InteractionResult.SUCCESS;
        }
        if (!opensTo(server, player, seal)) {
            final Optional<Knowledge> asked = seal.knowledge().flatMap(Knowledges::byId);
            final Text what = asked.map(knowledge -> knowledge.name().text()).orElseGet(UNKNOWN::text);
            player.displayClientMessage(GameText.component(SEALED.with(what)), true);
            return InteractionResult.CONSUME;
        }
        StructureStates.at(server, pos).ifPresent(structure -> StructureStates.update(server, structure, kept -> {
            final CompoundTag opened = kept.getCompound(OPENED);
            opened.putBoolean(Long.toString(pos.asLong()), true);
            kept.put(OPENED, opened);
            return kept;
        }));
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1F, 0.6F);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        return InteractionResult.CONSUME;
    }

    /** The seal's own: what it asks for. */
    public static class Entity extends BlockEntity {

        private @Nullable ResourceLocation knowledge;

        private static final String KNOWLEDGE = "knowledge";

        public Entity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
            super(type, pos, state);
        }

        /** What the seal asks the team to know; none for a seal any team opens. */
        public Optional<ResourceLocation> knowledge() {
            return Optional.ofNullable(this.knowledge);
        }

        /** Seals it behind {@code asked}. */
        public void ask(final @Nullable ResourceLocation asked) {
            this.knowledge = asked;
            setChanged();
        }

        @Override
        protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider lookup) {
            super.loadAdditional(tag, lookup);
            this.knowledge = tag.contains(KNOWLEDGE) ? ResourceLocation.tryParse(tag.getString(KNOWLEDGE)) : null;
        }

        @Override
        protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider lookup) {
            super.saveAdditional(tag, lookup);
            if (this.knowledge != null) {
                tag.putString(KNOWLEDGE, this.knowledge.toString());
            }
        }
    }
}
