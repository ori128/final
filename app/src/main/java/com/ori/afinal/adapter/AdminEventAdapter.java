package com.ori.afinal.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ori.afinal.R;
import com.ori.afinal.model.Event;

import java.util.List;

public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    private List<Event> eventList;
    private OnEventDeleteListener deleteListener;

    public interface OnEventDeleteListener {
        void onDeleteClick(Event event);
    }

    public AdminEventAdapter(List<Event> eventList, OnEventDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvTitle.setText(event.getTitle() != null ? event.getTitle() : "ללא כותרת");
        holder.tvDate.setText(event.getDateTime() != null ? event.getDateTime() : "ללא תאריך");
        holder.tvLocation.setText(event.getLocation() != null ? event.getLocation() : "ללא מיקום");

        // מאזין ללחיצה על פח האשפה (מחיקה)
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public void setEventList(List<Event> eventList) {
        this.eventList = eventList;
        notifyDataSetChanged();
    }

    public static class AdminEventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvLocation;
        ImageButton btnDelete;

        public AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_admin_event_title);
            tvDate = itemView.findViewById(R.id.tv_admin_event_date);
            tvLocation = itemView.findViewById(R.id.tv_admin_event_location);
            btnDelete = itemView.findViewById(R.id.btn_admin_delete_event);
        }
    }
}