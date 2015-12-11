package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.Client;
import com.jme3.network.Network;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import enums.Team;
import gameobjects.Mark;
import gameobjects.Player;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import mygame.Networking.*;

/**
 * MazeRace (client).
 *
 * @author
 */
public class ClientMain extends SimpleApplication {

    private static ClientMain app;
    private Client client;
    private LinkedBlockingQueue<AbstractMessage> messageQueue;
    private BulletAppState bas;
    private ChaseCamera chaseCam;
    //scene graph
    private Node markNode = new Node("Marks");
    //terrain
    private TerrainQuad terrain;
    private Material mat_terrain;
    //player
    private Player player;
    //player movement
    private boolean left = false, right = false, up = false, down = false;
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private float airTime = 0;
    private final float MOVE_SPEED = 800f;

    public static void main(String[] args) {

        Networking.initialiseSerializables();
        app = new ClientMain();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //flyCam.setEnabled(false);

        bas = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bas);

        this.pauseOnFocus = false;

        setUpGraph();
        setUpLight();
//        setUpWorld();
        setUpCharacter(Team.Red);
        setUpKeys();
        initCrossHairs();
        
        setUpNetworking();
    }

    private void setUpNetworking() {
        //Start connection
        try {
            client = Network.connectToServer("127.0.0.1", Networking.PORT);
            client.start();
        } catch (IOException ex) {
            Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Message sending
        messageQueue = new LinkedBlockingQueue<AbstractMessage>();
        Thread t = new Thread(new Sender());
        t.start();
    }

    private void setUpGraph() {
        rootNode.attachChild(markNode);
    }

    private void setUpCharacter(Team team) {
        player = new Player(team, this);
        player.addAnimEventListener(playerAnimListener);
        player.addToPhysicsSpace(bas);

        rootNode.attachChild(player);
    }
    private AnimEventListener playerAnimListener = new AnimEventListener() {
        public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        }

        public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        }
    };

    private void setUpKeys() {
        inputManager.addMapping("CharLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("CharRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("CharForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("CharBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("CharJump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Mark", new KeyTrigger(KeyInput.KEY_M), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(playerMoveListener, "CharLeft", "CharRight", "CharForward", "CharBackward", "CharJump");
        inputManager.addListener(playerShootListener, "Mark");
    }
    /*
     * Handles player movement actions.
     */
    private ActionListener playerMoveListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("CharLeft")) {
                if (isPressed) {
                    left = true;
                } else {
                    left = false;
                }
            } else if (name.equals("CharRight")) {
                if (isPressed) {
                    right = true;
                } else {
                    right = false;
                }
            } else if (name.equals("CharForward")) {
                if (isPressed) {
                    up = true;
                } else {
                    up = false;
                }
            } else if (name.equals("CharBackward")) {
                if (isPressed) {
                    down = true;
                } else {
                    down = false;
                }
            } else if (name.equals("CharJump")) {
                player.getCharacterControl().jump();
            }
        }
    };
    /*
     * Handles mark and attack actions.
     */
    private ActionListener playerShootListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Mark") && !keyPressed) {
                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                terrain.collideWith(ray, results);

                System.out.println(results.size());

                if (results.size() > 0) {
                    CollisionResult closest = results.getClosestCollision();
                    Mark mark = new Mark(player.getTeamColor(), app);
                    mark.setLocalTranslation(closest.getContactPoint());
                    markNode.attachChild(mark); //todo mark node
                }
            }
        }
    };

    private void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setColor(player.getTeamColor());
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation(settings.getWidth() / 2 - ch.getLineWidth() / 2, settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(5f));
        rootNode.addLight(al);
    }

    @Override
    public void simpleUpdate(float tpf) {
        Vector3f camDir = cam.getDirection().clone();
        Vector3f camLeft = cam.getLeft().clone();
        camDir.y = 0;
        camLeft.y = 0;
        camDir.normalizeLocal();
        camLeft.normalizeLocal();
        walkDirection.set(0, 0, 0);

        if (!player.getCharacterControl().isOnGround()) {
            airTime += tpf;
        } else {
            airTime = 0;
        }

        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }

        //change animation
        if (walkDirection.lengthSquared() == 0) { //Use lengthSquared() (No need for an extra sqrt())
            if (!"stand".equals(player.getAnimChannel().getAnimationName())) {
                player.getAnimChannel().setAnim("stand", 1f);
            }
        } else {
            player.getCharacterControl().setViewDirection(walkDirection);
            if (airTime > .5f) {
                if (!"stand".equals(player.getAnimChannel().getAnimationName())) {
                    player.getAnimChannel().setAnim("stand");
                }
            } else if (!"Walk".equals(player.getAnimChannel().getAnimationName())) {
                player.getAnimChannel().setAnim("Walk", 0.7f);
            }
        }

        walkDirection.multLocal(MOVE_SPEED).multLocal(tpf);// The use of the first multLocal here is to control the rate of movement multiplier for character walk speed. The second one is to make sure the character walks the same speed no matter what the frame rate is.
        player.getCharacterControl().setWalkDirection(walkDirection); // THIS IS WHERE THE WALKING HAPPENS
        //cam.lookAtDirection(player.getCharacterControl().getViewDirection(), new Vector3f());
        Vector3f player_pos = player.getWorldTranslation();
        cam.setLocation(new Vector3f(player_pos.getX(), player_pos.getY() + 5f, player_pos.getZ()));
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    /*
     * Adds message to the sending queue.
     */
    private void sendMessage(AbstractMessage msg) {
        messageQueue.add(msg);
    }

    /*
     * Periodically sends messages to the server.
     */
    private class Sender implements Runnable {

        public Sender() {
        }

        public void run() {
            while (true) {
                try {
                    Quaternion q = cam.getRotation();
                    float[] rotation = new float[4];
                                    
                    rotation[0] = q.getX();
                    rotation[1] = q.getY();
                    rotation[2] = q.getZ();
                    rotation[3] = q.getW();
                    client.send(new Moving(player.getWorldTranslation(), rotation));

                    //Send all messages in queue
                    while (messageQueue.size() > 0) {
                        client.send(messageQueue.poll());
                    }
                    
                    Thread.sleep((long) 1 * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public void destroy() {
        client.close();
        super.destroy();
    }
}
