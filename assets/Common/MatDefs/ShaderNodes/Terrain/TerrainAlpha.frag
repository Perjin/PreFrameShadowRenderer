
void main(){
  vec4 alphaMap = texture(terrainAlpha, inUV * 0.1 + vec2(0.5));
  terrainDiffuse = (1.0 - alphaMap.r) * texture(diffuse0, inUV) + alphaMap.r * texture(diffuse1, inUV);
 // outDiffuse = vec4(0.0,0.0,0.0,1.0);
}
