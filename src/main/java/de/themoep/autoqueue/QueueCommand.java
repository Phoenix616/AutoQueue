package de.themoep.autoqueue;

/*
 * AutoQueue
 * Copyright (C) 2020. Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.IOException;

public class QueueCommand extends PluginCommand<AutoQueue> {

    public QueueCommand(AutoQueue plugin) {
        super(plugin, "autoqueue");
    }

    @Override
    protected boolean run(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if (sender instanceof ProxiedPlayer && "leave".equalsIgnoreCase(args[0]) && sender.hasPermission(getPermission() + ".leave")) {
                Queue queue = plugin.removeFromQueue((ProxiedPlayer) sender);
                if (queue != null) {
                    sender.sendMessage(plugin.getLang(sender, "queue-left", "queue", queue.getName()));
                } else {
                    sender.sendMessage(plugin.getLang(sender, "no-active-queue"));
                }
                return true;
            } else if ("status".equalsIgnoreCase(args[0]) && sender.hasPermission(getPermission() + ".status")) {
                if (plugin.getLoginQueue() != null) {
                    sender.sendMessage(plugin.getLoginQueue().getName() + " (" + plugin.getLoginQueue().getAmount() + ")");
                }
                for (Queue queue : plugin.getQueues()) {
                    sender.sendMessage(queue.getName() + " (" + queue.getAmount() + ")");
                }
                return true;
            } else if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission(getPermission() + ".reload")) {
                try {
                    plugin.getConfig().loadConfig();
                } catch (IOException e) {
                    e.printStackTrace();
                    sender.sendMessage(plugin.getLang(sender, "error", "error", e.getMessage()));
                }
                plugin.loadConfig();
                return true;
            }
        }
        return false;
    }
}
