package com.google.firebase.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Minimal stand-in for Firebase's auto-initialising provider.
 *
 * The original manifest declares this provider; without the Firebase Gradle
 * dependency the class does not exist and the process would die on start-up
 * with a ClassNotFoundException. The game does not use Firebase at runtime, so
 * an empty provider that satisfies the manifest is all that is needed.
 */
public class FirebaseInitProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
