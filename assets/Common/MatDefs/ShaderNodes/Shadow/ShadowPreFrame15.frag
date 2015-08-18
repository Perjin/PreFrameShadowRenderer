// Because gpu_shader5 is actually where those
// gather functions are declared to work on shadowmaps
#extension GL_ARB_gpu_shader5 : enable

#ifdef HARDWARE_SHADOWS
    #define SHADOWMAP sampler2DShadow
    #define SHADOWCOMPAREOFFSET(tex,coord,offset) textureProjOffset(tex, coord, offset)
    #define SHADOWCOMPARE(tex,coord) textureProj(tex, coord)
    #define SHADOWGATHER(tex,coord) textureGather(tex, coord.xy, coord.z)
#else
    #define SHADOWMAP sampler2D
    #define SHADOWCOMPAREOFFSET(tex,coord,offset) step(coord.z, textureProjOffset(tex, coord, offset).r)
    #define SHADOWCOMPARE(tex,coord) step(coord.z, textureProj(tex, coord).r) 
    #define SHADOWGATHER(tex,coord) step(coord.z, textureGather(tex, coord.xy))
#endif


#if FILTER_MODE == 0
    #define GETSHADOW Shadow_Nearest
    #define KERNEL 1.0
#elif FILTER_MODE == 1
    #define GETSHADOW Shadow_DoBilinear_2x2
    #define KERNEL 1.0
#elif FILTER_MODE == 2
    #define GETSHADOW Shadow_DoDither_2x2
    #define KERNEL 1.0
#elif FILTER_MODE == 3
    #define GETSHADOW Shadow_DoPCF
    #define KERNEL 4.0
#elif FILTER_MODE == 4
    #define GETSHADOW Shadow_DoPCFPoisson
    #define KERNEL 4.0
#elif FILTER_MODE == 5
    #define GETSHADOW Shadow_DoPCF
    #define KERNEL 8.0
#endif


const vec2 pixSize2 = vec2(1.0 / SHADOWMAP_SIZE);
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

float Shadow_DoDither_2x2(in SHADOWMAP tex, in vec4 projCoord){
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;

    vec2 pixSize = pixSize2 * shadowBorderScale;
    
    float shadow = 0.0;
    ivec2 o = ivec2(mod(floor(gl_FragCoord.xy), 2.0));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2(-1.5, 1.5)+o), projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2( 0.5, 1.5)+o), projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2(-1.5, -0.5)+o), projCoord.zw));
    shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy+pixSize*(vec2( 0.5, -0.5)+o), projCoord.zw));
    shadow *= 0.25;
    return shadow;
}

float Shadow_DoBilinear_2x2(in SHADOWMAP tex, in vec4 projCoord){
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;
  
    #ifdef GL_ARB_gpu_shader5
        vec4 coord = vec4(projCoord.xyz / projCoord.www,0.0);
        vec4 gather = SHADOWGATHER(tex, coord);
    #else
        vec4 gather = vec4(0.0);
        gather.x = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(0, 1));
        gather.y = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(1, 1));
        gather.z = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(1, 0));
        gather.w = SHADOWCOMPAREOFFSET(tex, projCoord, ivec2(0, 0));
    #endif

   vec2 f = fract( projCoord.xy * SHADOWMAP_SIZE + 0.5);
   vec2 mx = mix( gather.wx, gather.zy, f.x);
   return mix( mx.x, mx.y, f.y );
}
#ifdef PCFEDGE
float Shadow_DoPCF(in SHADOWMAP tex, in vec4 projCoord){    

    vec2 pixSize = pixSize2 * shadowBorderScale;  
    float shadow = 0.0;
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;

    float bound = KERNEL * 0.5 - 0.5;
    bound *= PCFEDGE;
    for (float y = -bound; y <= bound; y += PCFEDGE){
        for (float x = -bound; x <= bound; x += PCFEDGE){
            vec4 coord = vec4(projCoord.xy + vec2(x,y) * pixSize, projCoord.zw);
            shadow += SHADOWCOMPARE(tex, coord);
        }
    }

    shadow = shadow / (KERNEL * KERNEL);
    return shadow;
}


//12 tap poisson disk
    const vec2 poissonDisk0 =  vec2(-0.1711046, -0.425016);
    const vec2 poissonDisk1 =  vec2(-0.7829809, 0.2162201);
    const vec2 poissonDisk2 =  vec2(-0.2380269, -0.8835521);
    const vec2 poissonDisk3 =  vec2(0.4198045, 0.1687819);
    const vec2 poissonDisk4 =  vec2(-0.684418, -0.3186957);
    const vec2 poissonDisk5 =  vec2(0.6026866, -0.2587841);
    const vec2 poissonDisk6 =  vec2(-0.2412762, 0.3913516);
    const vec2 poissonDisk7 =  vec2(0.4720655, -0.7664126);
    const vec2 poissonDisk8 =  vec2(0.9571564, 0.2680693);
    const vec2 poissonDisk9 =  vec2(-0.5238616, 0.802707);
    const vec2 poissonDisk10 = vec2(0.5653144, 0.60262);
    const vec2 poissonDisk11 = vec2(0.0123658, 0.8627419);


float Shadow_DoPCFPoisson(in SHADOWMAP tex, in vec4 projCoord){  

    float shadow = 0.0;
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0){
        return 1.0;
    }
     
    vec2 texelSize = pixSize2 * 4.0 * PCFEDGE * shadowBorderScale;        
    #ifdef GL_ARB_gpu_shader5
      vec4 coord = vec4(projCoord.xyz / projCoord.www,0.0);
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk0 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk1 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk2 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk3 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk4 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk5 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk6 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk7 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk8 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk9 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk10 * texelSize, projCoord.zw));
      shadow += Shadow_DoBilinear_2x2(tex, vec4(projCoord.xy + poissonDisk11 * texelSize, projCoord.zw));
    #else
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk0 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk1 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk2 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk3 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk4 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk5 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk6 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk7 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk8 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk9 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk10 * texelSize, projCoord.zw));
      shadow += SHADOWCOMPARE(tex, vec4(projCoord.xy + poissonDisk11 * texelSize, projCoord.zw));
    #endif
    //this is divided by 12
    return shadow * 0.08333333333;
}
#endif

#ifdef POINTLIGHT       
    float getPointLightShadows(in vec4 worldPos,in vec3 lightPos,
                           in SHADOWMAP shadowMap0,in SHADOWMAP shadowMap1,in SHADOWMAP shadowMap2,in SHADOWMAP shadowMap3,in SHADOWMAP shadowMap4,in SHADOWMAP shadowMap5,
                           in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3,in vec4 projCoord4,in vec4 projCoord5){
        float shadow = 1.0;
        vec3 vect = worldPos.xyz - lightPos;
        vec3 absv= abs(vect);
        float maxComp = max(absv.x,max(absv.y,absv.z));
        if(maxComp == absv.y){
           if(vect.y < 0.0){
               shadow = GETSHADOW(shadowMap0, projCoord0 / projCoord0.w);
           }else{
               shadow = GETSHADOW(shadowMap1, projCoord1 / projCoord1.w);
           }
        }else if(maxComp == absv.z){
           if(vect.z < 0.0){
               shadow = GETSHADOW(shadowMap2, projCoord2 / projCoord2.w);
           }else{
               shadow = GETSHADOW(shadowMap3, projCoord3 / projCoord3.w);
           }
        }else if(maxComp == absv.x){
           if(vect.x < 0.0){
               shadow = GETSHADOW(shadowMap4, projCoord4 / projCoord4.w);
           }else{
               shadow = GETSHADOW(shadowMap5, projCoord5 / projCoord5.w);
           }
        }  
        return shadow;
    }
#else
 #ifdef PSSM
    float getDirectionalLightShadows(in vec4 splits,in float shadowPosition,
                                    in SHADOWMAP shadowMap0,in SHADOWMAP shadowMap1,in SHADOWMAP shadowMap2,in SHADOWMAP shadowMap3,
                                    in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3){    
        float shadow = 1.0;   
        if(shadowPosition < splits.x){
            shadow = GETSHADOW(shadowMap0, projCoord0 );   
        }else if( shadowPosition <  splits.y){
            shadowBorderScale = 0.5;
            shadow = GETSHADOW(shadowMap1, projCoord1);  
        }else if( shadowPosition <  splits.z){
            shadowBorderScale = 0.25;
            shadow = GETSHADOW(shadowMap2, projCoord2); 
        }else if( shadowPosition <  splits.w){
            shadowBorderScale = 0.125;
            shadow = GETSHADOW(shadowMap3, projCoord3); 
        }
        return shadow;
    }
 #else
    float getSpotLightShadows(in SHADOWMAP shadowMap,in  vec4 projCoord){
        float shadow = 1.0;     
        projCoord /= projCoord.w;
        shadow = GETSHADOW(shadowMap,projCoord);
        
        //a small falloff to make the shadow blend nicely into the not lighten
        //we translate the texture coordinate value to a -1,1 range so the length 
        //of the texture coordinate vector is actually the radius of the lighten area on the ground
        projCoord = projCoord * 2.0 - 1.0;
        float fallOff = ( length(projCoord.xy) - 0.9 ) / 0.1;
        return mix(shadow,1.0,clamp(fallOff,0.0,1.0));

    }
 #endif
#endif


















void main(){
   // shadowIntensity = 1.0;
    #ifdef POINTLIGHT         
            shadowIntensity = getPointLightShadows(worldPos, LightPos,
                           ShadowMap0,ShadowMap1,ShadowMap2,ShadowMap3,ShadowMap4,ShadowMap5,
                           projCoord0, projCoord1, projCoord2, projCoord3, projCoord4, projCoord5);
    #else
       #ifdef PSSM
            shadowIntensity = getDirectionalLightShadows(Splits, shadowPosition,
                           ShadowMap0,ShadowMap1,ShadowMap2,ShadowMap3,
                           projCoord0, projCoord1, projCoord2, projCoord3);
       #else
            //spotlight
            if(inLightDot < 0){
                shadowIntensity = 1.0;
            } else {
              shadowIntensity = getSpotLightShadows(ShadowMap0,projCoord0);
            }
       #endif
    #endif   
    #ifdef FADE
            shadowIntensity = max(0.0,mix(shadowIntensity,1.0,(shadowPosition - FadeInfo.x) * FadeInfo.y));    
    #endif
}
