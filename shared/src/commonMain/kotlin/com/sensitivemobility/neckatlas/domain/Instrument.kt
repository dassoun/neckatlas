package com.sensitivemobility.neckatlas.domain

/** Famille de sonorité d'un instrument : décide du timbre de la corde pincée quand l'appli joue une note. */
enum class Timbre { GUITAR, BASS }

/**
 * Un instrument à cordes accordé à vide. Les cordes sont rangées de la plus grave à la plus aiguë (indice 0 = la plus
 * grave) ; par convention on numérote pourtant les cordes à partir de l'aiguë (corde 1 = Mi aigu d'une guitare).
 */
enum class Instrument(midiOpenStrings: List<Int>, val timbre: Timbre) {
    /** Guitare standard : Mi La Ré Sol Si Mi. */
    GUITAR_6(listOf(40, 45, 50, 55, 59, 64), Timbre.GUITAR),

    /** Guitare 7 cordes : Si grave ajouté. */
    GUITAR_7(listOf(35, 40, 45, 50, 55, 59, 64), Timbre.GUITAR),

    /** Basse 4 cordes : Mi La Ré Sol. */
    BASS_4(listOf(28, 33, 38, 43), Timbre.BASS),

    /** Basse 5 cordes : Si grave ajouté. */
    BASS_5(listOf(23, 28, 33, 38, 43), Timbre.BASS);

    val openStrings: List<Pitch> = midiOpenStrings.map { Pitch(it) }

    val stringCount: Int get() = openStrings.size

    /** Numéro de corde tel qu'on le dit à voix haute : 1 pour la plus aiguë. */
    fun stringNumber(stringIndex: Int): Int = stringCount - stringIndex
}
