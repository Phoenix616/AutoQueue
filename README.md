# AutoQueue

BungeeCord plugin to automatically queue players that try to join your network or certain servers.

## Commands

Aliases: `/autoqueue`, `/aq`, `/queue`, `/q`

| Command       | Description                                               |
|---------------|-----------------------------------------------------------|
| `/aq leave`   | Leave the current queue                                   |
| `/aq status`  | Show the players in all queues                            |
| `/aq reload`  | Reload the config, removes all players from their queues! |

## Permissions

| Permission                    | Description                       |
|-------------------------------|-----------------------------------|
| autoqueue.command             | Access the command                |
| autoqueue.command.leave       | Access the leave command          |
| autoqueue.command.status      | Access the status command         |
| autoqueue.command.reload      | Access the reload command         |
| autoqueue.bypass              | Bypass queues                     |
| autoqueue.bypass.maxplayers   | Bypass max player restrictions    |
| autoqueue.queue.<name>.bypass | Bypass a specific queue           |

## Config

```yaml
default-locale: en
# Automatically enter the player into a queue to join a server when joining
# Usefull if you want to put a void server as a queue in front of your main one
# Leave empty/invalid if you don't want that
login-queue:
  # Delay between joins
  delay: 500
  # The target server to send the player to
  target:
  - freebuild
  # Optional: Only start this queue once there is this amount of players on the target server
  target-amount: 10
switch-queues:
  examplequeue:
    # Delay between joins in milliseconds
    delay: 500
    # Optional: Maximum amount of players that can be in this queue at the same time
    max-size: 50
    # Optional: Source server list
    source:
    - lobby
    # Optional: Target server list
    # Automatically triggers when player tries to switch from any source server to any target server
    target:
    - freebuild
    # Optional: Only start this queue once there is this amount of players on the target server
    target-min-amount: 10
    # Optional: Only allow joining via the queue up to this many players on the target server
    target-max-amount: 50
    # Optional: The ServerConnectEvent.Reason for this server switch
    connect-reason:
    - COMMAND
    - PLUGIN_MESSAGE
    - PLUGIN
    - UNKNOWN
```

Language configs can be found [here](https://github.com/Phoenix616/AutoQueue/tree/master/src/main/resources/languages). 
By default English and German language files are available.

## Downloads

Downloads are available from the Minebench.de Jenkins Server: https://ci.minebench.de/job/AutoQueue/ ([direct](https://ci.minebench.de/job/AutoQueue/lastSuccessfulBuild/artifact/target/AutoQueue.jar))

## License

```
Copyright (C) 2020. Max Lee aka Phoenix616 (mail@moep.tv)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
