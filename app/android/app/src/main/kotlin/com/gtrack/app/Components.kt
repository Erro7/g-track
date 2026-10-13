package com.gtrack.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ================= 共享组件（逐屏对照 prototype.html） =================

@Composable
fun LargeTitle(text: String) {
    Text(text, color = GTRACKLabel, fontSize = 34.sp, fontWeight = FontWeight.Bold)
}

/** 推页大标题（与首页同级，34pt） */
@Composable
fun ScanHeader(title: String) {
    Text(title, color = GTRACKLabel, fontSize = 34.sp, fontWeight = FontWeight.Bold)
}

/**
 * 原型 .iconbtn：34px 圆形 fill 底 + **label 色**线稿（不是绿色强调），stroke 1.8。
 * 之前两端都是"裸图标 + 绿色/次色"，跟原型不是一回事。
 */
@Composable
fun GIconButton(
    icon: GTIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = GTRACKLabel,
    iconSize: Dp = 17.dp,
    iconModifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(34.dp)
            .background(GTRACKFill, RoundedCornerShape(17.dp))
            .noRippleClickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        GTLineIcon(icon, size = iconSize, tint = tint, strokeWidth = 1.8f, modifier = iconModifier)
    }
}

/** iOS 分段控件（对齐原型 .seg：容器槽 + 选中亮块） */
@Composable
fun GTSegmented(
    options: List<String>,
    selected: Int,
    onChange: (Int) -> Unit,
    icons: List<GTIcon>? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(GTRACKLabel2.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            active && !LocalGTLight.current -> GTRACKLabel.copy(alpha = 0.18f)
                            active -> Color.White
                            else -> Color.Transparent
                        }
                    )
                    .padding(vertical = 7.dp)
                    .noRippleClickable { onChange(i) },
                contentAlignment = Alignment.Center,
            ) {
                // 原型 .seg button：图标 + 文案（深色=月牙 / 浅色=太阳）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icons?.getOrNull(i)?.let {
                        GTLineIcon(
                            it, size = 14.dp,
                            tint = if (active) GTRACKLabel else GTRACKLabel2,
                            strokeWidth = 1.8f,
                        )
                    }
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) GTRACKLabel else GTRACKLabel2,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, trailing: (@Composable () -> Unit)? = null) {
    // 全局统一节距：上 10 / 下 8（DESIGN.md §3 间距栅格）
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = GTRACKLabel2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        trailing?.invoke()
    }
}

/** 玻璃分组卡（对齐原型 .group：行间细分隔线） */
@Composable
fun GroupCard(rows: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().gtGlass()) { rows() }
}

@Composable
fun KRow(label: String, value: String, mono: Boolean = false, sub: String? = null, onCopy: (() -> Unit)? = null) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = GTRACKLabel2, fontSize = 15.sp)
                // 原型 .row .rs：行副标题（数据源/字段说明）
                if (sub != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(sub, color = GTRACKLabel3, fontSize = 12.sp)
                }
            }
            Text(
                value,
                color = GTRACKLabel,
                fontSize = if (mono) 13.sp else 14.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                maxLines = 1,
            )
            onCopy?.let {
                Spacer(Modifier.width(8.dp))
                CopyChip(it)
            }
        }
        HorizontalDivider(color = GTRACKSeparator, thickness = 0.5.dp)
    }
}

@Composable
fun CopyChip(onCopy: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .background(GTRACKLabel2.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .noRippleClickable { onCopy() },
        contentAlignment = Alignment.Center,
    ) {
        GTLineIcon(GTIcon.Copy, size = 14.dp, tint = GTRACKLabel, strokeWidth = 2f)
    }
}

/** 主 CTA：绿色实心；禁用降透明；加载中显示点动 */
@Composable
fun CTAButton(text: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (enabled) GTRACKGreen else GTRACKGreen.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .noRippleClickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // DESIGN.md §6：加载中保留文案（「追踪中…」）+ 禁点，防重复提交
        if (loading) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DotSpinner(color = GTRACKOnGreen)
                Text(text, color = GTRACKOnGreen, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        } else Text(text, color = GTRACKOnGreen, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

/** 行内错误（保留输入，给修正示例） */
@Composable
fun InlineError(message: String?) {
    if (message != null) {
        Text(message, color = GTRACKRed, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 居中 toast（对齐原型玻璃胶囊） */
@Composable
fun GlassToast(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(GTRACKSurface.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
            .noRippleClickable { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message, color = GTRACKLabel, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun DotSpinner(size: Int = 3, color: Color = GTRACKGreen) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(size) { i ->
            Box(Modifier.size(6.dp).background(color.copy(alpha = 0.4f + 0.2f * i), RoundedCornerShape(999.dp)))
        }
    }
}

@Composable
fun MonoText(text: String, size: Int = 17, bold: Boolean = false, color: Color = GTRACKLabel) {
    Text(
        text, color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    )
}

/** 等宽玻璃输入框（对齐原型 .inp） */
@Composable
fun GTRACKInput(value: String, onValue: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = { onValue(it) },
        placeholder = { Text(placeholder, fontFamily = FontFamily.Monospace, color = GTRACKLabel2) },
        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace, color = GTRACKLabel, fontSize = 17.sp
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GTRACKGreen,
            unfocusedBorderColor = GTRACKLabel2.copy(alpha = 0.25f),
            focusedContainerColor = GTRACKSurface,
            unfocusedContainerColor = GTRACKSurface,
            focusedTextColor = GTRACKLabel,
            unfocusedTextColor = GTRACKLabel,
        ),
    )
}

/** 无涟漪点击 */
fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

/** 复制到剪贴板 + toast */
@Composable
fun rememberCopier(onToast: (String) -> Unit): (String) -> Unit {
    val cm = androidx.compose.ui.platform.LocalClipboardManager.current
    return { text ->
        cm.setText(androidx.compose.ui.text.AnnotatedString(text))
        onToast("已拷贝")
    }
}
