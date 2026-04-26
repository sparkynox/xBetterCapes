# xBetter Capes
**Authors:** SparkyNox & SPA4RKIEE_XD  
**MC Version:** 1.21.4 – 1.21.11 (Fabric)  
**Keybind:** G → Opens Cape GUI

---

## Features
- Works on **cracked / offline accounts** — no Mojang auth needed
- **Built-in capes** — pre-loaded textures inside the mod jar
- **URL capes** — paste any image URL in the GUI (loaded async, no lag)
- Click a cape card to equip it instantly
- Config saved in `.minecraft/config/xbettercapes.json`

---

## File Structure
```
src/main/
├── java/dev/sparkynox/xbettercapes/
│   ├── XBetterCapesClient.java         ← Entrypoint, keybind
│   ├── config/CapeConfig.java          ← Save/load selected cape
│   ├── cape/
│   │   ├── CapeEntry.java              ← Data class
│   │   ├── CapeRegistry.java           ← ADD NEW BUILT-IN CAPES HERE
│   │   └── CapeTextureManager.java     ← Lazy load + cache textures
│   ├── gui/CapeSelectScreen.java       ← The GUI
│   └── mixin/PlayerEntityRendererMixin.java  ← Cape injection
│
└── resources/
    ├── fabric.mod.json
    ├── xbettercapes.mixins.json
    └── assets/xbettercapes/
        ├── lang/en_us.json
        └── textures/capes/
            ├── mystic.png       ← PUT YOUR CAPE PNGs HERE
            ├── fire.png
            ├── galaxy.png
            ├── yori.png
            └── sparkynox.png
```

---

## Adding a New Built-in Cape

1. Drop your PNG into `resources/assets/xbettercapes/textures/capes/`
2. Open `CapeRegistry.java` and add one line:
   ```java
   register("myid", "My Cape Name", "xbettercapes:textures/capes/myfile.png");
   ```
3. Done. Build and the cape shows in GUI.

---

## Cape Texture Format
Standard Minecraft cape format: **64×32 PNG**  
Cape pixels are at: x=0, y=0, width=22, height=17 (on the 64×32 grid)

---

## Build
```bash
./gradlew build
```
Output jar → `build/libs/xbettercapes-1.0.0.jar`

---

## Performance Notes
- Zero tick loops — only a single `wasPressed()` check per tick
- Textures loaded **once**, cached in `ConcurrentHashMap`
- URL textures fetched on a **single daemon thread** (no thread pool spam)
- URL textures **released from VRAM** when GUI closes
- Built-in capes use Minecraft's resource system — no extra loading
