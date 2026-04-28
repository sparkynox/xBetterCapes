package dev.sparkynox.xbettercapes.gui;

import dev.sparkynox.xbettercapes.cape.CapeEntry;
import dev.sparkynox.xbettercapes.cape.CapeRegistry;
import dev.sparkynox.xbettercapes.cape.CapeTextureManager;
import dev.sparkynox.xbettercapes.config.CapeConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class CapeSelectScreen extends Screen {

    private static final int CARD_W    = 54;
    private static final int CARD_H    = 64;
    private static final int CARD_GAP  = 6;
    private static final int COLS      = 4;
    private static final int SCROLLBAR_W = 6;

    // Exact 1.21.4 drawTexture signature (confirmed from Yarn docs):
    // drawTexture(renderLayers, sprite, x, y, u, v, drawW, drawH, regionW, regionH, texW, texH)
    // Cape region in 64x32 sheet: top-left at 0,0 size 22x17
    private static final float  CAPE_U  = 0f,  CAPE_V  = 0f;
    private static final int    CAPE_RW = 22,  CAPE_RH = 17;  // region in texture
    private static final int    PREV_W  = 44,  PREV_H  = 34;  // drawn size (2x)
    private static final int    TEX_W   = 64,  TEX_H   = 32;

    private List<CapeEntry> capes;
    private String statusMsg = "";

    private int scrollOffset = 0;
    private boolean draggingScroll = false;
    private int dragStartY = 0, dragStartOffset = 0;

    private int gridX, gridY, gridW, gridH;
    private int totalContentH;
    private int scrollbarX, scrollbarH;
    private int bottomBarY;

    public CapeSelectScreen() {
        super(Text.literal("xBetter Capes"));
    }

    @Override
    protected void init() {
        capes = CapeRegistry.getBuiltinCapes();

        int rows      = (int) Math.ceil(capes.size() / (double) COLS);
        int contentGW = COLS * (CARD_W + CARD_GAP) - CARD_GAP;

        bottomBarY    = this.height - 32;
        gridX         = (this.width - contentGW) / 2;
        gridY         = 32;
        gridW         = contentGW;
        gridH         = bottomBarY - gridY - 4;
        totalContentH = rows * (CARD_H + CARD_GAP) - CARD_GAP;
        scrollbarX    = gridX + gridW + 6;
        scrollbarH    = gridH;
        scrollOffset  = 0;

        int cx = this.width / 2;

        // "Load Custom Cape" button — opens file picker
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Load Custom Cape"), btn -> openFilePicker())
                .dimensions(cx - 70, bottomBarY + 6, 140, 18).build());
    }

    // ── File picker ───────────────────────────────────────────────────────────
    private void openFilePicker() {
        // Use AWT FileDialog — works on desktop Java including Pojav
        new Thread(() -> {
            try {
                java.awt.FileDialog fd = new java.awt.FileDialog(
                        (java.awt.Frame) null, "Select Cape PNG", java.awt.FileDialog.LOAD);
                fd.setFilenameFilter((dir, name) ->
                        name.toLowerCase().endsWith(".png") ||
                        name.toLowerCase().endsWith(".jpg") ||
                        name.toLowerCase().endsWith(".jpeg"));
                fd.setVisible(true);

                String dir  = fd.getDirectory();
                String file = fd.getFile();
                fd.dispose();

                if (dir != null && file != null) {
                    File chosen = new File(dir, file);
                    if (chosen.exists()) {
                        loadLocalFile(chosen);
                    }
                }
            } catch (Exception e) {
                // AWT unavailable (rare) — show message
                MinecraftClient.getInstance().execute(() ->
                        statusMsg = "File picker unavailable on this device.");
            }
        }, "xBetterCapes-FilePicker").start();
    }

    private void loadLocalFile(File file) {
        new Thread(() -> {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                // Validate PNG magic bytes
                if (bytes.length < 4 ||
                    (bytes[0] & 0xFF) != 0x89 ||
                    (bytes[1] & 0xFF) != 0x50) {
                    MinecraftClient.getInstance().execute(() ->
                            statusMsg = "Not a valid PNG file!");
                    return;
                }
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        net.minecraft.client.texture.NativeImage img =
                                net.minecraft.client.texture.NativeImage.read(
                                        new java.io.ByteArrayInputStream(bytes));
                        net.minecraft.client.texture.NativeImageBackedTexture tex =
                                new net.minecraft.client.texture.NativeImageBackedTexture(img);
                        Identifier id = Identifier.of("xbettercapes", "custom_local_cape");
                        MinecraftClient.getInstance()
                                .getTextureManager().registerTexture(id, tex);

                        // Update custom slot in registry and select it
                        CapeRegistry.setCustomLocal(id, file.getName());
                        CapeTextureManager.putCache("builtin:custom_local", id);
                        CapeConfig.selectedCape = "builtin:custom_local";
                        CapeConfig.save();
                        statusMsg = "Loaded: " + file.getName();

                        // Refresh capes list
                        capes = CapeRegistry.getBuiltinCapes();
                        // Recompute layout
                        int rows = (int) Math.ceil(capes.size() / (double) COLS);
                        totalContentH = rows * (CARD_H + CARD_GAP) - CARD_GAP;
                    } catch (Exception e) {
                        statusMsg = "Failed to load image!";
                    }
                });
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() ->
                        statusMsg = "Could not read file!");
            }
        }, "xBetterCapes-FileLoader").start();
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────
    private int maxScroll()  { return Math.max(0, totalContentH - gridH); }
    private void clampScroll() { scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll())); }
    private int thumbH() {
        if (totalContentH <= gridH) return scrollbarH;
        return Math.max(20, scrollbarH * gridH / totalContentH);
    }
    private int thumbY() {
        int max = maxScroll();
        if (max == 0) return gridY;
        return gridY + (scrollbarH - thumbH()) * scrollOffset / max;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);

        int px = gridX - 10, py = 2;
        int pw = gridW + SCROLLBAR_W + 22, ph = this.height - 4;
        ctx.fill(px, py, px + pw, py + ph, 0xFF0C0C16);
        drawBorder(ctx, px, py, pw, ph, 0xFF182030);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("xBetter Capes"), this.width / 2, 7, 0x00CCEE);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("SparkyNox & SPA4RKIEE_XD"), this.width / 2, 17, 0x446677);
        ctx.fill(px + 4, 27, px + pw - 4, 28, 0xFF183040);

        // Clipped grid
        ctx.enableScissor(gridX, gridY, gridX + gridW, gridY + gridH);
        for (int i = 0; i < capes.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx  = gridX + col * (CARD_W + CARD_GAP);
            int cy  = gridY + row * (CARD_H + CARD_GAP) - scrollOffset;
            if (cy + CARD_H < gridY || cy > gridY + gridH) continue;
            drawCapeCard(ctx, capes.get(i), cx, cy, mouseX, mouseY);
        }
        ctx.disableScissor();

        // Edge fades
        ctx.fill(gridX, gridY, gridX + gridW, gridY + 8, 0xBB060609);
        ctx.fill(gridX, gridY + gridH - 8, gridX + gridW, gridY + gridH, 0xBB060609);

        // Scrollbar
        if (maxScroll() > 0) {
            ctx.fill(scrollbarX, gridY,
                     scrollbarX + SCROLLBAR_W, gridY + scrollbarH, 0xFF0E0E18);
            boolean hov = mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_W
                       && mouseY >= thumbY() && mouseY <= thumbY() + thumbH();
            ctx.fill(scrollbarX, thumbY(),
                     scrollbarX + SCROLLBAR_W, thumbY() + thumbH(),
                     (draggingScroll || hov) ? 0xFF00AACC : 0xFF2A4A5A);
        }

        // Divider above bottom bar
        ctx.fill(px + 2, bottomBarY - 2, px + pw - 2, bottomBarY - 1, 0xFF183040);

        // Status message
        if (!statusMsg.isEmpty()) {
            int col = statusMsg.startsWith("Not") || statusMsg.startsWith("Failed")
                    || statusMsg.startsWith("Could") ? 0xFF4444 : 0x00CC88;
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(statusMsg), this.width / 2, bottomBarY - 10, col);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private void drawCapeCard(DrawContext ctx, CapeEntry entry,
                               int x, int y, int mouseX, int mouseY) {
        boolean selected = CapeConfig.selectedCape.equals(entry.toConfigString());
        boolean hovered  = mouseX >= x && mouseX < x + CARD_W
                        && mouseY >= y && mouseY < y + CARD_H
                        && mouseY >= gridY && mouseY < gridY + gridH;

        ctx.fill(x, y, x + CARD_W, y + CARD_H,
                selected ? 0xFF0A2030 : (hovered ? 0xFF121220 : 0xFF0A0A16));
        drawBorder(ctx, x, y, CARD_W, CARD_H,
                selected ? 0xFF0099BB : (hovered ? 0xFF223344 : 0xFF141424));
        if (selected)
            ctx.fill(x + 1, y + 1, x + CARD_W - 1, y + 2, 0xFF00BBDD);

        int imgX = x + (CARD_W - PREV_W) / 2;
        int imgY = y + 5;

        Identifier tex = CapeTextureManager.getTexture(entry);

        if (tex != null && entry.resourcePath != null) {
            // CONFIRMED 1.21.4 signature from Yarn docs:
            // drawTexture(renderLayers, sprite, x, y, u, v, drawW, drawH, regionW, regionH, texW, texH)
            ctx.drawTexture(RenderLayer::getGuiTextured,
                    tex,
                    imgX, imgY,
                    CAPE_U, CAPE_V,
                    PREV_W, PREV_H,
                    CAPE_RW, CAPE_RH,
                    TEX_W, TEX_H);
        } else if ("custom_local".equals(entry.id)) {
            // Custom slot — not yet loaded
            ctx.fill(imgX, imgY, imgX + PREV_W, imgY + PREV_H, 0xFF101020);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("+"),
                    imgX + PREV_W / 2, imgY + PREV_H / 2 - 4, 0x446677);
        } else if (entry.resourcePath != null) {
            ctx.fill(imgX, imgY, imgX + PREV_W, imgY + PREV_H, 0xFF0E0E1C);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("..."),
                    imgX + PREV_W / 2, imgY + PREV_H / 2 - 4, 0x223344);
        } else {
            // No Cape slot
            ctx.fill(imgX, imgY, imgX + PREV_W, imgY + PREV_H, 0xFF080810);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NONE"),
                    imgX + PREV_W / 2, imgY + PREV_H / 2 - 4, 0x223333);
        }

        String name = entry.displayName;
        if (textRenderer.getWidth(name) > CARD_W - 4)
            name = textRenderer.trimToWidth(name, CARD_W - 8) + "..";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
                x + CARD_W / 2, y + CARD_H - 10,
                selected ? 0x00BBDD : (hovered ? 0xAABBCC : 0x556677));

        if (selected)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("v"),
                    x + CARD_W - 6, y + 4, 0x00EE88);
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(double mx, double my, double hA, double vA) {
        scrollOffset -= (int)(vA * (CARD_H + CARD_GAP));
        clampScroll();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Scrollbar thumb
            if (maxScroll() > 0
                    && mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_W
                    && mouseY >= thumbY() && mouseY <= thumbY() + thumbH()) {
                draggingScroll = true;
                dragStartY = (int) mouseY;
                dragStartOffset = scrollOffset;
                return true;
            }
            // Cape card
            if (mouseY >= gridY && mouseY < gridY + gridH) {
                for (int i = 0; i < capes.size(); i++) {
                    int cx = gridX + (i % COLS) * (CARD_W + CARD_GAP);
                    int cy = gridY + (i / COLS) * (CARD_H + CARD_GAP) - scrollOffset;
                    if (mouseX >= cx && mouseX < cx + CARD_W
                     && mouseY >= cy && mouseY < cy + CARD_H) {
                        CapeEntry entry = capes.get(i);
                        if ("custom_local".equals(entry.id)) {
                            // Clicking Custom slot opens file picker
                            openFilePicker();
                        } else {
                            CapeConfig.selectedCape = entry.toConfigString();
                            CapeConfig.save();
                            statusMsg = "";
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingScroll && btn == 0 && maxScroll() > 0) {
            int range = scrollbarH - thumbH();
            if (range > 0)
                scrollOffset = dragStartOffset + (int)(my - dragStartY) * maxScroll() / range;
            clampScroll();
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) draggingScroll = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public void close() {
        CapeTextureManager.releaseUrlTextures();
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
