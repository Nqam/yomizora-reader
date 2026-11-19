package com.yomizora.reader.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomizora.reader.domain.model.ContentCategory
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.MaterialStatus
import com.yomizora.reader.domain.model.MaterialWithProgress
import com.yomizora.reader.ui.theme.CategoryColors
import com.yomizora.reader.ui.theme.LocalSemanticColors
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    initialLevel: JlptLevel?,
    onOpenMaterial: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialLevel) {
        if (initialLevel != null) {
            viewModel.selectLevel(initialLevel)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Библиотека",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${state.completedInLevel} / ${state.totalInLevel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = viewModel::toggleViewMode) {
                        Icon(
                            imageVector = if (state.isListView) {
                                Icons.Rounded.GridView
                            } else {
                                Icons.AutoMirrored.Rounded.ViewList
                            },
                            contentDescription = if (state.isListView) "Сетка" else "Список"
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                JlptLevel.activeLevels.forEachIndexed { index, level ->
                    SegmentedButton(
                        selected = state.selectedLevel == level,
                        onClick = { viewModel.selectLevel(level) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = JlptLevel.activeLevels.size
                        )
                    ) {
                        Text(level.titleRu)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Поиск по названию, категории, тексту...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.availableCategories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("Все") }
                        )
                    }
                    items(state.availableCategories) { category ->
                        val catStyle = CategoryColors.styleFor(category)
                        val isSelected = state.selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(catStyle.accent, CircleShape)
                                )
                            },
                            label = { Text(ContentCategory.toRussian(category)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = catStyle.container.copy(alpha = 0.55f),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = catStyle.accent,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = catStyle.accent.copy(alpha = 0.45f),
                                selectedBorderColor = catStyle.accent
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Материалы не найдены",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.isListView) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.material.id }) { item ->
                        MaterialListRow(
                            item = item,
                            onClick = {
                                if (item.status == MaterialStatus.LOCKED) {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            "Текст #${item.material.orderIndex.toString().padStart(2, '0')} заблокирован. Сначала пройдите предыдущий текст."
                                        )
                                    }
                                } else {
                                    onOpenMaterial(item.material.id)
                                }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.material.id }) { item ->
                        NumberedMaterialTile(
                            item = item,
                            onClick = {
                                if (item.status == MaterialStatus.LOCKED) {
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            "Текст #${item.material.orderIndex.toString().padStart(2, '0')} заблокирован. Сначала пройдите предыдущий текст."
                                        )
                                    }
                                } else {
                                    onOpenMaterial(item.material.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberedMaterialTile(
    item: MaterialWithProgress,
    onClick: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val catStyle = CategoryColors.styleFor(item.material.category)
    val numberLabel = item.material.orderIndex.toString().padStart(2, '0')

    val (bgColor, contentColor, borderColor, icon) = when (item.status) {
        MaterialStatus.LOCKED -> Quad(
            catStyle.container.copy(alpha = 0.30f),
            semantic.locked,
            catStyle.accent.copy(alpha = 0.22f),
            Icons.Rounded.Lock
        )
        MaterialStatus.AVAILABLE -> Quad(
            catStyle.container,
            catStyle.accent,
            catStyle.accent.copy(alpha = 0.70f),
            Icons.Rounded.PlayArrow
        )
        MaterialStatus.IN_PROGRESS -> Quad(
            catStyle.container,
            catStyle.accent,
            catStyle.accent,
            Icons.AutoMirrored.Rounded.MenuBook
        )
        MaterialStatus.COMPLETED -> Quad(
            catStyle.accent,
            MaterialTheme.colorScheme.onPrimary,
            catStyle.accent,
            Icons.Rounded.CheckCircle
        )
    }

    val borderWidth = if (item.status == MaterialStatus.IN_PROGRESS) 2.5.dp else 1.5.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = numberLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(Modifier.height(2.dp))
            Icon(
                imageVector = icon,
                contentDescription = item.status.name,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun MaterialListRow(
    item: MaterialWithProgress,
    onClick: () -> Unit
) {
    val semantic = LocalSemanticColors.current
    val catStyle = CategoryColors.styleFor(item.material.category)
    val mat = item.material
    val numberLabel = mat.orderIndex.toString().padStart(2, '0')
    val isLocked = item.status == MaterialStatus.LOCKED

    val icon = when (item.status) {
        MaterialStatus.LOCKED -> Icons.Rounded.Lock
        MaterialStatus.AVAILABLE -> Icons.Rounded.PlayArrow
        MaterialStatus.IN_PROGRESS -> Icons.AutoMirrored.Rounded.MenuBook
        MaterialStatus.COMPLETED -> Icons.Rounded.CheckCircle
    }
    val statusColor = when (item.status) {
        MaterialStatus.LOCKED -> semantic.locked
        MaterialStatus.COMPLETED -> catStyle.accent
        else -> catStyle.accent
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isLocked) {
            catStyle.container.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (item.status == MaterialStatus.IN_PROGRESS) 2.dp else 1.dp,
            color = if (isLocked) {
                catStyle.accent.copy(alpha = 0.2f)
            } else {
                catStyle.accent.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (item.status == MaterialStatus.COMPLETED) {
                    catStyle.accent
                } else {
                    catStyle.container
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = numberLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.status == MaterialStatus.COMPLETED) {
                            MaterialTheme.colorScheme.onPrimary
                        } else if (isLocked) {
                            semantic.locked
                        } else {
                            catStyle.accent
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "${ContentCategory.toRussian(mat.category)} • ${mat.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isLocked) semantic.locked else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mat.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = item.status.name,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
