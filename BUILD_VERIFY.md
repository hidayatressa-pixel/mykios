# MYKIOS V3 build verification

From the project root in PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean :app:assembleFreeRelease :app:assembleProRelease
```

Expected result: `BUILD SUCCESSFUL` and both `freeRelease` and `proRelease` APK outputs.

## V5 KSP / Room schema fix
- Removed the zero-byte `app/schemas/.gitkeep` file. Room/KSP treats files in the configured schema directory as schema inputs; the empty placeholder caused JSON decoding to hit EOF.
- Kept `room.schemaLocation` enabled so successful builds can export real Room schema JSON.
- Kept database hardening and FREE/PRO flavors unchanged.

Verify with:
`./gradlew.bat clean :app:assembleFreeRelease :app:assembleProRelease`
