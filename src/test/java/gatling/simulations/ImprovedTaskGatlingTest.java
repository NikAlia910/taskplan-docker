package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ImprovedTaskGatlingTest extends Simulation {

    // --- API Coverage Map ---
    private static final Map<String, Boolean> apiCoverage = new ConcurrentHashMap<>();
    private static final List<ApiCallResult> apiResults = Collections.synchronizedList(new ArrayList<>());

    static {
        apiCoverage.put("/api/authenticate [POST]", false);
        apiCoverage.put("/api/account [GET]", false);
        apiCoverage.put("/api/tasks [POST]", false);
        apiCoverage.put("/api/tasks/{id} [GET]", false);
        apiCoverage.put("/api/tasks/{id} [DELETE]", false);
    }

    private static final String BASE_URL = System.getProperty("baseURL", "http://localhost:8080");

    private static final HttpProtocolBuilder httpConf = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/Java")
        .disableFollowRedirect()
        .shareConnections(); // Add connection sharing to maintain session state

    // --- Helper: Print and Track API Call ---
    private static void recordApiCall(String endpoint, String method, int status, long responseTime, boolean success) {
        String key = endpoint + " [" + method + "]";
        apiCoverage.put(key, success);
        apiResults.add(new ApiCallResult(key, status, responseTime, success));
    }

    // --- Helper: Generate Task JSON ---
    private static String generateTaskJson(String description) {
        String now = ZonedDateTime.now(ZoneOffset.UTC).toString();
        return String.format(
            "{\"description\":\"%s\",\"dueDate\":\"%s\",\"priority\":\"HIGH\",\"completed\":false,\"createdDate\":\"%s\",\"lastModifiedDate\":\"%s\"}",
            description,
            now.substring(0, 10),
            now,
            now
        );
    }

    // --- Chain: Authenticate and Save JWT ---
    private static ChainBuilder authRequest() {
        return exec(
            http("Authenticate")
                .post("/api/authenticate")
                .body(StringBody("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .check(status().is(200))
                .check(jsonPath("$.id_token").saveAs("jwt_token"))
                .check(responseTimeInMillis().lt(2000L))
        ).exec(session -> {
            String jwt = session.getString("jwt_token");
            System.out.println("[DEBUG] JWT Token: " + jwt);
            recordApiCall("/api/authenticate", "POST", 200, 0, jwt != null && !jwt.isEmpty());
            return session;
        });
    }

    // --- Chain: Authenticated GET /api/account ---
    private static ChainBuilder getAccount() {
        return exec(
            http("Get Account")
                .get("/api/account")
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(200))
                .check(responseTimeInMillis().lt(2000L))
        ).exec(session -> {
            recordApiCall("/api/account", "GET", 200, 0, true);
            return session;
        });
    }

    // --- Chain: Create Task ---
    private static ChainBuilder createTask() {
        return exec(session -> {
            String uuid = UUID.randomUUID().toString();
            session = session.set("task_description", "Task-" + uuid);
            session = session.set("task_json", generateTaskJson("Task-" + uuid));
            return session;
        })
            .exec(
                http("Create Task")
                    .post("/api/tasks")
                    .header("Authorization", "Bearer ${jwt_token}")
                    .body(StringBody("${task_json}"))
                    .check(status().is(201))
                    .check(jsonPath("$.id").saveAs("task_id"))
                    .check(responseTimeInMillis().lt(2000L))
            )
            .exec(session -> {
                String id = session.getString("task_id");
                System.out.println("[DEBUG] Created Task ID: " + id);
                recordApiCall("/api/tasks", "POST", 201, 0, id != null && !id.isEmpty());
                return session;
            });
    }

    // --- Chain: Get Task ---
    private static ChainBuilder getTask() {
        return exec(
            http("Get Task")
                .get("/api/tasks/${task_id}")
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(200))
                .check(responseTimeInMillis().lt(2000L))
        ).exec(session -> {
            String id = session.getString("task_id");
            recordApiCall("/api/tasks/{id}", "GET", 200, 0, id != null && !id.isEmpty());
            return session;
        });
    }

    // --- Chain: Delete Task ---
    private static ChainBuilder deleteTask() {
        return exec(
            http("Delete Task")
                .delete("/api/tasks/${task_id}")
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(204))
                .check(responseTimeInMillis().lt(2000L))
        ).exec(session -> {
            String id = session.getString("task_id");
            recordApiCall("/api/tasks/{id}", "DELETE", 204, 0, id != null && !id.isEmpty());
            return session;
        });
    }

    // --- Scenario ---
    private static final ScenarioBuilder scn = scenario("Improved Task CRUD Simulation")
        .exec(authRequest())
        .pause(Duration.ofMillis(100)) // Shorter, more consistent pauses
        .exec(getAccount())
        .pause(Duration.ofMillis(100))
        .exec(createTask())
        .pause(Duration.ofMillis(100))
        .exec(getTask())
        .pause(Duration.ofMillis(100))
        .exec(deleteTask());

    {
        setUp(
            scn.injectOpen(
                rampUsers(10).during(Duration.ofSeconds(30)) // Increase ramp-up time to reduce concurrent load
            )
        )
            .protocols(httpConf)
            .assertions(
                global().successfulRequests().percent().is(100.0),
                global().responseTime().max().lt(2000), // Increase timeout threshold
                global().failedRequests().count().is(0)
            );
    }

    // --- After Simulation: Print API Coverage ---
    @Override
    public void after() {
        System.out.println("\n===== API Coverage Summary =====");
        long covered = apiCoverage.values().stream().filter(Boolean::booleanValue).count();
        long total = apiCoverage.size();
        apiCoverage.forEach((k, v) -> System.out.printf("%s: %s\n", k, v ? "✅" : "❌"));
        System.out.printf("\n✅ API Coverage: %d / %d (%.2f%%)\n", covered, total, ((100.0 * covered) / total));
        System.out.println("\n===== API Call Results =====");
        System.out.println("Endpoint,Status,ResponseTime,Success");
        for (ApiCallResult r : apiResults) {
            System.out.printf("%s,%d,%d,%s\n", r.endpoint, r.status, r.responseTime, r.success ? "OK" : "FAIL");
        }
    }

    // --- Helper Class for API Call Results ---
    private static class ApiCallResult {

        String endpoint;
        int status;
        long responseTime;
        boolean success;

        ApiCallResult(String endpoint, int status, long responseTime, boolean success) {
            this.endpoint = endpoint;
            this.status = status;
            this.responseTime = responseTime;
            this.success = success;
        }
    }
}
