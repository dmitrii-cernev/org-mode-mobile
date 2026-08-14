package com.orgzly.android.workspace

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.orgzly.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkspaceActivity : AppCompatActivity() {
    private lateinit var store: WorkspaceStore
    private lateinit var drawer: DrawerLayout
    private lateinit var content: FrameLayout
    private var screen = Screen.NOTES
    private var selectedNodeId: String? = null
    private var agendaDate: String = today()
    private var plainMode = false
    private val blue = Color.rgb(0, 91, 191)
    private val surface = Color.rgb(248, 249, 250)
    private val container = Color.rgb(231, 232, 233)

    enum class Screen { NOTES, CALENDAR, TASKS, DETAIL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = WorkspaceStore(this)
        screen = savedInstanceState?.getString("screen")?.let { Screen.valueOf(it) } ?: Screen.NOTES
        selectedNodeId = savedInstanceState?.getString("node")
        agendaDate = savedInstanceState?.getString("day") ?: today()
        plainMode = savedInstanceState?.getBoolean("plain") ?: false
        buildShell()
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen", screen.name); outState.putString("node", selectedNodeId)
        outState.putString("day", agendaDate.toString()); outState.putBoolean("plain", plainMode)
        super.onSaveInstanceState(outState)
    }

    private fun buildShell() {
        drawer = DrawerLayout(this).apply { setBackgroundColor(surface) }
        content = FrameLayout(this)
        drawer.addView(content, DrawerLayout.LayoutParams(-1, -1))
        val navigation = NavigationView(this).apply {
            setBackgroundColor(Color.WHITE)
            val header = TextView(context).apply {
                text = "ORGZLY\nWorkspace"; setTextColor(blue); textSize = 24f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(28), dp(56), dp(28), dp(28))
            }
            addHeaderView(header)
            menu.add(0, 1, 0, "Notes").setIcon(android.R.drawable.ic_menu_edit)
            menu.add(0, 2, 1, "Calendar").setIcon(android.R.drawable.ic_menu_my_calendar)
            menu.add(0, 3, 2, "Tasks").setIcon(android.R.drawable.checkbox_on_background)
            menu.addSubMenu("WORKSPACE").add("All files • ${store.books().size}").isEnabled = false
            setNavigationItemSelectedListener { item ->
                screen = when (item.itemId) { 2 -> Screen.CALENDAR; 3 -> Screen.TASKS; else -> Screen.NOTES }
                selectedNodeId = null; drawer.closeDrawers(); render(); true
            }
        }
        drawer.addView(navigation, DrawerLayout.LayoutParams(dp(304), -1, Gravity.START))
        setContentView(drawer)
    }

    private fun render() {
        content.removeAllViews()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(surface) }
        root.addView(toolbar(), LinearLayout.LayoutParams(-1, dp(64)))
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(96)) }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(BottomAppBar(this).apply {
            setBackgroundColor(container); replaceMenu(android.view.Menu.NONE)
        }, LinearLayout.LayoutParams(-1, dp(64)))
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
        content.addView(ExtendedFloatingActionButton(this).apply {
            text = if (screen == Screen.DETAIL) "Add child" else "New"
            setIconResource(android.R.drawable.ic_input_add); setBackgroundColor(blue); setTextColor(Color.WHITE)
            setOnClickListener { if (screen == Screen.DETAIL) showCreateDialog(selectedNodeId) else showCreateDialog(null) }
        }, FrameLayout.LayoutParams(-2, dp(56), Gravity.END or Gravity.BOTTOM).apply { setMargins(0, 0, dp(20), dp(24)) })
        when (screen) { Screen.NOTES -> notes(page); Screen.CALENDAR -> calendar(page); Screen.TASKS -> tasks(page); Screen.DETAIL -> detail(page) }
    }

    private fun toolbar(): MaterialToolbar = MaterialToolbar(this).apply {
        setBackgroundColor(surface); elevation = 0f
        val title = TextView(context).apply { text = when(screen) { Screen.NOTES -> "Notes"; Screen.CALENDAR -> "Agenda"; Screen.TASKS -> "Tasks"; Screen.DETAIL -> "Note" }; textSize = 22f; setTextColor(Color.rgb(25,28,29)); setTypeface(typeface, android.graphics.Typeface.BOLD) }
        addView(title, android.widget.Toolbar.LayoutParams(-2, -2, Gravity.CENTER))
        val nav = MaterialButton(context).apply {
            text = if (screen == Screen.DETAIL) "‹" else "☰"; textSize = 26f; setTextColor(blue); minWidth = dp(48)
            setOnClickListener { if (screen == Screen.DETAIL) { screen = Screen.NOTES; selectedNodeId = null; render() } else drawer.openDrawer(GravityCompat.START) }
        }
        addView(nav, android.widget.Toolbar.LayoutParams(dp(56), -1, Gravity.START))
    }

    private fun notes(page: LinearLayout) {
        page.addView(heading("Your knowledge base", "${store.books().size} files • Tap a node to explore its tree"))
        store.books().forEach { book ->
            label(page, book.uppercase())
            store.children(null).filter { it.book == book }.forEach { node -> page.addView(nodeCard(node, 0)) }
        }
    }

    private fun detail(page: LinearLayout) {
        val node = selectedNodeId?.let(store::node) ?: run { screen = Screen.NOTES; render(); return }
        val crumb = TextView(this).apply { text = "${node.book}  /  ${ancestorTitles(node).joinToString(" / ")}"; setTextColor(Color.DKGRAY); textSize = 13f; setPadding(0,0,0,dp(12)) }
        page.addView(crumb)
        page.addView(TextView(this).apply { text = node.title; textSize = 28f; setTextColor(Color.rgb(25,28,29)); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        if (node.state != null) page.addView(chip("${node.state}${node.priority?.let { "  #$it" } ?: ""}"))
        val toggle = MaterialButtonToggleGroup(this).apply { isSingleSelection = true; addView(MaterialButton(context).apply { id = R.id.mode_compiled; text = "Compiled" }); addView(MaterialButton(context).apply { id = R.id.mode_plain; text = "Plain" }); check(if (plainMode) R.id.mode_plain else R.id.mode_compiled); addOnButtonCheckedListener { _, id, checked -> if (checked) { plainMode = id == R.id.mode_plain; render() } } }
        page.addView(toggle, LinearLayout.LayoutParams(-2, dp(48)).apply { topMargin = dp(16) })
        page.addView(card(TextView(this).apply { text = if (plainMode) orgSource(node) else node.body.ifBlank { "No body text yet." }; textSize = if (plainMode) 13f else 16f; setTextColor(Color.DKGRAY); setPadding(dp(16)) }, dp(12)))
        val children = store.children(node.id)
        label(page, "CHILDREN · ${children.size}")
        if (children.isEmpty()) empty(page, "This node has no children. Use Add child to extend the outline.") else children.forEach { page.addView(nodeCard(it, 1)) }
        MaterialButton(this).apply { text = "Browse children"; setOnClickListener { showChildrenSheet(node) } }.also { page.addView(it) }
    }

    private fun calendar(page: LinearLayout) {
        page.addView(heading(displayDate(agendaDate), "Scheduled and deadline entries across your vault"))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        listOf("‹ Previous" to -1L, "Today" to 0L, "Next ›" to 1L).forEach { (label, amount) -> controls.addView(MaterialButton(this).apply { text = label; setOnClickListener { agendaDate = if (amount == 0L) today() else shiftedDay(amount); render() } }) }
        page.addView(controls)
        val entries = store.agenda(agendaDate)
        if (entries.isEmpty()) empty(page, "No scheduled items. Enjoy the open space.") else entries.forEach { n -> page.addView(nodeCard(n, 0)) }
    }

    private fun tasks(page: LinearLayout) {
        page.addView(heading("Open work", "TODOs grouped by Org file and project"))
        val tasks = store.all().filter { it.state != null }
        if (tasks.isEmpty()) empty(page, "No tasks yet. Create one with the New button.")
        tasks.groupBy { it.book }.toSortedMap().forEach { (book, list) ->
            label(page, "$book · ${list.count { it.state != "DONE" }} OPEN")
            list.forEach { task ->
                val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(6), dp(8), dp(6)) }
                row.addView(CheckBox(this).apply { isChecked = task.state == "DONE"; setOnCheckedChangeListener { _, _ -> store.toggle(task.id); render() } })
                row.addView(nodeCard(task, 0), LinearLayout.LayoutParams(0, -2, 1f))
                page.addView(card(row, dp(6)))
            }
        }
    }

    private fun nodeCard(node: WorkspaceNode, depth: Int): View = card(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16 + depth * 16), dp(14), dp(16), dp(14))
        addView(TextView(context).apply { text = buildString { if (node.state != null) append("${node.state}  "); append(node.title) }; textSize = 17f; setTextColor(Color.rgb(25,28,29)); setTypeface(typeface, android.graphics.Typeface.BOLD) })
        val meta = listOfNotNull(node.priority?.let { "Priority $it" }, node.scheduled?.let { "Scheduled $it" }, node.deadline?.let { "Due $it" }, node.tags.takeIf { it.isNotEmpty() }?.joinToString(" · ")).joinToString("  •  ")
        if (meta.isNotBlank()) addView(TextView(context).apply { text = meta; textSize = 12f; setTextColor(blue); setPadding(0, dp(6), 0, 0) })
        setOnClickListener { screen = Screen.DETAIL; selectedNodeId = node.id; plainMode = false; render() }
    })

    private fun showCreateDialog(parent: String?) {
        val field = EditText(this).apply { hint = if (parent == null) "Title" else "Child title"; setSingleLine() }
        val task = CheckBox(this).apply { text = "Create as a task" }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24),0,dp(24),0); addView(field); addView(task) }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle(if (parent == null) "Quick capture" else "Add child").setView(box).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
            val title = field.text.toString().trim(); if (title.isNotEmpty()) { val parentNode = parent?.let(store::node); store.add(WorkspaceNode(parentId = parent, book = parentNode?.book ?: store.books().firstOrNull() ?: "inbox.org", title = title, state = if (task.isChecked) "TODO" else null, scheduled = if (task.isChecked) today() else null)); render() }
        }.show()
    }

    private fun showChildrenSheet(node: WorkspaceNode) {
        val dialog = BottomSheetDialog(this); val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24),dp(20),dp(24),dp(32)); addView(TextView(context).apply { text = "Children of ${node.title}"; textSize = 20f; setTypeface(typeface, android.graphics.Typeface.BOLD) }); store.children(node.id).forEach { child -> addView(MaterialButton(context).apply { text = child.title; gravity = Gravity.LEFT; setOnClickListener { selectedNodeId = child.id; dialog.dismiss(); render() } }) } }; dialog.setContentView(list); dialog.show()
    }

    private fun ancestorTitles(node: WorkspaceNode): List<String> { val result = mutableListOf<String>(); var cursor = node.parentId?.let(store::node); while (cursor != null) { result.add(0, cursor.title); cursor = cursor.parentId?.let(store::node) }; return result + node.title }
    private fun orgSource(node: WorkspaceNode) = "* ${node.state?.plus(" ") ?: ""}${node.title}\n${node.scheduled?.let { "SCHEDULED: <$it>\n" } ?: ""}${node.deadline?.let { "DEADLINE: <$it>\n" } ?: ""}\n${node.body}"
    private fun heading(title: String, subtitle: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,0,0,dp(16)); addView(TextView(context).apply { text = title; textSize = 26f; setTextColor(Color.rgb(25,28,29)); setTypeface(typeface, android.graphics.Typeface.BOLD) }); addView(TextView(context).apply { text = subtitle; textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0,dp(4),0,0) }) }
    private fun label(parent: LinearLayout, value: String) = parent.addView(TextView(this).apply { text = value; textSize = 12f; setTextColor(Color.DKGRAY); setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0,dp(16),0,dp(6)) })
    private fun empty(parent: LinearLayout, message: String) = parent.addView(card(TextView(this).apply { text = message; textSize = 16f; setTextColor(Color.DKGRAY); setPadding(dp(18),dp(22),dp(18),dp(22)) }))
    private fun chip(value: String) = TextView(this).apply { text = "  $value  "; textSize = 12f; setTextColor(Color.WHITE); setBackgroundColor(blue); setPadding(dp(8),dp(6),dp(8),dp(6)) }
    private fun card(child: View): MaterialCardView = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = dp(1).toFloat(); setCardBackgroundColor(Color.WHITE); addView(child); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) } }
    private fun card(child: View, marginTop: Int): MaterialCardView = card(child).also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = marginTop }
    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    private fun shiftedDay(amount: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, amount.toInt()) }.time)
    private fun displayDate(value: String): String = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value) }.getOrNull()?.let { SimpleDateFormat("EEEE, MMMM d", Locale.US).format(it) } ?: value
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
