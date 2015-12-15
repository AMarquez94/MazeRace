/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import enums.Team;

/**
 *
 * @author NVE Project
 */
public class Networking {

    public static final int PORT = 6000;
    public static final String HOST = "127.0.0.1";

    public static void initialiseSerializables() {
        Serializer.registerClass(Alive.class);
        Serializer.registerClass(Connect.class);
        Serializer.registerClass(ConnectionRejected.class);
        Serializer.registerClass(DeadPlayer.class);
        Serializer.registerClass(DisconnectedPlayer.class);
        Serializer.registerClass(FireInput.class);
        Serializer.registerClass(Firing.class);
        Serializer.registerClass(NewPlayerConnected.class);
        Serializer.registerClass(PickTreasureInput.class);
        Serializer.registerClass(PlayerRespawn.class);
        Serializer.registerClass(PlayerShooted.class);
        Serializer.registerClass(Prepare.class);
        Serializer.registerClass(TreasurePicked.class);
        Serializer.registerClass(PlayerMoved.class);
        Serializer.registerClass(MovingPlayers.class);
        Serializer.registerClass(MarkInput.class);
        Serializer.registerClass(PutMark.class);
        Serializer.registerClass(Start.class);
        Serializer.registerClass(End.class);
        Serializer.registerClass(Pause.class);
        Serializer.registerClass(Resume.class);
    }

    /**
     * Client -> Server A player with "nickname" nickname wants to connect
     */
    @Serializable
    public static class Connect extends AbstractMessage {

        private String nickname;

        public Connect() {
        }

        public Connect(String nickname) {
            this.nickname = nickname;
        }

        public String getNickname() {
            return nickname;
        }
    }

    /**
     * Server -> Client Server says to the client that it can't connect because
     * of given reason
     */
    @Serializable
    public static class ConnectionRejected extends AbstractMessage {

        private String reason;

        public ConnectionRejected() {
        }

        public ConnectionRejected(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Server -> Clients Server says to all connected clients that client with
     * id "id", nickname "nickname" and team with index "team" has connected and
     * spawn in position "position
     */
    @Serializable
    public static class NewPlayerConnected extends AbstractMessage {

        private int id;
        private String nickname;
        private Team team;
        private Vector3f position;

        public NewPlayerConnected() {
        }

        public NewPlayerConnected(int id, String nickname, Team team, Vector3f position) {
            this.id = id;
            this.nickname = nickname;
            this.team = team;
            this.position = position;
        }

        public int getId() {
            return id;
        }

        public String getNickname() {
            return nickname;
        }

        public Team getTeam() {
            return team;
        }

        public Vector3f getPosition() {
            return position;
        }
    }

    /**
     * Client -> Server Client informs the Server that he wants to shoot
     */
    @Serializable
    public static class FireInput extends AbstractMessage {

        public FireInput() {
        }
    }

    /**
     * Server -> Clients Server informs all the clients that player with ID
     * "playerID" has shooted bullet with ID "bulletID" from position "position
     * with direction "direction"
     */
    @Serializable
    public static class Firing extends AbstractMessage {

        private int playerID;
        private int bulletID;
        private Vector3f position;
        private Vector3f direction;

        public Firing() {
        }

        public Firing(int playerID, int bulletID, Vector3f position, Vector3f direction) {
            this.playerID = playerID;
            this.bulletID = bulletID;
            this.position = position;
            this.direction = direction;
        }

        public int getPlayerID() {
            return playerID;
        }

        public int getBulletID() {
            return bulletID;
        }

        public Vector3f getPosition() {
            return position;
        }

        public Vector3f getDirection() {
            return direction;
        }
    }

    /**
     * Server -> Client Server says to the clients that the Player with index
     * "shootedPlayerId" has been shooted by another with index
     * "shootingPlayerId"
     */
    @Serializable
    public static class PlayerShooted extends AbstractMessage {

        private int shootedPlayerId;
        private int shootingPlayerId;

        public PlayerShooted() {
        }

        public PlayerShooted(int shootedPlayerId, int shootingPlayerId) {
            this.shootedPlayerId = shootedPlayerId;
            this.shootingPlayerId = shootingPlayerId;
        }

        public int getShootedPlayerId() {
            return shootedPlayerId;
        }

        public int getShootingPlayerId() {
            return shootingPlayerId;
        }
    }

    /**
     * Server -> Clients Server says the clients that player with index
     * deadPlayer has died
     */
    @Serializable
    public static class DeadPlayer extends AbstractMessage {

        private int deadPlayer;

        public DeadPlayer() {
        }

        public DeadPlayer(int deadPlayer) {
            this.deadPlayer = deadPlayer;
        }

        public int getDeadPlayer() {
            return deadPlayer;
        }
    }

    /**
     * Server -> Clients Server says to the clients that player with index
     * "playerRespawn" has respawned in position "position"
     */
    @Serializable
    public static class PlayerRespawn extends AbstractMessage {

        private int playerRespawn;
        private Vector3f position;

        public PlayerRespawn() {
        }

        public PlayerRespawn(int playerRespawn, Vector3f position) {
            this.playerRespawn = playerRespawn;
            this.position = position;
        }

        public int getPlayerRespawn() {
            return playerRespawn;
        }

        public Vector3f getPosition() {
            return position;
        }
    }

    /**
     * Client -> Server Client says to the Server that it pressed the pick
     * treasure input
     */
    @Serializable
    public static class PickTreasureInput extends AbstractMessage {

        public PickTreasureInput() {
        }
    }

    /**
     * Server -> Clients Server says that Client with id "playerId" picked the
     * treasure
     */
    @Serializable
    public static class TreasurePicked extends AbstractMessage {

        private int playerID;

        public TreasurePicked() {
        }

        public TreasurePicked(int playerID) {
            this.playerID = playerID;
        }

        public int getPlayerID() {
            return playerID;
        }
    }

    /**
     * Server -> Clients Server says to the Clients that player with id
     * "playerID" has been disconnected
     */
    @Serializable
    public static class DisconnectedPlayer extends AbstractMessage {

        private int playerID;

        public DisconnectedPlayer() {
        }

        public DisconnectedPlayer(int playerID) {
            this.playerID = playerID;
        }

        public int getPlayerID() {
            return playerID;
        }
    }

    /**
     * Client -> Server Client says to the server that it is alive
     */
    @Serializable
    public static class Alive extends AbstractMessage {

        public Alive() {
        }
    }

    /**
     * Client -> Server Client says to the server that its new position,
     * rotation and current animation is "position", "rotation" and "animation"
     */
    @Serializable
    public static class PlayerMoved extends AbstractMessage {

        Vector3f position;
        float[] rotation;
        String animation;

        public PlayerMoved() {
        }

        public PlayerMoved(Vector3f position, float[] rotation, String animation) {
            this.position = position;
            this.rotation = rotation;
            this.animation = animation;
        }

        public Vector3f getPosition() {
            return position;
        }

        public float[] getRotation() {
            return rotation;
        }

        public String getAnimation() {
            return animation;
        }
    }

    /**
     * Server -> Client Server says the other clients that the player with ID
     * "playerID" is in position "position", with rotation "rotation" and
     * performing the animation "animation"
     */
    @Serializable
    public static class MovingPlayers extends AbstractMessage {

        int playerID;
        Vector3f position;
        float[] rotation;
        String animation;

        public MovingPlayers() {
        }

        public MovingPlayers(int playerID, Vector3f position, float[] rotation, String animation) {
            this.playerID = playerID;
            this.position = position;
            this.rotation = rotation;
            this.animation = animation;
        }

        public int getPlayerID() {
            return playerID;
        }

        public Vector3f getPosition() {
            return position;
        }

        public float[] getRotation() {
            return rotation;
        }

        public String getAnimation() {
            return animation;
        }
    }

    /**
     * Server -> Client Server says to the clients that the game has started
     */
    @Serializable
    public static class Start extends AbstractMessage {

        public Start() {
        }
    }

    /**
     * Server -> Clients Server says to the clients that the game has ended, and
     * which team was the winner
     */
    @Serializable
    public static class End extends AbstractMessage {

        Team winnerTeam;

        public End() {
        }

        public End(Team team) {
            this.winnerTeam = team;
        }

        public Team getWinnerTeam() {
            return winnerTeam;
        }
    }

    /**
     * Client -> Server Client says to the server that wants to paint the wall
     */
    @Serializable
    public static class MarkInput extends AbstractMessage {

        public MarkInput() {
        }
    }
    
    /**
     * Server -> Clients
     * Server says to the clients that a new mark with the colour of the team
     * has been put in position "position"
     */
    @Serializable
    public static class PutMark extends AbstractMessage {
        
        private Team team;
        private Vector3f position;
        
        public PutMark(){
            
        }
        
        public PutMark(Team team, Vector3f position){
            this.team = team;
            this.position = position;
        }

        public Team getTeam() {
            return team;
        }

        public Vector3f getPosition() {
            return position;
        }
        
    }

    /**
     * Server -> Client Server says to the clients that the game has been paused
     */
    @Serializable
    public static class Pause extends AbstractMessage {

        public Pause() {
        }
    }

    /**
     * Server -> Client Server says to the clients that the game has been
     * resumed
     */
    @Serializable
    public static class Resume extends AbstractMessage {

        public Resume() {
        }
    }
    
    /**
     * Server -> Clients
     * Sends to all players the positions, orientations, nicknames and teams of 
     * the connected players.
     */
    @Serializable
    public static class Prepare extends AbstractMessage {

        private Vector3f[] positions;
        private float[][] orientations;
        private String[] nicknames; 
        private Team[] teams;

        public Prepare() {
        }

        public Prepare(Vector3f[] positions, float[][] orientations, String[] nicknames, Team[] teams) {
            this.positions = positions;
            this.orientations = orientations;
            this.nicknames = nicknames;
            this.teams = teams;
        }

        public Vector3f[] getPositions() {
            return positions;
        }

        public float[][] getOrientations() {
            return orientations;
        }

        public String[] getNicknames() {
            return nicknames;
        }

        public Team[] getTeams() {
            return teams;
        }
        
    }
}
