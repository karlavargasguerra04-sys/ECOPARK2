package cl.duoc.ecopark.dao;

import cl.duoc.ecopark.model.Estadia;
import cl.duoc.ecopark.model.EstadiaAuto;
import cl.duoc.ecopark.model.EstadiaMoto;
import cl.duoc.ecopark.model.ResumenTipo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;


public class MySqlEstadiaDAO implements EstadiaDAO {

    private static final Path CONFIG = Path.of("config", "db.properties");
    private static final int TIEMPO_CONSULTA_SEGUNDOS = 10;

    private Connection conectar() throws IOException, SQLException {
        Properties propiedades = new Properties();

        // Lee URL, usuario y contraseña desde config/db.properties.
        try (InputStream entrada = Files.newInputStream(CONFIG)) {
            propiedades.load(entrada);
        } catch (IOException e) {
            throw new IOException("No se pudo leer " + CONFIG
                    + ". Revisa la carpeta de trabajo del proyecto.", e);
        }

        String url = propiedades.getProperty("db.url", "").trim();
        String usuario = propiedades.getProperty("db.user", "").trim();
        String clave = propiedades.getProperty("db.password", "");

        if (!url.startsWith("jdbc:mysql://") || url.contains("NOMBRE_DE_TU_BD")) {
            throw new IOException("Configura db.url con el nombre real de la BD en " + CONFIG);
        }
        if (usuario.isBlank()) {
            throw new IOException("Configura db.user en " + CONFIG);
        }

        Properties conexionConfig = new Properties();
        conexionConfig.setProperty("user", usuario);
        conexionConfig.setProperty("password", clave);
        conexionConfig.setProperty("connectTimeout", "5000");
        conexionConfig.setProperty("socketTimeout", "10000");

        Connection conexion = DriverManager.getConnection(url, conexionConfig);

        if (!conexion.isValid(3)) {
            conexion.close();
            throw new SQLException("No se pudo conectar");
        }
        return conexion;
    }

    private Estadia convertirFila(ResultSet rs) throws SQLException, IOException {
        long id =  rs.getLong("id");
        String patente = rs.getString("patente");
        String tipo = rs.getString("tipo");
        int horas = rs.getInt("horas");
        boolean convenio = rs.getBoolean("convenio");

        try {
            Estadia estadia;
            if("Auto".equals(tipo)) {
                estadia = new EstadiaAuto(patente, horas);
            }else if("Moto".equals(tipo)) {
                estadia = new EstadiaMoto(patente, horas, convenio);
            }else{
                throw new IOException("Tipo de estadia incorrecto:" + tipo);
            }
            estadia.setId(id);
            return estadia;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de datos incorrecto:");
        }
    }

    @Override
    public List<Estadia> cargar() throws IOException {
        List<Estadia> lista = new ArrayList<>();
        String sql = "SELECT id, patente, tipo, horas, convenio FROM estadias ORDER BY patente";

        try (Connection conexion = conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)){

             ps.setQueryTimeout(TIEMPO_CONSULTA_SEGUNDOS);
             try(ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    lista.add(convertirFila(rs));
                }
             }
        } catch (SQLException e) {
            throw new IOException("No fue posible consultar la tabla estadias en MySQL: "
                    + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void registrar(Estadia estadia) throws IOException {
        if (estadia == null) {
            throw new IllegalArgumentException("La estadía no puede ser nula.");
        }

        String sql = "INSERT INTO estadias (patente, tipo, horas, convenio) VALUES (?, ?, ?, ?)";

        try (Connection conexion = conectar()) {
            conexion.setAutoCommit(false);
            try(PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setQueryTimeout(TIEMPO_CONSULTA_SEGUNDOS);
                ps.setString(1, estadia.getPatente());
                ps.setString(2, estadia.getTipo());
                ps.setInt(3, estadia.getHoras());
                boolean convenio = estadia instanceof EstadiaMoto moto && moto.isConvenio();
                ps.setBoolean(4, convenio);

                if (ps.executeUpdate() != 1) {
                    throw new IOException("MySQL no confirmó el registro de la estadía.");
                }

                long idNuevo;
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException("Mysql no devolvio ID generada");
                    }
                    idNuevo = claves.getLong(1);
                    if(idNuevo <= 0){
                        throw new SQLException("Mysql  devolvio un ID incorrecto");
                    }
                }
                conexion.commit();
                estadia.setId(idNuevo);
            }catch (SQLException e) {
                try{
                    conexion.rollback();
                }catch (SQLException rollBackError) {
                    e.addSuppressed(rollBackError);
                }
                //throw e;

            }
        } catch (SQLIntegrityConstraintViolationException e) {

            throw new IOException("La patente " + estadia.getPatente()
                    + " ya existe o no cumple las restricciones de MySQL.", e);
        } catch (SQLException e) {
            throw new IOException("No se pudo registrar en MySQL: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(long id) throws IOException {
        String sql = "DELETE FROM estadias WHERE patente = ?";
        //String sql = "DELETE FROM estadias WHERE id = ?";

        try (Connection conexion = conectar();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setQueryTimeout(TIEMPO_CONSULTA_SEGUNDOS);

            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new IOException("La patente ya no se encuentra registrada en MySQL.");
                //throw new IOException("La estadia ya no se encuentra registrada en MySQL.");
            }
        } catch (SQLException e) {
            throw new IOException("No se pudo eliminar en MySQL: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarHoras(long id, int horas) throws IOException{
        if( id <= 0){
            throw new IllegalArgumentException("Id no valido");
        }
        if(horas < 1 || horas > 24){
            throw new IllegalArgumentException("Ingreso de Horas no valido");
        }
        String sql = "UPDATE estadias SET horas = ? WHERE id = ?";
        try(Connection conexion = conectar();
            PreparedStatement ps = conexion.prepareStatement(sql)){
            ps.setQueryTimeout(TIEMPO_CONSULTA_SEGUNDOS);
            ps.setInt(1, horas);
            ps.setLong(2, id);
            if(ps.executeUpdate() != 1){
                throw new IOException("No existe el dato");
            }
        }catch (SQLException e) {
            throw new IOException("No se pudo actualizar estadias en MySQL: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Estadia> buscarPorPatente(String patente) throws IOException {
        if (patente == null || patente.isBlank()) {
            throw new IllegalArgumentException("Patente no valido");
        }
        String sql = "SELECT id, patente, tipo, horas, convenio " +
                "FROM estadias " +
                "WHERE patente = ?";
        //filtro
        String criterio = patente.trim().toUpperCase(Locale.ROOT);

        //conectamos a la DB
        try (Connection conexion = conectar();
        PreparedStatement ps = conexion.prepareStatement(sql)){
            ps.setQueryTimeout(TIEMPO_CONSULTA_SEGUNDOS);
            ps.setString(1, criterio);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) {
                    return Optional.of(convertirFila(rs));
                }
                return Optional.empty();
            }

        }
        catch (SQLException e) {
            throw new IOException("No se pudo buscar estadias en MySQL: " + e.getMessage(), e);
        }
    }


    @Override
    public List<ResumenTipo> resumenPorTipo(int cantidadMinima) throws IOException {
        if (cantidadMinima <= 0) {
            throw new IllegalArgumentException("Cantidad Minima no valido");
        }
        String sql = "SELECT tipo, COUNT(*) AS cantidad, SUM(horas) AS horas_acumuladas " +
                "FROM estadias " +
                "GROUP BY tipo " +
                "HAVING COUNT(*) >= ? " +
                "ORDER BY tipo";
        List<ResumenTipo> resultado = new ArrayList<>();

        try(Connection conexion = conectar();
        PreparedStatement ps = conexion.prepareStatement(sql)){
            ps.setQueryTimeout(TIEMPO_CONSULTA_SEGUNDOS);
            ps.setInt(1, cantidadMinima);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new ResumenTipo(
                            rs.getString("tipo"),
                            rs.getLong("cantidad"),
                            rs.getLong("horas_acumuladas")
                    ));
                }
            }
        }catch (SQLException e) {
            throw new IOException("No se pudo buscar estadias en MySQL: " + e.getMessage(), e);
        }
        return resultado;
    }
}
