(() => {
  "use strict";

  const filterRules = new Map();
  const userScripts = [];
  const injectedScripts = new Set();
  let filterLoadGeneration = 0;
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
    rules.set(host, { host, thirdParty });
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
      const rule = filterRules.get(host);
      if (rule && (!rule.thirdParty || !isSameSite(target.hostname, documentHost))) return true;
    }
    return false;
  };

  const loadFilters = async (urls) => {
    const generation = ++filterLoadGeneration;
    const nextRules = new Map();
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
    if (generation !== filterLoadGeneration) return;
    filterRules.clear();
    nextRules.forEach((rule, host) => filterRules.set(host, rule));
  };

  const globToRegExp = (pattern) => {
    if (pattern === "<all_urls>") return /^https?:\/\/.+/i;
    const escaped = pattern.replace(/[.+?^${}()|[\]\\]/g, "\\$&");
    return new RegExp(`^${escaped.replace(/\*/g, ".*")}$`, "i");
  };

  const parseUserScript = (source, sourceUrl) => {
    const metadataMatch = source.match(/==UserScript==([\s\S]*?)==\/UserScript==/);
    const metadata = {
      matches: [],
      excludes: [],
      includes: [],
      requires: [],
      runAt: "document_idle",
      noframes: false,
    };
    if (metadataMatch) {
      for (const line of metadataMatch[1].split(/\r?\n/)) {
        const match = line.match(/^\s*\/\/\s*@([\w-]+)(?:\s+(.+?))?\s*$/);
        if (!match) continue;
        const key = match[1].toLowerCase();
        const value = match[2] || "";
        if (key === "match") metadata.matches.push(value);
        if (key === "exclude") metadata.excludes.push(value);
        if (key === "include") metadata.includes.push(value);
        if (key === "require" && /^https?:\/\/\S+$/i.test(value)) metadata.requires.push(value);
        if (key === "run-at" && ["document-start", "document-end", "document-idle"].includes(value)) metadata.runAt = value.replace("-", "_");
        if (key === "noframes") metadata.noframes = true;
      }
    }
    if (metadata.matches.length === 0 && metadata.includes.length === 0) metadata.matches.push("*://*/*");
    return {
      sourceUrl,
      code: source,
      matches: metadata.matches.map(globToRegExp),
      excludes: [...metadata.excludes, ...metadata.includes.filter((pattern) => pattern.startsWith("!")).map((pattern) => pattern.slice(1))].map(globToRegExp),
      includes: metadata.includes.filter((pattern) => !pattern.startsWith("!")).map(globToRegExp),
      requires: metadata.requires,
      runAt: metadata.runAt,
      allFrames: !metadata.noframes,
    };
  };

  const matchesUserScript = (script, url) => {
    const matches = script.matches.some((pattern) => pattern.test(url));
    const includes = script.includes.length === 0 || script.includes.some((pattern) => pattern.test(url));
    const excluded = script.excludes.some((pattern) => pattern.test(url));
    return matches && includes && !excluded;
  };

  const injectUserScripts = async (tabId, url, phase) => {
    if (!url || !/^https?:\/\//i.test(url)) return;
    for (const script of userScripts) {
      if (script.runAt !== phase || !matchesUserScript(script, url)) continue;
      const key = `${tabId}|${url}|${script.sourceUrl}|${phase}`;
      if (injectedScripts.has(key)) continue;
      injectedScripts.add(key);
      try {
        await browser.tabs.executeScript(tabId, {
          code: script.code,
          runAt: phase,
          allFrames: script.allFrames,
        });
      } catch (_) {
        injectedScripts.delete(key);
      }
    }
  };

  const injectIntoOpenTabs = async () => {
    injectedScripts.clear();
    try {
      const tabs = await browser.tabs.query({});
      for (const tab of tabs) {
        const phase = tab.status === "loading" ? "document_start" : "document_end";
        await injectUserScripts(tab.id, tab.url, phase);
        if (tab.status !== "loading") await injectUserScripts(tab.id, tab.url, "document_idle");
      }
    } catch (_) {
      // A tab can disappear while subscriptions are being refreshed.
    }
  };

  const loadUserScripts = async (urls) => {
    const loaded = [];
    for (const url of urls) {
      try {
        const response = await fetch(url);
        if (!response.ok) continue;
        const source = await response.text();
        const script = parseUserScript(source, url);
        const dependencies = [];
        for (const dependencyUrl of script.requires) {
          const dependencyResponse = await fetch(dependencyUrl);
          if (dependencyResponse.ok) dependencies.push(await dependencyResponse.text());
        }
        loaded.push({ ...script, code: `${dependencies.join("\n")}\n${source}` });
      } catch (_) {
        // Ignore unavailable scripts and keep the scripts that did load.
      }
    }
    userScripts.splice(0, userScripts.length, ...loaded);
    await injectIntoOpenTabs();
  };

  const connectToDextra = () => {
    try {
      const port = browser.runtime.connectNative("dextra");
      port.onMessage.addListener((message) => {
        if (message?.type === "setZoom") {
          const zoomFactor = Number(message.zoomFactor);
          if (!Number.isFinite(zoomFactor)) return;
          browser.tabs.query({ active: true, currentWindow: true }).then((tabs) => {
            const tabId = tabs[0]?.id;
            if (typeof tabId !== "number") return;
            return browser.tabs.setZoom(tabId, Math.min(2, Math.max(0.5, zoomFactor)));
          }).catch(() => {});
          return;
        }
        if (message?.type === "updateUserscripts") {
          loadUserScripts(Array.isArray(message.urls) ? message.urls : []);
          return;
        }
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

  browser.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    const url = changeInfo.url || tab.url;
    if (changeInfo.status === "loading") {
      for (const key of injectedScripts) {
        if (key.startsWith(`${tabId}|`)) injectedScripts.delete(key);
      }
      injectUserScripts(tabId, url, "document_start");
    }
    if (changeInfo.status === "complete") {
      injectUserScripts(tabId, url, "document_end");
      injectUserScripts(tabId, url, "document_idle");
    }
  });

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
