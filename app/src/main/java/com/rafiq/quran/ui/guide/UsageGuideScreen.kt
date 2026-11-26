package com.rafiq.quran.ui.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// دليل الاستخدام: خطوات عن وظائف التطبيق نفسه فقط، ولا يحتوي أي نص قرآني أو ديني.
// محتوى الخطوات يُمرَّر من الخارج (مثلًا من موارد strings.xml) ليطابق الوظائف الموجودة فعلًا في التطبيق.
data class GuideSection(
    val id: String,
    val title: String,
    val steps: List<String>
)

@Composable
fun UsageGuideScreen(
    sections: List<GuideSection>,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var expandedId by remember { mutableStateOf<String?>(null) }

        if (sections.isEmpty()) {
            Box(modifier.fillMaxSize()) {
                Text(
                    "لا يوجد محتوى للدليل حاليًا",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@CompositionLocalProvider
        }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("دليل الاستخدام", style = MaterialTheme.typography.headlineSmall)
            }
            items(sections, key = { it.id }) { section ->
                val expanded = expandedId == section.id
                OutlinedCard(
                    onClick = { expandedId = if (expanded) null else section.id },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        if (expanded) {
                            section.steps.forEachIndexed { index, step ->
                                Text(
                                    "${index + 1}. $step",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
