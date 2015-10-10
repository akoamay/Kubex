#version 330 core
uniform sampler2D tiles;
uniform sampler2D shadowMap;
uniform float alpha;
uniform float daylightAmount;
in vec3 Location;
in vec3 sunLocation;
in vec2 Properties;
in vec2 Brightness;
const vec4 fogcolor = vec4(0.7, 0.9, 1.0, 1.0);
const float fogdensity = .00001;

layout(location = 0) out vec4 outcolor;
layout(location = 1) out vec3 outPosition;
layout(location = 2) out vec3 outNormalsAndLight;

float adjFract(float val)
{
return (fract(val)*0.999f) + 0.0005;
}
void main(){
vec4 outColor;
	if(Properties.y<1.5f)
	{
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.z)+Properties.x)/16,
				((1-adjFract(Location.y))+floor(((Properties.x)+0.001)/16))/16
				));
		/*outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x)+Properties.x)/16,
				(adjFract(Location.z)+floor(((Properties.x)+0.001)/16))/16
				));//**vec4(1.0,1.0,1.0,alpha);vec4(adjFract(Location.x/10),adjFract(Location.y/10),adjFract(Location.z/10),1.0);
		//outColor=vec4(0.5f,1.0f*(Location.y/32),0.5f,alpha);*/
	}
	else if(Properties.y<3.5f)
	{
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x)+Properties.x)/16,
				(adjFract(Location.z)+floor(((Properties.x)+0.001)/16))/16
				));
	}
	else{
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x)+Properties.x)/16,
				((1-adjFract(Location.y))+floor(((Properties.x)+0.001)/16))/16
				));
	}
	if(outColor.w<0.1) discard;
	outColor=outColor*vec4(1,1,1,alpha);
	
	float bias = 0.005;
	vec2 shadowLoc=vec2((sunLocation.x+1)/2,(sunLocation.y+1)/2);
	//float shadow=texture(shadowMap,vec3(shadowLoc,(sunLocation.z+1)/2 -bias));
	vec4 shadow=texture(shadowMap,vec2(shadowLoc));
	if(shadow.z < (sunLocation.z+1)/2 -bias) outColor=outColor*0.5f;
	
	float daylightBrightness=Brightness.x*daylightAmount;
	float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	outColor=outColor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
	
	float z = gl_FragCoord.z / gl_FragCoord.w;
 	float fog = clamp(exp(-fogdensity * z * z), 0.2, 1);
  	outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outColor, fog);
  	outPosition=(shadow.xyz - 0.5)*2;
  	//outNormalsAndLight=vec3(Properties.y,Brightness.x,Brightness.y);
  	if(sunLocation.x>1||sunLocation.x<-1||sunLocation.y>1||sunLocation.y<-1||sunLocation.z>1||sunLocation.z<-1) outNormalsAndLight=vec3(0,0,0);
  	else outNormalsAndLight=(vec3((sunLocation.x+1)/2,(sunLocation.y+1)/2,(sunLocation.z+1)/2) - 0.5)*2;
  	//gl_FragColor = gl_FragColor - vec4(0.35,0.35,0.35,0.35);
  	
  	
  	
  	//gl_FragColor =outColor;
}