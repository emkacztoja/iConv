package com.example.iconv;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader {

    // Regex to capture the progress percentage and ETA from yt-dlp output
    private static final Pattern YTDLP_PROGRESS_PATTERN = Pattern.compile(
            "\\[download\\]\\s+([\\d.]+)% of.*ETA (\\d{2}:\\d{2})"
    );

    private static final String YT_DLP_URL_WINDOWS = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    private static final String YT_DLP_URL_LINUX = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp";
    private static final String YT_DLP_URL_MACOS = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_macos";

    public void download(String url, File outputDirectory, boolean extractAudio, String quality, Consumer<String> onProgress) throws IOException, InterruptedException {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }

        File ytDlpExecutable = ensureYtDlpInstalled(onProgress);

        List<String> command = new ArrayList<>();
        command.add(ytDlpExecutable.getAbsolutePath());
        command.add("--progress"); // Ensure progress output is enabled
        command.add("--ignore-errors");
        command.add("--output");
        command.add(new File(outputDirectory, "%(title)s.%(ext)s").getAbsolutePath());
        command.add("--retries");
        command.add("10");

        if (extractAudio) {
            command.add("--extract-audio");
            command.add("--audio-format");
            command.add("mp3");
            command.add("--audio-quality");
            command.add("0"); // Best quality
        } else {
            String formatString = "bestvideo[height<=?%s]+bestaudio/best";
            switch (quality) {
                case "1080p":
                    formatString = String.format(formatString, "1080");
                    break;
                case "720p":
                    formatString = String.format(formatString, "720");
                    break;
                case "480p":
                    formatString = String.format(formatString, "480");
                    break;
                default: // Best
                    formatString = "bestvideo+bestaudio/best";
                    break;
            }
            command.add("--format");
            command.add(formatString);
        }

        command.add(url);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge stdout and stderr

        onProgress.accept("Starting download for: " + url);
        onProgress.accept("Executing command: " + String.join(" ", command));

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = YTDLP_PROGRESS_PATTERN.matcher(line);
                if (matcher.find()) {
                    String percentage = matcher.group(1);
                    String eta = matcher.group(2);
                    onProgress.accept(String.format("Download progress: %s%%, ETA: %s", percentage, eta));
                } else {
                    // Log other output lines as well, as they might contain useful info
                    onProgress.accept(line);
                }
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            onProgress.accept("Download finished successfully!");
        } else {
            onProgress.accept("Download failed with exit code: " + exitCode);
            // The error stream is already redirected, so errors would have been logged.
        }
    }

    private File ensureYtDlpInstalled(Consumer<String> onProgress) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String executableName = "yt-dlp";
        String downloadUrl;

        if (os.contains("win")) {
            executableName += ".exe";
            downloadUrl = YT_DLP_URL_WINDOWS;
        } else if (os.contains("mac")) {
            downloadUrl = YT_DLP_URL_MACOS;
        } else {
            downloadUrl = YT_DLP_URL_LINUX;
        }

        // Store the executable in the user's home directory under .iconv
        File installDir = new File(System.getProperty("user.home"), ".iconv");
        if (!installDir.exists()) {
            installDir.mkdirs();
        }

        File executableFile = new File(installDir, executableName);

        if (!executableFile.exists()) {
            onProgress.accept("yt-dlp not found. Downloading from " + downloadUrl + "...");
            try (InputStream in = new URL(downloadUrl).openStream()) {
                Files.copy(in, executableFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            executableFile.setExecutable(true);
            onProgress.accept("yt-dlp downloaded successfully to " + executableFile.getAbsolutePath());
        } else {
            onProgress.accept("Using existing yt-dlp at " + executableFile.getAbsolutePath());
        }

        return executableFile;
    }
}
