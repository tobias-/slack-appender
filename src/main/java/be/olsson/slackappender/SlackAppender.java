package be.olsson.slackappender;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class SlackAppender extends AppenderSkeleton implements Closeable {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Callback RESPONSE_CALLBACK = new Callback() {
	@Override
	public void onFailure(Request request, IOException e) {
	    System.err.println(e.getMessage());
	}

	@Override
	public void onResponse(Response response) throws IOException {
	    response.body().string();
	}
    };

    private static class MessageStat {
	int countSinceLastLog;
	long lastLogged;
	long lastSeen;
    }

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Map<Integer, String> iconMap;
    private final Map<Integer, String> colorMap;
    private URL webhookUrl;
    private String username = "Blazkowicz";
    private String channel;
    private final Gson gson = new GsonBuilder().create();
    private boolean markdown;
    private boolean meltdownProtection = true;
    private int similarMessageSize = 50;
    private int timeBetweenSimilarLogsMs = 60000;

    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    private final LinkedHashMap<String, MessageStat> similar = new LinkedHashMap<String, MessageStat>() {
	private static final long serialVersionUID = -4974367564537005090L;

	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
	    return size() > similarMessageSize;
	}
    };

    public SlackAppender() {
	Map<Integer, String> iconMap = new HashMap<>();
	iconMap.put(Level.TRACE.toInt(), ":pawprints:");
	iconMap.put(Level.DEBUG.toInt(), ":beetle:");
	iconMap.put(Level.INFO.toInt(), ":suspect:");
	iconMap.put(Level.WARN.toInt(), ":goberserk:");
	iconMap.put(Level.ERROR.toInt(), ":feelsgood:");
	iconMap.put(Level.FATAL.toInt(), ":finnadie:");
	this.iconMap = unmodifiableMap(iconMap);
	Map<Integer, String> colorMap = new HashMap<>();
	colorMap.put(Level.TRACE.toInt(), "#6f6d6d");
	colorMap.put(Level.DEBUG.toInt(), "#b5dae9");
	colorMap.put(Level.INFO.toInt(), "#5f9ea0");
	colorMap.put(Level.WARN.toInt(), "#ff9122");
	colorMap.put(Level.ERROR.toInt(), "#ff4444");
	colorMap.put(Level.FATAL.toInt(), "#b03e3c");
	this.colorMap = unmodifiableMap(colorMap);
    }

    protected SlackAppender(Layout layout) {
	this();
	setLayout(layout);
	activateOptions();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    protected void append(final LoggingEvent event) {
	if (!isAppenderDisabled() && event.getMessage() != null) {
	    SlackAppender.MessageStat stat = getMessageStat(event);

	    if (stat == null || System.currentTimeMillis() - stat.lastLogged > timeBetweenSimilarLogsMs) {
		event.getNDC();
		event.getThreadName();
		event.getMDCCopy();
		event.getLocationInformation();
		event.getRenderedMessage();
		String logStatement = getLayout().format(event);
		SlackMessage slackMessage = new SlackMessage();
		slackMessage.channel = channel;
		slackMessage.iconEmoji = iconMap.get(event.getLevel().toInt());
		slackMessage.username = username;
		slackMessage.attachments = new ArrayList<>();
		Attachment attachment = new Attachment();
		attachment.color = colorMap.get(event.getLevel().toInt());
		attachment.fallback = logStatement;
		event.getThrowableStrRep();
		StringWriter stringWriter = new StringWriter();
		appendMutedMessages(stat, stringWriter);
		slackMessage.text = logStatement;
		stringWriter.append(event.getRenderedMessage()).append('\n');
		if (event.getThrowableInformation() != null) {
		    event.getThrowableInformation().getThrowable().printStackTrace(new PrintWriter(stringWriter));
		}
		attachment.text = stringWriter.toString();
		if (markdown) {
		    slackMessage.mrkdwn = true;
		    attachment.mrkdwn_in = singletonList("text");
		}
		slackMessage.attachments.add(attachment);
		postSlackMessage(slackMessage);
	    }
	}
    }

    private void appendMutedMessages(SlackAppender.MessageStat stat, StringWriter stringWriter) {
	if (meltdownProtection) {
	    if (stat.countSinceLastLog > 1) {
		stringWriter.append("Message was repeated ").append(String.valueOf(stat.countSinceLastLog)).append(" since last logging of message\n");
		stat.countSinceLastLog = 0;
		stat.lastLogged = System.currentTimeMillis();
	    }
	}
    }

    private MessageStat getMessageStat(LoggingEvent event) {
	if (meltdownProtection) {
	    String key = event.getMessage().toString();
	    MessageStat stat = similar.get(key);
	    if (stat == null) {
		stat = new MessageStat();
		stat.countSinceLastLog = 0;
		stat.lastLogged = System.currentTimeMillis();
		stat.lastSeen = System.currentTimeMillis();
	    }
	    similar.put(key, stat);

	    stat.countSinceLastLog++;
	    stat.lastSeen = System.currentTimeMillis();
	}
	return null;
    }

    protected void postSlackMessage(SlackMessage slackMessage) {
	try {
	    String payload = gson.toJson(slackMessage);
	    Request request = new Builder().url(webhookUrl).post(RequestBody.create(JSON, payload)).build();
	    Call call = okHttpClient.newCall(request);
	    call.enqueue(RESPONSE_CALLBACK);
	} catch (Exception e) {
	    e.printStackTrace();
	    // Not much to do. Can't really log it via log4j
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
	if (webhookUrl == null || webhookUrl.isEmpty()) {
	    this.webhookUrl = null;
	} else {
	    try {
		this.webhookUrl = new URL(webhookUrl);
	    } catch (MalformedURLException e) {
		throw new IllegalArgumentException(e);
	    }
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

    public boolean isMeltdownProtection() {
	return meltdownProtection;
    }

    public void setMeltdownProtection(boolean meltdownProtection) {
	this.meltdownProtection = meltdownProtection;
    }

    public int getSimilarMessageSize() {
	return similarMessageSize;
    }

    public void setSimilarMessageSize(int similarMessageSize) {
	this.similarMessageSize = similarMessageSize;
    }

    public int getTimeBetweenSimilarLogsMs() {
	return timeBetweenSimilarLogsMs;
    }

    public void setTimeBetweenSimilarLogsMs(int timeBetweenSimilarLogsMs) {
	this.timeBetweenSimilarLogsMs = timeBetweenSimilarLogsMs;
    }
}
