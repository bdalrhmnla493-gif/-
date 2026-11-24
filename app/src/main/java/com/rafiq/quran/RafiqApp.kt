package com.rafiq.quran

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.rafiq.quran.data.MushafPageMetadata
import com.rafiq.quran.data.MushafPageRepository
import com.rafiq.quran.data.QuranSearch
import com.rafiq.quran.data.QuranSurah
import com.rafiq.quran.domain.TasbeehCounter
import com.rafiq.quran.ui.theme.RafiqTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        var readerFullScreen by rememberSaveable { mutableStateOf(false) }
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
                if (tab != AppTab.HOME && tab != AppTab.QURAN) {
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
                if (!readerFullScreen) {
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
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    AppTab.HOME -> HomePage(
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
                        readerFullScreen = readerFullScreen,
                        onReaderFullScreenChange = { readerFullScreen = it },
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
    val cairoCalendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Cairo"), Locale("ar", "EG")).apply {
        timeInMillis = nowMillis
    }
    val dayOfYear = cairoCalendar.get(Calendar.DAY_OF_YEAR)
    val dailyDhikr = dhikrPhrases[(dayOfYear - 1) % dhikrPhrases.size]
    val isFriday = cairoCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

    val baseTypography = MaterialTheme.typography
    val homeTypography = remember(baseTypography) {
        baseTypography.copy(
            displayLarge = baseTypography.displayLarge.copy(fontFamily = HomeArabicFont),
            displayMedium = baseTypography.displayMedium.copy(fontFamily = HomeArabicFont),
            displaySmall = baseTypography.displaySmall.copy(fontFamily = HomeArabicFont),
            headlineLarge = baseTypography.headlineLarge.copy(fontFamily = HomeArabicFont),
            headlineMedium = baseTypography.headlineMedium.copy(fontFamily = HomeArabicFont),
            headlineSmall = baseTypography.headlineSmall.copy(fontFamily = HomeArabicFont),
            titleLarge = baseTypography.titleLarge.copy(fontFamily = HomeArabicFont),
            titleMedium = baseTypography.titleMedium.copy(fontFamily = HomeArabicFont),
            titleSmall = baseTypography.titleSmall.copy(fontFamily = HomeArabicFont),
            bodyLarge = baseTypography.bodyLarge.copy(fontFamily = HomeArabicFont),
            bodyMedium = baseTypography.bodyMedium.copy(fontFamily = HomeArabicFont),
            bodySmall = baseTypography.bodySmall.copy(fontFamily = HomeArabicFont),
            labelLarge = baseTypography.labelLarge.copy(fontFamily = HomeArabicFont),
            labelMedium = baseTypography.labelMedium.copy(fontFamily = HomeArabicFont),
            labelSmall = baseTypography.labelSmall.copy(fontFamily = HomeArabicFont)
        )
    }
    MaterialTheme(typography = homeTypography) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF4DF),
                    Color(0xFFFFF9EF),
                    Color(0xFFF4E7CC)
                )
            )
        )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    HomeSectionTitle("ذكر اليوم")
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, HomeGold, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = HomeCream)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp
                                )
                                Text(
                                    "ذكر عام من القائمة المحلية، دون نسبته إلى حديث",
                                    color = HomeMuted,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                                Text(
                                    "عدد التكرار حسب هدفك الشخصي",
                                    color = HomeGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(onClick = onOpenAdhkar) {
                                    Text("افتح الأذكار", color = HomeGreen)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    HomeSectionTitle("رحلتك وإنجازاتك")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        HomeStatusCard(
                            "🔥", "سلسلة الاستمرار", "غير مسجلة بعد",
                            Modifier.weight(1f)
                        ) { onFeature("سلسلة الاستمرار") }
                        HomeStatusCard(
                            "🏆", "إنجازاتي", "لا توجد بيانات بعد",
                            Modifier.weight(1f)
                        ) { onFeature("الإنجازات") }
                        HomeStatusCard(
                            "🪙", "نقاطي", "غير مفعّلة",
                            Modifier.weight(1f)
                        ) { onFeature("النقاط") }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    HomeSectionTitle("المميزات السريعة")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        HomeFeatureCard("🎧", "مشغل القرآن", "", Modifier.weight(1f)) {
                            onFeature("مشغل القرآن")
                        }
                        HomeFeatureCard("📖", "ختمتي ورحلتي", "", Modifier.weight(1f)) {
                            onFeature("ختمتي ورحلتي")
                        }
                        HomeFeatureCard("🧭", "القبلة", "", Modifier.weight(1f)) {
                            onFeature("القبلة")
                        }
                        HomeFeatureCard("🌿", "تدبر آية", "", Modifier.weight(1f)) {
                            onFeature("تدبر آية")
                        }
                        HomeFeatureCard("📿", "السبحة الذكية", "", Modifier.weight(1f), onOpenTasbeeh)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenQuran)
                        .border(1.dp, HomeGold, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = HomeCream)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("متابعة القراءة", color = HomeGreen, fontWeight = FontWeight.Bold)
                            Text(
                                if (savedSurah in 1..114) {
                                    "آخر موضع محفوظ: السورة $savedSurah • الآية $savedAyah"
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
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    HomeSectionTitle("محتوى مميز يوميًا")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        HomeFeatureCard("🌙", "أذكار النوم", "غير متاحة بعد", Modifier.weight(1f)) {
                            onFeature("أذكار النوم")
                        }
                        HomeFeatureCard("✨", "قيام الليل", "غير متاح بعد", Modifier.weight(1f)) {
                            onFeature("قيام الليل")
                        }
                        HomeFeatureCard(
                            "🕌",
                            "أذكار الجمعة",
                            if (isFriday) "اليوم الجمعة" else "غير متاحة بعد",
                            Modifier.weight(1f)
                        ) {
                            onFeature("أذكار الجمعة")
                        }
                    }
                }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        "مواقيت الصلاة والطقس يحتاجان اتصالًا لتحديث البيانات.",
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = HomeMuted,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
    }
}
private val HomeArabicFont = FontFamily(
    Font(R.font.noto_kufi_arabic_variable, FontWeight.Normal)
)

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
            .height(208.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, HomeGold, RoundedCornerShape(26.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.kaaba_home_header),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.BottomCenter
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeHeaderAction(symbol = "🔔", onClick = onNotifications)
            Column(
                Modifier.weight(1f).padding(horizontal = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "رفيق القرآن",
                    color = HomeGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "نور في قلبك .. وطمأنينة في حياتك",
                    color = HomeGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (weather != null) {
                    Text(
                        "${weather.temperatureCelsius.toInt()}° م • ${weather.description}",
                        color = HomeGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
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
    Box(
        Modifier.clip(CircleShape)
            .background(HomeCream)
            .border(1.2.dp, HomeGold, CircleShape)
            .clickable(onClick = onClick)
            .padding(9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = HomeGreen, fontSize = 23.sp)
    }
}

@Composable
private fun HomeHeaderLogo() {
    Box(
        Modifier.size(54.dp).clip(CircleShape)
            .background(HomeCream)
            .border(1.4.dp, HomeGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_rafiq_logo),
            contentDescription = "شعار رفيق القرآن",
            modifier = Modifier.size(42.dp)
        )
    }
}
@Composable
private fun HomeSectionTitle(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("❧", color = HomeGold, fontSize = 21.sp)
        Box(
            Modifier.clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF234E36), HomeGreen, Color(0xFF234E36))
                    )
                )
                .border(1.dp, HomeGold, RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text("❧", color = HomeGold, fontSize = 21.sp)
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
        modifier = modifier.height(86.dp).clickable(onClick = onClick)
            .border(1.dp, HomeGold, RoundedCornerShape(17.dp)),
        shape = RoundedCornerShape(17.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCream)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                title,
                color = HomeGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
            Text(
                status,
                color = HomeMuted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
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
        modifier = modifier.height(106.dp).clickable(onClick = onClick)
            .border(1.dp, HomeGold, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCream)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 21.sp)
            Text(
                title,
                color = HomeGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 2
            )
            Text(
                subtitle,
                color = HomeMuted,
                fontSize = 7.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MascotIllustration() {
    Canvas(Modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFF3E9D6), radius = w * 0.49f)
        drawCircle(
            Color(0xFFD8C29A).copy(alpha = 0.28f),
            radius = w * 0.34f,
            center = Offset(w * 0.50f, h * 0.91f)
        )

        val robe = Path().apply {
            moveTo(w * 0.13f, h * 0.96f)
            cubicTo(w * 0.16f, h * 0.68f, w * 0.32f, h * 0.57f, w * 0.50f, h * 0.59f)
            cubicTo(w * 0.68f, h * 0.57f, w * 0.84f, h * 0.68f, w * 0.87f, h * 0.96f)
            close()
        }
        drawPath(robe, Color(0xFF315A40))
        drawPath(
            Path().apply {
                moveTo(w * 0.22f, h * 0.91f)
                cubicTo(w * 0.28f, h * 0.70f, w * 0.40f, h * 0.65f, w * 0.47f, h * 0.66f)
                lineTo(w * 0.43f, h * 0.95f)
                close()
            },
            Color(0xFF477657)
        )

        // Soft face outline, ears and warm skin shading.
        drawCircle(Color(0xFFD8A777), w * 0.205f, Offset(w * 0.50f, h * 0.405f))
        drawCircle(Color(0xFFEFC49E), w * 0.183f, Offset(w * 0.50f, h * 0.405f))
        drawCircle(Color(0xFFE7B78F), w * 0.038f, Offset(w * 0.315f, h * 0.43f))
        drawCircle(Color(0xFFE7B78F), w * 0.038f, Offset(w * 0.685f, h * 0.43f))
        drawCircle(Color.White, w * 0.032f, Offset(w * 0.425f, h * 0.405f))
        drawCircle(Color.White, w * 0.032f, Offset(w * 0.575f, h * 0.405f))
        drawCircle(Color(0xFF49372B), w * 0.018f, Offset(w * 0.425f, h * 0.405f))
        drawCircle(Color(0xFF49372B), w * 0.018f, Offset(w * 0.575f, h * 0.405f))
        drawCircle(Color(0xFFFFD2C2).copy(alpha = 0.9f), w * 0.035f, Offset(w * 0.37f, h * 0.475f))
        drawCircle(Color(0xFFFFD2C2).copy(alpha = 0.9f), w * 0.035f, Offset(w * 0.63f, h * 0.475f))
        drawLine(Color(0xFF9A5F4E), Offset(w * 0.465f, h * 0.515f), Offset(w * 0.535f, h * 0.515f), w * 0.012f)

        // Rounded green kufi with a fine gold edge.
        val cap = Path().apply {
            moveTo(w * 0.285f, h * 0.31f)
            cubicTo(w * 0.30f, h * 0.13f, w * 0.39f, h * 0.105f, w * 0.50f, h * 0.105f)
            cubicTo(w * 0.61f, h * 0.105f, w * 0.70f, h * 0.13f, w * 0.715f, h * 0.31f)
            close()
        }
        drawPath(cap, Color(0xFF315A40))
        drawLine(Color(0xFFD7B76F), Offset(w * 0.30f, h * 0.285f), Offset(w * 0.70f, h * 0.285f), w * 0.018f)
        drawCircle(Color(0xFFD7B76F), w * 0.018f, Offset(w * 0.50f, h * 0.145f))

        // Open Quran held close to the body.
        val leftPage = Path().apply {
            moveTo(w * 0.235f, h * 0.705f)
            quadraticTo(w * 0.37f, h * 0.635f, w * 0.49f, h * 0.73f)
            lineTo(w * 0.49f, h * 0.91f)
            quadraticTo(w * 0.35f, h * 0.845f, w * 0.235f, h * 0.875f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(w * 0.51f, h * 0.73f)
            quadraticTo(w * 0.63f, h * 0.635f, w * 0.765f, h * 0.705f)
            lineTo(w * 0.765f, h * 0.875f)
            quadraticTo(w * 0.65f, h * 0.845f, w * 0.51f, h * 0.91f)
            close()
        }
        drawPath(leftPage, Color(0xFFFFF0CE))
        drawPath(rightPage, Color(0xFFFFF8E8))
        drawLine(Color(0xFFB88B3E), Offset(w * 0.50f, h * 0.715f), Offset(w * 0.50f, h * 0.925f), w * 0.02f)
        drawLine(Color(0xFFD9B86F), Offset(w * 0.29f, h * 0.755f), Offset(w * 0.43f, h * 0.78f), w * 0.012f)
        drawLine(Color(0xFFD9B86F), Offset(w * 0.57f, h * 0.78f), Offset(w * 0.71f, h * 0.755f), w * 0.012f)
        drawLine(Color(0xFFD9B86F), Offset(w * 0.29f, h * 0.81f), Offset(w * 0.43f, h * 0.835f), w * 0.012f)
        drawLine(Color(0xFFD9B86F), Offset(w * 0.57f, h * 0.835f), Offset(w * 0.71f, h * 0.81f), w * 0.012f)
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
    readerFullScreen: Boolean,
    onReaderFullScreenChange: (Boolean) -> Unit,
    retry: () -> Unit,
    savePosition: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var selectedSurah by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var pendingAyahNumber by rememberSaveable { mutableIntStateOf(savedAyah.coerceAtLeast(1)) }
    var pendingReaderTool by rememberSaveable { mutableStateOf<String?>(null) }
    var pageMetadata by remember { mutableStateOf<MushafPageMetadata?>(null) }

    LaunchedEffect(context) {
        pageMetadata = withContext(Dispatchers.IO) {
            runCatching { MushafPageRepository.load(context) }.getOrNull()
        }
    }

    val allSurahs = surahs ?: emptyList()
    val active = allSurahs.firstOrNull { it.number == selectedSurah }
    val lastSurah = allSurahs.firstOrNull { it.number == savedSurah }
    val lastPage = if (savedSurah in 1..114) {
        pageMetadata?.let {
            MushafPageRepository.findPageNumber(it.pages, savedSurah, savedAyah.coerceAtLeast(1))
        }
    } else null
    val lastJuz = if (savedSurah in 1..114) {
        pageMetadata?.let {
            MushafPageRepository.findJuzNumber(it.juz, savedSurah, savedAyah.coerceAtLeast(1))
        }
    } else null

    fun openReader(surahNumber: Int, ayahNumber: Int = 1, tool: String? = null) {
        selectedSurah = surahNumber.coerceIn(1, 114)
        pendingAyahNumber = ayahNumber.coerceAtLeast(1)
        pendingReaderTool = tool
        savePosition(selectedSurah, pendingAyahNumber)
    }

    if (active != null) {
        MushafReaderPage(
            surahs = allSurahs,
            initialSurah = active.number,
            initialAyah = pendingAyahNumber,
            readerFullScreen = readerFullScreen,
            onBack = {
                selectedSurah = 0
                pendingReaderTool = null
                onReaderFullScreenChange(false)
            },
            onReaderFullScreenChange = onReaderFullScreenChange,
            onSavePosition = savePosition,
            initialTool = pendingReaderTool,
            onInitialToolHandled = { pendingReaderTool = null }
        )
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
        val filtered = allSurahs.filter {
            query.isBlank() ||
                it.name.contains(query.trim()) ||
                it.transliteration.contains(query.trim(), ignoreCase = true) ||
                it.englishName.contains(query.trim(), ignoreCase = true)
        }
        val matchingAyahs = remember(allSurahs, query) {
            QuranSearch.search(allSurahs, query, limit = 100)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "القرآن الكريم",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "مصحفك وقراءتك وفهارسك في مكان واحد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (lastSurah != null) {
                            Text(
                                "آخر موضع قراءة: \u0024{lastSurah.name} • الآية \u0024{savedAyah.coerceAtLeast(1)}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                buildString {
                                    if (lastPage != null) append("الصفحة \u0024lastPage")
                                    if (lastJuz != null) {
                                        if (isNotEmpty()) append(" • ")
                                        append("الجزء \u0024lastJuz")
                                    }
                                },
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { openReader(lastSurah.number, savedAyah.coerceAtLeast(1)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Bookmark, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("متابعة القراءة")
                            }
                        } else {
                            Button(
                                onClick = { openReader(1, 1) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("ابدأ القراءة")
                            }
                        }
                    }
                }
            }

            item {
                Text("الوصول السريع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "فهرس السور",
                        subtitle = "114 سورة",
                        icon = Icons.Filled.MenuBook,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "surahs") }
                    )
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "فهرس الأجزاء",
                        subtitle = "30 جزءًا",
                        icon = Icons.Filled.AutoStories,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "juz") }
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "فهرس الصفحات",
                        subtitle = "604 صفحات",
                        icon = Icons.Filled.Bookmark,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "pages") }
                    )
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "علامات الصفحات",
                        subtitle = "مواضعك المحفوظة",
                        icon = Icons.Filled.Bookmark,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "bookmarks") }
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "علامات الآيات",
                        subtitle = "أسماء وألوان",
                        icon = Icons.Filled.Bookmark,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "ayah-bookmarks") }
                    )
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "الآيات المفضلة",
                        subtitle = "مع ملاحظاتك",
                        icon = Icons.Filled.AutoStories,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "favorites") }
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "البحث في القرآن",
                        subtitle = "بحث دون إنترنت",
                        icon = Icons.Filled.TouchApp,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "search") }
                    )
                    QuranHubAction(
                        modifier = Modifier.weight(1f),
                        title = "إعدادات المصحف",
                        subtitle = "المظهر والقراءة",
                        icon = Icons.Filled.Settings,
                        onClick = { openReader(savedSurah.takeIf { it in 1..114 } ?: 1, savedAyah, "settings") }
                    )
                }
            }
            item {
                Text(
                    "التلاوات والتفسير ومعاني الكلمات قيد التجهيز بعد اعتماد مصادرها وحقوق استخدامها.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ابحث عن سورة أو آية") },
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (query.isNotBlank()) {
                item {
                    Text("نتائج البحث في الآيات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (matchingAyahs.isEmpty()) {
                    item {
                        Text("لا توجد آيات مطابقة لعبارة البحث.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(matchingAyahs, key = { "search-\u0024{it.surahNumber}-\u0024{it.ayahNumber}" }) { result ->
                        Card(
                            Modifier.fillMaxWidth().clickable {
                                openReader(result.surahNumber, result.ayahNumber)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "\u0024{result.surahName} • الآية \u0024{result.ayahNumber}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(result.text, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                            }
                        }
                    }
                }
                if (matchingAyahs.size == 100) {
                    item {
                        Text(
                            "تظهر أول 100 نتيجة. أضف كلمات أكثر لتضييق البحث.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item { Text("السور • \u0024{filtered.size}", fontWeight = FontWeight.Bold) }
            items(filtered, key = { it.number }) { surah ->
                Card(
                    Modifier.fillMaxWidth().clickable { openReader(surah.number, 1) },
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
private fun QuranHubAction(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
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
