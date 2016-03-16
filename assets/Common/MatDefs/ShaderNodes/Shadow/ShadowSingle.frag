// Because gpu_shader5 is actually where those
// gather functions are declared to work on shadowmaps
#extension GL_ARB_gpu_shader5 : enable

// Lights need to be sortet: DirectionalShadowLights, PointShadowLights, SpotShadowLights (shadow maps for each light type have to be rendered in the same order)
// Then all lights without shadows.

							
// ShadowMaps: three types: directional, point, spot
// Defines required:
// MAXDIRECTIONALSHADOWLIGHTS
// MAXPOINTSHADOWLIGHTS
// MAXSPOTSHADOWLIGHTS
// SHADOWMAPSPERDIRECTIONALLIGHT
// DIRECTIONALSHADOWMAP_SIZE
// POINTSHADOWMAP_SIZE
// SPOTSHADOWMAP_SIZE
#ifdef TOTALSHADOWMAPS
  #define SHADOWCOMPAREOFFSET(tex,coord,offset) textureOffset(tex, coord, offset)
  #define SHADOWCOMPARE(tex,coord) texture(tex, coord)
  #define SHADOWGATHER(tex,coord) textureGather(tex, coord.xyw, coord.z)
  #define GETSHADOW Shadow_DoBilinear_2x2
  #define SHADOWMAP sampler2DArrayShadow


  float shadowBorderScale = 1.0;

  float Shadow_BorderCheck(in vec2 coord){
      // Fastest, "hack" method (uses 4-5 instructions)
      vec4 t = vec4(coord.xy, 0.0, 1.0);
      t = step(t.wwxy, t.xyzz);
      return dot(t,t);  
  }

  float Shadow_DoBilinear_2x2(in SHADOWMAP tex, in vec4 projCoord, in float layer, in float shadowMapSize){
      float border = Shadow_BorderCheck(projCoord.xy/projCoord.w);
      if (border > 0.0)
          return 1.0;

      #ifdef GL_ARB_gpu_shader5

          vec4 coord = vec4(projCoord.xyz/projCoord.w,layer);
          vec4 gather = SHADOWGATHER(tex, coord);
      #else
          vec4 gather = vec4(0.0);
          gather.x = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(0.0, 1.0));
          gather.y = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(1.0, 1.0));
          gather.z = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(1.0, 0.0));
          gather.w = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(0.0, 0.0));
      #endif

     vec2 f = fract( projCoord.xy * shadowMapSize + 0.5);
     vec2 mx = mix( gather.wx, gather.zy, f.x);
     return mix( mx.x, mx.y, f.y );
  }



  // SHADOWMAPSPERDIRECTIONALLIGHT (Point lights have always six shadows and spot lights always one, Directional ligths do get always 4 for now)
    #define MAXTOTALSHADOWLIGHTS MAXDIRECTIONALSHADOWLIGHTS + MAXPOINTSHADOWLIGHTS + MAXSPOTSHADOWLIGHTS
    #define SHADOWMAPSPERDIRECTIONALLIGHT 4
    #define DIRECTIONALSHADOWMAPS MAXDIRECTIONALSHADOWLIGHTS * SHADOWMAPSPERDIRECTIONALLIGHT
    #define POINTSHADOWMAPS MAXPOINTSHADOWLIGHTS * 6
    #define SPOTSHADOWMAPS MAXSPOTSHADOWLIGHTS
  // #ifdef TOTALSHADOWMAPS
  //   in vec4[TOTALSHADOWMAPS] shadowProjCoord;
  // #endif

  #if (MAXDIRECTIONALSHADOWLIGHTS > 0)
      float getDirectionalLightShadows(in vec4 splits,in float shadowPosition,
                                      in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3, in float layer){    
          float shadow = 1.0;   
          if(shadowPosition < splits.x){
              shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord0, 0.0 + layer, DIRECTIONALSHADOWMAP_SIZE);   
          }else if( shadowPosition <  splits.y){
              shadowBorderScale = 0.5;
              shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord1, 1.0 + layer, DIRECTIONALSHADOWMAP_SIZE);
          }else if( shadowPosition <  splits.z){
              shadowBorderScale = 0.25;
              shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord2, 2.0 + layer, DIRECTIONALSHADOWMAP_SIZE); 
          }else if( shadowPosition <  splits.w){
              shadowBorderScale = 0.125;
              shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord3, 3.0 + layer, DIRECTIONALSHADOWMAP_SIZE); 
          }
          return shadow;
      }
  #endif

  #if (MAXPOINTSHADOWLIGHTS > 0)
      float getPointLightShadows(in vec3 lightVec,
                             in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3,in vec4 projCoord4,in vec4 projCoord5, in float layer){
          float shadow = 1.0;
          vec3 absv= abs(lightVec);
          float maxComp = max(absv.x,max(absv.y,absv.z));
          if(maxComp == absv.y){
             if(lightVec.y < 0.0){
                 shadow = GETSHADOW(m_PointShadowMaps, projCoord0, 0.0 + layer, POINTSHADOWMAP_SIZE);
             }else{
                 shadow = GETSHADOW(m_PointShadowMaps, projCoord1, 1.0 + layer, POINTSHADOWMAP_SIZE);
             }
          }else if(maxComp == absv.z){
             if(lightVec.z < 0.0){
                 shadow = GETSHADOW(m_PointShadowMaps, projCoord2, 2.0 + layer, POINTSHADOWMAP_SIZE);
             }else{
                 shadow = GETSHADOW(m_PointShadowMaps, projCoord3, 3.0 + layer, POINTSHADOWMAP_SIZE);
             }
          }else if(maxComp == absv.x){
             if(lightVec.x < 0.0){
                 shadow = GETSHADOW(m_PointShadowMaps, projCoord4, 4.0 + layer, POINTSHADOWMAP_SIZE);
             }else{
                 shadow = GETSHADOW(m_PointShadowMaps, projCoord5, 5.0 + layer, POINTSHADOWMAP_SIZE);
             }
          }  
          return shadow;
      }
  #endif
#endif
// int m_ActiveDirectionalShadows
// int m_ActivePointShadows
// int m_ActiveSpotShadows
///// int m_ShadowCounter (to allow more shadows than a single render pass?)
// shadowProjCoord
// m_splits[MAXDIRECTIONALSHADOWLIGHTS]
// m_lightPos
// m_worldPos


void main(){
  #ifdef TOTALSHADOWMAPS
    int shadowCounter = 0;
    int shadowMapCounter = 0;

    for (int i = 0; i < LIGHTS; i++){
        shadowIntensity[i] = 1.0;
    }
    #if (MAXDIRECTIONALSHADOWLIGHTS > 0)
        for (int i = 0; i < MAXDIRECTIONALSHADOWLIGHTS; i++){
            // skipping inactive lights should be a single instruction (dynamic branching is the same for every pixel)
            if (i < ActiveDirectionalShadows){
                    if (i >= startIndex){
                        shadowIntensity[shadowCounter] = getDirectionalLightShadows(splits[i], shadowPosition,
                                        shadowProjCoord[shadowMapCounter], shadowProjCoord[shadowMapCounter + 1],
                                        shadowProjCoord[shadowMapCounter + 2], shadowProjCoord[shadowMapCounter + 3],
                                        i*SHADOWMAPSPERDIRECTIONALLIGHT);
                        shadowCounter++;
                    }
                    shadowMapCounter += SHADOWMAPSPERDIRECTIONALLIGHT;
            }
        }
    #endif
    #if (MAXPOINTSHADOWLIGHTS > 0)
        for (int i = 0; i < MAXPOINTSHADOWLIGHTS; i++){
            if (i < ActivePointShadows){
                    if (i >= startIndex - ActiveDirectionalShadows){
                        shadowIntensity[shadowCounter] = getPointLightShadows(worldPos.xyz - pointLightPosition[i].xyz,
                                    shadowProjCoord[shadowMapCounter], shadowProjCoord[shadowMapCounter + 1],
                                    shadowProjCoord[shadowMapCounter + 2], shadowProjCoord[shadowMapCounter + 3],
                                    shadowProjCoord[shadowMapCounter + 4], shadowProjCoord[shadowMapCounter + 5], i * 6);
                        shadowCounter++;
                    }
                    shadowMapCounter += 6;
            }
        }
    #endif
    #if (MAXSPOTSHADOWLIGHTS > 0)
        for (int i = 0; i < MAXSPOTSHADOWLIGHTS; i++){
            if (i < ActiveSpotShadows){
                    if (i >= startIndex - ActiveDirectionalShadows - ActivePointShadows){
                        shadowIntensity[shadowCounter] = GETSHADOW(m_SpotShadowMaps, shadowProjCoord[shadowMapCounter], i, SPOTSHADOWMAP_SIZE);
                        shadowCounter++;
                    }
                    shadowMapCounter++;
            }
        }
    #endif
  #endif
}