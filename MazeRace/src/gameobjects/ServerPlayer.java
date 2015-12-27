package gameobjects;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import enums.Team;

/**
 * Represents a player.
 *
 * @author root
 */
public class ServerPlayer extends Node{
    //Objects

    private Node player;
    //Player settings
    public final static float JUMP_FORCE = 10f;
    public final static float GRAVITY = 1f;
    public final static float MOVE_SPEED = 2000f;
    //Player attributes
    private Team team;
    private Vector3f position;
    private String nickname;
    private Quaternion orientation;
    
    private BetterCharacterControl control;
    private GhostControl otroControl;

    public ServerPlayer(int id, Team team, Vector3f position, String nickname,
            SimpleApplication app) {
        this.team = team;
        this.position = position;
        this.nickname = nickname;
        this.setName(id + "");
       
        
        

        // Load model
        Spatial character = app.getAssetManager().loadModel("Models/Oto/Oto.mesh.xml"); // You can set the model directly to the player. (We just wanted to explicitly show that it's a spatial.)
        character.setName(id + "");
         DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        character.addLight(sun);
        player = new Node(id + "");
        player.attachChild(character);
        this.attachChild(player); // add it to the wrapper
//        characterControl = new BetterCharacterControl(1.5f, 6f, 1f);
        // Position player
        this.setLocalTranslation(position);
        player.move(0, 3.0f, 0); // adjust position to ensure collisions occur correctly.
        player.setLocalScale(0.5f); // optionally adjust scale of model
        orientation = this.getWorldRotation();
        //control = new BetterCharacterControl(1.5f, 6f, 1f); // construct character. (If your character bounces, try increasing height and weight.)
//        CapsuleCollisionShape c = new CapsuleCollisionShape(1.5f,6f,1);
//        otroControl = new GhostControl(c);
//        otroControl.setPhysicsLocation(position);
//        this.addControl(otroControl); // attach to wrapper
//        control.warp(position);
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
//        control.warp(position);
//        otroControl.setPhysicsLocation(position);
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
    
    public void addToPhysicsSpace(BulletAppState bas) {
        bas.getPhysicsSpace().add(otroControl);
        bas.getPhysicsSpace().addAll(this);
    }
}
