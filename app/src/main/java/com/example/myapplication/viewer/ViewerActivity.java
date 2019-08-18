package com.example.myapplication.viewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.Chat.ChatData;
import com.example.myapplication.Chat.ChatListAdapter;
import com.example.myapplication.rtc_peer.kurento.websocket.SuminDefaultSocketService;
import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.example.myapplication.R;
import com.nhancv.webrtcpeer.rtc_plugins.ProxyRenderer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;

/**
 * Created by nhancao on 7/20/17.
 */
@EActivity(R.layout.activity_viewer)
public class ViewerActivity extends MvpActivity<ViewerView, ViewerPresenter> implements ViewerView {
    private static final String TAG = ViewerActivity.class.getSimpleName();

    @ViewById(R.id.vGLSurfaceViewCall)
    protected SurfaceViewRenderer vGLSurfaceViewCall;

    private EglBase rootEglBase;
    private ProxyRenderer remoteProxyRenderer;
    private Toast logToast;


    //채팅 리사이클러뷰 설정
    @ViewById(R.id.chatList)
    RecyclerView mRecyclerView;

    RecyclerView.LayoutManager mLayoutManager;
    ChatListAdapter mAdapter;
    ArrayList<ChatData> mArrayList;

    //채팅할 때 쓸 웹 소켓
    private SuminDefaultSocketService socketService;

    @ViewById(R.id.typeChatContent)
    EditText typeChatContent;

    @Click(R.id.chatButton)
    void chatButtonClick(){
        String message = typeChatContent.getText().toString();
        sendChatMessage(message);
    }

    @Background
    void sendChatMessage(String chatMessage){

        SharedPreferences sf = getSharedPreferences("BroadcasterInfo", Context.MODE_PRIVATE);
        String userId = sf.getString("presenterId","");

        try {
            JSONObject obj = new JSONObject();
            obj.put("id", "chat");
            obj.put("userId", userId);
            obj.put("content", chatMessage);

            socketService.sendMessage(obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @UiThread
    void notifyRecyclerViewChanged(){
        Log.d("Viewer","Notified!!!!!!!!!!!!!!!!");
        mAdapter.notifyDataSetChanged();
        Log.d("Viewer", mArrayList.get(0).getSessionId());
    }

    @Override
    public void updateRecyclerView(String message){
        try {
            Log.d("Viewer","Yeah, I came In!!!!!!");
                JSONObject object = new JSONObject(message);
                ChatData roomInfo = new ChatData(object.getString("sessionId"),"userId","roomId",object.getString("content"));
                mArrayList.add(roomInfo);
            Log.d("Viewer",object.getString("sessionId"));
            //어? 핸들러 안써도 되네? runOnUiThread 써도 되네?
            // 핸들러랑 runOnUiThread랑 AsyncTask랑 차이?
            // 이번엔 운이 좋았다. runOnUiThread가 만능은 아니더라.
            //runOnUiThread는 핸들러의 한 종류이더라.
            //https://stackoverflow.com/questions/12618038/why-to-use-handlers-while-runonuithread-does-the-same
//            ViewerActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mAdapter.notifyDataSetChanged();
//                }
//            });

            notifyRecyclerViewChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //RecyclerView 설정을 따로 해줘야 한다면 여기서 해줄것.
    //onCreate에 설정해줘야 할 것이 있으면 여기서 함.
    @AfterViews
    protected void init() {
        //config peer
        remoteProxyRenderer = new ProxyRenderer();
        rootEglBase = EglBase.create();

        vGLSurfaceViewCall.init(rootEglBase.getEglBaseContext(), null);
        vGLSurfaceViewCall.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        vGLSurfaceViewCall.setEnableHardwareScaler(true);
        vGLSurfaceViewCall.setMirror(false);
        remoteProxyRenderer.setTarget(vGLSurfaceViewCall);

        presenter.initPeerConfig();
        presenter.startCall();
        //peer 설정 끝

        //이제 채팅 설정 해줘야 하지 않을까.
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mArrayList = new ArrayList<>();
        mAdapter = new ChatListAdapter(getApplicationContext(),mArrayList);

        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.bringToFront();
    }

    @Override
    public void disconnect() {
        remoteProxyRenderer.setTarget(null);
        if (vGLSurfaceViewCall != null) {
            vGLSurfaceViewCall.release();
            vGLSurfaceViewCall = null;
        }

        finish();
    }

    @NonNull
    @Override
    public ViewerPresenter createPresenter() {
        ViewerPresenter vp = new ViewerPresenter(getApplication());
        //ViewerPresenter을 만들고 나서 socketService를 생성해서 넘겨준다.
        socketService = vp.getSocketService();
        return vp;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        presenter.disconnect();
    }

    @Override
    public void stopCommunication() {
        onBackPressed();
    }

    @Override
    public void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    @Override
    public EglBase.Context getEglBaseContext() {
        return rootEglBase.getEglBaseContext();
    }

    @Override
    public VideoRenderer.Callbacks getRemoteProxyRenderer() {
        return remoteProxyRenderer;
    }

}