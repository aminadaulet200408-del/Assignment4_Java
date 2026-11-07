import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

public final class Main {
    private Main() {}

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            Object parsed = Json.parse(line);
            if (!(parsed instanceof Map)) {
                throw new IllegalArgumentException("Each input line must contain a JSON object");
            }
            Map<String, Object> request = (Map<String, Object>) parsed;
            Object taskValue = request.get("task");
            if (!(taskValue instanceof String)) {
                throw new IllegalArgumentException("Missing string field: task");
            }
            Map<String, Object> response = dispatch((String) taskValue, request);
            System.out.println(Json.stringify(response));
        }
    }

    private static Map<String, Object> dispatch(String task, Map<String, Object> request) {
        switch (task) {
            case "ping": return java.util.Collections.singletonMap("status", "ready");
            case "lcs": return Algorithms.lcs(request);
            case "merge": return Algorithms.merge(request);
            case "tree": return Algorithms.tree(request);
            case "naive": return Algorithms.naive(request);
            case "kmp": return Algorithms.kmp(request);
            case "rk": return Algorithms.rk(request);
            case "suffix": return Algorithms.suffix(request);
            case "aho": return Algorithms.aho(request);
            default: throw new IllegalArgumentException("Unknown task: " + task);
        }
    }
}
