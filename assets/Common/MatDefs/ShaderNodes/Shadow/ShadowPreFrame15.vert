#if (NUMBEROFSHADOWS == 6)
  #ifndef POINTLIGHT
    #define POINTLIGHT
  #endif
#endif
#if (NUMBEROFSHADOWS == 4)
  #ifndef PSSM
    #define PSSM
  #endif
#endif
#if (NUMBEROFSHADOWS == 1)
  #ifndef SPOTLIGHT
    #define SPOTLIGHT
  #endif
#endif

const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);
out vec4[6] outVertexProjCoord;
void main(){
   // for (int i = 0; NUMBEROFSHADOWS < 1; i++){
   //   outVertexProjCoord[i] = biasMat * LightViewProjectionMatrix[i] * worldPos;
  //  }


    outVertexProjCoord[0] = biasMat * LightViewProjectionMatrix0 * worldPos;
    #ifndef SPOTLIGHT
      outVertexProjCoord[1] = biasMat * LightViewProjectionMatrix1 * worldPos;
      outVertexProjCoord[2] = biasMat * LightViewProjectionMatrix2 * worldPos;
      outVertexProjCoord[3] = biasMat * LightViewProjectionMatrix3 * worldPos;
      #ifndef PSSM
      outVertexProjCoord[4] = biasMat * LightViewProjectionMatrix4 * worldPos;
      outVertexProjCoord[5] = biasMat * LightViewProjectionMatrix5 * worldPos;
      #endif
    #endif

    // populate the light view matrices array and convert vertex to light viewProj space
   // projCoord0 = biasMat * LightViewProjectionMatrix0 * worldPos;
   // projCoord1 = biasMat * LightViewProjectionMatrix1 * worldPos;
   // projCoord2 = biasMat * LightViewProjectionMatrix2 * worldPos;
   // projCoord3 = biasMat * LightViewProjectionMatrix3 * worldPos;
   //     projCoord4 = biasMat * LightViewProjectionMatrix4 * worldPos;
   //     projCoord5 = biasMat * LightViewProjectionMatrix5 * worldPos;
   // #else        
    #ifdef SPOTLIGHT
            vec3 lightDir = worldPos.xyz - LightPos;
            outLightDot = dot(LightDir,lightDir);
    #endif

}