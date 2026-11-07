# Assignment 4 — Algorithm Analysis

## Interface and implementation overview

The program reads UTF-8 JSON Lines from standard input. Each non-empty line is parsed as one request object. The `task` field selects one of eight algorithms. One JSON object is printed for each request. Algorithm data structures are created inside the corresponding method, so counters and other mutable state do not leak between requests.

`Main.java` contains the dispatcher. `Json.java` is a small dependency-free JSON parser and serializer. `Algorithms.java` contains the algorithm implementations. All indices and positions are zero-based. Long-valued totals and counts use Java `long`.

## 1. Longest Common Subsequence (LCS)

Let `dp[i][j]` be the length of an LCS of the first `i` characters of `a` and the first `j` characters of `b`. The base cases are `dp[0][j] = 0` and `dp[i][0] = 0`, because an empty prefix has no non-empty common subsequence.

If the last characters are equal, an optimal solution can append that character to an LCS of the two shorter prefixes:

`dp[i][j] = dp[i-1][j-1] + 1`.

Otherwise, at least one of the two last characters is not used, so the optimum is the better of removing the last character from either prefix:

`dp[i][j] = max(dp[i-1][j], dp[i][j-1])`.

Reconstruction starts at `(n,m)`. Equal final characters are appended and both indices decrease. On a mismatch, move to the neighbouring state with the same optimal value (the implementation prefers moving up on a tie). The collected characters are reversed at the end. Every move follows an optimal DP predecessor, so the resulting sequence is an LCS of both strings.

**Time:** `O((n+1)(m+1))`. **Auxiliary space:** `O((n+1)(m+1))` for the DP table, plus `O(n+m)` for reconstruction/output.

## 2. Minimum-cost adjacent merge (interval DP)

Let `dp[l][r]` be the minimum cost to merge all original blocks in the closed interval `[l,r]` into one block. A single block needs no merge, so `dp[i][i] = 0`.

The final operation must split the interval at some `k`: first merge `[l,k]`, then merge `[k+1,r]`, then merge those two adjacent resulting blocks. The final merge costs the sum of all original sizes in `[l,r]`. Prefix sums calculate this interval sum in constant time:

`dp[l][r] = min(dp[l][k] + dp[k+1][r] + sum(l,r))`, for `l <= k < r`.

The split giving the minimum is stored. To construct a chronological plan, recursively output the left subinterval's plan, then the right subinterval's plan, and finally `[l,k,r]`. Thus both child blocks already exist when their parent merge is emitted, and the two blocks are adjacent. Every internal interval contributes one merge, for exactly `n-1` operations.

**Time:** `O(n^3)`. **Auxiliary space:** `O(n^2)` for cost and split tables, plus `O(n)` prefix sums and plan storage. The constraints cap `n` at 200.

## 3. Maximum-weight independent set on a tree

Root the tree at vertex `0`. For each vertex `v`, define:

- `take[v]`: best total weight in the subtree if `v` is selected;
- `skip[v]`: best total weight in the subtree if `v` is not selected.

If `v` is selected, none of its children can be selected:

`take[v] = weight[v] + sum(skip[child])`.

If `v` is skipped, each child can independently use its better state:

`skip[v] = sum(max(take[child], skip[child]))`.

An iterative depth-first traversal first records a parent and an order. Processing this order backwards is a postorder traversal, ensuring children are computed before parents. Iteration avoids call-stack overflow on a chain of up to 100,000 vertices.

Reconstruction begins at the root. A vertex is selected only when its parent was not selected and `take[v] > skip[v]`. If the parent was selected, the child must be skipped. Ties are resolved by skipping; this remains optimal. Since the empty set is permitted, negative weights never force a negative result. For `n=0`, the answer is zero and the selected list is empty.

**Time:** `O(n)` because each vertex and edge is processed a constant number of times. **Auxiliary space:** `O(n)` for adjacency lists, traversal arrays, DP arrays and reconstruction stack.

## 4. Exact string search

All three search methods return every zero-based match, in increasing order, including overlaps. An empty pattern matches all `n+1` text boundaries. A pattern longer than the text has no matches.

### Naive search

Try each valid start `0..n-m`, comparing characters from left to right until a mismatch or a full match. This is the baseline implementation and does not call a built-in substring search.

**Time:** `O(n*m+n+1)` including empty-pattern output. **Auxiliary space:** `O(1)` excluding `O(z+1)` result storage.

### Knuth–Morris–Pratt (KMP)

The prefix function `pi[i]` is the length of the longest proper prefix of `pattern[0..i]` that is also a suffix of that substring. When a mismatch occurs after `j` matched characters, KMP sets `j = pi[j-1]` instead of restarting from zero. After a full match, the same fallback is applied; this is what preserves overlapping occurrences.

**Time:** `O(n+m+z+1)`. **Auxiliary space:** `O(m+1)` for the prefix function, plus `O(z+1)` output.

### Rabin–Karp (RK)

A window and the pattern are represented by polynomial hashes using base 31 and modulus 101. The highest power corresponds to the leftmost character. After checking a window, the rolling update removes the outgoing character's contribution, multiplies by the base, and adds the incoming character. A negative remainder is normalized into `[0,100]`.

The modulus is intentionally small, so equal hashes do not prove equal strings. Every hash candidate is verified character by character before its start is returned. This prevents false positives from collisions. Overlaps are considered because the window advances by one position.

**Time:** `O(n+m+c*m)` for non-empty patterns, where `c` is the number of windows with the same hash as the pattern; worst-case `O(n*m+n+m+1)`. **Auxiliary space:** `O(1)` excluding output.

## 5. Suffix array and Kasai LCP

The suffix array stores starting indices of the non-empty suffixes in lexicographic order. Prefix doubling initially ranks suffixes by their first character. In each round, suffixes are sorted by a pair of ranks: the first `width` characters and the next `width` characters. New equivalence classes are assigned, and `width` doubles until all suffixes have distinct ranks or the text is fully covered. The implementation sorts rank pairs with comparison sorting and does not create suffix strings or add a sentinel.

Kasai's algorithm builds the inverse suffix-array position. It compares each suffix with its predecessor in suffix-array order and reuses the previous LCP minus one character, when possible. `lcp[i]` is the common-prefix length of `sa[i-1]` and `sa[i]`; `lcp[0]` is zero. The maximum LCP is the longest repeated substring because any two suffixes sharing a prefix are adjacent somewhere within the lexicographically ordered group of suffixes sharing that prefix. Overlap is allowed.

**Time:** `O(n log^2(n+1))` for prefix doubling with comparison sorting, plus `O(n)` for Kasai. **Auxiliary space:** `O(n)`; output arrays also use `O(n)`.

## 6. Aho–Corasick multi-pattern counting

A trie stores all dictionary patterns. A terminal-state array maps each original dictionary position to its terminal trie state; duplicate patterns therefore share a terminal state but remain separate output columns.

Breadth-first construction computes failure links. For each state and ASCII character, missing transitions are completed using the transition of its failure state. The resulting automaton can consume each text character with one transition. Root transitions are handled separately.

For one text, the automaton state is reset to the root and a visit counter is incremented for the state reached after each character. Then states are processed in reverse breadth-first order, adding each state's visits to its failure state. This propagates counts to suffix-pattern states without walking an output list for every match. The terminal-state array converts these aggregated counts back to the original dictionary order. A fresh visit array is created for every text, so texts are independent. Duplicate entries read the same terminal count. Empty texts naturally produce zero counts; an empty dictionary produces one empty row per text.

The alphabet is fixed to printable ASCII codes 32 through 126 (95 symbols), as specified by the contract.

Let `L` be total pattern length, `S` the number of trie states including the root, `p` the number of patterns, and `q` the number of texts.

**Construction time:** `O(L + 95*S + p)`. **Per-text time:** `O(|text| + S + p)`. **Auxiliary space:** `O(95*S + p + S)` for transitions, failure links, queue and terminal mapping, plus `O(S)` visit counters per text. Output storage is `O(q*p)`.

## Reproducible validation and own tests

**Environment used for this run:** Linux x86_64 container, AMD EPYC 9V74 virtual CPU (3 CPUs exposed), approximately 5.8 GiB RAM visible to the container, OpenJDK 21.0.12. The source is compiled with `javac --release 17`, so generated bytecode targets Java 17. Python 3.13.5 runs the supplied-style launcher.

Commands executed from the repository root:

```bash
python3 tools/run.py --setup
python3 tools/run.py --check
python3 tests/student/run_student_tests.py
```

Results:
- `--setup`: PASS (Java compilation and the `ping` JSON-lines transport check).
- `--check`: PASS for all 44 supplied public cases and for the repeated two-batch state-reset check (88 additional requests in one persistent process).
- Student fixtures: PASS, 9 cases including the transport ping and the eight examples supplied with this submission.

The public checker and 44-case fixture file are included so the checks can be reproduced locally. The official grader may use separate tests; a local pass is not a guarantee of hidden-test results.

| Command | Fixture | Expected / actual result | Defect targeted |
|---|---|---|---|
| `aho` | `tests/student/own_cases.jsonl`, case 1 | `[[2,2,2],[2,2,2],[0,0,0]]` / PASS | Duplicate patterns must each receive counts; suffix matches count; each text starts with fresh counters. |
| `kmp` | case 2 | `[0,2,4]` / PASS | Overlapping matches must not be skipped after a full match. |
| `lcs` | case 3 | length 4, sequence `team` / PASS | Reconstruct a common subsequence, not a substring. |
| `merge` | case 4 | cost 14; plan `[[0,0,1],[0,1,2]]` / PASS | Merges must be adjacent and listed in chronological order. |
| `naive` | case 5 | `[0,1,2]` / PASS | Check every alignment, including overlaps. |
| `rk` | case 6 | `[0]` / PASS | Return exact matches, not hash-only candidates. |
| `suffix` | case 7 | SA `[5,3,1,0,4,2]`, LCP `[0,1,3,0,0,2]`, repeated `ana` / PASS | Lexicographic suffix ordering, predecessor LCP and repeated-substring extraction. |
| `tree` | case 8 | weight 10, vertices `[0,2]` / PASS | Select a maximum-weight independent set, not adjacent vertices. |

## Naive / KMP / Rabin–Karp comparison

The benchmark is implemented in `Java/source/Benchmark.java`; raw measurements are in `tests/student/search_benchmark.csv`. Run it after compilation with:

```bash
javac --release 17 -encoding UTF-8 -d Java/build Java/source/*.java
java -Xmx384m -cp Java/build Benchmark
```

For each `n` in 300, 1000 and 3000, `m=n/10`. Random inputs use a four-character alphabet and deterministic seed `20261107+n`; the pattern is independently generated from the same seeded generator. Repetitive inputs contain only `a`, with an all-`a` pattern. Each algorithm receives the same text and pattern. Fifteen warm-up calls are performed before the three timed calls. The timer encloses only the algorithm method call; request construction, correctness comparison and output formatting are outside the timed interval. Correctness is checked against Naive before recording each row. Values below are medians of three runs, in microseconds; raw nanoseconds are preserved in the CSV.

| Case | n | m | Algorithm | Run 1 (µs) | Run 2 (µs) | Run 3 (µs) | Median (µs) | Correct |
|---|---:|---:|---|---:|---:|---:|---:|---|
| random | 300 | 30 | Naive | 23.937 | 23.686 | 23.025 | 23.686 | yes |
| random | 300 | 30 | KMP | 26.490 | 29.104 | 26.089 | 26.490 | yes |
| random | 300 | 30 | RK | 33.230 | 35.614 | 33.751 | 33.751 | yes |
| repetitive | 300 | 30 | Naive | 98.990 | 98.599 | 184.329 | 98.990 | yes |
| repetitive | 300 | 30 | KMP | 46.220 | 45.799 | 46.831 | 46.220 | yes |
| repetitive | 300 | 30 | RK | 151.780 | 151.479 | 152.140 | 151.780 | yes |
| random | 1000 | 100 | Naive | 15.704 | 15.103 | 14.922 | 15.103 | yes |
| random | 1000 | 100 | KMP | 115.374 | 99.451 | 95.544 | 99.451 | yes |
| random | 1000 | 100 | RK | 91.158 | 92.300 | 92.870 | 92.300 | yes |
| repetitive | 1000 | 100 | Naive | 180.232 | 181.364 | 175.335 | 180.232 | yes |
| repetitive | 1000 | 100 | KMP | 233.052 | 230.528 | 166.873 | 230.528 | yes |
| repetitive | 1000 | 100 | RK | 366.434 | 364.711 | 382.178 | 366.434 | yes |
| random | 3000 | 300 | Naive | 5.008 | 4.837 | 4.697 | 4.837 | yes |
| random | 3000 | 300 | KMP | 112.550 | 109.837 | 88.724 | 109.837 | yes |
| random | 3000 | 300 | RK | 273.324 | 327.876 | 62.945 | 273.324 | yes |
| repetitive | 3000 | 300 | Naive | 1181.196 | 977.548 | 1051.320 | 1051.320 | yes |
| repetitive | 3000 | 300 | KMP | 80.872 | 81.723 | 79.761 | 80.872 | yes |
| repetitive | 3000 | 300 | RK | 2068.848 | 2076.509 | 2272.807 | 2076.509 | yes |

The random Naive cases are especially cheap when most alignments fail at the first character; this is input-dependent behavior and does not change its `O(nm)` worst-case bound. Repetitive inputs force Naive to compare long matching prefixes at nearly every alignment, while KMP remains linear. RK is slower on the repetitive cases because the small modulus creates many candidate hash matches, each of which must be verified character by character. The deliberately small modulus 101 is part of this assignment's specification and should not be generalized to production hash designs. Timing noise is visible in a few three-run samples; these measurements illustrate behavior, not prove asymptotic complexity.

The public `rk-collision` fixture (`text="bcek"`, `pattern="bc"`) specifically checks that the algorithm verifies candidate windows against the actual characters. A matching hash alone must never be reported as a match.

## Sources and AI assistance

The assignment contract, public fixtures, and launcher/validator behavior were taken from the course materials supplied for this task. OpenJDK 21 was used to compile and run the Java implementation with Java 17 compatibility mode. ChatGPT assisted with implementation, test harness construction, analysis drafting and debugging. The implementation was compiled and checked against the included 44 public cases, the repeated-batch check, the student's eight sample requests, and the recorded search comparison. The student should review the code, understand each algorithm, and be prepared to explain and independently verify it during the defence.
