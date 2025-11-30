package br.edu.fatecgru.empresta.network;

import com.google.gson.annotations.SerializedName;

public class AddressResponse {

    @SerializedName("logradouro")
    private String street;

    @SerializedName("bairro")
    private String neighborhood;

    @SerializedName("localidade")
    private String city;

    @SerializedName("uf")
    private String state;

    public String getStreet() {
        return street;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }
}
