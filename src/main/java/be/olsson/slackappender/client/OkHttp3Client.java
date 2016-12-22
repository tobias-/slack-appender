package be.olsson.slackappender.client;

import java.io.IOException;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OkHttp3Client implements Client {
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final Callback RESPONSE_CALLBACK = new Callback() {
	@Override
	public void onFailure(Call call, IOException e) {
	    e.printStackTrace();
	}

	@Override
	public void onResponse(Call call, Response response) throws IOException {
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
