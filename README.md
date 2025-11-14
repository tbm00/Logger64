# Logger64
A spigot plugin that logs players' connection info.

Created by tbm00 for play.mc64.wtf. Highly recommended for cracked/offline servers.

## Features
- **Log Every Player & Every IP** Creates a database storing a history of every authenticated IP-Player relation.
- **Retrieve IPs by Player, or Vice-a-Versa** Find all IPs a player has used, or all usernames an IP has used!
- **Simple Yet Powerful** Easy to use commands allow for admins to quickly find players on the same network!
- **Configuable Save Delay** To ensure the player is authenticated before saving the IP & player entries.
- **Asynchronous MySQL** With configurable Hikari Connection Pool options.
- **Powerful Blacklist** Blacklist IP ranges (either for everyone, or for only unseen/unregistered players)

## Dependencies
- **Java 17+**: REQUIRED
- **Spigot 1.18.1+**: UNTESTED ON OLDER VERSIONS
- **MySQL**: REQUIRED
- **AuthMe**: OPTIONAL

## Commands & Permissions
### Commands
- `/logger seen <username>` Display player's join info
- `/logger user <username>` Get player's info table
- `/logger ip <IP>` Get IP's info table
- `/logger cidr <network>/<prefix>` Get table of IPs on CIDR block
- `/logger cidrp <username> [prefix]` Get table of IPs on each CIDR block player has used

### Permissions
- `logger64.seen` Ability to use seen command *(Default: OP)*
- `logger64.admin` Ability to use other commands *(Default: OP)*

## Default Config
```# Logger64 v0.1.10-beta by @tbm00
# https://github.com/tbm00/Logger64

hook:
  AuthMe: true

mysql:
  host: 'host'
  port: 3306
  database: 'db'
  username: 'user'
  password: 'pass'
  useSSL: false
  hikari:
    maximumPoolSize: 16
    minimumPoolSize: 2
    idleTimeout: 240 # 4 minutes
    connectionTimeout: 30 # 30 seconds
    maxLifetime: 1800 # 30 minutes
    leakDetection:
      enabled: false
      threshold: 2 # 2 seconds

logger:
  enabled: true
  # logJoinEventMethod - When does the player's connection get logged?
  # immediate: as soon as they join
  # timer: after timerTicks, if they're still online
  # authme: after authenticating via AuthMe
  logJoinEventMethod: authme
  timerTicks: 3600 # 3 minutes
  nonLoggedIPs: []
  #- 0.0.0.0
  #- 255.255.255.255

protection:
  playerWhitelist: []
  cidrBlacklistAll: []
  cidrBlacklistUnseen: []
```