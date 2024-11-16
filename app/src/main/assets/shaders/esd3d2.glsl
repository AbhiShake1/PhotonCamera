precision highp float;
precision highp sampler2D;
uniform sampler2D InputBuffer;
uniform sampler2D GradBuffer;
uniform sampler2D NoiseMap;
uniform ivec2 size;
uniform vec2 mapsize;
uniform int yOffset;
uniform float noiseS;
uniform float noiseO;
out vec4 Output;

#define SIGMA 10.0
#define BSIGMA 0.1
#define KERNELSIZE 3.5
#define MSIZE 15
#define KSIZE (MSIZE-1)/2
#define TRANSPOSE 1
#define INSIZE 1,1
#define NRcancell (0.90)
#define NRshift (+0.6)
#define maxNR (7.)
#define minNR (0.2)
#define NOISES 0.0
#define NOISEO 0.0
#define INTENSE 1.0
#define PI 3.1415926535897932384626433832795

float normpdf(in float x, in float sigma)
{
return 0.39894*exp(-0.5*x*x/(sigma*sigma))/sigma;
}
float normpdf3(in vec3 v, in float sigma)
{
return 0.39894*exp(-0.5*dot(v,v)/(sigma*sigma))/sigma;
}
float normpdf2(in vec2 v, in float sigma)
{
return 0.39894*exp(-0.5*dot(v,v)/(sigma*sigma))/sigma;
}

float lum(in vec4 color) {
    return length(color.xyz);
}

float atan2(in float y, in float x) {
bool s = (abs(x) > abs(y));
return mix(PI/2.0 - atan(x,y+0.00001), atan(y,x+0.00001), s);
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    xy+=ivec2(0,yOffset);
    vec3 cin = vec3(texelFetch(InputBuffer, xy, 0).rgb);
    float noisefactor = dot(cin,vec3(0.15,0.7,0.15));
    vec3 final_colour = vec3(0.0);
    float sigX = 2.5;
    float sigY = (noisefactor*noisefactor*NOISES + NOISEO + 0.0000001);
    float Z = 0.01f;
    final_colour += cin*Z;
    //sigY /= 25.0;
    // Use hybrid SNN filtering to denoise the image
    //vec3 cc[4];
    for (int i=0; i <= KSIZE; ++i)
    {
        for (int j=0; j <= KSIZE; ++j)
        {
            ivec2 pos = ivec2(i,j);
            ivec2 pos2 = ivec2(-i,-j);
            ivec2 pos3 = ivec2(i,-j);
            ivec2 pos4 = ivec2(-i,j);
            vec3 cc0 = vec3(texelFetch(InputBuffer, xy+pos, 0).rgb);
            vec3 cc1 = vec3(texelFetch(InputBuffer, xy+pos2, 0).rgb);
            vec3 cc2 = vec3(texelFetch(InputBuffer, xy+pos3, 0).rgb);
            vec3 cc3 = vec3(texelFetch(InputBuffer, xy+pos4, 0).rgb);
            // Compute the weights
            vec4 d = vec4(length(abs(cc0-cin)),length(abs(cc1-cin)),length(abs(cc2-cin)),length(abs(cc3-cin)));
            vec4 w = (1.0-d*d/(d*d + sigY));
            float wm = min(min(min(w[0],w[1]),w[2]),w[3])*1.0;
            w -= wm;
            float f1 = normpdf(float(i),KERNELSIZE)*normpdf(float(j),KERNELSIZE);
            final_colour += f1*mat4x3(cc0,cc1,cc2,cc3)*w;
            Z += dot(vec4(f1),w);
        }
    }

    //if (Z <= 0.002f) {
    //    Output = vec4(cin,1.0);
    //} else {
    Output = vec4(clamp(final_colour/Z,0.0,1.0),1.0);
    //}
}
