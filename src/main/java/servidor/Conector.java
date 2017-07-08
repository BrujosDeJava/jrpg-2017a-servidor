package servidor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import dominio.Inventario;
import dominio.Item;
import mensajeria.PaquetePersonaje;
import mensajeria.PaqueteUsuario;

public class Conector {

	private String url = "primeraBase.bd";
	Connection connect;

	public void connect() {
		try {
			Servidor.log.append("Estableciendo conexi�n con la base de datos..." + System.lineSeparator());
			connect = DriverManager.getConnection("jdbc:sqlite:" + url);
			Servidor.log.append("Conexión con la base de datos establecida con Éxito." + System.lineSeparator());
		} catch (SQLException ex) {
			Servidor.log.append("Fallo al intentar establecer la conexi�n con la base de datos. " + ex.getMessage()
					+ System.lineSeparator());
		}
	}

	public void close() {
		try {
			connect.close();
		} catch (SQLException ex) {
			Servidor.log.append("Error al intentar cerrar la conexión con la base de datos." + System.lineSeparator());
			Logger.getLogger(Conector.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public boolean registrarUsuario(PaqueteUsuario user) {
		ResultSet result = null;
		try {
			PreparedStatement st1 = connect.prepareStatement("SELECT * FROM registro WHERE usuario= ? ");
			st1.setString(1, user.getUsername());
			result = st1.executeQuery();

			if (!result.next()) {

				PreparedStatement st = connect.prepareStatement("INSERT INTO registro (usuario, password, idPersonaje) VALUES (?,?,?)");
				st.setString(1, user.getUsername());
				st.setString(2, user.getPassword());
				st.setInt(3, user.getIdPj());
				st.execute();
				Servidor.log.append("El usuario " + user.getUsername() + " se ha registrado." + System.lineSeparator());
				return true;
			} else {
				Servidor.log.append("El usuario " + user.getUsername() + " ya se encuentra en uso." + System.lineSeparator());
				return false;
			}
		} catch (SQLException ex) {
			Servidor.log.append("Eror al intentar registrar el usuario " + user.getUsername() + System.lineSeparator());
			System.err.println(ex.getMessage());
			return false;
		}

	}

	public boolean registrarPersonaje(PaquetePersonaje paquetePersonaje, PaqueteUsuario paqueteUsuario) {

		try {

			// Registro al personaje en la base de datos
			PreparedStatement stRegistrarPersonaje = connect.prepareStatement(
					"INSERT INTO personaje (idInventario,casta,raza,fuerza,destreza,inteligencia,saludTope,energiaTope,nombre,experiencia,nivel,idAlianza) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			stRegistrarPersonaje.setInt(1, -1);
			stRegistrarPersonaje.setString(2, paquetePersonaje.getCasta());
			stRegistrarPersonaje.setString(3, paquetePersonaje.getRaza());
			stRegistrarPersonaje.setInt(4, paquetePersonaje.getFuerza());
			stRegistrarPersonaje.setInt(5, paquetePersonaje.getDestreza());
			stRegistrarPersonaje.setInt(6, paquetePersonaje.getInteligencia());
			stRegistrarPersonaje.setInt(7, paquetePersonaje.getSaludTope());
			stRegistrarPersonaje.setInt(8, paquetePersonaje.getEnergiaTope());
			stRegistrarPersonaje.setString(9, paquetePersonaje.getNombre());
			stRegistrarPersonaje.setInt(10, 0);
			stRegistrarPersonaje.setInt(11, 1);
			stRegistrarPersonaje.setInt(12, -1);
			stRegistrarPersonaje.execute();

			// Recupero la �ltima key generada
			ResultSet rs = stRegistrarPersonaje.getGeneratedKeys();
			if (rs != null && rs.next()) {

				// Obtengo el id
				int idPersonaje = rs.getInt(1);

				// Le asigno el id al paquete personaje que voy a devolver
				paquetePersonaje.setId(idPersonaje);

				// Le asigno el personaje al usuario
				PreparedStatement stAsignarPersonaje = connect.prepareStatement("UPDATE registro SET idPersonaje=? WHERE usuario=? AND password=?");
				stAsignarPersonaje.setInt(1, idPersonaje);
				stAsignarPersonaje.setString(2, paqueteUsuario.getUsername());
				stAsignarPersonaje.setString(3, paqueteUsuario.getPassword());
				stAsignarPersonaje.execute();

				// Por ultimo registro el inventario y la mochila
				if (this.registrarInventarioMochila(idPersonaje)) {
					Servidor.log.append("El usuario " + paqueteUsuario.getUsername() + " ha creado el personaje "
							+ paquetePersonaje.getId() + System.lineSeparator());
					return true;
				} else {
					Servidor.log.append("Error al registrar la mochila y el inventario del usuario " + paqueteUsuario.getUsername() + " con el personaje" + paquetePersonaje.getId() + System.lineSeparator());
					return false;
				}
			}
			return false;

		} catch (SQLException e) {
			Servidor.log.append(
					"Error al intentar crear el personaje " + paquetePersonaje.getNombre() + System.lineSeparator());
			e.printStackTrace();
			return false;
		}

	}

	public boolean registrarInventarioMochila(int idPersonaje) {
		try {
			// Preparo la consulta para el registro el inventario en la base de
			// datos
			PreparedStatement stRegistrarInventario = connect.prepareStatement("INSERT INTO inventario(slot1,slot2,slot3,slot4,slot5,slot6) VALUES (-1,-1,-1,-1,-1,-1)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			ResultSet rs =stRegistrarInventario.getGeneratedKeys();
			// Registro inventario y mochila
			stRegistrarInventario.execute();
			int aux = rs.getInt(1);
			// Le asigno el inventario y la mochila al personaje
			
			
			PreparedStatement stRegistrarMochila = connect.prepareStatement("INSERT INTO mochila(slot1,slot2,slot3,slot4,slot5,slot6,slot7,slot8,slot9,slot10) VALUES (-1,-1,-1,-1,-1,-1,-1,-1,-1,-1)",
					PreparedStatement.RETURN_GENERATED_KEYS);
			ResultSet rs2 =stRegistrarMochila.getGeneratedKeys();
			// Registro inventario y mochila
			stRegistrarMochila.execute();
			int aux2 = rs2.getInt(1);
			
			PreparedStatement stAsignarPersonaje = connect
					.prepareStatement("UPDATE personaje SET idInventario=?, idMochila=? WHERE idPersonaje=?");
			stAsignarPersonaje.setInt(1, aux);
			stAsignarPersonaje.setInt(2, aux2);
			stAsignarPersonaje.setInt(3, idPersonaje);
			stAsignarPersonaje.execute();
			
			
			Servidor.log.append("Se ha registrado el inventario de " + idPersonaje + System.lineSeparator());
			return true;

		} catch (SQLException e) {
			Servidor.log.append("Error al registrar el inventario de " + idPersonaje + System.lineSeparator());
			e.printStackTrace();
			return false;
		}
	}

	public boolean loguearUsuario(PaqueteUsuario user) {
		ResultSet result = null;
		try {
			// Busco usuario y contrase�a
			PreparedStatement st = connect
					.prepareStatement("SELECT * FROM registro WHERE usuario = ? AND password = ? ");
			st.setString(1, user.getUsername());
			st.setString(2, user.getPassword());
			result = st.executeQuery();

			// Si existe inicio sesion
			if (result.next()) {
				Servidor.log.append("El usuario " + user.getUsername() + " ha iniciado sesi�n." + System.lineSeparator());
				return true;
			}

			// Si no existe informo y devuelvo false
			Servidor.log.append("El usuario " + user.getUsername() + " ha realizado un intento fallido de inicio de sesi�n." + System.lineSeparator());
			return false;

		} catch (SQLException e) {
			Servidor.log.append("El usuario " + user.getUsername() + " fallo al iniciar sesi�n." + System.lineSeparator());
			e.printStackTrace();
			return false;
		}

	}

	public void actualizarPersonaje(PaquetePersonaje paquetePersonaje) {
		try {
			PreparedStatement stActualizarPersonaje = connect
					.prepareStatement("UPDATE personaje SET fuerza=?, destreza=?, inteligencia=?, saludTope=?, energiaTope=?, experiencia=?, nivel=? "
							+ "  WHERE idPersonaje=?");
			
			stActualizarPersonaje.setInt(1, paquetePersonaje.getFuerza());
			stActualizarPersonaje.setInt(2, paquetePersonaje.getDestreza());
			stActualizarPersonaje.setInt(3, paquetePersonaje.getInteligencia());
			stActualizarPersonaje.setInt(4, paquetePersonaje.getSaludTope());
			stActualizarPersonaje.setInt(5, paquetePersonaje.getEnergiaTope());
			stActualizarPersonaje.setInt(6, paquetePersonaje.getExperiencia());
			stActualizarPersonaje.setInt(7, paquetePersonaje.getNivel());
			stActualizarPersonaje.setInt(8, paquetePersonaje.getId());
			stActualizarPersonaje.executeUpdate();
			
			Inventario aux = paquetePersonaje.getInv();

			PreparedStatement stActualizarInventario = connect
					.prepareStatement("UPDATE inventario SET slot1=?, slot2=?, slot3=?, slot4=?, slot5=?, slot6=?"
							+ "  WHERE idInventario=?");
			stActualizarInventario.setInt(1,paquetePersonaje.getInv().getCabeza().getId());
			stActualizarInventario.setInt(2,paquetePersonaje.getInv().getManos().getId());
			stActualizarInventario.setInt(3,paquetePersonaje.getInv().getPies().getId());
			stActualizarInventario.setInt(4,paquetePersonaje.getInv().getCuerpo().getId());
			stActualizarInventario.setInt(5,paquetePersonaje.getInv().getAccesorio().getId());
			stActualizarInventario.setInt(6,paquetePersonaje.getInv().getArma().getId());
			stActualizarInventario.setInt(7, paquetePersonaje.getId());
			stActualizarInventario.executeUpdate();
			
			
			PreparedStatement stActualizarMochila = connect
					.prepareStatement("UPDATE mochila SET slot1=?, slot2=?, slot3=?, slot4=?, slot5=?, slot6=?, slot7=?, slot8=?, slot9=?, slot10=?"
							+ "  WHERE idMochila=?");
			int cant = aux.getMochila().size();
			for(int i=0;i<10;i++){
				if(i<cant){
				stActualizarMochila.setInt(i+1, aux.getMochila().get(i).getId());
				}
				else
				stActualizarMochila.setInt(i+1, -1);
			}
			stActualizarMochila.setInt(11, paquetePersonaje.getId());
			stActualizarMochila.executeUpdate();
			
			
			Servidor.log.append("El personaje " + paquetePersonaje.getNombre() + " se ha actualizado con Éxito."  + System.lineSeparator());;
		} catch (SQLException e) {
			Servidor.log.append("Fallo al intentar actualizar el personaje " + paquetePersonaje.getNombre()  + System.lineSeparator());
			e.printStackTrace();
		}
		
		
	}

	public PaquetePersonaje getPersonaje(PaqueteUsuario user) {
		ResultSet result = null;
		try {
			// Selecciono el personaje de ese usuario
			PreparedStatement st = connect.prepareStatement("SELECT * FROM registro WHERE usuario = ?");
			st.setString(1, user.getUsername());
			result = st.executeQuery();

			// Obtengo el id
			int idPersonaje = result.getInt("idPersonaje");

			// Selecciono los datos del personaje
			PreparedStatement stSeleccionarPersonaje = connect
					.prepareStatement("SELECT * FROM personaje WHERE idPersonaje = ?");
			stSeleccionarPersonaje.setInt(1, idPersonaje);
			result = stSeleccionarPersonaje.executeQuery();

			// Obtengo los atributos del personaje
			PaquetePersonaje personaje = new PaquetePersonaje();
			personaje.setId(idPersonaje);
			personaje.setRaza(result.getString("raza"));
			personaje.setCasta(result.getString("casta"));
			personaje.setFuerza(result.getInt("fuerza"));
			personaje.setInteligencia(result.getInt("inteligencia"));
			personaje.setDestreza(result.getInt("destreza"));
			personaje.setEnergiaTope(result.getInt("energiaTope"));
			personaje.setSaludTope(result.getInt("saludTope"));
			personaje.setNombre(result.getString("nombre"));
			personaje.setExperiencia(result.getInt("experiencia"));
			personaje.setNivel(result.getInt("nivel"));
			
			Inventario inv = new Inventario();
			PreparedStatement stSeleccionarInv = connect
					.prepareStatement("SELECT * FROM inventario WHERE idInventario = ?");
			stSeleccionarInv.setInt(1, idPersonaje);
			ResultSet resultInv = stSeleccionarInv.executeQuery();
			Item aux;
			for(int i=0;i<6;i++){
				aux =getItem(resultInv.getInt("slot"+(i+1)));
				if(aux.getId()!=-1)
				inv.añadir(aux);
			}
			
			PreparedStatement stSeleccionarMochila = connect
					.prepareStatement("SELECT * FROM mochila WHERE idMochila = ?");
			stSeleccionarMochila.setInt(1, idPersonaje);
			ResultSet resultMochila = stSeleccionarMochila.executeQuery();
			for(int i=0;i<10;i++){
				aux =getItem(resultMochila.getInt("slot"+(i+1)));
				if(aux.getId()!=-1)
				inv.añadir(aux);
			}
			personaje.setInv(inv);
			// Devuelvo el paquete personaje con sus datos
			return personaje;

		} catch (SQLException ex) {
			Servidor.log.append("Fallo al intentar recuperar el personaje " + user.getUsername() + System.lineSeparator());
			Servidor.log.append(ex.getMessage() + System.lineSeparator());
			ex.printStackTrace();
		}

		return new PaquetePersonaje();
	}
	
	public PaqueteUsuario getUsuario(String usuario) {
		ResultSet result = null;
		PreparedStatement st;
		
		try {
			st = connect.prepareStatement("SELECT * FROM registro WHERE usuario = ?");
			st.setString(1, usuario);
			result = st.executeQuery();

			String password = result.getString("password");
			int idPersonaje = result.getInt("idPersonaje");
			
			PaqueteUsuario paqueteUsuario = new PaqueteUsuario();
			paqueteUsuario.setUsername(usuario);
			paqueteUsuario.setPassword(password);
			paqueteUsuario.setIdPj(idPersonaje);
			
			return paqueteUsuario;
		} catch (SQLException e) {
			Servidor.log.append("Fallo al intentar recuperar el usuario " + usuario + System.lineSeparator());
			Servidor.log.append(e.getMessage() + System.lineSeparator());
			e.printStackTrace();
		}
		
		return new PaqueteUsuario();
	}
	
	public Item getItem(int id){
		if(id==-1)
			return new Item(-1);
		Item aux = new Item(id);
		try {
			PreparedStatement st = connect.prepareStatement("SELECT * FROM Item WHERE idItem = ?");
			st.setInt(1, id);
			ResultSet result = st.executeQuery();
			aux.setAtaque(result.getInt("Ataque"));
			aux.setDefensa(result.getInt("Defensa"));
			aux.setMagia(result.getInt("Magia"));
			aux.setSalud(result.getInt("Salud"));
			aux.setEnergia(result.getInt("Energia"));
			aux.setNombre(result.getString("Nombre"));
			aux.setTipo(result.getInt("Tipo"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return aux;
	}

	public void actualizarInventario(PaquetePersonaje paquetePersonaje) {
		Inventario inv = paquetePersonaje.getInv();
		if(inv.getCabeza().getNombre()==null){
		}
		
		
		paquetePersonaje.setInv(inv);		
	}
}
