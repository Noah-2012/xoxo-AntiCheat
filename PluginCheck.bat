@echo off
setlocal EnableDelayedExpansion

:: Enable ANSI colours (Windows 10+)
reg add HKCU\Console /v VirtualTerminalLevel /t REG_DWORD /d 1 /f >nul 2>&1

for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"

set "GREY=%ESC%[90m"
set "GREEN=%ESC%[92m"
set "ORANGE=%ESC%[38;5;214m"
set "RED=%ESC%[91m"
set "CYAN=%ESC%[96m"
set "WHITE=%ESC%[97m"
set "BOLD=%ESC%[1m"
set "DIM=%ESC%[2m"
set "RESET=%ESC%[0m"

:: ---- Argument Defaults ------------------------------------------
set "NO_PLAYER_CHECK=0"
set "JAR_VERSION=1.0.2"
set "STARTUP_COMMAND="
set "AUTO_BUILD=0"

:: ---- Argument Parser Loop ---------------------------------------
:PARSE_ARGS
if "%~1"=="" goto :END_PARSE

if /I "%~1"=="--no-player-check" (
    set "NO_PLAYER_CHECK=1"
    shift
    goto :PARSE_ARGS
)
if /I "%~1"=="--jar-version" (
    set "JAR_VERSION=%~2"
    shift & shift
    goto :PARSE_ARGS
)
if /I "%~1"=="-jv" (
    set "JAR_VERSION=%~2"
    shift & shift
    goto :PARSE_ARGS
)
if /I "%~1"=="--startup-command" (
    set "STARTUP_COMMAND=%~2"
    shift & shift
    goto :PARSE_ARGS
)
if /I "%~1"=="-sc" (
    set "STARTUP_COMMAND=%~2"
    shift & shift
    goto :PARSE_ARGS
)
if /I "%~1"=="--auto-build" (
    set "AUTO_BUILD=1"
    shift
    goto :PARSE_ARGS
)

:: Skip unknown parameters
shift
goto :PARSE_ARGS
:END_PARSE

goto :MAIN

:: ------------------------------------------------------------------
:LOG_INFO
set "_MSG=%~1"
echo %GREY%[%GREEN%+%GREY%]%RESET% %WHITE%!_MSG!%RESET%
exit /b

:LOG_WARN
set "_MSG=%~1"
echo %GREY%[%ORANGE%-%GREY%]%RESET% %ORANGE%!_MSG!%RESET%
exit /b

:LOG_ERROR
set "_MSG=%~1"
echo %GREY%[%RED%x%GREY%]%RESET% %RED%!_MSG!%RESET%
exit /b

:SEP
echo %DIM%%GREY%  --------------------------------------------------------%RESET%
exit /b

:HEADER
echo.
echo %BOLD%%CYAN%  +------------------------------------------------------+%RESET%
echo %BOLD%%CYAN%  ^|     xoxo-AntiCheat  -  Deploy ^& Debug Tool           ^|%RESET%
echo %BOLD%%CYAN%  +------------------------------------------------------+%RESET%
echo.
exit /b
:: ------------------------------------------------------------------

:MAIN
call :HEADER

:: ---- Dynamic Path Definitions based on Arguments ----------------

set "BUILD_1211=C:\Users\noahn\Documents\xoxo-AntiCheat\build\libs\xoxo-AntiCheat-%JAR_VERSION%-1_21.jar"
set "BUILD_1206=C:\Users\noahn\Documents\xoxo-AntiCheat\build\libs\xoxo-AntiCheat-%JAR_VERSION%-1_20_6.jar"

set "SERVERDIR_1211=C:\Users\noahn\AppData\Roaming\ATLauncher\servers\PaperTestServer12110"
set "SERVERDIR_1206=C:\Users\noahn\AppData\Roaming\ATLauncher\servers\PaperTestServer1206"

set "PLUGIN_1211=%SERVERDIR_1211%\plugins\xoxo-AntiCheat-%JAR_VERSION%-1_21.jar"
set "REMAP_1211=%SERVERDIR_1211%\plugins\.paper-remapped\xoxo-AntiCheat-%JAR_VERSION%-1_21.jar"

set "PLUGIN_1206=%SERVERDIR_1206%\plugins\xoxo-AntiCheat-%JAR_VERSION%-1_20_6.jar"
set "REMAP_1206=%SERVERDIR_1206%\plugins\.paper-remapped\xoxo-AntiCheat-%JAR_VERSION%-1_20_6.jar"

set "PLUGINDIR_1211=%SERVERDIR_1211%\plugins"
set "PLUGINDIR_1206=%SERVERDIR_1206%\plugins"

set "LAUNCH_1211=%SERVERDIR_1211%\LaunchServer.bat"
set "LAUNCH_1206=%SERVERDIR_1206%\LaunchServer.bat"

set "LOG_1211=%SERVERDIR_1211%\logs\latest.log"
set "LOG_1206=%SERVERDIR_1206%\logs\latest.log"

:: =================================================================
:: STEP 0 - Auto Build (Conditional)
:: =================================================================
if "!AUTO_BUILD!"=="1" (
    echo %BOLD%%WHITE%  [STEP 0] Automated Project Build%RESET%
    call :SEP
    call :LOG_INFO "Cleaning old localized build artifacts..."
    if exist "%BUILD_1211%" del /f /q "%BUILD_1211%" >nul 2>&1
    if exist "%BUILD_1206%" del /f /q "%BUILD_1206%" >nul 2>&1
    
    call :LOG_INFO "Executing: gradlew.bat jar1_20_6 jar1_21"
    echo.
    call .\gradlew.bat jar1_20_6 jar1_21
    if errorlevel 1 (
        echo.
        call :LOG_ERROR "Gradle compilation failed! Aborting process."
        goto :FAIL
    )
    echo.
    call :LOG_INFO "Compilation successful!"
    echo.
)

:: =================================================================
:: STEP 1 - Clean old JARs
:: =================================================================
echo %BOLD%%WHITE%  [STEP 1] Cleaning old plugin JARs%RESET%
call :SEP

call :DELETE_JAR "%PLUGIN_1211%" "PaperTestServer12110\plugins\xoxo-AntiCheat-%JAR_VERSION%-1_21.jar"
call :DELETE_JAR "%REMAP_1211%"  "PaperTestServer12110\plugins\.paper-remapped\xoxo-AntiCheat-%JAR_VERSION%-1_21.jar"
call :DELETE_JAR "%PLUGIN_1206%" "PaperTestServer1206\plugins\xoxo-AntiCheat-%JAR_VERSION%-1_20_6.jar"
call :DELETE_JAR "%REMAP_1206%"  "PaperTestServer1206\plugins\.paper-remapped\xoxo-AntiCheat-%JAR_VERSION%-1_20_6.jar"

echo.

:: =================================================================
:: STEP 2 - Verify build artifacts exist
:: =================================================================
echo %BOLD%%WHITE%  [STEP 2] Verifying build artifacts%RESET%
call :SEP

set "BUILD_OK=1"

if exist "%BUILD_1211%" (
    call :LOG_INFO "Build artifact found: xoxo-AntiCheat-%JAR_VERSION%-1_21.jar"
) else (
    call :LOG_ERROR "Build artifact NOT found: %BUILD_1211%"
    call :LOG_ERROR "Run ./gradlew build first, or append parameter --auto-build"
    set "BUILD_OK=0"
)

if exist "%BUILD_1206%" (
    call :LOG_INFO "Build artifact found: xoxo-AntiCheat-%JAR_VERSION%-1_20_6.jar"
) else (
    call :LOG_ERROR "Build artifact NOT found: %BUILD_1206%"
    call :LOG_ERROR "Run ./gradlew build first, or append parameter --auto-build"
    set "BUILD_OK=0"
)

if "!BUILD_OK!"=="0" (
    echo.
    call :LOG_ERROR "Cannot continue - one or more build artifacts are missing."
    goto :FAIL
)
echo.

:: =================================================================
:: STEP 3 - Deploy JARs
:: =================================================================
echo %BOLD%%WHITE%  [STEP 3] Deploying JARs to plugin folders%RESET%
call :SEP

call :DEPLOY_JAR "%BUILD_1211%" "%PLUGINDIR_1211%" "xoxo-AntiCheat-%JAR_VERSION%-1_21.jar    -> PaperTestServer12110"
if errorlevel 1 goto :FAIL

call :DEPLOY_JAR "%BUILD_1206%" "%PLUGINDIR_1206%" "xoxo-AntiCheat-%JAR_VERSION%-1_20_6.jar  -> PaperTestServer1206"
if errorlevel 1 goto :FAIL

echo.

:: =================================================================
:: STEP 4 - Launch servers
:: =================================================================
echo %BOLD%%WHITE%  [STEP 4] Launching servers%RESET%
call :SEP

if exist "%LOG_1211%" del /f /q "%LOG_1211%" >nul 2>&1
if exist "%LOG_1206%" del /f /q "%LOG_1206%" >nul 2>&1

:: Double check enforcement truncation to prevent ghost flags from old files
if exist "%LOG_1211%" type nul > "%LOG_1211%" >nul 2>&1
if exist "%LOG_1206%" type nul > "%LOG_1206%" >nul 2>&1

if not exist "%LAUNCH_1211%" (
    call :LOG_ERROR "LaunchServer.bat not found: %LAUNCH_1211%"
    goto :FAIL
)
if not exist "%LAUNCH_1206%" (
    call :LOG_ERROR "LaunchServer.bat not found: %LAUNCH_1206%"
    goto :FAIL
)

:: If a startup command is present, don't launch minimized so window can accept text input focus
set "LAUNCH_MODE=/MIN"
if not "%STARTUP_COMMAND%"=="" set "LAUNCH_MODE="

start "XOXO-Server-1211" %LAUNCH_MODE% /D "%SERVERDIR_1211%" cmd /c "%LAUNCH_1211%"
call :LOG_INFO "Started PaperTestServer12110  (1.21.10)"

start "XOXO-Server-1206" %LAUNCH_MODE% /D "%SERVERDIR_1206%" cmd /c "%LAUNCH_1206%"
call :LOG_INFO "Started PaperTestServer1206   (1.20.6)"

echo.

:: =================================================================
:: STEP 5 - Wait for both servers to finish startup
:: =================================================================
echo %BOLD%%WHITE%  [STEP 5] Waiting for servers to finish startup%RESET%
call :SEP
call :LOG_INFO "Monitoring logs - this may take up to 120 seconds..."
echo.

set "READY_1211=0"
set "READY_1206=0"
set "FAIL_1211=0"
set "FAIL_1206=0"
set "TICK=0"

:WAIT_LOOP
    timeout /t 1 /nobreak >nul
    set /a TICK+=1

    :: ---- Monitor 1.21.10 Startup ----
    if "!READY_1211!"=="0" if "!FAIL_1211!"=="0" if exist "%LOG_1211%" (
        findstr /C:"Done (" "%LOG_1211%" >nul 2>&1
        if not errorlevel 1 (
            set "READY_1211=1"
            call :LOG_INFO "PaperTestServer12110 (1.21.10) started successfully!"
        )
        findstr /I /C:"FAILED TO BIND" /C:"Unexpected exception" "%LOG_1211%" >nul 2>&1
        if not errorlevel 1 (
            set "FAIL_1211=1"
            call :LOG_ERROR "PaperTestServer12110 (1.21.10) failed to start correctly! (Port crash)"
        )
        findstr /I /C:"Could not load" /C:"FAILED" "%LOG_1211%" | findstr /I "xoxo" >nul 2>&1
        if not errorlevel 1 (
            set "FAIL_1211=1"
            call :LOG_ERROR "xoxo-AntiCheat FAILED to load on PaperTestServer12110!"
            call :PRINT_STACKTRACE "%LOG_1211%" "PaperTestServer12110"
        )
    )

    :: ---- Monitor 1.20.6 Startup ----
    if "!READY_1206!"=="0" if "!FAIL_1206!"=="0" if exist "%LOG_1206%" (
        findstr /C:"Done (" "%LOG_1206%" >nul 2>&1
        if not errorlevel 1 (
            set "READY_1206=1"
            call :LOG_INFO "PaperTestServer1206  (1.20.6) started successfully!"
        )
        findstr /I /C:"FAILED TO BIND" /C:"Unexpected exception" "%LOG_1206%" >nul 2>&1
        if not errorlevel 1 (
            set "FAIL_1206=1"
            call :LOG_ERROR "PaperTestServer1206 (1.20.6) failed to start correctly! (Port crash)"
        )
        findstr /I /C:"Could not load" /C:"FAILED" "%LOG_1206%" | findstr /I "xoxo" >nul 2>&1
        if not errorlevel 1 (
            set "FAIL_1206=1"
            call :LOG_ERROR "xoxo-AntiCheat FAILED to load on PaperTestServer1206!"
            call :PRINT_STACKTRACE "%LOG_1206%" "PaperTestServer1206"
        )
    )

    set "HANDLED_COUNT=0"
    if "!READY_1211!"=="1" set /a HANDLED_COUNT+=1
    if "!FAIL_1211!"=="1" set /a HANDLED_COUNT+=1
    if "!READY_1206!"=="1" set /a HANDLED_COUNT+=1
    if "!FAIL_1206!"=="1" set /a HANDLED_COUNT+=1

    if "!HANDLED_COUNT!"=="2" (
        if "!FAIL_1211!"=="1" goto :SERVER_BOOT_FAIL
        if "!FAIL_1206!"=="1" goto :SERVER_BOOT_FAIL
        goto :BOTH_READY
    )

    if !TICK! geq 120 (
        call :LOG_ERROR "Timed out waiting for servers to start after 120 seconds."
        goto :SERVER_BOOT_FAIL
    )

goto :WAIT_LOOP

:BOTH_READY
echo.
call :LOG_INFO "Both servers running - plugin loaded successfully on both."
echo.

:: ---- Handle Optional Startup Commands ----
if not "%STARTUP_COMMAND%"=="" (
    call :LOG_INFO "Injecting console command: %STARTUP_COMMAND%"
    call :SEND_CONSOLE_CMD "XOXO-Server-1211" "%STARTUP_COMMAND%"
    call :SEND_CONSOLE_CMD "XOXO-Server-1206" "%STARTUP_COMMAND%"
    echo.
)

:: ---- Handle Optional Runtime Skip (--no-player-check) ----
if "!NO_PLAYER_CHECK!"=="1" (
    call :LOG_WARN "--no-player-check enabled. Skipping session loops."
    call :LOG_INFO "Terminating server background tasks gracefully..."
    call :SEND_CONSOLE_CMD "XOXO-Server-1211" "stop"
    call :SEND_CONSOLE_CMD "XOXO-Server-1206" "stop"
    goto :SUCCESS
)

:: =================================================================
:: STEP 6 - Monitor player sessions
:: =================================================================
echo %BOLD%%WHITE%  [STEP 6] Monitoring player sessions%RESET%
call :SEP
call :LOG_INFO "Waiting for a player to join each server..."
echo.

set "JOINED_1211=0"
set "JOINED_1206=0"
set "LEFT_1211=0"
set "LEFT_1206=0"
set "STICK=0"

:SESSION_LOOP
    timeout /t 1 /nobreak >nul
    set /a STICK+=1

    :: ---- 1.21.10 Independent Validation Loop ----
    if "!JOINED_1211!"=="0" if exist "%LOG_1211%" (
        findstr /I /C:"joined the game" "%LOG_1211%" >nul 2>&1
        if not errorlevel 1 (
            set "JOINED_1211=1"
            call :LOG_INFO "[1.21.10] A player has joined the server."
            call :LOG_INFO "Watching for runtime errors on PaperTestServer12110..."
        )
    )

    if "!JOINED_1211!"=="1" if "!LEFT_1211!"=="0" if exist "%LOG_1211%" (
        findstr /I /C:"left the game" "%LOG_1211%" >nul 2>&1
        if not errorlevel 1 (
            findstr /I /C:"Exception" /C:"Caused by:" /C:"NullPointer" /C:"ArrayIndex" /C:"StackOverflow" "%LOG_1211%" >nul 2>&1
            if not errorlevel 1 (
                call :LOG_ERROR "Runtime errors detected on PaperTestServer12110 during session!"
                call :PRINT_STACKTRACE "%LOG_1211%" "PaperTestServer12110"
            ) else (
                call :LOG_INFO "No runtime errors detected on PaperTestServer12110."
            )
            set "LEFT_1211=1"
            call :LOG_INFO "Player left PaperTestServer12110 - safely stopping server."
            call :SEND_CONSOLE_CMD "XOXO-Server-1211" "stop"
        )
    )

    :: ---- 1.20.6 Independent Validation Loop ----
    if "!JOINED_1206!"=="0" if exist "%LOG_1206%" (
        findstr /I /C:"joined the game" "%LOG_1206%" >nul 2>&1
        if not errorlevel 1 (
            set "JOINED_1206=1"
            call :LOG_INFO "[1.20.6] A player has joined the server."
            call :LOG_INFO "Watching for runtime errors on PaperTestServer1206..."
        )
    )

    if "!JOINED_1206!"=="1" if "!LEFT_1206!"=="0" if exist "%LOG_1206%" (
        findstr /I /C:"left the game" "%LOG_1206%" >nul 2>&1
        if not errorlevel 1 (
            findstr /I /C:"Exception" /C:"Caused by:" /C:"NullPointer" /C:"ArrayIndex" /C:"StackOverflow" "%LOG_1206%" >nul 2>&1
            if not errorlevel 1 (
                call :LOG_ERROR "Runtime errors detected on PaperTestServer1206 during session!"
                call :PRINT_STACKTRACE "%LOG_1206%" "PaperTestServer1206"
            ) else (
                call :LOG_INFO "No runtime errors detected on PaperTestServer1206."
            )
            set "LEFT_1206=1"
            call :LOG_INFO "Player left PaperTestServer1206 - safely stopping server."
            call :SEND_CONSOLE_CMD "XOXO-Server-1206" "stop"
        )
    )

    :: Script will only proceed to exit once BOTH servers have independently finished their loops
    if "!LEFT_1211!"=="1" if "!LEFT_1206!"=="1" goto :SUCCESS

    if !STICK! geq 600 (
        call :LOG_WARN "Session monitor timed out after 600 seconds. Terminating remaining tasks."
        call :SEND_CONSOLE_CMD "XOXO-Server-1211" "stop"
        call :SEND_CONSOLE_CMD "XOXO-Server-1206" "stop"
        goto :FAIL
    )

goto :SESSION_LOOP

:: =================================================================
:SUCCESS
:: =================================================================
echo.
echo %BOLD%%CYAN%  +------------------------------------------------------+%RESET%
echo %BOLD%%GREEN%  ^|   OK  -  All checks passed. xoxo-AntiCheat is good!  ^|%RESET%
echo %BOLD%%CYAN%  +------------------------------------------------------+%RESET%
echo.
call :LOG_INFO "Both servers processed successfully."
call :LOG_INFO "Plugin versions verified: %JAR_VERSION%"
call :LOG_INFO "No runtime exceptions caught."
echo.
call :LOG_INFO "Closing in 5 seconds..."
timeout /t 5 /nobreak >nul
exit /b 0

:: =================================================================
:SERVER_BOOT_FAIL
:: =================================================================
echo.
call :LOG_ERROR "One or both servers encountered a critical boot error. Shutting down."
call :SEND_CONSOLE_CMD "XOXO-Server-1211" "stop"
call :SEND_CONSOLE_CMD "XOXO-Server-1206" "stop"
goto :FAIL

:: =================================================================
:FAIL
:: =================================================================
echo.
echo %BOLD%%RED%  +------------------------------------------------------+%RESET%
echo %BOLD%%RED%  ^|   FAILED  -  Deployment encountered errors.            ^|%RESET%
echo %BOLD%%RED%  +------------------------------------------------------+%RESET%
echo.
call :LOG_ERROR "Press any key to close."
pause >nul
exit /b 1

:: =================================================================
:: Subroutines
:: =================================================================

:DELETE_JAR
set "_DP=%~1"
set "_DN=%~2"
if exist "!_DP!" (
    del /f /q "!_DP!" >nul 2>&1
    if errorlevel 1 (
        call :LOG_ERROR "Found but COULD NOT delete: !_DN!"
    ) else (
        call :LOG_INFO  "Deleted: !_DN!"
    )
) else (
    call :LOG_WARN "Not found (skipping): !_DN!"
)
exit /b

:DEPLOY_JAR
set "_DS=%~1"
set "_DD=%~2"
set "_DL=%~3"
if not exist "!_DD!" (
    call :LOG_ERROR "Plugin directory not found: !_DD!"
    exit /b 1
)
copy /y "!_DS!" "!_DD!\" >nul 2>&1
if errorlevel 1 (
    call :LOG_ERROR "Failed to copy: !_DL!"
    exit /b 1
) else (
    call :LOG_INFO  "Deployed: !_DL!"
)
exit /b 0

:SEND_CONSOLE_CMD
set "_TARGET_WINDOW=%~1"
set "_LOCAL_CMD=%~2"
:: Packages the text string entirely localized inside the subroutine context to preserve characters
setlocal
set "_CMD_STRING=%_LOCAL_CMD%"
powershell -NoProfile -Command "$wshell = New-Object -ComObject Wscript.Shell; if ($wshell.AppActivate('%_TARGET_WINDOW%')) { Start-Sleep -m 500; $cmd = $env:_CMD_STRING; $escaped = ''; foreach ($c in $cmd.ToCharArray()) { if ('+%%^~()[]{}' -contains $c) { $escaped += \"{$c}\" } else { $escaped += $c } }; $wshell.SendKeys($escaped + '{ENTER}'); }"
endlocal
exit /b

:PRINT_STACKTRACE
set "_PL=%~1"
set "_PS=%~2"
echo.
echo %GREY%[%RED%x%GREY%]%RESET% %RED%-- Stacktrace from !_PS! --%RESET%
if exist "!_PL!" (
    for /f "tokens=*" %%S in ('findstr /I /C:"ERROR" /C:"Exception" /C:"Caused by:" /C:"at com." /C:"at org." /C:"at java." /C:"at net.minecraft" /C:"xoxo" "!_PL!"') do (
        set "_LINE=%%S"
        echo %GREY%[%RED%x%GREY%]%RESET%  %RED%!_LINE!%RESET%
    )
) else (
    call :LOG_ERROR "Log file not accessible: !_PL!"
)
echo %GREY%[%RED%x%GREY%]%RESET% %RED%-----------------------------%RESET%
echo.
exit /b