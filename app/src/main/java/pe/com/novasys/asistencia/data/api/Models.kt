package pe.com.novasys.asistencia.data.api

import kotlinx.serialization.Serializable

// Wire models for the asistencia-novasys-v2 API. Only the fields the app reads
// are declared; the decoder ignores the rest so the backend can grow freely.

@Serializable
data class ProfileResponse(val employee: Profile)

@Serializable
data class Profile(
    val employeeId: String,
    val email: String,
    val fullName: String? = null,
    val firstName: String? = null,
    val area: String? = null,
    val position: String? = null,
    val role: String? = null,
    val avatarUrl: String? = null,
) {
    val isAdmin: Boolean get() = role == "ADMIN" || role == "SUPER_ADMIN"

    val displayFirstName: String
        get() = firstName?.takeIf { it.isNotBlank() }
            ?: fullName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            ?: email.substringBefore('@')
}

/** `GET /api/attendance/today` — mirrors `TodayStatus` in attendance.service.ts. */
@Serializable
data class TodayStatus(
    val date: String,
    val status: String,
    val firstInLocal: String? = null,
    val lastOutLocal: String? = null,
    val breakStartLocal: String? = null,
    val breakEndLocal: String? = null,
    val breakMinutes: Double = 0.0,
    val workedMinutes: Double = 0.0,
    val workedHHMM: String = "00:00",
    val plannedMinutes: Double = 0.0,
    val deltaMinutes: Double = 0.0,
    val deltaHHMM: String = "00:00",
    val lateMinutes: Double = 0.0,
    val hasOpenBreak: Boolean = false,
    val hasOpenShift: Boolean = false,
    val isHoliday: Boolean = false,
    val holidayName: String? = null,
) {
    val phase: ShiftPhase
        get() = when {
            hasOpenBreak -> ShiftPhase.OnBreak
            hasOpenShift -> ShiftPhase.Working
            firstInLocal != null -> ShiftPhase.Finished
            else -> ShiftPhase.NotStarted
        }
}

enum class ShiftPhase { NotStarted, Working, OnBreak, Finished }

/** The event types `recordEventSchema` accepts. */
enum class AttendanceEvent(val wire: String) {
    Start("START"),
    BreakStart("BREAK_START"),
    BreakEnd("BREAK_END"),
    End("END"),
}

@Serializable
data class RecordEventRequest(
    val eventType: String,
    val clientTime: String,
    val deviceId: String,
)

@Serializable
data class ApiErrorBody(val error: String? = null)
