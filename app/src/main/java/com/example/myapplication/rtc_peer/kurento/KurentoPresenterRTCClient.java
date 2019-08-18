package com.example.myapplication.rtc_peer.kurento;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.myapplication.rtc_peer.kurento.websocket.SuminDefaultSocketService;
import com.nhancv.webrtcpeer.rtc_comm.ws.BaseSocketCallback;
import com.nhancv.webrtcpeer.rtc_comm.ws.DefaultSocketService;
import com.nhancv.webrtcpeer.rtc_comm.ws.SocketService;
import com.nhancv.webrtcpeer.rtc_peer.RTCClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Created by nhancao on 7/18/17.
 */

public class KurentoPresenterRTCClient implements RTCClient {
    private static final String TAG = KurentoPresenterRTCClient.class.getSimpleName();

    /*
    이 부분 바뀌었습니다
    //private SocketService socketService;
    아니... 인터페이스를 제대로 선언도 안해놓은 상태로 진행시키면 어쩌라는거야
    그래서 이렇게 바꿔줬는데
    //private DefaultSocketService socketService;
     내부 함수가 강제적으로 소켓을 종료시켜서 ㅋㅋ 짜증나서 새로 만들었다.
     */
    private SuminDefaultSocketService socketService;
    /*
    SharedPreference를 사용하려고 추가해줬다.
     */
    private Context context;

    public KurentoPresenterRTCClient(SuminDefaultSocketService socketService, Context context) {
        this.socketService = socketService;

        /*
        SharedPrefereces 사용하려고 만들어줬다.
         */
        this.context = context;
    }

    public void connectToRoom(String host, BaseSocketCallback socketCallback) {
        socketService.connect(host, socketCallback);
    }


    /*
    여기서
     */
    @Override
    public void sendOfferSdp(SessionDescription sdp) {

        /*
        MainActivity에서 띄운 다이얼로그에서 가져온(입력한)
        방송자 ID를 가져온다.
        SharedPreference를 작동시키려면 기본적으로 Context가 필요하다.
        액티비티에서 실행하든, 아니면 다른 클래스로부터 Parameter로 Activity를 끌어오든 해서 SharedPreference를 쓸 수 있다.
         */

        SharedPreferences sf = context.getSharedPreferences("BroadcasterInfo",Context.MODE_PRIVATE);
        String userId = sf.getString("presenterId","");


        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "presenter");
            obj.put("userId", userId);
            obj.put("roomId", "roomId1");
            obj.put("sdpOffer", sdp.description);

            socketService.sendMessage(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendAnswerSdp(SessionDescription sdp) {
        Log.e(TAG, "sendAnswerSdp: ");
    }

    @Override
    public void sendLocalIceCandidate(IceCandidate iceCandidate) {
        try {

            /*
        MainActivity에서 띄운 다이얼로그에서 가져온(입력한)
        방송자 ID를 가져온다.
        SharedPreference를 작동시키려면 기본적으로 Context가 필요하다.
        액티비티에서 실행하든, 아니면 다른 클래스로부터 Parameter로 Activity를 끌어오든 해서 SharedPreference를 쓸 수 있다.
         */

            SharedPreferences sf = context.getSharedPreferences("BroadcasterInfo",Context.MODE_PRIVATE);
            String userId = sf.getString("presenterId","");

            JSONObject obj = new JSONObject();
            obj.put("id", "onIceCandidate");
            obj.put("userId", userId);
            obj.put("roomId", "roomId1");
            JSONObject candidate = new JSONObject();
            candidate.put("candidate", iceCandidate.sdp);
            candidate.put("sdpMid", iceCandidate.sdpMid);
            candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            obj.put("candidate", candidate);

            socketService.sendMessage(obj.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendLocalIceCandidateRemovals(IceCandidate[] candidates) {
        Log.e(TAG, "sendLocalIceCandidateRemovals: ");
    }

}