
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

    // SDL keyboard actions
    private static final int PRESS = 1;
    private static final int REPEAT = 2;

    // SDL scancodes
    private static final int KEY_G = 10;
    private static final int KEY_ESCAPE = 41;
    private static final int KEY_ENTER = 40;
    private static final int KEY_BACKSPACE = 42;
    private static final int KEY_SPACE = 44;

    // SDL modifier bitmask
    private static final int MOD_SHIFT = 0x0003;

    // Enable or disable keyboard debug output
    private static final boolean DEBUG = true;

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

        // Debug output for latest.log
        if (DEBUG) {
            System.out.println(
                "[iOS Chat DEBUG] key=" + key
                + " keycode=" + event.keycode()
                + " modifiers=" + mods
                + " action=" + action
            );
        }

        // Activate composer using G.
        // Only activate when it is currently closed.
        if (
            key == KEY_G
            && action == PRESS
            && !ChatBuffer.isOpen()
        ) {

            ChatBuffer.toggle();

            status(
                mc,
                "Composer ON - Enter sends, Esc cancels"
            );

            ci.cancel();
            return;
        }

        // Let Minecraft process keys normally
        // while the composer is closed.
        if (!ChatBuffer.isOpen()) {
            return;
        }

        // Intercept keyboard input while composing.
        ci.cancel();

        // Escape: cancel composition.
        if (key == KEY_ESCAPE) {
            ChatBuffer.close();

            status(mc, "Composer cancelled");
            return;
        }

        // Enter: send the composed message.
        if (key == KEY_ENTER) {

            String message = ChatBuffer.text();

            ChatBuffer.close();

            if (
                !message.isBlank()
                && mc.player.connection != null
            ) {
                mc.player.connection.sendChat(message);
            }

            status(mc, "Composer closed");
            return;
        }

        // Backspace: remove last character.
        if (key == KEY_BACKSPACE) {
            ChatBuffer.backspace();
        }

        // Space
        else if (key == KEY_SPACE) {
            ChatBuffer.append(' ');
        }

        // Letters A-Z: SDL scancodes 4-29.
        else if (key >= 4 && key <= 29) {

            boolean shift =
                (mods & MOD_SHIFT) != 0;

            char letter =
                (char) ('a' + key - 4);

            if (shift) {
                letter = Character.toUpperCase(letter);
            }

            ChatBuffer.append(letter);
        }

        // Digits 1-9: SDL scancodes 30-38.
        else if (key >= 30 && key <= 38) {

            char digit =
                (char) ('1' + key - 30);

            ChatBuffer.append(digit);
        }

        // Digit 0: SDL scancode 39.
        else if (key == 39) {
            ChatBuffer.append('0');
        }

        // Display current buffer.
        status(
            mc,
            "> " + ChatBuffer.text()
        );
    }

    private static void status(
        Minecraft mc,
        String message
    ) {

        if (mc.player != null) {

            mc.player.sendSystemMessage(
                Component.literal(
                    "[iOS Chat] " + message
                )
            );
        }
    }
}
