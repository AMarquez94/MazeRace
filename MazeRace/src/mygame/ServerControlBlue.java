package mygame;

import com.jme3.app.SimpleApplication;
import enums.ServerGameState;
import java.awt.Dimension;
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
public class ServerControlBlue extends JFrame {

    private static JLabel stateLabel;
    private static SimpleApplication server;

    public ServerControlBlue(SimpleApplication serverApp) {
        super();

        server = serverApp;

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        stateLabel = new JLabel();
        
        panel.add(stateLabel);

        JButton button = new JButton("connect");

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BlueServer.connect();
            }
        });

        panel.add(button);


        this.setTitle("Blue Controll");
        this.add(panel);
        this.pack();
        Dimension d = new Dimension(250, 0);
        this.setMinimumSize(d);
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
//        setStateLabel(state);
    }

    public static void setStateLabel(ServerGameState state) {
        stateLabel.setText("Current state: " + state.toString());
    }
}