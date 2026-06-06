package com.example.themagicofknowledge.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;

import com.example.themagicofknowledge.R;
import com.example.themagicofknowledge.models.UserChild;
import com.example.themagicofknowledge.models.UserParent;
import com.example.themagicofknowledge.services.DatabaseService;
import com.example.themagicofknowledge.utils.ImageUtil;
import com.example.themagicofknowledge.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class AvatarSelectionActivity extends BaseActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;

    private GridView gvAvatars;
    private MaterialButton btnConfirmAvatar;
    private ImageView ivSelectedPreview; // תצוגה מקדימה של תמונה מגלריה/מצלמה
    private List<String> avatarNames;
    private String childId;
    private UserParent currentParent;

    private int selectedPosition = -1;
    private AvatarAdapter adapter;
    private String selectedCustomImageBase64 = null; // תמונה מגלריה/מצלמה

    // Launchers לגלריה ומצלמה
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        handleSelectedBitmap(bitmap);
                    } catch (Exception e) {
                        Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (bitmap != null) handleSelectedBitmap(bitmap);
                }
            });

    @Override
    protected boolean hasSideMenu() { return false; }

    @Override
    protected boolean showToolbar() { return false; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_selection);

        childId = getIntent().getStringExtra("childId");
        currentParent = SharedPreferencesUtil.getUser(this);

        gvAvatars = findViewById(R.id.gvAvatars);
        btnConfirmAvatar = findViewById(R.id.btnConfirmAvatar);
        ivSelectedPreview = findViewById(R.id.ivSelectedPreview);
        MaterialButton btnBack = findViewById(R.id.btnBackAvatar);
        MaterialButton btnFromGallery = findViewById(R.id.btnFromGallery);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                boolean fromRegistration = getIntent().getBooleanExtra("isFromRegistration", false);

                if (fromRegistration) {
                    // במקום רק לעשות finish() שאולי סוגר את האפליקציה כי המחסנית ריקה
                    // נפתח מחדש את מסך ההרשמה
                    Intent intent = new Intent(this, RegisterActivity.class);
                    // דגלים שמונעים יצירת כפילויות של מסכים
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    finish();
                }
            });
        }

        initAvatarList();
        adapter = new AvatarAdapter();
        gvAvatars.setAdapter(adapter);

        gvAvatars.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            selectedCustomImageBase64 = null; // איפוס תמונה מגלריה
            ivSelectedPreview.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            btnConfirmAvatar.setEnabled(true);
            btnConfirmAvatar.setAlpha(1.0f);
        });

        btnFromGallery.setOnClickListener(v -> showImageSourceDialog());

        btnConfirmAvatar.setOnClickListener(v -> {
            if (selectedCustomImageBase64 != null) {
                saveAvatar("base64:" + selectedCustomImageBase64);
            } else if (selectedPosition >= 0) {
                saveAvatar(avatarNames.get(selectedPosition));
            } else {
                Toast.makeText(this, "אנא בחרו תמונה תחילה", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        btnConfirmAvatar.setAlpha(0.5f);
        btnConfirmAvatar.setEnabled(false);
    }

    private void saveAvatar(String avatarValue) {
        boolean isParent = getIntent().getBooleanExtra("isParent", false);

        if (isParent) {
            String parentId = getIntent().getStringExtra("parentId");
            DatabaseService.getInstance().updateParentAvatar(parentId, avatarValue, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void unused) {
                    UserParent parent = SharedPreferencesUtil.getUser(AvatarSelectionActivity.this);
                    if (parent != null) {
                        parent.setAvatar(avatarValue);
                        SharedPreferencesUtil.saveUser(AvatarSelectionActivity.this, parent);
                    }
                    Toast.makeText(AvatarSelectionActivity.this, "התמונה עודכנה! 🎉", Toast.LENGTH_SHORT).show();
                    boolean fromProfile = getIntent().getBooleanExtra("fromProfile", false);
                    if (fromProfile) {
                        finish();
                    } else {
                        Intent intent = new Intent(AvatarSelectionActivity.this, SelectChildActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }
                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(AvatarSelectionActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (avatarValue.startsWith("base64:")) {
                saveBase64Avatar(avatarValue.substring(7));
            } else {
                updateChildAvatarInFirebase(avatarValue);
            }
        }
    }

    private void showImageSourceDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.findViewById(R.id.btnCamera).setOnClickListener(v -> {
            dialog.dismiss();
            openCamera();
        });

        dialog.findViewById(R.id.btnGallery).setOnClickListener(v -> {
            dialog.dismiss();
            openGallery();
        });

        dialog.findViewById(R.id.btnCancelSource).setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(true);
        dialog.show();
    }

    private void openCamera() {
        ImageUtil.requestPermission(this);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        ImageUtil.requestPermission(this);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void handleSelectedBitmap(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

        ivSelectedPreview.setImageBitmap(resized);
        ivSelectedPreview.setVisibility(View.VISIBLE);

        // הצג גם את הכרטיס
        CardView cardPreview = findViewById(R.id.cardPreview);
        cardPreview.setVisibility(View.VISIBLE);
        cardPreview.setCardElevation(12f);
        // הוסף מסגרת כתומה
        ((com.google.android.material.card.MaterialCardView) cardPreview)
                .setStrokeColor(android.graphics.Color.parseColor("#FF9800"));
        ((com.google.android.material.card.MaterialCardView) cardPreview)
                .setStrokeWidth(6);

        selectedCustomImageBase64 = ImageUtil.convertBitmapTo64Base(resized);
        selectedPosition = -1;
        adapter.notifyDataSetChanged();
        btnConfirmAvatar.setEnabled(true);
        btnConfirmAvatar.setAlpha(1.0f);
    }

    private void saveBase64Avatar(String base64) {
        String avatarValue = "base64:" + base64;
        DatabaseService.getInstance().updateChildAvatar(currentParent.getId(), childId, avatarValue, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                UserParent parent = SharedPreferencesUtil.getUser(AvatarSelectionActivity.this);
                if (parent != null && parent.getChildrenList() != null) {
                    UserChild childInParent = parent.getChildrenList().get(childId);
                    if (childInParent != null) {
                        childInParent.setAvatar(avatarValue);
                        SharedPreferencesUtil.saveUser(AvatarSelectionActivity.this, parent);
                        SharedPreferencesUtil.saveCurrentChild(AvatarSelectionActivity.this, childInParent);
                    }
                }
                showPlacementTestIntroDialog();
                Toast.makeText(AvatarSelectionActivity.this, "תמונה נשמרה! 🎉", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AvatarSelectionActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadAvatar(ImageView imageView, String avatar) {
        if (avatar == null) {
            imageView.setImageResource(R.drawable.logo);
        } else if (avatar.startsWith("base64:")) {
            // תמונה מגלריה/מצלמה
            String base64 = avatar.substring(7);
            Bitmap bitmap = ImageUtil.convertFrom64base(base64);
            if (bitmap != null) imageView.setImageBitmap(bitmap);
        } else {
            // אווטר רגיל
            int resId = getResources().getIdentifier(avatar, "drawable", getPackageName());
            imageView.setImageResource(resId != 0 ? resId : R.drawable.logo);
        }
    }

    private void initAvatarList() {
        avatarNames = new ArrayList<>();

        boolean isParent = getIntent().getBooleanExtra("isParent", false);

        if (isParent) {
            // אווטרים להורים
            for (int i = 10; i <= 18; i++) {
                avatarNames.add("avatar_" + i);
            }
        } else {
            // אווטרים לילדים
            for (int i = 1; i <= 9; i++) {
                avatarNames.add("avatar_" + i);
            }
        }
    }

    private void updateChildAvatarInFirebase(String avatarName) {
        DatabaseService.getInstance().updateChildAvatar(currentParent.getId(), childId, avatarName, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                UserParent parent = SharedPreferencesUtil.getUser(AvatarSelectionActivity.this);
                if (parent != null && parent.getChildrenList() != null) {
                    UserChild childInParent = parent.getChildrenList().get(childId);
                    if (childInParent != null) {
                        childInParent.setAvatar(avatarName);
                        SharedPreferencesUtil.saveUser(AvatarSelectionActivity.this, parent);
                        SharedPreferencesUtil.saveCurrentChild(AvatarSelectionActivity.this, childInParent);
                    }
                }
                showPlacementTestIntroDialog();
                Toast.makeText(AvatarSelectionActivity.this, "בחירה נהדרת! עכשיו נבדוק את הרמה שלך 🎯", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AvatarSelectionActivity.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class AvatarAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return avatarNames.size();
        }

        @Override
        public Object getItem(int position) {
            return avatarNames.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_avatar, parent, false);
            }

            FrameLayout container = convertView.findViewById(R.id.avatarContainer);
            ImageView imageView = convertView.findViewById(R.id.ivAvatarItem);

            String name = avatarNames.get(position);
            int resId = getResources().getIdentifier(name, "drawable", getPackageName());

            if (resId != 0) {
                imageView.setImageResource(resId);
            }

            // ⭐ מסמן את התמונה הנבחרת
            if (container != null) {
                if (position == selectedPosition) {
                    container.setBackgroundResource(R.drawable.selected_avatar_border);
                } else {
                    container.setBackgroundResource(R.drawable.default_avatar_background);
                }
            }

            return convertView;
        }
    }

    private void showPlacementTestIntroDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_placement_intro);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.findViewById(R.id.btnStartTest).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(AvatarSelectionActivity.this, PlacementTestActivity.class);
            intent.putExtra("isNewChild", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        dialog.findViewById(R.id.btnLater).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(AvatarSelectionActivity.this, SelectChildActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }
}