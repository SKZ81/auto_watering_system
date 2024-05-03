package com.skz81.simplenfc2http;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DebugTagInjector {

    private final static String TAG = "AutoWatS-debugTagInjector";
    private List<String> uuids;
    private AlertDialog uuidDialog;
    private MainActivity mainActivity;

    public DebugTagInjector(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.uuids = new ArrayList<>();

        AppConfiguration config = AppConfiguration.instance();
        new SendToServerTask(new SendToServerTask.ReplyListener() {
            @Override
            public void onRequestFailure(int errorCode, String data) {
                if (errorCode == 404) {
                    onError("No UUID available in database. Please create tag(s).");
                }
                onError("Server error " + errorCode + ((data != null) ? ", " + data : ""));
            }
            @Override
            public void onReplyFromServer(String data) {
                try {
                    uuids.clear();
                    JSONArray uuid_list = new JSONArray(data);
                    JSONInfoAdapter jsonAdapter = new JSONInfoAdapter();

                    jsonAdapter.addAttributeRegex(
                            "list\\[\\d+\\]\\.UUID", String.class,
                            value -> uuids.add((String) value),
                            () -> {});
                    jsonAdapter.parseJSONArray("list", uuid_list);
                    Log.i(TAG, "uuid list:" + uuids);

                    displayUUIDsDialog();

                } catch (JSONException e) {
                    onError("Error parsing JSON plant info: " + e.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                mainActivity.toastDisplay(TAG, "Error fetching plant info: " + error, true);
                uuids.clear();
            }
        }).GET(config.getServerURL() + config.DEBUG_GET_UUIDS, new HashMap<>());
    }

    private void displayUUIDsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        Spinner uuidSpinner = new Spinner(mainActivity);

        builder.setTitle("Inject Tag")
                .setMessage("Please scan the tag to proceed...")
                .setPositiveButton("Inject", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String uuid = (String) uuidSpinner.getSelectedItem();
                        Log.i(TAG, "DEBUG : inject UUID:" + uuid);
                        mainActivity.simulateTagScan(uuid);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        LinearLayout ll = new LinearLayout(mainActivity);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(30, 30, 30, 30);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(llParam);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_item, uuids);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uuidSpinner.setAdapter(adapter);
        ll.addView(uuidSpinner);
        builder.setView(ll);
        uuidDialog = builder.create();

        uuidDialog.show();
    }

}
