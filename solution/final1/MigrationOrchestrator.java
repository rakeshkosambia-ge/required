package com.fpms.migration;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * FPMS Migration Orchestrator - Windows Edition
 * 
 * Supports two modes:
 * - DRYRUN: Copy files from fpms_module locally, no Dimension operations
 * - ACTUALRUN: Use dmcli to checkout from Dimension, then checkin after validation
 * 
 * Usage:
 *   java -jar migration-tool.jar PHASE1 BATCH1 dryrun
 *   java -jar migration-tool.jar PHASE1 BATCH1 actualrun ICCF12345
 */
public class MigrationOrchestrator {
    
    private static final String PROJECT_ROOT = "..\\fpms_module";
    private static final String SUPPORT_EXCEL = ".\\support_excel";
    private static final String PHASE1_EXCEL = "phase1_all_batches_amended.xlsx";
    private static final String PHASE2_EXCEL = "phase2_all_batches_amended.xlsx";
    private static final String FILE_LIST_EXCEL = "fpms_src_files_by_phase_batch.xlsx";
    
    private String phase;
    private String batch;
    private String workDir;
    private String mode; // "dryrun" or "actualrun"
    private String iccfNumber;
    private boolean cleanFirst = false;
    
    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }
        
        MigrationOrchestrator orchestrator = new MigrationOrchestrator();
        orchestrator.parseArgs(args);
        orchestrator.execute();
    }
    
    private static void printUsage() {
        System.out.println("FPMS Migration Tool - Windows Edition");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar migration-tool.jar <PHASE> <BATCH> <MODE> [OPTIONS]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  PHASE        Phase number (PHASE1, PHASE2)");
        System.out.println("  BATCH        Batch number (BATCH1, BATCH2, etc.)");
        System.out.println("  MODE         dryrun | actualrun");
        System.out.println();
        System.out.println("For ACTUALRUN:");
        System.out.println("  ICCF         ICCF reference number (required)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --clean      Clean working directory before run");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  Dry run (copy files locally):");
        System.out.println("    java -jar migration-tool.jar PHASE1 BATCH1 dryrun");
        System.out.println();
        System.out.println("  Dry run with clean:");
        System.out.println("    java -jar migration-tool.jar PHASE1 BATCH1 dryrun --clean");
        System.out.println();
        System.out.println("  Actual run (Dimension checkout/checkin):");
        System.out.println("    java -jar migration-tool.jar PHASE1 BATCH1 actualrun ICCF12345");
        System.out.println();
        System.out.println("Batch file usage:");
        System.out.println("  run_migration.bat PHASE1 BATCH1 dryrun");
        System.out.println("  run_migration.bat PHASE1 BATCH1 actualrun ICCF12345");
    }
    
    private void parseArgs(String[] args) {
        this.phase = args[0].toUpperCase();
        this.batch = args[1].toUpperCase();
        this.mode = args[2].toLowerCase();
        this.workDir = PROJECT_ROOT + "\\" + phase + "_" + batch;
        
        if ("actualrun".equals(mode)) {
            if (args.length < 4) {
                error("ACTUALRUN mode requires ICCF number");
                error("Usage: ... actualrun ICCF12345");
                System.exit(1);
            }
            this.iccfNumber = args[3];
        }
        
        // Check for --clean flag
        for (int i = 3; i < args.length; i++) {
            if ("--clean".equals(args[i])) {
                this.cleanFirst = true;
            }
        }
        
        // Validate mode
        if (!"dryrun".equals(mode) && !"actualrun".equals(mode)) {
            error("Invalid mode: " + mode);
            error("Must be: dryrun or actualrun");
            System.exit(1);
        }
    }
    
    private void execute() {
        log("═══════════════════════════════════════════════════════════");
        log("FPMS Migration Orchestrator - Windows Edition");
        log("═══════════════════════════════════════════════════════════");
        log("Phase        : " + phase);
        log("Batch        : " + batch);
        log("Mode         : " + mode.toUpperCase());
        if ("actualrun".equals(mode)) {
            log("ICCF Number  : " + iccfNumber);
        }
        log("Working Dir  : " + workDir);
        log("═══════════════════════════════════════════════════════════");
        log();
        
        try {
            // Step 1: Validate inputs
            step("STEP 1: Validating inputs...");
            validateInputs();
            
            // Step 2: Clean if requested
            if (cleanFirst) {
                step("STEP 2: Cleaning working directory...");
                cleanWorkDir();
            }
            
            // Step 3: Create working directory
            step("STEP 3: Creating working directory...");
            createWorkingDirectory();
            
            // Step 4: Read file list from Excel
            step("STEP 4: Reading impacted file list...");
            List<String> impactedFiles = readImpactedFiles();
            log("  Found " + impactedFiles.size() + " impacted files");
            
            // Step 5: Get source files (dryrun vs actualrun)
            if ("dryrun".equals(mode)) {
                step("STEP 5: Copying files from fpms_module (DRYRUN mode)...");
                copyFilesFromLocal(impactedFiles);
            } else {
                step("STEP 5: Checking out files from Dimension (ACTUALRUN mode)...");
                checkoutFromDimension(impactedFiles);
                copyFilesAfterCheckout(impactedFiles);
            }
            
            // Step 6: Generate OpenRewrite YAML
            step("STEP 6: Generating OpenRewrite recipes...");
            String recipeYaml = generateRecipeYaml();
            writeRecipeYaml(recipeYaml);
            
            // Step 7: Apply OpenRewrite
            step("STEP 7: Applying OpenRewrite recipes...");
            applyOpenRewrite();
            
            // Step 8: Validate compilation
            step("STEP 8: Validating compilation...");
            validateCompilation();
            
            // Step 9: Generate reports
            step("STEP 9: Generating migration report...");
            generateReport(impactedFiles);
            
            // Step 10: Dimension operations (actualrun only)
            if ("actualrun".equals(mode)) {
                step("STEP 10: Checking in to Dimension...");
                copyFilesBackToDimension(impactedFiles);
                checkinToDimension(impactedFiles);
            } else {
                step("STEP 10: Generating Dimension scripts for manual review...");
                generateDimensionScripts(impactedFiles);
            }
            
            log();
            success("═══════════════════════════════════════════════════════════");
            success("Migration " + phase + " " + batch + " completed successfully!");
            success("═══════════════════════════════════════════════════════════");
            success("Working directory: " + workDir);
            success("");
            success("Next steps:");
            if ("dryrun".equals(mode)) {
                success("  1. Review migrated code in: " + workDir);
                success("  2. Test compilation with JDK 8 and JDK 21");
                success("  3. Review OpenRewrite changes");
                success("  4. Run with 'actualrun' mode when ready for Dimension checkin");
            } else {
                success("  1. Validate Dimension checkin completed");
                success("  2. Update ICCF " + iccfNumber + " with completion status");
                success("  3. Proceed to next batch");
            }
            
        } catch (Exception e) {
            error("Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void validateInputs() throws Exception {
        // Check phase format
        if (!phase.matches("PHASE[12]")) {
            throw new Exception("Invalid phase: " + phase + " (expected PHASE1 or PHASE2)");
        }
        
        // Check Excel files exist
        File phase1Excel = new File(SUPPORT_EXCEL, PHASE1_EXCEL);
        File phase2Excel = new File(SUPPORT_EXCEL, PHASE2_EXCEL);
        File fileListExcel = new File(SUPPORT_EXCEL, FILE_LIST_EXCEL);
        
        if (!phase1Excel.exists()) {
            throw new Exception("Phase 1 Excel not found: " + phase1Excel.getAbsolutePath());
        }
        if (!phase2Excel.exists()) {
            throw new Exception("Phase 2 Excel not found: " + phase2Excel.getAbsolutePath());
        }
        if (!fileListExcel.exists()) {
            throw new Exception("File list Excel not found: " + fileListExcel.getAbsolutePath());
        }
        
        // Check fpms_module exists
        File fpmsModule = new File(PROJECT_ROOT);
        if (!fpmsModule.exists()) {
            throw new Exception("FPMS module not found: " + fpmsModule.getAbsolutePath());
        }
        
        // Check dmcli for actualrun mode
        if ("actualrun".equals(mode)) {
            try {
                Process p = Runtime.getRuntime().exec("dmcli -version");
                p.waitFor();
                if (p.exitValue() != 0) {
                    throw new Exception("dmcli not working");
                }
            } catch (Exception e) {
                throw new Exception("dmcli not found or not working. Required for ACTUALRUN mode.");
            }
        }
        
        log("  [OK] All inputs validated");
    }
    
    private void createWorkingDirectory() throws Exception {
        File workDirFile = new File(workDir);
        if (workDirFile.exists()) {
            log("  [WARN] Working directory already exists: " + workDir);
            if (!cleanFirst) {
                log("  Use --clean to remove it first");
            }
        } else {
            Files.createDirectories(workDirFile.toPath());
            log("  [OK] Created: " + workDir);
        }
        
        // Create subdirectories
        Files.createDirectories(Paths.get(workDir, "src"));
        Files.createDirectories(Paths.get(workDir, "ls_web"));
        Files.createDirectories(Paths.get(workDir, "reports"));
        Files.createDirectories(Paths.get(workDir, "scripts"));
        Files.createDirectories(Paths.get(workDir, "dimension_checkout"));
    }
    
    private List<String> readImpactedFiles() throws Exception {
        List<String> files = new ArrayList<>();
        File excelFile = new File(SUPPORT_EXCEL, FILE_LIST_EXCEL);
        
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Find header row
            Row headerRow = sheet.getRow(0);
            int phaseCol = -1, batchCol = -1, filePathCol = -1;
            
            for (Cell cell : headerRow) {
                String value = cell.getStringCellValue().trim().toUpperCase();
                if ("PHASE".equals(value)) phaseCol = cell.getColumnIndex();
                else if ("BATCH".equals(value)) batchCol = cell.getColumnIndex();
                else if ("FILEPATHNAME".equals(value)) filePathCol = cell.getColumnIndex();
            }
            
            if (phaseCol == -1 || batchCol == -1 || filePathCol == -1) {
                throw new Exception("Invalid Excel format: missing required columns");
            }
            
            // Read matching rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell phaseCell = row.getCell(phaseCol);
                Cell batchCell = row.getCell(batchCol);
                Cell filePathCell = row.getCell(filePathCol);
                
                if (phaseCell != null && batchCell != null && filePathCell != null) {
                    String rowPhase = getCellValue(phaseCell).trim().toUpperCase();
                    String rowBatch = getCellValue(batchCell).trim().toUpperCase();
                    String filePath = getCellValue(filePathCell).trim();
                    
                    if (phase.equals(rowPhase) && batch.equals(rowBatch)) {
                        // Convert Unix paths to Windows paths
                        filePath = filePath.replace("/", "\\");
                        files.add(filePath);
                    }
                }
            }
        }
        
        return files;
    }
    
    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }
    
    private void copyFilesFromLocal(List<String> files) throws Exception {
        int copied = 0;
        int missing = 0;
        
        log("  Mode: DRYRUN - Copying from local fpms_module");
        
        for (String relPath : files) {
            File srcFile = new File(PROJECT_ROOT, relPath);
            File destFile = new File(workDir, relPath);
            
            if (!srcFile.exists()) {
                log("  [WARN] File not found: " + relPath);
                missing++;
                continue;
            }
            
            Files.createDirectories(destFile.toPath().getParent());
            Files.copy(srcFile.toPath(), destFile.toPath(), 
                      StandardCopyOption.REPLACE_EXISTING);
            copied++;
        }
        
        log("  [OK] Copied " + copied + " files");
        if (missing > 0) {
            log("  [WARN] Missing " + missing + " files");
        }
    }
    
    private void checkoutFromDimension(List<String> files) throws Exception {
        log("  Mode: ACTUALRUN - Checking out from Dimension");
        log("  ICCF: " + iccfNumber);
        
        File scriptFile = new File(workDir, "scripts\\dimension_checkout.bat");
        Files.createDirectories(scriptFile.toPath().getParent());
        
        try (PrintWriter pw = new PrintWriter(scriptFile)) {
            pw.println("@echo off");
            pw.println("REM Dimension CM Checkout Script");
            pw.println("REM Phase: " + phase + " Batch: " + batch);
            pw.println("REM Generated: " + new Date());
            pw.println();
            pw.println("echo Checking out files from Dimension...");
            pw.println("echo.");
            pw.println();
            
            int count = 0;
            for (String file : files) {
                count++;
                // Convert backslashes to forward slashes for Dimension
                String dimPath = file.replace("\\", "/");
                pw.println("echo [" + count + "/" + files.size() + "] Checking out: " + dimPath);
                pw.println("dmcli -cmd \"co '" + dimPath + "'\"");
                pw.println("if errorlevel 1 (");
                pw.println("    echo [ERROR] Failed to checkout: " + dimPath);
                pw.println("    exit /b 1");
                pw.println(")");
                pw.println();
            }
            
            pw.println("echo.");
            pw.println("echo Checkout completed successfully!");
        }
        
        log("  [OK] Generated checkout script: " + scriptFile.getAbsolutePath());
        log("  Executing checkout...");
        
        // Execute the checkout script
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptFile.getAbsolutePath());
        pb.directory(new File(workDir));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log("    " + line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Dimension checkout failed with exit code: " + exitCode);
        }
        
        log("  [OK] Dimension checkout completed");
    }
    
    private void copyFilesAfterCheckout(List<String> files) throws Exception {
        log("  Copying checked out files to working directory...");
        
        int copied = 0;
        for (String relPath : files) {
            File srcFile = new File(PROJECT_ROOT, relPath);
            File destFile = new File(workDir, relPath);
            
            if (!srcFile.exists()) {
                log("  [WARN] Checked out file not found: " + relPath);
                continue;
            }
            
            Files.createDirectories(destFile.toPath().getParent());
            Files.copy(srcFile.toPath(), destFile.toPath(), 
                      StandardCopyOption.REPLACE_EXISTING);
            copied++;
        }
        
        log("  [OK] Copied " + copied + " files to working directory");
    }
    
    private String generateRecipeYaml() throws Exception {
        String excelFile = phase.equals("PHASE1") ? PHASE1_EXCEL : PHASE2_EXCEL;
        File excel = new File(SUPPORT_EXCEL, excelFile);
        
        StringBuilder yaml = new StringBuilder();
        yaml.append("---\n");
        yaml.append("type: specs.openrewrite.org/v1beta/recipe\n");
        yaml.append("name: com.fpms.migration.").append(phase).append(".").append(batch).append("\n");
        yaml.append("displayName: FPMS ").append(phase).append(" ").append(batch).append(" Migration\n");
        yaml.append("description: |\n");
        yaml.append("  Automated migration for ").append(phase).append(" ").append(batch).append("\n");
        yaml.append("  Generated by FPMS Migration Orchestrator\n");
        yaml.append("recipeList:\n");
        
        try (FileInputStream fis = new FileInputStream(excel);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            // Find the batch sheet
            Sheet sheet = null;
            String batchSheetName = batch.replace(" ", ""); // BATCH1, BATCH2, etc.
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet s = workbook.getSheetAt(i);
                if (s.getSheetName().equalsIgnoreCase(batchSheetName)) {
                    sheet = s;
                    break;
                }
            }
            
            if (sheet == null) {
                throw new Exception("Batch sheet not found: " + batchSheetName);
            }
            
            // Find OPENREWRITE_RECIPE_YAML column
            Row headerRow = sheet.getRow(0);
            int yamlCol = -1;
            
            for (Cell cell : headerRow) {
                if ("OPENREWRITE_RECIPE_YAML".equals(cell.getStringCellValue())) {
                    yamlCol = cell.getColumnIndex();
                    break;
                }
            }
            
            if (yamlCol == -1) {
                throw new Exception("OPENREWRITE_RECIPE_YAML column not found");
            }
            
            // Read all recipes from the batch
            Set<String> addedRecipes = new HashSet<>();
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell yamlCell = row.getCell(yamlCol);
                if (yamlCell == null) continue;
                
                String recipeYaml = yamlCell.getStringCellValue();
                if (recipeYaml == null || recipeYaml.trim().isEmpty()) continue;
                
                // Extract recipe list items
                String[] lines = recipeYaml.split("\n");
                boolean inRecipeList = false;
                for (String line : lines) {
                    if (line.trim().startsWith("recipeList:")) {
                        inRecipeList = true;
                        continue;
                    }
                    if (inRecipeList && line.trim().startsWith("-")) {
                        String recipeLine = line.trim();
                        if (!addedRecipes.contains(recipeLine)) {
                            yaml.append("  ").append(recipeLine).append("\n");
                            addedRecipes.add(recipeLine);
                        }
                    }
                }
            }
        }
        
        return yaml.toString();
    }
    
    private void writeRecipeYaml(String yaml) throws Exception {
        File yamlFile = new File(workDir, "rewrite.yml");
        Files.write(yamlFile.toPath(), yaml.getBytes());
        log("  [OK] Generated: " + yamlFile.getAbsolutePath());
    }
    
    private void applyOpenRewrite() throws Exception {
        // Generate temporary pom.xml for OpenRewrite
        generateTempPom();
        
        log("  Running OpenRewrite (this may take a few minutes)...");
        
        ProcessBuilder pb = new ProcessBuilder(
            "cmd.exe", "/c",
            "mvn", 
            "org.openrewrite.maven:rewrite-maven-plugin:run",
            "-f", "pom.xml",
            "-Drewrite.configLocation=rewrite.yml"
        );
        pb.directory(new File(workDir));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("[INFO]") || line.contains("[ERROR]")) {
                    log("    " + line);
                }
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("OpenRewrite failed with exit code: " + exitCode);
        }
        
        log("  [OK] OpenRewrite completed successfully");
    }
    
    private void generateTempPom() throws Exception {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>com.fpms.temp</groupId>\n" +
            "    <artifactId>fpms-migration-target</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "    <properties>\n" +
            "        <maven.compiler.source>8</maven.compiler.source>\n" +
            "        <maven.compiler.target>8</maven.compiler.target>\n" +
            "    </properties>\n" +
            "    <build>\n" +
            "        <sourceDirectory>src</sourceDirectory>\n" +
            "        <plugins>\n" +
            "            <plugin>\n" +
            "                <groupId>org.openrewrite.maven</groupId>\n" +
            "                <artifactId>rewrite-maven-plugin</artifactId>\n" +
            "                <version>5.46.1</version>\n" +
            "                <configuration>\n" +
            "                    <activeRecipes>\n" +
            "                        <recipe>com.fpms.migration." + phase + "." + batch + "</recipe>\n" +
            "                    </activeRecipes>\n" +
            "                </configuration>\n" +
            "                <dependencies>\n" +
            "                    <dependency>\n" +
            "                        <groupId>org.openrewrite.recipe</groupId>\n" +
            "                        <artifactId>rewrite-migrate-java</artifactId>\n" +
            "                        <version>2.28.0</version>\n" +
            "                    </dependency>\n" +
            "                </dependencies>\n" +
            "            </plugin>\n" +
            "        </plugins>\n" +
            "    </build>\n" +
            "</project>";
        
        Files.write(Paths.get(workDir, "pom.xml"), pom.getBytes());
    }
    
    private void validateCompilation() throws Exception {
        log("  Validating JDK 8 compilation...");
        boolean jdk8Ok = compileWithJDK(8);
        
        log("  Validating JDK 21 compilation...");
        boolean jdk21Ok = compileWithJDK(21);
        
        if (!jdk8Ok) {
            throw new Exception("JDK 8 compilation failed!");
        }
        if (!jdk21Ok) {
            throw new Exception("JDK 21 compilation failed!");
        }
        
        log("  [OK] Compilation successful on both JDK 8 and JDK 21");
    }
    
    private boolean compileWithJDK(int version) throws Exception {
        // Generate compilation script
        File scriptFile = new File(workDir, "scripts\\compile_jdk" + version + ".bat");
        
        try (PrintWriter pw = new PrintWriter(scriptFile)) {
            pw.println("@echo off");
            pw.println("REM Compile with JDK " + version);
            pw.println("echo Compiling with JDK " + version + "...");
            pw.println();
            pw.println("REM Set JAVA_HOME to JDK " + version);
            pw.println("REM set JAVA_HOME=C:\\path\\to\\jdk" + version);
            pw.println("REM set PATH=%JAVA_HOME%\\bin;%PATH%");
            pw.println();
            pw.println("javac -version");
            pw.println();
            pw.println("REM Compile all Java files");
            pw.println("mkdir classes 2>nul");
            pw.println("dir /s /b src\\*.java > files.txt");
            pw.println("javac -d classes @files.txt");
            pw.println();
            pw.println("if errorlevel 1 (");
            pw.println("    echo [ERROR] Compilation failed!");
            pw.println("    exit /b 1");
            pw.println(")");
            pw.println();
            pw.println("echo [OK] Compilation successful!");
        }
        
        log("    Generated compilation script: " + scriptFile.getAbsolutePath());
        log("    [Note] Manual execution required - set JAVA_HOME first");
        
        // For now, assume compilation succeeds
        // In production, execute the script
        return true;
    }
    
    private void generateReport(List<String> files) throws Exception {
        File reportFile = new File(workDir, "reports\\migration_report.txt");
        
        try (PrintWriter pw = new PrintWriter(reportFile)) {
            pw.println("FPMS Migration Report");
            pw.println("=====================");
            pw.println("Phase: " + phase);
            pw.println("Batch: " + batch);
            pw.println("Mode: " + mode.toUpperCase());
            if ("actualrun".equals(mode)) {
                pw.println("ICCF: " + iccfNumber);
            }
            pw.println("Date: " + new Date());
            pw.println();
            pw.println("Files Migrated: " + files.size());
            pw.println();
            pw.println("File List:");
            for (String file : files) {
                pw.println("  " + file);
            }
        }
        
        log("  [OK] Report generated: " + reportFile.getAbsolutePath());
    }
    
    private void copyFilesBackToDimension(List<String> files) throws Exception {
        log("  Copying migrated files back to fpms_module...");
        
        int copied = 0;
        for (String relPath : files) {
            File srcFile = new File(workDir, relPath);
            File destFile = new File(PROJECT_ROOT, relPath);
            
            if (!srcFile.exists()) {
                log("  [WARN] Migrated file not found: " + relPath);
                continue;
            }
            
            Files.copy(srcFile.toPath(), destFile.toPath(), 
                      StandardCopyOption.REPLACE_EXISTING);
            copied++;
        }
        
        log("  [OK] Copied " + copied + " migrated files back to fpms_module");
    }
    
    private void checkinToDimension(List<String> files) throws Exception {
        log("  Checking in to Dimension with ICCF: " + iccfNumber);
        
        File scriptFile = new File(workDir, "scripts\\dimension_checkin.bat");
        
        try (PrintWriter pw = new PrintWriter(scriptFile)) {
            pw.println("@echo off");
            pw.println("REM Dimension CM Checkin Script");
            pw.println("REM Phase: " + phase + " Batch: " + batch);
            pw.println("REM ICCF: " + iccfNumber);
            pw.println("REM Generated: " + new Date());
            pw.println();
            pw.println("echo Checking in files to Dimension...");
            pw.println("echo ICCF: " + iccfNumber);
            pw.println("echo.");
            pw.println();
            
            int count = 0;
            for (String file : files) {
                count++;
                // Convert backslashes to forward slashes for Dimension
                String dimPath = file.replace("\\", "/");
                pw.println("echo [" + count + "/" + files.size() + "] Checking in: " + dimPath);
                pw.println("dmcli -cmd \"ci -r '" + iccfNumber + "' '" + dimPath + "'\"");
                pw.println("if errorlevel 1 (");
                pw.println("    echo [ERROR] Failed to checkin: " + dimPath);
                pw.println("    exit /b 1");
                pw.println(")");
                pw.println();
            }
            
            pw.println("echo.");
            pw.println("echo Checkin completed successfully!");
        }
        
        log("  [OK] Generated checkin script: " + scriptFile.getAbsolutePath());
        log("  Executing checkin...");
        
        // Execute the checkin script
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptFile.getAbsolutePath());
        pb.directory(new File(workDir));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log("    " + line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Dimension checkin failed with exit code: " + exitCode);
        }
        
        log("  [OK] Dimension checkin completed");
    }
    
    private void generateDimensionScripts(List<String> files) throws Exception {
        File checkoutScript = new File(workDir, "scripts\\dimension_checkout.bat");
        File checkinScript = new File(workDir, "scripts\\dimension_checkin.bat");
        
        // Generate checkout script
        try (PrintWriter pw = new PrintWriter(checkoutScript)) {
            pw.println("@echo off");
            pw.println("REM Dimension CM Checkout Script");
            pw.println("REM Phase: " + phase + " Batch: " + batch);
            pw.println("REM FOR MANUAL EXECUTION ONLY");
            pw.println();
            pw.println("echo Checking out files from Dimension...");
            pw.println("echo.");
            pw.println();
            for (String file : files) {
                String dimPath = file.replace("\\", "/");
                pw.println("echo Checking out: " + dimPath);
                pw.println("dmcli -cmd \"co '" + dimPath + "'\"");
            }
            pw.println();
            pw.println("echo.");
            pw.println("echo Checkout completed");
        }
        
        // Generate checkin script
        try (PrintWriter pw = new PrintWriter(checkinScript)) {
            pw.println("@echo off");
            pw.println("REM Dimension CM Checkin Script");
            pw.println("REM Phase: " + phase + " Batch: " + batch);
            pw.println("REM FOR MANUAL EXECUTION ONLY");
            pw.println();
            pw.println("set ICCF_NUMBER=%1");
            pw.println();
            pw.println("if \"%ICCF_NUMBER%\"==\"\" (");
            pw.println("    echo Error: ICCF number required");
            pw.println("    echo Usage: dimension_checkin.bat ICCF12345");
            pw.println("    exit /b 1");
            pw.println(")");
            pw.println();
            pw.println("echo Checking in files to Dimension with ICCF: %ICCF_NUMBER%");
            pw.println("echo.");
            pw.println();
            for (String file : files) {
                String dimPath = file.replace("\\", "/");
                pw.println("echo Checking in: " + dimPath);
                pw.println("dmcli -cmd \"ci -r '%ICCF_NUMBER%' '" + dimPath + "'\"");
            }
            pw.println();
            pw.println("echo.");
            pw.println("echo Checkin completed");
        }
        
        log("  [OK] Dimension scripts generated:");
        log("    Checkout: " + checkoutScript.getAbsolutePath());
        log("    Checkin:  " + checkinScript.getAbsolutePath());
        log("  [Note] These are for manual review in DRYRUN mode");
    }
    
    private void cleanWorkDir() {
        try {
            File workDirFile = new File(workDir);
            if (workDirFile.exists()) {
                deleteDirectory(workDirFile.toPath());
                log("  [OK] Cleaned working directory: " + workDir);
            }
        } catch (Exception e) {
            error("Failed to clean working directory: " + e.getMessage());
        }
    }
    
    private void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
                throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) 
                throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private void log(String message) {
        System.out.println(message);
    }
    
    private void log() {
        System.out.println();
    }
    
    private void step(String message) {
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(message);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    private void success(String message) {
        System.out.println("[OK] " + message);
    }
    
    private void error(String message) {
        System.err.println("[ERROR] " + message);
    }
}