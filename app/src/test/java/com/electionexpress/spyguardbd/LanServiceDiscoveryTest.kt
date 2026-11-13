package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Test

class LanServiceDiscoveryTest {
    @Test fun identifiesCommonLanServiceTypes() {
        assertEquals("মিডিয়া/কাস্ট ডিভাইস", classifyLanServiceType("_googlecast._tcp."))
        assertEquals("প্রিন্টার", classifyLanServiceType("_ipp._tcp."))
        assertEquals("কম্পিউটার/সার্ভার সেবা", classifyLanServiceType("_ssh._tcp."))
        assertEquals("স্মার্ট-হোম ডিভাইস", classifyLanServiceType("_hap._tcp."))
        assertEquals("ওয়েব সেবা", classifyLanServiceType("_https._tcp."))
    }

    @Test fun unknownServiceTypeStaysGeneric() {
        assertEquals("অন্যান্য LAN সেবা", classifyLanServiceType("_unlisted._tcp."))
    }
}
