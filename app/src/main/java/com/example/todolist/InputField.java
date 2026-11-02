package com.example.todolist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class InputField extends LinearLayout {
    private TextView txtLabel;
    private TextView txtRightLabel;
    private EditText edtInput;
    private ImageView imgStartIcon;
    private ImageView imgEndIcon;
    private View layoutStrengthBars;
    private View barStrength1, barStrength2, barStrength3, barStrength4;
    private boolean isPasswordVisible = false;

    public InputField(Context context) {
        super(context);
        init(context);
    }

    public InputField(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InputField(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_input_field, this, true);
        txtLabel = findViewById(R.id.txtLable);
        txtRightLabel = findViewById(R.id.txtRightLabel);
        edtInput = findViewById(R.id.edtInput);
        imgStartIcon = findViewById(R.id.imgStartIcon);
        imgEndIcon = findViewById(R.id.imgEndIcon);
        layoutStrengthBars = findViewById(R.id.layoutStrengthBars);
        barStrength1 = findViewById(R.id.barStrength1);
        barStrength2 = findViewById(R.id.barStrength2);
        barStrength3 = findViewById(R.id.barStrength3);
        barStrength4 = findViewById(R.id.barStrength4);

        if (edtInput != null) {
            edtInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (layoutStrengthBars != null && layoutStrengthBars.getVisibility() == VISIBLE) {
                        updateStrengthBars(s != null ? s.length() : 0);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    public void setLabel(String label) {
        if (txtLabel != null) {
            txtLabel.setText(label);
        }
        if (label != null) {
            String lower = label.toLowerCase();
            if (lower.contains("user")) {
                setStartIcon(R.drawable.ic_mention);
            } else if (lower.contains("email")) {
                setStartIcon(R.drawable.ic_mail);
            } else if (lower.contains("confirm")) {
                setStartIcon(R.drawable.ic_shield_check);
                enablePasswordToggle();
            } else if (lower.contains("password") || lower.contains("master")) {
                setStartIcon(R.drawable.ic_lock);
                enablePasswordToggle();
            }
        }
    }

    public void setRightLabel(String rightLabel) {
        if (txtRightLabel != null) {
            if (TextUtils.isEmpty(rightLabel)) {
                txtRightLabel.setVisibility(GONE);
            } else {
                txtRightLabel.setText(rightLabel);
                txtRightLabel.setVisibility(VISIBLE);
            }
        }
    }

    public void setRightLabelColor(int color) {
        if (txtRightLabel != null) {
            txtRightLabel.setTextColor(color);
        }
    }

    public void setHint(String hint) {
        if (edtInput != null) {
            edtInput.setHint(hint);
        }
    }

    public String getText() {
        if (edtInput != null && edtInput.getText() != null) {
            return edtInput.getText().toString().trim();
        }
        return "";
    }

    public void setInputType(int type) {
        if (edtInput != null) {
            edtInput.setInputType(type);
            boolean isPassword = (type & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0
                    || (type & InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0;
            if (isPassword) {
                enablePasswordToggle();
            }
        }
    }

    public void setStartIcon(int resId) {
        if (imgStartIcon != null) {
            if (resId != 0) {
                imgStartIcon.setImageResource(resId);
                imgStartIcon.setVisibility(VISIBLE);
            } else {
                imgStartIcon.setVisibility(GONE);
            }
        }
    }

    public void setEndIcon(int resId) {
        if (imgEndIcon != null) {
            if (resId != 0) {
                imgEndIcon.setImageResource(resId);
                imgEndIcon.setVisibility(VISIBLE);
            } else {
                imgEndIcon.setVisibility(GONE);
            }
        }
    }

    public void setEndIconTint(int color) {
        if (imgEndIcon != null) {
            imgEndIcon.setImageTintList(ColorStateList.valueOf(color));
        }
    }

    public void showStrengthBar(boolean show) {
        if (layoutStrengthBars != null) {
            layoutStrengthBars.setVisibility(show ? VISIBLE : GONE);
            if (show && edtInput != null) {
                updateStrengthBars(edtInput.getText().length());
            }
        }
    }

    private void updateStrengthBars(int length) {
        if (barStrength1 == null) return;
        int active = R.drawable.bg_strength_bar_active;
        int inactive = R.drawable.bg_strength_bar_inactive;

        barStrength1.setBackgroundResource(length >= 2 ? active : inactive);
        barStrength2.setBackgroundResource(length >= 6 ? active : inactive);
        barStrength3.setBackgroundResource(length >= 8 ? active : inactive);
        barStrength4.setBackgroundResource(length >= 10 ? active : inactive);
    }

    public void enablePasswordToggle() {
        if (imgEndIcon != null && edtInput != null) {
            imgEndIcon.setVisibility(VISIBLE);
            imgEndIcon.setImageResource(R.drawable.ic_eye);
            imgEndIcon.setOnClickListener(v -> {
                isPasswordVisible = !isPasswordVisible;
                int selection = edtInput.getSelectionEnd();
                if (isPasswordVisible) {
                    edtInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgEndIcon.setImageResource(R.drawable.ic_eye_off);
                } else {
                    edtInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgEndIcon.setImageResource(R.drawable.ic_eye);
                }
                if (selection >= 0) {
                    edtInput.setSelection(selection);
                }
            });
        }
    }

    public EditText getEditText() {
        return edtInput;
    }
}
