package com.rafiq.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rafiq.quran.data.MushafPageMetadata
import com.rafiq.quran.data.MushafPageRepository
import com.rafiq.quran.data.MushafBookmarkRepository
import com.rafiq.quran.data.QuranFavoriteRepository
import com.rafiq.quran.data.QuranAyahReference
import com.rafiq.quran.data.QuranAyahBookmark
import com.rafiq.quran.data.QuranAyahBookmarkRepository
import com.rafiq.quran.data.QuranSearch
import com.rafiq.quran.data.QuranSearchResult
import com.rafiq.quran.data.QuranSurah
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val Context.mushafBookmarkStore by preferencesDataStore(name = "rafiq_mushaf_bookmarks")
private val MushafBookmarkedPagesKey = stringSetPreferencesKey("bookmarked_pages")
private val FavoriteAyahsKey = stringSetPreferencesKey("favorite_ayahs")
private val AyahBookmarksKey = stringSetPreferencesKey("ayah_bookmarks")
private val AutoFlipSecondsKey = intPreferencesKey("auto_flip_seconds")
private val AutoFlipDirectionKey = intPreferencesKey("auto_flip_direction")

private val MushafPaper = Color(0xFFFFFCF5)
private val MushafInk = Color(0xFF1C211C)
private val MushafGreen = Color(0xFF254B37)
private val MushafGold = Color(0xFFB99A60)
private val MushafLine = Color(0xFFD8C9A8)

@Composable
fun MushafReaderPage(
    surahs: List<QuranSurah>,
    initialSurah: Int,
    initialAyah: Int,
    readerFullScreen: Boolean,
    onBack: () -> Unit,
    onReaderFullScreenChange: (Boolean) -> Unit,
    onSavePosition: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedBookmarkKeys by context.mushafBookmarkStore.data
        .map { preferences -> preferences[MushafBookmarkedPagesKey] ?: emptySet() }
        .collectAsState(initial = emptySet())
    val bookmarkedPages = remember(savedBookmarkKeys) {
        MushafBookmarkRepository.decode(savedBookmarkKeys)
    }
    val savedFavoriteKeys by context.mushafBookmarkStore.data
        .map { preferences -> preferences[FavoriteAyahsKey] ?: emptySet() }
        .collectAsState(initial = emptySet())
    val favoriteAyahs = remember(savedFavoriteKeys) {
        QuranFavoriteRepository.decode(savedFavoriteKeys)
    }

    val savedAyahBookmarkKeys by context.mushafBookmarkStore.data
        .map { preferences -> preferences[AyahBookmarksKey] ?: emptySet() }
        .collectAsState(initial = emptySet())
    val ayahBookmarks = remember(savedAyahBookmarkKeys) {
        QuranAyahBookmarkRepository.decode(savedAyahBookmarkKeys)
    }

    val autoFlipSeconds by context.mushafBookmarkStore.data
        .map { preferences -> preferences[AutoFlipSecondsKey] ?: 0 }
        .collectAsState(initial = 0)
    val autoFlipDirection by context.mushafBookmarkStore.data
        .map { preferences -> preferences[AutoFlipDirectionKey] ?: 1 }
        .collectAsState(initial = 1)
    var metadata by remember { mutableStateOf<MushafPageMetadata?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        try {
            metadata = withContext(Dispatchers.IO) { MushafPageRepository.load(context) }
        } catch (_: Exception) {
            loadError = "تعذر تحميل خريطة صفحات المصحف."
        }
    }

    if (metadata == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loadError == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("جاري تجهيز صفحات المصحف...")
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError ?: "حدث خطأ")
                    TextButton(onClick = onBack) { Text("العودة لقائمة السور") }
                }
            }
        }
        return
    }

    val pageMap = metadata ?: return
    var pageNumber by rememberSaveable(initialSurah, initialAyah) { mutableIntStateOf(1) }
    var pageZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var pageOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var pageBitmap by remember(pageNumber) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var initialPageReady by rememberSaveable(initialSurah, initialAyah) { mutableStateOf(false) }
    var showPageDialog by rememberSaveable { mutableStateOf(false) }
    var showSurahIndex by rememberSaveable { mutableStateOf(false) }
    var showJuzIndex by rememberSaveable { mutableStateOf(false) }
    var showBookmarksDialog by rememberSaveable { mutableStateOf(false) }
    var showAyahBookmarksDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveAyahBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var pendingBookmarkSurah by rememberSaveable { mutableIntStateOf(1) }
    var pendingBookmarkAyah by rememberSaveable { mutableIntStateOf(1) }
    var ayahBookmarkName by rememberSaveable { mutableStateOf("") }
    var ayahBookmarkColorHex by rememberSaveable { mutableStateOf("#B99A60") }
    var showFavoritesDialog by rememberSaveable { mutableStateOf(false) }
    var showSurahInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showAutoFlipDialog by rememberSaveable { mutableStateOf(false) }
    var autoFlipPauseToken by remember { mutableIntStateOf(0) }
    var showPageIndex by rememberSaveable { mutableStateOf(false) }
    var showReaderMenu by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var showAyahPickerDialog by rememberSaveable { mutableStateOf(false) }
    var pickerSurahNumber by rememberSaveable { mutableIntStateOf(initialSurah.coerceIn(1, 114)) }
    var pickerAyahInput by rememberSaveable { mutableStateOf("1") }
    var pageInput by rememberSaveable { mutableStateOf("") }
    var searchInput by rememberSaveable { mutableStateOf("") }
    var surahIndexQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(pageMap, initialSurah, initialAyah) {
        pageNumber = MushafPageRepository.findPageNumber(
            pageMap.pages,
            initialSurah.coerceIn(1, 114),
            initialAyah.coerceAtLeast(1)
        )
        initialPageReady = true
    }

    LaunchedEffect(pageNumber) {
        pageZoom = 1f
        pageOffset = Offset.Zero
    }

    LaunchedEffect(context, pageNumber) {
        pageBitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open("mushaf-pages/$pageNumber.png").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val currentBoundary = pageMap.pages.getOrNull(pageNumber - 1) ?: pageMap.pages.first()
    val currentJuz = MushafPageRepository.findJuzNumber(
        pageMap.juz, currentBoundary.surah, currentBoundary.ayah
    )
    val currentSurah = surahs.firstOrNull { it.number == currentBoundary.surah }

    LaunchedEffect(pageNumber, currentBoundary.surah, currentBoundary.ayah, initialPageReady) {
        if (initialPageReady) {
            onSavePosition(currentBoundary.surah, currentBoundary.ayah)
        }
    }

    fun moveToPage(target: Int) {
        pageNumber = target.coerceIn(1, pageMap.pages.size)
    }

    fun toggleFavorite(result: QuranSearchResult) {
        scope.launch {
            context.mushafBookmarkStore.edit { preferences ->
                preferences[FavoriteAyahsKey] = QuranFavoriteRepository.toggle(
                    preferences[FavoriteAyahsKey] ?: emptySet(),
                    result.surahNumber,
                    result.ayahNumber
                )
            }
        }
    }

    val favoriteResults = remember(surahs, favoriteAyahs) {
        favoriteAyahs.mapNotNull { reference ->
            val surah = surahs.firstOrNull { it.number == reference.surahNumber }
            val ayah = surah?.ayahs?.firstOrNull { it.number == reference.ayahNumber }
            if (surah == null || ayah == null) null else {
                QuranSearchResult(surah.number, surah.name, ayah.number, ayah.text)
            }
        }.sortedWith(compareBy<QuranSearchResult> { it.surahNumber }.thenBy { it.ayahNumber })
    }

    val isReaderDialogOpen = showPageDialog || showSurahIndex || showJuzIndex || showPageIndex ||
        showBookmarksDialog || showAyahBookmarksDialog || showSaveAyahBookmarkDialog ||
        showFavoritesDialog || showSurahInfoDialog || showReaderMenu ||
        showSearchDialog || showAutoFlipDialog || showAyahPickerDialog

    LaunchedEffect(
        pageNumber,
        autoFlipSeconds,
        autoFlipDirection,
        isReaderDialogOpen,
        autoFlipPauseToken
    ) {
        if (autoFlipSeconds > 0 && !isReaderDialogOpen) {
            delay(autoFlipSeconds * 1_000L)
            val targetPage = pageNumber + if (autoFlipDirection < 0) -1 else 1
            if (targetPage in 1..pageMap.pages.size) {
                moveToPage(targetPage)
            } else {
                scope.launch {
                    context.mushafBookmarkStore.edit { preferences ->
                        preferences[AutoFlipSecondsKey] = 0
                    }
                }
            }
        }
    }

    val searchResults = remember(surahs, searchInput) {
        if (searchInput.isBlank()) emptyList()
        else QuranSearch.search(surahs, searchInput, limit = 30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4EFE4))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MushafPaper, RoundedCornerShape(14.dp))
                .border(1.dp, MushafLine, RoundedCornerShape(14.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = { showSearchDialog = true }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Filled.Search, contentDescription = "البحث في القرآن", tint = MushafGreen)
            }
            IconButton(
                onClick = {
                    scope.launch {
                        context.mushafBookmarkStore.edit { preferences ->
                            preferences[MushafBookmarkedPagesKey] = MushafBookmarkRepository.toggle(
                                preferences[MushafBookmarkedPagesKey] ?: emptySet(),
                                pageNumber
                            )
                        }
                    }
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = if (pageNumber in bookmarkedPages) "إزالة علامة الصفحة" else "حفظ الصفحة في العلامات",
                    tint = if (pageNumber in bookmarkedPages) MushafGold else MushafGreen
                )
            }
            Column(
                modifier = Modifier.weight(1f).clickable { showSurahInfoDialog = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    currentSurah?.name ?: "القرآن الكريم",
                    color = MushafInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "الجزء ${toArabicDigits(currentJuz)}  •  صفحة ${toArabicDigits(pageNumber)}",
                    color = MushafGreen,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = { showReaderMenu = true }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Filled.Menu, contentDescription = "قائمة المصحف", tint = MushafGreen)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(5.dp))
                .onSizeChanged { viewportSize = it }
                .pointerInput(pageNumber, viewportSize) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val startingZoom = pageZoom
                        var totalHorizontalPan = 0f
                        var usedMultiplePointers = false

                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount > 1) usedMultiplePointers = true

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (usedMultiplePointers || pageZoom > 1f || zoomChange != 1f) {
                                val oldZoom = pageZoom.coerceAtLeast(1f)
                                val newZoom = (oldZoom * zoomChange).coerceIn(1f, 4f)
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val center = Offset(
                                    viewportSize.width / 2f,
                                    viewportSize.height / 2f
                                )
                                val anchor = centroid - center
                                val scaleFactor = newZoom / oldZoom
                                val candidateOffset = Offset(
                                    x = (pageOffset.x - anchor.x) * scaleFactor + anchor.x + panChange.x,
                                    y = (pageOffset.y - anchor.y) * scaleFactor + anchor.y + panChange.y
                                )
                                val maxX = (viewportSize.width * (newZoom - 1f) / 2f).coerceAtLeast(0f)
                                val maxY = (viewportSize.height * (newZoom - 1f) / 2f).coerceAtLeast(0f)
                                pageZoom = newZoom
                                pageOffset = if (newZoom == 1f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        candidateOffset.x.coerceIn(-maxX, maxX),
                                        candidateOffset.y.coerceIn(-maxY, maxY)
                                    )
                                }
                            } else {
                                totalHorizontalPan += panChange.x
                            }

                            if (event.changes.any { it.positionChanged() }) {
                                autoFlipPauseToken++
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (!usedMultiplePointers && startingZoom <= 1f && pageZoom <= 1f) {
                            when {
                                totalHorizontalPan < -55f -> moveToPage(pageNumber + 1)
                                totalHorizontalPan > 55f -> moveToPage(pageNumber - 1)
                            }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MushafPaper, RoundedCornerShape(5.dp))
                    .border(2.dp, MushafGold, RoundedCornerShape(5.dp))
                    .graphicsLayer {
                        scaleX = pageZoom
                        scaleY = pageZoom
                        translationX = pageOffset.x
                        translationY = pageOffset.y
                    }
                    .padding(5.dp)
                    .border(1.dp, MushafLine, RoundedCornerShape(3.dp))
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pageBitmap != null) {
                    Image(
                        bitmap = pageBitmap!!,
                        contentDescription = "صفحة المصحف رقم ${toArabicDigits(pageNumber)}",
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "صورة صفحة المصحف غير متاحة حاليًا. لن يتم إنشاء نص بديل أو إعادة ترتيب الآيات.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                if (pageBitmap == null) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MushafLine))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("۞", color = MushafGold, fontSize = 18.sp)
                        Text(
                            toArabicDigits(pageNumber),
                            color = MushafGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text("۞", color = MushafGold, fontSize = 18.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { moveToPage(pageNumber - 1) }, enabled = pageNumber > 1) {
                Text("الصفحة السابقة")
            }
            Text(
                "صفحة ${toArabicDigits(pageNumber)} من ٦٠٤",
                color = MushafGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            TextButton(onClick = { moveToPage(pageNumber + 1) }, enabled = pageNumber < pageMap.pages.size) {
                Text("الصفحة التالية")
            }
        }
    }

    if (showReaderMenu) {
        AlertDialog(
            onDismissRequest = { showReaderMenu = false },
            title = { Text("قائمة المصحف") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        showReaderMenu = false
                        showSurahIndex = true
                    }) { Text("فهرس السور") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showAutoFlipDialog = true
                    }) {
                        Text(if (autoFlipSeconds > 0) "إعداد التقليب التلقائي" else "التقليب التلقائي")
                    }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showJuzIndex = true
                    }) { Text("فهرس الأجزاء") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showPageIndex = true
                    }) { Text("فهرس الصفحات") }
                    TextButton(onClick = {
                        onReaderFullScreenChange(!readerFullScreen)
                        showReaderMenu = false
                    }) {
                        Text(if (readerFullScreen) "إنهاء وضع القراءة الكامل" else "وضع القراءة الكامل")
                    }
                    TextButton(onClick = {
                        showSurahInfoDialog = true
                        showReaderMenu = false
                    }) { Text("معلومات السورة الحالية") }
                    TextButton(onClick = {
                        pageZoom = (pageZoom + 0.25f).coerceAtMost(4f)
                        showReaderMenu = false
                    }) { Text("تكبير الصفحة") }
                    TextButton(onClick = {
                        pageZoom = (pageZoom - 0.25f).coerceAtLeast(1f)
                        if (pageZoom == 1f) pageOffset = Offset.Zero
                        showReaderMenu = false
                    }) { Text("تصغير الصفحة") }
                    TextButton(onClick = {
                        pageZoom = 1f
                        pageOffset = Offset.Zero
                        showReaderMenu = false
                    }) { Text("إعادة ضبط الحجم") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showFavoritesDialog = true
                    }) { Text("آياتي المفضلة") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showBookmarksDialog = true
                    }) { Text("علامات الصفحات") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showAyahBookmarksDialog = true
                    }) { Text("علامات الآيات") }
                    TextButton(onClick = {
                        pickerSurahNumber = currentBoundary.surah
                        pickerAyahInput = currentBoundary.ayah.toString()
                        showReaderMenu = false
                        showAyahPickerDialog = true
                    }) { Text("اختيار سورة وآية") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showPageDialog = true
                    }) { Text("الانتقال إلى صفحة") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        onBack()
                    }) { Text("العودة لقائمة القرآن") }
                }
            },
            confirmButton = { TextButton(onClick = { showReaderMenu = false }) { Text("إغلاق") } }
        )
    }

    if (showAutoFlipDialog) {
        AlertDialog(
            onDismissRequest = { showAutoFlipDialog = false },
            title = { Text("التقليب التلقائي للصفحات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("اختر المدة بين كل صفحة والتي تليها.")
                    Text("اتجاه التقليب")
                    TextButton(
                        onClick = {
                            scope.launch {
                                context.mushafBookmarkStore.edit { preferences ->
                                    preferences[AutoFlipDirectionKey] = 1
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (autoFlipDirection > 0) "✓ من الصفحة الحالية إلى التالية" else "إلى الصفحة التالية")
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                context.mushafBookmarkStore.edit { preferences ->
                                    preferences[AutoFlipDirectionKey] = -1
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (autoFlipDirection < 0) "✓ من الصفحة الحالية إلى السابقة" else "إلى الصفحة السابقة")
                    }
                    listOf(10, 20, 30, 60).forEach { seconds ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    context.mushafBookmarkStore.edit { preferences ->
                                        preferences[AutoFlipSecondsKey] = seconds
                                    }
                                }
                                showAutoFlipDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("كل ${toArabicDigits(seconds)} ثانية")
                        }
                    }
                    if (autoFlipSeconds > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    context.mushafBookmarkStore.edit { preferences ->
                                        preferences[AutoFlipSecondsKey] = 0
                                    }
                                }
                                showAutoFlipDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("إيقاف التقليب التلقائي") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoFlipDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showSurahInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSurahInfoDialog = false },
            title = { Text("معلومات السورة") },
            text = {
                if (currentSurah == null) {
                    Text("معلومات السورة غير متاحة حاليًا.")
                } else {
                    val startPage = MushafPageRepository.findPageNumber(
                        pageMap.pages, currentSurah.number, 1
                    )
                    val startJuz = MushafPageRepository.findJuzNumber(
                        pageMap.juz, currentSurah.number, 1
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("الاسم: ${currentSurah.name}", fontWeight = FontWeight.Bold)
                        Text("رقم السورة: ${toArabicDigits(currentSurah.number)}")
                        Text("عدد الآيات: ${toArabicDigits(currentSurah.ayahs.size)}")
                        Text("نوع النزول بحسب بيانات المصحف: ${currentSurah.revelationType}")
                        Text("صفحة البداية: ${toArabicDigits(startPage)}")
                        Text("الجزء الذي تبدأ فيه: ${toArabicDigits(startJuz)}")
                        Text("الاسم بالإنجليزية: ${currentSurah.englishName}")
                        Text("النقل الصوتي للاسم: ${currentSurah.transliteration}")
                        Text(
                            "المعلومات المعروضة هنا هي الحقول المتاحة في بيانات التطبيق، ولا تتضمن أسباب نزول أو فضائل غير موثقة.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurahInfoDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { showFavoritesDialog = false },
            title = { Text("آياتي المفضلة") },
            text = {
                if (favoriteResults.isEmpty()) {
                    Text("لم تضف آيات للمفضلة بعد. استخدم علامة النجمة بجوار نتيجة البحث لحفظ آية.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(favoriteResults, key = { "${it.surahNumber}-${it.ayahNumber}" }) { result ->
                            val resultPage = MushafPageRepository.findPageNumber(
                                pageMap.pages, result.surahNumber, result.ayahNumber
                            )
                            val resultJuz = MushafPageRepository.findJuzNumber(
                                pageMap.juz, result.surahNumber, result.ayahNumber
                            )
                            SearchResultRow(
                                result = result,
                                isFavorite = true,
                                locationLabel = "صفحة ${toArabicDigits(resultPage)} • الجزء ${toArabicDigits(resultJuz)}",
                                onClick = {
                                    moveToPage(resultPage)
                                    showFavoritesDialog = false
                                },
                                onFavoriteClick = { toggleFavorite(result) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFavoritesDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = { Text("الصفحات المحفوظة") },
            text = {
                if (bookmarkedPages.isEmpty()) {
                    Text("لم تحفظ أي صفحة بعد. اضغط علامة الكتاب أعلى المصحف لحفظ الصفحة الحالية.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(bookmarkedPages.toList(), key = { it }) { savedPage ->
                            val boundary = pageMap.pages[savedPage - 1]
                            val surahName = surahs.firstOrNull { it.number == boundary.surah }?.name.orEmpty()
                            TextButton(
                                onClick = {
                                    moveToPage(savedPage)
                                    showBookmarksDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "صفحة ${toArabicDigits(savedPage)}  •  ${surahName} ${toArabicDigits(boundary.ayah)}",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showSurahIndex) {
        val normalizedSurahQuery = surahIndexQuery.trim()
        val filteredSurahs = remember(surahs, normalizedSurahQuery) {
            if (normalizedSurahQuery.isBlank()) {
                surahs
            } else {
                surahs.filter { surah ->
                    surah.name.contains(normalizedSurahQuery, ignoreCase = true) ||
                        surah.englishName.contains(normalizedSurahQuery, ignoreCase = true) ||
                        surah.transliteration.contains(normalizedSurahQuery, ignoreCase = true) ||
                        surah.number.toString() == normalizedSurahQuery
                }
            }
        }
        AlertDialog(
            onDismissRequest = {
                showSurahIndex = false
                surahIndexQuery = ""
            },
            title = { Text("فهرس السور") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = surahIndexQuery,
                        onValueChange = { surahIndexQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("ابحث باسم السورة أو رقمها") }
                    )
                    if (filteredSurahs.isEmpty()) {
                        Text("لا توجد سورة مطابقة للبحث.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredSurahs, key = { it.number }) { surah ->
                                val startPage = MushafPageRepository.findPageNumber(
                                    pageMap.pages,
                                    surah.number,
                                    1
                                )
                                val startJuz = MushafPageRepository.findJuzNumber(
                                    pageMap.juz,
                                    surah.number,
                                    1
                                )
                                val revelationLabel = when {
                                    surah.revelationType.contains("Medinan", ignoreCase = true) ||
                                        surah.revelationType.contains("مدنية") -> "مدنية"
                                    surah.revelationType.contains("Meccan", ignoreCase = true) ||
                                        surah.revelationType.contains("مكية") -> "مكية"
                                    else -> surah.revelationType
                                }
                                TextButton(
                                    onClick = {
                                        moveToPage(startPage)
                                        onSavePosition(surah.number, 1)
                                        showSurahIndex = false
                                        surahIndexQuery = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            "${toArabicDigits(surah.number)}. ${surah.name}",
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            "${toArabicDigits(surah.ayahs.size)} آية  •  ${revelationLabel}  •  صفحة ${toArabicDigits(startPage)}  •  الجزء ${toArabicDigits(startJuz)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSurahIndex = false
                    surahIndexQuery = ""
                }) { Text("إغلاق") }
            }
        )
    }

    if (showJuzIndex) {
        AlertDialog(
            onDismissRequest = { showJuzIndex = false },
            title = { Text("فهرس الأجزاء") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pageMap.juz, key = { it.index }) { juz ->
                        val juzSurah = surahs.firstOrNull { it.number == juz.surah }
                        TextButton(
                            onClick = {
                                moveToPage(
                                    MushafPageRepository.findPageNumber(
                                        pageMap.pages, juz.surah, juz.ayah
                                    )
                                )
                                showJuzIndex = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val startPage = MushafPageRepository.findPageNumber(
                                pageMap.pages, juz.surah, juz.ayah
                            )
                            Text(
                                "الجزء ${toArabicDigits(juz.index)}  •  ${juzSurah?.name.orEmpty()} ${toArabicDigits(juz.ayah)}  •  صفحة ${toArabicDigits(startPage)}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJuzIndex = false }) { Text("إغلاق") }
            }
        )
    }

    if (showPageIndex) {
        AlertDialog(
            onDismissRequest = { showPageIndex = false },
            title = { Text("فهرس صفحات المصحف") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(pageMap.pages, key = { it.page }) { boundary ->
                        val pageSurah = surahs.firstOrNull { it.number == boundary.surah }
                        val pageJuz = MushafPageRepository.findJuzNumber(
                            pageMap.juz,
                            boundary.surah,
                            boundary.ayah
                        )
                        TextButton(
                            onClick = {
                                moveToPage(boundary.page)
                                showPageIndex = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "صفحة ${toArabicDigits(boundary.page)}  •  ${pageSurah?.name.orEmpty()} ${toArabicDigits(boundary.ayah)}  •  الجزء ${toArabicDigits(pageJuz)}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageIndex = false }) { Text("إغلاق") }
            }
        )
    }

    if (showPageDialog) {
        AlertDialog(
            onDismissRequest = { showPageDialog = false },
            title = { Text("الانتقال إلى صفحة") },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { value -> pageInput = value.filter { it.isDigit() || it in '٠'..'٩' } },
                    label = { Text("رقم الصفحة من ١ إلى ٦٠٤") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = pageInput.map { char ->
                        when (char) {
                            in '٠'..'٩' -> char.code - '٠'.code
                            else -> char.code - '0'.code
                        }
                    }.joinToString("").toIntOrNull()
                    if (parsed != null && parsed in 1..pageMap.pages.size) {
                        moveToPage(parsed)
                        showPageDialog = false
                        pageInput = ""
                    }
                }) { Text("انتقال") }
            },
            dismissButton = {
                TextButton(onClick = { showPageDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showAyahPickerDialog) {
        val selectedPickerSurah = surahs.firstOrNull { it.number == pickerSurahNumber }
        val parsedPickerAyah = pickerAyahInput
            .mapNotNull { char ->
                Character.digit(char, 10).takeIf { digit -> digit >= 0 }
            }
            .joinToString("")
            .toIntOrNull()
        val validPickerAyah = selectedPickerSurah?.ayahs?.firstOrNull {
            it.number == parsedPickerAyah
        }
        AlertDialog(
            onDismissRequest = { showAyahPickerDialog = false },
            title = { Text("اختيار سورة وآية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر السورة")
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(surahs, key = { it.number }) { surah ->
                            TextButton(
                                onClick = {
                                    pickerSurahNumber = surah.number
                                    pickerAyahInput = "1"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${if (surah.number == pickerSurahNumber) "✓  " else ""}${surah.name} • ${toArabicDigits(surah.ayahs.size)} آية",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    color = if (surah.number == pickerSurahNumber) MushafGreen else MushafInk
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = pickerAyahInput,
                        onValueChange = { value ->
                            pickerAyahInput = value.filter { it.isDigit() || it in '٠'..'٩' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("رقم الآية (١–${selectedPickerSurah?.ayahs?.size ?: 0})") }
                    )
                    if (parsedPickerAyah != null && validPickerAyah == null) {
                        Text("رقم الآية غير موجود في السورة المختارة.", color = MaterialTheme.colorScheme.error)
                    }
                    if (validPickerAyah != null) {
                        Text(validPickerAyah.text, color = MushafInk, textAlign = TextAlign.End)
                    }
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            enabled = selectedPickerSurah != null && validPickerAyah != null,
                            onClick = {
                                val targetSurah = selectedPickerSurah ?: return@TextButton
                                val targetAyah = validPickerAyah ?: return@TextButton
                                moveToPage(
                                    MushafPageRepository.findPageNumber(
                                        pageMap.pages, targetSurah.number, targetAyah.number
                                    )
                                )
                                onSavePosition(targetSurah.number, targetAyah.number)
                                showAyahPickerDialog = false
                            }
                        ) { Text("فتح الآية") }
                        TextButton(
                            enabled = selectedPickerSurah != null && validPickerAyah != null,
                            onClick = {
                                val targetSurah = selectedPickerSurah ?: return@TextButton
                                val targetAyah = validPickerAyah ?: return@TextButton
                                toggleFavorite(
                                    QuranSearchResult(
                                        targetSurah.number,
                                        targetSurah.name,
                                        targetAyah.number,
                                        targetAyah.text
                                    )
                                )
                                showAyahPickerDialog = false
                            }
                        ) { Text("حفظ كمفضلة") }
                    }
                    TextButton(
                        enabled = selectedPickerSurah != null && validPickerAyah != null,
                        onClick = {
                            pendingBookmarkSurah = selectedPickerSurah!!.number
                            pendingBookmarkAyah = validPickerAyah!!.number
                            ayahBookmarkName = "${selectedPickerSurah.name} - الآية ${validPickerAyah.number}"
                            ayahBookmarkColorHex = "#B99A60"
                            showAyahPickerDialog = false
                            showSaveAyahBookmarkDialog = true
                        }
                    ) { Text("إضافة علامة مرجعية") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAyahPickerDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showSaveAyahBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAyahBookmarkDialog = false },
            title = { Text("حفظ علامة مرجعية للآية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${surahs.firstOrNull { it.number == pendingBookmarkSurah }?.name.orEmpty()} • الآية ${toArabicDigits(pendingBookmarkAyah)}"
                    )
                    OutlinedTextField(
                        value = ayahBookmarkName,
                        onValueChange = { ayahBookmarkName = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("اسم العلامة") }
                    )
                    Text("لون العلامة")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("#B99A60", "#2E7D5B", "#4776B8", "#9A5AA6", "#C66A43").forEach { colorHex ->
                            TextButton(
                                onClick = { ayahBookmarkColorHex = colorHex },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (ayahBookmarkColorHex == colorHex) "✓" else "●",
                                    color = Color(android.graphics.Color.parseColor(colorHex))
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = ayahBookmarkName.isNotBlank(),
                    onClick = {
                        val bookmark = QuranAyahBookmark(
                            id = UUID.randomUUID().toString(),
                            surahNumber = pendingBookmarkSurah,
                            ayahNumber = pendingBookmarkAyah,
                            label = ayahBookmarkName.trim(),
                            colorHex = ayahBookmarkColorHex,
                            createdAtMillis = System.currentTimeMillis()
                        )
                        scope.launch {
                            context.mushafBookmarkStore.edit { preferences ->
                                preferences[AyahBookmarksKey] = QuranAyahBookmarkRepository.add(
                                    preferences[AyahBookmarksKey] ?: emptySet(),
                                    bookmark
                                )
                            }
                        }
                        showSaveAyahBookmarkDialog = false
                    }
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAyahBookmarkDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showAyahBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showAyahBookmarksDialog = false },
            title = { Text("علامات الآيات المرجعية") },
            text = {
                if (ayahBookmarks.isEmpty()) {
                    Text("لا توجد علامات محفوظة. أضف علامة من قائمة اختيار السورة والآية.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ayahBookmarks, key = { it.id }) { bookmark ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "●",
                                    color = Color(android.graphics.Color.parseColor(bookmark.colorHex)),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    TextButton(
                                        onClick = {
                                            moveToPage(
                                                MushafPageRepository.findPageNumber(
                                                    pageMap.pages,
                                                    bookmark.surahNumber,
                                                    bookmark.ayahNumber
                                                )
                                            )
                                            onSavePosition(bookmark.surahNumber, bookmark.ayahNumber)
                                            showAyahBookmarksDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(bookmark.label, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${surahs.firstOrNull { it.number == bookmark.surahNumber }?.name.orEmpty()} • الآية ${toArabicDigits(bookmark.ayahNumber)}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            context.mushafBookmarkStore.edit { preferences ->
                                                preferences[AyahBookmarksKey] =
                                                    QuranAyahBookmarkRepository.remove(
                                                        preferences[AyahBookmarksKey] ?: emptySet(),
                                                        bookmark.id
                                                    )
                                            }
                                        }
                                    }
                                ) { Icon(Icons.Filled.Delete, contentDescription = "حذف العلامة") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAyahBookmarksDialog = false }) { Text("إغلاق") }
            }
        )
    }

    if (showSearchDialog) {
        Dialog(
            onDismissRequest = { showSearchDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 560.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("البحث في آيات القرآن", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("اكتب كلمة أو عبارة") }
                    )
                    if (searchInput.isNotBlank() && searchResults.isEmpty()) {
                        Text("لا توجد نتائج مطابقة.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(searchResults, key = { "${it.surahNumber}-${it.ayahNumber}" }) { result ->
                                val resultPage = MushafPageRepository.findPageNumber(
                                    pageMap.pages, result.surahNumber, result.ayahNumber
                                )
                                val resultJuz = MushafPageRepository.findJuzNumber(
                                    pageMap.juz, result.surahNumber, result.ayahNumber
                                )
                                SearchResultRow(
                                    result = result,
                                    isFavorite = QuranAyahReference(result.surahNumber, result.ayahNumber) in favoriteAyahs,
                                    locationLabel = "صفحة ${toArabicDigits(resultPage)} • الجزء ${toArabicDigits(resultJuz)}",
                                    onClick = {
                                        moveToPage(resultPage)
                                        showSearchDialog = false
                                        searchInput = ""
                                    },
                                    onFavoriteClick = { toggleFavorite(result) }
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { showSearchDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("إغلاق") }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: QuranSearchResult,
    isFavorite: Boolean,
    locationLabel: String = "",
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    val shareText = "${result.text}\n(${result.surahName}: ${result.ayahNumber})\nرفيق القرآن"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4EFE4), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "${result.surahName} • الآية ${toArabicDigits(result.ayahNumber)}",
                color = MushafGreen,
                fontWeight = FontWeight.Bold
            )
            Text(result.text, color = MushafInk, textAlign = TextAlign.End, maxLines = 2)
            if (locationLabel.isNotBlank()) {
                Text(
                    locationLabel,
                    color = MushafGreen,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("آية قرآنية", shareText))
                Toast.makeText(context, "تم نسخ الآية", Toast.LENGTH_SHORT).show()
            }
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الآية", tint = MushafGreen)
        }
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "مشاركة الآية"))
            }
        ) {
            Icon(Icons.Filled.Share, contentDescription = "مشاركة الآية", tint = MushafGreen)
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = if (isFavorite) "إزالة الآية من المفضلة" else "إضافة الآية للمفضلة",
                tint = if (isFavorite) MushafGold else MushafGreen
            )
        }
    }
}

private fun toArabicDigits(number: Int): String = number.toString().map { digit ->
    if (digit in '0'..'9') (digit.code - '0'.code + '٠'.code).toChar() else digit
}.joinToString("")
