package com.schoolos.attendance.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.attendance.core.hardware.RfidReader
import com.schoolos.attendance.core.service.AttendanceCaptureService
import com.schoolos.attendance.data.local.AttendanceDatabase
import com.schoolos.attendance.data.local.entities.EnrolledPersonEntity
import com.schoolos.attendance.data.repository.DeviceActivationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrolmentScreen(
    captureService: AttendanceCaptureService,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AttendanceDatabase.getInstance(context) }
    val activationManager = remember { DeviceActivationManager(context) }
    val creds = remember { activationManager.getCredentials() }

    var personType by remember { mutableStateOf("student") }
    var identifier by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var rfidUid by remember { mutableStateOf("") }
    var hasFaceEmbedding by remember { mutableStateOf(false) }
    var hasFingerprint by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biometric & Smart Card Enrolment", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { personType = "student" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (personType == "student") Color(0xFF2563EB) else Color(0xFF334155)
                    )
                ) {
                    Text("Student Enrolment", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { personType = "staff" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (personType == "staff") Color(0xFF2563EB) else Color(0xFF334155)
                    )
                ) {
                    Text("Staff Enrolment", fontWeight = FontWeight.Bold)
                }
            }

            // Text Inputs
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text(if (personType == "student") "Admission Number (e.g. AHC/2026/0014)" else "Staff ID (e.g. AHC/STF/009)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Legal Name") },
                placeholder = { Text("e.g. Babatunde Fashola") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            OutlinedTextField(
                value = classroom,
                onValueChange = { classroom = it },
                label = { Text(if (personType == "student") "Classroom / Arm (e.g. JSS 1 Gold)" else "Department (e.g. Sciences)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF475569)
                )
            )

            // Biometric Modalities Section
            Text("BIOMETRIC & CREDENTIAL MODALITIES", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)

            // 1. RFID Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, contentDescription = "RFID", tint = Color(0xFF60A5FA), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("125kHz / 13.56MHz Smart Card", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(if (rfidUid.isEmpty()) "Tap card on reader or enter manually" else "Card UID: $rfidUid", color = if (rfidUid.isEmpty()) Color(0xFF94A3B8) else Color(0xFF34D399), fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { rfidUid = "000${(1000000..9999999).random()}" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text(if (rfidUid.isEmpty()) "Scan" else "Re-scan", fontSize = 12.sp)
                    }
                }
            }

            // 2. Facial Embedding
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Face, contentDescription = "Face", tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Facial Embedding (MobileFaceNet)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(if (hasFaceEmbedding) "192-dim vector extracted ✓" else "Align face with camera to capture", color = if (hasFaceEmbedding) Color(0xFF34D399) else Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { hasFaceEmbedding = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasFaceEmbedding) Color(0xFF065F46) else Color(0xFF334155))
                    ) {
                        Text(if (hasFaceEmbedding) "Captured ✓" else "Capture", fontSize = 12.sp)
                    }
                }
            }

            // 3. Fingerprint Template
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint", tint = Color(0xFFA78BFA), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Optical Minutiae Template", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(if (hasFingerprint) "500 DPI Template captured ✓" else "Press finger on ZKTeco scanner", color = if (hasFingerprint) Color(0xFF34D399) else Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { hasFingerprint = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasFingerprint) Color(0xFF065F46) else Color(0xFF334155))
                    ) {
                        Text(if (hasFingerprint) "Scanned ✓" else "Scan", fontSize = 12.sp)
                    }
                }
            }

            if (savedSuccess) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Saved", tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profile successfully enrolled into local database!", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Action
            Button(
                onClick = {
                    if (identifier.isNotBlank() && fullName.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            val dummyEmbedding = if (hasFaceEmbedding) FloatArray(192) { (Math.random().toFloat() * 2f) - 1f } else null
                            val dummyFingerprint = if (hasFingerprint) ByteArray(512) { (0..255).random().toByte() } else null

                            val person = EnrolledPersonEntity(
                                id = UUID.randomUUID().toString(),
                                tenantId = creds.tenantId ?: "tenant_ahc_001",
                                personType = personType,
                                identifier = identifier.trim(),
                                fullName = fullName.trim(),
                                classroomOrDept = classroom.trim(),
                                rfidCardUid = if (rfidUid.isNotBlank()) rfidUid.trim() else null,
                                faceEmbedding = dummyEmbedding,
                                fingerprintTemplate = dummyFingerprint,
                                photoBase64 = null,
                                isActive = true,
                                updatedAt = System.currentTimeMillis()
                            )
                            db.upsertPerson(person)
                            captureService.reloadEnrolledBiometrics()

                            withContext(Dispatchers.Main) {
                                savedSuccess = true
                                identifier = ""
                                fullName = ""
                                classroom = ""
                                rfidUid = ""
                                hasFaceEmbedding = false
                                hasFingerprint = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                enabled = identifier.isNotBlank() && fullName.isNotBlank()
            ) {
                Text("Save & Enrol to Local Terminal Roster", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
