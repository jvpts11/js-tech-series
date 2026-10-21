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
import dev.ftb.mods.ftbquests.quest.task.AbstractBooleanTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.jstech.core.progression.Milestones;
import dev.jstech.core.progression.QuestGoals;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** A quest's task met once the player's team passes a milestone, {@code jscore:milestone}. */
final class MilestoneTask extends AbstractBooleanTask {

    private String milestone = "";

    private static final String KEY = "milestone";
    /** The kind, as FTB Quests registered it. */
    static TaskType type;

    MilestoneTask(final long id, final Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public boolean canSubmit(final TeamData teamData, final ServerPlayer player) {
        return player.getServer() != null && QuestGoals.milestone(player.getServer(), player.getUUID(),
                this.milestone);
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return CoreQuestTasks.CHECK_TICKS;
    }

    @Override
    public Component getAltTitle() {
        final ResourceLocation id = ResourceLocation.tryParse(this.milestone);
        final Text name = id == null ? Text.literal(this.milestone) : Milestones.byId(id)
                .map(found -> found.name().text()).orElseGet(() -> Text.literal(this.milestone));
        return GameText.component(QuestTexts.PASS.with(name));
    }

    @Override
    public void writeData(final CompoundTag nbt, final HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString(KEY, this.milestone);
    }

    @Override
    public void readData(final CompoundTag nbt, final HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        this.milestone = nbt.getString(KEY);
    }

    @Override
    public void writeNetData(final RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(this.milestone);
    }

    @Override
    public void readNetData(final RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        this.milestone = buffer.readUtf();
    }

    @Override
    public void fillConfigGroup(final ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString(KEY, this.milestone, value -> this.milestone = value, "");
    }
}
