package com.example.themagicofknowledge.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.utils.ImageUtil;

import java.util.List;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private List<UserChild> children;
    private OnChildClickListener listener;
    private OnChildLongClickListener longClickListener;

    // ממשק חדש
    public interface OnChildEditListener {
        void onChildEdit(UserChild child);
    }

    // קונסטרקטור מעודכן
    private OnChildEditListener editListener;

    public ChildAdapter(List<UserChild> children, OnChildClickListener listener,
                        OnChildLongClickListener longClickListener, OnChildEditListener editListener) {
        this.children = children;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // טעינת ה-Layout החדש שעיצבנו (הריבוע)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        UserChild child = children.get(position);

        // 1. עדכון שם וגיל
        holder.tvName.setText(child.getName());
        holder.tvAge.setText("גיל: " + child.getAge());

        ImageUtil.loadAvatar(holder.itemView.getContext(), holder.ivAvatar, child.getAvatar());

        // 3. הגדרת לחיצה רגילה (לבחירת הילד)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChildClick(child);
            }
        });

        // 4. הגדרת לחיצה ארוכה (למחיקה או עריכה)
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onChildLongClick(child);
            }
            return true;
        });

        holder.btnEditChild.setOnClickListener(v -> {
            if (editListener != null) editListener.onChildEdit(child);
        });
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    // ממשקים להאזנה ללחיצות
    public interface OnChildClickListener {
        void onChildClick(UserChild child);
    }

    public interface OnChildLongClickListener {
        void onChildLongClick(UserChild child);
    }

    // ה-ViewHolder שמחזיק את הרכיבים של כל ריבוע
    public static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAge;
        ImageView ivAvatar;
        View btnEditChild;

        public ChildViewHolder(@NonNull View view) {
            super(view);
            tvName = view.findViewById(R.id.tvChildName);
            tvAge = view.findViewById(R.id.tvChildAge);
            ivAvatar = view.findViewById(R.id.ivChildAvatar);
            btnEditChild = view.findViewById(R.id.btnEditChild);
        }
    }
}