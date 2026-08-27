(() => {
  "use strict";

  const selectionStyle = document.createElement("style");
  selectionStyle.id = "dextra-selection-style";
  selectionStyle.textContent = `
    ::selection { background: #5b57c8 !important; color: #ffffff !important; }
    ::-moz-selection { background: #5b57c8 !important; color: #ffffff !important; }
  `;
  (document.head || document.documentElement)?.appendChild(selectionStyle);

  const selectors = [".adbox.banner_ads.adsbox", ".textads"];

  const openLinkInNewTab = (event) => {
    const isMiddleClick = event.type === "auxclick" && event.button === 1;
    const isModifiedClick = event.type === "click" && event.button === 0 && (event.ctrlKey || event.metaKey);
    if (!isMiddleClick && !isModifiedClick) return;
    const target = event.target instanceof Element ? event.target.closest("a[href]") : null;
    const url = target?.href || "";
    if (!/^https?:\/\//i.test(url)) return;

    event.preventDefault();
    event.stopImmediatePropagation();
    browser.runtime.sendNativeMessage("dextra", {
      type: "openLinkInNewTab",
      url,
    }).catch(() => {});
  };

  document.addEventListener("click", openLinkInNewTab, true);
  document.addEventListener("auxclick", openLinkInNewTab, true);

  document.addEventListener("contextmenu", (event) => {
    const target = event.target instanceof Element ? event.target : null;
    const link = target?.closest("a[href]");
    const media = target?.closest("img,video,audio");
    browser.runtime.sendNativeMessage("dextra", {
      type: "contextMenu",
      x: Math.round(event.clientX),
      y: Math.round(event.clientY),
      linkUrl: link?.href || null,
      selectedText: window.getSelection()?.toString()?.trim() || null,
      textContent: link?.textContent?.trim() || target?.textContent?.trim() || null,
      resourceUri: media?.currentSrc || media?.src || null,
      resourceType: media?.tagName?.toLowerCase() || null,
    }).catch(() => {});
    event.preventDefault();
    event.stopImmediatePropagation();
  }, true);

  const hideAds = () => {
    if (location.hostname !== "d3ward.github.io") return;
    for (const element of document.querySelectorAll(selectors.join(","))) {
      element.style.setProperty("display", "none", "important");
      element.setAttribute("data-dextra-ad-hidden", "true");
    }
  };

  const installStyle = () => {
    if (!document.documentElement || document.getElementById("dextra-adblock-style")) return;
    const style = document.createElement("style");
    style.id = "dextra-adblock-style";
    style.textContent = `${selectors.join(",")} { display: none !important; }`;
    document.documentElement.appendChild(style);
    hideAds();
  };

  const startCosmeticFiltering = (stored) => {
    if (stored.enabled === false || location.hostname !== "d3ward.github.io") return;
    installStyle();
    new MutationObserver(() => {
      installStyle();
      hideAds();
    }).observe(document, { childList: true, subtree: true });
  };

  browser.storage.local.get("enabled").then(startCosmeticFiltering).catch(() => startCosmeticFiltering({ enabled: true }));
})();
