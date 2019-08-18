package com.example.myapplication.viewer;

import com.hannesdorfmann.mosby.mvp.MvpView;

import org.webrtc.EglBase;
import org.webrtc.VideoRenderer;

/**
 * Created by nhancao on 7/20/17.
 */

public interface ViewerView extends MvpView {
    //ok
    void stopCommunication();

    //ok
    void logAndToast(String msg);

    //ok
    void disconnect();

    void updateRecyclerView(String message);

    EglBase.Context getEglBaseContext();

    VideoRenderer.Callbacks getRemoteProxyRenderer();
}