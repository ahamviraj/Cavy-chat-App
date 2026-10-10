const admin = require("firebase-admin");

if (!admin.apps.length) {
  try {
    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: process.env.FIREBASE_DATABASE_URL || "https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app"
      });
    } else {
      admin.initializeApp({
        projectId: process.env.FIREBASE_PROJECT_ID || "cavy-chat-app",
        databaseURL: process.env.FIREBASE_DATABASE_URL || "https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app"
      });
    }
  } catch (e) {
    console.error("Firebase Admin initialization error:", e);
  }
}

module.exports = async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed. Use POST." });
  }

  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Unauthorized. Missing ID token." });
  }

  const idToken = authHeader.split("Bearer ")[1];
  let decodedToken;

  try {
    decodedToken = await admin.auth().verifyIdToken(idToken);
  } catch (e) {
    return res.status(401).json({ error: "Unauthorized. Invalid ID token." });
  }

  const authUid = decodedToken.uid;
  const { conversationId, messageText } = req.body || {};

  if (!conversationId || !messageText) {
    return res.status(400).json({ error: "Missing conversationId or messageText." });
  }

  try {
    const db = admin.database();

    // Verify conversation existence and participant membership
    const convSnapshot = await db.ref(`conversations/${conversationId}`).once("value");
    if (!convSnapshot.exists()) {
      return res.status(404).json({ error: "Conversation not found." });
    }

    const convData = convSnapshot.val();
    const participants = convData.participants || {};

    if (!participants[authUid]) {
      return res.status(403).json({ error: "Forbidden. Caller is not a participant in this conversation." });
    }

    // Identify recipient UID (the participant that is NOT caller)
    const recipientUid = Object.keys(participants).find(uid => uid !== authUid);
    if (!recipientUid) {
      return res.status(400).json({ error: "Recipient participant not found." });
    }

    // Get recipient FCM token from privateProfiles
    const recipientTokenSnap = await db.ref(`privateProfiles/${recipientUid}/fcmToken`).once("value");
    if (!recipientTokenSnap.exists() || !recipientTokenSnap.val()) {
      return res.status(200).json({ success: false, message: "Recipient has no registered FCM token." });
    }

    const fcmToken = recipientTokenSnap.val();

    // Get sender display name
    const senderProfileSnap = await db.ref(`profiles/${authUid}/displayName`).once("value");
    const senderDisplayName = senderProfileSnap.exists() ? senderProfileSnap.val() : "Cavy User";

    const payload = {
      token: fcmToken,
      notification: {
        title: `New message from ${senderDisplayName}`,
        body: messageText
      },
      data: {
        conversationId: conversationId,
        otherUid: authUid,
        otherDisplayName: senderDisplayName,
        text: messageText
      }
    };

    const response = await admin.messaging().send(payload);
    return res.status(200).json({ success: true, messageId: response });
  } catch (error) {
    console.error("Failed to send push notification:", error);
    return res.status(500).json({ error: error.message || "Failed to send notification." });
  }
};
