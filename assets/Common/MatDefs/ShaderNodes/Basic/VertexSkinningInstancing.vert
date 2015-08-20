#import "Common/ShaderLib/InstancingNode.glsllib"
#import "Common/ShaderLib/SkinningNode.glsllib"


#ifdef USE_REFLECTION
    /**
     * Input:
     * attribute inPosition
     * attribute inNormal
     * uniform g_WorldMatrix
     * uniform g_CameraPosition
     *
     * Output:
     * varying refVec
     */
    void computeRef(in vec4 modelSpacePos){
        vec3 worldPos = TransformWorld(modelSpacePos).xyz;

        vec3 I = normalize( CameraPosition - worldPos  ).xyz;
        vec3 N = normalize( TransformWorld(vec4(inNormal, 0.0)).xyz );

        refVec.xyz = reflect(I, N);
        refVec.w   = FresnelParams.x + FresnelParams.y * pow(1.0 + dot(I, N), FresnelParams.z);
    }
#endif

void main(){
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   vec3 modelSpaceNorm = inNormal;
   
   #if  defined(NORMALMAP)
        vec3 modelSpaceTan  = inTangent.xyz;
   #endif

   #ifdef NUM_BONES
        #if defined(NORMALMAP)
        Skinning_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
        #else
        Skinning_Compute(modelSpacePos, modelSpaceNorm);
        #endif
   #endif

   worldViewProjectionPosition = TransformWorldViewProjection(modelSpacePos);
   texCoord = inTexCoord;
   #ifdef SEPARATE_TEXCOORD
      texCoord2 = inTexCoord2;
   #endif

   vec3 wvPosition = TransformWorldView(modelSpacePos).xyz;
   vec3 wvNormal  = normalize(TransformNormal(modelSpaceNorm));
   vec3 viewDir = normalize(-wvPosition);
  
       
    #if defined(NORMALMAP)
      vTangent = TransformNormal(modelSpaceTan);
      vBinormal = cross(wvNormal, vTangent)* inTangent.w;      
      vNormal = wvNormal;         
      vPos = wvPosition;
    #else
      vNormal = wvNormal;          
      vPos = wvPosition;
    #endif
   
    #ifdef VERTEX_COLOR             
      outVertexColor = inVertexColor;
    #endif
    
    #ifdef USE_REFLECTION
      computeRef(modelSpacePos);
    #endif 
}