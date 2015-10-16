package be.olsson.slackappender;

import java.util.List;

public class Attachment {
    // "Required text summary of the attachment that is shown by clients that understand attachments but choose not to show them."
    public String fallback;
    // "Optional text that should appear within the attachment"
    public String text;
    // "Optional text that should appear above the formatted data"
    public String pretext;
    // e.g. "#36a64f", // Can either be one of 'good', 'warning', 'danger', or any hex color code
    public String color;
    // Fields are displayed in a table on the message
    public List<Field> fields;

    public List<String> mrkdwn_in;

}
