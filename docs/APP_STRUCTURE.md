# Application folder layout

Platform application hosts are grouped under the repository's `app/` directory:

```text
app/
├── android/   Android phone application and Android-specific services
├── ios/       Reserved for the future iOS application host
└── desktop/   Existing Compose/JVM desktop application and headless UI tests

shared/        Kotlin Multiplatform domain, playback, design and UI modules
providers/     Catalog, metadata and media-source adapters
```

The Android and desktop Gradle modules are `:app:android` and `:app:desktop`. The `app/ios/` folder currently contains a scope note only: there is no iOS Gradle target, Xcode project or verified Apple-platform build yet. The desktop module remains a JVM application; moving it into `app/` does not add Windows/macOS native packaging. Platform-specific desktop sources can be added under `app/desktop/` without putting them in the Android or iOS hosts.
