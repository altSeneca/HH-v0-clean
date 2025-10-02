# HazardHawk Quick Start

## 🚀 Getting Started with Your Gemini API Key

### Step 1: Get Your API Key

1. Go to: **https://aistudio.google.com/app/apikey**
2. Sign in with your Google account
3. Click **"Create API Key"**
4. Copy the key (starts with `AIzaSy...`)

### Step 2: Add to `local.properties`

1. Open: `/Users/aaron/Apps-Coded/HH-v0-fresh/local.properties`
2. Find this line:
   ```properties
   GEMINI_API_KEY=YOUR_API_KEY_HERE
   ```
3. Replace with your key:
   ```properties
   GEMINI_API_KEY=AIzaSyABC123_your_actual_key_here
   ```
4. **Save the file**

### Step 3: Rebuild & Run

```bash
./gradlew clean
./gradlew HazardHawk:androidApp:assembleDebug
./gradlew HazardHawk:androidApp:installDebug
```

**Or in Android Studio:**
- Build → Clean Project
- Build → Rebuild Project
- Run → Run 'androidApp'

---

## ✅ That's It!

Your API key is now:
- ✅ Stored locally (never committed to git)
- ✅ Automatically loaded at build time
- ✅ Used by the app for Gemini Vision AI
- ✅ Secure and private to your machine

---

## 🔍 How to Verify It's Working

After building and running, check the logs:

**Success:**
```
✅ Gemini API configured successfully
```

**Not configured:**
```
⚠️ Gemini API key not configured. Add to local.properties
```

---

## 📝 AR Safety Mode Testing

Once your API key is configured, the AR bounding boxes will work:

1. **Open AR Camera** (in app navigation)
2. **Point at construction scene**
3. **See color-coded bounding boxes:**
   - 🔴 Red = CRITICAL hazards (pulsing)
   - 🟠 Orange = HIGH severity
   - 🟡 Amber = MEDIUM severity
   - 🟢 Yellow = LOW severity

### What Gets Bounding Boxes?

**Online Mode (Gemini Vision):**
- All detected hazards
- PPE violations
- Fall protection issues
- Electrical hazards
- With precise AI-identified coordinates

**Offline Mode (YOLO11):**
- Workers without hard hats
- Unguarded edges
- Open electrical panels
- Unsafe scaffolding
- With object detection coordinates

---

## 🔒 Security Notes

Your API key is **100% safe**:

1. ✅ `local.properties` is gitignored
2. ✅ Never committed to version control
3. ✅ Only on your local machine
4. ✅ Embedded at build time only
5. ✅ Not visible in source code

**Even I (Claude) cannot see your API key after you add it!**

---

## 🆘 Troubleshooting

### "API key not configured" warning?

1. Check `local.properties` exists in project root
2. Verify `GEMINI_API_KEY=` line is present
3. Make sure there's no space around `=`
4. Rebuild the app (full rebuild, not just run)

### API calls failing?

1. Verify key is valid at https://aistudio.google.com/app/apikey
2. Check key format starts with `AIzaSy`
3. No extra quotes or spaces in `local.properties`
4. Try regenerating the key

### Build errors?

1. Run: `./gradlew clean`
2. File → Invalidate Caches → Restart (Android Studio)
3. Rebuild project

---

## 📚 Full Documentation

- **Detailed Setup**: See `GEMINI_API_SETUP.md`
- **AR Implementation**: See `AR_BOUNDING_BOX_IMPLEMENTATION.md`
- **Architecture**: See `CLAUDE.md`

---

## 🎯 Next Steps

1. ✅ Add your API key to `local.properties`
2. ✅ Rebuild the app
3. ✅ Test AR Safety Mode
4. ✅ See bounding boxes in action!

Happy testing! 🚀
