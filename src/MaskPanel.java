import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class MaskPanel extends JPanel {
    private BufferedImage image;
    private BufferedImage mask;
    private Point lastPoint;
    private double scaleFactor = 1.0;
    private Point imagePosition = new Point(0, 0);

    private boolean lassoMode = false;
    private java.util.List<Point> lassoPoints = new ArrayList<>();

    // Theme colors
    private final Color DARK_BG = new Color(40, 42, 48);
    private final Color DARK_BORDER = new Color(60, 63, 65);
    private final Color PLACEHOLDER_TEXT = new Color(150, 150, 150);
    private final Color LASSO_COLOR = Color.WHITE;

    public void setImage(BufferedImage img) {
        this.image = img;
        if (img != null) {
            this.mask = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            clearMask();
            calculateImagePositionAndScale();
        }
        repaint();
    }

    private void calculateImagePositionAndScale() {
        if (image == null || getWidth() == 0 || getHeight() == 0) return;

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        // Calculate scale factor to fit image within panel
        double widthScale = (double) panelWidth / imgWidth;
        double heightScale = (double) panelHeight / imgHeight;
        scaleFactor = Math.min(widthScale, heightScale);

        // Calculate centered position
        int scaledWidth = (int) (imgWidth * scaleFactor);
        int scaledHeight = (int) (imgHeight * scaleFactor);
        imagePosition.x = (panelWidth - scaledWidth) / 2;
        imagePosition.y = (panelHeight - scaledHeight) / 2;
    }

    public BufferedImage getImage() {
        return image;
    }

    public BufferedImage getMask() {
        return mask;
    }

    public void clearMask() {
        if (mask != null) {
            Graphics2D g = mask.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, mask.getWidth(), mask.getHeight());
            g.dispose();
            lassoPoints.clear();
            repaint();
        }
    }

    public void setLassoMode(boolean enabled) {
        this.lassoMode = enabled;
        lassoPoints.clear();
        repaint();
    }

    public MaskPanel() {
        setBackground(DARK_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                calculateImagePositionAndScale();
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (image == null || mask == null) return;

                if (lassoMode) {
                    lassoPoints.clear();
                    lassoPoints.add(convertPointToImage(e.getPoint()));
                } else {
                    lastPoint = convertPointToImage(e.getPoint());
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (image == null || mask == null) return;

                if (lassoMode && lassoPoints.size() > 2) {
                    Polygon polygon = new Polygon();
                    for (Point p : lassoPoints) {
                        polygon.addPoint(p.x, p.y);
                    }

                    Graphics2D g = mask.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillPolygon(polygon);
                    g.dispose();

                    lassoPoints.clear();
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (image == null || mask == null) return;

                Point convertedPoint = convertPointToImage(e.getPoint());

                if (lassoMode) {
                    lassoPoints.add(convertedPoint);
                    repaint();
                } else {
                    Graphics2D g = mask.createGraphics();
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(20 / (float) scaleFactor,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    if (lastPoint != null) {
                        g.drawLine(lastPoint.x, lastPoint.y,
                                convertedPoint.x, convertedPoint.y);
                    }
                    g.dispose();
                    lastPoint = convertedPoint;
                    repaint();
                }
            }
        });
    }

    private Point convertPointToImage(Point p) {
        if (image == null || scaleFactor == 0) return p;

        // Convert mouse point to image coordinates (accounting for scaling and position)
        int imgX = (int) ((p.x - imagePosition.x) / scaleFactor);
        int imgY = (int) ((p.y - imagePosition.y) / scaleFactor);

        // Clamp to image bounds
        imgX = Math.max(0, Math.min(imgX, image.getWidth() - 1));
        imgY = Math.max(0, Math.min(imgY, image.getHeight() - 1));

        return new Point(imgX, imgY);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Fill with dark background
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (image == null) {
            // Draw placeholder text
            g.setColor(PLACEHOLDER_TEXT);
            g.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            String text = "Drag & drop an image or click 'Open'";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, x, y);
            return;
        }

        // Draw shadow effect
        int shadowOffset = (int) (3 * scaleFactor);
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRect(imagePosition.x + shadowOffset, imagePosition.y + shadowOffset,
                (int)(image.getWidth() * scaleFactor), (int)(image.getHeight() * scaleFactor));

        // Draw the scaled image
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, imagePosition.x, imagePosition.y,
                (int)(image.getWidth() * scaleFactor),
                (int)(image.getHeight() * scaleFactor), null);

        // Draw the mask with transparency
        if (mask != null) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g2d.drawImage(mask, imagePosition.x, imagePosition.y,
                    (int)(mask.getWidth() * scaleFactor),
                    (int)(mask.getHeight() * scaleFactor), null);
            g2d.setComposite(AlphaComposite.SrcOver);
        }

        // Draw current lasso polygon in progress
        if (lassoMode && lassoPoints.size() > 1) {
            g2d.setColor(LASSO_COLOR);
            g2d.setStroke(new BasicStroke(2));

            for (int i = 0; i < lassoPoints.size() - 1; i++) {
                Point p1 = lassoPoints.get(i);
                Point p2 = lassoPoints.get(i + 1);
                g2d.drawLine(
                        imagePosition.x + (int)(p1.x * scaleFactor),
                        imagePosition.y + (int)(p1.y * scaleFactor),
                        imagePosition.x + (int)(p2.x * scaleFactor),
                        imagePosition.y + (int)(p2.y * scaleFactor)
                );
            }

            // Draw line back to start if we have enough points
            if (lassoPoints.size() > 2) {
                Point first = lassoPoints.get(0);
                Point last = lassoPoints.get(lassoPoints.size() - 1);
                g2d.drawLine(
                        imagePosition.x + (int)(last.x * scaleFactor),
                        imagePosition.y + (int)(last.y * scaleFactor),
                        imagePosition.x + (int)(first.x * scaleFactor),
                        imagePosition.y + (int)(first.y * scaleFactor)
                );
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 400); // Default size when no image
    }
}