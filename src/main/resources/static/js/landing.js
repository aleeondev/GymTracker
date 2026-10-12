// ==========================================================
// LANDING.JS
// Pequeñas interacciones visuales para la landing page.
// No depende de ninguna librería externa.
// ==========================================================

document.addEventListener("DOMContentLoaded", () => {
  initHeaderScroll();
  initHeroSpotlight();
  initFeatureTilt();
  initScrollReveal();
});

// -----------------------------------------------------------
// 1) Header con sombra al hacer scroll
// -----------------------------------------------------------
function initHeaderScroll() {
  const header = document.querySelector("header");
  if (!header) return;

  const onScroll = () => {
    header.classList.toggle("scrolled", window.scrollY > 10);
  };

  onScroll();
  window.addEventListener("scroll", onScroll, { passive: true });
}

// -----------------------------------------------------------
// 2) Spotlight que sigue al cursor dentro del hero
// -----------------------------------------------------------
function initHeroSpotlight() {
  const hero = document.querySelector(".landing-hero");
  if (!hero) return;

  hero.addEventListener("mousemove", (event) => {
    const rect = hero.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width) * 100;
    const y = ((event.clientY - rect.top) / rect.height) * 100;

    hero.style.setProperty("--x", `${x}%`);
    hero.style.setProperty("--y", `${y}%`);
  });

  hero.addEventListener("mouseenter", () => {
    hero.classList.add("spotlight-active");
  });

  hero.addEventListener("mouseleave", () => {
    hero.classList.remove("spotlight-active");
  });
}

// -----------------------------------------------------------
// 3) Tilt 3D sutil en las tarjetas de "Funciones"
// -----------------------------------------------------------
function initFeatureTilt() {
  const features = document.querySelectorAll(".feature");
  if (!features.length) return;

  const MAX_TILT_DEGREES = 6;

  features.forEach((feature) => {
    feature.addEventListener("mousemove", (event) => {
      const rect = feature.getBoundingClientRect();

      // Posición del cursor relativa al centro de la tarjeta, entre -1 y 1
      const relativeX = (event.clientX - rect.left) / rect.width - 0.5;
      const relativeY = (event.clientY - rect.top) / rect.height - 0.5;

      const rotateY = relativeX * MAX_TILT_DEGREES * 2;
      const rotateX = relativeY * -MAX_TILT_DEGREES * 2;

      feature.style.transform =
        `perspective(700px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-2px)`;
    });

    feature.addEventListener("mouseleave", () => {
      feature.style.transform = "";
    });
  });
}

// -----------------------------------------------------------
// 4) Revelar secciones al hacer scroll (IntersectionObserver)
// -----------------------------------------------------------
function initScrollReveal() {
  const targets = document.querySelectorAll(
    ".feature-grid .feature, .project-section, .final-cta"
  );
  if (!targets.length) return;

  targets.forEach((el) => el.classList.add("reveal"));

  // Si el navegador no soporta IntersectionObserver, mostramos todo directamente
  if (!("IntersectionObserver" in window)) {
    targets.forEach((el) => el.classList.add("is-visible"));
    return;
  }

  const observer = new IntersectionObserver(
    (entries, obs) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          obs.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.15 }
  );

  targets.forEach((el) => observer.observe(el));
}