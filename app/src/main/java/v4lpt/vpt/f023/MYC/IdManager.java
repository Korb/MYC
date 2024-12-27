package v4lpt.vpt.f023.MYC;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IdManager {
    private static final String PREF_NAME = "IdManagerPrefs";
    private static final String LAST_ID_KEY = "LastAssignedId";

    private SharedPreferences prefs;

    public IdManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getNextId() {
        int lastId = prefs.getInt(LAST_ID_KEY, 0);
        int newId = lastId + 1;
        prefs.edit().putInt(LAST_ID_KEY, newId).apply();
        return newId;
    }

    public void setLastId(int id) {
        prefs.edit().putInt(LAST_ID_KEY, id).apply();
    }

    public List<FileItem> getAllFiles(File memorizeDir) {
        List<FileItem> allFiles = new ArrayList<>();
        String[] categories = {"Songs", "Poems", "Music"};

        for (String category : categories) {
            File categoryDir = new File(memorizeDir, category);
            if (categoryDir.exists() && categoryDir.isDirectory()) {
                File[] files = categoryDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            int id = extractIdFromFileName(file.getName());
                            allFiles.add(new FileItem(id, file, category));
                        }
                    }
                }
            }
        }

        Collections.sort(allFiles, (a, b) -> Integer.compare(a.getId(), b.getId()));
        return allFiles;
    }

    private int extractIdFromFileName(String fileName) {
        try {
            return Integer.parseInt(fileName.split("_")[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    public static class FileItem {
        private int id;
        private File file;
        private String category;

        public FileItem(int id, File file, String category) {
            this.id = id;
            this.file = file;
            this.category = category;
        }

        public int getId() { return id; }
        public File getFile() { return file; }
        public String getCategory() { return category; }
    }
}