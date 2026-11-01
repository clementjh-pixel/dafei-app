package com.example.dafei.util

/**
 * 码数排序：XS → S → M → L → XL(1XL) → XXL(2XL) → XXXL(3XL) → 4XL...
 * 纯数字码（童装 80/90/100/110）排在字母码之后，按数值升序。
 * 未知码排在最后，按字典序，保证稳定输出。
 */
object SizeOrder {

    /** 标准顺序，下标即权重 */
    private val STANDARD = listOf(
        "XXS", "XS", "S", "M", "L",
        "XL", "XXL", "XXXL", "4XL", "5XL", "6XL", "7XL", "8XL"
    )

    /** 别名 -> 标准名 */
    private val ALIAS = mapOf(
        "1XL" to "XL",
        "2XL" to "XXL",
        "3XL" to "XXXL",
        "XXXXL" to "4XL",
        "XXXXXL" to "5XL"
    )

    fun rank(size: String): Int {
        val raw = size.trim().uppercase()
        if (raw.isEmpty()) return Int.MAX_VALUE

        val std = ALIAS[raw] ?: raw
        STANDARD.indexOf(std).takeIf { it >= 0 }?.let { return it }

        // 通用规则："nXL" -> n 个 X + L（如 5XL -> XXXXXL，若不在 STANDARD 里再兜底）
        Regex("^(\\d+)XL$").find(raw)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: 0
            if (n >= 1) {
                val candidate = "X".repeat(n) + "L"
                STANDARD.indexOf(candidate).takeIf { it >= 0 }?.let { return it }
            }
        }

        // 纯数字码：排在字母码之后，数值升序
        raw.toIntOrNull()?.let { return 1000 + it }

        // 未知：最后
        return 100_000
    }

    /** 去重 + 升序排序 */
    fun sorted(sizes: Collection<String>): List<String> =
        sizes.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(compareBy({ rank(it) }, { it }))
}