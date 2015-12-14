package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
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
import enums.Team;
import java.io.IOException;
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
    private final int MAX_PLAYERS = 6;
    //GLOBAL VARIABLES
    private Server server;
    private HostedConnection[] hostedConnections;
    private BulletAppState bas;
    private String[] nicknames;
    private int players;
    private Vector3f[] initialPositions;
    private int redPlayers;
    private int bluePlayers;
    private TerrainQuad terrain;
    private Material mat_terrain;

    //
    public static void main(String[] args) {
        ServerMain app = new ServerMain();
        app.start(/*JmeContext.Type.Headless*/);
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
        players = 0;
        redPlayers = 0;
        bluePlayers = 0;
        nicknames = new String[MAX_PLAYERS];
        for (int i = 0; i < nicknames.length; i++) {
            nicknames[i] = "";
        }
        hostedConnections = new HostedConnection[MAX_PLAYERS];


        this.pauseOnFocus = false;
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }

    private boolean repeatedNickname(String nickname) {
        int i = 0;
        boolean rep = false;
        while (i < nicknames.length && !rep) {
            if (nicknames[i] != null && nicknames[i].equals(nickname)) {
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
        while (i < nicknames.length && !find) {
            if (nicknames[i].equals("")) {
                nicknames[i] = nickname;
                hostedConnections[i] = s;
                players++;
                find = true;
            } else {
                i++;
            }
        }
        return i;
    }

    private void setUpInitialPositions() {
        initialPositions = new Vector3f[MAX_PLAYERS];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            initialPositions[i] = new Vector3f(0, 0, 0);
        }
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

    //TEMPORAL
    private class MessageHandler implements MessageListener<HostedConnection> {

        public void messageReceived(HostedConnection source, Message m) {

            if (m instanceof Connect) {
                Connect message = (Connect) m;
                String nickname = message.getNickname();
                if (players != MAX_PLAYERS) {
                    if (nickname.length() > 0 && nickname.length() <= 8) {
                        if (!repeatedNickname(nickname)) {
                            //TODO ID must be saved
                            int idNew = connectPlayer(nickname, source);
                            server.broadcast(Filters.in(hostedConnections),
                                    new NewPlayerConnected(idNew, nickname,
                                    chooseTeam(idNew), initialPositions[idNew]));
                            
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
            }
        }
    }
}
