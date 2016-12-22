package be.olsson.slackappender.client;

import java.net.URL;

public interface Client {
    void send(URL webhookUrl, String payload);
}
