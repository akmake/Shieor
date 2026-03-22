package com.example.goodstart.models;

import java.util.List;

public class Section {
    public String id;
    public boolean isHeader;
    public boolean isAliyahHeader;
    public boolean isChapterHeader;
    public String he;
    public String en;
    public String ordinal;
    public List<RashiItem> rashi;
    public Integer verseNum;
    public Integer chapterNum;

    public static class RashiItem {
        public String he;
    }
}
