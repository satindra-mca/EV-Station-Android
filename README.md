
# EV Station Finder & Slot Booking — Android Frontend

![Java](https://img.shields.io/badge/Java-17-orange)
![Android](https://img.shields.io/badge/Android-API%2025%2B-green)
![Gradle](https://img.shields.io/badge/Gradle-8.7-blue)
![UI](https://img.shields.io/badge/UI-XML-purple)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

## 1. Project Overview

EV Station Finder & Slot Booking is an Android-based application
designed to help electric vehicle users discover nearby EV charging
stations, view charging facilities, check slot availability, and
reserve charging slots.

This repository contains the Android frontend of the application,
developed using Java and XML in Android Studio.

The application is intended to communicate with a Spring Boot
REST API backend for authentication, charging station discovery,
booking management, and role-based operations.

This project is being developed as an MCA Semester-3 academic
project with a structured, modular development approach.

## 2. Project Objectives

- Provide a user-friendly Android application for EV users.
- Discover nearby EV charging stations using location services.
- Display charging station, charger, and connector information.
- Allow users to reserve available charging slots.
- Support booking history and cancellation.
- Implement secure authentication using JWT.
- Provide role-based interfaces for users, station owners, and
  administrators.
- Maintain a modular and maintainable application architecture.

## 3. Project Repositories

The project is divided into two independent repositories.

| Component | Repository |
|---|---|
| Android Frontend | [EV-Station-Android](https://github.com/satindra-mca/EV-Station-Android) |
| Spring Boot Backend | [EV-Station-Backend](https://github.com/satindra-mca/EV-Station-Backend) |

The Android application handles the user interface and client-side
operations, while the backend handles business logic, database
operations, authentication, and authorization.

## 4. Technology Stack

### Android Frontend

| Technology | Version / Details |
|---|---|
| IDE | Android Studio |
| Programming Language | Java 17 |
| UI Development | XML |
| Build System | Gradle |
| Gradle Version | 8.7 |
| Android Gradle Plugin | 8.5.2 |
| Compile SDK | 34 |
| Target SDK | 34 |
| Minimum SDK | 25 |
| AppCompat | 1.7.0 |
| Material Components | 1.12.0 |
| ConstraintLayout | 2.2.1 |
| AndroidX SplashScreen | 1.0.1 |
| Version Control | Git and GitHub |

### Backend Technologies

- Spring Boot
- Java
- Spring Security
- JWT Authentication
- REST APIs
- MySQL

### Planned Frontend Integrations

- Retrofit
- OkHttp
- JSON serialization and deserialization
- Google Maps SDK for Android
- Android location services
- RecyclerView
- Secure session management

## 5. Current Implementation

The initial Android application foundation has been implemented.

### 5.1 Project Configuration

- Android application configured using Java 17.
- Gradle-based build configuration.
- Centralized dependency version management.
- Organized Activity package structure.
- Centralized colors, strings, and themes.
- Git repository initialized and connected to GitHub.

### 5.2 Splash Screen

- Dedicated SplashActivity.
- AndroidX SplashScreen API integration.
- Custom EV-themed splash image.
- Custom splash screen theme.
- Status bar visibility management.
- Delayed navigation using Handler and Looper.
- Lifecycle-aware removal of pending Handler callbacks.
- Automatic navigation to HomeActivity after 1500 milliseconds.

The current splash navigation is temporary and will be replaced
with authentication-based navigation after session management
is implemented.

### 5.3 Home Screen

- Initial XML-based home screen layout.
- EV-themed user interface.
- Reusable drawable resources.
- Centralized color and string resources.
- Dedicated HomeActivity.

The current home screen is a UI prototype. Live charging station
data and backend integration are not yet implemented.

## 6. Development Status

| Module | Status |
|---|---|
| Android Project Setup | Completed |
| Java 17 Configuration | Completed |
| Activity Package Refactoring | Completed |
| Splash Screen | Implemented |
| Initial Home Screen UI | Implemented |
| Colors and Strings Resources | Implemented |
| Git and GitHub Setup | Completed |
| Login Screen | Planned |
| Registration Screen | Planned |
| REST API Integration | Planned |
| JWT Session Management | Planned |
| Role-Based Navigation | Planned |
| Dynamic Home Screen | Planned |
| GPS and Location Services | Planned |
| Google Maps Integration | Planned |
| Charging Station Discovery | Planned |
| Charger and Connector Details | Planned |
| Slot Booking | Planned |
| Payment Flow | Planned |
| Booking History | Planned |
| Booking Cancellation | Planned |
| Owner Dashboard | Planned |
| Admin Dashboard | Planned |
| End-to-End Testing | Planned |

## 7. Project Structure

The following structure represents the current tracked Android
project files and directories.

```text
Ev_Station_Frontend/
|
|-- app/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/
|   |   |   |   |-- com/example/ev_station/
|   |   |   |       |-- activity/
|   |   |   |           |-- MainActivity.java
|   |   |   |           |-- SplashActivity.java
|   |   |   |           |-- HomeActivity.java
|   |   |   |
|   |   |   |-- res/
|   |   |   |   |-- drawable/
|   |   |   |   |-- layout/
|   |   |   |   |   |-- activity_main.xml
|   |   |   |   |   |-- activity_splash_screen.xml
|   |   |   |   |   |-- activity_home.xml
|   |   |   |   |-- mipmap-*/
|   |   |   |   |-- values/
|   |   |   |   |   |-- colors.xml
|   |   |   |   |   |-- strings.xml
|   |   |   |   |   |-- themes.xml
|   |   |   |   |-- xml/
|   |   |   |
|   |   |   |-- AndroidManifest.xml
|   |   |
|   |   |-- test/
|   |   |-- androidTest/
|   |
|   |-- build.gradle.kts
|   |-- proguard-rules.pro
|   |-- .gitignore
|
|-- gradle/
|   |-- wrapper/
|   |-- libs.versions.toml
|
|-- .gitignore
|-- build.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- settings.gradle.kts
|-- README.md
```

## 8. Planned Application Architecture

The application will follow a modular architecture to separate
user interface logic, network communication, data models,
repositories, and session management.

```text
com.example.ev_station/
|
|-- activity/
|-- adapter/
|-- api/
|-- model/
|   |-- request/
|   |-- response/
|-- repository/
|-- session/
|-- utils/
```

### Package Responsibilities

| Package | Responsibility |
|---|---|
| activity | Application screens and navigation |
| adapter | RecyclerView and list adapters |
| api | Retrofit configuration and API interfaces |
| model.request | API request models |
| model.response | API response models |
| repository | Data access and API coordination |
| session | Authentication and session management |
| utils | Shared utilities and validation helpers |

These packages will be populated as the corresponding modules
are implemented. Empty directories are not tracked by Git.

### Planned Data Flow

```text
Android Activity
       |
       v
Repository
       |
       v
Retrofit API Interface
       |
       v
Spring Boot REST API
       |
       v
Service Layer
       |
       v
Repository Layer
       |
       v
MySQL Database
```

## 9. Application Navigation

### Current Navigation

```text
Application Launch
        |
        v
SplashActivity
        |
        v
HomeActivity
```

### Planned Authentication-Based Navigation

```text
Application Launch
        |
        v
SplashActivity
        |
        v
Session Validation
        |
        +-- No Valid Session --> Login Screen
        |                            |
        |                            +--> Registration Screen
        |                            |
        |                            +--> Authentication API
        |                                     |
        |                                     v
        +-- Valid Session ------------> Role-Based Navigation
                                              |
                                              +--> USER Dashboard
                                              |
                                              +--> OWNER Dashboard
                                              |
                                              +--> ADMIN Dashboard
```

Role-based frontend navigation will complement backend
authorization. The backend remains responsible for enforcing
access control.

## 10. Prerequisites

Before running the Android application, ensure the following
software and tools are available:

- Android Studio compatible with Android Gradle Plugin 8.5.2.
- Java Development Kit (JDK) 17.
- Android SDK Platform 34.
- Android SDK Build Tools.
- Git.
- Android Emulator or a physical Android device.
- Internet connection for initial dependency downloads.

The minimum supported Android version is Android 7.1
(API level 25).

A separate Gradle installation is not required because the
project includes the Gradle Wrapper.

## 11. Installation and Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/satindra-mca/EV-Station-Android.git
```

### Step 2: Navigate to the Project Directory

```bash
cd EV-Station-Android
```

### Step 3: Open the Project

1. Launch Android Studio.
2. Select Open.
3. Select the cloned project directory.
4. Configure the Android SDK if required.
5. Configure Gradle to use JDK 17.
6. Wait for Gradle Sync to complete.

### Step 4: Run the Application

1. Start an Android Emulator or connect a physical Android device.
2. Select the app run configuration.
3. Click Run in Android Studio.

The current application launches the splash screen and then
navigates to the initial home screen.

## 12. Build Commands

### Windows PowerShell

```powershell
.\gradlew.bat clean assembleDebug
```

### Linux / macOS

```bash
./gradlew clean assembleDebug
```

If required, grant execution permission to the Gradle Wrapper:

```bash
chmod +x gradlew
```

### Generated Debug APK

After a successful build, the debug APK is available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 13. Testing

The project includes the initial Android unit test and
instrumentation test structure.

### Run Unit Tests

```powershell
.\gradlew.bat testDebugUnitTest
```

### Run Instrumentation Tests

Connect an Android device or start an emulator before running:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Current Build Verification

The initial Android project has been successfully compiled
using the Gradle Wrapper.

Authentication, API integration, booking, and end-to-end tests
will be added during the corresponding development phases.

## 14. Backend Integration

The Android frontend will communicate with the existing
Spring Boot backend using REST APIs.

Backend repository:

https://github.com/satindra-mca/EV-Station-Backend

The planned integration includes:

- User authentication and registration.
- JWT-based authenticated requests.
- Charging station discovery.
- Charger and connector information.
- Slot availability and booking.
- Booking history and cancellation.
- Owner and administrator operations.

API endpoints, request models, response models, and validation
rules will be implemented according to the verified backend
API contracts.

Backend connectivity is not required to run the current
frontend UI prototype.

## 15. Security Guidelines

- Do not commit passwords, JWT tokens, or private credentials.
- Do not commit local.properties or secrets.properties.
- Do not commit signing keys or keystore files.
- Keep sensitive configuration outside version control.
- Use secure session storage when authentication is implemented.
- Use HTTPS for production API communication.
- Validate user input before sending API requests.
- Handle authentication failures and expired sessions.
- Enforce authorization through backend security controls.
- Restrict Android API keys appropriately when introduced.

The repository includes a .gitignore configuration to exclude
local configuration, generated build files, IDE files, and
common sensitive files.

## 16. Development Roadmap

### Phase 1: Android Foundation

- [x] Android project setup
- [x] Java 17 configuration
- [x] Activity package refactoring
- [x] Splash screen implementation
- [x] Initial home screen UI
- [x] Centralized resource configuration
- [x] Git and GitHub setup

### Phase 2: Authentication

- [ ] Backend authentication API verification
- [ ] Login screen
- [ ] Registration screen
- [ ] Retrofit configuration
- [ ] Authentication API integration
- [ ] JWT session management
- [ ] Splash screen session validation
- [ ] Role-based navigation

### Phase 3: Charging Station Discovery

- [ ] Dynamic home screen
- [ ] Charging station API integration
- [ ] RecyclerView station listing
- [ ] Location permission handling
- [ ] GPS integration
- [ ] Google Maps integration
- [ ] Nearby charging station search
- [ ] Charging station details

### Phase 4: Booking Management

- [ ] Charger and connector selection
- [ ] Slot availability
- [ ] Date and time selection
- [ ] Booking creation
- [ ] Advance payment flow
- [ ] Booking history
- [ ] Booking cancellation
- [ ] Booking status handling

### Phase 5: Role-Based Dashboards

- [ ] User dashboard
- [ ] Station owner dashboard
- [ ] Owner booking management
- [ ] Charger and connector management
- [ ] Administrator dashboard
- [ ] Station and owner management

### Phase 6: Final Testing and Documentation

- [ ] API integration testing
- [ ] Input validation testing
- [ ] Error handling
- [ ] Authentication and authorization testing
- [ ] UI and navigation testing
- [ ] End-to-end testing
- [ ] Application screenshots
- [ ] Final documentation
- [ ] Release build preparation

## 17. Git Workflow

The project follows incremental, module-based development.

Each module is implemented, built, tested, reviewed, and
committed before proceeding to the next development stage.

### Commit Message Conventions

| Prefix | Purpose |
|---|---|
| feat | New feature |
| fix | Bug fix |
| refactor | Code restructuring |
| docs | Documentation changes |
| test | Test-related changes |
| chore | Configuration and maintenance |
| style | Formatting and visual styling |

### Standard Development Workflow

```bash
git status
git diff
git add <changed-files>
git diff --cached --check
git diff --cached --stat
git commit -m "type(scope): meaningful description"
git push origin main
git status
git log -1 --oneline
```

Generated files and sensitive configuration must be reviewed
before staging and committing changes.

## 18. Application Screenshots

Application screenshots will be added as the corresponding
screens are finalized and tested.

## 19. Project Status

**Status: Active Development**

The Android application foundation, splash screen, initial
home screen, and GitHub repository setup are complete.

The next development milestone is the Authentication Module,
followed by backend API integration and charging station
discovery.

This README will be updated incrementally as additional
modules are implemented and verified.

---

EV Station Finder & Slot Booking — Android Frontend
