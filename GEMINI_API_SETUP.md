# Gemini API Key Setup Guide

## Secure Local API Key Storage

Your Gemini API key is stored securely in `local.properties`, which is **automatically gitignored** and never committed to version control. This keeps your API key private and local to your development machine.

---

## Setup Instructions

### 1. Get Your Gemini API Key

1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click **"Get API Key"** or **"Create API Key"**
4. Copy your API key (starts with `AIzaSy...`)

### 2. Add API Key to `local.properties`

1. Open `/Users/aaron/Apps-Coded/HH-v0-fresh/local.properties`
2. Find the line: `GEMINI_API_KEY=YOUR_API_KEY_HERE`
3. Replace `YOUR_API_KEY_HERE` with your actual API key:

```properties
GEMINI_API_KEY=AIzaSyABCDEF1234567890_your_actual_key_here
```

4. Save the file

### 3. Rebuild the App

The API key is embedded into the app during build time via BuildConfig:

```bash
# Clean and rebuild
./gradlew clean
./gradlew HazardHawk:androidApp:assembleDebug

# Or in Android Studio
# Build → Clean Project
# Build → Rebuild Project
```

### 4. Verify Setup

The app will automatically use your API key when initialized. Check the logs:

```
✅ Gemini API configured successfully
```

If you see this warning, the key is not configured:
```
⚠️ Gemini API key not configured. Add to local.properties
```

---

## How It Works

### Build-Time Configuration

1. **`local.properties`** - Stores your API key (gitignored)
   ```properties
   GEMINI_API_KEY=AIzaSy...
   ```

2. **`build.gradle.kts`** - Reads the key at build time
   ```kotlin
   val localProperties = Properties()
   val localPropertiesFile = rootProject.file("../local.properties")
   if (localPropertiesFile.exists()) {
       localPropertiesFile.inputStream().use { localProperties.load(it) }
   }
   val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
   buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
   ```

3. **`BuildConfig`** - Generated class with your API key
   ```kotlin
   object BuildConfig {
       const val GEMINI_API_KEY = "AIzaSy..." // Your actual key
   }
   ```

4. **`AIConfig.kt`** - Helper to access the key
   ```kotlin
   object AIConfig {
       fun getGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY
   }
   ```

5. **`LiveDetectionViewModel`** - Uses the key to initialize AI
   ```kotlin
   init {
       val apiKey = AIConfig.getGeminiApiKey()
       smartAIOrchestrator.configure(apiKey)
   }
   ```

---

## Security Features

### ✅ What Keeps Your Key Safe

1. **Gitignored File**
   - `local.properties` is in `.gitignore`
   - Never committed to version control
   - Stays on your local machine only

2. **Build-Time Embedding**
   - Key is embedded during compilation
   - Not stored in source code
   - Only exists in compiled APK

3. **No Code Changes Needed**
   - Update key in `local.properties`
   - Rebuild app
   - New key is automatically used

4. **Per-Developer Configuration**
   - Each developer has their own `local.properties`
   - Use different keys for testing/production
   - No shared secrets in code

### 🔒 What You Should NOT Do

❌ **DO NOT** commit `local.properties` to git
❌ **DO NOT** hardcode API keys in source files
❌ **DO NOT** share your API key publicly
❌ **DO NOT** use production keys for testing

---

## Troubleshooting

### Issue: "API key not configured" warning

**Solution**:
1. Check `local.properties` exists
2. Verify `GEMINI_API_KEY=` line is present
3. Ensure no spaces around the `=` sign
4. Rebuild the app

### Issue: API key not working

**Solution**:
1. Verify key is valid at [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Check key format starts with `AIzaSy`
3. Ensure no extra quotes or spaces
4. Try generating a new key

### Issue: Build fails with "unresolved reference: GEMINI_API_KEY"

**Solution**:
1. Clean project: `./gradlew clean`
2. Rebuild: `./gradlew HazardHawk:androidApp:assembleDebug`
3. In Android Studio: File → Invalidate Caches → Restart

### Issue: Changes not taking effect

**Solution**:
1. Make sure you saved `local.properties`
2. Do a **full rebuild** (not just run)
3. Restart Android Studio if needed

---

## File Reference

### `local.properties` (Project Root)
```properties
sdk.dir=/Users/aaron/Library/Android/sdk

# Add your Gemini API key here (this file is gitignored)
# Get your key from: https://aistudio.google.com/app/apikey
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

### `.gitignore` (Project Root)
```gitignore
# Local configuration (API keys, secrets)
local.properties
```

### `build.gradle.kts` (Android App)
```kotlin
defaultConfig {
    // Load API key from local.properties
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("../local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }
    val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
    buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
}
```

### `AIConfig.kt` (Android App)
```kotlin
package com.hazardhawk.ai

import com.hazardhawk.BuildConfig

object AIConfig {
    fun getGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    fun isGeminiConfigured(): Boolean {
        return getGeminiApiKey().isNotBlank() &&
               !getGeminiApiKey().contains("YOUR_API_KEY_HERE")
    }
}
```

---

## Team Collaboration

### For New Developers

1. Clone the repository
2. Create your own `local.properties` file
3. Add your personal Gemini API key
4. Build and run

### For Production Deployment

Use environment-specific configuration:

```kotlin
// In build.gradle.kts
buildTypes {
    release {
        // Use environment variable or CI/CD secret
        val prodApiKey = System.getenv("GEMINI_API_KEY_PROD") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$prodApiKey\"")
    }
    debug {
        // Use local.properties for development
        val localProperties = Properties()
        // ... (current implementation)
    }
}
```

---

## Additional Security (Optional)

### ProGuard Obfuscation

In `proguard-rules.pro`:
```proguard
# Obfuscate BuildConfig
-keep class com.hazardhawk.BuildConfig {
    *;
}
-assumenosideeffects class com.hazardhawk.BuildConfig {
    public static final java.lang.String GEMINI_API_KEY;
}
```

### Runtime Encryption (Advanced)

For production apps, consider encrypting the API key:

```kotlin
object AIConfig {
    private val cipher = // AES encryption

    fun getGeminiApiKey(): String {
        val encrypted = BuildConfig.GEMINI_API_KEY
        return cipher.decrypt(encrypted)
    }
}
```

---

## API Key Rotation

To update your API key:

1. Generate new key at [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Update `local.properties`:
   ```properties
   GEMINI_API_KEY=AIzaSy_NEW_KEY_HERE
   ```
3. Rebuild the app
4. Old key is automatically replaced

No code changes needed! 🎉

---

## Summary

✅ **API Key Location**: `local.properties` (gitignored)
✅ **Access Method**: `AIConfig.getGeminiApiKey()`
✅ **Security**: Never committed, build-time only
✅ **Updates**: Edit file → Rebuild → Done

Your API key is safe, secure, and easy to manage! 🔒
