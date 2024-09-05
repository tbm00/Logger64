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
- `/logger ip <IP>` Get IP's info table
- `/logger user <username>` Get player's info table
- `/logger seen <username>` Display player's join info

## Permissions
#### Admin Permissions
- `logger64.admin` Ability to use table commands
- `logger64.seen` Ability to use seen command

## Config
```
mysql:
  host: 'host'
  port: 3306
  database: 'db'
  username: 'user'
  password: 'pass'
  useSSL: false

logger:
  enabled: true
  ticksUntilConnectionLogged: 3600
  nonLoggedIPs: []
  #- 0.0.0.0
  #- 255.255.255.255
```