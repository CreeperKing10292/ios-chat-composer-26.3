
package dev.creeperking.ioschat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CommandCompletion {

    private static List<Suggestion> suggestions = List.of();
    private static int selected = 0;
    private static int generation = 0;
    private static String queriedText = "";
    private static boolean loading = false;

    private CommandCompletion() {
    }

    public static void clear() {
        generation++;
        suggestions = List.of();
        selected = 0;
        queriedText = "";
        loading = false;
    }

    public static List<Suggestion> suggestions() {
        return suggestions;
    }

    public static int selected() {
        return selected;
    }

    public static boolean loading() {
        return loading;
    }

    public static void refresh(Minecraft mc) {
        clear();

        String text = ChatBuffer.text();

        if (
            !text.startsWith("/") ||
            mc.player == null ||
            mc.player.connection == null
        ) {
            return;
        }

        queriedText = text;
        loading = true;

        final int requestId = generation;

        try {
            CommandDispatcher<ClientSuggestionProvider> dispatcher =
                mc.player.connection.getCommands();

            ClientSuggestionProvider provider =
                mc.player.connection.getSuggestionsProvider();

            String command = text.substring(1);

            ParseResults<ClientSuggestionProvider> parsed =
                dispatcher.parse(command, provider);

            CompletableFuture<Suggestions> future =
                dispatcher.getCompletionSuggestions(parsed);

            future.whenComplete((result, error) -> {
                mc.execute(() -> {
                    if (
                        requestId != generation ||
                        !ChatBuffer.isOpen() ||
                        !ChatBuffer.text().equals(text)
                    ) {
                        return;
                    }

                    loading = false;

                    if (error != null || result == null) {
                        suggestions = List.of();
                        return;
                    }

                    suggestions = List.copyOf(
                        result.getList()
                    );

                    selected = 0;
                });
            });
        } catch (RuntimeException ex) {
            loading = false;
            suggestions = List.of();

            System.err.println(
                "[iOS Chat] Completion error: " + ex
            );
        }
    }

    public static void acceptOrRequest(Minecraft mc) {
        String text = ChatBuffer.text();

        if (!text.startsWith("/")) {
            return;
        }

        if (!queriedText.equals(text)) {
            refresh(mc);
            return;
        }

        if (suggestions.isEmpty()) {
            if (!loading) {
                refresh(mc);
            }
            return;
        }

        Suggestion suggestion = suggestions.get(selected);

        String command = text.substring(1);

        // Brigadier ranges refer to the command
        // without the initial slash.
        String completed = suggestion.apply(command);

        replaceBuffer("/" + completed);

        // Refresh suggestions for the new command.
        refresh(mc);
    }

    private static void replaceBuffer(String text) {
        String previous = ChatBuffer.text();

        for (int i = 0; i < previous.length(); i++) {
            ChatBuffer.backspace();
        }

        for (int i = 0; i < text.length(); i++) {
            ChatBuffer.append(text.charAt(i));
        }
    }
}
