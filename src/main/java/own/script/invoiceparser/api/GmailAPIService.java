package own.script.invoiceparser.api;

import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
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
    public final GmailServiceProvider provider;
    public String MESSAGE_LIST_URL = "https://gmail.googleapis.com/gmail/v1/users/{userId}/messages";

    @Autowired
    public GmailAPIService(GmailServiceProvider provider) {
        this.provider = provider;
    }

    @SneakyThrows
    @GetMapping("/listMails")
    public List<Message> listMessages(String query) {
        ListMessagesResponse response = null;
        try {
            var service = provider.getGmailService();
            response = service.users().messages().list(USER_ID)
                    .setQ(query)
                    .execute();
        }catch (IOException e) {
            log.error("Failed to establish connections with Gmail API: {}", e.getMessage(), e);
            throw e;
        }
        // Return the list of messages
        return Optional.ofNullable(response)
                .map(ListMessagesResponse::getMessages)
                .orElse(Collections.emptyList());
    }
}
