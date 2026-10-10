# CAVY Push Notification Backend (Vercel Serverless)

This is the trusted, zero-cost push notification backend for CAVY. It runs outside the Android client on Vercel's free serverless platform.

## Features & Security Properties
1. **Zero-Cost:** Runs on Vercel's generous free tier (no Firebase Blaze plan or paid Cloud Functions needed).
2. **Authenticated ID Token Verification:** Verifies caller's Firebase Auth ID token before executing any database reads or sending push messages.
3. **Server-Side Participant Validation:** Verifies caller's membership in `/conversations/{conversationId}` directly in RTDB before finding recipient token.
4. **Private FCM Token Protection:** Retrieves recipient FCM token from `/privateProfiles/{recipientUid}/fcmToken` using Firebase Admin SDK.
5. **Anti-Spam & Abuse Protection:** Callers cannot specify arbitrary recipient UIDs or forge notification titles.

## Vercel Deployment Instructions
1. Install Vercel CLI or link your repository to [Vercel](https://vercel.com).
2. From the `backend/` directory, run:
   ```bash
   vercel
   ```
3. Set the following Environment Variables in Vercel Dashboard -> **Settings** -> **Environment Variables**:
   - `FIREBASE_SERVICE_ACCOUNT_JSON`: Service account JSON string from Firebase Console -> Project Settings -> Service Accounts -> Generate new private key.
   - `FIREBASE_DATABASE_URL`: `https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app`
4. Deploy to production:
   ```bash
   vercel --prod
   ```
5. Note your production endpoint URL (e.g., `https://your-cavy-backend.vercel.app/api/sendNotification`).
