package com.example.photoviewer;

import java.time.LocalDateTime;

public class PostItem {
    private int id;
    private String title;
    private String text;
    private String created_date;
    private String published_date;
    private String image;
    private String author;

    public PostItem(int id, String title, String text,
                    String created_date, String published_date,
                    String image, String author) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.created_date = created_date;
        this.published_date = published_date;
        this.image = image;
        this.author = author;
    }

    // ---------- GETTERS ----------
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getCreatedDateRaw() {
        return created_date;
    }

    public String getPublishedDateRaw() {
        return published_date;
    }

    public String getDate() {
        // 화면에 보여줄 날짜: published_date 기준
        return (published_date != null && !published_date.isEmpty())
                ? published_date
                : created_date;
    }

    public String getImageUrl() {
        return image;
    }

    public String getAuthor() {
        return author;
    }

    // 날짜 정렬용
    public LocalDateTime getCreatedDate() {
        try {
            return LocalDateTime.parse(created_date.substring(0, 19));
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }
}
