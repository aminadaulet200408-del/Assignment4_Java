import java.util.*;

public final class Algorithms {
    private Algorithms() {}

    private static String string(Map<String, Object> x, String key) {
        return (String) x.get(key);
    }

    private static long number(Object value) {
        return ((Number) value).longValue();
    }

    private static List<?> list(Map<String, Object> x, String key) {
        return (List<?>) x.get(key);
    }

    private static Map<String, Object> result(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            out.put((String) pairs[i], pairs[i + 1]);
        }
        return out;
    }

    private static List<Long> longList(int[] values) {
        List<Long> out = new ArrayList<>(values.length);
        for (int value : values) out.add((long) value);
        return out;
    }

    /* Longest Common Subsequence: prefix DP + reconstruction. */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = string(input, "a");
        String b = string(input, "b");
        int n = a.length(), m = b.length();

        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                if (ca == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        StringBuilder reversed = new StringBuilder(dp[n][m]);
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                reversed.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        String sequence = reversed.reverse().toString();
        return result("length", (long) sequence.length(), "sequence", sequence);
    }

    /* Optimal adjacent merge order: interval DP. */
    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> raw = list(input, "sizes");
        int n = raw.size();
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + number(raw.get(i));

        if (n <= 1) return result("cost", 0L, "plan", new ArrayList<>());

        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];

        for (int length = 2; length <= n; length++) {
            for (int left = 0; left + length <= n; left++) {
                int right = left + length - 1;
                long intervalSum = prefix[right + 1] - prefix[left];
                long best = Long.MAX_VALUE;
                int bestK = left;
                for (int k = left; k < right; k++) {
                    long candidate = dp[left][k] + dp[k + 1][right] + intervalSum;
                    if (candidate < best) {
                        best = candidate;
                        bestK = k;
                    }
                }
                dp[left][right] = best;
                split[left][right] = bestK;
            }
        }

        List<List<Long>> plan = new ArrayList<>(n - 1);
        buildMergePlan(0, n - 1, split, plan);
        return result("cost", dp[0][n - 1], "plan", plan);
    }

    private static void buildMergePlan(int left, int right, int[][] split,
                                       List<List<Long>> plan) {
        if (left >= right) return;
        int k = split[left][right];
        buildMergePlan(left, k, split, plan);
        buildMergePlan(k + 1, right, split, plan);
        plan.add(Arrays.asList((long) left, (long) k, (long) right));
    }

    /* Maximum-weight independent set on a tree: iterative take/skip DP. */
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = (int) number(input.get("n"));
        List<?> rawWeights = list(input, "weights");
        long[] weights = new long[n];
        for (int i = 0; i < n; i++) weights[i] = number(rawWeights.get(i));
        if (n == 0) return result("weight", 0L, "selected", new ArrayList<>());

        List<?> edges = list(input, "edges");
        @SuppressWarnings("unchecked")
        List<Integer>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) graph[i] = new ArrayList<>();
        for (Object edgeObject : edges) {
            List<?> edge = (List<?>) edgeObject;
            int u = (int) number(edge.get(0));
            int v = (int) number(edge.get(1));
            graph[u].add(v);
            graph[v].add(u);
        }

        int[] parent = new int[n];
        Arrays.fill(parent, -2);
        int[] order = new int[n];
        int size = 0;
        int[] stack = new int[n];
        int top = 0;
        stack[top++] = 0;
        parent[0] = -1;

        while (top > 0) {
            int v = stack[--top];
            order[size++] = v;
            for (int u : graph[v]) {
                if (u == parent[v]) continue;
                parent[u] = v;
                stack[top++] = u;
            }
        }

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int idx = size - 1; idx >= 0; idx--) {
            int v = order[idx];
            take[v] = weights[v];
            skip[v] = 0L;
            for (int u : graph[v]) {
                if (parent[u] == v) {
                    take[v] += skip[u];
                    skip[v] += Math.max(take[u], skip[u]);
                }
            }
        }

        List<Long> selected = new ArrayList<>();
        int[] decisionStack = new int[n];
        boolean[] parentTakenStack = new boolean[n];
        int ds = 0;
        decisionStack[ds] = 0;
        parentTakenStack[ds++] = false;
        while (ds > 0) {
            int v = decisionStack[--ds];
            boolean parentTaken = parentTakenStack[ds];
            boolean choose = !parentTaken && take[v] > skip[v];
            if (choose) selected.add((long) v);
            for (int u : graph[v]) {
                if (parent[u] == v) {
                    decisionStack[ds] = u;
                    parentTakenStack[ds++] = choose;
                }
            }
        }

        long optimum = Math.max(take[0], skip[0]);
        return result("weight", optimum, "selected", selected);
    }

    /* Straightforward exact substring search. */
    static Map<String, Object> naive(Map<String, Object> input) {
        String text = string(input, "text");
        String pattern = string(input, "pattern");
        int n = text.length(), m = pattern.length();
        List<Long> positions = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) positions.add((long) i);
        } else if (m <= n) {
            for (int start = 0; start <= n - m; start++) {
                int j = 0;
                while (j < m && text.charAt(start + j) == pattern.charAt(j)) j++;
                if (j == m) positions.add((long) start);
            }
        }
        return result("positions", positions);
    }

    private static int[] prefixFunction(String pattern) {
        int m = pattern.length();
        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int j = pi[i - 1];
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }
        return pi;
    }

    /* Knuth-Morris-Pratt substring search. */
    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = string(input, "text");
        String pattern = string(input, "pattern");
        int n = text.length(), m = pattern.length();
        List<Long> positions = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) positions.add((long) i);
            return result("positions", positions);
        }
        if (m > n) return result("positions", positions);

        int[] pi = prefixFunction(pattern);
        int j = 0;
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            while (j > 0 && c != pattern.charAt(j)) j = pi[j - 1];
            if (c == pattern.charAt(j)) j++;
            if (j == m) {
                positions.add((long) (i - m + 1));
                j = pi[j - 1];
            }
        }
        return result("positions", positions);
    }

    /* Rabin-Karp with the assignment's deliberately small modulus. */
    static Map<String, Object> rk(Map<String, Object> input) {
        String text = string(input, "text");
        String pattern = string(input, "pattern");
        int n = text.length(), m = pattern.length();
        List<Long> positions = new ArrayList<>();
        if (m == 0) {
            for (int i = 0; i <= n; i++) positions.add((long) i);
            return result("positions", positions);
        }
        if (m > n) return result("positions", positions);

        final long base = 31L, mod = 101L;
        long highestPower = 1L;
        for (int i = 1; i < m; i++) highestPower = (highestPower * base) % mod;

        long patternHash = 0L, windowHash = 0L;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * base + pattern.charAt(i)) % mod;
            windowHash = (windowHash * base + text.charAt(i)) % mod;
        }

        for (int start = 0; start <= n - m; start++) {
            if (windowHash == patternHash && regionEquals(text, start, pattern)) {
                positions.add((long) start);
            }
            if (start < n - m) {
                long removed = (text.charAt(start) * highestPower) % mod;
                windowHash = (windowHash - removed) % mod;
                if (windowHash < 0) windowHash += mod;
                windowHash = (windowHash * base + text.charAt(start + m)) % mod;
            }
        }
        return result("positions", positions);
    }

    private static boolean regionEquals(String text, int start, String pattern) {
        for (int j = 0; j < pattern.length(); j++) {
            if (text.charAt(start + j) != pattern.charAt(j)) return false;
        }
        return true;
    }

    /* Suffix array by prefix doubling, then Kasai LCP. */
    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = string(input, "text");
        int n = text.length();
        if (n == 0) {
            return result("sa", new ArrayList<>(), "lcp", new ArrayList<>(), "repeated", "");
        }

        int[] sa = new int[n];
        int[] rank = new int[n];
        int[] nextRank = new int[n];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
            order[i] = i;
        }

        for (int width = 1; width < n; width <<= 1) {
            final int w = width;
            final int[] currentRank = rank;
            Arrays.sort(order, (a, b) -> {
                if (currentRank[a] != currentRank[b]) {
                    return Integer.compare(currentRank[a], currentRank[b]);
                }
                int ra = a + w < n ? currentRank[a + w] : -1;
                int rb = b + w < n ? currentRank[b + w] : -1;
                return Integer.compare(ra, rb);
            });

            for (int i = 0; i < n; i++) sa[i] = order[i];
            nextRank[sa[0]] = 0;
            for (int i = 1; i < n; i++) {
                int previous = sa[i - 1], current = sa[i];
                boolean different = rank[previous] != rank[current];
                int previousSecond = previous + width < n ? rank[previous + width] : -1;
                int currentSecond = current + width < n ? rank[current + width] : -1;
                if (previousSecond != currentSecond) different = true;
                nextRank[current] = nextRank[previous] + (different ? 1 : 0);
            }
            int[] swap = rank;
            rank = nextRank;
            nextRank = swap;
            if (rank[sa[n - 1]] == n - 1) break;
            if (width > n / 2) break; // prevents integer overflow in width <<= 1
        }

        int[] position = new int[n];
        for (int i = 0; i < n; i++) position[sa[i]] = i;
        int[] lcp = new int[n];
        int common = 0;
        for (int i = 0; i < n; i++) {
            int r = position[i];
            if (r == 0) {
                common = 0;
                continue;
            }
            int j = sa[r - 1];
            while (i + common < n && j + common < n
                    && text.charAt(i + common) == text.charAt(j + common)) {
                common++;
            }
            lcp[r] = common;
            if (common > 0) common--;
        }

        int bestLength = 0, bestStart = 0;
        for (int i = 1; i < n; i++) {
            if (lcp[i] > bestLength) {
                bestLength = lcp[i];
                bestStart = sa[i];
            }
        }
        String repeated = bestLength == 0 ? "" : text.substring(bestStart, bestStart + bestLength);
        return result("sa", longList(sa), "lcp", longList(lcp), "repeated", repeated);
    }

    /* Aho-Corasick: one trie, failure links, and per-text visit propagation. */
    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> rawPatterns = list(input, "patterns");
        List<?> rawTexts = list(input, "texts");
        int p = rawPatterns.size();
        int totalLength = 0;
        String[] patterns = new String[p];
        for (int i = 0; i < p; i++) {
            patterns[i] = (String) rawPatterns.get(i);
            totalLength += patterns[i].length();
        }

        int capacity = totalLength + 1;
        final int alphabet = 95;
        int[] transitions = new int[capacity * alphabet];
        int[] failure = new int[capacity];
        int[] terminal = new int[p];
        int states = 1;

        for (int i = 0; i < p; i++) {
            int state = 0;
            String pattern = patterns[i];
            for (int j = 0; j < pattern.length(); j++) {
                int c = pattern.charAt(j) - 32;
                int index = state * alphabet + c;
                int child = transitions[index];
                if (child == 0) {
                    child = states++;
                    transitions[index] = child;
                }
                state = child;
            }
            terminal[i] = state;
        }

        int[] queue = new int[states];
        int head = 0, tail = 0;
        for (int c = 0; c < alphabet; c++) {
            int child = transitions[c];
            if (child != 0) {
                queue[tail++] = child;
                failure[child] = 0;
            }
        }

        while (head < tail) {
            int v = queue[head++];
            int row = v * alphabet;
            int failRow = failure[v] * alphabet;
            for (int c = 0; c < alphabet; c++) {
                int child = transitions[row + c];
                if (child != 0) {
                    failure[child] = transitions[failRow + c];
                    queue[tail++] = child;
                } else {
                    transitions[row + c] = transitions[failRow + c];
                }
            }
        }

        List<List<Long>> allCounts = new ArrayList<>(rawTexts.size());
        for (Object textObject : rawTexts) {
            String text = (String) textObject;
            long[] visits = new long[states];
            int state = 0;
            for (int i = 0; i < text.length(); i++) {
                int c = text.charAt(i) - 32;
                state = transitions[state * alphabet + c];
                visits[state]++;
            }

            for (int i = tail - 1; i >= 0; i--) {
                int v = queue[i];
                visits[failure[v]] += visits[v];
            }

            List<Long> row = new ArrayList<>(p);
            for (int i = 0; i < p; i++) row.add(visits[terminal[i]]);
            allCounts.add(row);
        }
        return result("counts", allCounts);
    }
}
