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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.themoep.bungeeplugin.BungeePlugin;
import de.themoep.minedown.MineDown;
import de.themoep.utils.lang.bungee.LanguageManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class AutoQueue extends BungeePlugin {

    private LanguageManager lang;
    private Queue loginQueue;

    private ScheduledTask notificationTask;

    private final List<Queue> queues = new ArrayList<>();
    private final Cache<UUID, String> immunities = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();
    private final Map<UUID, Queue> playerQueues = new HashMap<>();
    private final Map<String, Integer> serverSlots = new HashMap<>();

    private final Runnable notificationRunnable = () -> {
        if (playerQueues.isEmpty()) {
            notificationTask.cancel();
            notificationTask = null;
        } else {
            for (Map.Entry<UUID, Queue> entry : playerQueues.entrySet()) {
                ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                if (player != null) {
                    Queue queue = entry.getValue();
                    int place = queue.getPlace(player);
                    int amount = queue.getAmount();

                    String indicator = lang.getConfig(player).get("loading-bar.symbol");
                    String loadingBar = lang.getConfig(player).get("loading-bar.format",
                            "before", String.join("", Collections.nCopies(place - 1, indicator)),
                            "player", indicator,
                            "after", String.join("", Collections.nCopies(amount - place, indicator))
                    );

                    long expectedJoinIn = TimeUnit.MILLISECONDS.toSeconds(place * queue.getDelay());
                    sendNotification(player, "in-queue", false,
                            "place", String.valueOf(place),
                            "amount", String.valueOf(amount),
                            "expected-wait", String.valueOf(expectedJoinIn),
                            "loading-bar", loadingBar,
                            "time-in-queue", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - queue.getEntry(player).getEntered()))
                    );
                }
            }
        }
    };

    @Override
    public void onEnable() {
        loadConfig();
        getProxy().getPluginManager().registerListener(this, new QueueListener(this));
        getProxy().getPluginManager().registerCommand(this, new QueueCommand(this));
    }

    void loadConfig() {
        lang = new LanguageManager(this, getConfig().getString("default-locale"));

        loginQueue = loadQueue("Login Queue", getConfig().getSection("login-queue"));
        // TODO: Properly handle moving players to new queues on reload :S
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            removeFromQueue(player);
        }
        for (Queue queue : queues) {
            if (queue.getTask() != null) {
                queue.getTask().cancel();
                queue.setTask(null);
            }
        }
        queues.clear();
        for (String queueName : getConfig().getSection("switch-queues").getKeys()) {
            Queue queue = loadQueue(queueName, getConfig().getSection("switch-queues." + queueName));
            if (queue != null) {
                queues.add(queue);
            }
        }

        serverSlots.clear();
        for (ServerInfo serverInfo : getProxy().getServers().values()) {
            serverInfo.ping((sp, e) -> {
                if (e != null) {
                    getLogger().warning("Could not get max slot count of " + serverInfo.getName() + "! " + e.getMessage());
                } else {
                    getLogger().info("Max slot count of " + serverInfo.getName() + " is " + sp.getPlayers().getMax());
                    serverSlots.put(serverInfo.getName(), sp.getPlayers().getMax());
                }
            });
        }
    }

    private Queue loadQueue(String name, Configuration section) {
        if (!section.contains("delay")) {
            return null;
        }
        return new Queue(
                name,
                section.getLong("delay"),
                section.getInt("max-size", -1),
                section.getStringList("source"),
                section.getStringList("target"),
                section.getInt("target-min-amount", -1),
                section.getInt("target-max-amount", -1),
                section.getStringList("connect-reason")
        );
    }

    public BaseComponent[] getLang(CommandSender sender, String key, String... replacements) {
        MineDown md = new MineDown(lang.getConfig(sender).get(key));
        md.replace(replacements);
        md.replace("prefix", lang.getConfig(sender).get("prefix"));
        return md.toComponent();
    }

    public Queue getLoginQueue() {
        return loginQueue;
    }

    public List<Queue> getQueues() {
        return queues;
    }

    public void sendNotification(ProxiedPlayer player, String key, String... replacements) {
        sendNotification(player, key, true, replacements);
    }

    public void sendNotification(ProxiedPlayer player, String key, boolean fade, String... replacements) {
        if (lang.getConfig(player).contains(key)) {
            player.sendMessage(getLang(player, key, replacements));
        } else {
            if (lang.getConfig(player).contains(key + ".chat")) {
                player.sendMessage(getLang(player, key + ".chat", replacements));
            }
            if (lang.getConfig(player).contains(key + ".actionbar")) {
                player.sendMessage(ChatMessageType.ACTION_BAR, getLang(player, key + ".actionbar", replacements));
            }

            BaseComponent[] title = null;
            if (lang.getConfig(player).contains(key + ".title")) {
                title = getLang(player, key + ".title", replacements);
            }
            BaseComponent[] subTitle = null;
            if (lang.getConfig(player).contains(key + ".subtitle")) {
                subTitle = getLang(player, key + ".subtitle", replacements);
            }
            if (title != null || subTitle != null) {
                Title t = getProxy().createTitle();
                if (title != null) {
                    t.title(title);
                }
                if (subTitle != null) {
                    t.subTitle(subTitle);
                }
                if (!fade) {
                    t.fadeIn(0);
                    t.fadeOut(0);
                }
                player.sendTitle(t);
            }

        }
    }

    public Queue getQueue(ProxiedPlayer player) {
        Queue queue = playerQueues.get(player.getUniqueId());
        if (queue != null) {
            if (queue.contains(player)) {
                return queue;
            }
            playerQueues.remove(player.getUniqueId());
        }
        return null;
    }

    public Queue getQueue(ServerInfo source, ServerInfo target, ServerConnectEvent.Reason reason) {
        if (source == null || reason == ServerConnectEvent.Reason.JOIN_PROXY) {
            return loginQueue;
        }
        for (Queue queue : queues) {
            if (queue.accepts(source, target, reason)) {
                return queue;
            }
        }
        return null;
    }

    public boolean addToQueue(Queue queue, ProxiedPlayer player, ServerInfo target) {
        if (queue.add(player, target)) {
            Queue oldQueue = playerQueues.put(player.getUniqueId(), queue);
            if (oldQueue == queue) {
                return true;
            } else if (oldQueue != null) {
                oldQueue.remove(player);
            }
            if (queue.getTask() == null) {
                queue.setTask(getProxy().getScheduler().schedule(this, () -> {
                    Queue.Entry entry = queue.fetchEntry();
                    if (entry != null && isRoomOnServer(queue, entry, player)) {
                        entry = queue.popEntry();
                        playerQueues.remove(entry.getPlayerId());
                        ProxiedPlayer p = getProxy().getPlayer(entry.getPlayerId());
                        if (p != null) {
                            getLogger().info("Connecting " + p.getName() + " to " + entry.getServer().getName() + " (" + queue.getName() + " - " + queue.getAmount() + " waiting)");
                            immunities.put(p.getUniqueId(), entry.getServer().getName());
                            player.connect(entry.getServer(), (b, e) -> immunities.invalidate(p.getUniqueId()), ServerConnectEvent.Reason.PLUGIN);
                        }
                    }
                }, queue.getDelay(), queue.getDelay(), TimeUnit.MILLISECONDS));
            }
            if (notificationTask == null) {
                notificationTask = getProxy().getScheduler().schedule(this, notificationRunnable, 60 * 20, 60 * 20, TimeUnit.MILLISECONDS);
            }
            return true;
        }
        return false;
    }

    private boolean isRoomOnServer(Queue queue, Queue.Entry entry, ProxiedPlayer player) {
        if (queue.getTargetMaxAmount() > 0
                && entry.getServer().getPlayers().size() >= queue.getTargetMaxAmount()
                && !player.hasPermission("autoqueue.bypass.maxplayers")) {
            return false;
        }

        if (entry.getServer().getPlayers().size() >= serverSlots.getOrDefault(entry.getServer().getName(), Integer.MAX_VALUE)) {
            return false;
        }
        return true;
    }

    public Queue removeFromQueue(ProxiedPlayer player) {
        Queue queue = playerQueues.remove(player.getUniqueId());
        if (queue != null && queue.remove(player)) {
            return queue;
        }
        return null;
    }

    boolean isImmune(ProxiedPlayer player, ServerInfo target) {
        return target.getName().equals(immunities.getIfPresent(player));
    }
}
