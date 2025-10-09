package com.hazardhawk.ui.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hazardhawk.models.dashboard.ActivityFeedItem
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Activity feed list with pull-to-refresh functionality.
 *
 * Features:
 * - Pull-to-refresh for latest updates
 * - 72dp item height for construction-friendly touch targets
 * - Fade-in animations for new items
 * - Empty state handling
 * - Loading state support
 *
 * @param items List of activity feed items to display
 * @param isRefreshing Whether the feed is currently refreshing
 * @param onRefresh Callback when user pulls to refresh
 * @param onItemClick Callback when an item is clicked
 * @param modifier Optional modifier for the list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedList(
    items: List<ActivityFeedItem>,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onItemClick: (ActivityFeedItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (items.isEmpty() && !isRefreshing) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyFeedState()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    // Fade-in animation for each item
                    var visible by remember { mutableStateOf(false) }

                    LaunchedEffect(item.id) {
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        ) + expandVertically(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        ),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        ActivityFeedItem(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .heightIn(min = 72.dp) // Construction-friendly touch target
                        )
                    }
                }

                // Loading indicator at bottom when refreshing
                if (isRefreshing && items.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ConstructionColors.SafetyOrange,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state component shown when there are no activity items.
 */
@Composable
private fun EmptyFeedState() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "No Recent Activity",
            style = MaterialTheme.typography.titleLarge,
            color = ConstructionColors.OnSurfaceVariant
        )
        Text(
            text = "Your activity feed will appear here.\nStart by capturing a safety photo or creating a PTP.",
            style = MaterialTheme.typography.bodyMedium,
            color = ConstructionColors.OnSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
