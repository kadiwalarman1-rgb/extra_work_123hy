package com.example.chatwave

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("chatwave_local", MODE_PRIVATE) }
    private val chats = mutableListOf<ChatItem>()
    private val messagesByChat = mutableMapOf<String, MutableList<ChatMessage>>()
    private var currentChatId: String? = null
    private var currentScreen = "Chats"
    private var currentFilter = "All"
    private var searchQuery = ""
    private var chatListArea: LinearLayout? = null
    private var composer: EditText? = null
    private var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) { prefs.edit().putBoolean("dark_mode", value).apply() }

    private data class ChatItem(
        val id: String,
        var name: String,
        var category: String,
        var avatarColor: String,
        var online: Boolean,
        var lastMessage: String,
        var time: String,
        var unread: Int,
        var pinned: Boolean
    )

    private data class ChatMessage(
        val id: Long,
        var text: String,
        val mine: Boolean,
        var time: String,
        var reaction: String = "",
        var pinned: Boolean = false
    )

    private val bg: Int get() = color(if (darkMode) "#10121A" else "#F5F7FB")
    private val surface: Int get() = color(if (darkMode) "#191D29" else "#FFFFFF")
    private val textColor: Int get() = color(if (darkMode) "#F4F6FC" else "#192033")
    private val muted: Int get() = color(if (darkMode) "#A1A9BC" else "#737D93")
    private val border: Int get() = color(if (darkMode) "#2A3040" else "#E7EBF3")
    private val accent: Int get() = color(prefs.getString("accent", "#6366F1") ?: "#6366F1")
    private val incomingBubble: Int get() = color(if (darkMode) "#242A39" else "#FFFFFF")
    private val outgoingBubble: Int get() = color(if (darkMode) "#37316F" else "#E8E8FF")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadChats()
        refreshSystemBars()
        renderHome()
    }

    private fun loadChats() {
        val raw = prefs.getString("chats", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    chats.add(
                        ChatItem(
                            obj.getString("id"), obj.getString("name"),
                            obj.optString("category", "People"),
                            obj.optString("avatarColor", "#6366F1"),
                            obj.optBoolean("online", false),
                            obj.optString("lastMessage", "Say hello"),
                            obj.optString("time", ""),
                            obj.optInt("unread", 0),
                            obj.optBoolean("pinned", false)
                        )
                    )
                }
            } catch (_: Exception) {
                chats.clear()
            }
        }
        if (chats.isEmpty()) {
            chats.addAll(
                listOf(
                    ChatItem("aarav", "Aarav Mehta", "People", "#3B82F6", true, "Bro, the new build looks great!", "11:42", 2, true),
                    ChatItem("design", "Design Crew", "Groups", "#EC4899", false, "Mira: sent the updated screens", "11:18", 4, true),
                    ChatItem("updates", "Tech Updates", "Channels", "#8B5CF6", false, "Android 16 tips and useful tools", "10:54", 0, false),
                    ChatItem("mom", "Mom", "People", "#F97316", true, "Call me when you are free.", "10:20", 1, false),
                    ChatItem("travel", "Weekend Explorers", "Groups", "#14B8A6", false, "Anyone up for a short trip?", "Yesterday", 0, false),
                    ChatItem("code", "Code with Arman", "Channels", "#64748B", false, "A new Kotlin tutorial is live", "Yesterday", 0, false),
                    ChatItem("saved", "Saved Notes", "People", "#EAB308", false, "Keep useful links and messages here", "Mon", 0, false)
                )
            )
            saveChats()
        }
    }

    private fun saveChats() {
        val array = JSONArray()
        chats.forEach { chat ->
            array.put(
                JSONObject()
                    .put("id", chat.id).put("name", chat.name)
                    .put("category", chat.category).put("avatarColor", chat.avatarColor)
                    .put("online", chat.online).put("lastMessage", chat.lastMessage)
                    .put("time", chat.time).put("unread", chat.unread).put("pinned", chat.pinned)
            )
        }
        prefs.edit().putString("chats", array.toString()).apply()
    }

    private fun messagesFor(chat: ChatItem): MutableList<ChatMessage> {
        messagesByChat[chat.id]?.let { return it }
        val list = mutableListOf<ChatMessage>()
        val raw = prefs.getString("messages_" + chat.id, null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChatMessage(
                            obj.optLong("id", System.currentTimeMillis() + i),
                            obj.optString("text", ""),
                            obj.optBoolean("mine", false),
                            obj.optString("time", ""),
                            obj.optString("reaction", ""),
                            obj.optBoolean("pinned", false)
                        )
                    )
                }
            } catch (_: Exception) {
                list.clear()
            }
        }
        if (list.isEmpty()) {
            list.add(ChatMessage(System.currentTimeMillis(), chat.lastMessage, false, chat.time))
        }
        messagesByChat[chat.id] = list
        saveMessages(chat.id)
        return list
    }

    private fun saveMessages(chatId: String) {
        val array = JSONArray()
        (messagesByChat[chatId] ?: mutableListOf()).forEach { msg ->
            array.put(
                JSONObject().put("id", msg.id).put("text", msg.text)
                    .put("mine", msg.mine).put("time", msg.time)
                    .put("reaction", msg.reaction).put("pinned", msg.pinned)
            )
        }
        prefs.edit().putString("messages_" + chatId, array.toString()).apply()
    }

    private fun renderHome() {
        currentChatId = null
        currentScreen = "Chats"
        refreshSystemBars()
        val root = column(bg)
        root.addView(header("Chats", "Your people, one place.", "＋" to { showNewChatDialog() }, "⚙" to { renderSettings() }))
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val page = column(bg)
        page.setPadding(dp(18), dp(8), dp(18), dp(22))
        page.addView(text("Good day, Arman", 25f, textColor, true))
        page.addView(text("Premium-style tools included · no subscription in this demo", 12f, muted, false), marginParams(bottom = 14))
        val search = EditText(this).apply {
            hint = "Search chats and channels"
            setSingleLine(true)
            textSize = 14f
            setTextColor(textColor)
            setHintTextColor(muted)
            background = roundedBackground(surface, 16, border)
            setPadding(dp(16), dp(2), dp(16), dp(2))
            setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            setText(searchQuery)
        }
        page.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                fillChatList()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))
        }
        listOf("All", "People", "Groups", "Channels").forEach { filter ->
            val chip = text(filter, 12f, if (currentFilter == filter) Color.WHITE else muted, currentFilter == filter)
            chip.gravity = Gravity.CENTER
            chip.setPadding(dp(13), dp(9), dp(13), dp(9))
            chip.background = roundedBackground(if (currentFilter == filter) accent else surface, 22, if (currentFilter == filter) null else border)
            chip.setOnClickListener {
                currentFilter = filter
                chipsRefresh()
            }
            chips.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)).apply {
                rightMargin = dp(7)
            })
        }
        page.addView(chips)
        chatListArea = column(bg)
        page.addView(chatListArea)
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomNav("Chats"))
        setContentView(root)
        fillChatList()
    }

    private fun chipsRefresh() {
        renderHome()
    }

    private fun fillChatList() {
        val area = chatListArea ?: return
        area.removeAllViews()
        var shown = chats.filter {
            (currentFilter == "All" || it.category == currentFilter) &&
                (searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.lastMessage.contains(searchQuery, true))
        }.sortedWith(compareByDescending<ChatItem> { it.pinned }.thenByDescending { it.time })
        if (shown.isEmpty()) {
            area.addView(text("No chats found. Try a different search.", 14f, muted), marginParams(top = 24, bottom = 24))
            return
        }
        if (shown.any { it.pinned }) {
            area.addView(text("PINNED", 11f, muted, true), marginParams(top = 3, bottom = 9))
        }
        shown.forEach { chat -> area.addView(chatRow(chat), marginParams(bottom = 9)) }
    }

    private fun chatRow(chat: ChatItem): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(13), dp(12), dp(13))
            background = roundedBackground(surface, 18, border)
            isClickable = true
            isFocusable = true
        }
        row.addView(avatar(chat.name, chat.avatarColor, 50))
        val middle = column(surface)
        middle.setPadding(dp(11), 0, dp(5), 0)
        val nameRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(text(chat.name, 14f, textColor, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nameRow.addView(text(if (chat.pinned) "📌 " + chat.time else chat.time, 10f, muted))
        middle.addView(nameRow)
        middle.addView(text(chat.lastMessage, 12f, muted, false).apply { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }, marginParams(top = 5))
        row.addView(middle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (chat.unread > 0) {
            val badge = text(chat.unread.toString(), 10f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = roundedBackground(accent, 18)
            }
            row.addView(badge, LinearLayout.LayoutParams(dp(25), dp(25)).apply { leftMargin = dp(4) })
        }
        row.setOnClickListener { openChat(chat.id) }
        row.setOnLongClickListener { showChatActions(chat); true }
        return row
    }

    private fun renderConversation(chatId: String) {
        val chat = chats.firstOrNull { it.id == chatId } ?: run { renderHome(); return }
        currentChatId = chatId
        currentScreen = "Conversation"
        refreshSystemBars()
        val root = column(bg)
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = surface
        }
        top.addView(iconButton("‹") { currentChatId = null; renderHome() }, LinearLayout.LayoutParams(dp(42), dp(42)))
        top.addView(avatar(chat.name, chat.avatarColor, 42), LinearLayout.LayoutParams(dp(42), dp(42)).apply { leftMargin = dp(5) })
        val info = column(surface)
        info.setPadding(dp(10), 0, 0, 0)
        info.addView(text(chat.name, 15f, textColor, true))
        info.addView(text(if (chat.online) "online · local demo" else chat.category + " · local demo", 11f, muted))
        top.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(iconButton("⋯") { showChatActions(chat) }, LinearLayout.LayoutParams(dp(40), dp(42)))
        root.addView(top)
        root.addView(separator())
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val messagesColumn = column(bg)
        messagesColumn.setPadding(dp(14), dp(14), dp(14), dp(18))
        messagesFor(chat).forEach { msg ->
            val holder = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = if (msg.mine) Gravity.END else Gravity.START
                setPadding(0, dp(3), 0, dp(3))
            }
            val bubble = text(
                (if (msg.pinned) "📌 PINNED\n" else "") + msg.text +
                    (if (msg.reaction.isNotEmpty()) "\n" + msg.reaction else "") + "\n" + msg.time,
                14f, textColor
            )
            bubble.setPadding(dp(13), dp(10), dp(13), dp(8))
            bubble.maxWidth = dp(285)
            bubble.background = roundedBackground(if (msg.mine) outgoingBubble else incomingBubble, 18, if (msg.mine) null else border)
            holder.addView(bubble)
            bubble.setOnLongClickListener { showMessageActions(chat, msg); true }
            messagesColumn.addView(holder, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        scroll.addView(messagesColumn)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val composeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = surface
        }
        composeRow.addView(iconButton("＋") { chooseAttachment() }, LinearLayout.LayoutParams(dp(40), dp(44)))
        composer = EditText(this).apply {
            hint = "Message (saved on this device)"
            textSize = 14f
            setTextColor(textColor)
            setHintTextColor(muted)
            setSingleLine(true)
            background = roundedBackground(bg, 22, border)
            setPadding(dp(15), 0, dp(15), 0)
        }
        composeRow.addView(composer, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(5); rightMargin = dp(5) })
        composeRow.addView(iconButton("☺") {
            val input = composer ?: return@iconButton
            val at = input.selectionStart.coerceAtLeast(0)
            input.text.insert(at, " 😊")
        }, LinearLayout.LayoutParams(dp(40), dp(44)))
        composeRow.addView(iconButton("➤", true) { sendMessage() }, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(composeRow)
        setContentView(root)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val id = currentChatId ?: return
        val value = composer?.text?.toString()?.trim().orEmpty()
        if (value.isEmpty()) {
            Toast.makeText(this, "Type a message first", Toast.LENGTH_SHORT).show()
            return
        }
        val chat = chats.firstOrNull { it.id == id } ?: return
        val message = ChatMessage(System.currentTimeMillis(), value, true, nowTime())
        messagesFor(chat).add(message)
        chat.lastMessage = value
        chat.time = nowTime()
        chat.unread = 0
        messagesByChat[id] = messagesFor(chat)
        saveMessages(id)
        saveChats()
        renderConversation(id)
    }

    private fun chooseAttachment() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Choose a file"), 404)
        } catch (_: Exception) {
            Toast.makeText(this, "No file picker is available on this device", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Kept for compatibility with the simple demo file picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 404 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val fileName = displayName(uri) ?: uri.lastPathSegment ?: "selected file"
            val id = currentChatId ?: return
            val chat = chats.firstOrNull { it.id == id } ?: return
            val msg = ChatMessage(System.currentTimeMillis(), "📎 $fileName\nAttachment selected locally; cloud upload is not connected in this demo.", true, nowTime())
            messagesFor(chat).add(msg)
            chat.lastMessage = "📎 $fileName"
            chat.time = nowTime()
            saveMessages(id)
            saveChats()
            renderConversation(id)
        }
    }

    private fun displayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else null
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun showMessageActions(chat: ChatItem, message: ChatMessage) {
        val choices = arrayOf("React 👍", "React ❤️", "React 😂", "React 🔥", if (message.pinned) "Unpin message" else "Pin message", "Delete message")
        AlertDialog.Builder(this)
            .setTitle("Message options")
            .setItems(choices) { _, index ->
                when (index) {
                    0, 1, 2, 3 -> message.reaction = choices[index].removePrefix("React ")
                    4 -> message.pinned = !message.pinned
                    5 -> messagesFor(chat).removeAll { it.id == message.id }
                }
                saveMessages(chat.id)
                renderConversation(chat.id)
            }
            .show()
    }

    private fun showChatActions(chat: ChatItem) {
        val choices = arrayOf(if (chat.pinned) "Unpin chat" else "Pin chat", if (chat.unread > 0) "Mark read" else "Mark unread", "Delete local chat")
        AlertDialog.Builder(this)
            .setTitle(chat.name)
            .setItems(choices) { _, index ->
                when (index) {
                    0 -> chat.pinned = !chat.pinned
                    1 -> chat.unread = if (chat.unread > 0) 0 else 1
                    2 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Delete this local chat?")
                            .setMessage("This removes the conversation from this device only.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                chats.removeAll { it.id == chat.id }
                                messagesByChat.remove(chat.id)
                                prefs.edit().remove("messages_" + chat.id).apply()
                                saveChats()
                                if (currentChatId == chat.id) renderHome() else fillChatList()
                            }.show()
                        return@setItems
                    }
                }
                saveChats()
                if (currentChatId == null) fillChatList() else renderConversation(chat.id)
            }
            .show()
    }

    private fun showNewChatDialog() {
        val input = EditText(this).apply {
            hint = "Contact or group name"
            setSingleLine(true)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("New conversation")
            .setMessage("Create a local demo chat")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val id = "local_" + System.currentTimeMillis()
                    val colors = listOf("#6366F1", "#EC4899", "#14B8A6", "#F97316", "#3B82F6")
                    val chat = ChatItem(id, name, "People", colors[chats.size % colors.size], false, "New conversation", nowTime(), 0, false)
                    chats.add(0, chat)
                    messagesByChat[id] = mutableListOf(ChatMessage(System.currentTimeMillis(), "You created this local conversation. Messages are stored on this device.", false, nowTime()))
                    saveChats()
                    saveMessages(id)
                    searchQuery = ""
                    currentFilter = "All"
                    openChat(id)
                }
            }
            .show()
    }

    private fun renderContacts() {
        currentChatId = null
        currentScreen = "Contacts"
        refreshSystemBars()
        val root = column(bg)
        root.addView(header("Contacts", "Start a local conversation.", "＋" to { showNewChatDialog() }, "⌕" to { renderHome() }))
        val scroll = ScrollView(this)
        val content = column(bg)
        content.setPadding(dp(18), dp(15), dp(18), dp(20))
        chats.sortedBy { it.name.lowercase(Locale.getDefault()) }.forEach { chat ->
            content.addView(chatRow(chat), marginParams(bottom = 9))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomNav("Contacts"))
        setContentView(root)
    }

    private fun renderSettings() {
        currentChatId = null
        currentScreen = "Settings"
        refreshSystemBars()
        val root = column(bg)
        root.addView(header("Settings", "Make ChatWave feel like yours.", "＋" to { showNewChatDialog() }, "✓" to { renderHome() }))
        val scroll = ScrollView(this)
        val content = column(bg)
        content.setPadding(dp(18), dp(12), dp(18), dp(24))
        val intro = column(surface).apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBackground(surface, 22, border)
        }
        intro.addView(text("CHATWAVE PLUS", 11f, accent, true))
        intro.addView(text("Included in this app", 22f, textColor, true), marginParams(top = 7))
        intro.addView(text("Premium-style demo features are available without an in-app subscription.", 13f, muted), marginParams(top = 6, bottom = 12))
        intro.addView(text("✓ Themes    ✓ Reactions    ✓ Pinned chats    ✓ Saved local history", 12f, textColor, true))
        content.addView(intro, marginParams(bottom = 14))

        val themeCard = column(surface).apply {
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = roundedBackground(surface, 20, border)
        }
        val toggleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val toggleInfo = column(surface)
        toggleInfo.addView(text("Dark mode", 15f, textColor, true))
        toggleInfo.addView(text("Change the app appearance", 12f, muted))
        toggleRow.addView(toggleInfo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val toggle = Switch(this).apply {
            isChecked = darkMode
            setOnCheckedChangeListener { _, enabled ->
                darkMode = enabled
                if (currentScreen == "Settings") renderSettings()
            }
        }
        toggleRow.addView(toggle)
        themeCard.addView(toggleRow)
        themeCard.addView(separator(), marginParams(top = 12, bottom = 12))
        themeCard.addView(text("Accent color", 14f, textColor, true), marginParams(bottom = 9))
        val swatches = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("#6366F1", "#0EA5E9", "#10B981", "#EC4899", "#F97316").forEach { hex ->
            val swatch = TextView(this).apply {
                text = if (prefs.getString("accent", "#6366F1") == hex) "✓" else ""
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = ovalBackground(color(hex))
                setOnClickListener {
                    prefs.edit().putString("accent", hex).apply()
                    renderSettings()
                }
            }
            swatches.addView(swatch, LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(10) })
        }
        themeCard.addView(swatches)
        content.addView(themeCard, marginParams(bottom = 14))

        val featureCard = column(surface).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBackground(surface, 20, border)
        }
        featureCard.addView(text("What's included", 17f, textColor, true), marginParams(bottom = 10))
        listOf(
            "🔎  Search chats and channels",
            "📌  Pin chats and individual messages",
            "💬  React to messages with emoji",
            "🎨  Dark mode and five accent colors",
            "📎  Select a local file to reference in chat",
            "💾  Keep your conversations on this device"
        ).forEach { featureCard.addView(text(it, 13f, textColor), marginParams(top = 8, bottom = 8)) }
        content.addView(featureCard, marginParams(bottom = 14))
        val limitation = text(
            "Prototype status: no online accounts, cloud sync, remote message delivery, calls, or real cloud file uploads are connected. The demo's messages are local to this device.",
            12f, muted
        )
        limitation.setPadding(dp(5), dp(5), dp(5), dp(12))
        content.addView(limitation)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomNav("Settings"))
        setContentView(root)
    }

    private fun header(
        title: String,
        subtitle: String,
        firstAction: Pair<String, () -> Unit>,
        secondAction: Pair<String, () -> Unit>
    ): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(15), dp(12))
            background = surface
        }
        bar.addView(avatar("ChatWave", "#6366F1", 44))
        val labels = column(surface)
        labels.setPadding(dp(10), 0, dp(5), 0)
        labels.addView(text(title, 17f, textColor, true))
        labels.addView(text(subtitle, 10f, muted))
        bar.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(iconButton(firstAction.first, firstAction.second), LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(3) })
        bar.addView(iconButton(secondAction.first, secondAction.second), LinearLayout.LayoutParams(dp(42), dp(42)))
        return bar
    }

    private fun bottomNav(selected: String): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = surface
        }
        listOf("Chats", "Contacts", "Settings").forEach { name ->
            val button = text(
                when (name) { "Chats" -> "◉  Chats"; "Contacts" -> "◎  Contacts"; else -> "⚙  Settings" },
                12f, if (selected == name) accent else muted, selected == name
            )
            button.gravity = Gravity.CENTER
            button.setPadding(dp(8), dp(13), dp(8), dp(13))
            button.background = roundedBackground(if (selected == name) color(if (darkMode) "#292747" else "#EEEEFF") else surface, 15)
            button.setOnClickListener {
                when (name) { "Chats" -> renderHome(); "Contacts" -> renderContacts(); else -> renderSettings() }
            }
            nav.addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        }
        return nav
    }

    private fun iconButton(symbol: String, primary: Boolean = false, action: () -> Unit): TextView {
        return text(symbol, 22f, if (primary) Color.WHITE else textColor, true).apply {
            gravity = Gravity.CENTER
            background = roundedBackground(if (primary) accent else bg, 14, if (primary) null else border)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun avatar(name: String, hex: String, size: Int): TextView {
        val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase(Locale.getDefault()) }.ifBlank { "C" }
        return text(initials, if (size > 45) 14f else 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = ovalBackground(color(hex))
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        }
    }

    private fun column(backgroundColor: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(backgroundColor)
    }

    private fun text(value: String, size: Float, tint: Int, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(tint)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun separator(): View = View(this).apply {
        setBackgroundColor(border)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
    }

    private fun marginParams(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun roundedBackground(fill: Int, radius: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun ovalBackground(fill: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
        }

    private fun color(hex: String): Int = Color.parseColor(hex)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun refreshSystemBars() {
        window.statusBarColor = if (darkMode) color("#10121A") else color("#F5F7FB")
        window.navigationBarColor = if (darkMode) color("#191D29") else Color.WHITE
        window.decorView.systemUiVisibility = if (darkMode) 0
        else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun openChat(id: String) {
        chats.firstOrNull { it.id == id }?.unread = 0
        saveChats()
        renderConversation(id)
    }

    override fun onBackPressed() {
        if (currentChatId != null) {
            currentChatId = null
            renderHome()
        } else if (currentScreen != "Chats") {
            renderHome()
        } else {
            super.onBackPressed()
        }
    }
}
