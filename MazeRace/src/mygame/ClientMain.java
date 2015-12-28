package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.font.BitmapText;
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
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import enums.ClientGameState;
import enums.Team;
import gameobjects.Mark;
import gameobjects.Player;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import maze.Maze;
import mygame.Networking.*;
import static gameobjects.Player.*;
import maze.Treasure;
import static mygame.ServerMain.*;

/**
 * MazeRace (client).
 *
 * @author NVE Project
 */
public class ClientMain extends SimpleApplication {

    private static ClientMain app;
    private Client client;
    private LinkedBlockingQueue<AbstractMessage> messageQueue;
    private BulletAppState bas;
    //scene graph
    private Node markNode = new Node("Marks");
    private Node playerNode = new Node("Players");
    private Treasure treasureNode = null;
    //terrain
    private TerrainQuad terrain;
    private Material mat_terrain;
    //own id
    private static int id;
    //players
    private HashMap<Integer, Player> players = new HashMap<Integer, Player>();
    //player movement
    private boolean left = false, right = false, up = false, down = false;
    //Game state
    private static ClientGameState state;
    //Nickname Variables (used in nicknameHUD)
    private String nickname;
    private boolean goodNickname;
    private float counter;
    private NicknameHUDListener initialListener;
    private BitmapText nickNameHud;
    
    private String nickNameHudAux = "";

    public static void main(String[] args) {
        app = new ClientMain();

        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(60);
        app.setSettings(settings);

        app.start();
    }

    @Override
    public void simpleInitApp() {
        state = ClientGameState.NicknameScreen;
        Networking.initialiseSerializables();
        

        //flyCam.setEnabled(false);
        setUpNetworking();
        this.pauseOnFocus = false;

        /* We initialize the first dialogue to choose nickname */
        goodNickname = false;
        initialListener = new NicknameHUDListener();
        inputManager.addRawInputListener(initialListener);
        nickNameHud = new BitmapText(guiFont, false);
        nickNameHud.setSize(guiFont.getCharSet().getRenderedSize() + 10);      // font size
        nickNameHud.setColor(ColorRGBA.White);                             // font color
        nickNameHud.setText("Insert nickname: ");
        nickNameHud.setLocalTranslation( // position
                settings.getWidth() / 2 - (guiFont.getLineWidth(nickNameHud.getText() + "    ")),
                settings.getHeight() / 2 + (guiFont.getCharSet().getRenderedSize() + 10) / 2, 0);
        guiNode.attachChild(nickNameHud);
        nickname = "";
        counter = 0;

        //Set up the environment
        bas = new BulletAppState();
        
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bas);
        bas.getPhysicsSpace().enableDebug(assetManager);
        setUpGraph();
        setUpLight();
        setUpKeys();
        new Maze(this).setUpWorld(rootNode, bas);
    }

    private Player getPlayer() {
        return this.players.get(id);
    }

    /*
     * Starts the game.
     */
    private void startGame() {
        state = ClientGameState.GameRunning;
        System.out.println("The game has begun");
    }

    /*
     * Ends the game and announces the winners
     */
    private void endGame(Team winner) {
        state = ClientGameState.GameStopped;
        //TODO announce winning team
    }

    /*
     * Removes player with given id.
     */
    private void removePlayer(int id) {
        players.remove(id);
        //TODO remove avatar from the scene graph
    }

    private void setUpNetworking() {
        //Start connection
        try {
            client = Network.connectToServer(Networking.HOST, Networking.PORT);
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
        rootNode.attachChild(playerNode);
    }

    /*
     * Adds the player to the environment.
     */
    private void setUpCharacter(int id, Team team, Vector3f position, String nick) {
        if (players.containsKey(id)) { //if player already exists
            return;
        }

        //create player and store in players map
        Player p = new Player(team, position, nick, app);
        players.put(id, p);

        //adds the player to the game
        //players.get(id).addAnimEventListener(playerAnimListener);
        players.get(id).addToPhysicsSpace(bas);
        rootNode.attachChild(players.get(id));
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
        inputManager.addMapping("Shoot", new KeyTrigger(KeyInput.KEY_N), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("PickUp", new KeyTrigger(KeyInput.KEY_B)); //maybe find a better binding?
        inputManager.addMapping("Mark", new KeyTrigger(KeyInput.KEY_M), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(playerMoveListener, "CharLeft", "CharRight", "CharForward", "CharBackward", "CharJump");
        inputManager.addListener(playerShootListener, "Shoot", "Mark", "PickUp");
    }
    /*
     * Handles player movement actions.
     */
    private ActionListener playerMoveListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (state == ClientGameState.GameRunning) {
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
                    getPlayer().getCharacterControl().jump();
                }
            }
        }
    };
    /*
     * Handles mark and attack actions.
     */
    private ActionListener playerShootListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (state == ClientGameState.GameRunning) {
                if (name.equals("Mark") && !keyPressed) {
                    sendMessage(new MarkInput(cam.getDirection(),cam.getLocation()));
                } else if (name.equals("Shoot") && !keyPressed) {
                    sendMessage(new FireInput(cam.getDirection(),cam.getLocation()));
                } else if (name.equals("PickUp") && !keyPressed) {
                    //if treasure is not picked up already
                    if (rootNode.hasChild(treasureNode)) {
                        Vector3f player_pos = getPlayer().getWorldTranslation();
                        Vector3f treasure_pos = treasureNode.getWorldTranslation();
                        float distance = player_pos.distance(treasure_pos);

                        if (distance < PICKUP_MARGIN) { //TODO test for a good value for PICKUP_MARGIN
                            sendMessage(new PickTreasureInput(cam.getLocation(), cam.getDirection()));
                        }
                    }
                }
            }
        }
    };

    private void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setColor(getPlayer().getTeamColor());
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
        if (state == ClientGameState.NicknameScreen || getPlayer() == null) {
            
            
            /* Nickname part */
            counter += tpf;
            if (counter > 0.5f) {
                nickNameHud.setText(nickNameHudAux + "Insert nickname: " + nickname + "|");
                if (counter > 1f) {
                    counter = 0;
                }
            } else {
                nickNameHud.setText(nickNameHudAux + "Insert nickname: " + nickname);
            }
        } else if (state == ClientGameState.GameRunning) {
            Vector3f camDir = cam.getDirection().clone();
            Vector3f camLeft = cam.getLeft().clone();
            camDir.y = 0;
            camLeft.y = 0;
            camDir.normalizeLocal();
            camLeft.normalizeLocal();
            getPlayer().walkDirection.set(0, 0, 0);

            if (!getPlayer().getCharacterControl().isOnGround()) {
                getPlayer().airTime += tpf;
            } else {
                getPlayer().airTime = 0;
            }

            if (left) {
                getPlayer().walkDirection.addLocal(camLeft);
            }
            if (right) {
                getPlayer().walkDirection.addLocal(camLeft.negate());
            }
            if (up) {
                getPlayer().walkDirection.addLocal(camDir);
            }
            if (down) {
                getPlayer().walkDirection.addLocal(camDir.negate());
            }

            //change animation
            if (getPlayer().walkDirection.lengthSquared() == 0) { //Use lengthSquared() (No need for an extra sqrt())
                if (!"stand".equals(getPlayer().getAnimChannel().getAnimationName())) {
                    getPlayer().getAnimChannel().setAnim("stand", 1f);
                }
            } else {
                getPlayer().getCharacterControl().setViewDirection(getPlayer().walkDirection);
                if (getPlayer().airTime > .5f) {
                    if (!"stand".equals(getPlayer().getAnimChannel().getAnimationName())) {
                        getPlayer().getAnimChannel().setAnim("stand");
                    }
                } else if (!"Walk".equals(getPlayer().getAnimChannel().getAnimationName())) {
                    getPlayer().getAnimChannel().setAnim("Walk", 0.7f);
                }
            }

            getPlayer().walkDirection.multLocal(MOVE_SPEED).multLocal(tpf);// The use of the first multLocal here is to control the rate of movement multiplier for character walk speed. The second one is to make sure the character walks the same speed no matter what the frame rate is.

            getPlayer().getCharacterControl().setWalkDirection(getPlayer().walkDirection); // THIS IS WHERE THE WALKING HAPPENS

            Vector3f player_pos = getPlayer().getWorldTranslation();
            getPlayer().setPosition(player_pos);
            //cam.lookAtDirection(getPlayer().getCharacterControl().getViewDirection(), new Vector3f());
            cam.setLocation(new Vector3f(player_pos.getX(), player_pos.getY() + 5f, player_pos.getZ()));

            //send new state to server TODO: rotation
            sendMessage(new PlayerMoved(getPlayer().getPosition(),
                    quaternionToArray(getPlayer().getWorldRotation()),
                    getPlayer().getCharacterControl().getViewDirection(),
                    getPlayer().getAnimChannel().getAnimationName()));
        } else if (state == ClientGameState.GameStopped) {
            Vector3f player_pos = getPlayer().getWorldTranslation();
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

    private float[] quaternionToArray(Quaternion q) {
        float[] f = new float[4];
        f[0] = q.getX();
        f[1] = q.getY();
        f[2] = q.getZ();
        f[3] = q.getW();
        return f;
    }

    public class NicknameHUDListener implements RawInputListener {

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
            if (evt.isPressed()) {
                if (evt.getKeyCode() == KeyInput.KEY_RETURN) {
                    try {
                        goodNickname = true;
                        sendMessage(new Connect(nickname));
                    } catch (Throwable ex) {
                        Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (evt.getKeyCode() == KeyInput.KEY_BACK) {
                    if (nickname.length() > 0) {
                        nickname = nickname.substring(0, nickname.length() - 1);
                    }
                    nickNameHud.setText("Insert nickname: " + nickname + "|");
                } else {
                    nickname = nickname + evt.getKeyChar();
                    nickNameHud.setText("Insert nickname: " + nickname + "|");
                }
            }
        }

        public void onTouchEvent(TouchEvent evt) {
        }
    }

    private class NetworkMessageListener implements MessageListener<Client> {

        public void messageReceived(Client source, Message m) {

            if (m instanceof ConnectionRejected) {

                ConnectionRejected message = (ConnectionRejected) m;
                
                final String reason = "Connection refused: " + message.getReason();
                
                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        nickNameHud.setLocalTranslation( // position
                            settings.getWidth() / 2 - (guiFont.getLineWidth(reason)) / 2,
                            settings.getHeight() / 2 + (guiFont.getCharSet().getRenderedSize() + 10) / 2, 0);
                        nickNameHudAux = reason + "\n";
                        return null;
                    }
                });
                
            } else if (m instanceof NewPlayerConnected) {
                System.out.println("A player connected");
                final NewPlayerConnected message = (NewPlayerConnected) m;

                //Set up the character
                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        setUpCharacter(message.getId(), message.getTeam(), message.getPosition(), message.getNickname());
                        return null;
                    }
                });

                //if it is my own connection
                if (message.getNickname().equals(nickname)) {
                    id = message.getId();

                    app.enqueue(new Callable() {
                        public Object call() throws Exception {
                            //Remove nickname part
                            inputManager.removeRawInputListener(initialListener);
                            nickNameHud.removeFromParent();
                            initCrossHairs();
                            return null;
                        }
                    });
                }
            } else if (m instanceof DisconnectedPlayer) {
                DisconnectedPlayer message = (DisconnectedPlayer) m;
                final int id = message.getPlayerID();
                removePlayer(id);
                System.out.println("Player " + id + " left the game.");
            } else if (m instanceof MovingPlayers) {
                final MovingPlayers message = (MovingPlayers) m;

                if (id != message.getPlayerID()) {
                    app.enqueue(new Callable() {
                        public Object call() throws Exception {
                            //TODO set rotation
                            Player p = players.get(message.getPlayerID());
                            p.getCharacterControl().warp(message.getPosition());
                            p.setPosition(message.getPosition());
                            Vector3f rotation = message.getOrientation();
                            p.getCharacterControl().setViewDirection(rotation);

                            //change anim only if not the same, else shocking motion
                            if (!p.getAnimChannel().getAnimationName().equals(message.getAnimation())) {
                                p.getAnimChannel().setAnim(message.getAnimation());
                            }

                            return null;
                        }
                    });
                }
            } else if (m instanceof Firing) {
                final Firing message = (Firing) m;

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        players.get(message.getPlayerID()).playGunAudio();
                        return null;
                    }
                });
            } else if (m instanceof PlayerShooted) {
                final PlayerShooted message = (PlayerShooted) m;
                
                System.out.println("I (" + message.getShootedPlayerId() + ") have been shooted by " + message.getShootingPlayerId());
                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        
                        int idShooted = message.getShootedPlayerId();
                        int idShooting = message.getShootingPlayerId();
                        int newHealth = message.getNewHealth();
                        System.out.println("new health = " + newHealth);
                        players.get(idShooted).setHealth(newHealth);
                        return null;
                    }
                });
                //TODO decrease health points?
            } else if (m instanceof DeadPlayer) {
                final DeadPlayer message = (DeadPlayer) m;

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        //Remove dead player from the scene graph
                        playerNode.detachChild(players.get(message.getDeadPlayer()));
                        return null;
                    }
                });

            } else if (m instanceof PlayerRespawn) {
                final PlayerRespawn message = (PlayerRespawn) m;
                final int id = message.getPlayerRespawn();
                final Vector3f position = message.getPosition();

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        //move player back to its initial position (spawn point)
                        players.get(id).setLocalTranslation(position);
                        playerNode.attachChild(players.get(id));
                        return null;
                    }
                });
            } else if (m instanceof PutMark) {
                final PutMark message = (PutMark) m;

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        Mark mark = new Mark(message.getTeam(), app);
                        mark.setLocalTranslation(message.getPosition());
                        markNode.attachChild(mark);
                        return null;
                    }
                });
            } else if (m instanceof TreasureDropped) {
                TreasureDropped message = (TreasureDropped) m;
                final Vector3f location = message.getLocation();

                //create treasure if not exists yet
                if (treasureNode == null) {
                    treasureNode = new Treasure(app, bas);
                    rootNode.attachChild(treasureNode);
                }

                //put treasure in position
                treasureNode.setLocalTranslation(location);
            } else if (m instanceof TreasurePicked) {
                TreasurePicked message = (TreasurePicked) m;
                final int id = message.getPlayerID();
                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        //remove treasure from scene graph
                        rootNode.detachChild(rootNode.getChild("Treasure"));

                        //let player display it has treasure
                        players.get(id).setTreasureMode(true);

                        //other players should not
                        for (int i : players.keySet()) {
                            if (i != id) {
                                players.get(i).setTreasureMode(false);
                            }
                        }
                        return null;
                    }
                });
            } else if (m instanceof Start) {
                startGame();
            } else if (m instanceof End) {
                final End message = (End) m;
                endGame(message.winnerTeam);
            } else if (m instanceof Pause) {
                //not implemented yet
            } else if (m instanceof Resume) {
                //not implemented yet
            } else if (m instanceof Prepare) {
                final Prepare message = (Prepare) m;
                final String[] nicknames = message.getNicknames();
                final Team[] teams = message.getTeams();
                final Vector3f[] positions = message.getPositions();
                final float[][] orientations = message.getOrientations();

                for (int i = 0; i < message.getNicknames().length; i++) {
                    if (!players.containsKey(i) && !nicknames[i].equals("")) { //if player does not already exist
                        final int id = i;
                        app.enqueue(new Callable() {
                            public Object call() throws Exception {
                                //Set up the character. TODO does not include orientation (maybe not needed)
                                setUpCharacter(id, teams[id], positions[id], nicknames[id]);
                                return null;
                            }
                        });
                    }
                }
            }
        }
    }
}
