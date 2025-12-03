package br.edu.fatecgru.empresta;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import br.edu.fatecgru.empresta.databinding.ActivityAddToolBinding;
import br.edu.fatecgru.empresta.databinding.ItemToolImageBinding;

public class AddToolActivity extends AppCompatActivity {

    private ActivityAddToolBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;
    private User currentUserProfile;

    private final ArrayList<String> imageUrls = new ArrayList<>();
    private final ArrayList<Uri> newImageUris = new ArrayList<>();
    private final ArrayList<String> imagesToDelete = new ArrayList<>();
    private ImageAdapter imageAdapter;
    private Uri photoURI;
    private String editingToolId = null;
    private int replacingImagePosition = -1;

    private final String[] categories = {"Elétrica", "Marcenaria", "Jardinagem", "Hidráulica", "Mecânica", "Outros"};
    private final boolean[] selectedCategories;
    private final List<String> selectedCategoriesList = new ArrayList<>();

    public AddToolActivity() {
        selectedCategories = new boolean[categories.length];
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count && (imageUrls.size() + newImageUris.size()) < 5; i++) {
                            newImageUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        if ((imageUrls.size() + newImageUris.size()) < 5) {
                            newImageUris.add(result.getData().getData());
                        }
                    }
                    updateImageAdapter();
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result && (imageUrls.size() + newImageUris.size()) < 5) {
                    newImageUris.add(photoURI);
                    updateImageAdapter();
                }
            });

    private final ActivityResultLauncher<Intent> replaceImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri newImageUri = result.getData().getData();
                    if (newImageUri != null && replacingImagePosition != -1) {
                        handleImageReplacement(newImageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddToolBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        checkUserProfile();
        setupRecyclerView();

        binding.toolCategorySelector.setOnClickListener(v -> showCategorySelectionDialog());
        binding.addImageButton.setOnClickListener(v -> showImageSourceDialog());
        binding.saveToolButton.setOnClickListener(v -> saveTool());
        binding.completeProfileButtonAddTool.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserProfile();
    }

    private void checkUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentUserProfile = documentSnapshot.toObject(User.class);
                    if (isProfileComplete()) {
                        binding.addToolFormScroll.setVisibility(View.VISIBLE);
                        binding.incompleteProfileWarningAddTool.setVisibility(View.GONE);
                        initializeForm();
                    } else {
                        binding.addToolFormScroll.setVisibility(View.GONE);
                        binding.incompleteProfileWarningAddTool.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.addToolFormScroll.setVisibility(View.GONE);
                    binding.incompleteProfileWarningAddTool.setVisibility(View.VISIBLE);
                }
            });
    }

    private boolean isProfileComplete() {
        return currentUserProfile != null &&
               !TextUtils.isEmpty(currentUserProfile.getCep()) &&
               !TextUtils.isEmpty(currentUserProfile.getStreet()) &&
               !TextUtils.isEmpty(currentUserProfile.getNumber());
    }

    private void initializeForm() {
        if (getIntent().hasExtra("EDIT_TOOL_ID")) {
            editingToolId = getIntent().getStringExtra("EDIT_TOOL_ID");
            if (binding.toolName.getText().toString().isEmpty()) { // Load only if not already loaded
                loadToolData(editingToolId);
            }
            setTitle("Editar Ferramenta");
        }
    }

    private byte[] compressImage(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Resize and compress
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float aspectRatio = (float) originalWidth / originalHeight;
        int targetWidth = 1024;
        int targetHeight = (int) (targetWidth / aspectRatio);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

        return baos.toByteArray();
    }

    private void loadToolData(String toolId) {
        db.collection("tools").document(toolId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Tool tool = documentSnapshot.toObject(Tool.class);
                        if (tool == null) return;

                        binding.toolName.setText(tool.getName());
                        binding.toolBrand.setText(tool.getBrand());
                        binding.toolDescription.setText(tool.getDescription());

                        if (tool.getCategories() != null) {
                            selectedCategoriesList.clear();
                            selectedCategoriesList.addAll(tool.getCategories());
                        }
                        binding.toolCategorySelector.setText(TextUtils.join(", ", selectedCategoriesList));
                        for (int i = 0; i < categories.length; i++) {
                            selectedCategories[i] = selectedCategoriesList.contains(categories[i]);
                        }

                        if (tool.getImageUrls() != null) {
                            imageUrls.clear();
                            imageUrls.addAll(tool.getImageUrls());
                        }
                        updateImageAdapter();
                    }
                });
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter();
        binding.toolImagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.toolImagesRecyclerView.setAdapter(imageAdapter);
    }

    private void updateImageAdapter() {
        List<Object> combinedList = new ArrayList<>();
        combinedList.addAll(imageUrls);
        combinedList.addAll(newImageUris);
        imageAdapter.setItems(combinedList);
    }

    private void showCategorySelectionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Selecione as Categorias")
                .setMultiChoiceItems(categories, selectedCategories, (dialog, which, isChecked) -> selectedCategories[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedCategoriesList.clear();
                    for (int i = 0; i < selectedCategories.length; i++) {
                        if (selectedCategories[i]) {
                            selectedCategoriesList.add(categories[i]);
                        }
                    }
                    binding.toolCategorySelector.setText(TextUtils.join(", ", selectedCategoriesList));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showImageSourceDialog() {
        if ((imageUrls.size() + newImageUris.size()) >= 5) {
            Toast.makeText(this, "Você já adicionou 5 fotos.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Adicionar Foto")
                .setItems(new String[]{"Tirar Foto", "Escolher da Galeria"}, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(intent);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            photoURI = createImageFileUri();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(photoURI);
        } catch (IOException ex) {
            Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createImageFileUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", image);
    }

    private void saveTool() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado. Faça o login novamente.", Toast.LENGTH_LONG).show();
            return;
        }

        String name = binding.toolName.getText().toString().trim();
        String brand = binding.toolBrand.getText().toString().trim();
        String description = binding.toolDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(brand) || selectedCategoriesList.isEmpty() || TextUtils.isEmpty(description) || (imageUrls.isEmpty() && newImageUris.isEmpty())) {
            Toast.makeText(this, "Preencha todos os campos e adicione ao menos uma foto.", Toast.LENGTH_LONG).show();
            return;
        }

        binding.saveToolButton.setEnabled(false);
        Toast.makeText(this, "Salvando ferramenta...", Toast.LENGTH_SHORT).show();

        String toolId = (editingToolId != null) ? editingToolId : db.collection("tools").document().getId();
        deleteOldImagesAndSaveTool(toolId, currentUser.getUid());
    }

    private void deleteOldImagesAndSaveTool(String toolId, String ownerId) {
        if (imagesToDelete.isEmpty()) {
            uploadNewImagesAndSaveTool(toolId, ownerId);
            return;
        }

        List<Task<Void>> deleteTasks = new ArrayList<>();
        for (String url : imagesToDelete) {
            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);
            deleteTasks.add(photoRef.delete());
        }

        Tasks.whenAllComplete(deleteTasks).addOnCompleteListener(task -> {
            imagesToDelete.clear();
            uploadNewImagesAndSaveTool(toolId, ownerId);
        });
    }

    private void uploadNewImagesAndSaveTool(String toolId, String ownerId) {
        final List<String> finalImageUrls = new ArrayList<>(imageUrls);
        if (newImageUris.isEmpty()) {
            saveDataToFirestore(toolId, finalImageUrls, ownerId);
            return;
        }

        List<Task<Uri>> uploadTasks = new ArrayList<>();
        for (Uri uri : newImageUris) {
            try {
                byte[] compressedImage = compressImage(uri);
                StorageReference fileRef = storageRef.child("tool_images/" + toolId + "/" + UUID.randomUUID().toString() + ".jpg");
                uploadTasks.add(fileRef.putBytes(compressedImage).continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        if (task.getException() != null) {
                            throw task.getException();
                        }
                    }
                    return fileRef.getDownloadUrl();
                }));
            } catch (IOException e) {
                Toast.makeText(this, "Falha ao comprimir imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                binding.saveToolButton.setEnabled(true);
                return; // Stop the process if one image fails to compress
            }
        }

        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(urls -> {
            for (Object url : urls) {
                finalImageUrls.add(url.toString());
            }
            saveDataToFirestore(toolId, finalImageUrls, ownerId);
        }).addOnFailureListener(e -> {
            binding.saveToolButton.setEnabled(true);
            Toast.makeText(AddToolActivity.this, "Falha no upload das imagens.", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveDataToFirestore(String toolId, List<String> finalImageUrls, String ownerId) {
        String name = binding.toolName.getText().toString().trim();
        String brand = binding.toolBrand.getText().toString().trim();
        String description = binding.toolDescription.getText().toString().trim();

        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("brand", brand);
        tool.put("categories", selectedCategoriesList);
        tool.put("description", description);
        tool.put("imageUrls", finalImageUrls);
        tool.put("ownerId", ownerId);
        tool.put("available", true);

        db.collection("tools").document(toolId).set(tool)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Ferramenta salva com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    binding.saveToolButton.setEnabled(true);
                    Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleImageClick(int position) {
        List<Object> combinedList = new ArrayList<>();
        combinedList.addAll(imageUrls);
        combinedList.addAll(newImageUris);
        Object item = combinedList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Gerenciar Imagem")
                .setItems(new String[]{"Excluir Imagem", "Substituir Imagem"}, (dialog, which) -> {
                    if (which == 0) { // Excluir
                        if (item instanceof String) {
                            imagesToDelete.add((String) item);
                            imageUrls.remove((String) item);
                        } else if (item instanceof Uri) {
                            newImageUris.remove((Uri) item);
                        }
                        updateImageAdapter();
                    } else if (which == 1) { // Substituir
                        replacingImagePosition = position;
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        replaceImageLauncher.launch(intent);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void handleImageReplacement(Uri newImageUri) {
        List<Object> combinedList = new ArrayList<>();
        combinedList.addAll(imageUrls);
        combinedList.addAll(newImageUris);
        Object itemToReplace = combinedList.get(replacingImagePosition);

        if (itemToReplace instanceof String) {
            imagesToDelete.add((String) itemToReplace);
            imageUrls.remove((String) itemToReplace);
        } else if (itemToReplace instanceof Uri) {
            newImageUris.remove((Uri) itemToReplace);
        }

        newImageUris.add(newImageUri);
        updateImageAdapter();
        replacingImagePosition = -1;
    }


    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private List<Object> items = new ArrayList<>();

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemToolImageBinding itemBinding = ItemToolImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ImageViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Object item = items.get(position);
            if (item instanceof String) {
                Glide.with(AddToolActivity.this).load((String) item).into(holder.binding.toolImageItem);
            } else if (item instanceof Uri) {
                Glide.with(AddToolActivity.this).load((Uri) item).into(holder.binding.toolImageItem);
            }
            holder.itemView.setOnClickListener(v -> {
                int bindingAdapterPosition = holder.getBindingAdapterPosition();
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    handleImageClick(bindingAdapterPosition);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void setItems(List<Object> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            private final ItemToolImageBinding binding;

            ImageViewHolder(ItemToolImageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
