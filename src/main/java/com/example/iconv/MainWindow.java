package com.example.iconv;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindow extends JFrame {

    // Wrapper class to store file and its base directory for relative path calculation
    private static class FileItem {
        final File file;
        final File baseDir;

        FileItem(File file, File baseDir) {
            this.file = file;
            this.baseDir = baseDir;
        }

        @Override
        public String toString() {
            return file.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileItem fileItem = (FileItem) o;
            return file.equals(fileItem.file);
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }
    }

    private JList<FileItem> imageConversionList;
    private DefaultListModel<FileItem> imageConversionListModel;
    private JList<FileItem> videoConversionList;
    private DefaultListModel<FileItem> videoConversionListModel;
    private JTextField outputDirField;
    private JComboBox<String> imageFormatBox;
    private JComboBox<String> videoFormatBox;
    private JTextArea logArea;
    private JTextField urlField;
    private JCheckBox audioOnlyCheckbox;
    private JComboBox<String> qualityBox;
    private JProgressBar downloadProgressBar;
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("Download progress: (\\d+\\.\\d+)%.*");


    public MainWindow() {
        // Set up the look and feel from Main.java
        setTitle("Iconv - Media Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Image Converter", createImagePanel());
        tabbedPane.addTab("Video Converter", createVideoPanel());
        tabbedPane.addTab("Downloader", createDownloaderPanel());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setRows(10);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        mainPanel.add(logScroll, BorderLayout.SOUTH);

        // Set up drag and drop for the entire window
        setTransferHandler(createFileDropHandler());

        // Set default output directory
        setDefaultOutputDirectory();
    }

    private JPanel createImagePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // File List Panel (for drag & drop)
        imageConversionListModel = new DefaultListModel<>();
        imageConversionList = new JList<>(imageConversionListModel);
        imageConversionList.setCellRenderer(new FileListCellRenderer());
        JScrollPane fileListScroll = new JScrollPane(imageConversionList);
        fileListScroll.setBorder(BorderFactory.createTitledBorder("Files to Convert (Drag & Drop here)"));
        panel.add(fileListScroll, BorderLayout.CENTER);

        // Control panel for buttons and options
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add/Remove Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addFilesBtn = new JButton("Add Files...");
        addFilesBtn.addActionListener(e -> addFiles(imageConversionListModel, true));
        JButton addDirBtn = new JButton("Add Folder...");
        addDirBtn.addActionListener(e -> addDirectory(imageConversionListModel, true));
        JButton clearBtn = new JButton("Clear List");
        clearBtn.addActionListener(e -> imageConversionListModel.clear());
        buttonPanel.add(addFilesBtn);
        buttonPanel.add(addDirBtn);
        buttonPanel.add(clearBtn);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        controlPanel.add(buttonPanel, gbc);

        // Output Format
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Output Format:"), gbc);

        Set<String> formats = new TreeSet<>();
        for (String format : ImageIO.getWriterFormatNames()) {
            formats.add(format.toLowerCase());
        }
        if (formats.isEmpty()) {
            formats.addAll(Arrays.asList("png", "jpg", "jpeg", "bmp", "gif"));
        }
        imageFormatBox = new JComboBox<>(new Vector<>(formats));
        if (formats.contains("png")) {
            imageFormatBox.setSelectedItem("png");
        }
        gbc.gridx = 1; gbc.gridwidth = 2;
        controlPanel.add(imageFormatBox, gbc);

        // Output Directory
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Output Directory:"), gbc);

        outputDirField = new JTextField(25);
        gbc.gridx = 1; gbc.gridwidth = 1;
        controlPanel.add(outputDirField, gbc);

        JButton browseOutputBtn = new JButton("Browse...");
        browseOutputBtn.addActionListener(e -> chooseFile(outputDirField, JFileChooser.DIRECTORIES_ONLY));
        gbc.gridx = 2; gbc.gridwidth = 1;
        controlPanel.add(browseOutputBtn, gbc);
        
        panel.add(controlPanel, BorderLayout.SOUTH);

        // Convert Button in a separate panel for better layout
        JPanel convertPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton convertBtn = new JButton("Convert All Images");
        convertBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        convertBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        convertBtn.addActionListener(this::convertImages);
        convertPanel.add(convertBtn);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        controlPanel.add(convertPanel, gbc);

        return panel;
    }
    
    private JPanel createVideoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // File List Panel
        videoConversionListModel = new DefaultListModel<>();
        videoConversionList = new JList<>(videoConversionListModel);
        videoConversionList.setCellRenderer(new FileListCellRenderer());
        JScrollPane fileListScroll = new JScrollPane(videoConversionList);
        fileListScroll.setBorder(BorderFactory.createTitledBorder("Videos to Convert (Drag & Drop here)"));
        panel.add(fileListScroll, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add/Remove Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addFilesBtn = new JButton("Add Files...");
        addFilesBtn.addActionListener(e -> addFiles(videoConversionListModel, false));
        JButton addDirBtn = new JButton("Add Folder...");
        addDirBtn.addActionListener(e -> addDirectory(videoConversionListModel, false));
        JButton clearBtn = new JButton("Clear List");
        clearBtn.addActionListener(e -> videoConversionListModel.clear());
        buttonPanel.add(addFilesBtn);
        buttonPanel.add(addDirBtn);
        buttonPanel.add(clearBtn);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        controlPanel.add(buttonPanel, gbc);

        // Output Format
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Output Format:"), gbc);

        String[] videoFormats = {"mp4", "mkv", "mov", "avi", "mp3"};
        videoFormatBox = new JComboBox<>(videoFormats);
        gbc.gridx = 1;
        controlPanel.add(videoFormatBox, gbc);
        
        panel.add(controlPanel, BorderLayout.SOUTH);

        // Convert Button
        JPanel convertPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton convertBtn = new JButton("Convert All Videos");
        convertBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        convertBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        convertBtn.addActionListener(this::convertVideos);
        convertPanel.add(convertBtn);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        controlPanel.add(convertPanel, gbc);

        return panel;
    }

    private JPanel createDownloaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // URL input
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Video URL:"), gbc);

        urlField = new JTextField(35);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(urlField, gbc);

        // Quality selection
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quality:"), gbc);

        String[] qualities = {"Best", "1080p", "720p", "480p"};
        qualityBox = new JComboBox<>(qualities);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(qualityBox, gbc);

        // Options
        audioOnlyCheckbox = new JCheckBox("Download audio only (MP3)");
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(audioOnlyCheckbox, gbc);
        
        // Progress Bar
        downloadProgressBar = new JProgressBar();
        downloadProgressBar.setStringPainted(true);
        downloadProgressBar.setForeground(new Color(66, 139, 202)); // A nice blue color
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(downloadProgressBar, gbc);

        // Download Button Panel
        JPanel downloadButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JButton downloadBtn = new JButton("Download");
        downloadBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        downloadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        downloadBtn.addActionListener(this::downloadVideo);
        downloadButtonPanel.add(downloadBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(downloadButtonPanel, gbc);

        return panel;
    }

    private void addFiles(DefaultListModel<FileItem> model, boolean isImage) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                FileItem item = new FileItem(file, file.getParentFile());
                if (!model.contains(item)) {
                    model.addElement(item);
                }
            }
        }
    }

    private void addDirectory(DefaultListModel<FileItem> model, boolean isImage) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            addFilesInDirectory(dir, dir.getParentFile(), model, isImage);
        }
    }

    private void addFilesInDirectory(File dir, File baseDir, DefaultListModel<FileItem> model, boolean isImage) {
        File[] files = dir.listFiles();
        if (files != null) {
            log("Scanning folder: " + dir.getName());
            for (File file : files) {
                if (file.isDirectory()) {
                    addFilesInDirectory(file, baseDir, model, isImage); // Pass the original baseDir
                } else {
                    boolean addFile = isImage ? isImageFile(file) : isVideoFile(file);
                    if (addFile) {
                        FileItem item = new FileItem(file, baseDir);
                        if (!model.contains(item)) {
                            model.addElement(item);
                        }
                    }
                }
            }
        }
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".bmp") || name.endsWith(".gif");
    }
    
    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".mov") || name.endsWith(".avi") || name.endsWith(".flv") || name.endsWith(".wmv");
    }

    private void setDefaultOutputDirectory() {
        File documentsDir = FileSystemView.getFileSystemView().getDefaultDirectory();
        File defaultOutputDir = new File(documentsDir, "iConv");
        if (!defaultOutputDir.exists()) {
            defaultOutputDir.mkdirs();
        }
        outputDirField.setText(defaultOutputDir.getAbsolutePath());
    }

    private TransferHandler createFileDropHandler() {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                Transferable transferable = support.getTransferable();
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    log("Processing " + files.size() + " dropped items...");
                    for (File file : files) {
                        if (file.isDirectory()) {
                            addFilesInDirectory(file, file.getParentFile(), imageConversionListModel, true);
                            addFilesInDirectory(file, file.getParentFile(), videoConversionListModel, false);
                        } else {
                            if (isImageFile(file)) {
                                FileItem item = new FileItem(file, file.getParentFile());
                                if (!imageConversionListModel.contains(item)) {
                                    imageConversionListModel.addElement(item);
                                }
                            } else if (isVideoFile(file)) {
                                FileItem item = new FileItem(file, file.getParentFile());
                                if (!videoConversionListModel.contains(item)) {
                                    videoConversionListModel.addElement(item);
                                }
                            }
                        }
                    }
                    return true;
                } catch (Exception e) {
                    log("Error handling dropped files: " + e.getMessage());
                    e.printStackTrace();
                }
                return false;
            }
        };
    }

    private JPanel createPlaceholderPanel(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel(message);
        label.setFont(new Font("SansSerif", Font.ITALIC, 16));
        panel.add(label);
        return panel;
    }

    private void chooseFile(JTextField targetField, int mode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(mode);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void convertImages(ActionEvent e) {
        String outputDirPath = outputDirField.getText();
        String format = (String) imageFormatBox.getSelectedItem();

        if (imageConversionListModel.isEmpty()) {
            log("Please add images to the list before converting.");
            return;
        }
        if (outputDirPath.isEmpty()) {
            log("Please select an output directory.");
            return;
        }

        File outputDir = new File(outputDirPath);
        List<FileItem> itemsToConvert = new ArrayList<>();
        for (int i = 0; i < imageConversionListModel.getSize(); i++) {
            itemsToConvert.add(imageConversionListModel.getElementAt(i));
        }

        ImageConverter converter = new ImageConverter();
        new Thread(() -> {
            int successCount = 0;
            File lastOutputDir = null;
            for (FileItem item : itemsToConvert) {
                try {
                    Path relativePath = item.baseDir.toPath().relativize(item.file.toPath());
                    Path parentPath = relativePath.getParent();
                    File targetDir = (parentPath != null) ? new File(outputDir, parentPath.toString()) : outputDir;
                    lastOutputDir = targetDir;

                    log("Converting " + item.file.getName() + " to " + format + "...");
                    converter.convert(item.file, format, targetDir);
                    successCount++;
                } catch (IOException ex) {
                    log("Error converting " + item.file.getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            final int finalSuccessCount = successCount;
            final File finalOutputDir = lastOutputDir;
            SwingUtilities.invokeLater(() -> {
                log("Image conversion finished. " + finalSuccessCount + "/" + itemsToConvert.size() + " files converted successfully.");
                if (finalSuccessCount > 0 && finalOutputDir != null) {
                    openOutputDirectory(finalOutputDir);
                }
            });
        }).start();
    }
    
    private void convertVideos(ActionEvent e) {
        String outputDirPath = outputDirField.getText();
        String format = (String) videoFormatBox.getSelectedItem();

        if (videoConversionListModel.isEmpty()) {
            log("Please add videos to the list before converting.");
            return;
        }
        if (outputDirPath.isEmpty()) {
            log("Please select an output directory.");
            return;
        }

        File outputDir = new File(outputDirPath);
        List<FileItem> itemsToConvert = new ArrayList<>();
        for (int i = 0; i < videoConversionListModel.getSize(); i++) {
            itemsToConvert.add(videoConversionListModel.getElementAt(i));
        }

        VideoConverter converter = new VideoConverter();
        new Thread(() -> {
            int successCount = 0;
            File lastOutputDir = null;
            for (FileItem item : itemsToConvert) {
                try {
                    Path relativePath = item.baseDir.toPath().relativize(item.file.toPath());
                    Path parentPath = relativePath.getParent();
                    File targetDir = (parentPath != null) ? new File(outputDir, parentPath.toString()) : outputDir;
                    lastOutputDir = targetDir;

                    converter.convert(item.file, format, targetDir, this::log);
                    successCount++;
                } catch (IOException | InterruptedException ex) {
                    log("Error converting " + item.file.getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            final int finalSuccessCount = successCount;
            final File finalOutputDir = lastOutputDir;
            SwingUtilities.invokeLater(() -> {
                log("Video conversion finished. " + finalSuccessCount + "/" + itemsToConvert.size() + " files converted successfully.");
                if (finalSuccessCount > 0 && finalOutputDir != null) {
                    openOutputDirectory(finalOutputDir);
                }
            });
        }).start();
    }

    private void downloadVideo(ActionEvent e) {
        String url = urlField.getText();
        String outputDirPath = outputDirField.getText();
        boolean audioOnly = audioOnlyCheckbox.isSelected();
        String quality = (String) qualityBox.getSelectedItem();

        if (url.isEmpty()) {
            log("Please enter a video URL.");
            return;
        }
        if (outputDirPath.isEmpty()) {
            log("Please select an output directory.");
            return;
        }

        File outputDir = new File(outputDirPath);
        Downloader downloader = new Downloader();

        downloadProgressBar.setValue(0);
        downloadProgressBar.setString("Starting...");

        new Thread(() -> {
            try {
                downloader.download(url, outputDir, audioOnly, quality, this::handleDownloadProgress);
                SwingUtilities.invokeLater(() -> openOutputDirectory(outputDir));
            } catch (Exception ex) {
                log("Error during download: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void openOutputDirectory(File directory) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(directory);
            } else {
                log("Opening folders is not supported on this system.");
            }
        } catch (IOException e) {
            log("Could not open output directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void handleDownloadProgress(String message) {
        Matcher matcher = PROGRESS_PATTERN.matcher(message);
        if (matcher.find()) {
            String percentageStr = matcher.group(1);
            try {
                double percentage = Double.parseDouble(percentageStr);
                SwingUtilities.invokeLater(() -> {
                    downloadProgressBar.setValue((int) percentage);
                    downloadProgressBar.setString(String.format("%.1f%%", percentage));
                });
            } catch (NumberFormatException e) {
                // Ignore if parsing fails
            }
        } else {
            log(message);
        }
    }

    private static class FileListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof FileItem) {
                FileItem item = (FileItem) value;
                setText(item.file.getName());
                setToolTipText(item.file.getAbsolutePath());
            }
            return this;
        }
    }
}
