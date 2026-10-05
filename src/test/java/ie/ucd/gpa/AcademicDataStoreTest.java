package ie.ucd.gpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AcademicDataStoreTest {
    @TempDir
    Path directory;

    @Test
    void defaultsNewModulesToLinear40() {
        assertEquals("linear40", module().gradeScaleId());
        assertEquals("linear40", UcdGradeData.SCALES.getFirst().id());
    }

    @Test
    void preservesSelectedScaleAndAssessmentScoresAcrossReloads() throws IOException {
        AcademicDataStore store = new AcademicDataStore(directory.resolve("hub.properties"));
        AcademicHubState state = new AcademicHubState();
        AcademicModule module = module();
        module.setGradeScaleId("standard40");
        state.modules().add(module);
        state.assessments().add(AssessmentEntry.create(module.id(), "Exam", 100.0, 80.0, true, null, "", ""));

        store.save(state);
        AcademicHubState loaded = store.load();

        assertEquals(module.id(), loaded.modules().getFirst().id());
        assertEquals("standard40", loaded.modules().getFirst().gradeScaleId());
        assertEquals(80.0, loaded.assessments().getFirst().grade());
        assertEquals(4.0, AcademicCalculations.currentGpa(loaded).orElseThrow(), 0.001);
    }

    @Test
    void loadsExistingModulesWithoutScaleAsLinear40() throws IOException {
        AcademicDataStore store = new AcademicDataStore(directory.resolve("hub.properties"));
        AcademicHubState state = new AcademicHubState();
        AcademicModule module = module();
        state.modules().add(module);
        store.save(state);

        Properties legacy = new Properties();
        try (InputStream input = Files.newInputStream(store.dataFile())) {
            legacy.load(input);
        }
        legacy.remove("module.0.grade_scale");
        try (OutputStream output = Files.newOutputStream(store.dataFile())) {
            legacy.store(output, "Existing modules without a grade scale");
        }

        AcademicModule loaded = store.load().modules().getFirst();
        assertEquals(module.id(), loaded.id());
        assertEquals("linear40", loaded.gradeScaleId());
        assertEquals(40.0, loaded.passGrade());
        assertEquals("A", UcdGradeData.scaleById(loaded.gradeScaleId()).gradeFor(92.0));
    }

    private static AcademicModule module() {
        return AcademicModule.create("Algorithms", "COMP", "", "#2f80ed", 40.0, 5.0, "Autumn", "2026/27", 0);
    }
}
