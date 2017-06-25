package com.ingenious.ishant.ipics.model;

public class CardModel {
    private String description;
    private String descriptionFull;
    private String photoUrl;

    public CardModel() {
    }

    public CardModel(String description, String descriptionFull, String photoUrl) {
        this.description = description;
        this.descriptionFull = descriptionFull;
        this.photoUrl = photoUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getDescriptionFull() {
        return descriptionFull;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}