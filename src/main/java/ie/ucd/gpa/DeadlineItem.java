package ie.ucd.gpa;

import java.time.LocalDate;

record DeadlineItem(
        LocalDate dueDate,
        String title,
        String type,
        String moduleId,
        String url,
        String priority
) {
}
