/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package maze;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author Dominik
 */
public class Treasure {

    //#TODO change treausre
    //#TODO change location
    
    private SimpleApplication simpleApplication;
    private AssetManager assetManager;
    private Node treasure = new Node("Treasure");

    public Treasure(SimpleApplication simpleApplication) {
        this.simpleApplication = simpleApplication;
        this.assetManager = this.simpleApplication.getAssetManager();
    }

    public void createTreasure(Node treasureNode, BulletAppState bas) {
        Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        Material mat_default = new Material(
                assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
        teapot.setMaterial(mat_default);
        
        treasure.setLocalTranslation(0f, -100f, 0f);
        treasure.attachChild(teapot);
        treasureNode.attachChild(treasure);
    }
    
    public Vector3f getPosition(){
        return treasure.getLocalTranslation();
    }
    
    public void setPosition(Vector3f position) {
        treasure.setLocalTranslation(position);
    }
}
