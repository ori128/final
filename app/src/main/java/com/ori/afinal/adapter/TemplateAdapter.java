package com.ori.afinal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ori.afinal.R;
import com.ori.afinal.model.MeetingTemplate;

import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {

    private List<MeetingTemplate> templateList;
    private OnTemplateClickListener listener;

    // ממשק להאזנה ללחיצה על תבנית
    public interface OnTemplateClickListener {
        void onTemplateClick(MeetingTemplate template);
    }

    public TemplateAdapter(List<MeetingTemplate> templateList, OnTemplateClickListener listener) {
        this.templateList = templateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template, parent, false);
        return new TemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        MeetingTemplate template = templateList.get(position);
        holder.tvTitle.setText(template.getTitle());
        holder.tvDuration.setText(template.getDurationText());

        // כשמשתמש לוחץ על הכרטיסייה, נעביר את הנתונים למסך הבית
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTemplateClick(template);
            }
        });
    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    public static class TemplateViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDuration;

        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_template_title);
            tvDuration = itemView.findViewById(R.id.tv_template_duration);
        }
    }
}