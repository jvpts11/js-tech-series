/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.edit;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.command.CommandRoot;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * {@code /jstech edit}, the large edits from the chat: {@code export <name> <from> <to>} saves a box of the world as
 * the game's structure file {@code name}; {@code import <name> <at> [<rotation>]} sets a structure file's blocks there
 * as a large edit, a share each tick, keeping what they replace; {@code undo} puts back what the last such edit
 * replaced, and undone again does it once more. The console's edits are undone from the console.
 */
public final class EditCommands {

    private static final String NAME = "name";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String AT = "at";
    private static final String ROTATION = "rotation";

    private EditCommands() {
    }

    /** Declares the command, from the Core's constructor. */
    public static void declare() {
        CommandRoot.CORE.declare("edit", CommandRoot.GAME_MASTERS, (node, context) -> node
                .then(Commands.literal("export")
                        .then(Commands.argument(NAME, ResourceLocationArgument.id())
                                .then(Commands.argument(FROM, BlockPosArgument.blockPos())
                                        .then(Commands.argument(TO, BlockPosArgument.blockPos())
                                                .executes(EditCommands::export)))))
                .then(Commands.literal("import")
                        .then(Commands.argument(NAME, ResourceLocationArgument.id())
                                .then(Commands.argument(AT, BlockPosArgument.blockPos())
                                        .executes(source -> importFile(source, Rotation.NONE))
                                        .then(Commands.argument(ROTATION, TemplateRotationArgument.templateRotation())
                                                .executes(source -> importFile(source, TemplateRotationArgument
                                                        .getRotation(source, ROTATION)))))))
                .then(Commands.literal("undo").executes(EditCommands::undo)));
    }

    private static int export(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, NAME);
        final BoundingBox box = BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, FROM),
                BlockPosArgument.getLoadedBlockPos(context, TO));
        if (!WorldEdits.export(context.getSource().getLevel(), box, id)) {
            return fail(context, EditTexts.NOT_EXPORTED.with(id));
        }
        final long blocks = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
        return answer(context, EditTexts.EXPORTED.with(blocks, id));
    }

    private static int importFile(final CommandContext<CommandSourceStack> context, final Rotation rotation)
            throws CommandSyntaxException {
        final ResourceLocation id = ResourceLocationArgument.getId(context, NAME);
        final ServerLevel level = context.getSource().getLevel();
        final BlockPos at = BlockPosArgument.getLoadedBlockPos(context, AT);
        final Optional<EditPlan.Builder> plan = WorldEdits.planFrom(level, id, at, rotation);
        if (plan.isEmpty()) {
            return fail(context, EditTexts.NO_FILE.with(id));
        }
        final EditPlan made = plan.get().undoable().build();
        if (WorldEdits.applyFor(level, who(context), made).isEmpty()) {
            return fail(context, EditTexts.AT_LIMIT.text());
        }
        return answer(context, EditTexts.IMPORTING.with(made.size(), id));
    }

    private static int undo(final CommandContext<CommandSourceStack> context) {
        final Optional<WorldEdit> last = WorldEdits.lastFor(who(context));
        if (last.isEmpty()) {
            return fail(context, EditTexts.NOTHING_TO_UNDO.text());
        }
        if (!last.get().over()) {
            return fail(context, EditTexts.STILL_UNDER_WAY.text());
        }
        final Optional<EditPlan> back = last.get().undo();
        if (back.isEmpty()) {
            return fail(context, EditTexts.NOTHING_TO_UNDO.text());
        }
        if (WorldEdits.applyFor(last.get().level(), who(context), back.get()).isEmpty()) {
            return fail(context, EditTexts.AT_LIMIT.text());
        }
        return answer(context, EditTexts.UNDOING.with(back.get().size()));
    }

    /* Whose edits these are: the player's, or the console's. */
    private static UUID who(final CommandContext<CommandSourceStack> context) {
        final ServerPlayer player = context.getSource().getPlayer();
        return player == null ? Util.NIL_UUID : player.getUUID();
    }

    private static int answer(final CommandContext<CommandSourceStack> context, final Text said) {
        context.getSource().sendSuccess(() -> GameText.component(said), true);
        return 1;
    }

    private static int fail(final CommandContext<CommandSourceStack> context, final Text said) {
        context.getSource().sendFailure(GameText.component(said));
        return 0;
    }
}
