package ie.ucd.gpa;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public final class UcdGpaCalculatorApp extends Application {
    private static final DecimalFormat GPA_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat POINT_FORMAT = new DecimalFormat("0.0");

    private final List<SubjectEntry> subjects = new ArrayList<>();
    private final BooleanProperty percentageMode = new SimpleBooleanProperty(true);
    private final Label gpaLabel = new Label("GPA: -");
    private final GridPane resultsGrid = new GridPane();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("UCD GPA Calculator");
        stage.setMinWidth(980);
        stage.setMinHeight(720);
        stage.setScene(new Scene(buildSubjectCountScreen(stage), 1100, 760));
        stage.getScene().getStylesheets().add(getClass().getResource("/ie/ucd/gpa/styles.css").toExternalForm());
        stage.show();
    }

    private BorderPane buildSubjectCountScreen(Stage stage) {
        Label heading = new Label("How many subjects?");
        heading.getStyleClass().add("hero-title");

        Label subheading = new Label("Pick 1 to 10, then enter percentages or letter grades.");
        subheading.getStyleClass().add("muted");

        FlowPane choices = new FlowPane(22, 22);
        choices.setAlignment(Pos.CENTER);
        for (int i = 1; i <= 10; i++) {
            int count = i;
            Button button = circleButton(String.valueOf(i), 86);
            button.setOnAction(event -> showCalculator(stage, count));
            choices.getChildren().add(button);
        }

        VBox center = new VBox(18, heading, subheading, choices);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(38));

        BorderPane root = new BorderPane(center);
        root.getStyleClass().add("root-pane");
        return root;
    }

    private void showCalculator(Stage stage, int subjectCount) {
        subjects.clear();
        for (int i = 1; i <= subjectCount; i++) {
            subjects.add(new SubjectEntry(i));
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTopBar(stage));
        root.setCenter(buildScrollableContent());
        stage.getScene().setRoot(root);
        recalculate();
    }

    private HBox buildTopBar(Stage stage) {
        Button back = new Button("Change subjects");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> stage.getScene().setRoot(buildSubjectCountScreen(stage)));

        Label title = new Label("UCD GPA Calculator");
        title.getStyleClass().add("page-title");

        ToggleButton percentage = new ToggleButton("Percentages");
        ToggleButton letters = new ToggleButton("Letter grades");
        ToggleGroup modeGroup = new ToggleGroup();
        percentage.setToggleGroup(modeGroup);
        letters.setToggleGroup(modeGroup);
        percentage.setSelected(true);
        percentageMode.bind(percentage.selectedProperty());
        percentage.getStyleClass().add("mode-toggle");
        letters.getStyleClass().add("mode-toggle");

        modeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                oldValue.setSelected(true);
            }
            recalculate();
        });

        HBox modePicker = new HBox(0, percentage, letters);
        modePicker.getStyleClass().add("segmented");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        gpaLabel.getStyleClass().add("gpa-pill");
        HBox bar = new HBox(16, back, title, spacer, modePicker, gpaLabel);
        bar.setPadding(new Insets(18, 24, 14, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private ScrollPane buildScrollableContent() {
        VBox content = new VBox(22, buildSubjectEditor(), buildResultsSection(), buildReferenceSection());
        content.setPadding(new Insets(24));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scrollPane;
    }

    private FlowPane buildSubjectEditor() {
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

    private VBox buildResultsSection() {
        Label title = new Label("Calculated results");
        title.getStyleClass().add("section-title");

        resultsGrid.setHgap(16);
        resultsGrid.setVgap(9);
        resultsGrid.getStyleClass().add("results-grid");

        Button calculate = new Button("Calculate GPA");
        calculate.getStyleClass().add("primary-button");
        calculate.setOnAction(event -> recalculate());

        VBox box = new VBox(12, title, resultsGrid, calculate);
        box.getStyleClass().add("panel");
        return box;
    }

    private VBox buildReferenceSection() {
        Label title = new Label("UCD grade scales and grade points");
        title.getStyleClass().add("section-title");

        VBox scales = new VBox(14);
        for (GradeScale scale : UcdGradeData.SCALES) {
            scales.getChildren().add(scaleReference(scale));
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

        VBox box = new VBox(16, title, scales, new Separator(), points, source);
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
        } else {
            gpaLabel.setText("GPA: " + GPA_FORMAT.format(total / validSubjects));
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

    private record Calculation(String inputText, String grade, double gradePoint) {
    }
}
