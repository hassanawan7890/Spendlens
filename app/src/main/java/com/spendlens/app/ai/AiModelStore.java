package com.spendlens.app.ai;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.content.res.AssetManager;

import com.spendlens.app.utils.PrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class AiModelStore {

    private static final String DIRECTORY_NAME = "on_device_ai";
    public static final String BUNDLED_MODEL_ASSET_PATH = "ai/gemma3-270m-it-q8.task";
    private static final String BUNDLED_MODEL_TARGET_NAME = "spendlens-default-gemma";

    private AiModelStore() {
    }

    public static String importFromUri(Context context, Uri uri, String targetBaseName) throws Exception {
        if (uri == null) {
            throw new IllegalArgumentException("No file selected.");
        }

        String displayName = resolveDisplayName(context, uri);
        String extension = getExtension(displayName);
        if (extension.isEmpty()) {
            extension = ".task";
        }

        File destination = new File(getManagedDirectory(context), targetBaseName + extension);
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("Could not open the selected file.");
            }

            copyStreamToFile(input, destination);
        }

        return destination.getAbsolutePath();
    }

    public static boolean ensureBundledModelConfigured(Context context, PrefsManager prefs) {
        if (prefs == null) {
            prefs = PrefsManager.getInstance(context);
        }
        if (prefs.isAiBundledSetupDone()) {
            return false;
        }

        try {
            if (!bundledModelAssetExists(context)) {
                return false;
            }

            AiConfig current = prefs.getAiConfig();
            prefs.saveAiConfig(new AiConfig(
                    true,
                    current.runtime,
                    AiConfig.BUNDLED_MODEL_SENTINEL,
                    "",
                    true,
                    true
            ));
            prefs.setAiBundledSetupDone(true);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String installBundledModelFromAssets(Context context) throws Exception {
        if (!bundledModelAssetExists(context)) {
            return "";
        }

        File destination = new File(getManagedDirectory(context), BUNDLED_MODEL_TARGET_NAME + ".task");
        if (destination.exists() && destination.length() > 0) {
            return destination.getAbsolutePath();
        }

        AssetManager assets = context.getAssets();
        try (InputStream input = assets.open(BUNDLED_MODEL_ASSET_PATH)) {
            copyStreamToFile(input, destination);
        }
        return destination.getAbsolutePath();
    }

    public static boolean bundledModelAssetExists(Context context) {
        try {
            String[] names = context.getAssets().list("ai");
            if (names == null) return false;
            for (String name : names) {
                if ("gemma3-270m-it-q8.task".equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void deleteManagedFile(String path) {
        if (path == null || path.trim().isEmpty()) return;

        File file = new File(path);
        String marker = File.separator + DIRECTORY_NAME + File.separator;
        if (file.exists() && file.getAbsolutePath().contains(marker)) {
            // Best effort cleanup of app-managed model files.
            file.delete();
        }
    }

    public static String getDisplayName(String path) {
        if (path == null || path.trim().isEmpty()) return "";
        return new File(path).getName();
    }

    public static boolean isBundledInstalledModel(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        File file = new File(path);
        return file.getName().equals(BUNDLED_MODEL_TARGET_NAME + ".task");
    }

    public static boolean isBundledModelReference(String path) {
        return AiConfig.BUNDLED_MODEL_SENTINEL.equals(path) || isBundledInstalledModel(path);
    }

    public static String resolveModelPath(Context context, AiConfig config) throws Exception {
        if (config != null && config.isBundledModel()) {
            return installBundledModelFromAssets(context);
        }
        return config != null ? config.modelPath : "";
    }

    private static String resolveDisplayName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return "model.task";
    }

    private static String getExtension(String displayName) {
        if (displayName == null) return "";
        int dotIndex = displayName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= displayName.length() - 1) {
            return "";
        }
        return displayName.substring(dotIndex);
    }

    private static File getManagedDirectory(Context context) {
        File directory = new File(context.getFilesDir(), DIRECTORY_NAME);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create local AI storage.");
        }
        return directory;
    }

    private static void copyStreamToFile(InputStream input, File destination) throws Exception {
        try (OutputStream output = new FileOutputStream(destination, false)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }
    }
}
