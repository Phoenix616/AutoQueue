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

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public class QueueListener implements Listener {

    private final AutoQueue plugin;

    public QueueListener(AutoQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        Queue queue = plugin.getQueue(event.getPlayer().getServer() != null ? event.getPlayer().getServer().getInfo() : null, event.getTarget(), event.getReason());

        if (queue != null && !event.getPlayer().hasPermission("autoqueue.bypass") && !event.getPlayer().hasPermission("autoqueue.queue." + queue.getName().toLowerCase() + ".bypass")) {
            if ((event.getReason() == ServerConnectEvent.Reason.PLUGIN && plugin.isImmune(event.getPlayer(), event.getTarget()))
                    || !queue.isActive()) {
                return;
            }
            event.setCancelled(true);
            if (plugin.addToQueue(queue, event.getPlayer(), event.getTarget())) {
                int place = queue.getPlace(event.getPlayer());
                plugin.getLogger().info("Added " + event.getPlayer().getName() + " to queue " + queue.getName() + "(" + place + "/" + queue.getAmount() + ")");
                plugin.sendNotification(event.getPlayer(), "added-to-queue",
                        "queue", queue.getName(),
                        "place", String.valueOf(place),
                        "amount", String.valueOf(queue.getAmount()),
                        "expected-wait", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(place * queue.getDelay()))
                );
            } else {
                plugin.sendNotification(event.getPlayer(), "queue-full", "queue", queue.getName());
            }
        }

    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        plugin.removeFromQueue(event.getPlayer());
    }
}
