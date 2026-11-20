(() => {
  "use strict";

  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));
  const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
  const easeOut = (t) => 1 - Math.pow(1 - t, 3);

  const tween = ({ from, to, duration, onUpdate, alive = () => true }) =>
    new Promise((resolve) => {
      if (reduced || duration <= 0) {
        onUpdate(to);
        resolve();
        return;
      }
      const start = performance.now();
      const frame = (now) => {
        if (!alive()) return resolve();
        const t = clamp((now - start) / duration, 0, 1);
        onUpdate(from + (to - from) * easeOut(t));
        if (t < 1) requestAnimationFrame(frame);
        else resolve();
      };
      requestAnimationFrame(frame);
    });

  const observeVisibility = (el, onChange, threshold = 0.3) => {
    if (!el) return;
    const io = new IntersectionObserver((entries) => entries.forEach((e) => onChange(e.isIntersecting)), { threshold });
    io.observe(el);
  };

  const loopWhileVisible = (el, play, threshold = 0.35) => {
    let token = 0;
    observeVisibility(el, (visible) => {
      token += 1;
      if (!visible) return;
      const mine = token;
      const alive = () => mine === token;
      (async () => {
        while (alive()) {
          await play(alive);
          await wait(50);
        }
      })();
    }, threshold);
  };

  /* Header */
  const header = $("#siteHeader");
  const onHeaderScroll = () => header.classList.toggle("is-scrolled", window.scrollY > 24);
  window.addEventListener("scroll", onHeaderScroll, { passive: true });
  onHeaderScroll();

  /* Reveal on scroll */
  const revealIO = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("in");
      revealIO.unobserve(entry.target);
    });
  }, { threshold: 0.12, rootMargin: "0px 0px -8% 0px" });
  $$(".reveal").forEach((el) => revealIO.observe(el));

  /* Hero: starfield + scroll-driven sunrise */
  const initHero = () => {
    const hero = $(".hero");
    const canvas = $("#stars");
    if (!hero || !canvas) return;
    const ctx = canvas.getContext("2d");
    let stars = [];
    let width = 0;
    let height = 0;
    let heroHeight = 1;
    let progress = reduced ? 0.35 : 0;
    let visible = true;
    let rafId = 0;

    const draw = (time) => {
      ctx.clearRect(0, 0, width, height);
      const fade = 1 - progress * 0.75;
      stars.forEach((s) => {
        const twinkle = reduced ? 1 : 0.65 + 0.35 * Math.sin(time / 1000 * s.speed + s.phase);
        const lowFade = 1 - (s.y / height) * progress * 0.8;
        ctx.globalAlpha = clamp(s.alpha * twinkle * fade * lowFade, 0, 1);
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
        ctx.fill();
      });
    };

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = canvas.clientWidth;
      height = canvas.clientHeight;
      heroHeight = hero.offsetHeight || 1;
      canvas.width = Math.round(width * dpr);
      canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.fillStyle = "#FFF8F1";
      const count = Math.round((width * height) / 4800);
      stars = Array.from({ length: count }, () => ({
        x: Math.random() * width,
        y: Math.random() * height * 0.95,
        r: Math.random() * 1.2 + 0.3,
        alpha: Math.random() * 0.6 + 0.35,
        speed: Math.random() * 1.6 + 0.4,
        phase: Math.random() * Math.PI * 2
      }));
      draw(performance.now());
    };

    const frame = (time) => {
      draw(time);
      rafId = visible ? requestAnimationFrame(frame) : 0;
    };

    const setProgress = () => {
      progress = reduced ? 0.35 : clamp(window.scrollY / (heroHeight * 0.75), 0, 1);
      hero.style.setProperty("--p", progress.toFixed(3));
    };

    let resizeTimer = 0;
    window.addEventListener("resize", () => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => { resize(); setProgress(); }, 150);
    });
    resize();
    setProgress();

    if (reduced) return;
    let scrollQueued = false;
    window.addEventListener("scroll", () => {
      if (scrollQueued) return;
      scrollQueued = true;
      requestAnimationFrame(() => { scrollQueued = false; setProgress(); });
    }, { passive: true });
    observeVisibility(hero, (isVisible) => {
      visible = isVisible;
      if (visible && !rafId) rafId = requestAnimationFrame(frame);
    }, 0);
  };

  /* Hero phone: the onboarding welcome screen */
  const initWelcome = () => {
    const screen = $("#welcomeDemo");
    const timer = $("#welcomeTimer");
    if (!screen) return;
    if (reduced) {
      screen.classList.add("p1", "p2");
      timer.textContent = "40";
      return;
    }
    loopWhileVisible(screen, async (alive) => {
      screen.classList.remove("p1", "p2");
      timer.textContent = "1";
      await wait(900);
      if (!alive()) return;
      screen.classList.add("p1");
      await wait(1600);
      if (!alive()) return;
      screen.classList.add("p2");
      await tween({ from: 1, to: 40, duration: 7000, alive, onUpdate: (v) => { timer.textContent = String(Math.round(v)); } });
      await wait(1800);
    }, 0.2);
  };

  /* The trap: scrollytelling */
  const initTrap = () => {
    const phone = $("#trapPhone");
    const steps = $$(".step");
    const clock = $("#trapClock");
    const ring = $("#trapClockRing");
    if (!phone || !steps.length) return;
    let clockToken = 0;

    const runClock = () => {
      clockToken += 1;
      const mine = clockToken;
      const showSeconds = (total) => {
        const minutes = Math.floor(total / 60);
        const seconds = Math.floor(total % 60);
        clock.textContent = `${minutes}:${String(seconds).padStart(2, "0")}`;
        ring.style.strokeDashoffset = String(100 - (total / 3600) * 100);
      };
      tween({ from: 0, to: 2400, duration: 3200, alive: () => mine === clockToken, onUpdate: showSeconds });
    };

    const setState = (state) => {
      if (phone.dataset.state === state) return;
      phone.dataset.state = state;
      steps.forEach((s) => s.classList.toggle("is-current", s.dataset.step === state));
      if (state === "clock") runClock();
    };

    steps[0].classList.add("is-current");
    const mobile = window.matchMedia("(max-width: 860px)").matches;
    const io = new IntersectionObserver((entries) => {
      entries.forEach((entry) => { if (entry.isIntersecting) setState(entry.target.dataset.step); });
    }, { rootMargin: mobile ? "-60% 0px -25% 0px" : "-45% 0px -45% 0px" });
    steps.forEach((s) => io.observe(s));
  };

  /* Calculator: the exact maths from OnboardingModel.kt */
  const SLIDER_MINUTES = [15, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360];
  const SLIDER_EMOJI = ["😌", "🙂", "🙂", "😐", "😕", "😟", "😩", "😵", "🥴", "😵‍💫", "😵‍💫"];
  const SHARES = {
    ALMOST_ALL: { label: "Almost all of it", fraction: 0.8 },
    ABOUT_HALF: { label: "About half", fraction: 0.5 },
    A_LITTLE: { label: "A little", fraction: 0.25 },
    NOT_SURE: { label: "Not sure", fraction: 0.6 }
  };
  const AGE_MID = { UNDER_18: 16, AGE_18_24: 21, AGE_25_34: 30, AGE_35_PLUS: 42 };
  const DEFAULT_AGE = 22;
  const LIFE_EXPECTANCY = 78;
  const WAKING_HOURS = 16;
  const RECLAIM_FACTOR = 0.5;
  const MOTIVATIONS = ["FRIENDS_FAMILY", "SLEEP", "GYM", "STUDY", "READING", "SIDE_PROJECT"];
  const UNITS = {
    FRIENDS_FAMILY: { emoji: "🫂", hours: 2, period: "YEAR", phrase: "evenings with friends a year", singular: "evening with friends a year" },
    SLEEP: { emoji: "😴", hours: 1, period: "MONTH", phrase: "extra hours of sleep a month", singular: "extra hour of sleep a month" },
    GYM: { emoji: "🏋️", hours: 1, period: "MONTH", phrase: "gym sessions a month", singular: "gym session a month" },
    STUDY: { emoji: "📚", hours: 1.5, period: "MONTH", phrase: "focused study sessions a month", singular: "focused study session a month" },
    READING: { emoji: "📖", hours: 6, period: "YEAR", phrase: "books a year", singular: "book a year" },
    SIDE_PROJECT: { emoji: "🚀", hours: 3, period: "MONTH", phrase: "build sessions a month", singular: "build session a month" }
  };
  const APPS = ["INSTAGRAM", "YOUTUBE", "SNAPCHAT", "TIKTOK"];
  const APP_LABEL = { INSTAGRAM: "Instagram", YOUTUBE: "YouTube", SNAPCHAT: "Snapchat", TIKTOK: "TikTok" };
  const KEEPABLE = { INSTAGRAM: ["DMS", "SEARCH", "PROFILES"], YOUTUBE: ["SUBSCRIPTIONS", "SEARCH"], SNAPCHAT: ["DMS", "SEARCH"], TIKTOK: ["DMS", "SEARCH"] };
  const SURFACE_PHRASE = { DMS: "DMs", SEARCH: "search", SUBSCRIPTIONS: "subscriptions", PROFILES: "profiles" };
  const BLOCKED_PHRASE = { INSTAGRAM: "Instagram Reels", YOUTUBE: "YouTube Shorts", SNAPCHAT: "Snapchat Spotlight", TIKTOK: "the TikTok feed" };
  const SCHEDULE_PHRASE = { LATE_NIGHT: "after 9 PM", MORNING: "before 10 AM", WORK_HOURS: "from 9 AM to 6 PM", ALL_DAY: "all day" };
  const DEFAULT_KEEP = ["DMS", "SEARCH"];

  const roundOneDecimal = (v) => Math.round(v * 10) / 10;
  const oneDecimal = (v) => v.toFixed(1);
  const formatMinutes = (minutes) => {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    if (hours === 0) return `${rest} m`;
    if (rest === 0) return `${hours} h`;
    return `${hours} h ${rest} m`;
  };

  const equivalentFor = (motivation, weeklyHours) => {
    const unit = UNITS[motivation];
    const hoursInPeriod = unit.period === "MONTH" ? weeklyHours * 52 / 12 : weeklyHours * 52;
    const count = Math.max(1, Math.floor(hoursInPeriod / unit.hours));
    return { emoji: unit.emoji, text: `${count} ${count === 1 ? unit.singular : unit.phrase}` };
  };

  const projectionFor = ({ minutes, share, ageBand, motivations }) => {
    const shortForm = Math.round(minutes * SHARES[share].fraction);
    const yearlyHoursExact = shortForm * 365 / 60;
    const weeklyHours = shortForm * 7 * RECLAIM_FACTOR / 60;
    const age = ageBand ? AGE_MID[ageBand] : DEFAULT_AGE;
    const chosen = MOTIVATIONS.filter((m) => motivations.includes(m)).slice(0, 2);
    const picked = [...new Set([...chosen, "FRIENDS_FAMILY", "SLEEP"])].slice(0, 2);
    return {
      shortForm,
      yearlyDays: Math.round(yearlyHoursExact / 24),
      lifetimeYears: roundOneDecimal(shortForm / 60 * Math.max(0, LIFE_EXPECTANCY - age) / WAKING_HOURS),
      reclaimedWeeklyHours: roundOneDecimal(weeklyHours),
      equivalents: picked.map((m) => equivalentFor(m, weeklyHours)),
      age
    };
  };

  const naturalList = (items) => items.length <= 1 ? items.join("") : `${items.slice(0, -1).join(", ")} and ${items[items.length - 1]}`;
  const clauseList = (items) => items.length <= 1 ? items.join("") : `${items.slice(0, -1).join(", ")}, and ${items[items.length - 1]}`;

  const intentFor = ({ apps, time }) => {
    const chosen = APPS.filter((a) => (apps.length ? apps : ["INSTAGRAM", "YOUTUBE"]).includes(a));
    const keepClauses = chosen
      .map((app) => {
        const kept = KEEPABLE[app].filter((s) => DEFAULT_KEEP.includes(s)).map((s) => SURFACE_PHRASE[s]);
        return kept.length ? `my ${APP_LABEL[app]} ${naturalList(kept)}` : null;
      })
      .filter(Boolean);
    const block = `${naturalList(chosen.map((a) => BLOCKED_PHRASE[a]))} ${SCHEDULE_PHRASE[time] || "all day"}.`;
    return keepClauses.length ? `Keep ${clauseList(keepClauses)}, but block ${block}` : `Block ${block}`;
  };

  const shareTextFor = (projection) =>
    `Reels were set to take ${oneDecimal(projection.lifetimeYears)} years of my waking life. I'm choosing to be present. #PresentApp`;

  const initCalculator = () => {
    const form = $("#calcForm");
    const out = $("#calcOut");
    if (!form || !out) return;
    const slider = $("#minutes");
    const minutesOut = $("#minutesOut");
    const emoji = $("#minutesEmoji");
    const numbers = {
      daily: { el: $("#outDaily"), value: 0, format: (v) => formatMinutes(Math.round(v)) },
      days: { el: $("#outDays"), value: 0, format: (v) => `${Math.round(v)} days` },
      years: { el: $("#outYears"), value: 0, format: (v) => `${oneDecimal(v)} years` },
      weekly: { el: $("#outWeekly"), value: 0, format: (v) => `${oneDecimal(v)} h` }
    };
    let revealed = reduced;
    let lastEquivalents = "";
    let liveTimer = 0;
    let currentProjection = null;

    const groupValues = (name) => $$(`[data-group="${name}"] .chip[aria-pressed="true"]`).map((c) => c.dataset.value);
    const readAnswers = () => ({
      apps: groupValues("apps"),
      minutes: SLIDER_MINUTES[Number(slider.value)],
      share: (form.querySelector('input[name="share"]:checked') || { value: "NOT_SURE" }).value,
      time: groupValues("time")[0] || null,
      ageBand: groupValues("age")[0] || null,
      motivations: groupValues("motivations")
    });

    const animateNumber = (key, target, delay = 0) => {
      const n = numbers[key];
      const from = n.value;
      n.value = target;
      if (!revealed) {
        n.el.textContent = n.format(target);
        return;
      }
      const token = (n.token || 0) + 1;
      n.token = token;
      const run = () => tween({ from, to: target, duration: 700, alive: () => n.token === token, onUpdate: (v) => { n.el.textContent = n.format(v); } });
      if (delay && !reduced) setTimeout(run, delay);
      else run();
    };

    const render = ({ fromZero = false } = {}) => {
      const answers = readAnswers();
      const index = Number(slider.value);
      const label = index === SLIDER_MINUTES.length - 1 ? `${formatMinutes(SLIDER_MINUTES[index])}+` : formatMinutes(SLIDER_MINUTES[index]);
      minutesOut.textContent = label;
      slider.setAttribute("aria-valuetext", label);
      if (emoji.textContent !== SLIDER_EMOJI[index]) {
        emoji.textContent = SLIDER_EMOJI[index];
        if (!reduced) {
          emoji.classList.remove("bump");
          void emoji.offsetWidth;
          emoji.classList.add("bump");
          setTimeout(() => emoji.classList.remove("bump"), 220);
        }
      }

      const p = projectionFor(answers);
      currentProjection = p;
      if (fromZero) Object.values(numbers).forEach((n) => { n.value = 0; });
      animateNumber("daily", p.shortForm, fromZero ? 0 : 0);
      animateNumber("days", p.yearlyDays, fromZero ? 350 : 0);
      animateNumber("years", p.lifetimeYears, fromZero ? 700 : 0);
      animateNumber("weekly", p.reclaimedWeeklyHours, fromZero ? 1000 : 0);
      $("#barToday").textContent = `${formatMinutes(p.shortForm)} / day`;
      $("#barPresent").textContent = `${formatMinutes(Math.round(p.shortForm * (1 - RECLAIM_FACTOR)))} / day`;

      const equivKey = p.equivalents.map((e) => e.text).join("|");
      if (equivKey !== lastEquivalents) {
        lastEquivalents = equivKey;
        const list = $("#outEquiv");
        list.replaceChildren(...p.equivalents.map((e, i) => {
          const li = document.createElement("li");
          const icon = document.createElement("span");
          icon.setAttribute("aria-hidden", "true");
          icon.textContent = e.emoji;
          li.append(icon, ` ${e.text}`);
          if (revealed && !reduced) {
            li.classList.add("pop");
            li.style.animationDelay = `${i * 80}ms`;
          }
          return li;
        }));
      }

      $("#outIntent").textContent = intentFor(answers);
      const shareLabel = SHARES[answers.share].label.toLowerCase();
      const formulas = [
        `Short-form a day = ${label} × Reels/Shorts share (${shareLabel}, ${SHARES[answers.share].fraction}) = ${formatMinutes(p.shortForm)}`,
        `Days a year = daily minutes × 365 ÷ 60 ÷ 24 ≈ ${p.yearlyDays}`,
        `Waking years = (minutes ÷ 60 ÷ 16 waking hours) × (78 − age ${p.age}) = ${oneDecimal(p.lifetimeYears)}`,
        `Time back = daily minutes × 7 × 50% ÷ 60 = ${oneDecimal(p.reclaimedWeeklyHours)} h a week (conservative; the one sec study measured 57% fewer opens)`,
        "Equivalents: evening with friends 2 h · extra sleep 1 h · gym session 1 h · study block 1.5 h · book 6 h · build session 3 h"
      ];
      $("#formulaList").replaceChildren(...formulas.map((f) => {
        const li = document.createElement("li");
        li.textContent = f;
        return li;
      }));

      clearTimeout(liveTimer);
      liveTimer = setTimeout(() => {
        $("#calcLive").textContent = `Estimate: ${formatMinutes(p.shortForm)} a day on Reels and Shorts, ${p.yearlyDays} days a year, ${oneDecimal(p.lifetimeYears)} years of your waking life. With Present, about ${oneDecimal(p.reclaimedWeeklyHours)} hours back every week.`;
      }, 700);
    };

    form.addEventListener("click", (event) => {
      const chip = event.target.closest(".chip");
      if (!chip) return;
      const group = chip.closest(".chips");
      const pressed = chip.getAttribute("aria-pressed") === "true";
      if (group.dataset.mode === "single" && !pressed) {
        $$(".chip", group).forEach((c) => c.setAttribute("aria-pressed", "false"));
      }
      chip.setAttribute("aria-pressed", String(!pressed));
      if (!reduced) {
        chip.classList.remove("bounce");
        void chip.offsetWidth;
        chip.classList.add("bounce");
      }
      render();
    });
    form.addEventListener("input", () => render());
    form.addEventListener("submit", (event) => event.preventDefault());

    render();
    if (!reduced) {
      observeVisibility(out, (visible) => {
        if (!visible || revealed) return;
        revealed = true;
        render({ fromZero: true });
      }, 0.25);
    }

    const shareBtn = $("#shareBtn");
    const shareMsg = $("#shareMsg");
    const flash = (text) => {
      shareMsg.textContent = text;
      setTimeout(() => { if (shareMsg.textContent === text) shareMsg.textContent = ""; }, 3500);
    };
    const copyText = async (text) => {
      try {
        await navigator.clipboard.writeText(text);
        return true;
      } catch {
        const area = document.createElement("textarea");
        area.value = text;
        area.setAttribute("readonly", "");
        area.style.position = "fixed";
        area.style.opacity = "0";
        document.body.append(area);
        area.select();
        const ok = document.execCommand && document.execCommand("copy");
        area.remove();
        return Boolean(ok);
      }
    };
    shareBtn.addEventListener("click", async () => {
      const text = shareTextFor(currentProjection);
      const url = window.location.href.split("#")[0];
      const data = { title: "Present", text, url };
      if (navigator.share && (!navigator.canShare || navigator.canShare(data))) {
        try {
          await navigator.share(data);
          return;
        } catch (error) {
          if (error && error.name === "AbortError") return;
        }
      }
      const ok = await copyText(`${text} ${url}`);
      flash(ok ? "Copied to clipboard ✓" : "Couldn't copy. Select the numbers and share them yourself.");
    });
  };

  /* How it works */
  const INTENT = "Keep my Instagram DMs and YouTube subscriptions, but kill Reels and Shorts after 9 PM.";
  const POLICY_JSON = `{
  "policy_version": "1",
  "summary": "Reels and Shorts off after 9 PM.",
  "rules": [{
    "platform": "instagram",
    "target_surfaces": ["reels_tab"],
    "allowed_surfaces": ["direct_messages", "search"],
    "schedule": { "mode": "time_window",
      "start_time": "21:00", "end_time": "06:00" }
  }, {
    "platform": "youtube",
    "target_surfaces": ["shorts_player"],
    "allowed_surfaces": ["subscriptions", "search"],
    "schedule": { "mode": "time_window",
      "start_time": "21:00", "end_time": "06:00" }
  }]
}`;

  const initHow = () => {
    const typed = $("#typedText");
    const compileBtn = $("#compileBtn");
    const jsonOut = $("#jsonOut");
    const jsonDemo = $(".demo--json");
    const miniApp = $("#miniApp");
    const tapDot = $("#tapDot");

    if (reduced) {
      typed.textContent = INTENT;
      jsonOut.textContent = POLICY_JSON;
      jsonDemo.classList.add("show-chips");
      miniApp.dataset.view = "dms";
      miniApp.classList.add("toast-allow");
      return;
    }

    loopWhileVisible($("#howSay"), async (alive) => {
      typed.textContent = "";
      compileBtn.classList.remove("is-pressed");
      await wait(500);
      for (let i = 1; i <= INTENT.length; i += 1) {
        if (!alive()) return;
        typed.textContent = INTENT.slice(0, i);
        await wait(INTENT[i - 1] === " " ? 55 : 32);
      }
      await wait(500);
      compileBtn.classList.add("is-pressed");
      await wait(350);
      compileBtn.classList.remove("is-pressed");
      await wait(2600);
    });

    loopWhileVisible($("#howCompile"), async (alive) => {
      jsonDemo.classList.remove("show-chips");
      jsonOut.textContent = "";
      await wait(600);
      for (let i = 0; i <= POLICY_JSON.length; i += 6) {
        if (!alive()) return;
        jsonOut.textContent = POLICY_JSON.slice(0, i);
        await wait(16);
      }
      jsonOut.textContent = POLICY_JSON;
      await wait(900);
      if (!alive()) return;
      jsonDemo.classList.add("show-chips");
      await wait(4200);
    });

    const moveTap = (target) => {
      const appBox = miniApp.getBoundingClientRect();
      const box = target.getBoundingClientRect();
      tapDot.style.left = `${box.left - appBox.left + box.width / 2 - 13}px`;
      tapDot.style.top = `${box.top - appBox.top + box.height / 2 - 13}px`;
    };
    const press = async () => {
      tapDot.classList.add("press");
      await wait(160);
      tapDot.classList.remove("press");
    };

    loopWhileVisible($("#howEnforce"), async (alive) => {
      miniApp.dataset.view = "home";
      miniApp.classList.remove("toast-block", "toast-allow");
      tapDot.classList.remove("on");
      await wait(700);
      if (!alive()) return;
      moveTap($(".nav-reels", miniApp));
      tapDot.classList.add("on");
      await wait(650);
      await press();
      miniApp.dataset.view = "reels";
      await wait(140);
      miniApp.dataset.view = "home";
      miniApp.classList.add("toast-block");
      await wait(1800);
      if (!alive()) return;
      miniApp.classList.remove("toast-block");
      moveTap($(".nav-dms", miniApp));
      await wait(650);
      await press();
      miniApp.dataset.view = "dms";
      miniApp.classList.add("toast-allow");
      tapDot.classList.remove("on");
      await wait(2600);
    });
  };

  /* App screens carousel */
  const CAPTIONS = [
    "Seven quick questions. One decision per screen.",
    "The mirror: your daily minutes, projected across your waking life.",
    "The upside: a conservative 50% cut, turned into real-world time.",
    "Gemini writes your plan, then compiles it into green and red chips. Nothing changes until you tap Activate.",
    "Home: time reclaimed today, your intent, and today's real-world quest.",
    "The turn-off guard: a countdown and a real person to reach out to."
  ];
  const SLIDE_MS = [6000, 7000, 6000, 7000, 6000, 8000];
  const MAX_VIDEO_SLIDE_MS = 24000;

  const initCarousel = () => {
    const carousel = $("#carousel");
    const track = $("#carTrack");
    if (!carousel || !track) return;
    const tabs = $$('[role="tab"]', carousel);
    const slides = $$(".slide", track);
    const caption = $("#carCaption");
    let current = 0;
    let autoplay = !reduced;
    let hovering = false;
    let visible = false;
    let timer = 0;
    let guardTimer = 0;
    let slideToken = 0;

    const runSlideScripts = (slide) => {
      slideToken += 1;
      const mine = slideToken;
      clearInterval(guardTimer);
      $$("[data-count]", slide).forEach((el, i) => {
        const target = Number(el.dataset.count);
        const formats = { min: (v) => formatMinutes(Math.round(v)), days: (v) => `${Math.round(v)} days`, years: (v) => `${oneDecimal(v)} years`, hours: (v) => `${oneDecimal(v)} h` };
        const format = formats[el.dataset.format] || formats.years;
        el.textContent = format(reduced ? target : 0);
        if (!reduced) setTimeout(() => tween({ from: 0, to: target, duration: 1100, alive: () => mine === slideToken, onUpdate: (v) => { el.textContent = format(v); } }), i * 900);
      });
      const count = $(".g-count", slide);
      if (count) {
        let remaining = 15;
        count.textContent = String(remaining);
        if (!reduced) {
          guardTimer = setInterval(() => {
            remaining = Math.max(0, remaining - 1);
            count.textContent = remaining === 0 ? "🌅" : String(remaining);
            if (remaining === 0) clearInterval(guardTimer);
          }, 1000);
        }
      }
    };

    const setActive = (index) => {
      if (index === current && slides[index].classList.contains("is-active")) return;
      current = index;
      tabs.forEach((tab, i) => {
        tab.setAttribute("aria-selected", String(i === index));
        tab.tabIndex = i === index ? 0 : -1;
      });
      slides.forEach((slide, i) => {
        slide.classList.remove("is-active");
        slide.setAttribute("aria-hidden", String(i !== index));
      });
      void slides[index].offsetWidth;
      slides[index].classList.add("is-active");
      caption.textContent = CAPTIONS[index];
      const tabList = tabs[index].parentElement;
      if (tabList.scrollWidth > tabList.clientWidth) {
        tabList.scrollTo({ left: tabs[index].offsetLeft - 20, behavior: reduced ? "auto" : "smooth" });
      }
      const video = activeVideo(index);
      if (video && !reduced) {
        video.currentTime = 0;
        video.play().catch(() => {});
      }
      runSlideScripts(slides[index]);
      schedule();
    };

    const goTo = (index) => {
      const target = (index + slides.length) % slides.length;
      track.scrollTo({ left: target * track.clientWidth, behavior: reduced ? "auto" : "smooth" });
      setActive(target);
    };

    const activeVideo = (index) => (slides[index].classList.contains("has-video") ? $("video", slides[index]) : null);

    const slideDuration = (index) => {
      const video = activeVideo(index);
      return video && video.duration ? Math.min(video.duration * 1000, MAX_VIDEO_SLIDE_MS) : SLIDE_MS[index];
    };

    const schedule = () => {
      clearTimeout(timer);
      const playing = autoplay && visible && !hovering;
      const ms = slideDuration(current);
      carousel.classList.toggle("is-playing", playing);
      carousel.style.setProperty("--slide-ms", `${ms}ms`);
      if (playing) timer = setTimeout(() => goTo(current + 1), ms);
    };

    const stopAutoplay = () => {
      autoplay = false;
      schedule();
    };

    tabs.forEach((tab, i) => tab.addEventListener("click", () => { stopAutoplay(); goTo(i); }));
    $(".carousel__tabs", carousel).addEventListener("keydown", (event) => {
      const keys = { ArrowRight: current + 1, ArrowLeft: current - 1, Home: 0, End: slides.length - 1 };
      if (!(event.key in keys)) return;
      event.preventDefault();
      stopAutoplay();
      const next = (keys[event.key] + slides.length) % slides.length;
      goTo(next);
      tabs[next].focus();
    });
    $("#carPrev").addEventListener("click", () => { stopAutoplay(); goTo(current - 1); });
    $("#carNext").addEventListener("click", () => { stopAutoplay(); goTo(current + 1); });

    let scrollTimer = 0;
    track.addEventListener("scroll", () => {
      clearTimeout(scrollTimer);
      scrollTimer = setTimeout(() => {
        const index = Math.round(track.scrollLeft / track.clientWidth);
        if (index !== current) setActive(clamp(index, 0, slides.length - 1));
      }, 90);
    }, { passive: true });
    track.addEventListener("touchstart", stopAutoplay, { passive: true });
    track.addEventListener("wheel", (event) => { if (Math.abs(event.deltaX) > Math.abs(event.deltaY)) stopAutoplay(); }, { passive: true });

    carousel.addEventListener("pointerenter", () => { hovering = true; schedule(); });
    carousel.addEventListener("pointerleave", () => { hovering = false; schedule(); });
    carousel.addEventListener("focusin", () => { hovering = true; schedule(); });
    carousel.addEventListener("focusout", () => { hovering = false; schedule(); });
    window.addEventListener("resize", () => { track.scrollLeft = current * track.clientWidth; });

    observeVisibility(carousel, (isVisible) => {
      visible = isVisible;
      if (visible) runSlideScripts(slides[current]);
      schedule();
    }, 0.4);

    slides.forEach((slide, i) => slide.setAttribute("aria-hidden", String(i !== 0)));
    runSlideScripts(slides[0]);

  };


  /* Real screen recordings replace the HTML recreations when listed in media/videos.json */
  const initVideos = () => {
    const hosts = $$("[data-video]");
    if (!hosts.length) return;
    fetch("media/videos.json", { cache: "no-cache" })
      .then((res) => (res.ok ? res.json() : { available: [] }))
      .then(({ available = [] }) => {
        const attachPoster = (host, poster) => {
          const img = document.createElement("img");
          img.className = "media-swap";
          img.alt = "";
          img.decoding = "async";
          img.addEventListener("load", () => host.classList.add("has-video"), { once: true });
          img.addEventListener("error", () => img.remove(), { once: true });
          img.src = poster;
          host.append(img);
        };

        const attachVideo = (host, src, poster) => {
          const video = document.createElement("video");
          video.className = "media-swap";
          video.muted = true;
          video.defaultMuted = true;
          video.loop = true;
          video.autoplay = true;
          video.playsInline = true;
          video.preload = "metadata";
          video.setAttribute("muted", "");
          video.setAttribute("playsinline", "");
          video.setAttribute("aria-hidden", "true");
          if (poster) video.poster = poster;
          video.addEventListener("loadeddata", () => host.classList.add("has-video"), { once: true });
          video.addEventListener("error", () => { host.classList.remove("has-video"); video.remove(); }, { once: true });
          video.src = src;
          host.append(video);
          if (poster) host.classList.add("has-video");
          observeVisibility(video, (isVisible) => {
            if (isVisible) video.play().catch(() => {});
            else video.pause();
          }, 0.5);
        };

        const lazyIO = new IntersectionObserver((entries) => {
          entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            lazyIO.unobserve(entry.target);
            const { src, poster } = entry.target.mediaSwap;
            attachVideo(entry.target, src, poster);
          });
        }, { rootMargin: "300px 0px" });

        hosts.forEach((host) => {
          const src = host.dataset.video;
          const name = src ? src.split("/").pop() : "";
          if (!name || !available.includes(name)) return;
          const posterName = name.replace(/\.mp4$/, ".jpg");
          const poster = available.includes(posterName) ? src.replace(/\.mp4$/, ".jpg") : "";
          if (reduced) {
            if (poster) attachPoster(host, poster);
            return;
          }
          host.mediaSwap = { src, poster };
          lazyIO.observe(host);
        });
      })
      .catch(() => {});
  };

  /* Turn-off guard demo */
  const QUESTS = [
    { emoji: "📞", title: "Call someone you miss", detail: "A 5-minute call beats 50 Reels. Who haven't you talked to this week?", action: "DIAL" },
    { emoji: "💬", title: "Make a plan with a friend", detail: "Text someone: \"Chai / coffee later?\" Real plans, real people.", action: "MESSAGE" },
    { emoji: "🗣️", title: "Talk to someone nearby", detail: "Walk over and ask a family member, roommate or colleague how their day is going.", action: "NONE" },
    { emoji: "🙏", title: "Thank someone", detail: "Send a thank-you to someone who helped you recently.", action: "MESSAGE" },
    { emoji: "🚶", title: "Step outside for 10 minutes", detail: "Leave the phone in your pocket. Notice three things around you.", action: "NONE" },
    { emoji: "🍽️", title: "Phone-free meal", detail: "Eat your next meal with someone, phone face-down on the table.", action: "NONE" }
  ];
  const countdownFor = (turnOffsToday) => Math.min(15 * Math.pow(2, clamp(turnOffsToday, 0, 2)), 60);

  const initGuard = () => {
    const root = $("#guardDemo");
    if (!root) return;
    const sw = $("#gdSwitch");
    const guard = $("#gdGuard");
    const state = { status: "on", turnOffs: 0, quest: 0, accepted: 0, remaining: 0, total: 15 };
    let interval = 0;

    const renderStatus = () => {
      const on = state.status === "on";
      sw.classList.toggle("switch--on", on);
      sw.setAttribute("aria-checked", String(on));
      $("#gdDot").className = `pulse${state.status === "off" ? " is-off" : state.status === "paused" ? " is-paused" : ""}`;
      const resume = new Date(Date.now() + 15 * 60 * 1000).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
      const labels = {
        on: ["Protected", "Reels & Shorts blocked · DMs kept"],
        paused: ["Paused", `Resumes at ${resume}`],
        off: ["Protection off", "Short-form feeds are open"]
      };
      $("#gdStatus").textContent = labels[state.status][0];
      $("#gdStatusSub").textContent = labels[state.status][1];
      $("#gdNote").textContent = `Turn-offs today: ${state.turnOffs}. Next wait: ${countdownFor(state.turnOffs)} seconds.`;
    };

    const renderQuest = () => {
      const q = QUESTS[state.quest % QUESTS.length];
      $("#gdQuestTitle").textContent = `${q.emoji} ${q.title}`;
      $("#gdQuestDetail").textContent = q.detail;
      $("#gdDo").textContent = q.action === "DIAL" ? "📞 Call now" : q.action === "MESSAGE" ? "💬 Message now" : "✓ I'll do it";
    };

    const renderCount = () => {
      const done = state.remaining === 0;
      $("#gdCount").textContent = done ? "🌅" : String(state.remaining);
      $("#gdCountLabel").hidden = done;
      $("#gdRing").style.strokeDashoffset = String(100 - (state.remaining / state.total) * 100);
      $("#gdWait").hidden = done;
      $("#gdWait").textContent = `You can switch off in ${state.remaining}s`;
      $("#gdOff").hidden = !done;
    };

    const closeGuard = () => {
      clearInterval(interval);
      guard.hidden = true;
      root.dataset.mode = "home";
      renderStatus();
      sw.focus();
    };

    const openGuard = () => {
      state.total = countdownFor(state.turnOffs);
      state.remaining = state.total;
      $("#gdSub").textContent = state.turnOffs > 0
        ? `This is turn-off #${state.turnOffs + 1} today. The feed will still be there in an hour. Will the people around you?`
        : "The feed will still be there in an hour. Will the people around you?";
      $("#gdReason").value = "";
      $("#gdTurnOff").disabled = true;
      $("#gdPraise").hidden = true;
      renderQuest();
      renderCount();
      root.dataset.mode = "guard";
      guard.hidden = false;
      guard.scrollTop = 0;
      $("#gdTitle").focus({ preventScroll: true });
      clearInterval(interval);
      interval = setInterval(() => {
        state.remaining = Math.max(0, state.remaining - 1);
        renderCount();
        if (state.remaining === 0) clearInterval(interval);
      }, 1000);
    };

    const keepOn = () => {
      state.accepted += 1;
      state.status = "on";
      const praise = $("#gdPraise");
      praise.textContent = `🌱 You've chosen real life over the feed ${state.accepted} ${state.accepted === 1 ? "time" : "times"}.`;
      praise.hidden = false;
      closeGuard();
    };

    sw.addEventListener("click", () => {
      if (state.status === "on") {
        openGuard();
        return;
      }
      state.status = "on";
      renderStatus();
    });
    $("#gdKeep").addEventListener("click", keepOn);
    $("#gdDo").addEventListener("click", keepOn);
    $("#gdPause").addEventListener("click", () => { state.status = "paused"; closeGuard(); });
    $("#gdShuffle").addEventListener("click", () => {
      state.quest += 1;
      renderQuest();
      const card = $(".quest-card", guard);
      if (!reduced) {
        card.style.animation = "none";
        void card.offsetWidth;
        card.style.animation = "flipIn .5s var(--ease) both";
      }
    });
    $("#gdReason").addEventListener("input", (event) => {
      $("#gdTurnOff").disabled = event.target.value.trim().length < 10;
    });
    $("#gdTurnOff").addEventListener("click", () => {
      state.turnOffs += 1;
      state.status = "off";
      closeGuard();
    });
    guard.addEventListener("keydown", (event) => { if (event.key === "Escape") keepOn(); });
    renderStatus();
  };

  initHero();
  initWelcome();
  initTrap();
  initCalculator();
  initHow();
  initCarousel();
  initGuard();
  initVideos();
})();
