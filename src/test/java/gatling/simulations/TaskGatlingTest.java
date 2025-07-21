package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.*;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TaskGatlingTest extends Simulation {

    // --- API Coverage Map ---
    private static final Map<String, Boolean> apiCoverage = new ConcurrentHashMap<>();
    private static final List<String> endpoints = List.of(
        "/api/authenticate [POST]",
        "/api/tasks [POST]",
        "/api/tasks [GET]",
        "/api/tasks/{id} [GET]",
        "/api/tasks/{id} [DELETE]",
        "/api/account [GET]",
        "/api/users [GET]",
        "/api/admin/users [GET]"
        // Add more endpoints as needed for full coverage
    );

    static {
        endpoints.forEach(e -> apiCoverage.put(e, false));
    }

    private static final String BASE_URL = System.getProperty("baseURL", "http://localhost:8080");

    private final HttpProtocolBuilder httpConf = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/Taskplan")
        .disableFollowRedirect();

    // --- Helper: Auth Request ---
    private ChainBuilder authRequest() {
        return exec(
            http("Authenticate")
                .post("/api/authenticate")
                .body(StringBody("{" + "\"username\":\"admin\"," + "\"password\":\"admin\"}"))
                .check(status().is(200))
                .check(jsonPath("$.id_token").saveAs("jwt_token"))
                .check(responseTimeInMillis().lt(1000))
        ).exec(session -> {
            if (session.contains("jwt_token")) {
                String token = session.getString("jwt_token");
                System.out.println("[DEBUG] JWT Token: " + token);
                apiCoverage.put("/api/authenticate [POST]", true);
            }
            return session;
        });
    }

    // --- Helper: Create Task ---
    private ChainBuilder createTask() {
        return exec(session -> {
            String uuid = UUID.randomUUID().toString();
            String now = ZonedDateTime.now(ZoneOffset.UTC).toString();
            String payload = String.format(
                "{\"description\":\"Task-%s\",\"dueDate\":\"%s\",\"priority\":\"HIGH\",\"completed\":false,\"createdDate\":\"%s\"}",
                uuid,
                now.substring(0, 10),
                now
            );
            return session.set("task_payload", payload);
        })
            .exec(
                http("Create Task")
                    .post("/api/tasks")
                    .header("Authorization", "Bearer ${jwt_token}")
                    .body(StringBody("${task_payload}"))
                    .check(status().is(201))
                    .check(headerRegex("Location", "/api/tasks/(\\d+)").saveAs("task_id"))
                    .check(responseTimeInMillis().lt(1000))
            )
            .exec(session -> {
                if (session.contains("task_id")) {
                    String id = session.getString("task_id");
                    System.out.println("[DEBUG] Created Task ID: " + id);
                    apiCoverage.put("/api/tasks [POST]", true);
                }
                return session;
            })
            .pause(1);
    }

    // --- Helper: Get Task ---
    private ChainBuilder getTask() {
        return exec(
            http("Get Task")
                .get(session -> "/api/tasks/" + session.getString("task_id"))
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(200))
                .check(responseTimeInMillis().lt(1000))
        )
            .exec(session -> {
                if (session.contains("task_id")) {
                    apiCoverage.put("/api/tasks/{id} [GET]", true);
                }
                return session;
            })
            .pause(1);
    }

    // --- Helper: Delete Task ---
    private ChainBuilder deleteTask() {
        return exec(
            http("Delete Task")
                .delete(session -> "/api/tasks/" + session.getString("task_id"))
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(204))
                .check(responseTimeInMillis().lt(1000))
        )
            .exec(session -> {
                if (session.contains("task_id")) {
                    apiCoverage.put("/api/tasks/{id} [DELETE]", true);
                }
                return session;
            })
            .pause(1);
    }

    // --- Helper: Get All Tasks ---
    private ChainBuilder getAllTasks() {
        return exec(
            http("Get All Tasks")
                .get("/api/tasks")
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(200))
                .check(responseTimeInMillis().lt(1000))
        )
            .exec(session -> {
                apiCoverage.put("/api/tasks [GET]", true);
                return session;
            })
            .pause(1);
    }

    // --- Helper: Get Account ---
    private ChainBuilder getAccount() {
        return exec(
            http("Get Account")
                .get("/api/account")
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(200))
                .check(responseTimeInMillis().lt(1000))
        )
            .exec(session -> {
                apiCoverage.put("/api/account [GET]", true);
                return session;
            })
            .pause(1);
    }

    // --- Helper: Get Public Users ---
    private ChainBuilder getPublicUsers() {
        return exec(http("Get Public Users").get("/api/users").check(status().is(200)).check(responseTimeInMillis().lt(1000)))
            .exec(session -> {
                apiCoverage.put("/api/users [GET]", true);
                return session;
            })
            .pause(1);
    }

    // --- Helper: Get Admin Users ---
    private ChainBuilder getAdminUsers() {
        return exec(
            http("Get Admin Users")
                .get("/api/admin/users")
                .header("Authorization", "Bearer ${jwt_token}")
                .check(status().is(200))
                .check(responseTimeInMillis().lt(1000))
        )
            .exec(session -> {
                apiCoverage.put("/api/admin/users [GET]", true);
                return session;
            })
            .pause(1);
    }

    private static void printApiCoverageSummary() {
        System.out.println("--- API Coverage Summary ---");
        int covered = 0;
        for (String endpoint : endpoints) {
            Boolean ok = apiCoverage.get(endpoint);
            String status = (ok != null && ok) ? "✅" : "❌";
            if (ok != null && ok) covered++;
            System.out.println(endpoint + ": " + status);
        }
        double percent = (100.0 * covered) / endpoints.size();
        System.out.printf("API Coverage: %d / %d (%.2f%%)\n", covered, endpoints.size(), percent);
    }

    // --- Scenario ---
    private final ScenarioBuilder scn = scenario("Full API Coverage Scenario")
        .exec(authRequest())
        .exec(getAccount())
        .exec(getPublicUsers())
        .exec(getAdminUsers())
        .exec(getAllTasks())
        .exec(createTask())
        .exec(getTask())
        .exec(deleteTask())
        .exec(session -> {
            // Print API coverage summary at the end of the scenario
            printApiCoverageSummary();
            return session;
        });

    {
        setUp(scn.injectOpen(rampUsers(10).during(Duration.ofSeconds(10)))).protocols(httpConf);
    }
}
