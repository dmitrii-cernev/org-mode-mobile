package com.orgzly.android.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkspaceNodeTest {
    @Test fun `node hierarchy retains parent relationship`() {
        val root = WorkspaceNode(id = "root", book = "work.org", title = "Project")
        val child = WorkspaceNode(parentId = root.id, book = "work.org", title = "Ship", state = "TODO")
        assertEquals("root", child.parentId)
        assertTrue(child.state == "TODO")
    }

    @Test fun `agenda date values use ISO format`() {
        val day = LocalDate.of(2026, 8, 14)
        val item = WorkspaceNode(book = "inbox.org", title = "Plan", scheduled = day.toString())
        assertEquals("2026-08-14", item.scheduled)
    }
}
