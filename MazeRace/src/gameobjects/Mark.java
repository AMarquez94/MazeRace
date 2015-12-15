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
    private ColorRGBA color;
    
    public Mark(Team team, SimpleApplication app) {
        if(team == Team.Red) {
            this.color = ColorRGBA.Red;
        } else {
            this.color = ColorRGBA.Blue;
        }
        
        this.team = team;
        
        Sphere sphere = new Sphere(30, 30, 1f);
        this.setName("Mark");
        this.setMesh(sphere);
        
        Material mark_mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", this.color);
        this.setMaterial(mark_mat);
    }
   
    public ColorRGBA getColor() {
        return this.color;
    }
    
    public Team getTeam() {
        return this.team;
    }
}
