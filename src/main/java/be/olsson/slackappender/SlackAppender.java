package be.olsson.slackappender;

import static java.util.Collections.unmodifiableMap;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

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

@Plugin(name = "Slack", category = Node.CATEGORY, elementType = AbstractAppender.ELEMENT_TYPE, printObject = true)
public class SlackAppender extends AbstractAppender implements Closeable {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Callback RESPONSE_CALLBACK = new Callback() {
	@Override
	public void onFailure(Request request, IOException e) {
	    e.printStackTrace();
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
    private String username;
    private String channel;
    private final Gson gson = new GsonBuilder().create();
    private boolean meltdownProtection;
    private int similarMessageSize;
    private int timeBetweenSimilarLogsMs;
    private int unmodifiedFirstLines = 5;
    private List<String> indentedPackagesToMute = Collections.emptyList();

    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    private final LinkedHashMap<String, MessageStat> similar = new LinkedHashMap<String, MessageStat>() {
	private static final long serialVersionUID = -4974367564537005090L;

	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
	    return size() > similarMessageSize;
	}
    };

    private SlackAppender(String name,
			  Filter filter,
			  Layout<? extends Serializable> layout,
			  final URL webhookUrl,
			  final String username,
			  final String channel,
			  final boolean meltdownProtection,
			  final int similarMessageSize,
			  final int timeBetweenSimilarLogsMs) {
	super(name, filter, layout, true);
	setWebhookUrl(webhookUrl);
	this.username = username;
	this.channel = channel;
	this.meltdownProtection = meltdownProtection;
	this.similarMessageSize = similarMessageSize;
	this.timeBetweenSimilarLogsMs = timeBetweenSimilarLogsMs;
	Map<Integer, String> iconMap = new HashMap<>();
	iconMap.put(Level.TRACE.intLevel(), ":pawprints:");
	iconMap.put(Level.DEBUG.intLevel(), ":beetle:");
	iconMap.put(Level.INFO.intLevel(), ":suspect:");
	iconMap.put(Level.WARN.intLevel(), ":goberserk:");
	iconMap.put(Level.ERROR.intLevel(), ":feelsgood:");
	iconMap.put(Level.FATAL.intLevel(), ":finnadie:");
	this.iconMap = unmodifiableMap(iconMap);
	Map<Integer, String> colorMap = new HashMap<>();
	colorMap.put(Level.TRACE.intLevel(), "#6f6d6d");
	colorMap.put(Level.DEBUG.intLevel(), "#b5dae9");
	colorMap.put(Level.INFO.intLevel(), "#5f9ea0");
	colorMap.put(Level.WARN.intLevel(), "#ff9122");
	colorMap.put(Level.ERROR.intLevel(), "#ff4444");
	colorMap.put(Level.FATAL.intLevel(), "#b03e3c");
	this.colorMap = unmodifiableMap(colorMap);
    }

    private class FilteredPrintWriter extends PrintWriter {
	private boolean wroteEllipsize = false;
	private int lineCount = 0;

	private FilteredPrintWriter(final Writer out) {
	    super(out);
	}

	@Override
	public void println(final Object objectToPrint) {
	    lineCount++;
	    String line = String.valueOf(objectToPrint);
	    if (lineCount >= unmodifiedFirstLines && !indentedPackagesToMute.isEmpty()) {
		for (String packageToMute : indentedPackagesToMute) {
		    if (line.startsWith(packageToMute)) {
			if (!wroteEllipsize) {
			    super.println("\t...");
			}
			wroteEllipsize = true;
			return;
		    }
		}
	    }
	    super.println(line.replace(' ', '\u00A0'));
	}
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void append(final LogEvent event) {
	if (!isAppenderDisabled() && event.getMessage() != null) {
	    SlackAppender.MessageStat stat = getMessageStat(event);
	    if (stat == null || System.currentTimeMillis() - stat.lastLogged > timeBetweenSimilarLogsMs) {
		String logStatement = event.getMessage().getFormat();
		SlackMessage slackMessage = new SlackMessage();
		slackMessage.channel = channel;
		slackMessage.iconEmoji = iconMap.get(event.getLevel().intLevel());
		slackMessage.username = username;
		slackMessage.attachments = new ArrayList<>();
		Attachment attachment = new Attachment();
		attachment.color = colorMap.get(event.getLevel().intLevel());
		attachment.fallback = logStatement;
		StringWriter stringWriter = new StringWriter();
		appendMutedMessages(stat, stringWriter);
		slackMessage.text = attachment.fallback;
		stringWriter.append(event.getMessage().getFormattedMessage());
		if (event.getThrown() != null) {
		    event.getThrown().printStackTrace(new FilteredPrintWriter(stringWriter));
		}
		attachment.text = stringWriter.toString();
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

    private synchronized MessageStat getMessageStat(LogEvent event) {
	if (meltdownProtection) {
	    String key = event.getMessage().getFormattedMessage();
	    MessageStat stat = similar.get(key);
	    if (stat == null) {
		stat = new MessageStat();
	    }
	    similar.put(key, stat);

	    stat.countSinceLastLog++;
	    stat.lastSeen = System.currentTimeMillis();
	    return stat;
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

    public URL getWebhookUrl() {
	return webhookUrl;
    }

    public void setWebhookUrl(URL webhookUrl) {
	this.webhookUrl = webhookUrl;
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

    public void setPackagesToMute(final String packagesToMute) {
	this.indentedPackagesToMute = new LinkedList<>();
	for (String packageToMute : packagesToMute.split(",")) {
	    indentedPackagesToMute.add("\tat " + packageToMute);
	}
    }

    @PluginFactory
    public static SlackAppender createAppender(
	    @PluginAttribute("name") String name,
	    @PluginElement("Layout") Layout<? extends Serializable> layout,
	    @PluginElement("Filter") final Filter filter,
	    @PluginAttribute("webhookUrl") URL webhookUrl,
	    @PluginAttribute("channel") String channel,
	    @PluginAttribute(value = "username", defaultString = "Blazkowicz") String username,
	    @PluginAttribute(value = "meltdownProtection", defaultBoolean = true) boolean meltdownProtection,
	    @PluginAttribute(value = "similarMessageSize", defaultInt = 50) int similarMessageSize,
	    @PluginAttribute(value = "timeBetweenSimilarLogsMs", defaultInt = 60000) int timeBetweenSimilarLogsMs) {
	if (name == null) {
	    LOGGER.error("No name provided for MyCustomAppenderImpl");
	    return null;
	}
	if (layout == null) {
	    layout = PatternLayout.createDefaultLayout();
	}
	return new SlackAppender(name, filter, layout, webhookUrl, username, channel, meltdownProtection, similarMessageSize, timeBetweenSimilarLogsMs);
    }

}
