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

// Blackbody-like palette for the disc: hot lavender-white -> violet -> deep blue.
vec3 tempColor(float T) {
    vec3 hot = vec3(1.0, 0.93, 1.0);
    vec3 mid = vec3(0.75, 0.35, 1.0);
    vec3 cool = vec3(0.12, 0.28, 1.0);
    vec3 c = mix(cool, mid, smoothstep(0.15, 0.55, T));
    return mix(c, hot, smoothstep(0.55, 1.10, T));
}

// Physically-inspired black hole: per-pixel Schwarzschild-style ray bending (horizon r = 1),
// thin accretion disc in the y = 0 plane (r = 3..9), Doppler beaming, turbulent differential rotation.
// q spans [-3, 3] over the quad. Disc turbulence uses a two-layer flow-map so the looping phase never pops.
vec4 blackHole(vec2 q, float ph, float level) {
    float R = length(q);
    float roll = -0.25;
    vec2 qq = vec2(cos(roll) * q.x - sin(roll) * q.y, sin(roll) * q.x + cos(roll) * q.y);

    float incl = 0.17;                                    // camera elevation above the disc plane
    vec3 camPos = 12.0 * vec3(0.0, sin(incl), -cos(incl));
    vec3 fwd = -normalize(camPos);
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), fwd));
    vec3 upv = cross(fwd, right);
    vec3 vel = normalize(fwd + (qq.x * right + qq.y * upv) * 0.394);
    vec3 pos = camPos;

    vec3 cp = cross(pos, vel);
    float h2 = dot(cp, cp);
    int maxSteps = 60 + int(level) * 15;
    int oct = 3 + (level > 1.5 ? 1 : 0);

    float t1 = fract(ph * 0.15915494);
    float t2 = fract(t1 + 0.5);
    float w1 = 1.0 - abs(2.0 * t1 - 1.0);

    vec3 col = vec3(0.0);
    float trans = 1.0;
    bool captured = true;

    for (int i = 0; i < 105; i++) {
        if (i >= maxSteps) break;
        float r2 = dot(pos, pos);
        float r = sqrt(r2);
        if (r < 1.0) break;                               // swallowed by the horizon
        if (r > 26.0 && dot(pos, vel) > 0.0) { captured = false; break; }

        float dt = clamp(0.07 * r, 0.04, 0.9);
        vec3 acc = -1.5 * h2 * pos / (r2 * r2 * r);
        vec3 nvel = normalize(vel + acc * dt);
        vec3 npos = pos + nvel * dt;

        if (pos.y * npos.y < 0.0) {                       // crossed the disc plane
            vec3 hit = mix(pos, npos, pos.y / (pos.y - npos.y));
            float rr = length(hit.xz);
            if (rr > 3.0 && rr < 9.0) {
                float T = pow(3.0 / rr, 0.75);
                vec3 vd = vec3(-hit.z, 0.0, hit.x) / rr;
                float shift = dot(vd, -nvel) * 0.6 * sqrt(3.0 / rr);       // >0 on the approaching side
                float beam = pow(clamp(1.0 + 1.3 * shift, 0.2, 2.5), 3.0);

                float a = atan(hit.z, hit.x);
                float spin = pow(3.0 / rr, 1.5) * 12.0 * 6.2831853;
                float a1 = a - t1 * spin;
                float a2 = a - t2 * spin;
                float n1 = fbm(vec3(cos(a1) * rr * 0.8, sin(a1) * rr * 0.8, rr * 3.0), oct);
                float n2 = fbm(vec3(cos(a2) * rr * 0.8, sin(a2) * rr * 0.8, rr * 3.0), oct);
                float n = mix(n2, n1, w1);

                float edge = smoothstep(3.0, 3.3, rr) * (1.0 - smoothstep(6.5, 9.0, rr));
                float dens = 0.3 + 0.7 * smoothstep(0.25, 0.75, n);
                float alpha = clamp(edge * dens * 1.15, 0.0, 1.0);
                float inten = 0.15 + 2.2 * pow(T, 1.5);
                col += trans * alpha * tempColor(T * (1.0 + 0.5 * shift)) * inten * beam;
                trans *= 1.0 - alpha * 0.95;
            }
        }
        pos = npos;
        vel = nvel;
    }

    vec3 c = 1.0 - exp(-col);                             // simple tone mapping, premultiplied
    float cover = captured ? 1.0 : 1.0 - trans;
    float a = max(cover, max(c.r, max(c.g, c.b)));
    vec4 res = vec4(min(c / max(a, 1e-4), vec3(1.0)), a);

    float halo = 0.22 * exp(-pow(R / 1.05, 2.0)) * (1.0 - smoothstep(2.0, 2.9, R));
    vec4 haloCol = vec4(mix(vec3(0.45, 0.2, 1.0), vec3(0.12, 0.3, 1.0), smoothstep(0.3, 1.6, R)), halo);
    return over(res, haloCol);
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
        vec4 bhc = blackHole(p * 3.0, ph, qual);
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
