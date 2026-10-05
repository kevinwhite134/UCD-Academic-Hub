package ie.ucd.gpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

final class ResponsiveDashboardPaneTest {
    @Test
    void fittedModuleGridKeepsThreeColumnsAtEveryWidth() {
        Region first = new Region();
        Region second = new Region();
        Region third = new Region();
        first.setPrefHeight(500);
        second.setPrefHeight(600);
        third.setPrefHeight(550);
        ResponsiveDashboardPane grid = new ResponsiveDashboardPane(320, 3, 14, first, second, third);
        ResponsiveCalendarPane fitted = new ResponsiveCalendarPane(grid, 988);
        for (double width : new double[]{500, 700, 1200}) {
            fitted.resize(width, fitted.prefHeight(width));
            fitted.layout();
            assertEquals(first.getLayoutY(), second.getLayoutY());
            assertEquals(first.getLayoutY(), third.getLayoutY());
            assertEquals(width, grid.getBoundsInParent().getWidth(), 0.001);
        }
    }

    @Test
    void reflowsSummaryCardsWithoutClippingAtNarrowAndWideWidths() {
        Region[] cards = new Region[5];
        for (int index = 0; index < cards.length; index++) {
            cards[index] = new Region();
            cards[index].setPrefHeight(100 + index * 10);
        }
        ResponsiveDashboardPane pane = new ResponsiveDashboardPane(220, 5, 14, cards);
        for (double width : new double[]{650, 1200, 650}) {
            pane.resize(width, pane.prefHeight(width));
            pane.layout();
            for (Region card : cards) {
                assertTrue(card.getBoundsInParent().getMaxX() <= width + 0.001);
                assertTrue(card.getBoundsInParent().getMaxY() <= pane.getHeight() + 0.001);
            }
            if (width == 650) {
                assertEquals(cards[0].getLayoutY(), cards[1].getLayoutY());
                assertTrue(cards[2].getLayoutY() > cards[0].getLayoutY());
            } else {
                assertEquals(cards[0].getLayoutY(), cards[4].getLayoutY());
            }
        }
    }

    @Test
    void showsTwoModulesOnLaptopAndThreeOnMonitor() {
        Region first = new Region();
        Region second = new Region();
        Region third = new Region();
        first.setPrefHeight(500);
        second.setPrefHeight(600);
        third.setPrefHeight(550);
        ResponsiveDashboardPane pane = new ResponsiveDashboardPane(320, 3, 14, first, second, third);
        pane.resize(700, pane.prefHeight(700));
        pane.layout();
        assertEquals(343, first.getWidth());
        assertEquals(first.getLayoutY(), second.getLayoutY());
        assertEquals(614, third.getLayoutY());
        pane.resize(1200, pane.prefHeight(1200));
        pane.layout();
        assertEquals(first.getLayoutY(), second.getLayoutY());
        assertEquals(first.getLayoutY(), third.getLayoutY());
        assertEquals(1172.0 / 3, first.getWidth(), 0.001);
        assertEquals(600, first.getHeight());
        pane.resize(500, pane.prefHeight(500));
        pane.layout();
        assertEquals(500, first.getWidth());
        assertTrue(second.getLayoutY() > first.getLayoutY());
    }
}
