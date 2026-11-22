package com.schoolos.attendance.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.schoolos.attendance.core.hardware.CameraSource
import com.schoolos.attendance.core.service.AttendanceCaptureService
import com.schoolos.attendance.core.service.CaptureResult
import com.schoolos.attendance.data.local.AttendanceDatabase
import com.schoolos.attendance.data.repository.DeviceActivationManager
import kotlinx.coroutines.delay

@Composable
fun KioskAttendanceScreen(
    captureService: AttendanceCaptureService,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToEnrolment: () -> Unit,
    onNavigateToActivation: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = remember { AttendanceDatabase.getInstance(context) }
    val activationManager = remember { DeviceActivationManager(context) }
    val creds = remember { activationManager.getCredentials() }

    var lastResult by remember { mutableStateOf<CaptureResult>(CaptureResult.Idle()) }
    var todayCount by remember { mutableIntStateOf(db.getTodayLogsCount()) }
    var pendingSyncCount by remember { mutableIntStateOf(db.getPendingSyncCount()) }
    var adminPinDialogOpen by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var destinationScreen by remember { mutableStateOf<String?>(null) }

    // Connect capture service callbacks
    DisposableEffect(captureService) {
        captureService.onCaptureResult = { result ->
            lastResult = result
            if (result is CaptureResult.Success) {
                todayCount = db.getTodayLogsCount()
                pendingSyncCount = db.getPendingSyncCount()
            }
        }
        onDispose {
            captureService.onCaptureResult = null
        }
    }

    // Auto-dismiss verified popup after 3.5 seconds
    LaunchedEffect(lastResult) {
        if (lastResult is CaptureResult.Success || lastResult is CaptureResult.LivenessFailed || lastResult is CaptureResult.UnknownPerson) {
            delay(3500)
            lastResult = CaptureResult.Idle()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Camera Viewfinder Canvas Placeholder / Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Simulated Viewfinder Box with High-Tech Corner Brackets
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(
                        width = 2.dp,
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF1E293B))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Facial Recognition",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "ALIGN FACE IN RETICLE",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Or tap RFID Card below",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Top Navigation & Institutional Branding Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color(0xFF1E293B).copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "AH",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = creds.schoolName ?: "Atlantic Hall College",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${creds.terminalName ?: "Main Gate Kiosk"} • ${creds.direction.uppercase()}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF065F46)
                    ) {
                        Text(
                            text = "● ONLINE",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Admin Settings Unlock Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF334155),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                destinationScreen = "diagnostics"
                                adminPinDialogOpen = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Admin Unlock",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Animated Result Toast / Card Overlay
        AnimatedVisibility(
            visible = lastResult !is CaptureResult.Idle,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            when (val res = lastResult) {
                is CaptureResult.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(50.dp),
                                shape = CircleShape,
                                color = Color(0xFF10B981)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = res.person.fullName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${res.person.classroomOrDept ?: "Student"} • ${res.person.identifier}",
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "✓ ${res.direction.uppercase()} RECORDED VIA ${res.mode.uppercase()} (${(res.confidence * 100).toInt()}%)",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
                is CaptureResult.DualFactorPendingFace -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "RFID Card",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Card: ${res.person.fullName}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = res.message,
                                    color = Color(0xFFBFDBFE),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                is CaptureResult.LivenessFailed -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Liveness Warning",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = res.reason,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is CaptureResult.UnknownPerson -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Unknown Card",
                                tint = Color(0xFFFDBA74),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Unregistered ${res.modality}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = res.identifier ?: "Please contact school administrator to enrol.",
                                    color = Color(0xFFFED7AA),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                is CaptureResult.Idle -> {}
            }
        }

        // Bottom Kiosk Operations & Statistics Footer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color(0xFF1E293B)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "TODAY'S ATTENDANCE",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$todayCount Verified",
                            color = Color(0xFFF8FAFC),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column {
                        Text(
                            text = "OFFLINE SYNC QUEUE",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (pendingSyncCount == 0) "All Synced ✓" else "$pendingSyncCount Pending",
                            color = if (pendingSyncCount == 0) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF334155),
                        modifier = Modifier.clickable {
                            destinationScreen = "enrolment"
                            adminPinDialogOpen = true
                        }
                    ) {
                        Text(
                            text = "+ Enrol",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Admin Security PIN Dialog
        if (adminPinDialogOpen) {
            AlertDialog(
                onDismissRequest = {
                    adminPinDialogOpen = false
                    enteredPin = ""
                    pinError = false
                },
                title = {
                    Text("Administrator Authentication", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Enter the 4-digit Master Kiosk PIN to access diagnostics and configuration."
                        )
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 6) {
                                    enteredPin = it
                                    pinError = false
                                }
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            label = { Text("Admin PIN") },
                            isError = pinError,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError) {
                            Text("Incorrect PIN. Please try again.", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val correctPin = activationManager.getKioskAdminPin()
                            if (enteredPin == correctPin) {
                                adminPinDialogOpen = false
                                enteredPin = ""
                                if (destinationScreen == "diagnostics") {
                                    onNavigateToDiagnostics()
                                } else if (destinationScreen == "enrolment") {
                                    onNavigateToEnrolment()
                                }
                            } else {
                                pinError = true
                            }
                        }
                    ) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            adminPinDialogOpen = false
                            enteredPin = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}
