package com.example.themagicofknowledge.adapter;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserParent;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<UserParent> userList;
    private final OnUserClickListener onUserClickListener;

    public UserAdapter(@Nullable final OnUserClickListener onUserClickListener) {
        userList = new ArrayList<>();
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public UserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserParent user = userList.get(position);
        if (user == null) return;

        // שם מלא
        holder.tvName.setText(user.getFirstName() + " " + user.getLastName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvPhone.setText(user.getPhone());

        // ראשי תיבות
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials += user.getFirstName().charAt(0);
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initials += user.getLastName().charAt(0);
        }
        holder.tvInitials.setText(initials);

        // הצגת תג "מנהל" אם המשתמש מנהל
        if (user.isAdmin()) {
            holder.chipRole.setVisibility(View.VISIBLE);
        } else {
            holder.chipRole.setVisibility(View.GONE);
        }

        // לחיצה על הכרטיס
        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(user);
            }
        });

        // לחיצה ארוכה
        holder.itemView.setOnLongClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onLongUserClick(user);
            }
            return true;
        });

        // ===== כפתור 3 הנקודות - תפריט פעולות =====
        holder.btnUserActions.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.inflate(R.menu.user_actions_menu);

            // הסתרת אפשרויות לפי המצב
            Menu menu = popupMenu.getMenu();
            if (user.isAdmin()) {
                // משתמש מנהל - מסתירים "הפוך למנהל"
                menu.findItem(R.id.action_make_admin).setVisible(false);
            } else {
                // משתמש רגיל - מסתירים "הסר הרשאת מנהל"
                menu.findItem(R.id.action_remove_admin).setVisible(false);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_make_admin) {
                    if (onUserClickListener != null) {
                        onUserClickListener.onMakeAdmin(user);
                    }
                    return true;
                } else if (id == R.id.action_remove_admin) {
                    if (onUserClickListener != null) {
                        onUserClickListener.onRemoveAdmin(user);
                    }
                    return true;
                } else if (id == R.id.action_delete) {
                    if (onUserClickListener != null) {
                        onUserClickListener.onDeleteUser(user);
                    }
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setUserList(List<UserParent> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }

    public void addUser(UserParent user) {
        userList.add(user);
        notifyItemInserted(userList.size() - 1);
    }

    public void updateUser(UserParent user) {
        if (user == null || user.getId() == null) return;
        for (int i = 0; i < userList.size(); i++) {
            if (Objects.equals(userList.get(i).getId(), user.getId())) {
                userList.set(i, user);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void removeUser(UserParent user) {
        if (user == null || user.getId() == null) return;
        for (int i = 0; i < userList.size(); i++) {
            if (Objects.equals(userList.get(i).getId(), user.getId())) {
                userList.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    // ===== Interface עם 5 פעולות =====
    public interface OnUserClickListener {
        void onUserClick(UserParent user);
        void onLongUserClick(UserParent user);
        void onMakeAdmin(UserParent user);
        void onRemoveAdmin(UserParent user);
        void onDeleteUser(UserParent user);
    }

    // ===== ViewHolder עם הכפתור החדש =====
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvInitials;
        View chipRole;
        MaterialButton btnUserActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_user_name);
            tvEmail = itemView.findViewById(R.id.tv_item_user_email);
            tvPhone = itemView.findViewById(R.id.tv_item_user_phone);
            tvInitials = itemView.findViewById(R.id.tv_user_initials);
            chipRole = itemView.findViewById(R.id.chip_user_role);
            btnUserActions = itemView.findViewById(R.id.btn_user_actions);
        }
    }
}