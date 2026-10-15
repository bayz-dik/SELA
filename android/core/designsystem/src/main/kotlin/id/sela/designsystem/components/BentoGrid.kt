package id.sela.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bento grid — sel dengan lebar bervariasi (span kolom) yang tersusun
 * otomatis per baris, seperti susunan kotak bento. Untuk dashboard dan
 * layar utama: kartu besar (ETA berikutnya) + kartu kecil (favorit, dll).
 *
 * Contoh:
 * ```
 * BentoGrid(columns = 2) {
 *     cell(columnSpan = 2) { PastelCard { ... } } // selebar penuh
 *     cell { PastelCard { ... } }                 // setengah
 *     cell { PastelCard { ... } }                 // setengah
 * }
 * ```
 */
interface BentoGridScope {
    /** Tambah satu sel dengan [columnSpan] kolom (1..columns). */
    fun cell(columnSpan: Int = 1, content: @Composable () -> Unit)
}

private data class BentoCell(val span: Int, val content: @Composable () -> Unit)

private class BentoGridScopeImpl : BentoGridScope {
    val cells = mutableListOf<BentoCell>()
    override fun cell(columnSpan: Int, content: @Composable () -> Unit) {
        cells.add(BentoCell(columnSpan, content))
    }
}

@Composable
fun BentoGrid(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    content: BentoGridScope.() -> Unit,
) {
    val scope = BentoGridScopeImpl().apply(content)
    Layout(
        content = { scope.cells.forEach { it.content() } },
        modifier = modifier,
    ) { measurables, constraints ->
        val cols = columns.coerceAtLeast(1)
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val cellUnit = (constraints.maxWidth - hGap * (cols - 1)) / cols
        val spans = scope.cells.map { it.span.coerceIn(1, cols) }

        val placeables = measurables.mapIndexed { index, measurable ->
            val span = spans[index]
            val width = cellUnit * span + hGap * (span - 1)
            measurable.measure(
                constraints.copy(minWidth = width, maxWidth = width, minHeight = 0),
            )
        }

        // Kelompokkan sel ke baris: bungkus bila span melebihi sisa kolom.
        val rows = mutableListOf<MutableList<Int>>()
        var current = mutableListOf<Int>()
        var used = 0
        spans.forEachIndexed { index, span ->
            if (used + span > cols && current.isNotEmpty()) {
                rows.add(current)
                current = mutableListOf()
                used = 0
            }
            current.add(index)
            used += span
        }
        if (current.isNotEmpty()) rows.add(current)

        val rowHeights = rows.map { row -> row.maxOf { placeables[it].height } }
        val totalHeight = rowHeights.sum() + vGap * (rows.size - 1).coerceAtLeast(0)

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEach { cellIndex ->
                    val span = spans[cellIndex]
                    placeables[cellIndex].placeRelative(x, y)
                    x += cellUnit * span + hGap * span
                }
                y += rowHeights[rowIndex] + vGap
            }
        }
    }
}
