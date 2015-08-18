#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
uniform vec4 g_LightData[NB_LIGHTS];

vec3 computeSingleLight(in vec4 lightColor, in vec4 lightData1, in vec4 lightData2, in vec3 viewPos, in vec3 viewDir, in vec3 viewNormal){
            vec4 lightDir;
            vec3 lightVec;    
            lightComputeDir(viewPos, lightColor.w, lightData1, lightDir,lightVec);

            float spotFallOff = 1.0;
            #if __VERSION__ >= 110
                // allow use of control flow
            if(lightColor.w > 1.0){
            #endif
                spotFallOff =  computeSpotFalloff(lightData2, lightVec);
            #if __VERSION__ >= 110
            }
            #endif
            
            vec2 light = computeLighting(viewNormal, viewDir, lightDir.xyz, lightDir.w * spotFallOff , 50.0);
          
            return lightColor.xyz* (light.x + light.y) ;
}

void main(){
  singlePassOut = vec4(0.0,0.0,0.0,1.0);
  vec3 viewDir = normalize(-viewPos.xyz);

  float shadowValue;
  vec4 colorData;
  for( int i = 0;i < NB_LIGHTS; i+=3){
    colorData = g_LightData[i];
    if (i == ShadowLight){
      shadowValue = shadow;
    } else {
      shadowValue = 1.0;
    }
    singlePassOut.xyz += computeSingleLight(colorData, g_LightData[i+1], g_LightData[i+2], viewPos, viewDir, viewNormal)*shadowValue;
  }
}
