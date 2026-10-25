
package dev.creeperking.ioschat;

import com.mojang.brigadier.suggestion.Suggestion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class ComposerHud
    implements ClientModInitializer {

    private static final int CYAN = 0xFF55FFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int BACKGROUND = 0xCC101820;
    private static final int HIGHLIGHT = 0xAA245060;

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

        String fullText = ChatBuffer.text();
        String text = fullText;

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
            fullText.startsWith("/")
                ? "Tab: complete  |  Enter: run  |  Esc: cancel"
                : "Enter: send  |  Esc: cancel",
            x + 4,
            y + 15,
            MUTED,
            false
        );

        if (!fullText.startsWith("/")) {
            return;
        }

        List<Suggestion> suggestions =
            CommandCompletion.suggestions();

        if (suggestions.isEmpty()) {
            if (CommandCompletion.loading()) {
                graphics.text(
                    mc.font,
                    "Searching commands...",
                    x + 4,
                    y - 18,
                    MUTED,
                    false
                );
            }
            return;
        }

        int visible = Math.min(4, suggestions.size());
        int rowHeight = 13;

        int panelBottom = y - 9;
        int panelTop =
            panelBottom - visible * rowHeight - 8;

        graphics.fill(
            x - 4,
            panelTop,
            x + width,
            panelBottom,
            BACKGROUND
        );

        for (int i = 0; i < visible; i++) {
            Suggestion suggestion = suggestions.get(i);

            int lineY = panelTop + 5 + i * rowHeight;

            if (i == CommandCompletion.selected()) {
                graphics.fill(
                    x,
                    lineY - 2,
                    x + width - 5,
                    lineY + 11,
                    HIGHLIGHT
                );
            }

            String label = suggestion.getText();

            int availableWidth = width - 18;

            while (
                !label.isEmpty() &&
                mc.font.width(label) > availableWidth
            ) {
                label = label.substring(0, label.length() - 1);
            }

            graphics.text(
                mc.font,
                label,
                x + 4,
                lineY,
                i == CommandCompletion.selected()
                    ? CYAN
                    : WHITE,
                false
            );
        }
    }
}
