package com.example.myapplication.broadcaster;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.myapplication.Chat.ChatData;
import com.example.myapplication.Chat.ChatListAdapter;
import com.example.myapplication.R;
import android.Manifest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.rtc_peer.kurento.websocket.SuminDefaultSocketService;
import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.nhancv.npermission.NPermission;
import com.nhancv.webrtcpeer.rtc_plugins.ProxyRenderer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;

/**
 * Created by nhancao on 7/20/17.
 */
@EActivity(R.layout.activity_broadcaster)
public class BroadCasterActivity extends MvpActivity<BroadCasterView, BroadCasterPresenter>
        implements BroadCasterView, NPermission.OnPermissionResult {
    private static final String TAG = BroadCasterActivity.class.getSimpleName();

    @ViewById(R.id.vGLSurfaceViewCall)
    protected SurfaceViewRenderer vGLSurfaceViewCall;

    private NPermission nPermission;
    private EglBase rootEglBase;
    private ProxyRenderer localProxyRenderer;
    private Toast logToast;
    private boolean isGranted;


    //MediaProjection에 필요한 데이터터
    private static Intent mediaProjectionPermissionResultData;
    private static int mediaProjectionPermissionResultCode;


    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1; // MediaProjection 전용 인텐트를 사용할 때 사용하는 코드이다. (onActivityResult인 거지.)

    private boolean isError = false;
    private boolean screenCaptureEnabled = true;

    //여기까지 MediaProjection에 필요한 변수들.

   //채팅 리사이클러뷰 설정
    @ViewById(R.id.bChatList)
    RecyclerView mRecyclerView;

    RecyclerView.LayoutManager mLayoutManager;
    ChatListAdapter mAdapter;
    ArrayList<ChatData> mArrayList;

    //채팅할 때 쓸 웹 소켓
    private SuminDefaultSocketService socketService;

    @ViewById(R.id.bTypeChatContent)
    EditText typeChatContent;

    @Click(R.id.bChatButton)
    void chatButtonClick(){
        String message = typeChatContent.getText().toString();
        Toast.makeText(this, "Clicked!!", Toast.LENGTH_SHORT);
        sendChatMessage(message);

        //edittext 텍스트 없애주는 코드.
        typeChatContent.getText().clear();
        //키보드 닫아주는 코드.
       InputMethodManager imm= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(typeChatContent.getWindowToken(), 0);
    }


    @Click(R.id.remote_controller)
    void controlButtonClick(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Context context = getApplicationContext();
            Log.i("TEST", "Permission Granted ? " + Settings.canDrawOverlays(context));
            //M 이상에서만 퍼미션 확인(그 이하에서는 자동으로 허용됨)
            //다른 앱 위에서 그리기 권한에 대한 허용 여부 체크

            if(Settings.canDrawOverlays(context)) {
                //이미 권한 설정 되어있음
                context.startService(new Intent(context, RecordingService.class));
            }else{
                //권한 없음
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);    //startActivityForResult로 대체 가능
            }
        }
    }


    //원래는 BroadCasterPresenter에서 실행해줘야 하는 함수인데 말이야.
    // 귀찮으니까 그냥 액티비티에서 실행할래.
    // 연결한 웹소켓으로 보낸다.
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
            Log.d("Viewer","Yeah, I came In 222222");
            ChatData roomInfo = new ChatData(object.getString("sessionId"),"userId","roomId",object.getString("content"));
            Log.d("Viewer","Yeah, I came In! 333333");
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


    @AfterViews
    protected void init() {

        nPermission = new NPermission(true);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        //config peer
        /*
        *   WebRTC는 다음과 같은 과정으로 설정한다.
        *   이제 주석을 이런 방식으로 적을거다.
        *   (번호) WebRTC 설정 순서
        *   (화살표) 내 앱 어디에서 해당 번호에서 필요한 설정을 해줬는가.
            1. PeerConnectionFactory를 생성하고 초기화
        *       -> BroadCasterPresenter의 initPeerConfig()에서 구현해놨음.
        *       -> 바로 밑에 presenter.initPeerConfig() 코드로 생성함. 즉, onCreate 때 바로 생성한다는거지.

            2. 장치의 카메라를 사용하는 VideoCapturer 인스턴스 생성
                -> 이 액티비티의 함수 createVideoCapturer에서 구현해놨음.
                    카메라 1을 사용할 것인지, 카메라 2를 사용할 것인지 여부를 여기에다 구현해놨음.
                    MediaProjection을 사용할거라면 여기다가 쓰면 되지 않을까 싶다.
                -> BroadCasterPresenter의 startCall() -> rtcClient.connectToRoom -> onOpen -> onSignalConnected에서 생성함.

            3. Captuer로 부터 VideoSource를 생성
            4. source로 부터 ViedoTrack 생성
                ->3,4과정을 전부 BroadCasterPresenter의 peerConnectionClient가 설정한다.
                -> VideoCapturer 시작도 peerConnectionClient에서 해준다.

            5. SurfaceViewRenderer View와 VIedoTrack 인스턴스를 사용하여 Viedo renderer를 생성
                ->(1) 이 액티비티의 init 함수에서 EglBase를 만들고 SurfaceView.init()을 해준다.
                ->(2) 만들어 놓은 vGLSurfaceViewCall을 바탕으로 localProxyRenderer을 만든다.
                ->(3) 이 액티비티의 getLocalProxyRender() 함수를 이용해 BroadCasterPresenter의 peerConnectionClient에서 localProxyRenderer을 호출한다.
        * */

        localProxyRenderer = new ProxyRenderer();
        rootEglBase = EglBase.create();

        vGLSurfaceViewCall.init(rootEglBase.getEglBaseContext(), null);
        /*
        * SCALE_ASPECT_FIT,
        SCALE_ASPECT_FILL,
        SCALE_ASPECT_BALANCED;
        * */
        vGLSurfaceViewCall.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        vGLSurfaceViewCall.setEnableHardwareScaler(true);
        vGLSurfaceViewCall.setMirror(false);

        //액티비티에 렌더링할 SurfaceView 정보는 localProxyRenderer가 가지고 있다.
        localProxyRenderer.setTarget(vGLSurfaceViewCall);


        // WebRTC 1
        //mvp presenter 이니까 방송 presenter이랑 헷갈리지 마라.
        presenter.initPeerConfig();


        //이제 채팅 설정 해줘야 하지 않을까.
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mArrayList = new ArrayList<>();
        mAdapter = new ChatListAdapter(getApplicationContext(),mArrayList);

        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.bringToFront();



        if(screenCaptureEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startScreenCapture();
        }
    }

    @Override
    public void disconnect() {
        localProxyRenderer.setTarget(null);
        if (vGLSurfaceViewCall != null) {
            vGLSurfaceViewCall.release();
            vGLSurfaceViewCall = null;
        }
        finish();
    }

    /*
        Screen Capture 소스코드를 적용하기 이전.
    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < 23 || isGranted) {



            //WebRTC 2 - case 1
            presenter.startCall();
        } else {
            nPermission.requestPermission(this, Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        nPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionResult(String permission, boolean isGranted) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                this.isGranted = isGranted;
                if (!isGranted) {
                    nPermission.requestPermission(this, Manifest.permission.CAMERA);
                } else {
                    //nPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);

                    //WebRTC 2 - case 2
                    presenter.startCall();

                }
                break;
            default:
                break;
        }
    }

    */

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < 23 || isGranted) {

            if(!screenCaptureEnabled) {
                presenter.startCall();
            }
        } else {
            nPermission.requestPermission(this, Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        nPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionResult(String permission, boolean isGranted) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                this.isGranted = isGranted;
                if (!isGranted) {
                    nPermission.requestPermission(this, Manifest.permission.CAMERA);
                } else {
                    //nPermission.requestPermission(this, Manifest.permission.RECORD_AUDIO);


                    if(screenCaptureEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        //startScreenCapture();
                    } else {
                        //WebRTC 2 - case 2
                        presenter.startCall();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        //camera2를 사용하지 않고 바로 mediaProjection을 사용하고 싶다면?
        //appRTC에서 mediaProjection을 어떻게 사용했더라?
        /*
        *   1. StartScreenCaputre 라는 함수가 있었다.
        *       ->여기서 MediaProjectionManager, StartActivityForResult를 사용했다. (권한 얻기)
        *   2. 같은 액티비티에 onActivityResult를 두고 그 속에서 startCall() 함수를 호출했다.
        *       -> 따라서
        *   3. createScreenCapturer 함수를 따로 만들어줬다.
        *       ->WebRTC에서 만든 ScreenCapturerAndroid 라이브러리를 사용했다. 이게 VideoCapturer 인터페이스를 상속한거거든.
        * */
        if(screenCaptureEnabled)
        {
            videoCapturer = createScreenCapturer();
        }
        else if (useCamera2()) {
            if (!captureToTexture()) {
                return null;
            }
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            return null;
        }
        return videoCapturer;
    }

    @TargetApi(21)
    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mediaProjectionPermissionResultCode = resultCode;
        mediaProjectionPermissionResultData = data;

        presenter.startCall();

    }

    @TargetApi(21)
    private VideoCapturer createScreenCapturer() {
        if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            reportError("User didn't give permission to capture the screen.");
            return null;
        }
        return new ScreenCapturerAndroid(
                mediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                reportError("User revoked permission to capture the screen.");
            }
        });
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }



    @NonNull
    @Override
    public BroadCasterPresenter createPresenter() {
        BroadCasterPresenter bp = new BroadCasterPresenter(getApplication());
        //ViewerPresenter을 만들고 나서 socketService를 생성해서 넘겨준다.
        socketService = bp.getSocketService();
        return bp;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        presenter.disconnect();
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

    private void disconnectWithErrorMessage(final String errorMessage) {

            new AlertDialog.Builder(this)
                    .setTitle("Connection error")
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            })
                    .create()
                    .show();
    }

    @Override
    public EglBase.Context getEglBaseContext() {
        return rootEglBase.getEglBaseContext();
    }

    @Override
    public VideoRenderer.Callbacks getLocalProxyRenderer() {
        return localProxyRenderer;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && presenter.getDefaultConfig().isUseCamera2();
    }

    private boolean captureToTexture() {
        return presenter.getDefaultConfig().isCaptureToTexture();
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

}