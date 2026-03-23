<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";

interface StyleMap {
  [key: string]: string;
}

function createSeededRandom(seed: number) {
  let value = seed;
  return () => {
    value = (value * 1664525 + 1013904223) % 4294967296;
    return value / 4294967296;
  };
}

function createStars(
  count: number,
  seed: number,
  sizeRange: [number, number],
  opacityRange: [number, number],
  twinkleRange: [number, number],
  driftRange: [number, number],
): StyleMap[] {
  const random = createSeededRandom(seed);
  const [sizeMin, sizeMax] = sizeRange;
  const [opacityMin, opacityMax] = opacityRange;
  const [twinkleMin, twinkleMax] = twinkleRange;
  const [driftMin, driftMax] = driftRange;

  return Array.from({ length: count }, () => {
    const size = sizeMin + random() * (sizeMax - sizeMin);
    const opacity = opacityMin + random() * (opacityMax - opacityMin);
    const twinkle = twinkleMin + random() * (twinkleMax - twinkleMin);
    const twinkleDelay = random() * 8;
    const drift = driftMin + random() * (driftMax - driftMin);
    const driftDelay = random() * 10;
    const driftX = (random() - 0.5) * 36;
    const driftY = (random() - 0.5) * 36;
    const pulseDuration = 2.2 + random() * 5.2;
    const pulseDelay = random() * 6;
    const jitterDuration = 1.4 + random() * 2.8;
    const jitterDelay = random() * 5;
    const chromaDuration = 7 + random() * 10;
    const chromaDelay = random() * 8;
    const flareDuration = 3 + random() * 6;
    const flareDelay = random() * 7;

    const palettePick = random();
    let hue = 205 + random() * 35;
    if (palettePick > 0.28 && palettePick <= 0.48) {
      hue = 160 + random() * 20;
    } else if (palettePick > 0.48 && palettePick <= 0.66) {
      hue = 255 + random() * 35;
    } else if (palettePick > 0.66 && palettePick <= 0.82) {
      hue = 34 + random() * 25;
    } else if (palettePick > 0.82) {
      hue = 332 + random() * 20;
    }

    const saturation = 72 + random() * 24;
    const lightness = 74 + random() * 18;
    const glowAlpha = 0.32 + random() * 0.62;
    const flareScale = 1.2 + random() * 1.5;

    return {
      left: `${(random() * 100).toFixed(2)}%`,
      top: `${(random() * 100).toFixed(2)}%`,
      width: `${size.toFixed(2)}px`,
      height: `${size.toFixed(2)}px`,
      "--star-opacity": opacity.toFixed(2),
      "--twinkle-duration": `${twinkle.toFixed(2)}s`,
      "--twinkle-delay": `${twinkleDelay.toFixed(2)}s`,
      "--drift-duration": `${drift.toFixed(2)}s`,
      "--drift-delay": `${driftDelay.toFixed(2)}s`,
      "--drift-x": `${driftX.toFixed(2)}px`,
      "--drift-y": `${driftY.toFixed(2)}px`,
      "--pulse-duration": `${pulseDuration.toFixed(2)}s`,
      "--pulse-delay": `${pulseDelay.toFixed(2)}s`,
      "--jitter-duration": `${jitterDuration.toFixed(2)}s`,
      "--jitter-delay": `${jitterDelay.toFixed(2)}s`,
      "--chroma-duration": `${chromaDuration.toFixed(2)}s`,
      "--chroma-delay": `${chromaDelay.toFixed(2)}s`,
      "--flare-duration": `${flareDuration.toFixed(2)}s`,
      "--flare-delay": `${flareDelay.toFixed(2)}s`,
      "--star-hue": `${hue.toFixed(2)}`,
      "--star-saturation": `${saturation.toFixed(2)}%`,
      "--star-lightness": `${lightness.toFixed(2)}%`,
      "--star-glow-alpha": `${glowAlpha.toFixed(2)}`,
      "--flare-scale": `${flareScale.toFixed(2)}`,
    };
  });
}

function createMeteors(count: number, seed: number): StyleMap[] {
  const random = createSeededRandom(seed);

  return Array.from({ length: count }, () => {
    const width = 90 + random() * 170;
    const top = 4 + random() * 44;
    const left = 56 + random() * 42;
    const delay = random() * 10;
    const duration = 4.5 + random() * 4.2;
    const travel = 320 + random() * 360;
    const hue = 180 + random() * 170;
    const saturation = 78 + random() * 20;

    return {
      top: `${top.toFixed(2)}%`,
      left: `${left.toFixed(2)}%`,
      width: `${width.toFixed(2)}px`,
      "--meteor-delay": `${delay.toFixed(2)}s`,
      "--meteor-duration": `${duration.toFixed(2)}s`,
      "--meteor-travel": `${travel.toFixed(2)}px`,
      "--meteor-hue": `${hue.toFixed(2)}`,
      "--meteor-sat": `${saturation.toFixed(2)}%`,
    };
  });
}

function createPrismSparks(count: number, seed: number): StyleMap[] {
  const random = createSeededRandom(seed);

  return Array.from({ length: count }, () => {
    const hue = 160 + random() * 210;
    const size = 1.4 + random() * 3.6;
    const opacity = 0.2 + random() * 0.42;
    const duration = 3.5 + random() * 7;
    const delay = random() * 9;

    return {
      left: `${(random() * 100).toFixed(2)}%`,
      top: `${(random() * 100).toFixed(2)}%`,
      width: `${size.toFixed(2)}px`,
      height: `${size.toFixed(2)}px`,
      "--spark-hue": `${hue.toFixed(2)}`,
      "--spark-opacity": `${opacity.toFixed(2)}`,
      "--spark-duration": `${duration.toFixed(2)}s`,
      "--spark-delay": `${delay.toFixed(2)}s`,
      "--spark-scale": `${(1.15 + random() * 1.8).toFixed(2)}`,
    };
  });
}

function createDustParticles(count: number, seed: number): StyleMap[] {
  const random = createSeededRandom(seed);

  return Array.from({ length: count }, () => {
    const size = 1.2 + random() * 2.8;
    const opacity = 0.08 + random() * 0.28;
    const duration = 14 + random() * 20;
    const delay = random() * 8;

    return {
      left: `${(random() * 100).toFixed(2)}%`,
      top: `${(random() * 100).toFixed(2)}%`,
      width: `${size.toFixed(2)}px`,
      height: `${size.toFixed(2)}px`,
      "--dust-opacity": opacity.toFixed(2),
      "--dust-duration": `${duration.toFixed(2)}s`,
      "--dust-delay": `${delay.toFixed(2)}s`,
      "--dust-shift-x": `${((random() - 0.5) * 120).toFixed(2)}px`,
      "--dust-shift-y": `${((random() - 0.5) * 120).toFixed(2)}px`,
    };
  });
}

function createConstellationSegments(count: number, seed: number): StyleMap[] {
  const random = createSeededRandom(seed);

  return Array.from({ length: count }, () => {
    const width = 40 + random() * 100;
    const angle = -35 + random() * 70;
    const opacity = 0.08 + random() * 0.2;
    const duration = 8 + random() * 10;
    const delay = random() * 6;

    return {
      left: `${(8 + random() * 84).toFixed(2)}%`,
      top: `${(10 + random() * 76).toFixed(2)}%`,
      width: `${width.toFixed(2)}px`,
      transform: `rotate(${angle.toFixed(2)}deg)`,
      "--line-opacity": opacity.toFixed(2),
      "--line-duration": `${duration.toFixed(2)}s`,
      "--line-delay": `${delay.toFixed(2)}s`,
    };
  });
}

const farStars = createStars(120, 101, [0.7, 1.3], [0.2, 0.58], [3.8, 8.5], [24, 38]);
const midStars = createStars(160, 202, [0.9, 1.8], [0.3, 0.78], [2.8, 7.2], [18, 30]);
const nearStars = createStars(210, 303, [1.2, 2.8], [0.38, 0.95], [2.2, 6], [14, 24]);
const glowStars = createStars(30, 404, [2.6, 4.6], [0.4, 0.92], [1.8, 4.6], [10, 18]);
const meteors = createMeteors(8, 505);
const dustParticles = createDustParticles(70, 606);
const constellationSegments = createConstellationSegments(22, 707);
const prismSparks = createPrismSparks(46, 808);
const supernovaActive = ref(false);
const supernovaStyle = ref<StyleMap>({
  left: "50%",
  top: "40%",
  "--supernova-size": "220px",
  "--supernova-hue": "205",
});

const backgroundRef = ref<HTMLElement | null>(null);
let frameId = 0;
let supernovaTimer = 0;
let supernovaEndTimer = 0;
const motion = {
  currentX: 0,
  currentY: 0,
  targetX: 0,
  targetY: 0,
};

function updateParallax() {
  motion.currentX += (motion.targetX - motion.currentX) * 0.08;
  motion.currentY += (motion.targetY - motion.currentY) * 0.08;

  if (backgroundRef.value) {
    backgroundRef.value.style.setProperty("--parallax-x", `${motion.currentX.toFixed(2)}px`);
    backgroundRef.value.style.setProperty("--parallax-y", `${motion.currentY.toFixed(2)}px`);
  }

  frameId = window.requestAnimationFrame(updateParallax);
}

function onMouseMove(event: MouseEvent) {
  const xCenter = window.innerWidth / 2;
  const yCenter = window.innerHeight / 2;
  const ratioX = (event.clientX - xCenter) / xCenter;
  const ratioY = (event.clientY - yCenter) / yCenter;

  motion.targetX = ratioX * 12;
  motion.targetY = ratioY * 12;
}

function triggerSupernova() {
  const size = 190 + Math.random() * 190;
  const left = 8 + Math.random() * 82;
  const top = 8 + Math.random() * 70;
  const hue = 180 + Math.random() * 180;

  supernovaStyle.value = {
    left: `${left.toFixed(2)}%`,
    top: `${top.toFixed(2)}%`,
    "--supernova-size": `${size.toFixed(2)}px`,
    "--supernova-hue": `${hue.toFixed(2)}`,
  };
  supernovaActive.value = true;

  window.clearTimeout(supernovaEndTimer);
  supernovaEndTimer = window.setTimeout(() => {
    supernovaActive.value = false;
  }, 1200);
}

function scheduleSupernova() {
  const delay = 9000 + Math.random() * 12000;
  supernovaTimer = window.setTimeout(() => {
    triggerSupernova();
    scheduleSupernova();
  }, delay);
}

onMounted(() => {
  window.addEventListener("mousemove", onMouseMove, { passive: true });
  frameId = window.requestAnimationFrame(updateParallax);
  scheduleSupernova();
});

onBeforeUnmount(() => {
  window.removeEventListener("mousemove", onMouseMove);
  window.cancelAnimationFrame(frameId);
  window.clearTimeout(supernovaTimer);
  window.clearTimeout(supernovaEndTimer);
});
</script>

<template>
  <section ref="backgroundRef" class="cosmic-bg" aria-hidden="true">
    <div class="nebula nebula-a"></div>
    <div class="nebula nebula-b"></div>
    <div class="nebula nebula-c"></div>

    <div class="aurora aurora-a"></div>
    <div class="aurora aurora-b"></div>

    <div class="supernova" :class="{ active: supernovaActive }" :style="supernovaStyle"></div>

    <div class="star-layer layer-far">
      <span
        v-for="(star, index) in farStars"
        :key="`far-${index}`"
        class="star"
        :style="star"
      ></span>
    </div>

    <div class="star-layer layer-mid">
      <span
        v-for="(star, index) in midStars"
        :key="`mid-${index}`"
        class="star"
        :style="star"
      ></span>
    </div>

    <div class="star-layer layer-near">
      <span
        v-for="(star, index) in nearStars"
        :key="`near-${index}`"
        class="star"
        :style="star"
      ></span>
    </div>

    <div class="star-layer layer-glow">
      <span
        v-for="(star, index) in glowStars"
        :key="`glow-${index}`"
        class="star star-glow"
        :style="star"
      ></span>
    </div>

    <div class="constellation-layer">
      <span
        v-for="(line, index) in constellationSegments"
        :key="`line-${index}`"
        class="constellation-line"
        :style="line"
      ></span>
    </div>

    <div class="dust-layer">
      <span
        v-for="(dust, index) in dustParticles"
        :key="`dust-${index}`"
        class="dust"
        :style="dust"
      ></span>
    </div>

    <div class="prism-layer">
      <span
        v-for="(spark, index) in prismSparks"
        :key="`spark-${index}`"
        class="prism-spark"
        :style="spark"
      ></span>
    </div>

    <div class="meteor-field">
      <span
        v-for="(meteor, index) in meteors"
        :key="`meteor-${index}`"
        class="meteor"
        :style="meteor"
      ></span>
    </div>
  </section>
</template>

<style scoped>
.cosmic-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
  --parallax-x: 0px;
  --parallax-y: 0px;
}

.nebula,
.aurora,
.star-layer,
.meteor-field,
.dust-layer,
.constellation-layer,
.prism-layer {
  position: absolute;
  inset: 0;
}

.supernova {
  position: absolute;
  width: var(--supernova-size);
  height: var(--supernova-size);
  transform: translate(-50%, -50%);
  opacity: 0;
  border-radius: 999px;
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.95),
    hsla(var(--supernova-hue), 95%, 82%, 0.5) 24%,
    transparent 68%
  );
  filter: blur(0.4px) drop-shadow(0 0 24px rgba(165, 224, 255, 0.85));
  mix-blend-mode: screen;
  pointer-events: none;
  z-index: 1;
}

.supernova.active {
  animation: supernova-burst 1.2s ease-out;
}

.nebula {
  filter: blur(54px);
  mix-blend-mode: screen;
  animation: nebula-breathe 12s ease-in-out infinite;
  transform: translate3d(calc(var(--parallax-x) * 0.55), calc(var(--parallax-y) * 0.55), 0);
}

.nebula-a {
  background: radial-gradient(circle at 18% 22%, rgba(101, 164, 255, 0.24), transparent 50%);
  animation-delay: 0s;
}

.nebula-b {
  background: radial-gradient(circle at 80% 30%, rgba(74, 255, 205, 0.16), transparent 48%);
  animation-delay: 3s;
}

.nebula-c {
  background: radial-gradient(circle at 48% 72%, rgba(85, 135, 255, 0.12), transparent 52%);
  animation-delay: 6s;
}

.aurora {
  opacity: 0.28;
  filter: blur(20px);
  mix-blend-mode: screen;
  transform: translate3d(calc(var(--parallax-x) * 0.35), calc(var(--parallax-y) * 0.35), 0);
}

.aurora-a {
  background: linear-gradient(120deg, transparent 20%, rgba(113, 209, 255, 0.28), transparent 70%);
  transform: translateX(-32%);
  animation: aurora-flow 14s ease-in-out infinite;
}

.aurora-b {
  background: linear-gradient(70deg, transparent 30%, rgba(76, 255, 212, 0.24), transparent 78%);
  transform: translateX(36%);
  animation: aurora-flow-reverse 18s ease-in-out infinite;
}

.layer-far {
  opacity: 0.45;
  transform: translate3d(calc(var(--parallax-x) * 0.22), calc(var(--parallax-y) * 0.22), 0);
}

.layer-mid {
  opacity: 0.65;
  transform: translate3d(calc(var(--parallax-x) * 0.4), calc(var(--parallax-y) * 0.4), 0);
}

.layer-near {
  opacity: 0.88;
  transform: translate3d(calc(var(--parallax-x) * 0.68), calc(var(--parallax-y) * 0.68), 0);
}

.layer-glow {
  opacity: 0.92;
  transform: translate3d(calc(var(--parallax-x) * 0.9), calc(var(--parallax-y) * 0.9), 0);
}

.constellation-layer {
  opacity: 0.52;
  mix-blend-mode: screen;
  transform: translate3d(calc(var(--parallax-x) * 0.58), calc(var(--parallax-y) * 0.58), 0);
}

.constellation-line {
  position: absolute;
  height: 1px;
  transform-origin: left center;
  background: linear-gradient(
    90deg,
    rgba(196, 235, 255, 0),
    rgba(196, 235, 255, var(--line-opacity)),
    rgba(196, 235, 255, 0)
  );
  animation: constellation-glimmer var(--line-duration) ease-in-out infinite;
  animation-delay: var(--line-delay);
}

.dust-layer {
  mix-blend-mode: screen;
  opacity: 0.42;
  transform: translate3d(calc(var(--parallax-x) * 0.8), calc(var(--parallax-y) * 0.8), 0);
}

.dust {
  position: absolute;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(181, 229, 255, 0.8), rgba(181, 229, 255, 0));
  opacity: var(--dust-opacity);
  animation: dust-float var(--dust-duration) linear infinite;
  animation-delay: var(--dust-delay);
}

.star {
  position: absolute;
  border-radius: 999px;
  background: hsl(var(--star-hue), var(--star-saturation), var(--star-lightness));
  opacity: var(--star-opacity);
  transform: translate3d(0, 0, 0);
  box-shadow: 0 0 7px hsla(var(--star-hue), 100%, 82%, var(--star-glow-alpha));
  animation:
    star-twinkle var(--twinkle-duration) ease-in-out infinite,
    star-drift var(--drift-duration) ease-in-out infinite;
  animation-delay: var(--twinkle-delay), var(--drift-delay);
}

.star::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 50%;
  width: 180%;
  height: 180%;
  border-radius: 999px;
  transform: translate(-50%, -50%) scale(0.4);
  background: radial-gradient(
    circle,
    hsla(var(--star-hue), 100%, 92%, 0.65),
    hsla(var(--star-hue), 100%, 80%, 0)
  );
  opacity: 0;
  animation: star-flare var(--flare-duration) ease-in-out infinite;
  animation-delay: var(--flare-delay);
}

.star:nth-child(4n) {
  animation:
    star-twinkle var(--twinkle-duration) ease-in-out infinite,
    star-drift var(--drift-duration) ease-in-out infinite,
    star-pulse var(--pulse-duration) ease-in-out infinite;
  animation-delay: var(--twinkle-delay), var(--drift-delay), var(--pulse-delay);
}

.star:nth-child(7n) {
  animation:
    star-twinkle var(--twinkle-duration) ease-in-out infinite,
    star-drift var(--drift-duration) ease-in-out infinite,
    star-jitter var(--jitter-duration) steps(2, end) infinite;
  animation-delay: var(--twinkle-delay), var(--drift-delay), var(--jitter-delay);
}

.star:nth-child(9n) {
  animation:
    star-twinkle var(--twinkle-duration) ease-in-out infinite,
    star-drift var(--drift-duration) ease-in-out infinite,
    star-color-shift var(--chroma-duration) ease-in-out infinite;
  animation-delay: var(--twinkle-delay), var(--drift-delay), var(--chroma-delay);
}

.star-glow {
  box-shadow:
    0 0 12px hsla(var(--star-hue), 100%, 90%, 0.9),
    0 0 22px hsla(var(--star-hue), 100%, 72%, 0.7);
}

.prism-layer {
  opacity: 0.62;
  mix-blend-mode: screen;
  transform: translate3d(calc(var(--parallax-x) * 1), calc(var(--parallax-y) * 1), 0);
}

.prism-spark {
  position: absolute;
  border-radius: 999px;
  background: hsla(var(--spark-hue), 98%, 78%, var(--spark-opacity));
  box-shadow: 0 0 10px hsla(var(--spark-hue), 98%, 72%, calc(var(--spark-opacity) + 0.12));
  transform: translate3d(0, 0, 0) scale(0.8);
  animation: prism-behavior var(--spark-duration) ease-in-out infinite;
  animation-delay: var(--spark-delay);
}

.prism-spark::before,
.prism-spark::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 50%;
  width: 230%;
  height: 1px;
  background: linear-gradient(
    90deg,
    hsla(var(--spark-hue), 96%, 80%, 0),
    hsla(var(--spark-hue), 96%, 84%, 0.65),
    hsla(var(--spark-hue), 96%, 80%, 0)
  );
  transform: translate(-50%, -50%);
}

.prism-spark::after {
  transform: translate(-50%, -50%) rotate(90deg);
}

.meteor {
  position: absolute;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    hsla(var(--meteor-hue), var(--meteor-sat), 90%, 0),
    hsla(var(--meteor-hue), var(--meteor-sat), 84%, 0.98)
  );
  box-shadow: 0 0 14px hsla(var(--meteor-hue), 100%, 82%, 0.82);
  transform: rotate(-26deg);
  opacity: 0;
  animation: meteor-fall var(--meteor-duration) linear infinite;
  animation-delay: var(--meteor-delay);
  transform-origin: center;
}

@keyframes star-twinkle {
  0% {
    opacity: calc(var(--star-opacity) * 0.5);
  }
  45% {
    opacity: var(--star-opacity);
  }
  100% {
    opacity: calc(var(--star-opacity) * 0.5);
  }
}

@keyframes star-drift {
  0% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(var(--drift-x), var(--drift-y), 0);
  }
  100% {
    transform: translate3d(0, 0, 0);
  }
}

@keyframes star-pulse {
  0% {
    filter: saturate(0.9) brightness(0.96);
  }
  50% {
    filter: saturate(1.35) brightness(1.2);
  }
  100% {
    filter: saturate(0.9) brightness(0.96);
  }
}

@keyframes star-jitter {
  0% {
    margin-left: 0;
    margin-top: 0;
  }
  35% {
    margin-left: 0.7px;
    margin-top: -0.4px;
  }
  65% {
    margin-left: -0.6px;
    margin-top: 0.5px;
  }
  100% {
    margin-left: 0;
    margin-top: 0;
  }
}

@keyframes star-color-shift {
  0% {
    filter: hue-rotate(0deg) brightness(1);
  }
  50% {
    filter: hue-rotate(38deg) brightness(1.14);
  }
  100% {
    filter: hue-rotate(0deg) brightness(1);
  }
}

@keyframes star-flare {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.35);
  }
  30% {
    opacity: 0.85;
    transform: translate(-50%, -50%) scale(var(--flare-scale));
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.35);
  }
}

@keyframes meteor-fall {
  0% {
    opacity: 0;
    transform: rotate(-26deg) translate3d(0, 0, 0);
  }
  7% {
    opacity: 1;
  }
  38% {
    opacity: 1;
    transform: rotate(-26deg)
      translate3d(calc(var(--meteor-travel) * -1), calc(var(--meteor-travel) * 0.52), 0);
  }
  48% {
    opacity: 0;
    transform: rotate(-26deg)
      translate3d(calc(var(--meteor-travel) * -1.22), calc(var(--meteor-travel) * 0.66), 0);
  }
  100% {
    opacity: 0;
    transform: rotate(-26deg)
      translate3d(calc(var(--meteor-travel) * -1.22), calc(var(--meteor-travel) * 0.66), 0);
  }
}

@keyframes dust-float {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
    opacity: calc(var(--dust-opacity) * 0.35);
  }
  50% {
    transform: translate3d(var(--dust-shift-x), var(--dust-shift-y), 0) scale(1.3);
    opacity: var(--dust-opacity);
  }
  100% {
    transform: translate3d(0, 0, 0) scale(1);
    opacity: calc(var(--dust-opacity) * 0.35);
  }
}

@keyframes prism-behavior {
  0% {
    opacity: 0;
    transform: translate3d(0, 0, 0) scale(0.65);
    filter: brightness(0.95);
  }
  30% {
    opacity: 1;
    transform: translate3d(0, -8px, 0) scale(var(--spark-scale));
    filter: brightness(1.3);
  }
  60% {
    opacity: 0.55;
    transform: translate3d(6px, -16px, 0) scale(0.88);
  }
  100% {
    opacity: 0;
    transform: translate3d(-4px, -24px, 0) scale(0.45);
    filter: brightness(0.9);
  }
}

@keyframes constellation-glimmer {
  0% {
    opacity: 0;
    filter: blur(0px);
  }
  30% {
    opacity: 0.85;
    filter: blur(0.2px);
  }
  60% {
    opacity: 0.22;
  }
  100% {
    opacity: 0;
  }
}

@keyframes nebula-breathe {
  0% {
    transform: scale(1);
    opacity: 0.38;
  }
  50% {
    transform: scale(1.12);
    opacity: 0.54;
  }
  100% {
    transform: scale(1);
    opacity: 0.38;
  }
}

@keyframes supernova-burst {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.35);
    filter: blur(0.2px) drop-shadow(0 0 12px rgba(165, 224, 255, 0.7));
  }
  24% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
    filter: blur(0.8px) drop-shadow(0 0 36px rgba(185, 236, 255, 1));
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(1.95);
    filter: blur(1px) drop-shadow(0 0 10px rgba(165, 224, 255, 0));
  }
}

@keyframes aurora-flow {
  0% {
    transform: translate3d(-32%, 0, 0) rotate(-8deg);
    opacity: 0.14;
  }
  50% {
    transform: translate3d(18%, -3%, 0) rotate(-2deg);
    opacity: 0.4;
  }
  100% {
    transform: translate3d(-32%, 0, 0) rotate(-8deg);
    opacity: 0.14;
  }
}

@keyframes aurora-flow-reverse {
  0% {
    transform: translate3d(36%, 0, 0) rotate(10deg);
    opacity: 0.16;
  }
  50% {
    transform: translate3d(-14%, 3%, 0) rotate(3deg);
    opacity: 0.36;
  }
  100% {
    transform: translate3d(36%, 0, 0) rotate(10deg);
    opacity: 0.16;
  }
}

@media (max-width: 900px) {
  .layer-far {
    opacity: 0.35;
  }

  .layer-mid {
    opacity: 0.54;
  }

  .layer-near {
    opacity: 0.72;
  }

  .meteor {
    height: 1.5px;
  }

  .constellation-layer {
    opacity: 0.35;
  }

  .dust-layer {
    opacity: 0.28;
  }

  .prism-layer {
    opacity: 0.45;
  }
}

@media (prefers-reduced-motion: reduce) {
  .star,
  .star::after,
  .meteor,
  .nebula,
  .aurora,
  .dust,
  .constellation-line,
  .supernova,
  .prism-spark {
    animation: none;
  }
}
</style>
