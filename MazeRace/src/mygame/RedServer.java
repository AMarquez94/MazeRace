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
import com.jme3.network.Client;
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
import gameobjects.ServerPlayer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import maze.Maze;
import static mygame.BlueServer.DAMAGE_BULLET;
import mygame.Networking.*;

/**
 * test
 *
 * @author normenhansen
 */
public class RedServer extends SimpleApplication {

    //CONSTANTS
    protected final static float TIMEOUT = 5f;
    protected static RedServer app;
    protected final static int MAX_PLAYERS = 6;
    protected final static int PICKUP_MARGIN = 5;
    protected final static int MAX_HEALTH = 100;
    protected final static int DAMAGE_BULLET = 25;
    //GLOBAL VARIABLES
    private static Server server;
    private static LinkedBlockingQueue<ServerMessage> messageQueue;
    private static HostedConnection[] hostedConnections;
    private BulletAppState bas;
    private static ServerPlayer[] players;
    private String[] nicknames;
    private static int connectedPlayers;
    private int redPlayers;
    private int bluePlayers;
    private static Vector3f treasureLocation;
    private TerrainQuad terrain;
    private static float[] timeouts;
    private static Vector3f[] initialPositions;
    private float periodic_threshold = 0; //temporary
    private static Client clientLoginServer;
    private static Client clientBlueServer;
    private static HashMap<String, Integer> nicknameToId = new HashMap<String, Integer>();
    //game state
    private static ServerGameState state;
    private static Node shootables;

    //
    public static void main(String[] args) {
        app = new RedServer();
        app.start(JmeContext.Type.Headless);
    }

    public RedServer() {
        super(new StatsAppState());
    }

    @Override
    public void simpleInitApp() {
        Networking.initialiseSerializables();

        try {
            server = Network.createServer(Networking.PORT_RED);
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

        timeouts = new float[MAX_PLAYERS];
//        for (int i = 0; i < MAX_PLAYERS; i++) {
//            timeouts[i] = TIMEOUT;
//        }

        state = ServerGameState.GameStopped;
        cam.setLocation(new Vector3f(0.74115396f, -70.0f, -150.33556f));
        this.pauseOnFocus = false;
        shootables = new Node("Shootables");
        rootNode.attachChild(shootables);


        new ServerControlRed(this);

        //networking
        messageQueue = new LinkedBlockingQueue<ServerMessage>();
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
                sendMessage(Filters.in(hostedConnections), packPrepareMessage());
                periodic_threshold = 0;
            } else {
                periodic_threshold++;
            }
        } else if (state == ServerGameState.GameRunning) {
            for (ServerPlayer p : players) {
                if (p != null && p.getHasTreasure() && p.getWorldTranslation().distanceSquared(getSpawnZonePoint(p.getTeam())) < 100) {
                    sendMessage(Filters.in(hostedConnections), new End(p.getTeam()));
                    ServerControlLogin.changeServerState(ServerGameState.GameStopped);
                }
            }
        }
    }

    private static void sendMessage(AbstractMessage message) {
        messageQueue.add(new ServerMessage(null, message));
    }

    private static void sendMessage(Filter<HostedConnection> filter, AbstractMessage message) {
        messageQueue.add(new ServerMessage(filter, message));
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public static Vector3f getInitialPosition(int id) {
        return initialPositions[id];
    }

    public static Vector3f getSpawnZonePoint(Team team) {
        if (team == Team.Red) {
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
    private static void connectPlayer(String nickname, Team team, HostedConnection s, int id) {
        players[id] = new ServerPlayer(id, team, initialPositions[MAX_PLAYERS / 2 + id % MAX_PLAYERS / 2],
                nickname, app);
        //players[i].addToPhysicsSpace(bas);
        hostedConnections[id] = s;
        connectedPlayers++;
        shootables.attachChild(players[id]);
    }

    private void disconnectPlayer(int id) {
        if (players[id].getTeam() == Team.Blue) {
            bluePlayers--;
        } else {
            redPlayers--;
        }
        hostedConnections[id] = null;
        connectedPlayers--;
        shootables.detachChild(players[id]);
        players[id] = null;
        sendMessage(Filters.in(hostedConnections), new DisconnectedPlayer(id));
        timeouts[id] = 0;
    }

    private void setUpInitialPositions() {
        //#TODO get inital positions from LS
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

    private static Quaternion arrayToQuaternion(float[] r) {
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

            boolean timeout; //for debug. for printing send reason

            while (QUORUM > 0) { //loop forever
                if (messageQueue.size() > 0) { //wait until update is available
                    timer = System.currentTimeMillis(); //set timer
                    timeout = false; //for debug

                    quorum_loop:
                    while (messageQueue.size() < QUORUM) { //while quorum is not reached
                        if (System.currentTimeMillis() - timer > TIMEOUT) { //check for timeout
                            timeout = true;
                            break quorum_loop;
                        }
                    }

                    //send packets
                    while (!messageQueue.isEmpty()) {
                        ServerMessage message = messageQueue.poll();

                        if (message.filter != null) {
                            server.broadcast(message.filter, message.message);
                        } else {
                            server.broadcast(message.message);
                        }
                    }

                    //debug
                    if (timeout) {
                        System.out.println("Messages sent by timeout.");
                    } else {
                        System.out.println("Messages sent by quorum.");
                    }
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
            if (m instanceof Connect) {
                Connect message = (Connect) m;
                final String nickname = message.getNickname();

                //TODO ID must be saved
                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        //Set up the character. TODO does not include orientation (maybe not needed)
                        while (!nicknameToId.containsKey(nickname)) {
                        }
                        int idNew = nicknameToId.get(nickname);
//                        int idNew = 0;
                        connectPlayer(nickname, Team.Red, source, idNew);
                        sendMessage(Filters.in(hostedConnections),
                                new NewPlayerConnected(idNew, players[idNew].getNickname(),
                                players[idNew].getTeam(), players[idNew].getPosition()));
                        sendMessage(Filters.in(hostedConnections),
                                packPrepareMessage());
                        sendMessage(Filters.in(hostedConnections), new TreasureDropped(treasureLocation));
                        clientBlueServer.send(new NewPlayerConnected(idNew, players[idNew].getNickname(),
                                Team.Red, players[idNew].getPosition()));
                        return null;
                    }
                });
            }
        }

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
                        MovingPlayers sendMessage = new MovingPlayers(id, position, rotation, orientation, animation);
                        sendMessage(Filters.in(hostedConnections), sendMessage);
                        clientBlueServer.send(sendMessage);
                        return null;
                    }
                });
            }
        }

        private void actionMarkInput(final HostedConnection source, final Message m) {
            if (m instanceof MarkInput) {
                System.out.println("RedServer: Got Mark Input");
                MarkInput message = (MarkInput) m;
                final int id = findId(source);
                timeouts[id] = TIMEOUT;

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
                                sendMessage(Filters.in(hostedConnections),
                                        new PutMark(Team.Red, closest.getContactPoint()));
                                clientBlueServer.send(new PutMark(Team.Red, closest.getContactPoint()));
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
                timeouts[id] = TIMEOUT;

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
                                int shooted = checkShooted(id, results);
                                if (shooted >= 0) {
                                    boolean dead = players[shooted].decreaseHealth(DAMAGE_BULLET);

                                    if (dead) {
                                        players[shooted].setDead(true);
                                        DeadPlayer deadMessage = new DeadPlayer(shooted, id);
                                        sendMessage(Filters.in(hostedConnections), deadMessage);
                                        clientBlueServer.send(deadMessage);
                                        if (players[shooted].getHasTreasure()) {
                                            treasureLocation = players[shooted].getWorldTranslation();
                                            treasureLocation.setY(-100f); // to prevent floating treasure

                                            sendMessage(Filters.in(hostedConnections),
                                                    new TreasureDropped(treasureLocation));
                                            clientBlueServer.send(new TreasureDropped(treasureLocation));
                                            players[shooted].setHasTreasure(false);
                                        }
                                    } else {
                                        PlayerShooted shootedMessage = new PlayerShooted(shooted, id, players[shooted].getHealth());
                                        sendMessage(Filters.in(hostedConnections), shootedMessage);
                                        clientBlueServer.send(shootedMessage);
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
                sendMessage(new Firing(id));
            }
        }

        private void actionPickTreasureInput(final HostedConnection source, final Message m) {
            if (m instanceof PickTreasureInput) {
                PickTreasureInput message = (PickTreasureInput) m;
                Vector3f location = message.getLocation();
                Vector3f direction = message.getDirection();
                int id = findId(source);


                //temporarily always allow pickup TODO
                sendMessage(Filters.in(hostedConnections), new TreasurePicked(findId(source)));
                clientBlueServer.send(new TreasurePicked(findId(source)));
                players[id].setHasTreasure(true);
                //TODO set to false for other players
            }
        }

        private void actionWantToRespawn(final HostedConnection source, final Message m) {
            final int id = findId(source);

            players[id].setDead(false);
            players[id].setHealth(MAX_HEALTH);
            players[id].setPosition(initialPositions[MAX_PLAYERS / 2 + id % MAX_PLAYERS / 2]);

            sendMessage(Filters.in(hostedConnections), new PlayerRespawn(id, initialPositions[MAX_PLAYERS / 2 + id % MAX_PLAYERS / 2]));
            clientBlueServer.send(new PlayerRespawn(id, initialPositions[MAX_PLAYERS / 2 + id % MAX_PLAYERS / 2]));
        }

        private void actionSendMessage(final HostedConnection source, final Message m) {
            if (m instanceof SendMessage) {

                SendMessage message = (SendMessage) m;
                final int id = findId(source);

                if (players[id].getTeam() == Team.Blue) {
                    sendMessage(Filters.in(hostedConnections), new BroadcastMessage(id, message.getMessage()));
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
                sendMessage(new Start());
            } else if (newState == ServerGameState.GameStopped) {
            }

            state = newState;
        }
    }

    private static Prepare packPrepareMessage() {
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

    private static int findId(HostedConnection source) {
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

    protected static void connect() {
        try {
            System.out.println("RedServer: Try to connect clients");
            clientLoginServer = Network.connectToServer(Networking.HOST_LOGIN, Networking.PORT_LOGIN);
            clientLoginServer.start();
            clientBlueServer = Network.connectToServer(Networking.HOST_LOGIN, Networking.PORT_BLUE);
            clientBlueServer.start();
            System.out.println("RedServer: Clients connected");
        } catch (IOException ex) {
            Logger.getLogger(RedServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        clientLoginServer.addMessageListener(new LoginServerListener());
        server.addMessageListener(new BlueServerListener());
        clientLoginServer.send(new LoginConnected("red"));
    }

    private static class LoginServerListener implements MessageListener<Client> {

        public void messageReceived(Client source, Message m) {
            if (m instanceof HeartBeat) {
                System.out.println("BlueServer: HeartBeat from LS");
            } else if (m instanceof NewConnection) {
                NewConnection message = (NewConnection) m;
                final String nickname = message.getNickname();
                final int id = message.getId();
                nicknameToId.put(nickname, id);
            } else if (m instanceof Start) {
                changeGameState(ServerGameState.GameRunning);
                server.broadcast(new Start());
            } else if (m instanceof PutMark) {
                PutMark message = (PutMark) m;
                server.broadcast(Filters.in(hostedConnections),
                        new PutMark(Team.Blue, message.getPosition()));
            }
        }
    }

    private static class BlueServerListener implements MessageListener<HostedConnection> {

        public void messageReceived(HostedConnection source, Message m) {

            if (m instanceof PutMark) {
                PutMark message = (PutMark) m;
                server.broadcast(Filters.in(hostedConnections),
                        new PutMark(Team.Blue, message.getPosition()));
            } else if (m instanceof NewPlayerConnected) {
                NewPlayerConnected message = (NewPlayerConnected) m;
                int idNew = message.getId();
                connectPlayer(message.getNickname(), Team.Blue, source, idNew);
                server.broadcast(Filters.in(hostedConnections),
                        new NewPlayerConnected(idNew, players[idNew].getNickname(),
                        players[idNew].getTeam(), players[idNew].getPosition()));
                server.broadcast(Filters.in(hostedConnections),
                        packPrepareMessage());
                server.broadcast(Filters.in(hostedConnections), new TreasureDropped(treasureLocation));
            } else if (m instanceof MovingPlayers) {
                MovingPlayers message = (MovingPlayers) m;
                final int id = findId(source);
                timeouts[id] = TIMEOUT;


                final String animation = message.getAnimation();
                final Vector3f position = message.getPosition();
                final float[] rotation = message.getRotation();
                final Vector3f orientation = message.getOrientation();


                players[id].setPosition(position);
                players[id].setOrientation(arrayToQuaternion(rotation));

                MovingPlayers sendMessage = new MovingPlayers(id, position, rotation, orientation, animation);
                server.broadcast(Filters.in(hostedConnections), sendMessage);
            } else if (m instanceof DeadPlayer) {
                DeadPlayer message = (DeadPlayer) m;
                int shooted = message.getDeadPlayer();
                int killer = message.getKillerPlayer();

                players[shooted].setDead(true);
                DeadPlayer deadMessage = new DeadPlayer(shooted, killer);
                server.broadcast(Filters.in(hostedConnections), deadMessage);

                if (players[shooted].getHasTreasure()) {
                    players[shooted].setHasTreasure(false);
                }
            } else if (m instanceof TreasureDropped) {
                TreasureDropped message = (TreasureDropped) m;
                treasureLocation = message.getLocation();
            } else if (m instanceof PlayerShooted) {
                PlayerShooted message = (PlayerShooted) m;
                int shooted = message.getShootedPlayerId();
                int id = message.getShootingPlayerId();

                players[shooted].decreaseHealth(DAMAGE_BULLET);

                PlayerShooted shootedMessage = new PlayerShooted(shooted, id, players[shooted].getHealth());
                server.broadcast(Filters.in(hostedConnections), shootedMessage);
            } else if (m instanceof TreasurePicked) {
                TreasurePicked message = (TreasurePicked) m;
                players[message.getPlayerID()].setHasTreasure(true);
                server.broadcast(Filters.in(hostedConnections), new TreasurePicked(message.getPlayerID()));
            } else if (m instanceof PlayerRespawn) {
                PlayerRespawn message = (PlayerRespawn) m;

                int id = message.getPlayerRespawn();
                Vector3f position = message.getPosition();

                players[id].setDead(false);
                players[id].setHealth(MAX_HEALTH);
                players[id].setPosition(initialPositions[id % MAX_PLAYERS / 2]);

                server.broadcast(Filters.in(hostedConnections), new PlayerRespawn(id, position));
            }
        }
    }
}
