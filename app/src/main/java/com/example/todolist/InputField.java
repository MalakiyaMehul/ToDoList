package com.example.todolist;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InputField extends LinearLayout {
    private TextView txtLabel;
    private EditText edtInput;

    public InputField(Context context) {
        super(context);
        init(context);
    }
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_input_field,this,true);
        txtLabel = findViewById(R.id.txtLable);
        edtInput = findViewById(R.id.edtInput);
    }
    public void setLabel(String label) {
        txtLabel.setText(label);
    }
    public void setHint(String hint) {
        edtInput.setHint(hint);
    }
    public String getText() {
        return edtInput.getText().toString().trim();
    }
    public void setInputType(int type) {
        edtInput.setInputType(type);
    }
    public InputField(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InputField(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
}
