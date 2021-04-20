/*
 * EntityLUT.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

data class EntityLUT(
    val entries: MutableList<Entry> = mutableListOf(),
)

data class Entry(
    val mine: String,
    val common: String,
)