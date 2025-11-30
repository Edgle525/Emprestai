package br.edu.fatecgru.empresta;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.edu.fatecgru.empresta.databinding.ActivityEditProfileBinding;
import br.edu.fatecgru.empresta.network.AddressResponse;
import br.edu.fatecgru.empresta.network.ViaCepService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ViaCepService viaCepService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://viacep.com.br/ws/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        viaCepService = retrofit.create(ViaCepService.class);

        loadUserProfile();

        binding.searchCepButton.setOnClickListener(v -> searchCep());
        binding.saveProfileButton.setOnClickListener(v -> saveProfileChanges());

        binding.addressHeader.setOnClickListener(v -> {
            if (binding.addressContainer.getVisibility() == View.VISIBLE) {
                binding.addressContainer.setVisibility(View.GONE);
                binding.addressHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
            } else {
                binding.addressContainer.setVisibility(View.VISIBLE);
                binding.addressHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_up, 0);
            }
        });
    }

    private void searchCep() {
        String cep = binding.editCep.getText().toString().trim();
        if (cep.length() == 8) {
            viaCepService.getAddress(cep).enqueue(new Callback<AddressResponse>() {
                @Override
                public void onResponse(Call<AddressResponse> call, Response<AddressResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        AddressResponse address = response.body();
                        binding.editStreet.setText(address.getStreet());
                        binding.editNeighborhood.setText(address.getNeighborhood());
                        binding.editCity.setText(address.getCity());
                        binding.editState.setText(address.getState());
                    }
                }

                @Override
                public void onFailure(Call<AddressResponse> call, Throwable t) {
                    Toast.makeText(EditProfileActivity.this, "Erro ao buscar CEP", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "CEP inválido", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                binding.editFullName.setText(documentSnapshot.getString("name"));
                binding.editEmail.setText("E-mail: " + documentSnapshot.getString("email"));
                binding.editCpf.setText(documentSnapshot.getString("cpf"));
                binding.editPhone.setText(documentSnapshot.getString("phone"));

                // Load address fields
                binding.editCep.setText(documentSnapshot.getString("cep"));
                binding.editStreet.setText(documentSnapshot.getString("street"));
                binding.editNumber.setText(documentSnapshot.getString("number"));
                binding.editNeighborhood.setText(documentSnapshot.getString("neighborhood"));
                binding.editCity.setText(documentSnapshot.getString("city"));
                binding.editState.setText(documentSnapshot.getString("state"));
            }
        });
    }

    private void saveProfileChanges() {
        // ... (existing validation)

        String cep = binding.editCep.getText().toString().trim();
        String street = binding.editStreet.getText().toString().trim();
        String number = binding.editNumber.getText().toString().trim();
        String neighborhood = binding.editNeighborhood.getText().toString().trim();
        String city = binding.editCity.getText().toString().trim();
        String state = binding.editState.getText().toString().trim();

        if (TextUtils.isEmpty(cep) || TextUtils.isEmpty(street) || TextUtils.isEmpty(number) || TextUtils.isEmpty(neighborhood) || TextUtils.isEmpty(city) || TextUtils.isEmpty(state)) {
            Toast.makeText(this, "Preencha o endereço completo", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Geocode the address to get coordinates
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(String.format("%s, %s - %s, %s", street, number, city, state), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                GeoPoint geoPoint = new GeoPoint(latitude, longitude);

                // Create a geohash for the location
                String geohash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(latitude, longitude));

                // Save all data, including GeoPoint and geohash
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", binding.editFullName.getText().toString().trim());
                updates.put("cpf", binding.editCpf.getText().toString().trim());
                updates.put("phone", binding.editPhone.getText().toString().trim());
                updates.put("cep", cep);
                updates.put("street", street);
                updates.put("number", number);
                updates.put("neighborhood", neighborhood);
                updates.put("city", city);
                updates.put("state", state);
                updates.put("geohash", geohash);
                updates.put("location", geoPoint); // Keep the GeoPoint for other uses

                db.collection("users").document(user.getUid()).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Falha ao atualizar o perfil", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Endereço não encontrado. Verifique os dados.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("EditProfileActivity", "Geocoding failed", e);
            Toast.makeText(this, "Erro ao processar a localização.", Toast.LENGTH_SHORT).show();
        }
    }
}
