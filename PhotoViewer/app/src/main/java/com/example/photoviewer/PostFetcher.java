package com.example.photoviewer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PostFetcher {

    private static final String SERVER_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final String TAG = "PostFetcher";

    public static List<PostItem> fetchPosts() {
        List<PostItem> list = new ArrayList<>();

        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                    jsonBuilder.append(line);

                JSONArray array = new JSONArray(jsonBuilder.toString());

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    int id = obj.optInt("id", 0);
                    String title = obj.optString("title", "제목 없음");
                    String text = obj.optString("text", "");
                    String created = obj.optString("created_date", "");
                    String published = obj.optString("published_date", "");
                    String image = obj.optString("image", "");
                    String author = obj.optString("author", "익명");

                    list.add(new PostItem(id, title, text, created, published, image, author));
                }

            } else {
                Log.e(TAG, "서버 응답 코드 : " + conn.getResponseCode());
            }

        } catch (Exception e) {
            Log.e(TAG, "POST 불러오기 실패 : " + e.getMessage());
        }

        return list;
    }
}
