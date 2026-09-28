(function () {
    "use strict";

    const STORAGE_KEY = "sierra-theme";
    const DARK = "dark";
    const LIGHT = "light";

    function preferredTheme() {
        const savedTheme = localStorage.getItem(STORAGE_KEY);

        if (savedTheme === DARK || savedTheme === LIGHT) {
            return savedTheme;
        }

        return window.matchMedia("(prefers-color-scheme: dark)").matches
                ? DARK : LIGHT;
    }

    function applyTheme(theme) {
        document.documentElement.dataset.theme = theme;

        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            const darkMode = theme === DARK;
            const label = darkMode
                    ? button.dataset.lightLabel : button.dataset.darkLabel;

            button.setAttribute("aria-label", label);
            button.setAttribute("title", label);
            button.setAttribute("aria-pressed", String(darkMode));
            button.querySelector(".pi").className = darkMode
                    ? "pi pi-sun" : "pi pi-moon";
        });
    }

    applyTheme(preferredTheme());

    document.addEventListener("DOMContentLoaded", function () {
        applyTheme(preferredTheme());
    });

    if (!window.sierraThemeToggleBound) {
        document.addEventListener("click", function (event) {
            const button = event.target.closest("[data-theme-toggle]");
            if (!button) {
                return;
            }

            const nextTheme = document.documentElement.dataset.theme === DARK
                    ? LIGHT : DARK;

            localStorage.setItem(STORAGE_KEY, nextTheme);
            applyTheme(nextTheme);
        });
        window.sierraThemeToggleBound = true;
    }
}());
