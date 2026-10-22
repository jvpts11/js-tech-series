/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.config.ConfigSync;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.GameText;
import dev.jstech.core.unify.Unification;
import dev.jstech.core.unify.UnifyPreview;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * What unification does to the world's recipes, or would do turned on ({@link Unification#preview}): each tag with
 * the item kept, those it stands for, how many recipes it changes and one of them before and after. Asked with
 * {@link StudioKeys#TURN_ON}, it first turns unification on and loads the world's data again, for a player who may
 * change the server's settings.
 */
final class UnificationService implements IStudioService {

    static final String NAME = "unification";

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        if (!args.getBoolean(StudioKeys.TURN_ON)) {
            reply.accept(written(Unification.preview(player.server)));
            return;
        }
        if (!CorePermissions.has(player, CorePermissions.EDIT_SETTINGS)) {
            reply.accept(refused(GameText.resolve(ConfigSync.REFUSED)));
            return;
        }
        // The answer waits for the data to load again, so it tells what unification changed, not what it would.
        Unification.turnOn(player.server).whenComplete((loaded, failed) -> reply.accept(failed == null
                ? written(Unification.preview(player.server)) : refused(String.valueOf(failed.getMessage()))));
    }

    /** The preview as the studio's answer holds it. */
    static CompoundTag written(final UnifyPreview preview) {
        final CompoundTag out = new CompoundTag();
        out.putBoolean(StudioKeys.ON, preview.on());
        out.putBoolean(StudioKeys.ASIDE, preview.steppedAside());
        out.putBoolean(StudioKeys.DROPS, preview.drops());
        final ListTag prefer = new ListTag();
        preview.prefer().forEach(mod -> prefer.add(StringTag.valueOf(mod)));
        out.put(StudioKeys.PREFER, prefer);
        final ListTag tags = new ListTag();
        for (final UnifyPreview.Tag tag : preview.tags()) {
            final CompoundTag one = new CompoundTag();
            one.putString(StudioKeys.TAG, tag.tag());
            one.putString(StudioKeys.KEPT, tag.kept());
            final ListTag replaced = new ListTag();
            tag.replaced().forEach(item -> replaced.add(StringTag.valueOf(item)));
            one.put(StudioKeys.REPLACED, replaced);
            one.putInt(StudioKeys.COUNT, tag.recipes());
            one.putString(StudioKeys.ID, tag.example());
            one.putString(StudioKeys.BEFORE, tag.before());
            tags.add(one);
        }
        out.put(StudioKeys.ENTRIES, tags);
        return out;
    }

    private static CompoundTag refused(final String why) {
        final CompoundTag out = new CompoundTag();
        out.putString(StudioServices.REFUSED, why);
        return out;
    }
}
