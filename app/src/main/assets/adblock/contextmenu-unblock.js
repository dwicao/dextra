(() => {
  "use strict";

  // Some sites install these handlers before GeckoView can dispatch a native menu.
  const ignorePageHandler = (target, property) => {
    try {
      Object.defineProperty(target, property, {
        configurable: true,
        enumerable: true,
        get: () => null,
        set: () => {},
      });
    } catch (_) {}
  };

  ignorePageHandler(window, "oncontextmenu");
  ignorePageHandler(document, "oncontextmenu");
  ignorePageHandler(document, "onmousedown");
  ignorePageHandler(document, "ondragstart");
})();
