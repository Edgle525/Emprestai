package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityRegisterBinding;
import com.santalu.maskara.Mask;
import com.santalu.maskara.MaskChangedListener;
import com.santalu.maskara.MaskStyle;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Apply masks
        Mask cpfMask = new Mask("___.___.___-__", '_', MaskStyle.NORMAL);
        Mask phoneMask = new Mask("(__) _____-____", '_', MaskStyle.NORMAL);
        binding.cpf.addTextChangedListener(new MaskChangedListener(cpfMask));
        binding.phone.addTextChangedListener(new MaskChangedListener(phoneMask));

        binding.registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String fullName = binding.fullName.getText().toString().trim();
        String cpf = binding.cpf.getText().toString().trim();
        String phone = binding.phone.getText().toString().trim();
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String confirmPassword = binding.confirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(cpf) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if CPF is unique
        db.collection("users").whereEqualTo("cpf", cpf).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Toast.makeText(this, "CPF já cadastrado", Toast.LENGTH_SHORT).show();
                    } else {
                        // CPF is unique, proceed with registration
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, authTask -> {
                                    if (authTask.isSuccessful()) {
                                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                        if (firebaseUser != null) {
                                            saveUserToFirestore(firebaseUser.getUid(), fullName, email, cpf, phone);
                                        }
                                    } else {
                                        Toast.makeText(this, "Falha no cadastro: " + authTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    private void saveUserToFirestore(String uid, String fullName, String email, String cpf, String phone) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", fullName);
        user.put("email", email);
        user.put("cpf", cpf);
        user.put("phone", phone);
        user.put("rating", 0.0);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cadastro realizado com sucesso", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao salvar dados do usuário", Toast.LENGTH_SHORT).show());
    }
}
