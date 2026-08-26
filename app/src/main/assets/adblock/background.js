(() => {
  "use strict";

  const blockedRules = [];
  const allowedHosts = new Set();
  let enabled = false;

  const addHost = (value, thirdParty, exception, rules, exceptions) => {
    const host = value
      .toLowerCase()
      .replace(/^\.+/, "")
      .replace(/\.$/, "");
    if (!host || host.includes("*") || host.includes("|") || host.includes("?")) return;
    if (!host.includes(".") || host.length > 253) return;
    if (exception) exceptions.add(host);
    else rules.push({ host, thirdParty });
  };

  const parseLine = (line, rules, exceptions) => {
    let value = line.trim();
    if (!value || value.startsWith("!") || value.startsWith("[") || value.startsWith("#")) return;

    const exception = value.startsWith("@@");
    if (exception) value = value.slice(2);
    if (value.startsWith("||")) {
      const rule = value.slice(2);
      const separator = rule.indexOf("^");
      if (separator <= 0) return;
      const host = rule.slice(0, separator);
      const suffix = rule.slice(separator + 1);
      if (suffix && !suffix.startsWith("$")) return;
      const options = suffix.startsWith("$") ? suffix.slice(1).split(",") : [];
      if (options.some((option) => option.startsWith("domain=") || option.startsWith("denyallow="))) return;
      addHost(host, options.includes("third-party"), exception && options.length === 0, rules, exceptions);
      return;
    }

    const hostsFileLine = value.match(/^(?:0\.0\.0\.0|127\.0\.0\.1|::1)\s+([^\s#]+)/);
    if (hostsFileLine && !exception) addHost(hostsFileLine[1], false, false, rules, exceptions);
  };

  const isSameSite = (left, right) => left === right || left.endsWith(`.${right}`) || right.endsWith(`.${left}`);

  const isBlocked = (hostname, documentUrl) => {
    if (!enabled) return false;
    const labels = hostname.toLowerCase().split(".");
    for (let index = 0; index < labels.length - 1; index += 1) {
      const host = labels.slice(index).join(".");
      if (allowedHosts.has(host)) return false;
      const rule = blockedRules.find((candidate) => candidate.host === host);
      if (!rule) continue;
      if (!rule.thirdParty) return true;
      try {
        const documentHost = new URL(documentUrl).hostname.toLowerCase();
        if (!documentHost || !isSameSite(hostname, documentHost)) return true;
      } catch (_) {
        return true;
      }
    }
    return false;
  };

  const loadFilters = async (urls) => {
    const nextRules = [];
    const nextAllowedHosts = new Set();
    for (const url of urls) {
      try {
        const response = await fetch(url);
        const text = await response.text();
        text.split(/\r?\n/).forEach((line) => parseLine(line, nextRules, nextAllowedHosts));
      } catch (_) {
        // A failed subscription is ignored so the browser remains usable.
      }
    }
    blockedRules.length = 0;
    nextRules.forEach((rule) => blockedRules.push(rule));
    allowedHosts.clear();
    nextAllowedHosts.forEach((host) => allowedHosts.add(host));
  };

  const connectToDextra = () => {
    try {
      const port = browser.runtime.connectNative("dextra");
      port.onMessage.addListener((message) => {
        if (message?.type !== "updateFilters") return;
        enabled = Boolean(message.enabled);
        loadFilters(Array.isArray(message.urls) ? message.urls : []);
      });
      port.onDisconnect.addListener(() => {
        setTimeout(connectToDextra, 2000);
      });
    } catch (_) {
      setTimeout(connectToDextra, 2000);
    }
  };

  connectToDextra();

  browser.webRequest.onBeforeRequest.addListener(
    (request) => {
      try {
        if (request.type === "main_frame") return {};
        return isBlocked(new URL(request.url).hostname, request.documentUrl || request.originUrl || "")
          ? { cancel: true }
          : {};
      } catch (_) {
        return {};
      }
    },
    { urls: ["http://*/*", "https://*/*"] },
    ["blocking"],
  );
})();
