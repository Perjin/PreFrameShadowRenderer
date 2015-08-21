
void main(){
    tbnMat = mat3(normalize(vTangent.xyz) , normalize(vBinormal.xyz) , normalize(viewNormal.xyz));

    if (!gl_FrontFacing)
    {
        tbnMat[2] = -tbnMat[2];
    }

    viewDir = normalize(-viewPos.xyz * tbnMat);
    outNormal = normalize((inNormalMap.xyz * vec3(2.0,2.0,2.0) - vec3(1.0,1.0,1.0)));
}