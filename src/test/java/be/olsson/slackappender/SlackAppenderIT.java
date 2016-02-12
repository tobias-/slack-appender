package be.olsson.slackappender;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.Enumeration;

@SuppressWarnings("ALL")
public class SlackAppenderIT {
    public static final String SLACK_WEBHOOK = "SLACK_WEBHOOK";

    @Before
    public void setup() {
        String webhookUrl = System.getProperty(SLACK_WEBHOOK, System.getenv(SLACK_WEBHOOK));
        setWebhookUrl(webhookUrl);
    }

    public void setWebhookUrl(String webhookUrl) {
        Logger logger = Logger.getRootLogger();
        for (Enumeration appenders = logger.getAllAppenders(); appenders.hasMoreElements(); ) {
            Appender appender = (Appender) appenders.nextElement();
            if (appender instanceof SlackAppender) {
                ((SlackAppender)appender).setWebhookUrl(webhookUrl);
            }
        }
    }

    @Test
    public void sendInfoWithoutStack() throws InterruptedException {
        Logger.getLogger(getClass()).warn("Test warning");
        Thread.sleep(1000);
    }

    @Test
    public void sendInfo() throws InterruptedException {
        Logger.getLogger(getClass()).warn("Test warning", new Throwable("This is a test exception"));
        Thread.sleep(1000);
    }
}
