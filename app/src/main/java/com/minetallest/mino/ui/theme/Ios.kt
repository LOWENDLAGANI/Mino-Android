package com.minetallest.mino.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Inset separator that starts at the text column, exactly like iOS grouped lists. */
@Composable
fun IosDivider(startIndent: Dp = 16.dp) {
    val colors = LocalMinoColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = startIndent)
            .background(colors.line),
    )
}

@Composable
fun IosSectionHeader(text: String) {
    val colors = LocalMinoColors.current
    Text(
        text = text.uppercase(),
        color = colors.textMid,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 7.dp),
    )
}

@Composable
fun IosGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMinoColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgRaised),
        content = content,
    )
}

@Composable
fun IosIconBadge(icon: ImageVector, tint: Color, size: Dp = 29.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.58f))
    }
}

/** Standard iOS list row. No subtitle: labels have to stand on their own. */
@Composable
fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = LocalMinoColors.current.accent,
    value: String? = null,
    chevron: Boolean = false,
    destructive: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IosIconBadge(icon = icon, tint = iconTint)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = if (destructive) colors.danger else colors.text,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(value, color = colors.textMid, fontSize = 17.sp)
            Spacer(Modifier.width(6.dp))
        }
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textLow,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun IosSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = LocalMinoColors.current.mint,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IosIconBadge(icon = icon, tint = iconTint)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = colors.text,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.switchTrack,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.lineStrong,
            ),
        )
    }
}

/** 44pt iOS navigation bar with a chevron back button and centred title. */
@Composable
fun IosNavBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterStart) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = colors.accent,
                        modifier = Modifier.size(26.dp),
                    )
                    Text("Back", color = colors.accent, fontSize = 17.sp)
                }
            }
        }
        Text(
            text = title,
            color = colors.textHi,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
            trailing?.invoke()
        }
    }
}

@Composable
fun IosLargeTitle(text: String, modifier: Modifier = Modifier) {
    val colors = LocalMinoColors.current
    Text(
        text = text,
        color = colors.textHi,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
    )
}

@Composable
fun IosPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalMinoColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) colors.primaryButton else colors.bgHover)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) colors.onAccent else colors.textLow,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** iOS segmented control: filled track with a raised white / dark selected capsule. */
@Composable
fun IosSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(colors.fill)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) colors.bgRaised else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) colors.textHi else colors.textMid,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun IosSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier,
) {
    val colors = LocalMinoColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.fill)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textMid, modifier = Modifier.size(17.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.textHi, fontSize = 17.sp),
            modifier = Modifier.weight(1f).padding(start = 6.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = colors.textMid, fontSize = 17.sp)
                inner()
            },
        )
    }
}

/** Bottom sheet presentation for dialogs and compact panels. */
@Composable
fun IosSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(LocalMinoColors.current.menuBackground)
                .clickable { }
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            content = content,
        )
    }
}

/** Small drag handle for sheets. */
@Composable
fun IosSheetHandle() {
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .width(36.dp)
            .height(5.dp)
            .clip(CircleShape)
            .background(LocalMinoColors.current.lineStrong),
    )
}
