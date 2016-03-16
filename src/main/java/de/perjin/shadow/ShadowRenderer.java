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
package de.perjin.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.PssmShadowUtil;
import com.jme3.shadow.ShadowUtil;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jan
 */
public class ShadowRenderer implements SceneProcessor, Savable {

  protected RenderManager renderManager;
  protected ViewPort viewPort;
  protected FrameBuffer[] directionalFB;
  protected FrameBuffer[] pointFB;
  protected FrameBuffer[] spotFB;
  protected TextureArray directionalShadowTextures;
  protected TextureArray spotShadowTextures;
  protected TextureArray pointShadowTextures;
  protected Texture2D dummyTex;
  protected Material preshadowMat;
  protected Matrix4f[] lightViewProjectionsMatrices;
  protected AssetManager assetManager;
  protected boolean debug = false;
  protected float edgesThickness = 1.0f;
  protected EdgeFilteringMode edgeFilteringMode = EdgeFilteringMode.Bilinear;
  protected CompareMode shadowCompareMode = CompareMode.Hardware;
//  protected Picture[] dispPic;
  /**
   * list of materials for post shadow queue geometries
   */
  protected List<Material> matCache = new ArrayList<Material>();
  protected List<DirectionalLight> directionalLights = new ArrayList<>(1);
  protected List<PointLight> pointLights = new ArrayList<>(1);
  protected List<SpotLight> spotLights = new ArrayList<>(1);
  protected GeometryList lightReceivers = new GeometryList(new OpaqueComparator());
  protected GeometryList shadowMapOccluders = new GeometryList(new OpaqueComparator());
//  private String[] lightViewStringCache;
  /**
   * fade shadows at distance
   */
  protected float zFarOverride = 0;
  protected Vector2f fadeInfo;
  protected float fadeLength;
  protected Camera frustumCam;

  private Camera directionalShadowCam;
  private Camera pointShadowCam[];
  private Camera spotShadowCam;
  private Vector4f splits[];
  private Vector3f pointLightPosition[];
  protected float[] splitsArray;
  protected float lambda = 0.65f;
  protected Vector3f[] points = new Vector3f[8];
  private final boolean stabilize = true;

  private int maxLightsPerPass;

  private int frameCounter = 0;
  private int directionalShadowLights;
  private float directionalShadowMapSize;
  private float pointShadowMapSize;
  private int pointShadowLights;
  private float spotShadowMapSize;
  private int spotShadowLights;

  private ShadowLightFilter shadowLightFilter;

  private final int shadowMapsPerDirectionalLight = 4;

  /**
   * used for serialization
   */
  protected ShadowRenderer() {
  }

  public ShadowRenderer(ShadowLightFilter shadowLightFilter, AssetManager assetManager,
          int directionalShadowMapSize, int directionalShadowLights,
          int pointShadowMapSize, int pointShadowLights,
          int spotShadowMapSize, int spotShadowLights) {
    this.shadowLightFilter = shadowLightFilter;

    this.assetManager = assetManager;

    this.directionalShadowMapSize = directionalShadowMapSize;
    this.directionalShadowLights = directionalShadowLights;

    this.pointShadowMapSize = pointShadowMapSize;
    this.pointShadowLights = pointShadowLights;

    this.spotShadowMapSize = spotShadowMapSize;
    this.spotShadowLights = spotShadowLights;
    init(assetManager);

  }

  private void init(AssetManager assetManager) {
    for (int i = 0; i < points.length; i++) {
      points[i] = new Vector3f();
    }
    directionalFB = new FrameBuffer[directionalShadowLights * shadowMapsPerDirectionalLight];
    pointFB = new FrameBuffer[pointShadowLights * 6];
    pointLightPosition = new Vector3f[pointShadowLights];
    for (int i = 0; i < pointShadowLights; i++) {
      pointLightPosition[i] = new Vector3f();
    }
    spotFB = new FrameBuffer[spotShadowLights];
    directionalShadowTextures = createTextureArray(directionalShadowLights * shadowMapsPerDirectionalLight, (int) directionalShadowMapSize);
    pointShadowTextures = createTextureArray(pointShadowLights * 6, (int) pointShadowMapSize);
    spotShadowTextures = createTextureArray(spotShadowLights, (int) spotShadowMapSize);

    lightViewProjectionsMatrices = new Matrix4f[directionalShadowLights * shadowMapsPerDirectionalLight + pointShadowLights * 6 + spotShadowLights];
    for (int i = 0; i < lightViewProjectionsMatrices.length; i++) {
      lightViewProjectionsMatrices[i] = new Matrix4f();
    }

    splits = new Vector4f[directionalShadowLights];
    splitsArray = new float[4 + 1];
    for (int i = 0; i < splits.length; i++) {
      splits[i] = new Vector4f();
    }
    directionalShadowCam = new Camera((int) directionalShadowMapSize, (int) directionalShadowMapSize);
    directionalShadowCam.setParallelProjection(true);
    pointShadowCam = new Camera[6];
    for (int i = 0; i < 6; i++) {
      pointShadowCam[i] = new Camera((int) pointShadowMapSize, (int) pointShadowMapSize);
    }
    spotShadowCam = new Camera((int) spotShadowMapSize, (int) spotShadowMapSize);

    //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
//         dummyTex = new Texture2D(shadowMapSize, shadowMapSize, Image.Format.RGBA8);
    preshadowMat = new Material(assetManager, "Common/MatDefs/Shadow/PreShadow.j3md");

    setupFrameBuffer(directionalFB, directionalShadowLights * shadowMapsPerDirectionalLight, (int) directionalShadowMapSize, directionalShadowTextures);
    setupFrameBuffer(pointFB, pointShadowLights * 6, (int) pointShadowMapSize, pointShadowTextures);
    setupFrameBuffer(spotFB, spotShadowLights, (int) spotShadowMapSize, spotShadowTextures);

    setShadowCompareMode(shadowCompareMode);
    setEdgeFilteringMode(edgeFilteringMode);
  }

  /**
   * Sets the filtering mode for shadow edges. See {@link EdgeFilteringMode} for more info.
   *
   * @param filterMode the desired filter mode (not null)
   */
  final public void setEdgeFilteringMode(EdgeFilteringMode filterMode) {
    if (filterMode == null) {
      throw new NullPointerException();
    }

    this.edgeFilteringMode = filterMode;
    if (shadowCompareMode == CompareMode.Hardware) {
      if (filterMode == EdgeFilteringMode.Bilinear || filterMode == EdgeFilteringMode.PCFPOISSON) {
        if (directionalShadowLights > 0) {
          directionalShadowTextures.setMagFilter(Texture.MagFilter.Bilinear);
          directionalShadowTextures.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        }
        if (pointShadowLights > 0) {
          pointShadowTextures.setMagFilter(Texture.MagFilter.Bilinear);
          pointShadowTextures.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        }
        if (spotShadowLights > 0) {
          spotShadowTextures.setMagFilter(Texture.MagFilter.Bilinear);
          spotShadowTextures.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        }
      } else {
        if (directionalShadowLights > 0) {
          directionalShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
          directionalShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        }
        if (pointShadowLights > 0) {
          pointShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
          pointShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        }
        if (spotShadowLights > 0) {
          spotShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
          spotShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        }
      }
    }
  }

  /**
   * returns the edge filtering mode
   *
   * @see EdgeFilteringMode
   * @return
   */
  public EdgeFilteringMode getEdgeFilteringMode() {
    return edgeFilteringMode;
  }

  /**
   * Sets the shadow compare mode. See {@link CompareMode} for more info.
   *
   * @param compareMode the desired compare mode (not null)
   */
  final public void setShadowCompareMode(CompareMode compareMode) {
    if (compareMode == null) {
      throw new IllegalArgumentException("Shadow compare mode cannot be null");
    }

    this.shadowCompareMode = compareMode;
    if (compareMode == CompareMode.Hardware) {
      if (directionalShadowLights > 0) {
        directionalShadowTextures.setShadowCompareMode(Texture.ShadowCompareMode.LessOrEqual);

      }
      if (pointShadowLights > 0) {

        pointShadowTextures.setShadowCompareMode(Texture.ShadowCompareMode.LessOrEqual);
      }
      if (spotShadowLights > 0) {

        spotShadowTextures.setShadowCompareMode(Texture.ShadowCompareMode.LessOrEqual);
      }
      if (edgeFilteringMode == EdgeFilteringMode.Bilinear) {
        if (directionalShadowLights > 0) {
          directionalShadowTextures.setMagFilter(Texture.MagFilter.Bilinear);
          directionalShadowTextures.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);

        }
        if (pointShadowLights > 0) {
          pointShadowTextures.setMagFilter(Texture.MagFilter.Bilinear);
          pointShadowTextures.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);

        }
        if (spotShadowLights > 0) {
          spotShadowTextures.setMagFilter(Texture.MagFilter.Bilinear);
          spotShadowTextures.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);

        }
      } else {
        if (directionalShadowLights > 0) {
          directionalShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
          directionalShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        }
        if (pointShadowLights > 0) {
          pointShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
          pointShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        }
        if (spotShadowLights > 0) {
          spotShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
          spotShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        }
      }
    } else {
      if (directionalShadowLights > 0) {
        directionalShadowTextures.setShadowCompareMode(Texture.ShadowCompareMode.Off);
        directionalShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
        directionalShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

      }
      if (pointShadowLights > 0) {
        pointShadowTextures.setShadowCompareMode(Texture.ShadowCompareMode.Off);
        pointShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
        pointShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

      }
      if (spotShadowLights > 0) {
        spotShadowTextures.setShadowCompareMode(Texture.ShadowCompareMode.Off);
        spotShadowTextures.setMagFilter(Texture.MagFilter.Nearest);
        spotShadowTextures.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

      }
    }
  }

  /**
   * returns the shadow compare mode
   *
   * @see CompareMode
   * @return the shadowCompareMode
   */
  public CompareMode getShadowCompareMode() {
    return shadowCompareMode;
  }

  /**
   * Initialize this shadow renderer prior to its first update.
   *
   * @param rm the render manager
   * @param vp the viewport
   */
  @Override
  public void initialize(RenderManager rm, ViewPort vp) {
    renderManager = rm;
    viewPort = vp;
    maxLightsPerPass = rm.getSinglePassLightBatchSize();
//        if (zFarOverride > 0 && frustumCam == null) {
//            initFrustumCam();
//        }
  }

//    /**
//     * delegates the initialization of the frustum cam to child renderers
//     */
//    protected void initFrustumCam() {
//    }
  /**
   * Test whether this shadow renderer has been initialized.
   *
   * @return true if initialized, otherwise false
   */
  public boolean isInitialized() {
    return viewPort != null;
  }

  /**
   * Invoked once per frame to update the shadow cams according to the light view.
   *
   * @param viewCam the scene cam
   * @param shadowIndex
   */
  protected void updateDirectionalShadowCams(Camera viewCam, int lightIndex, float frustumNear, float zFar) {

    //shadowCam.setDirection(direction);
    directionalShadowCam.getRotation().lookAt(((DirectionalLight) directionalLights.get(lightIndex)).getDirection(), Vector3f.UNIT_Y);
    directionalShadowCam.update();
    directionalShadowCam.updateViewProjection();

    PssmShadowUtil.updateFrustumSplits(splitsArray, frustumNear, zFar, lambda);

    // in parallel projection shadow position goe from 0 to 1
    if (viewCam.isParallelProjection()) {
      for (int i = 0; i < 5; i++) {
        splitsArray[i] = splitsArray[i] / (zFar - frustumNear);
      }
    }

    switch (splitsArray.length) {
      case 5:
        splits[lightIndex].w = splitsArray[4];
      case 4:
        splits[lightIndex].z = splitsArray[3];
      case 3:
        splits[lightIndex].y = splitsArray[2];
      case 2:
      case 1:
        splits[lightIndex].x = splitsArray[1];
        break;
    }
  }

  protected void updatePointShadowCams(Camera viewCam, int lightIndex) {

//        if (light == null) {
//            throw new IllegalStateException("The light can't be null for a " + this.getClass().getName());
//        }
    //bottom
    pointShadowCam[0].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y.mult(-1f));

    //top
    pointShadowCam[1].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z, Vector3f.UNIT_Y);

    //forward
    pointShadowCam[2].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_Z.mult(-1f));

    //backward
    pointShadowCam[3].setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);

    //left
    pointShadowCam[4].setAxes(Vector3f.UNIT_Z, Vector3f.UNIT_Y, Vector3f.UNIT_X.mult(-1f));

    //right
    pointShadowCam[5].setAxes(Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_X);

    for (int i = 0; i < 6; i++) {
      pointShadowCam[i].setFrustumPerspective(90f, 1f, 0.1f, pointLights.get(lightIndex).getRadius());
      pointShadowCam[i].setLocation(pointLights.get(lightIndex).getPosition());
      pointShadowCam[i].update();
      pointShadowCam[i].updateViewProjection();
    }
  }

  protected void updateSpotShadowCams(Camera viewCam, int camIndex) {

    spotShadowCam.setFrustumPerspective(spotLights.get(camIndex).getSpotOuterAngle() * FastMath.RAD_TO_DEG * 2.0f, 1, 1f, spotLights.get(camIndex).getSpotRange());
    spotShadowCam.getRotation().lookAt(spotLights.get(camIndex).getDirection(), spotShadowCam.getUp());
    spotShadowCam.setLocation(spotLights.get(camIndex).getPosition());

    spotShadowCam.update();
    spotShadowCam.updateViewProjection();
  }

  /**
   * Returns a subclass-specific geometryList containing the occluders to be rendered in the shadow map
   *
   * @param shadowMapIndex the index of the shadow map being rendered
   * @param shadowMapOccluders
   * @return
   */
//    protected GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders, int shadocCameraIndex) {
//        return null;
//    }
  protected GeometryList getDirectionalOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders, int shadowCameraIndex) {
    shadowMapOccluders.clear();
    // update frustum points based on current camera and split
    ShadowUtil.updateFrustumPoints(viewPort.getCamera(), splitsArray[shadowMapIndex % 4], splitsArray[shadowMapIndex % 4 + 1], 1.0f, points);

    //Updating shadow cam with curent split frustra
    if (lightReceivers.size() == 0) {
      for (Spatial scene : viewPort.getScenes()) {
        ShadowUtil.getGeometriesInCamFrustum(scene, viewPort.getCamera(), RenderQueue.ShadowMode.Receive, lightReceivers);
      }
    }
    ShadowUtil.updateShadowCamera(viewPort, lightReceivers, directionalShadowCam, points, shadowMapOccluders, stabilize ? directionalShadowMapSize : 0);
//        for (int i = 0; i < shadowMapOccluders.size(); i++)
//        System.out.println(shadowMapOccluders.get(i));
    return shadowMapOccluders;
  }

  protected GeometryList getSpotOccludersToRender(int shadowCameraIndex, GeometryList shadowMapOccluders) {
    shadowMapOccluders.clear();
    for (Spatial scene : viewPort.getScenes()) {
      // TODO: ShadowUtil.addGeometriesInCamFrustumFromNode() is probably not treating instanced meshes correctly.
      ShadowUtil.getGeometriesInCamFrustum(scene, spotShadowCam, RenderQueue.ShadowMode.Cast, shadowMapOccluders);
    }
    return shadowMapOccluders;
  }

  protected GeometryList getPointOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders) {
    shadowMapOccluders.clear();
    for (Spatial scene : viewPort.getScenes()) {
      // TODO: ShadowUtil.addGeometriesInCamFrustumFromNode() is probably not treating instanced meshes correctly.
      ShadowUtil.getGeometriesInCamFrustum(scene, pointShadowCam[shadowMapIndex], RenderQueue.ShadowMode.Cast, shadowMapOccluders);
    }
    return shadowMapOccluders;
  }

  @SuppressWarnings("fallthrough")
  @Override
  public void postQueue(RenderQueue rq) {

  }

  protected void renderDirectionalShadowMap(int shadowMapIndex, int camIndex) {
    shadowMapOccluders = getDirectionalOccludersToRender(shadowMapIndex, shadowMapOccluders, camIndex);

    //saving light view projection matrix for this split            
    lightViewProjectionsMatrices[shadowMapIndex].set(directionalShadowCam.getViewProjectionMatrix());
    renderManager.setCamera(directionalShadowCam, false);

    renderManager.getRenderer().setFrameBuffer(directionalFB[shadowMapIndex]);
    renderManager.getRenderer().clearBuffers(true, true, true);

    // render shadow casters to shadow map
    viewPort.getQueue().renderShadowQueue(shadowMapOccluders, renderManager, directionalShadowCam, false);
  }

  protected void renderPointShadowMap(int lightIndex) {
    for (int i = 0; i < 6; i++) {
      shadowMapOccluders = getPointOccludersToRender(i, shadowMapOccluders);

      lightViewProjectionsMatrices[directionalLights.size() * shadowMapsPerDirectionalLight + lightIndex * 6 + i].set(pointShadowCam[i].getViewProjectionMatrix());
      renderManager.setCamera(pointShadowCam[i], false);

      renderManager.getRenderer().setFrameBuffer(pointFB[i + lightIndex * 6]);
      renderManager.getRenderer().clearBuffers(true, true, true);

      // render shadow casters to shadow map
      viewPort.getQueue().renderShadowQueue(shadowMapOccluders, renderManager, pointShadowCam[i], false);
    }
  }

  protected void renderSpotShadowMap(int lightIndex) {
    shadowMapOccluders = getSpotOccludersToRender(lightIndex, shadowMapOccluders);

    //saving light view projection matrix for this split            
    lightViewProjectionsMatrices[directionalLights.size() * shadowMapsPerDirectionalLight + pointLights.size() * 6 + lightIndex].set(spotShadowCam.getViewProjectionMatrix());
    renderManager.setCamera(spotShadowCam, false);

    renderManager.getRenderer().setFrameBuffer(spotFB[lightIndex]);
    renderManager.getRenderer().clearBuffers(true, true, true);

    // render shadow casters to shadow map
    viewPort.getQueue().renderShadowQueue(shadowMapOccluders, renderManager, spotShadowCam, false);
  }

  protected void getReceivers(GeometryList lightReceivers) {
    lightReceivers.clear();
    for (Spatial scene : viewPort.getScenes()) {
      ShadowUtil.getGeometriesInCamFrustum(scene, viewPort.getCamera(), RenderQueue.ShadowMode.Receive, lightReceivers);
    }
  }

//    protected void getSpotReceivers(GeometryList lightReceivers) {
//        lightReceivers.clear();
//        for (Spatial scene : viewPort.getScenes()) {
//            ShadowUtil.getLitGeometriesInViewPort(scene, viewPort.getCamera(), spotShadowCam, RenderQueue.ShadowMode.Receive, lightReceivers);
//        }
//    }
  public void postFrame(FrameBuffer out) {

  }


  private void clearMatParams() {
    for (Material mat : matCache) {
      mat.clearParam("TOTALSHADOWMAPS");
      mat.clearParam("LIGHTS");
      mat.clearParam("ShadowLightViewProjectionMatrix");
      mat.clearParam("DirectionalShadowMaps");
      mat.clearParam("DIRECTIONALSHADOWMAP_SIZE");
      mat.clearParam("MAXDIRECTIONALSHADOWLIGHTS");
      mat.clearParam("Splits");
      mat.clearParam("ActiveDirectionalShadows");
      mat.clearParam("PointShadowMaps");
      mat.clearParam("POINTSHADOWMAP_SIZE");
      mat.clearParam("MAXPOINTSHADOWLIGHTS");
      mat.clearParam("ActivePointShadows");
      mat.clearParam("PointLightPosition");
      mat.clearParam("SpotShadowMaps");
      mat.clearParam("SPOTSHADOWMAP_SIZE");
      mat.clearParam("MAXSPOTSHADOWLIGHTS");
      mat.clearParam("ActiveSpotShadows");
    }
  }

  /**
   * This method is called once per frame and is responsible for setting any material parameters that subclasses may
   * need to set on the post material.
   *
   * @param material the material to use for the post shadow pass
   */
  protected void setMaterialParameters(Material material) {
    material.setInt("TOTALSHADOWMAPS", directionalShadowLights * shadowMapsPerDirectionalLight + pointShadowLights * 6 + spotShadowLights);
    material.setInt("LIGHTS", maxLightsPerPass);
    material.setParam("ShadowLightViewProjectionMatrix", VarType.Matrix4Array, lightViewProjectionsMatrices);
    if (directionalShadowLights > 0) {
      material.setParam("DirectionalShadowMaps", VarType.TextureArray, directionalShadowTextures);
      material.setFloat("DIRECTIONALSHADOWMAP_SIZE", directionalShadowMapSize);
      material.setInt("MAXDIRECTIONALSHADOWLIGHTS", directionalShadowLights);
      material.setParam("Splits", VarType.Vector4Array, splits);
      material.setInt("ActiveDirectionalShadows", directionalLights.size());
    }
    if (pointShadowLights > 0) {
      material.setParam("PointShadowMaps", VarType.TextureArray, pointShadowTextures);
      material.setFloat("POINTSHADOWMAP_SIZE", pointShadowMapSize);
      material.setInt("MAXPOINTSHADOWLIGHTS", pointShadowLights);
      material.setInt("ActivePointShadows", pointLights.size());
      material.setParam("PointLightPosition", VarType.Vector3Array, pointLightPosition);
    }
    if (spotShadowLights > 0) {
      material.setParam("SpotShadowMaps", VarType.TextureArray, spotShadowTextures);
      material.setFloat("SPOTSHADOWMAP_SIZE", spotShadowMapSize);
      material.setInt("MAXSPOTSHADOWLIGHTS", spotShadowLights);
      material.setInt("ActiveSpotShadows", spotLights.size());
    }
  }

//  protected void setMaterialParameters(Material material) {
//    material.setParam("ShadowLightViewProjectionMatrix", VarType.Matrix4Array, lightViewProjectionsMatrices);
//    if (directionalShadowLights > 0) {
//      material.setParam("Splits", VarType.Vector4Array, splits);
//      material.setInt("ActiveDirectionalShadows", directionalLights.size());
//    }
//    if (pointShadowLights > 0) {
//      material.setInt("ActivePointShadows", pointLights.size());
//      material.setParam("PointLightPosition", VarType.Vector3Array, pointLightPosition);
//    }
//    if (spotShadowLights > 0) {
//      material.setInt("ActiveSpotShadows", spotLights.size());
//    }
//  }
//
//  private void setMaterialParameterConstants(Material material) {
//    material.setInt("TOTALSHADOWMAPS", directionalShadowLights * shadowMapsPerDirectionalLight + pointShadowLights * 6 + spotShadowLights);
//    material.setInt("LIGHTS", maxLightsPerPass);
//    if (directionalShadowLights > 0) {
//      material.setParam("DirectionalShadowMaps", VarType.TextureArray, directionalShadowTextures);
//      material.setFloat("DIRECTIONALSHADOWMAP_SIZE", directionalShadowMapSize);
//      material.setInt("MAXDIRECTIONALSHADOWLIGHTS", directionalShadowLights);
//    }
//    if (pointShadowLights > 0) {
//      material.setParam("PointShadowMaps", VarType.TextureArray, pointShadowTextures);
//      material.setFloat("POINTSHADOWMAP_SIZE", pointShadowMapSize);
//      material.setInt("MAXPOINTSHADOWLIGHTS", pointShadowLights);
//    }
//    if (spotShadowLights > 0) {
//      material.setParam("SpotShadowMaps", VarType.TextureArray, spotShadowTextures);
//      material.setFloat("SPOTSHADOWMAP_SIZE", spotShadowMapSize);
//      material.setInt("MAXSPOTSHADOWLIGHTS", spotShadowLights);
//    }
//  }
  private void setMatParams(GeometryList l) {
    //iteration throught all the geometries of the list to gather the materials

    matCache.clear();
    for (int i = 0; i < l.size(); i++) {
      Material mat = l.get(i).getMaterial();
      //checking if the material has the post technique and adding it to the material cache
      if (!matCache.contains(mat)) {
        matCache.add(mat);
//        setMaterialParameterConstants(mat);
        setMaterialParameters(mat);
      }
    }

    //iterating through the mat cache and setting the parameters
//    for (Material mat : matCache) {
//      setMaterialParameters(mat);
//    }
  }

  /**
   * How far the shadows are rendered in the view
   *
   * @see #setShadowZExtend(float zFar)
   * @return shadowZExtend
   */
  public float getShadowZExtend() {
    return zFarOverride;
  }

  /**
   * Set the distance from the eye where the shadows will be rendered default value is dynamicaly computed to the shadow
   * casters/receivers union bound zFar, capped to view frustum far value.
   *
   * @param zFar the zFar values that override the computed one
   */
  public void setShadowZExtend(float zFar) {
    this.zFarOverride = zFar;
    if (zFarOverride == 0) {
      fadeInfo = null;
      frustumCam = null;
    } else if (fadeInfo != null) {
      fadeInfo.set(zFarOverride - fadeLength, 1f / fadeLength);
    } //            if (frustumCam == null && viewPort != null) {
    //                initFrustumCam();
    //            }
  }

  /**
   * Define the length over which the shadow will fade out when using a shadowZextend This is useful to make dynamic
   * shadows fade into baked shadows in the distance.
   *
   * @param length the fade length in world units
   */
  public void setShadowZFadeLength(float length) {
    if (length == 0) {
      fadeInfo = null;
      fadeLength = 0;
    } else {
      if (zFarOverride == 0) {
        fadeInfo = new Vector2f(0, 0);
      } else {
        fadeInfo = new Vector2f(zFarOverride - length, 1.0f / length);
      }
      fadeLength = length;
    }
  }

  /**
   * get the length over which the shadow will fade out when using a shadowZextend
   *
   * @return the fade length in world units
   */
  public float getShadowZFadeLength() {
    if (fadeInfo != null) {
      return zFarOverride - fadeInfo.x;
    }
    return 0f;
  }

  public void preFrame(float tpf) {
    Camera cam = viewPort.getCamera();
    Renderer r = renderManager.getRenderer();
    //renderManager.setForcedMaterial(preshadowMat);
    renderManager.setForcedTechnique("PreShadow");
    float zFar = zFarOverride;
    if (zFar == 0) {
      zFar = cam.getFrustumFar();
    }

    //We prevent computing the frustum points and splits with zeroed or negative near clip value
    float frustumNear = Math.max(cam.getFrustumNear(), 0.001f);
    ShadowUtil.updateFrustumPoints(cam, frustumNear, zFar, 1.0f, points);

    for (int i = 0; i < directionalLights.size(); i++) {
      updateDirectionalShadowCams(cam, i, frustumNear, zFar);
      for (int shadowMapIndex = i * 4; shadowMapIndex < i * 4 + 4; shadowMapIndex++) {
//                if (shadowMapIndex % 4 == 0 || (shadowMapIndex % 4 == 1 && frameCounter % 2 == 0) || (shadowMapIndex % 4 == 2 && frameCounter == 1) || (shadowMapIndex % 4 == 3 && frameCounter == 3)) {
        renderDirectionalShadowMap(shadowMapIndex, i);
//                }
      }
    }
    for (int i = 0; i < pointLights.size(); i++) {
      pointLightPosition[i].set(pointLights.get(i).getPosition());
      updatePointShadowCams(cam, i);
      renderPointShadowMap(i);
    }
    for (int i = 0; i < spotLights.size(); i++) {
      updateSpotShadowCams(cam, i);
      renderSpotShadowMap(i);
    }
    getReceivers(lightReceivers);
    setMatParams(lightReceivers);
    r.setFrameBuffer(viewPort.getOutputFrameBuffer());
    frameCounter = (frameCounter + 1) % 4;
    //resetting renderManager settings
    renderManager.setForcedTechnique(null);
//        renderManager.setForcedMaterial(null);
    renderManager.setCamera(cam, false);
  }

  @Override
  public void cleanup() {
    //clearing the params in case there are some other shadow renderers
    clearMatParams();
  }

  @Override
  public void reshape(ViewPort vp, int w, int h) {
  }

  /**
   * returns the edges thickness
   *
   * @see #setEdgesThickness(int edgesThickness)
   * @return edgesThickness
   */
  public int getEdgesThickness() {
    return (int) (edgesThickness * 10);
  }

  /**
   * Sets the shadow edges thickness. default is 1, setting it to lower values can help to reduce the jagged effect of
   * the shadow edges
   *
   * @param edgesThickness
   */
  public void setEdgesThickness(int edgesThickness) {
    this.edgesThickness = Math.max(1, Math.min(edgesThickness, 10));
    this.edgesThickness *= 0.1f;
  }

  /**
   * De-serialize this instance, for example when loading from a J3O file.
   *
   * @param im importer (not null)
   */
  public void read(JmeImporter im) throws IOException {
//    InputCapsule ic = (InputCapsule) im.getCapsule(this);
//    assetManager = im.getAssetManager();
//    nbShadowMaps = ic.readInt("nbShadowMaps", 1);
//    shadowMapSize = ic.readFloat("shadowMapSize", 0f);
//    shadowIntensity = ic.readFloat("shadowIntensity", 0.7f);
//    edgeFilteringMode = ic.readEnum("edgeFilteringMode", EdgeFilteringMode.class, EdgeFilteringMode.Bilinear);
//    shadowCompareMode = ic.readEnum("shadowCompareMode", CompareMode.class, CompareMode.Hardware);
//    init(assetManager, nbShadowMaps, (int) shadowMapSize);
//    edgesThickness = ic.readFloat("edgesThickness", 1.0f);
  }

  /**
   * Serialize this instance, for example when saving to a J3O file.
   *
   * @param ex exporter (not null)
   */
  public void write(JmeExporter ex) throws IOException {
//    OutputCapsule oc = (OutputCapsule) ex.getCapsule(this);
//    oc.write(nbShadowMaps, "nbShadowMaps", 1);
//    oc.write(shadowMapSize, "shadowMapSize", 0);
//    oc.write(shadowIntensity, "shadowIntensity", 0.7f);
//    oc.write(edgeFilteringMode, "edgeFilteringMode", EdgeFilteringMode.Bilinear);
//    oc.write(shadowCompareMode, "shadowCompareMode", CompareMode.Hardware);
//    oc.write(edgesThickness, "edgesThickness", 1.0f);
  }

  private TextureArray createTextureArray(int shadowMaps, int shadowMapSize) {
    if (shadowMaps > 0) {
      List<Image> directionalShadowImages = new ArrayList<>();
      for (int i = 0; i < shadowMaps; i++) {
        directionalShadowImages.add(new Image(Image.Format.Depth16, shadowMapSize, shadowMapSize,
                BufferUtils.createByteBuffer(shadowMapSize * shadowMapSize * 2), ColorSpace.Linear));
      }
      TextureArray shadowTexture = new TextureArray(directionalShadowImages);
      shadowTexture.setMagFilter(Texture.MagFilter.Bilinear);
      shadowTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
      //    dispPic = new Picture[nbShadowMaps];
      return shadowTexture;

    }
    return null;
  }

  private void setupFrameBuffer(FrameBuffer[] frameBuffer, int shadowMaps, int shadowMapSize, TextureArray shadowTextures) {
    for (int i = 0; i < shadowMaps; i++) {
      frameBuffer[i] = new FrameBuffer(shadowMapSize, shadowMapSize, 1);

      frameBuffer[i].setDepthTexture(shadowTextures, i);

      //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
//            frameBuffer[i].setColorTexture(dummyTex);
    }
  }

  public void addDirectionalLight(DirectionalLight dl) {
    if (directionalLights.size() < directionalShadowLights) {
      directionalLights.add(dl);
      shadowLightFilter.addDirectionalShadowLight(dl);
    }
  }

  public void addPointLight(PointLight pl) {
    if (pointLights.size() < pointShadowLights) {
      pointLights.add(pl);
      shadowLightFilter.addPointShadowLight(pl);
    }
  }

  public void addSpotLight(SpotLight sl) {
    if (spotLights.size() < spotShadowLights) {
      spotLights.add(sl);
      shadowLightFilter.addSpotShadowLight(sl);
    }
  }

  public void removeDirectionalLight(DirectionalLight dl) {
    directionalLights.remove(dl);
    shadowLightFilter.removeDirectionalLight(dl);
  }

  public void removePointLight(PointLight pl) {
    pointLights.remove(pl);
    shadowLightFilter.removePointShadowLight(pl);
  }

  public void removeSpotLight(SpotLight sl) {
    spotLights.remove(sl);
    shadowLightFilter.removeSpotShadowLight(sl);
  }
}
