package abertay.uad.ac.uk.myapplication;

public class Lobby {
    private String id;
    private String name;
    private String hostUsername, guestUsername;
    private int currentPlayers;
    private String guestFcmToken, hostFcmToken;

    public Lobby() {}

    public Lobby(String id, String name, String hostUsername, int currentPlayers, String guestFcmToken, String hostFcmToken, String guestUsername) {
        this.id = id;
        this.name = name;
        this.hostUsername = hostUsername;
        this.currentPlayers = currentPlayers;
        this.hostFcmToken = hostFcmToken;
        this.guestFcmToken = guestFcmToken;
        this.guestUsername = guestUsername;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
}