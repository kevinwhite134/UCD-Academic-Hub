package ie.ucd.gpa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;

final class AcademicDataStore {
    private final Path dataFile;

    AcademicDataStore() {
        this(Path.of(System.getProperty("user.home"), ".ucd-academic-hub", "academic-hub.properties"));
    }

    AcademicDataStore(Path dataFile) {
        this.dataFile = dataFile;
    }

    Path dataFile() {
        return dataFile;
    }

    AcademicHubState load() throws IOException {
        AcademicHubState state = new AcademicHubState();
        if (!Files.exists(dataFile)) {
            return state;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(dataFile)) {
            properties.load(input);
        }

        int moduleCount = intProperty(properties, "module.count", 0);
        for (int i = 0; i < moduleCount; i++) {
            String prefix = "module." + i + ".";
            state.modules().add(new AcademicModule(
                    property(properties, prefix + "id", ""),
                    property(properties, prefix + "name", ""),
                    property(properties, prefix + "module_code", ""),
                    property(properties, prefix + "description", ""),
                    property(properties, prefix + "colour", "#2f80ed"),
                    doubleProperty(properties, prefix + "pass_grade", 40.0),
                    doubleProperty(properties, prefix + "credits", 5.0),
                    property(properties, prefix + "semester", ""),
                    property(properties, prefix + "academic_year", ""),
                    intProperty(properties, prefix + "display_order", i)
            ));
        }

        int assessmentCount = intProperty(properties, "assessment.count", 0);
        for (int i = 0; i < assessmentCount; i++) {
            String prefix = "assessment." + i + ".";
            state.assessments().add(new AssessmentEntry(
                    property(properties, prefix + "id", ""),
                    property(properties, prefix + "module_id", ""),
                    property(properties, prefix + "name", ""),
                    doubleProperty(properties, prefix + "weight", 0.0),
                    optionalDoubleProperty(properties, prefix + "grade"),
                    booleanProperty(properties, prefix + "completed", false),
                    dateProperty(properties, prefix + "due_date"),
                    property(properties, prefix + "notes", ""),
                    property(properties, prefix + "url", ""),
                    booleanProperty(properties, prefix + "final_exam", false),
                    optionalIntProperty(properties, prefix + "term_week")
            ));
        }

        int taskCount = intProperty(properties, "task.count", 0);
        for (int i = 0; i < taskCount; i++) {
            String prefix = "task." + i + ".";
            state.tasks().add(new AcademicTask(
                    property(properties, prefix + "id", ""),
                    property(properties, prefix + "module_id", ""),
                    property(properties, prefix + "title", ""),
                    property(properties, prefix + "description", ""),
                    property(properties, prefix + "priority", "Normal"),
                    dateProperty(properties, prefix + "due_date"),
                    booleanProperty(properties, prefix + "completed", false),
                    property(properties, prefix + "url", ""),
                    property(properties, prefix + "button_text", "Open"),
                    intProperty(properties, prefix + "display_order", i),
                    optionalIntProperty(properties, prefix + "term_week")
            ));
        }

        state.renumberModules();
        state.renumberTasks();
        return state;
    }

    void save(AcademicHubState state) throws IOException {
        Files.createDirectories(dataFile.getParent());
        Properties properties = new Properties();

        properties.setProperty("module.count", String.valueOf(state.modules().size()));
        for (int i = 0; i < state.modules().size(); i++) {
            AcademicModule module = state.modules().get(i);
            String prefix = "module." + i + ".";
            put(properties, prefix + "id", module.id());
            put(properties, prefix + "name", module.name());
            put(properties, prefix + "module_code", module.moduleCode());
            put(properties, prefix + "description", module.description());
            put(properties, prefix + "colour", module.colour());
            put(properties, prefix + "pass_grade", module.passGrade());
            put(properties, prefix + "credits", module.credits());
            put(properties, prefix + "semester", module.semester());
            put(properties, prefix + "academic_year", module.academicYear());
            put(properties, prefix + "display_order", module.displayOrder());
        }

        properties.setProperty("assessment.count", String.valueOf(state.assessments().size()));
        for (int i = 0; i < state.assessments().size(); i++) {
            AssessmentEntry assessment = state.assessments().get(i);
            String prefix = "assessment." + i + ".";
            put(properties, prefix + "id", assessment.id());
            put(properties, prefix + "module_id", assessment.moduleId());
            put(properties, prefix + "name", assessment.name());
            put(properties, prefix + "weight", assessment.weight());
            put(properties, prefix + "grade", assessment.grade());
            put(properties, prefix + "completed", assessment.completed());
            put(properties, prefix + "due_date", assessment.dueDate());
            put(properties, prefix + "notes", assessment.notes());
            put(properties, prefix + "url", assessment.url());
            put(properties, prefix + "final_exam", assessment.finalExam());
            put(properties, prefix + "term_week", assessment.termWeek());
        }

        properties.setProperty("task.count", String.valueOf(state.tasks().size()));
        for (int i = 0; i < state.tasks().size(); i++) {
            AcademicTask task = state.tasks().get(i);
            String prefix = "task." + i + ".";
            put(properties, prefix + "id", task.id());
            put(properties, prefix + "module_id", task.moduleId());
            put(properties, prefix + "title", task.title());
            put(properties, prefix + "description", task.description());
            put(properties, prefix + "priority", task.priority());
            put(properties, prefix + "due_date", task.dueDate());
            put(properties, prefix + "completed", task.completed());
            put(properties, prefix + "url", task.url());
            put(properties, prefix + "button_text", task.buttonText());
            put(properties, prefix + "display_order", task.displayOrder());
            put(properties, prefix + "term_week", task.termWeek());
        }

        try (OutputStream output = Files.newOutputStream(dataFile)) {
            properties.store(output, "UCD Academic Hub local data");
        }
    }

    private static void put(Properties properties, String key, Object value) {
        properties.setProperty(key, value == null ? "" : String.valueOf(value));
    }

    private static String property(Properties properties, String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).trim();
    }

    private static int intProperty(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(property(properties, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static double doubleProperty(Properties properties, String key, double defaultValue) {
        try {
            return Double.parseDouble(property(properties, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Double optionalDoubleProperty(Properties properties, String key) {
        String value = property(properties, key, "");
        if (value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer optionalIntProperty(Properties properties, String key) {
        String value = property(properties, key, "");
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean booleanProperty(Properties properties, String key, boolean defaultValue) {
        return Boolean.parseBoolean(property(properties, key, String.valueOf(defaultValue)));
    }

    private static LocalDate dateProperty(Properties properties, String key) {
        String value = property(properties, key, "");
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
