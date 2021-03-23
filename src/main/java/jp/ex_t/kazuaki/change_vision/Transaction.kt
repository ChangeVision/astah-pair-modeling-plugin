/*
 * Transaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import kotlinx.serialization.Serializable

@Serializable
data class Transaction (
    val iClass: CreateIClass? = null,
    val iClassPresentation: CreateINodePresentation? = null
        )

@Serializable
data class CreateIClass(val name: String, val parentName: String)

@Serializable
data class CreateINodePresentation(val className: String, val location: Pair<Double, Double>, val diagramName: String)