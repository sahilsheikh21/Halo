<div align="center">

# 🌟 Halo

**A modern Android social and chat app powered by the Matrix protocol, delivering real-time messaging, secure media sharing, and community feeds all in one stunning interface.**

![App Banner](docs/banner.png)

[![Kotlin](https://img.shields.io/badge/Kotlin-17-blue.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Matrix SDK](https://img.shields.io/badge/Powered_by-Matrix-black.svg)](https://matrix.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

## 📖 Introduction

Welcome to **Halo**! In a world filled with walled-garden social networks, Halo brings the power of decentralized, secure communication straight to your Android device. 

Built natively with Kotlin and Jetpack Compose, Halo provides a gorgeous, fluid user experience. Under the hood, it leverages the robust Matrix Rust SDK to ensure your chats are blazing fast, reliable, and secure. Whether you're catching up with friends in real-time or scrolling through community feeds, Halo makes decentralized communication feel incredibly effortless.

---

## ✨ Core Features

*   **💬 Real-Time Messaging:** Blazing-fast chat synchronization powered by the Matrix protocol. Never miss a message.
*   **📸 Media Sharing:** Seamlessly upload and share high-quality images and videos with your contacts.
*   **🌐 Community Feeds:** Discover and engage with posts in a beautifully designed social feed.
*   **🛡️ Secure & Private:** Decentralized architecture utilizing the Matrix Rust SDK keeps your data out of the hands of big tech.
*   **🎨 Stunning UI:** A fluid, modern interface built from the ground up using Jetpack Compose and Material 3 design principles.

---

## 🚀 Quick Start / Installation

Ready to try Halo? Getting the app running on your local machine takes just a few minutes.

### Prerequisites
*   [Android Studio](https://developer.android.com/studio) (Latest Version)
*   An Android Emulator or physical device running Android 8.0 (API 26) or higher.

### Step-by-Step Guide

1. **Clone the repository:**
   ```bash
   git clone https://github.com/sahilsheikh21/Halo.git
   cd Halo
   ```
2. **Open in Android Studio:**
   Launch Android Studio, select **Open**, and choose the `Halo` directory.
3. **Sync Gradle:**
   Allow Android Studio to download dependencies and sync the project (this may take a minute).
4. **Run the App:**
   Select your emulator or physical device from the target dropdown and click the ▶️ **Run** button (or press `Shift + F10`).

---

## 🕹️ Usage

Using Halo is as simple as any major social app, but with the added benefits of decentralization:

1. **Create an Account:** Open the app and register a new Matrix account (or log into an existing one).
2. **Discover & Chat:** Use the Explore tab to find friends or join rooms, and start chatting instantly.
3. **Share & Post:** Tap the create button to upload media or share a new post to your community feed!

![Demo](docs/demo.gif)

---

## ❓ FAQ & Troubleshooting

**Q: The app isn't syncing my messages immediately. What should I do?**  
**A:** Ensure you have a stable internet connection. If the issue persists, try swiping down on the chat screen to force a manual refresh, or restart the app to re-establish the Matrix Sync pipeline.

**Q: I'm getting build errors related to the Matrix SDK.**  
**A:** Halo uses the Matrix Rust SDK. Ensure you have synced your Gradle files completely and are using Java 17 for your compilation targets. Try running `Build > Clean Project` followed by `Rebuild Project`.

---

## 🤝 Contributing

We love community contributions! If you have an idea to make Halo even better, here’s how you can help:

1. **Fork** the repository.
2. **Create a branch** for your feature (`git checkout -b feature/AmazingFeature`).
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`).
4. **Push** to the branch (`git push origin feature/AmazingFeature`).
5. Open a **Pull Request**.

If you find a bug, please [open an issue](https://github.com/sahilsheikh21/Halo/issues) with a detailed description of the problem.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
