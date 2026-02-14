# Complete IntelliJ Multi-Module Setup Guide
## Monolithic Application + OpenRewrite Maven Wrapper in Single Project

---

## Table of Contents
1. [Project Architecture Overview](#project-architecture-overview)
2. [IntelliJ Project Structure Setup](#intellij-project-structure-setup)
3. [Module 1: Monolithic Application](#module-1-monolithic-application-main-app)
4. [Module 2: OpenRewrite Maven Wrapper](#module-2-openrewrite-maven-wrapper-migration-tools)
5. [Compilation Workflow](#compilation-workflow-jdk-8--jdk-21)
6. [OpenRewrite Recipe Application](#openrewrite-recipe-application-workflow)
7. [Complete Step-by-Step Guide](#complete-step-by-step-guide)
8. [Daily Usage Workflow](#daily-usage-workflow)

---

## Project Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│         IntelliJ IDEA Project (Single Workspace)            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Module 1: monolithic-app (Main Application)               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ - WebRoot/ (JSP, HTML, CSS)                          │  │
│  │ - src/main/java/ (Java source)                       │  │
│  │ - lib/ (Third-party JARs)                            │  │
│  │ - build/ (Compiled .class files)                     │  │
│  │ - NO Maven - Traditional structure                   │  │
│  │ - Compile with JDK 8 or JDK 21 (switchable)        │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↕                                  │
│                   (Source code shared)                      │
│                          ↕                                  │
│  Module 2: migration-tools (OpenRewrite Wrapper)           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ - pom.xml (Maven configuration)                      │  │
│  │ - migration/recipes/ (OpenRewrite recipes)           │  │
│  │ - Runs OpenRewrite on Module 1 source                │  │
│  │ - ONLY for transformation, NOT for compilation       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Workflow:
1. Use Module 2 (migration-tools) to apply OpenRewrite recipes
2. Recipes transform source code in Module 1 (monolithic-app)
3. Use IntelliJ's compiler to build Module 1 with JDK 8 or JDK 21
4. Switch JDK versions easily for testing both
```

---

## IntelliJ Project Structure Setup

### Step 1: Create Root Project

1. **Open IntelliJ IDEA**
2. **File → New → Project**
3. Settings:
   - **Name:** `enterprise-app-migration`
   - **Location:** Choose your workspace (e.g., `C:\workspace\enterprise-app-migration`)
   - **Language:** Java
   - **Build system:** IntelliJ (NOT Maven - we'll add Maven module separately)
   - **JDK:** Select JDK 8 (we'll switch between 8 and 21 later)
4. **Create**

### Step 2: Configure Project Structure

After project creation:

**File → Project Structure** (Ctrl+Alt+Shift+S)

**Project Settings:**
- **Project name:** `enterprise-app-migration`
- **Project SDK:** JDK 8
- **Project language level:** 8 - Lambdas, type annotations etc.
- **Project compiler output:** `${PROJECT_DIR}/out`

**Click OK**

---

## Module 1: Monolithic Application (Main App)

### Step 1: Create Monolithic Application Module

**File → Project Structure → Modules**

1. **Click + (Add) → New Module**
2. Settings:
   - **Name:** `monolithic-app`
   - **Module file location:** `${PROJECT_DIR}/monolithic-app`
   - **Language:** Java
   - **Build system:** IntelliJ (Traditional, not Maven)
3. **Create**

### Step 2: Create Traditional Enterprise Structure

**In Project panel, right-click on `monolithic-app` → New → Directory**

Create these folders:

```
monolithic-app/
├── WebRoot/                    # Web resources
│   ├── WEB-INF/
│   │   ├── web.xml
│   │   ├── lib/               # Third-party JARs (copied from lib/)
│   │   └── classes/           # Will contain compiled .class files
│   ├── jsp/                   # JSP files
│   ├── html/                  # HTML files
│   ├── css/                   # CSS files
│   └── js/                    # JavaScript files
├── src/                       # Java source code
│   ├── com/
│   │   └── company/
│   │       ├── dao/
│   │       ├── service/
│   │       ├── servlet/
│   │       ├── bean/
│   │       └── util/
│   └── (your package structure)
├── lib/                       # Third-party JAR libraries
│   ├── struts-1.1.jar
│   ├── commons-*.jar
│   └── (all your third-party JARs)
├── build/                     # Compiled output
│   └── classes/              # .class files go here
└── dimension/                # Dimension CM integration scripts
    └── dmcli-scripts/
```

**Create these directories:**

```
Right-click on monolithic-app:
- New → Directory → "WebRoot"
- New → Directory → "WebRoot/WEB-INF"
- New → Directory → "WebRoot/WEB-INF/lib"
- New → Directory → "WebRoot/WEB-INF/classes"
- New → Directory → "WebRoot/jsp"
- New → Directory → "src"
- New → Directory → "lib"
- New → Directory → "build"
- New → Directory → "build/classes"
```

### Step 3: Configure Module Paths

**File → Project Structure → Modules → monolithic-app**

**Sources Tab:**

1. **Click on `src` folder → Mark as "Sources"** (should turn blue)
2. **Output path:** `${MODULE_DIR}/build/classes`

**Dependencies Tab:**

1. **Click + → JARs or directories**
2. **Navigate to `monolithic-app/lib`**
3. **Select all JAR files** (Ctrl+A)
4. **OK**

All JARs from `lib/` are now in classpath.

**Paths Tab:**

- **Use module compile output path:** ✅ Selected
- **Output path:** `${MODULE_DIR}/build/classes`
- **Test output path:** `${MODULE_DIR}/build/test-classes`

**Click OK**

### Step 4: Configure Compiler for Module

**File → Settings → Build, Execution, Deployment → Compiler → Java Compiler**

**Per-module bytecode version:**

| Module | Target bytecode version |
|--------|------------------------|
| monolithic-app | 8 |

This ensures compilation always uses JDK 8 bytecode (compatible with both 8 and 21).

### Step 5: Create Compilation Script

**Right-click on `monolithic-app` → New → File**

**File: `compile.bat`**

```batch
@echo off
REM Compilation script for monolithic application
REM Can switch between JDK 8 and JDK 21

setlocal

set PROJECT_DIR=%~dp0
set SRC_DIR=%PROJECT_DIR%src
set BUILD_DIR=%PROJECT_DIR%build\classes
set LIB_DIR=%PROJECT_DIR%lib
set WEBROOT_CLASSES=%PROJECT_DIR%WebRoot\WEB-INF\classes

REM Default to JDK 8 unless specified
if "%1"=="" (
    set JDK_VERSION=8
) else (
    set JDK_VERSION=%1
)

echo ===================================================
echo   Compiling Monolithic Application
echo   JDK Version: %JDK_VERSION%
echo ===================================================

REM Set JAVA_HOME based on version
if "%JDK_VERSION%"=="8" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
) else if "%JDK_VERSION%"=="21" (
    set JAVA_HOME=C:\Program Files\Java\jdk-21
) else (
    echo ERROR: Invalid JDK version. Use 8 or 21.
    exit /b 1
)

set PATH=%JAVA_HOME%\bin;%PATH%

echo Using Java: 
java -version
echo.

REM Clean build directory
echo Cleaning build directory...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"

REM Build classpath from all JARs in lib/
echo Building classpath...
set CLASSPATH=
for %%f in ("%LIB_DIR%\*.jar") do (
    set CLASSPATH=!CLASSPATH!%%f;
)

REM Compile Java source files
echo Compiling Java source files...
dir /s /b "%SRC_DIR%\*.java" > sources.txt

javac -d "%BUILD_DIR%" ^
      -cp "%CLASSPATH%" ^
      -sourcepath "%SRC_DIR%" ^
      -source 8 ^
      -target 8 ^
      -encoding UTF-8 ^
      @sources.txt

if %errorlevel% neq 0 (
    echo Compilation FAILED!
    del sources.txt
    exit /b 1
)

del sources.txt

echo.
echo Compilation successful!
echo Output: %BUILD_DIR%

REM Copy to WebRoot for deployment
echo.
echo Copying to WebRoot/WEB-INF/classes...
xcopy /s /y "%BUILD_DIR%\*" "%WEBROOT_CLASSES%\" >nul

echo.
echo ===================================================
echo   Compilation Complete
echo   JDK: %JDK_VERSION%
echo   Classes: %BUILD_DIR%
echo   WebRoot: %WEBROOT_CLASSES%
echo ===================================================

endlocal
```

**Make it executable:**

Right-click → Properties → Unblock (if needed)

**Usage:**

```batch
REM Compile with JDK 8
compile.bat 8

REM Compile with JDK 21
compile.bat 21

REM Default (JDK 8)
compile.bat
```

---

## Module 2: OpenRewrite Maven Wrapper (Migration Tools)

### Step 1: Create Maven Module for OpenRewrite

**File → New → Module**

Settings:
- **Name:** `migration-tools`
- **Location:** `${PROJECT_DIR}/migration-tools`
- **Language:** Java
- **Build system:** **Maven** ✅ (Important!)
- **JDK:** JDK 8
- **Add sample code:** Uncheck

**Create**

IntelliJ will create a Maven module with `pom.xml`.

### Step 2: Configure pom.xml for OpenRewrite

**Open: `migration-tools/pom.xml`**

**Replace entire content with:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company</groupId>
    <artifactId>migration-tools</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>OpenRewrite Migration Tools</name>
    <description>Maven wrapper for applying OpenRewrite recipes to monolithic-app</description>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <rewrite.version>5.25.0</rewrite.version>
        
        <!-- Path to monolithic-app source code -->
        <app.source.dir>${project.parent.basedir}/monolithic-app/src</app.source.dir>
    </properties>

    <build>
        <plugins>
            <!-- OpenRewrite Maven Plugin -->
            <plugin>
                <groupId>org.openrewrite.maven</groupId>
                <artifactId>rewrite-maven-plugin</artifactId>
                <version>${rewrite.version}</version>
                <configuration>
                    <!-- Path to master recipe -->
                    <configLocation>${project.basedir}/migration/recipes/master-safe-patterns.yml</configLocation>
                    
                    <!-- Source code location (monolithic-app) -->
                    <plainTextMasks>
                        <mask>**/*.java</mask>
                    </plainTextMasks>
                    
                    <!-- Active recipes -->
                    <activeRecipes>
                        <recipe>com.company.migration.AllSafePatterns</recipe>
                    </activeRecipes>
                    
                    <!-- OpenRewrite will scan and modify files in monolithic-app -->
                    <pomCacheEnabled>false</pomCacheEnabled>
                    
                    <!-- Export data tables for analysis -->
                    <exportDatatables>true</exportDatatables>
                </configuration>
                
                <dependencies>
                    <dependency>
                        <groupId>org.openrewrite.recipe</groupId>
                        <artifactId>rewrite-migrate-java</artifactId>
                        <version>2.11.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.openrewrite.recipe</groupId>
                        <artifactId>rewrite-static-analysis</artifactId>
                        <version>1.6.0</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 3: Create Migration Folder Structure

**In `migration-tools/` module, create:**

```
migration-tools/
├── pom.xml
├── migration/
│   ├── recipes/
│   │   ├── P001_recipe.yml
│   │   ├── P002_recipe.yml
│   │   ├── P003_recipe.yml
│   │   ├── P004_recipe.yml
│   │   ├── P005_recipe.yml
│   │   ├── P006_recipe.yml
│   │   ├── P007_recipe.yml
│   │   ├── P008_recipe.yml
│   │   └── master-safe-patterns.yml
│   ├── patterns/
│   │   ├── analysis_core_v4_enhanced.csv
│   │   └── file-lists/
│   └── scripts/
│       ├── apply-pattern.bat
│       └── apply-all.bat
└── rewrite.yml                   # OpenRewrite config pointing to monolithic-app
```

**Create directories:**

```
Right-click on migration-tools:
- New → Directory → "migration"
- New → Directory → "migration/recipes"
- New → Directory → "migration/patterns"
- New → Directory → "migration/patterns/file-lists"
- New → Directory → "migration/scripts"
```

### Step 4: Create rewrite.yml Configuration

**File: `migration-tools/rewrite.yml`**

```yaml
---
# OpenRewrite configuration for monolithic-app transformation
# This file tells OpenRewrite where to find source files

# Source paths (pointing to monolithic-app module)
sourcePath:
  - ../monolithic-app/src

# Recipe paths
recipePath:
  - migration/recipes

# Active style
activeStyles:
  - com.company.migration.AllSafePatterns

# Parser configuration
parserClasspathFromResources: false

# Exclusions
exclusions:
  - "**/build/**"
  - "**/target/**"
  - "**/WebRoot/**"
  - "**/.dimension/**"
```

### Step 5: Copy Recipe Files

**Copy all recipe YAML files from earlier section to:**

`migration-tools/migration/recipes/`

Files to create:
- `P001_recipe.yml` (Vector → ArrayList)
- `P002_recipe.yml` (Hashtable → HashMap)
- `P003_recipe.yml` (StringBuffer → StringBuilder)
- `P004_recipe.yml` (Enumeration → Iterator)
- `P005_recipe.yml` (Date → LocalDateTime)
- `P006_recipe.yml` (javax.servlet → jakarta.servlet)
- `P007_recipe.yml` (javax.persistence → jakarta.persistence)
- `P008_recipe.yml` (Class.newInstance() update)
- `master-safe-patterns.yml` (all recipes combined)

*Use the YAML content from the previous section - I won't repeat it here to save space*

### Step 6: Create Application Scripts

**File: `migration-tools/migration/scripts/apply-pattern.bat`**

```batch
@echo off
REM Apply OpenRewrite pattern to monolithic-app source code

setlocal

if "%1"=="" (
    echo Usage: apply-pattern.bat ^<PatternID^>
    echo Example: apply-pattern.bat P001
    exit /b 1
)

set PATTERN_ID=%1
set RECIPE=com.company.migration.%PATTERN_ID%

echo ===================================================
echo   Applying OpenRewrite Pattern
echo   Pattern: %PATTERN_ID%
echo   Recipe: %RECIPE%
echo ===================================================

REM Navigate to migration-tools directory
cd /d %~dp0\..\..

echo.
echo Working directory: %CD%
echo Target source code: ..\monolithic-app\src
echo.

REM Run OpenRewrite with Maven
echo Running: mvn rewrite:run -Drewrite.activeRecipes=%RECIPE%
echo.

call mvn rewrite:run -Drewrite.activeRecipes=%RECIPE%

if %errorlevel% equ 0 (
    echo.
    echo ===================================================
    echo   Pattern %PATTERN_ID% applied successfully!
    echo   Modified files in: ..\monolithic-app\src
    echo ===================================================
) else (
    echo.
    echo ===================================================
    echo   Pattern %PATTERN_ID% application FAILED
    echo ===================================================
    exit /b 1
)

endlocal
```

**File: `migration-tools/migration/scripts/apply-all.bat`**

```batch
@echo off
REM Apply all SAFE patterns to monolithic-app

setlocal enabledelayedexpansion

echo ===================================================
echo   Applying All SAFE Patterns
echo ===================================================

set PATTERNS=P001 P002 P003 P004 P005 P006 P007 P008
set SUCCESS=0
set FAILED=0

REM Navigate to migration-tools
cd /d %~dp0\..\..

for %%P in (%PATTERNS%) do (
    echo.
    echo ┌─────────────────────────────────────────────┐
    echo │ Pattern: %%P
    echo └─────────────────────────────────────────────┘
    
    call mvn rewrite:run -Drewrite.activeRecipes=com.company.migration.%%P
    
    if !errorlevel! equ 0 (
        echo √ %%P completed
        set /a SUCCESS+=1
    ) else (
        echo × %%P failed
        set /a FAILED+=1
    )
    
    timeout /t 2 /nobreak >nul
)

echo.
echo ===================================================
echo   Summary: Success: %SUCCESS% ^| Failed: %FAILED%
echo ===================================================

endlocal
```

---

## Compilation Workflow (JDK 8 & JDK 21)

### Method 1: IntelliJ Build (GUI)

#### Compile with JDK 8

1. **File → Project Structure → Project**
2. **Project SDK:** Select JDK 8
3. **Project language level:** 8
4. **Apply**
5. **Build → Rebuild Project**
6. Output: `monolithic-app/build/classes/`

#### Compile with JDK 21

1. **File → Project Structure → Project**
2. **Project SDK:** Select JDK 21
3. **Project language level:** 21 (or keep 8 for compatibility)
4. **Apply**
5. **Build → Rebuild Project**
6. Output: `monolithic-app/build/classes/`

### Method 2: Using Batch Script

**Navigate to `monolithic-app/` in Terminal:**

```batch
REM Compile with JDK 8
compile.bat 8

REM Compile with JDK 21
compile.bat 21
```

### Method 3: IntelliJ External Tool

**Create External Tool for Quick Compilation:**

**Tools → External Tools → +**

**Settings for JDK 8 Compilation:**
- **Name:** `Compile with JDK 8`
- **Program:** `$ModuleFileDir$/compile.bat`
- **Arguments:** `8`
- **Working directory:** `$ModuleFileDir$`

**Settings for JDK 21 Compilation:**
- **Name:** `Compile with JDK 21`
- **Program:** `$ModuleFileDir$/compile.bat`
- **Arguments:** `21`
- **Working directory:** `$ModuleFileDir$`

**Usage:**

Right-click on `monolithic-app` → **External Tools → Compile with JDK 8**

---

## OpenRewrite Recipe Application Workflow

### Method 1: IntelliJ Terminal

**View → Tool Windows → Terminal**

**Navigate to migration-tools:**

```batch
cd migration-tools
```

**Apply single pattern:**

```batch
mvn rewrite:run -Drewrite.activeRecipes=com.company.migration.P001
```

**Preview first (dry run):**

```batch
mvn rewrite:dryRun -Drewrite.activeRecipes=com.company.migration.P001
```

**Apply all SAFE patterns:**

```batch
mvn rewrite:run
```

### Method 2: Using Scripts

**In Terminal, navigate to scripts:**

```batch
cd migration-tools\migration\scripts
```

**Apply single pattern:**

```batch
apply-pattern.bat P001
```

**Apply all patterns:**

```batch
apply-all.bat
```

### Method 3: IntelliJ Maven Tool Window

1. **View → Tool Windows → Maven**
2. **Expand `migration-tools`**
3. **Expand `Plugins`**
4. **Expand `rewrite`**
5. **Double-click `rewrite:run`**

### Method 4: Run Configuration

**Create Run Configuration:**

**Run → Edit Configurations → + → Maven**

**Settings:**
- **Name:** `Apply Pattern P001`
- **Working directory:** `$ProjectFileDir$/migration-tools`
- **Command line:** `rewrite:run -Drewrite.activeRecipes=com.company.migration.P001`

**Create one for each pattern, or use variables**

---

## Complete Step-by-Step Guide

### Initial Setup (One-Time)

#### Day 1: Create Project Structure

**1. Create IntelliJ Project:**

```
File → New → Project
- Name: enterprise-app-migration
- Build system: IntelliJ
- JDK: 8
```

**2. Add monolithic-app module:**

```
File → New → Module
- Name: monolithic-app
- Build system: IntelliJ (NOT Maven)
```

**3. Copy your existing code:**

```
Copy WebRoot/ → monolithic-app/WebRoot/
Copy src/ → monolithic-app/src/
Copy lib/ → monolithic-app/lib/
```

**4. Configure monolithic-app:**

```
File → Project Structure → Modules → monolithic-app
- Sources: Mark src/ as Sources
- Dependencies: Add all JARs from lib/
- Paths: Output to build/classes/
```

**5. Add migration-tools Maven module:**

```
File → New → Module
- Name: migration-tools
- Build system: Maven ✅
```

**6. Configure migration-tools:**

```
Copy pom.xml (from this guide)
Create migration/ folder structure
Copy recipe YAML files
Copy scripts
```

**7. Reload Maven:**

```
Right-click on migration-tools/pom.xml → Maven → Reload Project
```

#### Day 2: Test Setup

**1. Compile monolithic-app with JDK 8:**

```batch
cd monolithic-app
compile.bat 8
```

Verify: `build/classes/` contains .class files

**2. Compile with JDK 21:**

```batch
compile.bat 21
```

Verify: Compiles successfully

**3. Test OpenRewrite:**

```batch
cd migration-tools
mvn rewrite:discover
```

Should list all recipes (P001-P008, AllSafePatterns)

**4. Dry run a pattern:**

```batch
mvn rewrite:dryRun -Drewrite.activeRecipes=com.company.migration.P001
```

Review output - should show what would change

---

## Daily Usage Workflow

### Morning: Choose Pattern to Apply

**1. Review CSV:**

Open: `migration-tools/migration/patterns/analysis_core_v4_enhanced.csv`

Choose pattern for today, e.g., **P001**

**2. Checkout from Dimension (if using):**

```batch
REM Get files affected by P001
for /f %%f in (migration-tools\migration\patterns\file-lists\P001_files.txt) do (
    dmcli checkout -project YOUR_PROJECT -workset SAFE-MIGRATION-WS -file monolithic-app\src\%%f
)
```

### Step 1: Apply OpenRewrite Recipe

**In IntelliJ Terminal:**

```batch
cd migration-tools
mvn rewrite:run -Drewrite.activeRecipes=com.company.migration.P001
```

**Or using script:**

```batch
cd migration-tools\migration\scripts
apply-pattern.bat P001
```

**OpenRewrite modifies files in:** `monolithic-app/src/`

### Step 2: Review Changes in IntelliJ

**Version Control panel (Alt+9):**

1. **Local Changes** tab shows modified files
2. **Click on file** to see diff
3. **Review each change**

**Navigate through changes:**
- **Ctrl+D** - Show diff
- **F7** - Next difference
- **Shift+F7** - Previous difference

### Step 3: Compile with JDK 8

**Method A: IntelliJ GUI:**

```
File → Project Structure → Project SDK → JDK 8
Build → Rebuild Project
```

**Method B: Script:**

```batch
cd monolithic-app
compile.bat 8
```

**Verify:** No compilation errors

### Step 4: Compile with JDK 21

**Method A: IntelliJ GUI:**

```
File → Project Structure → Project SDK → JDK 21
Build → Rebuild Project
```

**Method B: Script:**

```batch
cd monolithic-app
compile.bat 21
```

**Verify:** Still compiles (SAFE pattern works on both!)

### Step 5: Test (If Applicable)

Run your application tests to verify functionality.

### Step 6: Commit/Checkin

**If validation passes:**

**Option A: Git (if using):**

```batch
git add monolithic-app/src
git commit -m "MIGRATION P001: Vector → ArrayList | SAFE | JDK 8/21 validated"
```

**Option B: Dimension CM:**

```batch
REM Checkin modified files
for /f %%f in (migration-tools\migration\patterns\file-lists\P001_files.txt) do (
    dmcli checkin -project YOUR_PROJECT -workset SAFE-MIGRATION-WS ^
        -file monolithic-app\src\%%f ^
        -comment "MIGRATION P001: Vector → ArrayList | SAFE"
)
```

---

## Master Automation Script

**Create: `enterprise-app-migration/migrate-and-compile.bat`**

```batch
@echo off
REM Master script: Apply pattern, compile with both JDKs, validate

setlocal

if "%1"=="" (
    echo Usage: migrate-and-compile.bat ^<PatternID^>
    echo Example: migrate-and-compile.bat P001
    exit /b 1
)

set PATTERN_ID=%1

echo ═══════════════════════════════════════════════════
echo   JDK Migration Workflow - Pattern %PATTERN_ID%
echo ═══════════════════════════════════════════════════

REM Step 1: Apply OpenRewrite recipe
echo.
echo Step 1: Applying OpenRewrite recipe...
cd migration-tools
call mvn rewrite:run -Drewrite.activeRecipes=com.company.migration.%PATTERN_ID%

if %errorlevel% neq 0 (
    echo ERROR: OpenRewrite failed
    exit /b 1
)

cd ..

REM Step 2: Compile with JDK 8
echo.
echo Step 2: Compiling with JDK 8...
cd monolithic-app
call compile.bat 8

if %errorlevel% neq 0 (
    echo ERROR: JDK 8 compilation failed
    cd ..
    exit /b 1
)

REM Step 3: Compile with JDK 21
echo.
echo Step 3: Compiling with JDK 21...
call compile.bat 21

if %errorlevel% neq 0 (
    echo ERROR: JDK 21 compilation failed
    cd ..
    exit /b 1
)

cd ..

echo.
echo ═══════════════════════════════════════════════════
echo   ✓ Pattern %PATTERN_ID% applied and validated!
echo   ✓ Compiles with JDK 8
echo   ✓ Compiles with JDK 21
echo ═══════════════════════════════════════════════════

endlocal
```

**Usage:**

```batch
migrate-and-compile.bat P001
```

This runs the entire workflow automatically!

---

## IntelliJ Run Configurations

### Create Master Run Configuration

**Run → Edit Configurations → + → Compound**

**Settings:**
- **Name:** `Migrate Pattern P001 (Full Workflow)`

**Add configurations:**
1. **Apply OpenRewrite P001** (Maven)
2. **Compile with JDK 8** (External Tool)
3. **Compile with JDK 21** (External Tool)

**Usage:**

Click **Run → Migrate Pattern P001 (Full Workflow)**

All steps execute in sequence!

---

## Project Structure Summary

```
enterprise-app-migration/              # IntelliJ Project Root
├── monolithic-app/                    # Module 1: Your Application
│   ├── WebRoot/                       # Web resources
│   │   ├── WEB-INF/
│   │   │   ├── web.xml
│   │   │   ├── lib/                  # Copied from lib/
│   │   │   └── classes/              # Deployed .class files
│   │   ├── jsp/
│   │   └── (other web resources)
│   ├── src/                          # Java source code (OpenRewrite modifies this)
│   │   └── com/company/...
│   ├── lib/                          # Third-party JARs
│   │   ├── struts-1.1.jar
│   │   └── (all JARs)
│   ├── build/                        # Compilation output
│   │   └── classes/                  # .class files
│   └── compile.bat                   # Compilation script
│
├── migration-tools/                   # Module 2: OpenRewrite Wrapper
│   ├── pom.xml                       # Maven config
│   ├── rewrite.yml                   # OpenRewrite config
│   ├── migration/
│   │   ├── recipes/
│   │   │   ├── P001_recipe.yml
│   │   │   ├── ...
│   │   │   └── master-safe-patterns.yml
│   │   ├── patterns/
│   │   │   ├── analysis_core_v4_enhanced.csv
│   │   │   └── file-lists/
│   │   └── scripts/
│   │       ├── apply-pattern.bat
│   │       └── apply-all.bat
│   └── target/                       # Maven build output
│
└── migrate-and-compile.bat           # Master automation script
```

---

## Quick Reference

### Compilation

```batch
# Compile with JDK 8
cd monolithic-app
compile.bat 8

# Compile with JDK 21
compile.bat 21

# Or in IntelliJ:
File → Project Structure → Project SDK → Select JDK
Build → Rebuild Project
```

### Apply OpenRewrite

```batch
# Single pattern
cd migration-tools
mvn rewrite:run -Drewrite.activeRecipes=com.company.migration.P001

# Or using script
migration\scripts\apply-pattern.bat P001

# All patterns
migration\scripts\apply-all.bat
```

### Full Workflow

```batch
# From project root
migrate-and-compile.bat P001
```

---

## Summary

You now have:

✅ **Two-module IntelliJ project:**
- `monolithic-app` - Traditional Java project (no Maven)
- `migration-tools` - Maven wrapper for OpenRewrite

✅ **Separate compilation:**
- Compile `monolithic-app` with JDK 8 or JDK 21
- Switch JDKs easily in IntelliJ
- Use batch script for command-line compilation

✅ **OpenRewrite integration:**
- Apply recipes to `monolithic-app` source
- Use `migration-tools` Maven module
- Recipes modify source, don't affect compilation

✅ **Complete workflow:**
- Apply recipe → Compile JDK 8 → Compile JDK 21 → Validate
- All in one IntelliJ project
- Simple batch scripts for automation

**Your monolithic app stays traditional, while OpenRewrite lives in its own Maven module!** 🎉