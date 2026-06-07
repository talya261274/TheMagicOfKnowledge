package com.example.themagicofknowledge.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.Base64;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;

import com.example.themagicofknowledge.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;

/// מחלקת עזר לפעולות על תמונות
/// מכילה פונקציות לבקשת הרשאות, המרת תמונות ל-base64 ולהפך
public class ImageUtil {

    /// בקשת הרשאות למצלמה ולאחסון
    /// @param activity ה-Activity שממנו מבקשים הרשאות
    public static void requestPermission(@NotNull Activity activity) {
        // בקשת הרשאות למצלמה ולאחסון
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 1);
    }

    /// המרת מחרוזת base64 לתמונה
    /// @param base64Code מחרוזת base64 להמרה
    /// @return תמונה המיוצגת על ידי מחרוזת base64, או null אם המחרוזת ריקה
    public static @Nullable Bitmap convertFrom64base(@NotNull final String base64Code) {
        if (base64Code.isEmpty()) {
            return null;
        }
        byte[] decodedString = Base64.decode(base64Code, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    /// המרת Bitmap למחרוזת base64 באיכות מופחתת (70%)
    /// @param bitmap התמונה להמרה
    /// @return מחרוזת base64 המייצגת את התמונה
    public static String convertBitmapTo64Base(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /// טעינת אווטר לתוך ImageView לפי סוג התמונה
    /// אם null - מציג לוגו ברירת מחדל
    /// אם base64 - ממיר ומציג את התמונה
    /// אם שם drawable - מחפש את התמונה במשאבים
    /// @param context הקשר האפליקציה
    /// @param imageView רכיב התמונה להצגה
    /// @param avatar שם האווטר או מחרוזת base64
    public static void loadAvatar(Context context, ImageView imageView, String avatar) {
        if (avatar == null) {
            // אין תמונה - מציג לוגו ברירת מחדל
            imageView.setImageResource(R.drawable.logo);
        } else if (avatar.startsWith("base64:")) {
            // תמונה מגלריה/מצלמה - ממיר מ-base64
            String base64 = avatar.substring(7);
            Bitmap bitmap = convertFrom64base(base64);
            if (bitmap != null) imageView.setImageBitmap(bitmap);
        } else {
            // אווטר רגיל - מחפש לפי שם ב-drawable
            int resId = context.getResources().getIdentifier(avatar, "drawable", context.getPackageName());
            imageView.setImageResource(resId != 0 ? resId : R.drawable.logo);
        }
    }
}