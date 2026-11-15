# Neck Atlas

Application pour apprendre le manche d'une guitare ou d'une basse : où se trouve chaque note, puis les arpèges et les
triades (Kotlin Multiplatform + Compose Multiplatform, comme StillDive). Données 100 % locales, sur le téléphone.

Instruments : guitare 6 cordes (par défaut), guitare 7 cordes, basse 4 et 5 cordes (Si grave). Android d'abord ; iPhone
prévu dans le même code, mais **jamais compilé** (pas de Mac, voir `iosApp/README.md`).

## Fonctionnalités

Faites :

- **Explorateur de manche** : le manche est **debout**, sur toute la hauteur de l'écran (sillet en haut, une case par rangée,
  cordes graves à gauche comme sur un diagramme d'accord) : toute la zone de travail est visible d'un coup, sans défilement,
  et plus la zone est petite, plus les cases sont grandes. Un manche **en bois** (palissandre, frettes argentées, sillet d'os,
  incrustations nacrées, cordes de plus en plus épaisses vers le grave, filées pour les graves : voir « Apparence »). On
  touche un endroit pour **entendre** la note et connaître son nom, son octave, sa fréquence, sa corde et sa case ; la corde
  vibre, une onde part de la case et le téléphone vibre légèrement. Une rangée
  de 12 pastilles éclaire une note partout où elle se joue dans la zone ; « Tous les noms » affiche le nom de chaque note.
- **Sons** : les notes viennent de vraies prises de son (une guitare électrique et une basse, libres de droits), accordées à
  6 centièmes de ton près au pire, 1 ou 2 en général (mesuré sur toutes les notes, voir « Sons » plus bas). Une même note de
  guitare alterne trois variantes. Si les fichiers manquent, l'appli se rabat sur une corde pincée synthétisée
  (Karplus-Strong). « Cordes à vide » les joue l'une après l'autre. Réglable (Réglages > Son).
- **Instrument et zone de travail** (bouton en haut de l'écran) : guitare 6 (défaut), guitare 7, basse 4 ou 5 ; les 12
  premières cases par défaut, 22 ou 24 d'un geste, ou n'importe quelle plage (« cases 8 à 12 ») au curseur.
- **Accords** (onglet Accords) : triades (majeure, mineure, diminuée, augmentée) et accords de septième (dominante, majeure 7,
  mineure 7, demi-diminuée, diminuée 7) sur les 12 fondamentales. Deux affichages :
  « **Forme d'accord** » : une note par corde sur des cordes voisines (3·2·1, 4·3·2…), avec le renversement au choix
  (fondamentale, 1er, 2e et, pour une septième, 3e) ; ne sont proposées que les formes *serrées* (les notes montent de corde en
  corde et tiennent dans moins d'une octave) et jouables (au plus 5 cases de la main), dans la zone de travail. S'il y en
  a plusieurs (24 cases : la même forme revient une octave plus haut), un choix « Position 1, 2… » met l'une en avant et
  laisse les autres en filigrane. « **Arpège** » : toutes les notes de l'accord dans la zone, sur toutes les cordes (réduire
  la zone à une boîte de 4 ou 5 cases donne le dessin classique d'un arpège). Chaque note porte la couleur de son rôle
  (fondamentale ambre, tierce turquoise, quinte bleue, septième violette) et son étiquette (R, ♭3, 5, ♭7… ou le nom de la
  note). Tout se joue : « Accord » (les cordes grattées à la suite, du grave à l'aigu) et « Arpège » (note par note) ; toucher
  une case fait entendre sa note.
  « **Positions** » : le dictionnaire d'accords. Les accords tels qu'on les joue : accords ouverts (Do `x32010`, Sol
  `320003`, Mi `022100`…), formes barrées et formes mobiles plus haut sur le manche, avec cordes étouffées (×), cordes à
  vide (0), barré et numéro de doigt sur chaque note (interrupteur « Doigts », sinon le rôle ou le nom de la note). Une
  rangée de petits diagrammes montre toutes les positions de l'accord ; en toucher un l'affiche en grand sur le manche, avec
  sa tablature (« Accord ouvert · x 3 2 0 1 0 »). Pas de tableau écrit à la main : les positions sont calculées pour
  l'instrument choisi (guitare 6 ou 7, basse 4 ou 5), donc les 12 fondamentales et les 9 familles marchent partout.
- **Gammes** (onglet Gammes) : le dictionnaire de gammes. 12 gammes : majeure, les modes (dorien, phrygien, lydien,
  mixolydien, mineure naturelle, locrien), mineure harmonique et mélodique, pentatoniques majeure et mineure, blues, sur les
  12 fondamentales. Le manche montre les notes de la gamme avec leur **degré** (1, ♭3, 5… ou le nom de la note), la
  fondamentale en ambre : toutes dans la zone de travail, ou une **position** à la fois (quatre cases, sans bouger la main :
  sept positions pour une gamme de sept notes, cinq pour une pentatonique), les cases voisines en filigrane pour situer la
  main. « Jouer la gamme » la fait monter sur une octave (ou toute la position choisie) ; toucher une case fait entendre sa note.
- **Accordeur** (onglet Manche, bouton en haut à droite, puis « Accordeur » sous le choix de l'instrument) : l'appli écoute
  le micro et montre la note de la corde jouée, sa fréquence et l'écart en cents sur une jauge (« Juste » sous 5 cents). En
  « Auto » l'accordeur reconnaît la corde la plus proche ; toucher une corde la vise et la fait entendre, pour accorder à
  l'oreille. Marche pour les quatre instruments, basse 5 cordes comprise. Le son est analysé sur le téléphone, jamais
  enregistré ni envoyé ; la permission du micro n'est demandée qu'à l'ouverture de l'accordeur.
- **Quiz** (onglet Quiz) : trois exercices sur le manche debout, dans la zone de travail choisie.
  « **Trouver sur une corde** » (la note à trouver s'affiche en gros dans une pastille, la corde est cernée sur le manche, on touche la case),
  « **Trouver n'importe où** » (une fenêtre de 4 cases sur 3 cordes voisines, tirée au sort et toujours garantie de contenir la note, est éclairée ; le reste du manche est assombri et ignore les touchers) et « **Nommer la note** » (une case
  porte un « ? », on choisit son nom ; la note est jouée à l'oreille). Réglable : cordes interrogées (une ou plusieurs),
  notes naturelles ou toutes, 10 ou 20 questions, ou 60 secondes. Après chaque réponse : correction en vert et rouge, la
  bonne note jouée après une erreur. Bilan de la série (score, temps moyen, durée, notes ratées) et « Rejouer ». Le dernier
  réglage est mémorisé.
- **Quiz des accords** (mêmes réglages de durée, mêmes bilans) : « **Nommer l'accord** » (une forme d'accord s'allume avec
  ses notes anonymes et jouées ; on choisit sa fondamentale et son type, dans l'ordre qu'on veut, la réponse part quand les
  deux sont choisis ; la correction révèle le rôle de chaque note), « **Placer l'accord** » (« Place Do majeur, premier
  renversement, sur les cordes 3·2·1 » : les cordes sont surlignées, on touche une note par corde, la réponse est jugée
  dès que chaque corde en a une ; seule la note compte, pas l'octave ; une erreur montre la forme juste) et « **Accord à
  l'oreille** » (on entend un accord sur une fondamentale donnée, avec « Réécouter » ; on choisit son type). Réglable :
  fondamentales naturelles ou toutes, triades seules ou aussi septièmes, position fondamentale seule ou avec renversements.
  Les formes sont celles de l'onglet Accords, dans la zone de travail ; si la zone est trop petite pour en contenir une, le
  bouton Commencer est grisé avec un message.
- **Rappel des notes ratées** : les questions sont tirées au sort avec un poids par note ou, pour les accords, par type
  d'accord (jusqu'à 5 pour un élément toujours raté, 1 pour un maîtrisé, taux lissé), augmenté encore pour ce qui est raté
  pendant la série en cours (`QuestionGenerator`, `ChordQuestionGenerator`, `QuizStats.weight`, testés).
- **Progrès** (onglet Progrès) : bilan global, tableau des 12 notes et des 9 types d'accords (réussite, temps moyen, teinte
  du vert au rouge), éléments à retravailler (réussis à moins de 90 % sur au moins 3 réponses), 10 dernières séries, et
  effacement des statistiques avec confirmation. Tout est enregistré sur le téléphone (`StatsRepository`, dans le fichier de
  préférences). Une réponse sur un accord compte pour son type, pas pour sa fondamentale.
- **Carte de chaleur** (onglet Progrès) : le manche d'un instrument (au choix parmi les quatre), avec une pastille sur chaque
  case où le quiz a interrogé, colorée selon la réussite (vert 90 % et plus, turquoise 70 à 89, ambre 50 à 69, rouge moins
  de 50) ; une case sans pastille n'a jamais été demandée ; toucher une case donne sa note, sa réussite et son temps moyen.
  Elle se remplit avec « Trouver sur une corde » (une réussite crédite la case touchée, une erreur les cases qu'il fallait
  trouver) et « Nommer la note » (la case montrée) ; « Trouver n'importe où » et les accords ne visent pas de case précise
  et n'y comptent pas. Les résultats sont gardés par instrument, seulement à partir des séries jouées après l'ajout de la
  carte. Les cases ratées reviennent aussi plus souvent dans les questions (`QuizStats.cellFactor`, de 1/3 pour une case
  maîtrisée à 5/3 pour une case toujours ratée, multiplié au poids de la note).
- **Réglages** : son, retour vibré au toucher (activé par défaut), noms des notes (Do Ré Mi par défaut, ou C D E), altérations (dièses et bémols ensemble par défaut, ou
  l'un des deux), manche pour gaucher (miroir), thème (sombre par défaut, clair ou système), langue (Système par défaut,
  Français ou English). Enregistrés sur le téléphone (DataStore).
- Français et anglais, au choix dans Réglages > Langue ; par défaut, la langue du téléphone (voir « Langue » plus bas).
  Verrouillé en portrait sur téléphone (le manche est fait pour la hauteur) ; les tablettes tournent librement.

À faire (dans cet ordre probable) :

1. Publication Google Play.
2. Carte de chaleur pour les accords (réussite par forme et par groupe de cordes), si le besoin se confirme.

## Structure

| Dossier | Contenu |
|---|---|
| `shared/` | Tout le code commun : domaine, préférences, écrans, ressources FR/EN |
| `shared/src/androidMain` | Spécifique Android (barres système, sortie audio AudioTrack) |
| `shared/src/iosMain` | Spécifique iOS (**non compilé** : point d'entrée, sortie audio AVAudioEngine) |
| `androidApp/` | Point d'entrée Android |
| `iosApp/` | Sources Swift, projet Xcode à créer sur un Mac |
| `design/`, `tools/` | Logo Sensitive Mobility (SVG) et icône de l'appli (WebP, plus la version 512 px pour Google Play), avec les scripts qui en tirent les ressources Android ; `tools/make_samples.py` prépare les échantillons sonores |

Code commun (`shared/src/commonMain/kotlin/com/sensitivemobility/neckatlas`) :

- `domain/` : `Pitch` (hauteur MIDI), `NoteNames` (noms des notes), `Instrument` (accordages), `Fretboard` et `FretZone`.
  Du Kotlin pur, testé (`shared/src/commonTest`).
- `audio/` : `SampleBank` (lit les échantillons embarqués, choisit le plus proche, alterne les variantes, se rabat sur le
  synthétiseur si un fichier manque), `SampleManifest` (l'inventaire des notes enregistrées), `SampleShifter` (accorde un
  échantillon sur une autre note), `PluckSynth` (le synthétiseur de repli, testé), `NotePlayer` (joue une note ou une suite)
  et `PcmOutput` (interface de la sortie audio, une implémentation par plateforme ; mélange plusieurs notes à la fois).
- `domain/chord/` : `ChordQuality` (les 9 familles et leurs intervalles), `Chord`, `Voicings` (formes sur cordes voisines,
  renversements, arpèges) et `ChordPositions` (le dictionnaire de positions : voir plus bas). Du Kotlin pur, testé (formes
  connues comme Sol majeur 12-12-10 ou Do/Mi 9-8-8, accords ouverts et barrés connus, propriétés sur tous les instruments et
  toutes les fondamentales).
- `domain/scale/` : `ScaleType` (les 12 gammes : intervalles et nom des degrés), `Scale`, `ScalePositions` (les positions
  de quatre cases et la gamme qui monte). Du Kotlin pur, testé.
- `domain/quiz/` : `QuizConfig`, `Question` (notes et accords), `QuestionGenerator` et `ChordQuestionGenerator` (tirage
  pondéré), `QuizRules` (ce qui est juste) et `QuizStats` (résultats par note et par type d'accord, historique). Du Kotlin
  pur, testé.
- `data/prefs/` : `AppSettings` (dont le dernier réglage du quiz) et `SettingsRepository` (DataStore).
  `data/stats/` : `StatsRepository` et `StatsCodec` (écriture texte des statistiques, tolérante aux données abîmées).
- `feature/fretboard/` : l'écran et le dessin du manche (aussi utilisé par le quiz et les accords, via les pastilles
  `NeckMarker`, et les barrés `NeckBar`). `feature/chords/` : l'écran des accords et les petits diagrammes
  (`ChordDiagramCard`). `feature/scales/` : l'écran des gammes. `feature/setup/` : choix de
  l'instrument et de la zone, commun au manche et au quiz. `feature/quiz/` : `QuizController` (déroulement d'une série, au-dessus
  des onglets) et les écrans. `feature/progress/` : les statistiques. `feature/settings/` : les réglages.
- `ui/` : thème, écran de démarrage, textes des choix, et les briques que partagent Accords et Gammes (`Controls.kt` :
  champ à flèches, bouton d'écoute, carte d'information, feuille de choix de la fondamentale et du type).
- `domain/tuner/` : `PitchDetector` (YIN), `Tuning` (cents, corde la plus proche) et `PitchSmoother`. `audio/AudioInput.kt` : le micro
  (AudioRecord sur Android, AVAudioEngine sur iOS) et la permission. `feature/tuner/` : `TunerController` et l'écran.
- `AppContainer` : les services partagés, créés par chaque plateforme.

### Ajouter un écran

Ajouter une entrée à l'énumération `Tab` dans `App.kt`, son écran dans `feature/<nom>/` et ses textes dans les deux
`strings.xml` (`composeResources/values` et `values-fr`). Dans une chaîne, éviter le caractère `%` seul (les arguments
s'écrivent `%1$d`, `%2$s`) : le pourcentage se fabrique dans le code (`percentText`).

## Positions d'accords et gammes

**Positions d'accords** (`domain/chord/ChordPositions.kt`). On cherche, pour l'instrument, toutes les façons de jouer l'accord
qui se jouent vraiment, puis on garde les plus faciles. Une position est retenue si : la fondamentale est la note la plus
grave ; toutes les notes de l'accord y sont (la quinte d'une septième majeure, mineure ou de dominante peut manquer) ; les
cordes jouées se suivent (on n'étouffe que les cordes graves ou aiguës de bout) ; les cases appuyées tiennent dans quatre
cases (`MAX_SPAN`) et demandent au plus quatre doigts ; les cordes à vide ne servent que près du sillet (jusqu'à la case 4,
`OPEN_MAX_FRET`). Le doigté suit l'ordre des cases (index sur la plus basse) ; quand il y a plus de cordes appuyées que de
doigts, un barré à plat sur la case la plus basse en remplace plusieurs (jamais sur une corde à vide ou étouffée), et deux
cordes voisines à la case la plus basse avec d'autres cases au-dessus se tiennent d'un index (le petit Fa `xx3211`, doigts
3·2·1·1). Le classement (`ChordPosition.effort`) préfère peu de doigts, peu de cordes étouffées, pas de barré, proche du
sillet, une main resserrée ; une version réduite d'une autre position (mêmes cases, cordes en moins) est écartée, et deux
positions qui partent de la même corde et de la même case sont des variantes dont on ne garde que la plus facile. Résultat :
au plus 8 positions, les accords « de première position » d'abord, puis les formes mobiles en montant le manche (pour Do
majeur : `x32010`, les formes de La, Sol, Mi et Ré en barré). **Limites connues** : c'est un calcul, pas un livre
d'accords : il peut proposer une variante valide mais peu usuelle (un Fa `10321x` avant le barré `133211`), et il ignore les
accords sur un autre pied que la fondamentale (pas de renversements, pas de « slash chords »).

**Gammes** (`domain/scale/Scale.kt`). Une gamme est une liste d'intervalles ; les modes sont les mêmes notes que la majeure,
vues d'une autre fondamentale. La position n commence sur le n-ième degré de la gamme, joué sur la corde la plus grave
(position 1 = la fondamentale, position 2 = le degré suivant…), et couvre quatre cases (`WINDOW_FRETS`) sur toutes les cordes : sept positions pour sept notes, cinq pour une pentatonique, six pour le
blues. Le dessin se répète à la douzième case. Les noms des notes suivent le réglage « altérations » : avec « dièses et bémols
ensemble », le Mi♭ du Do dorien s'écrit « Ré♯/Mi♭ » (l'orthographe propre à chaque tonalité n'est pas faite).

## Accordeur

**Détection** (`domain/tuner/PitchDetector.kt`). L'algorithme YIN : une autocorrélation corrigée qui évite de prendre l'octave au-dessus
ou au-dessous, ce qui compte pour la corde grave d'une basse (presque toute l'énergie est dans les harmoniques 2 et 3). Le micro
rend une fenêtre de 8192 échantillons (~186 ms à 44,1 kHz) toutes les 2048 (~46 ms) ; la période est affinée par une parabole
et la précision passe sous le cent. Le silence (niveau trop bas) et les sons sans hauteur nette donnent « rien ». Une médiane
des cinq dernières mesures (`PitchSmoother`) calme l'aiguille. La plage cherchée suit l'instrument : de 0,8 fois la corde la
plus grave à 1,6 fois la plus aiguë. Les tests (`TunerTest`) utilisent des sons calculés : chaque corde à vide des quatre
instruments à moins d'un cent, une corde désaccordée de -30 à +25 cents, une basse à fondamentale faible, du bruit de fond.

**Diapason.** La = 440 Hz par défaut, réglable de 430 à 450 Hz (boutons − et + sous les cordes, enregistré avec les réglages). Le micro utilise la source Android « non traitée » quand elle existe : la suppression de bruit du téléphone prend sinon une note tenue pour du bruit et l'éteint. L'affichage garde la dernière mesure environ une demi-seconde quand le son devient trop faible.

**Limites.** Pas de mesure en continu hors des cordes à vide (c'est un accordeur, pas
un analyseur). Le micro d'un téléphone et la justesse réelle d'une guitare n'ont été vérifiés qu'avec ces sons calculés :
à confirmer sur l'instrument. Côté iOS, `AudioInput.ios.kt` n'a jamais été compilé et l'appli iOS devra déclarer
`NSMicrophoneUsageDescription`. Google Play : la permission `RECORD_AUDIO` demande une déclaration dans la fiche de
l'appli (le micro ne sert qu'à l'accordeur, rien n'est enregistré ni envoyé).

## Commandes

Java : celui d'Android Studio (`C:\Program Files\Android\Android Studio\jbr`), à mettre dans `JAVA_HOME`.

```bash
./gradlew :androidApp:assembleDebug        # APK dans androidApp/build/outputs/apk/debug
./gradlew :shared:testAndroidHostTest      # tests unitaires
```

Package : `com.sensitivemobility.neckatlas`. `local.properties` (chemin du SDK Android) n'est pas versionné.

## Écran de démarrage

Repris de StillDive : deux étapes enchaînées sur le fond du logo (`#08111C`), sans saut visible.

1. **Écran système** (API `core-splashscreen`, thème `Theme.NeckAtlas.Starting`) : le symbole seul, car Android découpe son
   icône dans un disque et rognerait le nom.
2. **Écran de l'appli** (`shared/.../ui/SplashScreen.kt`) : le même symbole, exactement à la même place, puis le nom
   « Sensitive Mobility » en fondu, environ 1,5 s (`SPLASH_DURATION_MS` dans `App.kt`), puis fondu vers l'appli. Il couvre
   aussi la lecture des préférences au démarrage.

Le logo source est `design/sensitive-mobility-logo.svg`. `python tools/svg_to_vector.py` le convertit en dessins vectoriels
(`logo_symbol.xml`, `logo_wordmark.xml`, `ic_splash_mark.xml`) et affiche les deux valeurs à reporter dans
`SplashScreen.kt` si l'on change l'échelle du symbole.

## Icône de l'appli

Icône adaptative (`minSdk` 26 : pas de version « legacy »), déclarée dans le manifeste (`android:icon`) et décrite par
`androidApp/src/main/res/mipmap-anydpi/ic_launcher.xml`. Elle vient d'une illustration unique, `design/neckatlas-icon.webp`
(1254 px, plein cadre) : un manche de guitare à six cordes en perspective, traversé d'une orbite, en un seul trait clair sur
fond uni. On n'en retient que la **forme** : les couleurs de l'icône se règlent dans `tools/make_icons.py` (`BACKGROUND` et
`INK`, ou `--background` et `--ink`), actuellement turquoise `#2EE6D0` en fond et bleu nuit `#08111C` pour le dessin, ceux du
logo Sensitive Mobility et de l'écran de démarrage (variante 3 de `VARIANTS`, choisie parmi cinq). L'image est restée telle qu'elle a été fournie (blanc sur violet `#660296`) ; la première version
(dégradé violet-bleu, orbite orange) est gardée dans `design/neckatlas-icon-v1-gradient.webp`.

Comme c'est un dessin à plat, `python -I tools/make_icons.py` (Pillow requis) le **sépare en deux** plutôt que de poser l'image
telle quelle :

- `mipmap-*/ic_launcher_foreground.webp` : le dessin seul, de la couleur de l'encre, sur fond transparent (la transparence est
  tirée de l'image : le canal vert est presque nul dans le fond et plein dans le trait), centré sur son emprise et cadré dans
  le cercle de sécurité du calque de 108 dp. Il sert aussi de calque **monochrome** : sur Android 13+, avec les icônes
  thématiques, le système ne garde que sa transparence et le colore lui-même ;
- `values/ic_launcher_background.xml` : la couleur de fond (fichier généré, ne pas le modifier à la main) ;
- `design/neckatlas-icon-512.png` : l'icône de la fiche Google Play (fond uni, sans le bruit de l'image d'origine).

Un fond uni et un calque de 2 à 11 Ko par densité remplacent les 10 images de l'ancienne version (une centaine de Ko chacune).
`--preview planche.png` écrit une planche de contrôle (rond, carré arrondi, carré, et la version thématique) pour vérifier le
cadrage ; `--variants planche.png` compare les variantes de `VARIANTS` sur fond de bureau sombre et clair, sans rien changer,
pour choisir une couleur avant de la retenir. Le calque monochrome n'a été vu que sur la planche de contrôle, pas sur un vrai
lanceur avec les icônes thématiques activées. L'écran de démarrage système garde, lui, le symbole Sensitive Mobility.

## Apparence

**Palette** (`ui/Theme.kt`) : celle du logo Sensitive Mobility et de l'écran de démarrage. Thème sombre : fond bleu nuit
(`#08111C`), turquoise de l'anneau (`#2EE6D0`) en accent, ambre pour ce qui est choisi ou joué. Thème clair : fond bleu très
pâle, turquoise foncé en accent. L'appli a été violette (pour aller avec une icône violette, `#660296`) avant de revenir à ces
couleurs, plus proches de l'écran de démarrage.

Quelques couleurs **portent un sens** et ne suivent pas la marque (`AccentColors`, `LocalAccentColors`) : le turquoise est la
tierce d'un accord et une réussite de 70 à 89 % sur la carte de chaleur, le bleu est la quinte, avec l'ambre de la
fondamentale, le violet de la septième, le vert d'une bonne réponse. Elles sont séparées du thème pour pouvoir changer la
couleur de la marque sans les confondre avec les éléments d'interface.

**Police** : les titres (en-têtes de section, nom d'un accord), le nom de la note affichée, le score d'un quiz et le bouton
« Commencer » sont en **Sora** (Sora Project Authors, licence SIL Open Font License 1.1, aucune redevance, mais la licence doit
accompagner la police, ce que fait `files/licenses/Sora-OFL.txt`, embarqué dans l'appli et vérifié par `FontAssetsTest`) ;
le texte courant et les petites étiquettes restent dans la police du système, plus étroite et plus lisible en petit
(`neckAtlasTypography` dans `ui/Theme.kt`). C'est une police à **graisse variable** (`font/sora.ttf`, 109 Ko, axe `wght` de 100
à 800) : un seul fichier donne toutes les graisses, car `Font(resource, weight)` règle l'axe d'après la graisse demandée. Pour
changer de police : remplacer `font/sora.ttf` (et la licence), et garder le nom `sora` ou le changer dans `Theme.kt`. Source :
`github.com/google/fonts`, dossier `ofl/sora`.

**Le manche** (`feature/fretboard/FretboardView.kt`, partagé par les onglets Manche, Accords et Quiz et par la carte de
chaleur) est toujours un manche en bois, en thème clair comme en thème sombre : palissandre sombre, avec
son grain (tiré avec une graine fixe, donc identique à chaque lancement) et des bords assombris pour le bomber ; frettes
d'argent avec ombre et reflet ; sillet d'os ; incrustations nacrées ; cordes dont l'épaisseur suit la hauteur de la corde à vide
(Mi aigu fin, basse épaisse, quel que soit l'instrument), **filées** (bronze, rayées) sous Mi3 et en acier au-dessus. Tout se
dessine au `Canvas`, sans image.

**Les écrans** :

- **Quiz** : les six exercices sont des cartes de deux par rangée (icône, nom, et le score de la dernière série jouée dans cet
  exercice, lu dans l'historique) ; la carte choisie est cerclée de la couleur d'accent. Le bouton « Commencer » et l'indication qui le
  précède sont **hors du défilement**, en bas : toujours sous le pouce. Le bilan montre un anneau qui se remplit de la part de
  réussite, dans la couleur de sa tranche, avec le score au centre.
- **Progrès** : l'anneau de réussite globale remplace la ligne de texte du haut. Sans aucune réponse, l'écran invite au lieu
  d'être vide : une petite illustration de manche (`NeckIllustration`, même bois et mêmes cordes que le vrai manche, avec l'orbite
  de l'icône) et un bouton « Lancer un quiz » qui ouvre l'onglet Quiz.
- **Accords** : quatre rangées de pastilles prenaient un quart de l'écran. Une **barre d'accord** (fondamentale et famille, avec
  des flèches pour passer à la suivante, en bouclant : après Si vient Do) et une seule rangée d'options (forme ou arpège, puis
  renversements, groupes de cordes et positions) les remplacent. Toucher un nom ouvre une feuille avec les douze fondamentales,
  les neuf familles et le réglage des noms de notes, pour aller directement à un accord éloigné.

Les couleurs de réussite sont dans `ui/Accuracy.kt` (`accuracyColor`, avec ses seuils testés) et l'anneau (`ScoreRing`) aussi.

**Au toucher** (désactivé sur la carte de chaleur, `touchFeedback = false`) : la corde vibre en onde stationnaire qui s'éteint
en 1,5 s (plus lentement pour les graves), une onde part de la case, la pastille de la note « claque » en place avec un petit
ressort, et le téléphone vibre (`HapticFeedbackType.VirtualKey`) si Réglages > Retour vibré est actif. `TextHandleMove`, essayé
d'abord, ne vibre pas du tout sur l'émulateur Android 15 ; `VirtualKey` est le clic que tous les téléphones associent à une
vibration. La vibration dépend aussi du réglage système « retour vibré au toucher » : s'il est coupé dans Android, rien ne
vibre, quel que soit le réglage de l'appli.

## Langue

Réglages > Langue : Système (la langue du téléphone, par défaut), Français ou English, enregistrée avec les autres
préférences (`AppLanguage` dans `AppSettings`). Une valeur inconnue ou absente d'un ancien fichier donne « Système ». Le nom de
chaque langue est écrit dans cette langue, pour qu'on la reconnaisse quelle que soit celle de l'interface.

Tous les textes passent par les ressources Compose (`stringResource`), choisies d'après la langue de la plateforme. Il n'existe
pas d'API commune pour la remplacer (méthode de la documentation, « Manage local resource environment ») : `AppLanguageScope`
(`ui/`) appelle `ProvideAppLanguage`, écrit pour chaque plateforme, puis **reconstruit ce qu'il contient** quand la langue change,
car la langue n'est pas un état que Compose observe. Ce qui doit survivre à ce changement (l'onglet ouvert, une série de quiz
en cours) est donc gardé **au-dessus** de l'appel, dans `App.kt`.

- **Android** (`AppLanguageScope.android.kt`) : remplace `Locale.getDefault()` (d'où Compose tire ses textes) et la
  configuration des ressources de l'activité (pour les composants Material). « Système » relit la langue du téléphone à la
  source (`Resources.getSystem()`), puisque la langue par défaut est justement ce qu'on modifie.
- **iOS** (`AppLanguageScope.ios.kt`) : écrit `AppleLanguages` dans les préférences de l'appli, ou le supprime pour « Système ».
  **Ni compilé ni essayé** (pas de Mac) : à vérifier, notamment que le changement se voit tout de suite.

Ajouter une langue : un dossier `values-xx/strings.xml`, une entrée dans `AppLanguage` et son code dans `AppLanguage.tag`.
`StringsParityTest` impose que chaque texte existe en français et en anglais avec les mêmes paramètres (`%1$d`…).

Vérifié sur émulateur (en-US) : passage en français immédiat, barre du bas comprise, sans quitter l'onglet des réglages ; le choix
survit à la fermeture de l'appli ; « Système » rend l'anglais ; une série de quiz en cours se poursuit, même question.

## Sons

Les notes sont jouées à partir de vraies prises de son, rangées dans les ressources
(`shared/src/commonMain/composeResources/files/samples/`, lues par `Res.readBytes` : même code sur Android et iOS) : 66
fichiers `.pcm` (16 bits, mono, 22 050 Hz) et `manifest.txt`, l'inventaire que l'appli lit pour savoir quelles notes existent.
Environ 8,8 Mo bruts, 7 Mo une fois compressés dans l'APK. Rien n'est lu avant la première note ; chaque fichier est ensuite
gardé en mémoire.

Pour une note demandée, `SampleBank` prend l'échantillon enregistré le plus proche et `SampleShifter` l'accorde en le lisant
plus vite ou plus lentement, puis la raccourcit à 1,5 s (`NOTE_SECONDS`) en lui ajoutant une décroissance de 30 dB
(`SampleShifter.DECAY_DB`) et un fondu de fin de 0,35 s. Cette décroissance est nécessaire : les prises tiennent presque
indéfiniment (à 2,5 s, la basse Mi1 n'a perdu que 6 dB et la guitare Mi2 16 dB) et sonnaient comme des notes tenues, pas comme
des cordes. Les deux constantes se règlent à l'oreille, sans régénérer les fichiers. La guitare a un échantillon tous les 3
demi-tons (Ré♭2 à
Ré6), donc un décalage de 1 ou 2 demi-tons au plus, et trois variantes de chaque, jouées à tour de rôle. La basse n'a qu'**une
octave** enregistrée (Mi1 à Ré♯2, une prise par note) : plus une note en est éloignée (aiguës de la basse, case 12 et au-delà),
plus l'échantillon est accéléré, et plus elle sonne courte et sèche. C'est la limite principale de ce son.

| Instrument | Source | Auteur | Licence |
|---|---|---|---|
| Guitare | « Emilyguitar » (Epiphone SG, cordes filets plats, couche `mf`) | Karoryfer Samples (D. Smolken) | CC0 |
| Basse | « Finger Bass YR » (Yamaha RBX aux doigts) | Andrea Biasior, via FreePats | CC0 |

CC0 : aucune attribution n'est exigée ; elles sont citées ici par courtoisie. Les banques d'origine ne sont pas dans le
dépôt : pour régénérer les fichiers, les télécharger ([Emilyguitar](https://github.com/sfzinstruments/karoryfer.emilyguitar),
[Finger Bass YR](https://freepats.zenvoid.org/ElectricGuitar/clean-electric-bass.html)) puis lancer
`python -I tools/make_samples.py <dossier notes d'Emilyguitar> <dossier samples/finger de la basse>` (bibliothèque standard
seulement). Le script retire la composante continue, **mesure la hauteur réelle de chaque échantillon et la ramène à la note
exacte** (les prises brutes étaient fausses de 0 à 21 cents, jusqu'à un cinquième de demi-ton), convertit en 16 bits à
22,05 kHz, normalise (crête à 0,9) et écrit l'inventaire. Passer à 44,1 kHz (`OUT_RATE`) double le poids pour un gain
inaudible sur un haut-parleur de téléphone ; la prise de son est sombre.

Si l'inventaire ou un fichier est introuvable ou illisible, `SampleBank.voice` se rabat sur `PluckSynth` (corde pincée
synthétisée) et écrit un message dans la console (`System.out` sur Android) plutôt que de rester muette.

Tests : `SampleManifestTest`, `SampleShifterTest`, `SampleBankTest` (sélection, variantes, cache, repli) dans `commonTest` ;
`SampleAssetsTest` (côté Android, il lit les vrais fichiers) joue **toutes les notes de tous les instruments**, de la corde la
plus grave à vide à la case 24, toutes variantes, et vérifie qu'elles sont audibles, non saturées et justes (écart mesuré :
6,0 cents au pire, 1,5 en médiane ; seuil du test 8 cents). Vérifié sur émulateur : lecture des échantillons sans repli, et
repli sur le synthétiseur quand l'inventaire est corrompu. Le code iOS n'est pas compilé (voir plus haut).
