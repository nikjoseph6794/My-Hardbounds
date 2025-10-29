# 📚 My Hardbounds — Personal Library App

**My Hardbounds** is a personal Android app to manage your book collection.
Scan ISBN barcodes, fetch details from Google Books, and organize titles into **Library** and **Wishlist**.
Keep track of what you’ve read, and what you want next.

---

## ✨ Features

- 📷 **ISBN barcode scan** using ZXing camera scanner
- 🌐 Fetches:
  - Title, Author(s), Description
  - Cover image (Coil with HTTPS fix)
- 📚 **Library** (Room DB)
- 📝 **Wishlist** (separate Room table)
- 🔁 Smart behavior:
  - Already in Library → “Already in library”
  - Already in Wishlist → “Already in wishlist”
  - Adding to Library removes it from Wishlist automatically
- 🔍 **Real-time search** (title or author)
- ✅ **Read/Unread** status toggle
- 🗑 Delete from Library / Remove from Wishlist (long-press)
- 💾 **Backup & Restore** library to JSON
- 🧭 Polished UX:
  - Cancel scan → return to home
  - Debounce search
  - Keyboard hides on scroll

---

## 🧩 Tech Stack

| Category | Technology |
|---------|------------|
| Language | Kotlin |
| Architecture | Room + View-binding |
| Networking | Retrofit + Moshi/Gson |
| Camera & Barcode | ZXing `IntentIntegrator` |
| Local Persistence | Room with migrations |
| UI Framework | XML + Material 3 components |
| Image Loading | Coil |
| Concurrency | Kotlin Coroutines + Flows |
| Splash Screen | AndroidX SplashScreen API |

---


---

## 🌐 API Used

- **Google Books REST API**
- GET https://www.googleapis.com/books/v1/volumes?q=isbn:{ISBN}

Uses:
- `volumeInfo.title`
- `volumeInfo.authors[]`
- `volumeInfo.description`
- `volumeInfo.imageLinks.thumbnail` / `smallThumbnail`

---

## 🪵 Database Schema

### Table: `books`
| Column | Notes |
|--------|------|
| isbn (PK) | Primary key |
| title | String |
| authors | Comma-separated |
| description | String |
| coverUrl | HTTPS thumbnail |
| isRead | Boolean |
| addedAt | Epoch millis |

### Table: `wishlist`
| Column | Notes |
|--------|------|
| isbn (PK) | Primary key |
| title | String |
| authors | String |
| description | String |
| coverUrl | String |
| addedAt | Epoch millis |

✅ Migration history included (`1→2`: isRead, `2→3`: coverUrl, `3→4`: wishlist)

---

## ▶️ Running the App

**Requirements**
- Android Studio Flamingo or higher
- Min SDK: 23+
- Real device recommended (scanner works better)

**Steps**
1. Open project in Android Studio
2. Sync Gradle
3. Run on device
4. Grant camera + internet permissions

---

## 🧪 Key Screens

- **Home Page** → Library / Scan / Wishlist
- **Scanner Page** → live barcode reading
- **Book Details** → cover + metadata + actions
- **Library** → saved books + search
- **Wishlist** → long-press to remove
- **Backup/Restore** → JSON import/export

---

## 🛠 Troubleshooting

| Issue | Solution |
|------|----------|
| Scanner closes with no result | Use real device + grant camera |
| Covers not loading | Check internet / HTTPS conversion |
| Migration crash | Uninstall app during dev |
| Splash icon cropped | Re-generate launcher icons with padding |

---

## 🔮 Future Enhancements

- 📊 Stats (books read per year)
- ⭐ Ratings & personal notes
- 🏷 Genre filtering / tags
- 🔦 Flash toggle in scanner
- ☁️ Cloud backup (Google Drive)
- 🖼 Thumbnails in library lists
- 📤 Export CSV/PDF

---

## 👤 Developer

- **App Developer:** Nikhil Joseph  
- **AI-assisted Documentation:** ChatGPT

---

## 📜 License

Personal and learning use only.  
You may modify with attribution.

---



