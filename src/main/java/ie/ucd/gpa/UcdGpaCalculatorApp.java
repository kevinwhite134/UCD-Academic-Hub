package ie.ucd.gpa;

import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import javafx.scene.layout.ColumnConstraints;
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
import javafx.util.StringConverter;

public final class UcdGpaCalculatorApp extends Application {
    static final DecimalFormat GPA_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat POINT_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.#");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final List<String> PRIORITIES = List.of("Critical", "High", "Normal", "Low");
    private static final double THREE_MODULE_COLUMN_BREAKPOINT = 1450.0;
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
    private final Label gpaLabel = new Label("GPA: -");
    private final Label classificationLabel = new Label("Classification: -");
    private final GridPane resultsGrid = new GridPane();
    private final AcademicDataStore dataStore = new AcademicDataStore();

    private AcademicHubState state = new AcademicHubState();
    private BorderPane shell;
    private HubPage activePage = HubPage.DASHBOARD;
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
        shell.setLeft(buildSidebar());
        shell.setCenter(buildDashboardPage());

        Scene scene = new Scene(shell, 1240, 820);
        scene.getStylesheets().add(getClass().getResource("/ie/ucd/gpa/styles.css").toExternalForm());

        stage.setTitle("UCD Academic Hub");
        stage.setMinWidth(1060);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    private void showPage(HubPage page) {
        activePage = page;
        if (page != HubPage.MODULES) {
            editingModule = null;
            editingAssessment = null;
        }
        if (page != HubPage.TASKS) {
            editingTask = null;
        }

        shell.setLeft(buildSidebar());
        shell.setCenter(switch (page) {
            case DASHBOARD -> buildDashboardPage();
            case MODULES -> buildModulesPage();
            case TASKS -> buildTasksPage();
            case CALENDAR -> buildCalendarPage();
            case GPA -> buildGpaLandingPage();
        });
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

        VBox nav = new VBox(8);
        for (HubPage page : HubPage.values()) {
            nav.getChildren().add(navButton(page));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label storage = new Label("Autosaves locally\n" + dataStore.dataFile());
        storage.getStyleClass().add("storage-note");
        storage.setWrapText(true);

        VBox sidebar = new VBox(22, brand, nav, spacer, storage);
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
                buildDashboardModulesSection(),
                lowerPanels
        );
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
        if (state.modules().isEmpty()) {
            return OptionalDouble.empty();
        }
        double total = state.modules().stream()
                .mapToDouble(module -> AcademicCalculations.progressFor(module, state.assessmentsFor(module.id())).completedWeight())
                .sum();
        return OptionalDouble.of(total / state.modules().size());
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
        return card;
    }

    private VBox metricRingCard(String labelText, OptionalDouble percentage, String detailText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("metric-label");
        Label detail = new Label(detailText);
        detail.getStyleClass().add("muted");
        detail.setWrapText(true);

        VBox card = new VBox(7, label, percentageRing(percentage.orElse(0.0), "#007aff", "Complete", 62), detail);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(190);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox percentageRing(double value, String colour, String labelText, double size) {
        double clamped = Math.max(0.0, Math.min(100.0, value));
        double strokeWidth = Math.max(3.0, size * 0.065);
        double inset = strokeWidth / 2.0 + 1.5;
        double diameter = size - inset * 2.0;

        Canvas graphic = new Canvas(size, size);
        GraphicsContext context = graphic.getGraphicsContext2D();
        context.setLineWidth(strokeWidth);
        context.setLineCap(StrokeLineCap.ROUND);
        context.setStroke(Color.rgb(21, 50, 66, 0.10));
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
        context.setFill(Color.web("#162b39"));
        context.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(11.0, size * 0.18)));
        context.fillText(percent(clamped), size / 2.0, size / 2.0);

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

    private VBox buildDashboardAssessmentChecklist(AcademicModule module) {
        Label title = new Label("Assignments");
        title.getStyleClass().add("small-label");

        VBox rows = new VBox(5);
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

        VBox box = new VBox(6, title, rows);
        box.getStyleClass().add("dashboard-assessments");
        return box;
    }

    private VBox dashboardAssessmentRow(AssessmentEntry assessment) {
        CheckBox completed = new CheckBox();
        completed.setSelected(assessment.completed());

        Label name = new Label(assessment.name());
        name.getStyleClass().add("assessment-title");
        name.setWrapText(true);

        Label detail = new Label(percent(assessment.weight()) + " weight | due " + formatDate(assessment.dueDate()));
        detail.getStyleClass().add("muted");
        detail.setWrapText(true);

        Label gradeLabel = new Label("Grade");
        gradeLabel.getStyleClass().add("small-label");
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

        completed.setOnAction(event -> {
            assessment.setCompleted(completed.isSelected());
            if (completed.isSelected()) {
                persistOnly();
                gradeRow.setVisible(true);
                gradeRow.setManaged(true);
                grade.requestFocus();
            } else {
                persistAndRefresh(HubPage.DASHBOARD);
            }
        });

        grade.setOnAction(event -> saveDashboardAssessmentGrade(assessment, grade));
        grade.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                saveDashboardAssessmentGrade(assessment, grade);
            }
        });

        Button open = smallButton("Open");
        open.setDisable(assessment.url() == null || assessment.url().isBlank());
        open.setOnAction(event -> openUrl(assessment.url()));

        HBox topRow = new HBox(8, completed, new VBox(2, name, detail), spacer(), open);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox row = new VBox(4, topRow, gradeRow);
        row.getStyleClass().add("dashboard-assessment-row");
        return row;
    }

    private void saveDashboardAssessmentGrade(AssessmentEntry assessment, TextField gradeField) {
        Double grade = parseOptionalPercent(gradeField, "Assessment grade");
        if (grade == InvalidNumber.VALUE) {
            gradeField.setText(assessment.grade() == null ? "" : PERCENT_FORMAT.format(assessment.grade()));
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

        GridPane moduleCards = new GridPane();
        moduleCards.setHgap(14);
        moduleCards.setVgap(14);
        List<VBox> cards = new ArrayList<>();
        for (AcademicModule module : state.orderedModules()) {
            cards.add(buildModuleCard(module, true));
        }
        int[] currentColumnCount = {0};
        moduleCards.widthProperty().addListener((observable, oldWidth, newWidth) -> {
            int columnCount = newWidth.doubleValue() >= THREE_MODULE_COLUMN_BREAKPOINT ? 3 : 2;
            if (columnCount != currentColumnCount[0]) {
                layoutModuleCards(moduleCards, cards, columnCount);
                currentColumnCount[0] = columnCount;
            }
        });
        layoutModuleCards(moduleCards, cards, 2);
        currentColumnCount[0] = 2;

        VBox section = new VBox(12, title, moduleCards);
        section.getStyleClass().add("section-block");
        return section;
    }

    private void layoutModuleCards(GridPane grid, List<VBox> cards, int columnCount) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();

        for (int column = 0; column < columnCount; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / columnCount);
            constraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(constraints);
        }

        for (int index = 0; index < cards.size(); index++) {
            VBox card = cards.get(index);
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setFillWidth(card, true);
            grid.add(card, index % columnCount, index / columnCount);
        }
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

        HBox header = new HBox(8, new VBox(2, code, name), spacer(), status);
        header.setAlignment(Pos.TOP_LEFT);

        HBox rings = new HBox(8,
                percentageRing(progress.completedWeight(), module.colour(), "Complete", 58),
                percentageRing(progress.securedGrade(), module.colour(), "Secured", 58),
                percentageRing(module.passGrade(), module.colour(), "Pass", 58)
        );
        rings.getStyleClass().add("ring-row");

        Label nextAssessment = new Label(nextAssessmentText(module));
        nextAssessment.getStyleClass().add("next-deadline");
        nextAssessment.setWrapText(true);

        Label weightStatus = new Label("Assessment weights: " + percent(progress.totalWeight()));
        weightStatus.getStyleClass().add(progress.assessmentWeightsComplete() ? "ok-text" : "warning-text");

        VBox card = new VBox(7,
                moduleColourBar(module.colour()),
                header,
                rings,
                buildTargetCalculator(progress),
                buildDashboardAssessmentChecklist(module),
                nextAssessment,
                new HBox(8, weightStatus, spacer(), credits)
        );
        card.getStyleClass().add("module-card");
        card.setPrefWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(0);
        card.setStyle(moduleCardStyle(module.colour()));

        if (showActions) {
            Button open = new Button("Open Module");
            open.getStyleClass().add("secondary-button");
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
        targetField.setPrefWidth(70);
        Label suffix = new Label("%");
        suffix.getStyleClass().add("small-label");

        VBox result = new VBox();
        result.getStyleClass().add("target-result");

        Runnable update = () -> updateTargetResult(progress, targetField, result);
        targetField.textProperty().addListener((observable, oldValue, newValue) -> update.run());

        HBox inputRow = new HBox(8, targetField, suffix);
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
                HBox row = new HBox(8,
                        percentageRing(required, "#007aff", "Need", 50),
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
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(editingAssessment != null && editingAssessment.completed());
        TextField grade = textField("75", editingAssessment == null || editingAssessment.grade() == null ? "" : PERCENT_FORMAT.format(editingAssessment.grade()));
        TextArea notes = textArea("Optional notes", editingAssessment == null ? "" : editingAssessment.notes());
        TextField url = textField("https://...", editingAssessment == null ? "" : editingAssessment.url());

        GridPane form = formGrid();
        addFormRow(form, 0, "Module", module);
        addFormRow(form, 1, "Name", name);
        addFormRow(form, 2, "Weight", weight);
        addFormRow(form, 3, "Due Date", dueDate);
        addFormRow(form, 4, "Status", completed);
        addFormRow(form, 5, "Grade", grade);
        addFormRow(form, 6, "Notes", notes);
        addFormRow(form, 7, "URL", url);

        Button save = new Button(editingAssessment == null ? "Add Assessment" : "Update Assessment");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveAssessment(module, name, weight, dueDate, completed, grade, notes, url));

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
            DatePicker dueDateField,
            CheckBox completedField,
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

        if (editingAssessment == null) {
            state.assessments().add(AssessmentEntry.create(
                    moduleId,
                    name,
                    weight,
                    grade,
                    completedField.isSelected(),
                    dueDateField.getValue(),
                    notesField.getText().trim(),
                    urlField.getText().trim()
            ));
        } else {
            editingAssessment.setModuleId(moduleId);
            editingAssessment.setName(name);
            editingAssessment.setWeight(weight);
            editingAssessment.setGrade(grade);
            editingAssessment.setCompleted(completedField.isSelected());
            editingAssessment.setDueDate(dueDateField.getValue());
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
        Label weight = new Label("Total weight: " + percent(totalWeight));
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
                percent(assessment.weight()) + " weight"
                        + " | " + (assessment.completed() ? "Completed" : "Remaining")
                        + " | Grade: " + (assessment.grade() == null ? "-" : percent(assessment.grade()))
                        + " | Earned: " + percent(assessment.earnedContribution())
        );
        details.getStyleClass().add("muted");
        Label date = new Label(formatDate(assessment.dueDate()));
        date.getStyleClass().add("date-pill");

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

        HBox row = new HBox(12, new VBox(4, name, details), spacer(), date, open, edit, delete);
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
        CheckBox completed = new CheckBox("Completed");
        completed.setSelected(editing && editingTask.completed());
        TextField url = textField("https://...", editing ? editingTask.url() : "");
        TextField buttonText = textField("Open Brightspace", editing ? editingTask.buttonText() : "Open");

        GridPane form = formGrid();
        addFormRow(form, 0, "Module", module);
        addFormRow(form, 1, "Title", taskTitle);
        addFormRow(form, 2, "Description", description);
        addFormRow(form, 3, "Priority", priority);
        addFormRow(form, 4, "Due Date", dueDate);
        addFormRow(form, 5, "Status", completed);
        addFormRow(form, 6, "URL", url);
        addFormRow(form, 7, "Button Label", buttonText);

        Button save = new Button(editing ? "Update Task" : "Add Task");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveTask(module, taskTitle, description, priority, dueDate, completed, url, buttonText));

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
            DatePicker dueDateField,
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

        if (editingTask == null) {
            state.tasks().add(AcademicTask.create(
                    moduleId,
                    title,
                    descriptionField.getText().trim(),
                    priority,
                    dueDateField.getValue(),
                    completedField.isSelected(),
                    urlField.getText().trim(),
                    buttonText,
                    state.nextTaskOrder()
            ));
        } else {
            editingTask.setModuleId(moduleId);
            editingTask.setTitle(title);
            editingTask.setDescription(descriptionField.getText().trim());
            editingTask.setPriority(priority);
            editingTask.setDueDate(dueDateField.getValue());
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
        DatePicker datePicker = new DatePicker(LocalDate.now());
        VBox selectedDateItems = new VBox(10);
        Runnable refreshDateItems = () -> selectedDateItems.getChildren().setAll(calendarItemsFor(datePicker.getValue()));
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> refreshDateItems.run());
        refreshDateItems.run();

        Label selectedTitle = new Label("Selected Date");
        selectedTitle.getStyleClass().add("section-title");
        VBox selectedPanel = new VBox(12, selectedTitle, datePicker, selectedDateItems);
        selectedPanel.getStyleClass().add("panel");

        return page(
                "Calendar",
                "Assessment and task deadlines by date",
                selectedPanel,
                buildUpcomingDeadlinesPanel(20)
        );
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
            resultsGrid.add(new Label("Subject " + subject.number()), 0, row);
            resultsGrid.add(new Label(calculation.inputText()), 1, row);
            resultsGrid.add(new Label(calculation.grade()), 2, row);
            resultsGrid.add(new Label(POINT_FORMAT.format(calculation.gradePoint())), 3, row++);
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
        return scrollPane;
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
        return "-fx-background-color: " + mixWithWhite(colour, 0.88) + ";"
                + "-fx-border-color: " + mixWithWhite(colour, 0.66) + ";";
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
        return button;
    }

    private Button circleButton(String text, double size) {
        Button button = new Button(text);
        button.setMinSize(size, size);
        button.setPrefSize(size, size);
        button.setMaxSize(size, size);
        button.getStyleClass().add("circle-button");
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
        state.renumberModules();
        state.renumberTasks();
        try {
            dataStore.save(state);
            showPage(page);
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

    private void addStyle(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
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
