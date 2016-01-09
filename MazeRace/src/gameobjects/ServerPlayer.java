package gameobjects;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.GhostControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import enums.Team;

/**
 * Represents a player processed by the server side.
 *
 * @authors Alejandro Marquez, Bjorn van der Laan, Dominik Gils
 */
public class ServerPlayer extends Node{

    //Objects
    private Node player;
    private int id;
    //Player attributes
    private Team team;
    private Vector3f position;
    private String nickname;
    private Quaternion orientation;
    private int health;
    private boolean hasTreasure;
    private boolean dead;
    private GhostControl otroControl;

    public ServerPlayer(int id, Team team, Vector3f position, String nickname,
            SimpleApplication app) {
        this.id = id;
        this.team = team;
        this.position = position;
        this.nickname = nickname;
        this.setName(id + "");
        this.health = 100;
        this.hasTreasure = false;
        // Load model
        Spatial character = app.getAssetManager().loadModel("Models/Oto/Oto.mesh.xml"); // You can set the model directly to the player. (We just wanted to explicitly show that it's a spatial.)
        character.setName(id + "");
        player = new Node(id + "");
        player.attachChild(character);
        this.attachChild(player); // add it to the wrapper
        // Players position
        this.setLocalTranslation(position);
        player.move(0, 3.0f, 0); // adjust position to ensure collisions occur correctly.
        player.setLocalScale(0.5f); // optionally adjust scale of model
        orientation = this.getWorldRotation();
        this.dead = false;
    }

    /**
     * @return Player's team
     */
    public Team getTeam() {
        return this.team;
    }

    /**
     * @return Player's team colour 
     */
    public ColorRGBA getTeamColor() {
        if (this.team == Team.Red) {
            return ColorRGBA.Red;
        } else if (this.team == Team.Blue) {
            return ColorRGBA.Blue;
        } //Default case. For other values.
        else {
            return ColorRGBA.White;
        }
    }

    /**
     * Set player's position to the position passed as parameter
     * @param position 
     */
    public void setPosition(Vector3f position) {
        this.position = position;
        this.setLocalTranslation(position);
    }

    /**
     * @return Player's position
     */
    public Vector3f getPosition() {
        return this.position;
    }

    /**
     * Set player's nickname to the nickname passed as parameter
     * @param nick 
     */
    public void setNickname(String nick) {
        this.nickname = nick;
    }

    /**
     * @return Player's nickname
     */
    public String getNickname() {
        return this.nickname;
    }
    
    /**
     * @return player's orientation
     */
    public Quaternion getRotation(){
        return this.orientation;
    }
    
    /**
     * @return player's rotation (in float[] format)
     */
    public float[] getRotationFloat(){
        float[] r = new float[4];
        r[0] = this.orientation.getX();
        r[1] = this.orientation.getY();
        r[2] = this.orientation.getZ();
        r[3] = this.orientation.getW();
        return r;
    }

    /**
     * Set player node
     * @param player 
     */
    public void setPlayer(Node player) {
        this.player = player;
    }

    /**
     * Set player's team
     * @param team 
     */
    public void setTeam(Team team) {
        this.team = team;
    }

    /**
     * Set player's orientation
     * @param orientation 
     */
    public void setOrientation(Quaternion orientation) {
        this.orientation = orientation;
        this.setLocalRotation(orientation);
    }
    
    /**
     * Add players to the physics space
     * @param bas 
     */
    public void addToPhysicsSpace(BulletAppState bas) {
        bas.getPhysicsSpace().add(otroControl);
        bas.getPhysicsSpace().addAll(this);
    }
    
    /**
     * @return player's health
     */
    public int getHealth(){
        return this.health;
    }
    
    /**
     * Set player's health
     * @param health 
     */
    public void setHealth(int health){
        this.health = health;
    }
    
    /**
     * Add health passed as parameter to player's health
     * @param health 
     */
    public void addHealth(int health){
        this.health = this.health + health;
    }
    
    /**
     * Decreases health passed as parameter from player's health
     * @param health
     * @return true if player is dead. False otherwise.
     */
    public boolean decreaseHealth(int health){
        this.health = this.health - health;
        return this.health <= 0;
    }
    
    /**
     * @return true if player is dead. False otherwise 
     */
    public boolean isDead(){
        return this.dead;
    }
    
    /**
     * Set player as dead if parameter equals true. If it equals false, set player
     * as alive
     * @param dead 
     */
    public void setDead(boolean dead){
        this.dead = dead;
    }
    
    /**
     * @return true if player has the treasure
     */
    public boolean getHasTreasure() {
        return this.hasTreasure;
    }
    
    /**
     * Set player as having treasure if hasTreasure equals true. If it is false, 
     * set player as not having the treasure.
     * @param hasTreasure 
     */
    public void setHasTreasure(boolean hasTreasure) {
        this.hasTreasure = hasTreasure;
    }
    
    /**
     * @return player's id
     */
    public int getId(){
        return this.id;
    }
}
