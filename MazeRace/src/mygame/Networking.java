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
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author NVE Project
 */
public class Networking {

    public static final int PORT = 6000;
    public static final String HOST = "192.168.1.2";

    public static void initialiseSerializables() {
        Serializer.registerClass(Aggregation.class);
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
        Serializer.registerClass(TreasureDropped.class);
        Serializer.registerClass(WantToRespawn.class);
    }

    /**
     * Special aggregation packet Used in packet aggregation algorithms
     */
    @Serializable
    public static class Aggregation extends AbstractMessage {

        private ArrayList<AbstractMessage> messages; //messages

        public Aggregation() {
            messages = new ArrayList<AbstractMessage>();
        }

        public void addMessage(AbstractMessage m) {
            messages.add(m);
        }

        public void addMessages(LinkedBlockingQueue<AbstractMessage> m) {
            messages.addAll(m);
        }

        public ArrayList<AbstractMessage> getMessages() {
            return messages;
        }

        public int getSize() {
            return messages.size();
        }
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
     * Client -> Server Client informs the Server that he wants to shoot and the
     * direction he was aiming
     */
    @Serializable
    public static class FireInput extends AbstractMessage {

        private Vector3f direction;
        private Vector3f position;

        public FireInput() {
        }

        public FireInput(Vector3f direction, Vector3f position) {
            this.direction = direction;
            this.position = position;
        }

        public Vector3f getDirection() {
            return direction;
        }

        public Vector3f getPosition() {
            return position;
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

        public Firing() {
        }

        public Firing(int playerID) {
            this.playerID = playerID;
        }

        public int getPlayerID() {
            return playerID;
        }
    }

    /**
     * Server -> Client Server says to the clients that the Player with index
     * "shootedPlayerId" has been shooted by another with index
     * "shootingPlayerId", and its health is now "newHealth"
     */
    @Serializable
    public static class PlayerShooted extends AbstractMessage {

        private int shootedPlayerId;
        private int shootingPlayerId;
        private int newHealth;

        public PlayerShooted() {
        }

        public PlayerShooted(int shootedPlayerId, int shootingPlayerId, int newHealth) {
            this.shootedPlayerId = shootedPlayerId;
            this.shootingPlayerId = shootingPlayerId;
            this.newHealth = newHealth;
        }

        public int getShootedPlayerId() {
            return shootedPlayerId;
        }

        public int getShootingPlayerId() {
            return shootingPlayerId;
        }

        public int getNewHealth() {
            return newHealth;
        }
    }

    /**
     * Server -> Clients Server says the clients that player with index
     * deadPlayer has died because of killerPlayer
     */
    @Serializable
    public static class DeadPlayer extends AbstractMessage {

        private int deadPlayer;
        private int killerPlayer;

        public DeadPlayer() {
        }

        public DeadPlayer(int deadPlayer, int killerPlayer) {
            this.deadPlayer = deadPlayer;
            this.killerPlayer = killerPlayer;
        }

        public int getDeadPlayer() {
            return deadPlayer;
        }

        public int getKillerPlayer() {
            return killerPlayer;
        }
    }

    /**
     * Client -> Server Client says he has dead and he wants to respawn
     */
    @Serializable
    public static class WantToRespawn extends AbstractMessage {

        public WantToRespawn() {
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

        private Vector3f location;
        private Vector3f direction;

        public PickTreasureInput() {
        }

        public PickTreasureInput(Vector3f location, Vector3f direction) {
            this.location = location;
            this.direction = direction;
        }

        public Vector3f getLocation() {
            return location;
        }

        public Vector3f getDirection() {
            return direction;
        }
    }

    /**
     * Server -> Clients Server says where the treasure is dropped Also used
     * when initializing the treasure
     */
    @Serializable
    public static class TreasureDropped extends AbstractMessage {

        private Vector3f location;

        public TreasureDropped() {
        }

        public TreasureDropped(Vector3f location) {
            this.location = location;
        }

        public Vector3f getLocation() {
            return this.location;
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
     * rotation and current animation is "position", "rotation", orientation
     * (where the player looks) "orientation" and "animation"
     */
    @Serializable
    public static class PlayerMoved extends AbstractMessage {

        Vector3f position;
        float[] rotation;
        Vector3f orientation;
        String animation;

        public PlayerMoved() {
        }

        public PlayerMoved(Vector3f position, float[] rotation, Vector3f orientation, String animation) {
            this.position = position;
            this.rotation = rotation;
            this.orientation = orientation;
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

        public Vector3f getOrientation() {
            return orientation;
        }
    }

    /**
     * Server -> Client Server says the other clients that the player with ID
     * "playerID" is in position "position", with rotation "rotation",
     * orientation (where the player looks) "orientation", and performing the
     * animation "animation"
     */
    @Serializable
    public static class MovingPlayers extends AbstractMessage {

        int playerID;
        Vector3f position;
        float[] rotation;
        Vector3f orientation;
        String animation;

        public MovingPlayers() {
        }

        public MovingPlayers(int playerID, Vector3f position, float[] rotation,
                Vector3f orientation, String animation) {
            this.playerID = playerID;
            this.position = position;
            this.orientation = orientation;
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

        public Vector3f getOrientation() {
            return orientation;
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
     * and the direction he was aiming
     */
    @Serializable
    public static class MarkInput extends AbstractMessage {

        private Vector3f direction;
        private Vector3f position;

        public MarkInput() {
        }

        public MarkInput(Vector3f direction, Vector3f position) {
            this.direction = direction;
            this.position = position;
        }

        public Vector3f getDirection() {
            return direction;
        }

        public Vector3f getPosition() {
            return position;
        }
    }

    /**
     * Server -> Clients Server says to the clients that a new mark with the
     * colour of the team has been put in position "position"
     */
    @Serializable
    public static class PutMark extends AbstractMessage {

        private Team team;
        private Vector3f position;

        public PutMark() {
        }

        public PutMark(Team team, Vector3f position) {
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
     * Server -> Clients Sends to all players the positions, orientations,
     * nicknames and teams of the connected players.
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
