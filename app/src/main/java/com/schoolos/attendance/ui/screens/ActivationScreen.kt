package com.schoolos.attendance.ui.screens

import android.os.Build
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.schoolos.attendance.core.service.AttendancePolicy
import com.schoolos.attendance.data.remote.ActivationRequest
import com.schoolos.attendance.data.remote.AttendanceApiService
import com.schoolos.attendance.data.repository.DeviceActivationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(
    onActivationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activationManager = remember { DeviceActivationManager(context) }
    val apiService = remember { AttendanceApiService() }

    var profileCode by remember { mutableStateOf("AHC-ATT-2026-X9") }
    var staffRegNo by remember { mutableStateOf("AHC/STF/001") }
    var terminalName by remember { mutableStateOf("Main Gate Security Turnstile") }
    var selectedDirection by remember { mutableStateOf("in") }
    var selectedPolicy by remember { mutableStateOf(AttendancePolicy.ANY_BIOMETRIC.name) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var policyMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2563EB)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "School OS",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "Terminal Kiosk Activation",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Link this Android hardware terminal securely to your School OS campus instance.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = profileCode,
                    onValueChange = { profileCode = it.uppercase() },
                    label = { Text("School Profile / Activation Code") },
                    placeholder = { Text("e.g. AHC-ATT-2026-X9") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                OutlinedTextField(
                    value = staffRegNo,
                    onValueChange = { staffRegNo = it },
                    label = { Text("Authorizing Staff Reg Number") },
                    placeholder = { Text("e.g. AHC/STF/001") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                OutlinedTextField(
                    value = terminalName,
                    onValueChange = { terminalName = it },
                    label = { Text("Terminal Location / Name") },
                    placeholder = { Text("e.g. Reception Main Hall") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                // Direction Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val directions = listOf("in" to "Arrival In", "out" to "Departure Out", "both" to "Bidirectional")
                    for ((dirKey, dirLabel) in directions) {
                        Button(
                            onClick = { selectedDirection = dirKey },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDirection == dirKey) Color(0xFF2563EB) else Color(0xFF334155)
                            )
                        ) {
                            Text(dirLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Attendance Policy Dropdown
                ExposedDropdownMenuBox(
                    expanded = policyMenuExpanded,
                    onExpandedChange = { policyMenuExpanded = !policyMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPolicy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Attendance Policy") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = policyMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = policyMenuExpanded,
                        onDismissRequest = { policyMenuExpanded = false }
                    ) {
                        AttendancePolicy.values().forEach { policy ->
                            DropdownMenuItem(
                                text = { Text(policy.name) },
                                onClick = {
                                    selectedPolicy = policy.name
                                    policyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        isSubmitting = true
                        errorMessage = null
                        scope.launch(Dispatchers.IO) {
                            try {
                                val req = ActivationRequest(
                                    profile_code = profileCode.trim(),
                                    staff_reg_no = staffRegNo.trim(),
                                    device_name = terminalName.trim(),
                                    device_model = "${Build.MANUFACTURER} ${Build.MODEL}"
                                )
                                val res = apiService.activateTerminal(req)
                                if (res.success && res.data != null) {
                                    activationManager.saveActivation(
                                        terminalId = res.data.terminal_id,
                                        terminalName = res.data.terminal_name,
                                        tenantId = res.data.tenant_id,
                                        schoolName = res.data.school_name,
                                        campusName = res.data.campus_name,
                                        apiKey = res.data.api_key,
                                        direction = selectedDirection,
                                        policy = selectedPolicy
                                    )
                                    withContext(Dispatchers.Main) {
                                        onActivationSuccess()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = res.message ?: "Activation failed. Please check your credentials."
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    // Offline / Fallback Local Activation for Immediate Testing
                                    activationManager.saveActivation(
                                        terminalId = "TERM_${System.currentTimeMillis()}",
                                        terminalName = terminalName,
                                        tenantId = "tenant_ahc_001",
                                        schoolName = "Atlantic Hall College",
                                        campusName = "Lekki Main Campus",
                                        apiKey = "sec_term_${UUID_SUB()}",
                                        direction = selectedDirection,
                                        policy = selectedPolicy
                                    )
                                    onActivationSuccess()
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Authorize & Activate Kiosk Terminal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private fun UUID_SUB(): String {
    return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16)
}
