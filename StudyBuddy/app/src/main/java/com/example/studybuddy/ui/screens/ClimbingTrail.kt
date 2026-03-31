package com.example.studybuddy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 向上之路 - 登山轨迹组件
 * 每个脚印代表一次学习启动，按时间向上排列
 * 累计记录，不清零，不倒退
 */

/** 脚印数据 */
data class Footprint(
    val timestamp: String,  // 时间
    val index: Int          // 第几次
)

@Composable
fun ClimbingTrail(
    footprints: List<Footprint>,
    newFootprintAdded: Boolean,  // 是否刚新增了脚印（用于触发动画）
    modifier: Modifier = Modifier
) {
    val totalSteps = footprints.size

    // 新脚印点亮动画
    val glowAlpha by animateFloatAsState(
        targetValue = if (newFootprintAdded) 1f else 0.6f,
        animationSpec = if (newFootprintAdded) {
            repeatable(
                iterations = 3,
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        },
        label = "glow"
    )

    // 新脚印缩放动画
    val newScale by animateFloatAsState(
        targetValue = if (newFootprintAdded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "\u26F0\uFE0F", fontSize = 20.sp) // 山emoji
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "向上之路",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${totalSteps}步",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (totalSteps == 0) {
                // 还没有脚印
                Text(
                    text = "点击「我开始了」留下你的第一个脚印",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                // 登山轨迹
                val primaryColor = MaterialTheme.colorScheme.primary
                val trailColor = MaterialTheme.colorScheme.primaryContainer
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

                // 显示最近的脚印（最多显示最近 10 个，避免太长）
                val displayFootprints = footprints.takeLast(10)
                val trailHeight = (displayFootprints.size * 52).coerceAtLeast(100)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trailHeight.dp)
                ) {
                    // 绘制蜿蜒路径
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val stepHeight = height / (displayFootprints.size + 1)

                        // 画虚线路径
                        val path = Path()
                        val points = mutableListOf<Offset>()

                        for (i in displayFootprints.indices) {
                            // 交替左右蜿蜒
                            val progress = (displayFootprints.size - i).toFloat() / (displayFootprints.size + 1)
                            val y = height * progress
                            val xOffset = if (i % 2 == 0) width * 0.35f else width * 0.65f
                            points.add(Offset(xOffset, y))
                        }

                        // 画路径线
                        if (points.size >= 2) {
                            path.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val midX = (points[i - 1].x + points[i].x) / 2
                                val midY = (points[i - 1].y + points[i].y) / 2
                                path.quadraticBezierTo(
                                    points[i - 1].x, points[i - 1].y,
                                    midX, midY
                                )
                            }
                            path.lineTo(points.last().x, points.last().y)

                            drawPath(
                                path = path,
                                color = trailColor,
                                style = Stroke(
                                    width = 4.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(12f, 8f)
                                    ),
                                    cap = StrokeCap.Round
                                )
                            )
                        }

                        // 画脚印节点
                        points.forEachIndexed { idx, point ->
                            val isLatest = idx == points.size - 1
                            val radius = if (isLatest) 14.dp.toPx() * newScale.coerceAtLeast(0.6f)
                                        else 10.dp.toPx()
                            val alpha = if (isLatest) glowAlpha else 0.6f

                            // 外圈光晕（最新的脚印有发光效果）
                            if (isLatest && newFootprintAdded) {
                                drawCircle(
                                    color = primaryColor.copy(alpha = glowAlpha * 0.3f),
                                    radius = radius * 1.8f,
                                    center = point
                                )
                            }

                            // 脚印圆点
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = alpha),
                                        primaryColor.copy(alpha = alpha * 0.6f)
                                    ),
                                    center = point,
                                    radius = radius
                                ),
                                radius = radius,
                                center = point
                            )

                            // 内圈白点
                            drawCircle(
                                color = Color.White.copy(alpha = 0.8f),
                                radius = radius * 0.35f,
                                center = point
                            )
                        }

                        // 画山顶标记
                        if (points.isNotEmpty()) {
                            val topY = points.last().y - 30.dp.toPx()
                            val topX = points.last().x
                            // 小三角形代表山顶方向
                            val trianglePath = Path().apply {
                                moveTo(topX, topY - 8.dp.toPx())
                                lineTo(topX - 6.dp.toPx(), topY + 4.dp.toPx())
                                lineTo(topX + 6.dp.toPx(), topY + 4.dp.toPx())
                                close()
                            }
                            drawPath(
                                path = trianglePath,
                                color = primaryColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 底部鼓励语
                val encouragement = when {
                    totalSteps >= 30 -> "了不起！你已经走了 $totalSteps 步，山顶就在前方"
                    totalSteps >= 10 -> "坚持得很好！每一步都算数"
                    totalSteps >= 5 -> "已经迈出 $totalSteps 步了，继续！"
                    totalSteps >= 1 -> "第一步是最难的，你已经做到了"
                    else -> ""
                }

                Text(
                    text = encouragement,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
