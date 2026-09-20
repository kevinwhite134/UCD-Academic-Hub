package ie.ucd.gpa;

import java.time.LocalDate;
import java.util.UUID;

final class AssessmentEntry {
    private final String id;
    private String moduleId;
    private String name;
    private double weight;
    private Double grade;
    private boolean completed;
    private LocalDate dueDate;
    private String notes;
    private String url;
    private boolean finalExam;
    private Integer termWeek;

    AssessmentEntry(
            String id,
            String moduleId,
            String name,
            double weight,
            Double grade,
            boolean completed,
            LocalDate dueDate,
            String notes,
            String url,
            boolean finalExam,
            Integer termWeek
    ) {
        this.id = id;
        this.moduleId = moduleId;
        this.name = name;
        this.weight = weight;
        this.grade = grade;
        this.completed = completed;
        this.dueDate = dueDate;
        this.notes = notes;
        this.url = url;
        this.finalExam = finalExam;
        this.termWeek = termWeek;
    }

    static AssessmentEntry create(
            String moduleId,
            String name,
            double weight,
            Double grade,
            boolean completed,
            LocalDate dueDate,
            String notes,
            String url
    ) {
        return create(moduleId, name, weight, grade, completed, dueDate, notes, url, false, null);
    }

    static AssessmentEntry create(
            String moduleId,
            String name,
            double weight,
            Double grade,
            boolean completed,
            LocalDate dueDate,
            String notes,
            String url,
            boolean finalExam
    ) {
        return create(moduleId, name, weight, grade, completed, dueDate, notes, url, finalExam, null);
    }

    static AssessmentEntry create(
            String moduleId,
            String name,
            double weight,
            Double grade,
            boolean completed,
            LocalDate dueDate,
            String notes,
            String url,
            boolean finalExam,
            Integer termWeek
    ) {
        return new AssessmentEntry(
                UUID.randomUUID().toString(),
                moduleId,
                name,
                weight,
                grade,
                completed,
                dueDate,
                notes,
                url,
                finalExam,
                termWeek
        );
    }

    String id() {
        return id;
    }

    String moduleId() {
        return moduleId;
    }

    void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    String name() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    double weight() {
        return weight;
    }

    void setWeight(double weight) {
        this.weight = weight;
    }

    Double grade() {
        return grade;
    }

    void setGrade(Double grade) {
        this.grade = grade;
    }

    boolean completed() {
        return completed;
    }

    void setCompleted(boolean completed) {
        this.completed = completed;
    }

    LocalDate dueDate() {
        return dueDate;
    }

    void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    String notes() {
        return notes;
    }

    void setNotes(String notes) {
        this.notes = notes;
    }

    String url() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    boolean finalExam() {
        return finalExam;
    }

    void setFinalExam(boolean finalExam) {
        this.finalExam = finalExam;
    }

    Integer termWeek() {
        return termWeek;
    }

    void setTermWeek(Integer termWeek) {
        this.termWeek = termWeek;
    }

    double earnedContribution() {
        if (!completed || grade == null) {
            return 0.0;
        }
        return weight * grade / 100.0;
    }
}
