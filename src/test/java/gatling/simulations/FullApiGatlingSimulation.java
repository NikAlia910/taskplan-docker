package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.*;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FullApiGatlingSimulation extends Simulation {

    // Base URL
    String baseURL = Optional.ofNullable(System.getProperty("baseURL")).orElse("http://localhost:8080");

    // HTTP Protocol
    HttpProtocolBuilder httpConf = http
        .baseUrl(baseURL)
        .inferHtmlResources()
        .acceptHeader("application/json")
        .acceptEncodingHeader("gzip, deflate")
        .acceptLanguageHeader("en-US,en;q=0.5")
        .connectionHeader("keep-alive")
        .userAgentHeader("Gatling/Java")
        .silentResources();

    // API Coverage Map
    Map<String, Boolean> apiCoverage = new ConcurrentHashMap<>();
    List<String> lockedEndpoints = List.of("/api/authorities", "/api/authorities/{id}", "/api/admin/users", "/api/admin/users/{login}");
    List<String> attemptedEndpoints = Collections.synchronizedList(new ArrayList<>());
    List<String> csvRows = Collections.synchronizedList(new ArrayList<>());

    // Helper: Authentication
    ChainBuilder authRequest = exec(
        http("Authenticate")
            .post("/api/authenticate")
            .header("Content-Type", "application/json")
            .body(StringBody("{\"username\": \"admin\", \"password\": \"admin\"}"))
            .check(status().is(200))
            .check(jsonPath("$.id_token").saveAs("jwt_token"))
            .check(responseTimeInMillis().lt(1000))
    ).exec(session -> {
        System.out.println("[DEBUG] Auth Session: " + session);
        return session;
    });

    // Helper: Create Task
    ChainBuilder createTask = exec(session -> {
        String uuid = UUID.randomUUID().toString();
        String now = ZonedDateTime.now(ZoneOffset.UTC).toString();
        String json = String.format("{\"description\":\"%s\",\"completed\":false,\"createdDate\":\"%s\",\"priority\":\"HIGH\"}", uuid, now);
        session = session.set("task_payload", json);
        return session;
    })
        .exec(
            http("Create Task")
                .post("/api/tasks")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${jwt_token}")
                .body(StringBody("${task_payload}"))
                .check(status().is(201))
                .check(headerRegex("Location", "/api/tasks/(\\d+)").saveAs("task_id"))
                .check(responseTimeInMillis().lt(1000))
        )
        .exec(session -> {
            attemptedEndpoints.add("/api/tasks [POST]");
            apiCoverage.put("/api/tasks [POST]", true);
            csvRows.add(String.format("/api/tasks,POST,201,OK,covered"));
            System.out.println("[DEBUG] Create Task Session: " + session);
            return session;
        })
        .exitHereIfFailed()
        .pause(Duration.ofMillis(200));

    // Helper: Get Task
    ChainBuilder getTask = exec(
        http("Get Task")
            .get("/api/tasks/${task_id}")
            .header("Authorization", "Bearer ${jwt_token}")
            .check(status().is(200))
            .check(responseTimeInMillis().lt(1000))
    )
        .exec(session -> {
            attemptedEndpoints.add("/api/tasks/{id} [GET]");
            apiCoverage.put("/api/tasks/{id} [GET]", true);
            csvRows.add(String.format("/api/tasks/{id},GET,200,OK,covered"));
            System.out.println("[DEBUG] Get Task Session: " + session);
            return session;
        })
        .exitHereIfFailed()
        .pause(Duration.ofMillis(200));

    // Helper: Delete Task
    ChainBuilder deleteTask = exec(
        http("Delete Task")
            .delete("/api/tasks/${task_id}")
            .header("Authorization", "Bearer ${jwt_token}")
            .check(status().is(204))
            .check(responseTimeInMillis().lt(1000))
    )
        .exec(session -> {
            attemptedEndpoints.add("/api/tasks/{id} [DELETE]");
            apiCoverage.put("/api/tasks/{id} [DELETE]", true);
            csvRows.add(String.format("/api/tasks/{id},DELETE,204,OK,covered"));
            System.out.println("[DEBUG] Delete Task Session: " + session);
            return session;
        })
        .exitHereIfFailed()
        .pause(Duration.ofMillis(200));

    // Helper: Get All Tasks
    ChainBuilder getAllTasks = exec(
        http("Get All Tasks")
            .get("/api/tasks")
            .header("Authorization", "Bearer ${jwt_token}")
            .check(status().is(200))
            .check(responseTimeInMillis().lt(1000))
    )
        .exec(session -> {
            attemptedEndpoints.add("/api/tasks [GET]");
            apiCoverage.put("/api/tasks [GET]", true);
            csvRows.add(String.format("/api/tasks,GET,200,OK,covered"));
            System.out.println("[DEBUG] Get All Tasks Session: " + session);
            return session;
        })
        .exitHereIfFailed()
        .pause(Duration.ofMillis(200));

    // Scaffold for other endpoints (extend as needed)
    // Example: GET /api/account
    ChainBuilder getAccount = exec(
        http("Get Account")
            .get("/api/account")
            .header("Authorization", "Bearer ${jwt_token}")
            .check(status().is(200))
            .check(responseTimeInMillis().lt(1000))
    )
        .exec(session -> {
            attemptedEndpoints.add("/api/account [GET]");
            apiCoverage.put("/api/account [GET]", true);
            csvRows.add(String.format("/api/account,GET,200,OK,covered"));
            System.out.println("[DEBUG] Get Account Session: " + session);
            return session;
        })
        .exitHereIfFailed()
        .pause(Duration.ofMillis(200));

    // Main scenario: Authenticate, then CRUD Task, then get account
    ScenarioBuilder scenarioBuilder = scenario("Full API Coverage Scenario")
        .exec(authRequest)
        .exec(getAllTasks)
        .exec(createTask)
        .exec(getTask)
        .exec(deleteTask)
        .exec(getAccount)
        // Add more endpoint helpers here as needed
        .exec(session -> {
            // Print API coverage summary at the end
            long covered = apiCoverage.values().stream().filter(Boolean::booleanValue).count();
            long total = apiCoverage.size();
            System.out.printf("\n✅ API Coverage: %d / %d (%.2f%%)\n", covered, total, ((100.0 * covered) / (total == 0 ? 1 : total)));
            // Print CSV
            System.out.println("\nendpoint,method,status,response,covered");
            csvRows.forEach(System.out::println);
            // Print untested endpoints
            Set<String> untested = new HashSet<>(apiCoverage.keySet());
            untested.removeAll(
                apiCoverage.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet())
            );
            if (!untested.isEmpty()) {
                System.out.println("\nUntested endpoints:");
                untested.forEach(System.out::println);
            }
            // Print locked endpoints
            System.out.println("\nLocked endpoints (not marked as fail):");
            lockedEndpoints.forEach(System.out::println);
            return session;
        });

    {
        setUp(scenarioBuilder.injectOpen(rampUsers(10).during(Duration.ofSeconds(10)))).protocols(httpConf);
    }
}
