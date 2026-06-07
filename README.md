# xoxo-AntiCheat

> **Fast, smart, and fair — keeping your server clean.**

xoxo-AntiCheat is a lightweight but powerful server-side anti-cheat plugin for Paper/Bukkit servers. Built with performance in mind, it features a novel tick-based flag queue system, instant rubberbanding, and a velocity simulation layer — so detections are accurate and false positives are kept low.

---

##  Features

### 🎯 Tick Flag Queue
Most anti-cheats process one flag and move on. xoxo-AntiCheat is smarter: **a player can only receive one flag per tick**, but if multiple violations are detected within a single tick, they are queued and distributed across subsequent ticks. No detection is ever dropped — every violation is counted.

### ⚡ Instant Rubberbanding
The rubberbanding system triggers **immediately** upon a flag, blocking:
- Movement
- Camera rotation
- Inventory actions

Players are snapped back before they can gain any advantage.

### 🚨 Ban System & Auto-Ban
- Custom, readable **ban messages** that clearly communicate the reason to the player
- Configurable **auto-ban threshold**: once a player accumulates a set number of flags, they are automatically banned

### 📐 Velocity Level (Simulation Layer)
xoxo-AntiCheat includes a Velocity Layer - a simulation component that correctly models player physics. This ensures the anti-cheat understands what movement is legitimate before flagging, drastically reducing false positives in edge-case movement scenarios.

### 🔧 `/xoxo` Command
A built-in management command for server administrators.

| Subcommand | Description |
|---|---|
| `/xoxo exceptions add <player>` | Add a player to the exceptions list — they will not be flagged or rubberbanded |
| `/xoxo exceptions remove <player>` | Remove a player from the exceptions list — they will be flagged and rubberbanded again |
| `/xoxo exceptions get` | List all players currently on the exceptions list |

More subcommands are planned for future releases.

---

## 📦 Installation

1. Download the latest `.jar` from the [Releases](https://modrinth.com) page
2. Drop it into your server's `plugins/` folder
3. Restart your server
4. The plugin stores ban history locally. Ban durations increase with repeated offenses. To reset a player's ban history, remove their record from the generated ban data files.

---

## 🖥️ Compatibility

| Version | Status |
|---|---|
| Paper/Bukkit/Spigot 1.20.5 (Java 21) | ✅ Supported (BETA) |
| Paper/Bukkit/Spigot 1.20.6 (Java 21) | ✅ Supported |
| Paper/Bukkit/Spigot 1.21.1 | ✅ Supported (BETA) |
| Paper/Bukkit/Spigot 1.21.4 | ✅ Supported (BETA) |
| Paper/Bukkit/Spigot 1.21.10 | ✅ Supported |
| Paper/Bukkit/Spigot 1.21.11 | ✅ Supported |
| Paper/Bukkit/Spigot 26.1 | 🔜 Planned |
| Paper/Bukkit/Spigot 26.1.2 | 🔜 Planned |

---

## ⚠️ Known Issues

These are known bugs being actively investigated. Workarounds or fixes will be included in future updates.

- **Rubberband misfires on certain jumps** — In rare cases the rubberbanding system fires unexpectedly, causing a brief, visible stutter in player movement. This can occur when:
  - Performing large/long jumps
  - Jumping out of water
  - Jumping frame-perfectly down the edge of a block

- **False flags on leaf/plant blocks in water** — Players standing on leaf or plant blocks while submerged in water will be continuously flagged. Avoid placing such blocks underwater until this is resolved.

---

## Planned Features

- [!] Support for Version under 1.20 and 26.1, 26.1.2
- [ ] Expanded `/xoxo` command (view flags, manage bans, reload config)
- [ ] Per-check enable/disable in config
- [ ] Flag logging to file / database
- [ ] Rubberband misfire fix for edge-case jumps
- [ ] Fix for leaf/plant blocks in water false positives

---

## 🤝 Contributing

Issues and pull requests are welcome! If you encounter a false positive or a missed detection, please open an issue with as much detail as possible (server version, what the player was doing, any relevant logs).

---

## 📄 License

[MIT](LICENSE) — free to use, modify, and distribute.

---

*xoxo-AntiCheat — because your players deserve a fair server.*
