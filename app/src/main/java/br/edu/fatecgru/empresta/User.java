package br.edu.fatecgru.empresta;

import com.google.firebase.firestore.Exclude;

public class User {
    private String uid;
    private String name;
    private String email;
    private String cpf;
    private String phone;
    private String photoUrl;
    private Double rating;
    private String secondaryEmail;
    private String cep;
    private String street;
    private String number;
    private String neighborhood;
    private String city;
    private String state;

    public User() {}

    @Exclude
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getSecondaryEmail() { return secondaryEmail; }
    public void setSecondaryEmail(String secondaryEmail) { this.secondaryEmail = secondaryEmail; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
