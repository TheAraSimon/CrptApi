import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final LinkedList<Long> requestTimestamps = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition condition = lock.newCondition();

    private final long intervalMillis;
    private final int requestLimit;
    private final String baseUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final String bearerToken;

    public CrptApi(TimeUnit timeUnit, int requestLimit, String bearerToken) {
        this.intervalMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.bearerToken = bearerToken;
    }

    public void createDocument(Document document, String signature) throws InterruptedException {
        throttle();

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("document", document);
            payload.put("signature", signature);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .header("Authorization", "Bearer " + bearerToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("API error: " + response.statusCode() + " - " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to send request", e);
        }
    }

    private void throttle() throws InterruptedException {
        lock.lock();
        try {
            long now = Instant.now().toEpochMilli();

            while (true) {
                if (requestTimestamps.size() < requestLimit) {
                    requestTimestamps.addLast(now);
                    break;
                }

                long earliest = requestTimestamps.peekFirst();
                long elapsed = now - earliest;

                if (elapsed >= intervalMillis) {
                    requestTimestamps.removeFirst();
                } else {
                    long waitTime = intervalMillis - elapsed;
                    condition.await(waitTime, TimeUnit.MILLISECONDS);
                    now = Instant.now().toEpochMilli();
                }
            }

            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static class Document {
        @JsonProperty("description")
        public Description description;

        @JsonProperty("doc_id")
        public String docId;

        @JsonProperty("doc_status")
        public String docStatus;

        @JsonProperty("doc_type")
        public String docType;

        @JsonProperty("importRequest")
        public boolean importRequest;

        @JsonProperty("owner_inn")
        public String ownerInn;

        @JsonProperty("participant_inn")
        public String participantInn;

        @JsonProperty("producer_inn")
        public String producerInn;

        @JsonProperty("production_date")
        public String productionDate;

        @JsonProperty("production_type")
        public String productionType;

        @JsonProperty("products")
        public List<Product> products;

        @JsonProperty("reg_date")
        public String regDate;

        @JsonProperty("reg_number")
        public String regNumber;
    }

    public static class Description {
        @JsonProperty("participantInn")
        public String participantInn;
    }

    public static class Product {
        @JsonProperty("certificate_document")
        public String certificateDocument;

        @JsonProperty("certificate_document_date")
        public String certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        public String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        public String ownerInn;

        @JsonProperty("producer_inn")
        public String producerInn;

        @JsonProperty("production_date")
        public String productionDate;

        @JsonProperty("tnved_code")
        public String tnvedCode;

        @JsonProperty("uit_code")
        public String uitCode;

        @JsonProperty("uitu_code")
        public String uituCode;
    }
}
