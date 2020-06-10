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

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Queue {
    private final String name;
    private final long delay;
    private final int maxSize;
    private final Set<String> sourceServers;
    private final Set<String> targetServers;
    private final int targetMinAmount;
    private final int targetMaxAmount;
    private final Set<ServerConnectEvent.Reason> reasons;

    private final Deque<Entry> priorityQueue = new ArrayDeque<>();
    private final Deque<Entry> queue = new ArrayDeque<>();

    private ScheduledTask task;
    private long lastCheck = 0;
    private long lastJoin = 0;

    public Queue(String name, long delay, int maxSize, List<String> sourceServers, List<String> targetServers, int targetMinAmount, int targetMaxAmount, List<String> reasons) {
        this.name = name;
        this.delay = delay;
        this.maxSize = maxSize;
        this.sourceServers = sourceServers.stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.targetServers = targetServers.stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.targetMinAmount = targetMinAmount;
        this.targetMaxAmount = targetMaxAmount;
        this.reasons = EnumSet.noneOf(ServerConnectEvent.Reason.class);
        for (String reason : reasons) {
            try {
                this.reasons.add(ServerConnectEvent.Reason.valueOf(reason.toUpperCase()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return name;
    }

    public long getDelay() {
        return delay;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public Set<String> getSourceServers() {
        return sourceServers;
    }

    public Set<String> getTargetServers() {
        return targetServers;
    }

    public int getTargetMinAmount() {
        return targetMinAmount;
    }

    public int getTargetMaxAmount() {
        return targetMaxAmount;
    }

    public Set<ServerConnectEvent.Reason> getReasons() {
        return reasons;
    }

    boolean add(ProxiedPlayer player, ServerInfo target) {
        if (maxSize > -1 && getAmount() >= maxSize) {
            return false;
        }
        Entry entry = new Entry(player, target);
        if (player.hasPermission("autoqueue.priority") || player.hasPermission("autoqueue.queue." + getName().toLowerCase() + ".priority")) {
            priorityQueue.addLast(entry);
        } else {
            queue.addLast(entry);
        }
        return true;
    }

    boolean remove(ProxiedPlayer player) {
        boolean removed = priorityQueue.removeIf(e -> e.playerId.equals(player.getUniqueId()));
        removed |= queue.removeIf(e -> e.playerId.equals(player.getUniqueId()));
        checkEmpty();
        return removed;
    }

    public boolean contains(ProxiedPlayer player) {
        return getEntry(player) != null;
    }

    public Entry getEntry(ProxiedPlayer player) {
        for (Entry entry : queue) {
            if (entry.playerId.equals(player.getUniqueId())) {
                return entry;
            }
        }
        for (Entry entry : priorityQueue) {
            if (entry.playerId.equals(player.getUniqueId())) {
                return entry;
            }
        }
        return null;
    }

    public boolean accepts(ServerInfo source, ServerInfo target, ServerConnectEvent.Reason reason) {
        if (!sourceServers.isEmpty() && (source == null || !sourceServers.contains(source.getName().toLowerCase()))) {
            return false;
        }
        if (!targetServers.isEmpty() && !targetServers.contains(target.getName().toLowerCase())) {
            return false;
        }
        if (!reasons.isEmpty() && !reasons.contains(reason)) {
            return false;
        }
        return true;
    }

    public boolean matchesTargetAmount(ServerInfo target) {
        return targetMinAmount <= -1 || target.getPlayers().size() >= targetMinAmount;
    }

    public int getAmount() {
        return priorityQueue.size() + queue.size();
    }

    public int getPlace(ProxiedPlayer player) {
        int i = 0;
        for (Entry entry : priorityQueue) {
            i++;
            if (entry.playerId.equals(player.getUniqueId())) {
                return i;
            }
        }
        for (Entry entry : queue) {
            i++;
            if (entry.playerId.equals(player.getUniqueId())) {
                return i;
            }
        }
        return -1;
    }

    public ScheduledTask getTask() {
        return task;
    }

    public void setTask(ScheduledTask task) {
        this.task = task;
    }

    Entry fetchEntry() {
        Entry entry = priorityQueue.getFirst();
        if (entry == null) {
            entry = queue.getFirst();
        }
        return entry;
    }

    Entry popEntry() {
        Entry entry = priorityQueue.pollFirst();
        if (entry == null) {
            entry = queue.pollFirst();
        }
        checkEmpty();
        lastJoin = System.currentTimeMillis();
        return entry;
    }

    private void checkEmpty() {
        if (getAmount() == 0) {
            task.cancel();
            task = null;
        }
    }

    boolean isActive() {
        if (task != null) {
            return true;
        }
        boolean r = lastCheck + delay >= System.currentTimeMillis();
        lastCheck = System.currentTimeMillis();
        return r;
    }

    public long getLastJoin() {
        return lastJoin;
    }

    class Entry {
        private final UUID playerId;
        private final ServerInfo server;
        private final long entered = System.currentTimeMillis();

        public Entry(ProxiedPlayer player, ServerInfo server) {
            this.playerId = player.getUniqueId();
            this.server = server;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public ServerInfo getServer() {
            return server;
        }

        public long getEntered() {
            return entered;
        }
    }
}
