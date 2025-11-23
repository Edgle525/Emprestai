package br.edu.fatecgru.empresta;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final ArrayList<String> imageUrls = new ArrayList<>();
    private final ArrayList<Uri> newImageUris = new ArrayList<>();
    private ImageAdapter imageAdapter;
    private Uri photoURI;
    private String editingToolId = null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddToolBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        setupRecyclerView();

        if (getIntent().hasExtra("EDIT_TOOL_ID")) {
            editingToolId = getIntent().getStringExtra("EDIT_TOOL_ID");
            loadToolData(editingToolId);
            setTitle("Editar Ferramenta");
        }

        binding.toolCategorySelector.setOnClickListener(v -> showCategorySelectionDialog());
        binding.addImageButton.setOnClickListener(v -> showImageSourceDialog());
        binding.saveToolButton.setOnClickListener(v -> saveTool());
    }

    private void loadToolData(String toolId) {
        db.collection("tools").document(toolId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Tool tool = documentSnapshot.toObject(Tool.class);
                        binding.toolName.setText(tool.getName());
                        binding.toolBrand.setText(tool.getBrand());
                        binding.toolDescription.setText(tool.getDescription());

                        selectedCategoriesList.addAll(tool.getCategories());
                        binding.toolCategorySelector.setText(TextUtils.join(", ", selectedCategoriesList));
                        for (int i = 0; i < categories.length; i++) {
                            if (selectedCategoriesList.contains(categories[i])) {
                                selectedCategories[i] = true;
                            }
                        }

                        if (tool.getImageUrls() != null) {
                            imageUrls.addAll(tool.getImageUrls());
                        }
                        updateImageAdapter();
                    }
                });
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter();
        binding.toolImagesRecyclerView.setAdapter(imageAdapter);
    }

    private void updateImageAdapter() {
        List<Object> combinedList = new ArrayList<>();
        combinedList.addAll(imageUrls);
        combinedList.addAll(newImageUris);
        imageAdapter.setItems(combinedList);
    }
    
    // ... (O resto do código permanece o mesmo, com a lógica de salvar/atualizar)
    // A lógica de `saveTool` precisará ser adaptada para verificar se `editingToolId` não é nulo.

}

// O Adapter precisará ser modificado para aceitar uma lista de `Object` (String ou Uri)
