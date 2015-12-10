package gameobjects;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import enums.Team;

/**
 * Represents the mark the players can place on the environment.
 *
 * @author
 */
public class Mark extends Geometry {

    private Team team;

    public Mark(Team team, SimpleApplication app) {
        this.team = team;
        Sphere sphere = new Sphere(30, 30, 1f);
        this.setName("Mark");
        this.setMesh(sphere);
        
        //Create material depending on team
        Material mark_mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        if(this.team == Team.Red) {
            mark_mat.setColor("Color", ColorRGBA.Red);
        } else {
            mark_mat.setColor("Color", ColorRGBA.Blue);
        }    
        this.setMaterial(mark_mat);
    }
   
    public Team getTeam() {
        return this.team;
    }
}
