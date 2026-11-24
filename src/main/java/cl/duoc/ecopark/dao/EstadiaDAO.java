package cl.duoc.ecopark.dao;

import cl.duoc.ecopark.model.Estadia;
import cl.duoc.ecopark.model.ResumenTipo;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * DAO = Data Access Object: define las operaciones del almacenamiento.
 * CAMBIO: JSON guardaba toda la lista; MySQL permite operar fila a fila.
 */
public interface EstadiaDAO {

    // SELECT: recupera los registros de la tabla.
    List<Estadia> cargar() throws IOException;

    // INSERT: guarda una estadía.
    void registrar(Estadia estadia) throws IOException;

    // DELETE: quita la fila que tiene la patente indicada.
    void eliminar(long id) throws IOException;

    void actualizarHoras(long id, int horas) throws IOException;

    Optional<Estadia> buscarPorPatente(String patente) throws IOException;

    List<ResumenTipo> resumenPorTipo(int cantidadMinima) throws IOException;
}
