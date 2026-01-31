import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class RegenerateThirdPartyRegex {

    // ====== CONFIG ======
    private static final String INPUT_CSV = "thirdparty_patterns.csv";
    private static final String LIB_DIR   = "lib";
    private static final String OUTPUT_CSV = "thirdparty_patterns_regex_regenerated.csv";

    // Limits to keep regex size reasonable
    private static final int MAX_MATCHED_JARS_PER_PATTERN = 3;
    private static final int TOP_N_ROOTS_PER_JAR = 10;
    private static final int MAX_TOTAL_ROOTS_PER_PATTERN = 12;

    private static final String[] IGNORE_PREFIXES = new String[] {
            "java.", "javax.", "jakarta.", "sun.", "com.sun.",
            "org.w3c.", "org.xml.", "org.omg.", "org.ietf.", "org.jcp.",
            "META-INF.", "module-info"
    };

    public static void main(String[] args) throws Exception {
        File input = new File(INPUT_CSV);
        File lib = new File(LIB_DIR);

        if (!input.exists()) {
            System.err.println("ERROR: Missing " + INPUT_CSV + " in current directory.");
            System.exit(2);
        }
        if (!lib.exists() || !lib.isDirectory()) {
            System.err.println("ERROR: Missing lib/ folder (expected at: " + lib.getAbsolutePath() + ")");
            System.err.println("Create lib/ and drop all .jar files there, then rerun.");
            System.exit(3);
        }

        // Index jar files
        List<File> allJars = findAllJars(lib);
        System.out.println("Jars found under lib/: " + allJars.size());

        Map<String, File> jarByFilenameLower = new HashMap<>();
        Map<String, List<File>> jarByNormalizedBase = new HashMap<>();

        for (File jar : allJars) {
            String fnLower = jar.getName().toLowerCase(Locale.ROOT);
            jarByFilenameLower.put(fnLower, jar);

            String base = normalizeJarBase(jar.getName());
            jarByNormalizedBase.computeIfAbsent(base, k -> new ArrayList<>()).add(jar);
        }

        // Read CSV rows
        List<Map<String, String>> rows = readCsvAsMaps(INPUT_CSV);

        // Ensure required column exists
        if (rows.isEmpty()) {
            System.err.println("ERROR: CSV has no rows.");
            System.exit(4);
        }

        // Add new columns if missing
        ensureColumn(rows, "RegexPattern_JarBackup");
        ensureColumn(rows, "MatchedJars_FromLib");
        ensureColumn(rows, "PackageRoots_FromLib");

        int rowsWithRoots = 0;

        for (Map<String, String> row : rows) {
            String patternName = val(row, "PatternName");

            // Backup existing RegexPattern
            row.put("RegexPattern_JarBackup", val(row, "RegexPattern"));

            // Find matching jars in lib/
            List<File> matched = matchJars(patternName, jarByFilenameLower, jarByNormalizedBase);

            // Extract package roots from those jars
            List<String> roots = new ArrayList<>();
            int countJarsUsed = 0;
            for (File jar : matched) {
                if (countJarsUsed >= MAX_MATCHED_JARS_PER_PATTERN) break;
                roots.addAll(extractTopPackageRoots(jar, TOP_N_ROOTS_PER_JAR));
                countJarsUsed++;
            }

            // De-dup roots and trim
            roots = dedupePreserveOrder(roots);
            if (roots.size() > MAX_TOTAL_ROOTS_PER_PATTERN) {
                roots = roots.subList(0, MAX_TOTAL_ROOTS_PER_PATTERN);
            }

            // Store diagnostics
            row.put("MatchedJars_FromLib", joinFileNames(matched, 5));
            row.put("PackageRoots_FromLib", String.join(";", roots));

            // Build RegexPattern
            String jarRegex = buildJarNameRegex(patternName);

            String newRegex;
            if (!roots.isEmpty()) {
                rowsWithRoots++;
                String srcRegex = buildSourceUsageRegex(roots);
                newRegex = srcRegex + "|" + jarRegex;
            } else {
                // Fallback to jar-name only if no jar matched
                newRegex = jarRegex;
            }

            row.put("RegexPattern", newRegex);
        }

        // Write output CSV with original header order + new columns appended if needed
        writeCsvFromMaps(rows, OUTPUT_CSV);

        System.out.println("Done.");
        System.out.println("Rows: " + rows.size());
        System.out.println("Rows with package roots extracted: " + rowsWithRoots);
        System.out.println("Output: " + OUTPUT_CSV);
    }

    // ====== Jar scanning ======

    private static List<File> findAllJars(File dir) {
        List<File> jars = new ArrayList<>();
        Deque<File> stack = new ArrayDeque<>();
        stack.push(dir);

        while (!stack.isEmpty()) {
            File cur = stack.pop();
            File[] children = cur.listFiles();
            if (children == null) continue;

            for (File f : children) {
                if (f.isDirectory()) stack.push(f);
                else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) jars.add(f);
            }
        }
        return jars;
    }

    private static List<String> extractTopPackageRoots(File jarFile, int topN) {
        Map<String, Integer> counts = new HashMap<>();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) continue;
                if (name.startsWith("META-INF/")) continue;

                // Convert to dotted class name without .class
                String dotted = name.substring(0, name.length() - 6).replace('/', '.');

                if (startsWithAny(dotted, IGNORE_PREFIXES)) continue;

                String[] parts = dotted.split("\\.");
                if (parts.length < 2) continue;

                // Candidate roots depth 2..5
                for (int depth : new int[]{2,3,4,5}) {
                    if (parts.length >= depth) {
                        String root = joinFirst(parts, depth);
                        if (startsWithAny(root, IGNORE_PREFIXES)) continue;
                        inc(counts, root);
                    }
                }
            }
        } catch (Exception e) {
            // If a jar cannot be opened, return empty roots
            return Collections.emptyList();
        }

        // Rank by frequency, prefer deeper (longer) roots
        List<Map.Entry<String,Integer>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort((a,b) -> {
            int c = Integer.compare(b.getValue(), a.getValue());
            if (c != 0) return c;
            return Integer.compare(b.getKey().length(), a.getKey().length());
        });

        // Filter generic roots
        List<String> roots = new ArrayList<>();
        for (Map.Entry<String,Integer> e : ranked) {
            String root = e.getKey();
            if (root.equals("org.apache") || root.equals("org.springframework")
                    || root.equals("com.oracle") || root.equals("com.google")
                    || root.equals("org.junit")) {
                continue;
            }
            // Keep most specific (avoid prefixes)
            boolean skip = false;
            for (String chosen : roots) {
                if (root.equals(chosen) || chosen.startsWith(root + ".")) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;
            roots.add(root);
            if (roots.size() >= topN) break;
        }
        return roots;
    }

    private static void inc(Map<String,Integer> map, String key) {
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private static boolean startsWithAny(String s, String[] prefixes) {
        for (String p : prefixes) {
            if (s.startsWith(p)) return true;
        }
        return false;
    }

    private static String joinFirst(String[] parts, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i > 0) sb.append('.');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private static List<String> matchJars(String patternName,
                                         Map<String, File> jarByFilenameLower,
                                         Map<String, List<File>> jarByNormalizedBase) {

        String pn = patternName == null ? "" : patternName.trim();
        String pnLower = pn.toLowerCase(Locale.ROOT);

        List<File> matched = new ArrayList<>();

        // Exact filename match
        File exact = jarByFilenameLower.get(pnLower);
        if (exact != null) {
            matched.add(exact);
            return matched;
        }

        // Normalized base match
        String base = normalizeJarBase(pn);
        List<File> byBase = jarByNormalizedBase.get(base);
        if (byBase != null) matched.addAll(byBase);

        // Fuzzy: if still none, try startsWith/contains
        if (matched.isEmpty() && base.length() >= 3) {
            for (Map.Entry<String, List<File>> e : jarByNormalizedBase.entrySet()) {
                String b = e.getKey();
                if (b.startsWith(base) || base.startsWith(b)) {
                    matched.addAll(e.getValue());
                }
            }
        }

        // Sort for determinism
        matched.sort(Comparator.comparing(File::getName));
        return matched;
    }

    // ====== Regex building ======

    private static String buildSourceUsageRegex(List<String> roots) {
        // Build alternation group: (?:root1|root2|...)
        StringBuilder alt = new StringBuilder();
        alt.append("(?:");
        for (int i = 0; i < roots.size(); i++) {
            if (i > 0) alt.append("|");
            alt.append(Pattern.quote(roots.get(i)).replace("\\Q", "").replace("\\E", "").replace(".", "\\."));
        }
        alt.append(")");

        // Regex:
        // - Java imports (multiline)
        // - JSP page directive import
        // - Fully-qualified usage
        //
        // Flags: (?ims) -> case-insensitive? (not needed), multiline, dotall
        // We use (?ms) mostly.
        return ""
                + "(?ms)"
                + "(?:^\\s*import\\s+(?:static\\s+)?" + alt + "(?:\\.[\\w$]+|\\.\\*)\\s*;)"
                + "|"
                + "(?:<%@\\s*page\\b[^%]*\\bimport\\s*=\\s*\\\"[^\\\"]*\\b" + alt + "\\.)"
                + "|"
                + "(?:\\b" + alt + "\\.[A-Za-z_$][\\w$]*\\b)";
    }

    private static String buildJarNameRegex(String patternName) {
        String base = normalizeJarBase(patternName);
        // boundary-safe without lookbehind
        return "(?i)(^|[^A-Za-z0-9_.-])" + escapeRegexLiteral(base)
                + "([-_][A-Za-z0-9_.-]+)?\\.jar([^A-Za-z0-9_.-]|$)";
    }

    private static String escapeRegexLiteral(String s) {
        // Escape regex metacharacters
        return s.replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("*", "\\*")
                .replace("?", "\\?")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("|", "\\|");
    }

    private static String normalizeJarBase(String jarName) {
        if (jarName == null) return "";
        String s = jarName.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("(?i)\\.jar$", "");
        s = s.replace("-snapshot", "").replace("_snapshot", "");
        s = s.replaceAll("-cvs-?\\d{6,8}$", "");
        s = s.replaceAll("-(alpha|beta|rc)\\b.*$", "");
        s = s.replaceAll("[-_]\\d.*$", ""); // drop trailing versions
        if (s.isEmpty()) return "unknown";
        return s;
    }

    // ====== CSV utilities (no external libs) ======

    private static List<Map<String, String>> readCsvAsMaps(String csvPath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(csvPath), StandardCharsets.UTF_8);
        if (lines.isEmpty()) return Collections.emptyList();

        List<String> headers = parseCsvLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;

            List<String> vals = parseCsvLine(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                String h = headers.get(c);
                String v = c < vals.size() ? vals.get(c) : "";
                row.put(h, v);
            }
            rows.add(row);
        }
        return rows;
    }

    private static void writeCsvFromMaps(List<Map<String, String>> rows, String outPath) throws IOException {
        if (rows.isEmpty()) return;

        // Collect headers in stable order: first row order, then any extras
        LinkedHashSet<String> headers = new LinkedHashSet<>(rows.get(0).keySet());
        for (Map<String, String> r : rows) headers.addAll(r.keySet());

        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(outPath), StandardCharsets.UTF_8)) {
            w.write(toCsvLine(new ArrayList<>(headers)));
            w.newLine();

            for (Map<String, String> r : rows) {
                List<String> vals = new ArrayList<>();
                for (String h : headers) {
                    vals.add(r.getOrDefault(h, ""));
                }
                w.write(toCsvLine(vals));
                w.newLine();
            }
        }
    }

    private static void ensureColumn(List<Map<String, String>> rows, String colName) {
        for (Map<String, String> r : rows) {
            if (!r.containsKey(colName)) r.put(colName, "");
        }
    }

    private static String val(Map<String, String> row, String key) {
        String v = row.get(key);
        return v == null ? "" : v;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"'); // escaped quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else {
                if (ch == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else if (ch == '"') {
                    inQuotes = true;
                } else {
                    cur.append(ch);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String toCsvLine(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(csvEscape(values.get(i)));
        }
        return sb.toString();
    }

    private static String csvEscape(String v) {
        if (v == null) v = "";
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        if (!needsQuotes) return v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static List<String> dedupePreserveOrder(List<String> list) {
        LinkedHashSet<String> set = new LinkedHashSet<>(list);
        return new ArrayList<>(set);
    }

    private static String joinFileNames(List<File> files, int max) {
        if (files == null || files.isEmpty()) return "";
        int lim = Math.min(max, files.size());
        List<String> names = new ArrayList<>();
        for (int i = 0; i < lim; i++) names.add(files.get(i).getName());
        return String.join(";", names);
    }
}
