package com.example.myapplication.rtc_peer.kurento.websocket;

import android.app.Application;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;


import com.nhancv.webrtcpeer.rtc_comm.ws.LooperExecutor;
import com.nhancv.webrtcpeer.rtc_comm.ws.SocketCallBack;
import com.nhancv.webrtcpeer.rtc_comm.ws.SocketService;

public class SuminDefaultSocketService implements SocketService {
    private static final String TAG = SuminDefaultSocketService.class.getSimpleName();
    private WebSocketClient client;
    private KeyStore keyStore;
    private LooperExecutor executor;
    private Application application;
    private SocketCallBack socketCallBack;

    public SuminDefaultSocketService(Application application) {
        this.application = application;
        this.executor = new LooperExecutor();
        this.executor.requestStart();
    }

    public void connect(String host) {
        this.connect(host, true);
    }

    public void connect(String host, boolean force) {
        if (force) {
            this.close();
        } else if (this.isConnected()) {
            return;
        }

        URI uri;
        try {
            uri = new URI(host);
        } catch (URISyntaxException var8) {
            var8.printStackTrace();
            return;
        }

        this.client = new WebSocketClient(uri) {
            public void onOpen(ServerHandshake serverHandshake) {
                if (SuminDefaultSocketService.this.socketCallBack != null) {
                    SuminDefaultSocketService.this.socketCallBack.onOpen(serverHandshake);
                }

            }

            public void onMessage(String s) {
                if (SuminDefaultSocketService.this.socketCallBack != null) {
                    SuminDefaultSocketService.this.socketCallBack.onMessage(s);
                }

            }

            public void onClose(int i, String s, boolean b) {
                if (SuminDefaultSocketService.this.socketCallBack != null) {
                    SuminDefaultSocketService.this.socketCallBack.onClose(i, s, b);
                }

            }

            public void onError(Exception e) {
                if (SuminDefaultSocketService.this.socketCallBack != null) {
                    SuminDefaultSocketService.this.socketCallBack.onError(e);
                }

            }
        };

        try {
            String scheme = uri.getScheme();
            if (scheme.equals("https") || scheme.equals("wss")) {
                // 이 부분이 문제였다 !! server.crt 라는 인증서가 필요했던 것.
                this.setTrustedCertificate(this.application.getAssets().open("server.crt"));
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(this.keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init((KeyManager[])null, tmf.getTrustManagers(), (SecureRandom)null);
                this.client.setSocket(sslContext.getSocketFactory().createSocket());
            }
        } catch (Exception var9) {
            var9.printStackTrace();
        }

        this.client.connect();
    }

    public void connect(String host, SocketCallBack socketCallBack) {
        this.connect(host, socketCallBack, false);
    }

    public void connect(String host, SocketCallBack socketCallBack, boolean force) {
        this.setCallBack(socketCallBack);
        this.connect(host, force);
    }

    public void setCallBack(SocketCallBack socketCallBack) {
        this.socketCallBack = socketCallBack;
    }

    public void close() {
        if (this.isConnected()) {
            this.client.close();
        }

    }

    public boolean isConnected() {
        return this.client != null && this.client.getConnection().isOpen();
    }

    public void sendMessage(String message) {

        /*
        이 부분이 바뀌었습니다.
        //this.executor.execute(SuminDefaultSocketService$$Lambda$1.lambdaFactory$(this, message));
        https://github.com/nhancv/nc-android-webrtcpeer/blob/master/webrtcpeer/src/main/java/com/nhancv/webrtcpeer/rtc_comm/ws/DefaultSocketService.java
        여기랑 다르더라고.
         */
        this.executor.execute(() -> {
            if (isConnected()) {
                try {
                    client.send(message.getBytes("UTF-8"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setTrustedCertificate(InputStream inputFile) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(inputFile);
            Certificate ca = cf.generateCertificate(caInput);
            String keyStoreType = KeyStore.getDefaultType();
            this.keyStore = KeyStore.getInstance(keyStoreType);
            this.keyStore.load((InputStream)null, (char[])null);
            this.keyStore.setCertificateEntry("ca", ca);
        } catch (Exception var6) {
            var6.printStackTrace();
        }

    }
}