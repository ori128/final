package com.ori.afinal.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ori.afinal.R;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Event> events = new ArrayList<>();
    private String currentUserId;
    private DatabaseService databaseService;
    private boolean isTrashMode = false;

    public NotificationAdapter(String currentUserId, boolean isTrashMode) {
        this.currentUserId = currentUserId;
        this.isTrashMode = isTrashMode;
        this.databaseService = DatabaseService.getInstance();
    }

    public void setEvents(List<Event> events) {
        this.events = events;
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
        Event event = events.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(event.getTitle());
        holder.tvLocation.setText(event.getLocation());
        holder.tvType.setText(event.getType());

        // תאריך ושעה
        if (event.getDateTime() != null) {
            String[] dateTimeParts = event.getDateTime().split(" ");
            if (dateTimeParts.length == 2) {
                holder.tvTime.setText(dateTimeParts[1]);
                holder.tvDate.setText(dateTimeParts[0]);
            }
        }

        // משתתפים
        int participants = event.getParticipantIds() != null ? event.getParticipantIds().size() : 0;
        int invited = event.getInvitedParticipantIds() != null ? event.getInvitedParticipantIds().size() : 0;
        holder.tvParticipants.setText(participants + "/" + (participants + invited));

        // בדיקת זמן שעבר
        checkIfTimePassed(event, holder);

        // כפתור איקס / שחזור
        holder.btnClose.setOnClickListener(v -> {
            if (isTrashMode) {
                showRestoreDialog(context, event);
            } else {
                showDeleteConfirmDialog(context, event);
            }
        });

        // כפתורי אישור ודחייה
        holder.btnAccept.setOnClickListener(v -> respond(event, true, context));
        holder.btnReject.setOnClickListener(v -> respond(event, false, context));

        if (isTrashMode) {
            holder.btnClose.setImageResource(android.R.drawable.ic_menu_revert);
            holder.llActions.setVisibility(View.GONE);
        }
    }

    // --- לוגיקה לעדכון חי של הרשימה ---
    private void removeItem(Event event) {
        int position = events.indexOf(event);
        if (position != -1) {
            events.remove(position);
            notifyItemRemoved(position);
            // מבטיח שהאנימציה תהיה חלקה ושהאינדקסים של שאר הפריטים יתעדכנו
            notifyItemRangeChanged(position, events.size());
        }
    }

    private void checkIfTimePassed(Event event, NotificationViewHolder holder) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date eventDate = sdf.parse(event.getDateTime());
            if (eventDate != null && eventDate.before(new Date())) {
                holder.llActions.setVisibility(View.GONE);
                holder.tvTimePassed.setVisibility(View.VISIBLE);
                if (isTrashMode) holder.btnClose.setVisibility(View.GONE);
            } else if (!isTrashMode) {
                holder.llActions.setVisibility(View.VISIBLE);
                holder.tvTimePassed.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDeleteConfirmDialog(Context context, Event event) {
        new AlertDialog.Builder(context)
                .setTitle("מחיקת התראה")
                .setMessage("האם להעביר את ההתראה לפח האשפה?")
                .setPositiveButton("העבר לפח", (dialog, which) -> {
                    databaseService.moveNotificationToTrash(event.getId(), currentUserId, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            removeItem(event); // נעלם מהמסך מיד!
                            Toast.makeText(context, "הועבר לפח", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(context, "שגיאה בפעולה", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showRestoreDialog(Context context, Event event) {
        new AlertDialog.Builder(context)
                .setTitle("שחזור התראה")
                .setMessage("האם להחזיר את ההתראה לרשימה הראשית?")
                .setPositiveButton("שחזר", (dialog, which) -> {
                    databaseService.restoreNotificationFromTrash(event.getId(), currentUserId, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            removeItem(event); // נעלם מהפח מיד!
                            Toast.makeText(context, "שוחזר בהצלחה", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(context, "שגיאה בשחזור", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void respond(Event event, boolean accept, Context context) {
        databaseService.respondToInvitation(event.getId(), currentUserId, accept, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                removeItem(event); // נעלם מהמסך מיד אחרי החלטה!
                String msg = accept ? "הפגישה אושרה!" : "הפגישה נדחתה";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(context, "שגיאה בעדכון", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() { return events.size(); }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvType, tvLocation, tvTime, tvDate, tvParticipants, tvTimePassed;
        ImageButton btnClose;
        MaterialButton btnAccept, btnReject;
        LinearLayout llActions;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvType = itemView.findViewById(R.id.tv_event_type);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvDate = itemView.findViewById(R.id.tv_event_date);
            tvParticipants = itemView.findViewById(R.id.tv_event_participants_count);
            tvTimePassed = itemView.findViewById(R.id.tv_notif_time_passed);
            btnClose = itemView.findViewById(R.id.btn_notif_close);
            btnAccept = itemView.findViewById(R.id.btn_notif_accept);
            btnReject = itemView.findViewById(R.id.btn_notif_reject);
            llActions = itemView.findViewById(R.id.ll_notif_actions);
        }
    }
}