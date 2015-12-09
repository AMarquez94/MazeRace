/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;

/**
 *
 * @author Alejandro
 */
public class Networking {
    
    public static final int PORT = 6000;
    
    public static void initialiseSerializables(){
        Serializer.registerClass(Alive.class);
        Serializer.registerClass(Connect.class);
        Serializer.registerClass(ConnectionRejected.class);
        Serializer.registerClass(ConnectionAccepted.class);
        Serializer.registerClass(DeadPlayer.class);
        Serializer.registerClass(DisconnectedPlayer.class);
        Serializer.registerClass(FireInput.class);
        Serializer.registerClass(Firing.class);
        Serializer.registerClass(NewPlayerConnected.class);
        Serializer.registerClass(PickTreasureInput.class);
        Serializer.registerClass(PlayerRespawn.class);
        Serializer.registerClass(PlayerShooted.class);
        Serializer.registerClass(TreasurePicked.class);
    }
    
    /**
     * Client -> Server
     * A player with "nickname" nickname wants to connect
     */
    @Serializable
    public static class Connect extends AbstractMessage{
        
        private String nickname;
        
        public Connect(){
            
        }
        
        public Connect(String nickname){
            this.nickname = nickname;
        }
        
        public String getNickname(){
            return nickname;
        }
    }
    
    /**
     * Server -> Client
     * Server says to the client that it can't connect because of
     * given reason
     */
    @Serializable
    public static class ConnectionRejected extends AbstractMessage{
        
        private String reason;
        
        public ConnectionRejected(){
            
        }
        
        public ConnectionRejected(String reason){
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
    
    /**
     * Server -> Client
     * Server says client that it connected, and what teams are
     * available
     */
    @Serializable
    public static class ConnectionAccepted extends AbstractMessage{
        
        private int[] teamsAvailable;
        
        public ConnectionAccepted(){
            
        }
        
        public ConnectionAccepted(int[] teamsAvailable){
            this.teamsAvailable = teamsAvailable;
        }
        
        public int[] getTeamsAvailable(){
            return teamsAvailable;
        }
    }
    
    /**
     * Server -> Clients
     * Server says to all connected clients that client with id "id",
     * nickname "nickname" and team with index "team" has connected and spawn
     * in position "position 
     */
    @Serializable
    public static class NewPlayerConnected extends AbstractMessage{
        
        private int id;
        private String nickname;
        private int team;
        private Vector3f position;
        
        public NewPlayerConnected(){
            
        }
        
        public NewPlayerConnected(int id, String nickname, int team, Vector3f position){
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

        public int getTeam() {
            return team;
        }

        public Vector3f getPosition() {
            return position;
        }
    }
    
    /**
     * Client -> Server
     * Client informs the Server that he wants to shoot
     */
    @Serializable
    public static class FireInput extends AbstractMessage{
        
        public FireInput(){
            
        }
    }
    
    /**
     * Server -> Clients
     * Server informs all the clients that player with ID "playerID" has
     * shooted bullet with ID "bulletID" from position "position with 
     * direction "direction"
     */
    @Serializable
    public static class Firing extends AbstractMessage{
        
        private int playerID;
        private int bulletID;
        private Vector3f position;
        private Vector3f direction;
        
        public Firing(){
            
        }
        
        public Firing(int playerID, int bulletID, Vector3f position, Vector3f direction){
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
     * Server -> Client
     * Server says to the clients that the Player with index "shootedPlayerId"
     * has been shooted by another with index "shootingPlayerId"
     */
    @Serializable
    public static class PlayerShooted extends AbstractMessage{
        
        private int shootedPlayerId;
        private int shootingPlayerId;
        
        public PlayerShooted(){
            
        }
        
        public PlayerShooted(int shootedPlayerId, int shootingPlayerId){
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
     * Server -> Clients
     * Server says the clients that player with index deadPlayer has died
     */
    @Serializable
    public static class DeadPlayer extends AbstractMessage{
        
        private int deadPlayer;
        
        public DeadPlayer(){
            
        }
        
        public DeadPlayer(int deadPlayer){
            this.deadPlayer = deadPlayer;
        }

        public int getDeadPlayer() {
            return deadPlayer;
        }
    }
    
    /**
     * Server -> Clients
     * Server says to the clients that player with index "playerRespawn" has
     * respawned in position "position"
     */
    @Serializable
    public static class PlayerRespawn extends AbstractMessage{
        
        private int playerRespawn;
        private Vector3f position;
        
        public PlayerRespawn(){
            
        }
        
        public PlayerRespawn(int playerRespawn, Vector3f position){
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
     * Client -> Server
     * Client says to the Server that it pressed the pick treasure input
     */
    @Serializable
    public static class PickTreasureInput extends AbstractMessage{
        
        public PickTreasureInput(){
            
        }
    }
    
    /**
     * Server -> Clients
     * Server says that Client with id "playerId" picked the treasure
     */
    @Serializable
    public static class TreasurePicked extends AbstractMessage{
        
        private int playerID;
        
        public TreasurePicked(){
            
        }
        
        public TreasurePicked(int playerID){
            this.playerID = playerID;
        }

        public int getPlayerID() {
            return playerID;
        }
    }
    
    /**
     * Server -> Clients
     * Server says to the Clients that player with id "playerID" has been
     * disconnected
     */
    @Serializable
    public static class DisconnectedPlayer extends AbstractMessage{
        
        private int playerID;
        
        public DisconnectedPlayer(){
            
        }
        
        public DisconnectedPlayer(int playerID){
            this.playerID = playerID;
        }

        public int getPlayerID() {
            return playerID;
        }
    } 
    
    /**
     * Client -> Server
     * Client says to the server that it is alive
     */
    public static class Alive extends AbstractMessage{
        public Alive(){
            
        }
    }
    
    /**
     * 
     */
}
