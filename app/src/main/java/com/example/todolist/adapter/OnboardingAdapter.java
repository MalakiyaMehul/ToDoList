package com.example.todolist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.model.OnboardingModel;

import java.util.ArrayList;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>{
    private ArrayList<OnboardingModel> list;
    public OnboardingAdapter(ArrayList<OnboardingModel> list)
    {
        this.list = list;
    }
    @NonNull
    @Override
    public OnboardingAdapter.OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingAdapter.OnboardingViewHolder holder, int position) {
        OnboardingModel model = list.get(position);
        holder.imageView.setImageResource(model.getImage());
        holder.txtTitle.setText(model.getTitle());
        holder.txtDescription.setText(model.getDescription());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class OnboardingViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView txtTitle, txtDescription;
        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgOnboarding);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDescription = itemView.findViewById(R.id.txtDescription);
        }
    }
}
