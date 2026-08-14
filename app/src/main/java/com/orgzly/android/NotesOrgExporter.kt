package com.orgzly.android

import com.orgzly.R
import com.orgzly.android.data.mappers.OrgMapper
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.NoteProperty
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.org.parser.OrgParserSettings
import com.orgzly.org.parser.OrgParserWriter
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.Charset

class NotesOrgExporter {
    /**
     * Writes content of the book to a specified file.
     */
    @Throws(IOException::class)
    fun exportBook(book: Book, notes: Collection<NoteView>, file: File) {
        val encoding = book.usedEncoding ?: Charset.defaultCharset().name()

        PrintWriter(file, encoding).use {
            exportBook(book, notes, it)
        }
    }

    @Throws(IOException::class)
    fun exportBook(book: Book, notes: Collection<NoteView>, writer: Writer) {
        val orgWriter = OrgParserWriter(getOrgParserSettingsFromPreferences())

        writer.write(orgWriter.whiteSpacedFilePreface(book.preface))

        notes.forEach { noteView ->
            writer.write(exportNote(noteView, emptyList(), book.isIndented == true))
        }
    }

    /**
     * Exports a single note from NoteView to Org format string.
     */
    fun exportNote(
        noteView: NoteView,
        properties: Collection<NoteProperty> = emptyList(),
        isIndented: Boolean = false
    ): String {
        val orgWriter = OrgParserWriter(getOrgParserSettingsFromPreferences())
        val note = noteView.note

        val head = OrgMapper.toOrgHead(noteView).apply {
            this.properties = OrgMapper.toOrgProperties(properties)
        }

        return orgWriter.whiteSpacedHead(head, note.position.level, isIndented)
    }

    companion object {
        private fun getOrgParserSettingsFromPreferences(): OrgParserSettings {
            val parserSettings = OrgParserSettings.getBasic()
            val context = App.getAppContext()

            when (AppPreferences.separateNotesWithNewLine(context)) {
                context.getString(R.string.pref_value_separate_notes_with_new_line_always) ->
                    parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.ALWAYS

                context.getString(R.string.pref_value_separate_notes_with_new_line_multi_line_notes_only) ->
                    parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.MULTI_LINE_NOTES_ONLY

                context.getString(R.string.pref_value_separate_notes_with_new_line_never) ->
                    parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.NEVER
            }

            parserSettings.separateHeaderAndContentWithNewLine =
                AppPreferences.separateHeaderAndContentWithNewLine(context)

            parserSettings.tagsColumn = AppPreferences.tagsColumn(context)
            parserSettings.orgIndentMode = AppPreferences.orgIndentMode(context)
            parserSettings.orgIndentIndentationPerLevel = AppPreferences.orgIndentIndentationPerLevel(context)

            return parserSettings
        }
    }
}
