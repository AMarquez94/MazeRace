package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.system.JmeContext;
import com.jme3.terrain.geomipmap.TerrainQuad;
import enums.ServerGameState;
import enums.Team;
import gameobjects.ServerPlayer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import maze.Maze;
import mygame.Networking.*;

/**
 * Represents the Login server of the game
 *
 * @authors Alejandro Marquez, Bjorn van der Laan, Dominik Gils
 */
public class LoginServer extends SimpleApplication {

    //CONSTANTS
    protected final static float TIMEOUT = 5f;
    protected static LoginServer app;
    protected final static int MAX_PLAYERS = 6;
    protected final static int PICKUP_MARGIN = 5;
    protected final static int MAX_HEALTH = 100;
    protected final static int DAMAGE_BULLET = 25;
    //GLOBAL VARIABLES
    private static Server server;
    private HostedConnection[] hostedConnections;
    private BulletAppState bas;
    private ServerPlayer[] players;
    private int connectedPlayers;
    private int redPlayers;
    private int bluePlayers;
    private Vector3f treasureLocation;
    private TerrainQuad terrain;
    private float[] timeouts;
    private static Vector3f[] initialPositions;
    private float periodic_threshold = 0; //temporary
    private boolean redServerConnected;
    private boolean blueServerConnected;
    private HostedConnection redServer;
    private HostedConnection blueServer;
    private HashMap<String, Integer> nicknameToId;
    //game state
    private static ServerGameState state;
    private Node shootables;

    /**
     * Starts the Server
     * @param args 
     */
    public static void main(String[] args) {
        app = new LoginServer();
        app.start(JmeContext.Type.Headless);
    }

    public LoginServer() {
        super(new StatsAppState());
    }

    /**
     * Init the server. This includes the connection with the clients, and some 
     * variables.
     */
    @Override
    public void simpleInitApp() {
        Networking.initialiseSerializables();

        try {
            server = Network.createServer(Networking.PORT_LOGIN);
            server.start();

        } catch (IOException ex) {
            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        bas = new BulletAppState();
        stateManager.attach(bas);
        bas.getPhysicsSpace().enableDebug(assetManager);

        treasureLocation = new Vector3f(0f, -100f, 0f); //initial location of the treasure

        terrain = new Maze(this).setUpWorld(rootNode, bas);
        setUpInitialPositions();
        server.addMessageListener(new MessageHandler());
        connectedPlayers = 0;
        redPlayers = 0;
        bluePlayers = 0;

        players = new ServerPlayer[MAX_PLAYERS];
        hostedConnections = new HostedConnection[MAX_PLAYERS];

        timeouts = new float[MAX_PLAYERS];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            timeouts[i] = TIMEOUT;
        }

        nicknameToId = new HashMap<String, Integer>();
        blueServerConnected = false;
        redServerConnected = false;
        state = ServerGameState.GameStopped;
        cam.setLocation(new Vector3f(0.74115396f, -70.0f, -150.33556f));
        this.pauseOnFocus = false;
        shootables = new Node("Shootables");
        rootNode.attachChild(shootables);

        new ServerControlLogin(this);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (state == ServerGameState.GameStopped) {
            //Send a Prepare every second
            if (periodic_threshold > 1) {
                server.broadcast(Filters.in(hostedConnections), packPrepareMessage());
                periodic_threshold = 0;
            } else {
                periodic_threshold++;
            }
        } else if (state == ServerGameState.GameRunning) {
            for (ServerPlayer p : players) {
                if (p != null && p.getHasTreasure() && p.getWorldTranslation().distanceSquared(getSpawnZonePoint(p.getTeam())) < 100) {
                    server.broadcast(Filters.in(hostedConnections), new End(p.getTeam()));
                    ServerControlLogin.changeServerState(ServerGameState.GameStopped);
                }
            }
        }
        tempCounter += tpf;
        if (tempCounter > 5 && redServerConnected && blueServerConnected) {
            tempCounter = 0;
        }
    }
    float tempCounter = 0;

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    /**
     * @param id
     * @return initial position corresponding to id passed as parameter
     */
    public static Vector3f getInitialPosition(int id) {
        return initialPositions[id];
    }

    /**
     * @param team
     * @return the spawn zone point of the team passed as parameter
     */
    public static Vector3f getSpawnZonePoint(Team team) {
        if (team == Team.Blue) {
            return new Vector3f(3.4365673f, -100.00009f, -252.54404f);
        } else {
            return new Vector3f(8.813507f, -100.00002f, 250.53908f);
        }
    }

    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }

    /**
     * @param nickname
     * @return true if the nickname passed as parameter has been chosen for another
     * player
     */
    private boolean repeatedNickname(String nickname) {
        int i = 0;
        boolean rep = false;
        while (i < players.length && !rep) {
            if (players[i] != null && players[i].getNickname().equals(nickname)) {
                rep = true;
            } else {
                i++;
            }
        }
        return rep;
    }

    /*
     * Connects a player and returns its id
     */
    private int connectPlayer(HostedConnection s) {
        int i = 0;
        boolean find = false;
        while (i < players.length && !find) {
            if (hostedConnections[i] == null) {
                hostedConnections[i] = s;
                connectedPlayers++;
                find = true;
            } else {
                i++;
            }
        }
        return i;
    }

    /**
     * Initialize the initial positions array
     */
    private void setUpInitialPositions() {
        initialPositions = new Vector3f[MAX_PLAYERS];
        try {
            //team 1 (color?)
            initialPositions[0] = new Vector3f(0.74115396f, -100.0f, -245.33556f);
            initialPositions[1] = new Vector3f(4.69698f, -100.0f, -245.20134f);
            initialPositions[4] = new Vector3f(8.940145f, -100.0f, -245.1395f);

            // team 2 (color?)
            initialPositions[2] = new Vector3f(-1.7150712f, -100.0f, 241.41965f);
            initialPositions[3] = new Vector3f(-6.002777f, -100.0f, 241.66374f);
            initialPositions[5] = new Vector3f(-12.222459f, -100.0f, 242.18967f);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Selects team for the new player, based on how many players are connected
     * in each team (They have to be balanced)
     * @param id
     * @return 
     */
    private Team chooseTeam(int id) {
        if (redPlayers > bluePlayers) {
            bluePlayers++;
            return Team.Blue;
        } else if (bluePlayers > redPlayers) {
            redPlayers++;
            return Team.Red;
        }
        else {
            if (Math.random() >= 0.5) {
                bluePlayers++;
                return Team.Blue;
            } else {
                redPlayers++;
                return Team.Red;
            }
        }
    }

    /**
     * @param r
     * @return rotation from float[] to Quaternion
     */
    private Quaternion arrayToQuaternion(float[] r) {
        return new Quaternion(r[0], r[1], r[2], r[3]);
    }

    /**
     * Handles the messages received from the Clients
     */
    private class MessageHandler implements MessageListener<HostedConnection> {

        public void messageReceived(final HostedConnection source, final Message m) {
            if (m instanceof Aggregation) {
                actionAggregation(source, m);
            } //Below this is legacy. Server should only receive aggregation packages.
            else if (m instanceof Connect) {
                actionConnect(source, m);
            } else if (m instanceof PlayerMoved) {
                actionPlayerMoved(source, m);
            } else if (m instanceof MarkInput) {
                actionMarkInput(source, m);
            } else if (m instanceof FireInput) {
                actionFireInput(source, m);
            } else if (m instanceof PickTreasureInput) {
                actionPickTreasureInput(source, m);
            } else if (m instanceof WantToRespawn) {
                actionWantToRespawn(source, m);
            } else if (m instanceof LoginConnected) {
                actionLoginConnected(source, m);
            }
        }

        /**
         * Processes an agregation message
         * @param source
         * @param m 
         */
        private void actionAggregation(final HostedConnection source, final Message m) {
            final Aggregation aggregation = (Aggregation) m;
            //get messages
            ArrayList<AbstractMessage> messages = aggregation.getMessages();

            //process messages
            for (final AbstractMessage message : messages) {
                if (message != null) {
                    if (message instanceof Connect) {
                        actionConnect(source, message);
                    } else if (message instanceof PlayerMoved) {
                        actionPlayerMoved(source, message);
                    } else if (message instanceof MarkInput) {
                        actionMarkInput(source, message);
                    } else if (message instanceof FireInput) {
                        actionFireInput(source, message);
                    } else if (message instanceof PickTreasureInput) {
                        actionPickTreasureInput(source, message);
                    } else if (message instanceof WantToRespawn) {
                        actionWantToRespawn(source, message);
                    }
                }
            }
        }

        /**
         * Process a connect message
         * @param source
         * @param m 
         */
        private void actionConnect(final HostedConnection source, final Message m) {
            if (m instanceof Connect) {
                Connect message = (Connect) m;
                final String nickname = message.getNickname();
                if (state == ServerGameState.GameStopped) {
                    if (connectedPlayers != MAX_PLAYERS) {
                        if (nickname.length() > 0 && nickname.length() <= 8) {
                            if (!repeatedNickname(nickname)) {
                                //TODO ID must be saved
                                app.enqueue(new Callable() {
                                    public Object call() throws Exception {
                                        int idNew = connectPlayer(source);
                                        nicknameToId.put(nickname, idNew);
                                        blueServer.send(new NewConnection(nickname, idNew));
                                        redServer.send(new NewConnection(nickname, idNew));
                                        final Team newTeam = chooseTeam(idNew);
                                        if (newTeam.equals(Team.Blue)) {
                                            source.send(new ConnectServer(Networking.HOST_BLUE, Networking.PORT_BLUE));
                                        } else if (newTeam.equals(Team.Red)) {
                                            source.send(new ConnectServer(Networking.HOST_RED, Networking.PORT_RED));
                                        }
                                        return null;
                                    }
                                });

                            } else {
                                server.broadcast(Filters.equalTo(source),
                                        new ConnectionRejected("Nickname already in use"));
                            }
                        } else {
                            server.broadcast(Filters.equalTo(source),
                                    new ConnectionRejected("Bad nickname"));
                        }
                    } else {
                        server.broadcast(Filters.equalTo(source),
                                new ConnectionRejected("Maximum number of clients already connected"));
                    }
                } else {
                    server.broadcast(Filters.equalTo(source),
                            new ConnectionRejected("Game has already started"));
                }
            }
        }

        /**
         * Processes an PlayerMoved message
         * @param source
         * @param m 
         */
        private void actionPlayerMoved(final HostedConnection source, final Message m) {
            if (m instanceof PlayerMoved) {

                final int id = findId(source);
                timeouts[id] = TIMEOUT;

                PlayerMoved message = (PlayerMoved) m;

                final String animation = message.getAnimation();
                final Vector3f position = message.getPosition();
                final float[] rotation = message.getRotation();
                final Vector3f orientation = message.getOrientation();


                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        players[id].setPosition(position);
                        players[id].setOrientation(arrayToQuaternion(rotation));
                        server.broadcast(Filters.in(hostedConnections),
                                new MovingPlayers(id, position, rotation, orientation, animation));
                        return null;
                    }
                });
            }
        }

        /**
         * Processes an MarkInput message
         * @param source
         * @param m 
         */
        private void actionMarkInput(final HostedConnection source, final Message m) {
            if (m instanceof MarkInput) {
                MarkInput message = (MarkInput) m;
                final int id = findId(source);
                timeouts[id] = TIMEOUT;

                final Vector3f direction = message.getDirection();
                final Vector3f position = message.getPosition();

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (state == ServerGameState.GameRunning) {
                            CollisionResults results = new CollisionResults();

                            Ray ray = new Ray(position, direction);
                            terrain.collideWith(ray, results);

                            if (results.size() > 0) {
                                CollisionResult closest = results.getClosestCollision();
                                server.broadcast(Filters.in(hostedConnections),
                                        new PutMark(players[id].getTeam(), closest.getContactPoint()));
                            }
                        }
                        return null;
                    }
                });
            }
        }

        /**
         * Processes an FireInput message
         * @param source
         * @param m 
         */
        private void actionFireInput(final HostedConnection source, final Message m) {
            if (m instanceof FireInput) {

                FireInput message = (FireInput) m;
                final int id = findId(source);
                timeouts[id] = TIMEOUT;

                final Vector3f direction = message.getDirection();
                final Vector3f position = message.getPosition();

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (state == ServerGameState.GameRunning) {
                            CollisionResults results = new CollisionResults();
                            Ray ray = new Ray(position, direction);

                            shootables.collideWith(ray, results);
                            if (results.size() > 0) {
                                int shooted = checkShooted(id, results);
                                if (shooted >= 0) {
                                    boolean dead = players[shooted].decreaseHealth(DAMAGE_BULLET);

                                    if (dead) {
                                        players[shooted].setDead(true);

                                        server.broadcast(Filters.in(hostedConnections),
                                                new DeadPlayer(shooted, id));

                                        if (players[shooted].getHasTreasure()) {
                                            treasureLocation = players[shooted].getWorldTranslation();
                                            treasureLocation.setY(-100f); // to prevent floating treasure

                                            server.broadcast(Filters.in(hostedConnections),
                                                    new TreasureDropped(treasureLocation));
                                            players[shooted].setHasTreasure(false);
                                        }
                                    } else {
                                        server.broadcast(Filters.in(hostedConnections),
                                                new PlayerShooted(shooted, id, players[shooted].getHealth()));
                                    }
                                }
                            }
                        }
                        return null;
                    }
                });

                //send message to tell clients that shot is fired
                server.broadcast(new Firing(id));
            }
        }

        /**
         * Processes an PickTreasureInput message
         * @param source
         * @param m 
         */
        private void actionPickTreasureInput(final HostedConnection source, final Message m) {
            if (m instanceof PickTreasureInput) {
                int id = findId(source);

                server.broadcast(Filters.in(hostedConnections), new TreasurePicked(findId(source)));
                players[id].setHasTreasure(true);
            }
        }

        /**
         * Processes an WantToRespawn message
         * @param source
         * @param m 
         */
        private void actionWantToRespawn(final HostedConnection source, final Message m) {
            final int id = findId(source);

            players[id].setDead(false);
            players[id].setHealth(MAX_HEALTH);
            players[id].setPosition(initialPositions[id]);

            server.broadcast(Filters.in(hostedConnections), new PlayerRespawn(id, initialPositions[id]));
        }

        /**
         * Connect the Login server with red or blue server
         * @param source
         * @param m 
         */
        private void actionLoginConnected(final HostedConnection source, final Message m) {
            LoginConnected message = (LoginConnected) m;
            if (message.getOrigin().equals("red")) {
                redServerConnected = true;
                redServer = source;
                System.out.println("LoginServer: RedServer connected");
            } else if (message.getOrigin().equals("blue")) {
                blueServerConnected = true;
                blueServer = source;
                System.out.println("LoginServer: BlueServer connected");
            }
        }
    }

    /**
     * @return Server game state
     */
    protected static ServerGameState getGameState() {
        return state;
    }

    /**
     * Change the server state to the new one passed as parameter
     * @param newState 
     */
    protected static void changeGameState(ServerGameState newState) {
        if (!(state == newState)) {
            if (newState == ServerGameState.GameRunning) {
                server.broadcast(new Start());
            } else if (newState == ServerGameState.GameStopped) {
            }

            state = newState;
        }
    }

    /**
     * @return a packed PrepareMessage, with all the fields needed
     */
    private Prepare packPrepareMessage() {
        Vector3f[] positions = new Vector3f[MAX_PLAYERS];
        float[][] orientations = new float[MAX_PLAYERS][4];
        String[] nickname = new String[MAX_PLAYERS];
        Team[] teams = new Team[MAX_PLAYERS];

        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] == null) {

                nickname[i] = ""; //This means no player connected with that id
                positions[i] = new Vector3f(0, 0, 0);
                orientations[i][0] = 0;
                orientations[i][1] = 0;
                orientations[i][2] = 0;
                orientations[i][3] = 0;
                teams[i] = Team.Blue;
            } else {
                nickname[i] = players[i].getNickname();
                positions[i] = players[i].getPosition();
                teams[i] = players[i].getTeam();
                orientations[i] = players[i].getRotationFloat();
            }
        }

        return new Prepare(positions, orientations, nickname, teams);
    }

    /**
     * @param source
     * @return id corresponding to the HostedConnection passed by parameter
     */
    private int findId(HostedConnection source) {
        int i = 0;
        boolean find = false;
        while (i < hostedConnections.length && !find) {
            if (hostedConnections[i] != null && hostedConnections[i].equals(source)) {
                find = true;
            } else {
                i++;
            }
        }
        if (i < hostedConnections.length) {
            return i;
        } else {
            return -1;
        }
    }

    /**
     * Returns the index of the first player that has been shooted. (-1 if
     * didn't shoot anything)
     */
    private int checkShooted(int id, CollisionResults results) {
        int result = -1;
        int i = 0;
        boolean find = false;
        while (i < results.size() && !find) {
            int shooted = Integer.parseInt(results.getCollision(i)
                    .getGeometry().getParent().getName());

            if (id != shooted && !players[shooted].isDead()) {
                result = shooted;
                find = true;
            } else {
                i++;
            }
        }
        return result;
    }

    /**
     * @param nick
     * @return id corresponding to the nickname passed as parameter
     */
    public int getId(String nick) {
        return nicknameToId.get(nick);
    }
}
