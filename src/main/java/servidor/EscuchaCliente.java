package servidor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.google.gson.Gson;

import dominio.Item;
import estados.Estado;
import mensajeria.Comando;
import mensajeria.Paquete;
import mensajeria.PaqueteAtacar;
import mensajeria.PaqueteBatalla;
import mensajeria.PaqueteChatPrivado;
import mensajeria.PaqueteDeMovimientos;
import mensajeria.PaqueteDePersonajes;
import mensajeria.PaqueteFinalizarBatalla;
import mensajeria.PaqueteInicioSesion;
import mensajeria.PaqueteIntercambio;
import mensajeria.PaqueteItem;
import mensajeria.PaqueteMensajeSala;
import mensajeria.PaqueteMercado;
import mensajeria.PaqueteMochila;
import mensajeria.PaqueteMovimiento;
import mensajeria.PaquetePersonaje;
import mensajeria.PaqueteUsuario;
import mensajeria.Usuario;

public class EscuchaCliente extends Thread {

	private final Socket socket;
	private final ObjectInputStream entrada;
	private final ObjectOutputStream salida;
	private int idPersonaje;
	private final Gson gson = new Gson();
	
	private PaquetePersonaje paquetePersonaje;
	private PaqueteItem paqueteItem;
	private PaqueteMovimiento paqueteMovimiento;
	private PaqueteBatalla paqueteBatalla;
	private PaqueteAtacar paqueteAtacar;
	private PaqueteFinalizarBatalla paqueteFinalizarBatalla;
	private PaqueteMensajeSala pqs;
	private PaqueteMochila pmochila;
	private PaqueteInicioSesion pini;
	private PaqueteDeMovimientos paqueteDeMovimiento;
	private PaqueteDePersonajes paqueteDePersonajes;
	private PaqueteMercado pmerca;
	private PaqueteChatPrivado pcp;
	private PaqueteIntercambio pi;

	public EscuchaCliente(String ip, Socket socket, ObjectInputStream entrada, ObjectOutputStream salida) {
		this.socket = socket;
		this.entrada = entrada;
		this.salida = salida;
		paquetePersonaje = new PaquetePersonaje();
	}

	public void run() {
		try {

			Paquete paquete;
			Paquete paqueteSv = new Paquete(null, 0);
			PaqueteUsuario paqueteUsuario = new PaqueteUsuario();

			String cadenaLeida = (String) entrada.readObject();
		
			while (!((paquete = gson.fromJson(cadenaLeida, Paquete.class)).getComando() == Comando.DESCONECTAR)){
								
				switch (paquete.getComando()) {
				
				case Comando.REGISTRO:
					
					// Paquete que le voy a enviar al usuario
					paqueteSv.setComando(Comando.REGISTRO);
					
					paqueteUsuario = (PaqueteUsuario) (gson.fromJson(cadenaLeida, PaqueteUsuario.class)).clone();

					// Si el usuario se pudo registrar le envio un msj de exito
					if (Servidor.getConector().registrarUsuario(paqueteUsuario)) {
						paqueteSv.setMensaje(Paquete.msjExito);
						salida.writeObject(gson.toJson(paqueteSv));
					// Si el usuario no se pudo registrar le envio un msj de fracaso
					} else {
						paqueteSv.setMensaje(Paquete.msjFracaso);
						salida.writeObject(gson.toJson(paqueteSv));
					}
					break;

				case Comando.CREACIONPJ:
					
					// Casteo el paquete personaje
					paquetePersonaje = (PaquetePersonaje) (gson.fromJson(cadenaLeida, PaquetePersonaje.class));
					
					// Guardo el personaje en ese usuario
					Servidor.getConector().registrarPersonaje(paquetePersonaje, paqueteUsuario);
					
					// Le envio el id del personaje
					salida.writeObject(gson.toJson(paquetePersonaje, paquetePersonaje.getClass()));
					
					break;

				case Comando.INICIOSESION:
					paqueteSv.setComando(Comando.INICIOSESION);
					
					// Recibo el paquete usuario
					paqueteUsuario = (PaqueteUsuario) (gson.fromJson(cadenaLeida, PaqueteUsuario.class));
					
					// Si se puede loguear el usuario le envio un mensaje de exito y el paquete personaje con los datos
					if (Servidor.getConector().loguearUsuario(paqueteUsuario)) {
						
						paquetePersonaje = new PaquetePersonaje();
						paquetePersonaje = Servidor.getConector().getPersonaje(paqueteUsuario);
						paquetePersonaje.setComando(Comando.INICIOSESION);
						paquetePersonaje.setMensaje(Paquete.msjExito);
						idPersonaje = paquetePersonaje.getId();
						
						salida.writeObject(gson.toJson(paquetePersonaje));
						pini = new PaqueteInicioSesion();
						pini.setComando(Comando.RECIBIRCONECTADOS);
						for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
							pini.add(new Usuario(conectado.getPaquetePersonaje().getNombre(), conectado.getIdPersonaje()));
						}
						
						for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
							conectado.getSalida().writeObject(gson.toJson(pini));

						}
						
					} else {
						paqueteSv.setMensaje(Paquete.msjFracaso);
						salida.writeObject(gson.toJson(paqueteSv));
					}
					
					
					
					
					
					break;

				case Comando.SALIR:
					
					
					// Cierro todo
					entrada.close();
					salida.close();
					socket.close();
					
					// Lo elimino de los clientes conectados
					Servidor.getClientesConectados().remove(this);
					
					// Indico que se desconecto
					Servidor.log.append(paquete.getIp() + " se ha desconectado." + System.lineSeparator());
					
					return;

				case Comando.CONEXION:
					paquetePersonaje = (PaquetePersonaje) (gson.fromJson(cadenaLeida, PaquetePersonaje.class)).clone();

					Servidor.getPersonajesConectados().put(paquetePersonaje.getId(), (PaquetePersonaje) paquetePersonaje.clone());
					Servidor.getUbicacionPersonajes().put(paquetePersonaje.getId(), (PaqueteMovimiento) new PaqueteMovimiento(paquetePersonaje.getId()).clone());
					
					synchronized(Servidor.atencionConexiones){
						Servidor.atencionConexiones.notify();
					}
					
					break;

				case Comando.MOVIMIENTO:					
					
					paqueteMovimiento = (PaqueteMovimiento) (gson.fromJson((String) cadenaLeida, PaqueteMovimiento.class));
					
					Servidor.getUbicacionPersonajes().get(paqueteMovimiento.getIdPersonaje()).setPosX(paqueteMovimiento.getPosX());
					Servidor.getUbicacionPersonajes().get(paqueteMovimiento.getIdPersonaje()).setPosY(paqueteMovimiento.getPosY());
					Servidor.getUbicacionPersonajes().get(paqueteMovimiento.getIdPersonaje()).setDireccion(paqueteMovimiento.getDireccion());
					Servidor.getUbicacionPersonajes().get(paqueteMovimiento.getIdPersonaje()).setFrame(paqueteMovimiento.getFrame());
					
					synchronized(Servidor.atencionMovimientos){
						Servidor.atencionMovimientos.notify();
					}
					
					break;

				case Comando.MOSTRARMAPAS:
					
					// Indico en el log que el usuario se conecto a ese mapa
					paquetePersonaje = (PaquetePersonaje) gson.fromJson(cadenaLeida, PaquetePersonaje.class);
					Servidor.log.append(socket.getInetAddress().getHostAddress() + " ha elegido el mapa " + paquetePersonaje.getMapa() + System.lineSeparator());
					break;
					
				case Comando.BATALLA:
					
					// Le reenvio al id del personaje batallado que quieren pelear
					paqueteBatalla = (PaqueteBatalla) gson.fromJson(cadenaLeida, PaqueteBatalla.class);
					Servidor.log.append(paqueteBatalla.getId() + " quiere batallar con " + paqueteBatalla.getIdEnemigo() + System.lineSeparator());
					
					//seteo estado de batalla
					Servidor.getPersonajesConectados().get(paqueteBatalla.getId()).setEstado(Estado.estadoBatalla);
					Servidor.getPersonajesConectados().get(paqueteBatalla.getIdEnemigo()).setEstado(Estado.estadoBatalla);
					paqueteBatalla.setMiTurno(true);
					salida.writeObject(gson.toJson(paqueteBatalla));
					for(EscuchaCliente conectado : Servidor.getClientesConectados()){
						if(conectado.getIdPersonaje() == paqueteBatalla.getIdEnemigo()){
							int aux = paqueteBatalla.getId();
							paqueteBatalla.setId(paqueteBatalla.getIdEnemigo());
							paqueteBatalla.setIdEnemigo(aux);
							paqueteBatalla.setMiTurno(false);
							conectado.getSalida().writeObject(gson.toJson(paqueteBatalla));
							break;
						}
					}
					
					synchronized(Servidor.atencionConexiones){
						Servidor.atencionConexiones.notify();
					}
					
					break;
					
				case Comando.ATACAR: 
					paqueteAtacar = (PaqueteAtacar) gson.fromJson(cadenaLeida, PaqueteAtacar.class);
					for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
						if(conectado.getIdPersonaje() == paqueteAtacar.getIdEnemigo()) {
							conectado.getSalida().writeObject(gson.toJson(paqueteAtacar));
						}
					}
					break;
					
				case Comando.FINALIZARBATALLA: 
					paqueteFinalizarBatalla = (PaqueteFinalizarBatalla) gson.fromJson(cadenaLeida, PaqueteFinalizarBatalla.class);
					Servidor.getPersonajesConectados().get(paqueteFinalizarBatalla.getId()).setEstado(Estado.estadoJuego);
					Servidor.getPersonajesConectados().get(paqueteFinalizarBatalla.getIdEnemigo()).setEstado(Estado.estadoJuego);
					for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
						if(conectado.getIdPersonaje() == paqueteFinalizarBatalla.getIdEnemigo()) {
							conectado.getSalida().writeObject(gson.toJson(paqueteFinalizarBatalla));
						}
					}
					
					synchronized(Servidor.atencionConexiones){
						Servidor.atencionConexiones.notify();
					}
					
					break;
					
				case Comando.ACTUALIZARPERSONAJE:
					paquetePersonaje = (PaquetePersonaje) gson.fromJson(cadenaLeida, PaquetePersonaje.class);
					Servidor.getConector().actualizarInventario(paquetePersonaje);
					Servidor.getConector().actualizarPersonaje(paquetePersonaje);
					
					Servidor.getPersonajesConectados().remove(paquetePersonaje.getId());
					Servidor.getPersonajesConectados().put(paquetePersonaje.getId(), paquetePersonaje);
					
					System.out.println(paquetePersonaje.getComando());
					for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
						conectado.getSalida().writeObject(gson.toJson(paquetePersonaje));
					}
					
					break;
				case Comando.ITEM:
					paqueteItem = (PaqueteItem) gson.fromJson(cadenaLeida, PaqueteItem.class);
					Item itemAux = Servidor.getConector().getItem(paqueteItem.getId());
					paqueteItem.setBonusAtaque(itemAux.getAtaque());
					paqueteItem.setBonusDefensa(itemAux.getDefensa());
					paqueteItem.setBonusEnergia(itemAux.getEnergia());
					paqueteItem.setBonusMagia(itemAux.getMagia());
					paqueteItem.setBonusSalud(itemAux.getSalud());
					paqueteItem.setNombre(itemAux.getNombre());	
					paqueteItem.setTipo(itemAux.getTipo());
					
					for(EscuchaCliente conectado : Servidor.getClientesConectados()){
						conectado.getSalida().writeObject(gson.toJson(paqueteItem));
					}
					
					break;
					
				case Comando.SALAMSJ:
					pqs = (PaqueteMensajeSala) gson.fromJson(cadenaLeida, PaqueteMensajeSala.class);
					pqs.setComando(Comando.SALAMSJ);
					for(EscuchaCliente conectado : Servidor.getClientesConectados()){
						
						conectado.getSalida().writeObject(gson.toJson(pqs));
					}
					break;
				case Comando.MOCHILA:
					pmochila = (PaqueteMochila) gson.fromJson(cadenaLeida, PaqueteMochila.class);
					Servidor.getMercado().añadir(pmochila.getId(), pmochila.getMochila());
					pmerca = new PaqueteMercado(Servidor.getMercado(), pmochila.getId());
					
					pmerca.setComando(Comando.MERCADO);
					for(EscuchaCliente conectado : Servidor.getClientesConectados()){
						conectado.getSalida().writeObject(gson.toJson(pmerca));
					}
					break;
				case Comando.MENSAJEPRIVADO:
					pcp = (PaqueteChatPrivado) gson.fromJson(cadenaLeida, PaqueteChatPrivado.class);
					pcp.setComando(Comando.MENSAJEPRIVADO);
					for(EscuchaCliente conectado : Servidor.getClientesConectados()){
						if(pcp.getDireccion()==conectado.getIdPersonaje())
						conectado.getSalida().writeObject(gson.toJson(pcp));
					}
					break;
				case Comando.INTERCAMBIO:
					pi = (PaqueteIntercambio) gson.fromJson(cadenaLeida, PaqueteIntercambio.class);
					pi.setComando(Comando.INTERCAMBIO);
					for(EscuchaCliente conectado: Servidor.getClientesConectados()){
						if(pi.getRequerido().getDuenio()==conectado.getIdPersonaje())
						conectado.getSalida().writeObject(gson.toJson(pi));
					}
					
					break;
				case Comando.RESPUESTAINTERCAMBIO:
					pi = (PaqueteIntercambio) gson.fromJson(cadenaLeida, PaqueteIntercambio.class);
					pi.setComando(Comando.RESPUESTAINTERCAMBIO);
					if(pi.getRespuesta()){
						Servidor.getMercado().getMochilas().remove(pi.getOfrecido().getDuenio());
						Servidor.getMercado().getMochilas().remove(pi.getRequerido().getDuenio());

					}
					for(EscuchaCliente conectado: Servidor.getClientesConectados()){
						if(pi.getRequerido().getDuenio()==conectado.getIdPersonaje()
								||pi.getOfrecido().getDuenio()==conectado.getIdPersonaje()){
							conectado.getSalida().writeObject(gson.toJson(pi));
						}
					}
					break;
				case Comando.SALIRMERCADO:
					
					if(Servidor.getMercado().getMochilas().get(idPersonaje)!=null){
						pmerca = new PaqueteMercado(Servidor.getMercado(), pmochila.getId());
						pmerca.setComando(Comando.MERCADO);
						Servidor.getMercado().getMochilas().remove(idPersonaje);
						for(EscuchaCliente conectado : Servidor.getClientesConectados()){
							conectado.getSalida().writeObject(gson.toJson(pmerca));
						}
					}
				default:
					break;
				}
				
				cadenaLeida = (String) entrada.readObject();
			}

			entrada.close();
			salida.close();
			socket.close();

			Servidor.getPersonajesConectados().remove(paquetePersonaje.getId());
			Servidor.getUbicacionPersonajes().remove(paquetePersonaje.getId());
			Servidor.getClientesConectados().remove(this);
			
			// se notifican ls cosas
			for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
				paqueteDePersonajes = new PaqueteDePersonajes(Servidor.getPersonajesConectados());
				paqueteDePersonajes.setComando(Comando.CONEXION);
				conectado.salida.writeObject(gson.toJson(paqueteDePersonajes, PaqueteDePersonajes.class));
			}
			
			
			pini = new PaqueteInicioSesion();
			pini.setComando(Comando.RECIBIRCONECTADOS);
			for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
				if(conectado.getIdPersonaje()!=idPersonaje)
				pini.add(new Usuario(conectado.getPaquetePersonaje().getNombre(), conectado.getIdPersonaje()));
			}
			
			for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
				conectado.getSalida().writeObject(gson.toJson(pini));

			}
			
			
			//con este se renueva el mercado cuando se desconecta
			if(Servidor.getMercado().getMochilas().get(idPersonaje)!=null){
				pmerca = new PaqueteMercado(Servidor.getMercado(), pmochila.getId());
				pmerca.setComando(Comando.MERCADO);
				Servidor.getMercado().getMochilas().remove(idPersonaje);
				for(EscuchaCliente conectado : Servidor.getClientesConectados()){
					conectado.getSalida().writeObject(gson.toJson(pmerca));
			}
			
			}
			
			// Aca hay que hacer magia
			
			
			Servidor.log.append(paquete.getIp() + " se ha desconectado." + System.lineSeparator());

		} catch (IOException | ClassNotFoundException e) {
			Servidor.log.append("Error de conexion: " + e.getMessage() + System.lineSeparator());
			e.printStackTrace();
		} 
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public ObjectInputStream getEntrada() {
		return entrada;
	}
	
	public ObjectOutputStream getSalida() {
		return salida;
	}
	
	public PaquetePersonaje getPaquetePersonaje(){
		return paquetePersonaje;
	}
	
	public int getIdPersonaje() {
		return idPersonaje;
	}
}

