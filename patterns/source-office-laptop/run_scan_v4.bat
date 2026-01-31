 @echo off
setlocal EnableExtensions EnableDelayedExpansion
REM ============================================
REM Run JDK 21 Scanner (Windows) - v4 (FINAL - CMD SAFE)
REM
REM Usage:
REM   run_scan_v4.bat <csvPath> <javaSrcDir> <jspSrcDir> <ApplicationName> [flags...]
REM
REM RECOMMENDED flag syntax (CMD safe):
REM   --exts ".java,.jsp,.xml,.properties,.yml,.yaml" --debug
REM Optional:
REM   --normalizeRegex
REM   --libDir="D:\Users\...\combined_lib"
REM ============================================

pushd "%~dp0" >nul

set "JAVA21_HOME=D:\Users\kosambia\OneDrive - The Great Eastern Life Assurance Company Limited\Software\jdk-21.0.9"
if not exist "%JAVA21_HOME%\bin\java.exe" echo [ERROR] JDK 21 not found at "%JAVA21_HOME%" & goto fail
set "JAVA_HOME=%JAVA21_HOME%"
set "PATH=%JAVA21_HOME%\bin;%PATH%"

echo [INFO] JAVA_HOME = %JAVA_HOME%
java -version
if errorlevel 1 echo [ERROR] Java not usable after switch. & goto fail

echo [DBG] Raw arguments: %*

set "datestamp=%date:~10,4%%date:~4,2%%date:~7,2%"
set "timestamp=%time:~0,2%%time:~3,2%%time:~6,2%"
set "timestamp=%timestamp: =0%"
set "STAMP=%datestamp%_%timestamp%"

set "CSV=%~1"
if "%CSV%"=="" set "CSV=%CD%\analysis_core_v4.csv"
set "SRC=%~2"
if "%SRC%"=="" set "SRC=D:\Users\kosambia\jdk21\fpms-src\src"
set "JSP=%~3"
set "APP=%~4"
if "%APP%"=="" set "APP=UnknownApp"

if not exist "%CSV%" echo [ERROR] CSV not found: "%CSV%" & goto fail
if not exist "%SRC%" echo [ERROR] Java source dir not found: "%SRC%" & goto fail

set "HAS_JSP=0"
if not "%JSP%"=="" (
  pushd "%JSP%" >nul 2>&1
  if not errorlevel 1 set "HAS_JSP=1"
  popd >nul 2>&1
)

echo [INFO] CSV     = "%CSV%"
echo [INFO] JavaSrc = "%SRC%"
echo [INFO] JspSrc  = "%JSP%" (enabled=%HAS_JSP%)
echo [INFO] AppName = "%APP%"

REM Capture up to 5 tokens for flags
set "FLAGS=%~5 %~6 %~7 %~8 %~9"
for /f "tokens=*" %%A in ("%FLAGS%") do set "FLAGS=%%A"

echo [INFO] Flags   = %FLAGS%

set "OUT=%CD%\%APP%-src-migration-report_%STAMP%.txt"
echo [INFO] Report  = "%OUT%"

echo [STEP] Compiling JdkMigrationScanner.java ...
javac -encoding UTF-8 -Xlint:deprecation -Xlint:unchecked "JdkMigrationScanner.java"
if errorlevel 1 echo [ERROR] Compilation failed. & goto fail
if not exist "JdkMigrationScanner.class" echo [ERROR] Class missing: "JdkMigrationScanner.class" & goto fail

echo [STEP] Running scanner ...
if "%HAS_JSP%"=="1" goto run_with_jsp

goto run_without_jsp

:run_with_jsp
echo [DBG] CMD: "%JAVA_HOME%\bin\java.exe" JdkMigrationScanner "%CSV%" "%SRC%" "%JSP%" "%APP%" %FLAGS%
"%JAVA_HOME%\bin\java.exe" JdkMigrationScanner "%CSV%" "%SRC%" "%JSP%" "%APP%" %FLAGS% 1>"%OUT%" 2>&1
set "RC=%ERRORLEVEL%"
goto after_run

:run_without_jsp
echo [DBG] CMD: "%JAVA_HOME%\bin\java.exe" JdkMigrationScanner "%CSV%" "%SRC%" "" "%APP%" %FLAGS%
"%JAVA_HOME%\bin\java.exe" JdkMigrationScanner "%CSV%" "%SRC%" "" "%APP%" %FLAGS% 1>"%OUT%" 2>&1
set "RC=%ERRORLEVEL%"

:after_run
echo [DBG] Java exit code: %RC%

set "OUTCSV=%CD%\output.csv"
set "TARGETCSV=%CD%\%APP%-src-output_%STAMP%.csv"

echo [DBG] OUTCSV=%OUTCSV%
echo [DBG] TARGETCSV=%TARGETCSV%

if exist "%OUTCSV%" (
  echo [STEP] Found output.csv - renaming to "%TARGETCSV%"
  del /q "%TARGETCSV%" >nul 2>&1
  ren "output.csv" "%APP%-src-output_%STAMP%.csv"
  echo [STEP] Rename completed.
) else (
  echo [WARN] output.csv not found; nothing to rename.
)

echo [RESULT] Exit code: %RC%
if exist "%TARGETCSV%" echo [RESULT] Output CSV: "%TARGETCSV%"

popd >nul
exit /b %RC%

:fail
popd >nul
exit /b 1