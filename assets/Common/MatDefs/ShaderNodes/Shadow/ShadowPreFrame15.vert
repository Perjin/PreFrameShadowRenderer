const mat4 biasMat = mat4(0.5, 0.0, 0.0, 0.0,
                          0.0, 0.5, 0.0, 0.0,
                          0.0, 0.0, 0.5, 0.0,
                          0.5, 0.5, 0.5, 1.0);


void main(){

    shadowPosition = gl_Position.z;

    // populate the light view matrices array and convert vertex to light viewProj space
    projCoord0 = biasMat * LightViewProjectionMatrix0 * worldPos;
    projCoord1 = biasMat * LightViewProjectionMatrix1 * worldPos;
    projCoord2 = biasMat * LightViewProjectionMatrix2 * worldPos;
    projCoord3 = biasMat * LightViewProjectionMatrix3 * worldPos;
    
}