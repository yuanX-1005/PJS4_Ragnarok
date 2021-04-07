package serveur;

import java.util.Hashtable;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

@ServerEndpoint(value = "/chatroom/{pseudo}", configurator = ChatRoom.EndpointConfigurator.class)

public class ChatRoom {

	private static ChatRoom singleton = new ChatRoom();
	private Hashtable<String, Session> sessions = new Hashtable<>();

	public ChatRoom() {
	}

	/**
	 * Acquisition de notre unique instance ChatRoom
	 */
	public static ChatRoom getInstance() {
		return ChatRoom.singleton;
	}

	/**
	 * On maintient toutes les sessions utilisateurs dans une collection.
	 */

	/**
	 * Cette mï¿½thode est dï¿½clenchï¿½e ï¿½ chaque connexion d'un utilisateur.
	 */
	@OnOpen
	public void open(Session session, @PathParam("pseudo") String pseudo) {
		sendMessage("Connexion établie pour " + pseudo);
		session.getUserProperties().put("pseudo", pseudo);
		sessions.put(session.getId(), session);
	}

	/**
	 * Cette mï¿½thode est dï¿½clenchï¿½e ï¿½ chaque dï¿½connexion d'un utilisateur.
	 */
	@OnClose
	public void close(Session session) {
		String pseudo = (String) session.getUserProperties().get("pseudo");
		sessions.remove(session.getId());
		sendMessage("Connexion fermée pour " + pseudo);
	}

	/**
	 * Cette mï¿½thode est dï¿½clenchï¿½e en cas d'erreur de communication.
	 */
	@OnError
	public void onError(Throwable error) {
		System.out.println("Error: " + error.getMessage());
	}

	/**
	 * Cette mï¿½thode est dï¿½clenchï¿½e ï¿½ chaque rï¿½ception d'un message
	 * utilisateur.
	 */
	@OnMessage
	public void handleMessage(String message, Session session) {
		String pseudo = (String) session.getUserProperties().get("pseudo");
		String fullMessage = pseudo + ": " + message;
		sendMessage(fullMessage);
	}

	/**
	 * Une mï¿½thode privï¿½e, spï¿½cifique ï¿½ notre exemple. Elle permet l'envoie
	 * d'un message aux participants de la discussion.
	 */
	private void sendMessage(String fullMessage) {
		// Affichage sur la console du server Web.
		System.out.println(fullMessage);

		// On envoie le message ï¿½ tout le monde.
		for (Session session : sessions.values()) {
			try {
				session.getBasicRemote().sendText(fullMessage);
			} catch (Exception exception) {
				System.out.println("ERROR: cannot send message to " + session.getId());
			}
		}
	}

	/**
	 * Permet de ne pas avoir une instance diffï¿½rente par client. ChatRoom est
	 * donc gï¿½rer en "singleton" et le configurateur utilise ce singleton.
	 */
	public static class EndpointConfigurator extends ServerEndpointConfig.Configurator {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T getEndpointInstance(Class<T> endpointClass) {
			return (T) ChatRoom.getInstance();
		}
	}
}