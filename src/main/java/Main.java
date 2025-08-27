import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        String token = "токен";

        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10, token);

        CrptApi.Document document = new CrptApi.Document();
        document.docId = UUID.randomUUID().toString();
        document.docStatus = "DRAFT";
        document.docType = "LP_INTRODUCE_GOODS";
        document.importRequest = false;
        document.ownerInn = "1234567890";
        document.participantInn = "1234567890";
        document.producerInn = "1234567890";
        document.productionDate = "2025-01-01";
        document.productionType = "OWN_PRODUCTION";
        document.regDate = "2025-01-02";
        document.regNumber = "DOC12345";

        CrptApi.Description description = new CrptApi.Description();
        description.participantInn = "1234567890";
        document.description = description;

        CrptApi.Product product = new CrptApi.Product();
        product.certificateDocument = "CERT";
        product.certificateDocumentDate = "2024-12-31";
        product.certificateDocumentNumber = "CERT123";
        product.ownerInn = "1234567890";
        product.producerInn = "1234567890";
        product.productionDate = "2025-01-01";
        product.tnvedCode = "1234567890";
        product.uitCode = "CODE0001";
        product.uituCode = "UITU0002";

        document.products = List.of(product);

        String signature = "подпись";

        api.createDocument(document, signature);
    }
}
