package com.example.themagicofknowledge.screens; //

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import java.util.List;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private List<UserChild> children;
    private OnChildClickListener listener;

    public interface OnChildClickListener {
        void onChildClick(UserChild child);
    }

    public ChildAdapter(List<UserChild> children, OnChildClickListener listener) {
        this.children = children;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        UserChild child = children.get(position);
        holder.tvName.setText(child.getName());
        holder.tvAge.setText("גיל: " + child.getAge());
        holder.itemView.setOnClickListener(v -> listener.onChildClick(child));
    }

    @Override
    public int getItemCount() {
        return children != null ? children.size() : 0;
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAge;
        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvChildName);
            tvAge = itemView.findViewById(R.id.tvChildAge);
        }
    }
}