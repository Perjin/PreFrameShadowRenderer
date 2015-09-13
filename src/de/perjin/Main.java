package de.perjin;

import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.shader.Shader;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;
import de.perjin.shadow.DirectionalLightShadowPreFrameRenderer;
import de.perjin.shadow.DirectionalShadowLight;
import de.perjin.shadow.PointLightShadowPreFrameRenderer;
import de.perjin.shadow.PointShadowLight;
import de.perjin.shadow.ShadowMaterial;
import de.perjin.shadow.SpotLightShadowPreFrameRenderer;
import de.perjin.shadow.SpotShadowLight;
import java.util.Collection;

/**
 *
 * @author Jan
 */
public class Main extends SimpleApplication {

  private Spatial geom;

  private Light shadowLight = null;
  private SceneProcessor shadowProcessor = null;
  private int activeShadowLight = -1;
  private int newShadowLight = 3;
  private boolean oldShadows = false;
  private boolean activeOldShadows = false;
  private boolean stopMovement = false;
  private final Vector3f lightDirection = new Vector3f(4f, -1f, 0f).normalizeLocal();
  private final Vector3f lightPosition = new Vector3f(2f, .6f, 0f);
  private Node lightSource;
  private Material mat2;
    private InstancedNode instancedNode;

  public static void main(String[] args) {
    Main app = new Main();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
    renderManager.setSinglePassLightBatchSize(6);
//    renderManager.setLightFilter(null);
    ScreenshotAppState screenshotAppState = new ScreenshotAppState("Screenshots/", "ShadowPreFrame", 0);
    stateManager.attach(screenshotAppState);
    flyCam.setMoveSpeed(10f);
    cam.setLocation(new Vector3f(5f, 3f, -4f));
    cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    initScene(rootNode);

    inputManager.addListener(switchShadows, "SwitchShadows");
    inputManager.addListener(switchOldNew, "SwitchToOldRenderer");
    inputManager.addListener(startStop, "StopLightMovement");
    inputManager.addMapping("SwitchShadows", new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addMapping("SwitchToOldRenderer", new KeyTrigger(KeyInput.KEY_1));
    inputManager.addMapping("StopLightMovement", new KeyTrigger(KeyInput.KEY_2));
  }

  private void initScene(Node rootNode) {
    addPointLight(new Vector3f(-2.5f, 0.5f, -2.5f), ColorRGBA.Green.mult(.35f), 6f);
    addPointLight(new Vector3f(2.5f, 1.0f, -2.5f), ColorRGBA.Red.mult(.35f), 5f);
    addPointLight(new Vector3f(-2.5f, 1f, 2.5f), ColorRGBA.Yellow.mult(.35f), 7f);
    addPointLight(new Vector3f(2.5f, 1.5f, 2.5f), ColorRGBA.Magenta.mult(.35f), 4f);

    DirectionalLight directionalLight = new DirectionalLight();
    directionalLight.setDirection(new Vector3f(-4f, -5f, 1f).normalizeLocal());
    directionalLight.setColor(ColorRGBA.White.mult(.0125f));
    rootNode.addLight(directionalLight);

    lightSource = new Node("LightSource");
    lightSource.attachChild(new Geometry("LightMesh", new Sphere(8, 8, .1f)));
    lightSource.attachChild(new Geometry("LightDirection", new Arrow(new Vector3f(0f, 0f, .5f))));
    lightSource.setMaterial(assetManager.loadMaterial("Common/Materials/WhiteColor.j3m"));
    lightSource.setShadowMode(RenderQueue.ShadowMode.Off);
    rootNode.attachChild(lightSource);

    Material mat;
    Box b = new Box(15f, .25f, 15f);
    Geometry plane = new Geometry("Plane", b);
    plane.setLocalTranslation(0f, -1.25f, 0f);
    mat = new ShadowMaterial(assetManager, "MatDefs/SinglePassNormal.j3md");
//    mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    plane.setMaterial(mat);
    TangentBinormalGenerator.generate(plane);
    plane.setShadowMode(RenderQueue.ShadowMode.Receive);
    rootNode.attachChild(plane);

    Sphere s = new Sphere(32, 32, .5f);
    Geometry sphere = new Geometry("Sphere", s);
    TangentBinormalGenerator.generate(sphere);
    sphere.setLocalTranslation(0f, 1.5f, 0f);
    sphere.setMaterial(mat);
    sphere.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(sphere);

    instancedNode = new InstancedNode("InstancedSphere");
    Spatial loadModel = assetManager.loadModel("Models/normal_cube/normal_cube.j3o");
//    Geometry instanceSphere = (Geometry) sphere.deepClone();
//    TangentBinormalGenerator.generate(instanceSphere);
    mat2 = new ShadowMaterial(assetManager, "MatDefs/SinglePassNormal.j3md");
    mat2.setBoolean("UseInstancing", true);
    loadModel.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    //mat2.setTexture("Diffuse", assetManager.loadTexture("Common/Textures/MissingMaterial.png"));
    mat2.setTexture("NormalMap", assetManager.loadTexture("Models/normal_cube/cube_normal.png"));
//    mat.setTexture("NormalMap", assetManager.loadTexture("Models/normal_cube/cube_normal.png"));
    loadModel.setMaterial(mat2);
    Geometry instanceSphere1 = (Geometry) loadModel.clone();
    Geometry instanceSphere2 = (Geometry) loadModel.clone();
    Geometry instanceSphere3 = (Geometry) loadModel.clone();
    Geometry instanceSphere4 = (Geometry) loadModel.clone();
    loadModel.setLocalTranslation(7f, 0.f, 0f);
    instanceSphere1.setLocalTranslation(0f, 0.f, -7f);
    instanceSphere2.setLocalTranslation(7f, 0.f, 7f);
    instanceSphere3.setLocalTranslation(0f, 0.f, 7f);
    instanceSphere4.setLocalTranslation(-7f, 0.f, -7f);
    instancedNode.attachChild(loadModel);
    instancedNode.attachChild(instanceSphere1);
    instancedNode.attachChild(instanceSphere2);
    instancedNode.attachChild(instanceSphere3);
    instancedNode.attachChild(instanceSphere4);
    instancedNode.instance();
    rootNode.attachChild(instancedNode);
    instancedNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    Box q = new Box(1, 1, 1);
    geom = loadModel.clone();
    geom.setLocalTranslation(0f, 0f, 0f);
    geom.setMaterial(mat);
    TangentBinormalGenerator.generate(geom);
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(geom);

    Cylinder cylinder = new Cylinder(4, 16, .25f, 4f, true);
    Geometry cylinderGeom = new Geometry("Cylinder", cylinder);
    TangentBinormalGenerator.generate(cylinderGeom);
    cylinderGeom.setLocalTranslation(3f, 1f, 3f);
    cylinderGeom.rotate(FastMath.HALF_PI, 0f, 0f);
    cylinderGeom.setMaterial(mat);
    cylinderGeom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(cylinderGeom);
    Geometry clone = cylinderGeom.clone();
    Geometry clone1 = cylinderGeom.clone();
    Geometry clone2 = cylinderGeom.clone();
    clone.setLocalTranslation(-3f, 1f, 3f);
    clone1.setLocalTranslation(3f, 1f, -3f);
    clone2.setLocalTranslation(-3f, 1f, -3f);
    rootNode.attachChild(clone);
    rootNode.attachChild(clone1);
    rootNode.attachChild(clone2);
    Spatial jaime = assetManager.loadModel("Models/Jaime/Jaime.j3o");
    rootNode.attachChild(jaime);
    Material jaimeMat = mat.clone();
    jaime.setLocalTranslation(0f, -1f, 3f);
    jaime.rotate(0f, -FastMath.HALF_PI, 0f);
    Texture jaimeNormal = assetManager.loadTexture("Models/Jaime/NormalMap-flipped.png");
    Texture jaimeDiffuse = assetManager.loadTexture("Models/Jaime/diffuseMap-flipped.jpg");
    jaimeMat.setTexture("NormalMap", jaimeNormal);
    jaimeMat.setTexture("DiffuseMap", jaimeDiffuse);
    jaime.setMaterial(jaimeMat);
    jaime.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    AnimControl control = jaime.getControl(AnimControl.class);
    if (control == null){
      System.out.println("Jaime doesn't have an anim control");
    } else {
      System.out.println(control.getAnimationNames().toString());
      control.createChannel().setAnim("Wave");
    }
    jaime.getControl(SkeletonControl.class).setHardwareSkinningPreferred(true);
    
  }

  float deltaRotation = 0f;
  float deltaPosition = 0f;

  boolean once = true;
  @Override
  public void simpleUpdate(float tpf) {
    if(once && deltaRotation > 4f){
      Collection<Shader.ShaderSource> sources = mat2.getActiveTechnique().getShader().getSources();
      sources.stream().forEach((shaderSource) -> {
        System.out.println(shaderSource.getDefines());
        System.out.println(shaderSource.getSource());
      });
      System.out.println();
      once = false;
    }
    if (!stopMovement) {
      geom.rotate(0f, tpf * FastMath.QUARTER_PI, 0f);
      deltaRotation += tpf;
      float sin = FastMath.sin(deltaPosition) * 4f - 2f;
      float cos = FastMath.cos(deltaPosition) * 4f - 1.5f;
      Quaternion rotation = new Quaternion(new float[]{0f, -tpf * 0.5f, 0f});
      rotation.multLocal(lightDirection);
      lightPosition.setX(sin);
      lightPosition.setZ(cos);
      rotation.lookAt(lightDirection, Vector3f.UNIT_Y);
      lightSource.setLocalRotation(rotation);
      if (shadowLight instanceof DirectionalLight) {
        ((DirectionalLight) shadowLight).setDirection(lightDirection);
      }

      if (shadowLight instanceof PointLight) {
        deltaPosition += tpf;
        lightSource.setLocalTranslation(lightPosition);
        ((PointLight) shadowLight).setPosition(lightPosition);
      }

      if (shadowLight instanceof SpotLight) {
        deltaPosition += tpf;
        lightSource.setLocalTranslation(lightPosition);
        ((SpotLight) shadowLight).setPosition(lightPosition);
        ((SpotLight) shadowLight).setDirection(lightDirection);
      }
    }
    if (activeOldShadows != oldShadows) {
      activeOldShadows = oldShadows;
      activeShadowLight = -1;
    }
    if (!oldShadows) {
      if (activeShadowLight != newShadowLight) {
        activeShadowLight = newShadowLight;
        switch (activeShadowLight) {
          case (0):
            activateDirectionalShadow();
            break;
          case (1):
            activatePointShadow();
            break;
          case (2):
            activateSpotShadow();
            break;
          case (3):
            deactivateShadow();
            break;
          default:
            break;
        }
      }
    } else {
      if (activeShadowLight != newShadowLight) {
        activeShadowLight = newShadowLight;
        switch (activeShadowLight) {
          case (0):
            activateDirectionalShadowOld();
            break;
          case (1):
            activatePointShadowOld();
            break;
          case (2):
            activateSpotShadowOld();
            break;
          case (3):
            deactivateShadow();
            break;
          default:
            break;
        }
      }
    }

  }

  @Override
  public void simpleRender(RenderManager rm) {
  }

  private PointLight addPointLight(Vector3f location, ColorRGBA color, float radius) {
    PointLight pl = new PointLight();
    pl.setPosition(location);
    pl.setColor(color);
    pl.setRadius(radius);
    rootNode.addLight(pl);
    return pl;
  }

  private PointShadowLight addPointShadowLight(Vector3f location, ColorRGBA color, float radius) {
    PointShadowLight pl = new PointShadowLight();
    pl.setPosition(location);
    pl.setColor(color);
    pl.setRadius(radius);
    rootNode.addLight(pl);
    return pl;
  }

  private void activateDirectionalShadow() {
    rootNode.removeLight(shadowLight);
    if (shadowProcessor != null) {
      viewPort.removeProcessor(shadowProcessor);
    }
    DirectionalShadowLight directionalLight = new DirectionalShadowLight();
    shadowLight = directionalLight;
    directionalLight.setDirection(lightDirection);
    directionalLight.setColor(ColorRGBA.White.mult(.375f));
    DirectionalLightShadowPreFrameRenderer dsipr = new DirectionalLightShadowPreFrameRenderer(assetManager, 2048, 4);
    shadowProcessor = dsipr;
    dsipr.setShadowCompareMode(CompareMode.Hardware);
    dsipr.setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
//    dsipr.setEdgesThickness(5);
    dsipr.setLight(directionalLight);
    rootNode.addLight(directionalLight);
    viewPort.addProcessor(dsipr);
  }

  private void activateDirectionalShadowOld() {
    rootNode.removeLight(shadowLight);
    if (shadowProcessor != null) {
      viewPort.removeProcessor(shadowProcessor);
    }
    DirectionalLight directionalLight = new DirectionalLight();
    shadowLight = directionalLight;
    directionalLight.setDirection(lightDirection);
    directionalLight.setColor(ColorRGBA.White.mult(.375f));
    DirectionalLightShadowRenderer dsipr = new DirectionalLightShadowRenderer(assetManager, 2048, 4);
    shadowProcessor = dsipr;
    dsipr.setShadowCompareMode(CompareMode.Hardware);
    dsipr.setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
//    dsipr.setEdgesThickness(5);
    dsipr.setLight(directionalLight);
    rootNode.addLight(directionalLight);
    viewPort.addProcessor(dsipr);
  }

  private void activatePointShadow() {
    rootNode.removeLight(shadowLight);
    if (shadowProcessor != null) {
      viewPort.removeProcessor(shadowProcessor);
    }
    PointLightShadowPreFrameRenderer plspfr = new PointLightShadowPreFrameRenderer(assetManager, 512);
    shadowProcessor = plspfr;
    plspfr.setShadowCompareMode(CompareMode.Hardware);
    PointShadowLight addPointShadowLight = addPointShadowLight(lightPosition, ColorRGBA.White.mult(1.0f), 12f);
    shadowLight = addPointShadowLight;
    plspfr.setLight(addPointShadowLight);
    viewPort.addProcessor(plspfr);
  }

  private void activatePointShadowOld() {
    rootNode.removeLight(shadowLight);
    if (shadowProcessor != null) {
      viewPort.removeProcessor(shadowProcessor);
    }
    PointLightShadowRenderer plspfr = new PointLightShadowRenderer(assetManager, 512);
    shadowProcessor = plspfr;
    plspfr.setShadowCompareMode(CompareMode.Hardware);
    PointLight addPointShadowLight = addPointLight(lightPosition, ColorRGBA.White.mult(1.0f), 12f);
    shadowLight = addPointShadowLight;
    plspfr.setLight(addPointShadowLight);
    viewPort.addProcessor(plspfr);
  }

  private void activateSpotShadow() {
    rootNode.removeLight(shadowLight);
    if (shadowProcessor != null) {
      viewPort.removeProcessor(shadowProcessor);
    }
    SpotLightShadowPreFrameRenderer slspfr = new SpotLightShadowPreFrameRenderer(assetManager, 2048);
    shadowProcessor = slspfr;
    SpotLight spot = new SpotShadowLight();
    shadowLight = spot;
    spot.setDirection(lightDirection);
    spot.setPosition(lightPosition);
    spot.setColor(ColorRGBA.White.mult(1f));
    spot.setSpotOuterAngle(.6f);
    slspfr.setLight(spot);
    slspfr.setShadowCompareMode(CompareMode.Hardware);
    viewPort.addProcessor(slspfr);
    rootNode.addLight(spot);
  }

  private void activateSpotShadowOld() {
    rootNode.removeLight(shadowLight);
    if (shadowProcessor != null) {
      viewPort.removeProcessor(shadowProcessor);
    }
    SpotLightShadowRenderer slspfr = new SpotLightShadowRenderer(assetManager, 512);
    shadowProcessor = slspfr;
    SpotLight spot = new SpotLight();
    shadowLight = spot;
    spot.setDirection(lightDirection);
    spot.setPosition(lightPosition);
    spot.setColor(ColorRGBA.White.mult(1f));
    spot.setSpotOuterAngle(.6f);
    slspfr.setLight(spot);
    slspfr.setShadowCompareMode(CompareMode.Hardware);
    viewPort.addProcessor(slspfr);
    rootNode.addLight(spot);
  }

  private void deactivateShadow() {
    rootNode.removeLight(shadowLight);
    if (shadowProcessor != null) {
      viewPort.removeProcessor(shadowProcessor);
    }
  }

  private ActionListener switchShadows = new ActionListener() {
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
      if (!isPressed) {
        newShadowLight = (newShadowLight + 1) % 3;
      }
    }
  };

  private ActionListener switchOldNew = new ActionListener() {
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
      if (!isPressed) {
        oldShadows = !oldShadows;
      }
    }
  };

  private ActionListener startStop = new ActionListener() {
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
      if (!isPressed) {
        stopMovement = !stopMovement;
      }
    }
  };
}
