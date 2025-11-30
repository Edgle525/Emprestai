package br.edu.fatecgru.empresta.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ViaCepService {
    @GET("{cep}/json/")
    Call<AddressResponse> getAddress(@Path("cep") String cep);
}
