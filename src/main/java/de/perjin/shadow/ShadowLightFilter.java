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

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightFilter;
import com.jme3.light.LightList;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.util.TempVars;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Jan
 */
public class ShadowLightFilter implements LightFilter {

  private Camera camera;
  private final HashSet<Light> processedLights = new HashSet<>();
  private final ArrayList<Light> directionalShadowLights = new ArrayList<>();
  private final ArrayList<Light> pointShadowLights = new ArrayList<>();
  private final ArrayList<Light> spotShadowLights = new ArrayList<>();

  @Override
  public void setCamera(Camera camera) {
    this.camera = camera;
    for (Light light : processedLights) {
      light.frustumCheckNeeded = true;
    }
  }

  public void addDirectionalShadowLight(Light directionalLight) {
    if (!this.directionalShadowLights.contains(directionalLight)) {
      this.directionalShadowLights.add(directionalLight);
    }
  }

  public void addPointShadowLight(Light pointLight) {
    if (!this.pointShadowLights.contains(pointLight)) {
      this.pointShadowLights.add(pointLight);
    }
  }

  public void addSpotShadowLight(Light spotShadowLight) {
    if (!this.spotShadowLights.contains(spotShadowLight)) {
      this.spotShadowLights.add(spotShadowLight);
    }
  }

  public void removeDirectionalLight(DirectionalLight directionalLight) {
    this.directionalShadowLights.remove(directionalLight);
  }

  public void removePointShadowLight(Light pointLight) {
    this.pointShadowLights.remove(pointLight);
  }

  public void removeSpotShadowLight(Light spotShadowLight) {
    this.spotShadowLights.remove(spotShadowLight);
  }

  @Override
  public void filterLights(Geometry geometry, LightList filteredLightList) {
    TempVars vars = TempVars.get();
    try {
      LightList worldLights = geometry.getWorldLightList().clone();

      this.directionalShadowLights.stream().forEach((light -> {
        worldLights.remove(light);
        filteredLightList.add(light);
      }));

      this.pointShadowLights.stream().forEach((light -> {
        worldLights.remove(light);
        filteredLightList.add(light);
      }));

      this.spotShadowLights.stream().forEach((light -> {
        worldLights.remove(light);
        filteredLightList.add(light);
      }));

      for (int i = 0; i < worldLights.size(); i++) {
        Light light = worldLights.get(i);

        // If this light is not enabled it will be ignored.
        if (!light.isEnabled()) {
          continue;
        }

        if (light.frustumCheckNeeded) {
          processedLights.add(light);
          light.frustumCheckNeeded = false;
          light.intersectsFrustum = light.intersectsFrustum(camera, vars);
        }

        if (!light.intersectsFrustum) {
          continue;
        }

        BoundingVolume bv = geometry.getWorldBound();

        if (bv instanceof BoundingBox) {
          if (!light.intersectsBox((BoundingBox) bv, vars)) {
            continue;
          }
        } else if (bv instanceof BoundingSphere) {
          if (!Float.isInfinite(((BoundingSphere) bv).getRadius())) {
            if (!light.intersectsSphere((BoundingSphere) bv, vars)) {
              continue;
            }
          }
        }

        filteredLightList.add(light);
      }
    } finally {
      vars.release();
    }
  }
}
