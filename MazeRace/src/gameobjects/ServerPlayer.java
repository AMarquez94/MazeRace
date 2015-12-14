package gameobjects;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import enums.Team;

/**
 * Represents a player.
 *
 * @author root
 */
public class ServerPlayer extends Node {
    //Objects

    private Node player;
    //Player settings
    public final static float JUMP_FORCE = 10f;
    public final static float GRAVITY = 1f;
    public final static float MOVE_SPEED = 800f;
    //Player attributes
    private Team team;
    private Vector3f position;
    private String nickname;
    private Quaternion orientation;

    public ServerPlayer(Team team, Vector3f position, String nickname, 
            Quaternion orientation, SimpleApplication app) {
        this.team = team;
        this.position = position;
        this.nickname = nickname;
        this.orientation = orientation;

        // Load model
        player = (Node) app.getAssetManager().loadModel("Models/Oto/Oto.mesh.xml"); // You can set the model directly to the player. (We just wanted to explicitly show that it's a spatial.)
        this.attachChild(player); // add it to the wrapper

        // Position player
        player.setLocalTranslation(position);
        player.move(0, 3.5f, 0); // adjust position to ensure collisions occur correctly.
        player.setLocalScale(0.5f); // optionally adjust scale of model
        player.setLocalRotation(orientation);
    }

    public Team getTeam() {
        return this.team;
    }

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

    public void setPosition(Vector3f position) {
        this.position = position;
        this.setLocalTranslation(position);
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public void setNickname(String nick) {
        this.nickname = nick;
    }

    public String getNickname() {
        return this.nickname;
    }
    
    public Quaternion getRotation(){
        return this.orientation;
    }
    
    public float[] getRotationFloat(){
        float[] r = new float[4];
        r[0] = this.orientation.getX();
        r[1] = this.orientation.getY();
        r[2] = this.orientation.getZ();
        r[3] = this.orientation.getW();
        return r;
    }

    public void setPlayer(Node player) {
        this.player = player;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public void setOrientation(Quaternion orientation) {
        this.orientation = orientation;
        this.setLocalRotation(orientation);
    }
    
    
}
