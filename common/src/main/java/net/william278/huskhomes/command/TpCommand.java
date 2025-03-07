/*
 * This file is part of HuskHomes, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskhomes.command;

import com.google.common.collect.Lists;
import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.position.Position;
import net.william278.huskhomes.teleport.*;
import net.william278.huskhomes.user.CommandUser;
import net.william278.huskhomes.user.OnlineUser;
import net.william278.huskhomes.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class TpCommand extends Command implements TabCompletable {

    protected TpCommand(@NotNull HuskHomes plugin) {
        super(
                List.of("tp", "tpo"),
                "[<player|position>] [target]",
                plugin
        );

        addAdditionalPermissions(Map.of("coordinates", true, "other", true));
        setOperatorCommand(true);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        switch (args.length) {
            case 1 -> {
                if (!(executor instanceof OnlineUser user)) {
                    plugin.getLocales().getLocale("error_in_game_only")
                            .ifPresent(executor::sendMessage);
                    return;
                }
                //BTW 伺服器專用權限，用於防止玩家惡意TP
                if (!executor.hasPermission(getPermission("player"))) {
                    plugin.getCommand(TpRequestCommand.class).get().execute(executor, args);
                    //plugin.getLocales().getLocale("error_no_permission")
                    //      .ifPresent(executor::sendMessage);
                    return;
                }
                this.execute(executor, user, Target.username(args[0]), args);
            }
            case 2 -> this.execute(executor, Teleportable.username(args[0]), Target.username(args[1]), args);
            default -> {
                final Position basePosition = getBasePosition(executor);
                Optional<Position> target = executor.hasPermission(getPermission("coordinates"))
                        ? parsePositionArgs(basePosition, args, 0) : Optional.empty();
                if (target.isPresent()) {
                    if (!(executor instanceof OnlineUser user)) {
                        plugin.getLocales().getLocale("error_in_game_only")
                                .ifPresent(executor::sendMessage);
                        return;
                    }

                    this.execute(executor, user, target.get(), args);
                    return;
                }

                target = executor.hasPermission(getPermission("coordinates"))
                        ? parsePositionArgs(basePosition, args, 1) : Optional.empty();
                if (target.isPresent() && args.length >= 1) {
                    this.execute(executor, Teleportable.username(args[0]), target.get(), args);
                    return;
                }

                plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                        .ifPresent(executor::sendMessage);
            }
        }
    }

    // Execute a teleport
    private void execute(@NotNull CommandUser executor, @NotNull Teleportable teleporter, @NotNull Target target,
                         @NotNull String[] args) {
        // Build and execute the teleport
        final TeleportBuilder builder = Teleport.builder(plugin)
                .teleporter(teleporter)
                .target(target);

        // Determine teleporter and target names, validate permissions
        final @Nullable String targetName = target instanceof Username username ? username.name()
                : target instanceof OnlineUser online ? online.getName() : null;
        if (executor instanceof OnlineUser online) {
            if (online.equals(teleporter)) {
                if (teleporter.getName().equalsIgnoreCase(targetName)) {
                    plugin.getLocales().getLocale("error_cannot_teleport_self")
                            .ifPresent(executor::sendMessage);
                    return;
                }
            } else if (!executor.hasPermission(getPermission("other"))) {
                plugin.getLocales().getLocale("error_no_permission")
                        .ifPresent(executor::sendMessage);
                return;
            }
            builder.executor(online);
        }

        // Execute teleport
        if (!builder.buildAndComplete(false, args)) {
            return;
        }

        // Display the teleport completion message
        if (target instanceof Position position) {
            plugin.getLocales().getLocale("teleporting_other_complete_position", teleporter.getName(),
                            Integer.toString((int) position.getX()), Integer.toString((int) position.getY()),
                            Integer.toString((int) position.getZ()))
                    .ifPresent(executor::sendMessage);
            return;
        }
        plugin.getLocales().getLocale("teleporting_other_complete",
                        teleporter.getName(), Objects.requireNonNull(targetName))
                .ifPresent(executor::sendMessage);
    }

    @Override
    @NotNull
    public final List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        final Position relative = getBasePosition(user);
        final boolean serveCoordinateCompletions = user.hasPermission(getPermission("coordinates"));
        switch (args.length) {
            case 0, 1 -> {
                final ArrayList<String> completions = Lists.newArrayList(serveCoordinateCompletions
                        ? List.of("~", "~ ~", "~ ~ ~",
                        Integer.toString((int) relative.getX()),
                        ((int) relative.getX() + " " + (int) relative.getY()),
                        ((int) relative.getX() + " " + (int) relative.getY() + " " + (int) relative.getZ()))
                        : List.of());
                plugin.getUserList().stream().map(User::getName).forEach(completions::add);
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args.length == 1 ? args[0].toLowerCase() : ""))
                        .sorted().collect(Collectors.toList());
            }
            case 2 -> {
                final ArrayList<String> completions = new ArrayList<>();
                if (isCoordinate(args, 0)) {
                    completions.addAll(List.of("~", Integer.toString((int) relative.getY())));
                    completions.addAll(List.of("~ ~", (int) relative.getY() + " " + (int) relative.getZ()));
                } else {
                    completions.addAll(serveCoordinateCompletions
                            ? List.of("~", "~ ~", "~ ~ ~",
                            Integer.toString((int) relative.getX()),
                            ((int) relative.getX() + " " + (int) relative.getY()),
                            ((int) relative.getX() + " " + (int) relative.getY() + " " + (int) relative.getZ()))
                            : List.of()
                    );
                    if (user.hasPermission(getPermission("other"))) {
                        plugin.getUserList().stream().map(User::getName).forEach(completions::add);
                    }
                }
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .sorted().collect(Collectors.toList());
            }
            case 3 -> {
                final ArrayList<String> completions = new ArrayList<>();
                if (isCoordinate(args, 1) && isCoordinate(args, 2)) {
                    if (!serveCoordinateCompletions) {
                        return completions;
                    }
                    completions.addAll(List.of("~", Integer.toString((int) relative.getZ())));
                } else if (isCoordinate(args, 1)) {
                    if (!serveCoordinateCompletions) {
                        return completions;
                    }
                    completions.addAll(List.of("~", Integer.toString((int) relative.getY())));
                    completions.addAll(List.of("~ ~", (int) relative.getY() + " " + (int) relative.getZ()));
                }
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .sorted().collect(Collectors.toList());
            }
            case 4 -> {
                final ArrayList<String> completions = new ArrayList<>();
                if (isCoordinate(args, 1) && isCoordinate(args, 2) && !isCoordinate(args, 0)) {
                    if (!serveCoordinateCompletions) {
                        return completions;
                    }
                    completions.addAll(List.of("~", Integer.toString((int) relative.getZ())));
                }
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .sorted().collect(Collectors.toList());
            }
            default -> {
                return List.of();
            }
        }
    }

    private boolean isCoordinate(@NotNull String[] args, int index) {
        return parseCoordinateArg(args, index, 0d).isPresent();
    }

}
