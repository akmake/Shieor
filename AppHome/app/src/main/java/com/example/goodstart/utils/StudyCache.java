package com.example.goodstart.utils;

import android.content.Context;

import com.example.goodstart.models.StudyDay;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Simple file-based cache for StudyDay objects.
 * Each day is stored as study_YYYY-MM-DD.json in the app's internal files directory.
 * Mirrors the IndexedDB cache used on the website.
 */
public class StudyCache {

    private static final Gson GSON = new Gson();
    private static final String PREFIX = "study_";
    private static final String SUFFIX = ".json";

    public static void save(Context ctx, String date, StudyDay day) {
        try {
            File file = fileFor(ctx, date);
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(GSON.toJson(day));
            }
        } catch (IOException ignored) { }
    }

    /** Returns cached StudyDay or null if not cached. */
    public static StudyDay get(Context ctx, String date) {
        File file = fileFor(ctx, date);
        if (!file.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return GSON.fromJson(sb.toString(), StudyDay.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** Deletes all cached study files. */
    public static int clearAll(Context ctx) {
        File dir = ctx.getFilesDir();
        int deleted = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.getName().startsWith(PREFIX) && f.getName().endsWith(SUFFIX)) {
                if (f.delete()) deleted++;
            }
        }
        return deleted;
    }

    /** Returns number of cached days. */
    public static int cachedCount(Context ctx) {
        File dir = ctx.getFilesDir();
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File f : files) {
            if (f.getName().startsWith(PREFIX) && f.getName().endsWith(SUFFIX)) count++;
        }
        return count;
    }

    private static File fileFor(Context ctx, String date) {
        return new File(ctx.getFilesDir(), PREFIX + date + SUFFIX);
    }
}
