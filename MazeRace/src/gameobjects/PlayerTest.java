package gameobjects;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * test
 *
 * @author normenhansen
 */
public class PlayerTest extends SimpleApplication {

    private BulletAppState bas;
    private ChaseCamera chaseCam;
    //terrain
    private RigidBodyControl landscape;
    private Spatial sceneModel;
    //player
    private Player player;
    //player movement
    private boolean left = false, right = false, up = false, down = false;
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private float airTime = 0;
    private final float MOVE_SPEED = 800f;

    public static void main(String[] args) {
        PlayerTest app = new PlayerTest();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //flyCam.setEnabled(false);

        bas = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bas);

        setUpLight();
        setUpWorld();
        setUpCharacter();
        setUpKeys();
        
        
    }

    private void setUpCharacter() {
        player = new Player(this);
        player.addAnimEventListener(playerAnimListener);
        player.addToPhysicsSpace(bas);
        
        rootNode.attachChild(player);
    }
    private AnimEventListener playerAnimListener = new AnimEventListener() {
        public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        }

        public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        }
    };

    private void setUpKeys() {
        inputManager.addMapping("CharLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("CharRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("CharForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("CharBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("CharJump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(playerMoveListener, "CharLeft", "CharRight");
        inputManager.addListener(playerMoveListener, "CharForward", "CharBackward", "CharJump");
    }
    private ActionListener playerMoveListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("CharLeft")) {
                if (isPressed) {
                    left = true;
                } else {
                    left = false;
                }
            } else if (name.equals("CharRight")) {
                if (isPressed) {
                    right = true;
                } else {
                    right = false;
                }
            } else if (name.equals("CharForward")) {
                if (isPressed) {
                    up = true;
                } else {
                    up = false;
                }
            } else if (name.equals("CharBackward")) {
                if (isPressed) {
                    down = true;
                } else {
                    down = false;
                }
            } else if (name.equals("CharJump")) {
                player.getCharacterControl().jump();
            }
        }
    };

    private void setUpWorld() {
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape((Node) sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);
        rootNode.attachChild(sceneModel);
        bas.getPhysicsSpace().add(landscape);
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(5f));
        rootNode.addLight(al);
    }

    @Override
    public void simpleUpdate(float tpf) {
        Vector3f camDir = cam.getDirection().clone();
        Vector3f camLeft = cam.getLeft().clone();
        camDir.y = 0;
        camLeft.y = 0;
        camDir.normalizeLocal();
        camLeft.normalizeLocal();
        walkDirection.set(0, 0, 0);

        if (!player.getCharacterControl().isOnGround()) {
            airTime += tpf;
        } else {
            airTime = 0;
        }

        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }

        //change animation
        if (walkDirection.lengthSquared() == 0) { //Use lengthSquared() (No need for an extra sqrt())
            if (!"stand".equals(player.getAnimChannel().getAnimationName())) {
                player.getAnimChannel().setAnim("stand", 1f);
            }
        } else {
            player.getCharacterControl().setViewDirection(walkDirection);
            if (airTime > .5f) {
                if (!"stand".equals(player.getAnimChannel().getAnimationName())) {
                    player.getAnimChannel().setAnim("stand");
                }
            } else if (!"Walk".equals(player.getAnimChannel().getAnimationName())) {
                player.getAnimChannel().setAnim("Walk", 0.7f);
            }
        }
        
        walkDirection.multLocal(MOVE_SPEED).multLocal(tpf);// The use of the first multLocal here is to control the rate of movement multiplier for character walk speed. The second one is to make sure the character walks the same speed no matter what the frame rate is.
        player.getCharacterControl().setWalkDirection(walkDirection); // THIS IS WHERE THE WALKING HAPPENS
        //cam.lookAtDirection(player.getCharacterControl().getViewDirection(), new Vector3f());
        Vector3f player_pos = player.getWorldTranslation();
        cam.setLocation(new Vector3f(player_pos.getX(), player_pos.getY() + 5f, player_pos.getZ()));
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
