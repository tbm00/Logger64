# Logger64
A spigot plugin that logs players' connection info.

Created by tbm00 for play.mc64.wtf.

## Dependencies
- **Java 17+**: REQUIRED
- **Spigot 1.18.1+**: UNTESTED ON OLDER VERSIONS
- **MySQL**: REQUIRED

## Commands
#### Admin Commands
- `/logger` Display this command list
- `/logger ip <IP>` Get IP's info chart
- `/logger pl <player>` Get player's info chart

## Permissions
#### Admin Permissions
- `logger64.admin` Ability to use admin commands.

## Config
```
database:
  host: 'host'
  port: '3306'
  database: 'db'
  username: 'user'
  password: 'pass'
  options: '?autoReconnect=true'

logger:
  enabled: true
  ticksUntilAuthorized: 36000
```