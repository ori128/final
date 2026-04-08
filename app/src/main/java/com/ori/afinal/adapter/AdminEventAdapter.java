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
import java.util.ArrayList;
import java.util.List;

public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private OnEventActionListener listener;

    public interface OnEventActionListener {
        void onDeleteClick(Event event);
        void onEditClick(Event event);
    }

    public AdminEventAdapter(OnEventActionListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvTitle.setText(event.getTitle() != null ? event.getTitle() : "פגישה ללא שם");
        holder.tvDateTime.setText(event.getDateTime() != null ? event.getDateTime() : "ללא תאריך");
        holder.tvId.setText("ID: " + event.getId());

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(event));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class AdminEventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDateTime, tvId;
        ImageButton btnDelete, btnEdit;

        public AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_admin_event_title);
            tvDateTime = itemView.findViewById(R.id.tv_admin_event_datetime);
            tvId = itemView.findViewById(R.id.tv_admin_event_id);
            btnDelete = itemView.findViewById(R.id.btn_admin_delete_event);
            btnEdit = itemView.findViewById(R.id.btn_admin_edit_event);
        }
    }
}