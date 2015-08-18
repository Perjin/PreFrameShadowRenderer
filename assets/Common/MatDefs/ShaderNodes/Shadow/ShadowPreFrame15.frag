#extension GL_ARB_gpu_shader5 : enable
float ShadowMapSize;
float Shadow_BorderCheck(in vec2 coord){
    // Fastest, "hack" method (uses 4-5 instructions)
    vec4 t = vec4(coord.xy, 0.0, 1.0);
    t = step(t.wwxy, t.xyzz);
    return dot(t,t);  
}
float Shadow_Nearest(in sampler2DShadow tex, in vec4 projCoord){
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0){
        return 1.0;
    }
    return textureProj(tex, projCoord);
}

float Shadow_DoBilinear_2x2(in sampler2DShadow tex, in vec4 projCoord){
    float border = Shadow_BorderCheck(projCoord.xy);
    if (border > 0.0)
        return 1.0;

    vec3 coord = projCoord.xyz / projCoord.www;
    vec4 gather = textureGather(tex, coord.xy, coord.z);

    vec2 f = fract((projCoord.xy * (ShadowMapSize)) + 0.5);
    vec2 mx = mix( gather.wx, gather.zy, f.x);
    return mix( mx.x, mx.y, f.y);
}

float getDirectionalLightShadows(in vec4 splits,in float shadowPosition,
                                in sampler2DShadow shadowMap0,in sampler2DShadow shadowMap1,in sampler2DShadow shadowMap2,in sampler2DShadow shadowMap3,
                                in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3){    
    float shadow = 1.0;   
    if(shadowPosition < splits.x){
        shadow = Shadow_DoBilinear_2x2(shadowMap0, projCoord0);   
    }else if( shadowPosition <  splits.y){
        shadow = Shadow_DoBilinear_2x2(shadowMap1, projCoord1); 
    }else if( shadowPosition <  splits.z){
        shadow = Shadow_DoBilinear_2x2(shadowMap2, projCoord2); 
    }else if( shadowPosition <  splits.w){
        shadow = Shadow_DoBilinear_2x2(shadowMap3, projCoord3); 
    }
    return shadow;
}

float getPointLightShadows(in vec4 worldPos,in vec3 lightPos,
                       in sampler2DShadow shadowMap0,in sampler2DShadow shadowMap1,in sampler2DShadow shadowMap2,in sampler2DShadow shadowMap3,in sampler2DShadow shadowMap4,in sampler2DShadow shadowMap5,
                       in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3,in vec4 projCoord4,in vec4 projCoord5){
    float shadow = 1.0;
    vec3 vect = worldPos.xyz - lightPos;
    vec3 absv= abs(vect);
    float maxComp = max(absv.x,max(absv.y,absv.z));
    if(maxComp == absv.y){
       if(vect.y < 0.0){
           shadow = Shadow_DoBilinear_2x2(shadowMap0, projCoord0 / projCoord0.w);
       }else{
           shadow = Shadow_DoBilinear_2x2(shadowMap1, projCoord1 / projCoord1.w);
       }
    }else if(maxComp == absv.z){
       if(vect.z < 0.0){
           shadow = Shadow_DoBilinear_2x2(shadowMap2, projCoord2 / projCoord2.w);
       }else{
           shadow = Shadow_DoBilinear_2x2(shadowMap3, projCoord3 / projCoord3.w);
       }
    }else if(maxComp == absv.x){
       if(vect.x < 0.0){
           shadow = Shadow_DoBilinear_2x2(shadowMap4, projCoord4 / projCoord4.w);
       }else{
           shadow = Shadow_DoBilinear_2x2(shadowMap5, projCoord5 / projCoord5.w);
       }
    }  
    return shadow;
}
void main(){
    ShadowMapSize = shadowMapSize;
    #ifndef POINTLIGHT
    shadowIntensity = getDirectionalLightShadows(Splits, shadowPosition,
                   ShadowMap0,ShadowMap1,ShadowMap2,ShadowMap3,
                   projCoord0, projCoord1, projCoord2, projCoord3);
    #else
    shadowIntensity = getPointLightShadows(worldPos, LightPos,
                   ShadowMap0,ShadowMap1,ShadowMap2,ShadowMap3,ShadowMap4,ShadowMap5,
                   projCoord0, projCoord1, projCoord2, projCoord3, projCoord4, projCoord5);
    #endif
    shadowIntensity = max(0.0,mix(shadowIntensity,1.0,(shadowPosition - FadeInfo.x) * FadeInfo.y));    
   
}
