package com.sensitivemobility.neckatlas.ui

import androidx.compose.runtime.Composable
import com.sensitivemobility.neckatlas.domain.Instrument
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.date_short
import com.sensitivemobility.neckatlas.shared.resources.time_seconds
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Une durée en minutes et secondes : « 1:05 ». */
fun clockText(millis: Long): String {
    val seconds = (millis.coerceAtLeast(0L) / 1000).toInt()
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** Un temps de réponse en secondes, à un dixième près, avec la virgule ou le point de la langue : « 1,4 s ». */
@Composable
fun secondsText(millis: Long): String {
    val tenths = (millis.coerceAtLeast(0L) / 100).toInt()
    return stringResource(Res.string.time_seconds, tenths / 10, tenths % 10)
}

/** Les numéros de corde d'un groupe de [count] cordes voisines dont la plus grave est l'indice [first] : « 3·2·1 ». */
fun stringsText(instrument: Instrument, first: Int, count: Int): String =
    (first until first + count).joinToString("·") { instrument.stringNumber(it).toString() }

/** Un pourcentage entier : « 87 % ». */
fun percentText(part: Int, whole: Int): String = "${if (whole == 0) 0 else (100.0 * part / whole).toInt()} %"

/** Le jour et le mois (dans le fuseau du téléphone) d'un instant, dans l'ordre de la langue : « 14/11 » ou « 11/14 ». */
@OptIn(ExperimentalTime::class)
@Composable
fun dateText(epochMillis: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return stringResource(Res.string.date_short, date.day, date.month.number)
}
