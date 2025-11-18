package com.example.photoviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private EditText searchEdit;
    private PostAdapter adapter;

    private SwitchCompat switchDarkMode;
    private SharedPreferences prefs;

    private Button btnFilterAll, btnFilterEnter, btnFilterExit;

    private List<PostItem> originalList = new ArrayList<>();
    private List<PostItem> filteredList = new ArrayList<>();

    private final String SERVER_URL = "http://10.0.2.2:8000/api_root/Post/";

    // 🔄 자동 감지를 위한 Handler
    private Handler handler = new Handler();
    private Runnable autoRefreshTask;
    private int lastCount = 0; // 🔥 새 게시글 감지 비교용

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        applySavedNightMode();

        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        searchEdit = findViewById(R.id.searchEdit);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterEnter = findViewById(R.id.btnFilterEnter);
        btnFilterExit = findViewById(R.id.btnFilterExit);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        int mode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switchDarkMode.setChecked(mode == AppCompatDelegate.MODE_NIGHT_YES);
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            int newMode = isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;
            prefs.edit().putInt("night_mode", newMode).apply();
            AppCompatDelegate.setDefaultNightMode(newMode);
            recreate();
        });

        Button btnNewPost = findViewById(R.id.btnNewPost);
        btnNewPost.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PostUploadActivity.class))
        );

        btnFilterAll.setOnClickListener(v -> applyFilter("all"));
        btnFilterEnter.setOnClickListener(v -> applyFilter("enter"));
        btnFilterExit.setOnClickListener(v -> applyFilter("exit"));

        loadPosts();
        startAutoRefresh(); // 🔥 자동 감지 시작

        swipeRefresh.setOnRefreshListener(() -> {
            loadPosts();
            swipeRefresh.setRefreshing(false);
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applySavedNightMode() {
        int saved = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(saved);
    }

    private void loadPosts() {
        new LoadPostsTask().execute(SERVER_URL);
    }

    // 🔥 자동 감지 기능 (3초마다 실행)
    private void startAutoRefresh() {
        autoRefreshTask = new Runnable() {
            @Override
            public void run() {
                new LoadPostsTask().execute(SERVER_URL);
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(autoRefreshTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoRefreshTask);
    }

    // 🔔 새 게시글 알림 (소리 + 팝업)
    private void triggerAlert() {
        try {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP);
        } catch (Exception ignored) {}

        runOnUiThread(() -> {
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("새 침입 감지 발생!")
                    .setMessage("서버에서 새로운 이벤트가 감지되었습니다.")
                    .setPositiveButton("확인", null)
                    .show();
        });
    }

    private void applyFilter(String type) {
        filteredList.clear();

        for (PostItem item : originalList) {
            String text = item.getText();   // ← 여기 수정됨

            switch (type) {
                case "enter":
                    if (text.contains("입장")) filteredList.add(item);
                    break;
                case "exit":
                    if (text.contains("퇴장")) filteredList.add(item);
                    break;
                default:
                    filteredList.add(item);
                    break;
            }
        }

        adapter.updateData(filteredList);
    }


    private class LoadPostsTask extends AsyncTask<String, Void, List<PostItem>> {
        @Override
        protected List<PostItem> doInBackground(String... urls) {
            return PostFetcher.fetchPosts();
        }

        @Override
        protected void onPostExecute(List<PostItem> posts) {
            if (posts == null) return;

            boolean isNewPost = posts.size() > lastCount;
            lastCount = posts.size();

            originalList.clear();
            originalList.addAll(posts);

            Collections.sort(originalList, (a, b) ->
                    b.getCreatedDate().compareTo(a.getCreatedDate()));

            applyFilter("all");

            if (isNewPost) {
                triggerAlert();  // 🔥 새로운 게시글 감지 시 알림!
            }
        }
    }
}
