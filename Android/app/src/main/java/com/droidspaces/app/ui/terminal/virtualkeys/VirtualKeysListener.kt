package com.droidspaces.app.ui.terminal.virtualkeys

import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import com.droidspaces.app.ui.terminal.TerminalScreenState
import com.termux.terminal.TerminalSession

class VirtualKeysListener(val session: TerminalSession) : VirtualKeysView.IVirtualKeysView {

    override fun onVirtualKeyButtonClick(
        view: View?,
        buttonInfo: VirtualKeyButton?,
        button: Button?,
    ) {
        val key = buttonInfo?.key ?: return

        if (key == "PASTE") {
            pasteClipboard(view)
            return
        }
        // MORE and BACK are consumed by VirtualKeysView itself when a second
        // matrix is installed. Do not leak their labels into the shell.
        if (key == "MORE" || key == "BACK") return

        // Route every mapped key through TerminalView.onKeyDown(), not just the
        // arrows. That keeps DECCKM, modifier state and keypad mode in one place,
        // and makes DEL, INS, F1-F12 and TAB send their terminal sequences instead
        // of the literal key name.
        VirtualKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS[key]?.let { keyCode ->
            TerminalScreenState.terminalView.get()?.let { terminalView ->
                terminalView.requestFocus()
                terminalView.onKeyDown(keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            return
        }

        // Literal keys, such as a custom symbol added to a future layout.
        session.write(key)
    }

    private fun pasteClipboard(view: View?) {
        val context = view?.context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val text = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.takeIf { it.isNotEmpty() }
            ?: return
        TerminalScreenState.terminalView.get()?.requestFocus()
        session.emulator?.paste(text) ?: session.write(text)
    }

    override fun performVirtualKeyButtonHapticFeedback(
        view: View?,
        buttonInfo: VirtualKeyButton?,
        button: Button?,
    ): Boolean = false
}
