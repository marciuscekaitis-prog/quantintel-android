package com.quantintel.diagnostic;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

public class DiagnosticActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView message = new TextView(this);
        message.setText("INSTALL TEST PASSED\n\nYour Android phone installed and launched a simple APK built by our GitHub workflow.\n\nNext: diagnose the QUANTINTEL app package.");
        message.setTextSize(20);
        message.setTextColor(Color.BLACK);
        message.setGravity(Gravity.CENTER);
        message.setPadding(32, 32, 32, 32);
        setContentView(message);
    }
}
