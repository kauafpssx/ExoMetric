# ExoMetric

![GitHub Repo stars](https://img.shields.io/github/stars/zKauaFerreira/ExoMetric?style=for-the-badge&color=gold)
![GitHub repo size](https://img.shields.io/github/repo-size/zKauaFerreira/ExoMetric?style=for-the-badge&color=blue)
![GitHub license](https://img.shields.io/github/license/zKauaFerreira/ExoMetric?style=for-the-badge&color=green)

**ExoMetric** is a high-performance Fabric mod for Minecraft 26.2 designed for external telemetry. It exposes detailed server metrics (TPS, MSPT, Players) and Linux/Pterodactyl container data (CPU, RAM, Disk) via a secure internal HTTP API.

> [!TIP]
> **Tested on HidenCloud and it works perfectly!** 🚀

## 🚀 Quick Start

1. Place `exometric-1.0.0.jar` into your server's `mods/` folder.
2. Start the server once to generate the configuration file.
3. Edit `config/ExoMetric.json` and set your `api_port`.
4. **Save the file** and the mod will apply changes automatically within 5 seconds.
5. Access: `http://server-ip:port/mc-stats?token=YOUR_TOKEN`

## ✨ Features

- **Game Metrics**: TPS (Ticks Per Second), MSPT (Milli-seconds Per Tick), online players count, loaded chunks, seed, and weather.
- **System Metrics**: Real-time CPU usage (delta), RAM usage (cgroups/system), Disk usage, and Network traffic (RX/TX).
- **Player Data**: Detailed list including Name, UUID, Ping, Dimension, Gamemode, Health, Level, and Coordinates.
- **Security**: Authentication via Query string token (`?token=...`).
- **Auto-Reload (Hot-Swap)**: Change the token or port in the JSON file and the system restarts the API automatically without rebooting Minecraft.
- **HTTP or HTTPS**: Toggle between HTTP and HTTPS on the same port via config, using an auto-generated self-signed certificate for HTTPS.

## ⚙️ Configuration

The config file is located at `config/ExoMetric.json`:

| Field | Type | Description |
|-------|------|-----------|
| `api_enabled` | Boolean | Enables/Disables the metrics server. |
| `api_port` | Integer | HTTP Port (must be allocated/open on your host). |
| `api_token` | String | Automatically generated access token (can be customized). |
| `api_use_https` | Boolean | `false` = serve over HTTP, `true` = serve over HTTPS. Uses the same `api_port` either way. |
| `api_https_keystore_password` | String | Automatically generated password for the self-signed keystore. Do not share. |

> **Note:** The mod monitors this file. Any saved changes will be applied in real-time.

### 🔒 Choosing HTTP or HTTPS

ExoMetric serves the API over **either** HTTP or HTTPS on `api_port` — pick one via `api_use_https`, no extra port allocation needed.

1. In `config/ExoMetric.json`, set:
   ```json
   "api_use_https": true
   ```
2. On the next start (or hot-reload), ExoMetric generates a self-signed PKCS12 keystore (`config/exometric-ssl.p12`) automatically using the `keytool` bundled with the server's JDK — no manual certificate setup needed.
3. Access via `https://server-ip:port/mc-stats?token=YOUR_TOKEN` (same port as before).

Set `api_use_https` back to `false` to switch to plain HTTP again on the same port.

> **Note:** Since the certificate is self-signed, browsers will show a security warning ("Your connection is not private"). This is expected — click "Advanced → Proceed" to continue, or consume the API from a script/backend (e.g. `curl`) where certificate warnings aren't an issue. For a browser-trusted certificate you would need a reverse proxy with a real TLS certificate (e.g. Let's Encrypt), which typically isn't available inside shared game-hosting containers.
>
> If your browser insists on redirecting plain `http://` requests to HTTPS on its own (HSTS), that's a browser/domain policy, not ExoMetric — use `curl`, an incognito window, or disable "Always use secure connections" for that check, or just use the HTTPS endpoint directly.

## 🔗 API Reference

### GET `/mc-stats`
Returns the full server and system summary.

<details>
<summary><b>Example Response</b></summary>

```json
{
  "status": "running",
  "memory_bytes": 754241536,
  "cpu_percent": 0.31,
  "disk_bytes": 344161165312,
  "network_rx_bytes": 410768,
  "network_tx_bytes": 7628,
  "uptime_seconds": 66133,
  "players_online": 0,
  "tps": 20.00,
  "mspt": 50.00,
  "current_tick_time": 50.00,
  "loaded_chunks": 0,
  "world_seed": -6461033676995397900,
  "world_time": 12016878,
  "world_day": 500,
  "is_raining": false,
  "difficulty": "normal",
  "heap_used_bytes": 271404632,
  "heap_max_bytes": 369098752
}
```
</details>

### GET `/mc-stats/players`
Returns a detailed list of all online players with coordinates and status.

<details>
<summary><b>Example Response</b></summary>

```json
{
  "players_online": 1,
  "players": [
    {
      "name": "kauafpss_",
      "uuid": "e618e273-d894-3646-ade8-7a13ef58d6c6",
      "ping": 0,
      "dimension": "minecraft:overworld",
      "gamemode": "SURVIVAL",
      "level": 32,
      "health": 20,
      "food": 20,
      "saturation": 11,
      "x": 556.9,
      "y": 67,
      "z": 75.9,
      "online_seconds": 0,
      "avatar_url": "https://mc-heads.net/avatar/e618e273-d894-3646-ade8-7a13ef58d6c6/64",
      "main_hand": null,
      "off_hand": null,
      "armor": [
        {
          "id": "minecraft:diamond_boots",
          "count": 1,
          "slot": 36,
          "name": "Diamond Boots"
        },
        {
          "id": "minecraft:diamond_leggings",
          "count": 1,
          "slot": 37,
          "name": "Diamond Leggings"
        },
        {
          "id": "minecraft:diamond_chestplate",
          "count": 1,
          "slot": 38,
          "name": "Diamond Chestplate"
        }
      ],
      "hotbar": [
        {
          "id": "minecraft:diamond_axe",
          "count": 1,
          "slot": 0,
          "name": "Diamond Axe"
        },
        {
          "id": "minecraft:diamond_pickaxe",
          "count": 1,
          "slot": 1,
          "name": "Diamond Pickaxe"
        },
        {
          "id": "minecraft:diamond_shovel",
          "count": 1,
          "slot": 2,
          "name": "Diamond Shovel"
        }
      ],
      "main_inventory": [
        {
          "id": "minecraft:diamond_pickaxe",
          "count": 1,
          "slot": 9,
          "name": "Diamond Pickaxe"
        }
      ]
    }
  ]
}
```
</details>

### GET `/mc-stats/system`
Returns only hardware and container resource metrics.

<details>
<summary><b>Example Response</b></summary>

```json
{
  "memory_bytes": 1357377536,
  "cpu_percent": 76.04,
  "disk_bytes": 344162578432,
  "network_rx_bytes": 242614,
  "network_tx_bytes": 3891089,
  "uptime_seconds": 66747
}
```
</details>

**Required Parameter:** `?token=YOUR_TOKEN`

## 🛡️ Security

- The mod uses `SecureRandom` to generate 256-bit high-security tokens on the first boot.
- Recommended for integration with external Discord bots or status dashboards.

## 📄 License

This project is licensed under CC0-1.0.
