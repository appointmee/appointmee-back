package com.gg.turnlook.Backend.Service;


import com.gg.turnlook.Backend.DTO.Imagen.ImagenResponseDTO;
import com.gg.turnlook.Backend.DTO.Imagen.ImagenSubidaDTO;
import com.gg.turnlook.Backend.Enum.ImagenCloudinaryTipo;
import com.gg.turnlook.Backend.Excepciones.BadRequestException;
import com.gg.turnlook.Backend.Excepciones.NotFoundException;
import com.gg.turnlook.Backend.Model.Imagen;
import com.gg.turnlook.Backend.Model.Servicio;
import com.gg.turnlook.Backend.Model.Sucursal;
import com.gg.turnlook.Backend.Model.Usuario;
import com.gg.turnlook.Backend.Repository.ImagenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
public class ImagenService {


    private final ImagenRepository imagenRepo;
    private final ImagenCloudinaryService cloudinaryService;


    public ImagenService(ImagenRepository imagenRepo, ImagenCloudinaryService cloudinaryService){
        this.imagenRepo = imagenRepo;
        this.cloudinaryService = cloudinaryService;

    }


    ///  METODOS


    public void crearImagenSucursal(Sucursal sucursal, String url, String publicId){

        String imagenPublicId = validarPublicId(publicId);
        validarUrlParaPublicId(url, imagenPublicId);

        Imagen imagen = new Imagen();
        imagen.setSucursal(sucursal);
        imagen.setUrl(url);
        imagen.setPublicId(imagenPublicId);

        imagenRepo.save(imagen);
    }


    public void crearImagenSucursal(Sucursal sucursal, String url){
        crearImagenSucursal(sucursal, url, null);
    }


    public ImagenResponseDTO subirImagenSucursal(Sucursal sucursal, MultipartFile archivo){

        ImagenSubidaDTO subida = cloudinaryService.subir(archivo, ImagenCloudinaryTipo.GALERIA);

        try {
            Imagen imagen = new Imagen();
            imagen.setSucursal(sucursal);
            imagen.setUrl(subida.url());
            imagen.setPublicId(subida.publicId());

            imagenRepo.saveAndFlush(imagen);

            return new ImagenResponseDTO(imagen.getId(), imagen.getFotoValida());
        } catch (RuntimeException error) {
            try { cloudinaryService.eliminar(subida.publicId()); }
            catch (RuntimeException limpieza) { error.addSuppressed(limpieza); }
            throw error;
        }
    }


    public Imagen crearFotoPerfil(String url, String publicId){
        String imagenPublicId = validarPublicId(publicId);
        validarUrlParaPublicId(url, imagenPublicId);

        Imagen imagen = new Imagen();

        if(url != null && !url.isBlank()){
            imagen.setUrl(url);
        }
        imagen.setPublicId(imagenPublicId);

        return imagenRepo.save(imagen);
    }


    public Imagen crearFotoPerfil(String url){
        return crearFotoPerfil(url, null);
    }


    @Transactional
    public void eliminarFotoPerfilUsuario(Usuario usuario){

        Imagen imagen = usuario.getFotoPerfil();

        if(imagen == null) return;

        String publicIdAnterior = imagen.getPublicId();
        imagen.setUrl(null);
        imagen.setPublicId(null);

        imagenRepo.saveAndFlush(imagen);
        eliminarArchivoSinUso(publicIdAnterior);
    }


    @Transactional
    public void cambiarFotoPerfilUsuario(Usuario usuario, String url, String publicId){

        Imagen imagen = usuario.getFotoPerfil();

        if(imagen == null){
            Imagen nuevaImagen = crearFotoPerfil(url, publicId);
            usuario.setFotoPerfil(nuevaImagen);
            return;
        }

        reemplazarImagen(imagen, url, publicId);
    }


    @Transactional
    public void cambiarFotoPerfilSucursal(Sucursal sucursal, String url, String publicId){

        Imagen imagen = sucursal.getFotoPerfil();

        if(imagen == null){
            Imagen nuevaImagen = crearFotoPerfil(url, publicId);
            sucursal.setFotoPerfil(nuevaImagen);
            return;
        }

        reemplazarImagen(imagen, url, publicId);
    }


    @Transactional
    public void cambiarFotoPerfilServicio(Servicio servicio, String url, String publicId){

        Imagen imagen = servicio.getFotoPerfil();

        if(imagen == null){
            Imagen nuevaImagen = crearFotoPerfil(url, publicId);
            servicio.setFotoPerfil(nuevaImagen);
            return;
        }

        reemplazarImagen(imagen, url, publicId);
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
                .orElseThrow(() -> new NotFoundException("No se encontro la imagen"));
    }


    @Transactional
    public void eliminarImagen(Imagen imagen){

        String publicId = imagen.getPublicId();

        imagenRepo.delete(imagen);
        imagenRepo.flush();

        eliminarArchivoSinUso(publicId);
    }



    ///  METODOS DE IMAGEN (CLOUDINARY)


    private void reemplazarImagen(Imagen imagen, String url, String publicId) {

        String publicIdNuevo = validarPublicId(publicId);
        validarUrlParaPublicId(url, publicIdNuevo);

        String publicIdAnterior = imagen.getPublicId();

        if (publicIdNuevo == null && url != null && url.equals(imagen.getUrl())) {
            publicIdNuevo = publicIdAnterior;
        }

        imagen.setUrl(url);
        imagen.setPublicId(publicIdNuevo);

        imagenRepo.saveAndFlush(imagen);

        if (publicIdAnterior != null && !publicIdAnterior.equals(publicIdNuevo)) {
            eliminarArchivoSinUso(publicIdAnterior);
        }
    }


    private void eliminarArchivoSinUso(String publicId) {

        if (publicId != null && !publicId.isBlank()
                && imagenRepo.contarEnUsoPorPublicId(publicId) == 0) {
            cloudinaryService.eliminar(publicId);
        }
    }


    private String validarPublicId(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            return null;
        }

        if (!cloudinaryService.esImagenPropia(publicId)) {
            throw new BadRequestException("El public_id no corresponde a una imagen de AppointMee");
        }

        return publicId;
    }


    private void validarUrlParaPublicId(String url, String publicId) {

        if (url != null && url.length() > 1000) {
            throw new BadRequestException("La URL de la imagen no puede superar los 1000 caracteres");
        }

        if (publicId != null && (url == null || url.isBlank())) {
            throw new BadRequestException("La URL de la imagen es obligatoria junto con su public_id");
        }

        if (publicId != null) {
            cloudinaryService.validarReferencia(url, publicId);
        }
    }


}
