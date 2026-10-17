package com.gymtracker.gymtracker.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gymtracker.gymtracker.model.Perfil;

/** La clave del perfil es el id del usuario: findById(usuarioId) devuelve el suyo. */
public interface PerfilRepository extends JpaRepository<Perfil, UUID> {

    /** ¿Lo usa ya OTRA persona? (el propio usuario puede volver a guardar el suyo) */
    boolean existsByNombreUsuarioAndUsuarioIdNot(String nombreUsuario, UUID usuarioId);

    /** ¿Lo usa alguien? (para el registro, cuando aún no hay usuario) */
    boolean existsByNombreUsuario(String nombreUsuario);
}
