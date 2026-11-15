package com.sensitivemobility.neckatlas.feature.chords

import com.sensitivemobility.neckatlas.domain.chord.ChordQuality

/** La fondamentale (0 = Do … 11 = Si) [delta] demi-tons plus loin, en bouclant : après Si vient Do, avant Do vient Si. */
internal fun stepRoot(root: Int, delta: Int): Int = ((root + delta) % 12 + 12) % 12

/** La famille d'accords [delta] rangs plus loin dans l'ordre de [ChordQuality.entries], en bouclant. */
internal fun stepQuality(quality: ChordQuality, delta: Int): ChordQuality {
    val all = ChordQuality.entries
    return all[((quality.ordinal + delta) % all.size + all.size) % all.size]
}
