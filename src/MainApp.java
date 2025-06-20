// MainApp.java
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import dao.InpaintDAO;
import model.InpaintResult;

public class MainApp {
    public static void main(String[] args) {
        // Use FlatLaf for modern look
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            // Custom UI improvements
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Adrishya - AI Object Remover");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setMinimumSize(new Dimension(900, 600));
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());
            frame.getContentPane().setBackground(Color.WHITE);

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(240, 240, 240));
            header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            titlePanel.setOpaque(false);

            try {
                FlatSVGIcon logoIcon = new FlatSVGIcon("logo.svg", 28, 28);
                JLabel logo = new JLabel(logoIcon);
                titlePanel.add(logo);
            } catch (Exception e) {
                System.err.println("Logo not found");
            }

            JLabel title = new JLabel("Adrishya - AI Object Remover");
            title.setFont(new Font("Segoe UI", Font.BOLD, 22));
            title.setForeground(new Color(50, 50, 50));
            titlePanel.add(title);
            header.add(titlePanel, BorderLayout.WEST);

            JLabel versionLabel = new JLabel("v1.1.0");
            versionLabel.setForeground(new Color(150, 150, 150));
            versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            header.add(versionLabel, BorderLayout.EAST);
            frame.add(header, BorderLayout.NORTH);

            // Main content area
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBackground(Color.WHITE);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            MaskPanel maskPanel = new MaskPanel();
            JScrollPane scrollPane = new JScrollPane(maskPanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setBackground(Color.WHITE);
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            JPanel toolPanel = createToolPanel(frame, maskPanel);
            contentPanel.add(toolPanel, BorderLayout.EAST);

            frame.add(contentPanel, BorderLayout.CENTER);

            // Status bar
            JPanel statusBar = new JPanel(new BorderLayout());
            statusBar.setBackground(new Color(240, 240, 240));
            statusBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));

            JLabel statusLabel = new JLabel("Ready");
            statusLabel.setForeground(new Color(100, 100, 100));
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JProgressBar progressBar = new JProgressBar();
            progressBar.setVisible(false);
            progressBar.setIndeterminate(true);
            progressBar.setPreferredSize(new Dimension(150, 20));

            statusBar.add(statusLabel, BorderLayout.WEST);
            statusBar.add(progressBar, BorderLayout.EAST);
            frame.add(statusBar, BorderLayout.SOUTH);

            frame.setVisible(true);
        });
    }

    private static JPanel createToolPanel(JFrame frame, MaskPanel maskPanel) {
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
        toolPanel.setBackground(new Color(240, 240, 240));
        toolPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        toolPanel.setPreferredSize(new Dimension(250, 0));

        JLabel toolsTitle = new JLabel("Tools");
        toolsTitle.setForeground(new Color(50, 50, 50));
        toolsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        toolsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolPanel.add(toolsTitle);
        toolPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel filePanel = createSectionPanel("File");
        JButton openBtn = createToolButton("Open Image", "open.svg");
        JButton saveBtn = createToolButton("Save Result", "save.svg");
        filePanel.add(openBtn);
        filePanel.add(saveBtn);
        toolPanel.add(filePanel);
        toolPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel maskToolsPanel = createSectionPanel("Mask Tools");
        JButton clearBtn = createToolButton("Clear Mask", "clear.svg");
        JCheckBox lassoToggle = new JCheckBox("Lasso Mode");
        styleCheckBox(lassoToggle);
        maskToolsPanel.add(clearBtn);
        maskToolsPanel.add(lassoToggle);
        toolPanel.add(maskToolsPanel);
        toolPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel processPanel = createSectionPanel("Processing");
        JButton removeBtn = createToolButton("Remove Objects", "magic.svg");
        removeBtn.setBackground(new Color(88, 101, 242));
        removeBtn.setForeground(Color.WHITE);
        processPanel.add(removeBtn);
        toolPanel.add(processPanel);

        toolPanel.add(Box.createVerticalGlue());

        // Action Listeners

        openBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open Image");
            chooser.setApproveButtonText("Open");

            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();

                if (selectedFile == null || !selectedFile.isFile()) {
                    JOptionPane.showMessageDialog(frame, "No valid file selected.");
                    updateStatus(frame, "No file selected", true);
                    return;
                }

                String fileName = selectedFile.getName().toLowerCase();
                if (!(fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".bmp"))) {
                    JOptionPane.showMessageDialog(frame,
                            "Unsupported file format.\nPlease select a PNG, JPG, JPEG, or BMP image.",
                            "Invalid File",
                            JOptionPane.WARNING_MESSAGE);
                    updateStatus(frame, "Unsupported file type", true);
                    return;
                }

                try {
                    BufferedImage img = ImageIO.read(selectedFile);
                    if (img == null) throw new IOException("Could not decode image.");
                    Path(selectedFile.getAbsolutePath());
                    maskPanel.setImage(img);
                    updateStatus(frame, "Image loaded", false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Failed to load image.");
                    updateStatus(frame, "Error loading image", true);
                }
            } else {
                updateStatus(frame, "Image loading cancelled", false);
            }
        });

        removeBtn.addActionListener(e -> {
            if (maskPanel.getImage() == null) {
                JOptionPane.showMessageDialog(frame, "Please load an image first.");
                return;
            }
            if (maskPanel.getMask() == null) {
                JOptionPane.showMessageDialog(frame, "Please draw a mask to remove objects.");
                return;
            }

            updateStatus(frame, "Processing... Please wait", true);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try {
                        ImageIO.write(maskPanel.getImage(), "png", new File("input_image.png"));
                        ImageIO.write(maskPanel.getMask(), "png", new File("input_mask.png"));

                        ProcessBuilder pb = new ProcessBuilder("python", "src/inpaint.py",
                                "input_image.png", "input_mask.png", "output_image.png");
                        pb.redirectErrorStream(true);
                        Process process = pb.start();

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println("[Python] " + line);
                            }
                        }

                        int exitCode = process.waitFor();
                        if (exitCode != 0) throw new RuntimeException("Python script failed.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(frame,
                                    "<html>Error during processing:<br>" + ex.getMessage() + "</html>",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            updateStatus(frame, "Processing failed", true);
                        });
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        BufferedImage result = ImageIO.read(new File("output_image.png"));
                        maskPanel.setImage(result);
                        maskPanel.clearMask();
                        updateStatus(frame, "Objects removed successfully", false);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        updateStatus(frame, "Error displaying result", true);
                    }
                }
            }.execute();
        });

        clearBtn.addActionListener(e -> {
            if (maskPanel.getImage() == null) {
                JOptionPane.showMessageDialog(frame, "Please load an image before clearing mask.");
                return;
            }
            maskPanel.clearMask();
            updateStatus(frame, "Mask cleared", false);
        });

        saveBtn.addActionListener(e -> {
            if (maskPanel.getImage() == null) {
                JOptionPane.showMessageDialog(frame, "No image to save!");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Image");
            chooser.setSelectedFile(new File("output_image.png"));
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".png") || f.isDirectory();
                }

                public String getDescription() {
                    return "PNG Images (*.png)";
                }
            });

            if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    if (!file.getName().toLowerCase().endsWith(".png")) {
                        file = new File(file.getAbsolutePath() + ".png");
                    }

                    ImageIO.write(maskPanel.getImage(), "png", file);
                    JOptionPane.showMessageDialog(frame,
                            "<html>Image saved successfully!<br>" + file.getAbsolutePath() + "</html>");
                    updateStatus(frame, "Image saved", false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    updateStatus(frame, "Failed to save image", true);
                }
            }
        });

        lassoToggle.addActionListener(e -> {
            if (maskPanel.getImage() == null) {
                JOptionPane.showMessageDialog(frame, "Load an image before using lasso mode.");
                lassoToggle.setSelected(false);
                return;
            }
            maskPanel.setLassoMode(lassoToggle.isSelected());
            updateStatus(frame, lassoToggle.isSelected() ? "Lasso mode enabled" : "Lasso mode disabled", false);
        });

        return toolPanel;
    }

    private static JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(title);
        label.setForeground(new Color(50, 50, 50));
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        return panel;
    }

    private static JButton createToolButton(String text, String iconName) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setBackground(new Color(220, 220, 220));
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        try {
            FlatSVGIcon icon = new FlatSVGIcon(iconName, 20, 20);
            btn.setIcon(icon);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setIconTextGap(10);
        } catch (Exception e) {
            System.err.println("Icon not found: " + iconName);
        }

        return btn;
    }

    private static void updateStatus(JFrame frame, String message, boolean progress) {
        JPanel statusBar = (JPanel) ((BorderLayout) frame.getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        JLabel statusLabel = (JLabel) ((BorderLayout) statusBar.getLayout()).getLayoutComponent(BorderLayout.WEST);
        JProgressBar progressBar = (JProgressBar) ((BorderLayout) statusBar.getLayout()).getLayoutComponent(BorderLayout.EAST);

        statusLabel.setText(message);
        progressBar.setVisible(progress);

        if (message.toLowerCase().contains("error") || message.toLowerCase().contains("fail")) {
            statusLabel.setForeground(new Color(255, 100, 100));
        } else if (message.toLowerCase().contains("success") || message.toLowerCase().contains("ready")) {
            statusLabel.setForeground(new Color(100, 255, 100));
        } else {
            statusLabel.setForeground(new Color(100, 100, 100));
        }
    }

    static void Path(String path1) {
        InpaintResult result = new InpaintResult(path1, "input_mask.png", "output_image.png");
        InpaintDAO dao = new InpaintDAO();
        dao.saveResult(result);
    }

    private static void styleCheckBox(JCheckBox cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.setBackground(new Color(240, 240, 240));
        cb.setForeground(Color.BLACK);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setFocusPainted(false);
    }
}
