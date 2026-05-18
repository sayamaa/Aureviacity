/* ================================================================
   Aurevia Portal | Central JavaScript Logic
================================================================ */

function loadGenZMode(cityName) {
  const source = cityName?.closest ? cityName : null;
  const card = source?.closest('.city-card');
  const raw = source?.dataset?.slug || card?.dataset?.slug || cityName || 'chandigarh';
  const slug = String(raw).toLowerCase().trim().replace(/[^a-z0-9]+/g, '');
  if (!slug) return;
  window.location.href = `/cities/${encodeURIComponent(slug)}/genz`;
}

document.addEventListener('DOMContentLoaded', () => {

  /* ── 1. NAVBAR SCROLL EFFECT ── */
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    if (navbar) navbar.classList.toggle('scrolled', window.scrollY > 50);
  });

  /* ── 2. MOBILE MENU TOGGLE ── */
  const mobChk = document.getElementById('mob-chk');
  document.querySelectorAll('.mobile-nav a').forEach(a => {
    a.addEventListener('click', () => {
      if (mobChk) mobChk.checked = false;
    });
  });

  /* ── 3. DARK MODE TOGGLE ── */
  document.querySelectorAll('.topbar').forEach(topbar => {
    const nav = topbar.querySelector(':scope > nav');
    if (!nav || topbar.querySelector('.topbar-menu-toggle')) return;

    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'topbar-menu-toggle';
    toggle.setAttribute('aria-label', 'Menu');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.innerHTML = '<i class="fa-solid fa-bars"></i>';
    topbar.insertBefore(toggle, nav);

    const closeMenu = () => {
      topbar.classList.remove('menu-open');
      toggle.setAttribute('aria-expanded', 'false');
      toggle.innerHTML = '<i class="fa-solid fa-bars"></i>';
    };

    toggle.addEventListener('click', () => {
      const isOpen = topbar.classList.toggle('menu-open');
      toggle.setAttribute('aria-expanded', String(isOpen));
      toggle.innerHTML = isOpen
        ? '<i class="fa-solid fa-xmark"></i>'
        : '<i class="fa-solid fa-bars"></i>';
    });

    nav.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));
    window.addEventListener('resize', () => {
      if (window.innerWidth > 900) closeMenu();
    });
  });

  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    const icon = themeToggle.querySelector('i');
    const isGenzMode = document.body.classList.contains('genz-body');

    if (isGenzMode) {
      document.documentElement.setAttribute('data-theme', 'dark');
      icon?.classList.remove('fa-sun');
      icon?.classList.add('fa-moon');
      themeToggle.setAttribute('title', 'GenZ always stays in dark mode');

      const showGenzDarkNotice = () => {
        document.querySelector('.genz-dark-notice')?.remove();
        const notice = document.createElement('div');
        notice.className = 'genz-dark-notice';
        notice.innerHTML = '<i class="fa-solid fa-bolt"></i><span>GenZ is nocturnal. Dark mode stays ON.</span>';
        document.body.appendChild(notice);
        requestAnimationFrame(() => notice.classList.add('show'));
        window.setTimeout(() => {
          notice.classList.remove('show');
          window.setTimeout(() => notice.remove(), 350);
        }, 2300);
      };

      themeToggle.addEventListener('click', showGenzDarkNotice);
    } else {
      // Check saved preference
      if (localStorage.getItem('aurevia-theme') === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        icon?.classList.replace('fa-moon', 'fa-sun');
      }

      themeToggle.addEventListener('click', () => {
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        if (isDark) {
          document.documentElement.removeAttribute('data-theme');
          localStorage.setItem('aurevia-theme', 'light');
          icon?.classList.replace('fa-sun', 'fa-moon');
        } else {
          document.documentElement.setAttribute('data-theme', 'dark');
          localStorage.setItem('aurevia-theme', 'dark');
          icon?.classList.replace('fa-moon', 'fa-sun');
        }
      });
    }
  }

  /* ── 4. SCROLL FADE-IN ANIMATION ── */
  const fadeTargets = document.querySelectorAll('.fade-in, .reveal');
  const fadeObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
      }
    });
  }, { threshold: 0.1 });
  fadeTargets.forEach(el => fadeObserver.observe(el));

  /* 5. GENERIC CONTENT SLIDERS */
  document.querySelectorAll('.aurevia-slider').forEach((slider) => {
    const track = slider.querySelector('.slider-track');
    const slides = Array.from(slider.querySelectorAll('[data-slide-item], .feature-slide'));
    const dotsWrap = slider.querySelector('.slider-dots');
    const prev = slider.querySelector('.slider-nav.prev');
    const next = slider.querySelector('.slider-nav.next');

    if (!track || slides.length === 0 || !dotsWrap) return;

    let index = 0;
    let autoplayId = null;
    const wantsAutoplay = slider.dataset.autoplay === 'true' && slides.length > 1;

    const goTo = (nextIndex) => {
      index = (nextIndex + slides.length) % slides.length;
      track.style.transform = `translateX(-${index * 100}%)`;
      dotsWrap.querySelectorAll('.slider-dot').forEach((dot, dotIndex) => {
        dot.classList.toggle('active', dotIndex === index);
      });
    };

    slides.forEach((_, dotIndex) => {
      const dot = document.createElement('button');
      dot.type = 'button';
      dot.className = 'slider-dot';
      dot.setAttribute('aria-label', `Go to slide ${dotIndex + 1}`);
      dot.addEventListener('click', () => goTo(dotIndex));
      dotsWrap.appendChild(dot);
    });

    prev?.addEventListener('click', () => goTo(index - 1));
    next?.addEventListener('click', () => goTo(index + 1));

    const stopAutoplay = () => {
      if (autoplayId) {
        window.clearInterval(autoplayId);
        autoplayId = null;
      }
    };

    const startAutoplay = () => {
      if (!wantsAutoplay) return;
      stopAutoplay();
      autoplayId = window.setInterval(() => goTo(index + 1), 4200);
    };

    slider.addEventListener('mouseenter', stopAutoplay);
    slider.addEventListener('mouseleave', startAutoplay);

    goTo(0);
    startAutoplay();
  });

  /* 6. SWIPE / DRAG SCROLL ROWS */
  document.querySelectorAll('.swipe-scroll, .genz-grid.genz-rendered').forEach((track) => {
    let isDragging = false;
    let didDrag = false;
    let suppressClick = false;
    let startX = 0;
    let startY = 0;
    let startScrollLeft = 0;

    track.addEventListener('pointerdown', (event) => {
      if (event.pointerType === 'mouse' && event.button !== 0) return;
      isDragging = true;
      didDrag = false;
      startX = event.clientX;
      startY = event.clientY;
      startScrollLeft = track.scrollLeft;
      track.classList.add('dragging');
      track.setPointerCapture?.(event.pointerId);
    });

    track.addEventListener('pointermove', (event) => {
      if (!isDragging) return;
      const deltaX = Math.abs(event.clientX - startX);
      const deltaY = Math.abs(event.clientY - startY);
      if (deltaX > 18 && deltaX > deltaY) didDrag = true;
      track.scrollLeft = startScrollLeft - (event.clientX - startX);
    });

    const stopDragging = (event) => {
      if (!isDragging) return;
      isDragging = false;
      suppressClick = didDrag;
      track.classList.remove('dragging');
      track.releasePointerCapture?.(event.pointerId);
    };

    track.addEventListener('pointerup', stopDragging);
    track.addEventListener('pointercancel', stopDragging);
    track.addEventListener('pointerleave', stopDragging);
    track.addEventListener('click', (event) => {
      if (!suppressClick) return;
      event.preventDefault();
      event.stopPropagation();
      suppressClick = false;
    }, true);
  });

});
