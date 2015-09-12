/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.perjin.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.ShadowUtil;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 *
 * @author Jan
 */
public class PointLightShadowPreFrameRenderer extends AbstractShadowPreFrameRenderer{
    
    public static final int CAM_NUMBER = 6;
    protected PointShadowLight light;
    protected Camera[] shadowCams;
    private Geometry[] frustums = null;

    /**
     * Used for serialization use
     * PointLightShadowRenderer"PointLightShadowRenderer(AssetManager
     * assetManager, int shadowMapSize)
     */
    public PointLightShadowPreFrameRenderer() {
        super();
    }

    /**
     * Creates a PointLightShadowRenderer
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...)
     */
    public PointLightShadowPreFrameRenderer(AssetManager assetManager, int shadowMapSize) {
        super(assetManager, shadowMapSize, CAM_NUMBER);
        init(shadowMapSize);
    }

    private void init(int shadowMapSize) {
        shadowCams = new Camera[CAM_NUMBER];
        for (int i = 0; i < CAM_NUMBER; i++) {
            shadowCams[i] = new Camera(shadowMapSize, shadowMapSize);
        }
    }
    
    @Override
    protected void initFrustumCam() {
        Camera viewCam = viewPort.getCamera();
        frustumCam = viewCam.clone();
        frustumCam.setFrustum(viewCam.getFrustumNear(), zFarOverride, viewCam.getFrustumLeft(), viewCam.getFrustumRight(), viewCam.getFrustumTop(), viewCam.getFrustumBottom());
    }
    

    @Override
    protected void updateShadowCams(Camera viewCam) {

        if (light == null) {
            throw new IllegalStateException("The light can't be null for a " + this.getClass().getName());
        }

        //bottom
        shadowCams[0].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y.mult(-1f));

        //top
        shadowCams[1].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z, Vector3f.UNIT_Y);

        //forward
        shadowCams[2].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_Z.mult(-1f));

        //backward
        shadowCams[3].setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);

        //left
        shadowCams[4].setAxes(Vector3f.UNIT_Z, Vector3f.UNIT_Y, Vector3f.UNIT_X.mult(-1f));

        //right
        shadowCams[5].setAxes(Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_X);

        for (int i = 0; i < CAM_NUMBER; i++) {
            shadowCams[i].setFrustumPerspective(90f, 1f, 0.1f, light.getRadius());
            shadowCams[i].setLocation(light.getPosition());
            shadowCams[i].update();
            shadowCams[i].updateViewProjection();
        }

    }

    @Override
    protected GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders) {
        for (Spatial scene : viewPort.getScenes()) {
          // TODO: ShadowUtil.addGeometriesInCamFrustumFromNode() is probably not treating instanced meshes correctly.
            ShadowUtil.getGeometriesInCamFrustum(scene, shadowCams[shadowMapIndex], RenderQueue.ShadowMode.Cast, shadowMapOccluders);
        }
        return shadowMapOccluders;
    }

    @Override
    protected void getReceivers(GeometryList lightReceivers) {
        lightReceivers.clear();
        for (Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getLitGeometriesInViewPort(scene, viewPort.getCamera(), shadowCams, RenderQueue.ShadowMode.Receive, lightReceivers);
        }
    }

    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCams[shadowMapIndex];
    }

    @Override
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
        if (frustums == null) {
            frustums = new Geometry[CAM_NUMBER];
            Vector3f[] points = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                points[i] = new Vector3f();
            }
            for (int i = 0; i < CAM_NUMBER; i++) {
                ShadowUtil.updateFrustumPoints2(shadowCams[i], points);
                frustums[i] = createFrustum(points, i);
            }
        }
        if (frustums[shadowMapIndex].getParent() == null) {
            ((Node) viewPort.getScenes().get(0)).attachChild(frustums[shadowMapIndex]);
        }
    }

    @Override
    protected void setMaterialParameters(Material material) {
        material.setVector3("LightPos", light.getPosition());
    }

    @Override
    protected void clearMaterialParameters(Material material) {
        material.clearParam("LightPos");        
    }
    
    /**
     * gets the point light used to cast shadows with this processor
     *
     * @return the point light
     */
    public PointLight getLight() {
        return light;
    }

    /**
     * sets the light to use for casting shadows with this processor
     *
     * @param light the point light
     */
    public void setLight(PointShadowLight light) {
        this.light = light;
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = (InputCapsule) im.getCapsule(this);
        light = (PointShadowLight) ic.readSavable("light", null);
        init((int) shadowMapSize);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = (OutputCapsule) ex.getCapsule(this);
        oc.write(light, "light", null);
    }
    
    /**
     *
     * @param viewCam
     * @return 
     */
    @Override
    protected boolean checkCulling(Camera viewCam) {      
        Camera cam = viewCam;
        if(frustumCam != null){
            cam = frustumCam;            
            cam.setLocation(viewCam.getLocation());
            cam.setRotation(viewCam.getRotation());
        }
        TempVars vars = TempVars.get();
        boolean intersects = light.intersectsFrustum(cam,vars);
        vars.release();
        return intersects;
    }
}
