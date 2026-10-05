package ie.ucd.gpa;

import javafx.geometry.Orientation;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

final class ResponsiveCalendarPane extends Region {
    private final Region timeline;
    private final double naturalWidth;
    private final Scale scale = new Scale(1.0, 1.0, 0.0, 0.0);

    ResponsiveCalendarPane(Region timeline, double naturalWidth) {
        this.timeline = timeline;
        this.naturalWidth = naturalWidth;
        timeline.setManaged(false);
        timeline.getTransforms().add(scale);
        getChildren().add(timeline);
        setMinWidth(0.0);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefWidth(double height) {
        return naturalWidth;
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        return timeline.prefHeight(naturalWidth) * fitScale(width);
    }

    @Override
    protected void layoutChildren() {
        double factor = fitScale(getWidth());
        timeline.resizeRelocate(0.0, 0.0, naturalWidth, timeline.prefHeight(naturalWidth));
        scale.setX(factor);
        scale.setY(factor);
        timeline.layout();
    }

    private double fitScale(double width) {
        return width < 0.0 ? 1.0 : width / naturalWidth;
    }
}
