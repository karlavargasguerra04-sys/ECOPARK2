package cl.duoc.ecopark.model;

/**
 * Clase abstracta: contiene los atributos comunes de toda estadía.
 * Cada subtipo define su propio cálculo mediante calcularCosto().
 */
public abstract class Estadia {

    private long id;

    // Encapsulamiento: datos comunes protegidos por getters/setters.
    private String patente;
    private int horas;

    protected Estadia(String patente, int horas) {
        setPatente(patente);
        setHoras(horas);
    }

    public long getId(){
        return id;
    }

    public void setId(long id){
        if(id < 0){
            throw new IllegalArgumentException("Id invalido");
        }
        this.id = id;
    }

    public String getPatente() {
        return patente;
    }

    public void setPatente(String patente) {
        if (patente == null || patente.trim().isEmpty()) {
            throw new IllegalArgumentException("La patente no puede estar vacía.");
        }
        this.patente = patente.trim().toUpperCase();
    }

    public int getHoras() {
        return horas;
    }

    public void setHoras(int horas) {
        if (horas < 1 || horas > 24) {
            throw new IllegalArgumentException("Las horas deben estar entre 1 y 24.");
        }
        this.horas = horas;
    }

    // Polimorfismo: cada subtipo calcula su costo de manera distinta.
    public abstract int calcularCosto();

    // Permite mostrar el tipo en la tabla sin comprobar clases concretas.
    public abstract String getTipo();

    @Override
    public String toString() {
        return '#' + getId() + '|' + getTipo() + " | " + patente + " | " + horas + " hora(s)";
    }
}
