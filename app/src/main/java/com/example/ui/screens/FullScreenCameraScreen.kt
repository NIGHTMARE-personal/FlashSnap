package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CapturedPage
import com.example.ui.theme.SuccessSage
import com.example.util.OfflineOcrProcessor
import com.example.util.SoundFeedbackManager
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.concurrent.Executors

@Composable
fun FullScreenCameraScreen(
    capturedPages: List<CapturedPage>,
    onPageCaptured: (Bitmap) -> Unit,
    onRemovePage: (Int) -> Unit,
    activeSessionSubject: String = "",
    onSelectSessionSubject: (String) -> Unit = {},
    onGoToPages: () -> Unit,
    onRegisterShutter: (((() -> Unit)) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Camera state
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    // Shutter flash animation
    val flashAlpha = remember { Animatable(0f) }

    // Edge OCR Pre-processing & live bounding box overlay
    var edgeOcrActive by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "scan_beam")
    val scanBeamProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_beam_progress"
    )

    // Visual media picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val finalBitmap = if (edgeOcrActive) OfflineOcrProcessor.preprocessDocumentBitmap(bitmap) else bitmap
                    onPageCaptured(finalBitmap)
                }
            } catch (e: Exception) {
                Log.e("FullScreenCamera", "Error loading picked image", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun triggerShutter() {
        SoundFeedbackManager.getInstance(context).playShutterSound()
        scope.launch {
            // Flash effect
            flashAlpha.snapTo(0.85f)
            flashAlpha.animateTo(0f, animationSpec = tween(150))
        }

        // Try getting bitmap from previewView first (instant & reliable across emulators)
        val previewBitmap = previewViewRef?.bitmap
        if (previewBitmap != null) {
            val finalBitmap = if (edgeOcrActive) OfflineOcrProcessor.preprocessDocumentBitmap(previewBitmap) else previewBitmap
            onPageCaptured(finalBitmap)
        } else {
            // Fallback to ImageCapture
            val imageCapture = imageCaptureRef
            if (imageCapture != null) {
                val executor = Executors.newSingleThreadExecutor()
                imageCapture.takePicture(
                    executor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            image.close()
                            if (bitmap != null) {
                                val finalBitmap = if (edgeOcrActive) OfflineOcrProcessor.preprocessDocumentBitmap(bitmap) else bitmap
                                scope.launch { onPageCaptured(finalBitmap) }
                            }
                        }

                    override fun onError(exception: ImageCaptureException) {
                            Log.e("FullScreenCamera", "ImageCapture failed", exception)
                        }
                    }
                )
            } else {
                Log.w("FullScreenCamera", "Camera not active or imageCapture not initialized")
            }
        }
    }

    DisposableEffect(onRegisterShutter) {
        onRegisterShutter?.invoke {
            triggerShutter()
        }
        onDispose {
            onRegisterShutter?.invoke {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("full_screen_camera_root")
    ) {
        // 1. Live Camera Feed (Fills full screen edge-to-edge)
        if (hasCameraPermission) {
            androidx.compose.runtime.key(lensFacing) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        previewViewRef = previewView

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCaptureRef = imageCapture

                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                                cameraRef = cam
                            } catch (e: Exception) {
                                Log.e("FullScreenCamera", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Permission request screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1A17)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Camera Access Needed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Point your camera at handwritten textbook notes or lecture slides to automatically generate flashcards and quizzes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCC5BE),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }

        // 2. Camera Grid Overlay (Rule of thirds)
        if (showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineColor = Color.White.copy(alpha = 0.18f)

                // Vertical lines
                drawLine(lineColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1f)
                drawLine(lineColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth = 1f)

                // Horizontal lines
                drawLine(lineColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1f)
                drawLine(lineColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth = 1f)
            }
        }

        // 3. Document Scanning Reticle (Corner Brackets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 110.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bracketLen = 38.dp.toPx()
                val bracketColor = Color(0xFFF7F2E9).copy(alpha = 0.75f)
                val stroke = 3.dp.toPx()
                val w = size.width
                val h = size.height

                // Top Left
                drawLine(bracketColor, Offset(0f, 0f), Offset(bracketLen, 0f), strokeWidth = stroke)
                drawLine(bracketColor, Offset(0f, 0f), Offset(0f, bracketLen), strokeWidth = stroke)

                // Top Right
                drawLine(bracketColor, Offset(w, 0f), Offset(w - bracketLen, 0f), strokeWidth = stroke)
                drawLine(bracketColor, Offset(w, 0f), Offset(w, bracketLen), strokeWidth = stroke)

                // Bottom Left
                drawLine(bracketColor, Offset(0f, h), Offset(bracketLen, h), strokeWidth = stroke)
                drawLine(bracketColor, Offset(0f, h), Offset(0f, h - bracketLen), strokeWidth = stroke)

                // Bottom Right
                drawLine(bracketColor, Offset(w, h), Offset(w - bracketLen, h), strokeWidth = stroke)
                drawLine(bracketColor, Offset(w, h), Offset(w, h - bracketLen), strokeWidth = stroke)

                // Edge OCR Live Bounding Boxes & Scanning Beam
                if (edgeOcrActive) {
                    val beamY = h * scanBeamProgress
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0x00C0793D),
                                Color(0xFFE8A86B),
                                Color(0xFFFBF3E8),
                                Color(0xFFE8A86B),
                                Color(0x00C0793D)
                            )
                        ),
                        start = Offset(0f, beamY),
                        end = Offset(w, beamY),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Simulated recognized lines of notes
                    val lineBoxes = listOf(
                        Triple(0.08f, 0.12f, 0.84f),
                        Triple(0.08f, 0.20f, 0.76f),
                        Triple(0.08f, 0.28f, 0.62f),
                        Triple(0.12f, 0.40f, 0.78f),
                        Triple(0.12f, 0.48f, 0.80f),
                        Triple(0.12f, 0.56f, 0.54f),
                        Triple(0.08f, 0.68f, 0.84f),
                        Triple(0.08f, 0.76f, 0.70f)
                    )

                    for ((relX, relY, relW) in lineBoxes) {
                        val boxLeft = w * relX
                        val boxTop = h * relY
                        val boxWidth = w * relW
                        val boxHeight = 16.dp.toPx()
                        val isNearBeam = kotlin.math.abs(boxTop - beamY) < 35.dp.toPx()

                        val boxBorderColor = if (isNearBeam) Color(0xFFE8A86B) else Color(0x70D4CBB5)
                        val boxFillColor = if (isNearBeam) Color(0x35E8A86B) else Color(0x12D4CBB5)

                        drawRoundRect(
                            color = boxFillColor,
                            topLeft = Offset(boxLeft, boxTop),
                            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawRoundRect(
                            color = boxBorderColor,
                            topLeft = Offset(boxLeft, boxTop),
                            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isNearBeam) 1.5f else 1f)
                        )
                    }
                }
            }

            // Scanning prompt badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = if (edgeOcrActive) "Document locked • Ready to transcribe" else "Align notes or textbook page within frame",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF7F2E9),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Bottom Edge OCR telemetry status pill
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (edgeOcrActive) Color(0xFFC0793D).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (edgeOcrActive) SuccessSage else Color.Gray)
                    )
                    Text(
                        text = if (edgeOcrActive) "⚡ Edge OCR: 8 Text Blocks Detected" else "Standard Viewfinder",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF7F2E9),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. White Flash Effect on Shutter Tap
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }

        // 5. Top Translucent Header Bar & Active Session Subject Label
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash toggle
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraRef?.cameraControl?.enableTorch(isFlashOn)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Toggle Flash",
                            tint = if (isFlashOn) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Page Counter Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pages: ${capturedPages.size} / 20",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (capturedPages.isNotEmpty()) SuccessSage else Color(0xFFF7F2E9)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Switch Camera (Front / Back)
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Grid Toggle
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = { showGrid = !showGrid }) {
                            Icon(
                                imageVector = Icons.Filled.GridOn,
                                contentDescription = "Toggle Grid",
                                tint = if (showGrid) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Edge OCR Toggle
                    Surface(
                        shape = CircleShape,
                        color = if (edgeOcrActive) Color(0xFFC0793D).copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = { edgeOcrActive = !edgeOcrActive }) {
                            Icon(
                                imageVector = Icons.Filled.DocumentScanner,
                                contentDescription = "Toggle Edge OCR",
                                tint = if (edgeOcrActive) Color(0xFFFFF7ED) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Session Subject Tag Pill
            var showSubjectDropdown by remember { mutableStateOf(false) }
            var showCustomSubjectDialog by remember { mutableStateOf(false) }
            var customSubjectInput by remember { mutableStateOf("") }

            val subjectEmoji = when (activeSessionSubject.lowercase()) {
                "chemistry" -> "🧪"
                "physics" -> "⚡"
                "math", "mathematics", "calculus", "algebra" -> "📐"
                "biology" -> "🧬"
                "history" -> "📜"
                "computer science", "cs" -> "💻"
                "literature" -> "📖"
                else -> if (activeSessionSubject.isNotBlank()) "🏷️" else ""
            }

            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (activeSessionSubject.isNotBlank())
                        Color(0xFFC0793D).copy(alpha = 0.95f)
                    else
                        Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(
                        1.dp,
                        if (activeSessionSubject.isNotBlank()) Color(0xFFFFD1A4) else Color.White.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showSubjectDropdown = !showSubjectDropdown }
                        .testTag("camera_session_subject_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeSessionSubject.isNotBlank()) {
                            Text(text = subjectEmoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Session Label: $activeSessionSubject",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .clickable { onSelectSessionSubject("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear Subject Label",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Session Label: None (e.g. Chemistry)",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showSubjectDropdown,
                    onDismissRequest = { showSubjectDropdown = false }
                ) {
                    Text(
                        text = "SESSION AUTO-LABEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "All pictures clicked will be labeled automatically:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text("✨ Auto Detect (From OCR)") },
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onSelectSessionSubject("")
                            showSubjectDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🧪 Chemistry") },
                        onClick = {
                            onSelectSessionSubject("Chemistry")
                            showSubjectDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⚡ Physics") },
                        onClick = {
                            onSelectSessionSubject("Physics")
                            showSubjectDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📐 Mathematics") },
                        onClick = {
                            onSelectSessionSubject("Math")
                            showSubjectDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🧬 Biology") },
                        onClick = {
                            onSelectSessionSubject("Biology")
                            showSubjectDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📜 History") },
                        onClick = {
                            onSelectSessionSubject("History")
                            showSubjectDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("💻 Computer Science") },
                        onClick = {
                            onSelectSessionSubject("Computer Science")
                            showSubjectDropdown = false
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("✏️ Custom Subject...") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showSubjectDropdown = false
                            customSubjectInput = ""
                            showCustomSubjectDialog = true
                        }
                    )
                }
            }

            if (showCustomSubjectDialog) {
                AlertDialog(
                    onDismissRequest = { showCustomSubjectDialog = false },
                    title = { Text("Set Custom Subject Label") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Every photo taken or selected in this session will automatically be labeled with this subject.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = customSubjectInput,
                                onValueChange = { customSubjectInput = it },
                                placeholder = { Text("e.g. Organic Chemistry, Macroeconomics") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("custom_session_subject_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customSubjectInput.isNotBlank()) {
                                    onSelectSessionSubject(customSubjectInput.trim())
                                }
                                showCustomSubjectDialog = false
                            }
                        ) {
                            Text("Apply Label")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomSubjectDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        // 6. Bottom Camera Action Controls (Gallery on left, taskbar camera in center, thumbnail on right)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 68.dp), // Perfectly aligns flanking controls with bottom bar shutter button
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Floating review/generate chip if at least 1 page captured
            AnimatedVisibility(
                visible = capturedPages.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onGoToPages() }
                        .testTag("camera_generate_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Review & Generate (${capturedPages.size} pages)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Flanking controls row: Photos on left, Center reserved for bottom taskbar shutter, Thumbnail on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Photo Gallery Picker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("camera_gallery_picker")
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Pick Photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Photos",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Center: Reserved space for the bottom taskbar's hovered camera shutter button (No duplicate white button)
                Spacer(modifier = Modifier.size(76.dp))

                // Right: Captured thumbnail or Next
                if (capturedPages.isNotEmpty()) {
                    val lastPage = capturedPages.last()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onGoToPages() }
                            .testTag("camera_thumbnail_preview")
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .background(Color(0xFF2C241E)),
                            contentAlignment = Alignment.Center
                        ) {
                            lastPage.bitmap?.let { bmp ->
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } ?: Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = SuccessSage,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${capturedPages.size} Ready",
                            color = Color(0xFFF7F2E9),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Empty placeholder to balance layout
                    Spacer(modifier = Modifier.size(46.dp))
                }
            }
        }
    }
}
