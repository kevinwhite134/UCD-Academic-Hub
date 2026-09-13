package ie.ucd.gpa;

record ModuleProgress(
        double totalWeight,
        double completedWeight,
        double securedGrade,
        double remainingWeight,
        double requiredToPass,
        String passStatus
) {
    boolean assessmentWeightsComplete() {
        return Math.abs(totalWeight - 100.0) < 0.01;
    }
}
