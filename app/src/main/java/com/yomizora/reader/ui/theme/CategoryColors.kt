package com.yomizora.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.yomizora.reader.domain.model.ThemeMode

data class CategoryColorStyle(
    val accent: Color,
    val container: Color
)

object CategoryColors {
    @Composable
    fun styleFor(category: String, themeMode: ThemeMode = ThemeMode.SYSTEM): CategoryColorStyle {
        val isDark = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        return if (isDark) darkStyle(category) else lightStyle(category)
    }

    fun lightStyle(category: String): CategoryColorStyle = when (category) {
        "Stories" -> CategoryColorStyle(Color(0xFF365E96), Color(0xFFE5EEF9))
        "Daily Life" -> CategoryColorStyle(Color(0xFF267873), Color(0xFFE0F2F1))
        "Dialogues" -> CategoryColorStyle(Color(0xFF7A498F), Color(0xFFF2E7F7))
        "News" -> CategoryColorStyle(Color(0xFF495E70), Color(0xFFE7EDF2))
        "Japanese Culture" -> CategoryColorStyle(Color(0xFFB83B2A), Color(0xFFFBE9E6))
        "Fairy Tales" -> CategoryColorStyle(Color(0xFFB34467), Color(0xFFFBE6ED))
        "Humor" -> CategoryColorStyle(Color(0xFFC2721E), Color(0xFFFCEFE0))
        "Interesting Facts" -> CategoryColorStyle(Color(0xFF24759C), Color(0xFFE1F1F8))
        "Travel" -> CategoryColorStyle(Color(0xFF2E7D57), Color(0xFFE3F4EB))
        "Food" -> CategoryColorStyle(Color(0xFFC25329), Color(0xFFFCEBE4))
        "Education" -> CategoryColorStyle(Color(0xFF687826), Color(0xFFEEF2DF))
        else -> CategoryColorStyle(Color(0xFF2E4A62), Color(0xFFE5ECF2))
    }

    fun darkStyle(category: String): CategoryColorStyle = when (category) {
        "Stories" -> CategoryColorStyle(Color(0xFF82ACE3), Color(0xFF1F2D40))
        "Daily Life" -> CategoryColorStyle(Color(0xFF69BCB6), Color(0xFF1B3332))
        "Dialogues" -> CategoryColorStyle(Color(0xFFC18FD6), Color(0xFF312238))
        "News" -> CategoryColorStyle(Color(0xFF93ABC0), Color(0xFF222A32))
        "Japanese Culture" -> CategoryColorStyle(Color(0xFFE06D5D), Color(0xFF3D211D))
        "Fairy Tales" -> CategoryColorStyle(Color(0xFFE385A3), Color(0xFF3B202A))
        "Humor" -> CategoryColorStyle(Color(0xFFE49B4B), Color(0xFF3B2B1A))
        "Interesting Facts" -> CategoryColorStyle(Color(0xFF6CB7DB), Color(0xFF1B303B))
        "Travel" -> CategoryColorStyle(Color(0xFF62B88C), Color(0xFF1C3528))
        "Food" -> CategoryColorStyle(Color(0xFFE8865F), Color(0xFF3D251B))
        "Education" -> CategoryColorStyle(Color(0xFFA9BD5D), Color(0xFF2C3319))
        else -> CategoryColorStyle(Color(0xFF7BA1C2), Color(0xFF203242))
    }
}
