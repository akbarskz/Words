package com.example.akbarskz.words.Model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Theme {

    @SerializedName("theme")
    @Expose
    private String theme;
    @SerializedName("is_active")
    @Expose
    private Boolean isActive;
    @SerializedName("words")
    @Expose
    private List<Word> words = null;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {

        this.words = words;

    }

    public Boolean getIsActive() { return isActive; }

    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<Word> getValidWords() {

        List<Word> validWords = new ArrayList<Word>();
        for (Word word: words) {
            if (word.getWordEn().length() < 13) {
                validWords.add(word);
            }
        }

        return validWords;
    }
}