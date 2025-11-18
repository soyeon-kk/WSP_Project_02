package com.example.photoviewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.widget.Filter;
import android.widget.Filterable;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> implements Filterable {

    private final Context context;
    private final List<PostItem> all;      // 원본
    private final List<PostItem> shown;    // 표시용(필터 결과)

    public PostAdapter(Context context, List<PostItem> postList) {
        this.context = context;
        this.all = new ArrayList<>(postList);
        this.shown = new ArrayList<>(postList);
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View root;
        ImageView imageView;
        TextView title, date;
        Button btnEdit, btnDelete;

        public ViewHolder(View v) {
            super(v);
            root = v;
            imageView = v.findViewById(R.id.imageView);
            title = v.findViewById(R.id.title);
            date = v.findViewById(R.id.date);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    @Override
    public long getItemId(int position) {
        return shown.get(position).getId();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        PostItem post = shown.get(pos);
        h.title.setText(post.getTitle());
        h.date.setText(post.getDate());

        String url = FileUtils.normalizeImageUrl(post.getImageUrl());
        if (url != null && !url.isEmpty()) {
            Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(h.imageView);
        } else {
            h.imageView.setImageResource(R.drawable.placeholder);
        }

        // 상세 보기
        h.root.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return;
            PostItem item = shown.get(p);
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("id", item.getId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("date", item.getDate());
            intent.putExtra("body", item.getText());
            intent.putExtra("image", FileUtils.normalizeImageUrl(item.getImageUrl()));
            context.startActivity(intent);
        });

        // 수정
        h.btnEdit.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return;
            PostItem item = shown.get(p);
            Intent intent = new Intent(context, PostEditActivity.class);
            intent.putExtra("id", item.getId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("text", item.getText());
            intent.putExtra("imageUrl", item.getImageUrl());
            context.startActivity(intent);
        });

        // 삭제
        h.btnDelete.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return;
            PostItem item = shown.get(p);

            new AlertDialog.Builder(context)
                    .setTitle("삭제 확인")
                    .setMessage("이 게시글을 삭제할까요?")
                    .setPositiveButton("삭제", (d, w) -> {
                        new Thread(() -> {
                            boolean ok = PostUploader.deletePost(item.getId());
                            ((Activity) context).runOnUiThread(() -> {
                                Toast.makeText(context, ok ? "삭제됨" : "삭제 실패", Toast.LENGTH_SHORT).show();
                                if (ok) {
                                    removeById(item.getId());
                                }
                            });
                        }).start();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    private void removeById(int id) {
        for (Iterator<PostItem> it = all.iterator(); it.hasNext(); ) {
            if (it.next().getId() == id) {
                it.remove();
                break;
            }
        }
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).getId() == id) {
                shown.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    // ===== 검색 필터 =====
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence q) {
                String s = q == null ? "" : q.toString().trim().toLowerCase(Locale.ROOT);
                List<PostItem> res;
                if (s.isEmpty()) {
                    res = new ArrayList<>(all);
                } else {
                    res = new ArrayList<>();
                    for (PostItem p : all) {
                        String t = p.getTitle() == null ? "" : p.getTitle();
                        String body = p.getText() == null ? "" : p.getText();
                        if (t.toLowerCase(Locale.ROOT).contains(s)
                                || body.toLowerCase(Locale.ROOT).contains(s)) {
                            res.add(p);
                        }
                    }
                }
                FilterResults fr = new FilterResults();
                fr.values = res;
                fr.count = res.size();
                return fr;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                shown.clear();
                if (results.values != null) {
                    shown.addAll((List<PostItem>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    public void updateData(List<PostItem> newData) {
        all.clear();
        all.addAll(newData);
        shown.clear();
        shown.addAll(newData);
        notifyDataSetChanged();
    }
}
