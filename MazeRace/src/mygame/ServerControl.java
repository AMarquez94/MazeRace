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
        setStateLabel(LoginServer.getGameState());
        panel.add(stateLabel);

        for (final ServerGameState state : ServerGameState.values()) {
            JButton button = new JButton(state.toString());

            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changeServerState(state);
                }
            });

            panel.add(button);
        }


        this.add(panel);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public static void changeServerState(final ServerGameState state) {
        server.enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                LoginServer.changeGameState(state);
                return null;
            }
        });
        setStateLabel(state);
    }

    public static void setStateLabel(ServerGameState state) {
        stateLabel.setText("Current state: " + state.toString());
    }
}