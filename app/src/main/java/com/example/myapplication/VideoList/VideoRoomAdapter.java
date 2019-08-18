package com.example.myapplication.VideoList;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.myapplication.R;

import java.util.ArrayList;

public class VideoRoomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<VideoRoomInfo> videoRoomInfoArrayList;
    // RecyclerView를 포함하는 액티비티에서 가져온 context.
    // 사실 이 context는 alertDialog를 띄울 때 필요한 것. 사실 지금은 필요 없음.
    private Context mContext;


    public VideoRoomAdapter(Context context, ArrayList<VideoRoomInfo> list)
    {
        videoRoomInfoArrayList = list;
        mContext = context;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        //이미지뷰 + Picture 로 네이밍을 한 것 같다. 구려..
        ImageView vd_picture;
        //역시 TextView의 이니셜을 따서 tv+ Price(가격)으로 네이밍을 한 것 같다.
        TextView vd_name;

        MyViewHolder(View view){
            super(view);
            vd_picture=view.findViewById(R.id.vd_picture);
            vd_name=view.findViewById(R.id.vd_name);

        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_videolist, parent, false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        MyViewHolder myViewHolder = (MyViewHolder) holder;

        //이런 식으로 이미지 할당도 해주나 보다. setImageResource가 받는 drawableId 파라미터가 어떤 형태인지 잘 모르겠다.
        //myViewHolder.vd_picture.setImageResource(videoRoomInfoArrayList.get(position).drawableId);
        myViewHolder.vd_name.setText(videoRoomInfoArrayList.get(position).userId);
    }

    @Override
    public int getItemCount() {
        return videoRoomInfoArrayList.size();
    }
}
