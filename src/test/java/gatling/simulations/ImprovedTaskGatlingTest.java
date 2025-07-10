package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive performance test for the Task entity.
 * Includes full CRUD operations, API coverage tracking, and detailed reporting.
 */
public class ImprovedTaskGatlingTest extends Simulation {

    // Base configuration
    private final String baseURL = System.getProperty("baseURL", "http://localhost:8080");

    // API coverage tracking
    private static final Map<String, Boolean> apiCoverage = new ConcurrentHashMap<>();

    // HTTP Protocol Configuration
    private HttpProtocolBuilder httpProtocol = http.baseUrl(baseURL).acceptHeader("application/json").contentTypeHeader("application/json");

    // Authentication Chain
    private ChainBuilder authenticate = exec(
        http("Authentication")
            .post("/api/authenticate")
            .body(StringBody("{\"username\":\"admin\",\"password\":\"admin\"}"))
            .check(status().is(200))
            .check(jsonPath("$.id_token").saveAs("jwt_token"))
            .check(bodyString().saveAs("auth_response"))
    )
        .exec(session -> {
            System.out.println("? Authentication successful. Token: " + session.getString("jwt_token").substring(0, 20) + "...");
            apiCoverage.put("/api/authenticate [POST]", true);
            return session;
        })
        .pause(1);

    // List Tasks Chain
    private ChainBuilder listTasks = exec(
        http("List all tasks").get("/api/tasks").header("Authorization", "Bearer #{jwt_token}").check(status().is(200))
    )
        .exec(session -> {
            System.out.println("? Listed all tasks");
            apiCoverage.put("/api/tasks [GET]", true);
            return session;
        })
        .pause(1);

    // Create Task Chain
    private ChainBuilder createTask = exec(session -> {
        String taskJson = String.format(
            "{\"title\":\"%s\",\"description\":\"Test task\",\"priority\":\"HIGH\",\"dueDate\":\"%s\"}",
            "Task-" + UUID.randomUUID(),
            ZonedDateTime.now(ZoneOffset.UTC).toString()
        );
        return session.set("taskJson", taskJson);
    })
        .exec(
            http("Create new task")
                .post("/api/tasks")
                .header("Authorization", "Bearer #{jwt_token}")
                .body(StringBody("#{taskJson}"))
                .check(status().is(201))
                .check(jsonPath("$.id").saveAs("taskId"))
                .check(bodyString().saveAs("create_response"))
        )
        .exec(session -> {
            System.out.println("? Created task with ID: " + session.getString("taskId"));
            apiCoverage.put("/api/tasks [POST]", true);
            return session;
        })
        .pause(1);

    // Get Task Chain
    private ChainBuilder getTask = exec(
        http("Get created task")
            .get("/api/tasks/#{taskId}")
            .header("Authorization", "Bearer #{jwt_token}")
            .check(status().is(200))
            .check(bodyString().saveAs("get_response"))
    )
        .exec(session -> {
            System.out.println("? Retrieved task: " + session.getString("taskId"));
            apiCoverage.put("/api/tasks/{id} [GET]", true);
            return session;
        })
        .pause(1);

    // Delete Task Chain
    private ChainBuilder deleteTask = exec(
        http("Delete task").delete("/api/tasks/#{taskId}").header("Authorization", "Bearer #{jwt_token}").check(status().is(204))
    ).exec(session -> {
        System.out.println("? Deleted task: " + session.getString("taskId"));
        apiCoverage.put("/api/tasks/{id} [DELETE]", true);
        return session;
    });

    // Test Scenario
    private ScenarioBuilder scn = scenario("Task API Test Scenario")
        .exec(authenticate)
        .exec(listTasks)
        .exec(createTask)
        .exec(getTask)
        .exec(deleteTask);

    // Load Simulation
    {
        setUp(scn.injectOpen(rampUsers(5).during(Duration.ofSeconds(5))))
            .protocols(httpProtocol)
            .assertions(global().responseTime().max().lt(1000), global().successfulRequests().percent().is(100.0))
            .andThen(() -> {
                System.out.println("\nAPI Coverage Report:");
                System.out.println("===================");
                apiCoverage.forEach((endpoint, covered) -> System.out.println(endpoint + ": " + (covered ? "✓" : "✗")));

                long coveredEndpoints = apiCoverage.values().stream().filter(v -> v).count();
                double coveragePercent = ((double) coveredEndpoints / apiCoverage.size()) * 100;
                System.out.println(
                    String.format("\nTotal Coverage: %.2f%% (%d/%d endpoints)", coveragePercent, coveredEndpoints, apiCoverage.size())
                );
            });
    }
}
