package ie.ucd.gpa;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

final class AcademicCalculations {
    private AcademicCalculations() {
    }

    static ModuleProgress progressFor(AcademicModule module, List<AssessmentEntry> assessments) {
        double completedWeight = assessments.stream()
                .filter(AssessmentEntry::completed)
                .mapToDouble(AssessmentEntry::weight)
                .sum();
        double securedGrade = assessments.stream()
                .mapToDouble(AssessmentEntry::earnedContribution)
                .sum();
        double totalWeight = assessments.stream()
                .mapToDouble(AssessmentEntry::weight)
                .sum();
        double remainingWeight = Math.max(0.0, 100.0 - completedWeight);
        double requiredToPass = requiredAverage(securedGrade, remainingWeight, module.passGrade());
        String passStatus = passStatus(securedGrade, requiredToPass, module.passGrade());

        return new ModuleProgress(
                totalWeight,
                clamp(completedWeight, 0.0, 100.0),
                clamp(securedGrade, 0.0, 100.0),
                clamp(remainingWeight, 0.0, 100.0),
                requiredToPass,
                passStatus
        );
    }

    static double requiredAverage(double securedGrade, double remainingWeight, double targetGrade) {
        if (targetGrade <= securedGrade) {
            return 0.0;
        }
        if (remainingWeight <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return (targetGrade - securedGrade) / remainingWeight * 100.0;
    }

    static List<DeadlineItem> upcomingDeadlines(AcademicHubState state, int limit) {
        LocalDate today = LocalDate.now();
        return allDeadlines(state).stream()
                .filter(item -> item.dueDate() != null && !item.dueDate().isBefore(today))
                .sorted(Comparator.comparing(DeadlineItem::dueDate).thenComparing(DeadlineItem::title))
                .limit(limit)
                .toList();
    }

    static List<DeadlineItem> deadlinesForDate(AcademicHubState state, LocalDate date) {
        return allDeadlines(state).stream()
                .filter(item -> date.equals(item.dueDate()))
                .sorted(Comparator.comparing(DeadlineItem::type).thenComparing(DeadlineItem::title))
                .toList();
    }

    static OptionalDouble currentGpa(AcademicHubState state) {
        double weightedPoints = 0.0;
        double credits = 0.0;
        for (AcademicModule module : state.modules()) {
            ModuleProgress progress = progressFor(module, state.assessmentsFor(module.id()));
            if (progress.completedWeight() >= 99.99 && module.credits() > 0.0) {
                String grade = UcdGradeData.scaleById("linear40").gradeFor(progress.securedGrade());
                weightedPoints += UcdGradeData.gradePoint(grade) * module.credits();
                credits += module.credits();
            }
        }
        if (credits == 0.0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(weightedPoints / credits);
    }

    static double assessmentWeightTotal(AcademicHubState state, String moduleId) {
        return state.assessmentsFor(moduleId).stream()
                .mapToDouble(AssessmentEntry::weight)
                .sum();
    }

    private static List<DeadlineItem> allDeadlines(AcademicHubState state) {
        List<DeadlineItem> assessmentDeadlines = state.assessments().stream()
                .filter(assessment -> !assessment.completed())
                .map(assessment -> new DeadlineItem(
                        assessment.dueDate(),
                        assessment.name(),
                        "Assessment",
                        assessment.moduleId(),
                        assessment.url(),
                        ""
                ))
                .toList();
        List<DeadlineItem> taskDeadlines = state.tasks().stream()
                .filter(task -> !task.completed())
                .map(task -> new DeadlineItem(
                        task.dueDate(),
                        task.title(),
                        "Task",
                        task.moduleId(),
                        task.url(),
                        task.priority()
                ))
                .toList();
        return java.util.stream.Stream.concat(assessmentDeadlines.stream(), taskDeadlines.stream()).toList();
    }

    private static String passStatus(double securedGrade, double requiredToPass, double passGrade) {
        if (securedGrade >= passGrade) {
            return "Already Passed";
        }
        if (Double.isInfinite(requiredToPass) || requiredToPass > 100.0) {
            return "Mathematically Impossible";
        }
        if (requiredToPass <= 65.0) {
            return "On Track";
        }
        return "At Risk";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
