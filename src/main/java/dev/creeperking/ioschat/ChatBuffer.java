package dev.creeperking.ioschat;

/** Pure Java input buffer: deliberately independent of Minecraft text widgets. */
public final class ChatBuffer {
    private ChatBuffer() {}
    private static final StringBuilder TEXT = new StringBuilder();
    private static boolean open;
    public static boolean isOpen() { return open; }
    public static String text() { return TEXT.toString(); }
    public static void toggle() { open = !open; if (open) TEXT.setLength(0); }
    public static void close() { open = false; TEXT.setLength(0); }
    public static void backspace() {
        if (!TEXT.isEmpty()) TEXT.deleteCharAt(TEXT.length() - 1);
    }
    public static void append(char c) { if (TEXT.length() < 256) TEXT.append(c); }
}
