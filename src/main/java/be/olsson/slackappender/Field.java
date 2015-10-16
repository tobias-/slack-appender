package be.olsson.slackappender;

import com.google.gson.annotations.SerializedName;

public class Field {
    // "Required Field Title", // The title may not contain markup and will be escaped for you
    public String title;
    // "Text value of the field. May contain standard message markup and must be escaped as normal. May be multi-line."
    public String value;
    // Optional flag indicating whether the `value` is short enough to be displayed side-by-side with other values
    @SerializedName("short")
    public Boolean shortValue;
}
