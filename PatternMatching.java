public class PatternMatching {
    private static final int R = 256;
    private static final long Q = 2_147_483_647L;

    public static final class Metrics {
        public long iterations;
        public long instructions;
        public long comparisons;
        public long collisions;

        public void addInstructions(long amount) {
            instructions += amount;
        }
    }

    public static final class Result {
        public final int index;
        public final Metrics metrics;
        public final long elapsedMillis;

        private Result(int index, Metrics metrics, long elapsedMillis) {
            this.index = index;
            this.metrics = metrics;
            this.elapsedMillis = elapsedMillis;
        }
    }

    private static String generateText(int length, String alphabet) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(i % alphabet.length()));
        }
        return builder.toString();
    }

    private static String generateWorstCasePattern(int length) {
        if (length <= 0) {
            return "";
        }
        if (length == 1) {
            return "B";
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length - 1; i++) {
            builder.append('A');
        }
        builder.append('B');
        return builder.toString();
    }

    public static int bruteForceSearch(String text, String pattern, Metrics metrics) {
        int n = text.length();
        int m = pattern.length();
        metrics.addInstructions(2);

        if (m == 0) {
            metrics.addInstructions(1);
            return 0;
        }
        if (m > n) {
            metrics.addInstructions(1);
            return -1;
        }

        for (int i = 0; i <= n - m; i++) {
            metrics.iterations++;
            metrics.addInstructions(3);

            int j = 0;
            while (j < m) {
                metrics.iterations++;
                metrics.addInstructions(2);
                metrics.comparisons++;
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    metrics.addInstructions(2);
                    break;
                }
                j++;
                metrics.addInstructions(2);
            }

            if (j == m) {
                metrics.addInstructions(1);
                return i;
            }
        }

        metrics.addInstructions(1);
        return -1;
    }

    private static long hash(String value, int length) {
        long h = 0L;
        for (int i = 0; i < length; i++) {
            h = (h * R + value.charAt(i)) % Q;
        }
        return h;
    }

    public static int rollingHashSearch(String text, String pattern, Metrics metrics) {
        int n = text.length();
        int m = pattern.length();
        metrics.addInstructions(2);

        if (m == 0) {
            metrics.addInstructions(1);
            return 0;
        }
        if (m > n) {
            metrics.addInstructions(1);
            return -1;
        }

        long rm = 1L;
        for (int i = 1; i < m; i++) {
            rm = (rm * R) % Q;
            metrics.iterations++;
            metrics.addInstructions(3);
        }

        long patHash = hash(pattern, m);
        long txtHash = hash(text, m);
        metrics.addInstructions(2 * m + 2);

        for (int i = 0; i <= n - m; i++) {
            metrics.iterations++;
            metrics.addInstructions(2);

            if (patHash == txtHash) {
                boolean matched = true;
                for (int j = 0; j < m; j++) {
                    metrics.iterations++;
                    metrics.addInstructions(2);
                    metrics.comparisons++;
                    if (text.charAt(i + j) != pattern.charAt(j)) {
                        matched = false;
                        metrics.addInstructions(2);
                        break;
                    }
                }
                if (matched) {
                    metrics.addInstructions(1);
                    return i;
                }
                metrics.collisions++;
            }

            if (i < n - m) {
                long leading = text.charAt(i);
                long trailing = text.charAt(i + m);
                txtHash = (txtHash + Q - (leading * rm) % Q) % Q;
                txtHash = (txtHash * R + trailing) % Q;
                metrics.addInstructions(6);
            }
        }

        metrics.addInstructions(1);
        return -1;
    }

    public static int[] computeLPSArray(String pattern, Metrics metrics) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;

        while (i < m) {
            metrics.iterations++;
            metrics.addInstructions(2);
            metrics.comparisons++;

            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
                metrics.addInstructions(4);
            } else if (len != 0) {
                len = lps[len - 1];
                metrics.addInstructions(2);
            } else {
                lps[i] = 0;
                i++;
                metrics.addInstructions(3);
            }
        }

        return lps;
    }

    public static int kmpSearch(String text, String pattern, Metrics metrics) {
        int n = text.length();
        int m = pattern.length();
        metrics.addInstructions(2);

        if (m == 0) {
            metrics.addInstructions(1);
            return 0;
        }
        if (m > n) {
            metrics.addInstructions(1);
            return -1;
        }

        int[] lps = computeLPSArray(pattern, metrics);
        int i = 0;
        int j = 0;

        while (i < n) {
            metrics.iterations++;
            metrics.addInstructions(2);
            metrics.comparisons++;

            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
                metrics.addInstructions(4);
            }

            if (j == m) {
                metrics.addInstructions(1);
                return i - j;
            }

            metrics.comparisons++;
            if (i < n && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                    metrics.addInstructions(2);
                } else {
                    i++;
                    metrics.addInstructions(2);
                }
            }
        }

        metrics.addInstructions(1);
        return -1;
    }

    private static Result benchmark(SearchFunction searchFunction, String text, String pattern) {
        Metrics metrics = new Metrics();
        long startedAt = System.currentTimeMillis();
        int index = searchFunction.search(text, pattern, metrics);
        long elapsedMillis = System.currentTimeMillis() - startedAt;
        return new Result(index, metrics, elapsedMillis);
    }

    private static void printResult(String title, Result result, String text, String pattern) {
        System.out.println(title);
        System.out.println("  index=" + result.index + ", time=" + result.elapsedMillis + "ms");
        System.out.println("  text=" + text.length() + ", pattern=" + pattern.length());
        System.out.println("  iterations=" + result.metrics.iterations
                + ", instructions=" + result.metrics.instructions
                + ", comparisons=" + result.metrics.comparisons
                + ", collisions=" + result.metrics.collisions);
    }

    public static void main(String[] args) {
        String smallText = "ABCDCBDCBDACBDABDCBADF";
        String smallPattern = "ADF";

        String largeText = generateText(500_001, "ABCD");
        String largePattern = generateWorstCasePattern(1_000);
        String largePatternAtEnd = largeText.substring(largeText.length() - largePattern.length());

        runCase("Caso pequeno", smallText, smallPattern);
        runCase("Caso grande sem pior caso", largeText, largePattern);
        runCase("Caso grande com ocorrência no fim", largeText, largePatternAtEnd);
    }

    private static void runCase(String title, String text, String pattern) {
        System.out.println();
        System.out.println(title);
        printResult("Brute force", benchmark(PatternMatching::bruteForceSearch, text, pattern), text, pattern);
        printResult("Rolling Hash", benchmark(PatternMatching::rollingHashSearch, text, pattern), text, pattern);
        printResult("KMP", benchmark(PatternMatching::kmpSearch, text, pattern), text, pattern);
    }

    @FunctionalInterface
    private interface SearchFunction {
        int search(String text, String pattern, Metrics metrics);
    }
}