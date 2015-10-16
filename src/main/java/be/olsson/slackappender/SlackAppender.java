package be.olsson.slackappender;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.*;
import com.squareup.okhttp.Request.Builder;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class SlackAppender extends AppenderSkeleton implements Appender, Closeable {
    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Map<Level, String> iconMap;
    private final Map<Level, String> colorMap;
    private URL webhookUrl;
    private String username; // e.g. "Blazkowicz";
    private String channel;
    private final Gson gson = new GsonBuilder().create();
    private boolean markdown;


    public SlackAppender() {
        // url = "https://hooks.slack.com/services/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";
        Map<Level, String> iconMap = new HashMap<>();
        iconMap.put(Level.TRACE, ":pawprints:");
        iconMap.put(Level.DEBUG, ":beetle:");
        iconMap.put(Level.INFO, ":suspect:");
        iconMap.put(Level.WARN, ":goberserk:");
        iconMap.put(Level.ERROR, ":feelsgood:");
        iconMap.put(Level.FATAL, ":finnadie:");
        this.iconMap = unmodifiableMap(iconMap);
        Map<Level, String> colorMap = new HashMap<>();
        colorMap.put(Level.TRACE, "#6f6d6d");
        colorMap.put(Level.DEBUG, "#b5dae9");
        colorMap.put(Level.INFO, "#5f9ea0");
        colorMap.put(Level.WARN, "#ff9122");
        colorMap.put(Level.ERROR, "#ff4444");
        colorMap.put(Level.FATAL, "#b03e3c");
        this.colorMap = unmodifiableMap(colorMap);
    }

    protected SlackAppender(Layout layout) {
        this();
        setLayout(layout);
        activateOptions();
    }

    @Override
    protected void append(final LoggingEvent event) {
        event.getNDC();
        event.getThreadName();
        event.getMDCCopy();
        event.getLocationInformation();
        event.getRenderedMessage();
        String logStatement = getLayout().format(event);
        SlackMessage slackMessage = new SlackMessage();
        slackMessage.channel = channel;
        slackMessage.iconEmoji = iconMap.get(event.getLevel());
        slackMessage.username = username;
        slackMessage.text = logStatement;
        slackMessage.attachments = new ArrayList<>();
        Attachment attachment = new Attachment();
        attachment.color = colorMap.get(event.getLevel());
        attachment.fallback = logStatement;
        attachment.pretext = logStatement;
        event.getThrowableStrRep();
        attachment.text = getLayout().format(event);
        if (markdown) {
            slackMessage.mrkdwn = true;
            attachment.mrkdwn_in = Arrays.asList("text", "pretext");
        }
        slackMessage.attachments.add(attachment);
        postSlackMessage(slackMessage);
    }

    protected void postSlackMessage(SlackMessage slackMessage) {
        try {
            String payload = gson.toJson(slackMessage);
            Request request = new Builder().url(webhookUrl).post(RequestBody.create(JSON, payload)).build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    response.body().string();
                }
            });
        } catch (Exception e) {
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    public String getWebhookUrl() {
        return webhookUrl == null ? null : webhookUrl.toString();
    }

    public void setWebhookUrl(String webhookUrl) {
        try {
            this.webhookUrl = new URL(webhookUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAppenderDisabled() {
        return webhookUrl == null;
    }

    public boolean isMarkdown() {
        return markdown;
    }

    public void setMarkdown(boolean markdown) {
        this.markdown = markdown;
    }
}
