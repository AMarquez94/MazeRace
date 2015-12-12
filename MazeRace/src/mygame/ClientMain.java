package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.Client;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import enums.Team;
import gameobjects.Mark;
import gameobjects.Player;
import java.io.IOException;
import java.util.concurrent.Callable;
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
    private int id;
    private String nickname;
    //player movement
    private boolean left = false, right = false, up = false, down = false;
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private float airTime = 0;
    private final float MOVE_SPEED = 800f;
    //Nickname Variables
    private boolean goodNickname;
    private float counter;
    private MyListenerClass initialListener;
    private BitmapText nickNameHud;
    boolean playing = false;

    public static void main(String[] args) {

        Networking.initialiseSerializables();
        app = new ClientMain();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //flyCam.setEnabled(false);
        setUpNetworking();
        this.pauseOnFocus = false;
        
        /* We initialize the first dialogue to choose nickname */
        goodNickname = false;
        initialListener = new MyListenerClass();
        inputManager.addRawInputListener(initialListener);
        nickNameHud = new BitmapText(guiFont, false);          
        nickNameHud.setSize(guiFont.getCharSet().getRenderedSize()+10);      // font size
        nickNameHud.setColor(ColorRGBA.White);                             // font color
        nickNameHud.setText("Insert nickname: ");
        nickNameHud.setLocalTranslation(    // position
         settings.getWidth()/2 - (guiFont.getLineWidth(nickNameHud.getText() + "    ")),
         settings.getHeight()/2 + (guiFont.getCharSet().getRenderedSize()+10)/2, 0); 
        guiNode.attachChild(nickNameHud);
        nickname = "";
        counter = 0;
    }
    
    private void startGame(Vector3f position, Team team){
        //Remove nickname part
        inputManager.removeRawInputListener(initialListener);
        nickNameHud.removeFromParent();
        
        
        bas = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bas);
        setUpGraph();
        setUpLight();
        setUpWorld();
        setUpKeys();
        setUpCharacter(position,team);
        initCrossHairs();
        playing = true;
    }

    private void setUpNetworking() {
        //Start connection
        try {
            client = Network.connectToServer("127.0.0.1", Networking.PORT);
            client.start();
        } catch (IOException ex) {
            Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Message receiving
        client.addMessageListener(new NetworkMessageListener());
        //Message sending
        messageQueue = new LinkedBlockingQueue<AbstractMessage>();
        Thread t = new Thread(new Sender());
        t.start();
    }

    private void setUpGraph() {
        rootNode.attachChild(markNode);
    }

    private void setUpCharacter(Vector3f position,Team team) {
        player = new Player(team, this);
        player.addAnimEventListener(playerAnimListener);
        player.addToPhysicsSpace(bas);
        player.setLocalTranslation(position);

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
        if (!goodNickname){
            
            /* Nickname part */
            counter += tpf;
            if(counter > 0.5f){
                nickNameHud.setText("Insert nickname: " + nickname + "|");
                if(counter > 1f){
                    counter = 0;
                }
            }
            else{
                nickNameHud.setText("Insert nickname: " + nickname);
            }
        } else if(playing){
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
                    
                    //Send all messages in queue
                    while (messageQueue.size() > 0) {
                        client.send(messageQueue.poll());
                    }
                    
                    Thread.sleep((long) 1 * 30);
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
    
    private void setUpWorld() {
        mat_terrain = new Material(assetManager,
                "Common/MatDefs/Terrain/Terrain.j3md");

        mat_terrain.setTexture("Alpha", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png"));

        Texture grass = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        mat_terrain.setTexture("Tex1", grass);
        mat_terrain.setFloat("Tex1Scale", 64f);

        Texture dirt = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        mat_terrain.setTexture("Tex2", dirt);
        mat_terrain.setFloat("Tex2Scale", 32f);

        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        mat_terrain.setTexture("Tex3", rock);
        mat_terrain.setFloat("Tex3Scale", 128f);

        Texture heightMapImage = assetManager.loadTexture("Textures/Heightmap1.png");
        AbstractHeightMap heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();

        /**
         * 3. We have prepared material and heightmap. Now we create the actual
         * terrain: 3.1) Create a TerrainQuad and name it "my terrain". 3.2) A
         * good value for terrain tiles is 64x64 -- so we supply 64+1=65. 3.3)
         * We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 3.4) As LOD step scale we supply Vector3f(1,1,1). 3.5) We supply the
         * prepared heightmap itself.
         */
        int patchSize = 65;
        terrain = new TerrainQuad("Maze", patchSize, 257, heightmap.getHeightMap());

        terrain.setMaterial(mat_terrain);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 0.5f, 2f);

        terrain.addControl(new RigidBodyControl(0));
        bas.getPhysicsSpace().add(terrain);

        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        terrain.addControl(control);

        rootNode.attachChild(terrain);
    }
    
    public class MyListenerClass implements RawInputListener{
        public void beginInput() {
        }

        public void endInput() {
        }

        public void onJoyAxisEvent(JoyAxisEvent evt) {
        }

        public void onJoyButtonEvent(JoyButtonEvent evt) {
        }

        public void onMouseMotionEvent(MouseMotionEvent evt) {
        }

        public void onMouseButtonEvent(MouseButtonEvent evt) {
        }

        public void onKeyEvent(KeyInputEvent evt) {
            if(evt.isPressed()){
                if(evt.getKeyCode() == KeyInput.KEY_RETURN){
                    try {
                        goodNickname = true;
                        sendMessage(new Connect(nickname));
                    } catch (Throwable ex) {
                        Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else if(evt.getKeyCode() == KeyInput.KEY_BACK){
                    if(nickname.length()>0){
                        nickname = nickname.substring(0, nickname.length()-1);
                    }
                    nickNameHud.setText("Insert nickname: " + nickname + "|");
                }
                else{
                    nickname = nickname + evt.getKeyChar();
                    nickNameHud.setText("Insert nickname: " + nickname + "|");
                }
            }
        }

        public void onTouchEvent(TouchEvent evt) {
        
        }
    }
    
    private class NetworkMessageListener implements MessageListener<Client>{

        public void messageReceived(Client source, Message m) {

            if (m instanceof ConnectionRejected) {
                
                ConnectionRejected message = (ConnectionRejected) m;
                
                String reason = "Connection refused: " + message.getReason();
                
                nickNameHud.setLocalTranslation(    // position
                settings.getWidth()/2 - (guiFont.getLineWidth(reason))/2,
                settings.getHeight()/2 + (guiFont.getCharSet().getRenderedSize()+10)/2, 0); 
                nickNameHud.setText(reason);
            }
            
            if(m instanceof NewPlayerConnected){
                
                System.out.println("Recibido");
                
                NewPlayerConnected message = (NewPlayerConnected) m;
                
                final int receivedID = message.getId();
                final String receivedNick = message.getNickname();
                final Vector3f receivedPosition = message.getPosition();
                final Team receivedTeam = message.getTeam();
                
                if(receivedNick.equals(nickname)){
                    
                    //My connection was approved
                    id = receivedID;
                    
                    ClientMain.this.enqueue(new Callable(){
                        public Object call() throws Exception{
                            startGame(receivedPosition, receivedTeam);
                            return null;
                        }
                    });
                }
            }
        }
    }
}
