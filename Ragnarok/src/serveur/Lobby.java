package serveur;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import appli.Personne;

@ServerEndpoint(value = "/game/{pseudo}", configurator = Lobby.EndpointConfigurator.class)

public class Lobby {

	private static Lobby singleton = new Lobby();
	private Hashtable<String, Session> sessions = new Hashtable<>();
	private ArrayList<Personne> listeJoueursRecu = new ArrayList<>();
	private static int[] nbGentil = { 3, 4, 4, 5, 6, 6 };
	private int nbJoueur, capitaine;
	private static int votesOui = 0;
	private static int votesNon = 0;
	private int nbRoundSabote = 0;
	private int nbRoundGagne = 0;
	private int sabotage = 0;
	private int pasSabotage = 0;
	private final static String MECHANT = "Traitre";
	private final static String GENTIL = "Gentil";
	private static Hashtable<String, Session> compo = new Hashtable<>();
	private List<Boolean> sabotages = new ArrayList<Boolean>();

	public Lobby() {
	}

	/**
	 * Acquisition de notre unique instance ChatRoom
	 */
	public static Lobby getInstance() {
		return Lobby.singleton;
	}

	/**
	 * Cette methode est declenchee a chaque connexion d'utilisateur.
	 */
	@OnOpen
	public void open(Session session, @PathParam("pseudo") String pseudo) {

		int size = sessions.size() + 1;
		sendMessage("Admin >>> Connection established for " + pseudo);
		Personne personne = new Personne(pseudo);
		session.getUserProperties().put("personne", personne);
		sessions.put(session.getId(), session);
		if (size > 10) {
			close(session);
		}
		updateLobby();

	}

	/**
	 * Cette methode est declenchee a chaque deconnexion d'un utilisateur.
	 */
	@OnClose
	public void close(Session session) {
		Personne personne = (Personne) session.getUserProperties().get("personne");
		sessions.remove(session.getId());
		updateLobby();
	}

	/**
	 * Cette methode est declenchee en cas d'erreur de communication.
	 */
	@OnError
	public void onError(Throwable error) {
		System.out.println("Error: " + error.getMessage());
	}

	/**
	 * Cette methode est declenchee a chaque reception d'un message
	 * utilisateur.
	 */
	@OnMessage
	public void handleMessage(String message, Session sessionInitial) {
		if (message.equals("Start")) {
			nbJoueur = 1;
			nbRoundSabote = 0;
			nbRoundGagne = 0;
			List<Integer> listeSessionsTriees = new ArrayList<Integer>();
			for (Session session : sessions.values()) {
				int parseInt = Integer.parseInt(session.getId(), 16); // NumberFormatException
				listeSessionsTriees.add(parseInt);
			}
			Collections.sort(listeSessionsTriees);
			for (int numSession : listeSessionsTriees) {
				for (Session session : sessions.values()) {
					if (numSession == (Integer.parseInt(session.getId(), 16))) {
						Personne personne = (Personne) session.getUserProperties().get("personne");
						personne.setId(nbJoueur);
						listeJoueursRecu.add(personne); // les personnes qui jouent le jeu
						nbJoueur++;
					}
				}
			}
			sendMessage(message);
			attributionRoles();
			Random random = new Random();
			int a = random.nextInt(nbJoueur - 2); // a est entre 0 et nbJoueur-1
			capitaine = a + 1; // capitaine est un nb entre 1 et nbJoueur
			initCapitaine();
		} else if (message.contains("composition")) {
			sendMessage(message);
			compo = getListeChoisis(message);
		} else if (message.equals("Oui") || message.equals("Non")) {
			vote(message);
		}
		if (message.equals("Sabotage")) {
			// true pour dire que c'est saboté
			sabotages.add(true);
			if (sabotages.size() == compo.size()) { // taille de la liste des personnes selectionnes
				analyseSabotage(sabotages);
			}
		}

		if (message.equals("PasSabotage")) {
			// false pour dire que c'est non saboté
			sabotages.add(false);
			if (sabotages.size() == compo.size()) { // taille de la liste des personnes selectionnes
				analyseSabotage(sabotages);
			}
		}
	}

	// Prend en compte les votes : lorsque tout les votes sont réalisés
	private void vote(String message) {
		if (message.equals("Oui")) {
			votesOui++;
		} else if (message.equals("Non")) {
			votesNon++;
		}
		if (votesOui + votesNon == sessions.size()) {
			if (votesOui > votesNon) {
				startMission(compo);
			} else {
				restart();
			}
			votesOui = 0;
			votesNon = 0;
		}

	}

	// Reactualise les roles et change de capitaine
	private void restart() {
		sendMessage("Restart");
		getListeRole();
		changerCapitaine();
	}

	// Deduit si la manche a été saboté ou non
	private void analyseSabotage(List<Boolean> sabotages) {
		boolean sabotage = false;
		for (boolean temp : sabotages) {
			if (temp) {
				sabotage = true;
			}
		}
		if (sabotage) {
			roundSabote();
		} else if (!sabotage) {
			roundPasSabote();
		}
	}

	// Si la manche a ete sabote la manche est pour les traitres et si 3 manches ont
	// ete sabotes c'est la fin de la partie

	private void roundSabote() {
		nbRoundSabote += 1;

		if (nbRoundSabote == 3) {
			for (Session session : sessions.values()) {
				Personne personne = (Personne) session.getUserProperties().get("personne");
				if (personne.getRole() == "Traitre") {
					try {
						session.getBasicRemote().sendText("TraitreWin");
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (personne.getRole() == "Gentil") {
					try {
						session.getBasicRemote().sendText("GentilLose");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			returnGame("Oui");
			restart();
		}
		sabotages.clear();
	}

	// Si la manche n'a pas ete sabote la manche est pour les gentils et si 3
	// manches ont ete gagnes par les gentils
	// c'est la fin de la partie

	private void roundPasSabote() {
		nbRoundGagne += 1;
		if (nbRoundGagne == 3) {
			for (Session session : sessions.values()) {
				Personne personne = (Personne) session.getUserProperties().get("personne");
				if (personne.getRole() == "Traitre") {
					try {
						session.getBasicRemote().sendText("TraitreLose");
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (personne.getRole() == "Gentil") {
					try {
						session.getBasicRemote().sendText("GentilWin");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			returnGame("Non");
			restart();
		}
		sabotages.clear();
	}

	// 1ere etape, les joueurs ont la possibilites de choisir entre un oui ou un non
	// Un String est renvoye pour dire s'ils peuvent saboter ou non
	private void startMission(Hashtable<String, Session> compo) {
		sabotage = 0;
		pasSabotage = 0;
		TreeMap<String, Session> treemap = new TreeMap<String, Session>(compo);
		for (Session session : compo.values()) {
			Personne personne = (Personne) session.getUserProperties().get("personne");
			if (personne.getRole() == "Traitre") {
				try {
					session.getBasicRemote().sendText("TraitreChoixPage"); // Renvoyer sur la page
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (personne.getRole() == "Gentil") {
				try {
					session.getBasicRemote().sendText("GentilsChoixPage"); // Renvoyer sur la page
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	
	private void returnGame(String Sabotage) {
		if (Sabotage.equals("Oui")) {
			for (Session session : sessions.values()) {
				Personne personne = (Personne) session.getUserProperties().get("personne");
				try {
					session.getBasicRemote().sendText("ReturnGameSabote");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (Sabotage.equals("Non")) {
			for (Session session : sessions.values()) {
				Personne personne = (Personne) session.getUserProperties().get("personne");
				try {
					session.getBasicRemote().sendText("ReturnGamePasSabote");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {

		}
	}

	// A Partir de la string contenant les id des choisis renvoie la liste des
	// sessions

	private Hashtable<String, Session> getListeChoisis(String message) {
		String[] joueursChoisis = message.split("/");
		Hashtable<String, Session> liste = new Hashtable();
		TreeMap<String, Session> treemap = new TreeMap<String, Session>(sessions);
		for (int i = 1; i < joueursChoisis.length; i++) {
			for (Session session1 : treemap.values()) {
				Personne personne = (Personne) session1.getUserProperties().get("personne");
				if (Integer.parseInt(joueursChoisis[i]) == personne.getId()) {
					liste.put(session1.getId(), session1);
				}
			}
		}
		return liste;
	}

	// Initie le premier capitaine de la partie
	private void initCapitaine() {
		TreeMap<String, Session> treemap = new TreeMap<String, Session>(sessions);
		for (Session session1 : treemap.values()) {
			Personne personne = (Personne) session1.getUserProperties().get("personne");
			int id = personne.getId();
			if (id == capitaine) {
				personne.setCapitaine(true);
				sendMessage("capitaine /" + id);
			}
		}
	}

	// change le capitaine chaque tour
	private void changerCapitaine() {
		TreeMap<String, Session> treemap = new TreeMap<String, Session>(sessions);
		for (Session session1 : treemap.values()) {
			Personne personne = (Personne) session1.getUserProperties().get("personne");
			int id = personne.getId();
			if (id == capitaine) {
				personne.setCapitaine(false);
			}
		}
		capitaine++;
		if (capitaine == nbJoueur) {
			capitaine = 1;
		}
		initCapitaine();
	}
	
	// Permet l'affichage des rôles pour chaque page
	private void attributionRoles() {
		Random random = new Random();
		int tmp = nbJoueur - 2; // nb d'index de personne
		int nbGentils = nbGentil[nbJoueur - 1 - 5];
		int a;
		for (int i = 0; i < nbGentils; i++) {
			a = random.nextInt(tmp);
			listeJoueursRecu.get(a).setRole(GENTIL);
			listeJoueursRecu.remove(a);
			tmp--;
		}

		for (int i = 0; i < listeJoueursRecu.size(); i++) {
			listeJoueursRecu.get(i).setRole(MECHANT);
		}
		listeJoueursRecu.clear();
		getListeRole();
	}

	/**
	 * Permet l'envoie  d'un message aux participants de la discussion.	
	 */
	private void sendMessage(String fullMessage) {
		// On envoie le message ï¿½ tout le monde.
		for (Session session : sessions.values()) {
			try {
				session.getBasicRemote().sendText(fullMessage);
			} catch (Exception exception) {
				System.out.println("ERROR: cannot send message to " + session.getId());
			}
		}
	}

	// Permet d'envoyer un message a une session précise
	private void sendMessage(String fullMessage, Session session) {
		try {
			session.getBasicRemote().sendText(fullMessage);
		} catch (IOException e) {
			System.out.println("ERROR: cannot send message to " + session.getId());
		}
	}

	// Lorsqu'un joueur se connecte ou se deconnecte, met a jour pour chaque joueur la liste des joueurs
	private void updateLobby() {
		String listePseudos = "";
		int i = 0;

		List<Integer> listeSessionsTriees = new ArrayList<Integer>();
		// Initialisation de la liste avec toutes les sessions pour le tri
		for (Session session : sessions.values()) {
			int parseInt = Integer.parseInt(session.getId(), 16); // NumberFormatException
			listeSessionsTriees.add(parseInt);
		}
		Collections.sort(listeSessionsTriees);
		for (int numSession : listeSessionsTriees) {
			for (Session session : sessions.values()) {
				if (numSession == Integer.parseInt(session.getId(), 16)) {
					Personne personne = (Personne) session.getUserProperties().get("personne");
					if (i++ == sessions.size() - 1) {
						listePseudos += personne.getPseudo();
					} else {
						listePseudos += personne.getPseudo() + "/";

					}
				}
			}
		}
		for (Session session : sessions.values()) {
			try {
				session.getBasicRemote().sendText(listePseudos);
			} catch (Exception exception) {
				System.out.println("ERROR: cannot send message to " + session.getId());
			}
		}
	}

	// envoyer le role à toutes les joueur
	private void getListeRole() {
		TreeMap<String, Session> treemap = new TreeMap<String, Session>(sessions);
		for (Session session1 : treemap.values()) {
			Personne personne = (Personne) session1.getUserProperties().get("personne");
			String role = personne.getRole();
			sendListeRole(role, session1);
		}
	}
	
	// Envoie a chaque session son rôle
	private void sendListeRole(String role, Session session) {
		String listeRole = "";
		if (role == GENTIL) {
			Personne personne = (Personne) session.getUserProperties().get("personne");
			listeRole += "role /" + personne.getRole() + "/" + personne.getId();
			sendMessage(listeRole, session); // envoyer le role gentil
		} else {
			listeRole = "role ";
			for (Session session1 : sessions.values()) {
				Personne personne = (Personne) session1.getUserProperties().get("personne");
				if (personne.getRole() == MECHANT) {
					listeRole += "/" + personne.getRole() + "/" + personne.getId();
				}
			}
			sendMessage(listeRole, session); // envoyer roles traitres à tous les personnes ont un role traitre
		}
	}

	/**
	 * Permet de ne pas avoir une instance différente par client. ChatRoom est
	 * donc gere en "singleton" et le configurateur utilise ce singleton.
	 */
	public static class EndpointConfigurator extends ServerEndpointConfig.Configurator {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T getEndpointInstance(Class<T> endpointClass) {
			return (T) Lobby.getInstance();
		}
	}

}