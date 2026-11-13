package com.electionexpress.spyguardbd

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionReportStoreTest {
    @Test
    fun savesListsReadsAndDeletesOnlyLocalTextReports() {
        val directory = Files.createTempDirectory("spyguard-history").toFile()
        try {
            val store = InspectionReportStore(directory)
            val report = InspectionReport(
                roomLabel = "Bedroom",
                observedAt = "2026-11-13 10:00:00",
                observations = OpticalInspection(lensLikeElement = true),
                notes = "Checked from two angles"
            )

            val savedName = store.save(report, "2026-11-13_100000")
            val listed = store.list()

            assertEquals("2026-11-13_100000.txt", savedName)
            val secondSavedName = store.save(report.copy(notes = "Second inspection"), "2026-11-13_100000")
            assertEquals("2026-11-13_100000_2.txt", secondSavedName)
            assertEquals(2, store.list().size)
            assertTrue(store.list().any { it.content.contains("Second inspection") })
            assertEquals(2, listed.size)
            assertTrue(listed.single().content.contains("Bedroom"))
            assertTrue(listed.single().content.contains("Checked from two angles"))
            assertTrue(store.delete(savedName))
            assertTrue(store.list().isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsPathTraversalAndDoesNotCreateHistoryUntilSaved() {
        val directory = Files.createTempDirectory("spyguard-history-empty").toFile()
        try {
            val store = InspectionReportStore(directory)
            assertTrue(store.list().isEmpty())
            assertFalse(store.delete("../outside.txt"))
            assertFalse(store.delete("notes.pdf"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
