package maze;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;

/**
 *
 * @author dominik
 */
public class Maze {

    private TerrainQuad terrain;
    private Material mat_terrain;
    private SimpleApplication simpleApplication;
    private AssetManager assetManager;

    public Maze(SimpleApplication simpleApplication){
        this.simpleApplication = simpleApplication;
        assetManager = simpleApplication.getAssetManager();
    }
    
    public TerrainQuad setUpWorld(Node rootNode, BulletAppState bas) {
        
        /**
         * 1. Create terrain material and load four textures into it.
         */
        mat_terrain = new Material(assetManager,
                "Common/MatDefs/Terrain/Terrain.j3md");

        /**
         * 1.1) Add ALPHA map (for red-blue-green coded splat textures)
         */
        mat_terrain.setTexture("Alpha", assetManager.loadTexture(
                "Textures/maze_middle_color.png"));

        /**
         * 1.2) Add GRASS texture into the red layer (Tex1).
         */
        Texture grass = assetManager.loadTexture(
                "Textures/stone1.jpg");
        grass.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex1", grass);
        mat_terrain.setFloat("Tex1Scale", 64f);

        /**
         * 1.3) Add DIRT texture into the green layer (Tex2)
         */
        Texture dirt = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex2", dirt);
        mat_terrain.setFloat("Tex2Scale", 32f);
        
        /**
         * 1.4) Add ROAD texture into the blue layer (Tex3)
         */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex3", rock);
        mat_terrain.setFloat("Tex3Scale", 128f);

        /**
         * 2. Create the height map
         */
        AbstractHeightMap heightmap = null;
        //assetManager.registerLocator("assets\\Textures", FileLocator.class);
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/maze_middle_test.png");
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();

        /**
         * 3. We have prepared material and heightmap. Now we create the actual
         * terrain:
         *
         * 3.1) Create a TerrainQuad and name it "my terrain".
         *
         * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
         *
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply
         * 512+1=513.
         *
         * 3.4) As LOD step scale we supply Vector3f(1,1,1). 3.5) We supply the
         * prepared heightmap itself.
         */
        int patchSize = 65;
        terrain = new TerrainQuad("my terrain", patchSize, 257, heightmap.getHeightMap());

        /**
         * 4. We give the terrain its material, position & scale it, and attach
         * it.
         */
        terrain.setMaterial(mat_terrain);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 0.5f, 2f);
        rootNode.attachChild(terrain);

        terrain.addControl(new RigidBodyControl(0));
        bas.getPhysicsSpace().add(terrain);
        /**
         * 5. The LOD (level of detail) depends on were the camera is:
         */
        TerrainLodControl control = new TerrainLodControl(terrain, simpleApplication.getCamera());
        terrain.addControl(control);
        
        /* We create the sky */
      
//        Texture west = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_west.jpg");
//        Texture east = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_east.jpg");
//        Texture north = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_north.jpg");
//        Texture south = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_south.jpg");
//        Texture up = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_up.jpg");
//        Texture down = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_down.jpg");
//        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false);
        sky.setLocalTranslation(0, -100, 0);
        rootNode.attachChild(sky);
        
        return terrain;
    }
}