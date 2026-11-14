-- ==========================================================
-- V2 - Índices
--
-- Un índice es como el índice alfabético de un libro: Postgres va directo
-- a las filas que busca en vez de leer la tabla entera.
-- Postgres NO crea índices solo en las claves foráneas (rutina_id,
-- entrenamiento_id...) ni en usuario_id, y casi todas nuestras consultas
-- filtran por ahí ("los entrenos de ESTE usuario").
--
-- "if not exists": si alguno ya existiera, no da error.
-- Nombres: idx_<tabla>_<columnas>.
-- ==========================================================

-- "Mis ejercicios" (los comunes tienen usuario_id vacío)
create index if not exists idx_ejercicios_usuario on ejercicios (usuario_id);

-- "Mis rutinas"
create index if not exists idx_rutinas_usuario on rutinas (usuario_id);

-- Ejercicios de una rutina, y saber en qué rutinas sale un ejercicio
create index if not exists idx_rutina_ejercicios_rutina    on rutina_ejercicios (rutina_id);
create index if not exists idx_rutina_ejercicios_ejercicio on rutina_ejercicios (ejercicio_id);

-- Historial, panel y gráficas: "entrenos de este usuario, por fecha".
-- Índice de DOS columnas: sirve para filtrar por usuario y, dentro de él,
-- ya están ordenados por fecha (lo que pide el historial).
create index if not exists idx_entrenamientos_usuario_fecha on entrenamientos (usuario_id, fecha desc);
create index if not exists idx_entrenamientos_rutina        on entrenamientos (rutina_id);

-- Series de un entreno (cada vez que abres uno) y progreso de un ejercicio
create index if not exists idx_series_entrenamiento     on series_entrenamiento (entrenamiento_id);
create index if not exists idx_series_ejercicio         on series_entrenamiento (ejercicio_id);

-- "¿Este usuario ya ha opinado?"
create index if not exists idx_opiniones_usuario on opiniones (usuario_id);

-- No hacen falta:
--   perfiles: usuario_id ya es la clave primaria (tiene índice propio).
--   pesos_corporales: el "unique (usuario_id, fecha)" ya crea un índice.
