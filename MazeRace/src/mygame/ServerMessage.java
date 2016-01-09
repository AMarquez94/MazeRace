package mygame;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Filter;
import com.jme3.network.HostedConnection;

/**
 * Creates a Server Message (Message + destination filter)
 *
 * @authors Alejandro Marquez, Bjorn van der Laan, Dominik Gils
 */
public class ServerMessage {
    public Filter<HostedConnection> filter;
    public AbstractMessage message;
    
    public ServerMessage(Filter<HostedConnection> filter, AbstractMessage message) {
        this.filter = filter;
        this.message = message;
    }
}
