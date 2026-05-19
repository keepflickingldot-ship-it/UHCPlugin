# UHCPlugin (Paper 1.20 – 1.21)

Complete Ultra Hardcore plugin. Works on cracked/offline servers.

## Build

Requires JDK 17+ and Maven.

```
mvn package
```

Produces `target/UHCPlugin.jar`. Drop it into your server's `plugins/` folder
and restart. Configure via `plugins/UHCPlugin/config.yml` and `messages.yml`.

## Commands

See `/uhc help` in-game. Highlights:

- `/uhc menu` – open clickable GUI
- `/uhc start` / `/uhc stop`
- `/uhc setcenter`, `/uhc setborder <start> <end> <time>`
- `/uhc grace <seconds>`, `/uhc pvp on|off`
- `/uhc team create|join|leave|list`
- `/uhc scenario <name> on|off`
- `/uhc kit <name>`, `/uhc revive <player>`, `/uhc stats`, `/uhc reload`

## Scenarios

`cutclean`, `speeduhc`, `nofall`, `timebomb`, `blooddiamond`, `vanilla`.

## Team chat

Prefix a message with `!` to chat to your team only.
