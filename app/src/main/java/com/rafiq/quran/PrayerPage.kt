package com.rafiq.quran

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rafiq.quran.data.CurrentWeather
import com.rafiq.quran.data.DeviceLocationProvider
import com.rafiq.quran.data.EgyptAdministrativeArea
import com.rafiq.quran.data.EgyptGovernorate
import com.rafiq.quran.data.EgyptLocationsRepository
import com.rafiq.quran.data.PrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@Composable
internal fun PrayerSummaryCard(
    locationName: String,
    prayerTimes: PrayerTimes?,
    loading: Boolean,
    errorMessage: String?,
    onOpenPrayer: () -> Unit
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(prayerTimes != null) {
        if (prayerTimes != null) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }
    val nextPrayer = prayerTimes?.let { findNextPrayer(it, nowMillis) }
    val gregorianDate = SimpleDateFormat("d MMMM yyyy", Locale("ar", "EG")).format(Date(nowMillis))
    val hijriDate = prayerTimes?.hijriDate ?: "غير متاح حاليًا"

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Card(
            Modifier.fillMaxWidth().clickable(onClick = onOpenPrayer)
                .border(1.dp, Color(0xFFD5AD59), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF19432F))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    "الصلاة القادمة",
                    color = Color(0xFFFFE5AE),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (nextPrayer != null) {
                    Text(
                        nextPrayer.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatCountdown(nextPrayer.remainingMillis),
                        color = Color(0xFFFFD36F),
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "الوقت المتبقي • ${nextPrayer.time}",
                        color = Color(0xFFF4EBDD),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else if (loading) {
                    CircularProgressIndicator(color = Color(0xFFFFD36F))
                    Text("جاري تحميل مواقيت الصلاة...", color = Color.White)
                } else {
                    Text(
                        if (locationName.isBlank()) "اختَر مدينتك لعرض الصلاة القادمة"
                        else errorMessage ?: "مواقيت الصلاة غير متاحة حاليًا",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFF2B5A40)).padding(9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("التاريخ الميلادي", color = Color(0xFFFFE5AE), fontSize = 10.sp)
                        Text(gregorianDate, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFF2B5A40)).padding(9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("التاريخ الهجري", color = Color(0xFFFFE5AE), fontSize = 10.sp)
                        Text(hijriDate, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    locationName.ifBlank { "لم يتم اختيار مدينة" },
                    color = Color(0xFFE8DDC8),
                    style = MaterialTheme.typography.labelSmall
                )
                TextButton(onClick = onOpenPrayer) {
                    Text("اختيار المدينة وإعدادات الصلاة", color = Color(0xFFFFD36F))
                }
            }
        }

        if (prayerTimes != null) {
            val prayerItems = listOf(
                Triple("الفجر", prayerTimes.fajr, "🌅"),
                Triple("الظهر", prayerTimes.dhuhr, "☀️"),
                Triple("العصر", prayerTimes.asr, "🌤️"),
                Triple("المغرب", prayerTimes.maghrib, "🌇"),
                Triple("العشاء", prayerTimes.isha, "🌙")
            )
            Card(
                Modifier.fillMaxWidth().border(1.dp, Color(0xFFD5AD59), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8EA))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    prayerItems.forEach { (name, time, symbol) ->
                        val isNext = nextPrayer?.name == name
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (isNext) Color(0xFFE5EEDC) else Color.Transparent)
                                .padding(vertical = 5.dp, horizontal = 1.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(symbol, fontSize = 18.sp)
                            Text(
                                name,
                                color = Color(0xFF19432F),
                                fontSize = 10.sp,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                time,
                                color = Color(0xFF19432F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (!loading) {
            Text(
                "تظهر المواقيت الخمسة بعد اختيار مدينة وتوفر الاتصال.",
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}


@Composable
fun PrayerPage(
    locationName: String,
    savedGovernorateId: String,
    latitude: Double?,
    longitude: Double?,
    prayerTimes: PrayerTimes?,
    weather: CurrentWeather?,
    loading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onSaveLocation: (String, Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var governorates by remember { mutableStateOf<List<EgyptGovernorate>?>(null) }
    var locationsError by remember { mutableStateOf<String?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        try {
            governorates = withContext(Dispatchers.IO) { EgyptLocationsRepository.load(context) }
        } catch (_: Exception) {
            locationsError = "تعذر تحميل قائمة المحافظات والمراكز من ملفات التطبيق."
        }
    }

    fun useDeviceLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        scope.launch {
            locationLoading = true
            locationMessage = null
            try {
                val deviceLocation = DeviceLocationProvider.getCurrentLocation(context)
                val nearest = governorates?.let {
                    EgyptLocationsRepository.findNearestArea(
                        it, deviceLocation.latitude, deviceLocation.longitude
                    )
                }
                val nearestName = nearest?.let { "${it.second.name}، ${it.first.name}" }
                    ?: "موقع الجهاز"
                val qualityLabel = if (hasFine) nearestName else "موقع تقريبي • $nearestName"
                onSaveLocation(
                    qualityLabel,
                    deviceLocation.latitude,
                    deviceLocation.longitude,
                    nearest?.first?.id.orEmpty()
                )
                locationMessage = if (hasFine) {
                    "تم تحديد موقع الجهاز. يستخدم التطبيق الموقع أثناء ظهور التطبيق فقط."
                } else {
                    "تم استخدام موقع تقريبي حسب الإذن الذي اخترته."
                }
            } catch (_: SecurityException) {
                locationMessage = "لم يتم منح إذن الموقع. يمكنك اختيار مدينتك يدويًا."
            } catch (_: Exception) {
                locationMessage = "تعذر تحديد الموقع. تأكد من تشغيل خدمة الموقع، أو اختَر مدينتك يدويًا."
            } finally {
                locationLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            useDeviceLocation()
        } else {
            locationMessage = "لم يتم منح إذن الموقع. اختيار المدينة يدويًا يظل متاحًا."
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("إعدادات الصلاة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "اختر مدينة من مصر أو استخدم موقع الجهاز. لا يعمل تتبع الموقع في الخلفية.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("موقعك المعتمد", fontWeight = FontWeight.Bold)
                    Text(
                        locationName.ifBlank { "لم يتم اختيار مدينة بعد" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showPicker = true }, enabled = governorates != null) {
                            Text("اختيار المحافظة والمدينة")
                        }
                        OutlinedButton(
                            onClick = {
                                val fine = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fine || coarse) {
                                    useDeviceLocation()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !locationLoading
                        ) {
                            Text(if (locationLoading) "جارٍ تحديد الموقع..." else "تحديد موقعي")
                        }
                    }
                    if (locationLoading) CircularProgressIndicator()
                    if (locationsError != null) Text(locationsError!!, color = MaterialTheme.colorScheme.error)
                    if (locationMessage != null) Text(locationMessage!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "عند تحديث المواقيت والطقس عبر الإنترنت، تُرسل إحداثيات الموقع إلى خدمتي البيانات المذكورتين أدناه. لا يطلب التطبيق إذن الموقع إلا بعد ضغطك على «تحديد موقعي».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مواقيت الصلاة اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (prayerTimes != null) {
                        Text("الفجر: ${prayerTimes.fajr}     الشروق: ${prayerTimes.sunrise}")
                        Text("الظهر: ${prayerTimes.dhuhr}     العصر: ${prayerTimes.asr}")
                        Text("المغرب: ${prayerTimes.maghrib}     العشاء: ${prayerTimes.isha}")
                        Text("طريقة الحساب: الهيئة المصرية العامة للمساحة",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        prayerTimes.hijriDate?.let {
                            Text("التاريخ الهجري: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (loading) {
                        Text("جاري تحميل مواقيت الصلاة...")
                    } else {
                        Text(errorMessage ?: "اختَر مدينة أو حدّد موقعك لتحميل المواقيت.")
                    }
                    TextButton(onClick = onRefresh, enabled = latitude != null && longitude != null && !loading) {
                        Text("تحديث المواقيت")
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("الطقس الآن", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (weather != null) {
                        Text("${weather.temperatureCelsius.toInt()}° م • ${weather.description}")
                        Text("مصدر البيانات: Open-Meteo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (loading) {
                        Text("جاري تحميل بيانات الطقس...")
                    } else {
                        Text("بيانات الطقس غير متاحة حاليًا. تأكد من اتصال الإنترنت.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text(
                "طريقة الحساب: الهيئة المصرية العامة للمساحة (AlAdhan). الطقس: Open-Meteo. قد لا تتوفر البيانات عند انقطاع الإنترنت.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    val currentGovernorates = governorates
    if (showPicker && currentGovernorates != null) {
        EgyptianLocationPickerDialog(
            governorates = currentGovernorates,
            initialGovernorateId = savedGovernorateId,
            onDismiss = { showPicker = false },
            onSelect = { governorate, area ->
                onSaveLocation(
                    "${area.name}، ${governorate.name}",
                    area.latitude,
                    area.longitude,
                    governorate.id
                )
                showPicker = false
                locationMessage = "تم حفظ المدينة المختارة على جهازك."
            }
        )
    }
}

@Composable
private fun EgyptianLocationPickerDialog(
    governorates: List<EgyptGovernorate>,
    initialGovernorateId: String,
    onDismiss: () -> Unit,
    onSelect: (EgyptGovernorate, EgyptAdministrativeArea) -> Unit
) {
    var governorateId by rememberSaveable {
        mutableStateOf(initialGovernorateId.takeIf { id -> governorates.any { it.id == id } }
            ?: governorates.firstOrNull()?.id.orEmpty())
    }
    var governorateMenuExpanded by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val selectedGovernorate = governorates.firstOrNull { it.id == governorateId }
    val filteredAreas = selectedGovernorate?.areas?.filter {
        query.isBlank() || it.name.contains(query.trim(), ignoreCase = true)
    }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار مدينة في مصر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { governorateMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedGovernorate?.name ?: "اختر المحافظة")
                    }
                    DropdownMenu(
                        expanded = governorateMenuExpanded,
                        onDismissRequest = { governorateMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        governorates.forEach { governorate ->
                            DropdownMenuItem(
                                text = { Text(governorate.name) },
                                onClick = {
                                    governorateId = governorate.id
                                    query = ""
                                    governorateMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ابحث عن مدينة أو مركز أو قسم") },
                    shape = RoundedCornerShape(14.dp)
                )
                Text(
                    "اختَر المنطقة التابعة للمحافظة:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredAreas, key = { it.id }) { area ->
                        Card(
                            Modifier.fillMaxWidth().clickable {
                                selectedGovernorate?.let { onSelect(it, area) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(area.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    area.unitType,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    if (filteredAreas.isEmpty()) {
                        item {
                            Text("لا توجد نتيجة مطابقة في هذه المحافظة.",
                                Modifier.fillMaxWidth().padding(12.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

private data class NextPrayerInfo(
    val name: String,
    val time: String,
    val remainingMillis: Long
)

private fun findNextPrayer(times: PrayerTimes, nowMillis: Long): NextPrayerInfo? {
    val timeZone = TimeZone.getTimeZone(times.timezone)
    val candidates = listOf(
        "الفجر" to times.fajr,
        "الظهر" to times.dhuhr,
        "العصر" to times.asr,
        "المغرب" to times.maghrib,
        "العشاء" to times.isha
    )
    for ((name, time) in candidates) {
        val pieces = time.split(":")
        if (pieces.size != 2) continue
        val hour = pieces[0].toIntOrNull() ?: continue
        val minute = pieces[1].toIntOrNull() ?: continue
        val candidate = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.timeInMillis > nowMillis) {
            return NextPrayerInfo(name, time, candidate.timeInMillis - nowMillis)
        }
    }
    val fajr = times.fajr.split(":")
    if (fajr.size != 2) return null
    val nextFajr = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, fajr[0].toInt())
        set(Calendar.MINUTE, fajr[1].toInt())
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return NextPrayerInfo("الفجر", times.fajr, nextFajr.timeInMillis - nowMillis)
}

private fun formatCountdown(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remainingSeconds)
}
