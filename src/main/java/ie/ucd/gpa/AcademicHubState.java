package ie.ucd.gpa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class AcademicHubState {
    private final List<AcademicModule> modules = new ArrayList<>();
    private final List<AssessmentEntry> assessments = new ArrayList<>();
    private final List<AcademicTask> tasks = new ArrayList<>();

    List<AcademicModule> modules() {
        return modules;
    }

    List<AssessmentEntry> assessments() {
        return assessments;
    }

    List<AcademicTask> tasks() {
        return tasks;
    }

    List<AcademicModule> orderedModules() {
        return modules.stream()
                .sorted(Comparator.comparingInt(AcademicModule::displayOrder).thenComparing(AcademicModule::moduleCode))
                .toList();
    }

    List<AssessmentEntry> assessmentsFor(String moduleId) {
        return assessments.stream()
                .filter(assessment -> assessment.moduleId().equals(moduleId))
                .sorted(Comparator.comparing(
                        AssessmentEntry::dueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparing(AssessmentEntry::name))
                .toList();
    }

    List<AcademicTask> orderedTasks() {
        return tasks.stream()
                .sorted(Comparator.comparingInt(AcademicTask::displayOrder).thenComparing(AcademicTask::title))
                .toList();
    }

    List<AcademicTask> tasksFor(String moduleId) {
        return orderedTasks().stream()
                .filter(task -> task.moduleId().equals(moduleId))
                .toList();
    }

    Optional<AcademicModule> moduleById(String moduleId) {
        return modules.stream()
                .filter(module -> module.id().equals(moduleId))
                .findFirst();
    }

    void removeModule(AcademicModule module) {
        modules.remove(module);
        assessments.removeIf(assessment -> assessment.moduleId().equals(module.id()));
        tasks.removeIf(task -> task.moduleId().equals(module.id()));
        renumberModules();
        renumberTasks();
    }

    void renumberModules() {
        List<AcademicModule> ordered = orderedModules();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setDisplayOrder(i);
        }
    }

    void renumberTasks() {
        List<AcademicTask> ordered = orderedTasks();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setDisplayOrder(i);
        }
    }

    int nextModuleOrder() {
        return modules.stream()
                .mapToInt(AcademicModule::displayOrder)
                .max()
                .orElse(-1) + 1;
    }

    int nextTaskOrder() {
        return tasks.stream()
                .mapToInt(AcademicTask::displayOrder)
                .max()
                .orElse(-1) + 1;
    }
}
