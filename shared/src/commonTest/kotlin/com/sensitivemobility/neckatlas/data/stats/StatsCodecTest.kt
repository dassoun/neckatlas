package com.sensitivemobility.neckatlas.data.stats

import com.sensitivemobility.neckatlas.domain.FretPosition
import com.sensitivemobility.neckatlas.domain.chord.ChordQuality
import com.sensitivemobility.neckatlas.domain.quiz.NoteStat
import com.sensitivemobility.neckatlas.domain.quiz.QuizMode
import com.sensitivemobility.neckatlas.domain.quiz.SessionEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsCodecTest {
    @Test
    fun notesSurviveARoundTrip() {
        val notes = List(12) { NoteStat(attempts = it * 3, correct = it * 2, totalMillis = it * 1_234L) }
        assertEquals(notes, StatsCodec.decodeNotes(StatsCodec.encodeNotes(notes)))
    }

    @Test
    fun historySurvivesARoundTrip() {
        val history = listOf(
            SessionEntry(1_700_000_000_000, QuizMode.FIND_ON_STRING, 8, 10, 42_000),
            SessionEntry(1_700_000_100_000, QuizMode.NAME_NOTE, 0, 0, 0),
            SessionEntry(1_700_000_200_000, QuizMode.FIND_ANYWHERE, 31, 40, 60_000),
        )
        assertEquals(history, StatsCodec.decodeHistory(StatsCodec.encodeHistory(history)))
    }

    @Test
    fun chordFamiliesSurviveARoundTrip() {
        val size = ChordQuality.entries.size
        val qualities = List(size) { NoteStat(attempts = it + 2, correct = it, totalMillis = it * 700L) }
        assertEquals(qualities, StatsCodec.decodeQualities(StatsCodec.encodeQualities(qualities)))
        assertEquals(size, StatsCodec.decodeQualities(null).size)
        assertEquals(size, StatsCodec.decodeQualities("1,1,1").size)
        assertEquals(12, StatsCodec.decodeNotes("1,1,1").size)
    }

    @Test
    fun theNewChordModesSurviveInTheHistory() {
        val history = QuizMode.entries.mapIndexed { index, mode -> SessionEntry(index * 1_000L, mode, 1, 2, 3_000) }
        assertEquals(history, StatsCodec.decodeHistory(StatsCodec.encodeHistory(history)))
    }

    @Test
    fun cellsSurviveARoundTrip() {
        val cells = mapOf(
            FretPosition(0, 0) to NoteStat(3, 2, 4_500),
            FretPosition(5, 24) to NoteStat(1, 1, 700),
            FretPosition(2, 7) to NoteStat(10, 0, 31_000),
        )
        assertEquals(cells, StatsCodec.decodeCells(StatsCodec.encodeCells(cells)))
        assertEquals(emptyMap(), StatsCodec.decodeCells(null))
        assertEquals(emptyMap(), StatsCodec.decodeCells(""))
        assertEquals("", StatsCodec.encodeCells(emptyMap()))
    }

    @Test
    fun cellsAreWrittenInAStableOrder() {
        val a = mapOf(FretPosition(3, 1) to NoteStat(1, 1, 1), FretPosition(0, 9) to NoteStat(2, 1, 2))
        val b = mapOf(FretPosition(0, 9) to NoteStat(2, 1, 2), FretPosition(3, 1) to NoteStat(1, 1, 1))
        assertEquals(StatsCodec.encodeCells(a), StatsCodec.encodeCells(b))
    }

    @Test
    fun damagedCellsAreSkipped() {
        val decoded = StatsCodec.decodeCells("1:5:3,2,500;oups;2:99:1,1,1;99:3:1,1,1;1:6:4,9,100;3:4:5,5,5000;1:2")
        // Gardées : 1:5 et 3:4. Écartées : texte illisible, case ou corde hors limites, plus de justes que de réponses, champs manquants.
        assertEquals(
            mapOf(FretPosition(1, 5) to NoteStat(3, 2, 500), FretPosition(3, 4) to NoteStat(5, 5, 5000)),
            decoded,
        )
    }

    @Test
    fun missingDataGivesEmptyStats() {
        assertEquals(List(12) { NoteStat() }, StatsCodec.decodeNotes(null))
        assertEquals(List(12) { NoteStat() }, StatsCodec.decodeNotes(""))
        assertEquals(emptyList(), StatsCodec.decodeHistory(null))
        assertEquals(emptyList(), StatsCodec.decodeHistory(""))
    }

    @Test
    fun aDamagedNoteIsIgnoredWithoutLosingTheOthers() {
        val decoded = StatsCodec.decodeNotes("3,2,500;oups;4,9,100;-1,0,0;5,5,5000")
        assertEquals(NoteStat(3, 2, 500), decoded[0])
        assertEquals(NoteStat(), decoded[1]) // illisible
        assertEquals(NoteStat(), decoded[2]) // plus de justes que de réponses
        assertEquals(NoteStat(), decoded[3]) // négatif
        assertEquals(NoteStat(5, 5, 5000), decoded[4])
        assertEquals(12, decoded.size)
    }

    @Test
    fun tooManyNotesAreCutAndTooFewArePadded() {
        assertEquals(12, StatsCodec.decodeNotes(List(20) { "1,1,1" }.joinToString(";")).size)
        assertEquals(12, StatsCodec.decodeNotes("1,1,1").size)
    }

    @Test
    fun damagedHistoryEntriesAreSkipped() {
        val text = "100,NAME_NOTE,3,4,5000;pas,une,entree;200,INCONNU,1,1,1;300,FIND_ANYWHERE,5,2,100;400,FIND_ON_STRING,1,2,3000"
        assertEquals(
            listOf(
                SessionEntry(100, QuizMode.NAME_NOTE, 3, 4, 5000),
                SessionEntry(400, QuizMode.FIND_ON_STRING, 1, 2, 3000),
            ),
            StatsCodec.decodeHistory(text),
        )
    }
}
