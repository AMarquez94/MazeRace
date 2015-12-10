package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.renderer.RenderManager;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import enums.Team;
import gameobjects.Player;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import mygame.Networking.*;

/**
 * test
 * @author normenhansen
 */
public class ServerMain extends SimpleApplication {
    
    private Server server;
    private HostedConnection[] hostedConnections;
    
    private TerrainQuad terrain;
    private Material mat_terrain;
    
    private BulletAppState bas;
    
    private Player player;

    
    public static void main(String[] args) {
        Networking.initialiseSerializables();
        ServerMain app = new ServerMain();
        app.start();
    }
    
    public ServerMain(){
        super(new StatsAppState());
    }

    @Override
    public void simpleInitApp() {
        
        
        try {
            server = Network.createServer(Networking.PORT);
            server.start();
            
        } catch (IOException ex) {
            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         bas = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bas);
        
        setUpWorld();
        setUpLight();
        setUpCharacter(Team.Red);
        server.addMessageListener(new MessageHandler());
        
        this.pauseOnFocus = false;
    }

    @Override
    public void simpleUpdate(float tpf) {
        Vector3f player_pos = player.getWorldTranslation();
        cam.setLocation(new Vector3f(player_pos.getX(), player_pos.getY() + 5f, player_pos.getZ()));
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
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
    
     private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(5f));
        rootNode.addLight(al);
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
     
    @Override
    public void destroy() {
        server.close();
        super.destroy();
    }
    
    //TEMPORAL
    private class MessageHandler implements MessageListener<HostedConnection> {

        public void messageReceived(HostedConnection source, Message m) {

            if (m instanceof Moving) {
                Moving message = (Moving)m;
                
                final Vector3f position = message.getPosition();
                float[][] r = message.getRotation();
                
                final Quaternion q = new Quaternion(r[0][0],r[0][1],r[0][2],r[0][3]);
                ServerMain.this.enqueue(new Callable() {
                    public Object call() throws Exception {
                        player.setLocalTranslation(position);
                        cam.setRotation(q);
                        return null;
                    }
                });
                
            }
        }
    }

}
