package com.crossoverjie.cim.common.util;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2020-04-25 00:39
 * @since JDK 1.8
 */
public final class HttpClient{

    private static MediaType mediaType = MediaType.parse("application/json");

    public static Response post(OkHttpClient okHttpClient, String params, String url) throws IOException {
        RequestBody requestBody = RequestBody.create(mediaType, params);

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("request url [" + url + "], params [" + params +"] failed, response code: " + response.code()
                    + ", message: " + response.message());
        }

        return response;
    }

    public static Response get(OkHttpClient okHttpClient, String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        return response;
    }

}
