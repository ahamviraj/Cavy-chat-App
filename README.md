# Cavy 💬

**A simple, modern, real-time messaging app for Android.**

Cavy is a native Android chat application built with Kotlin and Jetpack Compose. It focuses on private one-to-one conversations, user profiles, real-time messaging, and push notifications, with a clean interface featuring white, black, and vibrant pink accents.

> 🚧 **Development status:** Cavy is under active development. Some features and deployment requirements may still need configuration before the app is ready for production use.

## ✨ Features

- **Authentication:** Sign up and sign in using Firebase Authentication.
- **User profiles:** Manage user profile information.
- **One-to-one messaging:** Private conversations between two users.
- **Real-time updates:** Firebase Realtime Database keeps conversation data synchronized.
- **Push notifications:** Firebase Cloud Messaging (FCM), supported by a Node.js backend deployed on Vercel.
- **Modern Android UI:** Built with Jetpack Compose and Material 3.
- **Privacy-focused data structure:** Public profile information is separated from private profile data.
- **Pink-accented design:** A minimal interface with Cavy's signature `#FF2D8D` accent color.

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Jetpack Compose | Native Android UI |
| Material 3 | UI components and styling |
| Firebase Authentication | User authentication |
| Firebase Realtime Database | Profiles, conversations, and messages |
| Firebase Cloud Messaging | Push notifications |
| Firebase Admin SDK | Server-side notification delivery |
| Node.js | Notification backend runtime |
| Vercel | Backend deployment |
| Git & GitHub | Version control and source hosting |

## 📁 Project Structure

The repository contains the Android application and the notification backend.

```text
Cavy-chat-App/
├── app/                         # Android application
│   └── src/
│       └── main/
│           ├── java/             # Kotlin source code
│           ├── res/              # Android resources
│           └── AndroidManifest.xml
├── backend/
│   └── api/
│       └── sendNotification.js   # Push notification API
├── gradle/                       # Gradle wrapper configuration
├── build.gradle.kts              # Root Gradle configuration
├── settings.gradle.kts           # Project settings
├── gradlew                       # Gradle wrapper (Unix)
├── gradlew.bat                   # Gradle wrapper (Windows)
└── README.md
```

*The structure above highlights the main project components. Individual files may vary as development continues.*

## 🚀 Getting Started

### Prerequisites

Before running Cavy locally, make sure you have:

- [Android Studio](https://developer.android.com/studio)
- [Git](https://git-scm.com/)
- A compatible JDK configured through Android Studio
- A Firebase project with the required services enabled
- [Node.js](https://nodejs.org/) and npm for backend development
- [Vercel](https://vercel.com/) account for backend deployment

### 1. Clone the Repository

```bash
git clone https://github.com/ahamviraj/Cavy-chat-App.git
cd Cavy-chat-App
```

### 2. Configure Firebase for Android

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Select the Firebase project configured for Cavy.
3. Register an Android app using the package name:

   `com.virudaya.cavy`

4. Download `google-services.json`.
5. Place the file in the Android application's `app/` directory.
6. Enable Firebase Authentication with the Email/Password provider.
7. Create or configure Firebase Realtime Database.
8. Publish the appropriate Realtime Database security rules.
9. Configure Firebase Cloud Messaging for push notifications.

**Important:** Use the Firebase project intended for this application. The Android configuration file must match that project.

### 3. Build and Run the Android App

1. Open the cloned project in Android Studio.
2. Allow Gradle synchronization to complete.
3. Connect an Android device or start an emulator.
4. Run the `app` configuration.

Alternatively, build a debug APK from the project root.

**Windows:**

```powershell
.\gradlew.bat assembleDebug
```

**Linux/macOS:**

```bash
./gradlew assembleDebug
```

The debug APK is normally generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## ☁️ Deploy the Notification Backend to Vercel

Cavy uses a Node.js API to send push notifications through Firebase Cloud Messaging. This backend must be deployed separately from the Android application.

### 1. Import the GitHub Repository

1. Open the [Vercel Dashboard](https://vercel.com/dashboard).
2. Select **Add New → Project**.
3. Import `ahamviraj/Cavy-chat-App`.
4. Configure the project to use the backend directory as its **Root Directory**:

   ```text
   backend
   ```

5. Confirm that the backend contains the required Vercel API files and Node.js dependencies.
6. Deploy the project.

The notification endpoint should be available at:

```text
https://YOUR-VERCEL-DOMAIN.vercel.app/api/sendNotification
```

Replace `YOUR-VERCEL-DOMAIN` with the actual domain assigned to your deployment.

### 2. Configure Environment Variables

Open your Vercel project settings and add the following environment variables.

| Variable | Value |
|---|---|
| `FIREBASE_SERVICE_ACCOUNT_JSON` | The complete Firebase service-account JSON |
| `FIREBASE_DATABASE_URL` | The Firebase Realtime Database URL |

For Cavy's existing Firebase project, the database URL is:

```text
https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app
```

For `FIREBASE_SERVICE_ACCOUNT_JSON`:

1. Open Firebase Console → Project Settings → Service Accounts.
2. Generate a service-account private key if you have the required permissions.
3. Store the complete JSON content in Vercel as the environment variable value.
4. Redeploy the backend after configuring the variables.

**Security warning:** Never commit the service-account JSON, private key, or other server credentials to GitHub. Keep them exclusively in a secure environment such as Vercel's environment-variable settings.

### 3. Connect the Android App to the Deployed Backend

The Android application must call the actual deployed endpoint.

Locate the backend URL configuration used by the notification helper, such as `BACKEND_URL` in `NotificationHelper.kt` or the corresponding startup configuration.

Set it to your real endpoint:

```text
https://YOUR-VERCEL-DOMAIN.vercel.app/api/sendNotification
```

Do not leave the URL unset or use the placeholder domain. If `BACKEND_URL` is `null` or blank, the notification helper may skip sending the request entirely.

Rebuild and install the Android app after changing its configuration.

### 4. Verify Push Notifications

Test notifications using two separate accounts on two Android devices, or suitable test devices.

- [ ] Both users can authenticate.
- [ ] Both users have valid FCM registration tokens.
- [ ] The tokens are saved to the expected private profile records.
- [ ] The Vercel deployment is successful.
- [ ] The required Firebase environment variables are configured.
- [ ] Sending a message invokes the backend endpoint.
- [ ] The backend authenticates the request and validates conversation membership.
- [ ] The recipient's FCM token is found.
- [ ] The backend successfully sends the notification.
- [ ] The recipient receives a notification when the app is in the background.
- [ ] Foreground notifications are handled by the Android messaging service.

A successful HTTP response alone does not guarantee delivery. Check the response body, Vercel logs, Android Logcat, and device notification permissions when troubleshooting.

## 🔐 Security

Cavy relies on Firebase Authentication and Realtime Database security rules to protect user data.

Important security practices:

- Keep private profile information separate from public profile information.
- Restrict private profile access to the authenticated owner where appropriate.
- Validate conversation participants before allowing message access.
- Keep Firebase Admin SDK credentials on the backend only.
- Never trust a client-provided user ID or conversation ID without server-side authorization checks.
- Publish and test the actual Firebase Realtime Database rules before production use.
- Request and handle Android notification permissions appropriately.

**Before a public release:** Review the database rules and backend authorization logic carefully. A repository containing security rules does not automatically mean those rules have been published to Firebase.

## 🧪 Development and Testing

The Android project can be developed in Android Studio, while the notification API can be maintained independently in the `backend/` directory.

Recommended checks before merging changes:

1. Build the Android application.
2. Run the available unit tests.
3. Verify authentication and real-time messaging.
4. Test notification delivery on physical Android devices.
5. Check Firebase and Vercel logs for errors.
6. Review database rules and environment configuration.

## 🗺️ Roadmap

Planned improvements may include:

- Further refinement of the chat and profile interfaces.
- More robust notification delivery and diagnostics.
- Additional messaging and account-management features.
- Improved testing, reliability, and production security.

The roadmap may change as development progresses.

## 🤝 Contributing

Contributions, bug reports, and suggestions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Make and test your changes.
4. Commit your work with a descriptive message.
5. Open a pull request describing the changes.

Please do not include private credentials, service-account keys, or personal user data in contributions.

## 📄 License

No license has been specified here. Unless a license is added to the repository, the project should not be assumed to grant permission for redistribution, modification, or other reuse.

## 👨‍💻 Author

**Viraj**

GitHub: [@ahamviraj](https://github.com/ahamviraj)

Project: [Cavy-chat-App](https://github.com/ahamviraj/Cavy-chat-App)

---

*Built with Kotlin, Firebase, and a little bit of pink. 💗*
