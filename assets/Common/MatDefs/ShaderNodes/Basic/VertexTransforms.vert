void main(){
  #ifndef USEINSTANCING
    normal = (worldMatrix * vec4(inNormal,0.0)).xyz;
    worldPosition = (worldMatrix * vec4(modelPosition, 1.0));
    worldViewPosition = (worldViewMatrix * vec4(modelPosition, 1.0)).xyz;
    worldViewProjectionPosition = (worldViewProjectionMatrix * vec4(modelPosition, 1.0));

    #ifdef NORMALMAP
      tangent = normalMatrix * inTangent.xyz;
      binormal = cross(normal, tangent)* inTangent.w;   
    #endif

  #else
    // INSTANCING
  
    worldPosition = (mat4(vec4(inInstanceData[0].xyz, 0.0), 
                              vec4(inInstanceData[1].xyz, 0.0), 
                              vec4(inInstanceData[2].xyz, 0.0), 
                              vec4(inInstanceData[3].xyz, 1.0)) * vec4(modelPosition, 1.0));
    vec4 quat = vec4(inInstanceData[0].w, inInstanceData[1].w, inInstanceData[2].w, inInstanceData[3].w);
    normal = (viewMatrix * vec4(inNormal + vec3(2.0) * cross(cross(inNormal, quat.xyz) + vec3(quat.w) * inNormal, quat.xyz),0.0)).xyz;
    worldViewPosition = (viewMatrix * worldPosition).xyz;
    worldViewProjectionPosition = viewProjectionMatrix * worldPosition;

    #ifdef NORMALMAP
      tangent.xyz =(viewMatrix * vec4(inTangent.xyz + vec3(2.0) * cross(cross(inTangent.xyz, quat.xyz) + vec3(quat.w) * inTangent.xyz, quat.xyz),0.0)).xyz;
      binormal = cross(normal, tangent.xyz)* inTangent.w;
    #endif
  #endif
}