package own.script.invoiceparser;

import com.google.api.services.gmail.model.MessagePart;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import own.script.invoiceparser.api.GmailAPIService;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
public class GmailProcessingService {
    public final GmailAPIService gmailAPIService;
    @Value("${gmail.invoice.search.query}")
    public String SEARCH_QUERY;

    @Value("${file.path.invoice.download}")
    private String INVOICE_OUTPUT_LOCATION;

    public GmailProcessingService(GmailAPIService gmailAPIService) {
        this.gmailAPIService = gmailAPIService;
    }
    @SneakyThrows
    public void processInvoices() {
        var messages = gmailAPIService.listMessages(SEARCH_QUERY).getBody();
        if(messages == null || messages.isEmpty()) {
            log.warn("No messages found for query {}", SEARCH_QUERY);
            return;
        }
        messages.forEach(message -> {
            log.debug("Processing message {}", message.getId());
            processMessagePayload(message.getId());
        });
    }

    @SneakyThrows
    private void processMessagePayload(String messageId) {
        var messagePayload = gmailAPIService.getMessagePayload(messageId).getBody();
        Optional.ofNullable(messagePayload)
                .map(MessagePart::getParts)
                .ifPresentOrElse(parts -> {
                    if(parts.isEmpty()) {
                        log.warn("No message part found for id {}", messageId);
                    }
                    parts.forEach(part ->
                            processMessageAttachment(messageId, part.getBody().getAttachmentId(), part.getFilename()));
                }, () -> log.warn("No message part found for id {}", messageId));
    }

    private void processMessageAttachment(String messageId, String attachmentId, String filename) {
        log.debug("Processing attachmentId: {} filename: {}", attachmentId, filename);
        try {
            var attachmentData = gmailAPIService.getAttachmentData(messageId, attachmentId).getBody();
            if(attachmentData == null || attachmentData.isEmpty()) {
                log.error("No content for attachmentId {}", attachmentId);
                return;
            }
            downloadAttachment(filename, attachmentData);
        }catch (IOException e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
        }
    }

    private void downloadAttachment(String filename, String attachmentData) throws IOException {
        String filaPath = INVOICE_OUTPUT_LOCATION + filename;
        log.info("Downloading {} ...", filaPath);
        try (var fos = new FileOutputStream(filaPath)) {
            fos.write(attachmentData.getBytes());
        }catch (FileNotFoundException e) {
            log.error("File {} not found: {}", filename, e.getMessage(), e);
        }
        log.info("File {} downloaded successfully.", filename);
    }
}
