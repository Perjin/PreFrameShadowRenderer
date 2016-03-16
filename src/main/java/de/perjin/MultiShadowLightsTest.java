/*
 * Copyright (c) 2015-2016 Jan Ivenz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'Jan Ivenz' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.perjin;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.shader.Shader;
import de.perjin.shadow.ShadowLightFilter;
import de.perjin.shadow.ShadowMaterial;
import de.perjin.shadow.ShadowRenderer;
import java.util.Collection;

/**
 *
 * @author Jan
 */
public class MultiShadowLightsTest extends SimpleApplication {

  private ShadowMaterial material;
  private ShadowRenderer shadowRenderer;
  private Node sphereNode;
  PointLight point0;

  private DirectionalLight directionalLight0;
  private DirectionalLight directionalLight1;
  private DirectionalLight directionalLight2;

  public static void main(String[] args) {
    MultiShadowLightsTest app = new MultiShadowLightsTest();
//    app.showSettings = false;
    app.start();

  }

  @Override
  public void simpleInitApp() {
    renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
    renderManager.setSinglePassLightBatchSize(12);
    ShadowLightFilter shadowLightFilter = new ShadowLightFilter();
    renderManager.setLightFilter(shadowLightFilter);
    stateManager.attachAll(new ScreenshotAppState(), new BasicProfilerState());
    flyCam.setMoveSpeed(20f);
//    material = assetManager.loadMaterial("Models/Jaime/Jaime.j3m");
//    MaterialDef matDef = (MaterialDef) assetManager.loadAsset("Common/MatDefs/Light/Lighting.j3md");
    MaterialDef matDef = (MaterialDef) assetManager.loadAsset("MatDefs/ShadowTest.j3md");
    material = new ShadowMaterial(matDef);

    Mesh boxMesh = new Box(.5f, .5f, .5f);
    Geometry boxGeom = new Geometry("box_0", boxMesh);
    boxGeom.setMaterial(material);
    boxGeom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(boxGeom);
//    
    Mesh sphereMesh = new Sphere(32, 32, .5f);
    Geometry sphereGeom = new Geometry("sphere_0", sphereMesh);
    sphereGeom.setMaterial(material);
    sphereGeom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    sphereNode = new Node("SphereNode");
    sphereNode.attachChild(sphereGeom);
    sphereGeom.setLocalTranslation(2.5f, 0f, 0f);
    rootNode.attachChild(sphereNode);

    Mesh groundMesh = new Box(10f, .5f, 10f);
    Geometry groundGeom = new Geometry("ground", groundMesh);
    groundGeom.setMaterial(material);
    groundGeom.setLocalTranslation(0f, -1f, 0f);
    groundGeom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(groundGeom);

    Mesh cylinder = new Cylinder(4, 16, .1f, 4f, true, false);
    Geometry cylinderGeom = new Geometry("cylinder", cylinder);
    cylinderGeom.setMaterial(material);
    cylinderGeom.rotate(FastMath.HALF_PI, 0f, 0f);
    cylinderGeom.setLocalTranslation(1f, 1f, 1f);
    cylinderGeom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    rootNode.attachChild(cylinderGeom);

    rootNode.setMaterial(material);
    directionalLight0 = new DirectionalLight(new Vector3f(1f, -1f, -1f), new ColorRGBA(0.0f, .125f, .12575f, 1.0f));
    directionalLight1 = new DirectionalLight(new Vector3f(-1f, -1f, -1f), new ColorRGBA(0.2575f, .0f, .125f, 1.0f));
    directionalLight2 = new DirectionalLight(new Vector3f(0.0f, -1f, 1f), new ColorRGBA(0.125f, 0.575f, 0.0f, 1.0f));
    rootNode.addLight(directionalLight0);
//    rootNode.addLight(directionalLight1);
//    rootNode.addLight(directionalLight2);

    shadowRenderer = new ShadowRenderer(shadowLightFilter, assetManager, 2048, 1, 512, 3, 512, 4);
    viewPort.addProcessor(shadowRenderer);

    SpotLight spot0 = new SpotLight(new Vector3f(-7f, 3f, 0f), new Vector3f(1.25f, -.410f, 0f).normalizeLocal(), 10f, new ColorRGBA(0.0f, 1.0f, 0.0f, 0.0f));
    SpotLight spot1 = new SpotLight(new Vector3f(7f, 2f, 0f), new Vector3f(-1.25f, -.210f, 0f).normalizeLocal(), 10f, new ColorRGBA(1.0f, 0.0f, 0.0f, 0.0f));
    SpotLight spot2 = new SpotLight(new Vector3f(0f, 2.33f, -7f), new Vector3f(0f, -.270f, 1.250f).normalizeLocal(), 10f, new ColorRGBA(0.0f, 0.0f, 1.0f, 0.0f));
    SpotLight spot3 = new SpotLight(new Vector3f(0f, 2.66f, 7f), new Vector3f(0f, -.340f, -1.250f).normalizeLocal(), 10f, new ColorRGBA(0.8f, 0.8f, 0.0f, 0.0f));
    spot3.setSpotInnerAngle(FastMath.HALF_PI*0.1f);
    spot3.setSpotOuterAngle(FastMath.HALF_PI*0.3f);
    point0 = new PointLight(new Vector3f(.35f, 3f, 0f), new ColorRGBA(0.620f, .620f, .120f, 1.0f));
    point0.setRadius(15f);
    PointLight point1 = new PointLight(new Vector3f(5f, 1f, 3f), new ColorRGBA(0.120f, 0.620f, .620f, 1.0f));
    point1.setRadius(25f);
    PointLight point2 = new PointLight(new Vector3f(-6f, 1f, 4f), new ColorRGBA(0.120f, .620f, 0.620f, 1.0f));
    point2.setRadius(20f);
    PointLight point3 = new PointLight(new Vector3f(-10f, 1f, 4f), new ColorRGBA(0.320f, .420f, 0.620f, 1.0f));
    point3.setRadius(5f);
    PointLight point4 = new PointLight(new Vector3f(-6f, 1f, 10f), new ColorRGBA(0.120f, .320f, 0.920f, 1.0f));
    point4.setRadius(5f);
    PointLight point5 = new PointLight(new Vector3f(10f, 1f, -4f), new ColorRGBA(0.420f, .320f, 0.620f, 1.0f));
    point5.setRadius(5f);
    PointLight point6 = new PointLight(new Vector3f(6f, 1f, -10f), new ColorRGBA(0.420f, .620f, 0.320f, 1.0f));
    point6.setRadius(5f);
    rootNode.addLight(point0);
    rootNode.addLight(point1);
    rootNode.addLight(point2);
    rootNode.addLight(point3);
    rootNode.addLight(point4);
    rootNode.addLight(point5);
    rootNode.addLight(point6);
//    rootNode.addLight(point1);
    rootNode.addLight(spot0);
    rootNode.addLight(spot1);
    rootNode.addLight(spot2);
    rootNode.addLight(spot3);
    shadowRenderer.addPointLight(point0);
    shadowRenderer.addPointLight(point1);
    shadowRenderer.addPointLight(point2);
//    shadowRenderer.addPointLight(point4);
    shadowRenderer.addSpotLight(spot0);
    shadowRenderer.addSpotLight(spot1);
    shadowRenderer.addSpotLight(spot2);
    shadowRenderer.addSpotLight(spot3);
    shadowRenderer.addDirectionalLight(directionalLight0);
//    shadowRenderer.addDirectionalLight(directionalLight1);

  }

  float time = 0f;
  boolean once = true;
  boolean lightOn = true;
  float delta = 2f;

  @Override
  public void simpleUpdate(float tpf) {
    time += tpf;
    if (once && time > 1f) {
      once = false;
      Collection<Shader.ShaderSource> sources = material.getActiveTechnique().getShader().getSources();
      sources.stream().forEach((shaderSource) -> {
        System.out.println(shaderSource.getDefines());
        System.out.println(shaderSource.getSource());
      });
      System.out.println();
//      shadowRenderer.removeDirectionalLight(directionalLight0);
//      rootNode.removeLight(directionalLight0);
//      dsspr.addLight(directionalLight1);
//      dsspr.addLight(directionalLight2);
//        viewPort.removeProcessor(dsspr);
    }
//    if (time > delta) {
//      delta += 2f;
//      if (lightOn) {
//        shadowRenderer.removePointLight(point0);
//      } else {
//        shadowRenderer.addPointLight(point0);
//      }
//      lightOn = !lightOn;
//    }
    sphereNode.rotate(0f, tpf, 0f);
    point0.getPosition().setZ(FastMath.sin(time) * 4f);
  }

}
