package com.ori.afinal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.R;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList = new ArrayList<>();
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

    public void setNotifications(List<Notification> notifications) {
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
        Notification notification = notificationList.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());

        // המרת ה-Timestamp לתאריך קריא
        if (notification.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(notification.getTimestamp()));
            holder.tvDateTime.setText(dateStr);
        } else {
            holder.tvDateTime.setVisibility(View.GONE);
        }

        // בדיקה איזה כפתורים להציג לפי סוג ההתראה
        if ("INVITE".equals(notification.getType())) {
            holder.llInviteActions.setVisibility(View.VISIBLE);
            holder.btnDismiss.setVisibility(View.GONE);

            // טיפול באישור הגעה
            holder.btnAccept.setOnClickListener(v -> {
                respondToInviteAndRemoveNotification(notification, true, v);
            });

            // טיפול בדחיית הגעה
            holder.btnDecline.setOnClickListener(v -> {
                respondToInviteAndRemoveNotification(notification, false, v);
            });

        } else {
            // זו התראת מידע (INFO / UPDATE / REMOVED)
            holder.llInviteActions.setVisibility(View.GONE);
            holder.btnDismiss.setVisibility(View.VISIBLE);

            // לחיצה על "הבנתי" פשוט מוחקת את ההתראה מהמסד
            holder.btnDismiss.setOnClickListener(v -> {
                databaseService.deleteNotification(notification.getId(), new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        if (onDataChanged != null) onDataChanged.run();
                    }
                    @Override
                    public void onFailed(Exception e) {}
                });
            });
        }
    }

    private void respondToInviteAndRemoveNotification(Notification notification, boolean isAccepted, View view) {
        if (currentUserId == null || notification.getEventId() == null) return;

        // 1. עדכון מצב המשתמש בפגישה (אישר/דחה)
        databaseService.respondToInvitation(notification.getEventId(), currentUserId, isAccepted, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                String msg = isAccepted ? "אישרת הגעה לפגישה!" : "דחית את ההזמנה.";
                Toast.makeText(view.getContext(), msg, Toast.LENGTH_SHORT).show();

                // 2. מחיקת ההתראה עצמה אחרי שהגבנו לה
                databaseService.deleteNotification(notification.getId(), new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        if (onDataChanged != null) onDataChanged.run();
                    }
                    @Override
                    public void onFailed(Exception e) {}
                });
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
        TextView tvTitle, tvDateTime, tvMessage;
        LinearLayout llInviteActions;
        Button btnAccept, btnDecline, btnDismiss;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvDateTime = itemView.findViewById(R.id.tv_notif_datetime);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            llInviteActions = itemView.findViewById(R.id.ll_invite_actions);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            btnDismiss = itemView.findViewById(R.id.btn_dismiss);
        }
    }
}