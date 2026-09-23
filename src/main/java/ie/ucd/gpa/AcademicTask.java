package ie.ucd.gpa;

import java.time.LocalDate;
import java.util.UUID;

final class AcademicTask {
    private final String id;
    private String moduleId;
    private String title;
    private String description;
    private String priority;
    private LocalDate dueDate;
    private boolean completed;
    private String url;
    private String buttonText;
    private int displayOrder;
    private Integer termWeek;

    AcademicTask(
            String id,
            String moduleId,
            String title,
            String description,
            String priority,
            LocalDate dueDate,
            boolean completed,
            String url,
            String buttonText,
            int displayOrder,
            Integer termWeek
    ) {
        this.id = id;
        this.moduleId = moduleId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = completed;
        this.url = url;
        this.buttonText = buttonText;
        this.displayOrder = displayOrder;
        this.termWeek = termWeek;
    }

    static AcademicTask create(
            String moduleId,
            String title,
            String description,
            String priority,
            LocalDate dueDate,
            boolean completed,
            String url,
            String buttonText,
            int displayOrder
    ) {
        return create(moduleId, title, description, priority, dueDate, completed, url, buttonText, displayOrder, null);
    }

    static AcademicTask create(
            String moduleId,
            String title,
            String description,
            String priority,
            LocalDate dueDate,
            boolean completed,
            String url,
            String buttonText,
            int displayOrder,
            Integer termWeek
    ) {
        return new AcademicTask(
                UUID.randomUUID().toString(),
                moduleId,
                title,
                description,
                priority,
                dueDate,
                completed,
                url,
                buttonText,
                displayOrder,
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

    String title() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String description() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String priority() {
        return priority;
    }

    void setPriority(String priority) {
        this.priority = priority;
    }

    LocalDate dueDate() {
        return dueDate;
    }

    void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    boolean completed() {
        return completed;
    }

    void setCompleted(boolean completed) {
        this.completed = completed;
    }

    String url() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    String buttonText() {
        return buttonText;
    }

    void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    int displayOrder() {
        return displayOrder;
    }

    void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    Integer termWeek() {
        return termWeek;
    }

    void setTermWeek(Integer termWeek) {
        this.termWeek = termWeek;
    }
}
