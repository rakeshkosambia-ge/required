// javac -encoding UTF-8 -Xlint:deprecation -Xlint:unchecked JdkMigrationScanner.java
// java JdkMigrationScanner <patterns.csv> <javaSrcDir> [jspSrcDir] [applicationName] [--exts=.java,.jsp,.xml,...] [--debug]
// Writes output.csv with columns:
// ApplicationName,PatternID,PatternName,PatternSet,PatternNature,PatternDescription,LineNo,FileType,FilePathName,FoundContent,Fix,RemediationDescription,RemediationEffort,Mandays,JDK8Compatible,JDK21Compatible,CompatibilityStatus,PlanFirstChange,Remediation,ScriptableBySearchReplace
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;
import java.util.stream.Stream;

public class JdkMigrationScanner {
  private static final String OUT_CSV = "output.csv";
  private static final int REGEX_FLAGS = Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE; // added CI
  private static final int SNIPPET_MAX_LEN = 300;

  private static final String[] DEFAULT_EXTS = new String[]{
    ".java", ".jsp", ".jspf", ".tag", ".tagx", ".xsl", ".xslt",
    ".xml", ".properties", ".yml", ".yaml", ".bat", ".cmd", ".sh",
    ".gradle", ".kts", ".pom", ".ivy", ".conf", ".cfg"
  };

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.out.println("Usage: java JdkMigrationScanner <patterns.csv> <javaSrcDir> [jspSrcDir] [applicationName] [--exts=.java,.jsp,...] [--debug]");
      return;
    }

    final String csvFile = args[0];
    final String javaSrcDir = args[1];
    String jspSrcDir = null;
    String applicationName = "UnknownApp";

    int argi = 2;
    if (args.length >= 3) {
      if (argi < args.length && !isFlag(args[argi])) {
        jspSrcDir = notEmpty(args[argi]);
        argi++;
      }
      if (argi < args.length && !isFlag(args[argi])) {
        applicationName = notEmpty(args[argi], applicationName);
        argi++;
      }
    }

    // Flags
    boolean debug = false;
    Set<String> exts = new LinkedHashSet<>();
    while (argi < args.length) {
      String a = args[argi++];
      if ("--debug".equalsIgnoreCase(a)) { debug = true; continue; }
      if (a != null && a.startsWith("--exts=")) {
        String list = a.substring("--exts=".length());
        for (String e : list.split("[,;]")) {
          e = e.trim();
          if (!e.isEmpty()) {
            if (!e.startsWith(".")) e = "." + e;
            exts.add(e.toLowerCase(Locale.ROOT));
          }
        }
      }
    }
    if (exts.isEmpty()) {
      exts.addAll(Arrays.asList(DEFAULT_EXTS));
    }

    List<PatternEntry> patterns = loadPatterns(csvFile);
    if (debug) System.out.println("[DEBUG] Loaded patterns: " + patterns.size());

    try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(OUT_CSV), StandardCharsets.UTF_8))) {
      // Extended header (includes PatternName and new compatibility fields)
      out.write("ApplicationName,PatternID,PatternName,PatternSet,PatternNature,PatternDescription,LineNo,FileType,FilePathName,FoundContent,Fix,RemediationDescription,RemediationEffort,Mandays,JDK8Compatible,JDK21Compatible,CompatibilityStatus,PlanFirstChange,Remediation,ScriptableBySearchReplace");
      out.newLine();

      AtomicInteger matchSn = new AtomicInteger(0);
      Set<String> seen = new HashSet<>();

      // Java root
      Path javaRoot = Paths.get(javaSrcDir);
      if (Files.isDirectory(javaRoot)) {
        if (debug) System.out.println("[DEBUG] Scanning Java root: " + javaRoot);
        scanTree(javaRoot, exts, patterns, out, matchSn, applicationName, seen, debug);
      } else {
        System.err.println("[WARN] Java source dir not found or not a directory: " + javaRoot);
      }

      // JSP/TAG/XSL root (optional)
      if (jspSrcDir != null) {
        Path jspRoot = Paths.get(jspSrcDir);
        if (Files.isDirectory(jspRoot)) {
          if (debug) System.out.println("[DEBUG] Scanning JSP/TAG/XSL root: " + jspRoot);
          scanTree(jspRoot, exts, patterns, out, matchSn, applicationName, seen, debug);
        } else {
          System.err.println("[WARN] JSP source dir not found or not a directory: " + jspRoot);
        }
      }
      out.flush();
    }
    System.out.println("[INFO] CSV written: " + OUT_CSV);
  }

  // Pattern loader (header-driven)
  static List<PatternEntry> loadPatterns(String csvFile) throws IOException {
    List<PatternEntry> patterns = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
      String header = br.readLine();
      if (header == null) return patterns;
      String[] headers = splitCsv(header);
      Map<String, Integer> idx = headerIndex(headers);

      Integer ixPatternID = idx.get("patternid");
      Integer ixPatternSet = idx.get("patternset");
      Integer ixPatternNature = idx.get("patternnature");
      Integer ixPatternName = idx.get("patternname");
      Integer ixPatternDesc = idx.get("patterndescription");
      Integer ixRegex = idx.get("regexpattern");
      Integer ixFix = idx.get("fix");
      Integer ixRem = idx.get("remediation");
      Integer ixRemDesc = idx.get("remediationdescription");
      Integer ixRemEffort = idx.get("remediationeffort");
      Integer ixMandays = idx.get("mandays");
      Integer ixJ8 = idx.get("jdk8compatible");
      Integer ixJ21 = idx.get("jdk21compatible");
      Integer ixStatus = idx.get("compatibilitystatus");
      Integer ixPlan = idx.get("planfirstchange");
      Integer ixScriptable = idx.get("scriptablebysearchreplace");

      if (ixPatternID == null || ixPatternName == null || ixRegex == null) {
        throw new IOException("Required headers missing. Need: PatternID, PatternName, RegexPattern");
      }

      String line;
      while ((line = br.readLine()) != null) {
        if (line.trim().isEmpty()) continue;
        String[] parts = splitCsv(line);
        String patternID = safeGet(parts, ixPatternID);
        String patternSet = safeGet(parts, ixPatternSet);
        String patternNature = safeGet(parts, ixPatternNature);
        String patternName = safeGet(parts, ixPatternName);
        String patternDesc = safeGet(parts, ixPatternDesc);
        String regex = safeGet(parts, ixRegex);
        String fix = safeGet(parts, ixFix);
        String remediation = safeGet(parts, ixRem);
        String remDesc = safeGet(parts, ixRemDesc);
        String remEffort = safeGet(parts, ixRemEffort);
        String mandays = safeGet(parts, ixMandays);
        String j8 = safeGet(parts, ixJ8);
        String j21 = safeGet(parts, ixJ21);
        String status = safeGet(parts, ixStatus);
        String plan = safeGet(parts, ixPlan);
        String scriptable = safeGet(parts, ixScriptable);
        if (patternID.isEmpty() || patternName.isEmpty() || regex.isEmpty()) continue;
        try {
          Pattern compiled = Pattern.compile(regex, REGEX_FLAGS);
          patterns.add(new PatternEntry(patternID, patternSet, patternNature, patternName, patternDesc,
              regex, fix, remediation, remDesc, remEffort, mandays,
              j8, j21, status, plan, scriptable, compiled));
        } catch (PatternSyntaxException ex) {
          System.err.println("[WARN] Skipping invalid regex (" + patternID + " - " + patternName + "): " + ex.getMessage());
        }
      }
    }
    return patterns;
  }

  private static Map<String, Integer> headerIndex(String[] headers) {
    Map<String, Integer> idx = new HashMap<>();
    for (int i = 0; i < headers.length; i++) {
      String h = headers[i] == null ? "" : headers[i].trim().replace("\"", "").toLowerCase(Locale.ROOT);
      switch (h) {
        case "patternid": case "pattern id": case "id": idx.put("patternid", i); break;
        case "patternset": case "pattern set": case "set": idx.put("patternset", i); break;
        case "patternnature": case "pattern nature": case "nature": idx.put("patternnature", i); break;
        case "patternname": case "pattern name": case "name": idx.put("patternname", i); break;
        case "patterndescription": case "pattern description": case "description": idx.put("patterndescription", i); break;
        case "regex pattern": case "regexpattern": case "regex": case "pattern": idx.put("regexpattern", i); break;
        case "fix": case "recommendedfix": case "resolution": idx.put("fix", i); break;
        case "remediation": case "resolutiontype": idx.put("remediation", i); break;
        case "remediationdescription": case "remediation description": idx.put("remediationdescription", i); break;
        case "remediationeffort": case "remediation effort": idx.put("remediationeffort", i); break;
        case "mandays": idx.put("mandays", i); break;
        case "jdk8compatible": case "jdk8": idx.put("jdk8compatible", i); break;
        case "jdk21compatible": case "jdk21": idx.put("jdk21compatible", i); break;
        case "compatibilitystatus": case "status": idx.put("compatibilitystatus", i); break;
        case "planfirstchange": case "plan": idx.put("planfirstchange", i); break;
        case "scriptablebysearchreplace": case "scriptable": idx.put("scriptablebysearchreplace", i); break;
        default: /* ignore */
      }
    }
    return idx;
  }

  private static String[] splitCsv(String line) {
    return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
  }
  private static String safeGet(String[] parts, Integer ix) {
    if (ix == null || ix < 0 || ix >= parts.length) return "";
    String t = parts[ix].trim();
    if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
      t = t.substring(1, t.length() - 1);
    }
    return t;
  }

  static void scanTree(Path root, Set<String> exts, List<PatternEntry> patterns, BufferedWriter out, AtomicInteger matchSn,
                       String appName, Set<String> seen, boolean debug) throws IOException {
    try (Stream<Path> paths = Files.walk(root)) {
      paths.filter(Files::isRegularFile)
           .filter(p -> isSupported(p, exts))
           .forEach(p -> scanFile(p, patterns, out, matchSn, appName, seen, debug));
    }
  }

  static boolean isSupported(Path p, Set<String> exts) {
    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
    for (String ext : exts) { if (n.endsWith(ext)) return true; }
    return false;
  }

  static void scanFile(Path file, List<PatternEntry> patterns, BufferedWriter out, AtomicInteger matchSn,
                       String appName, Set<String> seen, boolean debug) {
    try {
      String content = readContentWithFallback(file);
      int[] lineOffsets = computeLineOffsets(content);
      String filePathName = relativeFromFpmsSrc(file);
      String fileType = detectFileType(file);
      for (PatternEntry p : patterns) {
        Matcher m = p.compiled.matcher(content);
        while (m.find()) {
          int start = m.start();
          int lineNo = lineNumberFromOffset(lineOffsets, start);
          String found = m.group();
          String foundSanitized = sanitizeSnippet(found, SNIPPET_MAX_LEN);
          String key = p.patternID + "\n" + filePathName + "\n" + lineNo + "\n" + start;
          if (seen.contains(key)) continue;
          seen.add(key);
          if (debug) {
            System.out.println("Match: [" + p.patternID + "] " + p.patternName + " in " + filePathName + ":" + lineNo);
          }
          writeCsvRow(out,
              appName,
              p.patternID, p.patternName, p.patternSet, p.patternNature, p.patternDescription,
              String.valueOf(lineNo), fileType, filePathName,
              foundSanitized, p.fix, p.remediationDescription, p.remediationEffort, formatMandays(p.mandays),
              p.jdk8, p.jdk21, p.status, p.plan, p.remediation, p.scriptable
          );
          matchSn.incrementAndGet();
        }
      }
    } catch (IOException e) {
      System.err.println("[ERROR] Failed to scan file: " + file + " - " + e.getMessage());
    }
  }

  private static String formatMandays(String m) {
    if (m == null || m.trim().isEmpty()) return "";
    try {
      double d = Double.parseDouble(m.trim());
      DecimalFormat df = new DecimalFormat("0.00");
      return df.format(d);
    } catch (NumberFormatException ex) { return m; }
  }

  private static String detectFileType(Path file) {
    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
    if (name.endsWith(".java")) return "JAVA";
    if (name.endsWith(".jsp") || name.endsWith(".jspf")) return "JSP";
    if (name.endsWith(".tag") || name.endsWith(".tagx")) return "TAG";
    if (name.endsWith(".xsl") || name.endsWith(".xslt")) return "XSL";
    if (name.endsWith(".xml")) return "XML";
    if (name.endsWith(".properties")) return "PROPERTIES";
    if (name.endsWith(".yml") || name.endsWith(".yaml")) return "YAML";
    if (name.endsWith(".bat") || name.endsWith(".cmd")) return "BATCH";
    if (name.endsWith(".sh")) return "SHELL";
    if (name.endsWith(".gradle") || name.endsWith(".kts")) return "GRADLE";
    if (name.endsWith(".pom") || name.endsWith(".ivy")) return "BUILDXML";
    if (name.endsWith(".conf") || name.endsWith(".cfg")) return "CONF";
    return "OTHER";
  }

  private static String relativeFromFpmsSrc(Path file) {
    String abs = file.toAbsolutePath().toString().replace('/', '\\');
    String lower = abs.toLowerCase(Locale.ROOT);
    int idx = lower.indexOf("fpms-src\\");
    if (idx >= 0) return abs.substring(idx);
    idx = lower.indexOf("fpms-src");
    if (idx >= 0) return abs.substring(idx);
    return abs;
  }

  private static String readContentWithFallback(Path file) throws IOException {
    try { return Files.readString(file, StandardCharsets.UTF_8); }
    catch (MalformedInputException ex) {
      CharsetDecoder dec = Charset.forName("windows-1252").newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
      try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(file), dec))) {
        StringBuilder sb = new StringBuilder((int)Math.min(Files.size(file), 1_000_000L));
        String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
        return sb.toString();
      }
    }
  }

  private static int[] computeLineOffsets(String content) {
    ArrayList<Integer> offs = new ArrayList<>();
    offs.add(0);
    int len = content.length();
    for (int i = 0; i < len; i++) if (content.charAt(i) == '\n') offs.add(i + 1);
    offs.add(len + 1);
    int[] arr = new int[offs.size()];
    for (int i = 0; i < offs.size(); i++) arr[i] = offs.get(i);
    return arr;
  }

  private static int lineNumberFromOffset(int[] lineOffsets, int pos) {
    int lo = 0, hi = lineOffsets.length - 1;
    while (lo <= hi) {
      int mid = (lo + hi) >>> 1;
      int v = lineOffsets[mid];
      if (v == pos) return mid + 1;
      if (v < pos) lo = mid + 1; else hi = mid - 1;
    }
    return Math.max(1, hi + 1);
  }

  private static String sanitizeSnippet(String s, int maxLen) {
    if (s == null) return "";
    String t = s.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim();
    if (t.length() <= maxLen) return t;
    int keep = Math.max(20, maxLen / 2);
    return t.substring(0, maxLen - keep - 5) + " ... " + t.substring(t.length() - keep);
  }

  private static void writeCsvRow(BufferedWriter out,
    String applicationName,
    String patternID, String patternName, String patternSet, String patternNature, String patternDescription,
    String lineNo, String fileType, String filePathName,
    String foundContent, String fix, String remediationDescription, String remediationEffort, String mandays,
    String jdk8, String jdk21, String status, String plan, String remediation, String scriptable
  ) throws IOException {
    out.write(csv(applicationName)); out.write(',');
    out.write(csv(patternID)); out.write(',');
    out.write(csv(patternName)); out.write(',');
    out.write(csv(patternSet)); out.write(',');
    out.write(csv(patternNature)); out.write(',');
    out.write(csv(patternDescription)); out.write(',');
    out.write(csv(lineNo)); out.write(',');
    out.write(csv(fileType)); out.write(',');
    out.write(csv(filePathName)); out.write(',');
    out.write(csv(foundContent)); out.write(',');
    out.write(csv(fix)); out.write(',');
    out.write(csv(remediationDescription)); out.write(',');
    out.write(csv(remediationEffort)); out.write(',');
    out.write(csv(mandays)); out.write(',');
    out.write(csv(jdk8)); out.write(',');
    out.write(csv(jdk21)); out.write(',');
    out.write(csv(status)); out.write(',');
    out.write(csv(plan)); out.write(',');
    out.write(csv(remediation)); out.write(',');
    out.write(csv(scriptable));
    out.newLine();
  }

  private static String csv(String val) {
    if (val == null) val = "";
    String v = val;
    if (v.contains("\"")) v = v.replace("\"", "\"\"");
    boolean needsQuote = v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.startsWith(" ") || v.endsWith(" ");
    return needsQuote ? '"' + v + '"' : v;
  }

  private static boolean isFlag(String s) { return s != null && s.startsWith("--"); }
  private static String notEmpty(String s) { return (s != null && !s.trim().isEmpty()) ? s : null; }
  private static String notEmpty(String s, String def) { return (s != null && !s.trim().isEmpty()) ? s : def; }

  static class PatternEntry {
    final String patternID;
    final String patternSet;
    final String patternNature;
    final String patternName;
    final String patternDescription;
    final String regex;
    final String fix;
    final String remediation;
    final String remediationDescription;
    final String remediationEffort;
    final String mandays;
    final String jdk8;
    final String jdk21;
    final String status;
    final String plan;
    final String scriptable;
    final Pattern compiled;
    PatternEntry(String patternID, String patternSet, String patternNature,
                 String patternName, String patternDescription,
                 String regex, String fix,
                 String remediation, String remediationDescription, String remediationEffort, String mandays,
                 String jdk8, String jdk21, String status, String plan, String scriptable,
                 Pattern compiled) {
      this.patternID = patternID;
      this.patternSet = patternSet;
      this.patternNature = patternNature;
      this.patternName = patternName;
      this.patternDescription = patternDescription;
      this.regex = regex;
      this.fix = fix;
      this.remediation = remediation;
      this.remediationDescription = remediationDescription;
      this.remediationEffort = remediationEffort;
      this.mandays = mandays;
      this.jdk8 = jdk8;
      this.jdk21 = jdk21;
      this.status = status;
      this.plan = plan;
      this.scriptable = scriptable;
      this.compiled = compiled;
    }
  }
}
