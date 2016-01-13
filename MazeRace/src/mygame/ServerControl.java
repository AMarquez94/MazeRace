package mygame;

import com.jme3.app.SimpleApplication;
import enums.ServerGameState;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel to control the server's game state.
 * 
 * @authors Alejandro Marquez, Bjorn van der Laan, Dominik Gils
 */
public class ServerControl extends JFrame {

    private static JLabel stateLabel;
    private static SimpleApplication server;

    public ServerControl(SimpleApplication serverApp) {
        super();
        
        server = serverApp;

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        stateLabel = new JLabel();
        setStateLabel(ServerMain.getGameState());
        panel.add(stateLabel);

        JButton button = new JButton(ServerGameState.GameRunning.toString());
        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changeServerState(ServerGameState.GameRunning);
                }
            });
        panel.add(button);
        this.add(panel);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    /**
     * Change the server state to the one passed as parameter
     * @param state 
     */
    public static void changeServerState(final ServerGameState state) {
        server.enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                ServerMain.changeGameState(state);
                return null;
            }
        });
        setStateLabel(state);
    }

    /**
     * Set the state text to show in the Server Control box
     * @param state 
     */
    public static void setStateLabel(ServerGameState state) {
        stateLabel.setText("Current state: " + state.toString());
    }
}