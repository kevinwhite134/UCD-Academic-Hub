package ie.ucd.gpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

final class AcademicCalculationsTest {
    @Test
    void calculatesSecuredGradeAndCompletionSeparately() {
        AcademicModule module = module("Software Engineering", 40.0, 5.0);
        List<AssessmentEntry> assessments = List.of(
                assessment(module, "Assignment", 50.0, 80.0, true),
                assessment(module, "Exam", 50.0, null, false)
        );

        ModuleProgress progress = AcademicCalculations.progressFor(module, assessments);

        assertEquals(50.0, progress.completedWeight(), 0.001);
        assertEquals(40.0, progress.securedGrade(), 0.001);
        assertEquals("Already Passed", progress.passStatus());
    }

    @Test
    void calculatesRequiredAverageForTargetGrade() {
        double required = AcademicCalculations.requiredAverage(30.0, 50.0, 60.0);

        assertEquals(60.0, required, 0.001);
    }

    @Test
    void marksImpossiblePassWhenRemainingWeightCannotReachPassGrade() {
        AcademicModule module = module("Networks", 60.0, 5.0);
        List<AssessmentEntry> assessments = List.of(
                assessment(module, "Project", 80.0, 25.0, true),
                assessment(module, "Quiz", 20.0, null, false)
        );

        ModuleProgress progress = AcademicCalculations.progressFor(module, assessments);

        assertEquals(20.0, progress.securedGrade(), 0.001);
        assertEquals("Mathematically Impossible", progress.passStatus());
        assertTrue(AcademicCalculations.requiredAverage(progress.securedGrade(), progress.remainingWeight(), 60.0) > 100.0);
    }

    @Test
    void calculatesCreditWeightedGpaForCompletedModules() {
        AcademicHubState state = new AcademicHubState();
        AcademicModule first = module("Algorithms", 40.0, 5.0);
        AcademicModule second = module("Databases", 40.0, 5.0);
        state.modules().add(first);
        state.modules().add(second);
        state.assessments().add(assessment(first, "Final", 100.0, 80.0, true));
        state.assessments().add(assessment(second, "Final", 100.0, 60.0, true));

        OptionalDouble gpa = AcademicCalculations.currentGpa(state);

        assertTrue(gpa.isPresent());
        assertEquals(3.2, gpa.getAsDouble(), 0.001);
    }

    @Test
    void keepsCompletedTasksOnCalendarDateButOutOfUpcomingDeadlines() {
        AcademicHubState state = new AcademicHubState();
        AcademicModule module = module("Compilers", 40.0, 5.0);
        LocalDate dueDate = LocalDate.now().plusDays(2);
        state.modules().add(module);
        state.tasks().add(task(module, "Submit parser", dueDate, true));
        state.tasks().add(task(module, "Read lexer notes", dueDate, false));

        List<DeadlineItem> dateItems = AcademicCalculations.deadlinesForDate(state, dueDate);
        List<DeadlineItem> upcomingItems = AcademicCalculations.upcomingDeadlines(state, 10);

        assertEquals(2, dateItems.size());
        assertTrue(dateItems.stream().anyMatch(item -> item.title().equals("Submit parser") && item.completed()));
        assertEquals(1, upcomingItems.size());
        assertEquals("Read lexer notes", upcomingItems.getFirst().title());
    }

    private static AcademicModule module(String name, double passGrade, double credits) {
        return AcademicModule.create(name, name.substring(0, 4).toUpperCase(), "", "#2f80ed", passGrade, credits, "Autumn", "2026/27", 0);
    }

    private static AssessmentEntry assessment(AcademicModule module, String name, double weight, Double grade, boolean completed) {
        return AssessmentEntry.create(module.id(), name, weight, grade, completed, LocalDate.of(2026, 9, 18), "", "");
    }

    private static AcademicTask task(AcademicModule module, String title, LocalDate dueDate, boolean completed) {
        return AcademicTask.create(module.id(), title, "", "Medium", dueDate, completed, "", "", 0);
    }
}
