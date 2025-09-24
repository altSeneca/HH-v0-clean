package com.hazardhawk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A standardized dialog component for HazardHawk that handles:
 * - Fixed height constraints to prevent overflow
 * - Keyboard avoidance with IME padding
 * - Scrollable content section
 * - Consistent Material3 styling
 * - Proper dismiss behavior
 */
@Composable
fun StandardDialog(
    onDismissRequest: (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismissRequest ?: {},
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress && onDismissRequest != null,
            dismissOnClickOutside = dismissOnClickOutside && onDismissRequest != null
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 560.dp)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                // Title section
                title?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp, 24.dp, 24.dp, 0.dp)
                    ) {
                        it()
                    }
                }
                
                // Content section (scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    content()
                }
                
                // Button section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    buttons()
                }
            }
        }
    }
}