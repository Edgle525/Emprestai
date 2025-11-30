package br.edu.fatecgru.empresta;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.List;

public class Tool implements Serializable {
    private String id;
    private String name;
    private String brand;
    private String description;
    private List<String> categories;
    private List<String> imageUrls;
    private String ownerId;
    private boolean available;
    private String geohash;
    private GeoPoint location;

    // Firestore precisa de um construtor vazio
    public Tool() {}

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }
}
