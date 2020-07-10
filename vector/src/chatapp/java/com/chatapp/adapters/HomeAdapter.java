package com.chatapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chatapp.fragments.HomeModel;

import java.util.ArrayList;

import im.vector.R;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.HomeHolder> {

    ArrayList<HomeModel> list = new ArrayList<>();
    Context context;
    private iHomClick iHomClick;

    public HomeAdapter(Context context, ArrayList<HomeModel> list,iHomClick iHomClick) {
        this.context = context;
        this.list = list;
        this.iHomClick=iHomClick;
    }


    @NonNull
    @Override
    public HomeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_row_view, null);
        return new HomeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeHolder holder, int position) {
        try{
        HomeModel model = list.get(position);
        holder.homeName.setText(model.getName());
        holder.iv.setImageResource(model.getIcon());
        holder.container.setBackgroundColor(Color.parseColor(model.getColorCode()));
        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iHomClick.onHomeClick(model.getName());
            }
        });}catch (Exception e){

        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class HomeHolder extends RecyclerView.ViewHolder {
        TextView homeName;
        ImageView iv;
        LinearLayout container;

        public HomeHolder(@NonNull View itemView) {
            super(itemView);
            homeName=itemView.findViewById(R.id.homeTitle);
            iv=itemView.findViewById(R.id.iv);
            container=itemView.findViewById(R.id.container);
        }
    }

    public interface  iHomClick{
        public void onHomeClick(String title);
    }
}
