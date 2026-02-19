package com.ori.afinal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.R;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Event> notificationList = new ArrayList<>();
    private String currentUserId;
    private DatabaseService databaseService;
    private Runnable onDataChanged;

    public NotificationAdapter(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        this.databaseService = DatabaseService.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void setNotifications(List<Event> notifications) {
        this.notificationList = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Event event = notificationList.get(position);

        holder.tvTitle.setText("הזמנה לפגישה: " + event.getTitle());
        holder.tvDateTime.setText(event.getDateTime());
        holder.tvType.setText(event.getType());

        holder.btnAccept.setOnClickListener(v -> respondToInvite(event.getId(), true, v));
        holder.btnDecline.setOnClickListener(v -> respondToInvite(event.getId(), false, v));
    }

    private void respondToInvite(String eventId, boolean isAccepted, View view) {
        if (currentUserId == null) return;

        databaseService.respondToInvitation(eventId, currentUserId, isAccepted, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                String msg = isAccepted ? "אישרת הגעה לפגישה!" : "דחית את ההזמנה.";
                Toast.makeText(view.getContext(), msg, Toast.LENGTH_SHORT).show();
                if (onDataChanged != null) {
                    onDataChanged.run(); // מרענן את המסך כדי להעלים את מה שכבר אישרנו
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(view.getContext(), "שגיאה בביצוע הפעולה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDateTime, tvType;
        Button btnAccept, btnDecline;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvDateTime = itemView.findViewById(R.id.tv_notif_datetime);
            tvType = itemView.findViewById(R.id.tv_notif_type);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }
    }
}