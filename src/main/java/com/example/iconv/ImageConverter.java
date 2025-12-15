package com.example.iconv;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ImageConverter {

    // Formats that do not support transparency
    private static final Set<String> FORMATS_WITHOUT_ALPHA = new HashSet<>(Arrays.asList("jpg", "jpeg", "bmp"));

    public void convert(File inputFile, String outputFormat, File outputDir) throws IOException {
        if (!inputFile.exists()) {
            throw new IOException("Input file does not exist: " + inputFile.getAbsolutePath());
        }

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
            }
        }

        BufferedImage inputImage = ImageIO.read(inputFile);
        if (inputImage == null) {
            throw new IOException("Could not read input image: " + inputFile.getAbsolutePath());
        }

        BufferedImage finalImage = inputImage;

        // Check if we need to handle transparency
        boolean hasAlpha = inputImage.getTransparency() != Transparency.OPAQUE;
        if (hasAlpha && FORMATS_WITHOUT_ALPHA.contains(outputFormat.toLowerCase())) {
            finalImage = removeAlphaChannel(inputImage);
        }

        String fileName = inputFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);

        File outputFile = new File(outputDir, nameWithoutExtension + "." + outputFormat);

        boolean result = ImageIO.write(finalImage, outputFormat, outputFile);
        if (!result) {
            // This can happen if no writer is found for the format
            throw new IOException("Could not write image in format '" + outputFormat + "'. Please ensure the format is supported.");
        }

        System.out.println("Converted " + inputFile.getName() + " to " + outputFile.getAbsolutePath());
    }

    /**
     * Draws an image with transparency onto a new image with a solid white background.
     * @param source The image with an alpha channel.
     * @return A new image with the transparency removed.
     */
    private BufferedImage removeAlphaChannel(BufferedImage source) {
        // Create a new image with the same dimensions but an RGB color model (no alpha)
        BufferedImage newImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Create a graphics context for the new image
        Graphics2D g2d = newImage.createGraphics();

        // Fill the background with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());

        // Draw the source image on top of the white background
        g2d.drawImage(source, 0, 0, null);

        // Dispose of the graphics context to free up resources
        g2d.dispose();

        return newImage;
    }
}
