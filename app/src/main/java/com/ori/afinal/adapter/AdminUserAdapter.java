package com.ori.afinal.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ori.afinal.R;
import com.ori.afinal.model.User;
import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.AdminUserViewHolder> {

    private List<User> users = new ArrayList<>();
    private OnUserActionListener listener;

    private String currentUserEmail = "";

    public interface OnUserActionListener {
        void onDeleteClick(User user);
        void onMakeAdminClick(User user);
        void onRemoveAdminClick(User user); // הפונקציה החדשה
        void onEditClick(User user);
    }

    public AdminUserAdapter(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email != null ? email : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new AdminUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminUserViewHolder holder, int position) {
        User user = users.get(position);

        String name = user.getFullName() != null ? user.getFullName() : (user.getFname() + " " + user.getLname());
        holder.tvName.setText(name != null ? name.trim() : "משתמש ללא שם");
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        if (name != null && !name.trim().isEmpty()) {
            holder.tvAvatar.setText(String.valueOf(name.trim().charAt(0)).toUpperCase());
        }

        // איפוס תצוגה ברירת מחדל
        holder.btnDelete.setVisibility(View.VISIBLE);
        holder.btnEdit.setVisibility(View.VISIBLE);
        holder.btnMakeAdmin.setVisibility(View.VISIBLE);
        holder.btnRemoveAdmin.setVisibility(View.GONE); // מוסתר כברירת מחדל
        holder.tvAdminBadge.setVisibility(View.GONE);

        boolean iAmSuperAdmin = "ori@gmail.com".equalsIgnoreCase(currentUserEmail);
        boolean targetIsSuperAdmin = "ori@gmail.com".equalsIgnoreCase(user.getEmail());
        boolean targetIsAdmin = user.getAdmin() != null && user.getAdmin();

        if (targetIsSuperAdmin) {
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
            holder.tvAdminBadge.setText("מנהל ראשי 👑");
            holder.tvAdminBadge.setTextColor(Color.parseColor("#7C3AED"));

            holder.btnDelete.setVisibility(View.GONE);
            holder.btnMakeAdmin.setVisibility(View.GONE);

            if (!iAmSuperAdmin) {
                holder.btnEdit.setVisibility(View.GONE);
            }

        } else if (targetIsAdmin) {
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
            holder.tvAdminBadge.setText("מנהל");
            holder.tvAdminBadge.setTextColor(Color.parseColor("#059669"));

            holder.btnMakeAdmin.setVisibility(View.GONE);

            if (iAmSuperAdmin) {
                // רק מנהל ראשי רואה את כפתור הסרת הניהול למנהל רגיל
                holder.btnRemoveAdmin.setVisibility(View.VISIBLE);
            } else {
                holder.btnEdit.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
            }
        }

        // חיבור הכפתורים
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(user));
        holder.btnMakeAdmin.setOnClickListener(v -> listener.onMakeAdminClick(user));
        holder.btnRemoveAdmin.setOnClickListener(v -> listener.onRemoveAdminClick(user));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class AdminUserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvAvatar, tvAdminBadge;
        ImageButton btnDelete, btnMakeAdmin, btnRemoveAdmin, btnEdit;

        public AdminUserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_admin_user_name);
            tvEmail = itemView.findViewById(R.id.tv_admin_user_email);
            tvAvatar = itemView.findViewById(R.id.tv_admin_avatar);
            tvAdminBadge = itemView.findViewById(R.id.tv_admin_badge);
            btnDelete = itemView.findViewById(R.id.btn_admin_delete_user);
            btnMakeAdmin = itemView.findViewById(R.id.btn_admin_make_admin);
            btnRemoveAdmin = itemView.findViewById(R.id.btn_admin_remove_admin);
            btnEdit = itemView.findViewById(R.id.btn_admin_edit_user);
        }
    }
}