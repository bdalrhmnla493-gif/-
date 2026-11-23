package com.rafiq.quran

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rafiq.quran.data.CurrentWeather
import com.rafiq.quran.data.PrayerTimes
import com.rafiq.quran.data.PrayerWeatherRepository
import com.rafiq.quran.data.QuranRepository
import com.rafiq.quran.data.QuranSurah
import com.rafiq.quran.domain.TasbeehCounter
import com.rafiq.quran.ui.theme.RafiqTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val Context.rafiqDataStore by preferencesDataStore(name = "rafiq_preferences")
private val ThemeModeKey = stringPreferencesKey("theme_mode")
private val TasbeehCountKey = intPreferencesKey("tasbeeh_count")
private val TasbeehPhraseKey = stringPreferencesKey("tasbeeh_phrase")
private val LastReadSurahKey = intPreferencesKey("last_read_surah")
private val LastReadAyahKey = intPreferencesKey("last_read_ayah")
private val SelectedLocationNameKey = stringPreferencesKey("selected_location_name")
private val SelectedGovernorateIdKey = stringPreferencesKey("selected_governorate_id")
private val SelectedLatitudeKey = stringPreferencesKey("selected_location_latitude")
private val SelectedLongitudeKey = stringPreferencesKey("selected_location_longitude")

private val dhikrPhrases = listOf(
    "سُبْحَانَ اللَّهِ",
    "الْحَمْدُ لِلَّهِ",
    "اللَّهُ أَكْبَرُ",
    "لَا إِلَهَ إِلَّا اللَّهُ",
    "أَسْتَغْفِرُ اللَّهَ"
)

private enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Filled.Home),
    QURAN("القرآن", Icons.Filled.MenuBook),
    ADHKAR("الأذكار", Icons.Filled.Spa),
    TASBEEH("السبحة", Icons.Filled.TouchApp),
    PRAYER("الصلاة", Icons.Filled.AccessTime),
    MORE("المزيد", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RafiqApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences by context.rafiqDataStore.data.collectAsState(initial = emptyPreferences())
    val themeMode = preferences[ThemeModeKey] ?: "system"
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    RafiqTheme(darkTheme = darkTheme) {
        var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
        var surahs by remember { mutableStateOf<List<QuranSurah>?>(null) }
        var quranLoading by remember { mutableStateOf(false) }
        var quranError by remember { mutableStateOf<String?>(null) }
        var retryCount by remember { mutableIntStateOf(0) }
        var infoMessage by remember { mutableStateOf<String?>(null) }

        val tasbeehCount = preferences[TasbeehCountKey] ?: 0
        val phrase = preferences[TasbeehPhraseKey] ?: dhikrPhrases.first()
        val savedSurah = preferences[LastReadSurahKey] ?: 0
        val savedAyah = preferences[LastReadAyahKey] ?: 1
        val savedLocationName = preferences[SelectedLocationNameKey] ?: ""
        val savedGovernorateId = preferences[SelectedGovernorateIdKey] ?: ""
        val selectedLatitude = preferences[SelectedLatitudeKey]?.toDoubleOrNull()
        val selectedLongitude = preferences[SelectedLongitudeKey]?.toDoubleOrNull()

        var prayerTimes by remember { mutableStateOf<PrayerTimes?>(null) }
        var currentWeather by remember { mutableStateOf<CurrentWeather?>(null) }
        var prayerLoading by remember { mutableStateOf(false) }
        var prayerError by remember { mutableStateOf<String?>(null) }
        var prayerRefreshCount by remember { mutableIntStateOf(0) }
        var lastPrayerRequestKey by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(tab, selectedLatitude, selectedLongitude, prayerRefreshCount) {
            if (tab == AppTab.HOME || tab == AppTab.PRAYER) {
                if (selectedLatitude != null && selectedLongitude != null) {
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("Africa/Cairo")
                    }.format(Date())
                    val requestKey = "$selectedLatitude|$selectedLongitude|$todayKey|$prayerRefreshCount"
                    if (lastPrayerRequestKey != requestKey) {
                        lastPrayerRequestKey = requestKey
                        prayerLoading = true
                        prayerError = null
                        prayerTimes = null
                        currentWeather = null
                        var completed = false
                        try {
                            val results = coroutineScope {
                                val prayerTask = async {
                                    try {
                                        Result.success(
                                            PrayerWeatherRepository.fetchPrayerTimes(selectedLatitude, selectedLongitude)
                                        )
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        Result.failure(error)
                                    }
                                }
                                val weatherTask = async {
                                    try {
                                        Result.success(
                                            PrayerWeatherRepository.fetchCurrentWeather(selectedLatitude, selectedLongitude)
                                        )
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        Result.failure(error)
                                    }
                                }
                                prayerTask.await() to weatherTask.await()
                            }
                            val prayerResult = results.first
                            val weatherResult = results.second
                            prayerTimes = prayerResult.getOrNull()
                            currentWeather = weatherResult.getOrNull()
                            prayerError = when {
                                prayerResult.isFailure ->
                                    "تعذر تحميل مواقيت الصلاة. تحقق من الإنترنت ثم حاول التحديث."
                                weatherResult.isFailure ->
                                    "تم تحميل المواقيت، لكن الطقس غير متاح حاليًا."
                                else -> null
                            }
                            completed = true
                        } finally {
                            prayerLoading = false
                            if (!completed) lastPrayerRequestKey = null
                        }
                    }
                } else {
                    prayerTimes = null
                    currentWeather = null
                    prayerError = null
                }
            }
        }

        LaunchedEffect(tab, retryCount) {
            if (tab == AppTab.QURAN && surahs == null && !quranLoading) {
                quranLoading = true
                quranError = null
                try {
                    surahs = QuranRepository.load(context)
                } catch (_: Exception) {
                    quranError = "تعذر تحميل المصحف. تأكد من ملفات القرآن ثم أعد المحاولة."
                } finally {
                    quranLoading = false
                }
            }
        }

        fun savePreference(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
            scope.launch { context.rafiqDataStore.edit { it[key] = value } }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (tab != AppTab.HOME) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("رفيق القرآن", fontWeight = FontWeight.Bold)
                                Text(
                                    tab.title,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = HomeGreen,
                    contentColor = Color.White
                ) {
                    listOf(AppTab.HOME, AppTab.QURAN, AppTab.ADHKAR, AppTab.PRAYER, AppTab.MORE).forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            alwaysShowLabel = true,
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFFD36F),
                                selectedTextColor = Color(0xFFFFD36F),
                                indicatorColor = Color(0xFF315A40),
                                unselectedIconColor = Color(0xFFF2EAD8),
                                unselectedTextColor = Color(0xFFF2EAD8)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    AppTab.HOME -> HomePage(
                        count = tasbeehCount,
                        savedSurah = savedSurah,
                        savedAyah = savedAyah,
                        locationName = savedLocationName,
                        prayerTimes = prayerTimes,
                        weather = currentWeather,
                        prayerLoading = prayerLoading,
                        prayerError = prayerError,
                        onOpenQuran = { tab = AppTab.QURAN },
                        onOpenAdhkar = { tab = AppTab.ADHKAR },
                        onOpenTasbeeh = { tab = AppTab.TASBEEH },
                        onOpenPrayer = { tab = AppTab.PRAYER },
                        onFeature = { infoMessage = it }
                    )
                    AppTab.QURAN -> QuranPage(
                        surahs = surahs,
                        loading = quranLoading,
                        error = quranError,
                        savedSurah = savedSurah,
                        savedAyah = savedAyah,
                        retry = { retryCount++ },
                        savePosition = { surah, ayah ->
                            scope.launch {
                                context.rafiqDataStore.edit {
                                    it[LastReadSurahKey] = surah
                                    it[LastReadAyahKey] = ayah
                                }
                            }
                        }
                    )
                    AppTab.ADHKAR -> AdhkarPage(
                        selected = phrase,
                        onChoose = { chosen ->
                            scope.launch {
                                context.rafiqDataStore.edit {
                                    it[TasbeehPhraseKey] = chosen
                                    it[TasbeehCountKey] = 0
                                }
                            }
                            tab = AppTab.TASBEEH
                        }
                    )
                    AppTab.TASBEEH -> TasbeehPage(
                        count = tasbeehCount,
                        phrase = phrase,
                        onIncrement = {
                            scope.launch {
                                context.rafiqDataStore.edit {
                                    it[TasbeehCountKey] = TasbeehCounter.increment(it[TasbeehCountKey] ?: 0)
                                }
                            }
                        },
                        onReset = {
                            scope.launch { context.rafiqDataStore.edit { it[TasbeehCountKey] = 0 } }
                        },
                        onSelect = { chosen ->
                            scope.launch {
                                context.rafiqDataStore.edit {
                                    it[TasbeehPhraseKey] = chosen
                                    it[TasbeehCountKey] = 0
                                }
                            }
                        }
                    )
                    AppTab.PRAYER -> PrayerPage(
                        locationName = savedLocationName,
                        savedGovernorateId = savedGovernorateId,
                        latitude = selectedLatitude,
                        longitude = selectedLongitude,
                        prayerTimes = prayerTimes,
                        weather = currentWeather,
                        loading = prayerLoading,
                        errorMessage = prayerError,
                        onRefresh = { prayerRefreshCount++ },
                        onSaveLocation = { name, latitude, longitude, governorateId ->
                            scope.launch {
                                context.rafiqDataStore.edit {
                                    it[SelectedLocationNameKey] = name
                                    it[SelectedLatitudeKey] = String.format(Locale.US, "%.6f", latitude)
                                    it[SelectedLongitudeKey] = String.format(Locale.US, "%.6f", longitude)
                                    if (governorateId.isBlank()) {
                                        it.remove(SelectedGovernorateIdKey)
                                    } else {
                                        it[SelectedGovernorateIdKey] = governorateId
                                    }
                                }
                            }
                        }
                    )
                    AppTab.MORE -> MorePage(
                        themeMode = themeMode,
                        onTheme = { savePreference(ThemeModeKey, it) },
                        onFeature = { infoMessage = it }
                    )
                }
            }
        }

        if (infoMessage != null) {
            AlertDialog(
                onDismissRequest = { infoMessage = null },
                title = { Text(infoMessage ?: "قسم") },
                text = {
                    Text("القسم ده قيد التطوير. لن نعرض مواقيت أو معلومات دينية غير موثقة.")
                },
                confirmButton = {
                    TextButton(onClick = { infoMessage = null }) { Text("حسنًا") }
                }
            )
        }
    }
}

@Composable

private fun HomePage(
    count: Int,
    savedSurah: Int,
    savedAyah: Int,
    locationName: String,
    prayerTimes: PrayerTimes?,
    weather: CurrentWeather?,
    prayerLoading: Boolean,
    prayerError: String?,
    onOpenQuran: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenTasbeeh: () -> Unit,
    onOpenPrayer: () -> Unit,
    onFeature: (String) -> Unit
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000)
        }
    }
    val dayOfYear = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.DAY_OF_YEAR)
    val dailyDhikr = dhikrPhrases[(dayOfYear - 1) % dhikrPhrases.size]
    val isFriday = Calendar.getInstance().apply { timeInMillis = nowMillis }
        .get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            HomeHeader(
                locationName = locationName,
                weather = weather,
                onNotifications = { onFeature("الإشعارات") }
            )
        }
        item {
            PrayerSummaryCard(
                locationName = locationName,
                prayerTimes = prayerTimes,
                loading = prayerLoading,
                errorMessage = prayerError,
                onOpenPrayer = onOpenPrayer
            )
        }
        item {
            HomeSectionTitle("ذكر اليوم")
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, HomeGold, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = HomeCream)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MascotIllustration()
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            dailyDhikr,
                            color = HomeGreen,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "عبارة تسبيح عامة من القائمة المحلية — وليست معروضة كحديث أو ذكر مُخرَّج",
                            color = HomeMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                        TextButton(onClick = onOpenAdhkar) {
                            Text("افتح الأذكار", color = HomeGreen)
                        }
                    }
                }
            }
        }
        item {
            HomeSectionTitle("الاستمرار والإنجازات")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeStatusCard("🔥", "سلسلة الاستمرار", "قيد التجهيز", Modifier.weight(1f)) {
                    onFeature("سلسلة الاستمرار")
                }
                HomeStatusCard("🏆", "إنجازاتي", "غير متاحة بعد", Modifier.weight(1f)) {
                    onFeature("الإنجازات")
                }
                HomeStatusCard("🪙", "نقاطي", "غير مفعّلة", Modifier.weight(1f)) {
                    onFeature("النقاط")
                }
            }
        }
        item {
            HomeSectionTitle("المميزات السريعة")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeFeatureCard("🎧", "مشغل القرآن", "قريبًا", Modifier.weight(1f)) {
                        onFeature("مشغل القرآن")
                    }
                    HomeFeatureCard("📖", "ختمتي ورحلتي", "قريبًا", Modifier.weight(1f)) {
                        onFeature("ختمتي ورحلتي")
                    }
                    HomeFeatureCard("🧭", "القبلة", "قريبًا", Modifier.weight(1f)) {
                        onFeature("القبلة")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeFeatureCard("🌿", "تدبر آية", "قريبًا", Modifier.weight(1f)) {
                        onFeature("تدبر آية")
                    }
                    HomeFeatureCard("📿", "السبحة الذكية", "${count} تسبيحة محفوظة", Modifier.weight(1f), onOpenTasbeeh)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenQuran)
                    .border(1.dp, HomeGold, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HomeCream)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("متابعة القراءة", color = HomeGreen, fontWeight = FontWeight.Bold)
                        Text(
                            if (savedSurah in 1..114) {
                                "آخر موضع محفوظ: السورة ${savedSurah} • الآية ${savedAyah}"
                            } else {
                                "لم يتم حفظ موضع قراءة بعد"
                            },
                            color = HomeMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        if (savedSurah in 1..114) "استكمل القراءة  ←" else "افتح المصحف  ←",
                        color = HomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            HomeSectionTitle("محتوى مميز يوميًا")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeFeatureCard("🌙", "أذكار النوم", "قريبًا", Modifier.weight(1f)) {
                    onFeature("أذكار النوم")
                }
                HomeFeatureCard("✨", "قيام الليل", "قريبًا", Modifier.weight(1f)) {
                    onFeature("قيام الليل")
                }
                HomeFeatureCard(
                    "🕌",
                    "أذكار الجمعة",
                    if (isFriday) "اليوم الجمعة" else "قريبًا",
                    Modifier.weight(1f)
                ) {
                    onFeature("أذكار الجمعة")
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFEAF0E8))
                    .border(1.dp, HomeGold, RoundedCornerShape(30.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓", color = HomeGreen, fontWeight = FontWeight.Bold)
                Text(
                    "  المصحف والسبحة الأساسية متاحان دون إنترنت",
                    color = HomeGreen,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                "مواقيت الصلاة تحتاج اتصالًا لتحديث بياناتها.",
                Modifier.fillMaxWidth().padding(top = 3.dp),
                color = HomeMuted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private val HomeGreen = Color(0xFF19432F)
private val HomeGold = Color(0xFFD5AD59)
private val HomeCream = Color(0xFFFFF8EA)
private val HomeMuted = Color(0xFF776F60)

@Composable
private fun HomeHeader(
    locationName: String,
    weather: CurrentWeather?,
    onNotifications: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(26.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFE7BE), Color(0xFFFFF8E9), Color(0xFFE5D6B5))
                ),
                size = size
            )
            drawCircle(Color(0xFFFFF0C7), radius = h * 0.14f, center = Offset(w * 0.19f, h * 0.37f))
            val baseY = h * 0.83f
            drawRect(Color(0xFFD9C8A7), topLeft = Offset(0f, baseY), size = Size(w, h - baseY))

            fun dome(centerX: Float, width: Float, height: Float, color: Color) {
                val path = Path().apply {
                    moveTo(centerX - width / 2f, baseY)
                    cubicTo(
                        centerX - width * 0.48f, baseY - height * 0.68f,
                        centerX - width * 0.22f, baseY - height,
                        centerX, baseY - height
                    )
                    cubicTo(
                        centerX + width * 0.22f, baseY - height,
                        centerX + width * 0.48f, baseY - height * 0.68f,
                        centerX + width / 2f, baseY
                    )
                    close()
                }
                drawPath(path, color)
            }

            fun minaret(x: Float, height: Float, color: Color) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - w * 0.012f, baseY - height),
                    size = Size(w * 0.024f, height),
                    cornerRadius = CornerRadius(w * 0.01f)
                )
                drawCircle(color, radius = w * 0.025f, center = Offset(x, baseY - height))
                drawCircle(Color(0xFFC49B4F), radius = w * 0.009f, center = Offset(x, baseY - height - w * 0.03f))
            }

            val stone = Color(0xFFB9A887)
            drawRoundRect(
                color = Color(0xFFD2C2A2),
                topLeft = Offset(w * 0.22f, h * 0.61f),
                size = Size(w * 0.56f, h * 0.24f),
                cornerRadius = CornerRadius(w * 0.025f)
            )
            dome(w * 0.50f, w * 0.23f, h * 0.31f, stone)
            dome(w * 0.29f, w * 0.12f, h * 0.18f, Color(0xFFC8B796))
            dome(w * 0.71f, w * 0.12f, h * 0.18f, Color(0xFFC8B796))
            dome(w * 0.13f, w * 0.08f, h * 0.12f, Color(0xFFD0C09F))
            dome(w * 0.87f, w * 0.08f, h * 0.12f, Color(0xFFD0C09F))
            minaret(w * 0.23f, h * 0.55f, Color(0xFFB8A784))
            minaret(w * 0.77f, h * 0.55f, Color(0xFFB8A784))
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeHeaderAction(
                symbol = "🔔",
                onClick = onNotifications
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "رفيق القرآن",
                    color = HomeGreen,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "نور في قلبك .. وطمأنينة في حياتك",
                    color = HomeGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                if (weather != null) {
                    Text(
                        "${weather.temperatureCelsius.toInt()}° م • ${weather.description}",
                        color = HomeGreen,
                        fontSize = 10.sp
                    )
                } else {
                    Text(
                        locationName.ifBlank { "اختر مدينتك لمواقيت الصلاة" },
                        color = HomeMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            HomeHeaderLogo()
        }
    }
}

@Composable
private fun HomeHeaderAction(symbol: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(CircleShape)
            .background(HomeCream)
            .border(1.dp, HomeGold, CircleShape)
            .clickable(onClick = onClick)
            .padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(symbol, color = HomeGreen, fontSize = 23.sp)
    }
}

@Composable
private fun HomeHeaderLogo() {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(HomeCream)
            .border(1.dp, HomeGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("۞", color = HomeGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeSectionTitle(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("❧", color = HomeGold, fontSize = 22.sp)
        Box(
            Modifier.clip(RoundedCornerShape(30.dp))
                .background(HomeGreen)
                .border(1.dp, HomeGold, RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp, vertical = 7.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text("❧", color = HomeGold, fontSize = 22.sp)
    }
}

@Composable
private fun HomeStatusCard(
    icon: String,
    title: String,
    status: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(104.dp).clickable(onClick = onClick)
            .border(1.dp, HomeGold, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCream)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 21.sp)
            Text(title, color = HomeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(status, color = HomeMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HomeFeatureCard(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(112.dp).clickable(onClick = onClick)
            .border(1.dp, HomeGold, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCream)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 25.sp)
            Text(
                title,
                color = HomeGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
            Text(subtitle, color = HomeMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun MascotIllustration() {
    Canvas(Modifier.size(70.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFF1E4C8), radius = w * 0.48f)
        drawRoundRect(
            color = Color(0xFF416B4C),
            topLeft = Offset(w * 0.23f, h * 0.54f),
            size = Size(w * 0.54f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.15f)
        )
        drawCircle(Color(0xFFE9B989), radius = w * 0.22f, center = Offset(w * 0.50f, h * 0.38f))
        drawRoundRect(
            color = Color(0xFFFFFEF7),
            topLeft = Offset(w * 0.29f, h * 0.16f),
            size = Size(w * 0.42f, h * 0.15f),
            cornerRadius = CornerRadius(w * 0.12f)
        )
        drawCircle(Color(0xFF24372A), radius = w * 0.018f, center = Offset(w * 0.42f, h * 0.39f))
        drawCircle(Color(0xFF24372A), radius = w * 0.018f, center = Offset(w * 0.58f, h * 0.39f))
        drawRoundRect(
            color = Color(0xFFB78632),
            topLeft = Offset(w * 0.32f, h * 0.65f),
            size = Size(w * 0.36f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.025f)
        )
        drawLine(
            color = Color(0xFFFFF5D8),
            start = Offset(w * 0.50f, h * 0.66f),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = w * 0.015f
        )
    }
}


@Composable
private fun QuickCard(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.height(108.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun QuranPage(
    surahs: List<QuranSurah>?,
    loading: Boolean,
    error: String?,
    savedSurah: Int,
    savedAyah: Int,
    retry: () -> Unit,
    savePosition: (Int, Int) -> Unit
) {
    var selectedSurah by rememberSaveable { mutableIntStateOf(savedSurah) }
    var query by rememberSaveable { mutableStateOf("") }
    var ayahQuery by rememberSaveable { mutableStateOf("") }
    var readerTone by rememberSaveable { mutableStateOf("paper") }
    val context = LocalContext.current
    val active = surahs?.firstOrNull { it.number == selectedSurah }

    if (active != null) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { selectedSurah = 0 }) { Text("قائمة السور") }
                Column(horizontalAlignment = Alignment.End) {
                    Text(active.name, fontWeight = FontWeight.Bold)
                    Text(
                        active.revelationType + " • " + active.ayahs.size + " آية",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = readerTone == "paper", onClick = { readerTone = "paper" }, label = { Text("ورقي") })
                FilterChip(selected = readerTone == "white", onClick = { readerTone = "white" }, label = { Text("أبيض") })
                FilterChip(selected = readerTone == "dark", onClick = { readerTone = "dark" }, label = { Text("ليلي") })
            }
            OutlinedTextField(
                value = ayahQuery,
                onValueChange = { ayahQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                singleLine = true,
                label = { Text("بحث داخل السورة") },
                shape = RoundedCornerShape(14.dp)
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(active.ayahs.filter { ayahQuery.isBlank() || it.text.contains(ayahQuery.trim()) }, key = { it.number }) { ayah ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (readerTone) {
                                "dark" -> Color(0xFF17211B)
                                "white" -> Color.White
                                else -> Color(0xFFF7F3E8)
                            }
                        )
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                ayah.text,
                                Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = if (readerTone == "dark") Color(0xFFF1F1E8) else Color(0xFF26352B),
                                style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 44.sp)
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("آية " + ayah.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { savePosition(active.number, ayah.number) }) {
                                        Text("حفظ الموضع")
                                    }
                                    TextButton(onClick = {
                                        val shareText = ayah.text + "\n" + active.name + " • الآية " + ayah.number
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الآية"))
                                    }) {
                                        Text("مشاركة")
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "النص العثماني من مشروع Tanzil Project.",
                        Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    } else if (loading || (surahs == null && error == null)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("جاري تجهيز المصحف...")
            }
        }
    } else if (error != null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = retry) { Text("إعادة المحاولة") }
        }
    } else {
        val allSurahs = surahs ?: emptyList()
        val filtered = allSurahs.filter {
            query.isBlank() ||
                it.name.contains(query.trim()) ||
                it.transliteration.contains(query.trim(), ignoreCase = true) ||
                it.englishName.contains(query.trim(), ignoreCase = true)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ابحث عن سورة") },
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (savedSurah in 1..114) {
                val last = allSurahs.firstOrNull { it.number == savedSurah }
                if (last != null) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().clickable { selectedSurah = last.number },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("متابعة القراءة", fontWeight = FontWeight.Bold)
                                Text(last.name + " • الآية " + savedAyah)
                            }
                        }
                    }
                }
            }
            item { Text("السور • " + filtered.size, fontWeight = FontWeight.Bold) }
            items(filtered, key = { it.number }) { surah ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        selectedSurah = surah.number
                        savePosition(surah.number, 1)
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(surah.name, fontWeight = FontWeight.Bold)
                            Text(
                                surah.transliteration + " • " + surah.ayahs.size + " آية",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(surah.number.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdhkarPage(selected: String, onChoose: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("أذكار وتسبيح عام", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "اختَر الذكر لفتحه في السبحة. دي قائمة تسبيح عامة وليست مكتبة أذكار مأثورة كاملة.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(dhikrPhrases) { phrase ->
            Card(
                Modifier.fillMaxWidth().clickable { onChoose(phrase) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == phrase) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(phrase, fontWeight = FontWeight.SemiBold)
                    Text("ابدأ", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TasbeehPage(
    count: Int,
    phrase: String,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onSelect: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("السبحة الإلكترونية", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(phrase, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Box(
            Modifier.size(232.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(8.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Text("اضغط للتسبيح", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onIncrement) { Text("تسبيحة") }
            TextButton(onClick = onReset) { Text("تصفير العداد") }
        }
        Text("اختر الذكر", Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
        dhikrPhrases.forEach { option ->
            FilterChip(
                selected = phrase == option,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}

@Composable
private fun MorePage(
    themeMode: String,
    onTheme: (String) -> Unit,
    onFeature: (String) -> Unit
) {
    val sections = listOf(
        "مواقيت الصلاة",
        "اتجاه القبلة",
        "الحديث الشريف",
        "المكتبة الإسلامية",
        "التقويم الهجري",
        "المفضلة والتنبيهات"
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("الإعدادات والمزيد", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مظهر التطبيق", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(themeMode == "system", { onTheme("system") }, label = { Text("النظام") })
                        FilterChip(themeMode == "light", { onTheme("light") }, label = { Text("فاتح") })
                        FilterChip(themeMode == "dark", { onTheme("dark") }, label = { Text("داكن") })
                    }
                }
            }
        }
        item { Text("أقسام قيد التجهيز", fontWeight = FontWeight.Bold) }
        items(sections) { name ->
            Card(
                Modifier.fillMaxWidth().clickable { onFeature(name) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(17.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontWeight = FontWeight.SemiBold)
                    Text("قريبًا", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("مصدر نص القرآن", fontWeight = FontWeight.Bold)
                    Text(
                        "Tanzil Project — النص العثماني مدرج كما ورد دون تعديل. راجع ملف الترخيص المرفق داخل التطبيق.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("رفيق القرآن • إصدار تجريبي 0.2.0", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
