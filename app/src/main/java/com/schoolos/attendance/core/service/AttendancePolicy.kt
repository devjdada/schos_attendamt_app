package com.schoolos.attendance.core.service

enum class AttendancePolicy {
    FACE_ONLY,
    FINGERPRINT_ONLY,
    RFID_ONLY,
    RFID_PLUS_FACE,
    ANY_BIOMETRIC
}

enum class AttendanceDirection {
    ARRIVAL,
    DEPARTURE,
    AUTO_SCHEDULED // Morning = Arrival, Afternoon = Departure
}
