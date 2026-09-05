package cn.lineai.data.grok;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Encrypted-at-rest storage for Grok OAuth tokens and account metadata. */
public final class GrokAuthStore {
    private static final String PREFS_NAME = "linecode_grok_auth";
    private static final String TOKENS_KEY = "tokens";
    private static final String IDENTITY_KEY = "identity";
    private static final String KEY_ALIAS = "linecode_grok_auth_v1";
    private static final String SEPARATOR = ":";

    private final Context context;

    public GrokAuthStore(Context context) {
        this.context = context.getApplicationContext() == null
                ? context
                : context.getApplicationContext();
    }

    public void saveTokenResponse(JSONObject response) throws Exception {
        if (response == null) {
            throw new IllegalArgumentException("Token response is empty");
        }
        JSONObject previous = readJson(TOKENS_KEY);
        JSONObject saved = new JSONObject();
        copyString(response, previous, saved, "access_token");
        copyString(response, previous, saved, "refresh_token");
        copyString(response, previous, saved, "id_token");
        copyString(response, previous, saved, "token_type");
        copyString(response, previous, saved, "scope");

        long expiresAt = 0L;
        long expiresIn = response.optLong("expires_in", 0L);
        if (expiresIn > 0L) {
            expiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        } else if (response.has("expires_at")) {
            long raw = response.optLong("expires_at", 0L);
            expiresAt = raw > 0L && raw < 100000000000L ? raw * 1000L : raw;
        } else if (previous != null) {
            expiresAt = previous.optLong("expires_at", 0L);
        }
        if (expiresAt > 0L) {
            saved.put("expires_at", expiresAt);
        }
        if (saved.optString("access_token", "").length() == 0) {
            throw new IllegalArgumentException("Token response has no access token");
        }
        writeEncrypted(TOKENS_KEY, saved.toString());
    }

    public String getAccessToken() {
        JSONObject json = readJson(TOKENS_KEY);
        return json == null ? "" : json.optString("access_token", "");
    }

    public String getRefreshToken() {
        JSONObject json = readJson(TOKENS_KEY);
        return json == null ? "" : json.optString("refresh_token", "");
    }

    public long getExpiresAtMillis() {
        JSONObject json = readJson(TOKENS_KEY);
        return json == null ? 0L : json.optLong("expires_at", 0L);
    }

    public boolean hasAccessToken() {
        return getAccessToken().length() > 0;
    }

    public synchronized void saveIdentity(String userId, String planType, String email) throws Exception {
        JSONObject previous = readJson(IDENTITY_KEY);
        JSONObject identity = new JSONObject();
        identity.put("user_id", prefer(userId, previous == null ? "" : previous.optString("user_id", "")));
        identity.put("plan_type", prefer(planType, previous == null ? "" : previous.optString("plan_type", "")));
        identity.put("email", prefer(email, previous == null ? "" : previous.optString("email", "")));
        writeEncrypted(IDENTITY_KEY, identity.toString());
    }

    public String getUserId() {
        return identityValue("user_id");
    }

    public String getPlanType() {
        return identityValue("plan_type");
    }

    public String getEmail() {
        return identityValue("email");
    }

    public void clear() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(TOKENS_KEY)
                .remove(IDENTITY_KEY)
                .apply();
    }

    private String identityValue(String key) {
        JSONObject json = readJson(IDENTITY_KEY);
        return json == null ? "" : json.optString(key, "");
    }

    private void copyString(JSONObject response, JSONObject previous, JSONObject target, String key) throws Exception {
        String value = response.optString(key, "");
        if (value.length() == 0 && previous != null) {
            value = previous.optString(key, "");
        }
        if (value.length() > 0) {
            target.put(key, value);
        }
    }

    private String prefer(String value, String fallback) {
        return value != null && value.trim().length() > 0 ? value.trim() : (fallback == null ? "" : fallback);
    }

    private JSONObject readJson(String key) {
        String encrypted = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, null);
        if (encrypted == null || encrypted.length() == 0) {
            return null;
        }
        try {
            return new JSONObject(decrypt(encrypted));
        } catch (Exception ignored) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(key)
                    .apply();
            return null;
        }
    }

    private void writeEncrypted(String key, String value) throws Exception {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key, encrypt(value))
                .apply();
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv, Base64.NO_WRAP)
                + SEPARATOR
                + Base64.encodeToString(ciphertext, Base64.NO_WRAP);
    }

    private String decrypt(String value) throws Exception {
        int separator = value.indexOf(SEPARATOR);
        if (separator <= 0 || separator >= value.length() - 1) {
            throw new IllegalArgumentException("Invalid encrypted value");
        }
        byte[] iv = Base64.decode(value.substring(0, separator), Base64.DEFAULT);
        byte[] ciphertext = Base64.decode(value.substring(separator + 1), Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private SecretKey getKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
