package com.example.themagicofknowledge.models;

import android.content.Context;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;

public class UserRole {

    public enum Role {
        CHILD,
        PARENT,
        ADMIN
    }

    /**
     * מחזיר את התפקיד הנוכחי של המשתמש לפי המצב באפליקציה.
     */
    public static Role getCurrentRole(Context context) {
        UserParent parent = SharedPreferencesUtil.getUser(context);
        UserChild child = SharedPreferencesUtil.getCurrentChild(context);

        if (parent == null) {
            return Role.PARENT; // ברירת מחדל אם משהו לא תקין
        }

        // אם נבחר ילד פעיל - אנחנו במצב ילד
        if (child != null) {
            return Role.CHILD;
        }

        // אם המשתמש המחובר הוא אדמין
        if (parent.isAdmin()) {
            return Role.ADMIN;
        }

        return Role.PARENT;
    }

    public static boolean isAdmin(Context context) {
        return getCurrentRole(context) == Role.ADMIN;
    }

    public static boolean isChild(Context context) {
        return getCurrentRole(context) == Role.CHILD;
    }

    public static boolean isParent(Context context) {
        return getCurrentRole(context) == Role.PARENT;
    }
}