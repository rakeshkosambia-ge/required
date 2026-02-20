# FPMS Migration Tool - Complete Step-by-Step Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Initial Setup](#initial-setup)
4. [Workflow Overview](#workflow-overview)
5. [Step-by-Step Execution](#step-by-step-execution)
6. [Troubleshooting](#troubleshooting)
7. [Advanced Usage](#advanced-usage)

---

## Overview

The FPMS Migration Tool automates the JDK 8 → JDK 21 / WebLogic 12c → WebLogic 14c migration in controlled batches using OpenRewrite recipes and Dimension CM integration.

### Key Features
- ✅ Batch-by-batch controlled migration
- ✅ Automated OpenRewrite code transformations  
- ✅ Dual JDK compilation validation (JDK 8 & 21)
- ✅ Dimension CM integration (checkout/checkin)
- ✅ Dry run mode for safe testing
- ✅ ICCF change tracking

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| JDK 11+ | 11, 17, or 21 | Build migration tool |
| Maven | 3.6+ | Build and run tool |
| JDK 8 | 1.8.x | Validate backward compatibility |
| JDK 21 | 21.x | Validate forward compatibility |
| Dimension CM | Latest | Source control operations |

### Directory Structure (Before Setup)

```
fpms_src_web_content/
├── fpms_module/              # Your existing non-Maven project
│   ├── src/                  # Java source code
│   └── ls_web/               # Web resources (JSP, etc.)
└── migration_tool/           # Will be created
```

---

## Initial Setup

### Step 1: Create Migration Tool

Run the setup script from `fpms_src_web_content` directory:

```bash
cd fpms_src_web_content
bash setup_migration_tool.sh
```

This creates:
```
migration_tool/
├── pom.xml                          # Maven configuration
├── README.md                        # Quick reference
├── support_excel/                   # Excel files directory
├── src/main/java/com/fpms/migration/
│   └── MigrationOrchestrator.java  # Main orchestrator
└── scripts/                         # Helper scripts
```

### Step 2: Add Excel Files

Copy your three Excel files to `migration_tool/support_excel/`:

```bash
cd migration_tool/support_excel/

# Copy Phase 1 recipes
cp /path/to/phase1_all_batches_amended.xlsx .

# Copy Phase 2 recipes
cp /path/to/phase2_all_batches_amended.xlsx .

# Copy file list (you need to create this)
cp /path/to/fpms_src_files_by_phase_batch.xlsx .
```

### Step 3: Create File List Excel

Create `fpms_src_files_by_phase_batch.xlsx` with these columns:

| PHASE | BATCH | FILEPATHNAME |
|-------|-------|--------------|
| PHASE1 | BATCH1 | src/com/fpms/security/SecurityManager.java |
| PHASE1 | BATCH1 | src/com/fpms/util/Base64Util.java |
| PHASE1 | BATCH2 | src/com/fpms/util/DateUtil.java |
| PHASE1 | BATCH2 | src/com/fpms/core/TimestampHelper.java |

**Important Notes:**
- Paths are relative to `fpms_module/` directory
- Use forward slashes `/` for separators
- Do NOT include `fpms_module/` prefix
- List ALL files impacted by each phase/batch

**Example File List Structure:**

```
PHASE1, BATCH1 files:
- All files using SecurityManager
- All files using sun.* APIs
- All files with underscore identifiers

PHASE1, BATCH2 files:
- All files using java.util.Date
- All files using java.sql.Timestamp
- All files using Calendar

... and so on for each batch
```

### Step 4: Build the Tool

```bash
cd migration_tool
mvn clean package
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] 
[INFO] migration-tool-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

**Verify:**
```bash
ls -lh target/migration-tool-*-jar-with-dependencies.jar
# Should show ~15MB file
```

---

## Workflow Overview

### Migration Process Flow

```
┌─────────────────────────────────────────────────────────────┐
│  1. DRY RUN (Development/Testing)                           │
│     - Test each batch individually                          │
│     - Review OpenRewrite changes                            │
│     - Validate compilation (JDK 8 & 21)                    │
│     - Fix any issues, re-run                                │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  2. CODE REVIEW                                             │
│     - Review migrated code in working directory             │
│     - Validate OpenRewrite transformations                  │
│     - Check for manual fixes needed                         │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  3. PRODUCTION RUN (with ICCF)                             │
│     - Create ICCF ticket manually                           │
│     - Run tool with --iccf flag                             │
│     - Dimension checkout → migrate → checkin                │
│     - Update ICCF with completion                           │
└─────────────────────────────────────────────────────────────┘
```

### Phase Strategy

**Phase 1: SAFE Patterns** (Dual JDK 8 & 21 compatible)
- BATCH 1: sun.* APIs, Underscore identifiers
- BATCH 2: Date/Time APIs (java.util.Date → java.time)
- BATCH 3: CORBA, JTA (externalize dependencies)
- BATCH 4: Generics, Diamond operator
- BATCH 5: Security APIs, finalize(), Reflection
- BATCH 6: JAXB (externalize dependency)

**Phase 2: BREAKING CHANGES** (Must fix before JDK 21)
- BATCH 1: SecurityManager, JSP Scriptlets
- BATCH 2: Log4j 1.x → Log4j2
- BATCH 3: HTTP Client upgrades
- BATCH 4: OC4J removal, Struts migration
- BATCH 5: Third-party library upgrades

---

## Step-by-Step Execution

### Phase 1, Batch 1 - Complete Example

#### Step 1: Dry Run

```bash
cd migration_tool

# Run dry run
java -jar target/migration-tool-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
     PHASE1 BATCH1 --dry-run
```

**Expected Output:**
```
═══════════════════════════════════════════════════════════
FPMS Migration Orchestrator
═══════════════════════════════════════════════════════════
Phase        : PHASE1
Batch        : BATCH1
Mode         : DRY RUN
Working Dir  : ../fpms_module/PHASE1_BATCH1
═══════════════════════════════════════════════════════════

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 1: Validating inputs...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✓ All inputs validated

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 2: Creating working directory...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✓ Created: ../fpms_module/PHASE1_BATCH1

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 3: Reading impacted file list...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Found 247 impacted files

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 4: Copying impacted files...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✓ Copied 247 files

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 5: Generating OpenRewrite recipes...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✓ Generated: ../fpms_module/PHASE1_BATCH1/rewrite.yml

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 6: Applying OpenRewrite recipes...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Running OpenRewrite (this may take a few minutes)...
  ✓ OpenRewrite completed successfully

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 7: Validating compilation...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Validating JDK 8 compilation...
  Validating JDK 21 compilation...
  ✓ Compilation successful on both JDK 8 and JDK 21

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 8: Generating migration report...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✓ Report generated: ../fpms_module/PHASE1_BATCH1/reports/migration_report.txt

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 9: Generating Dimension CM scripts (dry run)...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✓ Dimension scripts generated:
    Checkout: ../fpms_module/PHASE1_BATCH1/scripts/dimension_checkout.sh
    Checkin:  ../fpms_module/PHASE1_BATCH1/scripts/dimension_checkin.sh

═══════════════════════════════════════════════════════════
Migration PHASE1 BATCH1 completed successfully!
═══════════════════════════════════════════════════════════
Working directory: ../fpms_module/PHASE1_BATCH1
Next steps:
  1. Review migrated code in: ../fpms_module/PHASE1_BATCH1
  2. Test compilation with JDK 8 and JDK 21
  3. Review OpenRewrite changes
  4. Run with --iccf <number> for production checkin
```

#### Step 2: Review Migrated Code

```bash
cd ../fpms_module/PHASE1_BATCH1

# Check directory structure
ls -la
# src/              - Migrated Java files
# ls_web/           - Migrated web files (if any)
# rewrite.yml       - OpenRewrite recipe used
# reports/          - Migration report
# scripts/          - Dimension scripts
# pom.xml           - Temporary Maven POM

# Review OpenRewrite recipe
cat rewrite.yml
```

**Example rewrite.yml content:**
```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.fpms.migration.PHASE1.BATCH1
displayName: FPMS PHASE1 BATCH1 Migration
description: |
  Automated migration for PHASE1 BATCH1
  Generated by FPMS Migration Orchestrator
recipeList:
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: sun.misc.BASE64Encoder
      newFullyQualifiedTypeName: java.util.Base64.Encoder
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: sun.misc.BASE64Decoder
      newFullyQualifiedTypeName: java.util.Base64.Decoder
  - org.openrewrite.java.search.FindTypes:
      fullyQualifiedTypeName: "sun..*"
  - org.openrewrite.java.RemoveUnusedImports
```

**Review Changes:**
```bash
# Compare original vs migrated
diff ../src/com/fpms/util/Base64Util.java \
     src/com/fpms/util/Base64Util.java

# Example diff:
- import sun.misc.BASE64Encoder;
+ import java.util.Base64;

- BASE64Encoder encoder = new BASE64Encoder();
- String encoded = encoder.encode(bytes);
+ String encoded = Base64.getEncoder().encodeToString(bytes);
```

#### Step 3: Manual Compilation Test

Test compilation manually with both JDKs:

```bash
# Set JAVA_HOME to JDK 8
export JAVA_HOME=/path/to/jdk8
export PATH=$JAVA_HOME/bin:$PATH

# Compile with JDK 8
javac -d classes -sourcepath src \
      $(find src -name "*.java")

# Check for errors
echo $?  # Should be 0

# Set JAVA_HOME to JDK 21
export JAVA_HOME=/path/to/jdk21
export PATH=$JAVA_HOME/bin:$PATH

# Compile with JDK 21
javac -d classes -sourcepath src \
      $(find src -name "*.java")

# Check for errors
echo $?  # Should be 0
```

#### Step 4: Code Review

Perform detailed code review:

1. **Check all modified files**
   ```bash
   find src -type f -name "*.java" | while read file; do
     echo "Reviewing: $file"
     # Use your preferred diff tool
   done
   ```

2. **Look for common issues:**
   - Incorrect API replacements
   - Missing imports
   - Deprecated warnings
   - Compilation errors

3. **Validate OpenRewrite transformations:**
   - Were all sun.* APIs replaced correctly?
   - Are Base64 encodings correct?
   - Were any manual changes needed?

#### Step 5: Fix Issues (If Needed)

If issues found, you have two options:

**Option A: Fix recipes and re-run**
```bash
cd ../../migration_tool/support_excel

# Edit phase1_all_batches_amended.xlsx
# Update OPENREWRITE_RECIPE_YAML column for problematic patterns

# Re-run with --clean to start fresh
cd ..
java -jar target/migration-tool-*.jar \
     PHASE1 BATCH1 --dry-run --clean
```

**Option B: Manual fixes**
```bash
cd ../fpms_module/PHASE1_BATCH1/src

# Manually edit files
vim com/fpms/util/ProblematicFile.java

# Re-compile to verify
javac -d ../classes -sourcepath . \
      com/fpms/util/ProblematicFile.java
```

#### Step 6: Production Execution

Once validated, run production with ICCF:

```bash
# 1. Create ICCF ticket manually in your change management system
#    Example: ICCF12345 - "Phase 1 Batch 1: sun.* API migration"

# 2. Run production migration
cd migration_tool

java -jar target/migration-tool-*.jar \
     PHASE1 BATCH1 --iccf ICCF12345
```

**Production Output:**
```
═══════════════════════════════════════════════════════════
FPMS Migration Orchestrator
═══════════════════════════════════════════════════════════
Phase        : PHASE1
Batch        : BATCH1
Mode         : PRODUCTION
ICCF Number  : ICCF12345
Working Dir  : ../fpms_module/PHASE1_BATCH1
═══════════════════════════════════════════════════════════

... (same steps 1-8 as dry run) ...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 9: Executing Dimension CM operations...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Checking out files from Dimension...
  Checking in files to Dimension (ICCF: ICCF12345)...
  ✓ Dimension operations completed

═══════════════════════════════════════════════════════════
Migration PHASE1 BATCH1 completed successfully!
═══════════════════════════════════════════════════════════
Working directory: ../fpms_module/PHASE1_BATCH1
Next steps:
  1. Validate Dimension checkin completed
  2. Update ICCF ICCF12345 with completion status
  3. Proceed to next batch
```

#### Step 7: Verify Dimension Checkin

```bash
# Check Dimension history
dmcli -sc "lh 'src/com/fpms/util/Base64Util.java'"

# Expected output shows recent checkin with ICCF12345
```

#### Step 8: Update ICCF

In your ICCF system:
- Status: Completed
- Notes: "Phase 1 Batch 1 migration completed successfully. 247 files migrated."
- Attachments: migration_report.txt

---

### Running All Phase 1 Batches

After successful BATCH1, proceed through all batches:

```bash
cd migration_tool

# Batch 2: Date/Time APIs
java -jar target/migration-tool-*.jar PHASE1 BATCH2 --dry-run
# ... review, validate, then production

# Batch 3: CORBA, JTA
java -jar target/migration-tool-*.jar PHASE1 BATCH3 --dry-run
# ... review, validate, then production

# Batch 4: Generics
java -jar target/migration-tool-*.jar PHASE1 BATCH4 --dry-run
# ... review, validate, then production

# Batch 5: Security APIs
java -jar target/migration-tool-*.jar PHASE1 BATCH5 --dry-run
# ... review, validate, then production

# Batch 6: JAXB
java -jar target/migration-tool-*.jar PHASE1 BATCH6 --dry-run
# ... review, validate, then production
```

### Running Phase 2 Batches

After ALL Phase 1 batches complete:

```bash
# Phase 2 Batch 1: SecurityManager, Scriptlets
java -jar target/migration-tool-*.jar PHASE2 BATCH1 --dry-run

# Phase 2 Batch 2: Log4j migration
java -jar target/migration-tool-*.jar PHASE2 BATCH2 --dry-run

# ... continue through all Phase 2 batches
```

---

## Troubleshooting

### Issue: Excel File Not Found

**Error:**
```
Exception: Phase 1 Excel not found: ./support_excel/phase1_all_batches_amended.xlsx
```

**Solution:**
```bash
# Verify files exist
ls -la migration_tool/support_excel/

# Expected:
# phase1_all_batches_amended.xlsx
# phase2_all_batches_amended.xlsx
# fpms_src_files_by_phase_batch.xlsx
```

### Issue: Working Directory Already Exists

**Error:**
```
⚠ Working directory already exists: ../fpms_module/PHASE1_BATCH1
```

**Solution:**
```bash
# Option 1: Run with --clean flag
java -jar migration-tool-*.jar PHASE1 BATCH1 --dry-run --clean

# Option 2: Manually remove
rm -rf ../fpms_module/PHASE1_BATCH1
```

### Issue: OpenRewrite Fails

**Error:**
```
Exception: OpenRewrite failed with exit code: 1
```

**Solutions:**

1. **Check rewrite.yml syntax:**
   ```bash
   cd ../fpms_module/PHASE1_BATCH1
   cat rewrite.yml
   # Look for YAML syntax errors
   ```

2. **Validate recipe format:**
   - Check Excel OPENREWRITE_RECIPE_YAML column
   - Ensure YAML is properly formatted
   - Check for special characters

3. **Run OpenRewrite manually:**
   ```bash
   mvn org.openrewrite.maven:rewrite-maven-plugin:run \
       -Drewrite.configLocation=rewrite.yml
   ```

### Issue: Compilation Fails

**Error:**
```
Exception: JDK 8 compilation failed!
```

**Solutions:**

1. **Check Java files:**
   ```bash
   cd ../fpms_module/PHASE1_BATCH1/src
   find . -name "*.java" -exec javac {} \;
   ```

2. **Review compilation errors:**
   - Missing imports?
   - API usage incorrect?
   - Classpath issues?

3. **Manual compilation test:**
   ```bash
   javac -classpath "path/to/libs/*" \
         -d classes \
         -sourcepath src \
         src/com/fpms/problem/File.java
   ```

### Issue: Dimension Commands Fail

**Error:**
```
dmcli: command not found
```

**Solutions:**

1. **Check Dimension installation:**
   ```bash
   which dmcli
   echo $DM_ROOT
   ```

2. **Source Dimension environment:**
   ```bash
   source /opt/dimension/dmenv.sh
   ```

3. **Manual Dimension operations:**
   ```bash
   cd ../fpms_module/PHASE1_BATCH1/scripts
   
   # Review generated scripts
   cat dimension_checkout.sh
   cat dimension_checkin.sh
   
   # Execute manually
   ./dimension_checkout.sh
   ./dimension_checkin.sh ICCF12345
   ```

---

## Advanced Usage

### Custom File Selection

Instead of Excel, provide file list via command line:

```bash
# Create custom file list
cat > /tmp/custom_files.txt << EOF
src/com/fpms/util/File1.java
src/com/fpms/util/File2.java
ls_web/jsp/page1.jsp
EOF

# Run with custom list (future enhancement)
# java -jar migration-tool-*.jar PHASE1 BATCH1 \
#      --file-list /tmp/custom_files.txt --dry-run
```

### Parallel Batch Execution

Run multiple batches in parallel (with caution):

```bash
# Terminal 1
java -jar migration-tool-*.jar PHASE1 BATCH1 --dry-run &

# Terminal 2  
java -jar migration-tool-*.jar PHASE1 BATCH2 --dry-run &

# Terminal 3
java -jar migration-tool-*.jar PHASE1 BATCH3 --dry-run &

# Wait for all
wait
```

### Custom OpenRewrite Recipes

Add custom recipes to `support_excel/custom_recipes.yml`:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.fpms.custom.MyCustomRecipe
displayName: Custom FPMS Transformation
recipeList:
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: com.legacy.OldClass
      newFullyQualifiedTypeName: com.modern.NewClass
```

Include in Excel OPENREWRITE_RECIPE_YAML column.

### Integration with CI/CD

Add to Jenkins/GitLab CI:

```groovy
// Jenkinsfile
stage('Migration Dry Run') {
    steps {
        sh '''
            cd migration_tool
            java -jar target/migration-tool-*.jar \
                 PHASE1 BATCH1 --dry-run
        '''
    }
}

stage('Validate Compilation') {
    steps {
        sh '''
            cd ../fpms_module/PHASE1_BATCH1
            ./scripts/compile_jdk8.sh
            ./scripts/compile_jdk21.sh
        '''
    }
}
```

---

## Best Practices

### 1. Always Dry Run First
Never skip dry run testing. Always validate before production.

### 2. One Batch at a Time
Complete each batch fully before moving to next:
- Dry run → Review → Fix → Production → Validate

### 3. Keep Backups
Dimension maintains history, but also:
```bash
# Backup before each batch
tar -czf fpms_backup_$(date +%Y%m%d).tar.gz fpms_module/
```

### 4. Document Everything
- ICCF tickets with detailed notes
- Code review comments
- Known issues and workarounds

### 5. Test Thoroughly
- Compile with both JDKs
- Run unit tests
- Integration testing on test environment

### 6. Version Control
- Commit working directory after successful batch
- Tag releases: `git tag -a v1.0-phase1-batch1`

---

## Summary

This migration tool provides a **controlled, automated, and safe** approach to migrating your monolithic FPMS application from JDK 8 to JDK 21. 

**Key Success Factors:**
1. ✅ Batch-by-batch approach minimizes risk
2. ✅ Dry run mode enables safe testing
3. ✅ OpenRewrite automates repetitive transformations
4. ✅ Dual JDK validation ensures compatibility
5. ✅ Dimension integration maintains version control

**Migration Timeline Estimate:**
- Phase 1 (6 batches): 2-3 weeks
- Phase 2 (5 batches): 2-3 weeks
- Total: 4-6 weeks with proper testing

Good luck with your migration! 🚀
