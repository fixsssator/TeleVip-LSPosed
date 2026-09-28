package com.my.televip.utils;

import com.my.televip.logging.Logger;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JsonAssetReader {

    private static final Map<String, String> rawCache = new ConcurrentHashMap<>();

    public static JSONObject readObject(String assetPath) {
        String raw = readRaw(assetPath, true);
        if (raw == null) return null;
        try {
            return new JSONObject(raw);
        } catch (Throwable e) {
            Logger.e(e);
            return null;
        }
    }

    public static String readRaw(String assetPath, boolean errorLog) {
        String normalized = normalize(assetPath);

        String cached = rawCache.get(normalized);
        if (cached != null) return cached;

        try (ZipFile zipFile = new ZipFile(Utils.modulePath)) {
            ZipEntry entry = zipFile.getEntry(normalized);
            if (entry == null) {
                if (errorLog) Logger.w("JsonAssetReader: entry not found -> " + normalized);
                return null;
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
                String content = readFully(is);
                rawCache.put(normalized, content);
                return content;
            }
        } catch (Throwable e) {
            Logger.e(e);
            return null;
        }
    }

    public static List<String> listFiles(String dirPath, String extension) {
        List<String> result = new ArrayList<>();
        String normalizedDir = normalize(dirPath);
        if (!normalizedDir.endsWith("/")) normalizedDir += "/";

        try (ZipFile zipFile = new ZipFile(Utils.modulePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith(normalizedDir) && name.endsWith(extension)) {
                    String fileName = name.substring(name.lastIndexOf('/') + 1);
                    result.add(fileName);
                }
            }
        } catch (Throwable e) {
            Logger.e(e);
        }

        return result;
    }

    private static String normalize(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static String readFully(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toString("UTF-8");
    }
}