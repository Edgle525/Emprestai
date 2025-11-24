package br.edu.fatecgru.empresta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Google Sign-In result received. Result Code: " + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "Result is OK. Attempting to get signed in account.");
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            Log.d(TAG, "Google account obtained successfully. ID Token: " + (account.getIdToken() != null ? "present" : "null"));
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Log.w(TAG, "GoogleSignInAccount is null.");
                            Toast.makeText(this, "Não foi possível obter a conta do Google.", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception during Google sign in.", e);
                        Toast.makeText(this, "Falha no login com Google", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                } else {
                    Log.w(TAG, "Google Sign-In was cancelled or failed. Result Code: " + result.getResultCode());
                    showLoading(false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        binding.googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Log.d(TAG, "Initiating Google Sign-In flow.");
        showLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Authenticating with Firebase using Google ID Token.");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase credential sign in successful.");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                            Log.d(TAG, "New user detected. Saving to Firestore.");
                            saveNewUserInFirestore(user);
                        } else {
                            Log.d(TAG, "Existing user. Updating UI.");
                            updateUI(user);
                        }
                    } else {
                        Log.e(TAG, "Firebase credential sign in failed.", task.getException());
                        Toast.makeText(this, "Falha na autenticação com Firebase.", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void saveNewUserInFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        Map<String, Object> user = new HashMap<>();
        user.put("name", firebaseUser.getDisplayName());
        user.put("email", firebaseUser.getEmail());
        user.put("rating", 0.0);

        Log.d(TAG, "Saving user data to Firestore for UID: " + firebaseUser.getUid());
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore save successful. Redirecting to EditProfileActivity.");
                    Toast.makeText(this, "Complete seu cadastro para continuar", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(LoginActivity.this, EditProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data to Firestore.", e);
                    Toast.makeText(LoginActivity.this, "Erro ao salvar dados.", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void loginUser() {
        String email = binding.email.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Preencha e-mail e senha.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        updateUI(mAuth.getCurrentUser());
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Falha na autenticação.", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        showLoading(false);
        if (user != null) {
            Log.d(TAG, "Updating UI and navigating to MainActivity.");
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!isLoading);
        binding.registerButton.setEnabled(!isLoading);
        binding.googleSignInButton.setEnabled(!isLoading);
        binding.email.setEnabled(!isLoading);
        binding.password.setEnabled(!isLoading);
    }
}
