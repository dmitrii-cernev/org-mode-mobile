package com.orgzly.android.workspace

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class WorkspaceNode(
    val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null,
    val book: String,
    var title: String,
    var body: String = "",
    var state: String? = null,
    var priority: String? = null,
    var scheduled: String? = null,
    var deadline: String? = null,
    var tags: List<String> = emptyList()
)

/** Small presentation repository. It persists user-created nodes while the retained Org parser/data
 * layer remains the source for future import/sync adapters. */
class WorkspaceStore(context: Context) {
    private val preferences = context.getSharedPreferences("workspace_nodes", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<MutableList<WorkspaceNode>>() {}.type
    private var nodes: MutableList<WorkspaceNode> = load()

    fun all(): List<WorkspaceNode> = nodes.toList()
    fun node(id: String): WorkspaceNode? = nodes.firstOrNull { it.id == id }
    fun children(parentId: String?): List<WorkspaceNode> = nodes.filter { it.parentId == parentId }
    fun books(): List<String> = nodes.map { it.book }.distinct().sorted()
    fun add(node: WorkspaceNode): WorkspaceNode { nodes.add(node); persist(); return node }
    fun toggle(nodeId: String) { node(nodeId)?.let { it.state = if (it.state == "DONE") "TODO" else "DONE"; persist() } }
    fun agenda(day: String): List<WorkspaceNode> = nodes.filter { it.scheduled == day || it.deadline == day }

    private fun load(): MutableList<WorkspaceNode> {
        val serialized = preferences.getString("nodes", null)
        if (serialized != null) return gson.fromJson(serialized, type)
        return seed().toMutableList().also { nodes -> preferences.edit().putString("nodes", gson.toJson(nodes)).apply() }
    }
    private fun persist() = preferences.edit().putString("nodes", gson.toJson(nodes)).apply()

    private fun seed(): List<WorkspaceNode> {
        val calendar = Calendar.getInstance()
        val today = dateString(calendar)
        val project = WorkspaceNode(book = "projects.org", title = "Mobile workspace", body = "A calm, focused home for the work that matters.", tags = listOf("STRATEGY"))
        val research = WorkspaceNode(book = "notes.org", title = "Research inbox", body = "Capture ideas, links, and references before refining them.", tags = listOf("REFERENCE"))
        return listOf(
            project,
            WorkspaceNode(parentId = project.id, book = "projects.org", title = "Polish navigation", body = "Review the drawer labels and breadcrumb flow.", state = "TODO", priority = "A", scheduled = today, tags = listOf("URGENT")),
            WorkspaceNode(parentId = project.id, book = "projects.org", title = "Share design review", body = "Prepare the weekly walkthrough.", state = "TODO", priority = "B", deadline = dateString(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) })),
            WorkspaceNode(book = "journal.org", title = "Weekly reflection", body = "What created momentum this week?", scheduled = today),
            research,
            WorkspaceNode(parentId = research.id, book = "notes.org", title = "Material 3 patterns", body = "Prefer tonal surfaces, clear hierarchy, and one obvious action.")
        )
    }

    private fun dateString(calendar: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}
