package com.example.todolist.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.example.todolist.databinding.ChangeUserProfileImageBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

public class ChangeUserProfileImage extends BottomSheetDialogFragment {
    private ChangeUserProfileImageBinding binding;
    public static final String REQUEST_KEY = "PROFILE_IMAGE_REQUEST";
    public static final String BUNDLE_IMAGE_URI = "BUNDLE_IMAGE_URI";
    private Uri cameraImageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ChangeUserProfileImageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.txtCamera.setOnClickListener(v -> openCamera());
        binding.txtGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        binding.txtGoogleDrive.setOnClickListener(v -> openGoogleDrive());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    sendResultAndDismiss(cameraImageUri);
                }
            });
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    sendResultAndDismiss(uri);
                }
            });
    private final ActivityResultLauncher<Intent> driveLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri driveUri = result.getData().getData();
                    if (driveUri != null) {
                        sendResultAndDismiss(driveUri);
                    }
                }
            });
    private void openCamera() {
        try {
            File imageFile = new File(requireContext().getCacheDir(), "profile_camera_temp.jpg");
            cameraImageUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", imageFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGoogleDrive() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, false);
        driveLauncher.launch(intent);
    }

    private void sendResultAndDismiss(Uri uri) {
        Bundle result = new Bundle();
        result.putString(BUNDLE_IMAGE_URI, uri.toString());
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }
}
