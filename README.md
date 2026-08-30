# Task & File Manager

> A native Android productivity application that combines **task management** and **local file management** into a single, clean and practical interface.

**Task & File Manager** was developed as an offline-first Android application using **Kotlin and Jetpack Compose**. The application provides users with tools for managing tasks as well as browsing and organizing files and folders available on the device.

The project focuses on a clean user experience, reliable local operations, persistent settings, and a structured Android architecture that keeps the UI and application logic separated.

---

## 📱 Project Overview

Task & File Manager brings two everyday productivity functions into one Android application:

* **Task management** for creating and managing personal tasks.
* **File management** for browsing and organizing files and folders.
* **Persistent application preferences** for maintaining user settings.
* **Modern Material-style UI** built with Jetpack Compose.
* **Local/offline operation** without requiring an internet connection for core functionality.

The application was designed with maintainability and extensibility in mind so that additional functionality can be introduced without restructuring the entire application.

---

## ✨ Key Features

### ✅ Task Management

* Create and manage tasks
* Edit task information
* Organize tasks according to their state/priority
* Mark tasks as completed
* Persistent local task data
* Clean task-oriented user interface

### 📁 File Management

* Browse files and folders
* Navigate through directory structures
* Open folders and navigate back through the hierarchy
* Select files and folders
* Copy files and folders
* Move files and folders
* Paste copied/moved items
* Rename files and folders
* Delete files and folders
* Refresh directory contents
* Search and filtering functionality
* Sorting support

### 🔤 Sorting

Files can be organized using different sorting options, including:

* Name — A to Z
* Name — Z to A
* Size — Small to Large
* Size — Large to Small
* Date — Newest First
* Date — Oldest First

### ⚙️ Settings & Preferences

* Persistent application settings
* Preferences stored locally on the device
* Settings remain available between application sessions

### 🎨 User Interface

* Modern Jetpack Compose UI
* Material-style components
* Clear navigation
* Loading and progress indicators
* Empty states
* Error handling states
* Dialog-based file/task actions
* Responsive interaction feedback

---

## 📸 Screenshots

The following screenshots demonstrate the implemented application and its major workflows.

### 🏠 Home / Dashboard

![Home Screen](screenshots/home.png)

---

### 📋 Task Management

![Task Management](screenshots/tasks.png)

---

### 📁 File Manager

![File Manager](screenshots/file-manager.png)

---

### 📂 Folder Navigation

![Folder Navigation](screenshots/folder-navigation.png)

---

### 📋 File Operations

![File Operations](screenshots/file-operations.png)

---

### ✏️ Rename

![Rename Dialog](screenshots/rename-dialog.png)

---

### 🔍 Search & Sorting

![Search and Sorting](screenshots/search-sorting.png)

---

### ⚙️ Settings

![Settings](screenshots/settings.png)

---

## 🛠️ Technology Stack

| Technology                   | Usage                                       |
| ---------------------------- | ------------------------------------------- |
| **Kotlin**                   | Primary programming language                |
| **Jetpack Compose**          | Android user interface                      |
| **Material Design**          | Application UI components and styling       |
| **Android SDK**              | Native Android platform                     |
| **ViewModel**                | UI state and application logic coordination |
| **Kotlin Coroutines**        | Asynchronous operations                     |
| **Kotlin Flow**              | Reactive state/data handling                |
| **DataStore Preferences**    | Persistent application preferences          |
| **Android Storage APIs**     | Local file and folder management            |
| **Storage Access Framework** | Access to user-selected storage locations   |

---

## 🏗️ Architecture

The application follows a structured architecture with responsibilities separated between the presentation, state-management, data, storage, and preferences layers.

```text
┌───────────────────────────────────┐
│           Jetpack Compose         │
│               UI                  │
└─────────────────┬─────────────────┘
                  │
                  ▼
┌───────────────────────────────────┐
│             ViewModel             │
│       UI State & Operations       │
└─────────────────┬─────────────────┘
                  │
                  ▼
┌───────────────────────────────────┐
│          Data / Storage           │
│                                   │
│  Tasks     File Operations        │
│            Storage Access         │
└─────────────────┬─────────────────┘
                  │
          ┌───────┴────────┐
          ▼                ▼
   Local Task Data     Device Storage
                           │
                           ▼
                  Android Storage APIs
```

This separation helps keep UI components focused on presentation while application operations are handled through dedicated state and data-management components.

---

## 📂 File Storage

The file manager works with the storage locations that Android makes available to the application.

The implementation uses Android's storage APIs and the **Storage Access Framework** where required, allowing the user to select and access storage locations while respecting Android's modern storage model.

The application does not require an internet connection for its core file-management functionality.

Storage access is handled according to Android's storage and permission model, including support for user-selected directories where applicable.

---

## 💾 Persistent Preferences

Application preferences are stored locally using **Android DataStore Preferences**.

This provides a modern asynchronous mechanism for maintaining settings between application sessions.

The preferences layer is separated from the UI so that screens do not directly manage persistent preference storage.

---

## 🔄 State Management

The application uses **ViewModel-based state management** together with Kotlin Coroutines and Flow.

This allows the UI to react to changes in application state while keeping operational logic outside individual Compose UI components.

Examples include:

* File listing state
* Loading state
* Error state
* Selection state
* Clipboard/copy-move state
* Directory navigation
* Task state
* Application preferences

---

## 📋 File Operations

The file manager supports the following core operations:

| Operation         | Supported |
| ----------------- | :-------: |
| Browse files      |     ✅     |
| Browse folders    |     ✅     |
| Folder navigation |     ✅     |
| Back navigation   |     ✅     |
| Copy              |     ✅     |
| Move              |     ✅     |
| Paste             |     ✅     |
| Rename            |     ✅     |
| Delete            |     ✅     |
| Refresh           |     ✅     |
| Search            |     ✅     |
| Sorting           |     ✅     |

Operations provide appropriate UI feedback, including loading/progress indicators and error states where necessary.

---

## 🔎 Search & Organization

The file manager provides tools for finding and organizing files more efficiently.

Users can search available items and sort directory contents according to:

* File/folder name
* Modification date

Sorting can be performed in ascending or descending order depending on the selected option.

---

## 🔐 Privacy & Offline Design

The application's core functionality is designed around local device usage.

The application does not depend on a remote server for its primary task-management and file-management functionality.

User files remain on the device and are accessed through Android's storage mechanisms.

---

## 🧪 Testing & Validation

The application was tested through the major user workflows, including:

* Task creation and management
* File and folder browsing
* Directory navigation
* File selection
* Copy and move operations
* Paste operations
* Rename operations
* Delete operations
* Search and sorting
* Settings persistence
* Loading and error states
* Navigation and UI interactions

Testing focused on ensuring that the application behaves correctly during normal user workflows and handles common storage-operation situations appropriately.

---

## 📱 Android Compatibility

The application is designed for modern Android devices and follows Android's current storage-access model.

Storage behavior may vary depending on the Android version and the storage permissions/access granted by the user.

---

## 📦 Project Deliverables

The completed project includes:

* Android application
* Task management functionality
* File management functionality
* Local persistence
* Modern Compose-based UI
* Structured application architecture
* File operation workflows
* Settings/preferences
* Testing of major workflows
* Project source maintained in a private Git repository

---

## 🔒 Source Code

The source code for this project is maintained in a **private GitHub repository**.

The repository is intentionally private because the implementation is part of a professional/client project.

This README provides a high-level overview of the application, its capabilities, architecture, and visual implementation without exposing the application's source code.

---

## 🚀 Project Status

**Completed**

Task & File Manager has been implemented as a functional native Android application with task-management and local file-management capabilities.

---

## 👨‍💻 Development

**Developer:** Adnan

**Platform:** Android

**Language:** Kotlin

**UI:** Jetpack Compose

**Architecture:** MVVM architecture

**Project Type:** Offline Android Utility / Productivity Application

---

## 📌 Summary

Task & File Manager demonstrates the development of a complete native Android utility application combining two major workflows — **task management and local file management** — within a single application.

The project demonstrates practical Android development using Kotlin, Jetpack Compose, ViewModel-based state management, Coroutines/Flow, DataStore Preferences, and Android's modern storage APIs.

The application was developed with an emphasis on **clean UI, maintainable structure, reliable local operations, and a practical user experience**.
