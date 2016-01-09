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
 * Represents the treasure that the players have to pick
 *
 * @authors Alejandro Marquez, Bjorn van der Laan, Dominik Gils
 */
public class Treasure extends Node {

    private SimpleApplication simpleApplication;
    private AssetManager assetManager;

    /**
     * Constructor of the class Treasure
     * @param simpleApplication
     * @param bas 
     */
    public Treasure(SimpleApplication simpleApplication, BulletAppState bas) {
        this.setName("Treasure");
        this.simpleApplication = simpleApplication;
        this.assetManager = this.simpleApplication.getAssetManager();

        Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        Material mat_default = new Material(
                assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
        teapot.setMaterial(mat_default);
        this.attachChild(teapot);
        
    }

    /**
     * @return Treasure's position
     */
    public Vector3f getPosition() {
        return this.getLocalTranslation();
    }

    /**
     * Set Treasure position to the position passed as parameter
     * @param position 
     */
    public void setPosition(Vector3f position) {
        this.setLocalTranslation(position);
    }
}
