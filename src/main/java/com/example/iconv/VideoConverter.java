package com.example.iconv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class VideoConverter {

    public void convert(File inputFile, String outputFormat, File outputDir, Consumer<String> onProgress) throws IOException, InterruptedException {
        if (!inputFile.exists()) {
            throw new IOException("Input file does not exist: " + inputFile.getAbsolutePath());
        }
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
            }
        }

        File ffmpegExecutable = FfmpegInstaller.ensureFfmpegInstalled(onProgress);

        String fileName = inputFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
        File outputFile = new File(outputDir, nameWithoutExtension + "." + outputFormat);

        List<String> command = new ArrayList<>();
        command.add(ffmpegExecutable.getAbsolutePath());
        command.add("-i");
        command.add(inputFile.getAbsolutePath());
        command.add("-y"); // Overwrite output file if it exists
        command.add(outputFile.getAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        onProgress.accept("Starting conversion for: " + inputFile.getName());
        onProgress.accept("Executing command: " + String.join(" ", command));

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                onProgress.accept(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            onProgress.accept("Conversion finished successfully!");
        } else {
            onProgress.accept("Conversion failed with exit code: " + exitCode);
        }
    }
}
