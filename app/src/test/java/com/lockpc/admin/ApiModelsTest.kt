package com.lockpc.admin

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsTest {

    private val gson = Gson()

    @Test
    fun pcItem_deserialization_handlesAllFieldsAndNulls() {
        val json = """{"id":"pc1","name":"Desktop-1","ip":"192.168.1.10","status":"online","connected":true}"""
        val item = gson.fromJson(json, PcItem::class.java)

        assertEquals("pc1", item.id)
        assertEquals("Desktop-1", item.name)
        assertEquals("192.168.1.10", item.ip)
        assertEquals("online", item.status)
        assertEquals(true, item.connected)

        val nullJson = "{}"
        val nullItem = gson.fromJson(nullJson, PcItem::class.java)
        assertNull(nullItem.id)
        assertNull(nullItem.name)
        assertNull(nullItem.ip)
        assertNull(nullItem.status)
        assertNull(nullItem.connected)
    }

    @Test
    fun blockPeriod_serializationAndDeserialization() {
        val request = BlockPeriodRequest(
            from = "08:00",
            to = "17:00",
            days = listOf("Mon", "Wed", "Fri")
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("08:00"))
        assertTrue(json.contains("Mon"))

        val responseJson = """{"id":1,"from":"08:00","to":"17:00","days":["Mon","Wed","Fri"]}"""
        val period = gson.fromJson(responseJson, BlockPeriod::class.java)
        assertEquals(1, period.id)
        assertEquals("08:00", period.from)
        assertEquals("17:00", period.to)
        assertEquals(3, period.days?.size)
    }

    @Test
    fun loginTokenResponse_deserialization() {
        val json = """{"token":"eyJhbGciOiJIUzI1NiJ9..."}"""
        val response = gson.fromJson(json, LoginTokenResponse::class.java)
        assertEquals("eyJhbGciOiJIUzI1NiJ9...", response.token)
    }

    @Test
    fun deviceLocation_deserialization() {
        val json = """
            {
                "device_id": "dev123",
                "latitude": 37.7749,
                "longitude": -122.4194,
                "accuracy": 5.0,
                "timestamp": 1600000000,
                "updated_at": "2023-01-01 12:00:00",
                "device_model": "Pixel 7",
                "user_id": 42,
                "user_email": "test@example.com"
            }
        """.trimIndent()

        val location = gson.fromJson(json, DeviceLocation::class.java)
        assertEquals("dev123", location.device_id)
        assertEquals(37.7749, location.latitude, 0.0001)
        assertEquals(-122.4194, location.longitude, 0.0001)
        assertEquals(5.0f, location.accuracy ?: 0f, 0.001f)
        assertEquals(1600000000L, location.timestamp)
        assertEquals("2023-01-01 12:00:00", location.updated_at)
        assertEquals("Pixel 7", location.device_model)
        assertEquals(42, location.user_id)
        assertEquals("test@example.com", location.user_email)
    }

    @Test
    fun forceLogoutRequest_serialization() {
        val req = ForceLogoutRequest(deviceId = "target-dev-99")
        val json = gson.toJson(req)
        assertTrue(json.contains("target-dev-99"))
    }

    @Test
    fun pauseBlockRequest_serializationAndDeserialization() {
        val req = PauseBlockRequest(
            pcId = "pc-123",
            durationMinutes = 30L,
            untilEndOfSession = true,
            endTimestampMs = 1700000000000L
        )
        val json = gson.toJson(req)
        assertTrue(json.contains("pc-123"))
        assertTrue(json.contains("30"))
        assertTrue(json.contains("true"))

        val parsed = gson.fromJson(json, PauseBlockRequest::class.java)
        assertEquals("pc-123", parsed.pcId)
        assertEquals(30L, parsed.durationMinutes)
        assertEquals(true, parsed.untilEndOfSession)
        assertEquals(1700000000000L, parsed.endTimestampMs)
    }
}
