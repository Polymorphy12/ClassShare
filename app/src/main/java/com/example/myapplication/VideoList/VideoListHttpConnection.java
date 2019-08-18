package com.example.myapplication.VideoList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;


public class VideoListHttpConnection {

    private OkHttpClient client;
    private static VideoListHttpConnection instance = new VideoListHttpConnection();
    public static VideoListHttpConnection getInstance() {
        return instance;
    }

    private VideoListHttpConnection(){
        //this.client = new OkHttpClient();


/*

      HTTPS에다가 그냥 요청을 했더니 아무 응답도 내놓지 않더란 말이지.
      인증되지 않은 인증서를 가지고 있는 HTTPS의 경우 그냥 블락을 먹이더란 말이야.
      그래서 코드로 강제 접속을 할 수 있게 조정을 해줬어.

      */
        this.client = getUnsafeOkHttpClient();
    }


    /** 웹 서버로 요청을 한다. */
    public void requestWebServer(String parameter, String parameter2, Callback callback) {


//        RequestBody body = new FormBody.Builder()
//                .add("parameter", parameter)
//                .add("parameter2", parameter2)
//                .build();
        Request request = new Request.Builder()
                .url("https://52.79.241.50/getRoomInfo")
//                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
