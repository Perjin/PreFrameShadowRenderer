package de.perjin;


import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import de.perjin.shadow.DirectionalLightShadowPreFrameRenderer;

public class Main extends SimpleApplication {

  private boolean newShadows = true;
  private Geometry geom;
  
  public static void main(String[] args) {
    Main app = new Main();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    ScreenshotAppState screenshotAppState = new ScreenshotAppState("Screenshots/","ShadowPreFrame",0);
    stateManager.attach(screenshotAppState);
    flyCam.setMoveSpeed(10f);
    cam.setLocation(new Vector3f(5f, 3f, -4f));
    DirectionalLight directionalLight = new DirectionalLight();
    directionalLight.setDirection(new Vector3f(1f, -2f, 1f).normalizeLocal());
    if (newShadows) {
      DirectionalLightShadowPreFrameRenderer dsipr = new DirectionalLightShadowPreFrameRenderer(assetManager, 1024, 4);
      dsipr.setLight(directionalLight);
      directionalLight.setColor(ColorRGBA.White);
      rootNode.addLight(directionalLight);
      viewPort.addProcessor(dsipr);
    } else {
      DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 4);
      dlsr.setLight(directionalLight);
      viewPort.addProcessor(dlsr);
      AmbientLight ambientLight = new AmbientLight();
      ambientLight.setColor(ColorRGBA.Blue);
      rootNode.addLight(ambientLight);
      rootNode.addLight(directionalLight);
    }
    initScene(rootNode);
    cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    this.setDisplayStatView(true);
    this.setDisplayFps(true);
  }

  private void initScene(Node rootNode) {
    Box b = new Box(5f, .25f, 5f);
    Geometry plane = new Geometry("Plane", b);
    plane.setLocalTranslation(0f, -1.25f, 0f);
    Material mat;
    if (newShadows) {
      mat = new Material(assetManager, "MatDefs/ShadowPreFrameLighting.j3md");
      mat.setColor("AmbientColor", ColorRGBA.Blue);
     // mat.setVector3("LightDir", new Vector3f(1f, -2f, 1f).normalizeLocal().negateLocal());
      mat.setColor("Color", ColorRGBA.White);
    } else {
      mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
      mat.setColor("Diffuse", ColorRGBA.White);
      mat.setBoolean("UseMaterialColors", true);
      mat.setColor("Ambient", ColorRGBA.Blue);
    }
    plane.setMaterial(mat);
    plane.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(plane);


    Sphere s = new Sphere(16, 16, .5f);
    Geometry sphere = new Geometry("Sphere", s);
    sphere.setLocalTranslation(0f, 1.5f, 0f);
    sphere.setMaterial(mat);
    sphere.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(sphere);
    
    
   
    Box q = new Box(1, 1, 1);
    geom = new Geometry("Box", q);
    mat = mat.clone();
    if (newShadows) {
      mat.setColor("Color", ColorRGBA.Gray);
    } else {
      mat.setColor("Diffuse", ColorRGBA.Gray);
    }
    
    
    
    geom.setMaterial(mat);
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(geom);
  }

  @Override
  public void simpleUpdate(float tpf) {
    geom.rotate(0f, tpf*FastMath.QUARTER_PI, 0f);
  }

  @Override
  public void simpleRender(RenderManager rm) {
    //TODO: add render code
  }
}
