package own.script.invoiceparser.api;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
public class GmailAPIService {

    @Value("${auth.user.id}")
    private String USER_ID;
    private Gmail service;

    @Autowired
    @SneakyThrows
    public GmailAPIService(GmailServiceProvider provider) {
        try {
            service = provider.getGmailService();
        }catch (IOException e){
            log.error("Failed to initiate GmailServiceProvider: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/listMails")
    public ResponseEntity<List<Message>> listMessages(@RequestParam String query) throws IOException {
        ListMessagesResponse response;
        try {
            response = service.users().messages().list(USER_ID)
                    .setQ(query)
                    .execute();
            log.info("Inbound GET message: /v1/users/{}/messages Body: {}", USER_ID, response);
        }catch (IOException e) {
            log.error("Failed to GET List of messages for user: {} based on query: {}\nError: {}", USER_ID, query, e.getMessage(), e);
            throw e;
        }
        return ResponseEntity.ok(
                Optional.ofNullable(response)
                .map(ListMessagesResponse::getMessages)
                .orElse(Collections.emptyList())
        );
    }

    @GetMapping("/mail/{messageId}")
    public ResponseEntity<MessagePart> getMessagePayload(@PathVariable String messageId) throws IOException {
        Message response;
        try {
            response = service.users().messages().get(USER_ID, messageId)
                    .setFormat("full")
                    .execute();
            log.info("Inbound GET message: /v1/users/{}/messages/{} Body: {}", USER_ID, messageId, response);
        }catch (IOException e) {
            log.error("Failed to GET messageId: {}\nError: {}",messageId, e.getMessage(), e);
            throw e;
        }
        return ResponseEntity.ok(
            Optional.ofNullable(response)
                .map(Message::getPayload)
                .orElse(null)
        );
    }

    @GetMapping("/mail/{messageId}/attachment/{attachmentId}")
    public ResponseEntity<String> getAttachmentData(@PathVariable String messageId, @PathVariable String attachmentId) throws IOException {
        MessagePartBody response;
        try {
            response = service.users().messages().attachments().get(USER_ID, messageId, attachmentId)
                    .execute();
            log.info("Inbound GET message: /v1/users/{}/messages/{}/attachments/{} Body: {}", USER_ID, messageId, attachmentId, response);
        }catch (IOException e) {
            log.error("Failed to GET attachmentId: {} from messageId: {}\nError: {}", attachmentId, messageId, e.getMessage(), e);
            throw e;
        }
        return ResponseEntity.ok(
                Optional.ofNullable(response)
                .map(MessagePartBody::decodeData)
                .map(String::new)
                .orElse(null)
        );
    }
}
