/*
 * BrokerAPI Copyright 2020 Justis Root
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.gmail.justisroot.broker.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.gmail.justisroot.broker.SpigotInitializer;
import com.google.common.collect.Lists;

public class BrokerCommands implements CommandExecutor, TabCompleter {

	private static final String COMMAND = "brokerapi";

	private final SpigotInitializer plugin;

	public BrokerCommands(SpigotInitializer plugin) {
		this.plugin = plugin;
		plugin.getCommand(COMMAND).setExecutor(this);
		plugin.getCommand(COMMAND).setTabCompleter(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission(command.getPermission())) {
			sender.sendMessage(ChatColor.RED + command.getPermissionMessage());
			return true;
		}
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("reload")) {
				plugin.reload();
				message(sender, "BrokerAPI has reloaded with the current configuration");
				return true;
			} else if (args[0].equalsIgnoreCase("list")) {
				Map<String, Set<String>> available = plugin.available();
				if (available.isEmpty()) {
					sender.sendMessage(ChatColor.YELLOW + "There are no registered Broker providers.");
					return false;
				}
				sender.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "Registered Broker providers:");
				for (Entry<String, Set<String>> map : plugin.available().entrySet()) {
					sender.sendMessage(ChatColor.GOLD + map.getKey() + ":");
					for (String broker : map.getValue())
						sender.sendMessage(ChatColor.GOLD + " - " + broker);
				}
				return true;
			}
		}
		message(sender, "BrokerAPI | Available Commands:", "/brokerapi reload", "/brokerapi list");
		return true;
	}

	private static final void message(CommandSender sender, String... messages) {
		for (String message : messages) sender.sendMessage(ChatColor.GOLD + message);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) return Arrays.asList(" ");
		else if (args.length == 1) return Arrays.asList("reload", "list");
		return Lists.newArrayList();
	}

}
