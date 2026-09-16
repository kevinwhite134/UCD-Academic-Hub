# UCD Academic Hub

A JavaFX desktop application for UCD students to manage modules, assessment deadlines, academic tasks, and GPA calculations in one local dashboard.

The project started as a UCD GPA calculator and has grown into a fuller academic planning tool: students can track modules, weighted assessments, progress toward pass marks, upcoming deadlines, and UCD grade-point outcomes without needing an account or cloud service.

## Why I Built This

University grade tracking can become messy quickly: modules have different credit values, assessments carry different weights, and deadlines often live across several systems. I built UCD Academic Hub to turn that information into a focused desktop workflow that answers practical questions:

- What is my current GPA based on completed modules?
- Which assessments or tasks are due next?
- How much of each module have I completed?
- What grade do I need in the remaining work to pass or reach a target?
- Are my assessment weights valid and complete?

## Features

- **Academic dashboard** with current GPA, module pass count, course completion, and next deadline.
- **Module management** for module codes, descriptions, colours, credits, semesters, academic years, and pass marks.
- **Assessment tracking** with weights, due dates, completion status, grades, notes, and useful links.
- **Progress calculations** for completed weight, secured grade, remaining weight, pass status, and target-grade requirements.
- **Deadline calendar** showing assessments and academic tasks due on a selected date.
- **Task board** for module-linked work, priorities, due dates, completion status, and custom URL buttons.
- **UCD GPA calculator** supporting percentage and letter-grade entry using UCD grade scales.
- **Local autosave** so data persists between sessions without a server, account, or database setup.

## Technical Highlights

- Built with **Java 21**, **JavaFX 21**, and **Maven**.
- Uses a modular Java structure through `module-info.java`.
- Separates calculation logic from UI code so GPA, pass-status, deadline, and progress behaviour can be unit tested.
- Includes JUnit 5 tests for core academic calculations.
- Persists user data locally with Java `Properties`, keeping the app simple to run and easy to inspect.
- Packages a runnable JAR and Windows launch script in `dist/`.

## Tech Stack

| Area | Tools |
| --- | --- |
| Language | Java 21 |
| UI | JavaFX 21 |
| Build | Maven |
| Testing | JUnit 5 |
| Storage | Local properties file |

## Getting Started

### Prerequisites

- Java 21 or newer
- Maven 3.9 or newer

### Run from Source

```powershell
mvn javafx:run
```

You can also open the project in IntelliJ IDEA and run:

```text
ie.ucd.gpa.UcdGpaCalculatorApp
```

### Run the Packaged App

The `dist/` folder includes a packaged JAR and Windows launcher:

```text
dist/Run UCD GPA Calculator.bat
```

## Testing

Run the unit test suite with:

```powershell
mvn test
```

Current tests cover the calculation layer, including secured grades, required averages, impossible pass scenarios, and credit-weighted GPA.

## Local Data

Academic Hub saves data locally at:

```text
%USERPROFILE%\.ucd-academic-hub\academic-hub.properties
```

No user data is sent to a server. The app works fully offline after dependencies are installed.

## Project Structure

```text
src/main/java/ie/ucd/gpa/
  AcademicCalculations.java    Core GPA, progress, deadline, and pass-status logic
  AcademicDataStore.java       Local persistence
  AcademicHubState.java        In-memory module, assessment, and task state
  UcdGpaCalculatorApp.java     JavaFX application and screens

src/test/java/ie/ucd/gpa/
  AcademicCalculationsTest.java

src/main/resources/ie/ucd/gpa/
  styles.css
  ucd-logo.png
```

## UCD Grade Sources

The GPA calculator is based on UCD's published grade and conversion information:

- [UCD Registry Grades](https://www.ucd.ie/registry/staff/registryservices/assessment/gradingsupport/grades/)
- [Standard Conversion Grade Scale 40% Pass](https://www.ucd.ie/registry/t4media/Standard_Conversion_Grade_Scale_40_Pass.pdf)
- [Alternative Linear Conversion Grade Scale 40% Pass](https://www.ucd.ie/registry/t4media/Alternative%20Linear%20Conversion%20Grade%20Scale%2040%20per%20cent%20Pass.pdf)
- [Alternative Non-Linear Conversion Grade Scale 50% Pass](https://www.ucd.ie/registry/t4media/Alternative%20Non-Linear%20Conversion%20Grade%20Scale%2050%20percent.pdf)

## Future Improvements

- Add import/export for academic data.
- Add richer reporting for semester and year-level performance.
- Move persistence to SQLite if the app grows beyond local properties storage.
- Add screenshots or a short demo GIF for the GitHub project page.
