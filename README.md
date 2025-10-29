📚 My Hardbounds — Personal Library App

My Hardbounds is a personal book library manager for Android devices.
Scan ISBN barcodes to instantly fetch book details from the Google Books API, and easily organize your Library & Wishlist.

Manage what you’ve read, track what’s on the shelf, and never forget a book again! 📖✨

🚀 Features
Feature Category	Details
📷 ISBN Scanner	Barcode scanning using ZXing to fetch book details automatically
🌐 Book Info Fetching	Title, Author, Description, Cover Image from Google Books API
📚 Personal Library	Add books you own to a local Room DB
📝 Wishlist	Add books to wishlist while browsing
🔄 Smart Logic	If a book exists in library: ✅ No duplicate entry
If moved to library: ✅ Auto-remove from wishlist
📕 Read Status	Mark books as Read / Unread and track progress
🗑 Delete Support	Delete from Library or remove from Wishlist
🔍 Search	Real-time search over title / author
💾 Local Storage	Works fully offline once books are saved
🎨 Clean UI	Material Design, padding fixes and responsive layouts
🎬 Smooth UX	Auto-return to home on scan cancel, live UI updates
🧩 Architecture & Tech Stack
Layer	Technology
App Platform	Android
Language	Kotlin
UI Framework	XML + Material Components (Material3)
Image Loading	Coil
Local Storage	Room Database
Networking	Retrofit + Gson
Barcode Scanning	ZXing
Asynchronous	Kotlin Coroutines
Splash Screen	Android SplashScreen API
🗂 Project Structure
📁 app/src/main/java/com.example.bookshelf
 ├── ui/               → Adapters & UI helpers
 ├── db/               → Room DAO, Entities, Database
 ├── data/             → Retrofit API Client
 ├── HomeActivity      → Main menu
 ├── ScanActivity      → Barcode scanning + fetch flow
 ├── LibraryActivity   → List of owned books
 ├── WishlistActivity  → Books to buy/read later
 └── BookDetailActivity → Detailed info & actions

🔌 External API

Google Books API

Endpoint Format:

GET https://www.googleapis.com/books/v1/volumes?q=isbn:{ISBN}


Data used:

title

authors

description

imageLinks.thumbnail

(Expandable for more metadata)

🧠 Business Rules & Logic
Scenario	Action
Scanning an ISBN already in Library	Show Already in library, disable add buttons
Scanning an ISBN already in Wishlist	Show Already in wishlist, only allow Add to Library
Adding a book to Library that is in Wishlist	✅ Auto-removes from Wishlist
Removing a book from Library	Remains removed unless re-scanned
Long-press Wishlist item	Option to Remove from Wishlist

Consistent database rules ensure no duplicates and clean cataloging ✅

🛠 Build Instructions

Requirements

Android Studio Flamingo or newer

Min SDK: Android 6.0 (API 23) recommended

Internet connection required for fetching metadata

To Run

Clone project or unzip provided archive

Open in Android Studio

Sync Gradle dependencies

Connect real device (camera required)

Run ▶

⚠️ Emulator cameras often fail with barcode scanning.
Use a real device for best results ✅

✅ Future Enhancements (Planned)

🔦 Flash toggle while scanning

📊 Statistics dashboard (books read per year)

⭐ Ratings & notes for each book

📁 Export / Backup library to cloud

🎭 Theming + Dark Mode refinements

📖 Book categories & genre filtering

🖼 Cover thumbnails in library lists (coming soon!)

🧑‍💻 Developer

Developer: Nikhil Joseph

📌 License

This is a personal learning project —
You may modify and reuse for personal purposes ✅
Please provide attribution if shared publicly.

✨ Final Note

Books are your hardbounds of imagination.
This app helps you celebrate every one of them. 📚💙
