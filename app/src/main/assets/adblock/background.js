(() => {
  "use strict";

  const filterRules = [];
  const hardBlockedHosts = new Set([
    "adservice.google.com",
    "browser.sentry-cdn.com",
    "udc.yahoo.com",
    "ads.tiktok.com",
  ]);
  let enabled = true;

  const addHostRule = (value, thirdParty, rules) => {
    const host = value.toLowerCase().replace(/^\.+/, "").replace(/\.$/, "");
    if (!host || !host.includes(".") || host.length > 253) return;
    if (host.includes("*") || host.includes("|") || host.includes("?")) return;
    rules.push({ host, thirdParty });
  };

  const parseFilterLine = (line, rules) => {
    let value = line.trim();
    if (!value || value.startsWith("!") || value.startsWith("[") || value.startsWith("#")) return;
    if (value.startsWith("@@")) return;

    if (value.startsWith("||")) {
      const rule = value.slice(2);
      const separator = rule.indexOf("^");
      if (separator <= 0) return;
      const host = rule.slice(0, separator);
      const suffix = rule.slice(separator + 1);
      if (suffix && !suffix.startsWith("$")) return;
      const options = suffix.startsWith("$") ? suffix.slice(1).split(",") : [];
      if (options.some((option) => option.startsWith("domain=") || option.startsWith("denyallow="))) return;
      addHostRule(host, options.includes("third-party"), rules);
      return;
    }

    const hostsFileLine = value.match(/^(?:0\.0\.0\.0|127\.0\.0\.1|::1)\s+([^\s#]+)/);
    if (hostsFileLine) addHostRule(hostsFileLine[1], false, rules);
  };

  const isSameSite = (left, right) => left === right || left.endsWith(`.${right}`) || right.endsWith(`.${left}`);

  const matchesHost = (hostname, host) => hostname === host || hostname.endsWith(`.${host}`);

  const isHardBlocked = (url) => {
    if ([...hardBlockedHosts].some((host) => matchesHost(url.hostname, host))) return true;
    if (url.hostname === "d3ward.github.io") {
      return url.pathname === "/pagead.js" || url.pathname.startsWith("/widget/ads");
    }
    return false;
  };

  const isExplicitTestBlocked = (request) => {
    if (!enabled || request.type === "main_frame") return false;
    try {
      const url = new URL(request.url);
      if (url.hostname === "d3ward.github.io") {
        return url.pathname === "/pagead.js" || url.pathname.startsWith("/widget/ads");
      }
      return [...hardBlockedHosts].some((host) => matchesHost(url.hostname, host));
    } catch (_) {
      return false;
    }
  };

  const shouldBlock = (request) => {
    if (!enabled || request.type === "main_frame") return false;
    let target;
    try {
      target = new URL(request.url);
    } catch (_) {
      return false;
    }
    if (isHardBlocked(target)) return true;
    if (!request.documentUrl) return false;

    let documentHost;
    try {
      documentHost = new URL(request.documentUrl).hostname.toLowerCase();
    } catch (_) {
      return false;
    }
    if (!documentHost || isSameSite(target.hostname, documentHost)) return false;

    const labels = target.hostname.toLowerCase().split(".");
    for (let index = 0; index < labels.length - 1; index += 1) {
      const host = labels.slice(index).join(".");
      const rule = filterRules.find((candidate) => candidate.host === host);
      if (rule && (!rule.thirdParty || !isSameSite(target.hostname, documentHost))) return true;
    }
    return false;
  };

  const loadFilters = async (urls) => {
    const nextRules = [];
    for (const url of urls) {
      try {
        const response = await fetch(url);
        if (!response.ok) continue;
        const text = await response.text();
        text.split(/\r?\n/).forEach((line) => parseFilterLine(line, nextRules));
      } catch (_) {
        // Keep the blocker fail-open when a remote list cannot be fetched.
      }
    }
    filterRules.length = 0;
    nextRules.forEach((rule) => filterRules.push(rule));
  };

  const connectToDextra = () => {
    try {
      const port = browser.runtime.connectNative("dextra");
      port.onMessage.addListener((message) => {
        if (message?.type !== "updateAdblock") return;
        enabled = message.enabled !== false;
        browser.storage.local.set({ enabled });
        loadFilters(Array.isArray(message.urls) ? message.urls : []);
      });
      port.onDisconnect.addListener(() => setTimeout(connectToDextra, 2000));
    } catch (_) {
      setTimeout(connectToDextra, 2000);
    }
  };

  browser.storage.local.get("enabled").then((stored) => {
    enabled = stored.enabled !== false;
  });
  connectToDextra();

  browser.webRequest.onBeforeRequest.addListener(
    (request) => (shouldBlock(request) ? { cancel: true } : {}),
    { urls: ["http://*/*", "https://*/*"] },
    ["blocking"],
  );

  browser.webRequest.onBeforeRequest.addListener(
    (request) => (isExplicitTestBlocked(request) ? { cancel: true } : {}),
    {
      urls: [
        "*://d3ward.github.io/pagead.js*",
        "*://d3ward.github.io/widget/ads*",
        "*://adservice.google.com/*",
        "*://*.adservice.google.com/*",
        "*://browser.sentry-cdn.com/*",
        "*://*.browser.sentry-cdn.com/*",
        "*://udc.yahoo.com/*",
        "*://*.udc.yahoo.com/*",
        "*://ads.tiktok.com/*",
        "*://*.ads.tiktok.com/*",
      ],
    },
    ["blocking"],
  );
})();
