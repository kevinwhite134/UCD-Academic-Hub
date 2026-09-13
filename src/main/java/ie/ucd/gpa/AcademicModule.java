package ie.ucd.gpa;

import java.util.UUID;

final class AcademicModule {
    private final String id;
    private String name;
    private String moduleCode;
    private String description;
    private String colour;
    private double passGrade;
    private double credits;
    private String semester;
    private String academicYear;
    private int displayOrder;

    AcademicModule(
            String id,
            String name,
            String moduleCode,
            String description,
            String colour,
            double passGrade,
            double credits,
            String semester,
            String academicYear,
            int displayOrder
    ) {
        this.id = id;
        this.name = name;
        this.moduleCode = moduleCode;
        this.description = description;
        this.colour = colour;
        this.passGrade = passGrade;
        this.credits = credits;
        this.semester = semester;
        this.academicYear = academicYear;
        this.displayOrder = displayOrder;
    }

    static AcademicModule create(
            String name,
            String moduleCode,
            String description,
            String colour,
            double passGrade,
            double credits,
            String semester,
            String academicYear,
            int displayOrder
    ) {
        return new AcademicModule(
                UUID.randomUUID().toString(),
                name,
                moduleCode,
                description,
                colour,
                passGrade,
                credits,
                semester,
                academicYear,
                displayOrder
        );
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String moduleCode() {
        return moduleCode;
    }

    void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    String description() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    String colour() {
        return colour;
    }

    void setColour(String colour) {
        this.colour = colour;
    }

    double passGrade() {
        return passGrade;
    }

    void setPassGrade(double passGrade) {
        this.passGrade = passGrade;
    }

    double credits() {
        return credits;
    }

    void setCredits(double credits) {
        this.credits = credits;
    }

    String semester() {
        return semester;
    }

    void setSemester(String semester) {
        this.semester = semester;
    }

    String academicYear() {
        return academicYear;
    }

    void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    int displayOrder() {
        return displayOrder;
    }

    void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    String displayName() {
        if (moduleCode == null || moduleCode.isBlank()) {
            return name;
        }
        return moduleCode + " - " + name;
    }
}
