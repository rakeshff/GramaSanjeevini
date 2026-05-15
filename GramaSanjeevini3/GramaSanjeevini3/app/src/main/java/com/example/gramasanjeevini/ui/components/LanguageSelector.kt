package com.example.gramasanjeevini.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasanjeevini.models.AppLanguage

@Composable
fun LanguageSelector(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    isDarkMode: Boolean
) {
    Column {
        Text(
            "Select Language / ಭಾಷೆ / भाषा",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns             = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.height(100.dp)
        ) {
            items(AppLanguage.entries) { lang ->
                val isSelected = selected == lang
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang) }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF6366F1)
                                    else if (isDarkMode) Color(0xFF334155)
                                    else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    color = if (isSelected)
                                Color(0xFF6366F1).copy(alpha = 0.12f)
                            else if (isDarkMode) Color(0xFF1E293B)
                            else Color.White,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            lang.nativeName,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (isSelected) Color(0xFF6366F1)
                                         else if (isDarkMode) Color.White
                                         else Color(0xFF1E293B)
                        )
                        Text(
                            lang.displayName,
                            fontSize = 10.sp,
                            color    = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
