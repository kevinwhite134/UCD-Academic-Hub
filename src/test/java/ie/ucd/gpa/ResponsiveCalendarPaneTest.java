package ie.ucd.gpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

final class ResponsiveCalendarPaneTest {
    @Test
    void fitsAllColumnsAndPreservesWeekAlignmentAcrossWindowSizes() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        for (int column = 0; column < 15; column++) {
            double width = column == 0 ? 205 : column == 14 ? 82 : column == 13 ? 64 : 58;
            grid.getColumnConstraints().add(new ColumnConstraints(width, width, width));
        }
        Region weekHeader = new Region();
        Region highlight = new Region();
        Region exam = new Region();
        weekHeader.setPrefHeight(42);
        highlight.setPrefHeight(100);
        exam.setPrefHeight(42);
        grid.add(highlight, 5, 0, 1, 2);
        grid.add(weekHeader, 5, 0);
        grid.add(exam, 13, 0);
        ResponsiveCalendarPane pane = new ResponsiveCalendarPane(grid, 1159);

        for (double width : new double[]{600, 900, 1500, 600}) {
            double height = pane.prefHeight(width);
            pane.resize(width, height);
            pane.layout();
            assertEquals(width, grid.getBoundsInParent().getWidth(), 0.001);
            assertEquals(height, grid.getBoundsInParent().getHeight(), 0.001);
            assertEquals(weekHeader.getLayoutX(), highlight.getLayoutX(), 0.001);
            assertEquals(weekHeader.getWidth(), highlight.getWidth(), 0.001);
            assertEquals(64, exam.getWidth(), 0.001);
        }
    }
}
