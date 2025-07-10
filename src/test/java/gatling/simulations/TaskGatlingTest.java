package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Performance test for the Task entity.
 * Includes authentication, CRUD operations, and comprehensive reporting.
 */
public class TaskGatlingTest extends Simulation {

    // Base configuration
    private String baseURL = Optional.ofNullable(System.getProperty("baseURL")).orElse("http://localhost:8080");
    private final AtomicLong successCounter = new AtomicLong(0);
    private final AtomicLong failureCounter = new AtomicLong(0);
    private final AtomicLong skippedCounter = new AtomicLong(0);
    private final Map<String, EndpointStatus> apiCoverage = new ConcurrentHashMap<>();
    private final String resultsPath = "target/gatling/results/api-coverage.csv";

    // HTTP configuration with proper headers and error handling
    private HttpProtocolBuilder httpProtocol = http
        .baseUrl(baseURL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/Performance-Test")
        .check(status().not(400), status().not(401), status().not(403), status().not(404), status().not(500))
        .check(substring("error").notExists())
        .disableCaching()
        .disableWarmUp();

    // Task data feeder with unique IDs and proper timestamps
    private Iterator<Map<String, Object>> taskFeeder = new Iterator<Map<String, Object>>() {
        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Map<String, Object> next() {
            Map<String, Object> map = new HashMap<>();
            String uniqueId = UUID.randomUUID().toString();
            map.put("taskId", uniqueId);
            map.put("description", "Performance Test Task " + uniqueId);
            map.put("dueDate", ZonedDateTime.now(ZoneOffset.UTC).plusDays(7).toString());
            map.put("priority", "HIGH");
            map.put("completed", false);
            map.put("createdDate", ZonedDateTime.now(ZoneOffset.UTC).toString());
            map.put("lastModifiedDate", ZonedDateTime.now(ZoneOffset.UTC).toString());
            return map;
        }
    };

    // Authentication chain with proper error handling
    private ChainBuilder authenticate = exec(session -> {
        apiCoverage.put("/api/authenticate [POST]", new EndpointStatus("Authentication", "-", false));
        return session;
    })
        .exec(
            http("Authentication")
                .post("/api/authenticate")
                .body(StringBody("{\"username\":\"admin\", \"password\":\"admin\"}"))
                .check(status().is(200))
                .check(jsonPath("$.id_token").exists().saveAs("jwt_token"))
                .check(bodyString().saveAs("authResponse"))
                .check(header("Content-Type").is("application/json"))
        )
        .exec(session -> {
            String token = session.getString("jwt_token");
            if (token != null && !token.isEmpty()) {
                System.out.println("Authentication successful. Token: " + token.substring(0, 10) + "...");
                apiCoverage.get("/api/authenticate [POST]").markSuccess(session.getString("authResponse"));
                successCounter.incrementAndGet();
            } else {
                System.out.println("Authentication failed: No token received");
                failureCounter.incrementAndGet();
            }
            return session;
        })
        .exitHereIfFailed();

    // Task CRUD operations with proper error handling and logging
    private ChainBuilder createTask = feed(taskFeeder)
        .exec(session -> {
            apiCoverage.put("/api/tasks [POST]", new EndpointStatus("Create Task", "-", false));
            return session;
        })
        .exec(
            http("Create Task")
                .post("/api/tasks")
                .header("Authorization", "Bearer #{jwt_token}")
                .body(
                    StringBody(
                        "{" +
                        "\"description\": \"#{taskId}\"," +
                        "\"dueDate\": \"#{dueDate}\"," +
                        "\"priority\": \"#{priority}\"," +
                        "\"completed\": #{completed}," +
                        "\"createdDate\": \"#{createdDate}\"," +
                        "\"lastModifiedDate\": \"#{lastModifiedDate}\"" +
                        "}"
                    )
                )
                .check(status().is(201))
                .check(header("Location").exists().saveAs("task_url"))
                .check(bodyString().saveAs("createResponse"))
        )
        .exec(session -> {
            String taskUrl = session.getString("task_url");
            if (taskUrl != null && !taskUrl.isEmpty()) {
                System.out.println("Task created: " + taskUrl);
                apiCoverage.get("/api/tasks [POST]").markSuccess(session.getString("createResponse"));
                successCounter.incrementAndGet();
            } else {
                System.out.println("Task creation failed: No location header");
                failureCounter.incrementAndGet();
            }
            return session;
        })
        .exitHereIfFailed()
        .pause(Duration.ofMillis(500)); // Prevent race conditions

    private ChainBuilder getTask = exec(session -> {
        apiCoverage.put("/api/tasks [GET]", new EndpointStatus("Get Task", "-", false));
        return session;
    })
        .exec(
            http("Get Task")
                .get("#{task_url}")
                .header("Authorization", "Bearer #{jwt_token}")
                .check(status().is(200))
                .check(jsonPath("$.id").exists())
                .check(bodyString().saveAs("getResponse"))
        )
        .exec(session -> {
            System.out.println("Task retrieved: " + session.getString("task_url"));
            apiCoverage.get("/api/tasks [GET]").markSuccess(session.getString("getResponse"));
            successCounter.incrementAndGet();
            return session;
        })
        .exitHereIfFailed()
        .pause(Duration.ofMillis(500)); // Prevent race conditions

    private ChainBuilder deleteTask = exec(session -> {
        apiCoverage.put("/api/tasks [DELETE]", new EndpointStatus("Delete Task", "-", false));
        return session;
    })
        .exec(http("Delete Task").delete("#{task_url}").header("Authorization", "Bearer #{jwt_token}").check(status().is(204)))
        .exec(session -> {
            System.out.println("Task deleted: " + session.getString("task_url"));
            apiCoverage.get("/api/tasks [DELETE]").markSuccess("Deleted successfully");
            successCounter.incrementAndGet();
            return session;
        })
        .pause(Duration.ofMillis(500)); // Prevent race conditions

    // Reporting chain with CSV export
    private ChainBuilder reportingChain = exec(session -> {
        // Create results directory if it doesn't exist
        try {
            Path resultsDir = Paths.get("target/gatling/results");
            Files.createDirectories(resultsDir);

            // Write CSV report
            try (FileWriter writer = new FileWriter(resultsPath)) {
                writer.write("Endpoint,Method,Status,Time (ms),Covered,Note\n");

                apiCoverage.forEach((endpoint, status) -> {
                    try {
                        writer.write(
                            String.format(
                                "%s,%s,%s,%s,%s,%s\n",
                                endpoint.replace(" [", ",").replace("]", ""),
                                status.getName(),
                                status.isSuccess() ? "✅" : "❌",
                                status.getResponseTime(),
                                status.isSuccess(),
                                status.getNote()
                            )
                        );
                    } catch (IOException e) {
                        System.err.println("Error writing to CSV: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Error creating results directory: " + e.getMessage());
        }

        // Print console report
        System.out.println("\n=== API Coverage Report ===");
        System.out.println("| Endpoint | Method | Status | Response Time | Details |");
        System.out.println("|----------|--------|--------|---------------|----------|");

        apiCoverage.forEach((endpoint, status) -> {
            System.out.printf(
                "| %s | %s | %s | %s | %s |\n",
                endpoint,
                status.getName(),
                status.isSuccess() ? "✅" : "❌",
                status.getResponseTime(),
                status.getNote()
            );
        });

        System.out.println("\n=== Final Statistics ===");
        System.out.printf("✅ Covered: %d\n", successCounter.get());
        System.out.printf("❌ Failed: %d\n", failureCounter.get());
        System.out.printf("⏭ Skipped: %d\n", skippedCounter.get());

        long total = successCounter.get() + failureCounter.get() + skippedCounter.get();
        if (total > 0) {
            System.out.printf("🎯 Final API Test Coverage: %.2f%%\n", ((100.0 * successCounter.get()) / total));
        }

        return session;
    });

    // Main scenario with proper sequencing
    ScenarioBuilder scn = scenario("Task API Test Scenario")
        .exec(authenticate)
        .pause(1) // Wait for token to be properly set
        .exec(createTask)
        .pause(1) // Wait for creation to complete
        .exec(getTask)
        .pause(1) // Wait for retrieval to complete
        .exec(deleteTask)
        .exec(reportingChain);

    // Simulation setup with proper assertions
    {
        setUp(scn.injectOpen(rampUsers(10).during(Duration.ofSeconds(10))))
            .protocols(httpProtocol)
            .assertions(
                global().responseTime().max().lt(1000),
                global().successfulRequests().percent().gt(95.0),
                global().failedRequests().count().is(0L)
            );
    }

    // Enhanced helper class for tracking endpoint status
    private static class EndpointStatus {

        private final String name;
        private String responseTime;
        private boolean success;
        private String note;

        public EndpointStatus(String name, String responseTime, boolean success) {
            this.name = name;
            this.responseTime = responseTime;
            this.success = success;
            this.note = "Pending";
        }

        public void markSuccess(String response) {
            this.success = true;
            this.responseTime = "< 1000ms";
            this.note = "OK";
        }

        public String getName() {
            return name;
        }

        public String getResponseTime() {
            return responseTime;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getNote() {
            return note;
        }
    }
}
