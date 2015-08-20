
void main(){
  #if defined(NORMALMAP)
      tbnMat = mat3(normalize(vTangent.xyz) , normalize(vBinormal.xyz) , normalize(viewNormal.xyz));

      if (!gl_FrontFacing)
      {
          tbnMat[2] = -tbnMat[2];
      }

      viewDir = normalize(-viewPos.xyz * tbnMat);
  #else
      viewDir = normalize(-viewPos.xyz);
  #endif
 // vec2 newTexCoord;
  #if defined(NORMALMAP) 
    vec4 normalHeight = texture2D(NormalMap, texCoord);
    //Note the -2.0 and -1.0. We invert the green channel of the normal map, 
    //as it's complient with normal maps generated with blender.
    //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
    //for more explanation.
    outNormal = normalize((normalHeight.xyz * vec3(2.0,2.0,2.0) - vec3(1.0,1.0,1.0)));
  #else
    outNormal = normalize(viewNormal); 

    if (!gl_FrontFacing)
    {
      outNormal = -outNormal;
    }           
  #endif
}