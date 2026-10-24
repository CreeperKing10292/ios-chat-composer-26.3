
package dev.creeperking.ioschat.mixin;

import dev.creeperking.ioschat.ChatBuffer;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    private static final int PRESS = 1;
    private static final int REPEAT = 2;

    // Provisional key identifiers; verify against SDL mappings.
    private static final int KEY_G = 103;
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_SPACE = 32;

    private static final int MOD_SHIFT = 0x0001;

    @Inject(
        method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private void ioschat$keyPress(
        long window,
        int action,
        KeyEvent event,
        CallbackInfo ci
    ) {
        if (action != PRESS && action != REPEAT) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        int key = event.key();
        int mods = event.modifiers();

        if (key == KEY_G && action == PRESS) {
            ChatBuffer.toggle();

            status(
                mc,
                ChatBuffer.isOpen()
                    ? "Composer ON - Enter sends, Esc cancels"
                    : "Composer OFF"
            );

            ci.cancel();
            return;
        }

        if (!ChatBuffer.isOpen()) {
            return;
        }

        ci.cancel();

        if (key == KEY_ESCAPE) {
            ChatBuffer.close();
            status(mc, "Composer cancelled");
            return;
        }

        if (key == KEY_ENTER) {
            String message = ChatBuffer.text();
            ChatBuffer.close();

            if (!message.isBlank()
                && mc.player.connection != null) {
                mc.player.connection.sendChat(message);
            }

            return;
        }

        if (key == KEY_BACKSPACE) {
            ChatBuffer.backspace();
        } else if (key == KEY_SPACE) {
            ChatBuffer.append(' ');
        } else if (key >= 65 && key <= 90) {
            boolean shift = (mods & MOD_SHIFT) != 0;

            ChatBuffer.append(
                (char) (key + (shift ? 0 : 32))
            );
        } else if (key >= 48 && key <= 57) {
            ChatBuffer.append((char) key);
        }

        status(mc, "> " + ChatBuffer.text());
    }

    private static void status(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(
                Component.literal("[iOS Chat] " + message)
            );
        }
    }
}
