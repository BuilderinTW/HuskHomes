package net.william278.huskhomes.command;

import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an abstract cross-platform representation for a plugin command
 */
public abstract class CommandBase {

    /**
     * The input string to match for this command
     */
    public final String command;

    /**
     * The permission node required to use this command
     */
    public final String permission;

    /**
     * Alias input strings for this command
     */
    public final String[] aliases;

    /**
     * Instance of the implementing plugin
     */
    public final HuskHomes plugin;


    public CommandBase(@NotNull String command, @NotNull String permission, @NotNull HuskHomes implementor, String... aliases) {
        this.command = command;
        this.permission = permission;
        this.plugin = implementor;
        this.aliases = aliases;
    }

    /**
     * Fires when the command is executed
     *
     * @param player {@link Player} executing the command
     * @param args   Command arguments
     */
    public abstract void onExecute(@NotNull Player player, @NotNull String[] args);

    /**
     * What should be returned when the player attempts to TAB-complete the command
     *
     * @param player {@link Player} doing the TAB completion
     * @param args   Current command arguments
     * @return List of String arguments to offer TAB suggestions
     */
    public abstract List<String> onTabComplete(@NotNull Player player, @NotNull String[] args);

    /**
     * Returns the localised description string of this command
     *
     * @return the command description
     */
    public String getDescription() {
        return plugin.getLocales().getRawLocale(command + "_command_description")
                .orElse("A HuskHomes command");
    }

}
