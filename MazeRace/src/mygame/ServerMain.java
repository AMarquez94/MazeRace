package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.renderer.RenderManager;
import com.jme3.system.JmeContext;
import com.jme3.terrain.geomipmap.TerrainQuad;
import enums.ServerGameState;
import enums.Team;
import gameobjects.ServerPlayer;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import maze.Maze;
import mygame.Networking.*;

/**
 * test
 *
 * @author normenhansen
 */
public class ServerMain extends SimpleApplication {

    //CONSTANTS
    private float TIMEOUT = 5f;
    private static ServerMain app;
    private final int MAX_PLAYERS = 6;
    //GLOBAL VARIABLES
    private static Server server;
    private HostedConnection[] hostedConnections;
    private BulletAppState bas;
    private ServerPlayer[] players;
    private String[] nicknames;
    private int connectedPlayers;
    private int redPlayers;
    private int bluePlayers;
    private TerrainQuad terrain;
    private float[] timeouts;
    private static Vector3f[] initialPositions;
    //game state
    private static ServerGameState state;

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
            server = Network.createServer(Networking.PORT);
            server.start();

        } catch (IOException ex) {
            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        bas = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bas);

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
        for (int i = 0; i < MAX_PLAYERS; i++) {
            timeouts[i] = TIMEOUT;
        }

        state = ServerGameState.GameStopped;
        this.pauseOnFocus = false;

        new ServerControl(this);
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public static Vector3f getInitialPosition(int id) {
        return initialPositions[id];
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
                players[i] = new ServerPlayer(chooseTeam(i), initialPositions[0],
                        nickname, new Quaternion(0, 0, 0, 0), app);
                hostedConnections[i] = s;
                connectedPlayers++;
                find = true;
            } else {
                i++;
            }
        }
        return i;
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

    private Quaternion arrayToQuaternion(float[] r) {
        return new Quaternion(r[0], r[1], r[2], r[3]);
    }

    //TEMPORAL
    private class MessageHandler implements MessageListener<HostedConnection> {

        public void messageReceived(HostedConnection source, Message m) {

            if (m instanceof Connect) {
                Connect message = (Connect) m;
                String nickname = message.getNickname();
                if (connectedPlayers != MAX_PLAYERS) {
                    if (nickname.length() > 0 && nickname.length() <= 8) {
                        if (!repeatedNickname(nickname)) {
                            //TODO ID must be saved
                            int idNew = connectPlayer(nickname, source);
                            server.broadcast(Filters.in(hostedConnections),
                                    new NewPlayerConnected(idNew, players[idNew].getNickname(),
                                    players[idNew].getTeam(), players[idNew].getPosition()));
                            server.broadcast(Filters.in(hostedConnections),
                                    packPrepareMessage());
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
                            new ConnectionRejected("Maximal number of clients already connected"));
                }
            } else if (m instanceof PlayerMoved) {

                final int id = findId(source);
                timeouts[id] = TIMEOUT;

                PlayerMoved message = (PlayerMoved) m;

                final String animation = message.getAnimation();
                final Vector3f position = message.getPosition();
                final float[] rotation = message.getRotation();

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        players[id].setPosition(position);
                        players[id].setOrientation(arrayToQuaternion(rotation));
                        server.broadcast(Filters.in(hostedConnections),
                                new MovingPlayers(id, position, rotation, animation));
                        return null;
                    }
                });
            } else if (m instanceof MarkInput) {

                final int id = findId(source);
                timeouts[id] = TIMEOUT;

                app.enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (state == ServerGameState.GameRunning) {
                            CollisionResults results = new CollisionResults();

                            //Must be changed by the coordinates and direction of the character
                            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
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
    }

    protected static ServerGameState getGameState() {
        return state;
    }

    protected static void changeGameState(ServerGameState newState) {
        if(!(state == newState)) {
            if(newState == ServerGameState.GameRunning) {
                server.broadcast(new Start());
            } else if (newState == ServerGameState.GameStopped) {
                server.broadcast(new End(Team.Blue)); //TODO should be passed the winning team
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
}
