/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.perjin.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;

/**
 *
 * @author Jan
 */
public class ShadowMaterial extends Material {

  public ShadowMaterial(AssetManager assetManager, String defName) {
    super(assetManager, defName);
  }

  public ShadowMaterial() {
    super();
  }

  public ShadowMaterial(MaterialDef materialDef) {
    super(materialDef);
  }

  @Override
  protected int updateLightListUniforms(Shader shader, Geometry g, LightList lightList, int numLights, RenderManager rm, int startIndex) {
    int returnValue = super.updateLightListUniforms(shader, g, lightList, numLights, rm, startIndex);
    int curIndex;
    int endIndex = numLights + startIndex;
    int shadowLight = -1;
    for (curIndex = startIndex; curIndex < endIndex && curIndex < lightList.size(); curIndex++) {

      Light l = lightList.get(curIndex);
      if (l.getType() == Light.Type.Ambient) {
        endIndex++;
        continue;
      }
      if (l instanceof ShadowLight) {
        shadowLight = curIndex - startIndex;
        break;
      }
    }
    shader.getUniform("m_ShadowLight").setValue(VarType.Int, shadowLight * 3);
    return returnValue;
  }

}
