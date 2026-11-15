# iosApp

Sources Swift de la version iPhone. **Rien ici n'a été compilé** : le développement se fait sous Windows, sans Mac. Le code
Kotlin partagé (`shared/src/iosMain`) est dans le même cas ; il est écrit d'après la documentation de Kotlin Multiplatform.

## À faire sur un Mac

1. Xcode > New Project > iOS > App (interface SwiftUI, langage Swift), nommé `iosApp`, dans ce dossier. Remplacer les
   fichiers Swift générés par `iosApp/iOSApp.swift` et `iosApp/ContentView.swift`.
2. Build Phases > nouvelle phase « Run Script », placée **avant** « Compile Sources » :
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
3. Build Settings : `Framework Search Paths` = `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`,
   `Other Linker Flags` = `$(inherited) -framework Shared`, `User Script Sandboxing` = No.
4. Écran de démarrage système : fond `#08111C` et le symbole du logo (`design/sensitive-mobility-logo.svg`), comme sur
   Android ; l'écran Compose (`SplashScreen.kt`) prend ensuite le relais avec le nom en fondu.
5. Compiler, corriger ce qui ne passe pas (probablement quelques ajustements dans `iosMain`), puis tester sur un iPhone.
