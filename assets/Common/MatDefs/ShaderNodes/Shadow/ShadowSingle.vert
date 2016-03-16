const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);


// #ifdef TOTALSHADOWMAPS
//   out vec4[TOTALSHADOWMAPS] shadowVertexProjCoord;
// #endif

// input ShadowLightViewProjectionMatrix
// input worldPos

void main(){
    #ifdef TOTALSHADOWMAPS
      for( int i = 0; i < TOTALSHADOWMAPS; i++){
          shadowVertexProjCoord[i] = biasMat * ShadowLightViewProjectionMatrix[i] * worldPos;
      }
    #endif
}