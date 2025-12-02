package br.edu.fatecgru.empresta;

public class User {
    private String uid;
    private String name;
    private String photoUrl;
    private Double rating;
    private String address;

    public User() {}

    public User(String uid, String name, String photoUrl) {
        this.uid = uid;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
