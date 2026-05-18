package com.nutrition.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutrition.scanner.data.NutritionAnalyzer
import com.nutrition.scanner.data.NutritionInfo
import com.nutrition.scanner.data.ScanResult
import kotlinx.coroutines.launch
import java.io.File

// ==================== API Key Manager ====================

object ApiKeyManager {
    private const val PREFS_NAME = "nutrio_prefs"
    private const val KEY_GEMINI = "gemini_api_key"

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI, "") ?: ""
    }

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GEMINI, key)
            .apply()
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_GEMINI)
            .apply()
    }
}

// ==================== Main App ====================

@Composable
fun NutritionApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scanHistory = remember { mutableStateListOf<ScanResult>() }
    var apiKey by remember { mutableStateOf(ApiKeyManager.getApiKey(context)) }

    NavHost(
        navController = navController,
        startDestination = if (apiKey.isBlank()) "onboarding" else "home"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onKeySaved = { key ->
                    ApiKeyManager.saveApiKey(context, key)
                    apiKey = key
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                navController = navController,
                scanHistory = scanHistory,
                apiKey = apiKey,
                onChangeKey = {
                    navController.navigate("onboarding")
                }
            )
        }
        composable("camera") {
            CameraScreen(
                navController = navController,
                apiKey = apiKey,
                onResult = { result ->
                    scanHistory.add(0, result)
                }
            )
        }
        composable("gallery") {
            GalleryScreen(
                navController = navController,
                apiKey = apiKey,
                onResult = { result ->
                    scanHistory.add(0, result)
                }
            )
        }
        composable("history") {
            HistoryScreen(
                navController = navController,
                scanHistory = scanHistory
            )
        }
        composable("result/{index}") { backStackEntry ->
            val index = backStackEntry.arguments?.getString("index")?.toIntOrNull() ?: 0
            if (index < scanHistory.size) {
                ResultScreen(
                    navController = navController,
                    result = scanHistory[index]
                )
            }
        }
    }
}

// ==================== Onboarding Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onKeySaved: (String) -> Unit) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "ยินดีต้อนรับสู่ Nutrio! 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "สแกนอาหาร ดูสารอาหารทันที",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // API Key Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ใส่ Gemini API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Nutrio ใช้ Google Gemini วิเคราะห์รูปอาหาร คุณต้องมี API key ฟรีจาก Google AI Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Key Input
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = {
                apiKeyInput = it
                showError = false
            },
            label = { Text("Gemini API Key") },
            placeholder = { Text("AIzaSy...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = showError,
            supportingText = if (showError) {
                { Text("กรุณาใส่ API key ที่ถูกต้อง", color = MaterialTheme.colorScheme.error) }
            } else null,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                if (apiKeyInput.isBlank() || !apiKeyInput.startsWith("AIza")) {
                    showError = true
                } else {
                    onKeySaved(apiKeyInput.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("เริ่มใช้งาน", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Help Section
        TextButton(onClick = { showHelp = !showHelp }) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("จะได้ API key ยังไง?")
        }

        if (showHelp) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "วิธีรับ API key ฟรี:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. ไปที่ aistudio.google.com/apikey", style = MaterialTheme.typography.bodySmall)
                    Text("2. กด \"Create API Key\"", style = MaterialTheme.typography.bodySmall)
                    Text("3. Copy key มาใส่ตรงนี้", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "✅ ฟรี! ไม่ต้องใส่บัตรเครดิต",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy note
        Text(
            "🔒 API key ของคุณถูกเก็บไว้ในเครื่องเท่านั้น\nไม่มีการส่งข้อมูลไปที่ server อื่น",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

// ==================== Home Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    scanHistory: List<ScanResult>,
    apiKey: String,
    onChangeKey: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Nutrio",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "สแกนอาหาร ดูสารอาหารทันที",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Camera Button
        Button(
            onClick = { navController.navigate("camera") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("ถ่ายรูปอาหาร", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gallery Button
        OutlinedButton(
            onClick = { navController.navigate("gallery") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("เลือกจากแกลเลอรี่", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History Button
        OutlinedButton(
            onClick = { navController.navigate("history") },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("ประวัติ (${scanHistory.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings / Change API Key
        TextButton(onClick = onChangeKey) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("เปลี่ยน API Key")
        }

        Spacer(modifier = Modifier.weight(1f))

        // Quick Stats
        if (scanHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "สรุปวันนี้",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val todayCalories = scanHistory
                        .filter { System.currentTimeMillis() - it.timestamp < 86400000 }
                        .sumOf { it.nutrition.calories }
                    Text(
                        "แคลอรี่รวม: $todayCalories kcal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// ==================== Camera Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    apiKey: String,
    onResult: (ScanResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "ต้องการสิทธิ์กล้อง", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            val bitmap = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(tempUri!!)
            )
            capturedBitmap = bitmap
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-launch camera (only if permission granted)
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val tempFile = File(context.cacheDir, "temp_photo.jpg")
            tempUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            takePictureLauncher.launch(tempUri!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap!!.asImageBitmap(),
                contentDescription = "Captured food",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("กำลังวิเคราะห์...", color = Color.White)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val tempFile = File(context.cacheDir, "temp_photo.jpg")
                                tempUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    tempFile
                                )
                                takePictureLauncher.launch(tempUri!!)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ถ่ายใหม่")
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val analyzer = NutritionAnalyzer(apiKey)
                                val result = analyzer.analyzeFood(capturedBitmap!!)
                                isLoading = false
                                result.onSuccess { nutrition ->
                                    val scanResult = ScanResult(
                                        imageUri = tempUri.toString(),
                                        nutrition = nutrition
                                    )
                                    onResult(scanResult)
                                    navController.popBackStack()
                                    Toast.makeText(context, "พบ: ${nutrition.foodName}", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "ผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("วิเคราะห์")
                    }
                }
            }
        } else {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("กำลังเปิดกล้อง...", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

// ==================== Gallery Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    apiKey: String,
    onResult: (ScanResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
            selectedBitmap = bitmap
        }
    }

    LaunchedEffect(Unit) {
        galleryLauncher.launch("image/*")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selectedBitmap != null) {
            Image(
                bitmap = selectedBitmap!!.asImageBitmap(),
                contentDescription = "Selected food",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("กำลังวิเคราะห์...")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("เลือกใหม่")
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val analyzer = NutritionAnalyzer(apiKey)
                                val result = analyzer.analyzeFood(selectedBitmap!!)
                                isLoading = false
                                result.onSuccess { nutrition ->
                                    val scanResult = ScanResult(nutrition = nutrition)
                                    onResult(scanResult)
                                    navController.popBackStack()
                                }.onFailure { e ->
                                    Toast.makeText(context, "ผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("วิเคราะห์")
                    }
                }
            }
        } else {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("กำลังเปิดแกลเลอรี่...", style = MaterialTheme.typography.titleLarge)
        }
    }
}

// ==================== Result Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController,
    result: ScanResult
) {
    val nutrition = result.nutrition

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Text(
            nutrition.foodName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "ปริมาณ: ${nutrition.servingSize}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Calories Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${nutrition.calories}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("แคลอรี่ (kcal)", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Macros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MacroCard(
                modifier = Modifier.weight(1f),
                title = "โปรตีน",
                value = "${nutrition.protein}g",
                color = Color(0xFF4CAF50),
                icon = Icons.Default.FitnessCenter
            )
            MacroCard(
                modifier = Modifier.weight(1f),
                title = "คาร์บ",
                value = "${nutrition.carbs}g",
                color = Color(0xFF2196F3),
                icon = Icons.Default.Grain
            )
            MacroCard(
                modifier = Modifier.weight(1f),
                title = "ไขมัน",
                value = "${nutrition.fat}g",
                color = Color(0xFFFF9800),
                icon = Icons.Default.Opacity
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed nutrition
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "สารอาหารเพิ่มเติม",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                NutritionRow("ใยอาหาร", "${nutrition.fiber}g")
                NutritionRow("น้ำตาล", "${nutrition.sugar}g")
                NutritionRow("โซเดียม", "${nutrition.sodium}mg")
            }
        }
    }
}

@Composable
fun MacroCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(title, fontSize = 12.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

// ==================== History Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    scanHistory: List<ScanResult>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("ประวัติการสแกน") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (scanHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ยังไม่มีประวัติ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                scanHistory.forEachIndexed { index, result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { navController.navigate("result/$index") }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    result.nutrition.foodName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${result.nutrition.calories} kcal • ${result.nutrition.servingSize}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
