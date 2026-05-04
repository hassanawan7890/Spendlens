# PROJECT STRUCTURE for SpendLens

## File and Folder Structure
The SpendLens directory contains the following main folders and files:

```
SpendLens/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aryan0744/spendlens/
│   │   │   │   ├── models/
│   │   │   │   ├── views/
│   │   │   │   └── controllers/
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── mipmap/
│   │   │   ├── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle
│   └── proguard-rules.pro
│
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
└── README.md
```

## Package Organization
- **com.aryan0744.spendlens**: This is the main package containing the application logic.
  - **models**: Contains all data models.
  - **views**: Contains UI components and layouts.
  - **controllers**: Contains the logic that connects models and views.

## Naming Conventions
- **Packages**: All package names should be in lowercase.
- **Classes**: Class names should use CamelCase.
- **Methods**: Method names should be in camelCase.
- **Constants**: Constant variables should be named using uppercase with underscores (UPPER_CASE).
- **Files**: File names should reflect the class they contain (e.g., `UserModel.java`).

## Code Organization Guidelines
1. **Code Structure**: Code should be organized in separate folders based on functionality.
2. **Comments**: All classes and methods should be commented appropriately to explain their purpose.
3. **Testing**: Unit tests should be organized in the `test` directory akin to the source directory structure.
4. **Version Control**: Always commit meaningful messages that describe the changes made to the project.
5. **Consistency**: Follow the same structure and style across the codebase to improve maintainability and readability.

