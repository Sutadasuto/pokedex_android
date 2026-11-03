package com.example.pokedex

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.content.ContextCompat
import com.example.pokedex.ui.theme.PokedexTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokedexTheme(dynamicColor = false) {
                PokedexScreen()
            }
        }
    }
}

private data class PokemonInfo(
    val id: Int,
    val name: String,
    val language: String,
    val genus: String?,
    val description: String,
    val descriptionLanguage: String,
    val types: List<String>,
    val height: Int,
    val weight: Int,
    val abilities: List<String>,
    val evolutions: List<EvolutionEntry>,
    val artwork: Bitmap?
)

private data class EvolutionEntry(
    val name: String,
    val artwork: Bitmap?
)

private data class Prediction(
    val id: Int,
    val probability: Float,
    val name: String = ""
)

private enum class AppLanguage(
    val apiCode: String,
    val speechTag: String,
    val label: String,
    val pokemon: String,
    val profile: String,
    val evolution: String,
    val type: String,
    val height: String,
    val weight: String,
    val abilities: String,
    val loading: String,
    val topPredictions: String,
    val confidence: String,
    val previous: String,
    val next: String,
    val ready: String,
    val scanning: String,
    val aim: String,
    val cameraStarting: String,
    val permissionRequired: String,
    val namesUnavailable: String,
    val helpTitle: String,
    val helpInstructions: String,
    val close: String
) {
    EN("en", "en-US", "EN", "POKÉMON", "POKÉMON PROFILE", "EVOLUTION LINE", "TYPE", "HEIGHT", "WEIGHT", "ABILITIES", "LOADING POKÉDEX DATA...", "TOP PREDICTIONS", "CONFIDENCE", "PREVIOUS POKÉMON", "NEXT POKÉMON", "PRESS GREEN TO SCAN", "ANALYZING IMAGE...", "AIM AT A POKÉMON", "CAMERA IS STARTING...", "CAMERA PERMISSION REQUIRED", "Names unavailable", "Pokédex Help", "Green button: press once to start the camera, then press again to capture and identify a Pokémon. Round black button: replay the voice for the Pokémon currently displayed. Blue button: show the five most likely predictions. Menu under the display: choose a language. Use the directional pad to browse Pokémon and their information.", "Close"),
    ES("es", "es-ES", "ES", "POKÉMON", "PERFIL POKÉMON", "LÍNEA EVOLUTIVA", "TIPO", "ALTURA", "PESO", "HABILIDADES", "CARGANDO POKÉDEX...", "PREDICCIONES", "PROBABILIDAD", "POKÉMON ANTERIOR", "SIGUIENTE POKÉMON", "PULSA EL BOTÓN VERDE PARA ESCANEAR", "ANALIZANDO IMAGEN...", "APUNTA A UN POKÉMON", "INICIANDO CÁMARA...", "SE REQUIERE PERMISO DE CÁMARA", "Nombres no disponibles", "Ayuda de la Pokédex", "Botón verde: púlsalo una vez para iniciar la cámara y otra vez para tomar la foto e identificar al Pokémon. Botón negro redondo: repite la voz del Pokémon que aparece en pantalla. Botón azul: muestra las cinco predicciones más probables. Menú bajo la pantalla: selecciona un idioma. Usa la cruceta para ver otros Pokémon y sus datos.", "Cerrar"),
    FR("fr", "fr-FR", "FR", "POKÉMON", "PROFIL POKÉMON", "ÉVOLUTIONS", "TYPE", "TAILLE", "POIDS", "TALENTS", "CHARGEMENT DU POKÉDEX...", "PRÉDICTIONS", "PROBABILITÉ", "POKÉMON PRÉCÉDENT", "POKÉMON SUIVANT", "APPUYEZ SUR LE BOUTON VERT POUR SCANNER", "ANALYSE DE L’IMAGE...", "Visez un Pokémon", "DÉMARRAGE DE LA CAMÉRA...", "AUTORISATION DE LA CAMÉRA REQUISE", "Noms indisponibles", "Aide du Pokédex", "Bouton vert : appuyez une fois pour démarrer la caméra, puis une seconde fois pour prendre une photo et identifier un Pokémon. Bouton noir rond : relance la voix du Pokémon affiché. Bouton bleu : affiche les cinq prédictions les plus probables. Menu sous l’écran : choisissez une langue. Utilisez la croix directionnelle pour parcourir les Pokémon et leurs informations.", "Fermer"),
    DE("de", "de-DE", "DE", "POKÉMON", "POKÉMON-PROFIL", "ENTWICKLUNGSREIHE", "TYP", "GRÖSSE", "GEWICHT", "FÄHIGKEITEN", "POKÉDEX WIRD GELADEN...", "VORHERSAGEN", "WAHRSCHEINLICHKEIT", "VORHERIGES POKÉMON", "NÄCHSTES POKÉMON", "GRÜNEN KNOPF ZUM SCANNEN DRÜCKEN", "BILD WIRD ANALYSIERT...", "AUF EIN POKÉMON ZIELEN", "KAMERA STARTET...", "KAMERABERECHTIGUNG ERFORDERLICH", "Namen nicht verfügbar", "Pokédex-Hilfe", "Grüne Taste: einmal drücken, um die Kamera zu starten, erneut drücken, um ein Foto aufzunehmen und ein Pokémon zu erkennen. Runde schwarze Taste: Sprachausgabe des angezeigten Pokémon wiederholen. Blaue Taste: die fünf wahrscheinlichsten Vorhersagen anzeigen. Menü unter dem Display: Sprache auswählen. Mit dem Steuerkreuz kannst du Pokémon und Informationen durchsuchen.", "Schließen"),
    IT("it", "it-IT", "IT", "POKÉMON", "PROFILO POKÉMON", "EVOLUZIONI", "TIPO", "ALTEZZA", "PESO", "ABILITÀ", "CARICAMENTO POKÉDEX...", "PREVISIONI", "PROBABILITÀ", "POKÉMON PRECEDENTE", "POKÉMON SUCCESSIVO", "PREMI IL PULSANTE VERDE PER SCANSIONARE", "ANALISI DELL’IMMAGINE...", "INQUADRA UN POKÉMON", "AVVIO DELLA FOTOCAMERA...", "AUTORIZZAZIONE FOTOCAMERA NECESSARIA", "Nomi non disponibili", "Guida del Pokédex", "Pulsante verde: premi una volta per avviare la fotocamera, poi di nuovo per scattare e identificare un Pokémon. Pulsante nero rotondo: ripete la voce del Pokémon visualizzato. Pulsante blu: mostra le cinque previsioni più probabili. Menu sotto lo schermo: scegli una lingua. Usa la croce direzionale per esplorare i Pokémon e le informazioni.", "Chiudi"),
    JA("ja", "ja-JP", "日本語", "ポケモン", "ポケモン情報", "進化ライン", "タイプ", "高さ", "重さ", "とくせい", "図鑑を読み込み中...", "予測結果", "確率", "前のポケモン", "次のポケモン", "緑のボタンを押してスキャン", "画像を解析中...", "ポケモンにカメラを向けてください", "カメラを起動中...", "カメラの許可が必要です", "名前を取得できません", "図鑑のヘルプ", "緑のボタン：1回押すとカメラが起動し、もう1回押すと撮影してポケモンを識別します。丸い黒いボタン：表示中のポケモンの音声を再生します。青いボタン：上位5件の予測を表示します。画面下のメニュー：言語を選択します。十字キーでポケモンと情報を切り替えます。", "閉じる");

    fun next(): AppLanguage = entries[(ordinal + 1) % entries.size]

    val versionLabel: String
        get() = when (this) {
            EN -> "Version"
            ES -> "Versión"
            FR -> "Version"
            DE -> "Version"
            IT -> "Versione"
            JA -> "バージョン"
        }

    companion object {
        fun fromApiCode(code: String): AppLanguage =
            entries.firstOrNull { it.apiCode == code } ?: EN
    }
}

@Composable
private fun PokedexScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) {
        context.getSharedPreferences("pokedex-settings", android.content.Context.MODE_PRIVATE)
    }
    var cameraActive by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var processing by remember { mutableStateOf(false) }
    var pokemon by remember { mutableStateOf<PokemonInfo?>(null) }
    var predictions by remember { mutableStateOf<List<Prediction>>(emptyList()) }
    var predictionNamesUnavailable by remember { mutableStateOf(false) }
    var showPredictions by remember { mutableStateOf(false) }
    var language by remember {
        mutableStateOf(AppLanguage.fromApiCode(preferences.getString("language", "en") ?: "en"))
    }
    var status by remember { mutableStateOf(language.ready) }
    var showHelp by remember { mutableStateOf(false) }
    var pendingAnnouncement by remember { mutableStateOf<PokemonInfo?>(null) }
    var pendingGenus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingDescription by remember { mutableStateOf<Pair<String, String>?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsInitializationFailed by remember { mutableStateOf(false) }
    var speechActive by remember { mutableStateOf(false) }
    val textToSpeech = remember(context) {
        TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            ttsInitializationFailed = result != TextToSpeech.SUCCESS
            if (result != TextToSpeech.SUCCESS && pendingAnnouncement != null) {
                status = "VOICE ENGINE UNAVAILABLE"
            }
        }
    }
    var selectedId by remember { mutableIntStateOf(1) }
    var detailPage by remember { mutableIntStateOf(0) }

    DisposableEffect(textToSpeech) {
        val mainHandler = Handler(Looper.getMainLooper())
        fun speakPendingDescription() {
            val description = pendingDescription
            pendingDescription = null
            if (description == null) {
                speechActive = false
                return
            }
            val (text, languageTag) = description
            val result = textToSpeech.setLanguage(Locale.forLanguageTag(languageTag))
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                speechActive = false
                status = "VOICE DATA UNAVAILABLE: ${languageTag.uppercase(Locale.ROOT)}"
            } else if (textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_ADD,
                    null,
                    "pokemon-description"
                ) == TextToSpeech.ERROR
            ) {
                speechActive = false
                status = "COULD NOT SPEAK POKÉMON ENTRY"
            }
        }
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { speechActive = true }
            }

            override fun onDone(utteranceId: String?) {
                when (utteranceId) {
                    "pokemon-name" -> mainHandler.post {
                        val genus = pendingGenus
                        pendingGenus = null
                        if (genus == null) {
                            speakPendingDescription()
                        } else {
                            val (text, languageTag) = genus
                            val result = textToSpeech.setLanguage(Locale.forLanguageTag(languageTag))
                            if (result == TextToSpeech.LANG_MISSING_DATA ||
                                result == TextToSpeech.LANG_NOT_SUPPORTED
                            ) {
                                speakPendingDescription()
                            } else if (textToSpeech.speak(
                                    text,
                                    TextToSpeech.QUEUE_ADD,
                                    null,
                                    "pokemon-genus"
                                ) == TextToSpeech.ERROR
                            ) {
                                speechActive = false
                                pendingDescription = null
                                status = "COULD NOT SPEAK POKÉMON GENUS"
                            }
                        }
                    }
                    "pokemon-genus" -> mainHandler.post { speakPendingDescription() }
                    "pokemon-description" -> mainHandler.post { speechActive = false }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { speechActive = false }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post { speechActive = false }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                mainHandler.post { speechActive = false }
            }
        })
        onDispose { textToSpeech.shutdown() }
    }

    fun speakPokemon(info: PokemonInfo) {
        if (!ttsReady) {
            if (ttsInitializationFailed) {
                status = "VOICE ENGINE UNAVAILABLE"
            } else {
                pendingAnnouncement = info
                status = "VOICE ENGINE STARTING..."
            }
            return
        }
        val speechTag = AppLanguage.fromApiCode(info.language).speechTag
        val languageResult = textToSpeech.setLanguage(Locale.forLanguageTag(speechTag))
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            status = "VOICE DATA UNAVAILABLE: ${info.language.uppercase(Locale.ROOT)}"
            return
        }
        pendingDescription = info.description to AppLanguage.fromApiCode(info.descriptionLanguage).speechTag
        pendingGenus = info.genus?.let {
            it to AppLanguage.fromApiCode(info.language).speechTag
        }
        if (textToSpeech.speak(info.name, TextToSpeech.QUEUE_FLUSH, null, "pokemon-name") == TextToSpeech.ERROR) {
            pendingGenus = null
            pendingDescription = null
            status = "COULD NOT SPEAK POKÉMON ENTRY"
        }
    }

    LaunchedEffect(ttsReady, pendingAnnouncement) {
        if (ttsReady) {
            pendingAnnouncement?.let { info ->
                pendingAnnouncement = null
                speakPokemon(info)
            }
        }
    }

    fun loadPokemon(
        id: Int,
        requestedLanguage: AppLanguage = language,
        announce: Boolean = false
    ) {
        val requestedId = id.coerceIn(1, 1025)
        selectedId = requestedId
        detailPage = 0
        showPredictions = false
        pendingAnnouncement = null
        pokemon = null
        status = requestedLanguage.loading
        scope.launch {
            try {
                val info = withContext(Dispatchers.IO) { PokeApi.fetch(requestedId, requestedLanguage) }
                if (requestedLanguage == language && requestedId == selectedId) {
                    pokemon = info
                    status = ""
                    if (announce) {
                        speakPokemon(info)
                    }
                }
            } catch (error: Exception) {
                if (requestedLanguage == language && requestedId == selectedId) {
                    status = "POKEAPI ERROR: ${error.message ?: "PLEASE TRY AGAIN"}"
                }
            }
        }
    }

    fun loadPredictionNames(items: List<Prediction>, requestedLanguage: AppLanguage) {
        scope.launch {
            try {
                val named = withContext(Dispatchers.IO) {
                    items.map { item ->
                        item.copy(name = PokeApi.fetchName(item.id, requestedLanguage))
                    }
                }
                if (requestedLanguage == language && predictions.map { it.id } == items.map { it.id }) {
                    predictions = named
                    predictionNamesUnavailable = false
                }
            } catch (_: Exception) {
                if (requestedLanguage == language && predictions.map { it.id } == items.map { it.id }) {
                    predictions = items.map { it.copy(name = "") }
                    predictionNamesUnavailable = true
                }
            }
        }
    }

    fun selectLanguage(nextLanguage: AppLanguage) {
        language = nextLanguage
        preferences.edit().putString("language", nextLanguage.apiCode).apply()
        status = if (cameraActive) nextLanguage.aim else nextLanguage.ready
        if (pokemon != null) {
            loadPokemon(selectedId, nextLanguage)
        }
        if (predictions.isNotEmpty()) {
            predictions = predictions.map { it.copy(name = "") }
            predictionNamesUnavailable = false
            loadPredictionNames(predictions, nextLanguage)
        }
    }

    fun changeLanguage() = selectLanguage(language.next())

    fun identify(bitmap: Bitmap) {
        processing = true
        pokemon = null
        predictions = emptyList()
        predictionNamesUnavailable = false
        showPredictions = false
        capturedImage = bitmap
        status = language.scanning
        scope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    PokemonClassifier(context).predict(bitmap)
                }
                predictions = results
                loadPredictionNames(results, language)
                loadPokemon(results.first().id, announce = true)
            } catch (error: Exception) {
                status = "SCAN ERROR: ${error.message ?: "PLEASE TRY AGAIN"}"
            } finally {
                processing = false
                capturedImage = null
            }
        }
    }

    fun takePicture() {
        val imageCapture = capture
        if (imageCapture == null) {
            status = language.cameraStarting
            return
        }
        val photo = File(context.cacheDir, "pokedex-capture.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(photo).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    cameraActive = false
                    capture = null
                    status = "READING CAPTURED IMAGE..."
                    scope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                BitmapFactory.decodeFile(photo.absolutePath)
                            }
                            if (bitmap == null) {
                                status = "COULD NOT READ CAPTURED IMAGE"
                            } else {
                                val squareBitmap = bitmap.centerCropSquare()
                                if (squareBitmap !== bitmap) bitmap.recycle()
                                identify(squareBitmap)
                            }
                        } catch (error: Exception) {
                            status = "IMAGE ERROR: ${error.message ?: "PLEASE RETRY"}"
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    status = "CAPTURE ERROR: ${exception.message ?: "PLEASE RETRY"}"
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraActive = true
            pokemon = null
            status = language.aim
        } else {
            status = language.permissionRequired
        }
    }

    fun startCamera() {
        if (cameraActive) {
            cameraActive = false
            capture = null
            status = if (pokemon == null) language.ready else ""
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            cameraActive = true
            pokemon = null
            status = language.aim
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.pokedex),
            contentDescription = "Pokédex device",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        val ledPulse by rememberInfiniteTransition(label = "voice led").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(300),
                repeatMode = RepeatMode.Reverse
            ),
            label = "voice led pulse"
        )
        Box(
            modifier = Modifier
                .offset(screenWidth * 0.085f, screenHeight * 0.057f)
                .size(screenWidth * 0.18f, screenHeight * 0.12f)
                .clip(CircleShape)
                .background(
                    if (speechActive && ledPulse > 0.5f) {
                        Color(0xAA8FE8FF)
                    } else {
                        Color(0xAA64C8F5)
                    }
                )
        )

        Box(
            modifier = Modifier
                .offset(screenWidth * 0.205f, screenHeight * 0.391f)
                .size(screenWidth * 0.52f, screenHeight * 0.255f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101416)),
            contentAlignment = Alignment.Center
        ) {
            if (cameraActive && capturedImage == null) {
                CameraPreview(
                    onCaptureReady = { capture = it },
                    onCameraError = { error ->
                        cameraActive = false
                        status = "CAMERA ERROR: ${error.message ?: "PLEASE RETRY"}"
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            capturedImage?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured Pokémon image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            pokemon?.let { info ->
                PokemonDisplay(info = info, page = detailPage)
            }
            if (pokemon == null && capturedImage == null) {
                if (processing) {
                    CircularProgressIndicator(
                        color = Color(0xFF51B7FF),
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp
                    )
                }
                if (status.isNotBlank()) {
                    Text(
                        text = status,
                        color = Color(0xFFE5F5EA),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0x990A1112))
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                }
            }
            if (showPredictions && predictions.isNotEmpty()) {
                PredictionDisplay(
                    predictions = predictions,
                    language = language,
                    namesUnavailable = predictionNamesUnavailable,
                    onSelect = { prediction -> loadPokemon(prediction.id) }
                )
            }
        }

        DeviceButton(
            description = if (cameraActive) "Take picture and identify Pokémon" else "Start camera",
            x = 0.25f,
            y = 0.815f,
            width = 0.25f,
            height = 0.105f,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            enabled = !processing,
            onClick = {
                if (cameraActive) takePicture() else startCamera()
            }
        )
        DeviceButton(
            description = "Replay voice for displayed Pokémon",
            x = 0.075f,
            y = 0.77f,
            width = 0.12f,
            height = 0.085f,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            enabled = pokemon != null,
            onClick = { pokemon?.let(::speakPokemon) }
        )

        DeviceButton(
            description = if (showPredictions) "Hide top predictions" else "Show top five predictions",
            x = 0.42f,
            y = 0.766f,
            width = 0.105f,
            height = 0.02f,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            enabled = predictions.isNotEmpty(),
            onClick = { showPredictions = !showPredictions }
        )

        LanguageSelector(
            selected = language,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            onSelected = ::selectLanguage
        )

        DpadControls(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            enabled = pokemon != null,
            language = language,
            onDirection = { direction ->
                when (direction) {
                    "up" -> detailPage = (detailPage + 2) % 3
                    "down" -> detailPage = (detailPage + 1) % 3
                    "left" -> loadPokemon(if (selectedId == 1) 1025 else selectedId - 1)
                    "right" -> loadPokemon(if (selectedId == 1025) 1 else selectedId + 1)
                }
            }
        )

        DeviceButton(
            description = language.helpTitle,
            x = 0.91f,
            y = 0.035f,
            width = 0.07f,
            height = 0.045f,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            indicator = "?",
            onClick = { showHelp = true }
        )

        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text(language.helpTitle) },
                text = {
                    Column {
                        Text(language.helpInstructions)
                        Spacer(Modifier.height(12.dp))
                        Text("${language.versionLabel} 0.1.0", fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) {
                        Text(language.close)
                    }
                }
            )
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: AppLanguage,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    onSelected: (AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .offset(screenWidth * 0.65f, screenHeight * 0.655f)
            .size(screenWidth * 0.105f, screenHeight * 0.05f)
            .semantics { contentDescription = "Select language: ${selected.label}" }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = { expanded = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (language) {
                                AppLanguage.EN -> "English"
                                AppLanguage.ES -> "Español"
                                AppLanguage.FR -> "Français"
                                AppLanguage.DE -> "Deutsch"
                                AppLanguage.IT -> "Italiano"
                                AppLanguage.JA -> "日本語"
                            }
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(language)
                    }
                )
            }
        }
    }
}

@Composable
private fun DeviceButton(
    description: String,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
    indicator: String? = null,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .offset(screenWidth * x, screenHeight * y)
            .size(screenWidth * width, screenHeight * height)
            .semantics { contentDescription = description }
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
    ) {
        indicator?.let {
            Text(
                text = it,
                color = Color(0xFF8EE6A0),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun DpadControls(
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    language: AppLanguage,
    onDirection: (String) -> Unit
) {
    val x = 0.58f
    val y = 0.765f
    val w = 0.25f
    val h = 0.165f
    DeviceButton(language.previous, x, y + h * 0.34f, w * 0.34f, h * 0.34f, screenWidth, screenHeight, enabled) {
        onDirection("left")
    }
    DeviceButton(language.next, x + w * 0.66f, y + h * 0.34f, w * 0.34f, h * 0.34f, screenWidth, screenHeight, enabled) {
        onDirection("right")
    }
    DeviceButton("More Pokémon information", x + w * 0.34f, y, w * 0.32f, h * 0.34f, screenWidth, screenHeight, enabled) {
        onDirection("up")
    }
    DeviceButton("Previous information", x + w * 0.34f, y + h * 0.66f, w * 0.32f, h * 0.34f, screenWidth, screenHeight, enabled) {
        onDirection("down")
    }
}

@Composable
private fun CameraPreview(
    onCaptureReady: (ImageCapture) -> Unit,
    onCameraError: (Exception) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: Executor = remember(context) { ContextCompat.getMainExecutor(context) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(focusPoint) {
        val selectedPoint = focusPoint ?: return@LaunchedEffect
        delay(1_000)
        if (focusPoint == selectedPoint) focusPoint = null
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val squareSize = minOf(maxWidth, maxHeight)
        Box(
            modifier = Modifier.size(squareSize),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera) {
                        detectTapGestures { point ->
                            camera?.let { activeCamera ->
                                val meteringPoint = previewView.meteringPointFactory
                                    .createPoint(point.x, point.y)
                                val action = FocusMeteringAction.Builder(meteringPoint)
                                    .setAutoCancelDuration(5, TimeUnit.SECONDS)
                                    .build()
                                activeCamera.cameraControl.startFocusAndMetering(action)
                                focusPoint = point
                            }
                        }
                    }
            )
            Canvas(Modifier.fillMaxSize()) {
                focusPoint?.let { point ->
                    drawCircle(
                        color = Color(0xFFFFE15A),
                        radius = 30f,
                        center = point,
                        style = Stroke(width = 3f)
                    )
                    drawLine(
                        color = Color(0xFFFFE15A),
                        start = Offset(point.x - 42f, point.y),
                        end = Offset(point.x + 42f, point.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFFFFE15A),
                        start = Offset(point.x, point.y - 42f),
                        end = Offset(point.x, point.y + 42f),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var disposed = false
        var boundProvider: ProcessCameraProvider? = null
        val listener = Runnable {
            if (disposed) return@Runnable
            try {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                boundProvider = provider
                onCaptureReady(imageCapture)
            } catch (error: Exception) {
                onCameraError(error)
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            disposed = true
            boundProvider?.unbindAll()
            camera = null
        }
    }
}

@Composable
private fun PokemonDisplay(info: PokemonInfo, page: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101416))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (page) {
            0 -> {
                info.artwork?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = info.name,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Spacer(Modifier.weight(1f))
                Text(
                    text = "#${info.id.toString().padStart(3, '0')}  ${info.name}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info.types.joinToString("  •  "),
                    color = Color(0xFF8EE6A0),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            1 -> {
                Spacer(Modifier.height(5.dp))
                val locale = AppLanguage.fromApiCode(info.language)
                Text(locale.profile, color = Color(0xFF8EE6A0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                InfoLine(locale.type, info.types.joinToString(" / "))
                InfoLine(locale.height, "${info.height / 10.0} m")
                InfoLine(locale.weight, "${info.weight / 10.0} kg")
                InfoLine(locale.abilities, info.abilities.joinToString(", "))
                Spacer(Modifier.weight(1f))
                PageIndicator(page)
            }
            else -> {
                Spacer(Modifier.height(7.dp))
                Text(AppLanguage.fromApiCode(info.language).evolution, color = Color(0xFF8EE6A0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    info.evolutions.forEachIndexed { index, evolution ->
                        if (index > 0) {
                            Text(
                                text = "›",
                                color = Color(0xFF8EE6A0),
                                fontSize = 20.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.width(54.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            evolution.artwork?.let { artwork ->
                                Image(
                                    bitmap = artwork.asImageBitmap(),
                                    contentDescription = "${evolution.name} evolution artwork",
                                    modifier = Modifier.size(43.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } ?: Spacer(Modifier.size(43.dp))
                            Text(
                                text = evolution.name,
                                color = Color.White,
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                PageIndicator(page)
            }
        }
        if (page == 0) PageIndicator(page)
    }
}

@Composable
private fun PredictionDisplay(
    predictions: List<Prediction>,
    language: AppLanguage,
    namesUnavailable: Boolean,
    onSelect: (Prediction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101416))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(language.topPredictions, color = Color(0xFF8EE6A0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (namesUnavailable) {
            Text(language.namesUnavailable, color = Color(0xFFB7C7C0), fontSize = 8.sp)
        }
        Spacer(Modifier.height(3.dp))
        predictions.forEach { prediction ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSelect(prediction) }
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("#${prediction.id.toString().padStart(3, '0')}")
                        if (prediction.name.isNotBlank()) append("  ${prediction.name}")
                    },
                    color = Color.White,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${language.confidence}: ${"%.1f".format(Locale.ROOT, prediction.probability * 100)}%",
                    color = Color(0xFF8EE6A0),
                    fontSize = 8.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = Color.White,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

@Composable
private fun PageIndicator(page: Int) {
    Text(
        text = "●".repeat(page + 1) + "○".repeat(2 - page),
        color = Color(0xFF8EE6A0),
        fontSize = 8.sp,
        modifier = Modifier.padding(top = 3.dp)
    )
}

private fun String.titleCase(): String =
    replace('-', ' ').split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

private fun Bitmap.centerCropSquare(): Bitmap {
    val cropSize = minOf(width, height)
    if (cropSize <= 0) throw IllegalStateException("Captured image has invalid dimensions")
    val cropX = (width - cropSize) / 2
    val cropY = (height - cropSize) / 2
    return Bitmap.createBitmap(this, cropX, cropY, cropSize, cropSize)
}

private object PokeApi {
    fun fetch(id: Int, language: AppLanguage): PokemonInfo {
        val pokemon = getJson("https://pokeapi.co/api/v2/pokemon/$id/")
        val species = getJson(pokemon.getJSONObject("species").getString("url"))
        val genus = localizedGenus(species, language)
        val (description, descriptionLanguage) = localizedDescription(species, language)
        val evolutionUrl = species.getJSONObject("evolution_chain").getString("url")
        val chainJson = getJson(evolutionUrl).getJSONObject("chain")
        val evolutionSpecies = mutableListOf<Pair<String, Int>>()
        collectEvolutions(chainJson, evolutionSpecies)
        val evolutions = evolutionSpecies.map { (name, speciesId) ->
            val evolutionPokemon = getJson("https://pokeapi.co/api/v2/pokemon/$speciesId/")
            val evolutionSpeciesData = getJson(evolutionPokemon.getJSONObject("species").getString("url"))
            val evolutionArtworkUrl = evolutionPokemon.getJSONObject("sprites")
                .getJSONObject("other")
                .getJSONObject("official-artwork")
                .optString("front_default")
            EvolutionEntry(
                name = localizedName(evolutionSpeciesData, name, language),
                artwork = evolutionArtworkUrl.takeIf(String::isNotBlank)?.let(::downloadBitmap)
            )
        }
        val types = pokemon.getJSONArray("types")
        val abilities = pokemon.getJSONArray("abilities")
        val typeNames = (0 until types.length()).map {
            val type = getJson(types.getJSONObject(it).getJSONObject("type").getString("url"))
            localizedName(type, type.getString("name"), language)
        }
        val abilityNames = (0 until abilities.length()).map {
            val ability = getJson(abilities.getJSONObject(it).getJSONObject("ability").getString("url"))
            localizedName(ability, ability.getString("name"), language)
        }
        val artworkUrl = pokemon.getJSONObject("sprites")
            .getJSONObject("other")
            .getJSONObject("official-artwork")
            .optString("front_default")
        val artwork = artworkUrl.takeIf(String::isNotBlank)?.let(::downloadBitmap)

        return PokemonInfo(
            id = pokemon.getInt("id"),
            name = localizedName(species, pokemon.getString("name"), language),
            language = language.apiCode,
            genus = genus,
            description = description,
            descriptionLanguage = descriptionLanguage,
            types = typeNames,
            height = pokemon.getInt("height"),
            weight = pokemon.getInt("weight"),
            abilities = abilityNames,
            evolutions = evolutions,
            artwork = artwork
        )
    }

    fun fetchName(id: Int, language: AppLanguage): String {
        val pokemon = getJson("https://pokeapi.co/api/v2/pokemon/$id/")
        val species = getJson(pokemon.getJSONObject("species").getString("url"))
        return localizedName(species, pokemon.getString("name"), language)
    }

    private fun localizedName(resource: JSONObject, fallback: String, language: AppLanguage): String {
        val names = resource.optJSONArray("names") ?: return fallback.titleCase()
        var englishName: String? = null
        for (index in 0 until names.length()) {
            val entry = names.getJSONObject(index)
            when (entry.getJSONObject("language").getString("name")) {
                language.apiCode -> return entry.getString("name")
                "en" -> englishName = entry.getString("name")
            }
        }
        return englishName ?: fallback.titleCase()
    }

    private fun localizedDescription(species: JSONObject, language: AppLanguage): Pair<String, String> {
        val entries = species.optJSONArray("flavor_text_entries")
            ?: throw IllegalStateException("Pokédex description is missing")
        var englishDescription: String? = null
        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            val languageCode = entry.getJSONObject("language").getString("name")
            val text = entry.getString("flavor_text")
                .replace(Regex("[\\n\\r\\f]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (text.isBlank()) continue
            if (languageCode == language.apiCode) return text to languageCode
            if (languageCode == "en" && englishDescription == null) {
                englishDescription = text
            }
        }
        return englishDescription?.let { it to "en" }
            ?: throw IllegalStateException("No Pokédex description is available in ${language.apiCode} or English")
    }

    private fun localizedGenus(species: JSONObject, language: AppLanguage): String? {
        val genera = species.optJSONArray("genera") ?: return null
        for (index in 0 until genera.length()) {
            val entry = genera.getJSONObject(index)
            if (entry.getJSONObject("language").getString("name") == language.apiCode) {
                return entry.getString("genus").takeIf(String::isNotBlank)
            }
        }
        return null
    }

    private fun collectEvolutions(chain: JSONObject, result: MutableList<Pair<String, Int>>) {
        val species = chain.getJSONObject("species")
        val speciesId = species.getString("url").trimEnd('/').substringAfterLast('/').toIntOrNull()
            ?: throw IllegalStateException("Invalid species URL for ${species.getString("name")}")
        result += species.getString("name") to speciesId
        val next = chain.getJSONArray("evolves_to")
        for (index in 0 until next.length()) {
            collectEvolutions(next.getJSONObject(index), result)
        }
    }

    private fun getJson(url: String): JSONObject =
        JSONObject(URL(url).openConnection().asHttpConnection().readText())

    private fun downloadBitmap(url: String): Bitmap? {
        val connection = URL(url).openConnection().asHttpConnection()
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Image request failed (HTTP $code)")
            }
            connection.inputStream.use(BitmapFactory::decodeStream)
        } finally {
            connection.disconnect()
        }
    }

    private fun java.net.URLConnection.asHttpConnection(): HttpURLConnection {
        val connection = this as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("User-Agent", "PokedexAndroid/1.0")
        return connection
    }

    private fun HttpURLConnection.readText(): String =
        try {
            val code = responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            inputStream.bufferedReader().use { it.readText() }
        } finally {
            disconnect()
        }
}

private class PokemonClassifier(private val context: android.content.Context) {
    fun predict(bitmap: Bitmap): List<Prediction> {
        val modelFile = getModelFile()
        val environment = ai.onnxruntime.OrtEnvironment.getEnvironment()
        val session = environment.createSession(modelFile.absolutePath, ai.onnxruntime.OrtSession.SessionOptions())
        try {
            val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            val input = java.nio.FloatBuffer.allocate(3 * INPUT_SIZE * INPUT_SIZE)
            for (channel in 0..2) {
                val mean = MEAN[channel]
                val std = STD[channel]
                for (pixel in pixels) {
                    val value = when (channel) {
                        0 -> android.graphics.Color.red(pixel)
                        1 -> android.graphics.Color.green(pixel)
                        else -> android.graphics.Color.blue(pixel)
                    } / 255f
                    input.put((value - mean) / std)
                }
            }
            input.rewind()
            val tensor = ai.onnxruntime.OnnxTensor.createTensor(
                environment,
                input,
                longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
            )
            tensor.use {
                session.run(mapOf("input" to tensor)).use { result ->
                    val logits = (result[0].value as? Array<*>)?.firstOrNull() as? FloatArray
                        ?: throw IllegalStateException("Model returned predictions in an unsupported format")
                    val labels = loadLabels()
                    if (logits.size != labels.size || labels.size != CLASS_COUNT) {
                        throw IllegalStateException(
                            "Model returned ${logits.size} predictions; expected ${labels.size} labels"
                        )
                    }
                    val maximum = logits.maxOrNull()
                        ?: throw IllegalStateException("Model returned no predictions")
                    val exponentialScores = logits.map {
                        kotlin.math.exp((it - maximum).toDouble())
                    }
                    val total = exponentialScores.sum()
                    if (!total.isFinite() || total <= 0.0) {
                        throw IllegalStateException("Model returned invalid prediction scores")
                    }
                    return logits.indices
                        .sortedByDescending { logits[it] }
                        .take(TOP_K)
                        .map { index ->
                            Prediction(
                                id = labels[index].first,
                                probability = (exponentialScores[index] / total).toFloat()
                            )
                        }
                }
            }
        } finally {
            session.close()
        }
    }

    private fun getModelFile(): File {
        val model = File(context.filesDir, MODEL_FILENAME)
        if (model.exists() && model.length() > 0) return model
        val temporary = File(context.filesDir, "$MODEL_FILENAME.tmp")
        try {
            context.assets.open(MODEL_FILENAME).use { input ->
                temporary.outputStream().use(input::copyTo)
            }
            if (!temporary.renameTo(model)) {
                throw IllegalStateException("Could not save classifier model")
            }
            return model
        } catch (error: Exception) {
            temporary.delete()
            throw IllegalStateException("Could not install bundled classifier model: ${error.message}", error)
        }
    }

    private fun loadLabels(): List<Pair<Int, String>> {
        val labelsJson = context.assets.open(LABELS_FILENAME)
            .bufferedReader()
            .use { it.readText() }
        val labels = org.json.JSONArray(labelsJson)
        if (labels.length() != CLASS_COUNT) {
            throw IllegalStateException("Bundled labels contain ${labels.length()} classes; expected $CLASS_COUNT")
        }
        return (0 until labels.length()).map { position ->
            val label = labels.getJSONObject(position)
            val index = label.getInt("index")
            if (index != position) {
                throw IllegalStateException("Bundled label indexes are not in model order")
            }
            val id = label.getInt("id")
            if (id !in 1..CLASS_COUNT) {
                throw IllegalStateException("Bundled Pokédex ID is out of range: $id")
            }
            id to label.getString("name")
        }
    }

    companion object {
        private const val INPUT_SIZE = 224
        private const val CLASS_COUNT = 1025
        private const val TOP_K = 5
        private const val MODEL_FILENAME = "pokemon_classifier_1025.onnx"
        private const val LABELS_FILENAME = "pokemon_labels_1025.json"
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
