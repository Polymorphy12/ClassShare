package com.example.myapplication.main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.VideoList.VideoListActivity;
import com.example.myapplication.broadcaster.RecordingService;
import com.hannesdorfmann.mosby.mvp.MvpActivity;
import com.example.myapplication.R;
import com.example.myapplication.broadcaster.BroadCasterActivity_;
import com.example.myapplication.viewer.ViewerActivity_;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;

/**
 * Created by nhancao on 9/18/16.
 */

@EActivity(R.layout.activity_main)
public class MainActivity extends MvpActivity<MainView, MainPresenter> implements MainView {
    private static final String TAG = MainActivity.class.getName();



    @Click(R.id.btBroadCaster)
    protected void btBroadCasterClick() {

        final EditText edittext = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

                        BroadCasterActivity_.intent(MainActivity.this).start();
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();


    }


    @Click(R.id.btViewer)
    protected void btViewerClick() {
        final EditText edittext = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

                        ViewerActivity_.intent(MainActivity.this).start();
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    @Click(R.id.btOne2One)
    protected void btOne2OneClick() {

        Intent myIntent = new Intent(getApplicationContext(), VideoListActivity.class);
        startActivity(myIntent);
//        One2OneActivity_.intent(this).start();
    }

    @NonNull
    @Override
    public MainPresenter createPresenter() {
        return new MainPresenter(getApplication());
    }

}