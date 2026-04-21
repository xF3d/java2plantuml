package gui;

import lombok.AllArgsConstructor;

import java.awt.*;

public class RatioGridLayout implements LayoutManager2 {
    private final int hgap;
    private final int vgap;
    private final Ratio ratio;

    @AllArgsConstructor
    public static class Ratio {
        private int col, row;

        public int getCols(int size) {
            double aspect = (double) col / (double) row;
            return Math.toIntExact(estimateDimension(aspect, size));
        }

        public int getRows(int size) {
            double aspect = (double) row / (double) col;
            return Math.toIntExact(estimateDimension(aspect, size));
        }

        private long estimateDimension(double aspect, int size) {
            return Math.max(1, Math.round(Math.ceil(Math.sqrt(size * aspect))));
        }
    }

    public RatioGridLayout(int hgap, int vgap, Ratio ratio) {
        this.hgap = hgap;
        this.vgap = vgap;
        this.ratio = ratio;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {}

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container target) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return preferredLayoutSize(target);
    }

    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            int count = parent.getComponentCount();
            if (count == 0) return new Dimension(0, 0);

            int rows = ratio.getRows(count);
            int cols = ratio.getCols(count);

            Dimension preferedDimension = preferredDimension(parent);

            int width = cols * preferedDimension.width + (cols - 1) * hgap;
            int height = rows * preferedDimension.height + (rows - 1) * vgap;

            Insets insets = parent.getInsets();
            width += insets.left + insets.right;
            height += insets.top + insets.bottom;

            return new Dimension(width, height);
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int count = parent.getComponentCount();
            if (count == 0) return;

            Insets insets = parent.getInsets(); //note: insets are unusable space regarding borders
            int rows = ratio.getRows(count);
            int cols = ratio.getCols(count);

            int totalWidth = parent.getWidth() - insets.left - insets.right;
            int totalHeight = parent.getHeight() - insets.top - insets.bottom;

            // Find max preferred size among components
            Dimension preferedDimension = preferredDimension(parent);

            int cellWidth = (totalWidth - (cols - 1) * hgap) / cols;
            int cellHeight = (totalHeight - (rows - 1) * vgap) / rows;

            cellWidth = Math.max(cellWidth, preferedDimension.width);
            cellHeight = Math.max(cellHeight, preferedDimension.height);

            for (int i = 0; i < count; i++) {
                int row = i / cols;
                int col = i % cols;
                int x = insets.left + col * (cellWidth + hgap);
                int y = insets.top + row * (cellHeight + vgap);
                parent.getComponent(i).setBounds(x, y, cellWidth, cellHeight);
            }
        }
    }

    public static Dimension preferredDimension(Container cmp) {
        int maxPreferredWidth = 0;
        int maxPreferredHeight = 0;
        for (Component comp : cmp.getComponents()) {
            Dimension d = comp.getPreferredSize();
            maxPreferredWidth = Math.max(maxPreferredWidth, d.width);
            maxPreferredHeight = Math.max(maxPreferredHeight, d.height);
        }

        return new Dimension(maxPreferredWidth, maxPreferredHeight);
    }
}
