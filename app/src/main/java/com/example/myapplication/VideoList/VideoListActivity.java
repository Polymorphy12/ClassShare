package com.example.myapplication.VideoList;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.broadcaster.BroadCasterActivity_;
import com.example.myapplication.viewer.ViewerActivity_;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class VideoListActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    VideoRoomAdapter mAdapter;
    ArrayList<VideoRoomInfo> mArrayList;

    private GestureDetector gestureDetector;


    //로그 찍을 때 이 액티비티에서 일어난 로그임을 알려주는 태그이다.
    private static final String TAG = "VideoListActivity";

    private VideoListHttpConnection httpConn = VideoListHttpConnection.getInstance();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videolist);

        //RecyclerView 설정해주기
        /*
        * 1.RecyclerView, LayoutManager, Adapter, 그리고 RecyclerView에 넣을 데이터를 관리하는 ArrayList를 액티비티 내 전역변수로 선언해준다.
        * 2.findViewById로 레이아웃과 RecyclerView를 동기화 시켜준다.
        * 3.LayoutManager 객체를 만들어준다.
        * 4.RecyclerView.setLayoutManager(레이아웃 메니저)를 해준다.
        * 5. ArrayList를 선언하고, Adapter 설정을 해준다.
        * 5-1. Adapter constructor에 맞게 설정을 해준다.
        * 5-2.
        * Q.Activity에 있는 ArrayList와 Adapter에 있는 ArrayList를 동기화 시켜줘야 하지 않을까?
        * A. 그럴 필요 없다. Reference 형태로 넘겨줬기 때문에 동기화 되어 있다.
        * 6. 아이템 클릭했을 때 동작하는 함수를 구현해준다.
        * 6-1. 이걸 구현하는 방법은 여러가지가 있다.
        * 아래에 구현해놓은 건 이 블로그를 참고했다.
        * https://onlyformylittlefox.tistory.com/9
        *
        * 이 영상을 참고해도 될 터이다.
        * https://www.youtube.com/watch?v=tta6FQn6fM8
        *
        * */

        //아이템 클릭했을 때 방송화면으로 넘어가게 하기 위한 것.
        //이걸 구현해주는 이유는 setOnTouchListener가 아이템을 누를 때, 뗄 때 모두 인식되는 함수이기 때문이다.
        gestureDetector = new GestureDetector(getApplicationContext(),new GestureDetector.SimpleOnGestureListener() {
            //누르고 뗄 때 한번만 인식하도록 하기위해서
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        //아이템 클릭했을 때 Viewer 입장에서 방송을 시청할 수 있도록 구현했다.
        RecyclerView.OnItemTouchListener onItemTouchListener = new RecyclerView.OnItemTouchListener() {

            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                //손으로 터치한 곳의 좌표를 토대로 해당 Item의 View를 가져옴
                View childView = rv.findChildViewUnder(e.getX(),e.getY());

                //터치한 곳의 View가 RecyclerView 안의 아이템이고 그 아이템의 View가 null이 아니라
                //정확한 Item의 View를 가져왔고, gestureDetector에서 한번만 누르면 true를 넘기게 구현했으니
                //한번만 눌려서 그 값이 true가 넘어왔다면
                if(childView != null && gestureDetector.onTouchEvent(e)){

                    //현재 터치된 곳의 position을 가져오고
                    int currentPosition = rv.getChildAdapterPosition(childView);

                    //해당 위치의 Data를 가져옴
                    VideoRoomInfo currentItemVideo = mArrayList.get(currentPosition);
                    Toast.makeText(VideoListActivity.this, "현재 터치한 Item의 Student Name은 " + currentItemVideo.getUserId(), Toast.LENGTH_SHORT).show();

                    SharedPreferences sf = getSharedPreferences("BroadcasterInfo", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sf.edit();

                    editor.putString("presenterId", currentItemVideo.getUserId());

                    editor.apply();

                    ViewerActivity_.intent(VideoListActivity.this).start();

                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        };

        mRecyclerView = findViewById(R.id.video_list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mArrayList = new ArrayList<>();
        mAdapter = new VideoRoomAdapter(this,mArrayList);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(onItemTouchListener);






        /*
        * 여기서 방 목록은 수정, 삭제는 해줄 필요 없어.
        * 추가만 해주면 돼.
        * SQLite에 저장하거나 하려는게 아니라
        * 액티비티에 접속할 때마다 http 요청으로 목록을 받아오려는 거니까.
        *
        * Q.만약 인터넷에 연결되지 않았는데 이 액티비티로 들어오면 어떻게 하지?
        * A. 예외처리를 해줘야지. 인터넷 연결하고 오라고.
        * */


        // 웹 서버로 데이터 전송
        getVideoList();

        //Floating Action Button을 클릭하면 스트리밍 방을 생성한다.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText edittext = new EditText(VideoListActivity.this);

                AlertDialog.Builder builder = new AlertDialog.Builder(VideoListActivity.this);
                builder.setTitle("방송자 입력");
                builder.setMessage("AlertDialog Content");
                builder.setView(edittext);
                builder.setPositiveButton("입력",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getApplicationContext(),edittext.getText().toString() , Toast.LENGTH_LONG).show();

                        /*
                        시청자가 Kurento에 접속할 때
                        어떤 방송을 볼 것인지 정보를 서버에 알려줘야 한다.

                        방송자 Id를 입력하자.

                        입력한 방송자 Id는 SharedPreferences에 저장해 놓는다.
                        저장한 방송자 ID는 KurentoPresenterRTCClient나 KurentoViewerRTCClient에서 다시 꺼낸 다음
                        WebSocket으로 정보를 보낸다.
                         */

                                SharedPreferences sf = getSharedPreferences("BroadcasterInfo", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sf.edit();

                                editor.putString("presenterId", edittext.getText().toString());

                                editor.apply();

                                BroadCasterActivity_.intent(VideoListActivity.this).start();
                            }
                        });
                builder.setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.show();
            }
        });
    }

    private void getVideoList() {
// 네트워크 통신하는 작업은 무조건 작업스레드를 생성해서 호출 해줄 것!!
        new Thread() {
            public void run() {
// 파라미터 2개와 미리정의해논 콜백함수를 매개변수로 전달하여 호출
                httpConn.requestWebServer("데이터1","데이터2", callback);
            }
        }.start();
    }

    private final Callback callback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.d(TAG, "콜백오류:"+e.getMessage());
        }
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String body = response.body().string();
            Log.d(TAG, "서버에서 응답한 Body:"+body);


            /*
            개 꿀팁 :  ctrl - alt - T를 눌러주면 if, try-catch, for loop, while loop등을 불필요하게 쳐줄 필요 없다.


            https://www.youtube.com/watch?v=xtVHvifaKC4
            이거 참고했음.
            response body를 위에서 String으로 만들어줬다.
            서버단이 제대로 되었다면, userId, roomId 정보를 담고 있는 JsonArray 형태가 도착했을 것이다.
            1. String을 JSONArray로 바꿔준다.
            2. for문을 이용해 JSONArray에 있는 원소들을 각각 빼서 분해시킨다.
            2-1. 각 분해한 원소들 정보를 객체화 시킨다.
            2-2. ArrayList에 추가한다.
            3. 다시 onCreate 함수로 올라간다.
            */
            try {

                JSONArray array = new JSONArray(body);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    VideoRoomInfo roomInfo = new VideoRoomInfo(object.getString("userId"),object.getString("roomId"));
                    mArrayList.add(roomInfo);
                }

                //어? 핸들러 안써도 되네? runOnUiThread 써도 되네?
                // 핸들러랑 runOnUiThread랑 AsyncTask랑 차이?
                // 이번엔 운이 좋았다. runOnUiThread가 만능은 아니더라.
                //runOnUiThread는 핸들러의 한 종류이더라.
                //https://stackoverflow.com/questions/12618038/why-to-use-handlers-while-runonuithread-does-the-same
                VideoListActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };


}
