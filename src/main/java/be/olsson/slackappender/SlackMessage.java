package be.olsson.slackappender;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SlackMessage {
    // Incoming webhooks have a default channel, but it can be overridden
    public String channel;
    public String text;
    public List<Attachment> attachments;
    // We will automatically fetch and create attachments for certain well-known media URLs, but we will not unfurl links to primarily text-based content, unless you specify in your payload that you want that.
    @SerializedName("unfurl_links")
    public Boolean unfurlLinks;
    // You can override the bot icon with either
    @SerializedName("icon_url")
    public String iconUrl;
    // .. or
    @SerializedName("icon_emoji")
    public String iconEmoji;
    // You can customize the name and icon of your Incoming Webhook in the Integration Settings section below. However, you can override the displayed name
    public String username;
    public Boolean mrkdwn;
}
