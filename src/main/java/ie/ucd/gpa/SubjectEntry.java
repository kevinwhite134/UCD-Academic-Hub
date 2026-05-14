package ie.ucd.gpa;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

final class SubjectEntry {
    private final int number;
    private final StringProperty scaleId = new SimpleStringProperty("standard40");
    private final StringProperty percentage = new SimpleStringProperty("");
    private final StringProperty letterBase = new SimpleStringProperty("A");
    private final StringProperty letterSuffix = new SimpleStringProperty("");

    SubjectEntry(int number) {
        this.number = number;
    }

    int number() {
        return number;
    }

    StringProperty scaleIdProperty() {
        return scaleId;
    }

    StringProperty percentageProperty() {
        return percentage;
    }

    StringProperty letterBaseProperty() {
        return letterBase;
    }

    StringProperty letterSuffixProperty() {
        return letterSuffix;
    }

    String selectedLetterGrade() {
        String base = letterBase.get();
        if ("NM".equals(base) || "ABS".equals(base)) {
            return base;
        }
        return base + letterSuffix.get();
    }
}
