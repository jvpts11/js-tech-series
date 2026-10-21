/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.ftbquests;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.QuestGoals;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A quest's task met as the player's team learns a piece of knowledge, {@code jscore:knowledge}: its progress is how
 * much of it the team knows, in hundredths, so knowledge learnt bit by bit fills the task bit by bit.
 */
final class KnowledgeTask extends Task {

    private String knowledge = "";

    private static final String KEY = "knowledge";
    private static final long WHOLE = 100L;
    /** The kind, as FTB Quests registered it. */
    static TaskType type;

    KnowledgeTask(final long id, final Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public long getMaxProgress() {
        return WHOLE;
    }

    @Override
    public String formatMaxProgress() {
        return WHOLE + "%";
    }

    @Override
    public String formatProgress(final TeamData teamData, final long progress) {
        return progress + "%";
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return CoreQuestTasks.CHECK_TICKS;
    }

    @Override
    public void submitTask(final TeamData teamData, final ServerPlayer player, final ItemStack craftedItem) {
        if (teamData.isCompleted(this) || !checkTaskSequence(teamData) || player.getServer() == null) {
            return;
        }
        final long known = Math.round(QuestGoals.knowledge(player.getServer(), player.getUUID(), this.knowledge)
                * WHOLE);
        if (known > teamData.getProgress(this)) {
            teamData.setProgress(this, known);
        }
    }

    @Override
    public Component getAltTitle() {
        final ResourceLocation id = ResourceLocation.tryParse(this.knowledge);
        final Text name = id == null ? Text.literal(this.knowledge) : Knowledges.byId(id)
                .map(found -> found.name().text()).orElseGet(() -> Text.literal(this.knowledge));
        return GameText.component(QuestTexts.KNOW.with(name));
    }

    @Override
    public void writeData(final CompoundTag nbt, final HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString(KEY, this.knowledge);
    }

    @Override
    public void readData(final CompoundTag nbt, final HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        this.knowledge = nbt.getString(KEY);
    }

    @Override
    public void writeNetData(final RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(this.knowledge);
    }

    @Override
    public void readNetData(final RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        this.knowledge = buffer.readUtf();
    }

    @Override
    public void fillConfigGroup(final ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString(KEY, this.knowledge, value -> this.knowledge = value, "");
    }
}
