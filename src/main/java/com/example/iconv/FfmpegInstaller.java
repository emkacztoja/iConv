package com.example.iconv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public class FfmpegInstaller {

    // Using a known-good public build of ffmpeg
    private static final String FFMPEG_URL_WINDOWS = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";
    // For simplicity, we'll focus on Windows first. Mac/Linux would need different URLs and extraction logic.

    public static File ensureFfmpegInstalled(Consumer<String> onProgress) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            onProgress.accept("Automatic ffmpeg installation is only supported on Windows in this version.");
            // Try to use system ffmpeg
            File ffmpeg = new File("ffmpeg");
            if (ffmpeg.canExecute()) {
                return ffmpeg;
            }
            throw new IOException("FFmpeg not found. Please install it and add it to your PATH.");
        }

        File installDir = new File(System.getProperty("user.home"), ".iconv");
        File ffmpegExe = new File(installDir, "ffmpeg-master-latest-win64-gpl/bin/ffmpeg.exe");

        if (!ffmpegExe.exists()) {
            onProgress.accept("ffmpeg not found. Downloading...");
            File zipFile = new File(installDir, "ffmpeg.zip");

            try (InputStream in = new URL(FFMPEG_URL_WINDOWS).openStream()) {
                Files.copy(in, zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            onProgress.accept("Extracting ffmpeg...");
            // Basic zip extraction
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File newFile = new File(installDir, entry.getName());
                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        new File(newFile.getParent()).mkdirs();
                        Files.copy(zis, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            zipFile.delete();
            onProgress.accept("ffmpeg installed successfully.");
        } else {
            onProgress.accept("Using existing ffmpeg at " + ffmpegExe.getAbsolutePath());
        }
        return ffmpegExe;
    }
}
