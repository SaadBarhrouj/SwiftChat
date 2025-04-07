@echo off
chcp 65001>nul
setlocal enabledelayedexpansion

:: Configuration
set "PROJECT_ROOT=%cd%"
set "SOURCE_DIR=src"
set "TARGET_DIRS=client Dao Entities Server utils"
set "IGNORE_DIRS=\\.git\\ \\.idea\\ \\out\\ \\DB\\"

echo [INFO] Copie des fichiers Java dans le presse-papiers...
echo [INFO] Racine du projet : %PROJECT_ROOT%

:: Création d'un fichier temporaire
set "TEMP_FILE=%temp%\swiftchat_java_%random%.txt"
echo ===== SWIFTCHAT - FICHIERS JAVA ===== > "%TEMP_FILE%"
echo. >> "%TEMP_FILE%"

:: Parcours des dossiers sources dans src
for %%d in (%TARGET_DIRS%) do (
    if exist "%SOURCE_DIR%\%%d" (
        echo ----- DOSSIER: %%d ----- >> "%TEMP_FILE%"
        echo. >> "%TEMP_FILE%"

        :: Recherche des fichiers .java
        dir /s /b /a-d "%SOURCE_DIR%\%%d\*.java" 2>nul | findstr /v /i "%IGNORE_DIRS%" > "%temp%\filelist.tmp"

        for /f "usebackq delims=" %%f in ("%temp%\filelist.tmp") do (
            echo ===== FICHIER: %%~nxf ===== >> "%TEMP_FILE%"
            echo [Chemin: %%f] >> "%TEMP_FILE%"
            echo. >> "%TEMP_FILE%"
            type "%%f" >> "%TEMP_FILE%"
            echo. >> "%TEMP_FILE%"
        )
        del "%temp%\filelist.tmp" 2>nul
    )
)

:: Copie dans le presse-papiers
powershell -command "Get-Content '%TEMP_FILE%' | clip"

:: Nettoyage et confirmation
del "%TEMP_FILE%" 2>nul
echo [SUCCES] Contenu Java copié dans le presse-papiers !
echo Vous pouvez maintenant coller (Ctrl+V) le contenu où vous voulez.
endlocal