package com.gg.turnlook.Backend.Service;


import com.gg.turnlook.Backend.DTO.Imagen.ImagenResponseDTO;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.Imagen;
import com.gg.turnlook.Backend.Model.Servicio;
import com.gg.turnlook.Backend.Model.Sucursal;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Repository.ImagenRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ImagenService {


    private final ImagenRepository imagenRepo;


    public ImagenService(ImagenRepository imagenRepo){
        this.imagenRepo = imagenRepo;

    }



    ///  METODOS



    public void crearImagenSucursal(Sucursal sucursal, String url){

        Imagen imagen = new Imagen();
        imagen.setSucursal(sucursal);
        imagen.setUrl(url);

        imagenRepo.save(imagen);
    }


    public Imagen crearFotoPerfil(String url){

        Imagen imagen = new Imagen();

        if(url != null && !url.isBlank()){
            imagen.setUrl(url);
        }

        return imagenRepo.save(imagen);
    }


    public void eliminarFotoPerfilUsuario(Usuario usuario){

        Imagen imagen = usuario.getFotoPerfil();

        if(imagen == null) return;

        imagen.setUrl(null);

        imagenRepo.save(imagen);
    }


    public void cambiarFotoPerfilUsuario(Usuario usuario, String url){

        Imagen imagen = usuario.getFotoPerfil();

        if(imagen == null){
            imagen = crearFotoPerfil(url);
            usuario.setFotoPerfil(imagen);
            return;
        }

        imagen.setUrl(url);
        imagenRepo.save(imagen);
    }


    public void cambiarFotoPerfilSucursal(Sucursal sucursal, String url){

        Imagen imagen = sucursal.getFotoPerfil();

        if(imagen == null){
            imagen = crearFotoPerfil(url);
            sucursal.setFotoPerfil(imagen);
            return;
        }

        imagen.setUrl(url);
        imagenRepo.save(imagen);
    }


    public void cambiarFotoPerfilServicio(Servicio servicio, String url){

        Imagen imagen = servicio.getFotoPerfil();

        if(imagen == null){
            imagen = crearFotoPerfil(url);
            servicio.setFotoPerfil(imagen);
            return;
        }

        imagen.setUrl(url);
        imagenRepo.save(imagen);
    }


    public List<ImagenResponseDTO> listarImagenesPorSucursal(Sucursal sucursal){

        return imagenRepo.findBySucursal(sucursal).stream()
                .map(img -> new ImagenResponseDTO(img.getId(), img.getFotoValida()))
                .toList();
    }


    public long cuantasImagenesPorSucursal(Sucursal sucursal){

        return imagenRepo.countBySucursal(sucursal);
    }


    public Imagen listarImagenPorId(Integer imagenId){

        return imagenRepo.findById(imagenId)
                .orElseThrow(() -> new NotFoundException("No se encontró la imagen"));
    }


    // ver mas este
    public void eliminarImagen(Imagen imagen){

        imagenRepo.delete(imagen);
    }

}
