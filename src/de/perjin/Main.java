package de.perjin;


import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.CompareMode;
import de.perjin.shadow.DirectionalLightShadowPreFrameRenderer;
import de.perjin.shadow.DirectionalShadowLight;
import de.perjin.shadow.PointLightShadowPreFrameRenderer;
import de.perjin.shadow.PointShadowLight;
import de.perjin.shadow.ShadowMaterial;
import de.perjin.shadow.SpotLightShadowPreFrameRenderer;
import de.perjin.shadow.SpotShadowLight;
import java.util.ArrayList;

public class Main extends SimpleApplication {

  private Geometry geom;
  
  private final ArrayList<PointLight> pointLights = new ArrayList<>();
  
  public static void main(String[] args) {
    Main app = new Main();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        renderManager.setSinglePassLightBatchSize(6);
    ScreenshotAppState screenshotAppState = new ScreenshotAppState("Screenshots/","ShadowPreFrame",0);
    stateManager.attach(screenshotAppState);
    flyCam.setMoveSpeed(10f);
    cam.setLocation(new Vector3f(5f, 3f, -4f));
//    DirectionalShadowLight directionalLight = new DirectionalShadowLight();
//    directionalLight.setDirection(new Vector3f(1f, -2f, 1f).normalizeLocal());
//    directionalLight.setColor(ColorRGBA.Red.mult(.75f));
//    DirectionalLightShadowPreFrameRenderer dsipr = new DirectionalLightShadowPreFrameRenderer(assetManager, 1024, 4);
//    dsipr.setShadowCompareMode(CompareMode.Hardware);
//    dsipr.setLight(directionalLight);
//    dsipr.setShadowZFadeLength(2f);
//    dsipr.setShadowZExtend(20f);
////      directionalLight.setColor(ColorRGBA.White.mult(.5f));
//    viewPort.addProcessor(dsipr);
//      PointLightShadowPreFrameRenderer plspfr = new PointLightShadowPreFrameRenderer(assetManager, 1024);
//      plspfr.setShadowCompareMode(CompareMode.Hardware);
    pointLights.add(addPointLight(new Vector3f(-3f, 1f, -1f),ColorRGBA.Green.mult(.5f),4f));
    pointLights.add(addPointLight(new Vector3f(3f, 1f, -7f),ColorRGBA.Red.mult(.5f),3f));
    pointLights.add(addPointLight(new Vector3f(-3f, 1f, -7f),ColorRGBA.Yellow.mult(.5f),6f));
    pointLights.add(addPointLight(new Vector3f(0.5f, 3.5f, -3f),ColorRGBA.Blue.mult(1.5f),8f));
    pointLights.add(addPointLight(new Vector3f(3f, 0.5f, -1f),ColorRGBA.Magenta.mult(.5f),3f));
//    plspfr.setLight(addPointShadowLight(new Vector3f(0.5f, 3.5f, -3f),ColorRGBA.Blue.mult(1.5f),8f));
//    viewPort.addProcessor(plspfr);
    
    SpotLightShadowPreFrameRenderer slspfr = new SpotLightShadowPreFrameRenderer(assetManager, 1024);
    SpotLight spot = new SpotShadowLight();
    spot.setDirection(new Vector3f(1f, -.01f, 0f).normalizeLocal());
    spot.setPosition(new Vector3f(-3f, .5f, 0.75f));
    spot.setColor(ColorRGBA.White.mult(1f));
    spot.setSpotOuterAngle(.6f);
    slspfr.setLight(spot);
    slspfr.setShadowCompareMode(CompareMode.Hardware);
    viewPort.addProcessor(slspfr);
    rootNode.addLight(spot);
    DirectionalLight directionalLight2 = new DirectionalLight();
    directionalLight2.setDirection(new Vector3f(-4f, -5f, 1f).normalizeLocal());
    directionalLight2.setColor(ColorRGBA.White.mult(.0125f));
//    rootNode.addLight(directionalLight);
    rootNode.addLight(directionalLight2);
    initScene(rootNode);
    cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    this.setDisplayStatView(true);
    this.setDisplayFps(true);
  }

  private  Material mat;
  private void initScene(Node rootNode) {
    Box b = new Box(5f, .25f, 5f);
    Geometry plane = new Geometry("Plane", b);
    plane.setLocalTranslation(0f, -1.25f, 0f);
    mat = new ShadowMaterial(assetManager, "MatDefs/SinglePassShadowTest.j3md");
    mat.setFloat("ShadowMapSize", 1024f);
//    mat.setBoolean("UseShadow", true);
    plane.setMaterial(mat);
    plane.setShadowMode(RenderQueue.ShadowMode.Receive);
    rootNode.attachChild(plane);


    Sphere s = new Sphere(32, 32, .5f);
    Geometry sphere = new Geometry("Sphere", s);
    sphere.setLocalTranslation(0f, 1.5f, 0f);
    sphere.setMaterial(mat);
    sphere.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(sphere);
    
    
   
    Box q = new Box(1, 1, 1);
    geom = new Geometry("Box", q);
//    mat = mat.clone();
//    if (newShadows) {
//      mat.setColor("Color", ColorRGBA.Gray);
//    } else {
//      mat.setColor("Diffuse", ColorRGBA.Gray);
//    }
    
    
    
    geom.setMaterial(mat);
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(geom);
  }

  float delta = 0f;
  boolean once = true;
  @Override
  public void simpleUpdate(float tpf) {
    geom.rotate(0f, tpf*FastMath.QUARTER_PI, 0f);
    delta += tpf;
    float sin = FastMath.sin(delta) * tpf * 4f;
    for (PointLight pl : pointLights){
      Vector3f position = pl.getPosition();
      position.addLocal(0f, 0f, sin);
    }
    if (once){
      if (delta > 1f){
        mat.getActiveTechnique().getShader().getSources().stream().forEach((source) -> {
          System.out.println(source.getDefines());
          System.out.println("============");
        });
      once = false;
      }
    }
  }

  @Override
  public void simpleRender(RenderManager rm) {
    //TODO: add render code
  }
  
  private PointLight addPointLight(Vector3f location, ColorRGBA color, float radius){
      PointLight pl = new PointLight();
      pl.setPosition(location);
      pl.setColor(color);
      pl.setRadius(radius);
      rootNode.addLight(pl);
      return pl;
  }
  private PointShadowLight addPointShadowLight(Vector3f location, ColorRGBA color, float radius){
      PointShadowLight pl = new PointShadowLight();
      pl.setPosition(location);
      pl.setColor(color);
      pl.setRadius(radius);
      rootNode.addLight(pl);
      return pl;
  }
  
}
