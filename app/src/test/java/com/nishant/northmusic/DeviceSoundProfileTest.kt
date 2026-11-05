package com.nishant.northmusic

import com.nishant.northmusic.data.model.DeviceCategory
import com.nishant.northmusic.data.model.DeviceSoundProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceSoundProfileTest {

    @Test
    fun testSamsungBSeriesMatching() {
        val b67ef = DeviceSoundProfiles.matchProfile("Samsung Soundbar B67EF")
        assertNotNull(b67ef)
        assertEquals(DeviceSoundProfiles.SAMSUNG_B_SERIES, b67ef)
        assertEquals(DeviceCategory.HOME_THEATER, b67ef?.category)

        val b670 = DeviceSoundProfiles.matchProfile("HW-B670")
        assertEquals(DeviceSoundProfiles.SAMSUNG_B_SERIES, b670)

        val b550 = DeviceSoundProfiles.matchProfile("Samsung B550 Soundbar")
        assertEquals(DeviceSoundProfiles.SAMSUNG_B_SERIES, b550)
    }

    @Test
    fun testSamsungQSeriesMatching() {
        val q600 = DeviceSoundProfiles.matchProfile("Samsung HW-Q600B Soundbar")
        assertNotNull(q600)
        assertEquals(DeviceSoundProfiles.SAMSUNG_Q_SERIES, q600)
    }

    @Test
    fun testSonyHomeTheaterMatching() {
        val s20r = DeviceSoundProfiles.matchProfile("Sony HT-S20R Soundbar")
        assertNotNull(s20r)
        assertEquals(DeviceSoundProfiles.SONY_HT_SERIES, s20r)

        val s500rf = DeviceSoundProfiles.matchProfile("Sony HT-S500RF Real 5.1ch")
        assertEquals(DeviceSoundProfiles.SONY_HT_SERIES, s500rf)
    }

    @Test
    fun testOnePlusBulletsZ2Matching() {
        val z2 = DeviceSoundProfiles.matchProfile("OnePlus Bullets Wireless Z2")
        assertNotNull(z2)
        assertEquals(DeviceSoundProfiles.ONEPLUS_BULLETS_Z2, z2)
        assertEquals(DeviceCategory.NECKBAND, z2?.category)
    }

    @Test
    fun testOnePlusNordSeriesMatching() {
        val nord4 = DeviceSoundProfiles.matchProfile("OnePlus Nord 4")
        assertNotNull(nord4)
        assertEquals(DeviceSoundProfiles.ONEPLUS_NORD_SERIES, nord4)
        assertEquals(DeviceCategory.EARBUDS, nord4?.category)

        val nordBuds2 = DeviceSoundProfiles.matchProfile("OnePlus Nord Buds 2r")
        assertNotNull(nordBuds2)
        assertEquals(DeviceSoundProfiles.ONEPLUS_NORD_SERIES, nordBuds2)

        val nordBuds3Pro = DeviceSoundProfiles.matchProfile("Nord Buds 3 Pro")
        assertNotNull(nordBuds3Pro)
        assertEquals(DeviceSoundProfiles.ONEPLUS_NORD_SERIES, nordBuds3Pro)
    }

    @Test
    fun testBoatRockerzMatching() {
        val rockerz255 = DeviceSoundProfiles.matchProfile("boAt Rockerz 255 Pro+")
        assertNotNull(rockerz255)
        assertEquals(DeviceSoundProfiles.BOAT_ROCKERZ, rockerz255)
    }

    @Test
    fun testRealmeBudsWirelessMatching() {
        val rmb = DeviceSoundProfiles.matchProfile("Realme Buds Wireless 3")
        assertNotNull(rmb)
        assertEquals(DeviceSoundProfiles.REALME_BUDS_WIRELESS, rmb)
    }

    @Test
    fun testSonyNeckbandMatching() {
        val wiC100 = DeviceSoundProfiles.matchProfile("Sony WI-C100")
        assertNotNull(wiC100)
        assertEquals(DeviceSoundProfiles.SONY_NECKBAND, wiC100)
    }

    @Test
    fun testGenericFallbacks() {
        val genericTheater = DeviceSoundProfiles.matchProfile("Generic Soundbar Cinema System")
        assertEquals(DeviceSoundProfiles.GENERIC_HOME_THEATER, genericTheater)

        val genericNeck = DeviceSoundProfiles.matchProfile("Generic In-Ear Neckband Earbuds")
        assertEquals(DeviceSoundProfiles.GENERIC_NECKBAND, genericNeck)
    }

    @Test
    fun testUnknownDeviceReturnsNull() {
        val unknown = DeviceSoundProfiles.matchProfile("ABC-XYZ-12345")
        assertNull(unknown)

        assertNull(DeviceSoundProfiles.matchProfile(null))
        assertNull(DeviceSoundProfiles.matchProfile(""))
    }
}
