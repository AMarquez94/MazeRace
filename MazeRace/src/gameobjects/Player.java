package gameobjects;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import enums.Team;

/**
 * Represents a player.
 *
 * @author root
 */
public class Player extends Node {
    //Objects

    private Node player, treasure;
    private AudioNode audio_gun;
    private AudioNode audio_hit;
    private BetterCharacterControl characterControl;
    private AnimControl animationControl;
    private AnimChannel animationChannel;
    //Player settings
    public final static float JUMP_FORCE = 10f;
    public final static float GRAVITY = 1f;
    public final static float MOVE_SPEED = 2000f;
    public final static float WIDTH_HEALTH_BAR = 4f;
    //Player attributes
    private Team team;
    private Vector3f position;
    private String nickname;
    private int health;
    //Used for moving
    public Vector3f walkDirection = new Vector3f(0, 0, 0);
    public float airTime = 0;
    //Healthbar
    private Geometry healthbar;
    private boolean me;

    public Player(Team team, Vector3f position, String nickname, SimpleApplication app, boolean me) {
        this.team = team;
        this.position = position;
        this.nickname = nickname;
        this.health = 100;
        this.setName(nickname);
        this.me = me;

        // Load model
        player = (Node) app.getAssetManager().loadModel("Models/Oto/Oto.mesh.xml"); // You can set the model directly to the player. (We just wanted to explicitly show that it's a spatial.)
        this.attachChild(player); // add it to the wrapper

        // Position player
        this.setLocalTranslation(position);
        player.move(0, 3.5f, 0); // adjust position to ensure collisions occur correctly.
        player.setLocalScale(0.5f); // optionally adjust scale of model

        // Audio gun
        audio_gun = new AudioNode(app.getAssetManager(), "Sound/Effects/Gun.wav", false);
        audio_gun.setPositional(false);
        audio_gun.setLooping(false);
        audio_gun.setVolume(2);
        this.attachChild(audio_gun);
        
        // Audio hit
        audio_hit = new AudioNode(app.getAssetManager(), "Sounds/hitsound.wav", false);
        audio_hit.setPositional(false);
        audio_hit.setLooping(false);
        audio_hit.setVolume(2);
        this.attachChild(audio_hit);
        
        BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        if(!me){
            // Nickname
            BitmapText hoverText = new BitmapText(guiFont, false);
            hoverText.setSize(guiFont.getCharSet().getRenderedSize() * 0.1f);
            hoverText.setText(this.nickname + "");
            Node nicknameNode = new Node("Nickname");
            nicknameNode.attachChild(hoverText);
            BillboardControl bc = new BillboardControl();
            this.attachChild(nicknameNode);
            hoverText.center();
            nicknameNode.setLocalTranslation(0, 10.5f, 0);
            nicknameNode.addControl(bc);

            BillboardControl billboard = new BillboardControl();
            healthbar = new Geometry("healthbar", new Quad(WIDTH_HEALTH_BAR, 0.5f));
            Material mathb = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            healthbar.setMaterial(mathb);
            this.attachChild(healthbar);
            healthbar.center();
            healthbar.move(0, 8, 0);
            healthbar.addControl(billboard);

            if(this.team == Team.Blue){
                hoverText.setColor(ColorRGBA.Blue);
                mathb.setColor("Color", ColorRGBA.Blue);
            }
            else{
                hoverText.setColor(ColorRGBA.Red);
                mathb.setColor("Color", ColorRGBA.Red);
            }
        }
        
        // Treasure text, but do not attach yet
        BitmapText treasureText = new BitmapText(guiFont, false);
        treasureText.setSize(guiFont.getCharSet().getRenderedSize() * 0.5f);
        treasureText.setText("Holding treasure");
        treasure = new Node("TreasureText");
        treasure.attachChild(treasureText);
        BillboardControl bct = new BillboardControl();
        treasure.addControl(bct);
        treasure.setLocalTranslation(0, 40, 0);

        //AnimControl control setup animation
        animationControl = player.getControl(AnimControl.class);
        animationChannel = animationControl.createChannel();
        animationChannel.setAnim("stand");
        characterControl = new BetterCharacterControl(1.5f, 6f, 1f); // construct character. (If your character bounces, try increasing height and weight.)
        this.addControl(characterControl); // attach to wrapper

        // set basic physical properties
        characterControl.setJumpForce(new Vector3f(0, JUMP_FORCE, 0));
        characterControl.setGravity(new Vector3f(0, GRAVITY, 0));
        characterControl.warp(position); // warp character into landscape at particular location

        System.out.println("Available animations for this model are:");
        for (String c : animationControl.getAnimationNames()) {
            System.out.println(c);
        }
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

    public void setPosition(Vector3f position) {
        //this.setLocalTranslation(position);
        this.position = position;
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

    public void playGunAudio() {
        this.audio_gun.playInstance();
    }
    
    public void playHitAudio(){
        this.audio_hit.playInstance();
    }

    public void walkToPosition(Vector3f position) {
        Vector3f old_pos = this.position.clone();
        Vector3f dir = position.subtract(old_pos).normalize();
        float dist = position.distance(old_pos);

        this.walkDirection = dir;
        this.walkDirection.multLocal(MOVE_SPEED).multLocal(dist);
        this.getCharacterControl().setWalkDirection(this.walkDirection);

        this.setPosition(position);
    }
    
    public boolean hasTreasure() {
        return this.hasChild(treasure);
    }
    
    public void setTreasureMode(boolean activate) {
        if(activate) { //activate
            this.attachChild(treasure);
        } else if(this.hasChild(treasure)) { //deactivate
            this.detachChild(treasure);
        }
    }
    
    public int getHealth(){
        return this.health;
    }
    
    public void setHealth(int health){
        this.health = health;
        if(!me){
            healthbar.setLocalScale(((float)health)/100, 1f, 1f);
        }
    }
}
