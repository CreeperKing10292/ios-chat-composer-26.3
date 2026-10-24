# iOS Chat Composer — Minecraft 26.3 (experimental)

A **separate** Fabric mod. Does not replace or modify `MC263-iOS-Fix`.

## Design
- F8 toggles raw-key capture during gameplay (when no Minecraft screen is open).
- A–Z, 0–9, Space and Backspace update a Java `StringBuilder`.
- Current buffer appears as a Minecraft action-bar message (not a native text box).
- Enter sends via the normal client chat connection; Escape cancels.
- Captures key events *only if they reach Minecraft's KeyboardHandler*.

## Known limitations
- **Unverified build**: Minecraft 26.3 may have changed `KeyboardHandler.keyPress` or `ClientPacketListener.sendChat` signatures. Check GitHub Actions compilation output. `require=0` avoids failing at runtime if injection target is missing, but then F8 will do nothing.
- Only basic US-layout letters and numbers; Shift symbols, IME and paste are unsupported.
- The first letter may still crash in native iOS code, before the Fabric mod receives it.
- This is a key-capture overlay, not a new GUI Screen; action bar shows the buffer.
- The first test should be F8, then one letter, then Escape. Do not enter sensitive information.

## Build from iPad
Create a **new GitHub repository**, upload the extracted project files, and run Actions → Build iOS Chat Composer → Run workflow. Download the JAR from the workflow artifact, not the `-sources.jar`.

## Test
Use your latest separate Amethyst 6.0.0 development test build with SDL forwarding disabled in both `pressesBegan:` and `pressesEnded:`, Minecraft 26.3, Fabric Loader 0.20.1 and Java 25. Keep `MC263-iOS-Fix` installed for the menu issue.
