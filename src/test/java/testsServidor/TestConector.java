package testsServidor;

import org.junit.Assert;
import org.junit.Test;

import dominio.Inventario;
import dominio.Item;
import mensajeria.PaquetePersonaje;
import mensajeria.PaqueteUsuario;
import servidor.Conector;
import servidor.Servidor;

public class TestConector {

  @Test
  public void testConexionConLaDB() {
    new Servidor();
    Servidor.main(null);

    Conector conector = new Conector();
    conector.connect();

    // Pasado este punto la conexi�n con la base de datos result� exitosa
    conector.close();
    Assert.assertEquals(1, 1);
  }

  @Test
  public void testRegistrarUsuario() {
    new Servidor();
    Servidor.main(null);

    Conector conector = new Conector();
    conector.connect();

    PaqueteUsuario pu = new PaqueteUsuario();
    pu.setUsername("UserTest");
    pu.setPassword("test");
    // pu.setIdPj(20);
    conector.registrarUsuario(pu);

    pu = conector.getUsuario("UserTest");
    conector.close();
    Assert.assertEquals("UserTest", pu.getUsername());
  }

  @Test
  public void testRegistrarPersonaje() {
    new Servidor();
    Servidor.main(null);

    Conector conector = new Conector();
    conector.connect();

    PaquetePersonaje pp = new PaquetePersonaje();
    pp.setCasta("Asesino");
    pp.setDestreza(1);
    pp.setEnergiaTope(1);
    pp.setExperiencia(1);
    pp.setFuerza(1);
    pp.setInteligencia(1);
    pp.setNivel(1);
    pp.setNombre("PjTest32");
    pp.setRaza("Humano");
    pp.setSaludTope(1);
    Inventario inv = new Inventario();
    inv.añadir(new Item(1,2));
    inv.añadir(new Item(1,2));
    System.out.println("fueradeladv "+inv.getMochila());
    pp.setInv(inv);
    PaqueteUsuario pu = new PaqueteUsuario();
    pu.setUsername("333");
    pu.setPassword("333");
    conector.registrarUsuario(pu);
    conector.registrarPersonaje(pp, pu);
    conector.actualizarPersonaje(pp);
    pp = conector.getPersonaje(pu);
    conector.close();
    Assert.assertEquals("PjTest32", pp.getNombre());
  }

  @Test
  public void testLoginUsuario() {
    new Servidor();
    Servidor.main(null);

    Conector conector = new Conector();
    conector.connect();

    PaqueteUsuario pu = new PaqueteUsuario();
    pu.setUsername("UserTest");
    pu.setPassword("test");

    conector.registrarUsuario(pu);

    boolean resultadoLogin = conector.loguearUsuario(pu);
    conector.close();
    Assert.assertEquals(true, resultadoLogin);
  }

  @Test
  public void testLoginUsuarioFallido() {
    new Servidor();
    Servidor.main(null);

    Conector conector = new Conector();
    conector.connect();

    PaqueteUsuario pu = new PaqueteUsuario();
    pu.setUsername("userInventado");
    pu.setPassword("test");

    boolean resultadoLogin = conector.loguearUsuario(pu);
    conector.close();
    Assert.assertEquals(false, resultadoLogin);
  }

  @Test
  public void testGetItem() {
    new Servidor();
    Servidor.main(null);
    Conector conector = new Conector();
    conector.connect();
    System.out.println(conector.getItem(2));
    conector.close();
  }

  @Test
  public void getPersonajeTest() {
    new Servidor();
    Servidor.main(null);
    Conector conector = new Conector();
    conector.connect();
    PaqueteUsuario pu = new PaqueteUsuario();
    pu.setUsername("123");
    pu.setPassword("");
    PaquetePersonaje pp = conector.getPersonaje(pu);
    System.out.println(pp);
    conector.close();
  }

}
