package com.flutteride.codweva;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private WifiManager wifiManager;
    private ListView listView;
    private TextView tvStatus, tvCurrentGuess;
    private Button btnScan, btnStartAttack;

    private ArrayList<String> wifiListNames;
    private ArrayAdapter<String> adapter;
    private List<ScanResult> scanResults;
    private ArrayList<String> customWordlist;

    private String selectedTargetSSID = "";
    private int currentGuessIndex = 0;
    private boolean isAttacking = false;
    private Handler handler = new Handler();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // بناء الواجهة برمجياً
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 30, 30, 30);

        tvStatus = new TextView(this);
        tvStatus.setText("الحالة: جاري التحقق من الأذونات...");
        tvStatus.setTextSize(16);
        mainLayout.addView(tvStatus);
        
        tvCurrentGuess = new TextView(this);
        tvCurrentGuess.setText("انتظار اختيار شبكة...");
        tvCurrentGuess.setTextSize(14);
        tvCurrentGuess.setTextColor(Color.BLUE);
        mainLayout.addView(tvCurrentGuess);

        btnScan = new Button(this);
        btnScan.setText("بحث عن الشبكات");
        mainLayout.addView(btnScan);

        listView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600);
        listView.setLayoutParams(listParams);
        mainLayout.addView(listView);

        btnStartAttack = new Button(this);
        btnStartAttack.setText("ابدأ التخمينات");
        btnStartAttack.setEnabled(false);
        mainLayout.addView(btnStartAttack);

        setContentView(mainLayout);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiListNames = new ArrayList<>();
        scanResults = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiListNames);
        listView.setAdapter(adapter);

        setupWordlist();

        // طلب الإذونات فور فتح التطبيق
        checkAndRequestPermissions();

        btnScan.setOnClickListener(v -> checkAndRequestPermissions());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (scanResults != null && position < scanResults.size()) {
                selectedTargetSSID = scanResults.get(position).SSID;
                tvStatus.setText("الشبكة المحددة: " + selectedTargetSSID);
                btnStartAttack.setEnabled(true);
            }
        });

        btnStartAttack.setOnClickListener(v -> {
            if (!selectedTargetSSID.isEmpty() && !isAttacking) {
                startAttackingLoop();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            tvStatus.setText("الحالة: الأذونات مقبولة، اضغط بحث");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "تم منح إذن الموقع بنجاح", Toast.LENGTH_SHORT).show();
                tvStatus.setText("الحالة: جاهز للبحث");
            } else {
                Toast.makeText(this, "يجب الموافقة على إذن الموقع ليعمل الفحص", Toast.LENGTH_LONG).show();
                tvStatus.setText("الحالة: تم رفض إذن الموقع");
            }
        }
    }

    private void setupWordlist() {
        customWordlist = new ArrayList<>();
        customWordlist.add("10001000"); customWordlist.add("20002000");
        customWordlist.add("30003000"); customWordlist.add("40004000");
        customWordlist.add("50005000"); customWordlist.add("60006000");
        customWordlist.add("70007000"); customWordlist.add("80008000");
        customWordlist.add("90009000"); customWordlist.add("90008000");
    }

    private void scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        try {
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            boolean success = wifiManager.startScan();
            if (success) {
                Toast.makeText(this, "جاري البحث عن الشبكات...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في بدء البحث", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            scanResults = wifiManager.getScanResults();
            wifiListNames.clear();
            for (ScanResult result : scanResults) {
                if (result.SSID != null && !result.SSID.isEmpty()) {
                    wifiListNames.add(result.SSID);
                }
            }
            adapter.notifyDataSetChanged();
            try {
                unregisterReceiver(this);
            } catch (Exception e) {}
        }
    };

    private void startAttackingLoop() {
        isAttacking = true;
        currentGuessIndex = 0;
        btnStartAttack.setEnabled(false);
        runNextGuess();
    }

    private void runNextGuess() {
        if (currentGuessIndex < customWordlist.size()) {
            String guess = customWordlist.get(currentGuessIndex);
            tvCurrentGuess.setText("تجربة: " + guess);
            tvStatus.setText("الهجوم: " + (currentGuessIndex + 1) + "/" + customWordlist.size());

            handler.postDelayed(() -> {
                currentGuessIndex++;
                runNextGuess();
            }, 1000);
        } else {
            tvStatus.setText("تم الانتهاء من الاختبار.");
            tvCurrentGuess.setText("الحالة: انتهى");
            btnStartAttack.setEnabled(true);
            isAttacking = false;
        }
    }
}
