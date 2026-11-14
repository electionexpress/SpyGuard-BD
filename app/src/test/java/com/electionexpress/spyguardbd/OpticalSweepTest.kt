package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpticalSweepTest {
    @Test
    fun newSweepStartsWithEveryAreaNotChecked() {
        val sweep = OpticalSweepSession()

        assertEquals(8, sweep.totalCount)
        assertEquals(0, sweep.checkedCount)
        assertEquals(0, sweep.followUpCount)
        assertTrue(sweep.orderedEntries.all { it.result == OpticalSweepResult.NOT_CHECKED })
    }

    @Test
    fun recordingAnAreaStoresItsResultAndTimestamp() {
        val sweep = OpticalSweepSession().record(
            areaId = "ceiling",
            result = OpticalSweepResult.POSSIBLE_REFLECTION,
            observedAtMillis = 123_456L
        )

        assertEquals(1, sweep.checkedCount)
        assertEquals(1, sweep.followUpCount)
        assertEquals(OpticalSweepResult.POSSIBLE_REFLECTION, sweep.entryFor("ceiling").result)
        assertEquals(123_456L, sweep.entryFor("ceiling").observedAtMillis)
    }

    @Test
    fun noVisibleCandidateIsNotPresentedAsAnAllClear() {
        val result = OpticalSweepResult.NO_VISIBLE_CANDIDATE

        assertFalse(result.needsFollowUp)
        assertTrue(result.reportText.contains("নিরাপত্তার নিশ্চয়তা নয়"))
    }

    @Test
    fun reflectionAndManualReviewRemainUnconfirmedFollowUpItems() {
        assertTrue(OpticalSweepResult.POSSIBLE_REFLECTION.needsFollowUp)
        assertTrue(OpticalSweepResult.POSSIBLE_REFLECTION.reportText.contains("ক্যামেরার প্রমাণ নয়"))
        assertTrue(OpticalSweepResult.NEEDS_MANUAL_RECHECK.reportText.contains("নিশ্চিত নয়"))
    }

    @Test
    fun selectingNotCheckedClearsTheTimestamp() {
        val recorded = OpticalSweepSession().record(
            areaId = "mirrors",
            result = OpticalSweepResult.NEEDS_MANUAL_RECHECK,
            observedAtMillis = 500L
        )
        val reset = recorded.record("mirrors", OpticalSweepResult.NOT_CHECKED, 600L)

        assertEquals(0, reset.checkedCount)
        assertEquals(0, reset.followUpCount)
        assertNull(reset.entryFor("mirrors").observedAtMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownAreaCannotBeRecorded() {
        OpticalSweepSession().record("unknown", OpticalSweepResult.POSSIBLE_REFLECTION, 100L)
    }
}
