package ie.ucd.gpa;

import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

import javafx.animation.AnimationTimer;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public final class UcdGpaCalculatorApp extends Application {
    private static final double SCROLL_DISTANCE_MULTIPLIER = 2.6;
    static final DecimalFormat GPA_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat POINT_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.#");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter CALENDAR_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy");
    private static final List<String> PRIORITIES = List.of("Critical", "High", "Normal", "Low");
    private static final List<ModuleColour> MODULE_COLOURS = List.of(
            new ModuleColour("Blue", "#2f80ed"),
            new ModuleColour("Purple", "#7c3aed"),
            new ModuleColour("Green", "#219653"),
            new ModuleColour("Coral", "#eb5757"),
            new ModuleColour("Gold", "#f2c94c"),
            new ModuleColour("Teal", "#119da4")
    );

    private final List<SubjectEntry> subjects = new ArrayList<>();
    private final BooleanProperty percentageMode = new SimpleBooleanProperty(true);
    private final BooleanProperty darkMode = new SimpleBooleanProperty(true);
    private final BooleanProperty gpaPointsMode = new SimpleBooleanProperty(false);
    private final DoubleProperty calendarZoom = new SimpleDoubleProperty(0.82);
    private final Label gpaLabel = new Label("GPA: -");
    private final Label classificationLabel = new Label("Classification: -");
    private final GridPane resultsGrid = new GridPane();
    private final AcademicDataStore dataStore = new AcademicDataStore();

    private AcademicHubState state = new AcademicHubState();
    private BorderPane shell;
    private HubPage activePage = HubPage.CALENDAR;
    private AcademicModule editingModule;
    private AssessmentEntry editingAssessment;
    private AcademicTask editingTask;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            state = dataStore.load();
        } catch (IOException ex) {
            state = new AcademicHubState();
            System.err.println("Could not load Academic Hub data: " + ex.getMessage());
        }

        shell = new BorderPane();
        shell.getStyleClass().add("hub-root");
        applyTheme();
        shell.setLeft(buildSidebar());
        shell.setCenter(buildCalendarPage());

        Scene scene = new Scene(shell, 1240, 820);
        scene.getStylesheets().add(getClass().getResource("/ie/ucd/gpa/styles.css").toExternalForm());
        scene.setOnKeyPressed(event -> {
            if (!event.isControlDown() || activePage != HubPage.CALENDAR) {
                return;
            }
            if (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.SUBTRACT) {
                changeCalendarZoom(-0.08);
                event.consume();
            } else if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.ADD || event.getCode() == KeyCode.EQUALS) {
                changeCalendarZoom(0.08);
                event.consume();
            } else if (event.getCode() == KeyCode.DIGIT0 || event.getCode() == KeyCode.NUMPAD0) {
                calendarZoom.set(0.82);
                showPage(HubPage.CALENDAR);
                event.consume();
            }
        });

        stage.setTitle("UCD Academic Hub");
        stage.setMinWidth(1060);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    private void showPage(HubPage page) {
        showPage(page, null);
    }

    private void showPage(HubPage page, Double scrollPosition) {
        activePage = page;
        if (page != HubPage.MODULES) {
            editingModule = null;
            editingAssessment = null;
        }
        if (page != HubPage.TASKS) {
            editingTask = null;
        }

        shell.setLeft(buildSidebar());
        Node content = switch (page) {
            case DASHBOARD -> buildDashboardPage();
            case MODULES -> buildModulesPage();
            case TASKS -> buildTasksPage();
            case CALENDAR -> buildCalendarPage();
            case GPA -> buildGpaLandingPage();
        };
        if (scrollPosition != null && content instanceof ScrollPane scrollPane) {
            scrollPane.setVvalue(scrollPosition);
        }
        shell.setCenter(content);
    }

    private VBox buildSidebar() {
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/ie/ucd/gpa/ucd-logo.png")));
        logo.setFitWidth(46);
        logo.setFitHeight(58);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        StackPane logoPlate = new StackPane(logo);
        logoPlate.getStyleClass().add("logo-plate");

        Label title = new Label("UCD Academic Hub");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label("Local academic dashboard");
        subtitle.getStyleClass().add("sidebar-subtitle");

        VBox brandText = new VBox(4, title, subtitle);
        HBox brand = new HBox(12, logoPlate, brandText);
        brand.setAlignment(Pos.CENTER_LEFT);

        VBox nav = new VBox(7,
                navButton(HubPage.CALENDAR),
                navButton(HubPage.DASHBOARD),
                navButton(HubPage.GPA)
        );
        Separator editDivider = new Separator();
        editDivider.getStyleClass().add("sidebar-divider");
        Label editLabel = new Label("EDIT");
        editLabel.getStyleClass().add("sidebar-section-label");
        nav.getChildren().addAll(editDivider, editLabel, navButton(HubPage.MODULES), navButton(HubPage.TASKS));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label storage = new Label("Autosaves locally\n" + dataStore.dataFile());
        storage.getStyleClass().add("storage-note");
        storage.setWrapText(true);

        ToggleButton themeToggle = new ToggleButton(darkMode.get() ? "Light theme" : "Dark theme");
        themeToggle.getStyleClass().add("theme-toggle");
        themeToggle.setMaxWidth(Double.MAX_VALUE);
        themeToggle.setOnAction(event -> {
            Double scrollPosition = currentPageScrollPosition();
            darkMode.set(!darkMode.get());
            applyTheme();
            showPage(activePage, scrollPosition);
        });

        ToggleButton displayModeToggle = new ToggleButton("GPA x100");
        displayModeToggle.getStyleClass().addAll("theme-toggle", "display-mode-toggle");
        displayModeToggle.setSelected(gpaPointsMode.get());
        displayModeToggle.setMaxWidth(Double.MAX_VALUE);
        displayModeToggle.setOnAction(event -> {
            Double scrollPosition = currentPageScrollPosition();
            gpaPointsMode.set(displayModeToggle.isSelected());
            showPage(activePage, scrollPosition);
        });

        HBox viewControls = new HBox(8, themeToggle, displayModeToggle);
        HBox.setHgrow(themeToggle, Priority.ALWAYS);
        HBox.setHgrow(displayModeToggle, Priority.ALWAYS);

        VBox sidebar = new VBox(22, brand, nav, spacer, viewControls, storage);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(214);
        return sidebar;
    }

    private Button navButton(HubPage page) {
        Button button = new Button(page.title());
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        if (page == activePage) {
            button.getStyleClass().add("nav-button-active");
        }
        button.setOnAction(event -> showPage(page));
        return button;
    }

    private ScrollPane buildDashboardPage() {
        HBox lowerPanels = new HBox(18, buildUpcomingDeadlinesPanel(6), buildPriorityTasksPanel());
        lowerPanels.getStyleClass().add("two-column-row");

        return page(
                "Dashboard",
                "Academic status at a glance",
                buildSummaryStrip(),
                buildModuleOverviewStrip(),
                buildDashboardModulesSection(),
                lowerPanels
        );
    }

    private HBox buildModuleOverviewStrip() {
        HBox overview = new HBox(10);
        overview.getStyleClass().add("module-overview-strip");
        for (AcademicModule module : state.orderedModules()) {
            ModuleProgress progress = AcademicCalculations.progressFor(module, state.assessmentsFor(module.id()));
            VBox wheel = compactProgressWheel(progress, module);
            Label name = new Label(module.displayName());
            name.getStyleClass().add("overview-module-name");
            name.setWrapText(true);
            VBox item = new VBox(4, wheel, name);
            item.getStyleClass().add("overview-module");
            item.setAlignment(Pos.CENTER);
            addHoverMotion(item, 1.035);
            overview.getChildren().add(item);
        }
        return overview;
    }

    private HBox buildSummaryStrip() {
        OptionalDouble gpa = AcademicCalculations.currentGpa(state);
        long modulesPassed = state.modules().stream()
                .filter(module -> AcademicCalculations.progressFor(module, state.assessmentsFor(module.id())).securedGrade() >= module.passGrade())
                .count();
        List<DeadlineItem> deadlines = AcademicCalculations.upcomingDeadlines(state, 1);
        String nextDeadline = deadlines.isEmpty() ? "None" : relativeDate(deadlines.getFirst().dueDate());

        HBox strip = new HBox(14,
                metricCard("Current GPA", gpa.isPresent() ? GPA_FORMAT.format(gpa.getAsDouble()) : "-", "Completed modules with credits"),
                metricRingCard("Course Completion", overallCourseCompletion(), "Average module completion"),
                metricCard("Modules Passed", modulesPassed + " / " + state.modules().size(), "Secured pass marks"),
                metricCard("Next Deadline", nextDeadline, deadlines.isEmpty() ? "No upcoming dates" : deadlines.getFirst().title())
        );
        strip.getStyleClass().add("metric-strip");
        return strip;
    }

    private OptionalDouble overallCourseCompletion() {
        double credits = totalCredits();
        if (credits <= 0.0) {
            return OptionalDouble.empty();
        }
        double total = state.modules().stream()
                .mapToDouble(module -> AcademicCalculations.progressFor(module, state.assessmentsFor(module.id())).completedWeight()
                        * module.credits())
                .sum();
        return OptionalDouble.of(total / credits);
    }

    private VBox metricCard(String labelText, String valueText, String detailText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("metric-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("metric-value");
        Label detail = new Label(detailText);
        detail.getStyleClass().add("muted");
        detail.setWrapText(true);

        VBox card = new VBox(5, label, value, detail);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(190);
        HBox.setHgrow(card, Priority.ALWAYS);
        addHoverMotion(card, 1.018);
        return card;
    }

    private VBox metricRingCard(String labelText, OptionalDouble percentage, String detailText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("metric-label");
        Label detail = new Label(detailText);
        detail.getStyleClass().add("muted");
        detail.setWrapText(true);

        double value = percentage.orElse(0.0);
        VBox card = new VBox(7, label, percentageRing(value, "#00e5ff", "Complete", 62, semesterValue(value)), detail);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(190);
        HBox.setHgrow(card, Priority.ALWAYS);
        addHoverMotion(card, 1.018);
        return card;
    }

    private VBox percentageRing(double value, String colour, String labelText, double size, String displayText) {
        double clamped = Math.max(0.0, Math.min(100.0, value));
        double strokeWidth = Math.max(3.0, size * 0.065);
        double inset = strokeWidth / 2.0 + 1.5;
        double diameter = size - inset * 2.0;

        Canvas graphic = new Canvas(size, size);
        GraphicsContext context = graphic.getGraphicsContext2D();
        context.setLineWidth(strokeWidth);
        context.setLineCap(StrokeLineCap.ROUND);
        context.setStroke(darkMode.get() ? Color.rgb(226, 232, 240, 0.13) : Color.rgb(21, 50, 66, 0.10));
        context.strokeOval(inset, inset, diameter, diameter);

        if (clamped >= 99.95) {
            context.setStroke(Color.web(colour));
            context.strokeOval(inset, inset, diameter, diameter);
        } else if (clamped > 0.0) {
            context.setStroke(Color.web(colour));
            context.strokeArc(inset, inset, diameter, diameter, 90.0, -clamped / 100.0 * 360.0, javafx.scene.shape.ArcType.OPEN);
        }

        context.setTextAlign(TextAlignment.CENTER);
        context.setTextBaseline(VPos.CENTER);
        context.setFill(Color.web(darkMode.get() ? "#f5f7fa" : "#162b39"));
        context.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(11.0, size * 0.18)));
        context.fillText(displayText, size / 2.0, size / 2.0);

        VBox wrapper;
        if (labelText == null || labelText.isBlank()) {
            wrapper = new VBox(graphic);
        } else {
            Label label = new Label(labelText);
            label.getStyleClass().add("ring-label");
            wrapper = new VBox(4, graphic, label);
        }
        wrapper.getStyleClass().add("percentage-ring");
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private VBox moduleProgressWheel(ModuleProgress progress, AcademicModule module) {
        double size = 118.0;
        double strokeWidth = 9.0;
        double inset = strokeWidth / 2.0 + 5.0;
        double diameter = size - inset * 2.0;
        double secured = Math.max(0.0, Math.min(100.0, progress.securedGrade()));
        double completed = Math.max(0.0, Math.min(100.0, progress.completedWeight()));
        double pass = Math.max(0.0, Math.min(100.0, module.passGrade()));

        Canvas graphic = new Canvas(size, size);
        GraphicsContext context = graphic.getGraphicsContext2D();
        context.setLineCap(StrokeLineCap.ROUND);
        context.setLineWidth(strokeWidth);
        context.setStroke(Color.rgb(21, 50, 66, 0.10));
        context.strokeOval(inset, inset, diameter, diameter);

        if (completed > 0.0) {
            context.setStroke(darkMode.get() ? Color.rgb(226, 232, 240, 0.24) : Color.rgb(21, 50, 66, 0.16));
            context.strokeArc(inset, inset, diameter, diameter, 90.0, -completed / 100.0 * 360.0, javafx.scene.shape.ArcType.OPEN);
        }
        if (secured > 0.0) {
            context.setStroke(Color.web(module.colour()));
            context.strokeArc(inset, inset, diameter, diameter, 90.0, -secured / 100.0 * 360.0, javafx.scene.shape.ArcType.OPEN);
        }

        double angle = Math.toRadians(90.0 - pass / 100.0 * 360.0);
        double center = size / 2.0;
        double outerRadius = diameter / 2.0 + 7.0;
        double innerRadius = diameter / 2.0 - 8.0;
        context.setStroke(Color.web("#f2c94c"));
        context.setLineWidth(4.0);
        context.strokeLine(
                center + Math.cos(angle) * innerRadius,
                center - Math.sin(angle) * innerRadius,
                center + Math.cos(angle) * outerRadius,
                center - Math.sin(angle) * outerRadius
        );

        context.setTextAlign(TextAlignment.CENTER);
        context.setTextBaseline(VPos.CENTER);
        context.setFill(Color.web(darkMode.get() ? "#f5f7fa" : "#162b39"));
        context.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22.0));
        context.fillText(moduleValue(secured, module), center, center - 5.0);
        context.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.0));
        context.setFill(Color.web(darkMode.get() ? "#aebdca" : "#647884"));
        context.fillText("secured", center, center + 16.0);

        Label complete = progressStat("Complete", progress.completedWeight(), "#162b39", module);
        Label passMark = progressStat("Pass mark", module.passGrade(), "#9a6a00", module);
        Label remaining = progressStat("Remaining", progress.remainingWeight(), "#647884", module);
        HBox stats = new HBox(10, complete, passMark, remaining);
        stats.setAlignment(Pos.CENTER);

        VBox wheel = new VBox(7, graphic, stats);
        wheel.getStyleClass().add("module-progress-wheel");
        wheel.setAlignment(Pos.CENTER);
        addHoverMotion(wheel, 1.035);
        return wheel;
    }

    private VBox compactProgressWheel(ModuleProgress progress, AcademicModule module) {
        double size = 66.0;
        double strokeWidth = 6.0;
        double inset = 7.0;
        double diameter = size - inset * 2.0;
        double secured = Math.max(0.0, Math.min(100.0, progress.securedGrade()));
        double pass = Math.max(0.0, Math.min(100.0, module.passGrade()));

        Canvas canvas = new Canvas(size, size);
        GraphicsContext context = canvas.getGraphicsContext2D();
        context.setLineCap(StrokeLineCap.ROUND);
        context.setLineWidth(strokeWidth);
        context.setStroke(darkMode.get() ? Color.rgb(226, 232, 240, 0.14) : Color.rgb(21, 50, 66, 0.10));
        context.strokeOval(inset, inset, diameter, diameter);
        if (secured > 0.0) {
            context.setStroke(Color.web(module.colour()));
            context.strokeArc(inset, inset, diameter, diameter, 90.0, -secured / 100.0 * 360.0, javafx.scene.shape.ArcType.OPEN);
        }

        double angle = Math.toRadians(90.0 - pass / 100.0 * 360.0);
        double center = size / 2.0;
        context.setStroke(Color.web("#f2c94c"));
        context.setLineWidth(3.0);
        context.strokeLine(
                center + Math.cos(angle) * (diameter / 2.0 - 5.0),
                center - Math.sin(angle) * (diameter / 2.0 - 5.0),
                center + Math.cos(angle) * (diameter / 2.0 + 4.0),
                center - Math.sin(angle) * (diameter / 2.0 + 4.0)
        );
        context.setTextAlign(TextAlignment.CENTER);
        context.setTextBaseline(VPos.CENTER);
        context.setFill(Color.web(darkMode.get() ? "#f5f7fa" : "#162b39"));
        context.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13.0));
        context.fillText(moduleValue(secured, module), center, center);

        VBox wheel = new VBox(canvas);
        wheel.setAlignment(Pos.CENTER);
        return wheel;
    }

    private Label progressStat(String labelText, double value, String colour, AcademicModule module) {
        Label label = new Label(labelText + "\n" + moduleValue(value, module));
        label.getStyleClass().add("progress-stat");
        String themedColour = darkMode.get()
                ? ("#9a6a00".equalsIgnoreCase(colour) ? "#f2c94c" : "#cbd5df")
                : colour;
        label.setTextFill(Color.web(themedColour));
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private VBox buildDashboardAssessmentChecklist(AcademicModule module) {
        Label title = new Label("Assignments");
        title.getStyleClass().add("small-label");

        VBox rows = new VBox(3);
        List<AssessmentEntry> assessments = state.assessmentsFor(module.id());
        if (assessments.isEmpty()) {
            Label empty = new Label("No assignments added yet.");
            empty.getStyleClass().add("muted");
            rows.getChildren().add(empty);
        } else {
            for (AssessmentEntry assessment : assessments) {
                rows.getChildren().add(dashboardAssessmentRow(assessment));
            }
        }

        VBox box = new VBox(4, title, rows);
        box.getStyleClass().add("dashboard-assessments");
        return box;
    }

    private VBox dashboardAssessmentRow(AssessmentEntry assessment) {
        CheckBox completed = new CheckBox();
        completed.setSelected(assessment.completed());
        completed.getStyleClass().add("completion-check");

        Label name = new Label(assessment.name());
        name.getStyleClass().add("assessment-title");
        name.setWrapText(true);

        Label finalBadge = new Label("FINAL");
        finalBadge.getStyleClass().add("assessment-final-badge");
        finalBadge.setVisible(assessment.finalExam());
        finalBadge.setManaged(assessment.finalExam());

        HBox titleRow = new HBox(8, name, finalBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        HBox metadata = new HBox(
                18,
                assessmentMetadata("Weight", moduleValue(assessment.weight(), assessment.moduleId())),
                assessmentMetadata("Due", formatDate(assessment.dueDate()))
        );
        metadata.getStyleClass().add("assessment-metadata");

        VBox summary = new VBox(4, titleRow, metadata);

        Label gradeLabel = new Label("Recorded grade");
        gradeLabel.getStyleClass().add("assessment-grade-label");
        TextField grade = new TextField(assessment.grade() == null ? "" : PERCENT_FORMAT.format(assessment.grade()));
        grade.setPromptText("0-100");
        grade.getStyleClass().add("dashboard-grade-field");
        grade.setPrefWidth(58);
        Label suffix = new Label("%");
        suffix.getStyleClass().add("small-label");

        HBox gradeRow = new HBox(6, gradeLabel, grade, suffix);
        gradeRow.setAlignment(Pos.CENTER_LEFT);
        gradeRow.getStyleClass().add("dashboard-grade-row");
        gradeRow.setVisible(assessment.completed());
        gradeRow.setManaged(assessment.completed());

        Label doneIcon = new Label("\u2713");
        doneIcon.getStyleClass().add("assessment-done-icon");
        doneIcon.setVisible(assessment.completed());
        doneIcon.setManaged(assessment.completed());

        Button open = smallButton("Open");
        boolean hasUrl = assessment.url() != null && !assessment.url().isBlank();
        open.setVisible(hasUrl);
        open.setManaged(hasUrl);
        open.setOnAction(event -> openUrl(assessment.url()));

        HBox topRow = new HBox(12, completed, summary, spacer(), doneIcon, open);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox row = new VBox(8, topRow, gradeRow);
        row.getStyleClass().add("dashboard-assessment-row");
        if (assessment.completed()) {
            row.getStyleClass().add("dashboard-assessment-row-completed");
        }

        boolean[] suppressGradeSave = {false};

        completed.setOnAction(event -> {
            assessment.setCompleted(completed.isSelected());
            row.getStyleClass().remove("dashboard-assessment-row-completed");
            if (completed.isSelected()) {
                row.getStyleClass().add("dashboard-assessment-row-completed");
            }
            doneIcon.setVisible(completed.isSelected());
            doneIcon.setManaged(completed.isSelected());
            if (completed.isSelected()) {
                persistOnly();
                gradeRow.setVisible(true);
                gradeRow.setManaged(true);
                grade.requestFocus();
            } else {
                suppressGradeSave[0] = true;
                persistAndRefresh(HubPage.DASHBOARD);
                suppressGradeSave[0] = false;
            }
        });

        grade.setOnAction(event -> {
            suppressGradeSave[0] = true;
            saveDashboardAssessmentGrade(assessment, grade);
            suppressGradeSave[0] = false;
        });
        grade.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                Platform.runLater(() -> {
                    if (!suppressGradeSave[0] && grade.getScene() != null) {
                        saveDashboardAssessmentGrade(assessment, grade);
                    }
                });
            }
        });

        return row;
    }

    private VBox assessmentMetadata(String labelText, String valueText) {
        Label label = new Label(labelText.toUpperCase());
        label.getStyleClass().add("assessment-meta-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("assessment-meta-value");
        return new VBox(1, label, value);
    }

    private void saveDashboardAssessmentGrade(AssessmentEntry assessment, TextField gradeField) {
        Double grade = parseOptionalPercent(gradeField, "Assessment grade");
        if (grade == InvalidNumber.VALUE) {
            gradeField.setText(assessment.grade() == null ? "" : PERCENT_FORMAT.format(assessment.grade()));
            return;
        }
        Double existingGrade = assessment.grade();
        boolean gradeChanged = existingGrade == null ? grade != null : !existingGrade.equals(grade);
        boolean completionChanged = grade != null && !assessment.completed();
        if (!gradeChanged && !completionChanged) {
            return;
        }
        assessment.setGrade(grade);
        if (grade != null && !assessment.completed()) {
            assessment.setCompleted(true);
        }
        persistAndRefresh(HubPage.DASHBOARD);
    }

    private VBox buildDashboardModulesSection() {
        Label title = new Label("Current Modules");
        title.getStyleClass().add("section-title");

        if (state.modules().isEmpty()) {
            Button addModule = new Button("Add Module");
            addModule.getStyleClass().add("primary-button");
            addModule.setOnAction(event -> showPage(HubPage.MODULES));
            return new VBox(12, title, emptyState("No modules yet", "Create your first module to start tracking grades, progress and deadlines.", addModule));
        }

        VBox moduleCards = new VBox(14);
        List<AcademicModule> modules = state.orderedModules();
        for (int index = 0; index < modules.size(); index += 2) {
            HBox row = new HBox(14);
            VBox firstCard = buildModuleCard(modules.get(index), true);
            HBox.setHgrow(firstCard, Priority.ALWAYS);
            row.getChildren().add(firstCard);

            if (index + 1 < modules.size()) {
                VBox secondCard = buildModuleCard(modules.get(index + 1), true);
                HBox.setHgrow(secondCard, Priority.ALWAYS);
                row.getChildren().add(secondCard);
            } else {
                Region emptyColumn = new Region();
                emptyColumn.setMinWidth(0);
                emptyColumn.setPrefWidth(0);
                HBox.setHgrow(emptyColumn, Priority.ALWAYS);
                row.getChildren().add(emptyColumn);
            }
            moduleCards.getChildren().add(row);
        }

        VBox section = new VBox(12, title, moduleCards);
        section.getStyleClass().add("section-block");
        return section;
    }

    private VBox buildModuleCard(AcademicModule module, boolean showActions) {
        ModuleProgress progress = AcademicCalculations.progressFor(module, state.assessmentsFor(module.id()));

        Label code = new Label(module.moduleCode().isBlank() ? "Module" : module.moduleCode());
        code.getStyleClass().add("eyebrow");
        Label name = new Label(module.name());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);

        Label status = new Label(progress.passStatus());
        status.getStyleClass().addAll("status-pill", statusClass(progress.passStatus()));

        Label credits = new Label(PERCENT_FORMAT.format(module.credits()) + " credits");
        credits.getStyleClass().add("credits-pill");

        HBox header = new HBox(6, new VBox(1, code, name), spacer(), status);
        header.setAlignment(Pos.TOP_LEFT);

        Label nextAssessment = new Label(nextAssessmentText(module));
        nextAssessment.getStyleClass().add("next-deadline");
        nextAssessment.setWrapText(true);

        Label weightStatus = new Label("Assessment weights: " + moduleValue(progress.totalWeight(), module));
        weightStatus.getStyleClass().add(progress.assessmentWeightsComplete() ? "ok-text" : "warning-text");

        VBox card = new VBox(5,
                moduleColourBar(module.colour()),
                header,
                moduleProgressWheel(progress, module),
                buildTargetCalculator(progress),
                buildDashboardAssessmentChecklist(module),
                nextAssessment,
                new HBox(6, weightStatus, spacer(), credits)
        );
        card.getStyleClass().add("module-card");
        card.setPrefWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(0);
        card.setStyle(moduleCardStyle(module.colour()));
        addHoverMotion(card, 1.012);

        if (showActions) {
            Button open = new Button("Open Module");
            open.getStyleClass().add("small-button");
            open.setOnAction(event -> {
                editingModule = module;
                showPage(HubPage.MODULES);
            });
            card.getChildren().add(open);
        }

        return card;
    }

    private Node buildTargetCalculator(ModuleProgress progress) {
        Label title = new Label("Target Grade");
        title.getStyleClass().add("small-label");

        TextField targetField = new TextField("60");
        targetField.getStyleClass().add("compact-number-field");
        targetField.setPrefWidth(58);
        Label suffix = new Label("%");
        suffix.getStyleClass().add("small-label");

        VBox result = new VBox();
        result.getStyleClass().add("target-result");

        Runnable update = () -> updateTargetResult(progress, targetField, result);
        targetField.textProperty().addListener((observable, oldValue, newValue) -> update.run());

        HBox inputRow = new HBox(6, targetField, suffix);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        update.run();
        return new VBox(4, title, inputRow, result);
    }

    private void updateTargetResult(ModuleProgress progress, TextField targetField, VBox result) {
        result.getChildren().clear();
        try {
            double target = Double.parseDouble(targetField.getText().trim());
            if (target < 0.0 || target > 100.0) {
                result.getChildren().add(targetMessage("Choose 0 to 100%."));
                return;
            }
            double required = AcademicCalculations.requiredAverage(progress.securedGrade(), progress.remainingWeight(), target);
            if (required == 0.0) {
                result.getChildren().add(targetMessage("Target already secured."));
            } else if (Double.isInfinite(required) || required > 100.0) {
                result.getChildren().add(targetMessage("This target is mathematically impossible from the remaining weight."));
            } else {
                HBox row = new HBox(6,
                        percentageRing(required, "#007aff", "Need", 42, percent(required)),
                        targetMessage("average across remaining assessments.")
                );
                row.setAlignment(Pos.CENTER_LEFT);
                result.getChildren().add(row);
            }
        } catch (NumberFormatException ex) {
            result.getChildren().add(targetMessage("Enter a target percentage."));
        }
    }

    private Label targetMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("target-message");
        label.setWrapText(true);
        return label;
    }

    private VBox buildUpcomingDeadlinesPanel(int limit) {
        Label title = new Label("Upcoming Deadlines");
        title.getStyleClass().add("section-title");

        VBox list = new VBox(9);
        List<DeadlineItem> deadlines = AcademicCalculations.upcomingDeadlines(state, limit);
        if (deadlines.isEmpty()) {
            list.getChildren().add(new Label("No upcoming deadlines."));
        } else {
            deadlines.forEach(deadline -> list.getChildren().add(deadlineRow(deadline)));
        }

        VBox panel = new VBox(12, title, list);
        panel.getStyleClass().add("panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private VBox buildPriorityTasksPanel() {
        Label title = new Label("Tasks Requiring Attention");
        title.getStyleClass().add("section-title");

        VBox list = new VBox(9);
        List<AcademicTask> tasks = state.orderedTasks().stream()
                .filter(task -> !task.completed())
                .sorted(Comparator
                        .comparingInt((AcademicTask task) -> priorityRank(task.priority()))
                        .thenComparing(AcademicTask::displayOrder))
                .limit(5)
                .toList();

        if (tasks.isEmpty()) {
            list.getChildren().add(new Label("No open tasks."));
        } else {
            tasks.forEach(task -> list.getChildren().add(taskCard(task, false)));
        }

        VBox panel = new VBox(12, title, list);
        panel.getStyleClass().add("panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private ScrollPane buildModulesPage() {
        HBox forms = new HBox(18, buildModuleFormPanel(), buildAssessmentFormPanel());
        forms.getStyleClass().add("two-column-row");

        return page(
                "Modules",
                "Create modules, set assessment structure and track weighted grades",
                forms,
                buildModuleManagementList(),
                buildAssessmentBreakdown()
        );
    }

    private VBox buildModuleFormPanel() {
        boolean editing = editingModule != null;
        Label title = new Label(editing ? "Edit Module" : "Add Module");
        title.getStyleClass().add("section-title");

        TextField name = textField("Software Engineering", editing ? editingModule.name() : "");
        TextField code = textField("COMP20010", editing ? editingModule.moduleCode() : "");
        TextArea description = textArea("Module notes", editing ? editingModule.description() : "");
        ComboBox<ModuleColour> colour = colourPicker(editing ? editingModule.colour() : MODULE_COLOURS.getFirst().hex());
        TextField passGrade = textField("40", editing ? PERCENT_FORMAT.format(editingModule.passGrade()) : "40");
        TextField credits = textField("5", editing ? PERCENT_FORMAT.format(editingModule.credits()) : "5");
        TextField semester = textField("Autumn", editing ? editingModule.semester() : "");
        TextField academicYear = textField("2026/27", editing ? editingModule.academicYear() : "");

        GridPane form = formGrid();
        addFormRow(form, 0, "Name", name);
        addFormRow(form, 1, "Code", code);
        addFormRow(form, 2, "Description", description);
        addFormRow(form, 3, "Colour", colour);
        addFormRow(form, 4, "Pass Grade", passGrade);
        addFormRow(form, 5, "Credits", credits);
        addFormRow(form, 6, "Semester", semester);
        addFormRow(form, 7, "Academic Year", academicYear);

        Button save = new Button(editing ? "Update Module" : "Add Module");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveModule(name, code, description, colour, passGrade, credits, semester, academicYear));

        HBox actions = new HBox(8, save);
        if (editing) {
            Button cancel = new Button("Cancel");
            cancel.getStyleClass().add("secondary-button");
            cancel.setOnAction(event -> {
                editingModule = null;
                showPage(HubPage.MODULES);
            });
            actions.getChildren().add(cancel);
        }

        VBox panel = new VBox(12, title, form, actions);
        panel.getStyleClass().add("panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private void saveModule(
            TextField nameField,
            TextField codeField,
            TextArea descriptionField,
            ComboBox<ModuleColour> colourField,
            TextField passGradeField,
            TextField creditsField,
            TextField semesterField,
            TextField academicYearField
    ) {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Module name needed", "Add a module name before saving.");
            return;
        }

        Double passGrade = parseRequiredPercent(passGradeField, "Pass grade");
        Double credits = parseRequiredNumber(creditsField, "Credits");
        if (passGrade == null || credits == null) {
            return;
        }
        if (credits <= 0.0) {
            showAlert(Alert.AlertType.WARNING, "Credits must be positive", "Enter a credit value above zero.");
            return;
        }

        ModuleColour colour = colourField.getValue() == null ? MODULE_COLOURS.getFirst() : colourField.getValue();
        if (editingModule == null) {
            state.modules().add(AcademicModule.create(
                    name,
                    codeField.getText().trim(),
                    descriptionField.getText().trim(),
                    colour.hex(),
                    passGrade,
                    credits,
                    semesterField.getText().trim(),
                    academicYearField.getText().trim(),
                    state.nextModuleOrder()
            ));
        } else {
            editingModule.setName(name);
            editingModule.setModuleCode(codeField.getText().trim());
            editingModule.setDescription(descriptionField.getText().trim());
            editingModule.setColour(colour.hex());
            editingModule.setPassGrade(passGrade);
            editingModule.setCredits(credits);
            editingModule.setSemester(semesterField.getText().trim());
            editingModule.setAcademicYear(academicYearField.getText().trim());
            editingModule = null;
        }
        persistAndRefresh(HubPage.MODULES);
    }

    private VBox buildAssessmentFormPanel() {
        Label title = new Label(editingAssessment == null ? "Add Assessment" : "Edit Assessment");
        title.getStyleClass().add("section-title");

        if (state.modules().isEmpty()) {
            Button addModule = new Button("Add Module First");
            addModule.getStyleClass().add("primary-button");
            addModule.setOnAction(event -> showPage(HubPage.MODULES));
            VBox panel = new VBox(12, title, new Label("Assessments need a module."), addModule);
            panel.getStyleClass().add("panel");
            HBox.setHgrow(panel, Priority.ALWAYS);
            return panel;
        }

        ComboBox<String> module = moduleSelector(false, editingAssessment == null ? state.orderedModules().getFirst().id() : editingAssessment.moduleId());
        TextField name = textField("Assignment 1", editingAssessment == null ? "" : editingAssessment.name());
        TextField weight = textField("20", editingAssessment == null ? "" : PERCENT_FORMAT.format(editingAssessment.weight()));
        DatePicker dueDate = new DatePicker(editingAssessment == null ? null : editingAssessment.dueDate());
        ComboBox<String> dueMode = new ComboBox<>();
        dueMode.getItems().addAll("Exact date", "Term week");
        dueMode.setValue(editingAssessment != null && editingAssessment.termWeek() != null ? "Term week" : "Exact date");
        ComboBox<Integer> dueWeek = new ComboBox<>();
        for (int week = 1; week <= 12; week++) {
            dueWeek.getItems().add(week);
        }
        dueWeek.setValue(editingAssessment != null && editingAssessment.termWeek() != null
                ? editingAssessment.termWeek()
                : currentTermWeek(termStart()));
        DayOfWeek savedAssessmentDay = editingAssessment != null
                && editingAssessment.termWeek() != null
                && editingAssessment.dueDate() != null
                ? editingAssessment.dueDate().getDayOfWeek()
                : DayOfWeek.MONDAY;
        ComboBox<DayOfWeek> dueDay = termDaySelector(savedAssessmentDay);
        dueDate.disableProperty().bind(dueMode.valueProperty().isEqualTo("Term week"));
        dueWeek.disableProperty().bind(dueMode.valueProperty().isNotEqualTo("Term week"));
        dueDay.disableProperty().bind(dueMode.valueProperty().isNotEqualTo("Term week"));
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(editingAssessment != null && editingAssessment.completed());
        CheckBox finalExam = new CheckBox("Final exam");
        finalExam.setSelected(editingAssessment != null && editingAssessment.finalExam());
        TextField grade = textField("75", editingAssessment == null || editingAssessment.grade() == null ? "" : PERCENT_FORMAT.format(editingAssessment.grade()));
        TextArea notes = textArea("Optional notes", editingAssessment == null ? "" : editingAssessment.notes());
        TextField url = textField("https://...", editingAssessment == null ? "" : editingAssessment.url());

        GridPane form = formGrid();
        addFormRow(form, 0, "Module", module);
        addFormRow(form, 1, "Name", name);
        addFormRow(form, 2, "Weight", weight);
        addFormRow(form, 3, "Schedule by", dueMode);
        addFormRow(form, 4, "Exact date", dueDate);
        addFormRow(form, 5, "Term week", dueWeek);
        addFormRow(form, 6, "Weekday", dueDay);
        addFormRow(form, 7, "Status", completed);
        addFormRow(form, 8, "Calendar", finalExam);
        addFormRow(form, 9, "Grade", grade);
        addFormRow(form, 10, "Notes", notes);
        addFormRow(form, 11, "URL", url);

        Button save = new Button(editingAssessment == null ? "Add Assessment" : "Update Assessment");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveAssessment(module, name, weight, dueMode, dueDate, dueWeek, dueDay, completed, finalExam, grade, notes, url));

        HBox actions = new HBox(8, save);
        if (editingAssessment != null) {
            Button cancel = new Button("Cancel");
            cancel.getStyleClass().add("secondary-button");
            cancel.setOnAction(event -> {
                editingAssessment = null;
                showPage(HubPage.MODULES);
            });
            actions.getChildren().add(cancel);
        }

        VBox panel = new VBox(12, title, form, actions);
        panel.getStyleClass().add("panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private void saveAssessment(
            ComboBox<String> moduleField,
            TextField nameField,
            TextField weightField,
            ComboBox<String> dueModeField,
            DatePicker dueDateField,
            ComboBox<Integer> dueWeekField,
            ComboBox<DayOfWeek> dueDayField,
            CheckBox completedField,
            CheckBox finalExamField,
            TextField gradeField,
            TextArea notesField,
            TextField urlField
    ) {
        String moduleId = moduleField.getValue();
        if (moduleId == null || moduleId.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Module needed", "Choose the module this assessment belongs to.");
            return;
        }

        String name = nameField.getText().trim();
        if (name.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Assessment name needed", "Add an assessment name before saving.");
            return;
        }

        Double weight = parseRequiredPercent(weightField, "Assessment weight");
        Double grade = parseOptionalPercent(gradeField, "Assessment grade");
        if (weight == null || grade == InvalidNumber.VALUE) {
            return;
        }
        if (completedField.isSelected() && grade == null) {
            showAlert(Alert.AlertType.WARNING, "Grade needed", "Completed assessments need a grade so the secured mark can be calculated.");
            return;
        }

        Integer termWeek = "Term week".equals(dueModeField.getValue()) ? dueWeekField.getValue() : null;
        LocalDate dueDate = termWeek == null
                ? dueDateField.getValue()
                : dateForTermWeek(termWeek, dueDayField.getValue());

        if (editingAssessment == null) {
            state.assessments().add(AssessmentEntry.create(
                    moduleId,
                    name,
                    weight,
                    grade,
                    completedField.isSelected(),
                    dueDate,
                    notesField.getText().trim(),
                    urlField.getText().trim(),
                    finalExamField.isSelected(),
                    termWeek
            ));
        } else {
            editingAssessment.setModuleId(moduleId);
            editingAssessment.setName(name);
            editingAssessment.setWeight(weight);
            editingAssessment.setGrade(grade);
            editingAssessment.setCompleted(completedField.isSelected());
            editingAssessment.setFinalExam(finalExamField.isSelected());
            editingAssessment.setDueDate(dueDate);
            editingAssessment.setTermWeek(termWeek);
            editingAssessment.setNotes(notesField.getText().trim());
            editingAssessment.setUrl(urlField.getText().trim());
            editingAssessment = null;
        }

        double totalWeight = AcademicCalculations.assessmentWeightTotal(state, moduleId);
        persistAndRefresh(HubPage.MODULES);
        if (totalWeight > 100.0) {
            showAlert(Alert.AlertType.WARNING, "Assessment weights exceed 100%", "This module now has " + percent(totalWeight) + " total assessment weight.");
        }
    }

    private VBox buildModuleManagementList() {
        Label title = new Label("Module Order");
        title.getStyleClass().add("section-title");

        VBox list = new VBox(10);
        if (state.modules().isEmpty()) {
            list.getChildren().add(new Label("No modules created yet."));
        } else {
            List<AcademicModule> modules = state.orderedModules();
            for (int i = 0; i < modules.size(); i++) {
                AcademicModule module = modules.get(i);
                list.getChildren().add(moduleManagementRow(module, i, modules.size()));
            }
        }

        VBox panel = new VBox(12, title, list);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private HBox moduleManagementRow(AcademicModule module, int index, int size) {
        Label name = new Label(module.displayName());
        name.getStyleClass().add("row-title");
        Label detail = new Label(module.semester() + (module.academicYear().isBlank() ? "" : " - " + module.academicYear()));
        detail.getStyleClass().add("muted");

        Button edit = smallButton("Edit");
        edit.setOnAction(event -> {
            editingModule = module;
            showPage(HubPage.MODULES);
        });

        Button up = smallButton("Up");
        up.setDisable(index == 0);
        up.setOnAction(event -> moveModule(module, -1));

        Button down = smallButton("Down");
        down.setDisable(index == size - 1);
        down.setOnAction(event -> moveModule(module, 1));

        Button delete = smallButton("Delete");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> {
            state.removeModule(module);
            persistAndRefresh(HubPage.MODULES);
        });

        HBox row = new HBox(12, moduleColourRule(module.colour()), new VBox(3, name, detail), spacer(), up, down, edit, delete);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    private void moveModule(AcademicModule module, int offset) {
        List<AcademicModule> ordered = new ArrayList<>(state.orderedModules());
        int index = ordered.indexOf(module);
        int targetIndex = index + offset;
        if (index < 0 || targetIndex < 0 || targetIndex >= ordered.size()) {
            return;
        }
        Collections.swap(ordered, index, targetIndex);
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setDisplayOrder(i);
        }
        persistAndRefresh(HubPage.MODULES);
    }

    private VBox buildAssessmentBreakdown() {
        Label title = new Label("Assessment Breakdown");
        title.getStyleClass().add("section-title");

        VBox content = new VBox(14);
        if (state.modules().isEmpty()) {
            content.getChildren().add(new Label("Assessments will appear here after modules are created."));
        } else {
            for (AcademicModule module : state.orderedModules()) {
                content.getChildren().add(assessmentModuleBlock(module));
            }
        }

        VBox panel = new VBox(12, title, content);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private VBox assessmentModuleBlock(AcademicModule module) {
        Label title = new Label(module.displayName());
        title.getStyleClass().add("reference-title");

        double totalWeight = AcademicCalculations.assessmentWeightTotal(state, module.id());
        Label weight = new Label("Total weight: " + moduleValue(totalWeight, module));
        weight.getStyleClass().add(Math.abs(totalWeight - 100.0) < 0.01 ? "ok-text" : "warning-text");

        VBox rows = new VBox(8);
        List<AssessmentEntry> assessments = state.assessmentsFor(module.id());
        if (assessments.isEmpty()) {
            rows.getChildren().add(new Label("No assessments yet."));
        } else {
            for (AssessmentEntry assessment : assessments) {
                rows.getChildren().add(assessmentRow(module, assessment));
            }
        }

        VBox block = new VBox(8, new HBox(10, moduleColourRule(module.colour()), new VBox(2, title, weight)), rows);
        block.getStyleClass().add("subsection");
        return block;
    }

    private HBox assessmentRow(AcademicModule module, AssessmentEntry assessment) {
        Label name = new Label(assessment.name());
        name.getStyleClass().add("row-title");
        Label details = new Label(
                moduleValue(assessment.weight(), module) + " weight"
                        + " | " + (assessment.completed() ? "Completed" : "Remaining")
                        + " | Grade: " + (assessment.grade() == null ? "-" : percent(assessment.grade()))
                        + " | Earned: " + moduleValue(assessment.earnedContribution(), module)
        );
        details.getStyleClass().add("muted");
        Label date = new Label(formatDate(assessment.dueDate()));
        date.getStyleClass().add("date-pill");
        Label finalBadge = new Label("Final");
        finalBadge.getStyleClass().add("final-badge");
        finalBadge.setVisible(assessment.finalExam());
        finalBadge.setManaged(assessment.finalExam());

        Button open = smallButton("Open");
        open.setDisable(assessment.url().isBlank());
        open.setOnAction(event -> openUrl(assessment.url()));

        Button edit = smallButton("Edit");
        edit.setOnAction(event -> {
            editingAssessment = assessment;
            editingModule = module;
            showPage(HubPage.MODULES);
        });

        Button delete = smallButton("Delete");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> {
            state.assessments().remove(assessment);
            persistAndRefresh(HubPage.MODULES);
        });

        HBox row = new HBox(12, new VBox(4, name, details), spacer(), finalBadge, date, open, edit, delete);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    private ScrollPane buildTasksPage() {
        return page(
                "Tasks",
                "Academic task cards with priority, order and links",
                buildTaskFormPanel(),
                buildTaskListPanel()
        );
    }

    private VBox buildTaskFormPanel() {
        boolean editing = editingTask != null;
        Label title = new Label(editing ? "Edit Task" : "Add Task");
        title.getStyleClass().add("section-title");

        ComboBox<String> module = moduleSelector(true, editing ? editingTask.moduleId() : "");
        TextField taskTitle = textField("Finish Scala Assignment", editing ? editingTask.title() : "");
        TextArea description = textArea("Task notes", editing ? editingTask.description() : "");
        ComboBox<String> priority = new ComboBox<>();
        priority.getItems().addAll(PRIORITIES);
        priority.setValue(editing ? editingTask.priority() : "Normal");
        DatePicker dueDate = new DatePicker(editing ? editingTask.dueDate() : null);
        ComboBox<String> dueMode = new ComboBox<>();
        dueMode.getItems().addAll("Exact date", "Term week");
        dueMode.setValue(editing && editingTask.termWeek() != null ? "Term week" : "Exact date");
        ComboBox<Integer> dueWeek = new ComboBox<>();
        for (int week = 1; week <= 12; week++) {
            dueWeek.getItems().add(week);
        }
        dueWeek.setValue(editing && editingTask.termWeek() != null
                ? editingTask.termWeek()
                : currentTermWeek(termStart()));
        DayOfWeek savedTaskDay = editing
                && editingTask.termWeek() != null
                && editingTask.dueDate() != null
                ? editingTask.dueDate().getDayOfWeek()
                : DayOfWeek.MONDAY;
        ComboBox<DayOfWeek> dueDay = termDaySelector(savedTaskDay);
        dueDate.disableProperty().bind(dueMode.valueProperty().isEqualTo("Term week"));
        dueWeek.disableProperty().bind(dueMode.valueProperty().isNotEqualTo("Term week"));
        dueDay.disableProperty().bind(dueMode.valueProperty().isNotEqualTo("Term week"));
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(editing && editingTask.completed());
        TextField url = textField("https://...", editing ? editingTask.url() : "");
        TextField buttonText = textField("Open Brightspace", editing ? editingTask.buttonText() : "Open");

        GridPane form = formGrid();
        addFormRow(form, 0, "Module", module);
        addFormRow(form, 1, "Title", taskTitle);
        addFormRow(form, 2, "Description", description);
        addFormRow(form, 3, "Priority", priority);
        addFormRow(form, 4, "Schedule by", dueMode);
        addFormRow(form, 5, "Exact date", dueDate);
        addFormRow(form, 6, "Term week", dueWeek);
        addFormRow(form, 7, "Weekday", dueDay);
        addFormRow(form, 8, "Status", completed);
        addFormRow(form, 9, "URL", url);
        addFormRow(form, 10, "Button Label", buttonText);

        Button save = new Button(editing ? "Update Task" : "Add Task");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveTask(module, taskTitle, description, priority, dueMode, dueDate, dueWeek, dueDay, completed, url, buttonText));

        HBox actions = new HBox(8, save);
        if (editing) {
            Button cancel = new Button("Cancel");
            cancel.getStyleClass().add("secondary-button");
            cancel.setOnAction(event -> {
                editingTask = null;
                showPage(HubPage.TASKS);
            });
            actions.getChildren().add(cancel);
        }

        VBox panel = new VBox(12, title, form, actions);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private void saveTask(
            ComboBox<String> moduleField,
            TextField titleField,
            TextArea descriptionField,
            ComboBox<String> priorityField,
            ComboBox<String> dueModeField,
            DatePicker dueDateField,
            ComboBox<Integer> dueWeekField,
            ComboBox<DayOfWeek> dueDayField,
            CheckBox completedField,
            TextField urlField,
            TextField buttonTextField
    ) {
        String title = titleField.getText().trim();
        if (title.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Task title needed", "Add a task title before saving.");
            return;
        }

        String moduleId = moduleField.getValue() == null ? "" : moduleField.getValue();
        String priority = priorityField.getValue() == null ? "Normal" : priorityField.getValue();
        String buttonText = buttonTextField.getText().trim().isBlank() ? "Open" : buttonTextField.getText().trim();
        Integer termWeek = "Term week".equals(dueModeField.getValue()) ? dueWeekField.getValue() : null;
        LocalDate dueDate = termWeek == null
                ? dueDateField.getValue()
                : dateForTermWeek(termWeek, dueDayField.getValue());

        if (editingTask == null) {
            state.tasks().add(AcademicTask.create(
                    moduleId,
                    title,
                    descriptionField.getText().trim(),
                    priority,
                    dueDate,
                    completedField.isSelected(),
                    urlField.getText().trim(),
                    buttonText,
                    state.nextTaskOrder(),
                    termWeek
            ));
        } else {
            editingTask.setModuleId(moduleId);
            editingTask.setTitle(title);
            editingTask.setDescription(descriptionField.getText().trim());
            editingTask.setPriority(priority);
            editingTask.setDueDate(dueDate);
            editingTask.setTermWeek(termWeek);
            editingTask.setCompleted(completedField.isSelected());
            editingTask.setUrl(urlField.getText().trim());
            editingTask.setButtonText(buttonText);
            editingTask = null;
        }
        persistAndRefresh(HubPage.TASKS);
    }

    private VBox buildTaskListPanel() {
        Label title = new Label("Task Cards");
        title.getStyleClass().add("section-title");

        VBox list = new VBox(10);
        List<AcademicTask> tasks = state.orderedTasks();
        if (tasks.isEmpty()) {
            list.getChildren().add(new Label("No tasks yet."));
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                list.getChildren().add(taskCard(tasks.get(i), true));
            }
        }

        VBox panel = new VBox(12, title, list);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private HBox taskCard(AcademicTask task, boolean showActions) {
        Label title = new Label(task.title());
        title.getStyleClass().add("row-title");
        title.setWrapText(true);

        Label meta = new Label(moduleName(task.moduleId()) + " | " + task.priority() + " | Due: " + formatDate(task.dueDate()));
        meta.getStyleClass().add("muted");
        meta.setWrapText(true);

        Label description = new Label(task.description().isBlank() ? "No description" : task.description());
        description.getStyleClass().add("task-description");
        description.setWrapText(true);

        Label status = new Label(task.completed() ? "Completed" : "Open");
        status.getStyleClass().addAll("status-pill", task.completed() ? "status-passed" : statusClass(task.priority()));

        VBox text = new VBox(4, title, meta, description);
        HBox row = new HBox(12, moduleColourRule(moduleColour(task.moduleId())), text, spacer(), status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-card");
        HBox.setHgrow(text, Priority.ALWAYS);

        if (!showActions) {
            Button open = smallButton(task.buttonText().isBlank() ? "Open" : task.buttonText());
            open.setDisable(task.url().isBlank());
            open.setOnAction(event -> openUrl(task.url()));
            row.getChildren().add(open);
        } else {
            Button up = smallButton("Up");
            Button down = smallButton("Down");
            List<AcademicTask> tasks = state.orderedTasks();
            int index = tasks.indexOf(task);
            up.setDisable(index <= 0);
            down.setDisable(index >= tasks.size() - 1);
            up.setOnAction(event -> moveTask(task, -1));
            down.setOnAction(event -> moveTask(task, 1));

            Button toggle = smallButton(task.completed() ? "Reopen" : "Done");
            toggle.setOnAction(event -> {
                task.setCompleted(!task.completed());
                persistAndRefresh(HubPage.TASKS);
            });

            Button open = smallButton(task.buttonText().isBlank() ? "Open" : task.buttonText());
            open.setDisable(task.url().isBlank());
            open.setOnAction(event -> openUrl(task.url()));

            Button edit = smallButton("Edit");
            edit.setOnAction(event -> {
                editingTask = task;
                showPage(HubPage.TASKS);
            });

            Button delete = smallButton("Delete");
            delete.getStyleClass().add("danger-button");
            delete.setOnAction(event -> {
                state.tasks().remove(task);
                state.renumberTasks();
                persistAndRefresh(HubPage.TASKS);
            });

            row.getChildren().addAll(up, down, toggle, open, edit, delete);
        }

        return row;
    }

    private void moveTask(AcademicTask task, int offset) {
        List<AcademicTask> ordered = new ArrayList<>(state.orderedTasks());
        int index = ordered.indexOf(task);
        int targetIndex = index + offset;
        if (index < 0 || targetIndex < 0 || targetIndex >= ordered.size()) {
            return;
        }
        Collections.swap(ordered, index, targetIndex);
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setDisplayOrder(i);
        }
        persistAndRefresh(HubPage.TASKS);
    }

    private ScrollPane buildCalendarPage() {
        LocalDate termStart = termStart();
        IntegerProperty selectedWeek = new SimpleIntegerProperty(currentTermWeek(termStart));
        String[] selectedModuleId = {null};
        VBox selectedWeekItems = new VBox(10);
        Label selectedTitle = new Label();
        selectedTitle.getStyleClass().add("calendar-detail-title");

        GridPane timeline = new GridPane();
        timeline.getStyleClass().add("term-grid");
        timeline.setHgap(8);
        timeline.setVgap(9);

        List<AcademicModule> modules = state.orderedModules();
        List<Button> weekButtons = new ArrayList<>();
        List<Region> weekHighlights = new ArrayList<>();
        List<Region> moduleHighlights = new ArrayList<>();
        Runnable refreshSelection = () -> {
            if (selectedModuleId[0] == null) {
                selectedWeekItems.getChildren().setAll(calendarItemsForWeek(termStart, selectedWeek.get()));
                selectedTitle.setText("Week " + selectedWeek.get() + " - " + weekRange(termStart, selectedWeek.get()));
            } else {
                AcademicModule selectedModule = state.moduleById(selectedModuleId[0]).orElse(null);
                selectedWeekItems.getChildren().setAll(calendarItemsForModule(termStart, selectedModuleId[0]));
                selectedTitle.setText(selectedModule == null ? "Module" : selectedModule.displayName());
            }
            weekButtons.forEach(button -> button.getStyleClass().remove("week-button-active"));
            if (selectedWeek.get() >= 1 && selectedWeek.get() <= weekButtons.size()) {
                weekButtons.get(selectedWeek.get() - 1).getStyleClass().add("week-button-active");
            }
            for (int index = 0; index < weekHighlights.size(); index++) {
                weekHighlights.get(index).getStyleClass().remove("calendar-week-selected");
                if (index + 1 == selectedWeek.get()) {
                    weekHighlights.get(index).getStyleClass().add("calendar-week-selected");
                }
            }
            for (int index = 0; index < moduleHighlights.size(); index++) {
                moduleHighlights.get(index).getStyleClass().remove("calendar-module-selected");
                if (selectedModuleId[0] != null && modules.get(index).id().equals(selectedModuleId[0])) {
                    moduleHighlights.get(index).getStyleClass().add("calendar-module-selected");
                }
            }
        };

        for (int week = 1; week <= 12; week++) {
            Region highlight = new Region();
            highlight.getStyleClass().add("calendar-week-highlight");
            highlight.setMouseTransparent(true);
            timeline.add(highlight, week, 0, 1, modules.size() + 2);
            weekHighlights.add(highlight);
        }
        for (int row = 0; row < modules.size(); row++) {
            Region highlight = new Region();
            highlight.getStyleClass().add("calendar-module-highlight");
            highlight.setMouseTransparent(true);
            timeline.add(highlight, 0, row + 2, 15, 1);
            moduleHighlights.add(highlight);
        }

        Label modulesHeader = new Label("Your term");
        modulesHeader.getStyleClass().add("term-axis-label");
        timeline.add(modulesHeader, 0, 0);

        for (int week = 1; week <= 12; week++) {
            int targetWeek = week;
            Button weekButton = new Button(week == currentTermWeek(termStart) ? week + "\nNOW" : String.valueOf(week));
            weekButton.getStyleClass().add("week-button");
            double weekWidth = 48.0 * calendarZoom.get();
            weekButton.setStyle("-fx-min-width: " + weekWidth + "; -fx-pref-width: " + weekWidth + ";");
            weekButton.setOnAction(event -> {
                selectedModuleId[0] = null;
                selectedWeek.set(targetWeek);
                refreshSelection.run();
            });
            weekButtons.add(weekButton);
            timeline.add(weekButton, week, 0);
        }

        Label examsHeader = new Label("EXAMS");
        examsHeader.getStyleClass().add("term-axis-label");
        timeline.add(examsHeader, 13, 0);

        timeline.add(calendarProgressHeader(), 14, 0);

        Label shareHeader = new Label("share of grade");
        shareHeader.getStyleClass().add("calendar-share-label");
        timeline.add(shareHeader, 0, 1);

        List<Double> weeklyShares = new ArrayList<>();
        for (int week = 1; week <= 12; week++) {
            weeklyShares.add(calendarWeekTermShare(termStart, week));
        }
        double examShare = calendarExamTermShare();
        double shareScale = Math.max(
                1.0,
                Math.max(examShare, weeklyShares.stream().mapToDouble(Double::doubleValue).max().orElse(0.0))
        );
        for (int week = 1; week <= 12; week++) {
            timeline.add(calendarTotalCell(weeklyShares.get(week - 1), false, shareScale), week, 1);
        }
        timeline.add(calendarTotalCell(examShare, true, shareScale), 13, 1);

        double overallSecured = averageSecuredGrade();
        double overallCompleted = overallCourseCompletion().orElse(0.0);
        timeline.add(calendarProgressBar(overallSecured, overallCompleted), 14, 1);

        for (int row = 0; row < modules.size(); row++) {
            AcademicModule module = modules.get(row);
            Label moduleName = new Label(module.displayName());
            moduleName.getStyleClass().add("calendar-module-name");
            Label moduleCode = new Label(module.moduleCode());
            moduleCode.getStyleClass().add("calendar-module-code");
            Region colourRule = moduleColourRule(module.colour());
            colourRule.getStyleClass().add("calendar-module-colour-rule");
            VBox moduleLabel = new VBox(2, new HBox(8, colourRule, new VBox(1, moduleName, moduleCode)));
            moduleLabel.getStyleClass().add("calendar-module-label");
            double moduleWidth = 155.0 * calendarZoom.get();
            moduleLabel.setStyle("-fx-min-width: " + moduleWidth + "; -fx-pref-width: " + moduleWidth + ";");
            moduleLabel.setOnMouseClicked(event -> {
                selectedModuleId[0] = module.id();
                refreshSelection.run();
            });
            timeline.add(moduleLabel, 0, row + 2);

            for (int week = 1; week <= 12; week++) {
                StackPane cell = calendarWeekCell(termStart, module, week, selectedWeek, selectedModuleId, refreshSelection);
                double cellWidth = 48.0 * calendarZoom.get();
                cell.setStyle("-fx-min-width: " + cellWidth + "; -fx-pref-width: " + cellWidth + ";");
                timeline.add(cell, week, row + 2);
            }

            StackPane examCell = calendarExamCell(module, selectedModuleId, refreshSelection);
            double examWidth = 52.0 * calendarZoom.get();
            examCell.setStyle("-fx-min-width: " + examWidth + "; -fx-pref-width: " + examWidth + ";");
            timeline.add(examCell, 13, row + 2);

            ModuleProgress progress = AcademicCalculations.progressFor(module, state.assessmentsFor(module.id()));
            timeline.add(calendarProgressBar(progress.securedGrade(), progress.completedWeight(), module), 14, row + 2);
        }

        Label termTitle = new Label("Autumn 2026");
        termTitle.getStyleClass().add("calendar-hero-title");
        Label termMeta = new Label("Week " + currentTermWeek(termStart) + " of 12");
        termMeta.getStyleClass().add("calendar-hero-meta");
        Label credits = new Label(PERCENT_FORMAT.format(totalCredits()) + " credits - " + state.modules().size() + " modules");
        credits.getStyleClass().add("calendar-credits");

        Button zoomOut = smallButton("-");
        zoomOut.setOnAction(event -> changeCalendarZoom(-0.08));
        Button zoomIn = smallButton("+");
        zoomIn.setOnAction(event -> changeCalendarZoom(0.08));
        Label zoomLabel = new Label(Math.round(calendarZoom.get() * 100.0) + "%");
        zoomLabel.getStyleClass().add("calendar-zoom-label");
        HBox zoomControls = new HBox(6, zoomOut, zoomLabel, zoomIn);
        zoomControls.getStyleClass().add("calendar-zoom-controls");
        zoomControls.setAlignment(Pos.CENTER);

        VBox titleStack = new VBox(3, termTitle, termMeta);
        HBox top = new HBox(12, titleStack, spacer(), zoomControls, credits);
        top.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(calendarHeadline(termStart));
        headline.getStyleClass().add("calendar-headline");
        headline.setWrapText(true);

        VBox detailPanel = new VBox(14, new HBox(selectedTitle, spacer()), selectedWeekItems);
        detailPanel.getStyleClass().add("calendar-detail-panel");

        ScrollPane timelineScroll = new ScrollPane(timeline);
        timelineScroll.setFitToHeight(true);
        timelineScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        timelineScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        timelineScroll.getStyleClass().add("timeline-scroll");

        VBox calendar = new VBox(22, top, headline, timelineScroll, detailPanel);
        calendar.getStyleClass().add("calendar-board");
        refreshSelection.run();

        return page(
                "Calendar",
                "Assessment and task deadlines by week",
                calendar,
                buildUpcomingDeadlinesPanel(20)
        );
    }

    private int currentTermWeek(LocalDate termStart) {
        return (int) Math.max(1, Math.min(12, ChronoUnit.WEEKS.between(termStart, LocalDate.now()) + 1));
    }

    private LocalDate termStart() {
        return LocalDate.of(LocalDate.now().getYear(), 9, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    private double totalCredits() {
        return state.modules().stream().mapToDouble(AcademicModule::credits).sum();
    }

    private double averageSecuredGrade() {
        double credits = totalCredits();
        if (credits <= 0.0) {
            return 0.0;
        }
        return state.modules().stream()
                .mapToDouble(module -> AcademicCalculations.progressFor(module, state.assessmentsFor(module.id())).securedGrade()
                        * module.credits())
                .sum() / credits;
    }

    private VBox calendarProgressHeader() {
        Label title = new Label("PROGRESS");
        title.getStyleClass().add("term-axis-label");
        Label grade = new Label("GRADE");
        grade.getStyleClass().add("calendar-progress-secured");
        Label done = new Label("DONE");
        done.getStyleClass().add("calendar-progress-completed");
        HBox legend = new HBox(7, grade, done);
        legend.setAlignment(Pos.CENTER);
        VBox header = new VBox(2, title, legend);
        header.setAlignment(Pos.CENTER);
        header.getStyleClass().add("calendar-progress-header");
        return header;
    }

    private VBox calendarProgressBar(double securedValue, double completedValue) {
        return calendarProgressBar(securedValue, completedValue, null);
    }

    private VBox calendarProgressBar(double securedValue, double completedValue, AcademicModule module) {
        double secured = Math.max(0.0, Math.min(100.0, securedValue));
        double completed = Math.max(0.0, Math.min(100.0, completedValue));
        double availableHeight = 36.0;
        double securedHeight = secured <= 0.0 ? 0.0 : Math.max(1.0, secured / 100.0 * availableHeight);
        double completedHeight = completed <= 0.0 ? 0.0 : Math.max(1.0, completed / 100.0 * availableHeight);

        Region completedBar = calendarProgressSegment("calendar-progress-bar-completed", completedHeight);
        Region securedBar = calendarProgressSegment("calendar-progress-bar-secured", securedHeight);
        StackPane fill = new StackPane(completedBar, securedBar);
        StackPane.setAlignment(completedBar, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(securedBar, Pos.BOTTOM_CENTER);

        StackPane track = new StackPane(fill);
        track.setAlignment(Pos.BOTTOM_CENTER);
        track.getStyleClass().add("calendar-progress-track");

        Label securedLabel = new Label(module == null
                ? calendarSemesterValue(secured)
                : calendarModuleValue(secured, module));
        securedLabel.getStyleClass().addAll("calendar-progress-secured", "calendar-progress-value");
        Label completedLabel = new Label(module == null
                ? calendarSemesterValue(completed)
                : calendarModuleValue(completed, module));
        completedLabel.getStyleClass().addAll("calendar-progress-completed", "calendar-progress-value");
        HBox values = new HBox(6, securedLabel, completedLabel);
        values.setAlignment(Pos.CENTER);

        VBox bar = new VBox(2, values, track);
        bar.setAlignment(Pos.BOTTOM_CENTER);
        bar.getStyleClass().add("calendar-progress-bar");
        addHoverMotion(bar, 1.05);
        return bar;
    }

    private Region calendarProgressSegment(String styleClass, double height) {
        Region segment = new Region();
        segment.getStyleClass().add(styleClass);
        segment.setMinHeight(height);
        segment.setPrefHeight(height);
        segment.setMaxHeight(height);
        return segment;
    }

    private String weekRange(LocalDate termStart, int week) {
        LocalDate start = termStart.plusWeeks(week - 1L);
        LocalDate end = start.plusDays(6);
        return start.format(DateTimeFormatter.ofPattern("d MMM")) + "-" + end.format(DateTimeFormatter.ofPattern("d MMM"));
    }

    private String calendarHeadline(LocalDate termStart) {
        int nextWeek = Math.min(12, currentTermWeek(termStart) + 1);
        List<DeadlineItem> items = deadlinesForWeek(termStart, nextWeek);
        double termShare = items.stream().mapToDouble(this::deadlineTermShare).sum();
        if (items.isEmpty()) {
            return "Nothing due next week. Use the clear lane to get ahead.";
        }
        long modulesDue = items.stream().map(DeadlineItem::moduleId).distinct().count();
        return "Next week: " + calendarSemesterValue(termShare) + " of your term across " + modulesDue + " module" + (modulesDue == 1 ? "." : "s.");
    }

    private StackPane calendarWeekCell(
            LocalDate termStart,
            AcademicModule module,
            int week,
            IntegerProperty selectedWeek,
            String[] selectedModuleId,
            Runnable refreshSelection
    ) {
        List<DeadlineItem> items = deadlinesForWeek(termStart, week).stream()
                .filter(item -> module.id().equals(item.moduleId()))
                .toList();
        StackPane cell = new StackPane();
        cell.getStyleClass().add("term-cell");
        if (items.isEmpty()) {
            Label dot = new Label("-");
            dot.getStyleClass().add("calendar-dot");
            cell.getChildren().add(dot);
        } else {
            double weight = items.stream().mapToDouble(this::deadlineWeight).sum();
            boolean tasksOnly = items.stream().allMatch(item -> "Task".equals(item.type()));
            boolean allCompleted = items.stream().allMatch(DeadlineItem::completed);
            Label pill = new Label(tasksOnly ? "Task" : calendarModuleValue(weight, module));
            pill.getStyleClass().add("deadline-pill");
            if (tasksOnly) {
                pill.getStyleClass().add("deadline-pill-task");
            } else if (allCompleted) {
                pill.getStyleClass().add("deadline-pill-completed");
            }
            cell.getChildren().add(pill);
            addHoverMotion(pill, 1.08);
        }
        cell.setOnMouseClicked(event -> {
            selectedModuleId[0] = null;
            selectedWeek.set(week);
            refreshSelection.run();
        });
        return cell;
    }

    private double calendarWeekTermShare(LocalDate termStart, int week) {
        return deadlinesForWeek(termStart, week).stream().mapToDouble(this::deadlineTermShare).sum();
    }

    private double calendarExamTermShare() {
        double credits = totalCredits();
        return credits <= 0.0 ? 0.0 : state.assessments().stream()
                .filter(AssessmentEntry::finalExam)
                .mapToDouble(assessment -> assessment.weight()
                        * state.moduleById(assessment.moduleId()).map(AcademicModule::credits).orElse(0.0)
                        / credits)
                .sum();
    }

    private StackPane calendarTotalCell(double weight, boolean exam, double scale) {
        Label value = new Label(weight > 0.0 ? calendarSemesterValue(weight) : "");
        value.getStyleClass().add("calendar-total-value");
        Region bar = new Region();
        bar.getStyleClass().add(exam ? "calendar-total-bar-exam" : "calendar-total-bar");
        bar.setPrefHeight(weight > 0.0 ? Math.max(1.0, weight / Math.max(1.0, scale) * 24.0) : 1.0);
        bar.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane cell = new StackPane(bar, value);
        cell.setAlignment(bar, Pos.BOTTOM_CENTER);
        cell.setAlignment(value, Pos.TOP_CENTER);
        cell.getStyleClass().add("calendar-total-cell");
        return cell;
    }

    private StackPane calendarExamCell(AcademicModule module, String[] selectedModuleId, Runnable refreshSelection) {
        List<AssessmentEntry> finalAssessments = state.assessmentsFor(module.id()).stream()
                .filter(AssessmentEntry::finalExam)
                .toList();
        double finalWeight = finalAssessments.stream().mapToDouble(AssessmentEntry::weight).sum();
        boolean completed = !finalAssessments.isEmpty() && finalAssessments.stream().allMatch(AssessmentEntry::completed);
        Label pill = new Label(finalWeight > 0.0 ? calendarModuleValue(finalWeight, module) : "-");
        pill.getStyleClass().add(finalWeight > 0.0 ? "exam-pill" : "calendar-dot");
        if (completed) {
            pill.getStyleClass().add("deadline-pill-completed");
        }
        StackPane cell = new StackPane(pill);
        cell.getStyleClass().add("term-cell");
        if (finalWeight > 0.0) {
            addHoverMotion(pill, 1.08);
        }
        cell.setOnMouseClicked(event -> {
            selectedModuleId[0] = module.id();
            refreshSelection.run();
        });
        return cell;
    }

    private List<Node> calendarItemsForModule(LocalDate termStart, String moduleId) {
        List<AssessmentEntry> assessments = state.assessmentsFor(moduleId);
        if (assessments.isEmpty()) {
            Label empty = new Label("No assessments in this module.");
            empty.getStyleClass().add("calendar-empty");
            return List.of(empty);
        }
        return assessments.stream()
                .sorted(Comparator
                        .comparing((AssessmentEntry assessment) -> assessment.dueDate() == null ? LocalDate.MAX : assessment.dueDate())
                        .thenComparing(AssessmentEntry::name))
                .map(assessment -> (Node) calendarModuleAssessmentRow(termStart, assessment))
                .toList();
    }

    private HBox calendarModuleAssessmentRow(LocalDate termStart, AssessmentEntry assessment) {
        int week = assessment.dueDate() == null ? 0 : (int) ChronoUnit.WEEKS.between(termStart, assessment.dueDate()) + 1;
        String dateText = week >= 1 && week <= 12
                ? "Week " + week + " - " + weekRange(termStart, week)
                : formatDate(assessment.dueDate());
        Label date = new Label(dateText);
        date.getStyleClass().add("calendar-detail-date");
        Label title = new Label(assessment.name());
        title.getStyleClass().add("calendar-detail-assessment");
        title.setWrapText(true);
        Label notes = new Label(assessment.notes().isBlank() ? (assessment.finalExam() ? "Final examination" : "Assessment") : assessment.notes());
        notes.getStyleClass().add("calendar-detail-notes");
        notes.setWrapText(true);
        HBox tags = new HBox(6);
        Label type = new Label(assessment.finalExam() ? "Final" : "Assessment");
        type.getStyleClass().add("calendar-detail-type");
        if (assessment.completed()) {
            type.getStyleClass().add("calendar-detail-type-completed");
        }
        tags.getChildren().add(type);
        Label weight = new Label(calendarModuleValue(assessment.weight(), assessment.moduleId()));
        weight.getStyleClass().add("calendar-detail-weight");
        if (assessment.completed()) {
            weight.getStyleClass().add("calendar-detail-weight-completed");
        }
        VBox description = new VBox(4, title, notes, tags);
        HBox row = new HBox(18, date, description, spacer(), weight);
        HBox.setHgrow(description, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("calendar-detail-row");
        if (assessment.completed()) {
            row.getStyleClass().add("calendar-detail-row-completed");
        }
        return row;
    }

    private List<Node> calendarItemsForWeek(LocalDate termStart, int week) {
        List<DeadlineItem> items = deadlinesForWeek(termStart, week);
        if (items.isEmpty()) {
            Label empty = new Label("Nothing due in this week.");
            empty.getStyleClass().add("calendar-empty");
            return List.of(empty);
        }
        return items.stream().map(item -> (Node) calendarDetailRow(item)).toList();
    }

    private HBox calendarDetailRow(DeadlineItem item) {
        Label module = new Label(moduleName(item.moduleId()));
        module.getStyleClass().add("calendar-detail-module");
        Label title = new Label(item.title());
        title.getStyleClass().add("calendar-detail-assessment");
        Label type = new Label(item.type());
        type.getStyleClass().add("calendar-detail-type");
        if ("Task".equals(item.type())) {
            type.getStyleClass().add("calendar-detail-type-task");
        } else if (item.completed()) {
            type.getStyleClass().add("calendar-detail-type-completed");
        }
        Label weight = new Label("Task".equals(item.type())
                ? "Task"
                : calendarModuleValue(deadlineWeight(item), item.moduleId()));
        weight.getStyleClass().add("calendar-detail-weight");
        if (item.completed()) {
            weight.getStyleClass().add("calendar-detail-weight-completed");
        }
        Label date = new Label(formatCalendarDate(item.dueDate()));
        date.getStyleClass().add("calendar-detail-date");
        HBox row = new HBox(18, new VBox(3, module, date), new VBox(4, title, type), spacer(), weight);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("calendar-detail-row");
        if (item.completed()) {
            row.getStyleClass().add("calendar-detail-row-completed");
        }
        addHoverMotion(row, 1.01);
        return row;
    }

    private List<DeadlineItem> deadlinesForWeek(LocalDate termStart, int week) {
        LocalDate start = termStart.plusWeeks(week - 1L);
        LocalDate end = start.plusDays(6);
        return allOpenDeadlines().stream()
                .filter(item -> item.dueDate() != null && !item.dueDate().isBefore(start) && !item.dueDate().isAfter(end))
                .filter(item -> !isFinalAssessment(item))
                .sorted(Comparator
                        .comparingDouble(this::deadlineWeight)
                        .reversed()
                        .thenComparing(DeadlineItem::dueDate)
                        .thenComparing(DeadlineItem::title))
                .toList();
    }

    private boolean isFinalAssessment(DeadlineItem item) {
        return state.assessments().stream()
                .anyMatch(assessment -> assessment.finalExam()
                        && assessment.moduleId().equals(item.moduleId())
                        && assessment.name().equals(item.title()));
    }

    private List<DeadlineItem> allOpenDeadlines() {
        List<DeadlineItem> deadlines = new ArrayList<>();
        state.assessments().stream()
                .forEach(assessment -> deadlines.add(new DeadlineItem(
                        assessment.dueDate(),
                        assessment.name(),
                        "Assessment",
                        assessment.moduleId(),
                        assessment.url(),
                        "",
                        assessment.completed()
                )));
        state.tasks().stream()
                .forEach(task -> deadlines.add(new DeadlineItem(
                        task.dueDate(),
                        task.title(),
                        "Task",
                        task.moduleId(),
                        task.url(),
                        task.priority(),
                        task.completed()
                )));
        return deadlines;
    }

    private double deadlineWeight(DeadlineItem item) {
        return state.assessments().stream()
                .filter(assessment -> assessment.moduleId().equals(item.moduleId()) && assessment.name().equals(item.title()))
                .findFirst()
                .map(AssessmentEntry::weight)
                .orElse(0.0);
    }

    private double deadlineTermShare(DeadlineItem item) {
        double credits = totalCredits();
        if (credits <= 0.0) {
            return 0.0;
        }
        double moduleCredits = state.moduleById(item.moduleId()).map(AcademicModule::credits).orElse(0.0);
        return deadlineWeight(item) * moduleCredits / credits;
    }

    private List<Node> calendarItemsFor(LocalDate date) {
        if (date == null) {
            return List.of(new Label("Choose a date."));
        }

        List<DeadlineItem> items = AcademicCalculations.deadlinesForDate(state, date);
        if (items.isEmpty()) {
            return List.of(new Label("Nothing due on " + formatDate(date) + "."));
        }
        return items.stream()
                .map(item -> (Node) deadlineRow(item))
                .toList();
    }

    private HBox deadlineRow(DeadlineItem item) {
        Label title = new Label(item.title());
        title.getStyleClass().add("row-title");
        Label detail = new Label(item.type() + " | " + moduleName(item.moduleId()) + priorityText(item.priority()));
        detail.getStyleClass().add("muted");
        Label due = new Label(formatDate(item.dueDate()) + " | " + relativeDate(item.dueDate()));
        due.getStyleClass().add("date-pill");

        Button open = smallButton("Open");
        open.setDisable(item.url() == null || item.url().isBlank());
        open.setOnAction(event -> openUrl(item.url()));

        HBox row = new HBox(12, moduleColourRule(moduleColour(item.moduleId())), new VBox(4, title, detail), spacer(), due, open);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    private ScrollPane buildGpaLandingPage() {
        Label heading = new Label("UCD GPA Calculator");
        heading.getStyleClass().add("hero-title");

        Label subheading = new Label("Choose how many subjects to calculate.");
        subheading.getStyleClass().add("muted");

        FlowPane choices = new FlowPane(18, 18);
        choices.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 10; i++) {
            int count = i;
            Button button = circleButton(String.valueOf(i), 72);
            button.setOnAction(event -> showGpaCalculator(count));
            choices.getChildren().add(button);
        }

        VBox card = new VBox(16, heading, subheading, choices);
        card.getStyleClass().add("panel");
        return page("Calculator", "The original GPA calculator now lives here as a hub option", card);
    }

    private void showGpaCalculator(int subjectCount) {
        subjects.clear();
        for (int i = 1; i <= subjectCount; i++) {
            subjects.add(new SubjectEntry(i));
        }
        shell.setCenter(page(
                "Calculator",
                "UCD percentage and letter-grade conversion",
                buildGpaTopBar(),
                buildGpaSubjectEditor(),
                buildGpaResultsSection(),
                buildReferenceSection()
        ));
        recalculate();
    }

    private HBox buildGpaTopBar() {
        Button back = new Button("Change Subjects");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> showPage(HubPage.GPA));

        ToggleButton percentage = new ToggleButton("Percentages");
        ToggleButton letters = new ToggleButton("Letter grades");
        ToggleGroup modeGroup = new ToggleGroup();
        percentage.setToggleGroup(modeGroup);
        letters.setToggleGroup(modeGroup);
        percentage.setSelected(true);
        percentageMode.set(true);
        percentage.getStyleClass().add("mode-toggle");
        letters.getStyleClass().add("mode-toggle");

        modeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null && oldValue != null) {
                oldValue.setSelected(true);
                return;
            }
            percentageMode.set(newValue == percentage);
            recalculate();
        });

        HBox modePicker = new HBox(0, percentage, letters);
        modePicker.getStyleClass().add("segmented");

        addStyle(gpaLabel, "gpa-pill");
        HBox bar = new HBox(14, back, spacer(), modePicker, gpaLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("calculator-toolbar");
        return bar;
    }

    private FlowPane buildGpaSubjectEditor() {
        FlowPane pane = new FlowPane(18, 18);
        pane.setPrefWrapLength(980);
        for (SubjectEntry subject : subjects) {
            pane.getChildren().add(buildSubjectCard(subject));
        }
        return pane;
    }

    private VBox buildSubjectCard(SubjectEntry subject) {
        StackPane badge = subjectBadge(subject.number());

        VBox percentageBox = buildPercentageControls(subject);
        VBox letterBox = buildLetterControls(subject);
        percentageBox.visibleProperty().bind(percentageMode);
        percentageBox.managedProperty().bind(percentageMode);
        letterBox.visibleProperty().bind(percentageMode.not());
        letterBox.managedProperty().bind(percentageMode.not());

        VBox card = new VBox(14, badge, percentageBox, letterBox);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("subject-card");
        card.setPrefWidth(300);
        return card;
    }

    private VBox buildPercentageControls(SubjectEntry subject) {
        Label label = new Label("Grade scale");
        label.getStyleClass().add("field-label");

        ToggleGroup scaleGroup = new ToggleGroup();
        HBox scaleButtons = new HBox(8);
        scaleButtons.setAlignment(Pos.CENTER);
        for (GradeScale scale : UcdGradeData.SCALES) {
            ToggleButton button = new ToggleButton(scaleButtonText(scale));
            button.setToggleGroup(scaleGroup);
            button.getStyleClass().add("small-scale-button");
            button.setSelected(scale.id().equals(subject.scaleIdProperty().get()));
            button.setOnAction(event -> {
                subject.scaleIdProperty().set(scale.id());
                recalculate();
            });
            scaleButtons.getChildren().add(button);
        }

        TextField percent = new TextField();
        percent.setPromptText("Percentage");
        percent.textProperty().bindBidirectional(subject.percentageProperty());
        percent.textProperty().addListener((observable, oldValue, newValue) -> recalculate());
        percent.getStyleClass().add("number-field");

        return new VBox(8, label, scaleButtons, percent);
    }

    private VBox buildLetterControls(SubjectEntry subject) {
        Label label = new Label("Letter grade");
        label.getStyleClass().add("field-label");

        ComboBox<String> base = new ComboBox<>();
        base.getItems().addAll(UcdGradeData.LETTER_BASES);
        base.valueProperty().bindBidirectional(subject.letterBaseProperty());
        base.valueProperty().addListener((observable, oldValue, newValue) -> {
            if ("NM".equals(newValue) || "ABS".equals(newValue)) {
                subject.letterSuffixProperty().set("");
            }
            recalculate();
        });
        base.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup suffixGroup = new ToggleGroup();
        HBox suffixes = new HBox(8);
        suffixes.setAlignment(Pos.CENTER);
        for (String suffix : UcdGradeData.LETTER_SUFFIXES) {
            ToggleButton button = new ToggleButton(suffix.isBlank() ? "plain" : suffix);
            button.setToggleGroup(suffixGroup);
            button.getStyleClass().add("suffix-button");
            button.setSelected(suffix.equals(subject.letterSuffixProperty().get()));
            button.disableProperty().bind(Bindings.createBooleanBinding(
                    () -> "NM".equals(subject.letterBaseProperty().get()) || "ABS".equals(subject.letterBaseProperty().get()),
                    subject.letterBaseProperty()
            ));
            button.setOnAction(event -> {
                subject.letterSuffixProperty().set(suffix);
                recalculate();
            });
            suffixes.getChildren().add(button);
        }

        return new VBox(8, label, base, suffixes);
    }

    private VBox buildGpaResultsSection() {
        Label title = new Label("Calculated Results");
        title.getStyleClass().add("section-title");

        addStyle(classificationLabel, "classification-pill");
        resultsGrid.setHgap(16);
        resultsGrid.setVgap(9);
        addStyle(resultsGrid, "results-grid");

        Button calculate = new Button("Calculate GPA");
        calculate.getStyleClass().add("primary-button");
        calculate.setOnAction(event -> recalculate());

        VBox box = new VBox(12, title, resultsGrid, classificationLabel, calculate);
        box.getStyleClass().add("panel");
        return box;
    }

    private VBox buildReferenceSection() {
        Label title = new Label("UCD Grade Scales And Grade Points");
        title.getStyleClass().add("section-title");

        VBox scales = new VBox(14);
        for (GradeScale scale : UcdGradeData.SCALES) {
            scales.getChildren().add(scaleReference(scale));
        }

        GridPane honours = new GridPane();
        honours.getStyleClass().add("reference-grid");
        honours.setHgap(12);
        honours.setVgap(6);
        int honoursRow = 0;
        honours.add(header("Award classification"), 0, honoursRow);
        honours.add(header("GPA range"), 1, honoursRow++);
        for (HonoursBand band : UcdGradeData.HONOURS_BANDS) {
            honours.add(new Label(band.name()), 0, honoursRow);
            honours.add(new Label(band.rangeText()), 1, honoursRow++);
        }

        GridPane points = new GridPane();
        points.getStyleClass().add("reference-grid");
        points.setHgap(12);
        points.setVgap(6);
        int row = 0;
        points.add(header("Grade"), 0, row);
        points.add(header("GPA point"), 1, row++);
        for (String grade : UcdGradeData.GRADE_POINTS.keySet().stream().sorted(UcdGpaCalculatorApp::compareGrades).toList()) {
            points.add(new Label(grade), 0, row);
            points.add(new Label(POINT_FORMAT.format(UcdGradeData.gradePoint(grade))), 1, row++);
        }

        Label source = new Label("Sources: UCD Registry grade scales. Standard 40% pass, Alternative Linear 40% pass, Alternative Non-Linear 50% pass, and UCD module grade points.");
        source.getStyleClass().add("muted");
        source.setWrapText(true);

        VBox box = new VBox(16, title, scales, new Separator(), honours, new Separator(), points, source);
        box.getStyleClass().add("panel");
        return box;
    }

    private VBox scaleReference(GradeScale scale) {
        Label name = new Label(scale.displayName());
        name.getStyleClass().add("reference-title");
        Label brightspace = new Label("Brightspace: " + scale.brightspaceName());
        brightspace.getStyleClass().add("muted");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("reference-grid");
        grid.setHgap(12);
        grid.setVgap(5);
        grid.add(header("Grade"), 0, 0);
        grid.add(header("Percentage"), 1, 0);
        int row = 1;
        for (GradeBand band : scale.bands()) {
            grid.add(new Label(band.grade()), 0, row);
            grid.add(new Label(band.rangeText()), 1, row++);
        }
        grid.add(new Label("NM"), 0, row);
        grid.add(new Label("0 to <0.01"), 1, row);

        VBox box = new VBox(6, name, brightspace, grid);
        box.getStyleClass().add("reference-scale");
        return box;
    }

    private void recalculate() {
        resultsGrid.getChildren().clear();
        int row = 0;
        resultsGrid.add(header("Subject"), 0, row);
        resultsGrid.add(header("Input"), 1, row);
        resultsGrid.add(header("Grade"), 2, row);
        resultsGrid.add(header("GPA point"), 3, row++);

        double total = 0.0;
        int validSubjects = 0;
        for (SubjectEntry subject : subjects) {
            Calculation calculation = calculateSubject(subject);
            resultsGrid.add(gpaResultLabel("Subject " + subject.number()), 0, row);
            resultsGrid.add(gpaResultLabel(calculation.inputText()), 1, row);
            resultsGrid.add(gpaResultLabel(calculation.grade()), 2, row);
            resultsGrid.add(gpaResultLabel(POINT_FORMAT.format(calculation.gradePoint())), 3, row++);
            total += calculation.gradePoint();
            validSubjects++;
        }

        if (validSubjects == 0) {
            gpaLabel.setText("GPA: -");
            classificationLabel.setText("Classification: -");
        } else {
            double gpa = total / validSubjects;
            gpaLabel.setText("GPA: " + GPA_FORMAT.format(gpa));
            classificationLabel.setText("Classification: " + UcdGradeData.honoursClassification(gpa));
        }
    }

    private Label gpaResultLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("gpa-result-cell");
        return label;
    }

    private Calculation calculateSubject(SubjectEntry subject) {
        if (percentageMode.get()) {
            String raw = subject.percentageProperty().get().trim();
            if (raw.isBlank()) {
                return new Calculation("No percentage", "-", 0.0);
            }
            try {
                double percentage = Double.parseDouble(raw);
                GradeScale scale = UcdGradeData.scaleById(subject.scaleIdProperty().get());
                String grade = scale.gradeFor(percentage);
                return new Calculation(percentage + "% using " + scale.displayName(), grade, UcdGradeData.gradePoint(grade));
            } catch (IllegalArgumentException ex) {
                return new Calculation(raw, "Invalid", 0.0);
            }
        }
        String grade = subject.selectedLetterGrade();
        return new Calculation(grade, grade, UcdGradeData.gradePoint(grade));
    }

    private ScrollPane page(String titleText, String subtitleText, Node... sections) {
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("page-subtitle");

        VBox content = new VBox(18, title, subtitle);
        content.getStyleClass().add("page-content");
        content.getChildren().addAll(Arrays.asList(sections));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("page-scroll");
        configureSmoothScrolling(scrollPane);
        return scrollPane;
    }

    private void configureSmoothScrolling(ScrollPane scrollPane) {
        double[] targetValue = {scrollPane.getVvalue()};
        boolean[] animating = {false};

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double current = scrollPane.getVvalue();
                double difference = targetValue[0] - current;
                if (Math.abs(difference) < 0.0004) {
                    scrollPane.setVvalue(targetValue[0]);
                    animating[0] = false;
                    stop();
                    return;
                }
                scrollPane.setVvalue(current + difference * 0.72);
            }
        };

        scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (!animating[0]) {
                targetValue[0] = newValue.doubleValue();
            }
        });

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isShiftDown() || Math.abs(event.getDeltaY()) < 0.01 || isInsideNestedScroller(event, scrollPane)) {
                return;
            }

            double scrollableHeight = scrollPane.getContent().getLayoutBounds().getHeight()
                    - scrollPane.getViewportBounds().getHeight();
            if (scrollableHeight <= 0.0) {
                return;
            }

            double normalizedDelta = event.getDeltaY() * SCROLL_DISTANCE_MULTIPLIER / scrollableHeight;
            targetValue[0] = clamp(
                    targetValue[0] - normalizedDelta,
                    scrollPane.getVmin(),
                    scrollPane.getVmax()
            );
            if (!animating[0]) {
                animating[0] = true;
                timer.start();
            }
            event.consume();
        });
    }

    private boolean isInsideNestedScroller(ScrollEvent event, ScrollPane owner) {
        if (!(event.getTarget() instanceof Node target)) {
            return false;
        }
        for (Node current = target; current != null && current != owner; current = current.getParent()) {
            if (current instanceof TextArea || current instanceof ScrollPane) {
                return true;
            }
        }
        return false;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private VBox emptyState(String titleText, String bodyText, Button action) {
        Label title = new Label(titleText);
        title.getStyleClass().add("empty-title");
        Label body = new Label(bodyText);
        body.getStyleClass().add("muted");
        body.setWrapText(true);

        VBox box = new VBox(10, title, body, action);
        box.getStyleClass().add("empty-state");
        return box;
    }

    private GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        return form;
    }

    private ComboBox<DayOfWeek> termDaySelector(DayOfWeek selectedDay) {
        ComboBox<DayOfWeek> selector = new ComboBox<>();
        selector.getItems().addAll(DayOfWeek.values());
        selector.setValue(selectedDay == null ? DayOfWeek.MONDAY : selectedDay);
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(DayOfWeek day) {
                if (day == null) {
                    return "";
                }
                String name = day.name().toLowerCase();
                return Character.toUpperCase(name.charAt(0)) + name.substring(1);
            }

            @Override
            public DayOfWeek fromString(String value) {
                return selector.getItems().stream()
                        .filter(day -> day.name().equalsIgnoreCase(value))
                        .findFirst()
                        .orElse(DayOfWeek.MONDAY);
            }
        });
        return selector;
    }

    private LocalDate dateForTermWeek(Integer week, DayOfWeek day) {
        int safeWeek = week == null ? currentTermWeek(termStart()) : Math.max(1, Math.min(12, week));
        DayOfWeek safeDay = day == null ? DayOfWeek.MONDAY : day;
        return termStart().plusWeeks(safeWeek - 1L).plusDays(safeDay.getValue() - 1L);
    }

    private void addFormRow(GridPane form, int row, String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        form.add(label, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof TextField textField) {
            textField.setMaxWidth(Double.MAX_VALUE);
        }
        if (field instanceof TextArea textArea) {
            textArea.setMaxWidth(Double.MAX_VALUE);
        }
        if (field instanceof ComboBox<?> comboBox) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private TextField textField(String prompt, String value) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPromptText(prompt);
        field.getStyleClass().add("number-field");
        return field;
    }

    private TextArea textArea(String prompt, String value) {
        TextArea area = new TextArea(value == null ? "" : value);
        area.setPromptText(prompt);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        area.getStyleClass().add("number-field");
        return area;
    }

    private ComboBox<ModuleColour> colourPicker(String selectedHex) {
        ComboBox<ModuleColour> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(MODULE_COLOURS);
        comboBox.setCellFactory(listView -> moduleColourCell());
        comboBox.setButtonCell(moduleColourCell());
        comboBox.setValue(findColour(selectedHex));
        return comboBox;
    }

    private ListCell<ModuleColour> moduleColourCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ModuleColour item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Rectangle swatch = new Rectangle(15, 15, Color.web(item.hex()));
                swatch.setArcHeight(4);
                swatch.setArcWidth(4);
                Label label = new Label(item.name());
                HBox row = new HBox(8, swatch, label);
                row.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(row);
            }
        };
    }

    private ModuleColour findColour(String hex) {
        return MODULE_COLOURS.stream()
                .filter(colour -> colour.hex().equalsIgnoreCase(hex))
                .findFirst()
                .orElse(MODULE_COLOURS.getFirst());
    }

    private ComboBox<String> moduleSelector(boolean allowGeneral, String selectedModuleId) {
        ComboBox<String> comboBox = new ComboBox<>();
        if (allowGeneral) {
            comboBox.getItems().add("");
        }
        state.orderedModules().forEach(module -> comboBox.getItems().add(module.id()));
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String moduleId) {
                if (moduleId == null || moduleId.isBlank()) {
                    return "General";
                }
                return state.moduleById(moduleId)
                        .map(AcademicModule::displayName)
                        .orElse("Missing module");
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        if (selectedModuleId != null && comboBox.getItems().contains(selectedModuleId)) {
            comboBox.setValue(selectedModuleId);
        } else if (!comboBox.getItems().isEmpty()) {
            comboBox.setValue(comboBox.getItems().getFirst());
        }
        return comboBox;
    }

    private Region moduleColourRule(String colour) {
        Region rule = new Region();
        rule.getStyleClass().add("colour-rule");
        rule.setStyle("-fx-background-color: " + colour + ";");
        return rule;
    }

    private Region moduleColourBar(String colour) {
        Region rule = new Region();
        rule.getStyleClass().add("colour-bar");
        rule.setStyle("-fx-background-color: " + colour + ";");
        return rule;
    }

    private String moduleCardStyle(String colour) {
        if (darkMode.get()) {
            return "-fx-background-color: " + mixWithDark(colour, 0.72) + ";"
                    + "-fx-border-color: " + mixWithDark(colour, 0.30) + ";";
        }
        return "-fx-background-color: " + mixWithWhite(colour, 0.88) + ";"
                + "-fx-border-color: " + mixWithWhite(colour, 0.66) + ";";
    }

    private String mixWithDark(String colour, double darkWeight) {
        try {
            Color base = Color.web(colour);
            Color mixed = base.interpolate(Color.web("#171b22"), darkWeight);
            return rgbHex(mixed);
        } catch (IllegalArgumentException ex) {
            return "#20252d";
        }
    }

    private String mixWithWhite(String colour, double whiteWeight) {
        try {
            Color base = Color.web(colour);
            Color mixed = base.interpolate(Color.WHITE, whiteWeight);
            return rgbHex(mixed);
        } catch (IllegalArgumentException ex) {
            return "#ffffff";
        }
    }

    private String rgbHex(Color color) {
        return String.format(
                "#%02x%02x%02x",
                Math.round(color.getRed() * 255.0),
                Math.round(color.getGreen() * 255.0),
                Math.round(color.getBlue() * 255.0)
        );
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Button smallButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("small-button");
        addHoverMotion(button, 1.04);
        return button;
    }

    private Button circleButton(String text, double size) {
        Button button = new Button(text);
        button.setMinSize(size, size);
        button.setPrefSize(size, size);
        button.setMaxSize(size, size);
        button.getStyleClass().add("circle-button");
        addHoverMotion(button, 1.06);
        return button;
    }

    private StackPane subjectBadge(int number) {
        Circle circle = new Circle(42);
        circle.getStyleClass().add("subject-circle");
        Label numberLabel = new Label(String.valueOf(number));
        numberLabel.getStyleClass().add("subject-number");
        Label subjectLabel = new Label("Subject");
        subjectLabel.getStyleClass().add("subject-word");
        VBox text = new VBox(0, subjectLabel, numberLabel);
        text.setAlignment(Pos.CENTER);
        return new StackPane(circle, text);
    }

    private Label header(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("table-header");
        return label;
    }

    private Double parseRequiredPercent(TextField field, String label) {
        Double value = parseRequiredNumber(field, label);
        if (value == null) {
            return null;
        }
        if (value < 0.0 || value > 100.0) {
            showAlert(Alert.AlertType.WARNING, label + " out of range", label + " must be between 0 and 100.");
            return null;
        }
        return value;
    }

    private Double parseOptionalPercent(TextField field, String label) {
        String raw = field.getText().trim();
        if (raw.isBlank()) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
                showAlert(Alert.AlertType.WARNING, label + " out of range", label + " must be between 0 and 100.");
                return InvalidNumber.VALUE;
            }
            return value;
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, label + " must be numeric", "Enter " + label.toLowerCase() + " as a number.");
            return InvalidNumber.VALUE;
        }
    }

    private Double parseRequiredNumber(TextField field, String label) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            if (!Double.isFinite(value)) {
                showAlert(Alert.AlertType.WARNING, label + " must be finite", "Enter " + label.toLowerCase() + " as a regular number.");
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, label + " must be numeric", "Enter " + label.toLowerCase() + " as a number.");
            return null;
        }
    }

    private void persistAndRefresh(HubPage page) {
        double scrollPosition = 0.0;
        Node currentCenter = shell.getCenter();
        boolean restoreScroll = page == activePage && currentCenter instanceof ScrollPane;
        if (restoreScroll) {
            scrollPosition = ((ScrollPane) currentCenter).getVvalue();
        }
        state.renumberModules();
        state.renumberTasks();
        try {
            dataStore.save(state);
            showPage(page, restoreScroll ? scrollPosition : null);
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Could not save data", ex.getMessage());
        }
    }

    private void persistOnly() {
        state.renumberModules();
        state.renumberTasks();
        try {
            dataStore.save(state);
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Could not save data", ex.getMessage());
        }
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String target = url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*") ? url : "https://" + url;
        try {
            URI uri = new URI(target);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                getHostServices().showDocument(target);
            }
        } catch (IOException | URISyntaxException | IllegalArgumentException ex) {
            showAlert(Alert.AlertType.ERROR, "Could not open link", "Check the URL and try again: " + target);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        if (shell != null && shell.getScene() != null) {
            alert.initOwner(shell.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private String nextAssessmentText(AcademicModule module) {
        return state.assessmentsFor(module.id()).stream()
                .filter(assessment -> !assessment.completed())
                .findFirst()
                .map(assessment -> "Next assessment: " + assessment.name() + " - " + formatDate(assessment.dueDate()))
                .orElse("No remaining assessments.");
    }

    private String moduleName(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return "General";
        }
        return state.moduleById(moduleId)
                .map(AcademicModule::displayName)
                .orElse("Missing module");
    }

    private String moduleColour(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return "#5d6e78";
        }
        return state.moduleById(moduleId)
                .map(AcademicModule::colour)
                .orElse("#5d6e78");
    }

    private String priorityText(String priority) {
        return priority == null || priority.isBlank() ? "" : " | " + priority;
    }

    private int priorityRank(String priority) {
        return switch (priority) {
            case "Critical" -> 0;
            case "High" -> 1;
            case "Normal" -> 2;
            case "Low" -> 3;
            default -> 4;
        };
    }

    private String statusClass(String status) {
        return switch (status) {
            case "Already Passed" -> "status-passed";
            case "On Track", "Normal", "Low" -> "status-track";
            case "At Risk", "High" -> "status-risk";
            case "Mathematically Impossible", "Critical" -> "status-critical";
            default -> "status-track";
        };
    }

    private String formatDate(LocalDate date) {
        return date == null ? "No date" : DATE_FORMAT.format(date);
    }

    private String formatCalendarDate(LocalDate date) {
        return date == null ? "No date" : CALENDAR_DATE_FORMAT.format(date);
    }

    private String relativeDate(LocalDate date) {
        if (date == null) {
            return "No date";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (days == 0) {
            return "Today";
        }
        if (days == 1) {
            return "Tomorrow";
        }
        if (days < 0) {
            return Math.abs(days) + " days ago";
        }
        return "In " + days + " days";
    }

    private String percent(double value) {
        return PERCENT_FORMAT.format(value) + "%";
    }

    private String semesterValue(double percentage) {
        return gpaPointsMode.get()
                ? PERCENT_FORMAT.format(percentage * 4.2)
                : percent(percentage);
    }

    private String moduleValue(double percentage, AcademicModule module) {
        return moduleValue(percentage, module == null ? null : module.id());
    }

    private String moduleValue(double percentage, String moduleId) {
        if (!gpaPointsMode.get()) {
            return percent(percentage);
        }
        return PERCENT_FORMAT.format(percentage * 4.2 * moduleCreditShare(moduleId));
    }

    private double moduleCreditShare(String moduleId) {
        double credits = totalCredits();
        if (credits <= 0.0 || moduleId == null) {
            return 0.0;
        }
        return state.moduleById(moduleId)
                .map(module -> module.credits() / credits)
                .orElse(0.0);
    }

    private String calendarSemesterValue(double value) {
        if (gpaPointsMode.get()) {
            return PERCENT_FORMAT.format(value * 4.2);
        }
        if (Math.abs(value) >= 10.0) {
            return Math.round(value) + "%";
        }
        return percent(value);
    }

    private String calendarModuleValue(double value, AcademicModule module) {
        return calendarModuleValue(value, module == null ? null : module.id());
    }

    private String calendarModuleValue(double value, String moduleId) {
        if (gpaPointsMode.get()) {
            return PERCENT_FORMAT.format(value * 4.2 * moduleCreditShare(moduleId));
        }
        if (Math.abs(value) >= 10.0) {
            return Math.round(value) + "%";
        }
        return percent(value);
    }

    private Double currentPageScrollPosition() {
        return shell.getCenter() instanceof ScrollPane scrollPane ? scrollPane.getVvalue() : null;
    }

    private void addStyle(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private void applyTheme() {
        if (shell == null) {
            return;
        }
        shell.getStyleClass().remove("dark-theme");
        if (darkMode.get()) {
            shell.getStyleClass().add("dark-theme");
        }
    }

    private void changeCalendarZoom(double delta) {
        calendarZoom.set(Math.max(0.58, Math.min(1.10, calendarZoom.get() + delta)));
        if (activePage == HubPage.CALENDAR) {
            showPage(HubPage.CALENDAR);
        }
    }

    private void addHoverMotion(Node node, double hoverScale) {
        node.setOnMouseEntered(event -> animateScale(node, hoverScale));
        node.setOnMouseExited(event -> animateScale(node, 1.0));
    }

    private void animateScale(Node node, double scale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(140), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private static String scaleButtonText(GradeScale scale) {
        return switch (scale.id()) {
            case "standard40" -> "40 Std";
            case "linear40" -> "40 Lin";
            case "nonLinear50" -> "50 Std";
            default -> scale.displayName();
        };
    }

    private static int compareGrades(String left, String right) {
        List<String> order = List.of(
                "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "D-",
                "E+", "E", "E-", "F+", "F", "F-", "G+", "G", "G-", "NM", "ABS"
        );
        return Integer.compare(order.indexOf(left), order.indexOf(right));
    }

    private enum HubPage {
        DASHBOARD("Dashboard"),
        MODULES("Modules"),
        TASKS("Tasks"),
        CALENDAR("Calendar"),
        GPA("GPA Calculator");

        private final String title;

        HubPage(String title) {
            this.title = title;
        }

        String title() {
            return title;
        }
    }

    private record ModuleColour(String name, String hex) {
    }

    private record Calculation(String inputText, String grade, double gradePoint) {
    }

    private static final class InvalidNumber {
        private static final Double VALUE = Double.NaN;

        private InvalidNumber() {
        }
    }
}
