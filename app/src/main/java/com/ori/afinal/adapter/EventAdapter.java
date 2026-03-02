package com.ori.afinal.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.ori.afinal.EventDetails;
import com.ori.afinal.R;
import com.ori.afinal.model.Event;
import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList = new ArrayList<>();
    private String currentUserId;

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

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

        if (event.getDateTime() != null) {
            if (event.getDateTime().contains(" ")) {
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
                holder.tvTime.setText("");
            }
        }

        int accepted = event.getParticipantIds() != null ? event.getParticipantIds().size() : 0;
        int pending = event.getInvitedParticipantIds() != null ? event.getInvitedParticipantIds().size() : 0;
        int declined = event.getDeclinedParticipantIds() != null ? event.getDeclinedParticipantIds().size() : 0;
        int totalInvited = accepted + pending + declined;

        holder.tvParticipantsCount.setText("👤 " + accepted + "/" + totalInvited);

        // בדיקה האם המשתמש המחובר הוא מנהל הפגישה
        boolean isAdmin = event.getEventAdmin() != null && event.getEventAdmin().getId().equals(currentUserId);

        if (isAdmin) {
            // צבע רקע תואם ל"מנהל" בפרטי הפגישה (כתום בהיר)
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEDD5"));
        } else {
            // צבע לבן רגיל למשתמש רגיל
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        }

        // לחיצה תמיד פותחת את פרטי הפגישה
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, EventDetails.class);
            intent.putExtra("EVENT_ID", event.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvType, tvLocation, tvTime, tvDate, tvParticipantsCount;
        MaterialCardView cardView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvType = itemView.findViewById(R.id.tv_event_type);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvDate = itemView.findViewById(R.id.tv_event_date);
            tvParticipantsCount = itemView.findViewById(R.id.tv_event_participants_count);
            cardView = (MaterialCardView) itemView;
        }
    }
}