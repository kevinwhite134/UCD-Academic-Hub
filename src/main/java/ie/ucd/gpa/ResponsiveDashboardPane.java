package ie.ucd.gpa;

import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.layout.Region;

final class ResponsiveDashboardPane extends Region {
    private final double minimumItemWidth;
    private final int maximumColumns;
    private final double gap;
    private final List<Region> items;

    ResponsiveDashboardPane(double minimumItemWidth, int maximumColumns, double gap, Region... items) {
        this.minimumItemWidth = minimumItemWidth;
        this.maximumColumns = maximumColumns;
        this.gap = gap;
        this.items = List.of(items);
        for (Region item : items) {
            item.setManaged(false);
            item.setMinWidth(0);
        }
        getChildren().addAll(items);
        setMinWidth(0);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefWidth(double height) {
        return snappedLeftInset() + snappedRightInset()
                + minimumItemWidth * maximumColumns + gap * (maximumColumns - 1);
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        return arrange(width < 0 ? computePrefWidth(-1) : width, false);
    }

    @Override
    protected void layoutChildren() {
        arrange(getWidth(), true);
    }

    private double arrange(double width, boolean layout) {
        double available = Math.max(0, width - snappedLeftInset() - snappedRightInset());
        int columns = Math.max(1, Math.min(maximumColumns,
                (int) Math.floor((available + gap) / (minimumItemWidth + gap))));
        double itemWidth = Math.max(0, (available - gap * (columns - 1)) / columns);
        double y = snappedTopInset();
        for (int start = 0; start < items.size(); start += columns) {
            int end = Math.min(items.size(), start + columns);
            double rowHeight = 0;
            for (int index = start; index < end; index++) {
                rowHeight = Math.max(rowHeight, items.get(index).prefHeight(itemWidth));
            }
            if (layout) {
                for (int index = start; index < end; index++) {
                    Region item = items.get(index);
                    item.resizeRelocate(snappedLeftInset() + (index - start) * (itemWidth + gap),
                            y, itemWidth, rowHeight);
                    item.layout();
                }
            }
            y += rowHeight + (end < items.size() ? gap : 0);
        }
        return y + snappedBottomInset();
    }
}
