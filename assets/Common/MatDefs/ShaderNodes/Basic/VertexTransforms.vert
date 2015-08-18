void main(){
     normal = (worldViewMatrix * vec4(normal, 0.0)).xyz;
     #ifdef LIGHTVIEWPROJECTIONMATRIX0
     worldPosition = (worldMatrix * vec4(modelPosition, 1.0));
     #endif
     worldViewPosition = (worldViewMatrix * vec4(modelPosition, 1.0)).xyz;
     worldViewProjectionPosition = (worldViewProjectionMatrix * vec4(modelPosition, 1.0));
     #ifdef LIGHTVIEWPROJECTIONMATRIX0
     shadowPosition = worldViewProjectionPosition.z;
     #endif
}