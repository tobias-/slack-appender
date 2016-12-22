package be.olsson.slackappender.client;

import java.io.IOException;
import java.net.URL;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class OkHttp2Client implements Client {
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

    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    public void send(final URL webhookUrl, final String payload) {
	Request request = new Builder().url(webhookUrl).post(RequestBody.create(JSON, payload)).build();
	Call call = okHttpClient.newCall(request);
	call.enqueue(RESPONSE_CALLBACK);
    }
}
