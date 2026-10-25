
package dev.creeperking.ioschat.mixin;

import dev.creeperking.ioschat.ChatBuffer;
import dev.creeperking.ioschat.CommandCompletion;

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
    private static final int KEY_ENTER = 40;
    private static final int KEY_ESCAPE = 41;
    private static final int KEY_BACKSPACE = 42;
    private static final int KEY_TAB = 43;
    private static final int KEY_SPACE = 44;

    private static final int KEY_DOWN = 81;
    private static final int KEY_UP = 82;

    private static final int MOD_SHIFT = 0x0003;

    private static final long DEBOUNCE_MS = 85L;
    private static final long ACTIVATION_GUARD_MS = 300L;

    private static final long[] lastPress = new long[512];
    private static long activatedAt = 0L;

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
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            ChatBuffer.close();
            CommandCompletion.clear();
            return;
        }

        int key = event.key();
        int mods = event.modifiers();

        // Ignore key releases.
        if (action == RELEASE) {
            if (ChatBuffer.isOpen()) {
                ci.cancel();
            }
            return;
        }

        // Ignore keyboard repeat events.
        if (action != PRESS) {
            if (ChatBuffer.isOpen()) {
                ci.cancel();
            }
            return;
        }

        long now = System.nanoTime() / 1_000_000L;

        // G activates the composer.
        if (!ChatBuffer.isOpen()) {
            if (key == KEY_G) {
                ChatBuffer.toggle();
                CommandCompletion.clear();

                activatedAt = now;
                lastPress[KEY_G] = now;

                ci.cancel();
            }
            return;
        }

        // Intercept keyboard input while composing.
        ci.cancel();

        // Prevent the activation G from being typed.
        if (
            key == KEY_G &&
            now - activatedAt < ACTIVATION_GUARD_MS
        ) {
            return;
        }

        // Escape cancels composition.
        if (key == KEY_ESCAPE) {
            ChatBuffer.close();
            CommandCompletion.clear();
            return;
        }

        // Enter sends chat or executes a command.
        if (key == KEY_ENTER) {
            String message = ChatBuffer.text();

            ChatBuffer.close();
            CommandCompletion.clear();

            if (
                !message.isBlank() &&
                mc.player.connection != null
            ) {
                if (message.startsWith("/")) {
                    String command = message.substring(1);

                    if (!command.isBlank()) {
                        mc.player.connection.sendCommand(command);
                    }
                } else {
                    mc.player.connection.sendChat(message);
                }
            }
            return;
        }

        // Navigate autocomplete suggestions.
        // Debounce arrow keys to prevent duplicate navigation events.
        if (key == KEY_UP || key == KEY_DOWN) {
        
            if (key >= 0 && key < lastPress.length) {
                if (now - lastPress[key] < DEBOUNCE_MS) {
                    return;
                }
        
                lastPress[key] = now;
            }
        
            CommandCompletion.moveSelection(
                key == KEY_UP ? -1 : 1
            );
        
            return;
        }

        // Tab accepts the selected suggestion.
        if (key == KEY_TAB) {
            CommandCompletion.acceptOrRequest(mc);
            return;
        }

        // Prevent duplicate characters.
        if (key >= 0 && key < lastPress.length) {
            if (now - lastPress[key] < DEBOUNCE_MS) {
                return;
            }

            lastPress[key] = now;
        }

        // Backspace removes one character.
        if (key == KEY_BACKSPACE) {
            ChatBuffer.backspace();
            CommandCompletion.refresh(mc);
            return;
        }

        boolean shift = (mods & MOD_SHIFT) != 0;

        Character typed = mapKey(key, shift);

        if (typed != null) {
            ChatBuffer.append(typed);
            CommandCompletion.refresh(mc);
        }
    }

    private static Character mapKey(
        int key,
        boolean shift
    ) {
        // Letters A-Z: SDL scancodes 4-29.
        if (key >= 4 && key <= 29) {
            char letter = (char) ('a' + key - 4);

            return shift
                ? Character.toUpperCase(letter)
                : letter;
        }

        // Digits 1-9 and shifted symbols.
        if (key >= 30 && key <= 38) {
            String symbols = "!@#$%^&*(";
            int index = key - 30;

            return shift
                ? symbols.charAt(index)
                : (char) ('1' + index);
        }

        // Digit 0 or closing parenthesis.
        if (key == 39) {
            return shift ? ')' : '0';
        }

        // Space.
        if (key == KEY_SPACE) {
            return ' ';
        }

        // US keyboard punctuation.
        return switch (key) {
            case 45 -> shift ? '_' : '-';
            case 46 -> shift ? '+' : '=';
            case 47 -> shift ? '{' : '[';
            case 48 -> shift ? '}' : ']';
            case 49 -> shift ? '|' : '\\';
            case 51 -> shift ? ':' : ';';
            case 52 -> shift ? '"' : '\'';
            case 53 -> shift ? '~' : '`';
            case 54 -> shift ? '<' : ',';
            case 55 -> shift ? '>' : '.';
            case 56 -> shift ? '?' : '/';
            default -> null;
        };
    }
}
