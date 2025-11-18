package com.example.photoviewer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

public class PostDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        ImageView imageView = findViewById(R.id.detail_image);
        TextView titleText = findViewById(R.id.detail_title);
        TextView dateText  = findViewById(R.id.detail_date);
        TextView bodyText  = findViewById(R.id.detail_body);
        Button btnEdit     = findViewById(R.id.btnEdit);
        Button btnDelete   = findViewById(R.id.btnDelete);

        Intent intent = getIntent();
        final int id         = intent.getIntExtra("id", -1);
        final String title   = intent.getStringExtra("title");
        final String date    = intent.getStringExtra("date");
        final String body    = intent.getStringExtra("body");

        String img = intent.getStringExtra("image");
        if (img == null || img.isEmpty()) {
            img = intent.getStringExtra("imageUrl");
        }
        final String imageUrl = FileUtils.normalizeImageUrl(img);

        titleText.setText(title != null ? title : "");
        dateText.setText(date != null ? date : "");
        bodyText.setText(body != null ? body : "");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl)
                    .error(R.drawable.placeholder)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.placeholder);
        }

        btnEdit.setOnClickListener(v -> {
            Intent editIntent = new Intent(this, PostEditActivity.class);
            editIntent.putExtra("id", id);
            editIntent.putExtra("title", title);
            editIntent.putExtra("text", body);
            editIntent.putExtra("imageUrl", imageUrl);
            startActivity(editIntent);
        });

        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                boolean success = PostUploader.deletePost(id);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this,
                            success ? "삭제됨" : "삭제 실패",
                            android.widget.Toast.LENGTH_SHORT).show();
                    if (success) finish();
                });
            }).start();
        });
    }
}
