package org.lilbrocodes.motivate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class MotivateCommands implements CommandExecutor, TabCompleter {
    private final Motivate plugin;
    public MotivateCommands(Motivate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args[0].equalsIgnoreCase("list")) {
            handleListCommand(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("show")) {
            try {
                handleShowCommand(sender, Integer.parseInt(args[1]));
                return true;
            } catch (NumberFormatException e) {
                plugin.sendMessage(sender, "Argument 'id' must be a valid integer.");
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("remove")) {
            try {
                handleRemoveCommand(sender, Integer.parseInt(args[1]) - 1);
                return true;
            } catch (NumberFormatException e) {
                plugin.sendMessage(sender, "Argument 'id' must be a valid integer.");
                return true;
            } catch (IOException e) {
                plugin.sendMessage(sender, "Failed to save config file.");
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfigC();
            plugin.sendMessage(sender, "Reloaded config.");
            return true;
        }

        return false;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return  List.of("list", "remove", "show", "reload");
        }
        if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("remove")) {
            List<Map<?, ?>> messages = plugin.config.getMapList("messages");
            List<String> tab = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                tab.add(String.valueOf(i + 1));
            }
            return tab;
        }
        return List.of();
    }
    
    private void handleListCommand(@NotNull CommandSender sender) {
        String hostName = "";
        if (sender instanceof Player player) hostName = Objects.requireNonNull(player.getAddress()).getHostName().replace(".", "_");

        plugin.reloadConfigC();
        List<Map<?, ?>> messages = plugin.config.getMapList("messages");
        plugin.sendMessage(sender, "Messages:");
        int id = 1;
        for (Map<?, ?> message : messages) {
            sender.sendMessage(String.format("§6§l|§r Message %d:", id));
            sender.sendMessage(String.format("§6§l|§r     %s", plugin.getMOTD(message, hostName).primary));
            sender.sendMessage(String.format("§6§l|§r     %s", plugin.getMOTD(message, hostName).secondary));
            id++;
        }
    }

    private void handleShowCommand(@NotNull CommandSender sender, int id) {
        String hostName = "";
        if (sender instanceof Player player) hostName = Objects.requireNonNull(player.getAddress()).getHostName().replace(".", "_");

        plugin.reloadConfigC();
        List<Map<?, ?>> messages = plugin.config.getMapList("messages");
        if (messages.size() <= id - 1) {
            plugin.sendMessage(sender, String.format("Message #%d not found.", id));
            return;
        }
        Map<?, ?> message = messages.get(id - 1);
        sender.sendMessage(String.format("§6§l|§r Message %d:", id));
        sender.sendMessage(String.format("§6§l|§r     %s", plugin.getMOTD(message, hostName).primary));
        sender.sendMessage(String.format("§6§l|§r     %s", plugin.getMOTD(message, hostName).secondary));
    }

    private void handleRemoveCommand(@NotNull CommandSender sender, int id) throws IOException {
        plugin.reloadConfigC();
        List<Map<?, ?>> messages = plugin.config.getMapList("messages");
        if (messages.size() <= id) {
            plugin.sendMessage(sender, String.format("Message #%d not found.", id));
            return;
        }
        messages.remove(id);
        plugin.config.set("messages", messages);
        plugin.writeConfig();
        plugin.sendMessage(sender, String.format("Successfully removed message #%d.", id));
    }
}
