package com.sensitivemobility.neckatlas.feature.scales

import com.sensitivemobility.neckatlas.domain.scale.ScaleType

/** La gamme [delta] rangs plus loin dans l'ordre de [ScaleType.entries], en bouclant : après le blues vient la majeure. */
internal fun stepScaleType(type: ScaleType, delta: Int): ScaleType {
    val all = ScaleType.entries
    return all[((type.ordinal + delta) % all.size + all.size) % all.size]
}

/**
 * La position choisie parmi [count], ou -1 pour « toute la zone ». Un choix trop grand (on est passé d'une gamme de sept
 * positions à une pentatonique de cinq) retombe sur la dernière.
 */
internal fun clampPosition(choice: Int, count: Int): Int = if (choice < 0 || count == 0) -1 else choice.coerceAtMost(count - 1)
