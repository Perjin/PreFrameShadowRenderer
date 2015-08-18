void main(){
  #ifdef VECTOR2_1
  dotProduct = dot(vector2_0, vector2_1);
  #endif
  #ifdef VECTOR3_1
  dotProduct = dot(vector3_0, vector3_1);
  #endif
  #ifdef VECTOR4_1
  dotProduct = dot(vector4_0, vector4_1);
  #endif
}