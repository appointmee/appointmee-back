package com.gg.turnlook.features.imagen;

import com.gg.turnlook.common.exception.BadRequestException;
import com.gg.turnlook.common.exception.NotFoundException;
import com.gg.turnlook.features.imagen.dto.ImagenResponseDTO;
import com.gg.turnlook.features.imagen.dto.ImagenSubidaDTO;
import com.gg.turnlook.features.servicio.Servicio;
import com.gg.turnlook.features.sucursal.Sucursal;
import com.gg.turnlook.features.usuario.Usuario;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;





@Service
public class ImagenService {


    private final ImagenRepository imagenRepo;
    private final ImagenCloudinaryService cloudinaryService;


    public ImagenService(ImagenRepository imagenRepo, ImagenCloudinaryService cloudinaryService){
        this.imagenRepo = imagenRepo;
        this.cloudinaryService = cloudinaryService;
    }



    ///  METODOS



    public void crearImagenSucursal(Sucursal sucursal, String url){
        crearImagenSucursal(sucursal, url, null);
    }

    public void crearImagenSucursal(Sucursal sucursal, String url, String publicId){
        String imagenPublicId = validarPublicId(publicId);
        validarUrlParaPublicId(url, imagenPublicId);

        Imagen imagen = new Imagen();
        imagen.setSucursal(sucursal);
        imagen.setUrl(url);
        imagen.setPublicId(imagenPublicId);

        imagenRepo.save(imagen);
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
            try {
                cloudinaryService.eliminar(subida.publicId());
            } catch (RuntimeException limpieza) {
                error.addSuppressed(limpieza);
            }
            throw error;
        }
    }


    public Imagen crearFotoPerfil(String url){
        return crearFotoPerfil(url, null);
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
    public void cambiarFotoPerfilUsuario(Usuario usuario, String url){
        cambiarFotoPerfilUsuario(usuario, url, null);
    }

    @Transactional
    public void cambiarFotoPerfilUsuario(Usuario usuario, String url, String publicId){

        Imagen imagen = usuario.getFotoPerfil();

        if(imagen == null){
            imagen = crearFotoPerfil(url, publicId);
            usuario.setFotoPerfil(imagen);
            return;
        }

        reemplazarImagen(imagen, url, publicId);
    }


    @Transactional
    public void cambiarFotoPerfilSucursal(Sucursal sucursal, String url){
        cambiarFotoPerfilSucursal(sucursal, url, null);
    }

    @Transactional
    public void cambiarFotoPerfilSucursal(Sucursal sucursal, String url, String publicId){

        Imagen imagen = sucursal.getFotoPerfil();

        if(imagen == null){
            imagen = crearFotoPerfil(url, publicId);
            sucursal.setFotoPerfil(imagen);
            return;
        }

        reemplazarImagen(imagen, url, publicId);
    }


    @Transactional
    public void cambiarFotoPerfilServicio(Servicio servicio, String url){
        cambiarFotoPerfilServicio(servicio, url, null);
    }

    @Transactional
    public void cambiarFotoPerfilServicio(Servicio servicio, String url, String publicId){

        Imagen imagen = servicio.getFotoPerfil();

        if(imagen == null){
            imagen = crearFotoPerfil(url, publicId);
            servicio.setFotoPerfil(imagen);
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
                .orElseThrow(() -> new NotFoundException("No se encontró la imagen"));
    }


    // ver mas este
    @Transactional
    public void eliminarImagen(Imagen imagen){
        String publicId = imagen.getPublicId();
        imagenRepo.delete(imagen);
        imagenRepo.flush();
        eliminarArchivoSinUso(publicId);
    }

    private void reemplazarImagen(Imagen imagen, String url, String publicId) {
        String publicIdNuevo = validarPublicId(publicId);
        validarUrlParaPublicId(url, publicIdNuevo);
        String publicIdAnterior = imagen.getPublicId();

        if (publicIdNuevo == null && url.equals(imagen.getUrl())) {
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
        if (publicId != null) cloudinaryService.validarReferencia(url, publicId);
    }

}
