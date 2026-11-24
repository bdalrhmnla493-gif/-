package com.rafiq.quran

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rafiq.quran.data.MushafPageMetadata
import com.rafiq.quran.data.MushafPageRepository
import com.rafiq.quran.data.QuranSearch
import com.rafiq.quran.data.QuranSearchResult
import com.rafiq.quran.data.QuranSurah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onBack: () -> Unit,
    onSavePosition: (Int, Int) -> Unit
) {
    val context = LocalContext.current
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
    var pageBitmap by remember(pageNumber) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var initialPageReady by rememberSaveable(initialSurah, initialAyah) { mutableStateOf(false) }
    var showPageDialog by rememberSaveable { mutableStateOf(false) }
    var showSurahIndex by rememberSaveable { mutableStateOf(false) }
    var showJuzIndex by rememberSaveable { mutableStateOf(false) }
    var showReaderMenu by rememberSaveable { mutableStateOf(false) }
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    var pageInput by rememberSaveable { mutableStateOf("") }
    var searchInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(pageMap, initialSurah, initialAyah) {
        pageNumber = MushafPageRepository.findPageNumber(
            pageMap.pages,
            initialSurah.coerceIn(1, 114),
            initialAyah.coerceAtLeast(1)
        )
        initialPageReady = true
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
    val pageAyahs = remember(surahs, pageMap, pageNumber) {
        MushafPageRepository.getPageAyahs(surahs, pageMap.pages, pageNumber)
    }
    val basmala = remember(surahs) {
        surahs.firstOrNull { it.number == 1 }?.ayahs?.firstOrNull()?.text.orEmpty()
    }
    val showBasmala = currentBoundary.ayah == 1 &&
        currentBoundary.surah != 1 && currentBoundary.surah != 9

    LaunchedEffect(pageNumber, currentBoundary.surah, currentBoundary.ayah, initialPageReady) {
        if (initialPageReady) {
            onSavePosition(currentBoundary.surah, currentBoundary.ayah)
        }
    }

    fun moveToPage(target: Int) {
        pageNumber = target.coerceIn(1, pageMap.pages.size)
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
            Column(
                modifier = Modifier.weight(1f),
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
                .pointerInput(pageNumber, pageMap.pages.size) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            when {
                                totalDrag < -55f -> moveToPage(pageNumber + 1)
                                totalDrag > 55f -> moveToPage(pageNumber - 1)
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MushafPaper, RoundedCornerShape(5.dp))
                    .border(2.dp, MushafGold, RoundedCornerShape(5.dp))
                    .padding(5.dp)
                    .border(1.dp, MushafLine, RoundedCornerShape(3.dp))
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pageBitmap != null) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pageBitmap!!,
                            contentDescription = "صفحة المصحف رقم ${toArabicDigits(pageNumber)}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 21.dp, vertical = 8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(19) {
                                Text("❧", color = MushafGold.copy(alpha = 0.78f), fontSize = 12.sp)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(19) {
                                Text("❧", color = MushafGold.copy(alpha = 0.78f), fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("۞", color = MushafGold, fontSize = 20.sp)
                        Text(
                            currentSurah?.name ?: "سورة",
                            color = MushafGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "الجزء ${toArabicDigits(currentJuz)}",
                            color = MushafGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text("۞", color = MushafGold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MushafLine))
                    if (showBasmala && basmala.isNotBlank()) {
                        Text(
                            text = basmala,
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 4.dp),
                            color = MushafInk,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 23.sp,
                            lineHeight = 36.sp
                        )
                    }
                    val pageText = remember(pageAyahs, showBasmala, basmala) {
                        buildAnnotatedString {
                            pageAyahs.forEachIndexed { index, item ->
                                val startsNewSurah = item.ayahNumber == 1 &&
                                    item.surahNumber != 1 && item.surahNumber != 9
                                if (startsNewSurah && !(index == 0 && showBasmala)) {
                                    append("\n\n")
                                    pushStyle(
                                        SpanStyle(
                                            color = MushafGreen,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    append("سورة ${item.surahName}")
                                    pop()
                                    append("\n")
                                    pushStyle(
                                        SpanStyle(
                                            color = MushafInk,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    append(basmala)
                                    pop()
                                    append("\n")
                                }
                                append(item.text)
                                append(" ")
                                pushStyle(SpanStyle(color = MushafGold, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                                append("﴿")
                                append(toArabicDigits(item.ayahNumber))
                                append("﴾")
                                pop()
                                if (index != pageAyahs.lastIndex) append("   ")
                            }
                        }
                    }
                    Text(
                        text = pageText,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        color = MushafInk,
                        textAlign = TextAlign.Justify,
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        lineHeight = 42.sp,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.ContentOrRtl
                        )
                    )
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showReaderMenu = false
                        showSurahIndex = true
                    }) { Text("فهرس السور") }
                    TextButton(onClick = {
                        showReaderMenu = false
                        showJuzIndex = true
                    }) { Text("فهرس الأجزاء") }
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

    if (showSurahIndex) {
        AlertDialog(
            onDismissRequest = { showSurahIndex = false },
            title = { Text("فهرس السور") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(surahs, key = { it.number }) { surah ->
                        TextButton(
                            onClick = {
                                moveToPage(
                                    MushafPageRepository.findPageNumber(
                                        pageMap.pages, surah.number, 1
                                    )
                                )
                                showSurahIndex = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${toArabicDigits(surah.number)}. ${surah.name}  •  ${toArabicDigits(surah.ayahs.size)} آية",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurahIndex = false }) { Text("إغلاق") }
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
                            Text(
                                "الجزء ${toArabicDigits(juz.index)}  •  ${juzSurah?.name.orEmpty()} ${toArabicDigits(juz.ayah)}",
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
                                SearchResultRow(result) {
                                    moveToPage(
                                        MushafPageRepository.findPageNumber(
                                            pageMap.pages, result.surahNumber, result.ayahNumber
                                        )
                                    )
                                    showSearchDialog = false
                                    searchInput = ""
                                }
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFFF4EFE4), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "${result.surahName} • الآية ${toArabicDigits(result.ayahNumber)}",
            color = MushafGreen,
            fontWeight = FontWeight.Bold
        )
        Text(result.text, color = MushafInk, textAlign = TextAlign.End, maxLines = 2)
    }
}

private fun toArabicDigits(number: Int): String = number.toString().map { digit ->
    if (digit in '0'..'9') (digit.code - '0'.code + '٠'.code).toChar() else digit
}.joinToString("")
