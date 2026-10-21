/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.developer.ThingNames;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineSounds;
import dev.jstech.core.text.GameText;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The sounds: what a machine declares it sounds like as it starts, works and stops, and the sounds of the thing's mod
 * named after it, each by the last part of its name; Open opens the mod's {@code sounds.json}. A silent thing has no
 * row.
 */
final class SoundReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.NOTE_BLOCK);
    private static final String SEPARATOR = ", ";

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        final Set<ResourceLocation> sounds = new LinkedHashSet<>();
        machineSounds(thing).ifPresent(declared -> Stream.of(declared.start(), declared.work(), declared.stop())
                .filter(sound -> sound != null).map(SoundKey::id).forEach(sounds::add));
        ModContent.all().stream().filter(content -> content.modid().equals(thing.modid()))
                .flatMap(content -> content.declaredSounds().stream()).map(SoundKey::id)
                .filter(id -> ThingNames.names(id.getPath(), thing.id().getPath())).forEach(sounds::add);
        if (sounds.isEmpty()) {
            return;
        }
        final String named = String.join(SEPARATOR, sounds.stream().map(ResourceLocation::getPath)
                .map(path -> path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('.')) + 1)).toList());
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.SOUNDS), named, StudioSheet.Mark.FINE,
                Optional.of(StudioFiles.asset(ResourceLocation.fromNamespaceAndPath(thing.id().getNamespace(),
                        "sounds.json")))));
    }

    /* A machine's block entity says what it sounds like. */
    private static Optional<MachineSounds> machineSounds(final StudioThing thing) {
        if (thing.kind() != ThingKind.MACHINES || thing.block() == null) {
            return Optional.empty();
        }
        return MadeBlockEntities.of(thing.block()).filter(MachineBlockEntity.class::isInstance)
                .map(made -> ((MachineBlockEntity) made).declaredSounds());
    }
}
