package com.example.todolist.Dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.todolist.databinding.ChangePasswordDialogBoxBinding;
import com.google.firebase.database.annotations.Nullable;

public class ChangeUserPassword extends DialogFragment {
    private ChangePasswordDialogBoxBinding binding;
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding=ChangePasswordDialogBoxBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }
}
