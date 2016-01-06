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
import com.jme3.network.Filter;
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
import gameobjects.Player;
import gameobjects.ServerPlayer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import maze.Maze;
import mygame.Networking.*;

/**
 * test
 *
 * @author normenhansen
 */
public class ServerMain extends SimpleApplication {

    //CONSTANTS
    protected final static float TIMEOUT = 5f;
    protected static ServerMain app;
    protected final static int MAX_PLAYERS = 6;
    protected final static int PICKUP_MARGIN = 5;
    protected final static int MAX_HEALTH = 100;
    protected final static int DAMAGE_BULLET = 25;
    //GLOBAL VARIABLES
    private static Server server;
    private static LinkedBlockingQueue<AbstractMessage> messageQueue;
    private HostedConnection[] hostedConnections;
    private ArrayList<HostedConnection> redPlayersCon;
    private ArrayList<HostedConnection> bluePlayersCon;
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
    //game state
    private static ServerGameState state;
    private Node shootables;
    //players being seen
    private boolean[][] seen;

    //
    public static void main(String[] args) {
        app = new ServerMain();
        app.start(JmeContext.Type.Headless);
    }

    public ServerMain() {
        super(new StatsAppState());
    }

    @Override
    public void simpleInitApp() {
        Networking.initialiseSerializables();

        try {
            String port = JOptionPane.showInputDialog("Insert server port number", Networking.PORT_LOGIN);
            int portNumber = Integer.parseInt(port);
            server = Network.createServer(portNumber);
            server.start();

        } catch (IOException ex) {
            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        bas = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
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
//        nicknames = new String[MAX_PLAYERS];
//        for (int i = 0; i < nicknames.length; i++) {
//            nicknames[i] = "";
//        }
        hostedConnections = new HostedConnection[MAX_PLAYERS];
        redPlayersCon = new ArrayList<HostedConnection>();
        bluePlayersCon = new ArrayList<HostedConnection>();

        timeouts = new float[MAX_PLAYERS];
//        for (int i = 0; i < MAX_PLAYERS; i++) {
//            timeouts[i] = 0;
//        }

        state = ServerGameState.GameStopped;
        cam.setLocation(new Vector3f(0.74115396f, -70.0f, -150.33556f));
        this.pauseOnFocus = false;
        shootables = new Node("Shootables");
        rootNode.attachChild(shootables);

        seen = new boolean[MAX_PLAYERS][MAX_PLAYERS];

        new ServerControl(this);


        //networking
        messageQueue = new LinkedBlockingQueue<AbstractMessage>();
        Thread t = new Thread(new Sender());
        t.start();

    }

    @Override
    public void simpleUpdate(float tpf) {
        for (int i = 0; i < timeouts.length; i++) {
            if (timeouts[i] != 0) {
                //System.out.println("Timeout " + i + " : " + timeouts[i]);
                timeouts[i] = timeouts[i] - tpf;
                if (timeouts[i] <= 0) {
                    disconnectPlayer(i);
                }
            }
        }
        if (state == ServerGameState.GameStopped) {
            //Send a Prepare every second. TODO implement this better.
            if (periodic_threshold > 1) {
                broadcastMessage(packPrepareMessage());
                periodic_threshold = 0;
            } else {
                periodic_threshold++;
            }
        } else if (state == ServerGameState.GameRunning) {
            for (ServerPlayer p : players) {
                if (p != null && p.getHasTreasure() && p.getWorldTranslation().distanceSquared(getSpawnZonePoint(p.getTeam())) < 100) {

                    ServerControlLogin.changeServerState(ServerGameState.GameStopped);
                    broadcastMessage(new End(p.getTeam()));
                }
                checkOtherPlayers(p);
            }
        }
    }

    /*
     * Queue broadcast message for aggregation.
     */
    private static void broadcastMessage(AbstractMessage message) {
        messageQueue.add(message);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public static Vector3f getInitialPosition(int id) {
        return initialPositions[id];
    }

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
    private int connectPlayer(String nickname, HostedConnection s) {
        int i = 0;
        boolean find = false;
        while (i < players.length && !find) {
            if (players[i] == null) {
                Team team = chooseTeam(i);
                Vector3f position;
                if (team == Team.Blue) {
                    position = initialPositions[i % MAX_PLAYERS / 2];
                } else {
                    position = initialPositions[MAX_PLAYERS / 2 + i % MAX_PLAYERS / 2];
                }
                players[i] = new ServerPlayer(i, team, position,
                        nickname, app);
                hostedConnections[i] = s;
                if (players[i].getTeam() == Team.Blue) {
                    bluePlayersCon.add(s);
                } else {
                    redPlayersCon.add(s);
                }
                connectedPlayers++;
                shootables.attachChild(players[i]);
                timeouts[i] = TIMEOUT;
                find = true;
            } else {
                i++;
            }
        }
        return i;
    }

    private void disconnectPlayer(int id) {
        if (players[id].getTeam() == Team.Blue) {
            bluePlayersCon.remove(hostedConnections[id]);
            bluePlayers--;
        } else {
            redPlayersCon.remove(hostedConnections[id]);
            redPlayers--;
        }
        hostedConnections[id] = null;
        connectedPlayers--;
        shootables.detachChild(players[id]);
        players[id] = null;
        broadcastMessage(new DisconnectedPlayer(id));
        timeouts[id] = 0;
    }

    private void setUpInitialPositions() {
        initialPositions = new Vector3f[MAX_PLAYERS];
        try {
            //team 1 (color?)
            initialPositions[0] = new Vector3f(0.74115396f, -100.0f, -245.33556f);
            initialPositions[1] = new Vector3f(4.69698f, -100.0f, -245.20134f);
            initialPositions[2] = new Vector3f(8.940145f, -100.0f, -245.1395f);

            // team 2 (color?)
            initialPositions[3] = new Vector3f(-1.7150712f, -100.0f, 241.41965f);
            initialPositions[4] = new Vector3f(-6.002777f, -100.0f, 241.66374f);
            initialPositions[5] = new Vector3f(-12.222459f, -100.0f, 242.18967f);
        } catch (Exception e) {
            System.out.println(e);
        }
//        for (int i = 0; i < MAX_PLAYERS; i++) {
//            initialPositions[i] = new Vector3f(0, 0, 0);
//        }
    }

    private Team chooseTeam(int id) {
        if (redPlayers > bluePlayers) {
            bluePlayers++;
            return Team.Blue;
        } else if (bluePlayers > redPlayers) {
            redPlayers++;
            return Team.Red;
        } //TODO RANDOM
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

    private void checkOtherPlayers(ServerPlayer p) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (p != null & players[i] != null && p != players[i]) {
                if (p.getPosition().distance(players[i].getPosition()) < Player.VIEW_DISTANCE) {
                    if (!seen[p.getId()][i]) {
                        seen[p.getId()][i] = true;
                        server.broadcast(Filters.in(hostedConnections[p.getId()]), new ShowMessage(true, i));

                    }
                } else {
                    if (seen[p.getId()][i]) {
                        seen[p.getId()][i] = false;
                        server.broadcast(Filters.in(hostedConnections[p.getId()]), new ShowMessage(false, i));
                    }
                }
            }
        }
    }

    private Quaternion arrayToQuaternion(float[] r) {
        return new Quaternion(r[0], r[1], r[2], r[3]);
    }

    /*
     * Periodically sends messages.
     */
    private class Sender implements Runnable {

        //Algorithm settings
        private final long TIMEOUT = 300; //timeout in milliseconds
        private final int QUORUM = 4; //quorum in amount of messages

        public Sender() {
        }

        public void run() {
            long timer; //declare timer
            Aggregation message; //declare aggregation packet

            while (QUORUM > 0) { //loop forever
                if (messageQueue.size() > 0) { //wait until update is available
                    message = new Aggregation(); //create an empty packet
                    message.addMessage(messageQueue.poll()); //add update to packet
                    timer = System.currentTimeMillis(); //set timer

                    quorum_loop:
                    while (message.getSize() < QUORUM) { //while quorum is not reached
                        if (System.currentTimeMillis() - timer > TIMEOUT) { //check for timeout
                            break quorum_loop;
                        } else if (messageQueue.size() > 0) { // else wait for more updates
                            message.addMessage(messageQueue.poll()); //add update to packet
                        }
                    }

                    server.broadcast(message); //send packet
                }
            }
        }
    }

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
            }
        }

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
                        int id = findId(source);
                        timeouts[id] = TIMEOUT;
                        actionPlayerMoved(source, message);
                    } else if (message instanceof MarkInput) {
                        int id = findId(source);
                        timeouts[id] = TIMEOUT;
                        actionMarkInput(source, message);
                    } else if (message instanceof FireInput) {
                        int id = findId(source);
                        timeouts[id] = TIMEOUT;
                        actionFireInput(source, message);
                    } else if (message instanceof PickTreasureInput) {
                        int id = findId(source);
                        timeouts[id] = TIMEOUT;
                        actionPickTreasureInput(source, message);
                    } else if (message instanceof WantToRespawn) {
                        int id = findId(source);
                        timeouts[id] = TIMEOUT;
                        actionWantToRespawn(source, message);
                    } else if (message instanceof SendMessage) {
                        int id = findId(source);
                        timeouts[id] = TIMEOUT;
                        actionSendMessage(source, message);
                    } else if (message instanceof Alive) {
                        int id = findId(source);
                        timeouts[id] = TIMEOUT;
                    }
                }
            }
        }

        private void actionConnect(final HostedConnection source, final Message m) {
            System.out.println("Connect request received");
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
                                        //Set up the character. TODO does not include orientation (maybe not needed)
                                        int idNew = connectPlayer(nickname, source);
                                        broadcastMessage(
                                                new NewPlayerConnected(idNew, players[idNew].getNickname(),
                                                players[idNew].getTeam(), players[idNew].getPosition()));
                                        broadcastMessage(
                                                packPrepareMessage());
                                        broadcastMessage(new TreasureDropped(treasureLocation));
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

        private void actionPlayerMoved(final HostedConnection source, final Message m) {
            if (m instanceof PlayerMoved) {

                final int id = findId(source);

                PlayerMoved message = (PlayerMoved) m;

                final String animation = message.getAnimation();
                final Vector3f position = message.getPosition();
                final float[] rotation = message.getRotation();
                final Vector3f orientation = message.getOrientation();


                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        players[id].setPosition(position);
                        players[id].setOrientation(arrayToQuaternion(rotation));
                        broadcastMessage(
                                new MovingPlayers(id, position, rotation, orientation, animation));
                        return null;
                    }
                });
            }
        }

        private void actionMarkInput(final HostedConnection source, final Message m) {
            if (m instanceof MarkInput) {
                MarkInput message = (MarkInput) m;
                final int id = findId(source);

                final Vector3f direction = message.getDirection();
                final Vector3f position = message.getPosition();

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (state == ServerGameState.GameRunning) {
                            CollisionResults results = new CollisionResults();

                            //Must be changed by the coordinates and direction of the character
                            Ray ray = new Ray(position, direction);
                            terrain.collideWith(ray, results);

                            if (results.size() > 0) {
                                CollisionResult closest = results.getClosestCollision();
                                broadcastMessage(
                                        new PutMark(players[id].getTeam(), closest.getContactPoint()));
                            }
                        }
                        return null;
                    }
                });
            }
        }

        private void actionFireInput(final HostedConnection source, final Message m) {
            if (m instanceof FireInput) {

                FireInput message = (FireInput) m;
                final int id = findId(source);

                final Vector3f direction = message.getDirection();
                final Vector3f position = message.getPosition();

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (state == ServerGameState.GameRunning) {
                            CollisionResults results = new CollisionResults();
                            //Must be changed by the coordinates and direction of the character
                            Ray ray = new Ray(position, direction);

                            shootables.collideWith(ray, results);
                            if (results.size() > 0) {
                                float[] checkResult = checkShooted(id, results);
                                int shooted = (int) checkResult[0];
                                boolean check = checkWall(ray, checkResult[1]);
                                if (check) {
                                    if (shooted >= 0) {
                                        boolean dead = players[shooted].decreaseHealth(DAMAGE_BULLET);

                                        if (dead) {
                                            players[shooted].setDead(true);

                                            broadcastMessage(
                                                    new DeadPlayer(shooted, id));

                                            if (players[shooted].getHasTreasure()) {
                                                treasureLocation = players[shooted].getWorldTranslation();
                                                treasureLocation.setY(-100f); // to prevent floating treasure

                                                broadcastMessage(
                                                        new TreasureDropped(treasureLocation));
                                                players[shooted].setHasTreasure(false);
                                            }
                                        } else {
                                            broadcastMessage(
                                                    new PlayerShooted(shooted, id, players[shooted].getHealth()));
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    }
                });

                /* TODO Calculate what it has hit.
                 and send messages according to what it has hit.
                 */

                //send message to tell clients that shot is fired
                server.broadcast(new Firing(id));
            }
        }

        private void actionPickTreasureInput(final HostedConnection source, final Message m) {
            if (m instanceof PickTreasureInput) {
                PickTreasureInput message = (PickTreasureInput) m;
                Vector3f location = message.getLocation();
                Vector3f direction = message.getDirection();
                int id = findId(source);


                //temporarily always allow pickup TODO
                broadcastMessage(new TreasurePicked(findId(source)));
                players[id].setHasTreasure(true);
                //TODO set to false for other players
            }
        }

        private void actionWantToRespawn(final HostedConnection source, final Message m) {
            final int id = findId(source);

            players[id].setDead(false);
            players[id].setHealth(MAX_HEALTH);

            Team team = players[id].getTeam();
            Vector3f position;
            if (team == Team.Blue) {
                position = initialPositions[id % MAX_PLAYERS / 2];
            } else {
                position = initialPositions[MAX_PLAYERS / 2 + id % MAX_PLAYERS / 2];
            }
            players[id].setPosition(position);
            System.out.println(id);
            broadcastMessage(new PlayerRespawn(id, position));
        }

        private void actionSendMessage(final HostedConnection source, final Message m) {
            if (m instanceof SendMessage) {

                SendMessage message = (SendMessage) m;
                final int id = findId(source);

                if (players[id].getTeam() == Team.Blue) {
                    server.broadcast(Filters.in(bluePlayersCon), new BroadcastMessage(id, message.getMessage()));
                } else {
                    server.broadcast(Filters.in(redPlayersCon), new BroadcastMessage(id, message.getMessage()));
                }
            }
        }
    }

    protected static ServerGameState getGameState() {
        return state;
    }

    protected static void changeGameState(ServerGameState newState) {
        if (!(state == newState)) {
            if (newState == ServerGameState.GameRunning) {
                server.broadcast(new Start());
            } else if (newState == ServerGameState.GameStopped) {
            }

            state = newState;
        }
    }

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
    private float[] checkShooted(int id, CollisionResults results) {
        int result = -1;
        int i = 0;
        float distance = -1;
        boolean find = false;
        while (i < results.size() && !find) {
            int shooted = Integer.parseInt(results.getCollision(i)
                    .getGeometry().getParent().getName());
            if (id != shooted && !players[shooted].isDead()) {
                distance = results.getCollision(i).getDistance();
                result = shooted;
                find = true;
            } else {
                i++;
            }
        }
        return new float[]{result, distance};
    }

    private boolean checkWall(Ray ray, float distance) {
        float distanceWall = -1;
        CollisionResults resultWall = new CollisionResults();
        rootNode.collideWith(ray, resultWall);

        for (int i = 0; i < resultWall.size(); i++) {
            if (resultWall.getCollision(i).getGeometry().getName().contains("terrain")) {
                distanceWall = resultWall.getCollision(i).getDistance();
            }
        }

        if (distance < distanceWall) {
            return true;
        } else {
            return false;
        }
    }
}
