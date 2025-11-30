package br.edu.fatecgru.empresta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.edu.fatecgru.empresta.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FragmentHomeBinding binding;
    private ToolsAdapter adapter;
    private final List<Tool> fullToolList = new ArrayList<>();
    private final List<Tool> filteredToolList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "Todas";
    private int currentDistanceFilter = 0; // 0 for all distances

    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupCategorySpinner();
        setupDistanceSpinner();
        setupSearchView();
        checkLocationPermission();
    }

    private void setupRecyclerView() {
        adapter = new ToolsAdapter(getContext(), filteredToolList);
        binding.toolsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.toolsRecyclerView.setAdapter(adapter);
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>(Arrays.asList("Todas", "Elétrica", "Marcenaria", "Jardinagem", "Hidráulica", "Mecânica", "Outros"));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(spinnerAdapter);

        binding.categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = parent.getItemAtPosition(position).toString();
                filterTools();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupDistanceSpinner() {
        List<String> distances = new ArrayList<>(Arrays.asList("Qualquer distância", "Até 1km", "Até 5km", "Até 10km", "Até 25km", "Até 50km"));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, distances);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.distanceSpinner.setAdapter(spinnerAdapter);

        binding.distanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentDistanceFilter = 0;
                        break;
                    case 1:
                        currentDistanceFilter = 1;
                        break;
                    case 2:
                        currentDistanceFilter = 5;
                        break;
                    case 3:
                        currentDistanceFilter = 10;
                        break;
                    case 4:
                        currentDistanceFilter = 25;
                        break;
                    case 5:
                        currentDistanceFilter = 50;
                        break;
                }
                filterTools();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterTools();
                return true;
            }
        });
    }

    private void loadTools() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return; // User not logged in
        }
        String currentUserId = currentUser.getUid();

        db.collection("tools").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fullToolList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Tool tool = document.toObject(Tool.class);
                            tool.setId(document.getId());

                            if (tool.getOwnerId() != null && !tool.getOwnerId().equals(currentUserId)) {
                                fullToolList.add(tool);
                            }
                        }
                        filterTools(); // Initial filter
                    }
                });
    }

    private void filterTools() {
        filteredToolList.clear();
        for (Tool tool : fullToolList) {
            boolean categoryMatches = currentCategoryFilter.equals("Todas") || (tool.getCategories() != null && tool.getCategories().contains(currentCategoryFilter));
            boolean nameMatches = tool.getName().toLowerCase().contains(currentSearchQuery.toLowerCase());
            boolean distanceMatches = true;

            if (currentDistanceFilter > 0 && lastLocation != null && tool.getLocation() != null) {
                Location toolLocation = new Location("");
                toolLocation.setLatitude(tool.getLocation().getLatitude());
                toolLocation.setLongitude(tool.getLocation().getLongitude());
                float distance = lastLocation.distanceTo(toolLocation) / 1000; // in kilometers
                distanceMatches = distance <= currentDistanceFilter;
            }

            if (categoryMatches && nameMatches && distanceMatches) {
                filteredToolList.add(tool);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                lastLocation = location;
                loadTools();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(getContext(), "A permissão de localização é necessária para o filtro de distância", Toast.LENGTH_SHORT).show();
                loadTools();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
