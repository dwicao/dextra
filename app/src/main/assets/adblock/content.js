(() => {
  "use strict";

  const selectors = [".adbox.banner_ads.adsbox", ".textads"];

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
    if (stored.enabled === false) return;
    installStyle();
    new MutationObserver(() => {
      installStyle();
      hideAds();
    }).observe(document, { childList: true, subtree: true });
  };

  browser.storage.local.get("enabled").then(startCosmeticFiltering).catch(() => startCosmeticFiltering({ enabled: true }));
})();
