package com.gg.turnlook.Backend.Repository;

import com.gg.turnlook.Backend.Model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

}
