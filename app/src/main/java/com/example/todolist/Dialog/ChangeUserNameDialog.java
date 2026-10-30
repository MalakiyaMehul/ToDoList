package com.example.todolist.Dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.todolist.R;
import com.example.todolist.databinding.UserNameChangeDialogBoxBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.Nullable;

public class ChangeUserNameDialog extends DialogFragment {
    public static final String REQUEST_KEY = "change_username_request";
    public static final String BUNDLE_USER_NAME = "new_user_name";

    private UserNameChangeDialogBoxBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = UserNameChangeDialogBoxBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid());

            userRef.child("userName").get()
                    .addOnSuccessListener(snapshot -> {
                        if (binding != null && snapshot.exists()) {
                            String name = snapshot.getValue(String.class);
                            binding.edtUserName.setText(name);
                        }
                    });

            binding.btnCancel.setOnClickListener(v -> dismiss());

            binding.btnEditTask.setOnClickListener(v -> {
                String newName = binding.edtUserName.getText().toString().trim();

                if (TextUtils.isEmpty(newName)) {
                    binding.edtUserName.setError("Name is required");
                    return;
                }

                binding.btnEditTask.setEnabled(false);

                userRef.child("userName").setValue(newName);
                userRef.child("username").setValue(newName);
                userRef.child("name").setValue(newName)
                        .addOnSuccessListener(unused -> {
                            if (!isAdded()) return;
                            
                            // Notify ProfileFragment about the change
                            Bundle result = new Bundle();
                            result.putString(BUNDLE_USER_NAME, newName);
                            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                            
                            Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                            dismiss();
                        })
                        .addOnFailureListener(error -> {
                            if (!isAdded()) return;
                            binding.btnEditTask.setEnabled(true);
                            Toast.makeText(requireContext(), "Failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
        }
    }
}
