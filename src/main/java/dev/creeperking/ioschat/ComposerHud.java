
package dev.creeperking.ioschat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;

public final class ComposerHud
    implements ClientModInitializer {

    private static final int CYAN = 0xFF55FFFF;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int BACKGROUND = 0xCC101820;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(
                "ioschatcomposer",
                "composer_preview"
            ),
            ComposerHud::render
        );
    }

    private static void render(
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (
            mc.player == null ||
            !ChatBuffer.isOpen()
        ) {
            return;
        }

        String text = ChatBuffer.text();

        // Keep long messages within the HUD.
        // Show the latest characters while typing.
        int maxCharacters = Math.max(
            12,
            Math.min(
                90,
                (graphics.guiWidth() - 110) / 6
            )
        );

        if (text.length() > maxCharacters) {
            text = "..." + text.substring(
                text.length() - maxCharacters + 3
            );
        }

        int x = 12;
        int y = graphics.guiHeight() - 72;

        int width = Math.min(
            graphics.guiWidth() - 24,
            Math.max(
                190,
                mc.font.width("[Composer] " + text + "_") + 16
            )
        );

        graphics.fill(
            x - 4,
            y - 6,
            x + width,
            y + 31,
            BACKGROUND
        );

        graphics.text(
            mc.font,
            "[Composer] " + text + "_",
            x + 4,
            y,
            CYAN,
            true
        );

        graphics.text(
            mc.font,
            "Enter: send  |  Esc: cancel",
            x + 4,
            y + 15,
            MUTED,
            false
        );
    }
}
