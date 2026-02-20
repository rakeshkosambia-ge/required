@echo off
REM ============================================================================
REM FPMS Migration Tool - Windows Setup Script
REM Creates complete Maven-based migration tool structure
REM Run from: fpms_src_web_content directory
REM ============================================================================

echo.
echo ========================================
echo FPMS Migration Tool Setup (Windows)
echo ========================================
echo.

REM Create directory structure
echo Creating directory structure...

mkdir migration_tool\support_excel 2>nul
mkdir migration_tool\src\main\java\com\fpms\migration 2>nul
mkdir migration_tool\src\main\resources 2>nul
mkdir migration_tool\scripts 2>nul
mkdir migration_tool\target 2>nul

REM Create Maven pom.xml
echo Creating pom.xml...
(
echo ^<?xml version="1.0" encoding="UTF-8"?^>
echo ^<project xmlns="http://maven.apache.org/POM/4.0.0"
echo          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
echo          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
echo          http://maven.apache.org/xsd/maven-4.0.0.xsd"^>
echo     ^<modelVersion^>4.0.0^</modelVersion^>
echo.
echo     ^<groupId^>com.fpms^</groupId^>
echo     ^<artifactId^>migration-tool^</artifactId^>
echo     ^<version^>1.0.0-SNAPSHOT^</version^>
echo     ^<packaging^>jar^</packaging^>
echo.
echo     ^<name^>FPMS Migration Tool^</name^>
echo     ^<description^>OpenRewrite-based JDK 8 to JDK 21 Migration Orchestrator^</description^>
echo.
echo     ^<properties^>
echo         ^<maven.compiler.source^>11^</maven.compiler.source^>
echo         ^<maven.compiler.target^>11^</maven.compiler.target^>
echo         ^<project.build.sourceEncoding^>UTF-8^</project.build.sourceEncoding^>
echo         ^<rewrite.version^>8.40.3^</rewrite.version^>
echo         ^<rewrite-maven-plugin.version^>5.46.1^</rewrite-maven-plugin.version^>
echo         ^<poi.version^>5.2.5^</poi.version^>
echo         ^<slf4j.version^>2.0.9^</slf4j.version^>
echo         ^<snakeyaml.version^>2.2^</snakeyaml.version^>
echo     ^</properties^>
echo.
echo     ^<dependencies^>
echo         ^<dependency^>
echo             ^<groupId^>org.apache.poi^</groupId^>
echo             ^<artifactId^>poi^</artifactId^>
echo             ^<version^>${poi.version}^</version^>
echo         ^</dependency^>
echo         ^<dependency^>
echo             ^<groupId^>org.apache.poi^</groupId^>
echo             ^<artifactId^>poi-ooxml^</artifactId^>
echo             ^<version^>${poi.version}^</version^>
echo         ^</dependency^>
echo         ^<dependency^>
echo             ^<groupId^>org.yaml^</groupId^>
echo             ^<artifactId^>snakeyaml^</artifactId^>
echo             ^<version^>${snakeyaml.version}^</version^>
echo         ^</dependency^>
echo         ^<dependency^>
echo             ^<groupId^>org.slf4j^</groupId^>
echo             ^<artifactId^>slf4j-api^</artifactId^>
echo             ^<version^>${slf4j.version}^</version^>
echo         ^</dependency^>
echo         ^<dependency^>
echo             ^<groupId^>org.slf4j^</groupId^>
echo             ^<artifactId^>slf4j-simple^</artifactId^>
echo             ^<version^>${slf4j.version}^</version^>
echo         ^</dependency^>
echo         ^<dependency^>
echo             ^<groupId^>org.openrewrite^</groupId^>
echo             ^<artifactId^>rewrite-java^</artifactId^>
echo             ^<version^>${rewrite.version}^</version^>
echo         ^</dependency^>
echo     ^</dependencies^>
echo.
echo     ^<build^>
echo         ^<plugins^>
echo             ^<plugin^>
echo                 ^<groupId^>org.apache.maven.plugins^</groupId^>
echo                 ^<artifactId^>maven-compiler-plugin^</artifactId^>
echo                 ^<version^>3.11.0^</version^>
echo                 ^<configuration^>
echo                     ^<source^>11^</source^>
echo                     ^<target^>11^</target^>
echo                 ^</configuration^>
echo             ^</plugin^>
echo             ^<plugin^>
echo                 ^<groupId^>org.apache.maven.plugins^</groupId^>
echo                 ^<artifactId^>maven-assembly-plugin^</artifactId^>
echo                 ^<version^>3.6.0^</version^>
echo                 ^<configuration^>
echo                     ^<archive^>
echo                         ^<manifest^>
echo                             ^<mainClass^>com.fpms.migration.MigrationOrchestrator^</mainClass^>
echo                         ^</manifest^>
echo                     ^</archive^>
echo                     ^<descriptorRefs^>
echo                         ^<descriptorRef^>jar-with-dependencies^</descriptorRef^>
echo                     ^</descriptorRefs^>
echo                 ^</configuration^>
echo                 ^<executions^>
echo                     ^<execution^>
echo                         ^<id^>make-assembly^</id^>
echo                         ^<phase^>package^</phase^>
echo                         ^<goals^>
echo                             ^<goal^>single^</goal^>
echo                         ^</goals^>
echo                     ^</execution^>
echo                 ^</executions^>
echo             ^</plugin^>
echo             ^<plugin^>
echo                 ^<groupId^>org.openrewrite.maven^</groupId^>
echo                 ^<artifactId^>rewrite-maven-plugin^</artifactId^>
echo                 ^<version^>${rewrite-maven-plugin.version}^</version^>
echo                 ^<configuration^>
echo                     ^<activeRecipes^>^</activeRecipes^>
echo                     ^<exportDatatables^>true^</exportDatatables^>
echo                 ^</configuration^>
echo                 ^<dependencies^>
echo                     ^<dependency^>
echo                         ^<groupId^>org.openrewrite.recipe^</groupId^>
echo                         ^<artifactId^>rewrite-migrate-java^</artifactId^>
echo                         ^<version^>2.28.0^</version^>
echo                     ^</dependency^>
echo                     ^<dependency^>
echo                         ^<groupId^>org.openrewrite.recipe^</groupId^>
echo                         ^<artifactId^>rewrite-logging-frameworks^</artifactId^>
echo                         ^<version^>2.15.0^</version^>
echo                     ^</dependency^>
echo                 ^</dependencies^>
echo             ^</plugin^>
echo         ^</plugins^>
echo     ^</build^>
echo ^</project^>
) > migration_tool\pom.xml

echo [OK] Created pom.xml

REM Create Java orchestrator (will create in next file due to size)
echo Creating MigrationOrchestrator.java...
echo [Note] Java source file will be created separately
echo [OK] Directory structure ready

REM Create batch file launcher
echo Creating run_migration.bat launcher...
(
echo @echo off
echo REM FPMS Migration Tool Launcher
echo java -jar target\migration-tool-1.0.0-SNAPSHOT-jar-with-dependencies.jar %%*
) > migration_tool\run_migration.bat

echo [OK] Created run_migration.bat

REM Create README
echo Creating README.txt...
(
echo FPMS Migration Tool - Windows Edition
echo ======================================
echo.
echo Quick Start:
echo 1. Place Excel files in support_excel\
echo    - phase1_all_batches_amended.xlsx
echo    - phase2_all_batches_amended.xlsx
echo    - fpms_src_files_by_phase_batch.xlsx
echo.
echo 2. Build: mvn clean package
echo.
echo 3. Run:
echo    run_migration.bat PHASE1 BATCH1 dryrun
echo    run_migration.bat PHASE1 BATCH1 actualrun ICCF12345
echo.
echo Modes:
echo   dryrun    - Copy files locally, no Dimension operations
echo   actualrun - Use dmcli checkout/checkin with ICCF
echo.
echo Examples:
echo   run_migration.bat PHASE1 BATCH1 dryrun
echo   run_migration.bat PHASE1 BATCH2 dryrun --clean
echo   run_migration.bat PHASE1 BATCH1 actualrun ICCF12345
) > migration_tool\README.txt

echo [OK] Created README.txt

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Directory structure created:
echo   migration_tool\
echo     - pom.xml
echo     - README.txt
echo     - run_migration.bat
echo     - support_excel\
echo     - src\main\java\com\fpms\migration\
echo     - scripts\
echo.
echo Next steps:
echo 1. Copy Java source files to src\main\java\com\fpms\migration\
echo 2. Copy Excel files to support_excel\
echo 3. Run: mvn clean package
echo 4. Execute: run_migration.bat PHASE1 BATCH1 dryrun
echo.