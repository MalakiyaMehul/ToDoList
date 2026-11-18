package com.example.todolist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class InputField extends LinearLayout {
    public static final int STRENGTH_NONE = 0;
    public static final int STRENGTH_LOW = 1;
    public static final int STRENGTH_MEDIUM = 2;
    public static final int STRENGTH_HIGH = 3;
    public static final int STRENGTH_STRONG = 4;

    private TextView txtLabel;
    private TextView txtRightLabel;
    private TextView txtStatusMessage;
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
        txtStatusMessage = findViewById(R.id.txtStatusMessage);
        edtInput = findViewById(R.id.edtInput);
        imgStartIcon = findViewById(R.id.imgStartIcon);
        imgEndIcon = findViewById(R.id.imgEndIcon);
        layoutStrengthBars = findViewById(R.id.layoutStrengthBars);
        barStrength1 = findViewById(R.id.barStrength1);
        barStrength2 = findViewById(R.id.barStrength2);
        barStrength3 = findViewById(R.id.barStrength3);
        barStrength4 = findViewById(R.id.barStrength4);

        if (edtInput != null) {
            edtInput.setSingleLine(true);
            edtInput.setMaxLines(1);
            edtInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (layoutStrengthBars != null && layoutStrengthBars.getVisibility() == VISIBLE) {
                        updateStrengthBars(s != null ? s.toString() : "");
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
            int currentImeOptions = edtInput.getImeOptions();
            edtInput.setInputType(type);
            edtInput.setImeOptions(currentImeOptions);
            boolean isPassword = (type & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0
                    || (type & InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0;
            if (isPassword) {
                enablePasswordToggle();
            }
        }
    }

    public void setImeOptions(int imeOptions) {
        if (edtInput != null) {
            edtInput.setImeOptions(imeOptions);
        }
    }

    public void focusInput() {
        if (edtInput != null) {
            edtInput.requestFocus();
            if (edtInput.getText() != null) {
                edtInput.setSelection(edtInput.getText().length());
            }
            edtInput.post(() -> {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(edtInput, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    public void setOnEnterActionListener(int imeOptions, Runnable action) {
        if (edtInput == null) {
            return;
        }
        edtInput.setImeOptions(imeOptions);
        edtInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean isImeAction = actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEND
                    || (actionId == EditorInfo.IME_ACTION_UNSPECIFIED && event == null);
            if (isImeAction) {
                if (action != null) {
                    action.run();
                }
                return true;
            }
            if (actionId == EditorInfo.IME_NULL && event != null) {
                if (event.getAction() == KeyEvent.ACTION_UP && action != null) {
                    action.run();
                }
                return true;
            }
            return false;
        });
        edtInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP && action != null) {
                    action.run();
                }
                return true;
            }
            return false;
        });
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
            imgEndIcon.setFocusable(false);
            imgEndIcon.setFocusableInTouchMode(false);
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

    public void setStatusMessage(String message, int color) {
        if (txtStatusMessage == null) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            txtStatusMessage.setText("");
            txtStatusMessage.setVisibility(GONE);
        } else {
            txtStatusMessage.setText(message);
            txtStatusMessage.setTextColor(color);
            txtStatusMessage.setVisibility(VISIBLE);
        }
    }

    public void clearStatusMessage() {
        setStatusMessage(null, 0);
    }

    public void addTextChangedListener(TextWatcher watcher) {
        if (edtInput != null && watcher != null) {
            edtInput.addTextChangedListener(watcher);
        }
    }

    public void showStrengthBar(boolean show) {
        if (layoutStrengthBars != null) {
            layoutStrengthBars.setVisibility(show ? VISIBLE : GONE);
            if (show && edtInput != null && edtInput.getText() != null) {
                updateStrengthBars(edtInput.getText().toString());
            } else if (!show) {
                clearStatusMessage();
            }
        }
    }

    public static int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return STRENGTH_NONE;
        }
        int length = password.length();
        if (length < 6) {
            return STRENGTH_LOW;
        }

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < length; i++) {
            char c = password.charAt(i);
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        int categories = (hasLower ? 1 : 0)
                + (hasUpper ? 1 : 0)
                + (hasDigit ? 1 : 0)
                + (hasSpecial ? 1 : 0);

        if ((length >= 8 && categories == 4) || (length >= 10 && categories >= 3)) {
            return STRENGTH_STRONG;
        }
        if ((length >= 8 && categories >= 3) || (length >= 10 && categories >= 2)) {
            return STRENGTH_HIGH;
        }
        if (length >= 8 || categories >= 2) {
            return STRENGTH_MEDIUM;
        }
        return STRENGTH_LOW;
    }

    private void updateStrengthBars(String password) {
        if (barStrength1 == null) {
            return;
        }
        int level = calculatePasswordStrength(password);
        if (level == STRENGTH_NONE) {
            applyBarState(barStrength1, false, 0);
            applyBarState(barStrength2, false, 0);
            applyBarState(barStrength3, false, 0);
            applyBarState(barStrength4, false, 0);
            clearStatusMessage();
            return;
        }

        int color;
        String label;
        switch (level) {
            case STRENGTH_STRONG:
                color = ContextCompat.getColor(getContext(), R.color.strength_strong);
                label = "Password strength: Strong";
                break;
            case STRENGTH_HIGH:
                color = ContextCompat.getColor(getContext(), R.color.strength_high);
                label = "Password strength: High";
                break;
            case STRENGTH_MEDIUM:
                color = ContextCompat.getColor(getContext(), R.color.strength_medium);
                label = "Password strength: Medium";
                break;
            default:
                color = ContextCompat.getColor(getContext(), R.color.strength_low);
                label = "Password strength: Low";
                break;
        }

        applyBarState(barStrength1, level >= STRENGTH_LOW, color);
        applyBarState(barStrength2, level >= STRENGTH_MEDIUM, color);
        applyBarState(barStrength3, level >= STRENGTH_HIGH, color);
        applyBarState(barStrength4, level >= STRENGTH_STRONG, color);
        setStatusMessage(label, color);
    }

    private void applyBarState(View bar, boolean active, int activeColor) {
        if (bar == null) {
            return;
        }
        if (!active) {
            bar.setBackgroundResource(R.drawable.bg_strength_bar_inactive);
            return;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(2f * getResources().getDisplayMetrics().density);
        drawable.setColor(activeColor);
        bar.setBackground(drawable);
    }

    public void enablePasswordToggle() {
        if (imgEndIcon != null && edtInput != null) {
            imgEndIcon.setFocusable(false);
            imgEndIcon.setFocusableInTouchMode(false);
            imgEndIcon.setVisibility(VISIBLE);
            imgEndIcon.setImageResource(isPasswordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            imgEndIcon.setOnClickListener(v -> {
                isPasswordVisible = !isPasswordVisible;
                int selection = edtInput.getSelectionEnd();
                int currentImeOptions = edtInput.getImeOptions();
                if (isPasswordVisible) {
                    edtInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgEndIcon.setImageResource(R.drawable.ic_eye_off);
                } else {
                    edtInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgEndIcon.setImageResource(R.drawable.ic_eye);
                }
                edtInput.setImeOptions(currentImeOptions);
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
