package com.example.akbarskz.words.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Word {
    @SerializedName("word_en")
    @Expose
    private String wordEn;
    @SerializedName("word_ru")
    @Expose
    private String wordRu;
    @SerializedName("sound")
    @Expose
    private String sound;

    public String getWordEn() {
        return wordEn;
    }

    public void setWordEn(String wordEn) { this.wordEn = wordEn; }

    public String getWordRu() {
        return wordRu;
    }

    public void setWordRu(String wordRu) {
        this.wordRu = wordRu;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }
}
