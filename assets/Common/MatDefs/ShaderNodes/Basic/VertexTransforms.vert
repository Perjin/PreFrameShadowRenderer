void main(){
     normal = (worldViewMatrix * vec4(normal, 0.0)).xyz;
     worldPosition = (worldMatrix * vec4(modelPosition, 1.0));
     worldViewPosition = (worldViewMatrix * vec4(modelPosition, 1.0)).xyz;
     worldViewProjectionPosition = (worldViewProjectionMatrix * vec4(modelPosition, 1.0));
}