# BazaarUpdater Android SDK

<p align="center">
<img src="./images/dark.png" alt="gif" width="700">
</p>

[![GitHub License](https://img.shields.io/github/license/cafebazaar/BazaarPay)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://jitpack.io/v/cafebazaar/BazaarUpdater.svg)](https://jitpack.io/#cafebazaar/BazaarUpdater)


BazaarUpdater is an Android library that simplifies checking for updates and managing the update process for your application on Bazaar.

## Setup

To get started with BazaarUpdater, you need to add the JitPack repository to your project and include the library dependency.

### Adding JitPack Repository

**Kotlin DSL**

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
```

**groovy**
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

## Adding Dependency

**Kotlin DSL**

```kotlin
dependencies {
    implementation("com.github.cafebazaar:bazaarUpdater:1.1.1")
}
```

**groovy**

```groovy
dependencies {
    implementation 'com.github.cafebazaar:bazaarUpdater:1.1.1'
}
```
## Usage

### Checking for Updates

To check if there are any updates available for your application on Bazaar, use the following code:


```kotlin
BazaarUpdater.getLastUpdateState(context = context) { result ->
    when(result) {
        UpdateResult.AlreadyUpdated -> {
            // Handle the case where the app is already updated
        }
        is UpdateResult.Error -> {
            // Handle the error case
            val errorMessage = result.getError()?.message
        }
        is UpdateResult.NeedUpdate -> {
            // Handle the case where an update is needed
            val targetVersion = result.getTargetVersionCode()
        }
    }
}
```

<details><summary><b>Java Usage</b></summary>

```java
BazaarUpdater.getLastUpdateState(context, result -> {
    if (result.isAlreadyUpdated()) {
        // Handle the case where the app is already updated
    } else if (result.isUpdateNeeded()) {
        // Handle the case where an update is needed
        long targetVersion = result.getTargetVersionCode();
    } else {
        // Handle the error case
        String errorMessage = result.getError().getMessage();
    }
});
```
</details>

#### Update Result States

##### 1. AlreadyUpdated: Indicates that your application is up-to-date.

##### 2. Error: Indicates an error occurred. Use `result.message` to get the error message.

##### 3. NeedUpdate: Indicates that a new update is available. Use `result.getTargetVersionCode()` to get the version code of the update.

### Updating the Application

To update your application when a new version is available on Bazaar, simply call:

```kotlin
BazaarUpdater.updateApplication(context = context)
```
## Auto Update

This feature allows you to enable automatic updates for your apps in Bazaar. Once enabled, Bazaar will check for application updates daily. If an update is available and Bazaar is the update owner of your app, it will automatically download and install the update.

> ⚠️ Note: This feature requires Bazaar version 26.2.0 or higher.

> ⚠️ Package Name : Ensure the package name (also known as the application ID in Android) in your source code exactly matches the one used in the already published app. This is typically defined in your app's AndroidManifest.xml file.

> ⚠️ Signature : The app must be signed with the same key as the published version. Make sure you’re using the correct keystore and alias that were used for the original app.

### Checking for Auto Update

To check whether Auto Update is enabled for your application in Bazaar, use the following code:

```kotlin
BazaarAutoUpdater.getLastAutoUpdateState(context = this) { result ->
    when (result.getState()) {
        AutoUpdateState.ENABLED -> {
            // Auto Update is active — nothing to do
        }
        AutoUpdateState.DISABLED -> {
            // Auto Update is supported but turned off — prompt the user to enable it
        }
        AutoUpdateState.NOT_SUPPORTED -> {
            // Bazaar is not installed or needs to be updated to version 26.2.0+
            val errorMessage = result.getError()?.message
        }
    }
}
```

<details>
<summary><strong>Java Usage</strong></summary>

```java
BazaarAutoUpdater.getLastAutoUpdateState(context, result -> {
    switch (result.getState()) {
        case ENABLED:
            // Auto Update is active — nothing to do
            break;
        case DISABLED:
            // Auto Update is supported but turned off — prompt the user to enable it
            break;
        case NOT_SUPPORTED:
            // Bazaar is not installed or needs to be updated to version 26.2.0+
            String errorMessage = result.getError().getMessage();
            break;
    }
});
```

</details>

#### Auto Update States

##### 1. `ENABLED`: Auto Update is active. Bazaar will automatically download and install updates daily.

##### 2. `DISABLED`: Bazaar supports Auto Update (version 26.2.0+) but the user has not enabled it. Call `enableAutoUpdate()` to prompt the user.

##### 3. `NOT_SUPPORTED`: Bazaar is not installed or its version is below 26.2.0. Use `result.getError()` for details.

### Enable Auto Update

To open the Bazaar Auto Update settings page for your application, call:

```kotlin
BazaarAutoUpdater.enableAutoUpdate(context = context)
```

## Contributing

Contributions are welcome! If you have suggestions or improvements, please open an issue or submit a pull request.


## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

<br/>
<br/>

<p align="center">
Made with 💚 in Bazaar Hackathon 1403
</p>


