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
    private Node player, playerNode;
    private BetterCharacterControl playerControl;
    private AnimControl animationControl;
    private AnimChannel animationChannel;
    //player movement
    private boolean left = false, right = false, up = false, down = false;
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private float airTime = 0;

    public static void main(String[] args) {
        PlayerTest app = new PlayerTest();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(100);

        bas = new BulletAppState();
        //bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bas);

        setUpLight();
        setUpWorld();
        setUpCharacter();
        setUpKeys();

        flyCam.setEnabled(false);
        chaseCam = new ChaseCamera(cam, playerNode, inputManager);
    }

    private void setUpCharacter() {
        // Load model
        player = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml"); // You can set the model directly to the player. (We just wanted to explicitly show that it's a spatial.)
        playerNode = new Node(); // You can create a new node to wrap your player to adjust the location. (This allows you to solve issues with character sinking into floor, etc.)
        playerNode.attachChild(player); // add it to the wrapper
        player.move(0, 3.5f, 0); // adjust position to ensure collisions occur correctly.
        player.setLocalScale(0.5f); // optionally adjust scale of model

        //AnimControl control setup animation
        animationControl = player.getControl(AnimControl.class);
        animationControl.addListener(playerAnimListener);
        animationChannel = animationControl.createChannel();
        animationChannel.setAnim("stand");
        playerControl = new BetterCharacterControl(1.5f, 6f, 1f); // construct character. (If your character bounces, try increasing height and weight.)
        playerNode.addControl(playerControl); // attach to wrapper

        // set basic physical properties
        playerControl.setJumpForce(new Vector3f(0, 5f, 0));
        playerControl.setGravity(new Vector3f(0, 1f, 0));
        playerControl.warp(new Vector3f(0, 10, 10)); // warp character into landscape at particular location

        // add to physics state
        bas.getPhysicsSpace().add(playerControl);
        bas.getPhysicsSpace().addAll(playerNode);
        rootNode.attachChild(playerNode);
        
        System.out.println("Available animations for this model are:");
        for(String c : animationControl.getAnimationNames()) {
            System.out.println(c);
        }
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
                playerControl.jump();
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

        if (!playerControl.isOnGround()) { // use !character.isOnGround() if the character is a BetterCharacterControl type.
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
            if (!"stand".equals(animationChannel.getAnimationName())) {
                animationChannel.setAnim("stand", 1f);
            }
        } else {
            playerControl.setViewDirection(walkDirection);
            if (airTime > .5f) {
                if (!"stand".equals(animationChannel.getAnimationName())) {
                    animationChannel.setAnim("stand");
                }
            } else if (!"Walk".equals(animationChannel.getAnimationName())) {
                animationChannel.setAnim("Walk", 0.7f);
            }
        }

        walkDirection.multLocal(500f).multLocal(tpf);// The use of the first multLocal here is to control the rate of movement multiplier for character walk speed. The second one is to make sure the character walks the same speed no matter what the frame rate is.
        playerControl.setWalkDirection(walkDirection); // THIS IS WHERE THE WALKING HAPPENS
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
