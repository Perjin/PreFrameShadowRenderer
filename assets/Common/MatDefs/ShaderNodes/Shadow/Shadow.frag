// Because gpu_shader5 is actually where those
// gather functions are declared to work on shadowmaps
#extension GL_ARB_gpu_shader5 : enable

#if (NUMBEROFPOINTSHADOWS)
  #ifndef POINTLIGHT
    #define POINTLIGHT
  #endif
#endif
#if (NUMBEROFDIRECTIONALSHADOWS)
  #ifndef PSSM
    #define PSSM
  #endif
#endif
#if (NUMBEROFSPOTSHADOWS)
  #ifndef SPOTLIGHT
    #define SPOTLIGHT
  #endif
#endif

    #define SHADOWMAP sampler2DArrayShadow
    #define SHADOWCOMPAREOFFSET(tex,coord,offset) textureOffset(tex, coord, offset)
    #define SHADOWCOMPARE(tex,coord) texture(tex, coord)
    #define SHADOWGATHER(tex,coord) textureGather(tex, coord.xyw, coord.z)


    #define GETSHADOW Shadow_DoBilinear_2x2
    #define KERNEL 1.0


const vec2 pixSize2 = vec2(1.0 / DIRECTIONALSHADOWMAP_SIZE);
float shadowBorderScale = 1.0;

float Shadow_BorderCheck(in vec2 coord){
    // Fastest, "hack" method (uses 4-5 instructions)
    vec4 t = vec4(coord.xy, 0.0, 1.0);
    t = step(t.wwxy, t.xyzz);
    return dot(t,t);  
}

float Shadow_Nearest(in SHADOWMAP tex, in vec4 projCoord){
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0){
        return 1.0;
    }
    return SHADOWCOMPARE(tex,projCoord);
}


float Shadow_DoBilinear_2x2(in SHADOWMAP tex, in vec4 projCoord, in float layer){
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

   vec2 f = fract( projCoord.xy * DIRECTIONALSHADOWMAP_SIZE + 0.5);
   vec2 mx = mix( gather.wx, gather.zy, f.x);
   return mix( mx.x, mx.y, f.y );
}

#ifdef POINTLIGHT       
    float getPointLightShadows(in vec4 worldPos,in vec3 lightPos,
                           in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3,in vec4 projCoord4,in vec4 projCoord5, in float layer){
        float shadow = 1.0;
        vec3 vect = worldPos.xyz - lightPos;
        vec3 absv= abs(vect);
        float maxComp = max(absv.x,max(absv.y,absv.z));
        if(maxComp == absv.y){
           if(vect.y < 0.0){
               shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord0, 0.0 + layer*6.0);
           }else{
               shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord1, 1.0 + layer*6.0);
           }
        }else if(maxComp == absv.z){
           if(vect.z < 0.0){
               shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord2, 2.0 + layer*6.0);
           }else{
               shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord3, 3.0 + layer*6.0);
           }
        }else if(maxComp == absv.x){
           if(vect.x < 0.0){
               shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord4, 4.0 + layer*6.0);
           }else{
               shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord5, 5.0 + layer*6.0);
           }
        }  
        return shadow;
    }
 #endif
 #ifdef PSSM
    float getDirectionalLightShadows(in vec4 splits,in float shadowPosition,
                                    in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3, in float layer){    
        float shadow = 1.0;   
        if(shadowPosition < splits.x){
            shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord0, 0.0 + layer*4.0);   
        }else if( shadowPosition <  splits.y){
            shadowBorderScale = 0.5;
            shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord1, 1.0 + layer*4.0);
        }else if( shadowPosition <  splits.z){
            shadowBorderScale = 0.25;
            shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord2, 2.0 + layer*4.0); 
        }else if( shadowPosition <  splits.w){
            shadowBorderScale = 0.125;
            shadow = GETSHADOW(m_DirectionalShadowMaps, projCoord3, 3.0 + layer*4.0); 
        }
        return shadow;
    }
 #endif
#ifdef SPOTLIGHT
    float getSpotLightShadows(in  vec4 projCoord){
        return GETSHADOW(m_ShadowMap,projCoord,0.0);
    }
#endif

#ifdef NUMBEROFDIRECTIONALSHADOWS
  in vec4[NUMBEROFDIRECTIONALSHADOWS] directionalVertexProjCoord;
#endif
#ifdef NUMBEROFPOINTSHADOWS
  in vec4[NUMBEROFPOINTSHADOWS] pointVertexProjCoord;
#endif
#ifdef NUMBEROFSPOTSHADOWS
  in vec4[NUMBEROFSPOTSHADOWS] spotVertexProjCoord;
#endif


void main(){
    #ifdef FADE
      float shadowFade = (shadowPosition - FadeInfo.x) * FadeInfo.y);
    #endif

    int shadowIntensityCounter = 0;

    for (int i = 0; i < NUMBEROFDIRECTIONALSHADOWLIGHTS;i++){
      shadowIntensity[shadowIntensityCounter] = getDirectionalLightShadows(splits[i], shadowPosition,
                             directionalVertexProjCoord[i*4], directionalVertexProjCoord[i*4+1], directionalVertexProjCoord[i*4+2], directionalVertexProjCoord[i*4+3], i);
      #ifdef FADE
        shadowIntensity[shadowIntensityCounter] = max(0.0,mix(shadowIntensity[shadowIntensityCounter],1.0,shadowFade);    
      #endif
      shadowIntensityCounter++;
    }
    for (int i = 0; i < NUMBEROFPOINTSHADOWLIGHTS;i++){
      shadowIntensity[shadowIntensityCounter] = getPointLightShadows(worldPos, shadowPosition,
                             pointVertexProjCoord[i*6], pointVertexProjCoord[i*6+1], pointVertexProjCoord[i*6+2], pointVertexProjCoord[i*6+3], pointVertexProjCoord[i*6+4], pointVertexProjCoord[i*6+5], i);
      #ifdef FADE
        shadowIntensity[shadowIntensityCounter] = max(0.0,mix(shadowIntensity[shadowIntensityCounter],1.0,shadowFade);    
      #endif
      shadowIntensityCounter++;
    }
    for (int i = 0; i < NUMBEROFSPOTSHADOWLIGHTS;i++){
      shadowIntensity[shadowIntensityCounter] = GETSHADOW(m_ShadowMap,SpotVertexProjCoord[i], i);
      #ifdef FADE
        shadowIntensity[shadowIntensityCounter] = max(0.0,mix(shadowIntensity[shadowIntensityCounter],1.0,shadowFade);    
      #endif
      shadowIntensityCounter++;
    }
}
