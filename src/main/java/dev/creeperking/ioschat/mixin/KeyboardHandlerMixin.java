
package dev.creeperking.ioschat.mixin;

import dev.creeperking.ioschat.ChatBuffer;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    private static final int PRESS = 1;
    private static final int RELEASE = 0;

    // SDL scancodes
    private static final int KEY_G = 10;
    private static final int KEY_ESCAPE = 41;
    private static final int KEY_ENTER = 40;
    private static final int KEY_BACKSPACE = 42;
    private static final int KEY_SPACE = 44;

    private static final int MOD_SHIFT = 0x0003;

    // Ignore rapid duplicate presses of the same key.
    private static final long DEBOUNCE_MS = 85L;

    // Extra protection for the activation key.
    private static final long ACTIVATION_GUARD_MS = 300L;

    private static final long[] lastPress =
        new long[512];

    private static long activatedAt = 0L;

    @Inject(
        method =
            "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
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
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            ChatBuffer.close();
            return;
        }

        int key = event.key();
        int mods = event.modifiers();

        // Release events are not text input.
        if (action == RELEASE) {
            if (ChatBuffer.isOpen()) {
                ci.cancel();
            }
            return;
        }

        // Ignore repeated key events.
        if (action != PRESS) {
            if (ChatBuffer.isOpen()) {
                ci.cancel();
            }
            return;
        }

        long now = System.nanoTime() / 1_000_000L;

        // Open composer using G.
        if (!ChatBuffer.isOpen()) {
            if (key == KEY_G) {
                ChatBuffer.toggle();
                activatedAt = now;
                lastPress[KEY_G] = now;

                ci.cancel();
            }
            return;
        }

        // The composer owns keyboard input while active.
        ci.cancel();

        // Do not type the G that activated the composer.
        if (
            key == KEY_G &&
            now - activatedAt < ACTIVATION_GUARD_MS
        ) {
            return;
        }

        // Escape always cancels.
        if (key == KEY_ESCAPE) {
            ChatBuffer.close();
            return;
        }

        // Enter always sends.
        if (key == KEY_ENTER) {
            String message = ChatBuffer.text();
            ChatBuffer.close();

            if (
                !message.isBlank() &&
                mc.player.connection != null
            ) {
                mc.player.connection.sendChat(message);
            }
            return;
        }

        // Suppress rapid duplicate key presses.
        if (key >= 0 && key < lastPress.length) {
            if (now - lastPress[key] < DEBOUNCE_MS) {
                return;
            }

            lastPress[key] = now;
        }

        if (key == KEY_BACKSPACE) {
            ChatBuffer.backspace();
            return;
        }

        if (key == KEY_SPACE) {
            ChatBuffer.append(' ');
            return;
        }

        // SDL scancodes A-Z: 4-29
        if (key >= 4 && key <= 29) {
            char letter = (char) ('a' + key - 4);

            if ((mods & MOD_SHIFT) != 0) {
                letter = Character.toUpperCase(letter);
            }

            ChatBuffer.append(letter);
            return;
        }

        // SDL scancodes 1-9: 30-38
        if (key >= 30 && key <= 38) {
            ChatBuffer.append(
                (char) ('1' + key - 30)
            );
            return;
        }

        // SDL scancode 0: 39
        if (key == 39) {
            ChatBuffer.append('0');
        }
    }
}
