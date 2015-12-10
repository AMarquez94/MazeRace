package gameobjects;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import enums.Team;

/**
 * Represents a player.
 * @author root
 */
public class Player extends Node {
    //Objects
    private Node player;
    private BetterCharacterControl characterControl;
    private AnimControl animationControl;
    private AnimChannel animationChannel;
    //Player settings
    private final float JUMP_FORCE = 10f;
    private final float GRAVITY = 1f;
    private Team team;
    

    public Player(Team team, SimpleApplication app) {
        this.team = team;
        // Load model
        player = (Node) app.getAssetManager().loadModel("Models/Oto/Oto.mesh.xml"); // You can set the model directly to the player. (We just wanted to explicitly show that it's a spatial.)
        this.attachChild(player); // add it to the wrapper
        player.move(0, 3.5f, 0); // adjust position to ensure collisions occur correctly.
        player.setLocalScale(0.5f); // optionally adjust scale of model

        //AnimControl control setup animation
        animationControl = player.getControl(AnimControl.class);
        animationChannel = animationControl.createChannel();
        animationChannel.setAnim("stand");
        characterControl = new BetterCharacterControl(1.5f, 6f, 1f); // construct character. (If your character bounces, try increasing height and weight.)
        this.addControl(characterControl); // attach to wrapper

        // set basic physical properties
        characterControl.setJumpForce(new Vector3f(0, JUMP_FORCE, 0));
        characterControl.setGravity(new Vector3f(0, GRAVITY, 0));
        characterControl.warp(new Vector3f(0, 10, 10)); // warp character into landscape at particular location

        System.out.println("Available animations for this model are:");
        for (String c : animationControl.getAnimationNames()) {
            System.out.println(c);
        }
    }
    
    public Team getTeam() {
        return this.team;
    }
    
    public ColorRGBA getTeamColor() {
        if(this.team == Team.Red) {
            return ColorRGBA.Red;
        }
        else if (this.team == Team.Blue) {
            return ColorRGBA.Blue;
        }
        //Default case. For other values.
        else {
            return ColorRGBA.White;
        }
    }
    
    public void addToPhysicsSpace(BulletAppState bas) {
        bas.getPhysicsSpace().add(characterControl);
        bas.getPhysicsSpace().addAll(this);
    }
    
    public void addAnimEventListener(AnimEventListener listener) {
        animationControl.addListener(listener);
    }
    
    public BetterCharacterControl getCharacterControl() {
        return characterControl;
    }
    
    public AnimChannel getAnimChannel() {
        return animationChannel;
    }
}
