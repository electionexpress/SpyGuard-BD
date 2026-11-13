package com.electionexpress.spyguardbd

import java.io.File

data class SavedInspectionReport(
    val fileName: String,
    val content: String
)

/**
 * Explicitly saved, app-private inspection history. Nothing is uploaded or shared.
 * The store keeps plain text so reports remain portable and easy to inspect.
 */
class InspectionReportStore(private val filesDirectory: File) {
    private val reportsDirectory: File
        get() = File(filesDirectory, "inspection-reports")

    fun save(report: InspectionReport, fileStem: String): String {
        require(fileStem.isNotBlank()) { "Report file name cannot be blank" }
        if (!reportsDirectory.exists() && !reportsDirectory.mkdirs()) {
            throw IllegalStateException("Could not create report history directory")
        }
        val safeStem = fileStem.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').take(48)
            .ifBlank { "inspection" }
        val file = File(reportsDirectory, "$safeStem.txt")
        file.writeText(report.toPlainText(), Charsets.UTF_8)
        return file.name
    }

    fun list(): List<SavedInspectionReport> {
        if (!reportsDirectory.exists()) return emptyList()
        return reportsDirectory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("txt", ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .map { file -> SavedInspectionReport(file.name, file.readText(Charsets.UTF_8)) }
    }

    fun delete(fileName: String): Boolean {
        val safeName = File(fileName).name
        if (safeName != fileName || !safeName.endsWith(".txt", ignoreCase = true)) return false
        val target = File(reportsDirectory, safeName)
        return target.isFile && target.delete()
    }
}
