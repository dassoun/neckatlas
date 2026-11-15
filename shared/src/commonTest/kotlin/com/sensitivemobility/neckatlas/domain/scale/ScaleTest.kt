package com.sensitivemobility.neckatlas.domain.scale

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.FretZone
import com.sensitivemobility.neckatlas.domain.Fretboard
import com.sensitivemobility.neckatlas.domain.Instrument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScaleTest {
    private val Do = 0
    private val Re = 2
    private val Mi = 4
    private val Fa = 5
    private val Sol = 7
    private val La = 9
    private val Si = 11

    @Test
    fun cMajorIsTheWhiteKeys() {
        assertEquals(listOf(0, 2, 4, 5, 7, 9, 11), Scale(Do, ScaleType.MAJOR).pitchClasses)
    }

    @Test
    fun gMajorHasAnFSharp() {
        assertEquals(listOf(7, 9, 11, 0, 2, 4, 6), Scale(Sol, ScaleType.MAJOR).pitchClasses)
    }

    @Test
    fun theMinorPentatonicOfAIsTheMajorPentatonicOfC() {
        assertEquals(
            Scale(Do, ScaleType.MAJOR_PENTATONIC).pitchClasses.toSet(),
            Scale(La, ScaleType.MINOR_PENTATONIC).pitchClasses.toSet(),
        )
    }

    @Test
    fun modesAreTheSameNotesAsTheirParentMajor() {
        // Do majeur : Ré dorien, Mi phrygien, Fa lydien, Sol mixolydien, La éolien (mineur naturel), Si locrien.
        val parent = Scale(Do, ScaleType.MAJOR).pitchClasses.toSet()
        assertEquals(parent, Scale(Re, ScaleType.DORIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(Mi, ScaleType.PHRYGIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(Fa, ScaleType.LYDIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(Sol, ScaleType.MIXOLYDIAN).pitchClasses.toSet())
        assertEquals(parent, Scale(La, ScaleType.NATURAL_MINOR).pitchClasses.toSet())
        assertEquals(parent, Scale(Si, ScaleType.LOCRIAN).pitchClasses.toSet())
    }

    @Test
    fun harmonicMinorRaisesTheSeventh() {
        // La mineur harmonique : la mineur naturel avec un Sol♯.
        assertEquals(listOf(9, 11, 0, 2, 4, 5, 8), Scale(La, ScaleType.HARMONIC_MINOR).pitchClasses)
    }

    @Test
    fun theBluesAddsTheBlueNoteToTheMinorPentatonic() {
        val blues = Scale(La, ScaleType.BLUES).pitchClasses.toSet()
        val pentatonic = Scale(La, ScaleType.MINOR_PENTATONIC).pitchClasses.toSet()
        assertEquals(setOf(3), blues - pentatonic) // le Mi♭ (♭5 de La)
        assertEquals(emptySet(), pentatonic - blues)
    }

    @Test
    fun everyTypeIsWellFormed() {
        for (type in ScaleType.entries) {
            assertEquals(type.intervals.size, type.degrees.size, type.name)
            assertEquals("1", type.degrees.first(), type.name)
            assertEquals(type.noteCount, Scale(Do, type).pitchClasses.toSet().size, "${type.name} : notes distinctes")
        }
    }

    @Test
    fun degreesAreFoundByPitchClass() {
        val scale = Scale(Sol, ScaleType.MAJOR)
        assertEquals(0, scale.degreeOf(Sol))
        assertEquals(6, scale.degreeOf(6)) // Fa♯, la septième
        assertEquals(null, scale.degreeOf(Fa)) // Fa naturel n'est pas dans Sol majeur
    }

    @Test
    fun allNotesAreOnlyScaleNotesAndCoverEveryOne() {
        val board = Fretboard(Instrument.GUITAR_6)
        val scale = Scale(Do, ScaleType.MAJOR)
        val notes = ScalePositions.allNotes(board, FretZone.DEFAULT, scale)
        assertTrue(notes.all { board.pitchAt(it.position).pitchClass in scale.pitchClasses })
        // Dans les 12 premières cases chaque classe de hauteur apparaît sur chaque corde : sept notes x 6 cordes, plus la 12e case.
        assertEquals(scale.pitchClasses.toSet(), notes.map { board.pitchAt(it.position).pitchClass }.toSet())
        assertEquals(0, notes.first { board.pitchAt(it.position).pitchClass == Do }.degree)
    }

    @Test
    fun aSevenNoteScaleHasSevenPositionsAndAPentatonicFive() {
        val board = Fretboard(Instrument.GUITAR_6)
        assertEquals(7, ScalePositions.find(board, Scale(Do, ScaleType.MAJOR)).size)
        assertEquals(5, ScalePositions.find(board, Scale(La, ScaleType.MINOR_PENTATONIC)).size)
        // Le blues est une pentatonique mineure et sa note bleue : cinq positions.
        assertEquals(5, ScalePositions.find(board, Scale(La, ScaleType.BLUES)).size)
    }

    private val everyCase = buildList {
        for (instrument in Instrument.entries) for (type in ScaleType.entries) for (root in 0..11) add(Triple(instrument, type, root))
    }

    @Test
    fun positionNStartsOnDegreeNOnTheLowString() {
        for ((instrument, type, root) in everyCase) {
            val board = Fretboard(instrument)
            val scale = Scale(root, type)
            val positions = ScalePositions.find(board, scale)
            assertEquals(type.coreDegrees.size, positions.size, "$instrument $type $root")
            positions.forEachIndexed { index, position ->
                val first = position.notes.first { it.position.string == 0 }
                assertEquals(type.coreDegrees[index], first.degree, "$instrument $type $root position ${index + 1}")
                assertTrue(first.position.fret in 0..23 && (first.position.fret % 12) == (0..11).first { board.pitchAt(FretPosition(0, it)).pitchClass == first.let { n -> scale.pitchClasses[n.degree] } }, "$instrument $type $root position ${index + 1} : case ${first.position.fret}")
            }
        }
    }

    @Test
    fun theFirstPositionOfCMajorIsTheClassicOne() {
        val board = Fretboard(Instrument.GUITAR_6)
        val first = ScalePositions.find(board, Scale(Do, ScaleType.MAJOR)).first()
        // Mi grave : Do Ré Mi (8, 10, 12) ; La : Fa Sol La (8, 10, 12) ; Ré : Si Do Ré (9, 10, 12) ; Sol : Mi Fa Sol (9, 10, 12) ;
        // Si : La Si Do (10, 12, 13) ; Mi aigu : Ré Mi Fa (10, 12, 13).
        val expected = listOf(
            listOf(8, 10, 12), listOf(8, 10, 12), listOf(9, 10, 12), listOf(9, 10, 12), listOf(10, 12, 13), listOf(10, 12, 13),
        )
        for (string in 0 until 6) {
            assertEquals(expected[string], first.notes.filter { it.position.string == string }.map { it.position.fret }, "corde $string")
        }
        // Le 3e degré (Mi) y est : sur la corde grave, case 12, et sur la corde de Sol, case 9.
        assertTrue(first.notes.any { it.degree == 2 })
    }

    @Test
    fun everyPositionOfEveryScaleHasEveryDegreeAndTheSameNumberOfNotesOnEachString() {
        for ((instrument, type, root) in everyCase) {
            val board = Fretboard(instrument)
            val scale = Scale(root, type)
            val perString = if (type.coreDegrees.size >= 7) 3 else 2
            ScalePositions.find(board, scale).forEachIndexed { index, position ->
                val where = "$instrument $type $root position ${index + 1}"
                // Tous les degrés de la gamme (hors note de passage) sont dans la position : aucun degré ne manque.
                assertTrue(position.notes.map { it.degree }.toSet().containsAll(type.coreDegrees), "$where : degrés ${position.notes.map { it.degree }.toSet()}")
                for (string in 0 until instrument.stringCount) {
                    val core = position.notes.filter { it.position.string == string && it.degree !in type.passing }
                    assertEquals(perString, core.size, "$where, corde $string")
                }
                // Que des notes de la gamme, sans doublon, sur le manche.
                assertTrue(position.notes.all { scale.degreeOf(board.pitchAt(it.position).pitchClass) == it.degree }, where)
                assertEquals(position.notes.size, position.notes.map { it.position }.toSet().size, where)
                assertTrue(position.notes.all { it.position.fret in 0..FretZone.MAX_FRET }, where)
            }
        }
    }

    @Test
    fun notesOnAStringFollowEachOtherInTheScaleWithoutSkippingOne() {
        for ((instrument, type, root) in everyCase) {
            val board = Fretboard(instrument)
            val scale = Scale(root, type)
            ScalePositions.find(board, scale).forEachIndexed { index, position ->
                for (string in 0 until instrument.stringCount) {
                    val frets = position.notes.filter { it.position.string == string && it.degree !in type.passing }.map { it.position.fret }.sorted()
                    val between = (frets.min()..frets.max()).count {
                        val d = scale.degreeOf(board.pitchAt(FretPosition(string, it)).pitchClass)
                        d != null && d !in type.passing
                    }
                    assertEquals(frets.size, between, "$instrument $type $root position ${index + 1}, corde $string : cases $frets")
                }
            }
        }
    }

    @Test
    fun aPositionStaysUnderTheHand() {
        // Quatre cases d'écart au plus par corde, et un dessin d'au plus huit cases de large (la seconde augmentée de la mineure harmonique l'étire) : pas d'éparpillement.
        for ((instrument, type, root) in everyCase) {
            val board = Fretboard(instrument)
            ScalePositions.find(board, Scale(root, type)).forEachIndexed { index, position ->
                val where = "$instrument $type $root position ${index + 1}"
                assertTrue(position.lastFret - position.firstFret <= 7, "$where : cases ${position.firstFret}-${position.lastFret}")
                for (string in 0 until instrument.stringCount) {
                    val frets = position.notes.filter { it.position.string == string }.map { it.position.fret }
                    assertTrue(frets.max() - frets.min() <= 4, "$where, corde $string : $frets")
                }
            }
        }
    }

    @Test
    fun theBlueNoteSitsBetweenTwoNotesOfAPentatonicBox() {
        val board = Fretboard(Instrument.GUITAR_6)
        val scale = Scale(La, ScaleType.BLUES)
        val box1 = ScalePositions.find(board, scale).first()
        // La mineur : le Mi♭ (♭5, degré 3) est case 6 de la corde de Ré, entre le Ré (5) et le Mi (7).
        assertTrue(box1.notes.any { it.degree == 3 })
        assertTrue(box1.notes.filter { it.degree == 3 }.all { note ->
            val others = box1.notes.filter { it.position.string == note.position.string && it.degree != 3 }.map { it.position.fret }
            note.position.fret in others.min()..others.max()
        })
    }

    @Test
    fun theScaleGoesUpOneOctaveFromTheLowestRoot() {
        val board = Fretboard(Instrument.GUITAR_6)
        val notes = ScalePositions.allNotes(board, FretZone.DEFAULT, Scale(Do, ScaleType.MAJOR))
        val midi = ScalePositions.ascending(board, notes).map { it.midi }
        // Do3 (48) à Do4 (60) : Do Ré Mi Fa Sol La Si Do.
        assertEquals(listOf(48, 50, 52, 53, 55, 57, 59, 60), midi)
    }

    @Test
    fun withoutARootTheScaleJustGoesUp() {
        val board = Fretboard(Instrument.GUITAR_6)
        val noRoot = ScalePositions.allNotes(board, FretZone(1, 2), Scale(Do, ScaleType.MAJOR)).filter { it.degree != 0 }
        val midi = ScalePositions.ascending(board, noRoot).map { it.midi }
        assertEquals(midi.sorted(), midi)
        assertEquals(midi.distinct(), midi)
        assertTrue(midi.isNotEmpty())
    }

    @Test
    fun scalesWorkOnABassToo() {
        val board = Fretboard(Instrument.BASS_4)
        val positions = ScalePositions.find(board, Scale(Do, ScaleType.MAJOR))
        assertEquals(7, positions.size)
        assertTrue(positions.all { p -> p.notes.all { it.position.string in 0..3 } })
    }
}
