package com.nosferatu.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class NosferatuLauncher extends Activity {
    private static final String TAG = "NosferatuLauncher";
    private static final String READER_PACKAGE = "org.koreader.launcher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchReader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        launchReader();
    }

    private void launchReader() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(READER_PACKAGE);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                startActivity(intent);
                Log.d(TAG, "Reader opened OK");
            } catch (Exception e) {
                Log.e(TAG, "Error opening reader app: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Reader not found in system: " + READER_PACKAGE);
        }
    }
}
