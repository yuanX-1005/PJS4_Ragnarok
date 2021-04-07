package appli;

public class Personne {

	private int id;
	private String pseudo;
	private String role;
	private boolean capitaine = false;
	
	public boolean isCapitaine() {
		return capitaine;
	}

	public void setCapitaine(boolean capitaine) {
		this.capitaine = capitaine;
	}

	public Personne(String pseudo) {
		this.id = 0;
		this.pseudo = pseudo;
		this.role = null;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public String getPseudo() {
		return pseudo;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
}
