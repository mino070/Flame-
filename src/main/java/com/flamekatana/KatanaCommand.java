package com.flamekatana;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KatanaCommand implements TabExecutor {

    private final FlameKatana plugin;

    public KatanaCommand(FlameKatana plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "give" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "give" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                        return true;
                    }
                } else if (sender instanceof Player self) {
                    target = self;
                } else {
                    sender.sendMessage(ChatColor.RED + "Console must use: /katana give <player>");
                    return true;
                }

                target.getInventory().addItem(plugin.katana().create()).values()
                        .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));

                target.sendMessage(ChatColor.GOLD + "You received the Flame Katana. Hold it and press F!");
                if (target != sender) {
                    sender.sendMessage(ChatColor.GREEN + "Gave the Flame Katana to " + target.getName() + ".");
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "FlameKatana config reloaded.");
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <give|reload> [player]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("give", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
