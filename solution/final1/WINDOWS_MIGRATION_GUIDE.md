# FPMS Migration Tool - Windows Edition
## Complete Setup and Usage Guide

---

## Table of Contents
1. [Quick Start](#quick-start)
2. [Initial Setup](#initial-setup)
3. [Understanding Modes](#understanding-modes)
4. [Step-by-Step Execution](#step-by-step-execution)
5. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites
- Windows 10/11
- JDK 11+ installed (for building migration tool)
- JDK 8 and JDK 21 installed (for validation)
- Maven 3.6+ installed
- Dimension CM client with dmcli (for actualrun mode)
- Command Prompt (cmd.exe) - **NOT PowerShell**

### Directory Structure
```
fpms_src_web_content\
├── fpms_module\              # Your monolithic application
│   ├── src\                  # Java source code
│   └── ls_web\               # Web resources
└── migration_tool\           # Will be created by setup
```

---

## Initial Setup

### Step 1: Run Setup Script

Open **Command Prompt** (NOT PowerShell) and navigate to your project root:

```batch
cd C:\path\to\fpms_src_web_content
setup_migration_tool.bat
```

This creates:
```
migration_tool\
├── pom.xml                   # Maven configuration
├── README.txt                # Quick reference
├── run_migration.bat         # Launcher script
├── support_excel\            # Excel files go here
├── src\main\java\com\fpms\migration\
│   └── MigrationOrchestrator.java
└── scripts\                  # Helper scripts
```

### Step 2: Copy Java Source

Copy the Java orchestrator to the correct location:

```batch
cd migration_tool
copy MigrationOrchestrator.java src\main\java\com\fpms\migration\
```

### Step 3: Add Excel Files

Copy your three Excel files to `migration_tool\support_excel\`:

```batch
cd support_excel

REM Copy Phase 1 and Phase 2 Excel files
copy "C:\path\to\phase1_all_batches_amended.xlsx" .
copy "C:\path\to\phase2_all_batches_amended.xlsx" .

REM Copy or create file list
copy "C:\path\to\fpms_src_files_by_phase_batch.xlsx" .
```

### Step 4: Create File List Excel

Create `fpms_src_files_by_phase_batch.xlsx` with these columns:

| PHASE | BATCH | FILEPATHNAME |
|-------|-------|--------------|
| PHASE1 | BATCH1 | src\com\fpms\security\SecurityManager.java |
| PHASE1 | BATCH1 | src\com\fpms\util\Base64Encoder.java |
| PHASE1 | BATCH2 | src\com\fpms\util\DateHelper.java |

**Important:** 
- Use Windows-style paths with backslashes `\`
- Paths are relative to `fpms_module\`
- Do NOT include `fpms_module\` prefix

**Example for different file types:**
```
PHASE1, BATCH1:
  src\com\fpms\security\SecurityMgr.java
  src\com\fpms\util\Base64Util.java
  
PHASE1, BATCH2:
  src\com\fpms\util\DateUtil.java
  src\com\fpms\core\TimestampHelper.java
  
PHASE1, BATCH6:
  src\com\fpms\xml\JaxbParser.java
  src\com\fpms\model\XmlEntity.java

PHASE2, BATCH1:
  ls_web\jsp\login.jsp
  ls_web\jsp\security\access.jsp
```

### Step 5: Build the Tool

```batch
cd migration_tool
mvn clean package
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] 
[INFO] migration-tool-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

Verify:
```batch
dir target\migration-tool-*-jar-with-dependencies.jar
```

---

## Understanding Modes

### DRYRUN Mode
- **Purpose:** Safe testing without affecting Dimension CM
- **Source:** Copies files from local `fpms_module\` directory
- **Destination:** Creates working directory in `fpms_module\PHASE1_BATCH1\`
- **Dimension:** No dmcli operations executed
- **Use Case:** Testing, validation, iterative refinement

### ACTUALRUN Mode
- **Purpose:** Production migration with Dimension CM integration
- **Source:** Checks out files from Dimension using dmcli
- **Process:** Checkout → Migrate → Validate → Checkin
- **Dimension:** Full dmcli checkout and checkin operations
- **Requirement:** ICCF number mandatory
- **Use Case:** Final production execution after validation

---

## Step-by-Step Execution

### Phase 1, Batch 1 - Complete Example

#### STEP 1: DRY RUN

```batch
cd migration_tool
run_migration.bat PHASE1 BATCH1 dryrun
```

**What happens:**
1. ✅ Validates inputs (Excel files, fpms_module exists)
2. ✅ Creates `fpms_module\PHASE1_BATCH1\` directory
3. ✅ Reads file list from Excel for PHASE1/BATCH1
4. ✅ **COPIES files from fpms_module\src** (local copy, no Dimension)
5. ✅ Generates OpenRewrite YAML from Phase 1 Excel recipes
6. ✅ Applies OpenRewrite transformations
7. ✅ Validates compilation (generates scripts)
8. ✅ Creates Dimension scripts for manual review
9. ✅ Generates migration report

**Output Example:**
```
═══════════════════════════════════════════════════════════
FPMS Migration Orchestrator - Windows Edition
═══════════════════════════════════════════════════════════
Phase        : PHASE1
Batch        : BATCH1
Mode         : DRYRUN
Working Dir  : ..\fpms_module\PHASE1_BATCH1
═══════════════════════════════════════════════════════════

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 1: Validating inputs...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  [OK] All inputs validated

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 3: Creating working directory...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  [OK] Created: ..\fpms_module\PHASE1_BATCH1

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 4: Reading impacted file list...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Found 247 impacted files

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 5: Copying files from fpms_module (DRYRUN mode)...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Mode: DRYRUN - Copying from local fpms_module
  [OK] Copied 247 files

... (OpenRewrite, compilation, reporting steps) ...

═══════════════════════════════════════════════════════════
Migration PHASE1 BATCH1 completed successfully!
═══════════════════════════════════════════════════════════
Working directory: ..\fpms_module\PHASE1_BATCH1
Next steps:
  1. Review migrated code in: ..\fpms_module\PHASE1_BATCH1
  2. Test compilation with JDK 8 and JDK 21
  3. Review OpenRewrite changes
  4. Run with 'actualrun' mode when ready for Dimension checkin
```

#### STEP 2: Review Migrated Code

```batch
cd ..\fpms_module\PHASE1_BATCH1

REM Check directory structure
dir /s

REM Review OpenRewrite recipe
type rewrite.yml

REM Check migration report
type reports\migration_report.txt

REM Review Dimension scripts (for reference)
type scripts\dimension_checkout.bat
type scripts\dimension_checkin.bat
```

**Directory structure created:**
```
PHASE1_BATCH1\
├── src\                        # Migrated Java files
│   └── com\fpms\...
├── ls_web\                     # Migrated web files (if any)
├── rewrite.yml                 # OpenRewrite recipe used
├── pom.xml                     # Temporary Maven POM
├── reports\
│   └── migration_report.txt   # Migration summary
└── scripts\
    ├── compile_jdk8.bat       # JDK 8 compilation script
    ├── compile_jdk21.bat      # JDK 21 compilation script
    ├── dimension_checkout.bat # Dimension checkout (for reference)
    └── dimension_checkin.bat  # Dimension checkin (for reference)
```

#### STEP 3: Manual Compilation Test

**Test with JDK 8:**
```batch
REM Set JAVA_HOME to JDK 8
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
set PATH=%JAVA_HOME%\bin;%PATH%

REM Verify Java version
java -version
REM Should show: java version "1.8.0_xxx"

REM Run compilation script
scripts\compile_jdk8.bat
```

**Test with JDK 21:**
```batch
REM Set JAVA_HOME to JDK 21
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

REM Verify Java version
java -version
REM Should show: java version "21.x.x"

REM Run compilation script
scripts\compile_jdk21.bat
```

Both should complete without errors.

#### STEP 4: Review Changes

Compare original vs migrated code:

```batch
REM Compare files using Windows FC command
fc ..\src\com\fpms\util\Base64Util.java ^
   src\com\fpms\util\Base64Util.java

REM Or use your preferred diff tool (WinMerge, Beyond Compare, etc.)
```

**Example changes you'll see:**
```java
// BEFORE (original in fpms_module\src\):
import sun.misc.BASE64Encoder;
...
BASE64Encoder encoder = new BASE64Encoder();
String encoded = encoder.encode(bytes);

// AFTER (migrated in PHASE1_BATCH1\src\):
import java.util.Base64;
...
String encoded = Base64.getEncoder().encodeToString(bytes);
```

#### STEP 5: Fix Issues (If Needed)

If you find issues, you have options:

**Option A: Update recipes and re-run**
```batch
REM Edit Excel file with corrected recipes
notepad ..\migration_tool\support_excel\phase1_all_batches_amended.xlsx

REM Re-run with clean flag
cd ..\migration_tool
run_migration.bat PHASE1 BATCH1 dryrun --clean
```

**Option B: Manual fixes**
```batch
cd ..\fpms_module\PHASE1_BATCH1\src

REM Manually edit files
notepad com\fpms\util\ProblematicFile.java

REM Re-compile to verify
javac -d ..\classes com\fpms\util\ProblematicFile.java
```

#### STEP 6: ACTUAL RUN (Production)

Once validated, run production with ICCF:

**1. Create ICCF Ticket**
```
Example ICCF: ICCF12345
Title: "FPMS Migration Phase 1 Batch 1 - sun.* API Removal"
Description: "Migrate 247 files from sun.* internal APIs to java.util.Base64"
```

**2. Execute Production Migration**
```batch
cd migration_tool
run_migration.bat PHASE1 BATCH1 actualrun ICCF12345
```

**What happens in ACTUALRUN:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 5: Checking out files from Dimension (ACTUALRUN mode)...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Mode: ACTUALRUN - Checking out from Dimension
  ICCF: ICCF12345
  [OK] Generated checkout script: ...\scripts\dimension_checkout.bat
  Executing checkout...
    [1/247] Checking out: src/com/fpms/security/SecurityMgr.java
    dmcli -cmd "co 'src/com/fpms/security/SecurityMgr.java'"
    [OK]
    [2/247] Checking out: src/com/fpms/util/Base64Util.java
    dmcli -cmd "co 'src/com/fpms/util/Base64Util.java'"
    [OK]
    ... (continues for all 247 files) ...
  [OK] Dimension checkout completed
  Copying checked out files to working directory...
  [OK] Copied 247 files to working directory

... (OpenRewrite, compilation, reporting steps) ...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 10: Checking in to Dimension...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Copying migrated files back to fpms_module...
  [OK] Copied 247 migrated files back to fpms_module
  Checking in to Dimension with ICCF: ICCF12345
  [OK] Generated checkin script: ...\scripts\dimension_checkin.bat
  Executing checkin...
    [1/247] Checking in: src/com/fpms/security/SecurityMgr.java
    dmcli -cmd "ci -r 'ICCF12345' 'src/com/fpms/security/SecurityMgr.java'"
    [OK]
    [2/247] Checking in: src/com/fpms/util/Base64Util.java
    dmcli -cmd "ci -r 'ICCF12345' 'src/com/fpms/util/Base64Util.java'"
    [OK]
    ... (continues for all 247 files) ...
  [OK] Dimension checkin completed

═══════════════════════════════════════════════════════════
Migration PHASE1 BATCH1 completed successfully!
═══════════════════════════════════════════════════════════
Working directory: ..\fpms_module\PHASE1_BATCH1
Next steps:
  1. Validate Dimension checkin completed
  2. Update ICCF ICCF12345 with completion status
  3. Proceed to next batch
```

#### STEP 7: Verify Dimension

```batch
REM Check Dimension history
dmcli -cmd "lh 'src/com/fpms/util/Base64Util.java'"

REM Expected: Recent checkin with ICCF12345
```

#### STEP 8: Update ICCF

In your ICCF system:
- Status: **Completed**
- Completion Notes: "Phase 1 Batch 1 completed. 247 files migrated successfully."
- Attach: `reports\migration_report.txt`

---

### Running All Batches

After successful Batch 1, proceed through remaining batches:

```batch
cd migration_tool

REM Phase 1 Batch 2 - Date/Time APIs
run_migration.bat PHASE1 BATCH2 dryrun
REM ... review, validate, then:
run_migration.bat PHASE1 BATCH2 actualrun ICCF12346

REM Phase 1 Batch 3 - CORBA, JTA
run_migration.bat PHASE1 BATCH3 dryrun
REM ... review, validate, then:
run_migration.bat PHASE1 BATCH3 actualrun ICCF12347

REM Continue for all batches...
```

---

## Command Reference

### Basic Commands

```batch
REM Dry run (no Dimension operations)
run_migration.bat <PHASE> <BATCH> dryrun

REM Dry run with clean
run_migration.bat <PHASE> <BATCH> dryrun --clean

REM Actual run (requires ICCF)
run_migration.bat <PHASE> <BATCH> actualrun <ICCF_NUMBER>
```

### Examples

```batch
REM Test Phase 1 Batch 1
run_migration.bat PHASE1 BATCH1 dryrun

REM Re-run with clean
run_migration.bat PHASE1 BATCH1 dryrun --clean

REM Production run
run_migration.bat PHASE1 BATCH1 actualrun ICCF12345

REM Test Phase 2 Batch 1
run_migration.bat PHASE2 BATCH1 dryrun
```

---

## Troubleshooting

### Issue: "Excel file not found"

**Error:**
```
Exception: Phase 1 Excel not found: .\support_excel\phase1_all_batches_amended.xlsx
```

**Solution:**
```batch
REM Verify files exist
dir migration_tool\support_excel

REM Expected files:
REM   phase1_all_batches_amended.xlsx
REM   phase2_all_batches_amended.xlsx
REM   fpms_src_files_by_phase_batch.xlsx

REM If missing, copy them:
copy C:\path\to\excel\files\*.xlsx migration_tool\support_excel\
```

### Issue: "Working directory already exists"

**Error:**
```
[WARN] Working directory already exists: ..\fpms_module\PHASE1_BATCH1
Use --clean to remove it first
```

**Solution:**
```batch
REM Option 1: Use --clean flag
run_migration.bat PHASE1 BATCH1 dryrun --clean

REM Option 2: Manually remove
rmdir /s /q ..\fpms_module\PHASE1_BATCH1
```

### Issue: "dmcli not found" (ACTUALRUN mode)

**Error:**
```
dmcli: command not found
Exception: dmcli not found or not working. Required for ACTUALRUN mode.
```

**Solution:**
```batch
REM Check Dimension installation
where dmcli

REM If not found, add to PATH
set PATH=C:\Program Files\Dimensions\CM\dmcli;%PATH%

REM Or use full path in scripts
```

### Issue: OpenRewrite fails

**Error:**
```
Exception: OpenRewrite failed with exit code: 1
```

**Solution:**
```batch
REM 1. Check rewrite.yml syntax
cd ..\fpms_module\PHASE1_BATCH1
type rewrite.yml

REM 2. Run OpenRewrite manually
mvn org.openrewrite.maven:rewrite-maven-plugin:run ^
    -Drewrite.configLocation=rewrite.yml

REM 3. Check Excel recipes
notepad ..\migration_tool\support_excel\phase1_all_batches_amended.xlsx
```

### Issue: Compilation fails

**Error:**
```
Exception: JDK 8 compilation failed!
```

**Solution:**
```batch
REM 1. Check Java version
java -version

REM 2. Manually compile to see errors
cd ..\fpms_module\PHASE1_BATCH1
javac -d classes -sourcepath src src\com\fpms\util\File.java

REM 3. Review error and fix code or recipe
```

---

## Best Practices

### 1. Always Dry Run First
```batch
REM NEVER skip dry run testing
run_migration.bat PHASE1 BATCH1 dryrun
REM Review, validate, fix issues, then:
run_migration.bat PHASE1 BATCH1 actualrun ICCF12345
```

### 2. One Batch at a Time
```batch
REM Complete each batch fully:
REM 1. Dry run
REM 2. Review
REM 3. Fix
REM 4. Actual run
REM 5. Verify
REM 6. Next batch
```

### 3. Keep Backups
```batch
REM Backup before each batch
xcopy /e /i fpms_module fpms_module_backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%
```

### 4. Document Everything
- Keep ICCF tickets updated
- Save migration reports
- Document manual fixes
- Track issues and resolutions

### 5. Test Thoroughly
```batch
REM After each batch:
REM 1. Compile with JDK 8
scripts\compile_jdk8.bat

REM 2. Compile with JDK 21
scripts\compile_jdk21.bat

REM 3. Run unit tests (if available)
REM 4. Integration test on test environment
```

---

## Quick Reference Card

### File Locations
```
migration_tool\support_excel\
  - phase1_all_batches_amended.xlsx
  - phase2_all_batches_amended.xlsx
  - fpms_src_files_by_phase_batch.xlsx

fpms_module\PHASE1_BATCH1\     (created by tool)
  - src\                        (migrated code)
  - rewrite.yml                 (OpenRewrite recipe)
  - reports\migration_report.txt
  - scripts\compile_jdk8.bat
  - scripts\compile_jdk21.bat
  - scripts\dimension_checkout.bat
  - scripts\dimension_checkin.bat
```

### Common Commands
```batch
REM Build tool
cd migration_tool
mvn clean package

REM Dry run
run_migration.bat PHASE1 BATCH1 dryrun

REM Production run
run_migration.bat PHASE1 BATCH1 actualrun ICCF12345

REM Check Dimension
dmcli -cmd "lh 'src/com/fpms/util/File.java'"
```

### Modes Summary
| Mode | Source | Dimension | Use Case |
|------|--------|-----------|----------|
| dryrun | Local copy from fpms_module | None | Testing, validation |
| actualrun | dmcli checkout | Full checkout/checkin | Production execution |

---

## Summary

This Windows-compatible migration tool provides:
- ✅ **DRYRUN mode** - Safe testing with local file copies
- ✅ **ACTUALRUN mode** - Production Dimension CM integration
- ✅ **Batch control** - One batch at a time
- ✅ **OpenRewrite automation** - Consistent transformations
- ✅ **Dual JDK validation** - Ensures compatibility
- ✅ **ICCF tracking** - Change management integration

**Success Path:**
1. Setup → Build → Add Excel files
2. For each batch: DRYRUN → Review → Fix → ACTUALRUN
3. Validate → Update ICCF → Next batch

Good luck with your migration! 🚀
