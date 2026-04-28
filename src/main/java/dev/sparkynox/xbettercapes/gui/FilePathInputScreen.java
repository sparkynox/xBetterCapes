package dev.sparkynox.xbettercapes.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.File;

/**
 * Simple screen that lets user type a file path to load a cape PNG.
 * Works on Android/Pojav where AWT FileDialog is unavailable.
 * Shows common Android paths as hints.
 */
public class FilePathInputScreen extends Screen {

    private final CapeSelectScreen parent;
    private TextFieldWidget pathField;
    private String errorMsg = "";

    // Common paths on Android/Pojav
    private static final String[] HINTS = {
        "/sdcard/cape.png",
        "/sdcard/Download/cape.png",
        "/storage/emulated/0/cape.png",
        "/storage/emulated/0/Download/cape.png",
    };
    private int hintIndex = 0;

    public FilePathInputScreen(CapeSelectScreen parent) {
        super(Text.literal("Load Custom Cape"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Path input field
        pathField = new TextFieldWidget(this.textRenderer,
                cx - 140, cy - 10, 280, 20, Text.literal("File path"));
        pathField.setPlaceholder(Text.literal("/sdcard/cape.png"));
        pathField.setMaxLength(512);
        pathField.setFocused(true);
        this.addDrawableChild(pathField);

        // Load button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Load"), btn -> tryLoad())
                .dimensions(cx - 60, cy + 18, 55, 18).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                btn -> this.client.setScreen(parent))
                .dimensions(cx + 6, cy + 18, 55, 18).build());

        // Hint cycle button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Hint >>"), btn -> {
            hintIndex = (hintIndex + 1) % HINTS.length;
            pathField.setText(HINTS[hintIndex]);
        }).dimensions(cx - 140, cy + 18, 70, 18).build());
    }

    private void tryLoad() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) { errorMsg = "Enter a file path!"; return; }
        File f = new File(path);
        if (!f.exists()) { errorMsg = "File not found: " + path; return; }
        if (!path.toLowerCase().endsWith(".png") &&
            !path.toLowerCase().endsWith(".jpg") &&
            !path.toLowerCase().endsWith(".jpeg")) {
            errorMsg = "Only PNG/JPG files supported!"; return;
        }
        // Go back to cape screen and load
        this.client.setScreen(parent);
        parent.loadLocalFile(f);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF060609);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Panel
        ctx.fill(cx-150, cy-50, cx+150, cy+45, 0xFF0C0C18);
        drawBorder(ctx, cx-150, cy-50, 300, 95, 0xFF182030);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Load Custom Cape"), cx, cy - 42, 0x00CCEE);
        ctx.fill(cx-146, cy-33, cx+146, cy-32, 0xFF183040);

        // Instructions
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Enter the full path to your cape PNG:"),
                cx, cy - 26, 0x556677);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("e.g. /sdcard/Download/mycape.png"),
                cx, cy - 16, 0x334455);

        // Error
        if (!errorMsg.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(errorMsg), cx, cy + 42, 0xFF4444);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,     y,     x+w, y+1,   color);
        ctx.fill(x,     y+h-1, x+w, y+h,   color);
        ctx.fill(x,     y,     x+1, y+h,   color);
        ctx.fill(x+w-1, y,     x+w, y+h,   color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter key = Load
        if (keyCode == 257 || keyCode == 335) { tryLoad(); return true; }
        // Escape = Cancel
        if (keyCode == 256) { this.client.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }
}
