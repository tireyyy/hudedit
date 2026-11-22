#version 330

// VoidSky procedural planet. One billboard quad per body; everything else is computed on the GPU.
// Parameters are packed into vertex attributes (no custom uniforms needed):
//   UV0.x = local.x(0..1) + 4*type + 64*quality      UV0.y = local.y(0..1) + 4*variant
//   Color.rg = 16-bit planet rotation phase          Color.ba = 16-bit cloud rotation phase

in vec2 vUV;
in vec4 vColor;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i + vec3(0, 0, 0)), hash(i + vec3(1, 0, 0)), f.x),
            mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y),
        mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x),
            mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float fbm(vec3 p, int oct) {
    float a = 0.5;
    float s = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= oct) break;
        s += a * noise(p);
        p = p * 2.03 + 17.1;
        a *= 0.5;
    }
    return s;
}

vec3 rotY(vec3 v, float a) {
    float c = cos(a), s = sin(a);
    return vec3(c * v.x + s * v.z, v.y, -s * v.x + c * v.z);
}

vec3 rotZ(vec3 v, float a) {
    float c = cos(a), s = sin(a);
    return vec3(c * v.x - s * v.y, s * v.x + c * v.y, v.z);
}


vec4 over(vec4 t, vec4 b) {
    float a = t.a + b.a * (1.0 - t.a);
    vec3 c = (t.rgb * t.a + b.rgb * b.a * (1.0 - t.a)) / max(a, 1e-4);
    return vec4(c, a);
}

vec3 discColor(float rho) {
    float t = clamp((rho - 0.6) / 1.4, 0.0, 1.0);
    vec3 c = mix(vec3(1.0, 0.92, 1.0), vec3(0.62, 0.25, 1.0), smoothstep(0.0, 0.4, t));
    return mix(c, vec3(0.10, 0.32, 1.0), smoothstep(0.4, 1.0, t));
}

// Black hole with a lensed, rotating accretion disc. q is in units where the quad spans [-2, 2].
vec4 blackHole(vec2 q, float ph, int oct) {
    float R = length(q);
    float ang = atan(q.y, q.x);
    float horizon = 0.45;
    float dopp = 1.0 - 0.45 * q.x / max(R, 1e-3);

    // outer purple/blue glow
    float glow = exp(-(R - horizon) * 2.6) * 0.35 * (1.0 - smoothstep(1.7, 2.0, R));
    vec4 res = vec4(mix(vec3(0.5, 0.2, 1.0), vec3(0.15, 0.35, 1.0), smoothstep(0.5, 1.6, R)), R > horizon ? glow : 0.0);

    // lensed image of the far side of the disc: arcs over the top and under the bottom of the hole
    vec2 ca = vec2(cos(ang + ph * 6.0), sin(ang + ph * 6.0));
    float lens = exp(-pow((R - 0.66) / 0.17, 2.0)) * pow(abs(q.y) / max(R, 1e-3), 0.6);
    float lsw = 0.6 + 0.4 * fbm(vec3(ca * 2.0, R * 6.0), oct);
    res = over(vec4(discColor(0.8 + (R - 0.5) * 2.0) * dopp, clamp(lens * lsw * 1.3, 0.0, 1.0)), res);

    // event horizon
    res = over(vec4(0.0, 0.0, 0.0, 1.0 - smoothstep(horizon - 0.01, horizon, R)), res);

    // near side of the tilted disc, in front of the hole
    float sinT = 0.2;
    vec2 rq = vec2(q.x, q.y / sinT);
    float rho = length(rq);
    if (q.y < 0.0 || R > horizon) {
        float a2 = atan(rq.y, rq.x) + ph * 6.0;
        float inDisc = smoothstep(0.75, 0.85, rho) * (1.0 - smoothstep(1.85, 1.95, rho));
        float swirl = 0.55 + 0.45 * fbm(vec3(cos(a2) * 3.0, sin(a2) * 3.0, rho * 5.0), oct);
        float bands = 0.7 + 0.3 * sin(rho * 30.0);
        float dA = clamp(inDisc * swirl * bands * dopp * 1.2, 0.0, 1.0);
        res = over(vec4(discColor(rho) * (0.8 + 0.4 * dopp), dA), res);
    }

    // photon ring
    float pring = exp(-pow((R - 0.47) / 0.02, 2.0));
    res = over(vec4(vec3(1.0, 0.9, 1.0), clamp(pring * 0.9, 0.0, 1.0)), res);
    return res;
}

void main() {
    float qual = floor(vUV.x / 64.0);
    float ux = vUV.x - 64.0 * qual;
    float type = floor(ux / 4.0);
    float lu = ux - 4.0 * type;
    float variant = floor(vUV.y / 4.0);
    float lv = vUV.y - 4.0 * variant;

    vec2 p = vec2(lu, lv) * 2.0 - 1.0;
    vec4 c = floor(vColor * 255.0 + 0.5);
    float th = (c.r * 256.0 + c.g) / 65535.0 * 6.2831853;
    float ph = (c.b * 256.0 + c.a) / 65535.0 * 6.2831853;

    int oct = 3 + int(qual);
    if (type > 3.5) {                // BLACK HOLE
        vec4 bhc = blackHole(p * 2.0, ph, oct);
        if (bhc.a <= 0.002) discard;
        fragColor = bhc;
        return;
    }
    bool saturn = type > 2.5;
    float pr = saturn ? 0.40 : 0.74;
    vec2 q = p / pr;
    float r = length(q);

    vec3 L = normalize(vec3(-0.55, 0.35, 0.75));
    vec3 atm = vec3(0.35, 0.6, 1.0);
    if (type > 0.5 && type < 1.5) atm = vec3(1.0, 0.65, 0.25);
    if (type > 1.5 && type < 2.5) atm = vec3(1.0, 0.55, 0.35);
    if (saturn) atm = vec3(0.9, 0.8, 0.55);

    vec4 res = vec4(0.0);

    if (r < 1.0) {
        vec3 n = vec3(q, sqrt(max(1.0 - r * r, 0.0)));
        vec3 nt = rotZ(n, 0.41);
        vec3 s = rotY(nt, th);
        vec3 cs = rotY(nt, ph);
        vec3 col;

        if (type < 0.5) {            // EARTH
            float h = fbm(s * 2.2 + 3.0, oct);
            float land = smoothstep(0.50, 0.53, h);
            vec3 ocean = mix(vec3(0.02, 0.09, 0.30), vec3(0.05, 0.25, 0.55), smoothstep(0.2, 0.5, h));
            vec3 ground = mix(vec3(0.18, 0.38, 0.12), vec3(0.52, 0.43, 0.26), smoothstep(0.55, 0.75, h));
            col = mix(ocean, ground, land);
            col = mix(col, vec3(0.95), smoothstep(0.78, 0.90, abs(s.y)));
            float spec = pow(max(dot(reflect(-L, n), vec3(0.0, 0.0, 1.0)), 0.0), 40.0) * 0.5 * (1.0 - land);
            col += spec;
            float cl = smoothstep(0.50, 0.75, fbm(cs * 3.5 + 9.0, oct));
            col = mix(col, vec3(1.0), cl * 0.85);
        } else if (type < 1.5) {     // VENUS
            float f = fbm(cs * vec3(2.0, 5.0, 2.0) + fbm(cs * 3.0, oct) * 1.5, oct);
            col = mix(vec3(0.85, 0.45, 0.12), vec3(1.0, 0.80, 0.45), f);
        } else if (type < 2.5) {     // MARS
            float h = fbm(s * 3.0, oct);
            col = mix(vec3(0.45, 0.17, 0.08), vec3(0.80, 0.40, 0.20), h);
            col = mix(col, vec3(0.25, 0.10, 0.06), smoothstep(0.55, 0.70, fbm(s * 1.5 + 5.0, oct)));
            col = mix(col, vec3(0.95), smoothstep(0.88, 0.95, abs(s.y)) * 0.8);
            col += vec3(0.9, 0.5, 0.3) * smoothstep(0.60, 0.80, fbm(cs * 2.5 + 4.0, oct)) * 0.15;
        } else {                     // SATURN
            float band = s.y * 14.0 + fbm(s * 2.0, oct) * 1.2;
            float b = 0.5 + 0.5 * sin(band);
            col = mix(vec3(0.62, 0.50, 0.32), vec3(0.90, 0.80, 0.60), b);
        }

        float diff = smoothstep(-0.05, 0.35, dot(n, L));
        col *= 0.04 + 0.96 * diff;
        col += atm * pow(1.0 - n.z, 3.0) * diff * 0.6;
        res = vec4(col, 1.0 - smoothstep(0.985, 1.0, r));
    } else if (!saturn) {
        float lit = clamp(dot(normalize(vec3(q, 0.0)), L) + 0.35, 0.0, 1.0);
        float glow = exp(-(r - 1.0) * 7.0) * 0.8 * lit * (1.0 - smoothstep(1.15, 1.34, r));
        res = vec4(atm, glow);
    }

    if (saturn) {                    // RINGS
        float sinT = 0.35;
        vec2 rq = vec2(q.x, q.y / sinT);
        float rho = length(rq);
        if (rho > 1.3 && rho < 2.4) {
            float ang = atan(rq.y, rq.x) + ph;
            float dens = 0.5 + 0.5 * sin(rho * 38.0);
            dens *= 1.0 - smoothstep(0.0, 0.03, 0.03 - abs(rho - 1.97));
            dens *= 0.8 + 0.2 * noise(vec3(rho * 30.0, ang * 3.0, 1.0));
            float edge = smoothstep(1.3, 1.36, rho) * (1.0 - smoothstep(2.34, 2.4, rho));
            float ringA = dens * 0.85 * edge;
            bool front = q.y < 0.0;
            if (front || r > 1.0) {
                vec3 ringCol = vec3(0.80, 0.70, 0.50) * (0.6 + 0.4 * dens);
                float outA = ringA + res.a * (1.0 - ringA);
                res.rgb = (ringCol * ringA + res.rgb * res.a * (1.0 - ringA)) / max(outA, 1e-4);
                res.a = outA;
            }
        }
    }

    if (res.a <= 0.002) discard;
    fragColor = res;
}
