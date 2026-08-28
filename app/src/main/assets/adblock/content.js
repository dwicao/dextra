(() => {
  "use strict";

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

})();
