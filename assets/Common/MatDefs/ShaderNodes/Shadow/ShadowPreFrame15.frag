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

float getDirectionalLightShadows(in vec4 splits,in float shadowPosition,
                                    in sampler2DShadow shadowMap0,in sampler2DShadow shadowMap1,in sampler2DShadow shadowMap2,in sampler2DShadow shadowMap3,
                                    in vec4 projCoord0,in vec4 projCoord1,in vec4 projCoord2,in vec4 projCoord3){    
        float shadow = 1.0;   
        if(shadowPosition < splits.x){
            shadow = Shadow_Nearest(shadowMap0, projCoord0 );   
        }else if( shadowPosition <  splits.y){
            shadow = Shadow_Nearest(shadowMap1, projCoord1);  
        }else if( shadowPosition <  splits.z){
            shadow = Shadow_Nearest(shadowMap2, projCoord2); 
        }else if( shadowPosition <  splits.w){
            shadow = Shadow_Nearest(shadowMap3, projCoord3); 
        }
        return shadow;
    }

void main(){

    shadowIntensity = 1.0;
   
    shadowIntensity = getDirectionalLightShadows(Splits, shadowPosition,
                   ShadowMap0,ShadowMap1,ShadowMap2,ShadowMap3,
                   projCoord0, projCoord1, projCoord2, projCoord3);
 
    shadowIntensity = max(0.0,mix(shadowIntensity,1.0,(shadowPosition - FadeInfo.x) * FadeInfo.y));    
   
}
