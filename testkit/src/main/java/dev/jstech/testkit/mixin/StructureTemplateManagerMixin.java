/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.mixin;

import dev.jstech.testkit.gametest.Arenas;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Finds an arena made in code where the game looks for a template, before its files: each time it looks, so a reload,
 * which forgets every template read, forgets none of these.
 */
@Mixin(StructureTemplateManager.class)
public abstract class StructureTemplateManagerMixin {

    @Shadow
    @Final
    private HolderGetter<Block> blockLookup;

    @Inject(method = "tryLoad", at = @At("HEAD"), cancellable = true)
    private void jstestkit$arena(final ResourceLocation id,
                                 final CallbackInfoReturnable<Optional<StructureTemplate>> found) {
        if (Arenas.declared(id)) {
            found.setReturnValue(Arenas.template(id, this.blockLookup));
        }
    }
}
