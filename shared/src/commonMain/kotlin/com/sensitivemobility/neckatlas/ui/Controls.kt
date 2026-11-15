package com.sensitivemobility.neckatlas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sensitivemobility.neckatlas.domain.AccidentalStyle
import com.sensitivemobility.neckatlas.domain.NoteNames
import com.sensitivemobility.neckatlas.domain.NoteNotation
import com.sensitivemobility.neckatlas.shared.resources.Res
import com.sensitivemobility.neckatlas.shared.resources.chords_sheet_root
import org.jetbrains.compose.resources.stringResource

// Les briques que partagent les onglets Accords et Gammes : même barre de choix, même carte d'information, même feuille.

/** Un champ à flèches : le nom au milieu (toucher ouvre la feuille de choix), une flèche de chaque côté pour passer au suivant. */
@Composable
fun Stepper(
    label: String,
    previousDescription: String,
    nextDescription: String,
    onStep: (Int) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onStep(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = previousDescription) }
        Text(
            label,
            Modifier.weight(1f).clickable(onClick = onOpen).padding(vertical = 14.dp),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        IconButton(onClick = { onStep(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = nextDescription) }
    }
}

/** Un bouton d'écoute : une flèche de lecture et son libellé. */
@Composable
fun PlayButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(2.dp))
        Text(label)
    }
}

/** La carte sous la barre de choix : un titre ambre avec ses boutons d'écoute à droite, puis quelques lignes de texte. */
@Composable
fun InfoCard(title: String, actions: @Composable RowScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            actions()
        }
        content()
    }
}

/**
 * La feuille de choix direct d'une fondamentale (les 12 notes) et d'un type (famille d'accords, gamme…), avec l'interrupteur
 * des noms de notes. Elle monte quand [open] est vrai.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T> RootAndTypeSheet(
    open: Boolean,
    onDismiss: () -> Unit,
    rootPc: Int,
    onRoot: (Int) -> Unit,
    notation: NoteNotation,
    accidentals: AccidentalStyle,
    typeTitle: String,
    types: List<T>,
    selectedType: T,
    typeLabel: @Composable (T) -> String,
    onType: (T) -> Unit,
    namesToggle: String,
    showNames: Boolean,
    onShowNames: (Boolean) -> Unit,
) {
    if (!open) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Section(stringResource(Res.string.chords_sheet_root)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (pitchClass in 0 until 12) {
                        FilterChip(
                            selected = pitchClass == rootPc,
                            onClick = { onRoot(pitchClass) },
                            label = { Text(NoteNames.label(pitchClass, notation, accidentals)) },
                        )
                    }
                }
            }
            Section(typeTitle) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { option ->
                        FilterChip(selected = option == selectedType, onClick = { onType(option) }, label = { Text(typeLabel(option)) })
                    }
                }
            }
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(namesToggle, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showNames, onCheckedChange = onShowNames)
            }
        }
    }
}
