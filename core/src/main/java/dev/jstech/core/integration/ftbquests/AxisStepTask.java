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
import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import dev.jstech.core.progression.QuestGoals;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A quest's task met once the player's team reaches a step along a progression axis, or a step past it,
 * {@code jscore:axis_step}: the hardware era {@code legacy}, an industrial tier, any axis a mod declares.
 */
final class AxisStepTask extends AbstractBooleanTask {

    private String axis = "";
    private String step = "";

    private static final String AXIS_KEY = "axis";
    private static final String STEP_KEY = "step";
    /** The kind, as FTB Quests registered it. */
    static TaskType type;

    AxisStepTask(final long id, final Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public boolean canSubmit(final TeamData teamData, final ServerPlayer player) {
        return player.getServer() != null && QuestGoals.step(player.getServer(), player.getUUID(), this.axis,
                this.step);
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return CoreQuestTasks.CHECK_TICKS;
    }

    @Override
    public Component getAltTitle() {
        final ResourceLocation id = ResourceLocation.tryParse(this.axis);
        final @Nullable ProgressionAxis<?> found = id == null ? null : ProgressionAxes.byId(id);
        if (found == null) {
            return GameText.component(QuestTexts.STEP.with(Text.literal(this.axis), Text.literal(this.step)));
        }
        final @Nullable IAxisStep named = found.byName(this.step);
        return GameText.component(QuestTexts.STEP.with(found.text(), named == null ? Text.literal(this.step)
                : named.text()));
    }

    @Override
    public void writeData(final CompoundTag nbt, final HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString(AXIS_KEY, this.axis);
        nbt.putString(STEP_KEY, this.step);
    }

    @Override
    public void readData(final CompoundTag nbt, final HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        this.axis = nbt.getString(AXIS_KEY);
        this.step = nbt.getString(STEP_KEY);
    }

    @Override
    public void writeNetData(final RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(this.axis);
        buffer.writeUtf(this.step);
    }

    @Override
    public void readNetData(final RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        this.axis = buffer.readUtf();
        this.step = buffer.readUtf();
    }

    @Override
    public void fillConfigGroup(final ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString(AXIS_KEY, this.axis, value -> this.axis = value, "");
        config.addString(STEP_KEY, this.step, value -> this.step = value, "");
    }
}
