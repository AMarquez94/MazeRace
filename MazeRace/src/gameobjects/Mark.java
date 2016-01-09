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
 * @authors Alejandro Marquez, Bjorn van der Laan, Dominik Gils
 */
public class Mark extends Geometry {

    private Team team;
    private ColorRGBA color;
    
    /**
     * Constructor of the class Mark. Creates a mark of the team passed by
     * parameter
     * @param team
     * @param app 
     */
    public Mark(Team team, SimpleApplication app) {
        if(team == Team.Red) {
            this.color = ColorRGBA.Red;
        } else {
            this.color = ColorRGBA.Blue;
        }
        
        this.team = team;
        
        Sphere sphere = new Sphere(30, 30, 0.3f);
        this.setName("Mark");
        this.setMesh(sphere);
        
        Material mark_mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", this.color);
        this.setMaterial(mark_mat);
    }
   
    
    /**
     * @return The color of the mark
     */
    public ColorRGBA getColor() {
        return this.color;
    }
    
    /**
     * @return The team that put the mark
     */
    public Team getTeam() {
        return this.team;
    }
}
