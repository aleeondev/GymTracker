package com.gymtracker.gymtracker.config;

import java.sql.SQLException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/*
 * Explica en español por qué no arranca la app cuando falla la conexión
 * con la base de datos (Supabase pausado, contraseña mal, sin internet...).
 *
 * Cómo funciona: si Spring Boot no consigue arrancar, recorre el error
 * buscando un "FailureAnalyzer" que lo entienda. Este se ocupa de los
 * errores de SQL (SQLException) de conexión o de usuario/contraseña
 * y, en lugar del larguísimo stack trace, imprime un resumen con qué ha
 * pasado y qué hacer.
 *
 * Usamos SQLException (viene con Java) y no PSQLException (la del driver de
 * Postgres) porque en el pom.xml el driver tiene scope "runtime": existe al
 * ejecutar la app, pero no al compilar. PSQLException hereda de SQLException,
 * así que la atrapamos igual.
 *
 * Se registra en src/main/resources/META-INF/spring.factories.
 */
public class BaseDeDatosNoDisponibleAnalyzer extends AbstractFailureAnalyzer<SQLException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SQLException causa) {
        if (!esErrorDeConexion(causa)) {
            return null; // otro tipo de error de SQL: que lo explique Spring como siempre
        }

        String descripcion = """
            No se ha podido conectar con la base de datos (Supabase).
            Mensaje de Postgres: %s""".formatted(causa.getMessage());

        String accion = """
            Revisa, en este orden:
              1. ¿El proyecto de Supabase está pausado? Entra en supabase.com, abre el proyecto
                 y pulsa "Restore project". (El plan gratis lo pausa si no se usa unos días.)
                 Pista: el mensaje "tenant/user ... not found" suele significar esto.
              2. ¿Son correctos DB_URL, DB_USERNAME y DB_PASSWORD en application-local.properties?
                 Cópialos de Supabase: botón "Connect" > "Session pooler".
              3. ¿Tienes conexión a internet?
            Cuando esté arreglado, vuelve a arrancar la app.""";

        return new FailureAnalysis(descripcion, accion, causa);
    }

    /**
     * Códigos SQLState de Postgres: los que empiezan por 08 son de conexión y
     * los que empiezan por 28, de usuario o contraseña. El pooler de Supabase
     * responde "tenant/user not found" (proyecto pausado o borrado) con otro código,
     * así que también lo reconocemos por el texto.
     */
    private static boolean esErrorDeConexion(SQLException e) {
        String estado = e.getSQLState() != null ? e.getSQLState() : "";
        String mensaje = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return estado.startsWith("08") || estado.startsWith("28") || mensaje.contains("tenant");
    }
}
