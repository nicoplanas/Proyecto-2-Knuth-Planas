package com.unimet.so.proyecto2.ui;

import com.unimet.so.proyecto2.model.Block;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class DiskPanel extends JPanel {
    private static final Color PANEL_BG = new Color(34, 40, 49); // Más parecido al fondo azul grisaceo de la captura
    private static final Color BORDER_COLOR = PANEL_BG; // Sin borde duro en los bloques para estilo flat
    private static final Color FREE_BLOCK_COLOR = new Color(57, 62, 70); // Gris oscurito para bloques libres
    private static final Color FREE_BLOCK_COLOR_ALT = new Color(67, 73, 82);

    // Paleta de colores flat-vibrant predefinida
    private static final Color[] VIBRANT_PALETTE = {
            new Color(255, 107, 107), // Coral / Rojo
            new Color(254, 202, 87),  // Amarillo
            new Color(72, 219, 251),  // Cian
            new Color(29, 209, 161),  // Turquesa / Verde
            new Color(84, 160, 255),  // Azul claro
            new Color(95, 39, 205),   // Morado vibrante
            new Color(255, 159, 243), // Rosa
            new Color(255, 159, 67),  // Naranja
            new Color(16, 172, 132),  // Verde flat
            new Color(46, 134, 222)   // Azul oscuro
    };
    private Block[] blocks = new Block[0];

    public DiskPanel() {
        setPreferredSize(new Dimension(560, 360));
        setBackground(PANEL_BG);
    }

    public void setBlocks(Block[] blocks) {
        this.blocks = blocks == null ? new Block[0] : blocks;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int columns = 20;
        int availableWidth = Math.max(1, getWidth() - 16);
        int cellSize = Math.max(18, Math.min(28, availableWidth / Math.max(columns, 1)));
        int padding = 8;
        FontMetrics metrics = g2.getFontMetrics();

        for (int index = 0; index < blocks.length; index++) {
            int row = index / columns;
            int column = index % columns;
            int x = padding + column * cellSize;
            int y = padding + row * cellSize;
            Block block = blocks[index];

            Color blockColor = colorForBlock(block, index);
            g2.setColor(blockColor);
            g2.fillRoundRect(x, y, cellSize - 3, cellSize - 3, 6, 6);
            if (!blockColor.equals(FREE_BLOCK_COLOR)) {
                g2.setColor(BORDER_COLOR);
                // Borde mucho mas ligero o nada
            }

            String label = String.valueOf(index);
            int textWidth = metrics.stringWidth(label);
            int textX = x + (cellSize - 3 - textWidth) / 2;
            int textY = y + ((cellSize - 3) + metrics.getAscent()) / 2 - 2;
            g2.setColor(textColorFor(blockColor));
            g2.drawString(label, textX, textY);
        }

        g2.dispose();
    }

    private Color colorForBlock(Block block, int index) {
        if (block == null || !block.isAllocated()) {
            return index % 2 == 0 ? FREE_BLOCK_COLOR : FREE_BLOCK_COLOR_ALT;
        }
        int rootSeed = block.getFilePath() != null ? block.getFilePath().hashCode() : 0;
        int colorIndex = Math.abs(rootSeed) % VIBRANT_PALETTE.length;
        return VIBRANT_PALETTE[colorIndex];
    }

    private Color textColorFor(Color background) {
        int luma = (background.getRed() * 299 + background.getGreen() * 587 + background.getBlue() * 114) / 1000;
        return luma > 150 ? new Color(20, 28, 40) : new Color(236, 241, 248);
    }
}