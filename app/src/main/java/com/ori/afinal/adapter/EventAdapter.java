package com.ori.afinal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ori.afinal.R;
import com.ori.afinal.model.Event;
import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList = new ArrayList<>();

    public void setEvents(List<Event> events) {
        this.eventList = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_dashboard, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvLocation.setText(event.getLocation());
        holder.tvType.setText(event.getType());

        // טיפול בתאריך ושעה (מניחים שהפורמט הוא "yyyy-MM-dd HH:mm")
        if (event.getDateTime() != null && event.getDateTime().contains(" ")) {
            String[] parts = event.getDateTime().split(" ");
            if (parts.length >= 2) {
                holder.tvDate.setText(parts[0]);
                holder.tvTime.setText(parts[1]);
            } else {
                holder.tvDate.setText(event.getDateTime());
                holder.tvTime.setText("");
            }
        } else {
            holder.tvDate.setText(event.getDateTime());
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvType, tvLocation, tvTime, tvDate;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvType = itemView.findViewById(R.id.tv_event_type);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvDate = itemView.findViewById(R.id.tv_event_date);
        }
    }
}