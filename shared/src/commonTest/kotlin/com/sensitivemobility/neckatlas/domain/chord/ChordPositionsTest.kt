package com.sensitivemobility.neckatlas.domain.chord

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChordPositionsTest {
    private val Do = 0
    private val Re = 2
    private val Mi = 4
    private val Fa = 5
    private val FaDiese = 6
    private val Sol = 7
    private val La = 9
    private val Si = 11

    private fun tabs(root: Int, quality: ChordQuality, instrument: Instrument = Instrument.GUITAR_6): List<String> =
        ChordPositions.find(instrument, Chord(root, quality)).map { it.tab }

    // --- Les accords que tout guitariste connaît doivent sortir tels quels -------------------------------------------

    @Test
    fun theOpenChordsEveryoneKnowsAreFound() {
        assertTrue("x 3 2 0 1 0" in tabs(Do, ChordQuality.MAJOR), "Do")
        assertTrue("x 0 2 2 2 0" in tabs(La, ChordQuality.MAJOR), "La")
        assertTrue("3 2 0 0 0 3" in tabs(Sol, ChordQuality.MAJOR), "Sol")
        assertTrue("0 2 2 1 0 0" in tabs(Mi, ChordQuality.MAJOR), "Mi")
        assertTrue("x x 0 2 3 2" in tabs(Re, ChordQuality.MAJOR), "Ré")
        assertTrue("x 0 2 2 1 0" in tabs(La, ChordQuality.MINOR), "La mineur")
        assertTrue("0 2 2 0 0 0" in tabs(Mi, ChordQuality.MINOR), "Mi mineur")
        assertTrue("x x 0 2 3 1" in tabs(Re, ChordQuality.MINOR), "Ré mineur")
    }

    @Test
    fun theOpenSeventhsAreFound() {
        assertTrue("x 3 2 3 1 0" in tabs(Do, ChordQuality.DOMINANT_7), "Do7")
        assertTrue("3 2 0 0 0 1" in tabs(Sol, ChordQuality.DOMINANT_7), "Sol7")
        assertTrue("0 2 0 1 0 0" in tabs(Mi, ChordQuality.DOMINANT_7), "Mi7")
        assertTrue("x x 0 2 1 2" in tabs(Re, ChordQuality.DOMINANT_7), "Ré7")
        assertTrue("x 2 1 2 0 2" in tabs(Si, ChordQuality.DOMINANT_7), "Si7")
        assertTrue("x 3 2 0 0 0" in tabs(Do, ChordQuality.MAJOR_7), "DoMaj7")
        assertTrue("x x 3 2 1 0" in tabs(Fa, ChordQuality.MAJOR_7), "FaMaj7")
        assertTrue("x 0 2 0 1 0" in tabs(La, ChordQuality.MINOR_7), "Lam7")
    }

    @Test
    fun theBarreChordsAreFound() {
        assertTrue("1 3 3 2 1 1" in tabs(Fa, ChordQuality.MAJOR), "Fa")
        assertTrue("x x 3 2 1 1" in tabs(Fa, ChordQuality.MAJOR), "Fa léger")
        assertTrue("x 2 4 4 3 2" in tabs(Si, ChordQuality.MINOR), "Si mineur")
        assertTrue("2 4 4 2 2 2" in tabs(FaDiese, ChordQuality.MINOR), "Fa♯ mineur")
        assertTrue("x 3 5 5 5 3" in tabs(Do, ChordQuality.MAJOR), "Do, forme de La")
        assertTrue("8 10 10 9 8 8" in tabs(Do, ChordQuality.MAJOR), "Do, forme de Mi")
        assertTrue("x x 10 12 13 12" in tabs(Do, ChordQuality.MAJOR), "Do, forme de Ré")
    }

    @Test
    fun theStandardFingeringsAreWhatATeacherWouldShow() {
        fun fingers(root: Int, quality: ChordQuality, tab: String) =
            ChordPositions.find(Instrument.GUITAR_6, Chord(root, quality)).first { it.tab == tab }.fingers
        assertEquals(listOf(null, 3, 2, null, 1, null), fingers(Do, ChordQuality.MAJOR, "x 3 2 0 1 0"))
        assertEquals(listOf(null, 2, 3, 1, null, null), fingers(Mi, ChordQuality.MAJOR, "0 2 2 1 0 0"))
        assertEquals(listOf(null, null, null, 1, 3, 2), fingers(Re, ChordQuality.MAJOR, "x x 0 2 3 2"))
        assertEquals(listOf(1, 3, 4, 2, 1, 1), fingers(Fa, ChordQuality.MAJOR, "1 3 3 2 1 1"))
        assertEquals(listOf(null, 1, 3, 4, 2, 1), fingers(Si, ChordQuality.MINOR, "x 2 4 4 3 2"))
    }

    @Test
    fun theLittleFHoldsItsTwoHighStringsWithTheIndexFinger() {
        val f = ChordPositions.find(Instrument.GUITAR_6, Chord(Fa, ChordQuality.MAJOR)).first { it.tab == "x x 3 2 1 1" }
        assertEquals(listOf(null, null, 3, 2, 1, 1), f.fingers) // 3 2 1 1, pas 4 3 1 2
        assertEquals(Barre(fret = 1, fromString = 4, toString = 5), f.barre)
        // Trois cordes voisines sur une seule case (le La « x02220 ») restent trois doigts : rien d'autre à jouer au-dessus.
        val a = ChordPositions.find(Instrument.GUITAR_6, Chord(La, ChordQuality.MAJOR)).first { it.tab == "x 0 2 2 2 0" }
        assertEquals(null, a.barre)
        assertEquals(3, a.fingerCount)
    }

    @Test
    fun aBarreChordReportsItsBarre() {
        val f = ChordPositions.find(Instrument.GUITAR_6, Chord(Fa, ChordQuality.MAJOR)).first { it.tab == "1 3 3 2 1 1" }
        assertEquals(Barre(fret = 1, fromString = 0, toString = 5), f.barre)
        assertEquals(4, f.fingerCount)
        val c = ChordPositions.find(Instrument.GUITAR_6, Chord(Do, ChordQuality.MAJOR)).first { it.tab == "x 3 2 0 1 0" }
        assertEquals(null, c.barre)
    }

    // --- Propriétés qui doivent tenir pour tous les accords, tous les instruments ------------------------------------

    private fun check(instrument: Instrument, chord: Chord, position: ChordPosition) {
        val label = "${instrument.name} ${chord.root} ${chord.quality.name} ${position.tab}"
        val board = Fretboard(instrument)
        assertEquals(instrument.stringCount, position.frets.size, label)
        val pitchClasses = position.frets.mapIndexed { s, f -> f?.let { board.pitchAt(FretPosition(s, it)).pitchClass } }

        // La fondamentale est la note la plus grave.
        assertEquals(chord.root, pitchClasses[position.lowestString], "$label : basse")
        // Seulement des notes de l'accord, et toutes celles dont on ne peut pas se passer.
        val played = pitchClasses.filterNotNull().toSet()
        assertTrue(chord.pitchClasses.toSet().containsAll(played), "$label : notes étrangères")
        val needed = if (chord.quality in setOf(ChordQuality.DOMINANT_7, ChordQuality.MAJOR_7, ChordQuality.MINOR_7)) {
            setOf(chord.pitchClasses[0], chord.pitchClasses[1], chord.pitchClasses[3])
        } else {
            chord.pitchClasses.toSet()
        }
        assertTrue(played.containsAll(needed), "$label : il manque une note de l'accord")
        // Les cordes jouées se suivent.
        val strings = position.playedStrings
        assertEquals((strings.first()..strings.last()).toList(), strings, "$label : trou dans les cordes")
        // La main tient dans quatre cases, avec au plus quatre doigts.
        assertTrue(position.highestFret - position.lowestFret <= ChordPositions.MAX_SPAN, "$label : main trop ouverte")
        assertTrue(position.fingerCount <= ChordPositions.MAX_FINGERS, "$label : trop de doigts")
        // Les cordes à vide ne servent que près du sillet.
        if (position.usesOpenStrings) {
            assertTrue(position.highestFret <= ChordPositions.OPEN_MAX_FRET, "$label : corde à vide trop haut")
        }
        // Un doigt sur chaque corde appuyée, et seulement sur celles-là ; un rôle sur chaque corde jouée.
        for (s in position.frets.indices) {
            val fret = position.frets[s]
            assertEquals(fret != null && fret > 0, position.fingers[s] != null, "$label : doigt corde $s")
            assertEquals(fret != null, position.roles[s] != null, "$label : rôle corde $s")
        }
        assertEquals(position.playedStrings.size, position.pitches.size, label)
        // Un barré ne passe jamais sur une corde à vide, étouffée ou plus basse que lui.
        position.barre?.let { barre ->
            for (s in barre.fromString..barre.toString) {
                val fret = position.frets[s]
                assertNotNull(fret, "$label : barré sur une corde étouffée")
                assertTrue(fret >= barre.fret, "$label : barré sur une case plus basse")
            }
        }
    }

    @Test
    fun everyChordHasSeveralValidPositionsOnAGuitar() {
        for (root in 0..11) {
            for (quality in ChordQuality.entries) {
                val chord = Chord(root, quality)
                val positions = ChordPositions.find(Instrument.GUITAR_6, chord)
                assertTrue(positions.size >= 2, "${chord.root} ${quality.name} : ${positions.size} position(s)")
                assertTrue(positions.size <= ChordPositions.DEFAULT_LIMIT)
                positions.forEach { check(Instrument.GUITAR_6, chord, it) }
            }
        }
    }

    @Test
    fun positionsAreDistinctAndOpenOnesComeFirst() {
        for (root in 0..11) {
            val positions = ChordPositions.find(Instrument.GUITAR_6, Chord(root, ChordQuality.MAJOR))
            assertEquals(positions.size, positions.map { it.tab }.toSet().size)
            val firstClosed = positions.indexOfFirst { !it.usesOpenStrings }.let { if (it < 0) positions.size else it }
            assertTrue(positions.drop(firstClosed).none { it.usesOpenStrings }, "racine $root : les accords à vide d'abord")
            val closed = positions.filter { !it.usesOpenStrings }.map { it.lowestFret }
            assertEquals(closed.sorted(), closed, "racine $root : formes mobiles dans l'ordre du manche")
        }
    }

    @Test
    fun theLimitIsHonoured() {
        val positions = ChordPositions.find(Instrument.GUITAR_6, Chord(Do, ChordQuality.MAJOR), limit = 3)
        assertEquals(3, positions.size)
    }

    @Test
    fun otherInstrumentsWorkToo() {
        for (instrument in listOf(Instrument.GUITAR_7, Instrument.BASS_4, Instrument.BASS_5)) {
            for (root in 0..11) {
                for (quality in listOf(ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.DOMINANT_7)) {
                    val chord = Chord(root, quality)
                    val positions = ChordPositions.find(instrument, chord)
                    assertTrue(positions.isNotEmpty(), "${instrument.name} ${chord.root} ${quality.name} : aucune position")
                    positions.forEach { check(instrument, chord, it) }
                }
            }
        }
    }

    @Test
    fun theZoneShowsTheNutNearTheNutAndOnlyTheHandHigherUp() {
        val all = ChordPositions.find(Instrument.GUITAR_6, Chord(Do, ChordQuality.MAJOR))
        val open = all.first { it.tab == "x 3 2 0 1 0" }
        assertEquals(0, ChordPositions.zoneOf(open).first)
        assertTrue(ChordPositions.zoneOf(open).last >= open.highestFret)
        val high = all.first { it.tab == "8 10 10 9 8 8" }
        val zone = ChordPositions.zoneOf(high)
        assertEquals(8, zone.first) // la main commence à la case 8
        assertTrue(zone.last >= high.highestFret)
        assertTrue(zone.last - zone.first >= ChordPositions.MAX_SPAN) // au moins quatre cases
        // Le Fa barré à la première case garde le sillet : ses cordes étouffées ou à vide se montrent sur sa rangée.
        val f = ChordPositions.find(Instrument.GUITAR_6, Chord(Fa, ChordQuality.MAJOR)).first { it.tab == "1 3 3 2 1 1" }
        assertEquals(0, ChordPositions.zoneOf(f).first)
    }

    @Test
    fun everyPositionFitsInItsZone() {
        for (root in 0..11) {
            for (quality in ChordQuality.entries) {
                for (position in ChordPositions.find(Instrument.GUITAR_6, Chord(root, quality))) {
                    val zone = ChordPositions.zoneOf(position)
                    assertTrue(position.frets.filterNotNull().all { it in zone.frets }, "${position.tab} hors de ${zone.first}-${zone.last}")
                }
            }
        }
    }
}
