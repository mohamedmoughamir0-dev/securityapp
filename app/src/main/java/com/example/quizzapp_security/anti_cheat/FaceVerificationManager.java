package com.example.quizzapp_security.anti_cheat;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FaceVerificationManager {

    private static final String PREFS_NAME = "face_verification";
    private static final String KEY_PREFIX = "embedding_";
    static final float SIMILARITY_THRESHOLD = 0.85f;

    private final SharedPreferences prefs;

    public FaceVerificationManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveEmbedding(float[] embedding) {
        prefs.edit().putString(getKey(), serialize(embedding)).apply();
    }

    public boolean hasEmbedding() {
        return prefs.contains(getKey());
    }

    /**
     * Returns true if the given embedding matches the saved reference, or if no reference exists.
     * Returns false only when a reference exists and the face does not match.
     */
    public boolean verify(float[] embedding) {
        if (embedding == null) return false;
        String saved = prefs.getString(getKey(), null);
        if (saved == null) return true;
        return cosineSimilarity(deserialize(saved), embedding) >= SIMILARITY_THRESHOLD;
    }

    // Keyed by Firebase UID so different users on the same device have separate references.
    private String getKey() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "default";
        return KEY_PREFIX + uid;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0f;
        float dot = 0f, normA = 0f, normB = 0f;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0f || normB == 0f) return 0f;
        return dot / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    private String serialize(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private float[] deserialize(String json) {
        String stripped = json.replace("[", "").replace("]", "").trim();
        String[] parts = stripped.split(",");
        float[] embedding = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            embedding[i] = Float.parseFloat(parts[i].trim());
        }
        return embedding;
    }
}
